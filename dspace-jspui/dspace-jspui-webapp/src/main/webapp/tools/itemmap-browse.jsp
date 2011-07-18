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
  
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="java.net.URLEncoder"            %>
<%@ page import="java.util.Iterator"             %>
<%@ page import="java.util.Map"                  %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.content.Collection"  %>
<%@ page import="org.dspace.content.DCValue"    %>
<%@ page import="org.dspace.content.Item"        %>

<%
    Collection collection  = (Collection)request.getAttribute("collection");
    Map items              = (Map)request.getAttribute("items");
    Map collections        = (Map)request.getAttribute("collections");
    String browsetext      = (String)request.getAttribute("browsetext");
    Boolean showcollection = new Boolean(false);
    String browsetype      = (String)request.getAttribute("browsetype");    // Only "Add" and "Remove" are handled properly
%>

<dspace:layout titlekey="jsp.tools.itemmap-browse.title">

    <%-- <h2>Browse <%=browsetext%></h2> --%>
    <h2>
        <% if (browsetype.equals("Add")) { %>
            <fmt:message key="jsp.tools.itemmap-browse.heading-authors">
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
    <p>
        <fmt:message key="jsp.tools.itemmap-browse.add">
            <fmt:param><%= collection.getName() %></fmt:param>
        </fmt:message>
    </p>
    <% }%>
    <% if (browsetype.equals("Remove")){ %>
    <p>
        <fmt:message key="jsp.tools.itemmap-browse.remove">
            <fmt:param><%= collection.getName() %></fmt:param>
        </fmt:message>
    </p>
    <% } %>
    
    <%-- %>p><fmt:message key="jsp.tools.itemmap-browse.infomsg"/></p--%>
    <form method="post" action="<%= request.getContextPath() %>/tools/itemmap">
        <input type="hidden" name="cid" value="<%=collection.getID()%>" />

        <table>     
          <tr>
            <td><input type="hidden" name="action" value="<%=browsetype%>" />
                <% if (browsetype.equals("Add")) { %>
                        <input type="submit" value="<fmt:message key="jsp.tools.general.add"/>" />
                <% } else if (browsetype.equals("Remove")) { %>
                        <input type="submit" value="<fmt:message key="jsp.tools.general.remove"/>" />
                <% } %>
            </td>
            <td><input type="submit" name="cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" /></td>
          </tr>
        </table>


        <table class="miscTable" align="center">
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
                        <input type="submit" value="<fmt:message key="jsp.tools.general.add"/>" />
                <% } else if (browsetype.equals("Remove")) { %>
                        <input type="submit" value="<fmt:message key="jsp.tools.general.remove"/>" />
                <% } %>
                </th>
            <% } %>     
        </tr>
<%
    String row = "even";
    Iterator i = items.keySet().iterator();

    while( i.hasNext() )
    {
        Item item = (Item)items.get(i.next());
        // get the metadata or placeholders to display for date, contributor and title
        String date = LocaleSupport.getLocalizedMessage(pageContext, "jsp.general.without-date");
        DCValue[] dates = item.getMetadata("dc", "date", "issued", Item.ANY);
        if (dates.length >= 1)
        {
            date = dates[0].value;
        }
        else
        {
         // do nothing the date is allready set to "without date"
        }
        String contributor = LocaleSupport.getLocalizedMessage(pageContext, "jsp.general.without-contributor");
        DCValue[] contributors = item.getMetadata("dc", "contributor", Item.ANY, Item.ANY);
        if (contributors.length >= 1)
        {
            contributor = contributors[0].value;
            
        }
        else
        {
         // do nothing the contributor is allready set to anonymous
        }
        String title = LocaleSupport.getLocalizedMessage(pageContext, "jsp.general.untitled");
        DCValue[] titles = item.getMetadata("dc", "title", null, Item.ANY);
        if (titles.length >= 1)
        {
            title = titles[0].value;
            
        }
        else
        {
         // do nothing the title is allready set to untitled
            
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

        <table>     
          <tr>
            <td>
                <% if (browsetype.equals("Add")) { %>
                        <input type="submit" value="<fmt:message key="jsp.tools.general.add"/>" />
                <% } else if (browsetype.equals("Remove")) { %>
                        <input type="submit" value="<fmt:message key="jsp.tools.general.remove"/>" />
                <% } %>
            </td>
            <td><input type="submit" name="cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" /></td>
          </tr>
        </table>

    </form>

</dspace:layout>
