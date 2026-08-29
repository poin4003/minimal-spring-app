# AI Search Infrastructure

## Hiện trạng

Semantic search hiện dùng multilingual E5 chạy qua ONNX Runtime để tạo
embedding và Apache Lucene làm vector index cục bộ. Đây là baseline phù hợp với
hạ tầng hiện tại vì ứng dụng vẫn dùng H2, chạy monolith và không cần thêm một
dịch vụ database riêng.

`post_search_index_state` là nguồn sự thật về lifecycle của projection tìm
kiếm. Mỗi thay đổi của post tăng `requested_revision`; worker lần lượt đi qua
`PENDING`, `QUEUED`, `PROCESSING` và `SYNCED`, hoặc chuyển sang `FAILED` để retry.
Lease ngăn nhiều worker xử lý cùng một state, còn `indexed_generation` làm các
state cũ tự trở thành recovery candidate khi Lucene index được tạo lại.
State vẫn được đánh dấu khi search đang tắt; chỉ bước enqueue và inference được
điều kiện hóa bởi capability. Nhờ vậy bật lại search không làm sống lại document
Lucene đã cũ hoặc bỏ sót các mutation xảy ra trong thời gian feature bị tắt.

Recurring reconciliation chỉ lấy tối đa `AI_SEARCH_RECONCILIATION_BATCH_SIZE`
state cần recovery. Dung lượng còn lại của batch mới được dùng để tạo state
backfill cho post public chưa từng được index. Vì vậy tác vụ định kỳ không quét
toàn bộ DB hay đọc toàn bộ document trong Lucene.

## Quyết định PostgreSQL

Nếu sau này chủ động chuyển hạ tầng sang PostgreSQL, lựa chọn ưu tiên cho vector
search là pgvector thay vì tiếp tục duy trì Lucene như một projection riêng.
Khi đó schema, truy vấn similarity, index strategy và lifecycle embedding phải
được thiết kế lại cho PostgreSQL/pgvector; không coi đây là thao tác chỉ đổi JDBC
URL.

Việc chấp nhận code phụ thuộc hạ tầng ở nhịp chuyển đổi đó là có chủ đích. Một
đợt thay stack thực tế còn kéo theo migration dữ liệu, cache sang Redis, vận
hành, backup, deployment và quan sát hệ thống. Dự án không cần gánh đồng thời
hai implementation H2/Lucene và PostgreSQL/pgvector chỉ để giữ tính trừu tượng.

pgvector có thể loại bỏ bước đồng bộ DB sang Lucene, nhưng không loại bỏ nhu cầu
quản lý model version, trạng thái tạo embedding, retry và backfill. Các phần
lifecycle này nên được tái sử dụng hoặc migrate có chủ đích khi đổi hạ tầng.
