<%--
  - list-communities.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
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
  - Display hierarchical list of communities and collections for admin editing
  -
  - Attributes to be passed in:
  -    communities     - array of communities
  -    collections.map  - Map where a keys is a community IDs and the value is
  -                      the array of collections in that community.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.Map" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");
    Map collectionMap = (Map) request.getAttribute("collections.map");
%>

<dspace:layout title="Edit Communities and Collections" navbar="admin" locbar="link" parentlink="/dspace-admin" parenttitle="Administer">

    <H1>Edit Communities and Collections</H1>
    
    <form method=POST>
        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_CREATE_COMMUNITY %>">
        <P align="center"><input type="submit" name="submit" value="Create Top Community..."></P>
    </form>

    <table class="miscTable" align="center">
<%
    for (int i = 0; i < communities.length; i++)
    {
%>
        <tr class="evenRowEvenCol">
            <td>
                <strong><A HREF="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><%= communities[i].getMetadata("name") %></A></strong>
                <small><%= communities[i].getHandle() %> (DB ID: <%= communities[i].getID()%>)</small>
            </td>
            <form method=POST>
                <td>
                    <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_EDIT_COMMUNITY %>">
                    <input type="hidden" name="community_id" value="<%= communities[i].getID() %>">
                    <input type="submit" name="submit" value="Edit...">
                </td>
            </form>
            <form method=POST>
                <td>
                    <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_DELETE_COMMUNITY %>">
                    <input type="hidden" name="community_id" value="<%= communities[i].getID() %>">
                    <input type="submit" name="submit" value="Delete...">
                </td>
            </form>
            <form method=POST>
                <td>
                    <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_CREATE_COLLECTION %>">
                    <input type="hidden" name="community_id" value="<%= communities[i].getID() %>">
                    <input type="submit" name="submit" value="Create Collection...">
                </td>
            </form>
            <form method=POST>
                <td>
                    <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_CREATE_COMMUNITY %>">
                    <input type="hidden" name="parent_community_id" value="<%= communities[i].getID() %>">
                    <input type="submit" name="submit" value="Create Sub-community...">
                </td>
            </form>
        </tr>
<%
        Collection[] collections =
            (Collection[]) collectionMap.get(new Integer(communities[i].getID()));

        for (int j = 0; j < collections.length; j++)
        {
%>
        <tr class="oddRowOddCol">
            <td>
                &nbsp;&nbsp;&nbsp;&nbsp;<A HREF="<%= request.getContextPath() %>/handle/<%= collections[j].getHandle() %>"><%= collections[j].getMetadata("name") %></A>
                <small><%= collections[j].getHandle() %> (DB ID: <%= collections[j].getID()%>)</small>
            </td>
            <form method=POST>
                <td>
                    <input type="hidden" name="action"        value="<%= EditCommunitiesServlet.START_EDIT_COLLECTION %>">
                    <input type="hidden" name="collection_id" value="<%= collections[j].getID() %>">
                    <input type="submit" name="submit"        value="Edit...">
                </td>
            </form>
            <form method=POST>
                <td>
                    <input type="hidden" name="action"        value="<%= EditCommunitiesServlet.START_DELETE_COLLECTION %>">
                    <input type="hidden" name="collection_id" value="<%= collections[j].getID() %>">
                    <input type="hidden" name="community_id"  value="<%= communities[i].getID() %>">
                    <input type="submit" name="submit"        value="Delete...">
                </td>
            </form>
        </tr>
<%
        }
%>
        <tr></tr>
<%
    }
%>
    </table>
</dspace:layout>
