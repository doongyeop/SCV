from minio import Minio
from dotenv import load_dotenv
from io import BytesIO
import os
import pickle

from torchvision.datasets import MNIST, FashionMNIST, CIFAR10, SVHN, EMNIST
from torchvision import transforms

from collections import defaultdict

from time import sleep

from torch.utils.data import Subset 



load_dotenv(verbose=True)
minio_user_name=os.getenv("MINIO_ROOT_USER")
minio_user_password=os.getenv("MINIO_ROOT_PASSWORD")
minio_host_name=os.getenv("MINIO_HOST_NAME")
minio_model_bucket=os.getenv("MINIO_MODEL_BUCKET")
minio_dataset_bucket=os.getenv("MINIO_DATASET_BUCKET")

client = Minio("{}:9002".format(minio_host_name),
        access_key=minio_user_name,
        secret_key=minio_user_password,
        # SSL 설정 해제
        secure=False
    )

def upload_dataset_to_minio(data, object_name):
    buffer = BytesIO()
    pickle.dump(data, buffer)
    buffer.seek(0)

    client.put_object(
        bucket_name=minio_dataset_bucket,
        object_name=object_name,
        data=buffer,
        length=buffer.getbuffer().nbytes
    )
    print(f"{object_name} 데이터 셋을 업로드 했습니다.")

def upload_cka_dataset_to_minio(test_data, dataset_name):
    indices_per_label = defaultdict(list)

    for idx, (image, label) in enumerate(test_data):
        if (len(indices_per_label[label])) < 10:
            indices_per_label[label].append(idx)
    
    for indices in indices_per_label.values() :
        print(len(indices))

    if all(len(indices) == 10 for indices in indices_per_label.values()):
        selected_indices = [idx for indices in indices_per_label.values() for idx in indices]
        upload_dataset_to_minio(Subset(test_data, selected_indices), f"{dataset_name}_cka")
        print(f"{dataset_name}의 cka 데이터 셋을 업로드 했습니다.")




if not client.bucket_exists(minio_model_bucket):
    client.make_bucket(minio_model_bucket)
if not client.bucket_exists(minio_dataset_bucket):
    client.make_bucket(minio_dataset_bucket)


while not client.bucket_exists(minio_dataset_bucket):
    sleep(2)


transform = transforms.Compose([transforms.ToTensor(), transforms.Normalize((0.5,), (0.5,))])

# MNIST 데이터셋
train_dataset_mnist = MNIST(root='./data', train=True, download=True, transform=transform)
test_dataset_mnist = MNIST(root='./data', train=False, download=True, transform=transform)
upload_dataset_to_minio(train_dataset_mnist,  "mnist_train")
upload_dataset_to_minio(test_dataset_mnist, "mnist_test")
upload_cka_dataset_to_minio(test_dataset_mnist, "mnist")

# Fashion-MNIST 데이터셋
train_dataset_fashion_mnist = FashionMNIST(root='./data', train=True, download=True, transform=transform)
test_dataset_fashion_mnist = FashionMNIST(root='./data', train=False, download=True, transform=transform)
upload_dataset_to_minio(train_dataset_fashion_mnist, "fashion_mnist_train")
upload_dataset_to_minio(test_dataset_fashion_mnist, "fashion_mnist_test")
upload_cka_dataset_to_minio(test_dataset_fashion_mnist, "fashion_mnist")

# # CIFAR-10 데이터셋
train_dataset_cifar10 = CIFAR10(root='./data', train=True, download=True, transform=transform)
test_dataset_cifar10 = CIFAR10(root='./data', train=False, download=True, transform=transform)
upload_dataset_to_minio(train_dataset_cifar10, "cifar10_train")
upload_dataset_to_minio(test_dataset_cifar10, "cifar10_test")
upload_cka_dataset_to_minio(test_dataset_cifar10, "cifar10")

# # SVHN 데이터셋
train_dataset_svhn = SVHN(root='./data', split='train', download=True, transform=transform)
test_dataset_svhn = SVHN(root='./data', split='test', download=True, transform=transform)
upload_dataset_to_minio(train_dataset_svhn, "svhn_train")
upload_dataset_to_minio(test_dataset_svhn, "svhn_test")
upload_cka_dataset_to_minio(test_dataset_svhn, "svhn")

# EMNIST 데이터셋
train_dataset_emnist = EMNIST(root='./data', split='letters', train=True, download=True, transform=transform)
test_dataset_emnist = EMNIST(root='./data', split='letters', train=False, download=True, transform=transform)
upload_dataset_to_minio(train_dataset_emnist, "emnist_train")
upload_dataset_to_minio(test_dataset_emnist, "emnist_test")
upload_cka_dataset_to_minio(test_dataset_emnist, "emnist")






