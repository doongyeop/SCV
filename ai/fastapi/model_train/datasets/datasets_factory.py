from torch.utils.data import DataLoader, Subset, Dataset
from torchvision import datasets, transforms
import random
from typing import Tuple, Optional
from .datasets_registry import DatasetRegistry, DatasetInfo


class DatasetFactory:
    @staticmethod
    def create_transforms(dataset_info: DatasetInfo) -> Tuple[transforms.Compose, transforms.Compose]:
        transform_list = []

        if dataset_info.resize_dims:
            transform_list.append(transforms.Resize(dataset_info.resize_dims))

        transform_list.extend([
            transforms.ToTensor(),
            transforms.Normalize(dataset_info.mean, dataset_info.std)
        ])

        basic_transform = transforms.Compose(transform_list)

        if dataset_info.augmentation:
            train_transform_list = transform_list.copy()
            train_transform_list.insert(0, transforms.RandomCrop(dataset_info.input_shape[1], padding=4))
            train_transform_list.insert(1, transforms.RandomHorizontalFlip())
            train_transform = transforms.Compose(train_transform_list)
            return train_transform, basic_transform

        return basic_transform, basic_transform

    @staticmethod
    def create_dataset(
            dataset_name: str,
            train_count: Optional[int] = None,
            test_count: Optional[int] = None
    ) -> Tuple[DataLoader, DataLoader]:
        dataset_info = DatasetRegistry.get_dataset_info(dataset_name)
        train_transform, test_transform = DatasetFactory.create_transforms(dataset_info)

        dataset_mapping = {
            "MNIST": datasets.MNIST,
            "FASHION_MNIST": datasets.FashionMNIST,
            "CIFAR10": datasets.CIFAR10,
            "CIFAR100": datasets.CIFAR100,
            "EMNIST": lambda *args, **kwargs: datasets.EMNIST(*args, **kwargs, split='balanced'),
            "SVHN": datasets.SVHN
        }

        dataset_class = dataset_mapping.get(dataset_name)
        if dataset_class is None:
            raise ValueError(f"Dataset {dataset_name} is not implemented")

        train_dataset = dataset_class('data', train=True, download=True, transform=train_transform)
        test_dataset = dataset_class('data', train=False, transform=test_transform)

        if train_count and train_count < len(train_dataset):
            train_dataset = Subset(train_dataset, random.sample(range(len(train_dataset)), train_count))
        if test_count and test_count < len(test_dataset):
            test_dataset = Subset(test_dataset, random.sample(range(len(test_dataset)), test_count))

        return DataLoader(train_dataset, batch_size=64, shuffle=True), DataLoader(test_dataset, batch_size=64)