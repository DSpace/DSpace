<%--
  - eperson-list.jsp
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
  - Display list of E-people, with pagination
  -
  - Attributes:
  -
  -   epeople    - EPerson[] - all epeople to browse
  -   sortby     - Integer - field to sort by (constant from EPerson.java)
  -   first      - Integer - index of first eperson to display
  -   multiple   - if non-null, this is for selecting multiple epeople
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.EPerson" %>

<%
	int PAGESIZE = 50;

    EPerson[] epeople =
        (EPerson[]) request.getAttribute("epeople");
    int sortBy = ((Integer)request.getAttribute("sortby" )).intValue();
    int first = ((Integer)request.getAttribute("first")).intValue();
	boolean multiple = (request.getAttribute("multiple") != null);

	// Make sure we won't run over end of list
	int last = first + PAGESIZE;
	if (last >= epeople.length) last = epeople.length - 1;

	// Index of first eperson on last page
	int jumpEnd = ((epeople.length - 1) / PAGESIZE) * PAGESIZE;

	// Now work out values for next/prev page buttons
	int jumpFiveBack = first - PAGESIZE * 5;
	if (jumpFiveBack < 0) jumpFiveBack = 0;

	int jumpOneBack = first - PAGESIZE;
	if (jumpOneBack < 0) jumpOneBack = 0;

	int jumpOneForward = first + PAGESIZE;
	if (jumpOneForward > epeople.length) jumpOneForward = first;

	int jumpFiveForward = first + PAGESIZE * 5;
	if (jumpFiveForward > epeople.length) jumpFiveForward = jumpEnd;

	// What's the link?
	String sortByParam = "lastname";
	if (sortBy == EPerson.EMAIL) sortByParam = "email";
	if (sortBy == EPerson.ID) sortByParam = "id";

	String jumpLink = request.getContextPath() + "/tools/eperson-list?multiple=" + multiple + "&sortby=" + sortByParam + "&first=";
	String sortLink = request.getContextPath() + "/tools/eperson-list?multiple=" + multiple + "&first=" + first + "&sortby=";
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN" "http://www.w3.org/TR/REC-html40/loose.dtd">
<HTML>
    <head>
        <%-- <title>Select E-people</title> --%>
        </title><fmt:message key="jsp.tools.eperson-list.title"/></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <link rel="stylesheet" href="<%= request.getContextPath() %>/styles.css.jsp">
        <link rel="shortcut icon" href="<%= request.getContextPath() %>/favicon.ico" type="image/x-icon">

<SCRIPT LANGUAGE="JavaScript">
<!-- Begin

// Add the selected items to main e-people list by calling method of parent
function addEPerson(id, email, name)
{
	self.opener.addEPerson(id, email, name);
}

// Clear selected items from main e-people list
function clearEPeople()
{
	var list = self.opener.document.forms[1].eperson_id;
	while (list.options.length > 0)
	{
		list.options[0] = null;
	}
}

// End -->
</script>
	</head>
	<body class="pageContents">

    <%-- <h3>E-people <%= first + 1 %>-<%= last + 1 %> of <%= epeople.length %></H3> --%>
	<h3><fmt:message key="jsp.tools.eperson-list.heading">
        <fmt:param><%= first + 1 %></fmt:param>
        <fmt:param><%= last + 1 %></fmt:param>
        <fmt:param><%= epeople.length %></fmt:param>
    </fmt:message></H3>

<%
	if (multiple)
	{ %>
	<%-- <P class="submitFormHelp">Clicking on the 'Add' button next to an e-person will add that
	e-person to the list on the main form. </P> --%>
	<P class="submitFormHelp"><fmt:message key="jsp.tools.eperson-list.info1"/></P>
<%  } %>

<%-- Controls for jumping around list--%>
	<table width="99%">
		<tr>
		 <%--   <td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %>0">First</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpFiveBack %>">&lt; 5 Pages</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpOneBack %>">&lt; 1 Page</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpOneForward %>">1 Page &gt;</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpFiveForward %>">5 Pages &gt;</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpEnd %>">Last</A></strong></small></td> --%>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %>0"><fmt:message key="jsp.tools.eperson-list.jump.first"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpFiveBack %>"><fmt:message key="jsp.tools.eperson-list.jump.five-back"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpOneBack %>"><fmt:message key="jsp.tools.eperson-list.jump.one-back"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpOneForward %>"><fmt:message key="jsp.tools.eperson-list.jump.one-forward"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpFiveForward %>"><fmt:message key="jsp.tools.eperson-list.jump.five-forward"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpEnd %>"><fmt:message key="jsp.tools.eperson-list.jump.last"/></A></strong></small></td>
		</tr>
	</table>
<br>

	<form method=GET> <%-- Will never actually be posted, it's just so buttons will appear --%>

    <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol">&nbsp;</th>
            <th class="oddRowEvenCol"><%
                if (sortBy == EPerson.ID)
                {
                    %><strong><fmt:message key="jsp.tools.eperson-list.th.id.sortedby" /></strong><%
                }
                else
                {
                    %><a href="<%= sortLink %>id"><fmt:message key="jsp.tools.eperson-list.th.id" /></a><%
                }
            %></th>
            <th class="oddRowOddCol"><%
                if (sortBy == EPerson.EMAIL)
                {
                    %><strong><fmt:message key="jsp.tools.eperson-list.th.email.sortedby" /></strong><%
                }
                else
                {
                    %><a href="<%= sortLink %>email"><fmt:message key="jsp.tools.eperson-list.th.email" /></a><%
                }
            %></th>
            <%-- <th class="oddRowEvenCol"><%= sortBy == EPerson.LASTNAME ? "<strong>Last Name &uarr;</strong>" : "<A HREF=\"" + sortLink + "lastname\">Last Name</A>" %></th> --%>
            <th class="oddRowEvenCol"><%
                if (sortBy == EPerson.LASTNAME)
                {
                    %><fmt:message key="jsp.tools.eperson-list.th.lastname.sortedby" /><%
                }
                else
                {
                    %><a href="<%= sortLink %>lastname"><fmt:message key="jsp.tools.eperson-list.th.lastname" /></a><%
                }
            %></th>

            <%-- <th class="oddRowOddCol">First Name</th> --%>
            <th class="oddRowOddCol"><fmt:message key="jsp.tools.eperson-list.th.firstname"/></th>
        </tr>
<%
    String row = "even";

	// If this is a dialogue to select a *single* e-person, we want
	// to clear any existing entry in the e-person list, and
	// to close this window when a 'select' button is clicked
	String clearList = (multiple ? "" : "clearEPeople();");
	String closeWindow = (multiple ? "" : "window.close();");


    for (int i = first; i <= last; i++)
    {
        EPerson e = epeople[i];
		// Make sure no quotes in full name will mess up our Javascript
        String fullname = e.getFullName().replace('\'', ' ');
%>
        <tr>
			<td class="<%= row %>RowOddCol">
			    <input type=button value="<%
	if (multiple) { %><fmt:message key="jsp.tools.general.add"/><% }
	else {          %><fmt:message key="jsp.tools.general.select"/><% } %>" onClick="javascript:<%= clearList %>addEPerson(<%= e.getID() %>, '<%= e.getEmail() %>', '<%= fullname %>');<%= closeWindow %>"></td>
			<td class="<%= row %>RowEvenCol"><%= e.getID() %></td>
			<td class="<%= row %>RowOddCol"><%= e.getEmail() %></td>
            <td class="<%= row %>RowEvenCol">
                <%= (e.getLastName() == null ? "" : e.getLastName()) %>
            </td>
            <td class="<%= row %>RowOddCol">
                <%= (e.getFirstName() == null ? "" : e.getFirstName()) %>
            </td>
        </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>

<br>

<%-- Controls for jumping around list--%>
	<table width="99%">
		<tr>
			<%--
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %>0">First</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpFiveBack %>">&lt; 5 Pages</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpOneBack %>">&lt; 1 Page</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpOneForward %>">1 Page &gt;</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpFiveForward %>">5 Pages &gt;</A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpEnd %>">Last</A></strong></small></td> --%>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %>0"><fmt:message key="jsp.tools.eperson-list.jump.first"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpFiveBack %>"><fmt:message key="jsp.tools.eperson-list.jump.five-back"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpOneBack %>"><fmt:message key="jsp.tools.eperson-list.jump.one-back"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpOneForward %>"><fmt:message key="jsp.tools.eperson-list.jump.one-forward"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpFiveForward %>"><fmt:message key="jsp.tools.eperson-list.jump.five-forward"/></A></strong></small></td>
			<td width="17%" align="center"><small><strong><A HREF="<%= jumpLink %><%= jumpEnd %>"><fmt:message key="jsp.tools.eperson-list.jump.last"/></A></strong></small></td>
		</tr>
	</table>

	<%-- <P align="center"><input type="button" value="Close" onClick="window.close();"></P> --%>
	<P align="center"><input type="button" value="<fmt:message key="jsp.tools.eperson-list.close.button"/>" onClick="window.close();"></P>

	</form>

	</body>
</html>
