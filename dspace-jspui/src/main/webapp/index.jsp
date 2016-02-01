<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Home page JSP
  -
  - Note that this is a "stand-alone" JSP that is invoked directly, and not
  - via a Servlet.  This is because the standard servlet 2.3 deployment
  - descriptor does not seem to allow a servlet to be deployed at "/" without
  - it becoming the servlet for dealing with every request to the site.
  -
  - This also means there's some business logic, basically some minimal stuff
  - from DSpaceServlet.java.  This shouldn't happen elsewhere in the JSPs.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.Locale"%>

<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>

<%@ page import="org.apache.log4j.Logger" %>

<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.LogManager" %>
<%@ page import="org.dspace.core.factory.CoreServiceFactory" %>
<%@ page import="org.dspace.plugin.SiteHomeProcessor" %>

<%
    Context context = null;
    
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
    
    try
    {
        // Obtain a context so that the location bar can display log in status
        context = UIUtil.obtainContext(request);
        
        try
        {
            SiteHomeProcessor[] chp = (SiteHomeProcessor[]) CoreServiceFactory.getInstance().getPluginService().getPluginSequence(SiteHomeProcessor.class);
            for (int i = 0; i < chp.length; i++)
            {
                chp[i].process(context, request, response);
            }
        }
        catch (Exception e)
        {
            Logger log = Logger.getLogger("org.dspace.jsp");
            log.error("caught exception: ", e);
            throw new ServletException(e);
        }
        
        // Show home page JSP
        JSPManager.showJSP(request, response, "/home.jsp");
    }
    catch (SQLException se)
    {
        // Database error occurred.
        Logger log = Logger.getLogger("org.dspace.jsp");
        log.warn(LogManager.getHeader(context,
            "database_error",
            se.toString()), se);

        // Also email an alert
        UIUtil.sendAlert(request, se);

        JSPManager.showInternalError(request, response);
    }
%>
