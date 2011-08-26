package org.swordapp.server.servlets;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.parser.Parser;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.swordapp.server.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwordServlet extends HttpServlet
{
    private static Logger log = Logger.getLogger(SwordServlet.class);

	protected SwordConfiguration config;

	public void init() throws ServletException
	{
		// load the configuration implementation
        this.config = (SwordConfiguration) this.loadImplClass("config-impl", false);
	}

    protected Object loadImplClass(String paramName, boolean allowNull)
            throws ServletException
    {
        String className = getServletContext().getInitParameter(paramName);
        if (className == null)
        {
            if (allowNull)
            {
                return null;
            }
            else
            {
                log.fatal("'" + paramName + "' init parameter not set in Servlet context");
                throw new ServletException("'" + paramName + "' init parameter not set in Servlet context");
            }
        }
        else
        {
            try
            {
                Object obj = Class.forName(className).newInstance();
                log.info("Using " + className + " as '" + paramName + "'");
                return obj;
            }
            catch (Exception e)
            {
                log.fatal("Unable to instantiate class from '" + paramName + "': " + className);
                throw new ServletException("Unable to instantiate class from '" + paramName + "': " + className, e);
            }
        }
    }
}
