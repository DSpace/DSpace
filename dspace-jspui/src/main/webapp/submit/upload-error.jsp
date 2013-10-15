<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - List of uploaded files
  -
  - Attributes to pass in to this page:
  -    submission.info  - the SubmissionInfo object
  -    submission.inputs  - the DCInputSet object
  -
  - FIXME: Merely iterates through bundles, treating all bit-streams as
  -        separate documents.  Shouldn't be a problem for early adopters.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.app.util.DCInputsReader" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request); 

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);
%>

<dspace:layout style="submission" locbar="off"
               navbar="off"
               titlekey="jsp.submit.upload-error.title"
               nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

        <%-- <h1>Submit: Error Uploading File</h1> --%>
		<h1><fmt:message key="jsp.submit.upload-error.heading"/></h1>

        <%-- <p>There was a problem uploading your file.  Either the filename you entered
        was incorrect, or there was a network problem which prevented the file from
        reaching us correctly.  Please try again.</p> --%>
		<p class="alert alert-warning"><fmt:message key="jsp.submit.upload-error.info"/></p>

        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

<%-- HACK: <center> tag needed for broken Netscape 4.78 behaviour --%>
        
        <input class="btn btn-primary col-md-offset-5" type="submit" name="submit_retry" value="<fmt:message key="jsp.submit.upload-error.retry.button"/>" />
        
    </form>

</dspace:layout>
