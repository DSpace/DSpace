<%--
  - edit-community.jsp
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
  - Show form allowing edit of community metadata
  -
  - Attributes:
  -    community   - community to edit, if editing an existing one.  If this
  -                  is null, we are creating one.
  --%>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Community" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Community community = (Community) request.getAttribute("community");
    
    String name = "";
    String shortDesc = "";
    String intro = "";
    String copy = "";
    String side = "";

    Bitstream logo = null;
    
    if (community != null)
    {
        name = community.getMetadata("name");
        shortDesc = community.getMetadata("short_description");
        intro = community.getMetadata("introductory_text");
        copy = community.getMetadata("copyright_text");
        side = community.getMetadata("side_bar_text");
        
        if (copy == null)
        {
            copy = "";
        }
        
        if (side == null)
        {
            side = "";
        }

        logo = community.getLogo();
    }
%>

<dspace:layout title="Edit Community" navbar="admin" locbar="link" parentlink="/admin" parenttitle="Administer">

<%
    if (community == null)
    {
%>
    <H1>Create Community</H1>
<%
    }
    else
    {
%>
    <H1>Edit Community ID: <%= community.getID() %></H1>
<%
    }
%>

    <form method=POST>
        <table>
<%-- ===========================================================
     Basic metadata
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel">Name:</td>
                <td><input type="text" name="name" value="<%= name %>" size=50></td>
            </tr>
            <tr>
                <td class="submitFormLabel">Short Description</td>
                <td>
                    <input type="text" name="short_description" value="<%= shortDesc %>" size=50>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel">Introductory text (HTML):</td>
                <td>
                    <textarea name="introductory_text" rows=6 cols=50><%= intro %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel">Copyright text (plain text):</td>
                <td>
                    <textarea name="copyright_text" rows=6 cols=50><%= copy %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel">Side bar text (HTML):</td>
                <td>
                    <textarea name="side_bar_text" rows=6 cols=50><%= side %></textarea>
                </td>
            </tr>
<%-- ===========================================================
     Logo
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel">Logo:</td>
                <td>
<%  if (logo != null) { %>
                    <table>
                        <tr>
                            <td>
                                <img src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>">
                            </td>
                            <td>
                                <input type="submit" name="submit_set_logo" value="Upload new logo"><br><br>
                                <input type="submit" name="submit_delete_logo" value="Delete (no logo)">
                            </td>
                        </tr>
                    </table>
<%  } else { %>
                    <input type="submit" name="submit_set_logo" value="Upload a logo">
<%  } %>
                </td>
            </tr>
        </table>

        <P>&nbsp;</P>

        <center>
            <table width="70%">
                <tr>
                    <td class="standard">
                        
<%
    if (community == null)
    {
%>
                        <input type="hidden" name="create" value="true">
                        <input type="submit" name="submit" value="Create">
<%
    }
    else
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>">
                        <input type="hidden" name="create" value="false">
                        <input type="submit" name="submit" value="Update">
<%
    }
%>

                    </td>
                    <td>
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COMMUNITY %>">
                        <input type="submit" name="submit_cancel" value="Cancel">
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>
