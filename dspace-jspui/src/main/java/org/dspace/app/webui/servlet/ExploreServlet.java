/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.util.IViewer;
import org.dspace.app.webui.services.ViewerConfigurationService;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.viewer.JSPViewer;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.handle.HandleManager;
import org.dspace.usage.UsageEvent;
import org.dspace.utils.DSpace;

/**
 * Servlet for retrieving bitstreams. The bits are simply piped to the user. If
 * there is an <code>If-Modified-Since</code> header, only a 304 status code is
 * returned if the containing item has not been modified since that date.
 * <P>
 * <code>/bitstream/handle/sequence_id/filename</code>
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class ExploreServlet extends DSpaceServlet {
	/** log4j category */
	private static Logger log = Logger.getLogger(ExploreServlet.class);

	@Override
	protected void doDSGet(Context context, HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException, SQLException, AuthorizeException {
		Bitstream bitstream = null;

		// Get the ID from the URL
		String bitstreamID = request.getParameter("bitstream_id");
		bitstream = Bitstream.find(context, Integer.parseInt(bitstreamID));

		if (bitstream == null) {
			// No bitstream found or filename was wrong -- ID invalid
			log.info(LogManager.getHeader(context, "invalid bitstream id", "ID=" + bitstreamID));
			JSPManager.showInvalidIDError(request, response, bitstreamID, Constants.BITSTREAM);

			return;
		}

		AuthorizeManager.authorizeAction(context, bitstream, Constants.READ);

		String handle = request.getParameter("handle");
		String viewname = request.getParameter("provider");
		Item i = (Item) HandleManager.resolveToObject(context, handle);
		String title = i.getMetadata("dc.title");
		String filename = bitstream.getName();

		if (!bitstream.getMetadataValue(IViewer.METADATA_STRING_PROVIDER).contains(viewname)) {
			throw new AuthorizeException(LogManager.getHeader(context, "explore",
					"Attempt to access a bitstream with an unregistered view. Bistream ID: " + bitstreamID
							+ " viewprovider: " + viewname));
		}

		log.info(LogManager.getHeader(context, "view_bitstream", "bitstream_id=" + bitstream.getID()));

		new DSpace().getEventService().fireEvent(new UsageEvent(UsageEvent.Action.VIEW, request, context, bitstream));

		request.setAttribute("filename", filename);
		request.setAttribute("handle", handle);
		request.setAttribute("itemTitle", title);

		ViewerConfigurationService viewerConfigurationService = new DSpace()
				.getSingletonService(ViewerConfigurationService.class);
		JSPViewer viewer = viewerConfigurationService.getMapViewers().get(viewname);
		if (viewer != null) {
			viewer.prepareViewAttribute(context, request, bitstream);
			if (viewer.isEmbedded()) {
				request.setAttribute("view", viewer.getViewJSP());
				JSPManager.showJSP(request, response, "explore.jsp");
			} else {
				JSPManager.showJSP(request, response, "/viewers/" + viewer.getViewJSP() + ".jsp");

			}
		} else {
			JSPManager.showJSP(request, response, "explore-error.jsp");
		}
	}
}
