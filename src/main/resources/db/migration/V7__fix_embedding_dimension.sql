-- Fix embedding dimension from 1536 (OpenAI) to 1024 (Mistral)
-- Drop the old index first
DROP INDEX IF EXISTS idx_document_embeddings_vector;

-- Alter the column type
ALTER TABLE document_embeddings ALTER COLUMN embedding TYPE vector(1024);

-- Recreate the index with the correct dimension
CREATE INDEX idx_document_embeddings_vector ON document_embeddings 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
