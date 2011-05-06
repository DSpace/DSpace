<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Message informing user who's tried to register that they're registered
  - already
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.register.already-registered.title">
    
<%-- <h1>Already Registered</h1> --%>
<h1><fmt:message key="jsp.register.already-registered.title"/></h1>

    <%-- <p>Our records show that you've already registered with DSpace and have
    an active account with us.</p> --%>
	<p><fmt:message key="jsp.register.already-registered.info1"/></p>

    <%-- <p><strong>You can <a href="<%= request.getContextPath() %>/forgot"> set
    a new password if you've forgotten it</a>.</p> --%>
	<p><fmt:message key="jsp.register.already-registered.info2">
        <fmt:param><%= request.getContextPath() %>/forgot</fmt:param>
    </fmt:message></p>

    <%-- <p>If you're having trouble logging in, please contact us.</p> --%>
	<p><fmt:message key="jsp.register.already-registered.info4"/></p>
    
    <dspace:include page="/components/contact-info.jsp" />

</dspace:layout>
