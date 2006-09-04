/*
 * BitstreamServlet.java
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
import java.io.InputStream;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;

/**
 * Servlet for retrieving bitstreams. The bits are simply piped to the user.
 * <P>
 * <code>/bitstream/handle/sequence_id/filename</code>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class BitstreamServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(BitstreamServlet.class);

    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        Bitstream bitstream = null;
        boolean isWithdrawn = false;

        // Get the ID from the URL
        String idString = request.getPathInfo();
        String handle = "";
        String sequence = "";

        if (idString != null)
        {
             // Parse 'handle' and 'sequence' (bitstream seq. number) out
             // of remaining URL path, which is typically of the format:
             //    {handle}/{sequence}/{bitstream-name}
             // But since the bitstream name MAY have any number of "/"s in
             // it, and the handle is guaranteed to have one slash, we
             // scan from the start to pick out handle and sequence:

             // Remove leading slash if any:
            if (idString.startsWith("/"))
            {
                idString = idString.substring(1);
            }

            // skip first slash within handle
            int slashIndex = idString.indexOf('/');
            if (slashIndex != -1)
            {
                slashIndex = idString.indexOf('/', slashIndex + 1);
                if (slashIndex != -1)
                {
                    handle = idString.substring(0, slashIndex);
                    int slash2 = idString.indexOf('/', slashIndex + 1);
                    if (slash2 != -1)
                        sequence = idString.substring(slashIndex+1,slash2);
                    else
                        sequence = idString.substring(slashIndex+1);
                }
                else
                    handle = idString;
            }
            
            // Find the corresponding bitstream
            try
            {
                Item item = (Item) HandleManager.resolveToObject(context,
                        handle);
                
                if (item == null)
                {
                    log.info(LogManager.getHeader(context, "invalid_id",
                            "path=" + handle));
                    JSPManager
                            .showInvalidIDError(request, response, handle, -1);

                    return;
                }
                
                // determine whether the item the bitstream belongs to has been withdrawn
                isWithdrawn = item.isWithdrawn();

                int sid = Integer.parseInt(sequence);
                boolean found = false;

                Bundle[] bundles = item.getBundles();

                for (int i = 0; (i < bundles.length) && !found; i++)
                {
                    Bitstream[] bitstreams = bundles[i].getBitstreams();

                    for (int k = 0; (k < bitstreams.length) && !found; k++)
                    {
                        if (sid == bitstreams[k].getSequenceID())
                        {
                            bitstream = bitstreams[k];
                            found = true;
                        }
                    }
                }
            }
            catch (NumberFormatException nfe)
            {
                // Invalid ID - this will be dealt with below
            }
        }
        
        if (!isWithdrawn)
        {
            // Did we get a bitstream?
            if (bitstream != null)
            {
                log.info(LogManager.getHeader(context, "view_bitstream",
                    "bitstream_id=" + bitstream.getID()));

                // Set the response MIME type
                response.setContentType(bitstream.getFormat().getMIMEType());

                // Response length
                response.setHeader("Content-Length", String.valueOf(bitstream
                    .getSize()));

                // Pipe the bits
                InputStream is = bitstream.retrieve();

                Utils.bufferedCopy(is, response.getOutputStream());
                is.close();
                response.getOutputStream().flush();
            }
            // display the tombstone instead of the bitstream if the item is withdrawn
            else
            {
                // No bitstream - we got an invalid ID
                log.info(LogManager.getHeader(context, "view_bitstream",
                    "invalid_bitstream_id=" + idString));

                JSPManager.showInvalidIDError(request, response, idString,
                    Constants.BITSTREAM);
            }
        }
        else
        {
            log.info(LogManager.getHeader(context, "view_bitstream", "item has been withdrawn"));
            JSPManager.showJSP(request, response, "/tombstone.jsp");
        }
       
    }
}
