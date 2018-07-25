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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.usage.UsageEvent;

/**
 * Servlet for retrieving bitstreams. The bits are simply piped to the user.
 * <P>
 * <code>/retrieve/bitstream-id</code>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class RetrieveServlet extends DSpaceServlet
{
    /** log4j category */
    private static final Logger log = Logger.getLogger(RetrieveServlet.class);

    /**
     * Threshold on Bitstream size before content-disposition will be set.
     */
    private int threshold;
    
    private final transient BitstreamService bitstreamService
             = ContentServiceFactory.getInstance().getBitstreamService();
    
    @Override
	public void init(ServletConfig arg0) throws ServletException {

		super.init(arg0);
		threshold = ConfigurationManager
				.getIntProperty("webui.content_disposition_threshold");
	}
    
    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        Bitstream bitstream = null;
        boolean displayLicense = ConfigurationManager.getBooleanProperty("webui.licence_bundle.show", false);
        boolean isLicense = false;
        

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
                bitstream = bitstreamService.findByIdOrLegacyId(context, idString);
            }
            catch (NumberFormatException nfe)
            {
                // Invalid ID - this will be dealt with below
            }
        }

        // Did we get a bitstream?
        if (bitstream != null)
        {

            // Check whether we got a License and if it should be displayed
            // (Note: list of bundles may be empty array, if a bitstream is a Community/Collection logo)
            Bundle bundle = bitstream.getBundles().size()>0 ? bitstream.getBundles().get(0) : null;
            
            if (bundle!=null && 
                bundle.getName().equals(Constants.LICENSE_BUNDLE_NAME) &&
                bitstream.getName().equals(Constants.LICENSE_BITSTREAM_NAME))
            {
                    isLicense = true;
            }
            
            if (isLicense && !displayLicense && !authorizeService.isAdmin(context))
            {
                throw new AuthorizeException();
            }
            log.info(LogManager.getHeader(context, "view_bitstream",
                    "bitstream_id=" + bitstream.getID()));

            DSpaceServicesFactory.getInstance().getEventService().fireEvent(
            		new UsageEvent(
            				UsageEvent.Action.VIEW,
            				request, 
            				context, 
            				bitstream));
            
            //UsageEvent ue = new UsageEvent();
           // ue.fire(request, context, AbstractUsageEvent.VIEW,
		   //Constants.BITSTREAM, bitstream.getID());

            // Pipe the bits
            InputStream is = bitstreamService.retrieve(context, bitstream);

            // Set the response MIME type
            response.setContentType(bitstream.getFormat(context).getMIMEType());

            // Response length
            response.setHeader("Content-Length", String.valueOf(bitstream
                    .getSizeBytes()));
            
    		if(threshold != -1 && bitstream.getSizeBytes() >= threshold)
    		{
    			UIUtil.setBitstreamDisposition(bitstream.getName(), request, response);
    		}

            Utils.bufferedCopy(is, response.getOutputStream());
            is.close();
            response.getOutputStream().flush();
        }
        else
        {
            // No bitstream - we got an invalid ID
            log.info(LogManager.getHeader(context, "view_bitstream",
                    "invalid_bitstream_id=" + idString));

            JSPManager.showInvalidIDError(request, response, idString,
                    Constants.BITSTREAM);
        }
    }
}
