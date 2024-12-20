### nn.Conv2d

- 레이어

  2D 컨볼루션 레이어로, 이미지의 공간적 특징을 추출하기 위해 사용됩니다. 작은 필터(커널)를 사용하여 이미지의 작은 영역을 스캔하며, 패턴, 경계, 색상 등의 특징을 학습합니다. 일반적으로 필터의 개수가 많을수록 다양한 특징을 포착할 수 있으며, 모델의 깊이에 따라 추출되는 특징의 복잡성이 증가합니다.

- 파라미터

  - `in_channels` : 입력 이미지의 채널 수를 지정합니다. 예를 들어, RGB 이미지는 3채널이므로 3으로 설정합니다.
  - `out_channels` : 출력할 채널 수로, 필터의 개수를 의미합니다. 이 값이 클수록 다양한 특징을 추출할 수 있지만, 계산 비용이 증가합니다.
  - `kernel_size` : 필터의 크기입니다. 일반적으로 (3, 3) 또는 (5, 5)와 같은 정사각형 필터를 많이 사용하며, 필터가 클수록 더 많은 정보를 얻을 수 있지만 계산 비용이 증가합니다.

- 팁
  1. **필터 수 결정** : out_channels는 특징 추출의 다양성과 모델의 계산량에 영향을 주므로, 적절한 값을 설정하여 복잡성을 조절하세요.
  2. **작은 커널부터 시작** : 일반적으로 작은 커널(예: 3x3)을 여러 번 사용하는 것이 큰 커널(예: 7x7)을 사용하는 것보다 좋은 성능을 보입니다.
  3. **초기 레이어에 낮은 채널 수 사용** : 모델의 초기 레이어에는 상대적으로 작은 out_channels를 사용하고, 모델이 깊어질수록 증가시키는 것이 일반적입니다.
