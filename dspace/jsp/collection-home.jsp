<%--
  - collection-home.jsp
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
  - Collection home JSP
  -
  - Attributes required:
  -    collection  - Collection to render home page for
  -    community   - Community this collection is in
  -    last.submitted.titles - String[], titles of recent submissions
  -    last.submitted.urls   - String[], corresponding URLs
  -    logged.in  - Boolean, true if a user is logged in
  -    subscribed - Boolean, true if user is subscribed to this collection
  -    admin_button - Boolean, show admin 'edit' button
  -    editor_button - Boolean, show collection editor (edit submitters, item mapping) buttons
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection"%>
<%@ page import="org.dspace.core.Utils"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.dspace.eperson.Group"     %>


<%
    // Retrieve attributes
    Collection collection = (Collection) request.getAttribute("collection");
    Community  community  = (Community) request.getAttribute("community");
    Group      submitters = (Group) request.getAttribute("submitters");

    String[] lastSubmittedTitles = (String[])
        request.getAttribute("last.submitted.titles");
    String[] lastSubmittedURLs = (String[])
        request.getAttribute("last.submitted.urls");

    boolean loggedIn =
        ((Boolean) request.getAttribute("logged.in")).booleanValue();
    boolean subscribed =
        ((Boolean) request.getAttribute("subscribed")).booleanValue();
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());

    Boolean editor_b      = (Boolean)request.getAttribute("editor_button");
    boolean editor_button = (editor_b == null ? false : editor_b.booleanValue());

    Boolean submit_b      = (Boolean)request.getAttribute("can_submit_button");
    boolean submit_button = (submit_b == null ? false : submit_b.booleanValue());


    // Put the metadata values into guaranteed non-null variables
    String name = collection.getMetadata("name");
    String intro = collection.getMetadata("introductory_text");
    if (intro == null)
    {
        intro = "";
    }
    String copyright = collection.getMetadata("copyright_text");
    if (copyright == null)
    {
        copyright = "";
    }
    String sidebar = collection.getMetadata("side_bar_text");
    if(sidebar == null)
    {
        sidebar = "";
    }

    String communityName = community.getMetadata("name");
    String communityLink = "/handle/" + community.getHandle();

    Bitstream logo = collection.getLogo();
%>

<dspace:layout locbar="commLink" title="<%= name %>">

<table border=0 cellpadding=5 width=100%>
    <tr>
      <td width=100%>
        <H1><%= name %>
<%
            if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
            {
%>
                : [<%= collection.countItems() %>]
<%
            }
%>
		</H1>

		<H3><fmt:message key="jsp.collection-home.heading1"/></H3>
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
				<small><strong><fmt:message key="jsp.general.location"/></strong></small>&nbsp;<select name="location">
				  <option value="/"><fmt:message key="jsp.general.genericScope"/></option>
                  <option selected value="<%= community.getHandle() %>"><%= communityName %></option>
                  <option selected value="<%= collection.getHandle() %>/"><%= name %></option>
                </select>
              </td>
            </tr>
            <tr>
              <td class="standard" align=center>
				<small><fmt:message key="jsp.general.searchfor"/>&nbsp;</small><input type="text" name="query">&nbsp;<input type="submit" name="submit_search" value="<fmt:message key="jsp.general.go"/>">
              </td>
            </tr>
            <tr>
              <td align=center class="standard">
                <small><fmt:message key="jsp.general.orbrowse"/>&nbsp;</small><input type="submit" name="submit_titles" value="<fmt:message key="jsp.general.titles.button"/>">&nbsp;<input type="submit" name="submit_authors" value="<fmt:message key="jsp.general.authors.button"/>">&nbsp;<input type="submit" name="submit_dates" value="<fmt:message key="jsp.general.date.button"/>">
			  </td>
            </tr>
          </table>
        </td>
      </tr>
    </table>
  </form>

  <table width="100%" align="center" cellspacing=10>
    <tr>
      <td>
<%-- HACK: <center> used for Netscape 4.x, which doesn't accept align=center
  for a paragraph with a button in it --%>
<%  if (submit_button)
    { %>
        <center>
          <form action="<%= request.getContextPath() %>/submit" method=POST>
            <input type=hidden name=collection value="<%= collection.getID() %>">
            <%--<input type=submit name=submit value="Submit to This Collection">--%>
			<input type=submit name=submit value="<fmt:message key="jsp.collection-home.submit.button"/>">
          </form>
        </center>
<%  } %>
      </td>
      <td class="oddRowEvenCol">
        <form method=GET>
          <table>
            <tr>
              <td class="standard">
<%  if (loggedIn && subscribed)
    { %>
                <%--<small>You are subscribed to this collection. <A HREF="<%= request.getContextPath() %>/subscribe">See&nbsp;Subscriptions</A></small>--%>
                <small><fmt:message key="jsp.collection-home.subscribed"/> <A HREF="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.collection-home.info"/></A></small>
			  </td>
              <td class="standard">
                <%--<input type="submit" name="submit_unsubscribe" value="Unsubscribe">--%>
				<input type="submit" name="submit_unsubscribe" value="<fmt:message key="jsp.collection-home.unsub"/>">
<%  } else { %>
                <small>
                  <%--Subscribe to this collection to receive daily e-mail notification of new additions--%>
				  <fmt:message key="jsp.collection-home.subscribe.msg"/>
                </small>
              </td>
              <td class="standard">
                <%--<input type="submit" name="submit_subscribe" value="Subscribe">--%>
				<input type="submit" name="submit_subscribe" value="<fmt:message key="jsp.collection-home.subscribe"/>">
<%  } %>
              </td>
            </tr>
          </table>
        </td>
      </form>
    </tr>
  </table>

  <%= intro %>



  <P class="copyrightText"><%= copyright %></P>

  <dspace:sidebar>
<% if(admin_button || editor_button ) { %>
    <table class=miscTable align=center>
      <tr>
	<td class="evenRowEvenCol" colspan=2>
	  <table>
            <tr>
              <th class="standard">
                 <%--<strong>Admin Tools</strong>--%>
				 <strong><fmt:message key="jsp.admintools"/></strong>
              <th>
            </tr>

<% if( editor_button ) { %>
            <tr>
              <td class="standard" align="center">
                 <form method=POST action="<%=request.getContextPath()%>/tools/edit-communities">
                  <input type="hidden" name="collection_id" value="<%= collection.getID() %>">
                  <input type="hidden" name="community_id" value="<%= community.getID() %>">
                  <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_EDIT_COLLECTION %>">
                  <%--<input type="submit" value="Edit...">--%>
				  <input type="submit" value="<fmt:message key="jsp.general.edit.button"/>">
                </form>
              </td>
            </tr>
<% } %>

<% if( admin_button ) { %>
            <tr>
              <td class="standard" align="center">
                 <form method=POST action="<%=request.getContextPath()%>/tools/itemmap">
                  <input type="hidden" name="cid" value="<%= collection.getID() %>">
                  <%--<input type="submit" value="Item Mapper">--%>
				  <input type="submit" value="<fmt:message key="jsp.collection-home.item.button"/>">
                </form>
              </td>
            </tr>
<% if(submitters != null) { %>
            <tr>
	      <td class="standard" align="center">
		<form method=POST action="<%=request.getContextPath()%>/tools/group-edit">
		  <input type=hidden name="group_id" value="<%=submitters.getID()%>">
		  <%--<input type=submit name="submit_edit" value="Edit Submitters">--%>
		  <input type=submit name="submit_edit" value="<fmt:message key="jsp.collection-home.editsub.button"/>">
		</form>
	      </td>
            </tr>
<% } %>
            <tr>
              <td class="standard" align="center">
                 <dspace:popup page="/help/collection-admin.html"><fmt:message key="jsp.adminhelp"/></dspace:popup>
              </td>
            </tr>
<% } %>

	  </table>
	</td>
      </tr>
    </table>
<%  } %>


	<H3><fmt:message key="jsp.collection-home.recentsub"/></H3>
<%
    for (int i = 0; i < lastSubmittedTitles.length; i++)
    {
    	String displayTitle = (lastSubmittedTitles[i] == null
    		? LocaleSupport.getLocalizedMessage(pageContext, "jsp.general.untitled")
    		: Utils.addEntities(lastSubmittedTitles[i]));
    		
%>
    <P class="recentItem"><A HREF="<%= request.getContextPath() %><%= lastSubmittedURLs[i] %>"><%= displayTitle %></A></P>
<%
  }
%>
    <%= sidebar %>
  </dspace:sidebar>

</dspace:layout>
