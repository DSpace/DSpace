<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Submission cancelled and removed page - displayed whenever the user has
  - clicked "cancel/save" during a submission and elected to remove the item.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<dspace:layout locbar="off" titlekey="jsp.submit.cancelled-removed.title">

    <%-- <h1>Submission Cancelled</h1> --%>
	<h1><fmt:message key="jsp.submit.cancelled-removed.title"/></h1>

    <%-- <p>Your submission has been cancelled, and the incomplete item removed
    from the system.</p> --%>
	<p><fmt:message key="jsp.submit.cancelled-removed.info"/></p>

    <%-- <p><a href="<%= request.getContextPath() %>/mydspace">Go to My DSpace</a></p> --%>
	<p><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.mydspace.general.goto-mydspace"/></a></p>

</dspace:layout>
