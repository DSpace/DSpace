/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import static org.dspace.eperson.service.EPersonService.MD_PHONE;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.Logger;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.filter.PresentFilter;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.ldap.support.LdapUtils;


/**
 * This combined LDAP authentication method supersedes both the 'LDAPAuthentication'
 * and the 'LDAPHierarchicalAuthentication' methods. It's capable of both:
 * <ul>
 *   <li>authentication against a flat LDAP tree where all users are in the same unit
 *       (if {@code search.user} or {@code search.password} is not set)</li>
 *   <li>authentication against structured hierarchical LDAP trees of users.</li>
 * </ul>
 * An initial bind is required using a user name and password in order to
 * search the tree and find the DN of the user. A second bind is then required to
 * check the credentials of the user by binding directly to their DN.
 *
 * @author Stuart Lewis
 * @author Chris Yates
 * @author Alex Barbieri
 * @author Flavio Botelho
 * @author Reuben Pasquini
 * @author Samuel Ottenhoff
 * @author Ivan Masár
 * @author Michael Plate
 */
public class LDAPAuthentication implements AuthenticationMethod {

    private static final Logger log
            = org.apache.logging.log4j.LogManager.getLogger(LDAPAuthentication.class);

    protected AuthenticationService authenticationService
            = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    protected EPersonService ePersonService
            = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService
            = EPersonServiceFactory.getInstance().getGroupService();
    protected ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static final String LDAP_AUTHENTICATED = "ldap.authenticated";


    /**
     * Let a real auth method return true if it wants.
     *
     * @throws SQLException if database error
     */
    @Override
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String username)
        throws SQLException {
        // Looks to see if autoregister is set or not
        return configurationService.getBooleanProperty("authentication-ldap.autoregister");
    }

    /**
     * Nothing here, initialization is done when auto-registering.
     *
     * @throws SQLException if database error
     */
    @Override
    public void initEPerson(Context context, HttpServletRequest request,
                            EPerson eperson)
        throws SQLException {
        // XXX should we try to initialize netid based on email addr,
        // XXX  for eperson created by some other method??
    }

    /**
     * Cannot change LDAP password through dspace, right?
     *
     * @throws SQLException if database error
     */
    @Override
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
        throws SQLException {
        // XXX is this right?
        return false;
    }

    /**
     * This is an explicit method.
     */
    @Override
    public boolean isImplicit() {
        return false;
    }

    /**
     * Add authenticated users to the group defined in dspace.cfg by
     * the login.specialgroup key.
     */
    @Override
    public List<Group> getSpecialGroups(Context context, HttpServletRequest request) {
        // Prevents anonymous users from being added to this group, and the second check
        // ensures they are LDAP users
        try {
            // without a logged in user, this method should return an empty list
            if (context.getCurrentUser() == null) {
                return Collections.EMPTY_LIST;
            }
            // if the logged in user does not have a netid, it's not an LDAP user
            // and this method should return an empty list
            if (context.getCurrentUser().getNetid() == null) {
                return Collections.EMPTY_LIST;
            }
            if (!context.getCurrentUser().getNetid().equals("")) {
                String groupName = configurationService.getProperty("authentication-ldap.login.specialgroup");
                if ((groupName != null) && (!groupName.trim().equals(""))) {
                    Group ldapGroup = groupService.findByName(context, groupName);
                    if (ldapGroup == null) {
                        // Oops - the group isn't there.
                        log.warn(LogHelper.getHeader(context,
                                                      "ldap_specialgroup",
                                                      "Group defined in login.specialgroup does not exist"));
                        return Collections.EMPTY_LIST;
                    } else {
                        return Arrays.asList(ldapGroup);
                    }
                }
            }
        } catch (SQLException ex) {
            // The user is not an LDAP user, so we don't need to worry about them
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * Authenticate the given credentials.
     * This is the heart of the authentication method: test the
     * credentials for authenticity, and if accepted, attempt to match
     * (or optionally, create) an <code>EPerson</code>.  If an <code>EPerson</code> is found it is
     * set in the <code>Context</code> that was passed.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param netid
     *  Username (or email address) when method is explicit. Use null for
     *  implicit method.
     *
     * @param password
     *  Password for explicit auth, or null for implicit method.
     *
     * @param realm
     *  Realm is an extra parameter used by some authentication methods, leave null if
     *  not applicable.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @return One of:
     *   SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     * <p>Meaning:
     * <br>SUCCESS         - authenticated OK.
     * <br>BAD_CREDENTIALS - user exists, but credentials (e.g. passwd) don't match
     * <br>CERT_REQUIRED   - not allowed to login this way without a cert.
     * <br>NO_SUCH_USER    - user not found using this method.
     * <br>BAD_ARGS        - user/pw not appropriate for this method
     */
    @Override
    public int authenticate(Context context,
                            String netid,
                            String password,
                            String realm,
                            HttpServletRequest request)
        throws SQLException {
        log.info(LogHelper.getHeader(context, "auth", "attempting trivial auth of user=" + netid));

        // Skip out when no netid or password is given.
        if (netid == null || password == null) {
            return BAD_ARGS;
        }

        // Locate the eperson
        EPerson eperson = null;
        try {
            eperson = ePersonService.findByNetid(context, netid.toLowerCase());
        } catch (SQLException e) {
            // ignore
        }
        SpeakerToLDAP ldap = new SpeakerToLDAP(log);

        // Get the DN of the user
        boolean anonymousSearch = configurationService.getBooleanProperty("authentication-ldap.search.anonymous");
        String adminUser = configurationService.getProperty("authentication-ldap.search.user");
        String adminPassword = configurationService.getProperty("authentication-ldap.search.password");
        String objectContext = configurationService.getProperty("authentication-ldap.object_context");
        String idField = configurationService.getProperty("authentication-ldap.id_field");
        String dn = "";

        if ((StringUtils.isBlank(adminUser) || StringUtils.isBlank(adminPassword)) && !anonymousSearch) {
            try {
                dn = LdapNameBuilder.newInstance(objectContext)
                        .add(idField, netid)
                        .build()
                        .toString();
            } catch (Exception e) {
                log.warn("Failed to build DN for user " + netid, e);
                return BAD_ARGS;
            }
        } else {
            dn = ldap.getDNOfUser(context, netid);
        }

        // Check a DN was found
        if (StringUtils.isBlank(dn)) {
            log.info(LogHelper
                         .getHeader(context, "failed_login", "no DN found for user " + netid));
            return BAD_CREDENTIALS;
        }

        // if they entered a netid that matches an eperson
        if (eperson != null) {
            // e-mail address corresponds to active account
            if (eperson.getRequireCertificate()) {
                return CERT_REQUIRED;
            } else if (!eperson.canLogIn()) {
                return BAD_ARGS;
            }

            if (ldap.ldapAuthenticate(dn, password, context)) {
                context.setCurrentUser(eperson);
                request.setAttribute(LDAP_AUTHENTICATED, true);

                // update eperson's attributes
                context.turnOffAuthorisationSystem();
                setEpersonAttributes(context, eperson, ldap, Optional.empty());
                try {
                    ePersonService.update(context, eperson);
                    context.dispatchEvents();
                } catch (AuthorizeException e) {
                    log.warn("update of eperson " + eperson.getID()  + " failed", e);
                } finally {
                    context.restoreAuthSystemState();
                }

                // assign user to groups based on ldap dn
                assignGroups(dn, ldap.ldapGroup, context);

                log.info(LogHelper
                             .getHeader(context, "authenticate", "type=ldap"));
                return SUCCESS;
            } else {
                return BAD_CREDENTIALS;
            }
        } else {
            // the user does not already exist so try and authenticate them
            // with ldap and create an eperson for them

            if (ldap.ldapAuthenticate(dn, password, context)) {
                // Register the new user automatically
                log.info(LogHelper.getHeader(context,
                                              "autoregister", "netid=" + netid));

                String email = ldap.ldapEmail;

                // Check if we were able to determine an email address from LDAP
                if (StringUtils.isEmpty(email)) {
                    // If no email, check if we have a "netid_email_domain". If so, append it to the netid to create
                    // email
                    if (configurationService.hasProperty("authentication-ldap.netid_email_domain")) {
                        email = netid + configurationService.getProperty("authentication-ldap.netid_email_domain");
                    } else {
                        // We don't have a valid email address. We'll default it to 'netid' but log a warning
                        log.warn(LogHelper.getHeader(context, "autoregister",
                                                      "Unable to locate email address for account '" + netid + "', so" +
                                                          " it has been set to '" + netid + "'. " +
                                                          "Please check the LDAP 'email_field' OR consider " +
                                                          "configuring 'netid_email_domain'."));
                        email = netid;
                    }
                }

                if (StringUtils.isNotEmpty(email)) {
                    try {
                        eperson = ePersonService.findByEmail(context, email);
                        if (eperson != null) {
                            log.info(LogHelper.getHeader(context,
                                                          "type=ldap-login", "type=ldap_but_already_email"));
                            context.turnOffAuthorisationSystem();
                            setEpersonAttributes(context, eperson, ldap, Optional.of(netid));
                            ePersonService.update(context, eperson);
                            context.dispatchEvents();
                            context.restoreAuthSystemState();
                            context.setCurrentUser(eperson);
                            request.setAttribute(LDAP_AUTHENTICATED, true);

                            // assign user to groups based on ldap dn
                            assignGroups(dn, ldap.ldapGroup, context);

                            return SUCCESS;
                        } else {
                            if (canSelfRegister(context, request, netid)) {
                                // TEMPORARILY turn off authorisation
                                try {
                                    context.turnOffAuthorisationSystem();
                                    eperson = ePersonService.create(context);
                                    setEpersonAttributes(context, eperson, ldap, Optional.of(netid));
                                    eperson.setCanLogIn(true);
                                    authenticationService.initEPerson(context, request, eperson);
                                    ePersonService.update(context, eperson);
                                    context.dispatchEvents();
                                    context.setCurrentUser(eperson);
                                    request.setAttribute(LDAP_AUTHENTICATED, true);


                                    // assign user to groups based on ldap dn
                                    assignGroups(dn, ldap.ldapGroup, context);
                                } catch (AuthorizeException e) {
                                    return NO_SUCH_USER;
                                } finally {
                                    context.restoreAuthSystemState();
                                }

                                log.info(LogHelper.getHeader(context, "authenticate",
                                                              "type=ldap-login, created ePerson"));
                                return SUCCESS;
                            } else {
                                // No auto-registration for valid certs
                                log.info(LogHelper.getHeader(context,
                                                              "failed_login", "type=ldap_but_no_record"));
                                return NO_SUCH_USER;
                            }
                        }
                    } catch (AuthorizeException e) {
                        eperson = null;
                    } finally {
                        context.restoreAuthSystemState();
                    }
                }
            }
        }
        return BAD_ARGS;
    }

    /**
     * Update eperson's attributes
     */
    private void setEpersonAttributes(Context context, EPerson eperson, SpeakerToLDAP ldap, Optional<String> netid)
        throws SQLException {

        if (StringUtils.isNotEmpty(ldap.ldapEmail)) {
            eperson.setEmail(ldap.ldapEmail);
        }
        if (StringUtils.isNotEmpty(ldap.ldapGivenName)) {
            eperson.setFirstName(context, ldap.ldapGivenName);
        }
        if (StringUtils.isNotEmpty(ldap.ldapSurname)) {
            eperson.setLastName(context, ldap.ldapSurname);
        }
        if (StringUtils.isNotEmpty(ldap.ldapPhone)) {
            ePersonService.setMetadataSingleValue(context, eperson, MD_PHONE, ldap.ldapPhone, null);
        }
        if (netid.isPresent()) {
            eperson.setNetid(netid.get().toLowerCase());
        }
    }

    /**
     * Internal class to manage LDAP query and results, mainly
     * because there are multiple values to return.
     */
    private static class SpeakerToLDAP {

        private Logger log = null;

        protected String ldapEmail = null;
        protected String ldapGivenName = null;
        protected String ldapSurname = null;
        protected String ldapPhone = null;
        protected ArrayList<String> ldapGroup = null;

        /**
         * LDAP settings
         */
        final String ldap_provider_url;
        final String ldap_id_field;
        final String ldap_search_context;
        final String ldap_search_scope;

        final String ldap_email_field;
        final String ldap_givenname_field;
        final String ldap_surname_field;
        final String ldap_phone_field;
        final String ldap_group_field;
        final boolean useTLS;

        private LdapTemplate ldapTemplate;

        SpeakerToLDAP(Logger thelog) {
            ConfigurationService configurationService
                    = DSpaceServicesFactory.getInstance().getConfigurationService();
            log = thelog;

            ldap_provider_url = configurationService.getProperty("authentication-ldap.provider_url");
            ldap_id_field = configurationService.getProperty("authentication-ldap.id_field");
            ldap_search_context = configurationService.getProperty("authentication-ldap.search_context");
            ldap_search_scope = configurationService.getProperty("authentication-ldap.search_scope");
            ldap_email_field = configurationService.getProperty("authentication-ldap.email_field");
            ldap_givenname_field = configurationService.getProperty("authentication-ldap.givenname_field");
            ldap_surname_field = configurationService.getProperty("authentication-ldap.surname_field");
            ldap_phone_field = configurationService.getProperty("authentication-ldap.phone_field");
            ldap_group_field = configurationService.getProperty("authentication-ldap.login.groupmap.attribute");
            useTLS = configurationService.getBooleanProperty("authentication-ldap.starttls", false);

            setupSpringLdap(configurationService);
        }

        private void setupSpringLdap(ConfigurationService cfg) {
            LdapContextSource contextSource = new LdapContextSource();
            if (StringUtils.isBlank(ldap_provider_url)) {
                throw new IllegalStateException(
                    "LDAP provider URL is empty! Please check 'authentication-ldap.provider_url' in your configuration."
                );
            }
            contextSource.setUrl(ldap_provider_url);

            String adminUser = cfg.getProperty("authentication-ldap.search.user");
            String adminPass = cfg.getProperty("authentication-ldap.search.password");

            if (StringUtils.isNotBlank(adminUser) && StringUtils.isNotBlank(adminPass)) {
                contextSource.setUserDn(adminUser);
                contextSource.setPassword(adminPass);
            } else {
                contextSource.setAnonymousReadOnly(true);
            }

            contextSource.setPooled(true);
            contextSource.afterPropertiesSet();
            this.ldapTemplate = new LdapTemplate(contextSource);
            this.ldapTemplate.setIgnorePartialResultException(true);
        }

        protected String getDNOfUser(Context context, String netid) {
            try {
                EqualsFilter filter = new EqualsFilter(ldap_id_field, netid);

                log.debug("Searching for user using Spring LDAP filter: {}", filter.toString());

                List<String> foundDNs = ldapTemplate.search(
                    LdapQueryBuilder.query().base(ldap_search_context).filter(filter),
                    (ContextMapper<String>) (originalCtx) -> {
                        DirContextOperations ctx = (DirContextOperations) originalCtx;

                        if (ldap_email_field != null) {
                            this.ldapEmail = ctx.getStringAttribute(ldap_email_field);
                        }

                        if (ldap_givenname_field != null) {
                            this.ldapGivenName = ctx.getStringAttribute(ldap_givenname_field);
                        }

                        if (ldap_surname_field != null) {
                            this.ldapSurname = ctx.getStringAttribute(ldap_surname_field);
                        }

                        if (ldap_phone_field != null) {
                            this.ldapPhone = ctx.getStringAttribute(ldap_phone_field);
                        }

                        if (ldap_group_field != null) {
                            String[] groups = ctx.getStringAttributes(ldap_group_field);
                            if (groups != null) {
                                this.ldapGroup = new ArrayList<>(Arrays.asList(groups));
                            }
                        }
                        return ctx.getNameInNamespace();
                    }
                );

                if (foundDNs.isEmpty()) {
                    log.debug(LogHelper.getHeader(context, "getDNOfUser", "no DN found for user " + netid));
                    return null;
                } else if (foundDNs.size() > 1) {
                    log.warn(LogHelper.getHeader(context, "getDNOfUser", "multiple DN found for user " + netid));
                    return null;
                }
                String resultDN = foundDNs.get(0);
                log.debug(LogHelper.getHeader(context, "got DN", resultDN));
                return resultDN;
            } catch (Exception e) {
                log.warn(LogHelper.getHeader(context, "ldap_authentication", "type=failed_search " + e));
                return null;
            }
        }

        /**
         * contact the ldap server and attempt to authenticate
         */
        protected boolean ldapAuthenticate(String dn, String password, Context context) {
            if (StringUtils.isBlank(password)) {
                return false;
            }

            try {
                boolean authenticated = ldapTemplate.authenticate(
                    LdapUtils.newLdapName(dn),
                    new PresentFilter(ldap_id_field).toString(),
                    password
                );
                return authenticated;

            } catch (Exception e) {
                log.warn(LogHelper.getHeader(context, "ldap_authentication", "type=failed_auth " + e));
                return false;
            }
        }
    }


    /**
     * Returns the URL of an external login page which is not applicable for this authn method.
     *
     * Note: Prior to DSpace 7, this method return the page of login servlet.
     *
     * @param context
     *  DSpace context, will be modified (ePerson set) upon success.
     *
     * @param request
     *  The HTTP request that started this operation, or null if not applicable.
     *
     * @param response
     *  The HTTP response from the servlet method.
     *
     * @return fully-qualified URL
     */
    @Override
    public String loginPageURL(Context context,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        return null;
    }

    @Override
    public String getName() {
        return "ldap";
    }

    /**
     * Add authenticated users to the group defined in dspace.cfg by
     * the authentication-ldap.login.groupmap.* key.
     *
     * @param dn
     *  The string containing distinguished name of the user
     *
     * @param group
     *  List of strings with LDAP dn of groups
     *
     * @param context
     *  DSpace context
     */
    private void assignGroups(String dn, ArrayList<String> group, Context context) {
        if (StringUtils.isNotBlank(dn)) {
            System.out.println("dn:" + dn);
            int groupmapIndex = 1;
            String groupMap = configurationService.getProperty("authentication-ldap.login.groupmap." + groupmapIndex);
            boolean cmp;


            // groupmap contains the mapping of LDAP groups to DSpace groups
            // outer loop with the DSpace groups
            while (groupMap != null) {
                String[] t = groupMap.split(":");
                String ldapSearchString = t[0];
                String dspaceGroupName = t[1];

                if (group == null) {
                    cmp = Strings.CI.contains(dn, ldapSearchString + ",");

                    if (cmp) {
                        assignGroup(context, groupmapIndex, dspaceGroupName);
                    }
                } else {
                    // list of strings with dn from LDAP groups
                    // inner loop
                    Iterator<String> groupIterator = group.iterator();
                    while (groupIterator.hasNext())  {

                        // save the current entry from iterator for further use
                        String currentGroup = groupIterator.next();

                        // very much the old code from DSpace <= 7.5
                        if (currentGroup == null) {
                            cmp = Strings.CI.contains(dn, ldapSearchString + ",");
                        } else {
                            cmp = Strings.CI.equals(currentGroup, ldapSearchString);
                        }

                        if (cmp) {
                            assignGroup(context, groupmapIndex, dspaceGroupName);
                        }
                    }
                }

                groupMap = configurationService.getProperty("authentication-ldap.login.groupmap." + ++groupmapIndex);
            }
        }
    }

    /**
     * Add the current authenticated user to the specified group
     *
     * @param context
     *  DSpace context
     *
     * @param groupmapIndex
     *  authentication-ldap.login.groupmap.* key index defined in dspace.cfg
     *
     * @param dspaceGroupName
     *  The DSpace group to add the user to
     */
    private void assignGroup(Context context, int groupmapIndex, String dspaceGroupName) {
        try {
            Group ldapGroup = groupService.findByName(context, dspaceGroupName);
            if (ldapGroup != null) {
                groupService.addMember(context, ldapGroup, context.getCurrentUser());
                groupService.update(context, ldapGroup);
            } else {
                // The group does not exist
                log.warn(LogHelper.getHeader(context,
                        "ldap_assignGroupsBasedOnLdapDn",
                        "Group defined in authentication-ldap.login.groupmap." + groupmapIndex
                        + " does not exist :: " + dspaceGroupName));
            }
        } catch (AuthorizeException ae) {
            log.debug(LogHelper.getHeader(context,
                    "assignGroupsBasedOnLdapDn could not authorize addition to " +
                            "group",
                            dspaceGroupName));
        } catch (SQLException e) {
            log.debug(LogHelper.getHeader(context, "assignGroupsBasedOnLdapDn could not find group",
                    dspaceGroupName));
        }
    }

    @Override
    public boolean isUsed(final Context context, final HttpServletRequest request) {
        if (request != null &&
                context.getCurrentUser() != null &&
                request.getAttribute(LDAP_AUTHENTICATED) != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean canChangePassword(Context context, EPerson ePerson, String currentPassword) {
        return false;
    }
}
