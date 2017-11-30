<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page representing an invalid ID error
  -
  - Attributes:
  -    bad.id   - Optional.  The ID that is invalid.
  -    bad.type - Optional.  The type of the ID (or the type the system thought
  -               is was!) from org.dspace.core.Constants.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.Constants" %>

<%@ page isErrorPage="true" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    String badID = (String) request.getAttribute("bad.id");
    Integer type = (Integer) request.getAttribute("bad.type");

    // Make sure badID isn't null
    if (badID == null)
    {
        badID = "";
    }

    // Get text for the type

    String typeString = LocaleSupport.getLocalizedMessage(pageContext, "jsp.error.invalid-id.type.object");
    if (type != null && type.intValue() > -1 && type.intValue() < 8)
    {
        typeString = LocaleSupport.getLocalizedMessage(pageContext, "jsp.error.invalid-id.constants.type." + type.intValue());
    }

%>

<dspace:layout locbar="off" titlekey="jsp.error.invalid-id.title">
    <h1><fmt:message key="jsp.error.invalid-id.title"/></h1>
	<p><fmt:message key="jsp.error.invalid-id.text1">
        <fmt:param><%= badID %></fmt:param>
        <fmt:param><%= typeString %></fmt:param>
    </fmt:message></p>

    <ul>
        <li><fmt:message key="jsp.error.invalid-id.list1"/></li>
        <li><fmt:message key="jsp.error.invalid-id.list2"/></li>
    </ul>
    <p><fmt:message key="jsp.error.invalid-id.text2"/></p>

    <dspace:include page="/components/contact-info.jsp" />

    <p align="center">
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>
	
</dspace:layout>
