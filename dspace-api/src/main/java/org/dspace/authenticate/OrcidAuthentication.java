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
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.client.OrcidClient;
import org.dspace.app.orcid.client.OrcidConfiguration;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.services.ConfigurationService;
import org.orcid.jaxb.model.v3.release.record.Email;
import org.orcid.jaxb.model.v3.release.record.Person;
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

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonService ePersonService;

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

        return authenticateWithOrcid(context, code, request);
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
        return canSelfRegister();
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

    private int authenticateWithOrcid(Context context, String code, HttpServletRequest request) throws SQLException {
        OrcidTokenResponseDTO token = getOrcidAccessToken(code);
        if (token == null) {
            return NO_SUCH_USER;
        }

        String orcid = token.getOrcid();

        List<EPerson> ePersons = ePersonService.findByOrcid(context, orcid);
        if (CollectionUtils.isNotEmpty(ePersons)) {
            context.setCurrentUser(ePersons.get(0));
            AuthenticateServiceFactory.getInstance().getAuthenticationService().initEPerson(context, request,
                ePersons.get(0));
            return SUCCESS;
        }

        Person person = getPerson(token);
        if (person == null) {
            return NO_SUCH_USER;
        }

        String email = getEmail(person);

        EPerson eperson = ePersonService.findByEmail(context, email);
        if (eperson != null) {
            eperson.setCanLogIn(true);
            ePersonService.addMetadata(context, eperson, "eperson", "orcid", null, null, orcid);
            context.setCurrentUser(eperson);
            return SUCCESS;
        }

        return canSelfRegister() ? registerNewEPerson(context, person, orcid) : NO_SUCH_USER;

    }

    private int registerNewEPerson(Context context, Person person, String orcid) throws SQLException {

        try {
            context.turnOffAuthorisationSystem();

            EPerson eperson = ePersonService.create(context);

            eperson.setNetid(orcid);
            eperson.setEmail(getEmail(person));
            eperson.setFirstName(context, getFirstName(person));
            eperson.setLastName(context, getLastName(person));
            eperson.setCanLogIn(true);
            eperson.setSelfRegistered(true);

            ePersonService.addMetadata(context, eperson, "eperson", "orcid", null, null, orcid);

            ePersonService.update(context, eperson);
            context.dispatchEvents();

            return SUCCESS;

        } catch (Exception ex) {
            LOGGER.error("An error occurs registering a new EPerson from ORCID", ex);
            context.rollback();
            return NO_SUCH_USER;
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private Person getPerson(OrcidTokenResponseDTO token) {
        try {
            return orcidClient.getPerson(token.getAccessToken(), token.getOrcid());
        } catch (Exception ex) {
            LOGGER.error("An error occurs retriving the ORCID record with id " + token.getOrcid(), ex);
            return null;
        }
    }

    private String getEmail(Person person) {
        List<Email> emails = person.getEmails() != null ? person.getEmails().getEmails() : Collections.emptyList();
        if (CollectionUtils.isEmpty(emails)) {
            throw new IllegalStateException("The found ORCID person has no emails");
        }
        return emails.get(0).getEmail();
    }

    private String getFirstName(Person person) {
        return Optional.ofNullable(person.getName())
            .map(name -> name.getGivenNames())
            .map(givenNames -> givenNames.getContent())
            .orElseThrow(() -> new IllegalStateException("The found ORCID person has no first name"));
    }

    private String getLastName(Person person) {
        return Optional.ofNullable(person.getName())
            .map(name -> name.getFamilyName())
            .map(givenNames -> givenNames.getContent())
            .orElseThrow(() -> new IllegalStateException("The found ORCID person has no last name"));
    }

    private boolean canSelfRegister() {
        return configurationService.getBooleanProperty("authentication-orcid.can-self-register", true);
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
