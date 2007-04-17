<%--
  - items_by_author.jsp
  -
  - Version: $Revision: 1.6 $
  -
  - Date: $Date: 2004/03/23 19:22:29 $
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
  - Display items by a particular author
  -
  - Attributes to pass in:
  -
  -   community      - pass in if the scope of the browse is a community, or
  -                    a collection within this community
  -   collection     - pass in if the scope of the browse is a collection
  -   browse.info    - the BrowseInfo containing the items to display
  -   author         - The name of the author
  -   sort.by.date   - Boolean.  if true, we're sorting by date, otherwise by
  -                    title.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.net.URLEncoder" %>

<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.core.Utils" %>

<%
    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");
    BrowseInfo browseInfo = (BrowseInfo) request.getAttribute("browse.info" );
    String author = (String) request.getAttribute("author");
    boolean orderByTitle = ((Boolean) request.getAttribute("order.by.title")).booleanValue();


    // Description of what the user is actually browsing
    String scopeName = "All of DSpace";
    if (collection != null)
    {
        scopeName = collection.getMetadata("name");
    }
    else if (community != null)
    {
        scopeName = community.getMetadata("name");
    }

    String pageTitle = "Items for Author " + author;
%>

<dspace:layout title="<%= pageTitle %>">

    <H2>Items for Author <%= Utils.addEntities(author) %> in <%= scopeName %></H2>

    <%-- Sorting controls --%>
    <table border=0 cellpadding=10 align=center>
        <tr>
            <td colspan=2 align=center class=standard>
                <a href="browse-author?starts_with=<%= URLEncoder.encode(author) %>">Return to Browse by Author</A>
            </td>
        </tr>
        <tr>
            <td class=standard>
<%
    if (orderByTitle)
    {
%>
                <strong>Sorting by Title</strong>
            </td>
            <td class=standard>
                <a href="items-by-author?author=<%= URLEncoder.encode(author) %>&order=date">Sort by Date</a>
<%
    }
    else
    {
%>
                <a href="items-by-author?author=<%= URLEncoder.encode(author) %>&order=title">Sort by Title</a>
            </td>
            <td class=standard>
                <strong>Sorting by Date</strong>
<%
    }
%>
            </td>
        </tr>
    </table>

    <P align=center>Showing <%= browseInfo.getResultCount() %> items.</P>


    <%-- The items --%>
<%
    String emphColumn = (orderByTitle ? "title" : "date");
%>
    <dspace:itemlist items="<%= browseInfo.getItemResults() %>" emphcolumn="<%= emphColumn %>" />


</dspace:layout>
