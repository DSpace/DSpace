<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Password changed message
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.register.password-changed.title">

    <%-- <h1>Password Changed</h1> --%>
	<h1><fmt:message key="jsp.register.password-changed.title"/></h1>

    <%-- <p>Thank you, your new password has been set and is active immediately.</p> --%>
	<p><fmt:message key="jsp.register.password-changed.info"/></p>

    <%-- <p><a href="<%= request.getContextPath() %>/">Go to DSpace Home</a></p> --%>
	<p><a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.register.password-changed.link"/></a></p>

</dspace:layout>
