<%--
  - preview-task.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
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
<%@ page import="org.dspace.workflow.WorkflowItem" %>
<%@ page import="org.dspace.workflow.WorkflowManager" %>

<%
    WorkflowItem workflowItem =
        (WorkflowItem) request.getAttribute("workflow.item");

    Collection collection = workflowItem.getCollection();
    Item item = workflowItem.getItem();
%>

<dspace:layout locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.preview-task.title"
               nocache="true">

	<h1><fmt:message key="jsp.mydspace.preview-task.title"/></h1>
    
<%
    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP1POOL)
    {
%>
	<p><fmt:message key="jsp.mydspace.preview-task.text1"> 
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
    </fmt:message></p>
<%
    }
    else if(workflowItem.getState() == WorkflowManager.WFSTATE_STEP2POOL)
    {
%>    
	<p><fmt:message key="jsp.mydspace.preview-task.text3"> 
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
    </fmt:message></p>
<%
    }
    else if(workflowItem.getState() == WorkflowManager.WFSTATE_STEP3POOL)
    {
%>
	<p><fmt:message key="jsp.mydspace.preview-task.text4"> 
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
    </fmt:message></p>
<%
    }
%>
    
    <dspace:item item="<%= item %>" />

    <form action="<%= request.getContextPath() %>/mydspace" method="post">
        <input type="hidden" name="workflow_id" value="<%= workflowItem.getID() %>"/>
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.PREVIEW_TASK_PAGE %>"/>
        <table border="0" width="90%" cellpadding="10" align="center">
            <tr>
                <td align="left">
		    <input type="submit" name="submit_start" value="<fmt:message key="jsp.mydspace.preview-task.accept.button"/>" />
                </td>
                <td align="right">
	  	    <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.mydspace.general.cancel"/>" />
                </td>
            </tr>
        </table>
    </form>
</dspace:layout>
