from setuptools import setup, find_packages

setup(
    name="neural-network-builder",
    version="0.1.0",
    packages=find_packages(),
    install_requires=[
        "torch>=2.0.0",
        "pydantic>=2.0.0",
        "pytest>=7.0.0",
    ],
    python_requires=">=3.8",
)