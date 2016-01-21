<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page that displays the list of choices of login pages
  - offered by multiple stacked authentication methods.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.Iterator" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="java.sql.SQLException" %>

<%@ page import="org.apache.log4j.Logger" %>

<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.authenticate.factory.AuthenticateServiceFactory" %>
<%@ page import="org.dspace.authenticate.service.AuthenticationService" %>
<%@ page import="org.dspace.authenticate.AuthenticationMethod" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.LogManager" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout navbar="off" locbar="off" titlekey="jsp.login.chooser.title" nocache="true">

    <table border="0" width="90%">
        <tr>
            <td align="left">
                <h1><fmt:message key="jsp.login.chooser.heading"/></h1>
            </td>
            <td align="right" class="standard">
                <dspace:popup page='<%= LocaleSupport.getLocalizedMessage(pageContext, "help.index") + "#login" %>'><fmt:message key="jsp.help"/></dspace:popup>
            </td>
        </tr>
    </table>
    <p></p>
    <table class="miscTable" align="center" width="70%">
      <tr>
        <td class="evenRowEvenCol">
          <h2><fmt:message key="jsp.login.chooser.chooseyour"/></h2>
          <ul>
<%
    AuthenticationService authenticationService = AuthenticateServiceFactory.getInstance().getAuthenticationService();
    Iterator ai = authenticationService.authenticationMethodIterator();
    AuthenticationMethod am;
    Context context = null;
    try
    {
    	context = UIUtil.obtainContext(request);
    	int count = 0;
    	String url = null;
    	while (ai.hasNext())
    	{
            am = (AuthenticationMethod)ai.next();
            if ((url = am.loginPageURL(context, request, response)) != null)
            {
%>
            <li><p><strong><a href="<%= url %>">
		<%-- This kludge is necessary because fmt:message won't
                     evaluate its attributes, so we can't use it on java expr --%>
                <%= javax.servlet.jsp.jstl.fmt.LocaleSupport.getLocalizedMessage(pageContext, am.loginPageTitle(context)) %>
                        </a></strong></p></li>
<%
            }
        }
    }
    catch(SQLException se)
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
          </ul>
        </td>
      </tr>
    </table>


</dspace:layout>
