<%--
  - navbar-admin.jsp
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
  - Navigation bar for admin pages
  --%>

<%@ page import="java.util.LinkedList" %>
<%@ page import="java.util.List" %>

<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%
    // Build up lists of menu item labels and links
    List labels = new LinkedList();
    List links = new LinkedList();
    
    labels.add("Communities/<br>Collections");
    links.add("edit-communities");
    
    labels.add("E-people");
    links.add("edit-epeople");
    
    labels.add("Groups");
    links.add("groups");
    
    labels.add("Correct/<br>Expunge Item");
    links.add("edit-item");
    
    labels.add("Dublin Core<br>Registry");
    links.add("dc-registry");
    
    labels.add("Bitstream Format<br>Registry");
    links.add("format-registry");
    
    labels.add("Workflow");
    links.add("workflow");
    
    labels.add("Authorization");
    links.add("authorize");

    // Get the current page, minus query string
    String currentPage = UIUtil.getOriginalURL(request);    
    int c = currentPage.indexOf( '?' );
    if( c > -1 )
    {
        currentPage = currentPage.substring(0, c);
    }
%>

<%-- HACK: width, border, cellspacing, cellpadding: for non-CSS compliant Netscape, Mozilla browsers --%>
<table width="100%" border="0" cellspacing="2" cellpadding="2">
<%
    for (int i = 0; i < labels.size(); i++)
    {
        String s = (String) labels.get(i);
        String l = (String) links.get(i);

        // Use "highlit" arrow if label corresponds to current page
        String image = (currentPage.endsWith(l) ? "arrow-highlight" : "arrow");
%>            
    <tr class="navigationBarItem">
        <td>
            <img alt="" src="<%= request.getContextPath() %>/image/<%= image %>.gif" width="16" height="16">
        </td>
        <td nowrap class="navigationBarItem">
            <a href="<%= request.getContextPath() %>/admin/<%= l %>"><%= s %></a>
        </td>
    </tr>
<%
    }
%>
    <tr>
        <td colspan="2">&nbsp;</td>
    </tr>

    <tr class="navigationBarItem">
        <td>
            <img alt="" src="<%= request.getContextPath() %>/image/arrow.gif" width="16" height="16">
        </td>
        <td nowrap class="navigationBarItem">
            <a href="<%= request.getContextPath() %>/logout">Log Out</a>
        </td>
    </tr>
</table>
