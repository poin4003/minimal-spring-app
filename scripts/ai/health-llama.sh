#!/bin/sh
set -eu

health_url="${1:-${LLAMA_HEALTH_URL:-http://127.0.0.1:8081/health}}"
timeout_seconds="${2:-${LLAMA_STARTUP_TIMEOUT_SECONDS:-180}}"

if ! command -v curl >/dev/null 2>&1; then
    echo "curl is required for the llama-server health check." >&2
    exit 1
fi

deadline=$(( $(date +%s) + timeout_seconds ))
while [ "$(date +%s)" -lt "$deadline" ]; do
    if curl --fail --silent --show-error --max-time 3 "$health_url" >/dev/null; then
        echo "llama-server is healthy at $health_url"
        exit 0
    fi
    sleep 1
done

echo "llama-server did not become healthy within ${timeout_seconds}s: $health_url" >&2
exit 1
