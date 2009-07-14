package edu.umd.lims.dspace.app.webui.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dspace.app.webui.util.JSPManager;

public class DefaultServlet extends org.apache.catalina.servlets.DefaultServlet {

    /**
     * Serve the specified resource, optionally including the data content.
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param content Should the content be included?
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet-specified error occurs
     */
    protected void serveResource(HttpServletRequest request,
                                 HttpServletResponse response,
                                 boolean content)
        throws IOException, ServletException {

		String path = getRelativePath(request);

		if (path != null && path.equals("/")) {
		  JSPManager.showJSP(request, response, "/index.jsp");
		  return;
		}

		super.serveResource(request, response, content);
  }

}
