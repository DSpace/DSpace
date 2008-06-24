/*
 * ItemExportArchiveServlet.java
 *
 * Version: $Revision: 2073 $
 *
 * Date: $Date: 2007-07-19 11:45:10 -0500 (Thu, 19 Jul 2007) $
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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.itemexport.ItemExport;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
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
	private static Logger log = Logger
			.getLogger(ItemExportArchiveServlet.class);

	/**
	 * Threshold on export size size before content-disposition will be set.
	 */
	private int threshold;

	@Override
	public void init(ServletConfig arg0) throws ServletException {

		super.init(arg0);
		threshold = ConfigurationManager
				.getIntProperty("webui.content_disposition_threshold");
	}

	@Override
	protected void doDSGet(Context context, HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException,
			SQLException, AuthorizeException {
		String filename = null;

		filename = request.getPathInfo().substring(
				request.getPathInfo().lastIndexOf('/')+1);
		System.out.println(filename);

		if (ItemExport.canDownload(context, filename)) {
			try {
				InputStream exportStream = ItemExport
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
				long lastModified = ItemExport
						.getExportFileLastModified(filename);
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
				response.setContentType(ItemExport.COMPRESSED_EXPORT_MIME_TYPE);

				// Response length
				long size = ItemExport.getExportFileSize(filename);
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
