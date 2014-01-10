/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * 
 * @author "João Melo <jmelo@lyncode.com>"
 */
public final class FileServlet extends HttpServlet {
	private static final long serialVersionUID = 8191397166155668213L;
	private static Logger log = LogManager.getLogger(FileServlet.class);

	public void doGet(HttpServletRequest req, HttpServletResponse res)
			throws IOException, ServletException {
		try {

			ServletOutputStream sos = res.getOutputStream();
			String queryString = req.getServletPath() + req.getPathInfo();

			if (queryString != null && queryString.indexOf("..") > -1) {
				// test for other security violations
				sos.println("FileServlet: Illegal Location");
				return;
			}
			
			String url = this.getServletContext().getRealPath(queryString);
			File file = new File(url);
			if (!file.exists()) {
				log.debug("FileServlet: exists[" + queryString
						+ "]: Missing file");
				// res.sendError(HttpServletResponse.SC_NOT_FOUND);
				sos.println("FileServlet: File Not Found");
				return;
			} else if (!file.canRead()) {
				log.debug("FileServlet: canRead[" + queryString
						+ "]: Cannot read");
				res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				return;
			}

			BufferedInputStream bis = null;
			try {
				bis = new BufferedInputStream(new FileInputStream(url),
						BUFFER_SIZE);
			} catch (FileNotFoundException e) {
				sos.println("File Not Found: " + queryString);
			}

			int contentLength = (int) file.length();
			if (contentLength >= 0) {
				res.setContentLength(contentLength);
			}

			String contentType = getServletContext().getMimeType(queryString);

			if (contentType == null) {
				contentType = "text/plain";
			}
			res.setContentType(contentType);

			try {
				byte buffer[] = new byte[BUFFER_SIZE];
				while (true) {
					int n = bis.read(buffer);
					if (n <= 0) {
						break;
					}
					try {
						sos.write(buffer, 0, n);
					} catch (SocketException e) {
						// user hit the stop button. Terminate
						return;
					}
				}
			} catch (IOException e) {
				log.debug("FileServlet: copy[" + queryString + "]", e);
				if (bis != null) {
					try {
						bis.close();
					} catch (IOException f) {
					}
				}
				throw e;
			}
			try {
				bis.close();
			} catch (IOException e) {
			}
			try {
				sos.flush();
				sos.close();
			} catch (IOException e) {
				log.debug("FileServlet: flush[" + queryString + "]", e);
			}

		} catch (Exception e) {
			log.error("PageHtmlServlet.doGet() catch all: " + e);
			e.printStackTrace(System.out);

		}
	}

	private static final int BUFFER_SIZE = 8192;

}
