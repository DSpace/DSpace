/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.dspace.app.rest.RestResourceController;
import org.dspace.core.EmailTemplateMimeType;

/**
 * REST model representing an email template in DSpace.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
public class EmailTemplateRest extends BaseObjectRest<String> {

    public static final String NAME = "emailtemplate";
    public static final String PLURAL_NAME = "emailtemplates";
    public static final String CATEGORY = RestAddressableModel.SYSTEM;

    @Size(max = 255)
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$")
    private String name;

    @Size(min = 10, max = 50000)
    private String content;

    @Size(max = 500)
    private String subject;

    private EmailTemplateMimeType mimetype;

    private List<EmailTemplateVariableRest> variables;
    private Instant lastModified;
    private String etag;

    /**
     * Default constructor.
     */
    public EmailTemplateRest() {
        this.variables = new ArrayList<>();
        this.mimetype = EmailTemplateMimeType.TEXT_PLAIN;
    }

    /**
     * Gets the REST resource identifier, which is the template name.
     *
     * @return the template name, or the raw id if name is not set
     */
    @Override
    public String getId() {
        return this.name != null ? this.name : this.id;
    }

    /**
     * Sets the REST resource identifier, syncing it with the template name.
     *
     * @param id the template name identifier to set
     */
    @Override
    public void setId(String id) {
        this.id = id;
        this.name = id;
    }

    /**
     * Gets the API category of this resource.
     *
     * @return the system category
     */
    @Override
    public String getCategory() {
        return CATEGORY;
    }

    /**
     * Gets the controller class handling this resource.
     *
     * @return the REST resource controller class
     */
    @Override
    @SuppressWarnings("rawtypes")
    public Class getController() {
        return RestResourceController.class;
    }

    /**
     * Gets the resource type name.
     *
     * @return the emailtemplate type name
     */
    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    /**
     * Gets the plural resource type name.
     *
     * @return the emailtemplates plural type name
     */
    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    /**
     * Gets the template filename identifier (e.g. "register").
     *
     * @return the template name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the template filename identifier, syncing it with the REST id.
     *
     * @param name the template name to set
     */
    public void setName(String name) {
        this.name = name;
        this.id = name;
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
     * Gets the email subject line parsed from the template.
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
     * @return the MIME type enum
     */
    public EmailTemplateMimeType getMimetype() {
        return mimetype;
    }

    /**
     * Sets the MIME type of the template output, defaulting to TEXT_PLAIN when null.
     *
     * @param mimetype the MIME type to set
     */
    public void setMimetype(EmailTemplateMimeType mimetype) {
        this.mimetype = mimetype != null ? mimetype : EmailTemplateMimeType.TEXT_PLAIN;
    }

    /**
     * Gets the template variables parsed from the comment headers.
     *
     * @return the list of template variables
     */
    public List<EmailTemplateVariableRest> getVariables() {
        return variables;
    }

    /**
     * Sets the template variables.
     *
     * @param variables the list of template variables to set
     */
    public void setVariables(List<EmailTemplateVariableRest> variables) {
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
     * Gets the ETag used for optimistic concurrency control.
     *
     * @return the ETag (SHA-256 hash of content)
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Sets the ETag used for optimistic concurrency control.
     *
     * @param etag the ETag to set
     */
    public void setEtag(String etag) {
        this.etag = etag;
    }
}
