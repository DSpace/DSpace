/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.authenticate;

import no.ntnu.it.fw.saml2api.EduPerson;
import no.ntnu.it.fw.saml2api.http.Common;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

/**
 * A stackable authentication method
 * based on the DSpace internal "EPerson" database.
 * See the <code>AuthenticationMethod</code> interface for more details.
 * <p>
 * The <em>username</em> is the E-Person's email address,
 * and and the <em>password</em> (given to the <code>authenticate()</code>
 * method) must match the EPerson password.
 * <p>
 * This is the default method for a new DSpace configuration.
 * If you are implementing a new "explicit" authentication method,
 * use this class as a model.
 * <p>
 * You can use this (or another explicit) method in the stack to
 * implement HTTP Basic Authentication for servlets, by passing the
 * Basic Auth username and password to the <code>AuthenticationManager</code>.
 *
 * @author Larry Stone
 * @version $Revision$
 */
public class FeideAuthentication implements AuthenticationMethod {

    private static final String BIBSYS_REALM = "@bibsys.no";
    private static final String UNIT_REALM = "@unit.no";
    private static final String BRAGE_BIBSYS_NO = "brage@bibsys.no";
    private static final String BRAGE_UNIT_NO = "brage@unit.no";
    /**
     * log4j category
     */
    private static Logger log = Logger.getLogger(FeideAuthentication.class);
    /**
     * The IP address this user first logged in from, do not allow this session
     * for other IP addresses.
     */
    private static final String CURRENT_IP_ADDRESS = "dspace.user.ip";
    /**
     * The effective user id, typically this will never change. However, if an
     * administrator has assumed login as this user then they will differ.
     */
    private static final String EFFECTIVE_USER_ID = "dspace.user.effective";
    private static final String AUTHENTICATED_USER_ID = "dspace.user.authenticated";
    private static final String NO_BRAGE_INSTITUTE = "xmlui.Eperson.FailedAuthentication.noBrageInstitution";
    /** User is not at an appropriate brage institution. */
    public static final int BAD_INST = 6;

    protected AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    protected EPersonService ePersonService = EPersonServiceFactory.getInstance().getEPersonService();
    protected GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    /**
     * Look to see if this email address is allowed to register.
     * <p>
     * The configuration key domain.valid is examined
     * in authentication-password.cfg to see what domains are valid.
     * <p>
     * Example - aber.ac.uk domain : @aber.ac.uk
     * Example - MIT domain and all .ac.uk domains: @mit.edu, .ac.uk
     */
    public boolean canSelfRegister(Context context,
                                   HttpServletRequest request,
                                   String email)
            throws SQLException {

        HttpSession session = request.getSession();
        EduPerson eduPerson = Common.getEduPerson(session);

        String allowedOrgNrs = ConfigurationManager.getProperty("dspace.orgnr");

        List<String> allowedOrgNrsList = Arrays.asList(allowedOrgNrs.split(","));

        boolean isUNITFeide = StringUtils.endsWith(eduPerson.getPrincipalName().trim(), UNIT_REALM);
        boolean isBIBSYSFeide = StringUtils.endsWith(eduPerson.getPrincipalName().trim(), BIBSYS_REALM);

        if(isUNITFeide || isSystemAdmin(eduPerson)) {

            return true;

        } else {

            boolean isAllowedOrgNr;
            boolean isMember = false;

            List<String> affiliations;

            if (isBIBSYSFeide) {
                affiliations = eduPerson.getEduPersonScopedAffiliation();

                // BIBSYS hosted institution OrgNr is found in attribute edupersonorgunitdn:noreduorgunituniqueidentifier
                List<String> orgNrs = eduPerson.getAttributesMap().get("noreduorgunituniqueidentifier");
                if (orgNrs == null || orgNrs.isEmpty()) {
                    isAllowedOrgNr = false;
                } else {
                    isAllowedOrgNr = allowedOrgNrsList.contains(orgNrs.get(0));
                }
            } else {
                affiliations = eduPerson.getAffiliation();

                // "Normal" OrgNr found in attribute edupersonorgdn:noreduorgnin
                isAllowedOrgNr = allowedOrgNrsList.contains(eduPerson.getNorEduOrgNIN());
            }

            if (isAllowedOrgNr && affiliations != null) {
                for (String affiliation : affiliations) {
                    if (StringUtils.startsWith(affiliation.toLowerCase(), "member")) {
                        isMember = true;
                        break;
                    }
                }
            }

            return isMember;
        }
    }

    private boolean isSystemAdmin(EduPerson eduPerson) {
        return BRAGE_BIBSYS_NO.equals(eduPerson.getPrincipalName()) || BRAGE_UNIT_NO.equals(eduPerson.getPrincipalName());
    }

    /**
     * Nothing extra to initialize.
     */
    public void initEPerson(Context context, HttpServletRequest request,
                            EPerson eperson)
            throws SQLException {
    }

    /**
     * We always allow the user to change their password.
     */
    public boolean allowSetPassword(Context context,
                                    HttpServletRequest request,
                                    String username)
            throws SQLException {
        return true;
    }

    /**
     * This is an explicit method, since it needs username and password
     * from some source.
     *
     * @return false
     */
    public boolean isImplicit() {
        return false;
    }

    /**
     * Add authenticated users to the group defined in authentication-password.cfg by
     * the login.specialgroup key.
     */
    public  List<Group> getSpecialGroups(Context context, HttpServletRequest request) {
        return ListUtils.EMPTY_LIST;
    }

    /**
     * Check credentials: username must match the email address of an
     * EPerson record, and that EPerson must be allowed to login.
     * Password must match its password.  Also checks for EPerson that
     * is only allowed to login via an implicit method
     * and returns <code>CERT_REQUIRED</code> if that is the case.
     *
     * @param context  DSpace context, will be modified (EPerson set) upon success.
     * @param username Username (or email address) when method is explicit. Use null for
     *                 implicit method.
     * @param password Password for explicit auth, or null for implicit method.
     * @param realm    Realm is an extra parameter used by some authentication methods, leave null if
     *                 not applicable.
     * @param request  The HTTP request that started this operation, or null if not applicable.
     * @return One of:
     * SUCCESS, BAD_CREDENTIALS, CERT_REQUIRED, NO_SUCH_USER, BAD_ARGS
     * <p>Meaning:
     * <br>SUCCESS         - authenticated OK.
     * <br>BAD_CREDENTIALS - user exists, but assword doesn't match
     * <br>CERT_REQUIRED   - not allowed to login this way without X.509 cert.
     * <br>NO_SUCH_USER    - no EPerson with matching email address.
     * <br>BAD_ARGS        - missing username, or user matched but cannot login.
     */
    public int authenticate(Context context,
                            String username,
                            String password,
                            String realm,
                            HttpServletRequest request)
            throws SQLException {
        HttpSession session = request.getSession();
        EduPerson eduPerson = Common.getEduPerson(session);
        log.info("eduPerson: " + eduPerson);
        if (eduPerson != null) {
            EPerson eperson = null;
            String netid = eduPerson.getPrincipalName();
            log.info(LogManager.getHeader(context, "authenticate", "attempting feide auth of user=" + eduPerson.dump()));
            try {
                eperson = ePersonService.findByNetid(context, netid);

                log.debug("findByNetid eperson: " + (eperson != null ? eperson.getEmail() : "null"));
            } catch (SQLException e) {
                log.trace("Failed to authorize looking up EPerson", e);
            }
            if (eperson == null) {
                if ((eduPerson.getEmail() != null) && (!eduPerson.getEmail().equals(""))) {
                    try {
                        eperson = ePersonService.findByEmail(context, eduPerson.getEmail());

                        log.debug("findByEmail eperson: " + (eperson != null ? eperson.getEmail() : "null"));
                        //Det eksisterer en eperson med epostadresse
                        if (eperson != null) {
                            this.finalizeEperson(context, request, eperson);
                            return SUCCESS;
                        }
                        // Hvis eperson ikke eksisterer så lager vi en.
                        if (canSelfRegister(context, request, netid)) {
                            try {
                                eperson = this.autoRegister(context, request, eduPerson);
                                this.finalizeEperson(context, request, eperson);
                            } catch (AuthorizeException e) {
                                log.debug("Error while autoRegistering", e);
                                return NO_SUCH_USER;
                            } finally {
                                //Do not close or abort context here
                                context.restoreAuthSystemState();
                            }
                            session.setAttribute("UserName", netid); //So Tomcat can "guess" the username in the manager-app
                            log.info("User " + netid + " successfully logged in");
                            return SUCCESS;
                        }
                    } catch (SQLException e) {
                        log.debug("Error while authorizing", e);
                        return NO_SUCH_USER;
                    }
                }
                // lookup failed.
                return NO_SUCH_USER;
            } else if (!eperson.canLogIn()) {
                // cannot login this way
                return BAD_ARGS;
            } else if (eperson.canLogIn()) {
                this.finalizeEperson(context, request, eperson);
                return SUCCESS;
            } else {
                return BAD_CREDENTIALS;
            }
        }
        // BAD_ARGS always defers to the next authentication method.
        // It means this method cannot use the given credentials.
        else {
            return BAD_ARGS;
        }
    }

    private EPerson autoRegister(Context context, HttpServletRequest request, EduPerson eduPerson) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        EPerson eperson = ePersonService.create(context);
        eperson.setEmail(eduPerson.getEmail());
        eperson.setFirstName(context, eduPerson.getGivenName());
        eperson.setLastName(context, eduPerson.getLastname());
        String fullName = eduPerson.getFullname();
        if (fullName != null && !fullName.trim().isEmpty()) {
            int index = fullName.indexOf(" ");
            if (eperson.getFirstName() == null || eperson.getFirstName().trim().isEmpty()) {
                eperson.setFirstName(context, fullName.substring(0, index != -1 ? index : fullName.length()));
            }
            if (eperson.getLastName() == null || eperson.getLastName().trim().isEmpty() && index != -1) {
                eperson.setLastName(context, fullName.substring(index, fullName.length()));
            }
        }
        eperson.setNetid(eduPerson.getPrincipalName());

        eperson.setSelfRegistered(true);
        eperson.setCanLogIn(true);
        eperson.setLanguage(context,"no");
        log.debug("before initEPerson");

        authenticationService.initEPerson(context, request, eperson);

        ePersonService.update(context, eperson);

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);
        if (anonymousGroup == null) {
            throw new IllegalStateException("Error, no Anonymous group found");
        }
        groupService.addMember(context, anonymousGroup, eperson);
        groupService.update(context, anonymousGroup);

        context.commit();
        context.restoreAuthSystemState();
        log.debug("after initEPerson");
        return eperson;
    }

    private void finalizeEperson(Context context, HttpServletRequest request, EPerson eperson) {
        // login is ok if password matches:
        context.setCurrentUser(eperson);
        // add the remote IP address to compare against later requests
        // so we can detect session hijacking.
        request.getSession().setAttribute(CURRENT_IP_ADDRESS, request.getRemoteAddr());
        // Set both the effective and authenticated user to the same.
        request.getSession().setAttribute(EFFECTIVE_USER_ID, context.getCurrentUser().getID());
        request.getSession().setAttribute(AUTHENTICATED_USER_ID, context.getCurrentUser().getID());
        log.info(LogManager.getHeader(context, "authenticate", "type=FeideAuthentication"));
    }

    /**
     * Returns URL of password-login servlet.
     *
     * @param context  DSpace context, will be modified (EPerson set) upon success.
     * @param request  The HTTP request that started this operation, or null if not applicable.
     * @param response The HTTP response from the servlet method.
     * @return fully-qualified URL
     */
    public String loginPageURL(Context context,
                               HttpServletRequest request,
                               HttpServletResponse response) {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(getBasePath(request));
        urlBuilder.append(request.getContextPath());
        urlBuilder.append("/Endpoint/AssertionConsumerService/POST/Response?RelayState=" + request.getContextPath() + "/feide-login");

        //relayState: the url to which the feide logic should redirect to after a successful login
        //This is the URL the user initially was trying to access

        return urlBuilder.toString();
    }

    private String getBasePath(HttpServletRequest request) {
        StringBuilder basePath = new StringBuilder();

        basePath.append(request.getScheme());
        basePath.append("://");
        basePath.append(request.getServerName());
        if ("http".equals(request.getScheme()) && request.getServerPort() != 80 ||
                "https".equals(request.getScheme()) && request.getServerPort() != 443) {
            basePath.append(":").append(request.getServerPort());
        }
        return basePath.toString();
    }

    /**
     * Returns message key for title of the "login" page, to use
     * in a menu showing the choice of multiple login methods.
     *
     * @param context DSpace context, will be modified (EPerson set) upon success.
     * @return Message key to look up in i18n message catalog.
     */
    public String loginPageTitle(Context context) {
        return "org.dspace.eperson.PasswordAuthentication.title";
    }
}
