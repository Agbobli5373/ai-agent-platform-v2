package com.platform.rest;

import com.platform.domain.Document;
import com.platform.repository.DocumentRepository;
import com.platform.service.DocumentProcessingService;
import com.platform.service.VectorStoreService;
import com.platform.security.SecurityContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Path("/api/documents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({ "USER", "ADMIN" })
public class DocumentResource {

    @Inject
    DocumentRepository documentRepository;

    @Inject
    DocumentProcessingService documentProcessingService;

    @Inject
    VectorStoreService vectorStoreService;

    @Inject
    SecurityContext securityContext;

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadDocument(
            @RestForm("file") InputStream file,
            @RestForm("fileName") String fileName,
            @RestForm("contentType") String contentType) {
        Document document = documentProcessingService.uploadDocument(
                fileName,
                contentType,
                file,
                securityContext.getCurrentOrganizationId(),
                securityContext.getCurrentUserId());

        return Response.ok(document).build();
    }

    @GET
    public Response listDocuments() {
        List<Document> documents = documentRepository.findByOrganization(
                securityContext.getCurrentOrganizationId());
        return Response.ok(documents).build();
    }

    @GET
    @Path("/{id}")
    public Response getDocument(@PathParam("id") UUID id) {
        Document document = documentRepository.findById(id);
        if (document == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!document.organization.id.equals(securityContext.getCurrentOrganizationId())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.ok(document).build();
    }

    @GET
    @Path("/{id}/progress")
    public Response getProgress(@PathParam("id") UUID id) {
        DocumentProcessingService.DocumentProgress progress = documentProcessingService.getProgress(id);

        if (progress == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.ok(progress).build();
    }

    @POST
    @Path("/search")
    public Response searchDocuments(SearchRequest request) {
        List<VectorStoreService.SearchResult> results = vectorStoreService.semanticSearch(
                request.query,
                securityContext.getCurrentOrganizationId(),
                request.limit != null ? request.limit : 10);

        return Response.ok(results).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteDocument(@PathParam("id") UUID id) {
        Document document = documentRepository.findById(id);
        if (document == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!document.organization.id.equals(securityContext.getCurrentOrganizationId())) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        documentProcessingService.deleteDocument(id);
        return Response.noContent().build();
    }

    public static class SearchRequest {
        public String query;
        public Integer limit;
    }
}
