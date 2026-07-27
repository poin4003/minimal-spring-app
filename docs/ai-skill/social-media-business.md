# Social Media Business Rules

## Product Scope
- The application provides one shared public social feed.
- The platform supports standard posts, shorts, movies, products, wikis, and blogs.
- There are no private posts, friend relationships, follows, or personalized feeds.
- Published posts and their media can be viewed without authentication.
- Authentication is required to submit posts, comments, and replies.
- Administrators moderate posts before they become publicly visible.
- User-generated content is not automatically translated.

## Identity And Preferences
- User profiles remain minimal and contain only a full name, avatar, language, and dark-theme preference.
- Legacy profile fields that have no active business use should be removed instead of retained speculatively.
- Supported application languages initially are English and Vietnamese.
- The profile language is the persisted source of truth for authenticated users.
- A locale cookie may mirror the profile language to avoid loading the profile on every request.
- Anonymous locale resolution falls back to the request `Accept-Language` header and then the configured default language.
- Backend error codes remain stable and language-neutral while their user-facing messages are localized.
- System logs remain in English and are not localized.
- The profile theme preference is the persisted source of truth for authenticated users.
- Browser storage may cache theme state or retain anonymous-user preferences, but it must not replace the authenticated profile preference.
- An avatar must reference an active, ready image owned by the user.
- Avatar URLs and physical storage keys must not be stored directly in the user profile.

## Registration And Onboarding
- Registration begins by verifying ownership of the submitted email address with an OTP.
- OTP state must be stored in the database because the project does not use Redis.
- OTP codes require expiry, resend cooldown, attempt limits, single-use consumption, and scheduled cleanup.
- OTP request and verification endpoints require rate limiting by both email and client IP.
- A user record is created only after successful OTP verification.
- Registration must not email or persist a plaintext temporary password.
- After verification, the system creates the account with an unusable generated credential and issues a restricted onboarding session.
- An onboarding session may access only profile completion, password setup, logout, and required supporting resources.
- The user must set a permanent password before receiving normal application access.
- Password setup or password changes revoke existing sessions.
- Registration email delivery uses the reusable email service directly and does not require an in-app notification.

## Localization
- Use Spring `MessageSource` resource bundles for backend and Thymeleaf messages.
- Maintain a default bundle plus English and Vietnamese bundles.
- Thymeleaf pages resolve labels through message keys rather than hard-coded display text.
- API clients select localized messages through `Accept-Language`.
- Jakarta validation messages must use the same localization infrastructure.
- `MyException` and its factories should carry stable error codes and resolvable message keys or arguments instead of pre-localizing inside domain services.
- Existing notifications may retain the language used when their content snapshot was created; changing profile language does not rewrite notification history.

## Navigation
- Menu definitions use stable IDs, message keys, paths, icons, and children.
- Menu JSON must not contain duplicated translated labels.
- Menu icons use the locally bundled Bootstrap Icons assets.
- Desktop navigation remains a left sidebar and can collapse into an icon rail.
- Mobile navigation uses a left-side Bootstrap Offcanvas and must not move above the page content.
- Sidebar collapsed state is browser UI state and may be stored in `localStorage`; it is not a profile-domain setting.
- Collapsed menu items retain accessible names and Bootstrap tooltips.
- Authenticated pages reuse shared shell and sidebar fragments instead of duplicating navigation markup.

## Actors

### Anonymous User
- Can view the public feed.
- Can view published post details, media, comments, and replies.
- Cannot submit posts or comments.

### Authenticated User
- Has all anonymous-user capabilities.
- Can submit posts for moderation.
- Can submit text comments on published posts.
- Can reply to top-level comments.
- Can review the moderation status of their own posts.

### Administrator
- Can review posts waiting for moderation.
- Can approve or reject submitted posts.
- Has access to the original post content and attached media during moderation.

## Post Lifecycle
- A newly submitted post starts with `PENDING_REVIEW`.
- A pending post is visible only to its author and administrators.
- Approving a post changes its status to `PUBLISHED`.
- `publishedAt` is assigned when the post is approved.
- Rejecting a post changes its status to `REJECTED`.
- Rejected posts remain visible to their author but never appear in the public feed.
- Only `PUBLISHED` posts can be viewed anonymously or commented on.

Allowed transitions:

`PENDING_REVIEW -> PUBLISHED`

`PENDING_REVIEW -> REJECTED`

## Post Content
- Every content item has one `PostEntity` root containing ownership, type, moderation status, publication time, and common audit state.
- Type-specific data belongs to dedicated relational detail tables rather than nullable columns on one large post table.
- Supported post types are `STANDARD`, `SHORT`, `MOVIE`, `PRODUCT`, `WIKI`, and `BLOG`.
- Common media attachments use an ordered relation that can distinguish roles such as cover, content, gallery, trailer, and episode media.
- Maximum text length, media count, duration, aspect ratio, and type-specific limits must be configurable.
- Editing and resubmitting published posts are outside the initial scope.

## Typed Content Architecture
- Do not use JPA entity inheritance for post types.
- Do not use one generic JSON payload or ad-hoc map to create every post type.
- Each post type uses a dedicated typed payload, detail entity, mapper, service behavior, and result model.
- A common post application service owns shared transactions, author validation, moderation state, and media attachment rules.
- Type-specific creation and validation use `PostTypeHandler` strategies selected by a registry keyed by `PostType`.
- Prefer a strategy registry over a broad Abstract Factory hierarchy.
- Do not generalize behavior until at least the standard-post and short-post implementations demonstrate the shared contract.

## Standard Posts
- Standard posts support normal text content and ordered media attachments.
- Standard posts use the common moderation lifecycle.
- They are the first content type implemented to validate the post kernel.

## Shorts
- Shorts contain concise content and restricted media.
- Allowed media count, kind, duration, and aspect-ratio policies are configurable.
- Shorts reuse the common moderation and attachment workflow but enforce their limits through the short-post handler.

## Movies
- A movie has its own description and ordered seasons and episodes.
- Seasons are optional; a movie without seasons may expose episodes directly through one implicit season.
- Each episode references its playable media and may include its own title, description, and ordering.
- Playback progress is browser-local state keyed by the current user or browser and episode.
- Playback progress is not persisted in the database during the initial implementation.

## Products
- Products provide descriptive catalog content only and do not introduce commerce workflows.
- Product scope excludes SKU, SPU, inventory, stock, cart, checkout, payment, shipping, and orders.
- Product categories are relational, dynamic, and may be organized hierarchically.
- Stable fields such as price, title, description, and primary media remain normal relational fields.
- Flexible queryable properties use typed attribute definitions and typed attribute values.
- Attribute definitions declare a stable key, display label, value type, optional unit, options, and whether the attribute is filterable.
- Attribute values use relational typed columns or option relations rather than filterable JSON.
- JSON is allowed only for non-filterable display metadata.
- Product filtering uses specifications and typed attribute predicates while retaining the shared paging infrastructure.

## Wikis
- A wiki may use an uploaded Markdown media file as its source.
- Markdown rendering must sanitize generated HTML before display.
- Wiki Markdown references platform media through stable public media keys rather than filesystem paths.
- A missing or unavailable linked media item must not expose storage details.

## Blogs
- Blogs provide a title, summary, optional cover media, and long-form body content.
- Blog content may reuse the same sanitized Markdown rendering infrastructure as wikis.
- Blogs remain distinct from wikis through presentation and content workflow rather than a generic untyped document table.

## Media Storage
- Uploaded media must not be written into `src/main/resources/static`.
- Media files are stored in a configurable local directory outside the packaged JAR, for example `./data/media`.
- The database stores original media metadata and generated media variants.
- Original filenames must not be used as physical storage paths.
- Physical filenames should be generated using UUIDs.
- Uploads must validate extension, content type, file size, and kind-specific content.
- Video and audio are processed asynchronously into HLS by JobRunr.
- Video HLS uses a configurable rendition ladder and does not generate profiles above the source resolution.
- Audio HLS uses a dedicated audio rendition referenced by the master playlist.
- Each media item owns a separate directory containing its original file and generated variants.
- Media paths must never accept user-controlled filesystem traversal.

## Media Delivery
- Media bytes are not embedded into rendered HTML.
- HTML renders public media or stream URLs.
- Spring exposes media through an opaque public key, such as `GET /api/v1/public/media/{publicKey}`.
- Database IDs and physical storage keys must never appear in public media URLs.
- The endpoint resolves media metadata from the database and streams the file.
- Media responses should provide the correct content type and support browser caching.
- Video and audio delivery uses generated HLS playlists and segments.
- Missing physical files return `404`.

## Media Thumbnails
- Each media item has at most one thumbnail, referenced directly by `thumbnailStorageKey` on the media record.
- Thumbnails are derived artifacts and must not be stored as `MediaVariant` rows or as child media records.
- Public thumbnail URLs are resolved from the media public key; physical storage keys are never exposed.
- Images and videos require an automatically generated JPEG thumbnail bounded by the configured dimensions.
- Audio may use embedded cover art; failure to create this optional thumbnail must not block the media from becoming ready.
- PDF files, general downloads, and unsupported documents use frontend file-type icons instead of generated thumbnails.
- A user-facing custom video or audio cover must reuse a ready image owned by the same user and copy its normalized thumbnail artifact into the target media directory.
- The admin CMS may select any ready image in the media library as a custom thumbnail; this override must use a separate admin service method and must not weaken the owner-scoped service.
- Custom thumbnail UI uses a two-step flow: upload the candidate image as normal library media, then select it only after processing reaches `READY`; do not poll and auto-attach immediately after upload.
- Retrying HLS processing must preserve an existing custom thumbnail.
- Thumbnail delivery must be revalidated rather than cached as immutable because a custom cover can replace it at the same public URL.
- Original files are retained after thumbnail and HLS processing for future moderation, inspection, and reprocessing flows.

## Media Upload
- Each media file is uploaded separately; small files use direct multipart upload and large files use resumable chunk upload according to the configured threshold.
- Chunk upload sessions are owned and validated by the authenticated user, and every chunk must pass the backend checksum and size checks.
- Direct multipart uploads must be rejected above the configured direct-upload threshold at both the servlet and service boundaries.
- Active chunk sessions are limited by both session count and reserved bytes per user; quota checks must lock the user row so concurrent starts cannot bypass the limits.
- An uploaded chunk is immutable: retrying the same index and checksum is idempotent, while attempting to replace it with different content is rejected.
- Every chunk request must declare the exact `Content-Length`; requests with an unknown or mismatched length are rejected before writing.
- Concurrent chunk writes may share an upload session, but completion, cancellation, and cleanup must run exclusively for that session.
- Upload session expiry uses both a sliding idle TTL and an absolute lifetime derived from `createdAt`.
- Deterministically invalid assembled content closes the upload session and deletes its chunks; only infrastructure failures may reset the session for retry.
- The browser may store only the upload session ID keyed by file name, size, and last-modified timestamp in `localStorage`; it must never persist the file or authentication token.
- Resuming after a page reload requires the user to select the same local file again, after which only missing chunks are uploaded.
- Post creation uses a JSON payload containing previously uploaded media IDs.
- A media can be attached only after its processing status is `READY`.
- If post persistence fails after files are written, newly written files must be removed.

## Comments
- Comments contain text only.
- Comments can only be created on `PUBLISHED` posts.
- A top-level comment has no parent.
- A reply must reference a top-level comment belonging to the same post.
- Replies to replies are rejected, keeping the comment tree at exactly two levels.
- Comments and replies become visible immediately after creation.
- Anonymous users can read comments but cannot create them.

## Feed
- The public feed contains only `PUBLISHED` posts.
- Posts are ordered by `publishedAt` descending.
- The feed must use the existing reusable paging infrastructure.
- Every user sees the same feed.
- Feed queries may filter by `PostType` without changing the shared publication rules.

## Public Profiles
- A public profile exposes the user's full name, avatar, and published content grouped by post type.
- Each post-type section uses independent paging and the shared paging infrastructure.
- Public profile media includes only active, ready media attached to `PUBLISHED` content.
- Pending, failed, unused, rejected, or owner-private library media must not appear on a public profile.
- Publishing standalone gallery media is outside the initial scope and requires an explicit future visibility model.

## Authorization
- Public feed, published post details, media, and comments allow anonymous access.
- Post submission and comment creation require authentication.
- Post moderation requires a dedicated permission such as `POST:MODERATE`.
- Public GET routes must not accidentally make POST actions publicly accessible.

## Out Of Scope
- Private posts
- Friends and followers
- Personalized feeds
- Likes and reactions
- Direct messages
- Hashtags and mentions
- Automatic translation of user-generated content
- Standalone public media galleries
- Product commerce and inventory workflows

## Implementation Order
1. [x] Profile and preference schema, service, and UI.
2. [ ] Backend and Thymeleaf internationalization.
3. [ ] OTP registration and restricted credential onboarding.
4. [x] Shared responsive admin shell and icon-based menu with JSON-driven local Bootstrap icons, desktop icon rail, and mobile offcanvas.
5. [ ] Common post kernel, moderation, and media attachments.
6. [ ] Standard posts and shorts.
7. [ ] Movies, seasons, and episodes.
8. [ ] Products, categories, typed attributes, and flexible filters.
9. [ ] Wikis and blogs with sanitized Markdown.
10. [ ] Public profiles and typed content galleries.
11. [ ] Two-level text comments and replies.
