/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import static java.lang.String.format;
import static java.net.URLEncoder.encode;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.client.OrcidConfiguration;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * ORCID authentication for DSpace.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidAuthentication implements AuthenticationMethod {

    public static final String ORCID_AUTH_ATTRIBUTE = "orcid-authentication";

    private final static Logger LOGGER = LoggerFactory.getLogger(OrcidAuthentication.class);

    private final static String LOGIN_PAGE_URL_FORMAT = "%s?client_id=%s&response_type=code&scope=%s&redirect_uri=%s";

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private OrcidConfiguration orcidConfiguration;

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
        throws SQLException {

        if (request == null) {
            LOGGER.warn("Unable to authenticate using ORCID because the request object is null.");
            return BAD_ARGS;
        }

        if (request.getAttribute(ORCID_AUTH_ATTRIBUTE) == null) {
            return NO_SUCH_USER;
        }

        String code = (String) request.getParameter("code");
        if (StringUtils.isEmpty(code)) {
            LOGGER.warn("The incoming request has not code parameter");
            return NO_SUCH_USER;
        }

        return authenticateWithOrcid(context, code);
    }

    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {

        String authorizeUrl = orcidConfiguration.getAuthorizeEndpointUrl();
        String clientId = orcidConfiguration.getClientId();
        String redirectUri = orcidConfiguration.getRedirectUri();
        String scopes = String.join("+", orcidConfiguration.getScopes());

        if (StringUtils.isAnyBlank(authorizeUrl, clientId, redirectUri, scopes)) {
            LOGGER.error("Missing mandatory configuration properties for OrcidAuthentication");
            return "";
        }

        try {
            return format(LOGIN_PAGE_URL_FORMAT, authorizeUrl, clientId, scopes, encode(redirectUri, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
            return "";
        }

    }

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request, EPerson eperson) throws SQLException {

    }

    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String username) throws SQLException {
        return false;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
        return Collections.emptyList();
    }

    @Override
    public String getName() {
        return "orcid";
    }

    private int authenticateWithOrcid(Context context, String code) {
        OrcidTokenResponseDTO token = getOrcidAccessToken(code);
        if (token == null) {
            return NO_SUCH_USER;
        }

        orcidClient.getRecord(token.getAccessToken(), token.getOrcid());
        return 0;
    }

    private OrcidTokenResponseDTO getOrcidAccessToken(String code) {
        try {
            return orcidClient.getAccessToken(code);
        } catch (Exception ex) {
            LOGGER.error("An error occurs retriving the ORCID access_token", ex);
            return null;
        }
    }

}
