<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Message informing user who said they'd forgot their password that their
  - account is inactive.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.register.inactive-account.title">

    <%-- <h1>Inactive Account</h1> --%>
	<h1><fmt:message key="jsp.register.inactive-account.title"/></h1>
    
    <%-- <p>The e-mail address you entered corresponds to an inactive account.
    Perhaps you haven't yet <a href="<%= request.getContextPath() %>/register">
    registered</a>.  Please feel free to contact the site administrators
    with any queries.</p> --%>
	<p><fmt:message key="jsp.register.inactive-account.info">
        <fmt:param><%= request.getContextPath() %>/register</fmt:param>
    </fmt:message></p>
    
    <dspace:include page="/components/contact-info.jsp" />
</dspace:layout>
