<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Error page for when the file uploaded exceeded the size limit
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="java.io.PrintWriter" %>

<%@ page isErrorPage="true" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.error.exceeded-size.title">
    <h1><fmt:message key="jsp.error.exceeded-size.title"/></h1>
    <p>
        <fmt:message key="jsp.error.exceeded-size.text1">
            <fmt:param><%= request.getAttribute("actualSize") %></fmt:param>
            <fmt:param><%= request.getAttribute("permittedSize") %></fmt:param>
        </fmt:message>
    </p>

    <dspace:include page="/components/contact-info.jsp" />

    <p align="center">
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>
    <!--
    <%
    String error = request.getAttribute("error.message").toString();
    if(error == null)
    {
        out.println("No stack trace available<br/>");
    }
    else
    {
        out.println(error);
    }
    %>
    -->
</dspace:layout>
