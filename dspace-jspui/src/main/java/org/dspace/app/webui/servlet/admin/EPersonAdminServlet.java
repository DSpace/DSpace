/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.jstl.core.Config;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authenticate.service.AuthenticationService;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.AccountService;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;

/**
 * Servlet for editing and creating e-people
 * 
 * @author David Stuve
 * @version $Revision$
 */
public class EPersonAdminServlet extends DSpaceServlet
{
	private final transient EPersonService personService
             = EPersonServiceFactory.getInstance().getEPersonService();
	
	private final transient GroupService groupService
             = EPersonServiceFactory.getInstance().getGroupService();
	
	private final transient AuthenticationService authenticationService
             = AuthenticateServiceFactory.getInstance().getAuthenticationService();
	
	private final transient AccountService accountService
             = EPersonServiceFactory.getInstance().getAccountService();
	
    /** Logger */
    private static final Logger log = Logger.getLogger(EPersonAdminServlet.class);
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        showMain(context, request, response);
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        if (button.equals("submit_add"))
        {
            // add an EPerson, then jump user to edit page
            EPerson e = personService.create(context);

            // create clever name and do update before continuing
            e.setEmail("newuser" + e.getID());
            personService.update(context, e);

            request.setAttribute("eperson", e);

            JSPManager.showJSP(request, response,
                    "/dspace-admin/eperson-edit.jsp");

            context.complete();
        }
        else if (button.equals("submit_edit"))
        {
            // edit an eperson
            EPerson e = personService.find(context, UIUtil.getUUIDParameter(request,
                    "eperson_id"));
            
            // Check the EPerson exists
            if (e == null)
            {
            	request.setAttribute("no_eperson_selected", Boolean.TRUE);
            	showMain(context, request, response);
            }
            else 
            {            
	            // what groups is this person a member of?
	            List<Group> groupMemberships = groupService.allMemberGroups(context, e);
	
	            request.setAttribute("eperson", e);
	            request.setAttribute("group.memberships", groupMemberships);
	
	            JSPManager.showJSP(request, response,
	                    "/dspace-admin/eperson-edit.jsp");
	
	            context.complete();
            }
        }
        else if (button.equals("submit_save") || button.equals("submit_resetpassword"))
        {
            // Update the metadata for an e-person
            EPerson e = personService.find(context, UIUtil.getUUIDParameter(request,
                    "eperson_id"));

            // see if the user changed the email - if so, make sure
            // the new email is unique
            String oldEmail = e.getEmail();
            String newEmail = request.getParameter("email").trim();
            String netid = request.getParameter("netid");

            if (!newEmail.equals(oldEmail))
            {
                // change to email, now see if it's unique
                if (personService.findByEmail(context, newEmail) == null)
                {
                    // it's unique - proceed!
                    e.setEmail(newEmail);

                    e
                            .setFirstName(context, request.getParameter("firstname")
                                    .equals("") ? null : request
                                    .getParameter("firstname"));

                    e
                            .setLastName(context, request.getParameter("lastname")
                                    .equals("") ? null : request
                                    .getParameter("lastname"));

                    if (netid != null)
                    {
                        e.setNetid(netid.equals("") ? null : netid.toLowerCase());
                    }
                    else
                    {
                        e.setNetid(null);
                    }

                    // FIXME: More data-driven?
                    personService.setMetadata(context, e, "phone", request.getParameter("phone")
                            .equals("") ? null : request.getParameter("phone"));
 
                    personService.setMetadata(context, e, "language", request.getParameter("language")
                           .equals("") ? null : request.getParameter("language"));
                    
                    e.setCanLogIn((request.getParameter("can_log_in") != null)
                            && request.getParameter("can_log_in")
                                    .equals("true"));

                    e.setRequireCertificate((request
                            .getParameter("require_certificate") != null)
                            && request.getParameter("require_certificate")
                                    .equals("true"));

                    personService.update(context, e);

                    if (button.equals("submit_resetpassword"))
                    {                        
                        try
                        {
                            resetPassword(context, request, response, e);
                        }
                        catch (MessagingException e1)
                        {
                            JSPManager
                                    .showJSP(request, response,
                                            "/dspace-admin/eperson-resetpassword-error.jsp");
                            return;
                        }
                    }
                    showMain(context, request, response);
                    context.complete();
                }
                else
                {
                    // not unique - send error message & let try again
                    request.setAttribute("eperson", e);
                    request.setAttribute("email_exists", Boolean.TRUE);

                    JSPManager.showJSP(request, response,
                            "/dspace-admin/eperson-edit.jsp");

                    context.complete();
                }
            }
            else
            {
                // no change to email
                if (netid != null)
                {
                    e.setNetid(netid.equals("") ? null : netid.toLowerCase());
                }
                else
                {
                    e.setNetid(null);
                }

                e
                        .setFirstName(context, request.getParameter("firstname").equals(
                                "") ? null : request.getParameter("firstname"));

                e
                        .setLastName(context, request.getParameter("lastname")
                                .equals("") ? null : request
                                .getParameter("lastname"));

                // FIXME: More data-driven?
                personService.setMetadata(context, e, "phone",
                        request.getParameter("phone").equals("") ? null
                                : request.getParameter("phone"));
                
                personService.setMetadata(context, e, "language", request.getParameter("language")
                        .equals("") ? null : request.getParameter("language"));
                         
                e.setCanLogIn((request.getParameter("can_log_in") != null)
                        && request.getParameter("can_log_in").equals("true"));

                e.setRequireCertificate((request
                        .getParameter("require_certificate") != null)
                        && request.getParameter("require_certificate").equals(
                                "true"));

                personService.update(context, e);

                if (button.equals("submit_resetpassword"))
                {
                    try
                    {
                        resetPassword(context, request, response, e);
                    }
                    catch (MessagingException e1)
                    {
                        JSPManager
                                .showJSP(request, response,
                                        "/dspace-admin/eperson-resetpassword-error.jsp");
                        return;
                    }                   
                }
                
                showMain(context, request, response);
                context.complete();
            }
            


        }
        else if (button.equals("submit_delete"))
        {
            // Start delete process - go through verification step
            EPerson e = personService.find(context, UIUtil.getUUIDParameter(request,
                    "eperson_id"));
            
            // Check the EPerson exists
            if (e == null)
            {
            	request.setAttribute("no_eperson_selected", Boolean.TRUE);
            	showMain(context, request, response);
            }
            else 
            {       
	            request.setAttribute("eperson", e);
	
	            JSPManager.showJSP(request, response,
	                    "/dspace-admin/eperson-confirm-delete.jsp");
            }
        }
        else if (button.equals("submit_confirm_delete"))
        {
            // User confirms deletion of type
            EPerson e = personService.find(context, UIUtil.getUUIDParameter(request,
                    "eperson_id"));

            try
            {
                personService.delete(context, e);
            }
            catch (SQLException ex)
            {
                request.setAttribute("eperson", e);
                request.setAttribute("tableList", ex.getMessage());
                JSPManager.showJSP(request, response,
                        "/dspace-admin/eperson-deletion-error.jsp");
            }

            showMain(context, request, response);
            context.complete();
        }
        else if (button.equals("submit_login_as"))
        {
            if (!ConfigurationManager.getBooleanProperty("webui.user.assumelogin", false))
            {
                throw new AuthorizeException("Turn on webui.user.assumelogin to activate Login As feature");                
            }
            EPerson e = personService.find(context, UIUtil.getUUIDParameter(request,
                    "eperson_id"));
            // Check the EPerson exists
            if (e == null)
            {
                request.setAttribute("no_eperson_selected", Boolean.TRUE);
                showMain(context, request, response);
            }
            // Only super administrators can login as someone else.
            else if (!authorizeService.isAdmin(context))
            {                
                throw new AuthorizeException("Only site administrators may assume login as another user.");
            }
            else
            {
                
                log.info(LogManager.getHeader(context, "login-as",
                        "current_eperson="
                                + context.getCurrentUser().getFullName()
                                + ", id=" + context.getCurrentUser().getID()
                                + ", as_eperson=" + e.getFullName() + ", id="
                                + e.getID()));
                
                // Just to be double be sure, make sure the administrator
                // is the one who actually authenticated himself.
                HttpSession session = request.getSession(false);
                UUID authenticatedID = (UUID) session.getAttribute("dspace.current.user.id"); 
                if (context.getCurrentUser().getID() != authenticatedID)
                {                                         
                    throw new AuthorizeException("Only authenticated users who are administrators may assume the login as another user.");                    
                }
                
                // You may not assume the login of another super administrator
                Group administrators = groupService.findByName(context,Group.ADMIN);
                if (groupService.isDirectMember(administrators, e))
                {                    
                    JSPManager.showJSP(request, response,
                            "/dspace-admin/eperson-loginas-error.jsp");
                    return;
                }
                               
                // store a reference to the authenticated admin
                session.setAttribute("dspace.previous.user.id", authenticatedID);
                
                // Logged in OK.
                Authenticate.loggedIn(context, request, e);

                // Set the Locale according to user preferences
                Locale epersonLocale = I18nUtil.getEPersonLocale(context
                        .getCurrentUser());
                context.setCurrentLocale(epersonLocale);
                Config.set(request.getSession(), Config.FMT_LOCALE,
                        epersonLocale);

                // Set any special groups - invoke the authentication mgr.
                List<Group> groupIDs = authenticationService.getSpecialGroups(
                        context, request);

                for (Group g : groupIDs)
                {
                    context.setSpecialGroup(g.getID());
                    log.debug("Adding Special Group id="
                            + String.valueOf(g.getID()));
                }

                response.sendRedirect(request.getContextPath() + "/mydspace");
            }
        }
        else
        {
            // Cancel etc. pressed - show list again
            showMain(context, request, response);
        }
    }

    private void resetPassword(Context context, HttpServletRequest request,
            HttpServletResponse response, EPerson e) throws SQLException,
            IOException, AuthorizeException, ServletException,
            MessagingException
    {
        // Note, this may throw an error is the email is bad.
        accountService.sendForgotPasswordInfo(context, e.getEmail());
        request.setAttribute("reset_password", Boolean.TRUE);
    }

    private void showMain(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        JSPManager.showJSP(request, response, "/dspace-admin/eperson-main.jsp");
    }
}
