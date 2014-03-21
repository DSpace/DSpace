<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page representing an authorization error
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ page isErrorPage="true" %>

<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.authorize.AuthorizeException" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<dspace:layout titlekey="jsp.error.authorize.title">

    <%-- <h1>Authorization Required</h1> --%>
    <h1><fmt:message key="jsp.error.authorize.title"/></h1>

<%
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    if (user != null) {
%>
        <%-- <p>You do not have permission to perform the action you just attempted.</p> --%>
        <p><fmt:message key="jsp.error.authorize.text1"/></p>
<%
    }
    else
    {
        /* nobody logged in + authorization error
         * => system decided based on implicit authorization alone
         * decide on error message
         */
         String errorMsg = null;
         Context context = UIUtil.obtainContext(request);
         String errorGroups = ConfigurationManager.getProperty("authentication", "error.groups");
         if (errorGroups != null) {
             String groups[] = errorGroups.split(",");
             for (String group : groups) {
                 Group g = Group.findByName(context, group.trim());
                 if (g != null  && (g.getID() == Group.ANONYMOUS_ID || context.inSpecialGroup(g.getID()))) {
                         errorMsg =  ConfigurationManager.getProperty("authentication", "error." + g.getName());
                         break;
                 }
             }
          }
          if (errorMsg == null) {
               errorMsg =
               "Configuration error - no setting for error." + Group.find(context, Group.ANONYMOUS_ID).getName();
          }
%>
          <p>  <%= errorMsg %> </p>
<%
    }
%>

<% if (ConfigurationManager.getBooleanProperty("dspace.debug")) {
    AuthorizeException exp = (AuthorizeException) request.getAttribute("Exception");
   if (exp != null) { 
%>
<div class="debug alert alert-danger"> 
<p> Debug Info: </p>

<p> <%= exp.toString() %>  </p>
<p> action: <%= exp.getAction() %>  </p>
<p> object: <%= exp.getObject() %>  </p>
</div>

<% } } %>

    <dspace:include page="/components/contact-info.jsp" />

    <p align="center">
        <%-- <a href="<%= request.getContextPath() %>/">Go to the DSpace home page</a> --%>
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>

</dspace:layout>
