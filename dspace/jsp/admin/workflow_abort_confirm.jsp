<%--
  - confirm-delete-collection.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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

<%@ page import="org.dspace.app.webui.servlet.admin.WorkflowAbortServlet" %>
<%@ page import="org.dspace.workflow.WorkflowItem" %>
<%@ page import="org.dspace.workflow.WorkflowManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    WorkflowItem workflow = (WorkflowItem) request.getAttribute("workflow");
%>

<dspace:layout title="Abort Workflow" navbar="admin" locbar="link" parentlink="/admin" parenttitle="Administer">

    <H1>Delete Workflow: <%= workflow.getID() %></H1>
    
    <P>Are you sure you want to abort this workflow?  It will return to the user's personal workspace</P>
    
    <UL>
        <LI>Collection:Any items and incomplete submissions in this collections that aren't contained in other collections</LI>
        <LI>Submitter: <%= WorkflowManager.getSubmitter(workflow) %></LI>
        <LI>Title: <%= WorkflowManager.getItemTitle(workflow %></LI>
    </UL>
    
    <form method=POST>
        <input type="hidden" name="workflow_id" value="<%= workflow.getID() %>">
 
        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <input type="submit" name="submit_abort_confirm" value="Abort">
                    </td>
                    <td align="right">
                        <input type="submit" name="submit_cancel" value="Cancel">
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>

