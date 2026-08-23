# Vibe

Vibe là một ứng dụng mạng xã hội xây dựng bằng Spring Boot. Hệ thống hỗ trợ
bài viết thường, Short, video HLS, quản lý media, thông báo, phân quyền và kiểm
duyệt bài viết thủ công hoặc bằng AI local.

## Công nghệ chính

- Java 21 và Spring Boot 4
- Spring MVC, Thymeleaf và HTMX
- Spring Security và RBAC
- Spring Data JPA, H2 và Flyway
- JobRunr cho background jobs
- FFmpeg/FFprobe cho image, audio và video pipeline
- llama.cpp/llama-server cho AI moderation tùy chọn

## Yêu cầu

- JDK 21
- GNU Make
- Maven trên Windows; Linux/macOS sử dụng Maven Wrapper `./mvnw`
- FFmpeg và FFprobe có trong `PATH`
- Kết nối Internet trong lần build đầu tiên
- Khoảng 5 GB dung lượng trống cho model, runtime và build cache AI

AI không bắt buộc. Nếu `POST_AI_MODERATION_ENABLED=false`, dự án chạy bình
thường mà không cần model hoặc llama-server.

## Chạy nhanh

Tạo file cấu hình local từ file mẫu.

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Linux/macOS:

```bash
cp .env.example .env
```

Kiểm tra các giá trị trong `.env`, sau đó chạy môi trường development:

```bash
make dev
```

Ứng dụng mặc định được phục vụ tại [http://localhost:8080](http://localhost:8080).
Database H2 và media local được lưu trong thư mục `data/`.

## Các lệnh Makefile

| Lệnh | Chức năng |
| --- | --- |
| `make dev` | Chạy trực tiếp bằng Spring Boot với profile `dev`. Nếu AI được bật, llama được kiểm tra hoặc khởi động trước. |
| `make build` | Clean và đóng gói executable JAR vào `target/`. Lệnh này bỏ qua test. |
| `make run` | Chạy JAR đã build với profile trong `APP_ENV`, mặc định là `dev`. |
| `make clean` | Xóa Maven build output trong `target/`. |
| `make check-build` | Kiểm tra executable JAR đã tồn tại trước khi chạy. |
| `make ai-setup` | Cài llama runtime theo hệ điều hành, tạo config, bật AI và health-check. |
| `make ai-run` | Windows: start llama nếu cần. Linux: kiểm tra systemd unit, không chặn Spring nếu AI unavailable. |
| `make ai-up` | Alias của `make ai-run`. |
| `make ai-health` | Kiểm tra strict endpoint health của llama-server. |
| `make ai-down` | Windows: dừng process do project quản lý. Linux: hiển thị lệnh systemd cần chạy. |

### Build và chạy JAR

```bash
make build
make run
```

Chạy profile production trên Linux:

```bash
APP_ENV=prod make run
```

Chạy profile production trên Windows PowerShell:

```powershell
$env:APP_ENV = "prod"
make run
```

`make run` không tự build lại. Nếu JAR chưa tồn tại hoặc source vừa thay đổi,
hãy chạy `make build` trước.

## AI moderation tùy chọn

Chạy installer để tự tải hai model GGUF từ `ggml-org`, cài runtime và cấu hình
AI:

```bash
make ai-setup
```

Các file đúng dung lượng và SHA-256 sẽ được giữ lại ở lần chạy tiếp theo.
Download dở dang được lưu với đuôi `.download` và tiếp tục khi chạy lại.
Installer chỉ đưa file vào sử dụng sau khi SHA-256 hợp lệ.

Trên Windows, installer tải llama.cpp runtime phù hợp và quản lý process local.
Trên Arch Linux, installer build Linux runtime, render `llama-server.service`
theo user và đường dẫn deploy hiện tại, sau đó start và health-check service.

Pin một phiên bản llama.cpp cụ thể trên Arch Linux:

```bash
LLAMA_CPP_REF=b12345 make ai-setup
```

Buộc tải lại model khi cần:

```powershell
# Windows
powershell -File scripts/ai/setup-llama-windows.ps1 -ForceModels
```

```bash
# Arch Linux
bash scripts/ai/setup-llama-arch.sh --force-models
```

Chi tiết vòng đời Windows, systemd và PM2 xem tại
[docs/guides/llama-server-runtime.md](docs/guides/llama-server-runtime.md).

## Kiểm thử

`make build` hiện dùng `-DskipTests`. Chạy test riêng bằng:

Windows:

```powershell
mvn test
```

Linux/macOS:

```bash
./mvnw test
```

## Cấu hình

Các cấu hình mẫu nằm trong [.env.example](.env.example). Những phần thường cần
điều chỉnh gồm database, storage, FFmpeg encoder, email, Telegram, JobRunr,
rate limit và AI moderation.

Các file `.env`, `llama-server.env`, `data/`, `.runtime/` và `ai-models/` là dữ
liệu theo từng máy và không được commit vào Git.
