<%--
  - workflow-abort-confirm.jsp
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
  - Confirm abort of a workflow
  -
  - Attributes:
  -    workflow   - WorkflowItem representing the workflow in question
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.app.webui.servlet.admin.WorkflowAbortServlet" %>
<%@ page import="org.dspace.workflow.WorkflowItem" %>
<%@ page import="org.dspace.workflow.WorkflowManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    WorkflowItem workflow = (WorkflowItem) request.getAttribute("workflow");
%>

<dspace:layout titlekey="jsp.dspace-admin.workflow-abort-confirm.title"
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
    <ul>
        <%-- <li>Collection: <%= workflow.getCollection().getMetadata("name") %></li> --%>
        <li><fmt:message key="jsp.dspace-admin.workflow-abort-confirm.collection">
            <fmt:param><%= workflow.getCollection().getMetadata("name") %></fmt:param>
        </fmt:message></li>
        <%-- <li>Submitter: <%= WorkflowManager.getSubmitterName(workflow) %></li> --%>
        <li><fmt:message key="jsp.dspace-admin.workflow-abort-confirm.submitter">
            <fmt:param><%= WorkflowManager.getSubmitterName(workflow) %></fmt:param>
        </fmt:message></li>
        <%-- <li>Title: <%= WorkflowManager.getItemTitle(workflow) %></li> --%>
        <li><fmt:message key="jsp.dspace-admin.workflow-abort-confirm.item-title">
            <fmt:param><%= WorkflowManager.getItemTitle(workflow) %></fmt:param>
        </fmt:message></li>
    </ul>
    <form method="post" action="">
        <input type="hidden" name="workflow_id" value="<%= workflow.getID() %>"/> 
        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <%-- <input type="submit" name="submit_abort_confirm" value="Abort"/> --%>
                        <input type="submit" name="submit_abort_confirm" value="<fmt:message key="jsp.dspace-admin.workflow-abort-confirm.button"/>" />
                    </td>
                    <td align="right">
                        <%-- <input type="submit" name="submit_cancel" value="Cancel"/> --%>
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>

