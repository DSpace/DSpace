/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization.impl;
import java.sql.SQLException;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.rest.authorization.AuthorizationFeature;
import org.dspace.app.rest.authorization.AuthorizationFeatureDocumentation;
import org.dspace.app.rest.model.BaseObjectRest;
import org.dspace.app.rest.model.SiteRest;
import org.dspace.core.Context;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The send feedback feature. It can be used to verify if the parameter that contain
 * recipient e-mail is configured.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
@Component
@AuthorizationFeatureDocumentation(name = CanSendFeedbackFeature.NAME,
    description = "It can be used to verify if the parameter that contain recipient e-mail is configured.")
public class CanSendFeedbackFeature implements AuthorizationFeature {

    public static final String NAME = "canSendFeedback";

    @Autowired
    private ConfigurationService configurationService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean isAuthorized(Context context, BaseObjectRest object) throws SQLException {
        String recipientEmail = configurationService.getProperty("feedback.recipient");
        return StringUtils.isNotBlank(recipientEmail);
    }

    @Override
    public String[] getSupportedTypes() {
        return new String[] { SiteRest.CATEGORY + "." + SiteRest.NAME };
    }

}