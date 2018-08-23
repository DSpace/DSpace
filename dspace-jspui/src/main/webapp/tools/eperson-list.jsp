<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of E-people, with pagination
  -
  - Attributes:
  -
  -   epeople    - EPerson[] - all epeople to browse
  -   sortby     - Integer - field to sort by (constant from EPerson.java) (when show all)
  -   first      - Integer - index of first eperson to display (when show all)
  -   multiple   - if non-null, this is for selecting multiple epeople
  -   search     - String - query string for search eperson
  -   offset     - Integer - offset in a search result set
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.Utils" %>
<%
	boolean noEPersonSelected = (request.getAttribute("no_eperson_selected") != null);
	boolean resetPassword = (request.getAttribute("reset_password") != null);
	boolean loginAs = ConfigurationManager.getBooleanProperty("webui.user.assumelogin", false);
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.eperson-main.title"
			   navbar="admin"
			   locbar="link"
			   parenttitlekey="jsp.administer"
			   parentlink="/dspace-admin">
<%
	int PAGESIZE = 50;

    EPerson[] epeople =
        (EPerson[]) request.getAttribute("epeople");
    int sortBy = ((Integer)request.getAttribute("sortby" )).intValue();
    int first = ((Integer)request.getAttribute("first")).intValue();
	boolean multiple = (request.getAttribute("multiple") != null);
	String search = (String) request.getAttribute("search");
	if (search == null) search = "";
	int offset = ((Integer)request.getAttribute("offset")).intValue();

	// Make sure we won't run over end of list
	int last;
	if (search != null && !search.equals(""))
	{
		last = offset + PAGESIZE;
	}
	else
	{
	  last = first + PAGESIZE;
	}
	if (last >= epeople.length) last = epeople.length - 1;

	// Index of first eperson on last page
	int jumpEnd = ((epeople.length - 1) / PAGESIZE) * PAGESIZE;

	// Now work out values for next/prev page buttons
	int jumpFiveBack;
	if (search != null && !search.equals(""))
	{
	    jumpFiveBack = offset - PAGESIZE * 5;
	}
	else
	{
		jumpFiveBack = first - PAGESIZE * 5;
	}
	if (jumpFiveBack < 0) jumpFiveBack = 0;

	int jumpOneBack;
	if (search != null && !search.equals(""))
	{
		jumpOneBack = offset - PAGESIZE;
	}
	else
	{
	   jumpOneBack = first - PAGESIZE;
	}
	if (jumpOneBack < 0) jumpOneBack = 0;

	int jumpOneForward;
	if (search != null && !search.equals(""))
	{
		jumpOneForward = offset + PAGESIZE;
	}
	else
	{
		jumpOneForward = first + PAGESIZE;
	}
	if (jumpOneForward > epeople.length) jumpOneForward = jumpEnd;

	int jumpFiveForward;
	if (search != null && !search.trim().equals(""))
	{
		jumpFiveForward = offset + PAGESIZE * 5;
	}
	else
	{
		jumpFiveForward = first + PAGESIZE * 5;
	}
	if (jumpFiveForward > epeople.length) jumpFiveForward = jumpEnd;

	// What's the link?
	String sortByParam = "lastname";
	if (sortBy == EPerson.EMAIL) sortByParam = "email";
	if (sortBy == EPerson.ID) sortByParam = "id";
	if (sortBy == EPerson.LANGUAGE) sortByParam = "language";

	String jumpLink;
	if (search != null && !search.equals(""))
	{
		jumpLink = request.getContextPath() + "/tools/eperson-list?multiple=" + multiple + "&sortby=" + sortByParam + "&first="+first+"&search="+search+"&offset=";
	}
	else
	{
		jumpLink = request.getContextPath() + "/tools/eperson-list?multiple=" + multiple + "&sortby=" + sortByParam + "&first=";
	}
	String sortLink = request.getContextPath() + "/tools/eperson-list?multiple=" + multiple + "&first=" + first + "&sortby=";
%>

<script type="text/javascript">
<!-- Begin

// Add the selected items to main e-people list by calling method of parent
function addEPerson(id, email, name)
{
	self.opener.addEPerson(id, email, name);
}

// Clear selected items from main e-people list
function clearEPeople()
{
	var list = self.opener.document.epersongroup.eperson_id;
	while (list.options.length > 0)
	{
		list.options[0] = null;
	}
}

// End -->
</script>

	<% if (resetPassword)
	{ %><p class="alert alert-success">
	<fmt:message key="jsp.dspace-admin.eperson-main.ResetPassword.success_notice"/>
</p>
	<%  } %>
    <%-- <h3>E-people <%= first + 1 %>-<%= last + 1 %> of <%= epeople.length %></h3> --%>
	<h3><fmt:message key="jsp.tools.eperson-list.heading">
        <fmt:param><%= ((search != null && !search.equals(""))?offset:first) + 1 %></fmt:param>
        <fmt:param><%= last + 1 %></fmt:param>
        <fmt:param><%= epeople.length %></fmt:param>
    </fmt:message></h3>

<%
	if (multiple)
	{ %>
	<%-- <p class="submitFormHelp">Clicking on the 'Add' button next to an e-person will add that
	e-person to the list on the main form. </p> --%>
	<p class="submitFormHelp"><fmt:message key="jsp.tools.eperson-list.info1"/></p>
<%  } %>

	<form method="get" class="form-inline pull-right" style="margin-bottom: 10px;">
	    <input type="hidden" name="first" value="<%= first %>" />
	    <input type="hidden" name="sortby" value="<%= sortBy %>" />
	    <input type="hidden" name="multiple" value="<%= multiple %>" />
	    <label for="search"><fmt:message key="jsp.tools.eperson-list.search.query"/></label>
	    <input class="form-control" style="width:200px;"type="text" name="search" value="<%= search %>"/>
	    <input class="btn btn-success" type="submit" value="<fmt:message key="jsp.tools.eperson-list.search.submit" />" />
	<%
	    if (search != null && !search.equals("")){   %>
	    <a class="btn btn-warning" href="<%= request.getContextPath() + "/tools/eperson-list?multiple=" + multiple + "&sortby=" + sortByParam + "&first="+first %>"><fmt:message key="jsp.tools.eperson-list.search.return-browse" /></a>
		<%}%>

	</form>

    <table class="table table-striped" align="center" summary="Epeople list">
<% if (search != null && !search.equals(""))
   {  %>
       <tr>
            <th>&nbsp;</th>
            <th><fmt:message key="jsp.tools.eperson-list.th.id" /></th>
            <th><fmt:message key="jsp.tools.eperson-list.th.email" /></th>
            <th><fmt:message key="jsp.tools.eperson-list.th.lastname" /></th>
            <th><fmt:message key="jsp.tools.eperson-list.th.lastname" /></th>
        </tr>
<% }
   else
   {  %>
        <tr>
			<th id="t0">&nbsp;</th>
            <th id="t1"><fmt:message key="jsp.tools.eperson-list.th.id" /></th>
            <th id="t2"><fmt:message key="jsp.tools.eperson-list.th.email" /></th>
            <th id="t3"><fmt:message key="jsp.tools.eperson-list.th.lastname" /></th>
            <th id="t4"><fmt:message key="jsp.tools.eperson-list.th.firstname"/></th>
             <th id="t5"><fmt:message key="jsp.tools.eperson-list.th.language" /></th>
			<th id="t6">&nbsp;</th>
        </tr>
<%  }
    String row = "even";

	// If this is a dialogue to select a *single* e-person, we want
	// to clear any existing entry in the e-person list, and
	// to close this window when a 'select' button is clicked
	String clearList = (multiple ? "" : "clearEPeople();");
	String closeWindow = (multiple ? "" : "window.close();");


    for (int i = (search != null && !search.equals(""))?offset:first; i <= last; i++)
    {
        EPerson e = epeople[i];
		// Make sure no quotes in full name will mess up our Javascript
        String fullname = StringEscapeUtils.escapeXml(StringEscapeUtils.escapeJavaScript(e.getFullName()));
        String email = StringEscapeUtils.escapeXml(StringEscapeUtils.escapeJavaScript(e.getEmail()));
%>
  <tr>
			<form method="post" action="<%= request.getContextPath() %>/dspace-admin/edit-epeople">
				<td headers="t1">
					<input class="btn btn-success" type="button" value="<%
			if (multiple) { %><fmt:message key="jsp.tools.general.add"/><% }
			else {          %><fmt:message key="jsp.tools.general.select"/><% } %>" onclick="javascript:<%= clearList %>addEPerson(<%= e.getID() %>, '<%= email %>', '<%= fullname %>');<%= closeWindow %>"/></td>
				<td headers="t2"><%= e.getID() %></td>
				<td headers="t3"><%= (e.getEmail() == null ? "" : Utils.addEntities(e.getEmail())) %></td>
				<td headers="t4">
					<%= (e.getLastName() == null ? "" : Utils.addEntities(e.getLastName())) %>
				</td>
				<td headers="t5">
					<%= (e.getFirstName() == null ? "" : Utils.addEntities(e.getFirstName())) %>
				</td>
				<td headers="t6">
					<%= (e.getLanguage() == null ? "" : Utils.addEntities(e.getLanguage())) %>
				</td>
				<td headers="t7">
					<input type="hidden" name="eperson_id" value="<%= e.getID() %>"/>
					<input type="submit" name="submit_edit" class="btn btn-primary" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
					<input type="submit" name="submit_delete" class="btn btn-danger" value="<fmt:message key="jsp.dspace-admin.general.delete-w-confirm"/>" />
				</td>
			</form>
        </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>

<br/>
	<div class="row" style="margin-bottom: 10px;">
		<form name="epersongroup" method="post" action="" class = "pull-right">
			<input class="btn btn-success" type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.eperson-main.add"/>" />
		</form>
	</div>
	<%@include file="../pagination/pagination-users.jsp"%>
</dspace:layout>