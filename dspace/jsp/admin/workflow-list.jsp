<%--
  - workflow_list.jsp
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
  - Display list of Workflows, with 'abort' buttons next to them
  -
  - Attributes:
  -
  -   workflows - WorkflowItem [] to choose from
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.administer.DCType" %>
<%@ page import="org.dspace.workflow.WorkflowManager" %>
<%@ page import="org.dspace.workflow.WorkflowItem" %>
<%@ page import="org.dspace.content.Collection" %>


<%
    WorkflowItem[] workflows =
        (WorkflowItem[]) request.getAttribute("workflows");
%>

<dspace:layout title="Workflow Abort"
               navbar="admin"
               locbar="link"
               parentlink="/admin"
               parenttitle="Administer">

    <h1>Currently Active Workflows</h1>

    <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol"> <strong>ID</strong></th>
            <th class="oddRowEvenCol"><strong>Collection</strong></th>
            <th class="oddRowOddCol"> <strong>Submitter</strong></th>
            <th class="oddRowEvenCol"><strong>Title</strong></th>
            <th class="oddRowOddCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    for (int i = 0; i < workflows.length; i++)
    {
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= workflows[i].getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= workflows[i].getCollection().getMetadata("name") %>
                </td>
                <td class="<%= row %>RowOddCol">
                    <%= WorkflowManager.getSubmitterName(workflows[i])   %>
                </td>
                <td class="<%= row %>RowEvenCol">
                    <%= WorkflowManager.getItemTitle(workflows[i])       %>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="hidden" name="workflow_id" value="<%= workflows[i].getID() %>">
                    <input type="submit" name="submit_abort" value="Abort...">
                </td>
            </tr>
        </form>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
        
</dspace:layout>
