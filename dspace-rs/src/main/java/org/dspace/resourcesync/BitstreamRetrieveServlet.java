/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.resourcesync;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.internet.MimeUtility;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

/**
 * Servlet for retrieving bitstreams in a UI indipendent way. The bits are simply piped to the user.
 * Taken from org.dspace.app.webui.servlet.RetrieveServlet
 * <P>
 * <code>/bitstreams/bitstream-id</code>
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Andrea Petrucci (andrea.petrucci at 4science.it)
 * 
 */
public class BitstreamRetrieveServlet extends HttpServlet
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** log4j category */
    private static Logger log = Logger.getLogger(BitstreamRetrieveServlet.class);

    /**
	 * Pattern used to get file.ext from filename (which can be a path)
	 */
	private static Pattern p = Pattern.compile("[^/]*$");

    /**
     * Threshold on Bitstream size before content-disposition will be set.
     */
    private int threshold;
    
    
    
    private boolean isResourceSyncRelevant(Bundle bnd) {
		if (bnd == null)
			return false;
		return ResourceSyncConfiguration.getBundlesToExpose().contains(bnd.getName());
	}
    @Override
    public void doGet(HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException
    {
    	Context context = null;
    	try
        {
    		context = new Context();
    		Bitstream bitstream = null;
    		// prendere il bundle dalla conf rs    		
    		
    		// sostituire con bundle rilevante o no
    		boolean isRelevant = false;
    		//boolean isLicense = false;


    		// Get the ID from the URL
    		String idString = request.getPathInfo();

    		if (idString != null)
    		{
    			// Remove leading slash
    			if (idString.startsWith("/"))
    			{
    				idString = idString.substring(1);
    			}

    			// If there's a second slash, remove it and anything after it,
    			// it might be a filename
    			int slashIndex = idString.indexOf('/');

    			if (slashIndex != -1)
    			{
    				idString = idString.substring(0, slashIndex);
    			}

    			// Find the corresponding bitstream
    			try
    			{
    				int id = Integer.parseInt(idString);
    				bitstream = Bitstream.find(context, id);
    			}
    			catch (NumberFormatException nfe)
    			{
    	        	log.error(nfe.getMessage(),nfe);

    				// Invalid ID - this will be dealt with below
    			}
    		}

    		// Did we get a bitstream?
    		if (bitstream != null)
    		{

    			// Check whether we got a License and if it should be displayed
    			// (Note: list of bundles may be empty array, if a bitstream is a Community/Collection logo)
    			Bundle bundle = bitstream.getBundles().length>0 ? bitstream.getBundles()[0] : null;
    			isRelevant = isResourceSyncRelevant(bundle);
//    			if (bundle!=null && 
//    					bundle.getName().equals(Constants.LICENSE_BUNDLE_NAME) &&
//    					bitstream.getName().equals(Constants.LICENSE_BITSTREAM_NAME))
//    			{
//    				isLicense = true;
//    			}
    			if (!isRelevant)
    			{
    				throw new AuthorizeException();
    			}
    			log.info(LogManager.getHeader(context, "rs_bitstream",
    					"bitstream_id=" + bitstream.getID()));

    			boolean usageStatistics = ConfigurationManager.getBooleanProperty("resourcesync","usage-statistics.track.download");
    			if (usageStatistics)
    			{
    				new DSpace().getEventService().fireEvent(
    					new UsageEvent(
    							UsageEvent.Action.VIEW,
    							request, 
    							context, 
    							bitstream));
    			}
    			//UsageEvent ue = new UsageEvent();
    			// ue.fire(request, context, AbstractUsageEvent.VIEW,
    			//Constants.BITSTREAM, bitstream.getID());

    			// Pipe the bits
    			InputStream is = bitstream.retrieve();

    			// Set the response MIME type
    			response.setContentType(bitstream.getFormat().getMIMEType());

    			// Response length
    			response.setHeader("Content-Length", String.valueOf(bitstream
    					.getSize()));

    			if(threshold != -1 && bitstream.getSize() >= threshold)
    			{
    				setBitstreamDisposition(bitstream.getName(), request, response);
    			}

    			Utils.bufferedCopy(is, response.getOutputStream());
    			is.close();
    			response.getOutputStream().flush();
    		}
    		else
    		{
    			// No bitstream - we got an invalid ID
    			log.info(LogManager.getHeader(context, "rs_bitstream",
    					"invalid_bitstream_id=" + idString));

    			// return NOT_FOUND
    			response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
    		}
        }catch(Exception e) {
        	log.error(e.getMessage(),e);
        }finally {
        	if (context != null && context.isValid())
        	{
        		context.abort();
        	}
        }
    }
    
    /**
	 * Evaluate filename and client and encode appropriate disposition
	 *
	 * @param filename
	 * @param request
	 * @param response
	 * @throws UnsupportedEncodingException
	 */
	public static void setBitstreamDisposition(String filename, HttpServletRequest request,
			HttpServletResponse response)
	{

		String name = filename;

		Matcher m = p.matcher(name);

		if (m.find() && !m.group().equals(""))
		{
			name = m.group();
		}

		try
		{
			String agent = request.getHeader("USER-AGENT");

			if (null != agent && -1 != agent.indexOf("MSIE"))
			{
				name = URLEncoder.encode(name, "UTF8");
			}
			else if (null != agent && -1 != agent.indexOf("Mozilla"))
			{
				name = MimeUtility.encodeText(name, "UTF8", "B");
			}

		}
		catch (UnsupportedEncodingException e)
		{
			log.error(e.getMessage(),e);
		}
		finally
		{
			response.setHeader("Content-Disposition", "attachment;filename=" + name);
		}
	}

}
