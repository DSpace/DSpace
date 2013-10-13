<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Show the user a license which they may grant or reject
  -
  - Attributes to pass in:
  -    license          - the license text to display
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
 
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

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

    String license = (String) request.getAttribute("license");
%>

<dspace:layout style="submission"
			   locbar="off"
               navbar="off"
               titlekey="jsp.submit.show-license.title"
               nocache="true">

    <form action="<%= request.getContextPath() %>/submit" method="post" onkeydown="return disableEnterKey(event);">

        <jsp:include page="/submit/progressbar.jsp"/>

	<h1><fmt:message key="jsp.submit.show-license.title" />
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") +\"#license\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></h1>
	<div class="alert alert-info"><fmt:message key="jsp.submit.show-license.info1"/></div>

        <%-- <p><strong>Not granting the license will not delete your submission.</strong>
        Your item will remain in your "My DSpace" page.  You can then either remove
        the submission from the system, or agree to the license later once any
        queries you might have are resolved.</p> --%>
		<p><fmt:message key="jsp.submit.show-license.info2"/></p>

        <pre class="panel panel-primary col-md-10 col-md-offset-1"><%= license %></pre>

        <%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

	    <div class="btn-group col-md-6 col-md-offset-3">
	    	<input class="btn btn-warning col-md-6" type="submit" name="submit_reject" value="<fmt:message key="jsp.submit.show-license.notgrant.button"/>" />
	    	<input class="btn btn-success col-md-6" type="submit" name="submit_grant" value="<fmt:message key="jsp.submit.show-license.grant.button"/>" />
        </div>              
    </form>
</dspace:layout>
