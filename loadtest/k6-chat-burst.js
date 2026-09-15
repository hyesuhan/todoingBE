// trouble-shooting/01-gpt-call-blocks-thread.md 측정 방법:
// 동시 사용자 N명이 거의 동시에 POST /chat/message 호출 -> 각자 GET /chat/result를
// 폴링해서 결과가 도착할 때까지 걸린 시간을 기록. VU마다 서로 다른 계정을 써서
// (버퍼/타이머가 userId별로 분리되므로) "여러 유저의 동시 요청이 서로를 막는지"만 순수하게 측정.
//
// 사용법 (mock 서버 + 앱을 먼저 띄운 상태에서):
//   k6 run -e VUS=5  -e BASE_URL=http://localhost:8080 loadtest/k6-chat-burst.js
//   k6 run -e VUS=10 -e BASE_URL=http://localhost:8080 loadtest/k6-chat-burst.js
//   k6 run -e VUS=20 -e BASE_URL=http://localhost:8080 loadtest/k6-chat-burst.js
//
// 결과에서 볼 것: chat_e2e_latency_ms 트렌드의 med/p95, 그리고 콘솔에 찍히는
// VU별 라인을 순서대로 보면 됨. 고치기 전엔 완료 시각이 VU 수만큼 계단식으로
// 늘어나야 하고(직렬 처리 증거), llmExecutor 고친 후엔 5개 동시 처리까지는 거의
// 붙어서 끝나야 함.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const VUS = Number(__ENV.VUS || 5);
const RUN_ID = __ENV.RUN_ID || `${Date.now()}`;
const POLL_INTERVAL_S = 0.2;
const POLL_TIMEOUT_S = 60;

export const options = {
  scenarios: {
    chat_burst: {
      executor: 'per-vu-iterations',
      vus: VUS,
      iterations: 1,
      maxDuration: '2m',
    },
  },
};

const e2eLatency = new Trend('chat_e2e_latency_ms', true);

export function setup() {
  const tokens = [];

  for (let i = 1; i <= VUS; i++) {
    const email = `k6load-${RUN_ID}-${i}@loadtest.local`;

    const signupRes = http.post(
      `${BASE_URL}/api/users/signup`,
      JSON.stringify({ name: `k6-${RUN_ID}-${i}`, email, password: 'pw123456' }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    if (signupRes.status !== 200) {
      throw new Error(`회원가입 실패 (VU ${i}): ${signupRes.status} ${signupRes.body}`);
    }

    const loginRes = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ email, password: 'pw123456' }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    const loginBody = JSON.parse(loginRes.body);
    const token = loginBody && loginBody.result && loginBody.result.accessToken;
    if (!token) {
      throw new Error(`로그인 실패 (VU ${i}): ${loginRes.status} ${loginRes.body}`);
    }

    http.post(
      `${BASE_URL}/chat/setting`,
      JSON.stringify({
        category: '운동',
        startDate: '2026-09-15',
        endDate: '2026-09-22',
        level: '초급',
      }),
      { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } }
    );

    tokens.push(token);
  }

  return { tokens };
}

export default function (data) {
  const token = data.tokens[__VU - 1];
  const authHeaders = {
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
  };

  const t0 = Date.now();

  const sendRes = http.post(
    `${BASE_URL}/chat/message`,
    JSON.stringify({ messages: [{ role: 'user', content: `안녕, 나는 VU ${__VU}` }] }),
    authHeaders
  );

  check(sendRes, { '메시지 전송 200': (r) => r.status === 200 });

  let result = null;
  const deadline = t0 + POLL_TIMEOUT_S * 1000;

  while (Date.now() < deadline) {
    sleep(POLL_INTERVAL_S);
    const pollRes = http.get(`${BASE_URL}/chat/result`, authHeaders);
    const body = JSON.parse(pollRes.body);
    if (body && body.result) {
      result = body.result;
      break;
    }
  }

  const latencyMs = Date.now() - t0;

  check(result, { '결과 수신함 (타임아웃 아님)': (r) => r !== null });

  e2eLatency.add(latencyMs, { vu: String(__VU) });

  console.log(`VU ${__VU}: ${latencyMs}ms${result ? '' : ' (타임아웃!)'}`);
}
