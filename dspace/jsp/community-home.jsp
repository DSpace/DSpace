<%--
  - community-home.jsp
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
  - Community home JSP
  -
  - Attributes required:
  -    community             - Community to render home page for
  -    collections           - array of Collections in this community
  -    subcommunities        - array of Sub-communities in this community
  -    last.submitted.titles - String[] of titles of recently submitted items
  -    last.submitted.urls   - String[] of URLs of recently submitted items
  -    admin_button - Boolean, show admin 'edit' button
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
    // Retrieve attributes
    Community community = (Community) request.getAttribute( "community" );
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");
    Community[] subcommunities =
        (Community[]) request.getAttribute("subcommunities");

    String[] lastSubmittedTitles = (String[])
        request.getAttribute("last.submitted.titles");
    String[] lastSubmittedURLs = (String[])
        request.getAttribute("last.submitted.urls");
    Boolean editor_b = (Boolean)request.getAttribute("editor_button");
    boolean editor_button = (editor_b == null ? false : editor_b.booleanValue());
    Boolean add_b = (Boolean)request.getAttribute("add_button");
    boolean add_button = (add_b == null ? false : add_b.booleanValue());
    Boolean remove_b = (Boolean)request.getAttribute("remove_button");
    boolean remove_button = (remove_b == null ? false : remove_b.booleanValue());


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

<dspace:layout locbar="commLink" title="<%= name %>">

<table border=0 cellpadding=5 width=100%>
    <tr>
      <td width=100%>
        <H1><%= name %></H1>
		<H3><fmt:message key="jsp.community-home.heading1"/></H3>
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
                <%--<small><strong>In:</strong></small>&nbsp;<select name="location">--%>
				<small><strong><fmt:message key="jsp.community-home.location1"/></strong></small>&nbsp;<select name="location">
                  <%--<option value="/">All of DSpace</option>--%>
				  <option value="/"><fmt:message key="jsp.community-home.location2"/></option>
                  <option selected value="<%= community.getHandle() %>"><%= name %></option>
<%
    for (int i = 0; i < collections.length; i++)
    {
%>    
                  <option value="<%= collections[i].getHandle() %>"><%= collections[i].getMetadata("name") %></option>
<%
    }
%>
<%
    for (int j = 0; j < subcommunities.length; j++)
    {
%>    
                  <option value="<%= subcommunities[j].getHandle() %>"><%= subcommunities[j].getMetadata("name") %></option>
<%
    }
%>
                </select>
              </td>
            </tr>
            <tr>
              <td class="standard" align=center>
                <small><fmt:message key="jsp.community-home.searchfor"/>&nbsp;</small><input type="text" name="query">&nbsp;<input type="submit" name="submit_search" value="<fmt:message key="jsp.community-home.go.button"/>"> 
			  </td>
            </tr>
            <tr>
              <td align=center class="standard">
                <small><fmt:message key="jsp.community-home.orbrowse"/>&nbsp;</small><input type="submit" name="submit_titles" value="<fmt:message key="jsp.community-home.titles.button"/>">&nbsp;<input type="submit" name="submit_authors" value="<fmt:message key="jsp.community-home.authors.button"/>">&nbsp;<input type="submit" name="submit_dates" value="<fmt:message key="jsp.community-home.date.button"/>">
			  </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </form>
    
  <%= intro %>

<%
    if (collections.length != 0)
    {
%>

        <%-- <H2>Collections in this community</H2> --%>
		<H2><fmt:message key="jsp.community-home.heading2"/></H2>
   
        <UL class="collectionListItem">
<%
        for (int i = 0; i < collections.length; i++)
        {
%>
    <LI>
	    <table>
	    <tr>
	    <td>
	      <A HREF="<%= request.getContextPath() %>/handle/<%= collections[i].getHandle() %>">
	      <%= collections[i].getMetadata("name") %></A>
<%
            if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
            {
%>
                [<%= collections[i].countItems() %>]
<%
            }
%>
	    </td>
	    <% if (remove_button) { %>
	    <td>
	      <form method=POST action="<%=request.getContextPath()%>/tools/edit-communities">
	          <input type="hidden" name="parent_community_id" value="<%= community.getID() %>">
	          <input type="hidden" name="community_id" value="<%= community.getID() %>">
	          <input type="hidden" name="collection_id" value="<%= collections[i].getID() %>">
	          <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_DELETE_COLLECTION%>">
	          <input type="image" src="<%= request.getContextPath() %>/image/remove.gif">
	      </form>
	    </td>
	    <% } %>
	    </tr>
	    </table>
      <P class="collectionDescription"><%= collections[i].getMetadata("short_description") %></P>
    </LI>
<%
        }
%>
  </UL>
<%
    }
%>

<%
    if (subcommunities.length != 0)
    {
%>
        <%--<H2>Sub-communities within this community</H2>--%>
		<H2><fmt:message key="jsp.community-home.heading3"/><H2>
   
        <UL class="collectionListItem">
<%
        for (int j = 0; j < subcommunities.length; j++)
        {
%>
            <LI>
			    <table>
			    <tr>
			    <td>
	                <A HREF="<%= request.getContextPath() %>/handle/<%= subcommunities[j].getHandle() %>">
	                <%= subcommunities[j].getMetadata("name") %></A>
<%
                if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
%>
                    [<%= subcommunities[j].countItems() %>]
<%
                }
%>
			    </td>
	    		<% if (remove_button) { %>
			    <td>
	                <form method=POST action="<%=request.getContextPath()%>/tools/edit-communities">
			          <input type="hidden" name="parent_community_id" value="<%= community.getID() %>">
			          <input type="hidden" name="community_id" value="<%= subcommunities[j].getID() %>">
			          <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_DELETE_COMMUNITY%>">
	                  <input type="image" src="<%= request.getContextPath() %>/image/remove.gif">
	                </form>
			    </td>
	    		<% } %>
			    </tr>
			    </table>
                <P class="collectionDescription"><%= subcommunities[j].getMetadata("short_description") %></P>
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
    <% if(editor_button || add_button)  // edit button(s)
    { %>
    <table class=miscTable align=center>
	  <tr>
	    <td class="evenRowEvenCol" colspan=2>
	      <table>
            <tr>
              <th class="standard">
                 <%--<strong>Admin Tools</strong>--%>
				 <strong><fmt:message key="jsp.admintools"/></strong>
              </th>
            </tr>
            <tr>
              <td class="standard" align="center">
             <% if(editor_button) { %>
	            <form method=POST action="<%=request.getContextPath()%>/tools/edit-communities">
		          <input type="hidden" name="community_id" value="<%= community.getID() %>">
		          <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_EDIT_COMMUNITY%>">
                  <%--<input type="submit" value="Edit...">--%>
				  <input type="submit" value="<fmt:message key="jsp.community-home.edit.button"/>">
                </form>
             <% } %>
             <% if(add_button) { %>
				<form method=POST action="<%=request.getContextPath()%>/tools/collection-wizard">
                    <input type="hidden" name="community_id" value="<%= community.getID() %>">
	   			    <input type="submit" value="<fmt:message key="jsp.community-home.create1.button"/>">
                </form>
                <form method=POST action="<%=request.getContextPath()%>/tools/edit-communities">
                    <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_CREATE_COMMUNITY%>">
                    <input type="hidden" name="parent_community_id" value="<%= community.getID() %>">
                    <%--<input type="submit" name="submit" value="Create Sub-community">--%>
					<input type="submit" name="submit" value="<fmt:message key="jsp.community-home.create2.button"/>">
                 </form>
             <% } %>
              </td>
            </tr>
            <tr>
              <td class="standard" align="center">
                 <dspace:popup page="/help/site-admin.html"><fmt:message key="jsp.adminhelp"/></dspace:popup>
              </td>
            </tr>
	  </table>
	</td>
      </tr>
    </table>
    <% } %>



	<H3><fmt:message key="jsp.community-home.recentsub"/></H3>
    
<%
    for (int i = 0; i < lastSubmittedTitles.length; i++)
    {
%>
    <P class="recentItem"><A HREF="<%= request.getContextPath() %><%= lastSubmittedURLs[i] %>"><%= Utils.addEntities(lastSubmittedTitles[i]) %></A></P>
<%
  }
%>
    <P>&nbsp;</P>

    <%= sidebar %>

  </dspace:sidebar>



</dspace:layout>
