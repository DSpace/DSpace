<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Submission removed message - this is displayed when the user has checked
  - the "This is a thesis" option on the first submission page, had their
  - submission removed, used the back button to go back to the submission
  - page, and tried carry on.  Normally this would result in an integrity error
  - since the workspace ID is no longer valid but in this special case we
  - will display a decent message.
  -
  - This page displays a message informing the user that theses are not
  - presently accepted in DSpace, and that their submission has been removed.
  - FIXME: MIT-SPECIFIC
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<dspace:layout titlekey="jsp.submit.thesis-removed-workaround.title">
    <%-- <h1>Submission Stopped: Theses Not Accepted in DSpace</h1> --%>
	<h1><fmt:message key="jsp.submit.thesis-removed-workaround.heading"/></h1>

    <%-- <p>Since DSpace does not accept theses, your submission has been stopped.
    To start submitting something else click below.</p> --%>
	<p><fmt:message key="jsp.submit.thesis-removed-workaround.info"/></p>

    <p align="center"><strong><a href="<%= request.getContextPath() %>/submit">
    <%-- Start a new submission</a></p> --%>
	<fmt:message key="jsp.submit.thesis-removed-workaround.link"/></a></p>

</dspace:layout>
