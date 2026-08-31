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

## Định Hướng Sản Phẩm

Giao diện độc lập của hạ tầng này là khu vực semantic search, không giả lập một
chatbot đa dụng. User nhập truy vấn ở thanh tìm kiếm và nhận danh sách content
liên quan được chia theo bài viết standard, short và video. Search chỉ dùng
embedding ONNX và Lucene retrieval; nó không gọi LLM generation hoặc mở kết nối
SSE.

Lucene vector index và embedding không phụ thuộc vào giao diện chat. Chúng được
giữ làm nền tảng cho các chức năng sau:

- Tìm kiếm theo ý nghĩa, đa ngôn ngữ và chịu được khác biệt từ khóa.
- Tìm content tương tự để hiển thị `Related posts` hoặc `More like this`.
- Tạo candidate cho recommendation từ các post user đã xem hoặc tương tác.
- Gom nhóm content gần nhau, hỗ trợ phát hiện chủ đề và nội dung trùng lặp.
- Bổ sung nguồn nội bộ cho các thao tác AI ngay tại post hoặc comment.

Không hiển thị Lucene KNN score như xác suất liên quan. Ngưỡng loại kết quả yếu
phải được hiệu chỉnh bằng benchmark positive/negative query thay vì chọn một
con số tùy ý.

`AI_SEARCH_MIN_SCORE` áp dụng cho kết quả semantic search. Giá trị mặc định
`0.0` tắt lọc để tránh loại nhầm kết quả trước khi corpus có bộ
positive/negative query đủ đại diện. Score hợp lệ của index `DOT_PRODUCT` hiện
tại nằm trong khoảng `0.0-1.0`, nhưng vẫn không được diễn giải hoặc hiển thị như
xác suất.

Sau khi comment lifecycle hoàn thiện, hệ thống có thể dành riêng mention
`@VibeAI`. Mention tạo một background job lấy post, nhánh comment hiện tại và
các nguồn semantic search phù hợp làm context, sau đó đăng câu trả lời bằng
system AI identity. Câu trả lời phải hiển thị công khai trong thread, có nhãn
AI, chịu rate limit và moderation, đồng thời không được tự kích hoạt thêm một
AI mention khác.

## Quyết Định Conversation History

Search request không nhận conversation history. Recent searches có thể được UI
lưu để hỗ trợ trải nghiệm, nhưng không được gửi như context cho retrieval hoặc
generation. Việc lấy một số message gần nhất rồi nối vào prompt không phải
memory lifecycle phù hợp cho sản phẩm search và có thể làm sai retrieval query.

Chỉ quay lại conversational AI sau khi backend có domain `Conversation` và
`Message` làm nguồn sự thật phía server. Khi đó context builder phải có token
budget, recent window, conversation summary, semantic memory retrieval và
structured user memory. History do browser tự gửi không được xem là nguồn sự
thật. Context cho `@VibeAI` cũng phải được dựng từ post và comment thread trong
DB, không tái sử dụng browser chat history.

## Trạng Thái Triển Khai Search

### Đợt 1 - Search Contract

- Thêm application contract chỉ nhận query và filter tối thiểu.
- Trả semantic search results độc lập với text generation.
- Search contract mới không phụ thuộc vào RAG hoặc conversation model cũ.
- Chat/RAG cũ chỉ tồn tại đến đợt thay thế UI kế tiếp rồi được xóa, không trở
  thành compatibility contract lâu dài.

### Đợt 2 - Search UI

- Thay chat bubbles bằng thanh search và danh sách content results.
- Đưa query lên URL để refresh, back và chia sẻ được.
- Chia kết quả thành các khu vực standard, short và video có media preview.
- Bỏ chat session, interrupted message và browser conversation history.

Đã triển khai tại `/search`. Kết quả KNN được tải bằng một HTTP request ngắn.
Search summary, endpoint SSE, heartbeat, worker queue và cấu hình generation
riêng của search đã được xóa vì không tạo đủ giá trị cho trải nghiệm tìm kiếm.
Route, controller, view, JavaScript, CSS và message key của `/ai-chat` cũng không
được giữ như compatibility layer.

### Đợt 3 - Cleanup Và Retrieval Quality

- API `/api/v1/ai/rag/ask` và các adapter `PostRag*` cũ đã được xóa.
- Search chỉ giữ cấu hình retrieval, index, threshold và reconciliation.
- Không giữ redirect hoặc compatibility alias cho route và biến môi trường cũ.
- Không hiển thị KNN score như phần trăm xác suất.
- Đã thêm relevance threshold cấu hình được; mặc định tắt cho đến khi có
  benchmark positive/negative query.
- Giữ BM25 hybrid search là đợt mở rộng riêng sau khi semantic search ổn định.

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
