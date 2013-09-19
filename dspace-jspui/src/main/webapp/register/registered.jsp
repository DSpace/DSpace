<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Registered OK message
  -
  - Displays a message indicating that the user has registered OK.
  - 
  - Attributes to pass in:
  -   eperson - eperson who's just registered
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.RegisterServlet" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.Utils" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");
%>

<dspace:layout titlekey="jsp.register.registered.title">

    <%-- <h1>Registration Complete</h1> --%>
	<h1><fmt:message key="jsp.register.registered.title"/></h1>
    
    <%-- <p>Thank you <%= Utils.addEntities(eperson.getFirstName()) %>,</p> --%>
	<p><fmt:message key="jsp.register.registered.thank">
        <fmt:param><%= Utils.addEntities(eperson.getFirstName()) %></fmt:param>
    </fmt:message></p>

    <%-- <p>You're now registered to use the DSpace system.  You can subscribe to
    collections to receive e-mail updates about new items.</p> --%>
	<p><fmt:message key="jsp.register.registered.info"/></p>
    
    <%-- <p><a href="<%= request.getContextPath() %>/">Return to DSpace Home</a></p> --%>
	<p><a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.register.general.return-home"/></a></p>
</dspace:layout>
