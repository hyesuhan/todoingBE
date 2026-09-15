#!/usr/bin/env bash
# 사용법: loadtest/debounce-delay-stats.sh <앱 로그 파일>
# ChatDebounceTimerManager가 남기는 "예약 200ms 대비 실제 발화까지 NNNms" 로그에서 통계 추출.
set -euo pipefail

if [ $# -lt 1 ]; then
  echo "사용법: $0 <앱-로그-파일>" >&2
  exit 1
fi

grep -oE '실제 발화까지 [0-9]+ms' "$1" \
  | grep -oE '[0-9]+' \
  | sort -n \
  | awk '
    { a[NR] = $1; sum += $1 }
    END {
      if (NR == 0) { print "매칭되는 로그 없음 — 앱이 이 로그 파일로 stdout을 리다이렉트했는지 확인"; exit 1 }
      p50 = a[int(NR * 0.50 + 0.5)]
      p95 = a[int(NR * 0.95 + 0.5)]
      printf "n=%d  min=%dms  avg=%.1fms  p50=%dms  p95=%dms  max=%dms\n", NR, a[1], sum / NR, p50, p95, a[NR]
    }'
