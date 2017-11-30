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

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.error.authorize.title">

    <%-- <h1>Authorization Required</h1> --%>
    <h1><fmt:message key="jsp.error.authorize.title"/></h1>

    <%-- <p>You do not have permission to perform the action you just attempted.</p> --%>
    <p><fmt:message key="jsp.error.authorize.text1"/></p>

    <%-- <p>If you think you should have authorization, please feel free to
    contact the DSpace administrators:</p> --%>
    <p><fmt:message key="jsp.error.authorize.text2"/></p>

    <dspace:include page="/components/contact-info.jsp" />

    <p align="center">
        <%-- <a href="<%= request.getContextPath() %>/">Go to the DSpace home page</a> --%>
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>
	
</dspace:layout>
