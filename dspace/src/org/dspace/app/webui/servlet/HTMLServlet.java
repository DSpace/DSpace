/*
 * RetrieveServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
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

import java.io.InputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;

import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;

/**
 * Servlet for HTML bitstream support.
 * <P>
 * <code>/html/handle/filename</code>
 *
 * @author  Robert Tansley
 * @version $Revision$
 */
public class HTMLServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(HTMLServlet.class);
    
    
    protected void doDSGet(Context context,
        HttpServletRequest request,
        HttpServletResponse response)
        throws ServletException, IOException, SQLException, AuthorizeException
    {
        Bitstream bitstream = null;
    
        // Get the ID from the URL
        String idString = request.getPathInfo();
	String filename = "";
	String handle = "";

        if (idString != null)
        {

            // Remove leading slash
            if (idString.startsWith("/"))
            {
                idString = idString.substring(1);
            }

            // Get filename
            int slashIndex = idString.lastIndexOf('/');
            if (slashIndex != -1)
            {
		filename = idString.substring(slashIndex + 1);
		filename = URLDecoder.decode(filename);
                handle = idString.substring(0, slashIndex);
            }

            // If there's still a second slash, remove it and anything after it,
            // it might be a relative directory name
            slashIndex = handle.indexOf('/');
	    slashIndex = handle.indexOf('/', slashIndex + 1);
            if (slashIndex != -1)
	    {
		handle = handle.substring(0, slashIndex);
	    }


            // Find the corresponding bitstream
            try
            {
		boolean found = false;

		Item item = (Item) HandleManager.resolveToObject(context, handle);

		if (item == null)
		{
		    log.info(LogManager.getHeader(context,
						  "invalid_id",
						  "path=" + handle));
		    JSPManager.showInvalidIDError(request, response, handle, -1);
		    return;
		}

		Bundle[] bundles = item.getBundles();
		for (int i = 0; i < bundles.length; i++) {
		    Bitstream[] bitstreams = bundles[i].getBitstreams();
		    if (!found) {
			for (int k = 0; k < bitstreams.length; k++) {
			    if (filename.equals(bitstreams[k].getName())) {
				bitstream = bitstreams[k];
				found = true;
			    }
			}
		    }
		}

                //bitstream = Bitstream.find(context, id);
            }
            catch (NumberFormatException nfe)
            {
                // Invalid ID - this will be dealt with below
            }

	}

        // Did we get a bitstream?
        if (bitstream != null)
        {
	    	  
            log.info(LogManager.getHeader(context,
                "view_bitstream",
                "bitstream_id=" + bitstream.getID()));

            // Set the response MIME type
            response.setContentType(bitstream.getFormat().getMIMEType());
            
            // Response length
            response.setHeader("Content-Length",
	      String.valueOf(bitstream.getSize()));
            
            // Pipe the bits
            InputStream is = bitstream.retrieve();
            
            Utils.bufferedCopy(is, response.getOutputStream());
            is.close();
            response.getOutputStream().flush();

        }
        else
        {

            // No bitstream - we got an invalid ID
            log.info(LogManager.getHeader(context,
                "view_bitstream",
                "invalid_bitstream_id=" + idString));
            
            JSPManager.showInvalidIDError(request,
                response,
                idString,
                Constants.BITSTREAM);

        }
    }
}
