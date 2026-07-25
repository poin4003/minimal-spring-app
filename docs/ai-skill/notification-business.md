# Notification Business

## Scope
- In-app notification is the source of truth.
- The first supported notification flow reports asynchronous media processing results.
- SSE, Email, and Telegram are later delivery layers built on top of committed in-app notifications.
- SSE only signals that new data is available; clients must still read notification state from the database.
- Email delivery is optional per user through `UserNotificationPreferenceEntity.emailEnabled`.
- Telegram broadcasts to one shared group configured through YAML and is not a per-user provider preference.
- Notification templates, provider tables, event-route tables, and social aggregation are out of scope until a concrete business requirement needs them.

## Media Notifications
- `MEDIA_READY` is created when asynchronously processed media reaches `READY`.
- For video and audio, `MEDIA_READY` means HLS content is ready to stream.
- For images, `MEDIA_READY` means required thumbnail processing completed successfully.
- `MEDIA_PROCESSING_FAILED` is created when required media processing fails.
- Immediate upload success, retry start, manual thumbnail assignment, and media deletion do not create notifications.
- Media notifications are system-generated, so their actor may be null.
- Starting a retry removes the existing failure notification for that media.
- A successful retry removes any stale failure notification before creating or refreshing the ready notification.
- Permanently deleting media removes every notification referencing that media.

## Notification Lifecycle
- Delete notifications when their referenced resource is permanently deleted.
- Resolve or remove actionable notifications when the corresponding action is completed elsewhere.
- Notification cleanup must support a configurable TTL and a configurable per-user hard limit.
- Cleanup must use the existing JobRunr recurring-job infrastructure.
- Cleanup implementation is deferred until the in-app notification flow is operational.
- Do not add aggregation fields until a notification type has real repeated-actor behavior.
- Aggregated notifications must use an explicit occurrence timestamp instead of using `updated_at` for inbox ordering.
- Notification indexes must follow actual inbox, cleanup, and resource-resolution queries.
- Prefer JPA-derived queries, specifications, or JPQL bulk operations over native cleanup SQL.

## Inbox Query
- Notification inbox listings must use `Page`, `Pageable`, `JpaSpecificationExecutor`, and a dedicated filter criteria.
- Recipient scope is mandatory for every user-facing inbox query.
- Recipient ID must come from the authenticated principal, never from request parameters or browser-submitted filters.
- Keep web filter state separate from the internal criteria that contains recipient ID.
- Use an entity graph when inbox results include related actor information.
- Default inbox ordering is `createdAt DESC`.

## Inbox UI
- Render the notification inbox as a shared authenticated-header widget rather than duplicating it in feature pages.
- Load the unread count when the page opens and load the latest notification page only when the Bootstrap dropdown opens.
- Mark-one and mark-all actions must use HTMX, the shared CSRF infrastructure, and typed view models.
- Successful inbox mutations emit the shared `notification:changed` browser event so the unread badge refreshes independently.
- Keep the widget page size intentionally small; a future full inbox page must reuse the same pageable service instead of loading every notification.
- SSE must emit only a refresh signal and reuse the existing unread-count and inbox endpoints.

## In-App Service
- Notification creation uses a structured payload and performs an insert rather than an implicit upsert.
- The notification business unique key is a database guard against duplicate creation, not an instruction to update an existing notification.
- Idempotent creation, refresh, and aggregation must use separately named service methods when a concrete business flow requires those behaviors.
- Generic create methods must not reset an existing notification to unread.
- Business features publish typed events and must not call `NotificationService` directly.
- Notification event handlers use `@TransactionalEventListener(AFTER_COMMIT)` so source-domain state commits before notification work starts.
- Notification persistence triggered by an event uses a separate `REQUIRES_NEW` transaction.
- Notification failures are best-effort: handlers log and contain the failure instead of rolling back or changing source-domain state.
- Do not hide event construction behind a generic producer annotation or SpEL-based event mapping.
- Every single-notification read or update operation must include recipient ownership in its repository lookup.
- Marking one notification as read is idempotent.
- Marking all notifications as read must use one recipient-scoped JPQL bulk update instead of loading entities into application memory.
- Bulk updates must explicitly maintain audit timestamps because entity callbacks do not run for JPQL bulk operations.

## JSON Metadata
- JSON columns may be used when they are supported by the current H2 and Hibernate stack.
- Use JSON only for metadata, immutable snapshots, or document-shaped values that are not filtered, joined, sorted, or constrained independently.
- Represent JSON content with typed Java models instead of raw strings or ad-hoc maps whenever the structure is known.
- Do not move stable business fields, lifecycle state, ownership, or relationships into JSON to avoid relational modeling.
- Prefer JSON when introducing normalized tables would add disproportionate complexity for metadata that is always read and written as one value.
- If JSON content later requires independent queries or frequent partial updates, promote those fields into relational columns or tables.
- A document database may be considered only when the broader business data is genuinely document-oriented; it is not part of this project's current baseline.

## Delivery Progression
- Implement channels in this order: in-app, SSE, Email, then Telegram.
- External delivery records store rendered content snapshots so retries remain deterministic.
- Email delivery is created only when the recipient has enabled Email notifications.
- Telegram group delivery must be created once per eligible business event, not once per in-app recipient.
- Provider credentials and the Telegram group chat ID belong in YAML backed by environment variables.
