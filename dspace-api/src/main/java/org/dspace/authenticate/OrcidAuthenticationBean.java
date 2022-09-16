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
import static org.apache.commons.lang.BooleanUtils.toBoolean;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.dspace.content.Item.ANY;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.EPersonService;
import org.dspace.orcid.OrcidToken;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.orcid.service.OrcidTokenService;
import org.dspace.profile.ResearcherProfile;
import org.dspace.profile.service.ResearcherProfileService;
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
public class OrcidAuthenticationBean implements AuthenticationMethod {

    public static final String ORCID_AUTH_ATTRIBUTE = "orcid-authentication";

    private final static Logger LOGGER = LoggerFactory.getLogger(OrcidAuthenticationBean.class);

    private final static String LOGIN_PAGE_URL_FORMAT = "%s?client_id=%s&response_type=code&scope=%s&redirect_uri=%s";

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private OrcidConfiguration orcidConfiguration;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private EPersonService ePersonService;

    @Autowired
    private ResearcherProfileService researcherProfileService;

    @Autowired
    private OrcidSynchronizationService orcidSynchronizationService;

    @Autowired
    private OrcidTokenService orcidTokenService;

    @Override
    public int authenticate(Context context, String username, String password, String realm, HttpServletRequest request)
        throws SQLException {

        if (request == null) {
            LOGGER.warn("Unable to authenticate using ORCID because the request object is null.");
            return BAD_ARGS;
        }

        String code = (String) request.getParameter("code");
        if (StringUtils.isEmpty(code)) {
            LOGGER.warn("The incoming request has not code parameter");
            return NO_SUCH_USER;
        }
        request.setAttribute(ORCID_AUTH_ATTRIBUTE, true);
        return authenticateWithOrcid(context, code, request);
    }

    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {

        String authorizeUrl = orcidConfiguration.getAuthorizeEndpointUrl();
        String clientId = orcidConfiguration.getClientId();
        String redirectUri = orcidConfiguration.getRedirectUrl();
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
    public boolean isUsed(Context context, HttpServletRequest request) {
        return request.getAttribute(ORCID_AUTH_ATTRIBUTE) != null;
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

        EPerson ePerson = ePersonService.findByNetid(context, orcid);
        if (ePerson != null) {
            return ePerson.canLogIn() ? logInEPerson(context, token, ePerson) : BAD_ARGS;
        }

        Person person = getPersonFromOrcid(token);
        if (person == null) {
            return NO_SUCH_USER;
        }

        String email = getEmail(person).orElse(null);

        ePerson = ePersonService.findByEmail(context, email);
        if (ePerson != null) {
            return ePerson.canLogIn() ? logInEPerson(context, token, ePerson) : BAD_ARGS;
        }

        return canSelfRegister() ? registerNewEPerson(context, person, token) : NO_SUCH_USER;

    }

    private int logInEPerson(Context context, OrcidTokenResponseDTO token, EPerson ePerson)
        throws SQLException {

        context.setCurrentUser(ePerson);

        setOrcidMetadataOnEPerson(context, ePerson, token);

        ResearcherProfile profile = findProfile(context, ePerson);
        if (profile != null) {
            orcidSynchronizationService.linkProfile(context, profile.getItem(), token);
        }

        return SUCCESS;

    }

    private ResearcherProfile findProfile(Context context, EPerson ePerson) throws SQLException {
        try {
            return researcherProfileService.findById(context, ePerson.getID());
        } catch (AuthorizeException e) {
            throw new RuntimeException(e);
        }
    }

    private int registerNewEPerson(Context context, Person person, OrcidTokenResponseDTO token) throws SQLException {

        try {
            context.turnOffAuthorisationSystem();

            String email = getEmail(person)
                .orElseThrow(() -> new IllegalStateException("The email is configured private on orcid"));

            String orcid = token.getOrcid();

            EPerson eperson = ePersonService.create(context);

            eperson.setNetid(orcid);

            eperson.setEmail(email);

            Optional<String> firstName = getFirstName(person);
            if (firstName.isPresent()) {
                eperson.setFirstName(context, firstName.get());
            }

            Optional<String> lastName = getLastName(person);
            if (lastName.isPresent()) {
                eperson.setLastName(context, lastName.get());
            }
            eperson.setCanLogIn(true);
            eperson.setSelfRegistered(true);

            setOrcidMetadataOnEPerson(context, eperson, token);

            ePersonService.update(context, eperson);
            context.setCurrentUser(eperson);
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

    private void setOrcidMetadataOnEPerson(Context context, EPerson person, OrcidTokenResponseDTO token)
        throws SQLException {

        String orcid = token.getOrcid();
        String accessToken = token.getAccessToken();
        String[] scopes = token.getScopeAsArray();

        ePersonService.setMetadataSingleValue(context, person, "eperson", "orcid", null, null, orcid);
        ePersonService.clearMetadata(context, person, "eperson", "orcid", "scope", ANY);
        for (String scope : scopes) {
            ePersonService.addMetadata(context, person, "eperson", "orcid", "scope", null, scope);
        }

        OrcidToken orcidToken = orcidTokenService.findByEPerson(context, person);
        if (orcidToken == null) {
            orcidTokenService.create(context, person, accessToken);
        } else {
            orcidToken.setAccessToken(accessToken);
        }

    }

    private Person getPersonFromOrcid(OrcidTokenResponseDTO token) {
        try {
            return orcidClient.getPerson(token.getAccessToken(), token.getOrcid());
        } catch (Exception ex) {
            LOGGER.error("An error occurs retriving the ORCID record with id " + token.getOrcid(), ex);
            return null;
        }
    }

    private Optional<String> getEmail(Person person) {
        List<Email> emails = person.getEmails() != null ? person.getEmails().getEmails() : Collections.emptyList();
        if (CollectionUtils.isEmpty(emails)) {
            return Optional.empty();
        }
        return Optional.ofNullable(emails.get(0).getEmail());
    }

    private Optional<String> getFirstName(Person person) {
        return Optional.ofNullable(person.getName())
            .map(name -> name.getGivenNames())
            .map(givenNames -> givenNames.getContent());
    }

    private Optional<String> getLastName(Person person) {
        return Optional.ofNullable(person.getName())
            .map(name -> name.getFamilyName())
            .map(givenNames -> givenNames.getContent());
    }

    private boolean canSelfRegister() {
        String canSelfRegister = configurationService.getProperty("authentication-orcid.can-self-register", "true");
        if (isBlank(canSelfRegister)) {
            return true;
        }
        return toBoolean(canSelfRegister);
    }

    private OrcidTokenResponseDTO getOrcidAccessToken(String code) {
        try {
            return orcidClient.getAccessToken(code);
        } catch (Exception ex) {
            LOGGER.error("An error occurs retriving the ORCID access_token", ex);
            return null;
        }
    }

    public OrcidClient getOrcidClient() {
        return orcidClient;
    }

    public void setOrcidClient(OrcidClient orcidClient) {
        this.orcidClient = orcidClient;
    }

}
