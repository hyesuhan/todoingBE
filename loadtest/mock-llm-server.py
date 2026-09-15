#!/usr/bin/env python3
"""
OpenAI 호환 chat completions mock 서버. 고정 지연(MOCK_DELAY_SECONDS)만 흉내내서
LLM 응답 시간의 변동성/rate limit 없이 "파이프라인 자체의 오버헤드"만 측정하기 위한 용도.
trouble-shooting/01-gpt-call-blocks-thread.md 측정 방법에서 사용.

사용법:
    MOCK_DELAY_SECONDS=2 python3 loadtest/mock-llm-server.py
"""
import json
import os
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

DELAY_SECONDS = float(os.environ.get("MOCK_DELAY_SECONDS", "2"))
PORT = int(os.environ.get("MOCK_PORT", "8090"))


class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        content_length = int(self.headers.get("Content-Length", 0))
        self.rfile.read(content_length)  # 요청 바디는 안 씀, 그냥 비움

        time.sleep(DELAY_SECONDS)

        body = json.dumps({
            "choices": [
                {"message": {"role": "assistant", "content": "mock response"}}
            ]
        }).encode("utf-8")

        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        pass  # 요청마다 콘솔에 찍히는 걸 막음


if __name__ == "__main__":
    # ThreadingHTTPServer여야 동시 요청을 병렬로 받음 (mock 자체가 병목이 되면 측정이 오염됨)
    server = ThreadingHTTPServer(("0.0.0.0", PORT), Handler)
    print(f"mock LLM server on :{PORT}, delay={DELAY_SECONDS}s")
    server.serve_forever()
