<%--
  - collection-select-list.jsp
  --%>


<%--
  - Display list of Collections, with pagination
  -
  - Attributes:
  -
  -   collections     - Collection[] - all collections to browse
  -   first      - Integer - index of first collection to display
  -   multiple   - if non-null, this is for selecting multiple collections
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Collection" %>

<%@ page import="org.dspace.core.Utils" %>


<%
    int PAGESIZE = 50;

    Collection[] collections =
        (Collection[]) request.getAttribute("collections");
    int first = ((Integer)request.getAttribute("first")).intValue();
	boolean multiple = (request.getAttribute("multiple") != null);

	// Make sure we won't run over end of list
	int last = first + PAGESIZE;
	if (last >= collections.length) last = collections.length - 1;

	// Index of first collection on last page
	int jumpEnd = ((collections.length - 1) / PAGESIZE) * PAGESIZE;

	// Now work out values for next/prev page buttons
	int jumpFiveBack = first - PAGESIZE * 5;
	if (jumpFiveBack < 0) jumpFiveBack = 0;
	
	int jumpOneBack = first - PAGESIZE;
	if (jumpOneBack < 0) jumpOneBack = 0;
	
	int jumpOneForward = first + PAGESIZE;
	if (jumpOneForward > collections.length) jumpOneForward = first;
	
	int jumpFiveForward = first + PAGESIZE * 5;
	if (jumpFiveForward > collections.length) jumpFiveForward = jumpEnd;
	
	String jumpLink = request.getContextPath() + "/tools/collection-select-list?multiple=" + multiple + "&first=";
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <%-- <title>Select Collections</title> --%>
        <title><fmt:message key="jsp.tools.collection-select-list.title"/></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <link rel="stylesheet" href="<%= request.getContextPath() %>/styles.css.jsp" type="text/css"/>
        <link rel="shortcut icon" href="<%= request.getContextPath() %>/favicon.ico" type="image/x-icon"/>

<script type="text/javascript">
<!-- Begin

// Add the selected items to main collection list by calling method of parent
function addCollection(id, name)
{
	self.opener.addCollection(id, name);
}

// Clear selected items from main collection list
function clearCollections()
{
	var list = self.opener.document.epersoncollection.collection_ids;
	while (list.options.length > 0)
	{
		list.options[0] = null;
	}
}

// End -->
</script>
	</head>
	<body class="pageContents">

    <%-- <h3>Collections <%= first + 1 %>-<%= last + 1 %> of <%= collections.length %></h3> --%>
	<h3><fmt:message key="jsp.tools.collection-select-list.heading">
        <fmt:param><%= first + 1 %></fmt:param>
        <fmt:param><%= last + 1 %></fmt:param>
        <fmt:param><%= collections.length %></fmt:param>
    </fmt:message></h3>

<%
	if (multiple)
	{ %>
		<%-- <p class="submitFormHelp">Clicking on the 'Add' button next to a collection will add that
			 collection to the list on the main form. </p> --%>
		<p class="submitFormHelp"><fmt:message key="jsp.tools.collection-select-list.info1"/></p>
<%  } %>
    
<%-- Controls for jumping around list--%>
	<table width="99%">
		<tr>
			<%-- <td width="17%" align="center"><small><strong><a href="<%= jumpLink %>0">First</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpFiveBack %>">&lt; 5 Pages</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpOneBack %>">&lt; 1 Page</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpOneForward %>">1 Page &gt;</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpFiveForward %>">5 Pages &gt;</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpEnd %>">Last</A></strong></small></td>
			--%>
		
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %>0"><fmt:message key="jsp.tools.collection-select-list.jump.first"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpFiveBack %>"><fmt:message key="jsp.tools.collection-select-list.jump.five-back"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpOneBack %>"><fmt:message key="jsp.tools.collection-select-list.jump.one-back"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpOneForward %>"><fmt:message key="jsp.tools.collection-select-list.jump.one-forward"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpFiveForward %>"><fmt:message key="jsp.tools.collection-select-list.jump.five-forward"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpEnd %>"><fmt:message key="jsp.tools.collection-select-list.jump.last"/></a></strong></small></td>
		</tr>
	</table>
<br/>

	<form method="get" action=""> <%-- Will never actually be posted, it's just so buttons will appear --%>

    <table class="miscTable" align="center" summary="Collection list">
        <tr>
         <th id="t1" class="oddRowOddCol">&nbsp;</th>
			<th id="t2" class="oddRowEvenCol">
           <fmt:message key="jsp.tools.collection-select-list.th.id" />
         </th>
         <th id="t3" class="oddRowOddCol">
           <fmt:message key="jsp.tools.collection-select-list.th.name" />
         </th>
        </tr>

<%
    String row = "even";

	// If this is a dialogue to select a *single* collection, we want
	// to clear any existing entry in the collection list, and
	// to close this window when a 'select' button is clicked
	String clearList   = (multiple ? "" : "clearCollections();" );
	String closeWindow = (multiple ? "" : "window.close();");


    for (int i = first; i <= last; i++)
    {
        Collection g = collections[i];
		// Make sure no quotes in full name will mess up our Javascript
        String fullname = g.getName().replace('\'', ' ');
%>
        <tr>
			<td headers="t1" class="<%= row %>RowOddCol">
				<input type="button" value="<%
	if (multiple) { %><fmt:message key="jsp.tools.general.add"/><% }
	else {          %><fmt:message key="jsp.tools.general.select"/><% } %>" onclick="javascript:<%= clearList %>addCollection('<%= g.getID() %>', '<%= Utils.addEntities(fullname) %>');<%= closeWindow %>"/></td>
			<td headers="t2" class="<%= row %>RowEvenCol"><%= g.getID() %></td>
			<td headers="t3" class="<%= row %>RowOddCol"> <%= g.getName()%></td>
        </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>

<br/>

<%-- Controls for jumping around list--%>
	<table width="99%">
		<tr>
			<%-- <td width="17%" align="center"><small><strong><a href="<%= jumpLink %>0">First</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpFiveBack %>">&lt; 5 Pages</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpOneBack %>">&lt; 1 Page</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpOneForward %>">1 Page &gt;</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpFiveForward %>">5 Pages &gt;</A></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpEnd %>">Last</A></strong></small></td>
			--%>
		
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %>0"><fmt:message key="jsp.tools.collection-select-list.jump.first"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpFiveBack %>"><fmt:message key="jsp.tools.collection-select-list.jump.five-back"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpOneBack %>"><fmt:message key="jsp.tools.collection-select-list.jump.one-back"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpOneForward %>"><fmt:message key="jsp.tools.collection-select-list.jump.one-forward"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpFiveForward %>"><fmt:message key="jsp.tools.collection-select-list.jump.five-forward"/></a></strong></small></td>
			<td width="17%" align="center"><small><strong><a href="<%= jumpLink %><%= jumpEnd %>"><fmt:message key="jsp.tools.collection-select-list.jump.last"/></a></strong></small></td>
		</tr>
	</table>

	<%-- <p align="center"><input type="button" value="Close" onClick="window.close();"></p> --%>
	<p align="center"><input type="button" value="<fmt:message key="jsp.tools.collection-select-list.close.button"/>" onclick="window.close();"/></p>

	</form>
	
	</body>
</html>
