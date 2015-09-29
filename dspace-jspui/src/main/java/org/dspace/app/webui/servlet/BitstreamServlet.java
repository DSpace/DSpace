/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Date;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

/**
 * Servlet for retrieving bitstreams. The bits are simply piped to the user. If
 * there is an <code>If-Modified-Since</code> header, only a 304 status code
 * is returned if the containing item has not been modified since that date.
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

    /**
     * When should a bitstream expire in milliseconds. This should be set to
     * some low value just to prevent someone hitting DSpace repeatedly from
     * killing the server. Note: there are 1000 milliseconds in a second.
     *
     * Format: minutes * seconds * milliseconds
     *  60 * 60 * 1000 == 1 hour
     */
    protected static final int expires = ConfigurationManager.getIntProperty("webui.bitstream.expires", 60 * 60 * 1000);

    /**
     * Block Archiving in Search Engines and Proxies, enable by default
     */
    protected static final boolean ALLOW_CACHING = ConfigurationManager.getBooleanProperty("webui.bitstream.cache", true);

    /**
     * Threshold on Bitstream size before content-disposition will be set.
     */
    protected static final int threshold = ConfigurationManager.getIntProperty("webui.content_disposition_threshold", 8388608);
    
    @Override
	public void init(ServletConfig arg0) throws ServletException {

		super.init(arg0);
	}

    @Override
	protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
    	Item item = null;
    	Bitstream bitstream = null;

        // Get the ID from the URL
        String idString = request.getPathInfo();
        String handle = "";
        String sequenceText = "";
        String filename = null;
        int sequenceID;

        if (idString == null)
        {
            idString = "";
        }
        
        // Parse 'handle' and 'sequence' (bitstream seq. number) out
        // of remaining URL path, which is typically of the format:
        // {handle}/{sequence}/{bitstream-name}
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
                {
                    sequenceText = idString.substring(slashIndex+1,slash2);
                    filename = idString.substring(slash2+1);
                }
            }
        }

        try
        {
            sequenceID = Integer.parseInt(sequenceText);
        }
        catch (NumberFormatException nfe)
        {
            sequenceID = -1;
        }
        
        // Now try and retrieve the item
        DSpaceObject dso = HandleManager.resolveToObject(context, handle);
        
        // Make sure we have valid item and sequence number
        if (dso != null && dso.getType() == Constants.ITEM && sequenceID >= 0)
        {
            item = (Item) dso;
        
            if (item.isWithdrawn())
            {
                log.info(LogManager.getHeader(context, "view_bitstream",
                        "handle=" + handle + ",withdrawn=true"));
                JSPManager.showJSP(request, response, "/tombstone.jsp");
                return;
            }

            boolean found = false;

            Bundle[] bundles = item.getBundles();

            for (int i = 0; (i < bundles.length) && !found; i++)
            {
                Bitstream[] bitstreams = bundles[i].getBitstreams();

                for (int k = 0; (k < bitstreams.length) && !found; k++)
                {
                    if (sequenceID == bitstreams[k].getSequenceID())
                    {
                        bitstream = bitstreams[k];
                        found = true;
                    }
                }
            }
        }

        if (bitstream == null || filename == null
                || !filename.equals(bitstream.getName()))
        {
            // No bitstream found or filename was wrong -- ID invalid
            log.info(LogManager.getHeader(context, "invalid_id", "path="
                    + idString));
            JSPManager.showInvalidIDError(request, response, idString,
                    Constants.BITSTREAM);

            return;
        }

        log.info(LogManager.getHeader(context, "view_bitstream",
                "bitstream_id=" + bitstream.getID()));
        
        //new UsageEvent().fire(request, context, AbstractUsageEvent.VIEW,
		//		Constants.BITSTREAM, bitstream.getID());

        new DSpace().getEventService().fireEvent(
        		new UsageEvent(
        				UsageEvent.Action.VIEW, 
        				request, 
        				context, 
        				bitstream));
        
        // Modification date
        // Only use last-modified if this is an anonymous access
        // Set Cache Control headers appropriately.
        // - caching content that may be generated under authorisation
        //   is a security problem
        // Gather last modified timestamp if possible
        Date lastModified;

        if(bitstream.getLastModified() != null)
            lastModified = bitstream.getLastModified();
        else if(item != null)
            lastModified = item.getLastModified();
        else
            lastModified = null;


        boolean isAnonymouslyReadable = false;

        if (context.getCurrentUser() == null)
        {
            isAnonymouslyReadable = true;
        }
        else
        {
            for (ResourcePolicy rp : AuthorizeManager.getPoliciesActionFilter(context, bitstream, Constants.READ))
            {
                if (rp.getGroupID() == 0)
                {
                    isAnonymouslyReadable = true;
                }
            }
        }

        if (isAnonymouslyReadable)
        {
            response.setDateHeader("Expires", System.currentTimeMillis() + expires);

            // Only allow caching if enabled in configuration
            if (ALLOW_CACHING) {
                // Allow caching of anonymously available content and configure expires.
                response.setHeader("Cache-Control", "max-age=" + expires + ", must-revalidate"); // HTTP 1.1.
            }
            else
            {
                // Block Caching of anonymously available content and configure expires
                response.setHeader("Cache-Control", "no-cache, no-store, max-age=" + expires + ", must-revalidate"); // HTTP 1.1.
                response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
                response.setHeader("X-Robots-Tag", "noarchive");
            }

            // Set Last-Modified so client can send If-Modified-Since based on DSpace modified timestamps.
            if (lastModified != null)
            {
                response.setDateHeader("Last-Modified", lastModified.getTime());
            }

            // If modified-since header provided, it was already cached,, then check it in all cases
            long modSince = request.getDateHeader("If-Modified-Since");

            if (modSince != -1 && lastModified != null && lastModified.getTime() <= modSince)
            {
                // Bitstream has not been modified since requested date, return 304
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
        }
        else
        {
            // Never Cache and always Expire Private Content, Crawler should never reach here
            // Proxies may get here when proxing content for authenticated users.
            response.setHeader("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"); // HTTP 1.1.
            response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
            response.setDateHeader("Expires", 0); // Proxies.
            response.setHeader("X-Robots-Tag", "noindex, noarchive");
        }

        // Pipe the bits
        InputStream is = bitstream.retrieve();
     
		// Set the response MIME type
        response.setContentType(bitstream.getFormat().getMIMEType());

        // Response length
        response.setHeader("Content-Length", String
                .valueOf(bitstream.getSize()));

		if(threshold != -1 && bitstream.getSize() >= threshold)
		{
			UIUtil.setBitstreamDisposition(bitstream.getName(), request, response);
		}

        //DO NOT REMOVE IT - WE NEED TO FREE DB CONNECTION TO AVOID CONNECTION POOL EXHAUSTION FOR BIG FILES AND SLOW DOWNLOADS
        context.complete();

        Utils.bufferedCopy(is, response.getOutputStream());
        is.close();
        response.getOutputStream().flush();
    }
}
