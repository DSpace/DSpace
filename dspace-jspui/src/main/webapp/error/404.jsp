<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Friendly 404 error message page
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page isErrorPage="true" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.error.404.title">

    <%-- <h1>Error: Document Not Found</h1> --%>
    <h1><fmt:message key="jsp.error.404.title"/></h1>
    <%-- <p>The document you are trying to access has not been found on the server.</p> --%>
    <p><fmt:message key="jsp.error.404.text1"/></p>
    <ul>
        <%-- <li><p>If you got here by following a link or bookmark provided by someone
        else, the link may be incorrect or you mistyped the link.  Please check
        the link and try again.  If you still get this error, then try going
        to the <a href="<%= request.getContextPath() %>/">DSpace home page</a>
        and looking for what you want from there.</p></li> --%>
		<li><p><fmt:message key="jsp.error.404.text2">
            <fmt:param><%= request.getContextPath() %>/</fmt:param>
        </fmt:message></p></li>
        <%-- <li><p>If you got to this error by clicking in a link on the DSpace site,
        please let us know so we can fix it!</p></li> --%>
		<li><p><fmt:message key="jsp.error.404.text3"/></p></li>
    </ul>

    <dspace:include page="/components/contact-info.jsp" />

    <p align="center">
        <%-- <a href="<%= request.getContextPath() %>/">Go to the DSpace home page</a> --%>
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>

</dspace:layout>