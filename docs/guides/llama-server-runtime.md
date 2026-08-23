# llama-server Runtime

## Lifecycle

`llama-server` is an independent machine-level process. Spring calls it over
HTTP but does not start or stop it from Java. Make and systemd consult
`POST_AI_MODERATION_ENABLED` before starting the optional sidecar.

## Windows Development

Download the two GGUF files, install the current Windows CPU runtime, create
local configuration, and start it with:

```powershell
make ai-setup
```

The installer is idempotent. Use `-Force` directly when the local llama.cpp
runtime should be replaced:

```powershell
powershell -File scripts/ai/setup-llama-windows.ps1 -Force
```

Set the capability in `.env` and start Spring normally:

```dotenv
POST_AI_MODERATION_ENABLED=true
```

```powershell
make dev
```

When the value is `true`, `make dev` runs `ai-run` before Spring. When it is
`false` or absent, Spring starts without llama. The start script is idempotent.
The sidecar can also be managed explicitly:

```powershell
make ai-run
make ai-health
make ai-down
```

`make ai-up` remains an alias for `make ai-run`.

Runtime PID and logs are stored under `.runtime/llama-server`.

## Arch Linux Production

The Windows `.exe` and `.dll` files are not portable. Run the Arch installer
from the deployed project; it downloads the GGUF files independently:

```bash
make ai-setup
```

It downloads and verifies the model files, installs the Arch build toolchain,
builds the current `llama-server`, writes machine configuration, renders the
systemd unit for the current project path and user, starts the unit, and
verifies its health. Use `LLAMA_CPP_REF` to pin a specific llama.cpp tag when
production requires reproducible upgrades:

```bash
LLAMA_CPP_REF=b12345 make ai-setup
```

### Manual `/opt` Layout

The following steps are the manual alternative to `make ai-setup`. They are
useful when both Spring and llama should run as dedicated system services.

Install the application under `/opt/minimal-spring-app`, including the Linux
`llama-server` binary, GGUF files, and `scripts/ai` directory. Create the
service account and machine configuration:

```bash
sudo useradd --system --home /opt/minimal-spring-app --shell /usr/bin/nologin minimal-spring-app
sudo install -d -o minimal-spring-app -g minimal-spring-app /opt/minimal-spring-app
sudo install -d -m 0750 /etc/minimal-spring-app
sudo cp .env.example /etc/minimal-spring-app/app.env
```

For an AI-enabled deployment, set `POST_AI_MODERATION_ENABLED=true`, install
the model files, and create the machine configuration:

```bash
sudo cp llama-server.env.example /etc/minimal-spring-app/llama-server.env
```

For a manual-only deployment, keep the flag `false`; the model files and
`llama-server.env` are not required. Install both unit definitions and enable
the Spring service:

```bash
sudo cp environment/systemd/llama-server.service /etc/systemd/system/
sudo cp environment/systemd/minimal-spring-app.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now minimal-spring-app.service
```

The Spring unit wants the llama unit. Its `ExecCondition` starts llama only
when `POST_AI_MODERATION_ENABLED=true`; otherwise the unit is skipped without
blocking Spring.

Check runtime state and logs with:

```bash
systemctl status llama-server.service minimal-spring-app.service
journalctl -u llama-server.service -f
journalctl -u minimal-spring-app.service -f
```

Spring uses `Wants` and `After`, so an enabled llama is started first but an AI
failure does not stop manual moderation or the rest of the application.
Changing the flag to `false` prevents future starts but does not terminate an
already running sidecar; stop it explicitly with:

```bash
sudo systemctl stop llama-server.service
```

### Spring Managed by PM2

When PM2 already manages Spring, do not also enable
`minimal-spring-app.service`. `make ai-setup` renders and enables only the
llama unit for the current deployment path and Linux user. PM2 can then be
restarted normally:

```bash
pm2 restart minimal-spring-app --update-env
```

PM2 may continue to start Spring with `make run`. On Linux, the `ai-run`
dependency only checks whether `llama-server.service` is active. A missing or
inactive unit produces a warning but does not stop Spring, because the AI
runtime is optional. Use the strict health target when deployment should verify
that llama is actually ready:

```bash
make ai-health
```
