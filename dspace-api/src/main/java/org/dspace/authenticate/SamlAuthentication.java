/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.content.NonUniqueMetadataException;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * SAML authentication for DSpace.
 *
 * @author Ray Lee
 */
public class SamlAuthentication implements AuthenticationMethod {
    private static final Logger log = LogManager.getLogger(SamlAuthentication.class);

    // Additional metadata mappings.
    protected Map<String, String> metadataHeaderMap = null;

    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    protected MetadataFieldService metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();

    protected MetadataSchemaService metadataSchemaService =
        ContentServiceFactory.getInstance().getMetadataSchemaService();

    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    /**
     * Authenticate the given or implicit credentials. This is the heart of the
     * authentication method: test the credentials for authenticity, and if
     * accepted, attempt to match (or optionally, create) an
     * <code>EPerson</code>. If an <code>EPerson</code> is found it is set in
     * the <code>Context</code> that was passed.
     *
     * DSpace supports authentication using NetID or email address. A user's NetID
     * is a unique identifier from the IdP that identifies a particular user. The
     * NetID can be of almost any form, such as a unique integer or string. In
     * SAML, this is referred to as a Name ID.
     *
     * There are two ways to supply identity information to DSpace:
     *
     * 1) Name ID from SAML attribute (best)
     *
     * The Name ID-based method is superior because users may change their email
     * address with the identity provider. When this happens DSpace will not be
     * able to associate their new address with their old account.
     *
     * 2) Email address from SAML attribute (okay)
     *
     * In the case where a Name ID header is not available or not found DSpace
     * will fall back to identifying a user based upon their email address.
     *
     * Identity Scheme Migration Strategies:
     *
     * If you are currently using Email based authentication (either 1 or 2) and
     * want to upgrade to NetID based authentication then there is an easy path.
     * Coordinate with the IdP to provide a Name ID in the SAML assertion. When a
     * user attempts to log in, DSpace will first look for an EPerson with the
     * passed Name ID. When this fails, DSpace will fall back to email based
     * authentication. Then DSpace will update the user's EPerson account record
     * to set their NetID, so all future authentications for this user will be based
     * upon NetID.
     *
     * DSpace will prevent an account from switching NetIDs. If an account already
     * has a NetID set, and a user tries to authenticate with the same email but
     * a different NetID, the authentication will fail.
     *
     * @param context  DSpace context, will be modified (EPerson set) upon success.
     * @param username Not used by SAML-based authentication.
     * @param password Not used by SAML-based authentication.
     * @param realm    Not used by SAML-based authentication.
     * @param request  The HTTP request that started this operation.
     * @return one of: SUCCESS, NO_SUCH_USER, BAD_ARGS
     * @throws SQLException if a database error occurs.
     */
    @Override
    public int authenticate(Context context, String username, String password,
                            String realm, HttpServletRequest request) throws SQLException {

        if (request == null) {
            log.warn("Unable to authenticate using SAML because the request object is null.");

            return BAD_ARGS;
        }

        // Initialize additional EPerson metadata mappings.

        initialize(context);

        String nameId = findSingleAttribute(request, getNameIdAttributeName());

        if (log.isDebugEnabled()) {
            log.debug("Starting SAML Authentication");
            log.debug("Received name ID: " + nameId);
        }

        // Should we auto register new users?

        boolean autoRegister = configurationService.getBooleanProperty("authentication-saml.autoregister", true);

        // Four steps to authenticate a user:

        try {
            // Step 1: Identify user

            EPerson eperson = findEPerson(context, request);

            // Step 2: Register new user, if necessary

            if (eperson == null && autoRegister) {
                eperson = registerNewEPerson(context, request);
            }

            if (eperson == null) {
                return AuthenticationMethod.NO_SUCH_USER;
            }

            if (!eperson.canLogIn()) {
                return AuthenticationMethod.BAD_ARGS;
            }

            // Step 3: Update user's metadata

            updateEPerson(context, request, eperson);

            // Step 4: Log the user in

            context.setCurrentUser(eperson);

            request.setAttribute("saml.authenticated", true);

            AuthenticateServiceFactory.getInstance().getAuthenticationService().initEPerson(context, request, eperson);

            log.info(eperson.getEmail() + " has been authenticated via SAML.");

            return AuthenticationMethod.SUCCESS;
        } catch (Throwable t) {
            // Log the error, and undo the authentication before returning a failure.

            log.error("Unable to successfully authenticate using SAML for user because of an exception.", t);

            context.setCurrentUser(null);

            return AuthenticationMethod.NO_SUCH_USER;
        }
    }

    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) throws SQLException {
        return List.of();
    }

    @Override
    public boolean allowSetPassword(Context context, HttpServletRequest request, String email) throws SQLException {
        // SAML authentication doesn't use a password.

        return false;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }

    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request,
                                   String username) throws SQLException {

        // SAML will auto create accounts if configured to do so, but that is not
        // the same as self register. Self register means that the user can sign up for
        // an account from the web. This is not supported with SAML.

        return false;
    }

    @Override
    public void initEPerson(Context context, HttpServletRequest request,
                            EPerson eperson) throws SQLException {
        // We don't do anything because all our work is done in authenticate.
    }

    /**
     * Returns the URL in the SAML relying party service that initiates a login with the IdP,
     * as configured.
     *
     * @see AuthenticationMethod#loginPageURL(Context, HttpServletRequest, HttpServletResponse)
     */
    @Override
    public String loginPageURL(Context context, HttpServletRequest request, HttpServletResponse response) {
        String samlLoginUrl = configurationService.getProperty("authentication-saml.authenticate-endpoint");

        return response.encodeRedirectURL(samlLoginUrl);
    }

    @Override
    public String getName() {
        return "saml";
    }

    /**
     * Check if the SAML plugin is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public static boolean isEnabled() {
        final String samlPluginName = new SamlAuthentication().getName();
        boolean samlEnabled = false;

        // Loop through all enabled authentication plugins to see if SAML is one of them.

        Iterator<AuthenticationMethod> authenticationMethodIterator =
            AuthenticateServiceFactory.getInstance().getAuthenticationService().authenticationMethodIterator();

        while (authenticationMethodIterator.hasNext()) {
            if (samlPluginName.equals(authenticationMethodIterator.next().getName())) {
                samlEnabled = true;
                break;
            }
        }
        return samlEnabled;
    }

    /**
     * Identify an existing EPerson based upon the SAML attributes provided on
     * the request object.
     *
     * 1) Name ID from SAML attribute (best)
     *    The Name ID-based method is superior because users may change their email
     *    address with the identity provider. When this happens DSpace will not be
     *    able to associate their new address with their old account.
     *
     * 2) Email address from SAML attribute (okay)
     *    In the case where a Name ID header is not available or not found DSpace
     *    will fall back to identifying a user based upon their email address.
     *
     * If successful then the identified EPerson will be returned, otherwise null.
     *
     * @param context The DSpace database context
     * @param request The current HTTP Request
     * @return The EPerson identified or null.
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    protected EPerson findEPerson(Context context, HttpServletRequest request) throws SQLException, AuthorizeException {
        String nameId = findSingleAttribute(request, getNameIdAttributeName());

        if (nameId != null) {
            EPerson ePerson = ePersonService.findByNetid(context, nameId);

            if (ePerson == null) {
                log.info("Unable to identify EPerson by netid (SAML name ID): " + nameId);
            } else {
                log.info("Identified EPerson by netid (SAML name ID): " + nameId);

                return ePerson;
            }
        }

        String emailAttributeName = getEmailAttributeName();
        String email = findSingleAttribute(request, emailAttributeName);

        if (email != null) {
            email = email.toLowerCase();

            EPerson ePerson = ePersonService.findByEmail(context, email);

            if (ePerson == null) {
                log.info("Unable to identify EPerson by email: " + emailAttributeName + "=" + email);
            } else {
                log.info("Identified EPerson by email: " + emailAttributeName + "=" + email);

                if (ePerson.getNetid() == null) {
                    return ePerson;
                }

                // The user has a netid that differs from the received SAML name ID.

                log.error("SAML authentication identified EPerson by email: " + emailAttributeName + "=" + email);
                log.error("Received SAML name ID: " + nameId);
                log.error("EPerson has netid: " + ePerson.getNetid());
                log.error(
                    "The SAML name ID is expected to be the same as the EPerson netid. " +
                    "This might be a hacking attempt to steal another user's credentials. If the " +
                    "user's netid has changed you will need to manually change it to the correct " +
                    "value or unset it in the database.");
            }
        }

        if (nameId == null && email == null) {
            log.error(
                "SAML authentication did not find a name ID or email in the request from which to indentify a user");
        }

        return null;
    }


    /**
     * Register a new EPerson. This method is called when no existing user was
     * found for the NetID or email and autoregister is enabled. When these conditions
     * are met this method will create a new EPerson object.
     *
     * In order to create a new EPerson object there is a minimal set of metadata
     * required: email, first name, and last name. If we don't have access to these
     * three pieces of information then we will be unable to create a new EPerson.
     *
     * Note that this method only adds the minimal metadata. Any additional metadata
     * will need to be added by the updateEPerson method.
     *
     * @param context The current DSpace database context
     * @param request The current HTTP Request
     * @return A new EPerson object or null if unable to create a new EPerson.
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    protected EPerson registerNewEPerson(Context context, HttpServletRequest request)
        throws SQLException, AuthorizeException {

        String nameId = findSingleAttribute(request, getNameIdAttributeName());

        String emailAttributeName = getEmailAttributeName();
        String firstNameAttributeName = getFirstNameAttributeName();
        String lastNameAttributeName = getLastNameAttributeName();

        String email = findSingleAttribute(request, emailAttributeName);
        String firstName = findSingleAttribute(request, firstNameAttributeName);
        String lastName = findSingleAttribute(request, lastNameAttributeName);

        if (email == null || firstName == null || lastName == null) {
            // We require that there be an email, first name, and last name.

            String message = "Unable to register new eperson because we are unable to find an email address, " +
                "first name, and last name for the user.\n";

            message += "  name ID: " + nameId + "\n";
            message += "  email: " + emailAttributeName + "=" + email + "\n";
            message += "  first name: " + firstNameAttributeName + "=" + firstName + "\n";
            message += "  last name: " + lastNameAttributeName + "=" + lastName;

            log.error(message);

            return null;
        }

        try {
            context.turnOffAuthorisationSystem();

            EPerson ePerson = ePersonService.create(context);

            // Set the minimum attributes for the new eperson

            if (nameId != null) {
                ePerson.setNetid(nameId);
            }

            ePerson.setEmail(email.toLowerCase());
            ePerson.setFirstName(context, firstName);
            ePerson.setLastName(context, lastName);
            ePerson.setCanLogIn(true);
            ePerson.setSelfRegistered(true);

            // Commit the new eperson

            AuthenticateServiceFactory.getInstance().getAuthenticationService().initEPerson(context, request, ePerson);

            ePersonService.update(context, ePerson);
            context.dispatchEvents();

            if (log.isInfoEnabled()) {
                String message = "Auto registered new eperson using SAML attributes:\n";

                message += "  netid: " + ePerson.getNetid() + "\n";
                message += "  email: " + ePerson.getEmail() + "\n";
                message += "  firstName: " + ePerson.getFirstName() + "\n";
                message += "  lastName: " + ePerson.getLastName();

                log.info(message);
            }

            return ePerson;
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);

            throw e;
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /**
     * After we successfully authenticated a user, this method will update the user's attributes. The
     * user's email, name, or other attribute may have been changed since the last time they
     * logged into DSpace. This method will update the database with their most recent information.
     *
     * This method handles the basic DSpace metadata (email, first name, last name) along with
     * additional metadata set using the setMetadata() methods on the EPerson object. The
     * additional metadata mappings are defined in configuration.
     *
     * @param context The current DSpace database context
     * @param request The current HTTP Request
     * @param eperson The eperson object to update.
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    protected void updateEPerson(Context context, HttpServletRequest request, EPerson eperson)
        throws SQLException, AuthorizeException {

        String nameId = findSingleAttribute(request, getNameIdAttributeName());

        String emailAttributeName = getEmailAttributeName();
        String firstNameAttributeName = getFirstNameAttributeName();
        String lastNameAttributeName = getLastNameAttributeName();

        String email = findSingleAttribute(request, emailAttributeName);
        String firstName = findSingleAttribute(request, firstNameAttributeName);
        String lastName = findSingleAttribute(request, lastNameAttributeName);

        try {
            context.turnOffAuthorisationSystem();

            // 1) Update the minimum metadata

            // Only update the netid if none has been previously set. This can occur when a repo switches
            // to netid based authentication. The current users do not have netids and fall back to email-based
            // identification but once they login we update their record and lock the account to a particular netid.

            if (nameId != null && eperson.getNetid() == null) {
                eperson.setNetid(nameId);
            }

            // The email could have changed if using netid based lookup.

            if (email != null) {
                eperson.setEmail(email.toLowerCase());
            }

            if (firstName != null) {
                eperson.setFirstName(context, firstName);
            }

            if (lastName != null) {
                eperson.setLastName(context, lastName);
            }

            if (log.isDebugEnabled()) {
                String message = "Updated the eperson's minimal metadata: \n";

                message += " Email: " + emailAttributeName + "=" + email + "' \n";
                message += " First name: " + firstNameAttributeName +  "=" + firstName + "\n";
                message += " Last name: " + lastNameAttributeName + "=" + lastName;

                log.debug(message);
            }

            // 2) Update additional eperson metadata

            for (String attributeName : metadataHeaderMap.keySet()) {
                String metadataFieldName = metadataHeaderMap.get(attributeName);
                String value = findSingleAttribute(request, attributeName);

                // Truncate values

                if (value == null) {
                    log.warn("Unable to update the eperson's '{}' metadata"
                            + " because the attribute '{}' does not exist.", metadataFieldName, attributeName);
                    continue;
                }

                ePersonService.setMetadataSingleValue(context, eperson,
                        MetadataSchemaEnum.EPERSON.getName(), metadataFieldName, null, null, value);

                log.debug("Updated the eperson's {} metadata using attribute: {}={}",
                        metadataFieldName, attributeName, value);
            }

            ePersonService.update(context, eperson);

            context.dispatchEvents();
        } catch (SQLException | AuthorizeException e) {
            log.error(e.getMessage(), e);

            throw e;
        } finally {
            context.restoreAuthSystemState();
        }
    }

    /**
     * Initialize SAML Authentication.
     *
     * During initalization the mapping of additional EPerson metadata will be loaded from the configuration
     * and cached. While loading the metadata mapping this method will check the EPerson object to see
     * if it supports the metadata field. If the field is not supported and autocreate is turned on then
     * the field will be automatically created.
     *
     * It is safe to call this method multiple times.
     *
     * @param context context
     * @throws SQLException if database error
     */
    protected synchronized void initialize(Context context) throws SQLException {
        if (metadataHeaderMap != null) {
            return;
        }

        HashMap<String, String> map = new HashMap<>();

        String[] mappingString = configurationService.getArrayProperty("authentication-saml.eperson.metadata");

        boolean autoCreate = configurationService
            .getBooleanProperty("authentication-saml.eperson.metadata.autocreate", false);

        // Bail out if not set, returning an empty map.

        if (mappingString == null || mappingString.length == 0) {
            log.debug("No additional eperson metadata mapping found: authentication-saml.eperson.metadata");

            metadataHeaderMap = map;
            return;
        }

        log.debug("Loading additional eperson metadata from: authentication-saml.eperson.metadata="
            + StringUtils.join(mappingString, ","));

        for (String metadataString : mappingString) {
            metadataString = metadataString.trim();

            String[] metadataParts = metadataString.split("=>");

            if (metadataParts.length != 2) {
                log.error("Unable to parse metadata mapping string: '" + metadataString + "'");

                continue;
            }

            String attributeName = metadataParts[0].trim();
            String metadataFieldName = metadataParts[1].trim().toLowerCase();

            boolean valid = checkIfEPersonMetadataFieldExists(context, metadataFieldName);

            if (!valid && autoCreate) {
                valid = autoCreateEPersonMetadataField(context, metadataFieldName);
            }

            if (valid) {
                // The eperson field is fine, we can use it.

                log.debug("Loading additional eperson metadata mapping for: {}={}",
                        attributeName, metadataFieldName);

                map.put(attributeName, metadataFieldName);
            } else {
                // The field doesn't exist, and we can't use it.

                log.error("Skipping the additional eperson metadata mapping for: {}={}"
                        + " because the field is not supported by the current configuration.",
                        attributeName, metadataFieldName);
            }
        }

        metadataHeaderMap = map;
    }

    /**
     * Check if a metadata field for an EPerson is available.
     *
     * @param metadataName The name of the metadata field.
     * @param context      context
     * @return True if a valid metadata field, otherwise false.
     * @throws SQLException if database error
     */
    protected synchronized boolean checkIfEPersonMetadataFieldExists(Context context, String metadataName)
        throws SQLException {

        if (metadataName == null) {
            return false;
        }

        MetadataField metadataField = metadataFieldService.findByElement(
            context, MetadataSchemaEnum.EPERSON.getName(), metadataName, null);

        return metadataField != null;
    }

    /**
     * Validate metadata field names
     */
    protected final String FIELD_NAME_REGEX = "^[_A-Za-z0-9]+$";

    /**
     * Automatically create a new metadata field for an EPerson
     *
     * @param context      context
     * @param metadataName The name of the new metadata field.
     * @return True if successful, otherwise false.
     * @throws SQLException if database error
     */
    protected synchronized boolean autoCreateEPersonMetadataField(Context context, String metadataName)
        throws SQLException {

        if (metadataName == null) {
            return false;
        }

        if (!metadataName.matches(FIELD_NAME_REGEX)) {
            return false;
        }

        MetadataSchema epersonSchema = metadataSchemaService.find(context, "eperson");
        MetadataField metadataField = null;

        try {
            context.turnOffAuthorisationSystem();

            metadataField = metadataFieldService.create(context, epersonSchema, metadataName, null, null);
        } catch (AuthorizeException | NonUniqueMetadataException e) {
            log.error(e.getMessage(), e);

            return false;
        } finally {
            context.restoreAuthSystemState();
        }

        return metadataField != null;
    }

    @Override
    public boolean isUsed(final Context context, final HttpServletRequest request) {
        if (request != null &&
            context.getCurrentUser() != null &&
            request.getAttribute("saml.authenticated") != null
        ) {
            return true;
        }

        return false;
    }

    @Override
    public boolean canChangePassword(Context context, EPerson ePerson, String currentPassword) {
        return false;
    }

    private String findSingleAttribute(HttpServletRequest request, String name) {
        if (StringUtils.isBlank(name)) {
            return null;
        }

        Object value = request.getAttribute(name);

        if (value instanceof List) {
            List<?> list = (List<?>) value;

            if (list.size() == 0) {
                value = null;
            } else {
                value = list.get(0);
            }
        }

        return (value == null ? null : value.toString());
    }

    private String getNameIdAttributeName() {
        return configurationService.getProperty("authentication-saml.attribute.name-id", "org.dspace.saml.NAME_ID");
    }

    private String getEmailAttributeName() {
        return configurationService.getProperty("authentication-saml.attribute.email", "org.dspace.saml.EMAIL");
    }

    private String getFirstNameAttributeName() {
        return configurationService.getProperty("authentication-saml.attribute.first-name",
            "org.dspace.saml.GIVEN_NAME");
    }

    private String getLastNameAttributeName() {
        return configurationService.getProperty("authentication-saml.attribute.last-name", "org.dspace.saml.SURNAME");
    }
}
