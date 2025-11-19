# Embedding Dimension Mismatch - Fix

## Problem
Documents were failing to index with error in `VectorStoreService.indexDocument()`.

## Root Cause
**Vector dimension mismatch** between database and AI model:
- **Database schema**: `vector(1536)` - configured for OpenAI embeddings
- **Mistral AI model**: Returns `1024` dimensions
- **Result**: Database rejected the embeddings due to size mismatch

## Solution

### 1. Created Migration V7
File: `V7__fix_embedding_dimension.sql`
```sql
-- Drop old index
DROP INDEX IF EXISTS idx_document_embeddings_vector;

-- Change vector dimension from 1536 to 1024
ALTER TABLE document_embeddings ALTER COLUMN embedding TYPE vector(1024);

-- Recreate index
CREATE INDEX idx_document_embeddings_vector ON document_embeddings 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

### 2. Updated DocumentEmbedding Entity
Changed from:
```java
@Column(columnDefinition = "vector(1536)")
```

To:
```java
@Column(columnDefinition = "vector(1024)")
```

## What Happens Now

1. **Migration runs automatically** when Quarkus reloads
2. **Existing embeddings** (if any) will be dropped due to type change
3. **New documents** will process successfully with 1024-dimension embeddings
4. **Status flow**: PENDING → INDEXED (should complete in 5-30 seconds)

## Testing

### After Server Restart:
1. Upload a new text file (.txt)
2. Watch the logs for:
   ```
   Processing document: filename.txt
   Document chunked into X pieces
   Document indexed successfully
   ```
3. Check document status changes to INDEXED
4. Try semantic search

### Expected Behavior:
- ✅ No more "Document indexing failed" errors
- ✅ Embeddings stored successfully
- ✅ Documents move from PENDING to INDEXED
- ✅ Semantic search works

## Notes

- **Mistral AI embedding model**: `mistral-embed` produces 1024-dimensional vectors
- **OpenAI embedding model**: `text-embedding-ada-002` produces 1536-dimensional vectors
- If you switch to OpenAI in the future, change back to `vector(1536)`
- The dimension must match between the AI model and database schema

## Verification

Check the migration was applied:
```sql
SELECT column_name, data_type, udt_name 
FROM information_schema.columns 
WHERE table_name = 'document_embeddings' 
AND column_name = 'embedding';
```

Should show: `vector(1024)`
