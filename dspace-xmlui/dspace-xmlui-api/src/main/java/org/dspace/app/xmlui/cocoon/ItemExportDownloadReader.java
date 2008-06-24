/*
 * ItemExportDownlaodReader.java
 *
 * Version: $Revision: 1.5 $
 *
 * Date: $Date: 2006/08/08 20:59:54 $
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

package org.dspace.app.xmlui.cocoon;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.environment.http.HttpResponse;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.cocoon.util.ByteRange;
import org.dspace.app.itemexport.ItemExport;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

/**
 * @author Jay Paz
 */

public class ItemExportDownloadReader extends AbstractReader implements Recyclable
{

	/**
     * Messages to be sent when the user is not authorized to view 
     * a particular bitstream. They will be redirected to the login
     * where this message will be displayed.
     */
	private final static String AUTH_REQUIRED_HEADER = "xmlui.ItemExportDownloadReader.auth_header";
	private final static String AUTH_REQUIRED_MESSAGE = "xmlui.ItemExportDownloadReader.auth_message";
	
    /**
     * How big of a buffer should we use when reading from the bitstream before
     * writting to the HTTP response?
     */
    protected static final int BUFFER_SIZE = 8192;

    /**
     * When should a download expire in milliseconds. This should be set to
     * some low value just to prevent someone hiting DSpace repeatily from
     * killing the server. Note: 60000 milliseconds are in a second.
     * 
     * Format: minutes * seconds * milliseconds
     */
    protected static final int expires = 60 * 60 * 60000;

    /** The Cocoon response */
    protected Response response;

    /** The Cocoon request */
    protected Request request;

    /** The bitstream file */
    protected InputStream compressedExportInputStream;
    
    /** The compressed export's reported size */
    protected long compressedExportSize;
    
    protected String compressedExportName;
    /**
     * Set up the export reader.
     * 
     * See the class description for information on configuration options.
     */
    public void setup(SourceResolver resolver, Map objectModel, String src,
            Parameters par) throws ProcessingException, SAXException,
            IOException
    {
        super.setup(resolver, objectModel, src, par);

        try
        {
            this.request = ObjectModelHelper.getRequest(objectModel);
            this.response = ObjectModelHelper.getResponse(objectModel);
            Context context = ContextUtil.obtainContext(objectModel);

            // Get our parameters that identify the bitstream
            String fileName = par.getParameter("fileName", null);
            
                
            // Is there a User logged in and does the user have access to read it?
            if (!ItemExport.canDownload(context, fileName))
            {
            	if(this.request.getSession().getAttribute("dspace.current.user.id")!=null){
            		// A user is logged in, but they are not authorized to read this bitstream, 
            		// instead of asking them to login again we'll point them to a friendly error 
            		// message that tells them the bitstream is restricted.
            		String redictURL = request.getContextPath() + "/restricted-resource?name=" + fileName;

            		HttpServletResponse httpResponse = (HttpServletResponse) 
            		objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
            		httpResponse.sendRedirect(redictURL);
            		return;
            	}
            	else{

            		// The user does not have read access to this bitstream. Inturrupt this current request
            		// and then forward them to the login page so that they can be authenticated. Once that is
            		// successfull they will request will be resumed.
            		AuthenticationUtil.interruptRequest(objectModel, AUTH_REQUIRED_HEADER, AUTH_REQUIRED_MESSAGE, null);

            		// Redirect
            		String redictURL = request.getContextPath() + "/login";

            		HttpServletResponse httpResponse = (HttpServletResponse) 
            		objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
            		httpResponse.sendRedirect(redictURL);
            		return;
            	}
            }
                
                
            // Success, bitstream found and the user has access to read it.
            // Store these for later retreval:
            this.compressedExportInputStream = ItemExport.getExportDownloadInputStream(fileName, context.getCurrentUser());
            this.compressedExportSize = ItemExport.getExportFileSize(fileName);
            this.compressedExportName = fileName;
        }
        catch (Exception e)
        {
            throw new ProcessingException("Unable to read bitstream.",e);
        } 
    }

    
    /**
	 * Write the actual data out to the response.
	 * 
	 * Some implementation notes,
	 * 
	 * 1) We set a short expires time just in the hopes of preventing someone
	 * from overloading the server by clicking reload a bunch of times. I
	 * realize that this is nowhere near 100% effective but it may help in some
	 * cases and shouldn't hurt anything.
	 * 
	 */
    public void generate() throws IOException, SAXException,
            ProcessingException
    {
    	if (this.compressedExportInputStream == null)
	    	return;
    	
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = -1;

        response.setDateHeader("Expires", System.currentTimeMillis()
                + expires);
        response.setHeader("Content-disposition","attachement; filename=" + this.compressedExportName );
        // Turn off partial downloads, they cause problems
        // and are only rarely used. Specifically some windows pdf
        // viewers are incapable of handling this request. By
        // uncommenting the following two lines you will turn this feature back on.
        // response.setHeader("Accept-Ranges", "bytes");
        // String ranges = request.getHeader("Range");
        String ranges = null;
        

        ByteRange byteRange = null;
        if (ranges != null)
        {
            try
            {
                ranges = ranges.substring(ranges.indexOf('=') + 1);
                byteRange = new ByteRange(ranges);
            }
            catch (NumberFormatException e)
            {
                byteRange = null;
                if (response instanceof HttpResponse)
                {
                    // Respond with status 416 (Request range not
                    // satisfiable)
                    ((HttpResponse) response).setStatus(416);
                }
            }
        }

        if (byteRange != null)
        {
            String entityLength;
            String entityRange;
            if (this.compressedExportSize != -1)
            {
                entityLength = "" + this.compressedExportSize;
                entityRange = byteRange.intersection(
                        new ByteRange(0, this.compressedExportSize)).toString();
            }
            else
            {
                entityLength = "*";
                entityRange = byteRange.toString();
            }

            response.setHeader("Content-Range", entityRange + "/"
                    + entityLength);
            if (response instanceof HttpResponse)
            {
                // Response with status 206 (Partial content)
                ((HttpResponse) response).setStatus(206);
            }

            int pos = 0;
            int posEnd;
            while ((length = this.compressedExportInputStream.read(buffer)) > -1)
            {
                posEnd = pos + length - 1;
                ByteRange intersection = byteRange
                        .intersection(new ByteRange(pos, posEnd));
                if (intersection != null)
                {
                    out.write(buffer, (int) intersection.getStart()
                            - pos, (int) intersection.length());
                }
                pos += length;
            }
        }
        else
        {
            response.setHeader("Content-Length", String
                    .valueOf(this.compressedExportSize));

            while ((length = this.compressedExportInputStream.read(buffer)) > -1)
            {
                out.write(buffer, 0, length);
            }
            out.flush();
        }
    }

    /**
     * Returns the mime-type of the bitstream.
     */
    public String getMimeType()
    {
    	return ItemExport.COMPRESSED_EXPORT_MIME_TYPE;
    }
    
    /**
	 * Recycle
	 */
    public void recycle() {        
        this.response = null;
        this.request = null;
        this.compressedExportInputStream = null;
        this.compressedExportSize = 0;
        
    }


}
