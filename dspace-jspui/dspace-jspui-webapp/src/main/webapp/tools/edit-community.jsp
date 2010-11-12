<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.Utils" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    Community community = (Community) request.getAttribute("community");
    int parentID = UIUtil.getIntParameter(request, "parent_community_id");
    // Is the logged in user a sys admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    
    Boolean adminCreateGroup = (Boolean)request.getAttribute("admin_create_button");
    boolean bAdminCreateGroup = (adminCreateGroup == null ? false : adminCreateGroup.booleanValue());

    Boolean adminRemoveGroup = (Boolean)request.getAttribute("admin_remove_button");
    boolean bAdminRemoveGroup = (adminRemoveGroup == null ? false : adminRemoveGroup.booleanValue());
    
    Boolean policy = (Boolean)request.getAttribute("policy_button");
    boolean bPolicy = (policy == null ? false : policy.booleanValue());

    Boolean delete = (Boolean)request.getAttribute("delete_button");
    boolean bDelete = (delete == null ? false : delete.booleanValue());

    String name = "";
    String shortDesc = "";
    String intro = "";
    String copy = "";
    String side = "";
    Group admins = null;

    Bitstream logo = null;
    
    if (community != null)
    {
        name = community.getMetadata("name");
        shortDesc = community.getMetadata("short_description");
        intro = community.getMetadata("introductory_text");
        copy = community.getMetadata("copyright_text");
        side = community.getMetadata("side_bar_text");
        logo = community.getLogo();
        admins = community.getAdministrators();
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
    <% if(bDelete) { %>
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
                <td><input type="text" name="name" value="<%= Utils.addEntities(name) %>" size="50" /></td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label2"/></td>
                <td>
                    <input type="text" name="short_description" value="<%= Utils.addEntities(shortDesc) %>" size="50" />
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label3"/></td>
                <td>
                    <textarea name="introductory_text" rows="6" cols="50"><%= Utils.addEntities(intro) %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label4"/></td>
                <td>
                    <textarea name="copyright_text" rows="6" cols="50"><%= Utils.addEntities(copy) %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label5"/></td>
                <td>
                    <textarea name="side_bar_text" rows="6" cols="50"><%= Utils.addEntities(side) %></textarea>
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
    <% if(bAdminCreateGroup || (admins != null && bAdminRemoveGroup)) { %>
 <%-- ===========================================================
     Community Administrators
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-community.form.label8"/></td>
                <td>
			<%  if (admins == null) {
					if (bAdminCreateGroup) {
			%>
                    <input type="submit" name="submit_admins_create" value="<fmt:message key="jsp.tools.edit-community.form.button.create"/>" />
			<%  	}
				} 
				else 
				{ 
					if (bAdminCreateGroup) { %>
                    <input type="submit" name="submit_admins_edit" value="<fmt:message key="jsp.tools.edit-community.form.button.edit"/>" />
				<%  }
					if (bAdminRemoveGroup) { %>
					<input type="submit" name="submit_admins_remove" value="<fmt:message key="jsp.tools.edit-community.form.button.remove"/>" />
			<%  	}
				}
			%>                    
                </td>
            </tr>   
    
	<% }
    	
    if (bPolicy) { 
    
    %>

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