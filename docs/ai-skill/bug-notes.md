# Bug Notes

Bug notes are kept separate from project rules.

Current status:
- The items below were recorded while reading the source on 2026-07-12.
- Some items are mismatches that still need confirmation during build or runtime, but they should still be tracked separately from now on.

## Current Notes
1. `UserServiceImpl.checkEmailUnique()` has the condition reversed.
   - It currently throws `alreadyExists` when the email does not exist yet.

2. `RbacController` has a mismatch in the update role endpoint.
   - The mapping is `"/role/{id}"` but the method parameter is `roleId`.

3. `RbacController` has a mismatch in the delete role endpoint.
   - The current mapping does not include `/{roleId}`, but the method still uses `@PathVariable`.

4. `RbacController` has a typo in the permission endpoint.
   - The path is currently `"/permisision"`.

5. `RbacController` calls the wrong service in the remove permission endpoint.
   - `remove-permissions` currently calls `assignPermToRole(...)` instead of `removePermFromRole(...)`.

6. `UserInfoEntity` has a shared primary key mapping that should be re-checked after the app runs.
   - It currently uses `@MapsId` together with `@GeneratedValue`.
   - It also uses `@JoinColumn(name = "id")`, which can be confused with the `userId` field.

7. The JWT configuration shows signs of being out of sync with runtime behavior.
   - The config contains `app.jwt.secret-key`.
   - The current runtime behavior generates dynamic HMAC keys per user/session and stores them in `key_store`.

8. `GlobalExceptionHandler` currently turns missing routes into HTTP 500.
   - `NoResourceFoundException` falls into the `Exception.class` handler and returns `INTERNAL_SERVER_ERROR` instead of HTTP 404.

## Resolved Notes
1. `UserBaseEntity` previously declared the `status` field twice, overlapping with `BaseUserDetailEntity`.
   - The duplicated field in the entity hid the parent class `NAMED_ENUM` mapping, causing Hibernate to validate the enum as `TINYINT` by default.
   - This was fixed by marking `BaseUserDetailEntity` as `@MappedSuperclass` and removing the duplicated field from the entity.
