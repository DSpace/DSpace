/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.services.email.oauth2;

/**
 * Gmail and Google Workspace OAuth2 provider.
 *
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class GmailOAuth2Provider extends AbstractOAuth2Provider {

    /** Provider identifier used in {@code mail.oauth2.provider}. */
    public static final String ID = "gmail";

    /** Default Google OAuth2 token endpoint. */
    public static final String DEFAULT_TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";

    /** Scope required to send mail through Gmail SMTP. */
    public static final String DEFAULT_SCOPE = "https://mail.google.com/";

    /**
     * Create the Gmail provider.
     */
    public GmailOAuth2Provider() {
        super(ID, DEFAULT_SCOPE);
    }

    @Override
    protected String resolveTokenEndpoint(String tenantId) {
        return DEFAULT_TOKEN_ENDPOINT;
    }
}
