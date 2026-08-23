#!/usr/bin/env bash
set -Eeuo pipefail

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
llama_cpp_ref="${LLAMA_CPP_REF:-master}"
service_user="${SUDO_USER:-$(id -un)}"
skip_packages=false
skip_systemd=false
skip_start=false
force_models=false

usage() {
    cat <<'EOF'
Usage: setup-llama-arch.sh [options]

Options:
  --project-root PATH   Deployed project directory.
  --ref REF             llama.cpp git ref (default: master).
  --service-user USER   Linux user that runs llama-server.
  --skip-packages       Do not install Arch build dependencies.
  --skip-systemd        Build/configure only; do not install the systemd unit.
  --skip-start          Install the unit without starting it.
  --force-models        Download and verify both GGUF files again.
  --help                Show this help.
EOF
}

fail() {
    echo "ERROR: $*" >&2
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --project-root)
            [[ $# -ge 2 ]] || fail "--project-root requires a path."
            project_root="$2"
            shift 2
            ;;
        --ref)
            [[ $# -ge 2 ]] || fail "--ref requires a git ref."
            llama_cpp_ref="$2"
            shift 2
            ;;
        --service-user)
            [[ $# -ge 2 ]] || fail "--service-user requires a user."
            service_user="$2"
            shift 2
            ;;
        --skip-packages)
            skip_packages=true
            shift
            ;;
        --skip-systemd)
            skip_systemd=true
            shift
            ;;
        --skip-start)
            skip_start=true
            shift
            ;;
        --force-models)
            force_models=true
            shift
            ;;
        --help|-h)
            usage
            exit 0
            ;;
        *)
            fail "Unknown option: $1"
            ;;
    esac
done

command -v realpath >/dev/null 2>&1 || fail "realpath is required."
project_root="$(realpath "$project_root")"
[[ -d "$project_root" ]] || fail "Project directory was not found: $project_root"
[[ -f /etc/arch-release ]] || fail "This installer supports Arch Linux only."
[[ "$project_root" != *[[:space:]]* ]] \
    || fail "The systemd installer does not support whitespace in the project path."
id "$service_user" >/dev/null 2>&1 \
    || fail "Service user does not exist: $service_user"
service_group="$(id -gn "$service_user")"

models_directory="$project_root/ai-models"
runtime_directory="$models_directory/llama-server"
model_path="$models_directory/SmolVLM2-2.2B-Instruct-Q4_K_M.gguf"
mmproj_path="$models_directory/mmproj-SmolVLM2-2.2B-Instruct-f16.gguf"
model_url="${LLAMA_MODEL_URL:-https://huggingface.co/ggml-org/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/SmolVLM2-2.2B-Instruct-Q4_K_M.gguf?download=true}"
mmproj_url="${LLAMA_MMPROJ_URL:-https://huggingface.co/ggml-org/SmolVLM2-2.2B-Instruct-GGUF/resolve/main/mmproj-SmolVLM2-2.2B-Instruct-f16.gguf?download=true}"
model_sha256="0cf76814555b8665149075b74ab6b5c1d428ea1d3d01c1918c12012e8d7c9f58"
mmproj_sha256="db9a3a1648cab1ebc3af4a2b0c8145dd8faebf6f7dd7b16e7dc1842229f14ac4"
model_size=1112602656
mmproj_size=872303680
source_directory="$project_root/.runtime/llama.cpp"
build_directory="$project_root/.runtime/llama.cpp-build"
local_llama_env="$project_root/llama-server.env"
local_app_env="$project_root/.env"
system_environment_directory="/etc/minimal-spring-app"
system_llama_env="$system_environment_directory/llama-server.env"
systemd_template="$project_root/environment/systemd/llama-server.service"
systemd_unit="/etc/systemd/system/llama-server.service"
build_jobs="${LLAMA_BUILD_JOBS:-$(nproc)}"

[[ -f "$systemd_template" ]] \
    || fail "Systemd template was not found: $systemd_template"

if [[ "$skip_packages" == false ]]; then
    command -v sudo >/dev/null 2>&1 || fail "sudo is required."
    sudo pacman -S --needed --noconfirm base-devel cmake git curl
fi

for command_name in cmake git curl sha256sum stat; do
    command -v "$command_name" >/dev/null 2>&1 \
        || fail "$command_name is required."
done

model_file_matches() {
    local file="$1"
    local expected_size="$2"
    local expected_sha256="$3"
    local verify_hash="${4:-false}"

    [[ -f "$file" ]] || return 1
    [[ "$(stat -c '%s' "$file")" == "$expected_size" ]] || return 1
    if [[ "$verify_hash" == true ]]; then
        [[ "$(sha256sum "$file" | awk '{ print $1 }')" == "$expected_sha256" ]]
    fi
}

install_model_file() {
    local name="$1"
    local destination="$2"
    local url="$3"
    local expected_size="$4"
    local expected_sha256="$5"
    local temporary_file="${destination}.download"

    if [[ "$force_models" == false ]] \
            && model_file_matches \
                "$destination" "$expected_size" "$expected_sha256" true; then
        echo "$name is already installed."
        return
    fi

    mkdir -p "$(dirname "$destination")"
    if [[ -f "$temporary_file" ]] \
            && (( $(stat -c '%s' "$temporary_file") > expected_size )); then
        rm -f "$temporary_file"
    fi

    if ! model_file_matches \
            "$temporary_file" "$expected_size" "$expected_sha256" true; then
        echo "Downloading $name ($expected_size bytes)..."
        curl --location --fail --retry 5 --retry-delay 3 \
            --continue-at - --output "$temporary_file" "$url"
    fi

    model_file_matches \
        "$temporary_file" "$expected_size" "$expected_sha256" true \
        || fail "Downloaded $name failed size or SHA-256 verification."
    mv -f "$temporary_file" "$destination"
    echo "Installed $name at $destination."
}

install_model_file \
    "SmolVLM2 Q4_K_M model" \
    "$model_path" \
    "$model_url" \
    "$model_size" \
    "$model_sha256"
install_model_file \
    "SmolVLM2 multimodal projector" \
    "$mmproj_path" \
    "$mmproj_url" \
    "$mmproj_size" \
    "$mmproj_sha256"

mkdir -p "$(dirname "$source_directory")"
if [[ ! -d "$source_directory/.git" ]]; then
    git clone --filter=blob:none --no-checkout \
        https://github.com/ggml-org/llama.cpp.git \
        "$source_directory"
fi

git -C "$source_directory" fetch --depth 1 origin "$llama_cpp_ref"
# This cache is owned by the installer, so each run can safely select its ref.
git -C "$source_directory" checkout --force --detach FETCH_HEAD

cmake -S "$source_directory" -B "$build_directory" \
    -DCMAKE_BUILD_TYPE=Release
cmake --build "$build_directory" --config Release \
    --target llama-server -j "$build_jobs"

built_server="$build_directory/bin/llama-server"
[[ -x "$built_server" ]] \
    || fail "The Linux llama-server binary was not produced: $built_server"

mkdir -p "$runtime_directory"
install -m 0755 "$built_server" "$runtime_directory/llama-server"
find "$build_directory/bin" -maxdepth 1 \
    \( -type f -o -type l \) -name '*.so*' \
    -exec cp -a {} "$runtime_directory/" \;

set_env_value() {
    local file="$1"
    local name="$2"
    local value="$3"
    local temporary_file
    temporary_file="$(mktemp)"

    if [[ -f "$file" ]]; then
        awk -v name="$name" -v value="$value" '
            BEGIN { updated = 0 }
            index($0, name "=") == 1 {
                print name "=" value
                updated = 1
                next
            }
            { print }
            END {
                if (!updated) {
                    print name "=" value
                }
            }
        ' "$file" > "$temporary_file"
    else
        printf '%s=%s\n' "$name" "$value" > "$temporary_file"
    fi

    mv "$temporary_file" "$file"
}

set_env_value "$local_llama_env" "POST_AI_MODERATION_ENABLED" "true"
set_env_value "$local_llama_env" "LLAMA_SERVER_BIN" \
    "$runtime_directory/llama-server"
set_env_value "$local_llama_env" "LLAMA_MODEL" "$model_path"
set_env_value "$local_llama_env" "LLAMA_MMPROJ" "$mmproj_path"
set_env_value "$local_llama_env" "LLAMA_ALIAS" "local-aimoderation"
set_env_value "$local_llama_env" "LLAMA_HOST" "127.0.0.1"
set_env_value "$local_llama_env" "LLAMA_PORT" "8081"
set_env_value "$local_llama_env" "LLAMA_THREADS" "4"
set_env_value "$local_llama_env" "LLAMA_CTX_SIZE" "4096"
set_env_value "$local_llama_env" "LLAMA_PARALLEL" "1"
set_env_value "$local_llama_env" "LLAMA_HEALTH_URL" \
    "http://127.0.0.1:8081/health"
set_env_value "$local_llama_env" "LLAMA_STARTUP_TIMEOUT_SECONDS" "180"
set_env_value "$local_app_env" "POST_AI_MODERATION_ENABLED" "true"

if [[ "$skip_systemd" == true ]]; then
    echo "Linux llama-server is ready at $runtime_directory/llama-server."
    echo "Systemd installation was skipped."
    exit 0
fi

command -v sudo >/dev/null 2>&1 || fail "sudo is required for systemd setup."
temporary_environment="$(mktemp)"
temporary_unit="$(mktemp)"
trap 'rm -f "$temporary_environment" "$temporary_unit"' EXIT
cp "$local_llama_env" "$temporary_environment"

escaped_project_root="$(printf '%s' "$project_root" | sed 's/[&|]/\\&/g')"
sed \
    -e "s|^User=.*|User=$service_user|" \
    -e "s|^Group=.*|Group=$service_group|" \
    -e "s|/opt/minimal-spring-app|$escaped_project_root|g" \
    "$systemd_template" > "$temporary_unit"

sudo install -d -m 0750 "$system_environment_directory"
sudo install -m 0640 "$temporary_environment" "$system_llama_env"
sudo install -m 0644 "$temporary_unit" "$systemd_unit"
sudo systemctl daemon-reload
sudo systemctl enable llama-server.service

if [[ "$skip_start" == false ]]; then
    sudo systemctl restart llama-server.service
    bash "$project_root/scripts/ai/health-llama.sh"
else
    echo "llama-server.service was installed without being started."
fi

echo "Arch Linux AI runtime setup completed."
