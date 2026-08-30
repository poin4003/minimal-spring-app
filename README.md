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
- ONNX Runtime và multilingual E5 cho text embedding local tùy chọn
- Apache Lucene cho semantic vector index local tùy chọn

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
| `make dev` | Chạy Spring Boot profile `dev` với ONNX CPU runtime. |
| `make dev-gpu` | Chạy Spring Boot profile `dev` với ONNX CUDA runtime. |
| `make ai-setup` | Setup các model AI có capability đang bật. |
| `make jlama-setup` | Chủ động tải model Jlama, không phụ thuộc cờ enabled. |
| `make embedding-setup` | Chủ động tải tokenizer và model multilingual E5 ONNX. |
| `make vision-setup` | Chủ động tải và kiểm tra model CLIP ONNX. |
| `make ai-benchmark` | Chạy ba benchmark AI, ONNX dùng CPU runtime. |
| `make ai-benchmark-gpu` | Chạy ba benchmark AI, ONNX dùng CUDA runtime. |
| `make benchmark-embedding` | Chỉ benchmark multilingual E5. |
| `make benchmark-vision` | Chỉ benchmark CLIP ONNX. |
| `make benchmark-jlama` | Chỉ benchmark Jlama generation. |
| `make build` | Đóng gói JAR với ONNX CPU runtime. Lệnh này bỏ qua test. |
| `make build-gpu` | Đóng gói JAR với ONNX CUDA runtime. Lệnh này bỏ qua test. |
| `make run` | Chạy JAR đã build với profile trong `APP_ENV`, mặc định là `dev`. |
| `make clean` | Xóa Maven build output trong `target/`. |
| `make check-build` | Kiểm tra executable JAR đã tồn tại trước khi chạy. |

### Build và chạy JAR

```bash
make build
make run
```

Máy có NVIDIA CUDA có thể build biến thể GPU rồi chạy cùng lệnh `make run`:

```bash
make build-gpu
make run
```

`make build` dùng artifact `onnxruntime` gọn hơn và chạy được trên mọi máy hỗ
trợ ONNX Runtime. `make build-gpu` kích hoạt Maven profile `onnx-gpu`, thay nó
bằng `onnxruntime_gpu`; dependency này lớn hơn đáng kể vì chứa native CUDA
runtime. Hai lệnh tạo cùng tên JAR, nên bản build sau cùng là bản được `make run`
sử dụng.

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

Để chuẩn bị các model cho những capability đang bật:

```bash
make ai-setup
```

`make ai-setup` gọi Jlama setup khi `AI_GENERATION_ENABLED=true`, embedding
setup khi `AI_EMBEDDING_ENABLED=true`, sau đó gọi vision setup khi
`AI_VISION_ENABLED=true`. Có thể chủ động setup từng model bằng
`make jlama-setup`, `make embedding-setup` hoặc `make vision-setup` mà không cần
bật capability. Các lệnh setup luôn dùng ONNX CPU runtime để máy không có CUDA
vẫn tải và kiểm tra model được; model đã tải dùng chung cho cả bản CPU và GPU.

Các model được tải vào thư mục tương ứng và không được commit vào Git. Với
repository riêng tư, đặt thêm `HF_TOKEN` trong môi trường chạy lệnh. Vision
setup pin model theo commit, kiểm tra SHA-256 và ONNX contract, chạy smoke
inference rồi ghi `.ready.json` vào thư mục revision.

Embedding setup tải cả `multilingual-e5-small` và tokenizer đã pin checksum.
Runtime cung cấp vector 384 chiều đã L2-normalize cho query và passage.

Lucene search infrastructure được bật bằng `AI_SEARCH_ENABLED=true` và lưu
index dưới `AI_SEARCH_INDEX_DIRECTORY`. Runtime chỉ chuyển sang `READY` khi
embedding runtime cũng đã sẵn sàng. Index được khóa theo schema version, model
version và vector dimension; metadata không khớp sẽ tạo lại projection rỗng để
tránh trộn vector từ các model khác nhau. Các factual event như
`PostPublishedEvent` và `PostArchivedEvent` cùng implement `PostMutationEvent`;
search handler chỉ enqueue sau khi transaction commit. JobRunr đọc lại DB rồi
index các post `ACTIVE + PUBLISHED`, hoặc xóa document nếu post không còn public.
Nội dung index
là standard content, short caption, hoặc video title + description. Bài chỉ có
media chưa được index cho đến khi vision label được tích hợp. Recurring job
`RECONCILE_POST_SEARCH_INDEX` chạy mỗi 15 phút và chỉ xử lý tối đa một batch
state `PENDING`, `FAILED`, lease hết hạn hoặc sai Lucene generation. Phần còn
lại của batch được dùng để backfill post public chưa có state; job không quét
toàn bộ DB hay toàn bộ Lucene index. Chi tiết lifecycle và định hướng pgvector
khi chuyển sang PostgreSQL nằm trong
[AI Search Infrastructure](docs/guides/ai-search-infrastructure.md). Route public
và UI tìm kiếm chưa được thêm ở đợt này.

ONNX có thể chọn execution provider riêng cho từng capability bằng
`AI_EMBEDDING_EXECUTION_PROVIDER` và `AI_VISION_EXECUTION_PROVIDER`, với các giá
trị `CPU`, `CUDA` hoặc `AUTO`. CUDA dùng device và giới hạn VRAM từ
`AI_ONNX_CUDA_DEVICE_ID` và `AI_ONNX_CUDA_MEMORY_LIMIT_MB`. Khi
`AI_ONNX_FALLBACK_TO_CPU=true`, runtime tự quay về CPU nếu máy không có CUDA
provider hoặc thiếu CUDA/cuDNN. Dự án không tự cài CUDA toolkit hay cuDNN.
`make dev` chủ động đặt hai provider về `CPU`; `make dev-gpu` đặt chúng thành
`CUDA` và kích hoạt đúng Maven profile GPU.

## Benchmark AI

Setup các model cần đo trước, sau đó chạy toàn bộ benchmark:

```bash
make ai-benchmark AI_BENCHMARK_OUTPUT_DIRECTORY=data/benchmarks/dev
```

Để đo ONNX bằng CUDA trên máy đã cài CUDA/cuDNN:

```bash
make ai-benchmark-gpu AI_BENCHMARK_OUTPUT_DIRECTORY=data/benchmarks/dev-gpu
```

Trên NUC dùng cùng source và lệnh, đổi output thành `data/benchmarks/nuc`.
Mỗi capability chạy trong một Maven JVM riêng và tạo ba file `embedding.json`,
`vision.json`, `jlama.json`. Có thể chạy riêng từng model:

```bash
make benchmark-embedding AI_BENCHMARK_OUTPUT_DIRECTORY=data/benchmarks/dev
make benchmark-vision AI_BENCHMARK_OUTPUT_DIRECTORY=data/benchmarks/dev
make benchmark-jlama AI_BENCHMARK_OUTPUT_DIRECTORY=data/benchmarks/dev
```

Số warm-up, iterations và Jlama output tokens có thể chỉnh bằng các biến Make
`AI_BENCHMARK_EMBEDDING_WARMUP`, `AI_BENCHMARK_EMBEDDING_ITERATIONS`,
`AI_BENCHMARK_VISION_WARMUP`, `AI_BENCHMARK_VISION_ITERATIONS`,
`AI_BENCHMARK_JLAMA_WARMUP`, `AI_BENCHMARK_JLAMA_ITERATIONS` và
`AI_BENCHMARK_JLAMA_MAX_TOKENS`.

Report ONNX ghi cả provider được yêu cầu và provider thực tế. Nếu cấu hình CUDA
nhưng report có `activeExecutionProvider=CPU`, hãy kiểm tra CUDA/cuDNN và log
fallback trước khi so sánh hiệu năng.

Vision hiện chỉ benchmark ONNX smoke inference bằng tensor đúng shape, chưa
bao gồm decode/preprocess ảnh hoặc đánh label. Java report có số liệu JVM heap,
không phải toàn bộ native RSS của ONNX/Jlama. Report được lưu dưới `data/` nên
không được commit vào Git.

`make dev`, `make build` và `make run` không tự tải model. Vì vậy cùng một source
và JAR vẫn chạy được trên máy không có model khi các capability tương ứng tắt.

Khi `AI_GENERATION_ENABLED=true`, Spring sẽ load model Jlama local trong lúc
khởi động và đóng model khi ứng dụng dừng. Generation là capability dùng chung,
không phụ thuộc vào việc post moderation đang bật hay tắt. Nếu model bị thiếu
hoặc không load được, ứng dụng vẫn khởi động; moderation tự động được đánh dấu
`UNAVAILABLE` còn moderation thủ công tiếp tục hoạt động. V1 chỉ đưa text vào
Jlama, còn thumbnail URL được giữ làm reference trong moderation request và log
chứ chưa được model phân tích hình ảnh.

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

Các file `.env`, `data/` và `.runtime/` là dữ liệu theo từng máy
và không được commit vào Git.
