/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.rest.model.EmailTemplateRest;
import org.dspace.app.rest.model.EmailTemplateVariableRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.core.EmailTemplate;
import org.dspace.core.EmailTemplateVariable;
import org.springframework.stereotype.Component;

/**
 * Converter that converts an {@link EmailTemplate} domain object into an {@link EmailTemplateRest} object.
 *
 * @author Moamen Elbarky (moamen.elbarky@gmail.com)
 */
@Component
public class EmailTemplateConverter implements DSpaceConverter<EmailTemplate, EmailTemplateRest> {

    /**
     * Converts an {@link EmailTemplate} domain object into its REST representation.
     *
     * @param modelObject the domain object to convert
     * @param projection  the active projection
     * @return the converted {@link EmailTemplateRest} object
     */
    @Override
    public EmailTemplateRest convert(EmailTemplate modelObject, Projection projection) {
        EmailTemplateRest rest = new EmailTemplateRest();
        rest.setProjection(projection);
        rest.setId(modelObject.getName());
        rest.setName(modelObject.getName());
        rest.setContent(modelObject.getContent());
        rest.setSubject(modelObject.getSubject());
        rest.setMimetype(modelObject.getMimetype());
        rest.setLastModified(modelObject.getLastModified());
        rest.setEtag(modelObject.getEtag());

        if (modelObject.getVariables() != null) {
            List<EmailTemplateVariableRest> variableRests = new ArrayList<>();
            for (EmailTemplateVariable var : modelObject.getVariables()) {
                variableRests.add(new EmailTemplateVariableRest(
                    var.getIndex(), var.getDescription(), var.getPlaceholder()));
            }
            rest.setVariables(variableRests);
        }

        return rest;
    }

    /**
     * Gets the domain model class handled by this converter.
     *
     * @return the {@link EmailTemplate} class
     */
    @Override
    public Class<EmailTemplate> getModelClass() {
        return EmailTemplate.class;
    }
}
