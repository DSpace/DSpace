<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm deletion of a collection
  -
  - Attributes:
  -    collection   - collection we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");
%>

<dspace:layout titlekey="jsp.tools.confirm-delete-collection.title"
		style="submission" navbar="admin"
		locbar="link"
		parentlink="/tools"
		parenttitlekey="jsp.administer">

    <%-- <h1>Delete Collection: <%= collection.getID() %></h1> --%>
    <h1><fmt:message key="jsp.tools.confirm-delete-collection.heading">
        <fmt:param><%= collection.getID() %></fmt:param>
    </fmt:message></h1>
    
    <%-- <p>Are you sure the collection <strong><%= collection.getMetadata("name") %></strong>
    should be deleted?  This will delete:</p> --%>
    <p><fmt:message key="jsp.tools.confirm-delete-collection.confirm">
        <fmt:param><%= collection.getName() %></fmt:param>
    </fmt:message></p>
    
    <ul>
        <li><fmt:message key="jsp.tools.confirm-delete-collection.info1"/></li>
        <li><fmt:message key="jsp.tools.confirm-delete-collection.info2"/></li>
        <li><fmt:message key="jsp.tools.confirm-delete-collection.info3"/></li>
    </ul>
    
    <form method="post" action="">
        <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_DELETE_COLLECTION %>" />

		<input class="btn btn-default col-md-2" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>"/>
        <input class="btn btn-danger col-md-2 pull-right" type="submit" name="submit" value="<fmt:message key="jsp.tools.general.delete"/>"/>
    </form>
</dspace:layout>
