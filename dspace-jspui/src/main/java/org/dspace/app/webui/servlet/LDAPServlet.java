/*
 * LDAPServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.Authenticate;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.AuthenticationManager;
import java.util.Hashtable;

import javax.naming.directory.*;
import javax.naming.*;

// Internal class to pass LDAP details from authentication method
class LDAPResult
{
    String email;
    String givenName;
    String surname;
    String phone;
}

/**
 * LDAP username and password authentication servlet.  Displays the
 * login form <code>/login/ldap.jsp</code> on a GET,
 * otherwise process the parameters as an ldap username and password.
 *
 * @author  John Finlay (Brigham Young University)
 * @version $Revision$
 */
public class LDAPServlet extends DSpaceServlet
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(LDAPServlet.class);

    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // check if ldap is enables and forward to the correct login form
        boolean ldap_enabled = ConfigurationManager.getBooleanProperty("ldap.enable");
        if (ldap_enabled)
            JSPManager.showJSP(request, response, "/login/ldap.jsp");
        else
            JSPManager.showJSP(request, response, "/login/password.jsp");
    }
    
    
    protected void doDSPost(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Process the POSTed email and password
        String netid = request.getParameter("login_netid");
        String password = request.getParameter("login_password");
        
        // Locate the eperson
        EPerson eperson = EPerson.findByNetid(context, netid.toLowerCase());
        EPerson eperson2 = EPerson.findByEmail(context, netid.toLowerCase());
        boolean loggedIn = false;

        // make sure ldap values are null with every request
        LDAPResult ldapResult = new LDAPResult();

        ldapResult.givenName = null;
        ldapResult.surname = null;
        ldapResult.email = null;
        ldapResult.phone = null;

        // if they entered a netid that matches an eperson
        if (eperson != null && eperson.canLogIn())
        {
            // e-mail address corresponds to active account
            if (eperson.getRequireCertificate())
            {
                // they must use a certificate
                JSPManager.showJSP(request,
                    response,
                    "/error/require-certificate.jsp");
                return;
            }
            else 
            {
                if (ldapAuthenticate(netid, password, context, ldapResult))
                {
                    // Logged in OK.
                    Authenticate.loggedIn(context, request, eperson);

                    log.info(LogManager
                        .getHeader(context, "login", "type=ldap"));

                    // resume previous request
                    Authenticate.resumeInterruptedRequest(request, response);
                    return;
                }
                else 
                {
                   JSPManager.showJSP(request, response, "/login/ldap-incorrect.jsp");
                   return;
                }
            }
        }
        // if they entered an email address that matches an eperson
        else if (eperson2 != null && eperson2.canLogIn())
        {
            // e-mail address corresponds to active account
            if (eperson2.getRequireCertificate())
            {
                // they must use a certificate
                JSPManager.showJSP(request,
                    response,
                    "/error/require-certificate.jsp");
                return;
            }
            else
            {
                if (eperson2.checkPassword(password))
                {
                    // Logged in OK.
                    Authenticate.loggedIn(context, request, eperson2);

                    log.info(LogManager
                        .getHeader(context, "login", "type=password"));

                    // resume previous request
                    Authenticate.resumeInterruptedRequest(request, response);
                    return;
                }
                else
                {
                   JSPManager.showJSP(request, response, "/login/ldap-incorrect.jsp");
                   return;
                }
            }
        }
        // the user does not already exist so try and authenticate them with ldap and create an eperson for them
        else {
            if (ldapAuthenticate(netid, password, context, ldapResult))
            {
                if (ConfigurationManager.getBooleanProperty("webui.ldap.autoregister"))
                {
                    // Register the new user automatically
                    log.info(LogManager.getHeader(context,
                                    "autoregister", "netid=" + netid));

                    if ((ldapResult.email!=null)&&(!ldapResult.email.equals(""))) 
                    {
                        eperson = EPerson.findByEmail(context, ldapResult.email);
                        if (eperson!=null)
                        {
                            log.info(LogManager.getHeader(context,
                                    "failed_autoregister", "type=ldap_but_already_email"));
                            JSPManager.showJSP(request, response,
                                    "/register/already-registered.jsp");
                            return;
                        }
                    }
                    // TEMPORARILY turn off authorisation
                    context.setIgnoreAuthorization(true);
                    eperson = EPerson.create(context);
                    if ((ldapResult.email!=null)&&(!ldapResult.email.equals(""))) eperson.setEmail(ldapResult.email);
                    else eperson.setEmail(netid);
                    if ((ldapResult.givenName!=null)&&(!ldapResult.givenName.equals(""))) eperson.setFirstName(ldapResult.givenName);
                    if ((ldapResult.surname!=null)&&(!ldapResult.surname.equals(""))) eperson.setLastName(ldapResult.surname);
                    if ((ldapResult.phone!=null)&&(!ldapResult.phone.equals(""))) eperson.setMetadata("phone", ldapResult.phone);
                    eperson.setNetid(netid);
                    eperson.setCanLogIn(true);
                    AuthenticationManager.initEPerson(context, request, eperson);
                    eperson.update();
                    context.commit();
                    context.setIgnoreAuthorization(false); 

                    Authenticate.loggedIn(context, request, eperson);
                    log.info(LogManager.getHeader(context, "login",
                                "type=ldap-login"));
                    Authenticate.resumeInterruptedRequest(request, response);
                    return;
                }
                else
                {
                    // No auto-registration for valid certs
                    log.info(LogManager.getHeader(context,
                                    "failed_login", "type=ldap_but_no_record"));
                    JSPManager.showJSP(request, response,
                                    "/login/not-in-records.jsp");
                    return;
                }
            }
        }


        // If we reach here, supplied email/password was duff.
        log.info(LogManager.getHeader(context,
            "failed_login",
            "netid=" + netid));
        JSPManager.showJSP(request, response, "/login/ldap-incorrect.jsp");
    }

    /**
     * contact the ldap server and attempt to authenticate
     */
    protected boolean ldapAuthenticate(String netid, String password, Context context, LDAPResult ldapResult)
    {
        //--------- START LDAP AUTH SECTION -------------
        if (!password.equals("")) 
        {
            String ldap_provider_url = ConfigurationManager.getProperty("ldap.provider_url");
            String ldap_id_field = ConfigurationManager.getProperty("ldap.id_field");
            String ldap_search_context = ConfigurationManager.getProperty("ldap.search_context");
            String ldap_object_context = ConfigurationManager.getProperty("ldap.object_context");

            // Set up environment for creating initial context
            Hashtable env = new Hashtable(11);
            env.put(javax.naming.Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(javax.naming.Context.PROVIDER_URL, ldap_provider_url);

            // Authenticate 
            env.put(javax.naming.Context.SECURITY_AUTHENTICATION, "simple");
            env.put(javax.naming.Context.SECURITY_PRINCIPAL, ldap_id_field+"="+netid+","+ldap_object_context);
            env.put(javax.naming.Context.SECURITY_CREDENTIALS, password);
            
            try 
            {
                // Create initial context
                DirContext ctx = new InitialDirContext(env);

                String ldap_email_field = ConfigurationManager.getProperty("ldap.email_field");
                String ldap_givenname_field = ConfigurationManager.getProperty("ldap.givenname_field");
                String ldap_surname_field = ConfigurationManager.getProperty("ldap.surname_field");
                String ldap_phone_field = ConfigurationManager.getProperty("ldap.phone_field");

                Attributes matchAttrs = new BasicAttributes(true);
                matchAttrs.put(new BasicAttribute(ldap_id_field, netid));

                String attlist[] = {ldap_email_field, ldap_givenname_field, ldap_surname_field, ldap_phone_field};

                // look up attributes
                try 
                {
                    NamingEnumeration answer = ctx.search(ldap_search_context, matchAttrs, attlist);
                    while(answer.hasMore()) {
                        SearchResult sr = (SearchResult)answer.next();
                        Attributes atts = sr.getAttributes();
                        Attribute att;
                        
                        if (attlist[0]!=null)
                        {
                        	att = atts.get(attlist[0]);
                        	if (att != null) ldapResult.email = (String)att.get();
                        }
                        
                        if (attlist[1]!=null)
                        {
                    		att = atts.get(attlist[1]);
                    		if (att != null) ldapResult.givenName = (String)att.get();
                        }
                        
                        if (attlist[2]!=null) 
                        {
               		     	att = atts.get(attlist[2]);
               		     	if (att != null) ldapResult.surname = (String)att.get();
                        }

                        if (attlist[3]!=null)
                        {
               		     	att = atts.get(attlist[3]);
               		     	if (att != null) ldapResult.phone = (String)att.get();
                        }                        
                    }
                }
                catch (NamingException e) 
                {
                    // if the lookup fails go ahead and create a new record for them because the authentication
                    // succeeded
                    log.warn(LogManager.getHeader(context,
                                    "ldap_attribute_lookup", "type=failed_search "+e));
                    return true;
                }
                // Close the context when we're done
                ctx.close();
            } 
            catch (NamingException e) 
            {
                log.warn(LogManager.getHeader(context,
                                    "ldap_authentication", "type=failed_auth "+e));
                return false;
            }
        }
        else 
        {
            return false;
        }
        //--------- END LDAP AUTH SECTION -------------
               
        return true;
    }
}
