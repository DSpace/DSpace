<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of Groups, with pagination
  -
  - Attributes:
  -
  -   groups     - Group[] - all groups to browse
  -   sortby     - Integer - field to sort by (constant from Group.java)
  -   first      - Integer - index of first group to display
  -   multiple   - if non-null, this is for selecting multiple groups
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.eperson.Group" %>

<%@ page import="org.dspace.core.Utils" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.eperson.service.GroupService" %>


<%
    int PAGESIZE = 50;

    List<Group> groups =
        (List<Group>) request.getAttribute("groups");
    int sortBy = ((Integer)request.getAttribute("sortby" )).intValue();
    int first = ((Integer)request.getAttribute("first")).intValue();
	boolean multiple = (request.getAttribute("multiple") != null);

	// Make sure we won't run over end of list
	int last = first + PAGESIZE;
	if (last >= groups.size()) last = groups.size() - 1;

	// Index of first group on last page
	int jumpEnd = ((groups.size() - 1) / PAGESIZE) * PAGESIZE;

	// Now work out values for next/prev page buttons
	int jumpFiveBack = first - PAGESIZE * 5;
	if (jumpFiveBack < 0) jumpFiveBack = 0;
	
	int jumpOneBack = first - PAGESIZE;
	if (jumpOneBack < 0) jumpOneBack = 0;
	
	int jumpOneForward = first + PAGESIZE;
	if (jumpOneForward > groups.size()) jumpOneForward = first;
	
	int jumpFiveForward = first + PAGESIZE * 5;
	if (jumpFiveForward > groups.size()) jumpFiveForward = jumpEnd;
	
	// What's the link?
	String sortByParam = "name";

	String jumpLink = request.getContextPath() + "/tools/group-select-list?multiple=" + multiple + "&sortby=" + sortByParam + "&first=";
	String sortLink = request.getContextPath() + "/tools/group-select-list?multiple=" + multiple + "&first=" + first + "&sortby=";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <%-- <title>Select Groups</title> --%>
        <title><fmt:message key="jsp.tools.group-select-list.title"/></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <link rel="stylesheet" href="<%= request.getContextPath() %>/styles.css" type="text/css"/>
        <link rel="shortcut icon" href="<%= request.getContextPath() %>/favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap.min.css" type="text/css" />
        <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap-theme.min.css" type="text/css" />
        <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/dspace-theme.css" type="text/css" />
        <script type='text/javascript' src='<%= request.getContextPath() %>/static/js/bootstrap/bootstrap.min.js'></script>
		
<script type="text/javascript">
<!-- Begin

// Add the selected items to main group list by calling method of parent
function addGroup(id, name)
{
	self.opener.addGroup(id, name);
}

// Clear selected items from main group list
function clearGroups()
{
	var list = self.opener.document.epersongroup.group_ids;
	while (list.options.length > 0)
	{
		list.options[0] = null;
	}
}

// End -->
</script>
	</head>
	<body class="pageContents">

    <%-- <h3>Groups <%= first + 1 %>-<%= last + 1 %> of <%= groups.size() %></h3> --%>
	<h3><fmt:message key="jsp.tools.group-select-list.heading">
        <fmt:param><%= first + 1 %></fmt:param>
        <fmt:param><%= last + 1 %></fmt:param>
        <fmt:param><%= groups.size() %></fmt:param>
    </fmt:message></h3>

<%
	if (multiple)
	{ %>
		<%-- <p class="submitFormHelp">Clicking on the 'Add' button next to a group will add that
			 group to the list on the main form. </p> --%>
		<p class="submitFormHelp"><fmt:message key="jsp.tools.group-select-list.info1"/></p>
<%  } %>
    
<%-- Controls for jumping around list--%>
<div class="span12" style="text-align:center">
	<ul class="pagination">			
			<li><a href="<%= jumpLink %>0"><fmt:message key="jsp.tools.group-select-list.jump.first"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpFiveBack %>"><fmt:message key="jsp.tools.group-select-list.jump.five-back"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpOneBack %>"><fmt:message key="jsp.tools.group-select-list.jump.one-back"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpOneForward %>"><fmt:message key="jsp.tools.group-select-list.jump.one-forward"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpFiveForward %>"><fmt:message key="jsp.tools.group-select-list.jump.five-forward"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpEnd %>"><fmt:message key="jsp.tools.group-select-list.jump.last"/></a></li>
	</ul>
</div>
<br/>

	<form method="get" action=""> <%-- Will never actually be posted, it's just so buttons will appear --%>

    <table class="table table-striped" align="center" summary="Group list">
        <tr>
            <th id="t1" class="oddRowOddCol">&nbsp;</th>
			<th id="t2" class="oddRowEvenCol">
				<a href="<%= sortLink %>id"><fmt:message key="jsp.tools.group-select-list.th.id" /></a><%
            %></th>
            <th id="t3" class="oddRowOddCol"><%
                if (sortBy == GroupService.NAME)
                {
                    %><fmt:message key="jsp.tools.group-select-list.th.name" /><span class="glyphicon glyphicon-arrow-down"><%
                }
                else
                {
                    %><a href="<%= sortLink %>name"><fmt:message key="jsp.tools.group-select-list.th.name" /></a><%
                }
            %></th>
        </tr>

<%
    String row = "even";

	// If this is a dialogue to select a *single* group, we want
	// to clear any existing entry in the group list, and
	// to close this window when a 'select' button is clicked
	String clearList   = (multiple ? "" : "clearGroups();" );
	String closeWindow = (multiple ? "" : "window.close();");


    for (int i = first; i <= last; i++)
    {
        Group g = groups.get(i);
		// Make sure no quotes in full name will mess up our Javascript
        String fullname = g.getName().replace('\'', ' ');
%>
        <tr>
			<td headers="t1" class="">
				<input type="button" class="btn btn-success" value="<%
	if (multiple) { %><fmt:message key="jsp.tools.general.add"/><% }
	else {          %><fmt:message key="jsp.tools.general.select"/><% } %>" onclick="javascript:<%= clearList %>addGroup('<%= g.getID() %>', '<%= Utils.addEntities(fullname) %>');<%= closeWindow %>"/></td>
			<td headers="t2" class=""><%= g.getID() %></td>
			<td headers="t3" class=""> <%= Utils.addEntities(g.getName())%></td>
        </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>

<br/>

<%-- Controls for jumping around list--%>
<div class="span12" style="text-align:center">
	<ul class="pagination">	
			<li><a href="<%= jumpLink %>0"><fmt:message key="jsp.tools.group-select-list.jump.first"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpFiveBack %>"><fmt:message key="jsp.tools.group-select-list.jump.five-back"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpOneBack %>"><fmt:message key="jsp.tools.group-select-list.jump.one-back"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpOneForward %>"><fmt:message key="jsp.tools.group-select-list.jump.one-forward"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpFiveForward %>"><fmt:message key="jsp.tools.group-select-list.jump.five-forward"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpEnd %>"><fmt:message key="jsp.tools.group-select-list.jump.last"/></a></li>
	</ul>
</div>

	<%-- <p align="center"><input type="button" value="Close" onClick="window.close();"></p> --%>
	<p align="center"><input type="button" class="btn btn-danger" value="<fmt:message key="jsp.tools.group-select-list.close.button"/>" onclick="window.close();"/></p>

	</form>
	
	</body>
</html>
