/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import com.sun.mail.smtp.SMTPAddressFailedException;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authenticate.AuthenticationManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.*;
import org.dspace.eperson.AccountManager;
import org.dspace.eperson.EPerson;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Hashtable;

/**
 * Servlet for handling user registration and forgotten passwords.
 * <P>
 * This servlet handles both forgotten passwords and initial registration of
 * users. Which it handles depends on the initialisation parameter "register" -
 * if it's "true", it is treated as an initial registration and the user is
 * asked to input their personal information.
 * <P>
 * The sequence of events is this: The user clicks on "register" or "I forgot my
 * password." This servlet then displays the relevant "enter your e-mail" form.
 * An e-mail address is POSTed back, and if this is valid, a token is created
 * and e-mailed, otherwise an error is displayed, with another "enter your
 * e-mail" form.
 * <P>
 * When the user clicks on the token URL mailed to them, this servlet receives a
 * GET with the token as the parameter "KEY". If this is a valid token, the
 * servlet then displays the "edit profile" or "edit password" screen as
 * appropriate.
 */
public class RegisterServlet extends DSpaceServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(RegisterServlet.class);

    /** The "enter e-mail" step */
    public static final int ENTER_EMAIL_PAGE = 1;

    /** The "enter personal info" page, for a registering user */
    public static final int PERSONAL_INFO_PAGE = 2;

    /** The simple "enter new password" page, for user who's forgotten p/w */
    public static final int NEW_PASSWORD_PAGE = 3;

    /** true = registering users, false = forgotten passwords */
    private boolean registering;

    /** ldap is enabled */
    private boolean ldap_enabled;

    public void init()
    {
        registering = getInitParameter("register").equalsIgnoreCase("true");
        ldap_enabled = ConfigurationManager.getBooleanProperty("authentication-ldap", "enable");
    }

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        /*
         * Respond to GETs. A simple GET with no parameters will display the
         * relevant "type in your e-mail" form. A GET with a "token" parameter
         * will go to the "enter personal info" or "enter new password" page as
         * appropriate.
         */

        // Get the token
        String token = request.getParameter("token");

        if (token == null)
        {
            // Simple "enter your e-mail" page
            if (registering)
            {
                // Registering a new user
                if (ldap_enabled)
                {
                    JSPManager.showJSP(request, response, "/register/new-ldap-user.jsp");
                }
                JSPManager.showJSP(request, response, "/register/new-user.jsp");
            }
            else
            {
                // User forgot their password
                JSPManager.showJSP(request, response,
                        "/register/forgot-password.jsp");
            }
        }
        else
        {
            // We have a token. Find out who the it's for
            String email = AccountManager.getEmail(context, token);

            EPerson eperson = null;

            if (email != null)
            {
                eperson = EPerson.findByEmail(context, email);
            }

            // Both forms need an EPerson object (if any)
            request.setAttribute("eperson", eperson);

            // And the token
            request.setAttribute("token", token);

            if (registering && (email != null))
            {
                // Indicate if user can set password
                boolean setPassword =
                    AuthenticationManager.allowSetPassword(context, request, email);
                request.setAttribute("set.password", Boolean.valueOf(setPassword));

                // Forward to "personal info page"
                JSPManager.showJSP(request, response,
                        "/register/registration-form.jsp");
            }
            else if (!registering && (eperson != null))
            {
                // Token relates to user who's forgotten password
                JSPManager.showJSP(request, response,
                        "/register/new-password.jsp");
            }
            else
            {
                // Duff token!
                JSPManager.showJSP(request, response,
                        "/register/invalid-token.jsp");

                return;
            }
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        /*
         * POSTs are the result of entering an e-mail in the "forgot my
         * password" or "new user" forms, or the "enter profile information" or
         * "enter new password" forms.
         */

        // First get the step
        int step = UIUtil.getIntParameter(request, "step");

        switch (step)
        {
        case ENTER_EMAIL_PAGE:
            processEnterEmail(context, request, response);

            break;

        case PERSONAL_INFO_PAGE:
            processPersonalInfo(context, request, response);

            break;

        case NEW_PASSWORD_PAGE:
            processNewPassword(context, request, response);

            break;

        default:
            log.warn(LogManager.getHeader(context, "integrity_error", UIUtil
                    .getRequestLogInfo(request)));
            JSPManager.showIntegrityError(request, response);
        }
    }

    /**
     * Process information from the "enter e-mail" page. If the e-mail
     * corresponds to a valid user of the system, a token is generated and sent
     * to that user.
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processEnterEmail(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String email = request.getParameter("email");
        if (email == null || email.length() > 64)
        {
        	// Malformed request or entered value is too long.
        	email = "";
        }
        else
        {
        	email = email.toLowerCase().trim();
        }
        
        String netid = request.getParameter("netid");
        String password = request.getParameter("password");
        EPerson eperson = EPerson.findByEmail(context, email);
        EPerson eperson2 = null;
        if (netid!=null)
        {
            eperson2 = EPerson.findByNetid(context, netid.toLowerCase());
        }

        try
        {
            if (registering)
            {
                // If an already-active user is trying to register, inform them so
                if ((eperson != null && eperson.canLogIn()) || (eperson2 != null && eperson2.canLogIn()))
                {
                    log.info(LogManager.getHeader(context,
                            "already_registered", "email=" + email));

                    JSPManager.showJSP(request, response,
                            "/register/already-registered.jsp");
                }
                else
                {
                    // Find out from site authenticator whether this email can
                    // self-register
                    boolean canRegister =
                        AuthenticationManager.canSelfRegister(context, request, email);

                    if (canRegister)
                    {
                        //-- registering by email
                        if ((!ldap_enabled)||(netid==null)||(netid.trim().equals("")))
                        {
                            // OK to register.  Send token.
                            log.info(LogManager.getHeader(context,
                                "sendtoken_register", "email=" + email));

                            try
                            {
                                AccountManager.sendRegistrationInfo(context, email);
                            }
                            catch (javax.mail.SendFailedException e)
                            {
                            	if (e.getNextException() instanceof SMTPAddressFailedException)
                            	{
                                    // If we reach here, the email is email is invalid for the SMTP server (i.e. fbotelho).
                                    log.info(LogManager.getHeader(context,
                                        "invalid_email",
                                        "email=" + email));
                                    request.setAttribute("retry", Boolean.TRUE);
                                    JSPManager.showJSP(request, response, "/register/new-user.jsp");
                                    return;
                            	}
                            	else
                            	{
                            		throw e;
                            	}
                            }
                            JSPManager.showJSP(request, response,
                                "/register/registration-sent.jsp");

                            // Context needs completing to write registration data
                            context.complete();
                        }
                        //-- registering by netid
                        else 
                        {
                            //--------- START LDAP AUTH SECTION -------------
                            if (password!=null && !password.equals("")) 
                            {
                                String ldap_provider_url = ConfigurationManager.getProperty("authentication-ldap", "provider_url");
                                String ldap_id_field = ConfigurationManager.getProperty("authentication-ldap", "id_field");
                                String ldap_search_context = ConfigurationManager.getProperty("authentication-ldap", "search_context");
                           
                                // Set up environment for creating initial context
                                Hashtable env = new Hashtable(11);
                                env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                                env.put(javax.naming.Context.PROVIDER_URL, ldap_provider_url);
                        
                                // Authenticate 
                                env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
                                env.put(javax.naming.Context.SECURITY_PRINCIPAL, ldap_id_field+"="+netid+","+ldap_search_context);
                                env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);
                        
                                try {
                                   // Create initial context
                                   DirContext ctx = new InitialDirContext(env);
             
                                   // Close the context when we're done
                                   ctx.close();
                                } 
                                catch (NamingException e) 
                                {
                                    // If we reach here, supplied email/password was duff.
                                    log.info(LogManager.getHeader(context,
                                        "failed_login",
                                        "netid=" + netid + e));
                                    JSPManager.showJSP(request, response, "/login/ldap-incorrect.jsp");
                                    return;
                                }
                            }
                            //--------- END LDAP AUTH SECTION -------------
                            // Forward to "personal info page"
                            JSPManager.showJSP(request, response, "/register/registration-form.jsp");
                        }
                    }
                    else
                    {
                        JSPManager.showJSP(request, response,
                            "/register/cannot-register.jsp");
                    }
                }
            }
            else
            {
                if (eperson == null)
                {
                    // Invalid email address
                    log.info(LogManager.getHeader(context, "unknown_email",
                            "email=" + email));

                    request.setAttribute("retry", Boolean.TRUE);

                    JSPManager.showJSP(request, response,
                            "/register/forgot-password.jsp");
                }
                else if (!eperson.canLogIn())
                {
                    // Can't give new password to inactive user
                    log.info(LogManager.getHeader(context,
                            "unregistered_forgot_password", "email=" + email));

                    JSPManager.showJSP(request, response,
                            "/register/inactive-account.jsp");
                }
                else if (eperson.getRequireCertificate() && !registering)
                {
                    // User that requires certificate can't get password
                    log.info(LogManager.getHeader(context,
                            "certificate_user_forgot_password", "email="
                                    + email));

                    JSPManager.showJSP(request, response,
                            "/error/require-certificate.jsp");
                }
                else
                {
                    // OK to send forgot pw token.
                    log.info(LogManager.getHeader(context,
                            "sendtoken_forgotpw", "email=" + email));

                    AccountManager.sendForgotPasswordInfo(context, email);
                    JSPManager.showJSP(request, response,
                            "/register/password-token-sent.jsp");

                    // Context needs completing to write registration data
                    context.complete();
                }
            }
        }
        catch (AddressException ae)
        {
            // Malformed e-mail address
            log.info(LogManager.getHeader(context, "bad_email", "email="
                    + email));

            request.setAttribute("retry", Boolean.TRUE);

            if (registering)
            {
                if (ldap_enabled)
                {
                    JSPManager.showJSP(request, response, "/register/new-ldap-user.jsp");
                }
                else
                {
                    JSPManager.showJSP(request, response, "/register/new-user.jsp");
                }
            }
            else
            {
                JSPManager.showJSP(request, response, "/register/forgot-password.jsp");
            }
        }
        catch (MessagingException me)
        {
            // Some other mailing error
            log.info(LogManager.getHeader(context, "error_emailing", "email=" + email), me);

            JSPManager.showInternalError(request, response);
        }
    }

    /**
     * Process information from "Personal information page"
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processPersonalInfo(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // Get the token
        String token = request.getParameter("token");

        // Get the email address
        String email = AccountManager.getEmail(context, token);
        String netid = request.getParameter("netid");
        if ((netid!=null)&&(email==null))
        {
            email = request.getParameter("email");
        }
        
        // If the token isn't valid, show an error
        if (email == null && netid==null)
        {
            log.info(LogManager.getHeader(context, "invalid_token", "token="
                + token));

            // Invalid token
            JSPManager
                .showJSP(request, response, "/register/invalid-token.jsp");

            return;
        }

        // If the token is valid, we create an eperson record if need be
        EPerson eperson = null;
        if (email!=null)
        {
            eperson = EPerson.findByEmail(context, email);
        }
        EPerson eperson2 = null;
        if (netid!=null)
        {
            eperson2 = EPerson.findByNetid(context, netid.toLowerCase());
        }
        if (eperson2 !=null)
        {
            eperson = eperson2;
        }
        
        if (eperson == null)
        {
            // Need to create new eperson
            // FIXME: TEMPORARILY need to turn off authentication, as usually
            // only site admins can create e-people
            context.turnOffAuthorisationSystem();
            eperson = EPerson.create(context);
            eperson.setEmail(email);
            if (netid!=null)
            {
                eperson.setNetid(netid.toLowerCase());
            }
            eperson.update();
            context.restoreAuthSystemState();
        }

        // Now set the current user of the context
        // to the user associated with the token, so they can update their
        // info
        context.setCurrentUser(eperson);

        // Set the user profile info
        boolean infoOK = EditProfileServlet.updateUserProfile(eperson, request);

        eperson.setCanLogIn(true);
        eperson.setSelfRegistered(true);

        // Give site auth a chance to set/override appropriate fields
        AuthenticationManager.initEPerson(context, request, eperson);

        // If the user set a password, make sure it's OK
        boolean passwordOK = true;
        if (!eperson.getRequireCertificate() && netid==null &&
            AuthenticationManager.allowSetPassword(context, request,
                eperson.getEmail()))
        {
            passwordOK = EditProfileServlet.confirmAndSetPassword(eperson,
                    request);
        }

        if (infoOK && passwordOK)
        {
            // All registered OK.
            log.info(LogManager.getHeader(context, "usedtoken_register",
                    "email=" + eperson.getEmail()));

            // delete the token
            if (token!=null)
            {
                AccountManager.deleteToken(context, token);
            }
            
            // Update user record
            eperson.update();

            request.setAttribute("eperson", eperson);

            JSPManager.showJSP(request, response, "/register/registered.jsp");
            context.complete();
        }
        else
        {
            request.setAttribute("token", token);
            request.setAttribute("eperson", eperson);
            request.setAttribute("netid", netid);
            request.setAttribute("missing.fields", Boolean.valueOf(!infoOK));
            request.setAttribute("password.problem", Boolean.valueOf(!passwordOK));

            // Indicate if user can set password
            boolean setPassword = AuthenticationManager.allowSetPassword(
                    context, request, email);
            request.setAttribute("set.password", Boolean.valueOf(setPassword));

            JSPManager.showJSP(request, response,
                    "/register/registration-form.jsp");

            // Changes to/creation of e-person in DB cancelled
            context.abort();
        }
    }

    /**
     * Process information from "enter new password"
     * 
     * @param context
     *            current DSpace context
     * @param request
     *            current servlet request object
     * @param response
     *            current servlet response object
     */
    private void processNewPassword(Context context,
            HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException,
            AuthorizeException
    {
        // Get the token
        String token = request.getParameter("token");

        // Get the eperson associated with the password change
        EPerson eperson = AccountManager.getEPerson(context, token);

        // If the token isn't valid, show an error
        if (eperson == null)
        {
            log.info(LogManager.getHeader(context, "invalid_token", "token="
                    + token));

            // Invalid token
            JSPManager
                    .showJSP(request, response, "/register/invalid-token.jsp");

            return;
        }

        // If the token is valid, we set the current user of the context
        // to the user associated with the token, so they can update their
        // info
        context.setCurrentUser(eperson);

        // Confirm and set the password
        boolean passwordOK = EditProfileServlet.confirmAndSetPassword(eperson,
                request);

        if (passwordOK)
        {
            log.info(LogManager.getHeader(context, "usedtoken_forgotpw",
                    "email=" + eperson.getEmail()));

            eperson.update();
            AccountManager.deleteToken(context, token);

            JSPManager.showJSP(request, response,
                    "/register/password-changed.jsp");
            context.complete();
        }
        else
        {
            request.setAttribute("password.problem", Boolean.TRUE);
            request.setAttribute("token", token);
            request.setAttribute("eperson", eperson);

            JSPManager.showJSP(request, response, "/register/new-password.jsp");
        }
    }
}
