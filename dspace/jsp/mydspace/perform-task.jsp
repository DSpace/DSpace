<%--
  - perform-task.jsp
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
  - Perform task page
  -
  - Attributes:
  -    workflow.item: The workflow item for the task being performed
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

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

<dspace:layout locbar="link" parenttitle="My DSpace" parentlink="/mydspace" title="Perform Task">

    <H1>Perform Task</H1>
    
<%
    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP1)
    {
%>
    <P>The following item has been submitted to collection <strong><%= collection.getMetadata("name") %></strong>.
    Please review the item, check that it meets the criteria for entry into
    the collection.  After reviewing the item, please approve or reject the
    item using the controls at the bottom of the page.</P>
<%
    }
    else if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP2)
    {
%>
    <P>The following item has been submitted to collection <strong><%= collection.getMetadata("name") %></strong>.
    Please review the item, check that it meets the criteria for entry into
    the collection.  After reviewing the item, you may edit the metadata with the
    item, and then approve or reject the item using the controls at the bottom of
    the page.</P>
<%
    }
    else if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP3)
    {
%>
    <P>The following item has been accepted for inclusion in collection
    <strong><%= collection.getMetadata("name") %></strong>.  Please perform any
    necessary edits of the metadata to conform with the standards of the collection,
    and then commit the item to the archive using the controls at the bottom
    of the page.</P>
<%
    }
%>
    
    <dspace:item item="<%= item %>" />

    <P>&nbsp;</P>

    <form action="<%= request.getContextPath() %>/mydspace" method=POST>
        <input type="hidden" name="workflow_id" value="<%= workflowItem.getID() %>">
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.PERFORM_TASK_PAGE %>">
        <table class="miscTable" width 80%>
<%
    String row = "odd";
    
    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP1 ||
        workflowItem.getState() == WorkflowManager.WFSTATE_STEP2)
    {
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    If you have reviewed the item and it is suitable for inclusion in the collection, select "Approve".
                </td>
                <td class="<%= row %>RowEvenCol" valign=middle>
                    <input type="submit" name="submit_approve" value="Approve">
                </td>
            </tr>
<%
    }
    else
    {
        // Must be an editor (step 3)
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    Once you've edited the item, use this option to commit the
                    item to the archive.
                </td>
                <td class="<%= row %>RowEvenCol" valign=middle>
                    <input type="submit" name="submit_approve" value="Commit to Archive">
                </td>
            </tr>
<%
    }
    row = "even";

    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP1 ||
        workflowItem.getState() == WorkflowManager.WFSTATE_STEP2)
    {
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    If you have reviewed the item and found it is <strong>not</strong> suitable
                    for inclusion in the collection, select "Reject".  You will then be asked 
                    to enter a message indicating why the item is unsuitable, and whether the
                    submitter should change something and re-submit.
                </td>
                <td class="<%= row %>RowEvenCol" valign=middle>
                    <input type=submit name=submit_reject value="Reject" />
                </td>
            </tr>
<%
        row = ( row.equals( "odd" ) ? "even" : "odd" );
    }

    if (workflowItem.getState() == WorkflowManager.WFSTATE_STEP2 ||
        workflowItem.getState() == WorkflowManager.WFSTATE_STEP3)
    {
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    Select this option to correct, amend or otherwise edit the item's metadata.
                </td>
                <td class="<%= row %>RowEvenCol" valign=middle>
                    <input type=submit name=submit_edit value="Edit Metadata" />
                </td>
            </tr>
<%
        row = (row.equals( "odd" ) ? "even" : "odd");
    }
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    If you wish to leave this task for now, and return to your "My DSpace", use this option.
                </td>
                <td class="<%= row %>RowEvenCol" valign=middle>
                    <input type=submit name=submit_cancel value="Do Later" />
                </td>
            </tr>
<%
    row = (row.equals( "odd" ) ? "even" : "odd");
%>
            <tr>
                <td class="<%= row %>RowOddCol">
                    To return the task to the pool so that another user can perform the task, use this option.
                </td>
                <td class="<%= row %>RowEvenCol" valign=middle>
                    <input type=submit name=submit_pool value="Return Task to Pool" />
                </td>
            </tr>
        </table>
    </form>
</dspace:layout>
