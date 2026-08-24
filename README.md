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
- AI moderation tùy chọn, tách khỏi moderation core qua internal client

## Yêu cầu

- JDK 21
- GNU Make
- Maven trên Windows; Linux/macOS sử dụng Maven Wrapper `./mvnw`
- FFmpeg và FFprobe có trong `PATH`
- Kết nối Internet trong lần build đầu tiên

AI không bắt buộc. Nếu `POST_AI_MODERATION_ENABLED=false`, dự án chạy bình
thường mà không cần model AI.

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
| `make dev` | Chạy trực tiếp bằng Spring Boot với profile `dev`. |
| `make build` | Clean và đóng gói executable JAR vào `target/`. Lệnh này bỏ qua test. |
| `make run` | Chạy JAR đã build với profile trong `APP_ENV`, mặc định là `dev`. |
| `make clean` | Xóa Maven build output trong `target/`. |
| `make check-build` | Kiểm tra executable JAR đã tồn tại trước khi chạy. |

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

Các file `.env`, `data/`, `.runtime/` và `ai-models/` là dữ liệu theo từng máy
và không được commit vào Git.
