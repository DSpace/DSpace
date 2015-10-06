<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm deletion of a community
  -
  - Attributes:
  -    community   - community we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Community" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    Community community = (Community) request.getAttribute("community");
%>

<dspace:layout style="submission" titlekey="jsp.tools.confirm-delete-community.title"
						navbar="admin"
						locbar="link"
						parentlink="/tools"
						parenttitlekey="jsp.administer">

    <%-- <h1>Delete Community: <%= community.getID() %></h1> --%>
    <h1><fmt:message key="jsp.tools.confirm-delete-community.heading">
        <fmt:param><%= community.getID() %></fmt:param>
    </fmt:message></h1>
    
    <%-- <p>Are you sure the community <strong><%= community.getMetadata("name") %></strong>
    should be deleted?  This will delete:</p> --%>
    <p><fmt:message key="jsp.tools.confirm-delete-community.confirm">
        <fmt:param><%= community.getName() %></fmt:param>
    </fmt:message></p>
        
    <ul>
        <li><fmt:message key="jsp.tools.confirm-delete-community.info1"/></li>
        <li><fmt:message key="jsp.tools.confirm-delete-community.info2"/></li>
        <li><fmt:message key="jsp.tools.confirm-delete-community.info3"/></li>
        <li><fmt:message key="jsp.tools.confirm-delete-community.info4"/></li>
    </ul>
    
    <form method="post" action="">
        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_DELETE_COMMUNITY %>" />

		<input class="btn btn-default col-md-2" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>"/>
        <input class="btn btn-danger col-md-2 pull-right" type="submit" name="submit" value="<fmt:message key="jsp.tools.general.delete"/>"/>
    </form>
</dspace:layout>
