# 챗봇 부하 테스트 — `trouble-shooting/01-gpt-call-blocks-thread.md` 측정용

동시 사용자 여러 명이 `/chat/message`를 거의 동시에 호출했을 때, GPT 호출이 스레드를 블로킹해서 응답이 직렬화되는지 실측하기 위한 스크립트.

## 1. mock LLM 서버 실행

실제 NVIDIA API를 쓰면 rate limit/응답시간 변동성 때문에 숫자가 오염되니, 고정 지연(기본 2초)만 흉내내는 로컬 서버를 씀.

```bash
MOCK_DELAY_SECONDS=2 python3 loadtest/mock-llm-server.py
```

## 2. 앱을 mock 서버 바라보게 띄우기

`llm.nvidia.base-url`이 `LLM_BASE_URL` 환경변수로 오버라이드 가능하게 되어 있음(기본값은 실제 NVIDIA 엔드포인트).

```bash
export LLM_BASE_URL=http://localhost:8090/v1/chat/completions
./gradlew bootRun
```

(IntelliJ로 띄운다면 Run Configuration의 환경변수에 `LLM_BASE_URL=http://localhost:8090/v1/chat/completions` 추가)

Postgres/Redis 컨테이너(`docker compose up -d`)는 평소대로 떠 있어야 함.

## 3. k6 실행 — VU 수 바꿔가며 반복

```bash
k6 run -e VUS=5  -e BASE_URL=http://localhost:8080 loadtest/k6-chat-burst.js
k6 run -e VUS=10 -e BASE_URL=http://localhost:8080 loadtest/k6-chat-burst.js
k6 run -e VUS=20 -e BASE_URL=http://localhost:8080 loadtest/k6-chat-burst.js
```

## 4. 볼 것

- k6 요약의 `chat_e2e_latency_ms` 트렌드 — `med`(P50), `p(95)` 값을 `01-gpt-call-blocks-thread.md`의 표에 기록
- 실행 중 콘솔에 찍히는 `VU N: ####ms` 줄 — 완료 순서/시간이 VU 수에 비례해서 계단식으로 늘어나면 직렬 처리 증거(고치기 전), `llmExecutor` 5스레드 한도까지 비슷한 시간에 몰려서 끝나면 병렬 처리 증거(고친 후)

수정 전/후 각각 VUS=5/10/20으로 3번씩, 총 6번 돌려서 문서 표를 채우면 됨.

## 5. 결과
`k6 run -e VUS=5  -e BASE_URL=http://localhost:8080 loadtest/k6-chat-burst.js`

chat_e2e_latency_ms............: avg=6.38s   min=2.29s  med=6.42s  max=10.34s   p(90)=9.6s   p(95)=9.97s

INFO[0005] VU 1: 2295ms                                  source=console
INFO[0007] VU 5: 4357ms                                  source=console
INFO[0009] VU 2: 6424ms                                  source=console
INFO[0011] VU 4: 8492ms                                  source=console
INFO[0013] VU 3: 10348ms                                 source=console

`k6 run -e VUS=10 -e BASE_URL=http://localhost:8080 loadtest/k6-chat-burst.js`

chat_e2e_latency_ms............: avg=11.41s  min=2.29s  med=11.35s max=20.37s   p(90)=18.71s p(95)=19.54s

VU 7: 2295ms                                  source=console
INFO[0009] VU 5: 4368ms                                  source=console
INFO[0011] VU 9: 6430ms                                  source=console
INFO[0013] VU 4: 8491ms                                  source=console
INFO[0015] VU 2: 10335ms                                 source=console
INFO[0017] VU 8: 12383ms                                 source=console
INFO[0019] VU 10: 14432ms                                source=console
INFO[0021] VU 3: 16482ms                                 source=console
INFO[0024] VU 1: 18526ms                                 source=console
INFO[0025] VU 6: 20374ms                                 source=console

`k6 run -e VUS=20 -e BASE_URL=http://localhost:8080 loadtest/k6-chat-burst.js`

chat_e2e_latency_ms............: avg=21.43s min=2.26s med=21.44s max=40.57s   p(90)=36.69s p(95)=38.63s


INFO[0012] VU 10: 2261ms                                 source=console
INFO[0014] VU 4: 4318ms                                  source=console
INFO[0016] VU 17: 6375ms                                 source=console
INFO[0018] VU 3: 8427ms                                  source=console
INFO[0020] VU 1: 10279ms                                 source=console
INFO[0022] VU 8: 12329ms                                 source=console
INFO[0024] VU 2: 14379ms                                 source=console
INFO[0026] VU 5: 16429ms                                 source=console
INFO[0028] VU 6: 18480ms                                 source=console
INFO[0030] VU 13: 20527ms                                source=console
INFO[0032] VU 19: 22370ms                                source=console
INFO[0034] VU 7: 24417ms                                 source=console
INFO[0036] VU 18: 26464ms                                source=console
INFO[0038] VU 9: 28513ms                                 source=console
INFO[0040] VU 20: 30554ms                                source=console
INFO[0042] VU 14: 32607ms                                source=console
INFO[0044] VU 16: 34446ms                                source=console
INFO[0046] VU 15: 36493ms                                source=console
INFO[0048] VU 12: 38531ms                                source=console
INFO[0050] VU 11: 40573ms                                source=console