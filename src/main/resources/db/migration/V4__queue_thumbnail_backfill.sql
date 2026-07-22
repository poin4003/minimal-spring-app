UPDATE media
SET processing_status = 'PENDING'
WHERE status = 'ACTIVE'
  AND processing_status = 'READY'
  AND (
      kind IN ('IMAGE', 'VIDEO', 'AUDIO')
      OR (kind = 'DOCUMENT' AND LOWER(content_type) = 'application/pdf')
  );
