/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * Microsoft 365 and Outlook OAuth2 provider.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class MicrosoftOAuth2Provider extends AbstractOAuth2Provider {

    /** Provider identifier used in {@code mail.oauth2.provider}. */
    public static final String ID = "microsoft";

    /** Scope required to send mail through Microsoft 365 SMTP. */
    public static final String DEFAULT_SCOPE = "https://outlook.office.com/SMTP.Send offline_access";

    private static final Pattern TENANT_PATTERN = Pattern.compile("[A-Za-z0-9.-]+");

    /**
     * Create the Microsoft 365 provider.
     */
    public MicrosoftOAuth2Provider() {
        super(ID, DEFAULT_SCOPE);
    }

    @Override
    protected void validateTenant(String tenantId) {
        if (StringUtils.isBlank(tenantId)) {
            throw new OAuth2ConfigurationException(
                "Missing required OAuth2 setting: mail.oauth2.microsoft.tenantId"
                    + " (or mail.oauth2.tenantId)"
            );
        }
        if (!TENANT_PATTERN.matcher(tenantId).matches()) {
            throw new OAuth2ConfigurationException(
                "Invalid OAuth2 tenant id for provider 'microsoft': " + tenantId
            );
        }
    }

    @Override
    protected String resolveTokenEndpoint(String tenantId) {
        return "https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/token";
    }
}
