/*
 * SuggestServlet.java
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

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.uri.*;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;
import org.dspace.uri.dao.ExternalIdentifierStorageException;

import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.MissingResourceException;


/**
 * Servlet for handling user email recommendations
 *
 * @author  Arnaldo Dantas
 * @version $Revision$
 */
public class SuggestServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(SuggestServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
    					   HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        try
        {
            ExternalIdentifierDAO identifierDAO =
                ExternalIdentifierDAOFactory.getInstance(context);

            // Obtain information from request
            String uri = request.getParameter("uri");
            ExternalIdentifier identifier = ExternalIdentifierService.parseCanonicalForm(context, uri);
            // ExternalIdentifier identifier = identifierDAO.retrieve(uri);
            ObjectIdentifier oi = identifier.getObjectIdentifier();

            // Lookup Item title & collection
            Item item = null;
            String link = "";
            String title = null;
            String collName = null;
            if (identifier != null)
            {
                item = (Item) IdentifierService.getResource(context, oi);
                link = IdentifierService.getURL(item).toString();
                request.setAttribute("link", link);

                if (item != null)
                {
                    DCValue[] titleDC = item.getDC("title", null, Item.ANY);
                    if (titleDC != null || titleDC.length > 0)
                    {
                        title = titleDC[0].value;
                    }
                    Collection[] colls = item.getCollections();
                    collName = colls[0].getMetadata("name");
                }
            }
            else
            {
                String path = request.getPathInfo();
                log.info(LogManager.getHeader(context, "invalid_id", "path=" + path));
                JSPManager.showInvalidIDError(request, response, path, -1);
                return;
            }
            if (title == null)
            {
                title = "";
            }
            if(collName == null)
            {
                collName = "";
            }
            request.setAttribute("suggest.title", title);

            // User email from context
            EPerson currentUser = context.getCurrentUser();
            String authEmail = null;
            String userName = null;

            if (currentUser != null)
            {
                authEmail = currentUser.getEmail();
                userName = currentUser.getFullName();
            }

            if (request.getParameter("submit") != null)
            {
                String recipAddr = request.getParameter("recip_email");
                // the only required field is recipient email address
                if (recipAddr == null || recipAddr.equals(""))
                {
                    log.info(LogManager.getHeader(context, "show_suggest_form",
                             "problem=true"));
                    request.setAttribute("suggest.problem", new Boolean(true));
                    JSPManager.showJSP(request, response, "/suggest/suggest.jsp");
                    return;
                }
                String recipName = request.getParameter("recip_name");
                if (recipName == null || "".equals(recipName))
                {
                    try
                    {
                    recipName = I18nUtil.getMessage("org.dspace.app.webui.servlet.SuggestServlet.recipient", context);
                    }
                    catch (MissingResourceException e)
                    {
                        log.warn(LogManager.getHeader(context, "show_suggest_form", "Missing Resource: org.dspace.app.webui.servlet.SuggestServlet.sender"));
                        recipName = "colleague";
                    }
                }
                String senderName = request.getParameter("sender_name");
                if (senderName == null || "".equals(senderName) )
                {
                    // use userName if available
                    if (userName != null)
                    {
                        senderName = userName;
                    }
                    else
                    {
                        try
                        {
                        senderName = I18nUtil.getMessage("org.dspace.app.webui.servlet.SuggestServlet.sender", context);
                        }
                        catch (MissingResourceException e)
                        {
                            log.warn(LogManager.getHeader(context, "show_suggest_form", "Missing Resource: org.dspace.app.webui.servlet.SuggestServlet.sender"));
                            senderName = "A DSpace User";
                        }
                    }
                }
                String senderAddr = request.getParameter("sender_email");
                if (senderAddr == null || "".equals(senderAddr) )
                {
                    // use authEmail if available
                    if (authEmail != null)
                    {
                        senderAddr = authEmail;
                    }
                }
                String itemUri = identifier.getURI().toString();
                String message = request.getParameter("message");
                String siteName = ConfigurationManager.getProperty("dspace.name");

                // All data is there, send the email
                try
                {
                    Email email = ConfigurationManager.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "suggest"));
                    email.addRecipient(recipAddr);	 // recipient address
                    email.addArgument(recipName);    // 1st arg - recipient name
                    email.addArgument(senderName);   // 2nd arg - sender name
                    email.addArgument(siteName);     // 3rd arg - repository name
                    email.addArgument(title);        // 4th arg - item title
                    email.addArgument(itemUri);      // 5th arg - item identifier URI
                    email.addArgument(link);         // 6th arg - item local URL
                    email.addArgument(collName);     // 7th arg - collection name
                    email.addArgument(message);      // 8th arg - user comments

                    // Set sender's address as 'reply-to' address if supplied
                    if ( senderAddr != null && ! "".equals(senderAddr))
                    {
                        email.setReplyTo(senderAddr);
                    }

                    // Only actually send the email if feature is enabled
                    if (ConfigurationManager.getBooleanProperty("webui.suggest.enable", false))
                    {
                        email.send();
                    } else
                    {
                        throw new MessagingException("Suggest item email not sent - webui.suggest.enable = false");
                    }

                    log.info(LogManager.getHeader(context, "sent_suggest",
                                                  "from=" + senderAddr));

                    JSPManager.showJSP(request, response, "/suggest/suggest_ok.jsp");
                }
                catch (MessagingException me)
                {
                    log.warn(LogManager.getHeader(context, "error_mailing_suggest", ""), me);
                    JSPManager.showInternalError(request, response);
                }
            }
            else
            {
                // Display suggest form
                log.info(LogManager.getHeader(context, "show_suggest_form", "problem=false"));
                request.setAttribute("authenticated.email", authEmail);
                request.setAttribute("eperson.name", userName);
                JSPManager.showJSP(request, response, "/suggest/suggest.jsp"); //asd
            }
        }
        catch (ExternalIdentifierStorageException e)
        {
            log.error("caught exception: ", e);
            throw new ServletException(e);
        }
        catch (IdentifierException e)
        {
            log.error("caught exception: ", e);
            throw new ServletException(e);
        }
    }

    protected void doDSPost(Context context, HttpServletRequest request,
    						HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        // Treat as a GET
        doDSGet(context, request, response);
    } 
}
