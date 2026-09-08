/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.converter.EmailTemplateConverter;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.NotModifiedException;
import org.dspace.app.rest.exception.PreconditionFailedException;
import org.dspace.app.rest.exception.PreconditionRequiredException;
import org.dspace.app.rest.model.EmailTemplateRest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.EmailTemplate;
import org.dspace.core.service.EmailTemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * REST repository for managing email templates.
 *
 * <p>Supports listing all templates, retrieving a template by name, and updating template content.
 * Optimistic concurrency control is implemented using HTTP ETags ({@code If-Match} / {@code If-None-Match}).</p>
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
@Component(EmailTemplateRest.CATEGORY + "." + EmailTemplateRest.PLURAL_NAME)
public class EmailTemplateRestRepository extends DSpaceRestRepository<EmailTemplateRest, String> {

    @Autowired
    private EmailTemplateService emailTemplateService;

    @Autowired
    private EmailTemplateConverter emailTemplateConverter;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public EmailTemplateRest findOne(Context context, String name) {
        try {
            EmailTemplate template = emailTemplateService.findByName(context, name);
            if (template == null) {
                return null;
            }

            String etag = template.getEtag();
            if (etag != null) {
                response.setHeader(HttpHeaders.ETAG, "\"" + etag + "\"");

                String ifNoneMatch = request.getHeader(HttpHeaders.IF_NONE_MATCH);
                if (ifNoneMatch != null) {
                    String cleanIfNoneMatch = unquote(ifNoneMatch);
                    if (cleanIfNoneMatch.equals(etag) || "*".equals(ifNoneMatch)) {
                        throw new NotModifiedException();
                    }
                }
            }

            return emailTemplateConverter.convert(template, utils.obtainProjection());
        } catch (AuthorizeException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    public Page<EmailTemplateRest> findAll(Context context, Pageable pageable) {
        try {
            List<EmailTemplate> templates = emailTemplateService.findAll(context);
            return converter.toRestPage(templates, pageable, utils.obtainProjection());
        } catch (AuthorizeException e) {
            throw new AccessDeniedException(e.getMessage(), e);
        } catch (IOException | SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Override
    @PreAuthorize("hasAuthority('ADMIN')")
    protected EmailTemplateRest put(Context context, HttpServletRequest req, String apiCategory, String model,
                                   String id, JsonNode jsonNode) throws SQLException, AuthorizeException {

        String ifMatch = req.getHeader(HttpHeaders.IF_MATCH);
        if (StringUtils.isBlank(ifMatch)) {
            throw new PreconditionRequiredException(
                "If-Match header is required for updating email templates");
        }

        EmailTemplateRest restBody;
        try {
            restBody = mapper.readValue(jsonNode.toString(), EmailTemplateRest.class);
        } catch (JsonProcessingException e) {
            throw new DSpaceBadRequestException("Cannot parse JSON in request body", e);
        }

        if (restBody == null || StringUtils.isBlank(restBody.getContent())) {
            throw new DSpaceBadRequestException("Template content cannot be blank");
        }

        EmailTemplate existing;
        try {
            existing = emailTemplateService.findByName(context, id);
        } catch (IOException e) {
            throw new RuntimeException("Error reading template: " + id, e);
        }

        if (existing == null) {
            throw new ResourceNotFoundException("Email template not found: " + id);
        }

        String cleanIfMatch = unquote(ifMatch);
        if (!"*".equals(cleanIfMatch) && !cleanIfMatch.equals(existing.getEtag())) {
            throw new PreconditionFailedException(
                "If-Match ETag mismatch. Template has been modified by another process.");
        }

        try {
            EmailTemplate updated = emailTemplateService.update(context, id, restBody.getContent());
            if (updated.getEtag() != null) {
                response.setHeader(HttpHeaders.ETAG, "\"" + updated.getEtag() + "\"");
            }
            return emailTemplateConverter.convert(updated, utils.obtainProjection());
        } catch (IllegalArgumentException e) {
            throw new DSpaceBadRequestException(e.getMessage(), e);
        } catch (IOException e) {
            throw new RuntimeException("Error updating template: " + id, e);
        }
    }

    @Override
    public Class<EmailTemplateRest> getDomainClass() {
        return EmailTemplateRest.class;
    }

    private String unquote(String headerValue) {
        if (headerValue != null && headerValue.startsWith("\"") && headerValue.endsWith("\"")
                && headerValue.length() >= 2) {
            return headerValue.substring(1, headerValue.length() - 1);
        }
        return headerValue != null ? headerValue : "";
    }
}
