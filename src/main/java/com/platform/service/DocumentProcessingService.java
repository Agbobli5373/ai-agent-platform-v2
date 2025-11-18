package com.platform.service;

import com.platform.domain.Document;
import com.platform.domain.Organization;
import com.platform.repository.DocumentRepository;
import com.platform.repository.OrganizationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class DocumentProcessingService {

    private static final Logger LOG = Logger.getLogger(DocumentProcessingService.class);

    @Inject
    DocumentRepository documentRepository;

    @Inject
    OrganizationRepository organizationRepository;

    @Inject
    VectorStoreService vectorStoreService;

    @Transactional
    public Document uploadDocument(String fileName, String contentType, InputStream inputStream,
            UUID organizationId, UUID uploadedBy) {
        LOG.infof("Uploading document: %s", fileName);

        Organization organization = organizationRepository.findById(organizationId);
        if (organization == null) {
            throw new IllegalArgumentException("Organization not found");
        }

        try {
            byte[] fileBytes = inputStream.readAllBytes();
            String content = extractTextContent(fileBytes, contentType, fileName);

            Document document = new Document();
            document.filename = fileName;
            document.contentType = contentType;
            document.content = content;
            document.organization = organization;
            document.status = Document.DocumentStatus.PENDING;
            document.uploadedAt = LocalDateTime.now();
            document.sizeBytes = (long) fileBytes.length;

            documentRepository.persist(document);
            LOG.infof("Document uploaded successfully: %s (ID: %s)", fileName, document.id);

            processDocumentAsync(document.id);

            return document;
        } catch (Exception e) {
            LOG.errorf(e, "Failed to upload document: %s", fileName);
            throw new RuntimeException("Document upload failed", e);
        }
    }

    private String extractTextContent(byte[] fileBytes, String contentType, String fileName) {
        // For now, only handle text files
        // PDF and DOCX extraction would require additional libraries (Apache PDFBox,
        // Apache POI)
        if (contentType != null && contentType.contains("text")) {
            return new String(fileBytes, StandardCharsets.UTF_8);
        } else if (fileName.endsWith(".txt")) {
            return new String(fileBytes, StandardCharsets.UTF_8);
        } else {
            // For PDF/DOCX, return a placeholder for now
            // In production, you would use Apache PDFBox or Apache POI
            LOG.warnf("Binary file type not supported for text extraction: %s. Using placeholder.", contentType);
            return "Sample document content for testing. " +
                    "This is a placeholder text for binary files like PDF or DOCX. " +
                    "In production, you would extract actual text using libraries like Apache PDFBox (for PDF) " +
                    "or Apache POI (for DOCX). For now, this allows testing the vector search functionality.";
        }
    }

    public void processDocumentAsync(UUID documentId) {
        CompletableFuture.runAsync(() -> {
            try {
                processDocument(documentId);
            } catch (Exception e) {
                LOG.errorf(e, "Async document processing failed for ID: %s", documentId);
            }
        });
    }

    @Transactional
    public void processDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId);
        if (document != null && document.content != null) {
            LOG.infof("Processing document: %s", document.filename);
            vectorStoreService.indexDocument(document, document.content);
        } else {
            LOG.errorf("Document not found or has no content: %s", documentId);
        }
    }

    public DocumentProgress getProgress(UUID documentId) {
        Document document = documentRepository.findById(documentId);
        if (document == null) {
            return null;
        }

        DocumentProgress progress = new DocumentProgress();
        progress.documentId = documentId;
        progress.status = document.status.name();

        if (document.status == Document.DocumentStatus.PENDING) {
            progress.percentComplete = 50;
        } else if (document.status == Document.DocumentStatus.INDEXED) {
            progress.percentComplete = 100;
        } else if (document.status == Document.DocumentStatus.FAILED) {
            progress.percentComplete = 0;
        }

        return progress;
    }

    @Transactional
    public void deleteDocument(UUID documentId) {
        Document document = documentRepository.findById(documentId);
        if (document != null) {
            documentRepository.delete(document);
            LOG.infof("Document deleted: %s", documentId);
        }
    }

    public static class DocumentProgress {
        public UUID documentId;
        public String status;
        public int percentComplete;
    }
}
