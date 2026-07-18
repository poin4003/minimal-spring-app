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
- A post contains text content and may contain zero or more images.
- The initial media scope supports images only.
- Video, audio, and general file attachments are outside the initial scope.
- Maximum text length, image count, and image size must be configurable.
- Editing and resubmitting published posts are outside the initial scope.

## Media Storage
- Uploaded media must not be written into `src/main/resources/static`.
- Media files are stored in a configurable local directory outside the packaged JAR, for example `./data/media`.
- The database stores media metadata and the generated storage name.
- Original filenames must not be used as physical storage paths.
- Physical filenames should be generated using UUIDs.
- Uploads must validate content type, file size, and image content.
- Media paths must never accept user-controlled filesystem traversal.

## Media Delivery
- Image bytes are not embedded into rendered HTML.
- HTML renders a media URL in the image `src` attribute.
- Spring exposes a public media endpoint such as `GET /api/v1/public/media/{mediaId}`.
- The endpoint resolves media metadata from the database and streams the file.
- Media responses should provide the correct content type and support browser caching.
- Missing physical files return `404`.

## Media Upload
- The initial Thymeleaf flow uploads images together with the post form using `multipart/form-data`.
- A separate upload API is not required for the initial implementation.
- A dedicated upload API may be added later for external clients, reusable media, rich editors, or upload progress.
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
- Video and audio uploads
