package com.platform.rest;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.util.List;

@Path("/documents")
@Produces(MediaType.TEXT_HTML)
public class DocumentPageResource {

    private static final Logger LOG = Logger.getLogger(DocumentPageResource.class);

    @Inject
    @io.quarkus.qute.Location("documents/list.html")
    Template documentsList;

    @Inject
    @io.quarkus.qute.Location("documents/upload.html")
    Template documentsUpload;

    @GET
    @PermitAll
    public TemplateInstance listPage() {
        LOG.info("Rendering documents list page");
        return documentsList
                .data("documents", List.of())
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization");
    }

    @GET
    @Path("/upload")
    @PermitAll
    public TemplateInstance uploadPage() {
        LOG.info("Rendering document upload page");
        return documentsUpload
                .data("userInitials", "U")
                .data("userName", "User")
                .data("userEmail", "user@example.com")
                .data("userRole", "USER")
                .data("currentOrgName", "My Organization");
    }
}
