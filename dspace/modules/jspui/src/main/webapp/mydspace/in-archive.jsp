<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Task completed message page
  -
  - Attributes:
  -   handle - Handle of the item that's just gone in the archive.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    String handle = (String) request.getAttribute("handle");
%>

<dspace:layout style="submission"
			   locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.in-archive.title">

    <%--<h1>Thank You</h1>--%>
    <h1><fmt:message key="jsp.mydspace.in-archive.heading1"/></h1>

    <%--<p>The submission has been placed in the main archive.  It has been assigned
    the following identifier:</p>--%>
	<p><fmt:message key="jsp.mydspace.in-archive.text1"/></p>
    <p><a href="<%= handle %>"><%= handle %></a></p>

    <%--<p>Notification has been sent to the appropriate people.</p>--%>
	<p><fmt:message key="jsp.mydspace.in-archive.text2"/></p>

    <p align="center">
        <a href="<%= request.getContextPath() %>/mydspace">
            <%--Return to My DSpace--%>
			<fmt:message key="jsp.mydspace.in-archive.return.link"/>
        </a>
    </p>
</dspace:layout>
