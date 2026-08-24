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
- Jlama cho AI moderation local tùy chọn, tách khỏi moderation core qua internal client

## Yêu cầu

- JDK 21
- GNU Make
- Maven trên Windows; Linux/macOS sử dụng Maven Wrapper `./mvnw`
- FFmpeg và FFprobe có trong `PATH`
- Kết nối Internet trong lần build đầu tiên

AI không bắt buộc. Nếu `POST_AI_MODERATION_ENABLED=false`, dự án chạy bình
thường mà không cần model AI. Các lệnh Makefile đã tự thêm Java preview flags
cần thiết cho Jlama trên JDK 21.

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
| `make ai-setup` | Tải model Jlama tùy chọn theo cấu hình vào máy hiện tại. |
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

## AI moderation tùy chọn

Để chuẩn bị model mặc định cho máy có sử dụng AI:

```bash
make ai-setup
```

Model được tải vào `POST_AI_MODERATION_MODEL_DIRECTORY` và không được commit
vào Git. Có thể đổi model bằng `POST_AI_MODERATION_MODEL_ID`; với repository
riêng tư, đặt thêm `HF_TOKEN` trong môi trường chạy lệnh.

`make dev`, `make build` và `make run` không tự tải model. Vì vậy cùng một source
và JAR vẫn chạy được trên máy không có model khi
`POST_AI_MODERATION_ENABLED=false`.

Khi `POST_AI_MODERATION_ENABLED=true`, Spring sẽ load model local trong lúc
khởi động và đóng model khi ứng dụng dừng. Nếu model bị thiếu hoặc không load
được, ứng dụng vẫn khởi động nhưng AI được đánh dấu `UNAVAILABLE`; moderation
thủ công tiếp tục hoạt động. V1 chỉ đưa text vào Jlama, còn thumbnail URL được
giữ làm reference trong moderation request và log chứ chưa được model phân
tích hình ảnh.

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
