<%--
  - supervise-list.jsp
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
   - This page lists the current supervisory settings for workspace items
   -
   - Attributes:
   -    supervised  - An array of supervised items
   --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.SupervisedItem" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group" %>

<%
    // get the object array out of the request
    SupervisedItem[] supervisedItems = (SupervisedItem[]) request.getAttribute("supervised");
%>

<dspace:layout titlekey="jsp.dspace-admin.supervise-list.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.supervise-list.heading"/></h1>

<h3><fmt:message key="jsp.dspace-admin.supervise-list.subheading"/></h3>

<br/><br/>

<div align="center" />
<%-- form to navigate to the "add supervisory settings" page --%> 
<form method="post" action="">
    <input type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.supervise-list.add.button"/>"/>
    <input type="submit" name="submit_base" value="<fmt:message key="jsp.dspace-admin.supervise-list.back.button"/>"/>
</form>

<table class="miscTable">
    <tr>
        <th class="oddRowOddCol">
            &nbsp;
        </th>
        <th class="oddRowEvenCol">
            <fmt:message key="jsp.dspace-admin.supervise-list.group"/>
        </th>
        <th class="oddRowOddCol">
            <fmt:message key="jsp.dspace-admin.supervise-list.author"/>
        </th>
        <th class="oddRowEvenCol">
            <fmt:message key="jsp.dspace-admin.supervise-list.title"/>
        </th>
        <th class="oddRowOddCol">
            &nbsp;
        </th>
    </tr>
<%
    String row = "even";

    for (int i = 0; i < supervisedItems.length; i++)
    {
        // get title (or "untitled" if not set), author, and supervisors of 
        // the supervised item
        DCValue[] titleArray = supervisedItems[i].getItem().getDC("title", null, Item.ANY);
//        String title = (titleArray.length > 0 ? titleArray[0].value : "Untitled");
        EPerson submitter = supervisedItems[i].getItem().getSubmitter();
        Group[] supervisors = supervisedItems[i].getSupervisorGroups();

        for (int j = 0; j < supervisors.length; j++)
        {
%>

    <tr>
        <td class="<%= row %>RowOddCol">
            <%-- form to navigate to the item policies --%>
            <form action="<%= request.getContextPath() %>/dspace-admin/authorize" method="post">
                <input type="hidden" name="item_id" value="<%=supervisedItems[i].getItem().getID() %>"/>
                <input type="submit" name="submit_item_select" value="<fmt:message key="jsp.dspace-admin.supervise-list.policies.button"/>"/>
            </form>
        </td>
        <td class="<%= row %>RowEvenCol">
            <%= supervisors[j].getName() %>
        </td>
        <td class="<%= row %>RowOddCol">
            <a href="mailto:<%= submitter.getEmail() %>"><%= submitter.getFullName() %></a>
        </td>
        <td class="<%= row %>RowEvenCol">
<%
		if (titleArray.length > 0)
		{
%>
			<%= titleArray[0].value %>
<%
		}
		else
		{
%>
			<fmt:message key="jsp.general.untitled"/>
<%
		}
%>
        </td>
        <td class="<%= row %>RowOddCol">
            <%-- form to request removal of supervisory linking --%>
            <form method="post" action="">
            <input type="hidden" name="gID" value="<%= supervisors[j].getID() %>"/>
            <input type="hidden" name="siID" value="<%= supervisedItems[i].getID() %>"/>
            <input type="submit" name="submit_remove" value="<fmt:message key="jsp.dspace-admin.general.remove"/>"/>
            </form>
        </td>
    </tr> 

<%
        row = (row.equals("even") ? "odd" : "even" );
        }
    }
%>

</table>
</dspace:layout>
