<%--
  - main.jsp
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
  - Main My DSpace page
  -
  -
  - Attributes:
  -    mydspace.user:    current user (EPerson)
  -    workspace.items:  WorkspaceItem[] array for this user
  -    workflow.items:   WorkflowItem[] array of submissions from this user in
  -                      workflow system
  -    workflow.owned:   WorkflowItem[] array of tasks owned
  -    workflow.pooled   WorkflowItem[] array of pooled tasks
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.SupervisedItem" %>
<%@ page import="org.dspace.content.WorkspaceItem" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.workflow.WorkflowItem" %>
<%@ page import="org.dspace.workflow.WorkflowManager" %>

<%
    EPerson user = (EPerson) request.getAttribute("mydspace.user");

    WorkspaceItem[] workspaceItems =
        (WorkspaceItem[]) request.getAttribute("workspace.items");

    WorkflowItem[] workflowItems =
        (WorkflowItem[]) request.getAttribute("workflow.items");

    WorkflowItem[] owned =
        (WorkflowItem[]) request.getAttribute("workflow.owned");

    WorkflowItem[] pooled =
        (WorkflowItem[]) request.getAttribute("workflow.pooled");

    SupervisedItem[] supervisedItems =
        (SupervisedItem[]) request.getAttribute("supervised.items");
%>

<dspace:layout title="My DSpace" nocache="true">

    <table width="100%" border=0>
        <tr>
            <td align=left>
                <H1>
                    My DSpace: <%= user.getFullName() %>
                </H1>
            </td>
            <td align=right class=standard>
                <dspace:popup page="/help/index.html#mydspace">Help...</dspace:popup>
            </td>
        <tr>
    </table>
  
<%-- Task list:  Only display if the user has any tasks --%>
<%
    if (owned.length > 0)
    {
%>
    <H2>Owned Tasks</H2>

    <P class="submitFormHelp">
        Below are the current tasks that you have chosen to do.
    </P>

    <table class=miscTable align=center>
        <tr>
            <th class=oddRowOddCol>Task</th>
            <th class=oddRowEvenCol>Item</th>
            <th class=oddRowOddCol>Submitted To</th>
            <th class=oddRowEvenCol>Submitted By</th>
            <th class=oddRowOddCol>&nbsp;</th>
            <th class=oddRowEvenCol>&nbsp;</th>
        </tr>
<%
        // even or odd row:  Starts even since header row is odd (1).  Toggled
        // between "odd" and "even" so alternate rows are light and dark, for
        // easier reading.
        String row = "even";

        for (int i = 0; i < owned.length; i++)
        {
            DCValue[] titleArray =
                owned[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                                                  : "Untitled" );
            EPerson submitter = owned[i].getItem().getSubmitter();
%>
        <tr>
            <form action="<%= request.getContextPath() %>/mydspace" method=post>
                <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>">
                <input type="hidden" name="workflow_id" value="<%= owned[i].getID() %>">
                <td class="<%= row %>RowOddCol">
<%
            switch (owned[i].getState())
            {
            case WorkflowManager.WFSTATE_STEP1: %>Review Submission<% break;
            case WorkflowManager.WFSTATE_STEP2: %>Check Submission<% break;
            case WorkflowManager.WFSTATE_STEP3: %>Final Edit of Submission<% break;
            }
%>
                </td>
                <td class="<%= row %>RowEvenCol"><%= Utils.addEntities(title) %></td>
                <td class="<%= row %>RowOddCol"><%= owned[i].getCollection().getMetadata("name") %></td>
                <td class="<%= row %>RowEvenCol"><A HREF="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></A></td>
                <td class="<%= row %>RowOddCol">
                    <input type="submit" name="submit_perform" value="Perform This Task">
                </td>
                <td class="<%= row %>RowEvemCol">
                    <input type="submit" name="submit_return" value="Return to Pool">
                </td>
            </form>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>
    </table>
<%
    }
  
    // Pooled tasks - only show if there are any
    if (pooled.length > 0)
    {
%>
    <H2>Tasks in the Pool</H2>

    <P class="submitFormHelp">
        Below are tasks in the task pool that have been assigned to you.
    </P>

    <table class=miscTable align=center>
        <tr>
            <th class=oddRowOddCol>Task</th>
            <th class=oddRowEvenCol>Item</th>
            <th class=oddRowOddCol>Submitted To</th>
            <th class=oddRowEvenCol>Submitted By</th>
            <th class=oddRowOddCol>&nbsp;</th>
        </tr>
<%
        // even or odd row:  Starts even since header row is odd (1).  Toggled
        // between "odd" and "even" so alternate rows are light and dark, for
        // easier reading.
        String row = "even";

        for (int i = 0; i < pooled.length; i++)
        {
            DCValue[] titleArray =
                pooled[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                                                  : "Untitled");
            EPerson submitter = pooled[i].getItem().getSubmitter();
%>
        <tr>
            <form action="<%= request.getContextPath() %>/mydspace" method=post>
                <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>">
                <input type="hidden" name="workflow_id" value="<%= pooled[i].getID() %>">
                <td class="<%= row %>RowOddCol">
<%
            switch (pooled[i].getState())
            {
            case WorkflowManager.WFSTATE_STEP1POOL: %>Review Submission<% break;
            case WorkflowManager.WFSTATE_STEP2POOL: %>Check Submission<% break;
            case WorkflowManager.WFSTATE_STEP3POOL: %>Final Edit of Submission<% break;
            }
%>
                </td>
                <td class="<%= row %>RowEvenCol"><%= Utils.addEntities(title) %></td>
                <td class="<%= row %>RowOddCol"><%= pooled[i].getCollection().getMetadata("name") %></td>
                <td class="<%= row %>RowEvenCol"><A HREF="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></A></td>
                <td class="<%= row %>RowOddCol">
                    <input type="submit" name="submit_claim" value="Take Task">
                </td>
            </form>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even");
        }
%>
    </table>
<%
    }
%>

    <form action="<%= request.getContextPath() %>/mydspace" method=post>
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>">
        <center>
            <table border=0 width="70%">
                <tr>
                    <td align=left>
                        <input type=submit name="submit_new" value="Start a New Submission">
                    </td>
                    <td align=right>
                        <input type=submit name="submit_own" value="View Accepted Submissions">
                    </td>
                </tr>
            </table>
        </center>
    </form>

    <P align="center"><A HREF="<%= request.getContextPath() %>/subscribe">See Your Subscriptions</A></P>

<%
    // Display workspace items (authoring or supervised), if any
    if (workspaceItems.length > 0 || supervisedItems.length > 0)
    {
        // even or odd row:  Starts even since header row is odd (1)
        String row = "even";
%>
    <H2>WorkSpace</H2>

    <p>This section is for use in the continued authoring of your document.</p>

    <table class=miscTable align=center>
        <tr>
            <th class=oddRowOddCol>&nbsp;</th>
            <th class=oddRowEvenCol>Submitted by</th>
            <th class=oddRowOddCol>Title</th>
            <th class=oddRowEvenCol>Submitted to</th>
            <th class=oddRowOddCol>&nbsp;</th>
        </tr>
<%
        if (supervisedItems.length > 0 && workspaceItems.length > 0) 
        {
%>
        <tr>
            <th colspan="5">
                Authoring
            </th>
        </tr>
<%
        }

        for (int i = 0; i < workspaceItems.length; i++)
        {
            DCValue[] titleArray =
                workspaceItems[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                                                  : "Untitled");
            EPerson submitter = workspaceItems[i].getItem().getSubmitter();
%>
        <tr>
            <form action="<%= request.getContextPath() %>/workspace" method="post">
            <td class="<%= row %>RowOddCol">
                <input type="hidden" name="workspace_id" value="<%= workspaceItems[i].getID() %>"/>
                <input type="submit" name="submit_open" value="Open"/>
            </td>
            </form>
            <td class="<%= row %>RowEvenCol">
                <A HREF="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></A>
            </td>
            <td class="<%= row %>RowOddCol"><%= Utils.addEntities(title) %></td>
            <td class="<%= row %>RowEvenCol"><%= workspaceItems[i].getCollection().getMetadata("name") %></td>
            <form action="<%= request.getContextPath() %>/mydspace" method="post">
            <td class="<%= row %>RowOddCol">
                <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>"/>
                <input type="hidden" name="workspace_id" value="<%= workspaceItems[i].getID() %>"/>
                <input type="submit" name="submit_delete" value="Remove"/>
            </td>
            </form>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>

<%-- Start of the Supervisors workspace list --%>
<%
        if (supervisedItems.length > 0) 
        {
%>
        <tr>
            <th colspan="5">
                Supervising
            </th>
        </tr>
<%
        }

        for (int i = 0; i < supervisedItems.length; i++)
        {
            DCValue[] titleArray =
                supervisedItems[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                                                  : "Untitled");
            EPerson submitter = supervisedItems[i].getItem().getSubmitter();
%>
        <tr>
            <form action="<%= request.getContextPath() %>/workspace" method="post">
            <td class="<%= row %>RowOddCol">
                <input type="hidden" name="workspace_id" value="<%= supervisedItems[i].getID() %>"/>
                <input type="submit" name="submit_open" value="Open"/>
            </td>
            </form>
            <td class="<%= row %>RowEvenCol">
                <A HREF="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></A>
            </td>
            <td class="<%= row %>RowOddCol"><%= Utils.addEntities(title) %></td>
            <td class="<%= row %>RowEvenCol"><%= supervisedItems[i].getCollection().getMetadata("name") %><
/td>
            <form action="<%= request.getContextPath() %>/mydspace" method="post">
            <td class="<%= row %>RowOddCol">
                <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>"/>
                <input type="hidden" name="workspace_id" value="<%= supervisedItems[i].getID() %>"/>
                <input type="submit" name="submit_delete" value="Remove"/>
            </td>
            </form>
        </tr>
<%
            row = (row.equals("even") ? "odd" : "even" );
        }
%>
    </table>
<%
    }
%>

<%
    // Display workflow items, if any
    if (workflowItems.length > 0)
    {
        // even or odd row:  Starts even since header row is odd (1)
        String row = "even";
%>
    <H2>Submissions In Workflow Process</H2>

    <table class=miscTable align=center>
        <tr>
            <th class=oddRowOddCol>Title</th>
            <th class=oddRowEvenCol>Submitted to</th>
        </tr>
<%
        for (int i = 0; i < workflowItems.length; i++)
        {
            DCValue[] titleArray =
                workflowItems[i].getItem().getDC("title", null, Item.ANY);
            String title = (titleArray.length > 0 ? titleArray[0].value
                                                  : "Untitled" );
%>
        <form action="<%= request.getContextPath() %>/mydspace" method=post>
            <tr>
                <input type="hidden" name="step" value="<%= MyDSpaceServlet.MAIN_PAGE %>">
                <input type="hidden" name="workflow_id" value="<%= workflowItems[i].getID() %>">
                <td class="<%= row %>RowOddCol"><%= Utils.addEntities(title) %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= workflowItems[i].getCollection().getMetadata("name") %>
                </td>
            </tr>
        </form>
<%
      row = (row.equals("even") ? "odd" : "even" );
    }
%>
    </table>
<%
  }
%>
</dspace:layout>
