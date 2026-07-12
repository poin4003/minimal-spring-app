# Bug Notes

Bug note duoc tach rieng khoi project rules.

Trang thai hien tai:
- Cac muc duoi day duoc ghi lai tu luc doc source ngay 2026-07-12.
- Co nhung muc la mismatch can xac nhan lai khi build/runtime, nhung van nen track rieng tu bay gio.

## Current Notes
1. `UserServiceImpl.checkEmailUnique()` dang dao nguoc dieu kien.
   - Hien tai email chua ton tai lai nem `alreadyExists`.

2. `RbacController` co mismatch o endpoint update role.
   - Mapping la `"/role/{id}"` nhung bien nhan la `roleId`.

3. `RbacController` co mismatch o endpoint delete role.
   - Mapping hien tai khong co `/{roleId}` nhung method lai dung `@PathVariable`.

4. `RbacController` co typo o endpoint lay permission.
   - Duong dan dang la `"/permisision"`.

5. `RbacController` goi nham service o endpoint remove permission.
   - `remove-permissions` dang goi `assignPermToRole(...)` thay vi `removePermFromRole(...)`.

6. `UserInfoEntity` co mapping shared primary key can xac nhan lai sau khi app chay.
   - Dang dung `@MapsId` cung voi `@GeneratedValue`.
   - Dang dung `@JoinColumn(name = "id")`, de nham voi field `userId`.

7. JWT config co dau hieu khong dong bo voi runtime behavior.
   - Config co `app.jwt.secret-key`.
   - Runtime hien tai dang tao khoa HMAC dong theo user/session va luu trong `key_store`.

8. `GlobalExceptionHandler` dang bien route khong ton tai thanh HTTP 500.
   - `NoResourceFoundException` roi vao handler `Exception.class` va tra `INTERNAL_SERVER_ERROR` thay vi HTTP 404.

## Resolved Notes
1. `UserBaseEntity` tung khai bao trung field `status` voi `BaseUserDetailEntity`.
   - Field o entity che mapping `NAMED_ENUM` cua lop cha, lam Hibernate mac dinh validate enum theo `TINYINT`.
   - Da danh dau `BaseUserDetailEntity` bang `@MappedSuperclass` va bo field trung lap trong entity.
