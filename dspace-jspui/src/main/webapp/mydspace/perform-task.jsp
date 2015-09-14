<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Perform task page
  -
  - Attributes:
  -    workflow.item: The workflow item for the task being performed
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

<dspace:layout style="submission" locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.perform-task.title"
               nocache="true">

    <%-- <h1>Perform Task</h1> --%>
    <h1><fmt:message key="jsp.mydspace.perform-task.title"/></h1>
    
<%
    if (workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP1)
    {
%>
	<p><fmt:message key="jsp.mydspace.perform-task.text1">
        <fmt:param><%= collection.getName() %></fmt:param>
         </fmt:message></p>
<%
    }
    else if (workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP2)
    {
%>
	<p><fmt:message key="jsp.mydspace.perform-task.text3">
        <fmt:param><%= collection.getName() %></fmt:param>
	</fmt:message></p>
<%
    }
    else if (workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP3)
    {
%>
	<p><fmt:message key="jsp.mydspace.perform-task.text4">
        <fmt:param><%= collection.getName() %></fmt:param>
    </fmt:message></p>
<%
    }
%>
    
    <dspace:item item="<%= item %>" />

    <p>&nbsp;</p>

    <form action="<%= request.getContextPath() %>/mydspace" method="post">
        <input type="hidden" name="workflow_id" value="<%= workflowItem.getID() %>"/>
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.PERFORM_TASK_PAGE %>"/>
<%
    
    if (workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP1 ||
        workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP2)
    {
%>
                    <div class="input-group">
                    <%-- <input type="submit" name="submit_approve" value="Approve"> --%>
					<span class="input-group-addon">
					<input style="width:150px;" class="btn btn-success" type="submit" name="submit_approve" value="<fmt:message key="jsp.mydspace.general.approve"/>" />
					</span>
                    <span class="form-control">
                    <%-- If you have reviewed the item and it is suitable for inclusion in the collection, select "Approve". --%>
					<fmt:message key="jsp.mydspace.perform-task.instruct1"/>
					</span>
                    </div>
<%
    }
    else
    {
        // Must be an editor (step 3)
%>
                    
                    <div class="input-group">
					<span class="input-group-addon">
					<%-- <input type="submit" name="submit_approve" value="Commit to Archive"> --%>
					<input style="width:150px;" class="btn btn-success" type="submit" name="submit_approve" value="<fmt:message key="jsp.mydspace.perform-task.commit.button"/>" />
					</span>
                    <span class="form-control">					
                    <%-- Once you've edited the item, use this option to commit the
                    item to the archive. --%>
					<fmt:message key="jsp.mydspace.perform-task.instruct2"/>
					</span>
                    </div>
<%
    }

    if (workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP1 ||
        workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP2)
    {
%>
				<div class="input-group">
					<span class="input-group-addon">
                    <input style="width:150px;" class="btn btn-danger" type="submit" name="submit_reject" value="<fmt:message key="jsp.mydspace.general.reject"/>"/>
                    </span>
                    <span class="form-control">
                    <%-- If you have reviewed the item and found it is <strong>not</strong> suitable
                    for inclusion in the collection, select "Reject".  You will then be asked 
                    to enter a message indicating why the item is unsuitable, and whether the
                    submitter should change something and re-submit. --%>
					<fmt:message key="jsp.mydspace.perform-task.instruct3"/>
	        		</span>
	        	</div>	
	        		
<%
    }

    if (workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP2 ||
        workflowItem.getState() == BasicWorkflowService.WFSTATE_STEP3)
    {
%>
				<div class="input-group">
					<span class="input-group-addon">
                    <input style="width:150px;" class="btn btn-primary" type="submit" name="submit_edit" value="<fmt:message key="jsp.mydspace.perform-task.edit.button"/>" />
                    </span>
                    <span class="form-control">
                    <%-- Select this option to correct, amend or otherwise edit the item's metadata. --%>
					<fmt:message key="jsp.mydspace.perform-task.instruct4"/>
					</span>
				</div>	
			
<%
    }
%>
				<div class="input-group">
					<span class="input-group-addon">
                    <input style="width:150px;" class="btn btn-default" type="submit" name="submit_cancel" value="<fmt:message key="jsp.mydspace.perform-task.later.button"/>" />
                    </span>
                    <span class="form-control">
                    <%-- If you wish to leave this task for now, and return to your "My DSpace", use this option. --%>
                    <fmt:message key="jsp.mydspace.perform-task.instruct5"/>
                    </span>
                </div>
                <div class="input-group">
					<span class="input-group-addon">
                    <input style="width:150px;" class="btn btn-default" type="submit" name="submit_pool" value="<fmt:message key="jsp.mydspace.perform-task.return.button"/>" />
                    </span>
                    <span class="form-control">
                    <%-- To return the task to the pool so that another user can perform the task, use this option. --%>
                    <fmt:message key="jsp.mydspace.perform-task.instruct6"/>
                    </span>
                </div>
    </form>
</dspace:layout>
