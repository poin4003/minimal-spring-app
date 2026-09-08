# Kế Hoạch Triển Khai Catalog Post

## 1. Mục Tiêu

`catalog_post` là loại bài đăng dùng để giới thiệu sản phẩm hoặc dịch vụ trên
Motumo. Nó vẫn là nội dung mạng xã hội, không phải một hệ thống thương mại điện
tử hoàn chỉnh.

Mục tiêu của feature:

- Nhà cung cấp có thể tạo bài giới thiệu sản phẩm hoặc dịch vụ.
- Mỗi bài có thumbnail, gallery ảnh/video, mô tả và thuộc tính linh hoạt.
- Một bài có thể có nhiều offer để biểu diễn biến thể, mức giá, nơi mua hoặc
  cách liên hệ khác nhau.
- Category tạo khuôn thuộc tính và bộ lọc, nhưng không bắt buộc khi tạo draft.
- User có thể dùng bài `STANDARD`, `SHORT` hoặc `VIDEO` để review, mention hoặc
  quảng bá một catalog post.
- Catalog post đi qua lifecycle và moderation chung của post.
- Public query và filter phải có paging, không load toàn bộ dữ liệu vào memory.

## 2. Phạm Vi Không Làm

Các phần sau chưa thuộc catalog V1:

- SKU, SPU và quản lý tồn kho.
- Giỏ hàng, checkout, đơn hàng, thanh toán và vận chuyển.
- Rating bằng sao hoặc bảng review riêng.
- Hoa hồng, affiliate tracking và chia doanh thu.
- Đăng ký tài khoản nhà cung cấp công khai.
- Recommendation cá nhân hóa.
- Đồng bộ dữ liệu tự động từ Shopee, Lazada hoặc nền tảng khác.
- Phân biệt cứng `PRODUCT` và `SERVICE` bằng enum.

## 3. Quyết Định Domain Đã Chốt

### 3.1 Catalog post dùng chung post kernel

`CatalogPostEntity` dùng shared primary key với `PostEntity`. Author, lifecycle,
moderation và thời gian publish lấy từ `PostEntity`; không lặp lại các field đó
trong bảng `catalog_post`.

Lifecycle phải được gọi qua `CatalogPostService`, tương tự
`StandardPostService`, `ShortPostService` và `VideoPostService`. Controller không
được gọi thẳng `PostLifecycleService`, vì như vậy sẽ bỏ qua policy riêng của
catalog.

### 3.2 Không có field kind

Catalog có thể giới thiệu cả sản phẩm lẫn dịch vụ nhưng không lưu enum phân loại
hai loại này. Category và attributes sẽ mô tả nội dung thực tế. Cách này tránh
đóng cứng domain trước khi có nhu cầu filter rõ ràng.

### 3.3 Category là nullable

User được tạo draft chưa có category. Trước khi submit:

- Nếu có category, service kiểm tra category còn `ACTIVE` và validate schema của
  category.
- Nếu không có category, bài vẫn có thể submit với custom attributes.
- Khi category hoặc attribute bị chuyển `INACTIVE`, dữ liệu cũ vẫn đọc được;
  không cascade xóa giá trị đã dùng.

### 3.4 Thuộc tính dùng typed EAV

`CatalogPostAttributeValueEntity` chứa thuộc tính dùng chung cho toàn catalog
post. `CatalogOfferAttributeValueEntity` chứa thuộc tính chỉ áp dụng cho một
offer.

Mỗi value phải thỏa đúng một nhánh dữ liệu theo `valueType`:

| Value type | Field lưu giá trị |
| --- | --- |
| `TEXT`, `URL`, `PHONE` | `textValue` |
| `NUMBER`, `MONEY` | `numberValue` |
| `BOOLEAN` | `booleanValue` |
| `DATE` | `dateValue` |
| `OPTION` | `option` hoặc custom text |

Với canonical attribute, client chỉ gửi `attributeId` và giá trị. Backend phải
lấy `valueType`, unit và option policy từ database; không tin `valueType` do
client gửi.

Với custom attribute, client gửi `customKey`, `customName`, `valueType`, unit và
giá trị. `attribute` và custom identity phải loại trừ lẫn nhau.

### 3.5 Category attribute là khuôn, không phải giới hạn tuyệt đối

`CatalogCategoryAttributeEntity` định nghĩa:

| Field | Ý nghĩa |
| --- | --- |
| `required` | Bắt buộc có giá trị trước khi submit |
| `filterable` | Được dùng trong bộ lọc public |
| `multipleValuesAllowed` | Một post/offer được có nhiều value cho attribute |
| `customOptionAllowed` | Attribute `OPTION` được nhận text ngoài option chuẩn |
| `sortOrder` | Thứ tự nhập và hiển thị |

Nhà cung cấp vẫn được thêm custom attribute ngoài khuôn category. Chỉ canonical
attributes mới tham gia bộ lọc chuẩn ở V1; custom attributes có thể được index
cho search nhưng chưa dùng để dựng bộ lọc động mặc định.

### 3.6 Offer là block biến thể hoặc phương án tiếp cận

Một `CatalogOfferEntity` có thể đại diện cho một biến thể hoặc một phương án cụ
thể, ví dụ `Size 45 - Trắng - Shopee` hoặc `Gói thuê 12 tháng`. Giá, URL mua,
số điện thoại liên hệ và thuộc tính biến thể được lưu bằng typed attributes của
offer.

Policy V1:

- Catalog post được phép không có offer nếu chỉ mang tính giới thiệu.
- Nếu có offer `ACTIVE`, phải có đúng một default offer ở service boundary.
- Position không trùng trong cùng catalog post.
- Xóa offer trong UI là soft disable bằng `RecordStatus.INACTIVE`; hard delete
  chỉ dùng khi draft chưa từng publish hoặc trong cleanup.

### 3.7 Media dùng post attachment hiện có

Không tạo quan hệ media riêng cho catalog:

- `PostMediaRole.COVER`: đúng một ảnh thumbnail trước khi submit.
- `PostMediaRole.GALLERY`: danh sách ảnh hoặc video, có thể rỗng nếu policy V1
  chỉ yêu cầu cover.
- Media phải thuộc author, có `RecordStatus.ACTIVE` và
  `MediaProcessingStatus.READY` trước khi submit.
- Do unique `(post_id, media_id)` hiện có, cover và gallery không dùng lại cùng
  một `MediaEntity`. UI phải thể hiện rõ thumbnail là media độc lập.
- Giới hạn số media nên là application property và Jakarta validation, không
  hard-code trong entity.

### 3.8 Review vẫn là post thông thường

`PostCatalogReferenceEntity` nối một post xã hội tới catalog post:

- `REVIEW`: nội dung đánh giá hoặc trải nghiệm.
- `MENTION`: chỉ nhắc tới catalog.
- `PROMOTION`: nội dung quảng bá.

Không tạo review lifecycle riêng và không tạo rating. Public catalog detail chỉ
trả các reference có source post đang `ACTIVE` và `PUBLISHED`.

Archive hoặc soft delete catalog không làm thay đổi source post. Hard cleanup
catalog chỉ xóa relation bằng foreign-key cascade, không xóa bài review.

## 4. Trạng Thái Code Hiện Tại

Đã có:

- Toàn bộ entity dưới `features/post/catalogpost/entity`.
- `CatalogAttributeValueType` và `CatalogReferenceType`.
- Migration `V19__catalog_post_schema.sql` với table, foreign key, check
  constraint và index nền.
- Migration `V20__catalog_post_foundation.sql` đổi discriminator sang `CATALOG`
  và seed RBAC cho supplier.
- Repository cùng fetch plan nền cho toàn bộ catalog aggregate.
- Permission constants, role `SUPPLIER` và integration test persistence nền.
- Specification, schema, mapper và service quản lý category, attribute, option.
- Admin UI cấu hình category tree, option và category-attribute schema.
- Form schema chỉ trả definition đang `ACTIVE` cho supplier flow.
- Common post lifecycle, moderation, media attachment và owner/public pattern để
  tái sử dụng.

Chưa có:

- Specification cho catalog post listing và typed filter.
- Payload, result, mapper và policy validation cho catalog aggregate.
- Service create/update/detail/list/lifecycle.
- Moderation detail cho catalog.
- Public/owner catalog pages.
- UI gắn catalog reference vào standard/short/video.
- Search indexing cho catalog.
- Test và i18n của feature.

## 5. Việc Cần Chốt Trước Khi Viết Service

Đợt 1 chuyển discriminator cũ `PostType.PRODUCT` sang `PostType.CATALOG` để
feature phản ánh đúng catalog cho cả sản phẩm lẫn dịch vụ.

Thay đổi này cần đồng bộ:

- Java enum và toàn bộ switch expression.
- `PostTypeEnum` trong database bằng một Flyway migration mới.
- Moderation filter, search index document và UI label.
- Dữ liệu cũ `PRODUCT -> CATALOG` nếu môi trường đã có row loại này.

Đây chỉ là đổi tên technical post type, không thêm lại field `kind`. Migration
`V20__catalog_post_foundation.sql` chịu trách nhiệm chuyển dữ liệu cũ và seed
RBAC cho supplier.

## 6. Cấu Trúc Package Mục Tiêu

```text
features/post/catalogpost
|-- entity
|-- enums
|-- mapper
|-- repository
|   `-- spec
|-- schema
|   |-- filter
|   |-- model
|   |-- payload
|   `-- result
|-- service
|   `-- impl
`-- web
    |-- controller
    |-- support
    `-- view
```

Các helper validate typed EAV nên nằm trong service/policy của catalog, không
đưa business query vào controller hoặc mapper.

## 7. Repository Contract

### 7.1 Repository chính

Tạo các repository sau:

- `CatalogPostRepository`: `JpaRepository` và `JpaSpecificationExecutor`.
- `CatalogOfferRepository`.
- `CatalogPostAttributeValueRepository`.
- `CatalogOfferAttributeValueRepository`.
- `CatalogCategoryRepository`: `JpaSpecificationExecutor` cho admin listing.
- `CatalogCategoryAttributeRepository`.
- `CatalogAttributeRepository`: `JpaSpecificationExecutor` cho admin listing.
- `CatalogAttributeOptionRepository`.
- `PostCatalogReferenceRepository`.

### 7.2 Query pattern

- General listing luôn trả `Page<T>`.
- Detail load root bằng entity graph hoặc query nhỏ có fetch plan rõ ràng.
- Offers, values, gallery và references được batch theo parent IDs; không chạy
  query trong vòng lặp mapper.
- Catalog card không load toàn bộ offers và EAV. Summary chỉ cần cover, category
  và default-offer summary nếu UI dùng.
- Typed filters dùng correlated `EXISTS` theo từng điều kiện để tránh duplicate
  root row do join nhiều value table.
- Filter nhiều điều kiện là phép `AND`; nhiều option trong cùng một attribute là
  phép `OR` nếu UI không chọn chế độ khác.
- Public specification luôn áp dụng `PostLifecycleStatus.ACTIVE` và
  `PostModerationStatus.PUBLISHED` ở backend, không dựa vào controller truyền
  đúng filter.
- Owner specification luôn ràng buộc `post.author.id = ownerId`.
- Reference listing public cũng kiểm tra trạng thái của cả catalog target và
  source post.

### 7.3 Lock và update order

- Update catalog aggregate phải lock `PostEntity` hoặc `CatalogPostEntity` root
  trước khi replace offers/values/media.
- Replace collection theo thứ tự: validate toàn payload, xóa child cũ cần thay,
  flush khi có unique position conflict, rồi insert child mới.
- Không giữ transaction mở trong lúc upload hoặc xử lý media.

## 8. Schema Contract

### 8.1 Shared value payload

Tạo `CatalogAttributeValuePayload` với các field:

```text
attributeId
customKey
customName
valueType
textValue
numberValue
booleanValue
dateValue
optionId
unit
position
```

Jakarta validation xử lý null, blank, length, range và collection size. Service
policy xử lý one-of typed value, database lookup, category rule và ownership.

### 8.2 Catalog payload

`CreateCatalogPostPayload` và `UpdateCatalogPostPayload` gồm:

```text
categoryId
title
description
coverMediaId
galleryMediaIds
attributes
offers
```

`CatalogOfferPayload` gồm:

```text
id                 // chỉ dùng cho update nếu giữ identity
name
defaultOffer
position
attributes
```

Update V1 nên dùng replace semantics cho attributes, offers và gallery. Contract
này đơn giản, transaction rõ và phù hợp kích thước form. Patch từng child chỉ
nên thêm sau khi payload thực tế trở nên quá lớn.

### 8.3 Reference payload

Tạo `PostCatalogReferencePayload` và
`ReplacePostCatalogReferencesPayload`:

```text
catalogPostId
referenceType
position
```

Create payload của `STANDARD`, `SHORT` và `VIDEO` có thể thêm một list reference
không null, mặc định rỗng. Mỗi subtype service gọi chung
`PostCatalogReferenceService`; không copy validation sang ba service.

### 8.4 Filter

Tạo các criteria riêng:

- `PublicCatalogPostFilterCriteria`: text, category, canonical typed filters.
- `OwnerCatalogPostFilterCriteria`: text, lifecycle, moderation, category.
- `CatalogCategoryFilterCriteria`: text, status, parent.
- `CatalogAttributeFilterCriteria`: text, status, value type, searchable.

Paging và sorting tiếp tục dùng query object/pageable riêng theo project rule.

### 8.5 Result

Tách result theo audience:

- `PublicCatalogPostSummaryResult` cho card.
- `PublicCatalogPostDetailResult` cho detail đầy đủ.
- `OwnerCatalogPostResult` có lifecycle/moderation/rejection state.
- `CatalogOfferResult`.
- `CatalogAttributeValueResult`.
- `CatalogCategoryResult` và `CatalogAttributeDefinitionResult`.
- `ModerationCatalogPostDetailResult`.

Public result không trả internal IDs không cần thiết, storage key, moderation
metadata hoặc inactive options.

## 9. Service Và Policy

### 9.1 Catalog configuration

`CatalogAttributeService` quản lý canonical attributes và options.

`CatalogCategoryService` quản lý category tree và category-attribute mappings.

Policy admin:

- Không tạo cycle trong category tree.
- Không đổi `valueType` của attribute đã có value; thay bằng attribute mới hoặc
  migrate dữ liệu có chủ đích.
- Không hard delete attribute/option/category đang được tham chiếu.
- Option phải thuộc đúng canonical attribute loại `OPTION`.
- Category inactive không được chọn cho bài mới nhưng dữ liệu cũ vẫn hiển thị.

### 9.2 Catalog aggregate

`CatalogPostService` chịu trách nhiệm:

- Create draft.
- Update owned draft/rejected/published theo common edit policy.
- Submit for review.
- Archive và restore archive.
- Soft delete và restore deleted.
- Owner detail/list.
- Public detail/list.
- Require helper cho moderation và internal service reuse.

`CatalogOfferService` và `CatalogAttributeValueService` nên là internal domain
services được gọi trong transaction của `CatalogPostService`. Chúng không cần
controller riêng ở V1.

### 9.3 Validation khi lưu draft

Draft cho phép dữ liệu chưa hoàn chỉnh nhưng vẫn phải bảo đảm:

- Media, category và referenced IDs tồn tại khi được gửi.
- Media thuộc owner.
- Không duplicate media, offer position hoặc value position.
- Typed value đúng shape.
- Canonical option thuộc đúng attribute.
- Không có UUID trùng trong collection.

### 9.4 Validation khi submit

Submit bổ sung các rule chặt hơn:

- Title không blank.
- Có đúng một `COVER` là ảnh và media đã `READY`.
- Gallery chỉ chứa loại media được policy hỗ trợ và tất cả đã `READY`.
- Category active nếu được gán.
- Đủ canonical attribute có `required = true`.
- Không vi phạm `multipleValuesAllowed`.
- Custom option chỉ được dùng khi `customOptionAllowed = true`.
- Nếu có active offer thì có đúng một default offer.
- URL, phone và money được normalize trước khi lưu hoặc submit.

### 9.5 Reference service

`PostCatalogReferenceService` chịu trách nhiệm:

- Replace reference cho owned source post.
- Chống duplicate catalog target và position.
- Chỉ cho reference tới catalog đang public tại thời điểm gắn.
- Cho phép owner sửa reference khi source post còn sửa được.
- Phân quyền `PROMOTION` theo policy đã chọn; V1 nên chỉ cho owner catalog hoặc
  account có catalog permission tạo promotion.
- Lấy related posts theo page và `referenceType` cho catalog detail.

## 10. RBAC

Thêm permission constants:

```text
CATALOG:CREATE
CATALOG:VIEW:OWN
CATALOG:UPDATE:OWN
CATALOG:CONFIGURE
```

Role mới `SUPPLIER` do admin tạo/cấp, không xuất hiện trong signup. Seed đề xuất:

| Role | Permission mặc định |
| --- | --- |
| `SUPPLIER` | Catalog create/view/update own và media own |
| `SUPER_ADMIN` | Toàn bộ catalog permissions |
| `USER` | Không có catalog create; vẫn được reference catalog từ social post |

Moderation catalog tiếp tục dùng `POST:MODERATE`. Việc supplier có được đăng
`STANDARD`, `SHORT` hoặc `VIDEO` độc lập hay không để RBAC quyết định, không
hard-code theo role name trong service.

Service vẫn kiểm tra ownership dù controller đã có `@Secured`.

## 11. Moderation Và Lifecycle

Mở rộng moderation switch để nhận catalog post:

- Load title, description, category, cover, gallery, common attributes và offers.
- Mapper trả `ModerationCatalogPostDetailResult`.
- Publish dùng `PostModerationCommandService` hiện có.
- Reject dùng flow hiện có và lưu reason trong `PostEntity`.
- AI moderation candidate phải nhận được catalog text và thumbnail sau khi manual
  flow đã chạy ổn.

Public query tuyệt đối không trả draft, pending, rejected, archived hoặc deleted
catalog. Owner query được thấy toàn bộ state của chính họ.

Khi catalog bị archive/delete:

- Catalog biến mất khỏi public list/detail/filter.
- Related social posts vẫn public như bài bình thường.
- Related post card có thể ẩn catalog preview nếu target không còn public.
- Restore catalog không tự thay đổi các social posts.

## 12. Web Controller Và Route

### 12.1 Public

```text
GET /catalog
GET /catalog/{postId}
GET /catalog/{postId}/related-posts
```

List và related posts hỗ trợ HTMX fragment paging/infinite loading. Detail URL
vẫn là URL thật để browser history và deep link hoạt động đúng.

### 12.2 Supplier owner pages

```text
GET  /my/catalog
GET  /my/catalog/create
POST /my/catalog/create
GET  /my/catalog/{postId}
GET  /my/catalog/{postId}/edit
POST /my/catalog/{postId}/edit
POST /my/catalog/{postId}/submit
POST /my/catalog/{postId}/archive
POST /my/catalog/{postId}/restore-archived
POST /my/catalog/{postId}/delete
POST /my/catalog/{postId}/restore-deleted
```

### 12.3 Admin configuration

```text
/admin/catalog/categories
/admin/catalog/attributes
/admin/catalog/attributes/{attributeId}/options
/admin/catalog/categories/{categoryId}/attributes
```

Category-attribute assignment là workflow page riêng, không nhồi thành modal
lớn.

## 13. Frontend

### 13.1 Public catalog

- Thêm menu `Catalog` khi backend public list đã hoàn chỉnh.
- Card cố định layout, dùng cover và không load full gallery.
- Detail có media gallery, title, author, category, common attributes, offer
  selector và related social posts.
- Offer selector đổi phần giá/link/contact bằng Alpine, không gọi server nếu dữ
  liệu offer đã có trong detail.
- Typed filters do server query; Alpine chỉ quản lý trạng thái mở/đóng, lựa chọn
  và serialize form.

### 13.2 Supplier composer

- Media picker tái sử dụng component hiện có.
- Category selection gọi server lấy schema attribute bằng HTMX.
- Thêm/xóa/reorder offer và custom attribute dùng Alpine.
- Submit form và validation response dùng HTMX/server.
- Không để HTMX quản lý state UI local như active offer, modal, preview hoặc
  reorder tạm thời.
- Owner list tái sử dụng status tabs và action pattern của post hiện tại.

### 13.3 Social reference picker

- Composer của standard/short/video có picker tìm catalog public.
- UI cho chọn `REVIEW`, `MENTION` hoặc `PROMOTION` theo quyền.
- Related catalog preview là component dùng chung cho cả ba post type.
- Source post detail hiển thị catalog references nhưng không biến thành catalog
  detail thứ hai.

### 13.4 Navigation ổn định

- Shell, header và menu giữ nguyên theo cơ chế partial navigation hiện có.
- Mỗi catalog page có content root riêng, không dùng chung ID với post/video
  fragment.
- Infinite list dùng `hx-push-url="false"`.
- Điều hướng detail dùng URL thật và history policy chung của app.
- Alpine component phải được dispose/re-init đúng sau `htmx:afterSwap` theo
  bootstrap frontend hiện tại; không đăng ký global listener lặp lại.

## 14. Search Và Filter

### 14.1 Filter database V1

Hỗ trợ trước:

- Search title/description cơ bản.
- Category và descendant category nếu tree yêu cầu.
- Canonical option.
- Number/money range.
- Boolean.
- Date range.

Không filter trực tiếp URL/PHONE. Text EAV filter chỉ thêm khi có use case rõ,
vì `%LIKE%` trên `TEXT` không tận dụng tốt index hiện tại.

### 14.2 AI search

Sau khi CRUD và moderation ổn định, mở rộng search candidate/indexer:

- Index title, description, category name, searchable catalog attributes và
  active offer names.
- Không đưa phone hoặc raw destination URL vào semantic text mặc định.
- Publish/reject/archive/delete/restore phải phát event để reconcile index.
- Search result có section/card catalog riêng, không map catalog thành standard
  post.

## 15. Kế Hoạch Theo Đợt

### Đợt 1: Căn chỉnh model và persistence

Trạng thái: hoàn thành.

Deliverables:

- Chốt và migrate `PostType.PRODUCT -> CATALOG`.
- Đối chiếu entity với `V19__catalog_post_schema.sql`.
- Bổ sung migration RBAC cho catalog và role `SUPPLIER`.
- Tạo repository nền và các fetch plan cần thiết.
- Thêm message keys cho validation/not-found/forbidden.

Kết quả mong đợi:

- Application start được trên database mới và database đã chạy V19.
- Supplier permission được cấp qua RBAC, không qua signup.
- Repository integration test xác nhận mapping và constraint chính.

### Đợt 2: Category, attribute và option

Trạng thái: hoàn thành.

Deliverables:

- Repository/specification, schema, mapper và service cấu hình catalog.
- Admin list/create/update/inactivate category và attribute.
- Quản lý option.
- Trang gán attribute vào category với required/filterable/multiple/custom flags.
- Chặn category cycle và thay đổi value type không an toàn.

Kết quả mong đợi:

- Admin tạo được một category hoàn chỉnh và supplier đọc được form schema.
- Inactive definitions không làm hỏng dữ liệu cũ.

### Đợt 3: Catalog draft và owner CRUD

Deliverables:

- Shared typed-value policy.
- `CatalogPostService` create/update/owner detail/owner paging.
- Replace gallery, values và offers trong một transaction.
- Owner lifecycle wrappers archive/delete/restore.
- Supplier composer và owner list/detail.

Kết quả mong đợi:

- Supplier tạo được draft có cover, gallery, custom/canonical attributes và
  nhiều offers.
- Update không sinh duplicate position hoặc orphan child.

### Đợt 4: Submit, moderation và public read

Deliverables:

- Submit-time policy đầy đủ.
- Catalog moderation detail và publish/reject action.
- Public catalog list/detail với paging và visibility guard.
- Public filter cho category, option, number/money, boolean và date.
- Catalog card/detail UI responsive.

Kết quả mong đợi:

- Draft không hợp lệ không submit được.
- Moderator duyệt được catalog.
- Chỉ catalog active/published xuất hiện public.

### Đợt 5: Social post references

Deliverables:

- `PostCatalogReferenceService` và repository.
- Shared payload/result và reference picker.
- Tích hợp create/update vào standard, short và video service.
- Related posts paging trên catalog detail.
- Visibility policy khi một trong hai phía archive/delete.

Kết quả mong đợi:

- User đăng review như post bình thường và liên kết được catalog.
- Catalog detail xem được review/mention/promotion mà không có rating table.

### Đợt 6: Search, AI moderation và profile integration

Deliverables:

- Catalog candidate cho manual/AI moderation.
- Catalog document cho search index và reconciliation events.
- Search result section catalog.
- Catalog tab trong supplier profile/owner profile nếu UX cần.
- Rebuild/reconcile command cho catalog index.

Kết quả mong đợi:

- Catalog được tìm bằng title, description và searchable attributes.
- Lifecycle change không để stale public search document.

### Đợt 7: Hardening

Deliverables:

- Unit test cho typed EAV và category policy.
- Repository test cho public/owner visibility và typed filters.
- Service integration test cho transaction, ownership và lifecycle.
- MVC/security test cho supplier/admin/moderator/user.
- Template/component test cho fragment isolation và responsive structure.
- Query-count test hoặc SQL inspection cho list/detail để chặn N+1.
- Kiểm tra index bằng dữ liệu đủ lớn trước khi thêm index mới.

Kết quả mong đợi:

- Không có đường đi vòng qua catalog lifecycle service.
- Không lộ draft hoặc catalog của owner khác.
- Public list và filter có query plan ổn định khi dữ liệu tăng.

## 16. Thứ Tự Triển Khai Khuyến Nghị

1. Hoàn thành đợt 1 để khóa naming, DB và security boundary.
2. Hoàn thành đợt 2 vì composer phụ thuộc category schema.
3. Làm đợt 3 để có owner flow end-to-end.
4. Làm đợt 4 để catalog thật sự xuất hiện public.
5. Chỉ sau đó nối social posts ở đợt 5.
6. Search/AI và hardening đi sau khi domain behavior đã ổn định.

Không nên làm UI trước service policy hoặc làm social references trước public
catalog visibility. Hai cách đó dễ tạo frontend contract tạm rồi phải sửa lại.

## 17. Definition Of Done Cho V1

Catalog V1 được xem là hoàn thành khi:

- Admin quản lý được category, attribute, option và category schema.
- Admin cấp được role/permissions cho supplier.
- Supplier tạo, sửa, submit, archive, delete và restore catalog của mình.
- Catalog hỗ trợ cover, gallery, common attributes và nhiều offers.
- Moderation manual và auto mode không gặp unsupported post type.
- Public list/detail chỉ trả catalog hợp lệ và có paging.
- Filter canonical attributes hoạt động đúng kiểu dữ liệu.
- Standard/short/video có thể reference catalog và related posts được phân trang.
- Search index hiểu catalog và được đồng bộ theo lifecycle.
- UI dùng Alpine cho local reactivity, HTMX cho server interaction và không làm
  full-page reload ngoài các boundary đã chủ đích.
- Các policy ownership, media readiness, typed EAV và public visibility có test.

## 18. Các Điểm Hoãn Có Chủ Đích

Sau V1 mới đánh giá lại:

- Có cần enum phân biệt sản phẩm/dịch vụ hay chỉ category là đủ.
- Có cần bảng destination riêng thay vì biểu diễn trong offer attributes.
- Có cần stable public slug cho catalog detail.
- Có cần patch từng offer/value thay cho replace aggregate.
- Có cần cache facet count hoặc precomputed search document.
- Có cần analytics click-out, conversion hoặc promotion disclosure.
- Có cần supplier organization với nhiều user thay cho role trên từng account.

Chỉ mở các phần này khi usage thực tế chứng minh schema V1 không còn đáp ứng,
tránh biến catalog giới thiệu thành một ecommerce platform ngoài scope.
