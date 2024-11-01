# prompt: Mnist 를 학습하는 분류 cnn 모델을 pytorch.sequantial 모듈을 이용해서 만들어줘. 학습과정과 추론까지 포함해서

import torch
import io

from minio import Minio
from dotenv import load_dotenv
import os

load_dotenv(verbose=True)
minio_user_name=os.getenv("MINIO_USER_NAME")
minio_user_password=os.getenv("MINIO_USER_PASSWORD")
minio_host_name=os.getenv("MINIO_HOST_NAME")
minio_model_bucket=os.getenv("MINIO_MODEL_BUCKET")

client = Minio("{}:9002".format(minio_host_name),
        access_key=minio_user_name,
        secret_key=minio_user_password,
        # SSL 설정 해제
        secure=False
    )

if not client.bucket_exists(minio_model_bucket):
        client.make_bucket(minio_model_bucket)

def save_model_to_minio(model, file_name: str):
    # 모델 저장
    buffer = io.BytesIO()
    torch.save(model, buffer)
    buffer.seek(0)
    client.put_object(minio_model_bucket, file_name, buffer, length=buffer.getbuffer().nbytes)