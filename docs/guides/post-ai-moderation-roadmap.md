# Lộ Trình AI Auto Kiểm Duyệt Post

## 1. Mục Tiêu

Tài liệu này chốt roadmap triển khai tính năng AI auto kiểm duyệt bài đăng
theo hướng thực dụng, đủ dùng trước, rồi mới nâng chất lượng sau.

Mục tiêu hiện tại:

- Admin có thể chọn `MANUAL` hoặc `AUTO`.
- Admin có thể nhập prompt moderation.
- User submit bài xong thì bài vào `PENDING_REVIEW`.
- Worker nền sẽ xử lý bài chờ duyệt khi mode `AUTO` đang bật.
- Nếu AI lỗi, timeout, parse fail, hoặc không chắc chắn thì bài vẫn giữ
  `PENDING_REVIEW`.

## 2. Quyết Định Kiến Trúc Hiện Tại

- AI backbone mục tiêu dùng Jlama và SafeTensors trực tiếp trong JVM.
- AI runtime là optional và không có process hoặc HTTP sidecar riêng.
- V1 chỉ kiểm duyệt text. Link thumbnail vẫn được giữ trong request để tránh
  thay đổi moderation core và phục vụ khả năng multimodal/hybrid sau này.
- Post lifecycle hiện tại vẫn được giữ nguyên:
  - `PENDING_REVIEW -> PUBLISHED`
  - `PENDING_REVIEW -> REJECTED`
- Không tạo queue moderation mới nếu lifecycle hiện tại đã đáp ứng đủ.
- V1 chỉ dùng text bài đăng và metadata cơ bản để đưa ra quyết định.
- Chưa đọc full video trong giai đoạn đầu.
- AI phải trả về JSON chặt chẽ thay vì text tự do.

## 3. Đợt 1: Ship Tính Năng End-To-End

Mục tiêu của đợt này là làm cho hệ thống chạy được từ lúc user submit bài đến
lúc AI ra quyết định.

Deliverables:

- Thêm moderation mode cho admin:
  - `MANUAL`
  - `AUTO`
- Thêm nơi lưu prompt moderation hiện tại.
- Khi user submit bài:
  - bài chuyển sang `PENDING_REVIEW`
  - sau commit thì enqueue AI moderation job nếu mode là `AUTO`
- Tạo worker/job AI moderation nhận `postId`.
- Worker load lại post, build prompt, gọi internal AI client, parse kết quả,
  rồi áp dụng decision.
- Quy ước AI decision:
  - `APPROVE`
  - `REJECT`
  - `ESCALATE`
- Nếu AI fail thì không publish/reject bừa; bài giữ nguyên `PENDING_REVIEW`.
- Lưu lại log quyết định AI để phục vụ debug.

Scope nên giữ gọn:

- Chỉ cần text + metadata; thumbnail URL chỉ là reference và không được model
  text-only xem như nội dung đã kiểm tra.
- Chưa cần nhiều policy riêng lẻ.
- Chưa cần vector database.
- Chưa cần đọc sâu video/audio.

Kết quả mong đợi:

- Prompt kiểu `duyệt toàn bộ bài đăng` chạy được ngay.
- Prompt kiểu `cấm nội dung 18+` chạy được ở mức cơ bản.

## 4. Đợt 2: Làm Cứng Để Vận Hành

Đợt này tập trung vào độ ổn định và khả năng vận hành dài hạn.

Deliverables:

- Thêm `retry` hợp lý cho AI moderation job.
- Thêm recovery job quét lại các bài `PENDING_REVIEW` bị bỏ sót.
- Có `system moderator` dành riêng cho AI decision.
- Admin xem được:
  - AI đã approve hay reject
  - reason AI trả về
  - prompt snapshot đã dùng
  - raw response khi cần debug
- Có nút hoặc action để `re-run AI moderation` cho một post.
- Tách và cấu hình rõ:
  - timeout
  - concurrency
  - runtime readiness
  - số inference chạy song song

Kết quả mong đợi:

- Hệ thống ít bị rớt job hơn.
- Dễ debug hơn khi moderation sai.
- Có thể tin tưởng để bật auto mode trong môi trường thật.

## 5. Đợt 3: Nâng Chất Lượng Duyệt

Đợt này tối ưu chất lượng decision thay vì chỉ tập trung cho hệ thống chạy.

Deliverables:

- Thêm `confidence threshold`.
- Case mơ hồ hoặc độ tin cậy thấp sẽ chuyển sang `ESCALATE`.
- Bổ sung rule cứng trước AI, ví dụ:
  - blacklist từ khóa
  - account mới thì siết chặt hơn
  - media chưa `READY` thì không cho AI duyệt
- Cải thiện multimodal input:
  - image: cho 1-3 thumbnail
  - video: ưu tiên thumbnail hoặc caption trước
- Tách prompt theo policy rõ hơn:
  - sexual content
  - spam
  - violence
  - illegal goods

Kết quả mong đợi:

- Ít false approve hơn.
- Ít false reject hơn.
- Những case khó sẽ quay về moderation tay thay vì xử lý liều.

## 6. Đợt Mở Rộng Sau: Tái Sử Dụng AI Backbone

Đợt này không thuộc moderation core, mà là tận dụng lại hạ tầng AI đã có.

Mục tiêu mở rộng:

- Bot chat basic cho hệ thống.
- Search assistant theo ngữ cảnh mạng xã hội.
- RAG để trả lời đúng trọng tâm hơn khi đã có context phù hợp.

Nguyên tắc:

- Jlama text model có thể được tái sử dụng làm generator basic.
- Retrieval nên tách riêng khỏi moderation.
- Embedding model nên là model chuyên embeddings, không ép model chat/VLM làm
  luôn nhiệm vụ này.
- Giai đoạn đầu của search assistant có thể bắt đầu bằng:
  - full-text search hoặc SQL search
  - lấy top N bài liên quan
  - nhét vào prompt để model tóm tắt hoặc gợi ý

## 7. Thứ Tự Thực Hiện Khuyến Nghị

Thứ tự nên làm:

1. Đợt 1: Ship tính năng.
2. Đợt 2: Làm cứng để vận hành.
3. Đợt 3: Nâng chất lượng duyệt.

Không nên trộn RAG hoặc chatbot vào phần moderation ngay từ đầu, vì sẽ làm
scope phình nhanh và khó chốt chất lượng.

## 8. Chốt Phạm Vi V1

Trong V1 hiện tại, các quyết định đã chốt là:

- Dùng Jlama và SafeTensors làm backbone AI trong cùng JVM.
- Chưa thêm image classifier hoặc multimodal runtime.
- Không thêm native runtime branch theo hệ điều hành.
- Chưa đọc full video.
- Chưa thêm vector DB.
- Chưa nhúng chatbot hoặc RAG vào moderation flow.

V1 chỉ cần làm tốt một việc:

- tự động duyệt bài ở mức cơ bản
- an toàn khi lỗi
- dễ mở rộng cho chat và RAG sau này
