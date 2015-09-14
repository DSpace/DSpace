<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Preview task page
  -
  -   workflow.item:  The workflow item for the task they're performing
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.workflowbasic.BasicWorkflowItem" %>
<%@ page import="org.dspace.workflowbasic.service.BasicWorkflowService" %>

<%
    BasicWorkflowItem workflowItem =
        (BasicWorkflowItem) request.getAttribute("workflow.item");

    Collection collection = workflowItem.getCollection();
    Item item = workflowItem.getItem();
%>

<dspace:layout style="submission"
			   locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.preview-task.title"
               nocache="true">

	<h1><fmt:message key="jsp.mydspace.preview-task.title"/></h1>
    
<%
    if (workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP1POOL)
    {
%>
	<p><fmt:message key="jsp.mydspace.preview-task.text1"> 
        <fmt:param><%= collection.getName() %></fmt:param>
    </fmt:message></p>
<%
    }
    else if(workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP2POOL)
    {
%>    
	<p><fmt:message key="jsp.mydspace.preview-task.text3"> 
        <fmt:param><%= collection.getName() %></fmt:param>
    </fmt:message></p>
<%
    }
    else if(workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP3POOL)
    {
%>
	<p><fmt:message key="jsp.mydspace.preview-task.text4"> 
        <fmt:param><%= collection.getName() %></fmt:param>
    </fmt:message></p>
<%
    }
%>
    
    <dspace:item item="<%= item %>" />

    <form action="<%= request.getContextPath() %>/mydspace" method="post">
        <input type="hidden" name="workflow_id" value="<%= workflowItem.getID() %>"/>
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.PREVIEW_TASK_PAGE %>"/>
		<input class="btn btn-default col-md-2" type="submit" name="submit_cancel" value="<fmt:message key="jsp.mydspace.general.cancel"/>" />
		<input class="btn btn-primary col-md-2 pull-right" type="submit" name="submit_start" value="<fmt:message key="jsp.mydspace.preview-task.accept.button"/>" />
    </form>
</dspace:layout>
