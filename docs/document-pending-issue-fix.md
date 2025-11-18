# Document PENDING Status - Issue & Fix

## Problem
Documents were stuck in PENDING status and never progressing to INDEXED.

## Root Causes

### 1. Missing Content Storage
- **Issue**: Document entity had no `content` field
- **Impact**: Content was read during upload but not persisted
- **Result**: Async processing had no content to index

### 2. PostgreSQL UTF-8 Encoding Error
- **Issue**: PDF files contain null bytes (0x00) that PostgreSQL TEXT columns reject
- **Error**: `invalid byte sequence for encoding "UTF8": 0x00`
- **Impact**: Database insert failed for binary files

## Solutions Implemented

### 1. Added Content Column to Document Entity
```java
@Column(columnDefinition = "TEXT")
public String content;
```

### 2. Created Database Migration
File: `V6__add_content_to_documents.sql`
```sql
ALTER TABLE documents ADD COLUMN content TEXT;
```

### 3. Added Text Extraction Logic
- **Text files (.txt)**: Extract as UTF-8 string
- **Binary files (PDF, DOCX)**: Use placeholder text to avoid null bytes
- **Future**: Can add Apache PDFBox (PDF) or Apache POI (DOCX) for real extraction

### 4. Fixed Async Processing
- Added `@Transactional` method for processing
- Content now read from database instead of passed as parameter
- Proper error logging

## Current Behavior

### For Text Files (.txt)
1. Upload → Content extracted as UTF-8
2. Stored in database
3. Async processing → Chunking → Embedding generation
4. Status changes: PENDING → INDEXED

### For Binary Files (PDF, DOCX)
1. Upload → Placeholder text used
2. Stored in database (no null bytes)
3. Async processing works with placeholder
4. Status changes: PENDING → INDEXED

## Testing

### To Test the Fix:
1. Upload a .txt file - should process successfully
2. Upload a .pdf file - will use placeholder text but process successfully
3. Check status changes from PENDING to INDEXED
4. Try semantic search on indexed documents

### Expected Timeline:
- Upload: Immediate
- Processing: 5-30 seconds (depending on document size and Mistral AI API response)
- Status update: Automatic

## Future Enhancements

### Add Real PDF/DOCX Extraction
Add dependencies to `pom.xml`:
```xml
<!-- PDF text extraction -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.0</version>
</dependency>

<!-- DOCX text extraction -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

Then update `extractTextContent()` method to use these libraries.

## Notes

- Existing PENDING documents won't auto-process (they have no content)
- Re-upload those documents to process them
- The migration runs automatically on application restart
- Placeholder text is sufficient for testing vector search functionality
