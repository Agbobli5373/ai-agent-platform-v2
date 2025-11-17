-- Add created_at column to conversations table
ALTER TABLE conversations ADD COLUMN created_at TIMESTAMP;

-- Set default value for existing rows
UPDATE conversations SET created_at = started_at WHERE created_at IS NULL;

-- Make the column NOT NULL after setting values
ALTER TABLE conversations ALTER COLUMN created_at SET NOT NULL;

-- Add index for status column (referenced in entity)
CREATE INDEX IF NOT EXISTS idx_conversations_status ON conversations(status);
