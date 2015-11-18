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

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.itemexport.ItemExportServiceImpl;
import org.dspace.app.itemexport.factory.ItemExportServiceFactory;
import org.dspace.app.itemexport.service.ItemExportService;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.core.Utils;

/**
 * Servlet for retrieving item export archives. The bits are simply piped to the
 * user. If there is an <code>If-Modified-Since</code> header, only a 304
 * status code is returned if the containing item has not been modified since
 * that date.
 * <P>
 * <code>/exportdownload/filename</code>
 * 
 * @author Jay Paz
 */
public class ItemExportArchiveServlet extends DSpaceServlet {
	/** log4j category */
	private static final Logger log = Logger.getLogger(ItemExportArchiveServlet.class);
	
	private final transient ItemExportService itemExportService
             = ItemExportServiceFactory.getInstance().getItemExportService();
	
	@Override
	protected void doDSGet(Context context, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException {
		String filename = null;

		filename = request.getPathInfo().substring(
				request.getPathInfo().lastIndexOf('/')+1);
		log.debug(filename);

		if (itemExportService.canDownload(context, filename)) {
			try {
				InputStream exportStream = itemExportService
						.getExportDownloadInputStream(filename, context
                                .getCurrentUser());

				if (exportStream == null || filename == null) {
					// No bitstream found or filename was wrong -- ID invalid
					log.info(LogManager.getHeader(context, "invalid_id",
							"path=" + filename));
					JSPManager.showInvalidIDError(request, response, filename,
							Constants.BITSTREAM);

					return;
				}

				log.info(LogManager.getHeader(context,
						"download_export_archive", "filename=" + filename));

				// Modification date
				// TODO: Currently the date of the item, since we don't have
				// dates
				// for files
				long lastModified = itemExportService
						.getExportFileLastModified(context, filename);
				response.setDateHeader("Last-Modified", lastModified);

				// Check for if-modified-since header
				long modSince = request.getDateHeader("If-Modified-Since");

				if (modSince != -1 && lastModified < modSince) {
					// Item has not been modified since requested date,
					// hence bitstream has not; return 304
					response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
					return;
				}

				// Set the response MIME type
				response.setContentType(ItemExportServiceImpl.COMPRESSED_EXPORT_MIME_TYPE);

				// Response length
				long size = itemExportService.getExportFileSize(context, filename);
				response.setHeader("Content-Length", String.valueOf(size));

				response.setHeader("Content-Disposition",
						"attachment;filename=" + filename);

				Utils.bufferedCopy(exportStream, response.getOutputStream());
				exportStream.close();
				response.getOutputStream().flush();
			} catch (Exception e) {
				throw new ServletException(e);
			}
		} else {
			throw new AuthorizeException(
					"You are not authorized to download this Export Archive.");
		}
	}

}
