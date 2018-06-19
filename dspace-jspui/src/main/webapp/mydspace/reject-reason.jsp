<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Get a reason for rejecting a submission (or just reject reason itself -
  -  this is JSP programming after all!)
  -
  - Attributes:
  -    workflow.item: The workflow item being rejected
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.workflowbasic.BasicWorkflowItem" %>

<%
    BasicWorkflowItem workflowItem =
        (BasicWorkflowItem) request.getAttribute("workflow.item");
%>

<dspace:layout style="submission" locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               title="reject-reason.title"
               nocache="true">

    <%-- <h1>Enter Reason for Rejection</h1> --%>
	<h1><fmt:message key="jsp.mydspace.reject-reason.title"/></h1>

    <%-- <p>Please enter the reason you are rejecting the submission into the box
    below.  Please indicate in your message whether the submitter should fix
    a problem and resubmit.</p> --%>
	<p><fmt:message key="jsp.mydspace.reject-reason.text1"/></p>
    
    <form action="<%= request.getContextPath() %>/mydspace" method="post">
        <input type="hidden" name="workflow_id" value="<%= workflowItem.getID() %>"/>
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.REJECT_REASON_PAGE %>"/>
        <textarea class="form-control" rows="6" cols="50" name="reason"></textarea>
		<br/>
		<div class="row container">
		<%-- <input type="submit" name="submit_cancel" value="Cancel Rejection" /> --%>
		<input class="btn btn-default" type="submit" name="submit_cancel" value="<fmt:message key="jsp.mydspace.reject-reason.cancel.button"/>" />
					
        <%-- <input type="submit" name="submit_send" value="Reject Item" /> --%>
	 	<input class="btn btn-danger pull-right" type="submit" name="submit_send" value="<fmt:message key="jsp.mydspace.reject-reason.reject.button"/>" />
	 	</div>
    </form>
</dspace:layout>
