# Kế Hoạch Stateful Worker Cho Post AI Moderation

## 1. Mục Tiêu

Worker AI moderation cần lưu trạng thái thực thi vào database để hệ thống biết
một yêu cầu đang chờ, đang chuẩn bị, đang inference, đã hoàn tất hay đã lỗi.
Trạng thái này phải độc lập với trạng thái kỹ thuật nội bộ của JobRunr.

`PostAiModerationDecisionLog` tiếp tục là audit log bất biến của quyết định cuối.
Không dùng decision log làm execution state mutable.

## 2. Execution State

Tạo entity `PostAiModerationRun` với state machine:

```text
QUEUED -> RUNNING -> COMPLETED
                  -> FAILED
QUEUED/RUNNING -> CANCELLED
```

Khi ở trạng thái `RUNNING`, run có phase chi tiết:

```text
PREPARING -> INFERENCING -> APPLYING_DECISION
```

`APPROVE`, `REJECT` và `ESCALATE` vẫn là decision outcome. Một decision
`ESCALATE` là run `COMPLETED`, không phải `FAILED`.

## 3. Dữ Liệu Dự Kiến

Các field chính của `PostAiModerationRun`:

- `id`
- `post_id`
- `jobrunr_job_id`
- `status`
- `phase`
- `attempt`
- `post_updated_at`
- `config_updated_at`
- `queued_at`
- `started_at`
- `heartbeat_at`
- `finished_at`
- `generated_tokens`
- `model_name`
- `error_message`
- `decision_log_id`
- `active_marker`

`active_marker` được dùng cùng unique constraint để mỗi post chỉ có tối đa một
run active. Run terminal sẽ bỏ marker để vẫn giữ được lịch sử nhiều lần chạy.

## 4. Luồng Xử Lý

```text
Post submitted
  -> create run QUEUED
  -> enqueue JobRunr bằng runId
  -> worker claim QUEUED -> RUNNING
  -> phase PREPARING
  -> phase INFERENCING + heartbeat
  -> phase APPLYING_DECISION
  -> update post + decision log + COMPLETED
```

Job nhận `runId` thay vì chỉ nhận `postId`. Worker phải claim run bằng update có
điều kiện. Nếu JobRunr gọi trùng cùng một run, chỉ worker đầu tiên được phép
chuyển `QUEUED` sang `RUNNING`.

Nếu runtime, parsing hoặc database gặp lỗi:

```text
run -> FAILED
post -> giữ PENDING_REVIEW
error_message -> lưu nguyên nhân phù hợp
```

Việc cập nhật post, tạo decision log và chuyển run sang `COMPLETED` phải nằm
trong cùng transaction để tránh decision đã áp dụng nhưng run còn `RUNNING`.

## 5. Heartbeat Và Progress

- Worker cập nhật heartbeat định kỳ, dự kiến mỗi 10 giây.
- Không ghi database sau mỗi token.
- Token progress phải được throttle trước khi persist.
- Phase và `generated_tokens` giúp phân biệt prefill chậm, inference đang chạy
  và worker bị mất.
- UI có thể hiển thị trạng thái như `INFERENCING - heartbeat 4s ago`.

Không đánh dấu `TIMED_OUT` rồi enqueue run mới khi inference cũ vẫn còn chạy.
Jlama chạy trong cùng JVM và interrupt không bảo đảm dừng computation ngay.
Chỉ recovery khi heartbeat thực sự stale.

## 6. Recovery Và Retry

Recovery job sẽ quét:

- `QUEUED` quá lâu nhưng chưa được xử lý.
- `RUNNING` có heartbeat quá hạn.
- Run chưa vượt quá số attempt tối đa.

Recovery phải dùng state transition có điều kiện để tránh hai recovery worker
enqueue trùng. Khi retry, tăng `attempt` và tái sử dụng cùng run hoặc tạo attempt
record theo quyết định được chốt ở đợt recovery.

## 7. Các Đợt Triển Khai

### 2A. Persistence Frame

Thêm migration, entity, status enum, phase enum, repository và state service.
Chưa thay đổi enqueue hoặc moderation behavior.

### 2B. Stateful Enqueue

Tạo run `QUEUED` trước khi enqueue, đổi job thành `execute(runId)` và lưu JobRunr
job ID. Enqueue lỗi vẫn để lại state đủ dữ liệu cho recovery sau này.

### 2C. Stateful Workflow

Worker claim run, cập nhật phase, lưu error và hoàn tất run cùng decision log.
Post vẫn giữ `PENDING_REVIEW` khi run thất bại.

### 2D. Heartbeat

Thêm heartbeat định kỳ và token progress có throttle. Không ghi theo từng token
và không đưa persistence logic vào Jlama integration adapter.

### 2E. Recovery

Thêm recovery job cho queued run bị bỏ sót và running run mất heartbeat, kèm
max attempts và idempotent transition.

### 2F. Admin UI

Hiển thị run status, phase, thời gian chạy, heartbeat, model, error và liên kết
tới decision log. Bổ sung retry action khi backend recovery contract đã ổn định.

## 8. Effect Radius

- Thêm schema và Flyway migration.
- Thêm entity, enum, repository và internal services.
- Đổi internal JobRunr payload từ `postId` sang `runId`.
- Đổi internal workflow và decision service contract.
- Không đổi public post API hoặc payload public.
- Không đổi post lifecycle `PENDING_REVIEW -> PUBLISHED/REJECTED`.
- Chưa thêm multimodal, RAG hoặc external queue.
