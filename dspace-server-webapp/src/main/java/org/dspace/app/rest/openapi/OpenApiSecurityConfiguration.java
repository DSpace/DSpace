/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.openapi;

import java.util.ArrayList;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI security configuration.
 * <p>
 * Defines the Bearer (JWT) authentication scheme used by the DSpace REST API.
 */
@Configuration
public class OpenApiSecurityConfiguration {

    public static final String BEARER_AUTH = "bearerAuth";

    private static final String CSRF_REQUEST_HEADER = "X-XSRF-TOKEN";
    private static final String CSRF_RESPONSE_HEADER = "DSPACE-XSRF-TOKEN";

    @Bean
    public OpenAPI dspaceOpenApi() {
        return new OpenAPI()
            .components(new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")))
            // Apply globally so Swagger UI sends the header on "Try it out".
            .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }

    @Bean
    public OpenApiCustomizer dspaceCsrfHeaderCustomizer() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }

            openApi.getPaths().values().forEach(pathItem -> {
                if (pathItem.getPost() != null) {
                    addHeaderIfMissing(pathItem.getPost());
                }
                if (pathItem.getPut() != null) {
                    addHeaderIfMissing(pathItem.getPut());
                }
                if (pathItem.getPatch() != null) {
                    addHeaderIfMissing(pathItem.getPatch());
                }
                if (pathItem.getDelete() != null) {
                    addHeaderIfMissing(pathItem.getDelete());
                }
            });
        };
    }

    private static void addHeaderIfMissing(Operation operation) {
        if (operation.getParameters() == null) {
            operation.setParameters(new ArrayList<>());
        }

        boolean alreadyPresent = operation.getParameters().stream()
                .anyMatch(p -> p != null
                    && CSRF_REQUEST_HEADER.equalsIgnoreCase(p.getName())
                    && "header".equals(p.getIn()));
        if (!alreadyPresent) {
            operation.addParametersItem(createCsrfHeaderParameter());
        }
    }

    private static Parameter createCsrfHeaderParameter() {
        return new Parameter()
            .in("header")
            .name(CSRF_REQUEST_HEADER)
            .required(false)
            .schema(new StringSchema())
                .description("CSRF token (required for POST/PUT/PATCH/DELETE). "
                    + "Obtain it from the response header '" + CSRF_RESPONSE_HEADER + "'. "
                    + "For example, call POST /api/authn/login once without the token; "
                    + "a 403 is expected and the header will be returned.");
    }
}
