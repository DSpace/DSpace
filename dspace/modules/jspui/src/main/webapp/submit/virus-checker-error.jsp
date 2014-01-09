<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - This is a variant of upload-error.jsp
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
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

<dspace:layout locbar="off"
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
		<p><fmt:message key="jsp.submit.virus-checker-error.info"/></p>

        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

<%-- HACK: <center> tag needed for broken Netscape 4.78 behaviour --%>
        <center>
            <p>
                <input type="submit" name="submit_retry" value="<fmt:message key="jsp.submit.upload-error.retry.button"/>" />
            </p>
        </center>
    </form>

</dspace:layout>
