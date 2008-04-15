/*
 * URIServlet.java
 *
 * Version: $Revision: 1726 $
 *
 * Date: $Date: 2007-01-18 16:49:52 +0000 (Thu, 18 Jan 2007) $
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
import org.dspace.uri.ResolvableIdentifier;
import org.dspace.uri.IdentifierService;
import org.dspace.uri.IdentifierException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;

/**
 * @author James Rutherford
 * @author Richard Jones
 */
public class URIServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(URIServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException, SQLException, AuthorizeException
    {
        this.doDSPost(context, request, response);
    }

    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        try {
            String extraPathInfo = null;
            DSpaceObject dso = null;

            // Original path info of the form:
            //
            // /<identifier namespace>/<identifier>[/<optional path info>]
            String path = request.getPathInfo();

            // get the identifier if there is one
            ResolvableIdentifier di = IdentifierService.resolve(context, path);

            // get the object if there is one
            if (di != null)
            {
                dso = (DSpaceObject) IdentifierService.getResource(context, di);
            }

            // if there is no object, display the invalid id error
            if (dso == null)
            {
                log.info(LogManager.getHeader(context, "invalid_id", "path=" + path));
                JSPManager.showInvalidIDError(request, response, path, -1);
            }
            else
            {
                String urlForm = di.getURLForm();
                int index = path.indexOf(urlForm);
                int startFrom = index + urlForm.length();
                if (startFrom < path.length())
                {
                    extraPathInfo = path.substring(startFrom);
                }

                // we've got a standard content delivery servlet to deal with this, to allow for alternative URI
                // handling mechanisms.  Not the best decoupling, but it'll do for the moment to allow the handle
                // system to offer a legacy url interpretation
                DSpaceObjectServlet dos = new DSpaceObjectServlet();
                dos.processDSpaceObject(context, request, response, dso, extraPathInfo);
            }
        }
        catch (IdentifierException e)
        {
            log.error("caught exception: ", e);
            throw new ServletException(e);
        }
    }
}
