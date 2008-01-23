/*
 * OpenURLServlet.java
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
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierMint;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * Simple servlet for open URL support. Presently, simply extracts terms from
 * open URL and redirects to search.
 * 
 * @author Robert Tansley
 * @author James Rutherford
 * @version $Revision$
 */
public class OpenURLServlet extends URIServlet
{
    /** Logger */
    private static Logger log = Logger.getLogger(OpenURLServlet.class);

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String id = request.getParameter("id");

        if (id != null)
        {
            processURI(context, request, response);
        }
        else
        {
            // Previous behaviour
            String query = "";

            // Extract open URL terms. Note: assume no repetition
            String title = request.getParameter("title");
            String authorFirst = request.getParameter("aufirst");
            String authorLast = request.getParameter("aulast");

            String logInfo = "";

            if (title != null)
            {
                query = query + " " + title;
                logInfo = logInfo + "title=\"" + title + "\",";
            }

            if (authorFirst != null)
            {
                query = query + " " + authorFirst;
                logInfo = logInfo + "aufirst=\"" + authorFirst + "\",";
            }

            if (authorLast != null)
            {
                query = query + " " + authorLast;
                logInfo = logInfo + "aulast=\"" + authorLast + "\",";
            }

            log.info(LogManager.getHeader(context, "openURL", logInfo
                    + "dspacequery=" + query));

            response.sendRedirect(response.encodeRedirectURL(request
                    .getContextPath()
                    + "/simple-search?query=" + query));
        }
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // Same as a GET
        doDSGet(context, request, response);
    }

    private void processURI(Context context, HttpServletRequest request,
            HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        String id = request.getParameter("id");

        if (id.startsWith("info:dspace/"))
        {
            id = id.substring(new String("info:dspace/").length());
            id = id.replaceFirst("/", ":");
        }

        ExternalIdentifier identifier = null;
        ObjectIdentifier oi = null;
        DSpaceObject dso = null;

        // The value of URI will be the persistent identifier in canonical
        // form, eg: xyz:1234/56
        identifier = ExternalIdentifierMint.parseCanonicalForm(context, id);
        //ExternalIdentifierDAO identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
        //identifier = identifierDAO.retrieve(id);

        oi = identifier.getObjectIdentifier();

        dso = oi.getObject(context);

        if (dso == null)
        {
            log.info(LogManager.getHeader(
                        context, "invalid_id", "id=" + id));
            JSPManager.showInvalidIDError(request, response, id, -1);

            return;
        }
        else
        {
            DSpaceObjectServlet dos = new DSpaceObjectServlet();
            dos.processDSpaceObject(context, request, response, dso, null);
        }
    }
}
