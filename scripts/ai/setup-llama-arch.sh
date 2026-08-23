#!/usr/bin/env bash
set -Eeuo pipefail

project_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
llama_cpp_ref="${LLAMA_CPP_REF:-master}"
service_user="${SUDO_USER:-$(id -un)}"
skip_packages=false
skip_systemd=false
skip_start=false

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
source_directory="$project_root/.runtime/llama.cpp"
build_directory="$project_root/.runtime/llama.cpp-build"
local_llama_env="$project_root/llama-server.env"
local_app_env="$project_root/.env"
system_environment_directory="/etc/minimal-spring-app"
system_llama_env="$system_environment_directory/llama-server.env"
systemd_template="$project_root/environment/systemd/llama-server.service"
systemd_unit="/etc/systemd/system/llama-server.service"
build_jobs="${LLAMA_BUILD_JOBS:-$(nproc)}"

[[ -f "$model_path" ]] || fail "Required GGUF file was not found: $model_path"
[[ -f "$mmproj_path" ]] || fail "Required GGUF file was not found: $mmproj_path"
[[ -f "$systemd_template" ]] \
    || fail "Systemd template was not found: $systemd_template"

if [[ "$skip_packages" == false ]]; then
    command -v sudo >/dev/null 2>&1 || fail "sudo is required."
    sudo pacman -S --needed --noconfirm base-devel cmake git curl
fi

for command_name in cmake git curl; do
    command -v "$command_name" >/dev/null 2>&1 \
        || fail "$command_name is required."
done

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
