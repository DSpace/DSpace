<%--
  - edit-community.jsp
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
  - Show form allowing edit of community metadata
  -
  - Attributes:
  -    community   - community to edit, if editing an existing one.  If this
  -                  is null, we are creating one.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    Community community = (Community) request.getAttribute("community");
    int parentID = UIUtil.getIntParameter(request, "parent_community_id");
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
    
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
        logo = community.getLogo();
    }
%>

<dspace:layout titlekey="jsp.tools.edit-community.title"
		       navbar="admin"
		       locbar="link"
		       parentlink="/dspace-admin"
		       parenttitlekey="jsp.administer" nocache="true">

  <table width="95%">
    <tr>
      <td align="left">
<%
    if (community == null)
    {
%>
    <h1><fmt:message key="jsp.tools.edit-community.heading1"/></h1>
<%
    }
    else
    {
%>
    <h1><fmt:message key="jsp.tools.edit-community.heading2">
        <fmt:param><%= community.getHandle() %></fmt:param>
        </fmt:message>
    </h1>
    <% if(admin_button ) { %>
      <center>
        <table width="70%">
          <tr>
            <td class="standard">
              <form method="post" action="">
                <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_DELETE_COMMUNITY %>" />
                <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-community.button.delete"/>" />
              </form>
            </td>
          </tr>
        </table>
      </center>
    <% } %>
<%
    }
%>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#editcommunity\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

    <form method="post" action="">
        <table>
<%-- ===========================================================
     Basic metadata
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label1"/></td>
                <td><input type="text" name="name" value="<%= name %>" size="50" /></td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label2"/></td>
                <td>
                    <input type="text" name="short_description" value="<%= shortDesc %>" size="50" />
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label3"/></td>
                <td>
                    <textarea name="introductory_text" rows="6" cols="50"><%= intro %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label4"/></td>
                <td>
                    <textarea name="copyright_text" rows="6" cols="50"><%= copy %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label5"/></td>
                <td>
                    <textarea name="side_bar_text" rows="6" cols="50"><%= side %></textarea>
                </td>
            </tr>
<%-- ===========================================================
     Logo
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label6"/></td>
                <td>
<%  if (logo != null) { %>
                    <table>
                        <tr>
                            <td>
                                <img src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" alt="logo" />
                            </td>
                            <td>
                                <input type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-community.form.button.add-logo"/>" /><br/><br/>
                                <input type="submit" name="submit_delete_logo" value="<fmt:message key="jsp.tools.edit-community.form.button.delete-logo"/>" />
                            </td>
                        </tr>
                    </table>
<%  } else { %>
                    <input type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-community.form.button.set-logo"/>" />
<%  } %>
                </td>
            </tr>
    <% if(admin_button ) { %>
<%-- ===========================================================
     Edit community's policies
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label7"/></td>
                <td>
                    <input type="submit" name="submit_authorization_edit" value="<fmt:message key="jsp.tools.edit-community.form.button.edit"/>" />
                </td>
            </tr>   
    <% } %>

        </table>

        <p>&nbsp;</p>

        <center>
            <table width="70%">
                <tr>
                    <td class="standard">
                        
<%
    if (community == null)
    {
%>
                        <input type="hidden" name="parent_community_id" value="<%= parentID %>" />
                        <input type="hidden" name="create" value="true" />
                        <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-community.form.button.create"/>" />
                    </td>
                    <td>
                        <input type="hidden" name="parent_community_id" value="<%= parentID %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COMMUNITY %>" />
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-community.form.button.cancel"/>" />
<%
    }
    else
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="create" value="false" />
                        <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-community.form.button.update"/>" />
                    </td>
                    <td>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COMMUNITY %>" />
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-community.form.button.cancel"/>" />
<%
    }
%>
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>