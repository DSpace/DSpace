<%--
  - community-home.jsp
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
  - Community home JSP
  -
  - Attributes required:
  -    community             - Community to render home page for
  -    collections           - array of Collections in this community
  -    last.submitted.titles - String[] of titles of recently submitted items
  -    last.submitted.urls   - String[] of URLs of recently submitted items
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>


<%
    // Retrieve attributes
    Community community = (Community) request.getAttribute( "community" );
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");
    String[] lastSubmittedTitles = (String[])
        request.getAttribute("last.submitted.titles");
    String[] lastSubmittedURLs = (String[])
        request.getAttribute("last.submitted.urls");


    // Put the metadata values into guaranteed non-null variables
    String name = community.getMetadata("name");
    String intro = community.getMetadata("introductory_text");
    if (intro == null)
    {
        intro = "";
    }
    String copyright = community.getMetadata("copyright_text");
    if (copyright == null)
    {
        copyright = "";
    }
    String sidebar = community.getMetadata("side_bar_text");
    if(sidebar == null)
    sidebar = "";

    Bitstream logo = community.getLogo();
%>


<dspace:layout locbar="link" title="<%= name %>">
  <table border=0 cellpadding=5 width=100%>
    <tr>
      <td width=100%>
        <H1><%= name %></H1>
        <H3>Community home page</H3>
      </td>
      <td valign=top>
<%  if (logo != null) { %>
        <img alt="Logo" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>">
<% } %></td>
    </tr>
  </table>


  <%-- Search/Browse --%>
  <form method=GET>
    <table class=miscTable align=center>
      <tr>
        <td class="evenRowEvenCol" colspan=2>
          <table>
            <tr>
              <td class="standard">
                <small><strong>In:</strong></small>&nbsp;<select name="location">
                  <option value="/">All of DSpace</option>
                  <option selected value="/communities/<%= community.getID() %>/"><%= name %></option>
<%
    for (int i = 0; i < collections.length; i++)
    {
%>    
                  <option value="/communities/<%= community.getID() %>/collections/<%= collections[i].getID() %>/"><%= collections[i].getMetadata("name") %></option>
<%
    }
%>
                </select>
              </td>
            </tr>
            <tr>
              <td class="standard" align=center>
                <small><strong>Search</strong>&nbsp;for&nbsp;</small><input type="text" name="query">&nbsp;<input type="submit" name="submit_search" value="Go">
              </td>
            </tr>
            <tr>
              <td align=center class="standard">
                <small>or&nbsp;<strong>browse</strong>&nbsp;</small><input type="submit" name="submit_titles" value="Titles">&nbsp;<input type="submit" name="submit_authors" value="Authors">&nbsp;<input type="submit" name="submit_dates" value="By Date">
              </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </form>

  <%= intro %>

  <H2>Collections in this community</H2>
   
<%
    if (collections.length == 0)
    {
%>
  <P>This community contains no collections.</P>
<%
    }
    else
    {
%>
  <UL class="collectionListItem">
<%
        for (int i = 0; i < collections.length; i++)
        {
%>
    <LI>
      <A HREF="collections/<%= collections[i].getID() %>/">
      <%= collections[i].getMetadata("name") %></A>
      <P class="collectionDescription"><%= collections[i].getMetadata("short_description") %></P>
    </LI>
<%
        }
%>
  </UL>
<%
    }
%>

  <P class="copyrightText"><%= copyright %></P>

  <dspace:sidebar>
    <H3>Recent&nbsp;Submissions</H3>
    
<%
    for (int i = 0; i < lastSubmittedTitles.length; i++)
    {
%>
    <P class="recentItem"><A HREF="<%= lastSubmittedURLs[i] %>"><%= lastSubmittedTitles[i] %></A></P>
<%
  }
%>
    <P>&nbsp;</P>

    <%= sidebar %>

  </dspace:sidebar>



</dspace:layout>
