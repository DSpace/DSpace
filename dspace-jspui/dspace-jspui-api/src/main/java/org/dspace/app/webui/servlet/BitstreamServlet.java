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
import org.dspace.app.webui.util.ViewAgreement;
import org.dspace.authorize.AuthorizeException;
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
     * Threshold on Bitstream size before content-disposition will be set.
     */
    private int threshold;
    
    @Override
	public void init(ServletConfig arg0) throws ServletException {

		super.init(arg0);
		threshold = ConfigurationManager
				.getIntProperty("webui.content_disposition_threshold");
	}

    @Override
    protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
        ReferredObjects ref = new ReferredObjects(context, request);

        InputStream is = null;

        try {
            if (ref.item.isWithdrawn()) {
                log.info(LogManager.getHeader(context, "view_bitstream", "handle=" + ref.item.getHandle() + ",withdrawn=true"));
                JSPManager.showJSP(request, response, "/tombstone.jsp");
                return;
            }

            if (ref.bitstream == null || ref.filename == null || !ref.filename.equals(ref.bitstream.getName())) {
                // No bitstream found or filename was wrong -- ID invalid
                log.info(LogManager.getHeader(context, "invalid_id", "path=" + ref.idString));
                JSPManager.showInvalidIDError(request, response, ref.idString, Constants.BITSTREAM);

                return;
            }

            // throws exception if not authorized to access
            is = ref.bitstream.retrieve();

            // now that we know that user has authorization to access bitstream
            // check whether we need to make user sign agreement before proceeding
            if (ViewAgreement.mustAgree(request.getSession(), ref.item)) {
                log.info(LogManager.getHeader(context, "view_bitstream", "handle=" + ref.item.getHandle() + ",mustAgree=true"));
                request.setAttribute("item", ref.item);
                JSPManager.showJSP(request, response, "/bitstream-agreement.jsp");
                return;
            }


            log.info(LogManager.getHeader(context, "view_bitstream", "bitstream_id=" + ref.bitstream.getID()));

            //new UsageEvent().fire(request, context, AbstractUsageEvent.VIEW,
            //		Constants.BITSTREAM, bitstream.getID());

            new DSpace().getEventService().fireEvent(new UsageEvent(UsageEvent.Action.VIEW, request, context, ref.bitstream));


            // Now put response together
            // Modification date
            // Only use last-modified if this is an anonymous access
            // - caching content that may be generated under authorisation
            //   is a security problem
            if (context.getCurrentUser() == null) {
                // TODO: Currently the date of the item, since we don't have dates
                // for files
                response.setDateHeader("Last-Modified", ref.item.getLastModified().getTime());

                // Check for if-modified-since header
                long modSince = request.getDateHeader("If-Modified-Since");

                if (modSince != -1 && ref.item.getLastModified().getTime() < modSince) {
                    // Item has not been modified since requested date,
                    // hence bitstream has not; return 304
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }
            }

            // Set the response MIME type
            response.setContentType(ref.bitstream.getFormat().getMIMEType());

            // Response length
            response.setHeader("Content-Length", String.valueOf(ref.bitstream.getSize()));

            if (threshold != -1 && ref.bitstream.getSize() >= threshold) {
                UIUtil.setBitstreamDisposition(ref.bitstream.getName(), request, response);
            }

            // piping the bits from input stream to response
            Utils.bufferedCopy(is, response.getOutputStream());
        } finally {
            if (is != null)
                is.close();
            response.getOutputStream().flush();
        }
    }

    @Override
    protected void doDSPost(Context context, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SQLException, AuthorizeException {
        ReferredObjects ref = new ReferredObjects(context, request);
        if (ref.item != null && ref.bitstream != null && !ref.item.isWithdrawn()) {
            String buttonPressed;
            buttonPressed = UIUtil.getSubmitButton(request, "submit_noagree");
            if (buttonPressed.equals("submit_agree")) {
                ViewAgreement.doAgree(request.getSession(), ref.item);
                response.sendRedirect(request.getRequestURI());
                return;
            }
        }
        super.doDSPost(context, request, response);
    }

}

class ReferredObjects {
    private static Logger log = Logger.getLogger(BitstreamServlet.class);

    Item item = null;
    Bitstream bitstream = null;
    String idString = "";
    String filename = null;

    ReferredObjects(Context context, HttpServletRequest request) throws SQLException {

        // Get the ID from the URL
        idString = request.getPathInfo();

        String handle = "";
        String sequenceText = "";
        int sequenceID;

        if (idString == null) {
            idString = "";
        }

        // Parse 'handle' and 'sequence' (bitstream seq. number) out
        // of remaining URL path, which is typically of the format:
        // {handle}/{sequence}/{bitstream-name}
        // But since the bitstream name MAY have any number of "/"s in
        // it, and the handle is guaranteed to have one slash, we
        // scan from the start to pick out handle and sequence:

        // Remove leading slash if any:
        if (idString.startsWith("/")) {
            idString = idString.substring(1);
        }

        // skip first slash within handle
        int slashIndex = idString.indexOf('/');
        if (slashIndex != -1) {
            slashIndex = idString.indexOf('/', slashIndex + 1);
            if (slashIndex != -1) {
                handle = idString.substring(0, slashIndex);
                int slash2 = idString.indexOf('/', slashIndex + 1);
                if (slash2 != -1) {
                    sequenceText = idString.substring(slashIndex + 1, slash2);
                    filename = idString.substring(slash2 + 1);
                }
            }
        }

        try {
            sequenceID = Integer.parseInt(sequenceText);
        } catch (NumberFormatException nfe) {
            sequenceID = -1;
        }

        // Now try and retrieve the item
        DSpaceObject dso = HandleManager.resolveToObject(context, handle);

        // Make sure we have valid item and sequence number
        if (dso != null && dso.getType() == Constants.ITEM && sequenceID >= 0) {
            item = (Item) dso;
            if (!item.isWithdrawn()) {
                Boolean found = false;
                Bundle[] bundles = item.getBundles();
                for (int i = 0; (i < bundles.length) && !found; i++) {
                    Bitstream[] bitstreams = bundles[i].getBitstreams();

                    for (int k = 0; (k < bitstreams.length) && !found; k++) {
                        if (sequenceID == bitstreams[k].getSequenceID()) {
                            bitstream = bitstreams[k];
                            found = true;
                        }
                    }
                }
            }
        }
    }
}
