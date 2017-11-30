<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Message informing user they are not allowed to self-register.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.register.cannot-register.title">

    <%-- <h1>Cannot Register</h1> --%>
	<h1><fmt:message key="jsp.register.cannot-register.title"/></h1>
    
    <%-- <p>The configuration of this DSpace site does not allow you to register
    yourself.  Please feel free to contact us with any queries.</p> --%>
	<p><fmt:message key="jsp.register.cannot-register.msg"/></p>
    
    <dspace:include page="/components/contact-info.jsp" />
</dspace:layout>
