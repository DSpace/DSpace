<%--
  - itemmap-browse.jsp
  -
  - Version: $ $
  -
  - Date: $ $
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
  - Display the results of an item browse
  -
  - Attributes to pass in:
  -
  -   items          - sorted Map of Items to display
  -   collection     - Collection we're managing
  -   collections    - Map of Collections, keyed by collection_id
  -   browsetext     - text to display at the top

  --%>
  
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.net.URLEncoder"            %>
<%@ page import="java.util.Iterator"             %>
<%@ page import="java.util.Map"                  %>
<%@ page import="org.dspace.content.Collection"  %>
<%@ page import="org.dspace.content.Item"        %>

<%
    Collection collection  = (Collection)request.getAttribute("collection");
    Map items              = (Map)request.getAttribute("items");
    Map collections        = (Map)request.getAttribute("collections");
    String browsetext      = (String)request.getAttribute("browsetext");
    Boolean showcollection = new Boolean(false);
    String browsetype      = (String)request.getAttribute("browsetype");
%>

<dspace:layout title="Browse Items">

    <h2>Browse <%=browsetext%></h2>

    <form method=POST action="<%= request.getContextPath() %>/tools/itemmap">
        <input type="hidden" name="cid" value="<%=collection.getID()%>">
        <input type="submit" name="submit" value="Cancel">

        <table class="miscTable" align="center">
        <tr>
            <th class="oddRowOddCol"><strong>Date</strong></th>
            <th class="oddRowEvenCol"><strong>First Author</strong></th>
            <th class="oddRowOddCol"><strong>Title</strong></th>
            <% if(showcollection.booleanValue()) { %>
                <th class="oddRowEvenCol"><strong>Action</strong></th>
                <th class="oddRowOddCol"> Remove </th>
            <% } else { %>     
                <th class="oddRowEvenCol"><input type=submit name="action"
                value="<%=browsetype%>"</th>
            <% } %>     
        </tr>
<%
    String row = "even";
    Iterator i = items.keySet().iterator();

    while( i.hasNext() )
    {
        Item item = (Item)items.get(i.next());    
%>
        <tr>
        <td class="<%= row %>RowOddCol">
        <%= item.getDC("date", "issued", Item.ANY)[0].value %>
        </td>
        <td class="<%= row %>RowEvenCol">
        <%= item.getDC("contributor", Item.ANY, Item.ANY)[0].value %>
        </td>
        <td class="<%= row %>RowOddCol">
          <%= item.getDC("title", null, Item.ANY)[0].value %></td>

<% if( showcollection.booleanValue() ) { %>
<%-- not currently implemented --%>
        <td class="<%= row %>RowEvenCol">  <%= collection.getID() %>
        <td class="<%= row %>RowOddCol"><%= item.getDC("title", null, Item.ANY)[0].value %></td>

<% } else { %>

        <td class="<%= row %>RowEvenCol"><input type=checkbox name="item_ids"
        value="<%=item.getID()%>">

<% }

        row = (row.equals("odd") ? "even" : "odd");
%>
        </tr>
<% } %>
        
        </table>     
    </form>

</dspace:layout>        
