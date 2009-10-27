/*
 * NewsEditServlet.java
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
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.jstl.fmt.LocaleSupport;

import org.apache.log4j.Logger;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;

/**
 * Servlet for editing the front page news
 * 
 * @author gcarpent
 */
public class NewsEditServlet extends DSpaceServlet
{
    private static Logger log = Logger.getLogger(NewsEditServlet.class);

    protected void doDSGet(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        //always go first to news-main.jsp
        JSPManager.showJSP(request, response, "/dspace-admin/news-main.jsp");
    }

    protected void doDSPost(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        //Get submit button
        String button = UIUtil.getSubmitButton(request, "submit");

        String news = "";

        //Are we editing the top news or the sidebar news?
        String position = request.getParameter("position");
        
        if (button.equals("submit_edit"))
        {
            //get the existing text from the file
            news = ConfigurationManager.readNewsFile(position);

            //pass the position back to the JSP
            request.setAttribute("position", position);

            //pass the existing news back to the JSP
            request.setAttribute("news", news);

            //show news edit page
            JSPManager
                    .showJSP(request, response, "/dspace-admin/news-edit.jsp");
        }
        else if (button.equals("submit_save"))
        {
            //get text string from form
            news = (String) request.getParameter("news");

            //write the string out to file
            ConfigurationManager.writeNewsFile(position, news);

            JSPManager
                    .showJSP(request, response, "/dspace-admin/news-main.jsp");
        }
        else
        {
            //the user hit cancel, so return to the main news edit page
            JSPManager
                    .showJSP(request, response, "/dspace-admin/news-main.jsp");
        }

        c.complete();
    }
}
