<%--
  - confirm-delete-collection.jsp
  -
  - Version: $Revision: 3705 $
  -
  - Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
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
		navbar="admin"
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
        <fmt:param><%= collection.getMetadata("name") %></fmt:param>
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

        <center>
            <table width="70%">
                <tr>
                    <td align="left">
                        <input type="submit" name="submit" value="<fmt:message key="jsp.tools.general.delete"/>"/>
                    </td>
                    <td align="right">
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>"/>
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
