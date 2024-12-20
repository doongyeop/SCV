from typing import Union, Literal, List
from pydantic import BaseModel
from fastapi import FastAPI, status
from pymilvus import connections, db, FieldSchema, MilvusClient
from classes import *
from exception import *
from gpt import get_gpt_answer
from fastapi.responses import JSONResponse
from openai import AsyncOpenAI
from dotenv import load_dotenv
from model_layer_class import Layer, deserialize_layers, serialize_layers
import json
import os
import redis
from fastapi.middleware.cors import CORSMiddleware
from config.cors import CORS_CONFIG

load_dotenv(verbose=True)
db_name = os.getenv("DB_NAME")
collection_name = os.getenv("COLLECTION_NAME")
# milvus_host_name = os.getenv("MILVUS_HOST_NAME")
# milvus_port = os.getenv("MILVUS_PORT")
redis_host_name = os.getenv("REDIS_HOST_NAME")
redis_port = os.getenv("REDIS_PORT")

app = FastAPI(root_path="/fast/v1/model/match")

app.add_middleware(
    CORSMiddleware,
    **CORS_CONFIG  # 설정을 언패킹하여 적용
)

client = MilvusClient(
    uri="/data/scv_milvus.db",
    db_name=db_name
)

client.load_collection(
    collection_name=collection_name
)

redis = redis.Redis(host=redis_host_name, port=redis_port, db=0)


# vectordb에서 한 레이어 조회
@app.get("/{model_id}/{version_id}/{layer_id}", response_model=Model_Read_Response)
def read_model(model_id: str, version_id: str, layer_id: str):
    print("{}_{} id 의 vector를 조회합니다.".format(f"model_{model_id}_v{version_id}", layer_id))

    res = client.get(
        collection_name=collection_name,
        ids=["{}_{}".format(f"model_{model_id}_v{version_id}", layer_id)]
    )

    if len(res) == 0:
        raise LayerNotFound()

    print(res[0])

    res[0]["layers"] = deserialize_layers(res[0]["layers"])

    return res[0]


# vectordb에 한 레이어 추가
@app.post("/{model_id}/{version_id}/{layer_id}", response_model=Model_Insert_Response)
def insert_model(model_id: str, version_id: str, layer_id: str, req: Model_Insert_Request):
    res = client.insert(
        collection_name=collection_name,
        data=[
            {
                "model_version_layer_id": f"model_{model_id}_v{version_id}_{layer_id}",
                "model_version_id": f"model_{model_id}_v{version_id}",
                "test_accuracy": req.test_accuracy,
                "layers": req.layers,
                "cka_vec": req.cka_vec
            }
        ]
    )

    print("{}_{} id로 vector를 insert 합니다. : {}".format(f"model_{model_id}_v{version_id}", layer_id, res))

    res = dict(res)

    if res["insert_count"] == 1:
        return {"model_version_layer_id": f"model_{model_id}_v{version_id}" + "_" + layer_id, "success": True}

    return {"model_version_layer_id": f"model_{model_id}_v{version_id}" + "_" + layer_id, "success": False}


# vectordb에서 모델 삭제
@app.delete("/{model_id}/{version_id}")
def delete_model(model_id: str, version_id: str):
    print(f"{f"model_{model_id}_v{version_id}"} 의 model_version_id를 가진 레이어를 지웁니다.")

    res = client.delete(
        collection_name=collection_name,
        filter="model_version_id == '{}'".format(f"model_{model_id}_v{version_id}")
    )
    print(res)
    cnt = -1
    try:
        res = dict(res)
        cnt = res["delete_count"]
    except:
        cnt = len(res)
    print("{}개의 layer가 삭제되었습니다.".format(cnt))
    return JSONResponse(
        status_code=status.HTTP_200_OK,
        content={
            "content": f"{cnt}개의 layer가 삭제되었습니다."
        }
    )


# 유사 모델 검색
@app.get("/{model_id}/{version_id}/{layer_id}/search", response_model=Model_Search_Response)
async def search_model(model_id: str, version_id: str, layer_id: str):
    model_version_layer_id = "{}_{}".format(f"model_{model_id}_v{version_id}", layer_id)
    model_version_id = f"model_{model_id}_v{version_id}"
    cached = redis.get(model_version_layer_id)
    if cached:
        print("응답이 캐싱되었습니다.")
        cached = json.loads(cached)
        cached["layers"] = deserialize_layers(cached["layers"])
        return cached

    model = client.get(
        collection_name=collection_name,
        ids=[model_version_layer_id]
    )

    if (len(model) == 0):
        raise InvalidModelId(model_version_layer_id)

    model = model[0]

    print("{} id로 가장 유사한 레이어를 검색합니다.".format(model_version_layer_id))

    results = client.search(
        collection_name=collection_name,
        data=[model["cka_vec"]],
        anns_field="cka_vec",
        output_fields=["model_version_layer_id", "test_accuracy", "cka_vec", "layers"],
        search_params={"metric_type": "IP"},
        limit=1,
        filter="model_version_id != '{}'".format(model_version_id)
        # 성능이 더 좋은 모델만 찾아주려면, 아무 것도 찾지 못했을 수 있음
        # filter="test_accuracy > {}".format(model[0]["test_accuracy"])
    )

    if (len(results[0]) == 0):
        raise LayerNotFound()

    results = dict(results[0][0])

    id_parse = results["id"].split("_")
    searched_model_version_id = id_parse[0] + id_parse[1] + id_parse[2]
    searched_layer_id = id_parse[1]
    searched_test_accuracy = results["entity"]["test_accuracy"]
    print(f"searched_model_version_id: {searched_model_version_id}")
    target = model["layers"]
    target_test_accuracy = model["test_accuracy"]
    searched = results["entity"]["layers"]

    gpt_description = await get_gpt_answer(target, searched, layer_id, searched_layer_id, target_test_accuracy,
                                           searched_test_accuracy)

    searched = deserialize_layers(searched)

    resp = {"model_version_id": searched_model_version_id, "layer_id": searched_layer_id,
            "gpt_description": gpt_description, "test_accuracy": searched_test_accuracy,
            "layers": serialize_layers(searched)}

    redis.set(model_version_layer_id, json.dumps(resp))
    redis.expire(model_version_layer_id, 3600)

    resp["layers"] = deserialize_layers(resp["layers"])
    return resp


@app.exception_handler(InvalidModelId)
def invalid_model_id_exception_handler(req, exc: InvalidModelId):
    return JSONResponse(
        status_code=status.HTTP_400_BAD_REQUEST,
        content={
            "content": "invalid model id 입니다.",
            "model_id": exc.model_version_layer_id
        }
    )


@app.exception_handler(LayerNotFound)
def layer_not_found_exception_handler(req, exc: LayerNotFound):
    return JSONResponse(
        status_code=status.HTTP_404_NOT_FOUND,
        content={
            "content": "레이어를 찾지 못했습니다."
        }
    )
