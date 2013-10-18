<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Submission saved message - displayed whenever the user has clicked
  - "cancel/save" during a submission and elected to save the item.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<dspace:layout style="submission" locbar="off" navbar="off" titlekey="jsp.submit.saved.title">

    <%-- <h1>Submission Saved</h1> --%>
	<h1><fmt:message key="jsp.submit.saved.title"/></h1>

    <%-- <p>Your submission has been saved for you to finish later.  You can continue
    the submission by going to your "My DSpace" page and clicking on the
    relevant "Resume" button.</p> --%>
	<p class="alert alert-info"><fmt:message key="jsp.submit.saved.info"/></p>

    <%-- <p><a href="<%= request.getContextPath() %>/mydspace">Go to My DSpace</a></p> --%>
	<p><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.mydspace.general.goto-mydspace"/></a></p>

</dspace:layout>
