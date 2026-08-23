# llama-server Runtime

## Lifecycle

`llama-server` is an independent machine-level process. Spring calls it over
HTTP but does not start or stop it. Restarting Spring therefore does not reload
the model.

## Windows Development

Start the AI sidecar before Spring:

```powershell
make ai-up
make dev
```

`make dev` also runs `ai-up` and the start script is idempotent. Check or stop
the managed process with:

```powershell
make ai-health
make ai-down
```

Runtime PID and logs are stored under `.runtime/llama-server`.

## Arch Linux Production

Install the application under `/opt/minimal-spring-app`, including the Linux
`llama-server` binary, GGUF files, and `scripts/ai` directory. Create the
service account and machine configuration:

```bash
sudo useradd --system --home /opt/minimal-spring-app --shell /usr/bin/nologin minimal-spring-app
sudo install -d -o minimal-spring-app -g minimal-spring-app /opt/minimal-spring-app
sudo install -d -m 0750 /etc/minimal-spring-app
sudo cp llama-server.env.example /etc/minimal-spring-app/llama-server.env
sudo cp .env.example /etc/minimal-spring-app/app.env
```

Review both environment files, then install and enable the services:

```bash
sudo cp environment/systemd/llama-server.service /etc/systemd/system/
sudo cp environment/systemd/minimal-spring-app.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now llama-server.service minimal-spring-app.service
```

Check runtime state and logs with:

```bash
systemctl status llama-server.service minimal-spring-app.service
journalctl -u llama-server.service -f
journalctl -u minimal-spring-app.service -f
```

Spring uses `Wants` and `After`, so llama is started first but an AI failure does
not stop manual moderation or the rest of the application. Both services start
at boot; restarting Spring does not restart llama.
