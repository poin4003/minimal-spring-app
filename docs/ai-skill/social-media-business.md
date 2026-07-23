# Social Media Business Rules

## Product Scope
- The application provides one shared public social feed.
- There are no private posts, friend relationships, follows, or personalized feeds.
- Published posts and their media can be viewed without authentication.
- Authentication is required to submit posts, comments, and replies.
- Administrators moderate posts before they become publicly visible.

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
- A post contains text content and may contain zero or more media attachments.
- Supported media kinds are image, video, audio, document, and general file.
- Maximum text length, media count, and per-type file size must be configurable.
- Editing and resubmitting published posts are outside the initial scope.

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
- Audio may use embedded cover art, and PDF documents may use a rendered first page; failure to create these optional thumbnails must not block the media from becoming ready.
- General downloads and unsupported documents use frontend file-type icons instead of generated thumbnails.
- A custom video or audio cover must reuse a ready image owned by the same user and copy its normalized thumbnail artifact into the target media directory.
- Custom thumbnail UI uses a two-step flow: upload the candidate image as normal library media, then select it only after processing reaches `READY`; do not poll and auto-attach immediately after upload.
- Retrying HLS processing must preserve an existing custom thumbnail.
- Thumbnail delivery must be revalidated rather than cached as immutable because a custom cover can replace it at the same public URL.
- Original files are retained after thumbnail and HLS processing for future moderation, inspection, and reprocessing flows.

## Media Upload
- Each media file is uploaded separately through a multipart API.
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
- Notifications
- Hashtags and mentions
