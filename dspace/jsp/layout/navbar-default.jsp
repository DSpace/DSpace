<%--
  - navbar-default.jsp
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
  - Default navigation bar
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.eperson.EPerson" %>

<%
    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");
    
    // Get the current page, minus query string
    String currentPage = UIUtil.getOriginalURL(request);    
    int c = currentPage.indexOf( '?' );
    if( c > -1 )
    {
        currentPage = currentPage.substring( 0, c );
    }

    // E-mail may have to be truncated
    String navbarEmail = null;
    
    if (user != null)
    {
        navbarEmail = user.getEmail();
        if (navbarEmail.length() > 18)
        {
            navbarEmail = navbarEmail.substring(0, 17) + "...";
        }
    }
%>

<%-- Search Box --%>
<form method=GET action="<%= request.getContextPath() %>/simple-search">

<%
    if (user != null)
    {
%>
  <P class="loggedIn">Logged&nbsp;in&nbsp;as <%= navbarEmail %>
    (<A HREF="<%= request.getContextPath() %>/logout">Logout</A>)</P>
<%
    }
%> 
  <table width="100%" class="searchBox">
    <tr>
      <td>
        <table width="100%" border=0 cellspacing=0 padding=2>
          <tr>
            <td class="searchBoxLabel">Search&nbsp;DSpace:</td>
          </tr>
          <tr>
            <td valign=middle nowrap>
              <input type=text name=query size=10><input type=image border=0 src="<%= request.getContextPath() %>/image/search-go.gif" name=submit alt="Go" value="Go">
            </td>
          </tr>
        </table>
      </td>
    </tr>
    <tr>
  </table>
</form>

<%-- HACK: width, border, cellspacing, cellpadding: for non-CSS compliant Netscape, Mozilla browsers --%>
<table width="100%" border="0" cellspacing="2" cellpadding="2">

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= (currentPage.endsWith("/index.jsp") ? "arrow-highlight" : "arrow") %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/">Home</a>
    </td>
  </tr>

  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>

  <tr>
    <td nowrap colspan="2" class="navigationBarSublabel">Browse</td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/community-list" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/community-list">Communities<br />&amp;&nbsp;Collections</a>
    </td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/browse-title" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/browse-title">Titles</a>
    </td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/browse-author" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/browse-author">Authors</a>
    </td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/browse-date" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/browse-date">By Date</a>
    </td>
  </tr>

  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>

  <tr>
    <td nowrap colspan="2" class="navigationBarSublabel">Sign on to:</td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/subscribe" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/subscribe">Receive email<br>updates</a>
    </td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/mydspace" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/mydspace">My DSpace</a><br><small>authorized users</small>
    </td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/profile" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <a href="<%= request.getContextPath() %>/profile">Edit Profile</a>
    </td>
  </tr>

  <tr>
    <td colspan="2">&nbsp;</td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/help" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <dspace:popup page="/help/index.html">Help</dspace:popup>
    </td>
  </tr>

  <tr class="navigationBarItem">
    <td>
      <img alt="" src="<%= request.getContextPath() %>/image/<%= ( currentPage.endsWith( "/about" ) ? "arrow-highlight" : "arrow" ) %>.gif" width="16" height="16">
    </td>
    <td nowrap class="navigationBarItem">
      <a href="http://www.dspace.org/">About DSpace</a>
    </td>
  </tr>

</table>
