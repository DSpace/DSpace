package org.dspace.authenticate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;

import edu.umd.lib.dspace.authenticate.Ldap;
import edu.umd.lims.util.ErrorHandling;
import edu.yale.its.tp.cas.client.ProxyTicketValidator;
// we use the Java CAS client
import edu.yale.its.tp.cas.client.ServiceTicketValidator;

/**
 * Authenticator for Central Authentication Service (CAS).
 *
 * @author Naveed Hashmi, University of Bristol based on code developed by
 *         Nordija A/S (www.nordija.com) for Center of Knowledge Technology
 *         (www.cvt.dk)
 * @version $Revision: 1.0 $
 */

public class CASAuthentication implements AuthenticationMethod
{

    /** log4j category */
    private static Logger log = Logger.getLogger(CASAuthentication.class);

    private static String casServicevalidate; // URL to validate ST tickets

    private static String casProxyvalidate; // URL to validate PT tickets

    // (optional) store user's details for self registration, can get this info
    // from LDAP, RDBMS etc
    private String email;

    private String firstName;

    private String lastName;

    public final static String CASUSER = "dspace.current.user.ldap";

    /**
     * Predicate, can new user automatically create EPerson. Checks
     * configuration value. You'll probably want this to be true to take
     * advantage of a Web certificate infrastructure with many more users than
     * are already known by DSpace.
     */
    @Override
    public boolean canSelfRegister(Context context, HttpServletRequest request,
            String username) throws SQLException
    {
        return ConfigurationManager
                .getBooleanProperty("webui.cas.autoregister");
    }

    /**
     * Nothing extra to initialize.
     */
    @Override
    public void initEPerson(Context context, HttpServletRequest request,
            EPerson eperson) throws SQLException
    {
    }

    /**
     * We don't use EPerson password so there is no reason to change it.
     */
    @Override
    public boolean allowSetPassword(Context context,
            HttpServletRequest request, String username) throws SQLException
    {
        return false;
    }

    /**
     * Returns true, CAS is an implicit method
     */
    @Override
    public boolean isImplicit()
    {
        return true;
    }

    /**
     * Groups mapped from Ldap Units
     */
    @Override
    public int[] getSpecialGroups(Context context, HttpServletRequest request)
    {
        try
        {
            Ldap ldap = new Ldap(context);
            if (context.getCurrentUser() != null)
            {
                ldap.checkUid(context.getCurrentUser().getNetid());
            }
            if (ldap != null)
            {
                ldap.setContext(context);
                int ldap_groups[] = ldap.getGroupsInt();

                Group CASGroup = Group.findByName(context, "CAS Authenticated");
                if (CASGroup == null)
                {
                    throw new Exception(
                            "Unable to find 'CAS Authenticated' group");
                }

                int groups[] = Arrays.copyOf(ldap_groups,
                        ldap_groups.length + 1);
                groups[groups.length - 1] = CASGroup.getID();

                return groups;
            }
        }
        catch (Exception e)
        {
            log.error("Ldap exception: " + ErrorHandling.getStackTrace(e));
        }

        return new int[0];
    }

    /**
     * CAS authentication.
     *
     * @return One of: SUCCESS, BAD_CREDENTIALS, NO_SUCH_USER, BAD_ARGS
     */
    @Override
    public int authenticate(Context context, String netid, String password,
            String realm, HttpServletRequest request) throws SQLException
    {
        final String ticket = request.getParameter("ticket");
        final String service = request.getRequestURL().toString();
        log.info(LogManager.getHeader(context, "login", " ticket: " + ticket));
        log.info(LogManager.getHeader(context, "login", "service: " + service));

        // administrator override, force login as a CAS user
        if (netid != null && password != null)
        {
            Ldap ldap = null;

            try
            {
                ldap = new Ldap(context);

                if (ldap.checkAdmin(password) && ldap.checkUid(netid))
                {

                    EPerson eperson = EPerson.findByNetid(context,
                            netid.toLowerCase());

                    if (eperson != null)
                    {

                        // Save the ldap object in the session
                        request.getSession().setAttribute(CASUSER, ldap);

                        log.debug(LogManager.getHeader(
                                context,
                                "authenticate",
                                CASUSER
                                        + "="
                                        + request.getSession().getAttribute(
                                                CASUSER)));

                        // Logged in OK.
                        context.setCurrentUser(eperson);
                        log.info(LogManager.getHeader(context, "authenticate",
                                "type=CAS (admin override)"));
                        return SUCCESS;

                    }
                }
            }
            catch (Exception ex)
            {
                log.error("Error checking admin override: "
                        + ErrorHandling.getStackTrace(ex));
            }
            finally
            {
                if (ldap != null)
                    ldap.close();
            }

        }

        // CAS ticket
        if (ticket != null)
        {
            try
            {
                // Determine CAS validation URL
                String validate = ConfigurationManager
                        .getProperty("cas.validate.url");
                log.info(LogManager.getHeader(context, "login",
                        "CAS validate:  " + validate));
                if (validate == null)
                {
                    throw new ServletException(
                            "No CAS validation URL specified. You need to set property 'cas.validate.url'");
                }

                // Validate ticket (it is assumed that CAS validator returns the
                // user network ID)
                netid = validate(service, ticket, validate);
                if (netid == null)
                {
                    throw new ServletException("Ticket '" + ticket
                            + "' is not valid");
                }

                // Check directory
                Ldap ldap = new Ldap(context);
                if (ldap.checkUid(netid))
                {
                    ldap.close();
                }
                else
                {
                    throw new ServletException("Unknown directory id " + netid);
                }

                // Save the ldap object in the session
                request.getSession().setAttribute(CASUSER, ldap);

                log.debug(LogManager.getHeader(context, "authenticate", CASUSER
                        + "=" + request.getSession().getAttribute(CASUSER)));

                // Locate the eperson in DSpace
                EPerson eperson = null;
                try
                {
                    eperson = EPerson.findByNetid(context, netid.toLowerCase());
                }
                catch (SQLException e)
                {
                }
                // if they entered a netd that matches an eperson and they are
                // allowed to login
                if (eperson != null)
                {
                    // e-mail address corresponds to active account
                    if (eperson.getRequireCertificate())
                    {
                        // they must use a certificate
                        return CERT_REQUIRED;
                    }
                    else if (!eperson.canLogIn())
                        return BAD_ARGS;

                    // Logged in OK.

                    context.setCurrentUser(eperson);
                    log.info(LogManager.getHeader(context, "authenticate",
                            "type=CAS"));
                    return SUCCESS;
                }

                // the user does not exist in DSpace so create an eperson
                else
                {
                    if (canSelfRegister(context, request, netid))
                    {
                        eperson = ldap.registerEPerson(netid);

                        context.setIgnoreAuthorization(false);
                        context.setCurrentUser(eperson);
                        return SUCCESS;
                    }
                    else
                    {
                        // No auto-registration for valid netid
                        log.warn(LogManager
                                .getHeader(context, "authenticate",
                                        "type=netid_but_no_record, cannot auto-register"));
                        return NO_SUCH_USER;
                    }
                }

            }
            catch (Exception e)
            {
                log.error("Unexpected exception caught", e);
                // throw new ServletException(e);
            }
        }
        return BAD_ARGS;
    }

    /**
     * Returns the NetID of the owner of the given ticket, or null if the ticket
     * isn't valid.
     *
     * @param service
     *            the service ID for the application validating the ticket
     * @param ticket
     *            the opaque service ticket (ST) to validate
     */
    public static String validate(String service, String ticket,
            String validateURL) throws IOException, ServletException
    {

        ServiceTicketValidator stv = null;
        String validateUrl = null;

        if (ticket.startsWith("ST"))
        {
            stv = new ServiceTicketValidator();
            validateUrl = casServicevalidate;
        }
        else
        {
            // uPortal uses this
            stv = new ProxyTicketValidator();
            validateUrl = casProxyvalidate;
        }

        stv.setCasValidateUrl(validateURL);
        stv.setService(java.net.URLEncoder.encode(service));
        stv.setServiceTicket(ticket);

        try
        {
            stv.validate();
        }
        catch (Exception e)
        {
            log.error("Unexpected exception caught", e);
            throw new ServletException(e);
        }

        if (!stv.isAuthenticationSuccesful())
            return null;
        String netid = stv.getUser();

        return netid;

    }

    /**
     * Add code here to extract user details email, firstname, lastname from
     * LDAP or database etc
     */

    public void registerUser(String netid) throws ClassNotFoundException,
            SQLException
    {
        // add your code here
    }

    /*
     * Returns URL to which to redirect to obtain credentials (either password
     * prompt or e.g. HTTPS port for client cert.); null means no redirect.
     * 
     * @param context DSpace context, will be modified (ePerson set) upon
     * success.
     * 
     * @param request The HTTP request that started this operation, or null if
     * not applicable.
     * 
     * @param response The HTTP response from the servlet method.
     * 
     * @return fully-qualified URL
     */
    @Override
    public String loginPageURL(Context context, HttpServletRequest request,
            HttpServletResponse response)
    {
        // Determine CAS server URL
        final String authServer = ConfigurationManager
                .getProperty("cas.server.url");
        final String origUrl = (String) request.getSession().getAttribute(
                "interrupted.request.url");
        // final String service = (origUrl != null ? origUrl : request
        // .getRequestURL().toString());
        final String service = (origUrl != null ? origUrl : request
                .getRequestURL().toString()).replace("login", "cas-login");
        log.info("CAS server:  " + authServer);

        // Redirect to CAS server
        return response.encodeRedirectURL(authServer + "?service=" + service);
    }

    /*
     * Returns message key for title of the "login" page, to use in a menu
     * showing the choice of multiple login methods.
     * 
     * @param context DSpace context, will be modified (ePerson set) upon
     * success.
     * 
     * @return Message key to look up in i18n message catalog.
     */
    @Override
    public String loginPageTitle(Context context)
    {
        // return null;
        return "org.dspace.eperson.CASAuthentication.title";
    }

}
