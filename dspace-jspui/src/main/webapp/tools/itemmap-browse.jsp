<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display the results of an item browse
  -
  - Attributes to pass in:
  -
  -   items          - sorted Map of mapped Items to display
  -   collection     - Collection we're mapping into
  -   collections    - Map of Collections mapped into this one, keyed by collection_id
  -   browsetext     - text to display at the top (name of collection we are mapping from)
  -   browsetype     - "Add" or "Remove"
  --%>
  
<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="java.net.URLEncoder"            %>
<%@ page import="java.util.Iterator"             %>
<%@ page import="java.util.Map"                  %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.content.Collection"  %>
<%@ page import="org.dspace.content.Item"        %>
<%@ page import="org.dspace.content.MetadataValue" %>
<%@ page import="org.dspace.content.service.ItemService" %>
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>
<%@ page import="java.util.List" %>

<%
    Collection collection  = (Collection)request.getAttribute("collection");
    Map items              = (Map)request.getAttribute("items");
    Map collections        = (Map)request.getAttribute("collections");
    String index = request.getParameter("index");
    String query = request.getParameter("query");
    String browsetext      = (String)request.getAttribute("browsetext");
    Boolean showcollection = new Boolean(false);
    String browsetype      = (String)request.getAttribute("browsetype");    // Only "Add" and "Remove" are handled properly
    Boolean more = (Boolean) request.getAttribute("more");
    boolean bMore = more != null?more:false;
    int pageResult = (Integer) request.getAttribute("page") != null ? (Integer) request
            .getAttribute("page") : 1;
%>

<dspace:layout style="submission" titlekey="jsp.tools.itemmap-browse.title">

    <%-- <h2>Browse <%=browsetext%></h2> --%>
    <h2>
        <% if (browsetype.equals("Add")) { %>
            <fmt:message key="jsp.tools.itemmap-browse.heading-search">
                <fmt:param><%= browsetext %></fmt:param>
            </fmt:message>
        <% } else if (browsetype.equals("Remove")) { %>
            <fmt:message key="jsp.tools.itemmap-browse.heading-collection">
                <fmt:param><%= browsetext %></fmt:param>
                <fmt:param><%= collection.getName() %></fmt:param>
            </fmt:message>
        <% } %>
    </h2>

    <%-- <p>Check the box next to items you wish to add or remove, and choose 'add' or 'remove'.</p> --%>
    <% if (browsetype.equals("Add")){ %>
    <p class="alert alert-info">
        <fmt:message key="jsp.tools.itemmap-browse.add">
            <fmt:param><%= collection.getName() %></fmt:param>
        </fmt:message>
    </p>
    <% }%>
    <% if (browsetype.equals("Remove")){ %>
    <p class="alert alert-warning">
        <fmt:message key="jsp.tools.itemmap-browse.remove">
            <fmt:param><%= collection.getName() %></fmt:param>
        </fmt:message>
    </p>
    <% } %>
    
    <%-- %>p><fmt:message key="jsp.tools.itemmap-browse.infomsg"/></p--%>
    <form method="post" action="<%= request.getContextPath() %>/tools/itemmap">
        <input type="hidden" name="cid" value="<%=collection.getID()%>" />
	<div class="btn-group">		
		<input type="hidden" name="action" value="<%=browsetype%>" />
                <% if (browsetype.equals("Add")) { %>
                        <input class="btn btn-success" type="submit" value="<fmt:message key="jsp.tools.general.add"/>" />
                <% } else if (browsetype.equals("Remove")) { %>
                        <input class="btn btn-danger" type="submit" value="<fmt:message key="jsp.tools.general.remove"/>" />
                <% } %>
        
        <input class="btn btn-default" type="submit" name="cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
	</div>        
	<div class="table-responsive">
        <table class="table">
        <tr>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.tools.itemmap-browse.th.date"/></strong></th>
            <th class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.itemmap-browse.th.author"/></strong></th>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.tools.itemmap-browse.th.title"/></strong></th>
            <% if(showcollection.booleanValue()) { %>
                <th class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.itemmap-browse.th.action"/></strong></th>
                <th class="oddRowOddCol"> <fmt:message key="jsp.tools.itemmap-browse.th.remove"/> </th>
            <% } else { %>
                <th class="oddRowEvenCol">
                <% if (browsetype.equals("Add")) { %>
                        <input class="btn btn-success" type="submit" value="<fmt:message key="jsp.tools.general.add"/>" />
                <% } else if (browsetype.equals("Remove")) { %>
                        <input class="btn btn-danger" type="submit" value="<fmt:message key="jsp.tools.general.remove"/>" />
                <% } %>
                </th>
            <% } %>     
        </tr>
<%
    String row = "even";
    Iterator i = items.keySet().iterator();

    ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    while( i.hasNext() )
    {
        Item item = (Item)items.get(i.next());
        // get the metadata or placeholders to display for date, contributor and title
        String date = LocaleSupport.getLocalizedMessage(pageContext, "jsp.general.without-date");
        List<MetadataValue> dates = itemService.getMetadata(item, "dc", "date", "issued", Item.ANY);
        if (dates.size() >= 1)
        {
            date = dates.get(0).getValue();
        }
        else
        {
         // do nothing the date is already set to "without date"
        }
        String contributor = LocaleSupport.getLocalizedMessage(pageContext, "jsp.general.without-contributor");
        List<MetadataValue> contributors = itemService.getMetadata(item, "dc", "contributor", Item.ANY, Item.ANY);
        if (contributors.size() >= 1)
        {
            contributor = contributors.get(0).getValue();
            
        }
        else
        {
         // do nothing the contributor is already set to anonymous
        }
        String title = LocaleSupport.getLocalizedMessage(pageContext, "jsp.general.untitled");
        List<MetadataValue> titles = itemService.getMetadata(item, "dc", "title", null, Item.ANY);
        if (titles.size() >= 1)
        {
            title = titles.get(0).getValue();
            
        }
        else
        {
         // do nothing the title is already set to untitled
            
        }
 

%>
        <tr>
        <td class="<%= row %>RowOddCol">
        <%= date %>
        </td>
        <td class="<%= row %>RowEvenCol">
        <%= contributor %>
        </td>
        <td class="<%= row %>RowOddCol">
        <%= title %></td>

<% if( showcollection.booleanValue() ) { %>
<%-- not currently implemented --%>
        <td class="<%= row %>RowEvenCol">  <%= collection.getID() %>
        <td class="<%= row %>RowOddCol"><%= title %></td>

<% } else { %>

        <td class="<%= row %>RowEvenCol"><input type="checkbox" name="item_ids"
        value="<%=item.getID()%>" /></td>

<% }

        row = (row.equals("odd") ? "even" : "odd");
%>
        </tr>
<% } %>
        
        </table>
	</div>
	<div class="btn-group">		
		<input type="hidden" name="action" value="<%=browsetype%>" />
                <% if (browsetype.equals("Add")) { %>
                        <input class="btn btn-success" type="submit" value="<fmt:message key="jsp.tools.general.add"/>" />
                <% } else if (browsetype.equals("Remove")) { %>
                        <input class="btn btn-danger" type="submit" value="<fmt:message key="jsp.tools.general.remove"/>" />
                <% } %>
        
        <input class="btn btn-default" type="submit" name="cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
	</div>
	
    </form>

<% if (bMore || pageResult > 1) { %>

<p class="alert"><fmt:message key="jsp.tools.itemmap-browse.info.change-page"/></p>
<div class="col-md-12">
<% if (pageResult > 1) { %>			

	<form method="post" class="standard10" action="">
        <input type="hidden" name="cid" value="<%=collection.getID()%>"/>
        <input type="hidden" name="action" value="search"/>
        <input type="hidden" name="index" id="index" value="<%= index %>"/>
        <input type="hidden" name="query" id="query" value="<%= query %>"/>
        <input type="hidden" name="page" id="page" value="<%= pageResult -1 %>"/>
        <input class="btn btn-default col-md-6" type="submit" value="<fmt:message key="jsp.tools.itemmap-browse.previous.button"/>"/> 
    </form>

<% 	}
	if (bMore) { %>    		
    		
	<form method="post" class="standard10" action="">
        <input type="hidden" name="cid" value="<%=collection.getID()%>"/>
        <input type="hidden" name="action" value="search"/>
        <input type="hidden" name="index" id="index" value="<%= index %>"/>
        <input type="hidden" name="query" id="query" value="<%= query %>"/>
        <input type="hidden" name="page" id="page" value="<%= pageResult +1 %>"/>
        <input class="btn btn-primary col-md-6" type="submit" value="<fmt:message key="jsp.tools.itemmap-browse.next.button"/>"/> 
    </form>
    		    
<% 	} %>
</div>
<%
}
%>
</dspace:layout>
