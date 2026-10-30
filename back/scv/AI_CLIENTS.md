# AI HTTP clients

학습·분석·벡터 삭제 요청은 `AiModelGateway`를 통해 호출합니다. HTTP 경로는 `AiTrainClient`, `AiAnalysisClient`, `AiSearchClient`에 선언하고 `AiClientConfig`에서 RestClient 기반 프록시를 등록합니다.

| 환경 변수 | 기본값 / 기존 설정 |
| --- | --- |
| FAST_MODEL_TRAIN_BASE_URL | 기존 FAST_MODEL_TRAIN_HOST_NAME / FAST_MODEL_TRAIN_PORT 조합 |
| FAST_MODEL_TEST_BASE_URL | 기존 FAST_MODEL_TEST_HOST_NAME / FAST_MODEL_TEST_PORT 조합 |
| FAST_MODEL_SEARCH_BASE_URL | http://fast-search-service.scv.svc.cluster.local:8001 |
| FAST_CONNECT_TIMEOUT | 5s |
| FAST_MODEL_TRAIN_READ_TIMEOUT | 30m |
| FAST_MODEL_TEST_READ_TIMEOUT | 5m |
| FAST_MODEL_SEARCH_READ_TIMEOUT | 10s |

기존 학습·분석 서버 환경 변수는 계속 사용할 수 있습니다. 타임아웃은 배포 환경의 학습 시간에 맞춰 조정하세요. 타임아웃이 발생해도 원격 학습이 취소된다는 보장은 없습니다. 중복 학습을 방지하기 위해 애플리케이션 수준의 자동 재시도는 추가하지 않았습니다.

AI 응답 시간 초과는 504, 연결 실패는 503, HTTP 오류 및 잘못된 응답은 502로 변환합니다. 오류 본문에는 외부 서버 응답을 노출하지 않습니다. 정확도 등 필수 값 누락, 다른 모델/버전 응답은 저장 전에 거부합니다. 분석 응답의 이미지 문자열과 JSON 배열은 기존 DB 저장 형식을 유지합니다.

벡터 삭제 실패 시 로그를 남기고 DB 삭제를 계속하는 기존 정책은 유지합니다. 트랜잭션 경계 분리와 삭제 재시도는 후속 작업입니다.

회귀 테스트: `gradle test --tests "com.scv.global.ai.*" --no-daemon`
