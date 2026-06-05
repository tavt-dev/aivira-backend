package com.tien.aivirabackend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

class OpenApiConfigTest {
    @Test
    void openApiDefinitionDescribesCurrentBookstoreApi() {
        OpenAPIDefinition definition = OpenApiConfig.class.getAnnotation(OpenAPIDefinition.class);

        assertThat(definition).isNotNull();
        assertThat(definition.info().title()).contains("Aivira");
        assertThat(definition.info().description())
                .contains("single-vendor online bookstore")
                .contains("/api/v1")
                .contains("ApiResponse")
                .contains("PageResponse");
        assertThat(definition.servers()).hasSize(1);
        assertThat(definition.servers()[0].url()).isEqualTo("/api/v1");
    }

    @Test
    void bearerJwtSecuritySchemeIsExposed() {
        SecurityScheme securityScheme = OpenApiConfig.class.getAnnotation(SecurityScheme.class);

        assertThat(securityScheme).isNotNull();
        assertThat(securityScheme.name()).isEqualTo("bearerAuth");
        assertThat(securityScheme.type()).isEqualTo(SecuritySchemeType.HTTP);
        assertThat(securityScheme.scheme()).isEqualTo("bearer");
        assertThat(securityScheme.bearerFormat()).isEqualTo("JWT");
    }
}
