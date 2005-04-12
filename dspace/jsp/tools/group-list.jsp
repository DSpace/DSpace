<%--
  - group_list.jsp
  -
  - $Id$
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
  - Display list of Groups, with 'edit' and 'delete' buttons next to them
  -
  - Attributes:
  -
  -   groups - Group [] of groups to work on
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
    
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group" %>

<%
    Group[] groups =
        (Group[]) request.getAttribute("groups");
%>

<dspace:layout titlekey="jsp.tools.group-list.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

  <table width=95%>
    <tr>
      <td align=left>
    <%--  <h1>Group Editor</h1> --%>
    <h1><fmt:message key="jsp.tools.group-list.title"/></h1>
      </td>
      <td align="right" class="standard">
        <%-- <dspace:popup page="/help/site-admin.html#groups">Help...</dspace:popup> --%>
		<dspace:popup page="/help/site-admin.html#groups"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

    <%-- <P>Note that you do not need to manually add users to the "anonymous"
    group - all users are members implicitly.</P> --%>
	<P><fmt:message key="jsp.tools.group-list.note1"/></P>

    <%-- <P>Warning - if you try to delete a group that is referred to by an
    authorization policy or is a workflow group you will get an internal
    server error.</P> --%>
	<P><fmt:message key="jsp.tools.group-list.note2"/></P>

    <p align="center">
        <form method=POST>
            <%-- <input type="submit" name="submit_add" value="Create New Group"> --%>
			<input type="submit" name="submit_add" value="<fmt:message key="jsp.tools.group-list.create.button"/>">
        </form>
    </p>

    <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol"><strong>ID</strong></th>
            <%-- <th class="oddRowEvenCol"><strong>Name</strong></th> --%>
			<th class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.group-list.name"/></strong></th>
            <th class="oddRowOddCol">&nbsp;</th>
            <th class="oddRowEvenCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    for (int i = 0; i < groups.length; i++)
    {
%>
        <form method=POST>
            <tr>
                <td class="<%= row %>RowOddCol"><%= groups[i].getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= groups[i].getName() %>
                </td>
                <td class="<%= row %>RowOddCol">
                    <input type="hidden" name="group_id" value="<%= groups[i].getID() %>">
                    <%-- <input type="submit" name="submit_edit" value="Edit..."> --%>
					<input type="submit" name="submit_edit" value="<fmt:message key="jsp.tools.group-list.edit.button"/>">
                </td>
                <td class="<%= row %>RowEvenCol">
                    <%-- <input type="submit" name="submit_group_delete" value="Delete"> --%>
					<input type="submit" name="submit_group_delete" value="<fmt:message key="jsp.tools.group-list.delete.button"/>">
                </td>
            </tr>
        </form>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
</dspace:layout>
