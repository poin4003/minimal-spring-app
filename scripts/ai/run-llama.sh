#!/bin/sh
set -eu

: "${LLAMA_SERVER_BIN:?LLAMA_SERVER_BIN is required}"
: "${LLAMA_MODEL:?LLAMA_MODEL is required}"
: "${LLAMA_MMPROJ:?LLAMA_MMPROJ is required}"

LLAMA_ALIAS="${LLAMA_ALIAS:-local-aimoderation}"
LLAMA_HOST="${LLAMA_HOST:-127.0.0.1}"
LLAMA_PORT="${LLAMA_PORT:-8081}"
LLAMA_THREADS="${LLAMA_THREADS:-4}"
LLAMA_CTX_SIZE="${LLAMA_CTX_SIZE:-4096}"
LLAMA_PARALLEL="${LLAMA_PARALLEL:-1}"

server_directory=$(CDPATH= cd -- "$(dirname -- "$LLAMA_SERVER_BIN")" && pwd)
export LD_LIBRARY_PATH="${server_directory}${LD_LIBRARY_PATH:+:${LD_LIBRARY_PATH}}"
cd "$server_directory"

exec "$LLAMA_SERVER_BIN" \
    --model "$LLAMA_MODEL" \
    --mmproj "$LLAMA_MMPROJ" \
    --alias "$LLAMA_ALIAS" \
    --host "$LLAMA_HOST" \
    --port "$LLAMA_PORT" \
    --threads "$LLAMA_THREADS" \
    --threads-batch "$LLAMA_THREADS" \
    --parallel "$LLAMA_PARALLEL" \
    --ctx-size "$LLAMA_CTX_SIZE" \
    --no-webui
