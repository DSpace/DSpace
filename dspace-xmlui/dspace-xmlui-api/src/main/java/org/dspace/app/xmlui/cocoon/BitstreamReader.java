/*
 * BitsreamReader.java
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Matcher;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.avalon.excalibur.pool.Recyclable;
import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.ProcessingException;
import org.apache.cocoon.ResourceNotFoundException;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.Response;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.apache.cocoon.environment.http.HttpResponse;
import org.apache.cocoon.reading.AbstractReader;
import org.apache.cocoon.util.ByteRange;
import org.dspace.app.xmlui.utils.AuthenticationUtil;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.xml.sax.SAXException;

import org.apache.log4j.Logger;
import org.dspace.core.LogManager;

/**
 * The BitstreamReader will query DSpace for a particular bitstream and transmit
 * it to the user. There are several method of specifing the bitstream to be
 * develivered. You may refrence a bitstream by either it's id or attempt to 
 * resolve the bitstream's name.
 * 
 *  /bitstream/{handle}/{sequence}/{name}
 *  
 *  &lt;map:read type="BitstreamReader">
 *    &lt;map:parameter name="handle" value="{1}/{2}"/&gt;
 *    &lt;map:parameter name="sequence" value="{3}"/&gt;
 *    &lt;map:parameter name="name" value="{4}"/&gt;
 *  &lt;/map:read&gt;
 * 
 *  When no handle is assigned yet you can access a bistream 
 *  using it's internal ID.
 *  
 *  /bitstream/id/{bitstreamID}/{sequence}/{name}
 *  
 *  &lt;map:read type="BitstreamReader">
 *    &lt;map:parameter name="bitstreamID" value="{1}"/&gt;
 *    &lt;map:parameter name="sequence" value="{2}"/&gt;
 *  &lt;/map:read&gt;
 *  
 *  Alternativly, you can access the bitstream via a name instead
 *  of directly through it's sequence.
 *  
 *  /html/{handle}/{name}
 *  
 *  &lt;map:read type="BitstreamReader"&gt;
 *    &lt;map:parameter name="handle" value="{1}/{2}"/&gt;
 *    &lt;map:parameter name="name" value="{3}"/&gt;
 *  &lt;/map:read&gt;
 *  
 *  Again when no handle is available you can also access it
 *  via an internal itemID & name.
 *  
 *  /html/id/{itemID}/{name}
 *  
 *  &lt;map:read type="BitstreamReader"&gt;
 *    &lt;map:parameter name="itemID" value="{1}"/&gt;
 *    &lt;map:parameter name="name" value="{2}"/&gt;
 *  &lt;/map:read&gt;
 * 
 * @author Scott Phillips
 */

public class BitstreamReader extends AbstractReader implements Recyclable
{

	private static Logger log = Logger.getLogger(BitstreamReader.class);
	
    /**
     * Messages to be sent when the user is not authorized to view 
     * a particular bitstream. They will be redirected to the login
     * where this message will be displayed.
     */
	private final static String AUTH_REQUIRED_HEADER = "xmlui.BitstreamReader.auth_header";
	private final static String AUTH_REQUIRED_MESSAGE = "xmlui.BitstreamReader.auth_message";
	
    /**
     * How big of a buffer should we use when reading from the bitstream before
     * writting to the HTTP response?
     */
    protected static final int BUFFER_SIZE = 8192;

    /**
     * When should a bitstream expire in milliseconds. This should be set to
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
    protected InputStream bitstreamInputStream;
    
    /** The bitstream's reported size */
    protected long bitstreamSize;
    
    /** The bitstream's mime-type */
    protected String bitstreamMimeType;
    
    /** The bitstream's name */
    protected String bitstreamName;
    
    /**
     * Set up the bitstream reader.
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
            int itemID = par.getParameterAsInteger("itemID", -1);
            int bitstreamID = par.getParameterAsInteger("bitstreamID", -1);
            String handle = par.getParameter("handle", null);
            
            int sequence = par.getParameterAsInteger("sequence", -1);
            String name = par.getParameter("name", null);
        

            // Reslove the bitstream
            Bitstream bitstream = null;
            Item item = null;
            DSpaceObject dso = null;
            
            if (bitstreamID > -1)
            {
            	// Direct refrence to the individual bitstream ID.
            	bitstream = Bitstream.find(context, bitstreamID);
            }
            else if (itemID > -1)
            {
            	// Referenced by internal itemID
            	item = Item.find(context, itemID);
            	
            	if (sequence > -1)
            	{
            		bitstream = findBitstreamBySequence(item, sequence);
            	}
            	else if (name != null)
            	{
            		bitstream = findBitstreamByName(item, name);
            	}
            }
            else if (handle != null)
            {
            	// Reference by an item's handle.
            	dso = HandleManager.resolveToObject(context,handle);
            	
            	if (dso instanceof Item && sequence > -1)
            	{
            		bitstream = findBitstreamBySequence((Item) dso,sequence);
            	}
            	else if (dso instanceof Item && name != null)
            	{
            		bitstream = findBitstreamByName((Item) dso,name);
            	}
            }
          

            // Was a bitstream found?
            if (bitstream == null)
            {
            	throw new ResourceNotFoundException("Unable to locate bitstream");
            }
                
            // Is there a User logged in and does the user have access to read it?
            if (!AuthorizeManager.authorizeActionBoolean(context, bitstream, Constants.READ))
            {
            	if(this.request.getSession().getAttribute("dspace.current.user.id")!=null){
            		// A user is logged in, but they are not authorized to read this bitstream, 
            		// instead of asking them to login again we'll point them to a friendly error 
            		// message that tells them the bitstream is restricted.
            		String redictURL = request.getContextPath() + "/handle/";
            		if (item!=null){
            			redictURL += item.getHandle();
            		}
            		else if(dso!=null){
            			redictURL += dso.getHandle();
            		}
            		redictURL += "/restricted-resource?bitstreamId=" + bitstream.getID();

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
            this.bitstreamInputStream = bitstream.retrieve();
            this.bitstreamSize = bitstream.getSize();
            this.bitstreamMimeType = bitstream.getFormat().getMIMEType();
            this.bitstreamName = bitstream.getName();
            
            // Trim any path information from the bitstream
    		int finalSlashIndex = bitstreamName.lastIndexOf("/");
    		if (finalSlashIndex > 0)
    		{
    			bitstreamName = bitstreamName.substring(finalSlashIndex+1);
    		}
    		
            
            // Log that the bitstream has been viewed.
			log.info(LogManager.getHeader(context, "view_bitstream", "bitstream_id=" + bitstream.getID()));
        }
        catch (SQLException sqle)
        {
            throw new ProcessingException("Unable to read bitstream.",sqle);
        } 
        catch (AuthorizeException ae)
        {
            throw new ProcessingException("Unable to read bitstream.",ae);
        } 
    }

    
    
    
    
    /**
     * Find the bitstream identified by a sequence number on this item. 
     * 
     * @param item A DSpace item
     * @param sequence The sequence of the bitstream
     * @return The bitstream or null if none found.
     */
    private Bitstream findBitstreamBySequence(Item item, int sequence) throws SQLException
    {
    	if (item == null)
    		return null;
    	
    	Bundle[] bundles = item.getBundles();
        for (Bundle bundle : bundles)
        {
            Bitstream[] bitstreams = bundle.getBitstreams();

            for (Bitstream bitstream : bitstreams)
            {
            	if (bitstream.getSequenceID() == sequence)
            	{
            		return bitstream;
                }
            }
        }	
        return null;
    }
    
    /**
     * Return the bitstream from the given item that is identified by the
     * given name. If the name has prepended directories they will be removed
     * one at a time until a bitstream is found. Note that if two bitstreams
     * have the same name then the first bitstream will be returned.
     * 
     * @param item A DSpace item
     * @param name The name of the bitstream
     * @return The bitstream or null if none found.
     */
    private Bitstream findBitstreamByName(Item item, String name) throws SQLException
    {
    	if (name == null || item == null)
    		return null;
    
    	// Determine our the maximum number of directories that will be removed for a path.
    	int maxDepthPathSearch = 3;
    	if (ConfigurationManager.getProperty("xmlui.html.max-depth-guess") != null)
    		maxDepthPathSearch = ConfigurationManager.getIntProperty("xmlui.html.max-depth-guess");
    	
    	// Search for the named bitstream on this item. Each time through the loop
    	// a directory is removed from the name until either our maximum depth is
    	// reached or the bitstream is found. Note: an extra pass is added on to the
    	// loop for a last ditch effort where all directory paths will be removed.
    	for (int i = 0; i < maxDepthPathSearch+1; i++)
    	{
    	   	// Search through all the bitstreams and see
	    	// if the name can be found
	    	Bundle[] bundles = item.getBundles();
	        for (Bundle bundle : bundles)
	        {
	            Bitstream[] bitstreams = bundle.getBitstreams();
	
	            for (Bitstream bitstream : bitstreams)
	            {
	            	if (name.equals(bitstream.getName()))
	            	{
	            		return bitstream;
	            	}
	            }
	        }
	        
	        // The bitstream was not found, so try removing a directory 
	        // off of the name and see if we lost some path information.
	        int indexOfSlash = name.indexOf('/');
	        
	        if (indexOfSlash < 0)
	        	// No more directories to remove from the path, so return null for no
	        	// bitstream found.
	        	return null;
	       
	        name = name.substring(indexOfSlash+1);
	        
	        // If this is our next to last time through the loop then 
	        // trim everything and only use the trailing filename.
    		if (i == maxDepthPathSearch-1)
    		{
    			int indexOfLastSlash = name.lastIndexOf('/');
    			if (indexOfLastSlash > -1)
    				name = name.substring(indexOfLastSlash+1);
    		}
	        
    	}
    	
    	// The named bitstream was not found and we exausted our the maximum path depth that
    	// we search.
    	return null;
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
	 * 2) We accept partial downloads, thus if you lose a connection half way
	 * through most web browser will enable you to resume downloading the
	 * bitstream.
	 */
    public void generate() throws IOException, SAXException,
            ProcessingException
    {
    	if (this.bitstreamInputStream == null)
	    	return;
    	
        byte[] buffer = new byte[BUFFER_SIZE];
        int length = -1;

        response.setDateHeader("Expires", System.currentTimeMillis()
                + expires);
    	
        // If this is a large bitstream then tell the browser it should treat it as a download.
        int threshold = ConfigurationManager.getIntProperty("xmlui.content_disposition_threshold");
        if (bitstreamSize > threshold && threshold != 0)
        {
	    	String name  = bitstreamName;
	    	
	    	// Try and make the download file name formated for each browser.
	    	try {
		    	String agent = request.getHeader("USER-AGENT");
		    	if (agent != null && agent.contains("MSIE"))
		    		name = URLEncoder.encode(name,"UTF8");
		    	else if (agent != null && agent.contains("Mozilla"))
		    		name = MimeUtility.encodeText(name, "UTF8", "B");
	    	}
	    	catch (UnsupportedEncodingException see)
	    	{
	    		// do nothing
	    	}
	        response.setHeader("Content-Disposition", "attachment;filename=" + name);
        }

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
            if (this.bitstreamSize != -1)
            {
                entityLength = "" + this.bitstreamSize;
                entityRange = byteRange.intersection(
                        new ByteRange(0, this.bitstreamSize)).toString();
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
            while ((length = this.bitstreamInputStream.read(buffer)) > -1)
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
                    .valueOf(this.bitstreamSize));

            while ((length = this.bitstreamInputStream.read(buffer)) > -1)
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
    	return this.bitstreamMimeType;
    }
    
    /**
	 * Recycle
	 */
    public void recycle() {        
        this.response = null;
        this.request = null;
        this.bitstreamInputStream = null;
        this.bitstreamSize = 0;
        this.bitstreamMimeType = null;
    }


}
