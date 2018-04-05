<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm abort of a workflow
  -
  - Attributes:
  -    workflow   - WorkflowItem representing the workflow in question
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.app.webui.servlet.admin.WorkflowAbortServlet" %>
<%@ page import="org.dspace.workflowbasic.BasicWorkflowItem" %>
<%@ page import="org.dspace.workflowbasic.BasicWorkflowServiceImpl" %>
<%@ page import="org.dspace.workflowbasic.service.BasicWorkflowService" %>
<%@ page import="org.dspace.workflowbasic.factory.BasicWorkflowServiceFactory" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    BasicWorkflowService basicWorkflowService = BasicWorkflowServiceFactory.getInstance().getBasicWorkflowService();
    BasicWorkflowItem workflow = (BasicWorkflowItem) request.getAttribute("workflow");
    request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout style="submission" 
			   titlekey="jsp.dspace-admin.workflow-abort-confirm.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Delete Workflow: <%= workflow.getID() %></h1> --%>
    
<h1><fmt:message key="jsp.dspace-admin.workflow-abort-confirm.heading">
    <fmt:param><%= workflow.getID() %></fmt:param>
</fmt:message></h1>   
    <%-- <p>Are you sure you want to abort this workflow?  It will return to the user's personal workspace</p> --%>
    <p><fmt:message key="jsp.dspace-admin.workflow-abort-confirm.warning"/></p>
    	<div class="row">
        <%-- <li>Collection: <%= workflow.getCollection().getMetadata("name") %></li> --%>
        <span class="col-md-4"><fmt:message key="jsp.dspace-admin.workflow-abort-confirm.collection">
            <fmt:param><%= workflow.getCollection().getName() %></fmt:param>
        </fmt:message></span>
        </div>
        <div class="row">
        <%-- <li>Submitter: <%= WorkflowManager.getSubmitterName(workflow) %></li> --%>
        <span class="col-md-4"><fmt:message key="jsp.dspace-admin.workflow-abort-confirm.submitter">
            <fmt:param><%= basicWorkflowService.getSubmitterName(workflow) %></fmt:param>
        </fmt:message></span>
        </div>
        <div class="row">
        <%-- <li>Title: <%= WorkflowManager.getItemTitle(workflow) %></li> --%>
        <span class="col-md-4"><fmt:message key="jsp.dspace-admin.workflow-abort-confirm.item-title">
            <fmt:param><%= basicWorkflowService.getItemTitle(workflow) %></fmt:param>
        </fmt:message></span>
    	</div>
    <form method="post" action="">
        <input type="hidden" name="workflow_id" value="<%= workflow.getID() %>"/> 

                        <%-- <input type="submit" name="submit_abort_confirm" value="Abort"/> --%>
                        <input class="btn btn-default" type="submit" name="submit_abort_confirm" value="<fmt:message key="jsp.dspace-admin.workflow-abort-confirm.button"/>" />
                    
                        <%-- <input type="submit" name="submit_cancel" value="Cancel"/> --%>
                        <input class="btn btn-default" type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                    
    </form>
</dspace:layout>

