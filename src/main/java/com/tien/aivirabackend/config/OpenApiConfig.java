package com.tien.aivirabackend.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Aivira Backend API",
                        version = "0.0.1-SNAPSHOT",
                        description =
                                """
                                OpenAPI documentation for the Aivira single-vendor online bookstore backend.
                                The API base path is /api/v1. Products represent books while the stable backend
                                resource name remains Product and the public catalog route remains /products.
                                Responses are wrapped in ApiResponse, and paginated lists are wrapped in
                                ApiResponse<PageResponse<T>>.
                                """,
                        contact = @Contact(name = "Aivira Store")),
        servers = @Server(url = "/api/v1", description = "Local API base path"))
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {}
