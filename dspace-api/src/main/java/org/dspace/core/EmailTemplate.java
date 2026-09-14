/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain model representing a DSpace email template.
 *
 * <p>This is a POJO (Plain Old Java Object), not a DSpace database entity,
 * because email templates are stored as VTL (Velocity Template Language)
 * files on the filesystem under {@code {dspace.dir}/config/emails/}.</p>
 *
 * <p>The ETag is computed as a SHA-256 hash of the template content,
 * used for optimistic concurrency control via HTTP conditional headers
 * ({@code If-Match} / {@code If-None-Match}).</p>
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class EmailTemplate {

    /**
     * The template filename identifier (e.g., "register", "submit_reject").
     * This serves as the unique ID for the template in the REST API.
     */
    private String name;

    /**
     * The full VTL template body, including comment headers and
     * {@code #set($subject = ...)} directive.
     */
    private String content;

    /**
     * The email subject line extracted from the template's
     * {@code #set($subject = ...)} directive.
     */
    private String subject;

    /**
     * The MIME type of the template output.
     * Either {@link EmailTemplateMimeType#TEXT_PLAIN} (default) or {@link EmailTemplateMimeType#TEXT_HTML}
     * (if the template includes {@code #set($mimetype = "text/html")}).
     * See DSpace PR #11755 for HTML email support.
     */
    private EmailTemplateMimeType mimetype;

    /**
     * The list of variables (parameters) available in this template,
     * parsed from the {@code ## {N} description} comment headers.
     */
    private List<EmailTemplateVariable> variables;

    /**
     * The last modified timestamp of the template file on disk.
     */
    private Instant lastModified;

    /**
     * The deterministic ETag for optimistic concurrency control (SHA-256).
     */
    private String etag;

    /**
     * Default constructor.
     */
    public EmailTemplate() {
        this.variables = new ArrayList<>();
        this.mimetype = EmailTemplateMimeType.TEXT_PLAIN;
    }

    /**
     * Gets the template filename identifier.
     *
     * @return the template name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the template filename identifier.
     *
     * @param name the template name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the full VTL template body.
     *
     * @return the template content
     */
    public String getContent() {
        return content;
    }

    /**
     * Sets the full VTL template body.
     *
     * @param content the template content to set
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     * Gets the email subject line.
     *
     * @return the subject
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the email subject line.
     *
     * @param subject the subject to set
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Gets the MIME type of the template output.
     *
     * @return the MIME type enum (TEXT_PLAIN or TEXT_HTML)
     */
    public EmailTemplateMimeType getMimetype() {
        return mimetype;
    }

    /**
     * Sets the MIME type of the template output.
     *
     * @param mimetype the MIME type to set
     */
    public void setMimetype(EmailTemplateMimeType mimetype) {
        this.mimetype = mimetype != null ? mimetype : EmailTemplateMimeType.TEXT_PLAIN;
    }

    /**
     * Gets the list of variables available in this template.
     *
     * @return the list of variables
     */
    public List<EmailTemplateVariable> getVariables() {
        return variables;
    }

    /**
     * Sets the list of variables available in this template.
     *
     * @param variables the list of variables to set
     */
    public void setVariables(List<EmailTemplateVariable> variables) {
        this.variables = variables;
    }

    /**
     * Gets the last modified timestamp of the template file.
     *
     * @return the last modified timestamp
     */
    public Instant getLastModified() {
        return lastModified;
    }

    /**
     * Sets the last modified timestamp of the template file.
     *
     * @param lastModified the last modified timestamp to set
     */
    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Gets the ETag for optimistic concurrency control.
     *
     * @return the ETag (SHA-256 hash of content)
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Sets the ETag for optimistic concurrency control.
     *
     * @param etag the ETag to set
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }
}
