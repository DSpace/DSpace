package org.dspace.app.bagit;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class BagItServlet extends HttpServlet {

	private static final Logger LOGGER = LoggerFactory.getLogger(BagItServlet.class);
	
	@Override
	protected void doGet(HttpServletRequest aRequest,
			HttpServletResponse aResponse) throws ServletException, IOException {
		PrintWriter writer = aResponse.getWriter();
		writer.println("Hello world!");
		writer.close();
	}

	@Override
	public void init() throws ServletException {
		ServletContext context = this.getServletContext();
		String configFileName = context.getInitParameter("dspace.config");
		
		if (LOGGER.isDebugEnabled() && configFileName != null) {
			LOGGER.debug("Using DSpace config from ", configFileName);
		}
	}
}
