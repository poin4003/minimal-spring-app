# llama-server Runtime

## Lifecycle

`llama-server` is an independent machine-level process. Spring calls it over
HTTP but does not start or stop it from Java. Make and systemd consult
`POST_AI_MODERATION_ENABLED` before starting the optional sidecar.

## Windows Development

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
