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

<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.Map" %>

<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.Locale"%>

<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>

<%@ page import="org.apache.log4j.Logger" %>

<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.CommunityGroup" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.LogManager" %>

<%
    // go to canonical home address
    if (! request.getServerName().equals("localhost")) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.equals("/index.jsp")) {
            response.sendRedirect(request.getContextPath() + "/");
            return;
        }
    }

    Context context = null;
    
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
    
    try
    {
        // Obtain a context so that the location bar can display log in status
        context = UIUtil.obtainContext(request);
        
        // Home page shows community list in groups
        Map topcommMap = new HashMap();
        CommunityGroup[] groups = CommunityGroup.findAll(context);
        for (int k=0; k < groups.length; k++) {
           Integer groupID = new Integer(groups[k].getID());

           // Find Communities in group
           Community[] communities = groups[k].getCommunities();
           topcommMap.put(groupID, communities);
        }

        request.setAttribute("groups", groups);
        request.setAttribute("communities.map", topcommMap);

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
    finally
    {
      if (context != null)
      {
      	context.abort();
      }
    }
%>
