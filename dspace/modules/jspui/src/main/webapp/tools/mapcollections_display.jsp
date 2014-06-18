<%--
  - edit-community.jsp
  -
  - Version: $Revision: 1.1 $
  -
  - Date: $Date: 2004/12/13 16:59:43 $
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
  - Show form allowing edit of community metadata
  -
  - Attributes:
  -    community   - community to edit, if editing an existing one.  If this
  -                  is null, we are creating one.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.CommunityGroup" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Context context = UIUtil.obtainContext((HttpServletRequest)request);

    Community community = (Community) request.getAttribute("community");
    Collection collection = (Collection) request.getAttribute("collection");
    String action = (String) request.getParameter("action");
%>

<dspace:layout title="Add/Remove Collections" navbar="admin" locbar="link" parentlink="/dspace-admin" parenttitle="Administer" nocache="true">

  <table width=95%>
    <tr>
      <td align=left>

        <H1>Add/Remove Collections : <%= community.getMetadata("name") %></H1>

      </td>
      <td align="right" class="standard">
        <dspace:popup page="/help/site-admin.html#mapcollections">Help...</dspace:popup>
      </td>
    </tr>
  </table>

  <% if (action != null && action.equals("confirm_map")) { %>

    <h2>Adding Collection: <%= collection.getMetadata("name") %></h2>

    <p>Please wait until this page is completely finished loading to perform
    any more operations.</p>

    <p>
    <%
	   community.addCollection(collection, pageContext.getOut());
	   context.commit();
    %>
    </p>

  <% } else if (action != null && action.equals("confirm_unmap")) { %>

    <h2>Removing Collection: <%= collection.getMetadata("name") %></h2>

    <p>Please wait until this page is completely finished loading to perform
    any more operations.</p>

    <p>
    <%
	   community.removeCollection(collection, pageContext.getOut());
	   context.commit();
    %>
    </p>

  <% } %>

  <center>

  </center>
</dspace:layout>
