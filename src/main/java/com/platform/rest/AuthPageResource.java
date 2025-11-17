package com.platform.rest;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/auth")
public class AuthPageResource {

    @Inject
    @io.quarkus.qute.Location("auth/login.html")
    Template login;

    @Inject
    @io.quarkus.qute.Location("auth/register.html")
    Template register;

    @Inject
    @io.quarkus.qute.Location("auth/forgot-password.html")
    Template forgotPassword;

    @GET
    @Path("/login")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance loginPage() {
        return login.instance();
    }

    @GET
    @Path("/register")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance registerPage() {
        return register.instance();
    }

    @GET
    @Path("/forgot-password")
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance forgotPasswordPage() {
        return forgotPassword.instance();
    }
}
