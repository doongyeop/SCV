### ReLU

- 레이어

  ReLU(Rectified Linear Unit)는 비선형 활성화 함수로, 입력 값이 0보다 작으면 0으로, 0보다 크면 그대로 출력합니다. 연산이 간단해 CNN에서 기본 활성화 함수로 널리 사용됩니다.

- 파라미터

  - N/A

- 팁
  1. **학습 속도 향상** : ReLU는 그 자체로 계산이 간단해 학습 속도가 빠르며, 그로 인해 모델이 더 깊어지더라도 효율적으로 학습할 수 있습니다.
  2. **기본 활성화 함수로 사용** : 대부분의 CNN 모델에서 기본 활성화 함수로 사용되며, 간단한 구조에서는 주로 ReLU만으로도 좋은 성능을 얻을 수 있습니다.