<%--
  - remove-item.jsp
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
  - Remove Item page
  -
  -  Attributes:
  -      workspace.item - the workspace item the user wishes to delete
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.MyDSpaceServlet" %>
<%@ page import="org.dspace.ingest.WorkspaceItem" %>

<%
    WorkspaceItem wi = (WorkspaceItem) request.getAttribute("workspace.item");
%>

<dspace:layout locbar="link" parenttitle="My DSpace" parentlink="/mydspace" title="Remove Item">

    <H1>Remove Item</H1>
    
    <P>Are you sure you want to remove the following incomplete item?</P>

    <dspace:item item="<%= wi.getItem() %>" />

    <form action="<%= request.getContextPath() %>/mydspace" method=post>
        <input type="hidden" name="workspace_id" value="<%= wi.getID() %>">
        <input type="hidden" name="step" value="<%= MyDSpaceServlet.REMOVE_ITEM_PAGE %>">

        <table align=center border=0 width=90%>
            <tr>
                <td align=left>
                    <input type="submit" name="submit_delete" value="Remove the Item">
                </td>
                <td align=right>
                    <input type="submit" name="submit_cancel" value="Cancel Removal">
                </td>
            </tr>
        </table>
    </form>
</dspace:layout>
