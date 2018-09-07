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

<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="java.util.List" %>

<%
	int PAGESIZE = 50;

    List<EPerson> epeople =
        (List<EPerson>) request.getAttribute("epeople");
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
	if (last >= epeople.size()) last = epeople.size() - 1;

	// Index of first eperson on last page
	int jumpEnd = ((epeople.size() - 1) / PAGESIZE) * PAGESIZE;

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
	if (jumpOneForward > epeople.size()) jumpOneForward = jumpEnd;

	int jumpFiveForward;
	if (search != null && !search.trim().equals(""))
	{
		jumpFiveForward = offset + PAGESIZE * 5;
	}
	else 
	{
		jumpFiveForward = first + PAGESIZE * 5;
	}
	if (jumpFiveForward > epeople.size()) jumpFiveForward = jumpEnd;

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

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html>
    <head>
        <%-- <title>Select E-people</title> --%>
        <title><fmt:message key="jsp.tools.eperson-list.title"/></title>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <link rel="stylesheet" href="<%= request.getContextPath() %>/styles.css" type="text/css"/>
        <link rel="shortcut icon" href="<%= request.getContextPath() %>/favicon.ico" type="image/x-icon"/>
        <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/jquery-ui-1.10.3.custom/redmond/jquery-ui-1.10.3.custom.css" type="text/css" />
        <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap.min.css" type="text/css" />
        <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/bootstrap-theme.min.css" type="text/css" />
        <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/bootstrap/dspace-theme.css" type="text/css" />
        <script type='text/javascript' src="<%= request.getContextPath() %>/static/js/jquery/jquery-1.10.2.min.js"></script>
        <script type='text/javascript' src='<%= request.getContextPath() %>/static/js/jquery/jquery-ui-1.10.3.custom.min.js'></script>
        <script type='text/javascript' src='<%= request.getContextPath() %>/static/js/bootstrap/bootstrap.min.js'></script>
        <script type='text/javascript' src='<%= request.getContextPath() %>/static/js/holder.js'></script>
        <script type="text/javascript" src="<%= request.getContextPath() %>/utils.js"></script>
							
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
	</head>
	<body class="pageContents">
    <%-- <h3>E-people <%= first + 1 %>-<%= last + 1 %> of <%= epeople.length %></h3> --%>
	<h3><fmt:message key="jsp.tools.eperson-list.heading">
        <fmt:param><%= ((search != null && !search.equals(""))?offset:first) + 1 %></fmt:param>
        <fmt:param><%= last + 1 %></fmt:param>
        <fmt:param><%= epeople.size() %></fmt:param>
    </fmt:message></h3>

<%
	if (multiple)
	{ %>
	<%-- <p class="submitFormHelp">Clicking on the 'Add' button next to an e-person will add that
	e-person to the list on the main form. </p> --%>
	<p class="submitFormHelp"><fmt:message key="jsp.tools.eperson-list.info1"/></p>
<%  } %>
<center>
	<form method="get">
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
</center>

<%-- Controls for jumping around list--%>
<div class="span12" style="text-align:center">
	<ul class="pagination">			
			<li><a href="<%= jumpLink %>0"><fmt:message key="jsp.tools.eperson-list.jump.first"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpFiveBack %>"><fmt:message key="jsp.tools.eperson-list.jump.five-back"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpOneBack %>"><fmt:message key="jsp.tools.eperson-list.jump.one-back"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpOneForward %>"><fmt:message key="jsp.tools.eperson-list.jump.one-forward"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpFiveForward %>"><fmt:message key="jsp.tools.eperson-list.jump.five-forward"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpEnd %>"><fmt:message key="jsp.tools.eperson-list.jump.last"/></a></li>
	</ul>
</div
<br/>

	<form method="get" action=""> <%-- Will never actually be posted, it's just so buttons will appear --%>

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
            <th id="t1">&nbsp;</th>
            <th id="t2"><%
                if (sortBy == EPerson.ID)
                {
                    %><fmt:message key="jsp.tools.eperson-list.th.id"/><span class="glyphicon glyphicon-arrow-down"><%
                }
                else
                {
                    %><a href="<%= sortLink %>id"><fmt:message key="jsp.tools.eperson-list.th.id" /></a><%
                }
            %></th>
            <th id="t3"><%
                if (sortBy == EPerson.EMAIL)
                {
                    %><fmt:message key="jsp.tools.eperson-list.th.email"/><span class="glyphicon glyphicon-arrow-down"><%
                }
                else
                {
                    %><a href="<%= sortLink %>email"><fmt:message key="jsp.tools.eperson-list.th.email" /></a><%
                }
            %></th>
            <%-- <th class="oddRowEvenCol"><%= sortBy == EPerson.LASTNAME ? "<strong>Last Name &uarr;</strong>" : "<a href=\"" + sortLink + "lastname\">Last Name</a>" %></th> --%>
            <th id="t4"><%
                if (sortBy == EPerson.LASTNAME)
                {
                    %><fmt:message key="jsp.tools.eperson-list.th.lastname"/><span class="glyphicon glyphicon-arrow-down"><%
                }
                else
                {
                    %><a href="<%= sortLink %>lastname"><fmt:message key="jsp.tools.eperson-list.th.lastname" /></a><%
                }
            %></th>

            <th id="t5"><fmt:message key="jsp.tools.eperson-list.th.firstname"/></th>
 
             <th id="t6"><%
                if (sortBy == EPerson.LANGUAGE)
                {
                    %><fmt:message key="jsp.tools.eperson-list.th.language"/><span class="glyphicon glyphicon-arrow-down"></span><%
                }
                else
                {
                    %><a href="<%= sortLink %>language"><fmt:message key="jsp.tools.eperson-list.th.language" /></a><%
                }
            %></th>
            
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
        EPerson e = epeople.get(i);
		// Make sure no quotes in full name will mess up our Javascript
        String fullname = StringEscapeUtils.escapeXml(StringEscapeUtils.escapeJavaScript(e.getFullName()));
        String email = StringEscapeUtils.escapeXml(StringEscapeUtils.escapeJavaScript(e.getEmail()));
%>
  <tr>
			<td headers="t1">
			    <input class="btn btn-success" type="button" value="<%
			if (multiple) { %><fmt:message key="jsp.tools.general.add"/><% }
			else {          %><fmt:message key="jsp.tools.general.select"/><% } %>" onclick="javascript:<%= clearList %>addEPerson('<%= e.getID() %>', '<%= email %>', '<%= Utils.addEntities(fullname) %>');<%= closeWindow %>"/></td>
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
			<li><a href="<%= jumpLink %>0"><fmt:message key="jsp.tools.eperson-list.jump.first"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpFiveBack %>"><fmt:message key="jsp.tools.eperson-list.jump.five-back"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpOneBack %>"><fmt:message key="jsp.tools.eperson-list.jump.one-back"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpOneForward %>"><fmt:message key="jsp.tools.eperson-list.jump.one-forward"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpFiveForward %>"><fmt:message key="jsp.tools.eperson-list.jump.five-forward"/></a></li>
			<li><a href="<%= jumpLink %><%= jumpEnd %>"><fmt:message key="jsp.tools.eperson-list.jump.last"/></a></li>
	</ul>
</div>

	<%-- <p align="center"><input type="button" value="Close" onClick="window.close();"/></p> --%>
	<p align="center">
		<input type="button" class="btn btn-danger" value="<fmt:message key="jsp.tools.eperson-list.close.button"/>" onclick="window.close();"/>
			</p>

	</form>

	</body>
</html>
