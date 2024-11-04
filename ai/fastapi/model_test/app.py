from typing import Union, Literal, List
from fastapi import FastAPI, status
from classes import *
from utils import *
from exception import *
from fastapi.responses import JSONResponse
from dotenv import load_dotenv
from load_minio import load_model_from_minio, load_dataset_from_minio
from model_layer_class import deserialize_layers, serialize_layers
import httpx
import os
import torch

app = FastAPI(root_path="/fast/v1/model/test")

fast_match_host_name = os.getenv("FAST_MATCH_HOST_NAME")
fast_match_port = os.getenv("FAST_MATCH_PORT")

# 모델이 확정되어 결과 분석, CKA 저장 하는 함수
@app.post("/analyze/{model_version_id}/{dataset}", response_model=Model_Analyze_Response)
async def analyze_model(model_version_id: str, dataset: Literal["mnist", "fashion_mnist", "cifar10", "svhn", "emnist"], req : Model_Analyze_Request):

    # model 가져오기
    model = load_model_from_minio(model_version_id)
    layers = req.layers
    # 데이터 셋 가져오기
    test_dataset = load_dataset_from_minio(dataset, "test")
    outputs = []

    # 테스트
    model.eval()
    with torch.no_grad():
        for input, label in test_dataset:
            outputs.append({
                "input" : input,
                "label": label,
                "output": model(input)
            })

    code = get_code() # 현재
    test_accuracy = get_test_accuracy() # 현재
    test_loss = get_test_loss() # 현재
    train_info = get_train_info() # 현재
    confusion_matrix = get_confusion_matrix() # 현재
    example_image = get_example_image(outputs, dataset) # 나
    total_params = get_total_params() # 현재
    params = get_params() # 현재
    feature_activation = get_feature_activation() # 나
    activation_maximization = get_activation_maximization() # 나

    # Milvus CKA 저장
    # 보내기만 하면 되므로, await 미사용

    layer_id=3
    async with httpx.AsyncClient() as client:
        res = await client.post(f"http://{fast_match_host_name}:{fast_match_port}/fast/v1/model/match/{model_version_id}/{layer_id}",
                    json={
                        "test_accuracy": test_accuracy,
                        "layers" : layers,
                        "cka_vec": [
                            0.0,1.0,2.0
                        ]
                    })
        print(res)

    return {
        "model_version_id": model_version_id,
        "code": code,
        "dataset": dataset,
        "test_accuracy":test_accuracy,
        "test_loss": test_loss,
        "train_info":train_info,
        "confusion_matrix": confusion_matrix,
        "example_image": example_image,
        "total_params": total_params,
        "params": params,
        "feature_activation" : feature_activation,
        "activation_maximization": activation_maximization
    }


@app.exception_handler(ModelNotFound)
def invalid_model_id_exception_handler(req, exc: ModelNotFound):
    return JSONResponse(
        status_code=status.HTTP_404_NOT_FOUND,
        content={
            "content": "model을 찾을 수 없습니다.",
            "model_version_id" : exc.model_version_id
        }
    )