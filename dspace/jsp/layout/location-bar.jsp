<%--
  - location-bar.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
  - Location bar component
  -
  - This component displays the "breadcrumb" style navigation aid at the top
  - of most screens.  Also indicates whether or not the user is logged in.
  -
  - Uses request attributes set in org.dspace.app.webui.jsptag.Layout, and
  - hence must only be used as part of the execution of that tag.  Plus,
  - dspace.layout.locbar should be verified to be true before this is included.
  -
  -  dspace.layout.parenttitles - List of titles of parent pages
  -  dspace.layout.parentlinks  - List of URLs of parent pages, empty string
  -                               for non-links
  -  dspace.current.user        - Current EPerson user, or null
  --%>

<%@ page import="java.util.List" %>

<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.eperson.EPerson" %>


<%
    List parentTitles = (List) request.getAttribute("dspace.layout.parenttitles");
    List parentLinks = (List) request.getAttribute("dspace.layout.parentlinks");
    EPerson eperson = (EPerson) request.getAttribute("dspace.current.user");
%>
<%-- HACK: Have to put some non-break spaces because "margin-left" CSS
  -        property is poorly supported
  -  HACK: height=32 inserted for Netscape 4.x which doesn't recognise
  -        the "height: 2.0em" CSS property. --%>

<td height=32 class="locationBar">
    <table width=100% border=0 cellpadding=0 cellspacing=0>
        <tr>
        <td class="locationBarCell">&nbsp;&nbsp;&nbsp;</td>
        <td class="locationBarCell" width="100%">
<%
    for (int i = 0; i < parentTitles.size(); i++)
    {
        String s = UIUtil.nonBreakSpace((String) parentTitles.get(i));
        String u = (String) parentLinks.get(i);

        if (i != 0)
        {
            // Not the first element, so need a preceding chevron
%>&gt;<%
        }

        if (u.equals(""))
        {
%>
  <%= s %>
<%
        }
        else
        {
%>
  <A HREF="<%= request.getContextPath() %><%= u %>"><%= s %></A>
<%
        }
    }
%>
        </td>
        <td class="locationBarCell">&nbsp;&nbsp;&nbsp;</td>
<%
    if (eperson != null)
    {
%>
<%-- HACK: &nbsp's are a workaround for Netscape 4.x, which doesn't honour
  -- "white-space: nowrap" CSS property --%>
        <td class="loggedInCell">
            Logged&nbsp;in&nbsp;as&nbsp;<%= eperson.getEmail() %>&nbsp;&nbsp;&nbsp;
      </td>
<%
    }
%>
    </tr>
  </table>
</td>
