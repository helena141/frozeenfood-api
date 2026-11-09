ALTER TABLE deliveries
ADD COLUMN IF NOT EXISTS deleted_at timestamptz;

ALTER TABLE payments ADD COLUMN IF NOT EXISTS deleted_at timestamptz;