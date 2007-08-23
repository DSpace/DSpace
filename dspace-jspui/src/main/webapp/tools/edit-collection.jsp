<%--
  - edit-collection.jsp
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
  - Show form allowing edit of collection metadata
  -
  - Attributes:
  -    community    - community to create new collection in, if creating one
  -    collection   - collection to edit, if editing an existing one.  If this
  -                  is null, we are creating one.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
    
    String name = "";
    String shortDesc = "";
    String intro = "";
    String copy = "";
    String side = "";
    String license = "";
    String provenance = "";

    Group[] wfGroups = new Group[3];
    wfGroups[0] = null;
    wfGroups[1] = null;
    wfGroups[2] = null;

    Group admins     = null;
    Group submitters = null;

    Item template = null;

    Bitstream logo = null;
    
    if (collection != null)
    {
        name = collection.getMetadata("name");
        shortDesc = collection.getMetadata("short_description");
        intro = collection.getMetadata("introductory_text");
        copy = collection.getMetadata("copyright_text");
        side = collection.getMetadata("side_bar_text");
        provenance = collection.getMetadata("provenance_description");

        if (collection.hasCustomLicense())
        {
            license = collection.getLicense();
        }
        
        wfGroups[0] = collection.getWorkflowGroup(1);
        wfGroups[1] = collection.getWorkflowGroup(2);
        wfGroups[2] = collection.getWorkflowGroup(3);

        admins     = collection.getAdministrators();
        submitters = collection.getSubmitters();

        template = collection.getTemplateItem();

        logo = collection.getLogo();
    }
%>

<dspace:layout titlekey="jsp.tools.edit-collection.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer"
               nocache="true">

<%
    if (collection == null)
    {
%>
    <h1><fmt:message key="jsp.tools.edit-collection.heading1"/></h1>
<% } else { %>
    <h1><fmt:message key="jsp.tools.edit-collection.heading2">
        <fmt:param><%= collection.getHandle() %></fmt:param>
        </fmt:message>
    </h1>
    <% if(admin_button ) { %>
      <center>
        <table width="70%">
          <tr>
            <td class="standard">
              <form method="post" action="">
                <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_DELETE_COLLECTION %>" />
                <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.button.delete"/>" />
              </form>
            </td>
            <td align="right" class="standard">
               <dspace:popup page="/help/site-admin.html#editcollection"><fmt:message key="jsp.help"/></dspace:popup>
            </td>
          </tr>
        </table>
      </center>
    <% } %>
<% } %>

    <form method="post" action="<%= request.getContextPath() %>/tools/edit-communities">
        <table>
<%-- ===========================================================
     Basic metadata
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label1"/></td>
                <td><input type="text" name="name" value="<%= name %>" size="50" /></td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label2"/></td>
                <td>
                    <input type="text" name="short_description" value="<%= shortDesc %>" size="50"/>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label3"/></td>
                <td>
                    <textarea name="introductory_text" rows="6" cols="50"><%= intro %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label4"/></td>
                <td>
                    <textarea name="copyright_text" rows="6" cols="50"><%= copy %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label5"/></td>
                <td>
                    <textarea name="side_bar_text" rows="6" cols="50"><%= side %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label6"/></td>
                <td>
                    <textarea name="license" rows="6" cols="50"><%= license %></textarea>
                </td>
            </tr>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label7"/></td>
                <td>
                    <textarea name="provenance_description" rows="6" cols="50"><%= provenance %></textarea>
                </td>
            </tr>
<%-- ===========================================================
     Logo
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label8"/></td>
                <td>
<%  if (logo != null) { %>
                    <table>
                        <tr>
                            <td>
                                <img src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" alt="collection logo"/>
                            </td>
                            <td>
                                <input type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-collection.form.button.add-logo"/>" /><br/><br/>
                                <input type="submit" name="submit_delete_logo" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete-logo"/>" />
                            </td>
                        </tr>
                    </table>
<%  } else { %>

                    <input type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-collection.form.button.set-logo"/>" />
<%  } %>
                </td>
            </tr>
            
            <tr><td>&nbsp;</td></tr>
            <tr><td colspan="2"><center><h3><fmt:message key="jsp.tools.edit-collection.form.label9"/></h3></center></td></tr>

<% if(admin_button ) { %>
<%-- ===========================================================
     Collection Submitters
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label10"/></td>
                <td>
<%  if (submitters == null) {%>
                    <input type="submit" name="submit_submitters_create" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%  } else { %>
                    <input type="submit" name="submit_submitters_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
<%  } %>                    
                </td>
            </tr>   
            
<%-- ===========================================================
     Workflow groups
     =========================================================== --%>
<%
    String[] roleTexts =
    {
        	LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-collection.wf-role1"),
        	LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-collection.wf-role2"),
        	LocaleSupport.getLocalizedMessage(pageContext, "jsp.tools.edit-collection.wf-role3")
    };

    for (int i = 0; i<3; i++) { %>
            <tr>
                <td class="submitFormLabel"><em><%= roleTexts[i] %></em> <fmt:message key="jsp.tools.edit-collection.form.label11"/></td>
                <td>
<%      if (wfGroups[i] == null) { %>
                    <input type="submit" name="submit_wf_create_<%= i + 1 %>" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%      } else { %>
                    <input type="submit" name="submit_wf_edit_<%= i + 1 %>" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input type="submit" name="submit_wf_delete_<%= i + 1 %>" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%      } %>
                </td>
            </tr>
<%  } %>

            <tr><td>&nbsp;</td></tr>

<%-- ===========================================================
     Collection Administrators
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label12"/></td>
                <td>
<%  if (admins == null) {%>
                    <input type="submit" name="submit_admins_create" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />
<%  } else { %>
                    <input type="submit" name="submit_admins_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
<%  } %>                    
                </td>
            </tr>   
<%  } %>
<%-- ===========================================================
     Item template
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label13"/></td>
                <td>
<%  if (template == null) {%>
                    <input type="submit" name="submit_create_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.create"/>" />

<%  } else { %>
                    <input type="submit" name="submit_edit_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                    <input type="submit" name="submit_delete_template" value="<fmt:message key="jsp.tools.edit-collection.form.button.delete"/>" />
<%  } %>                    
                </td>
            </tr>   
<% if(admin_button ) { %>
<%-- ===========================================================
     Edit collection's policies
     =========================================================== --%>
            <tr>
                <td class="submitFormLabel"><fmt:message key="jsp.tools.edit-collection.form.label14"/></td>
                <td>
                    <input type="submit" name="submit_authorization_edit" value="<fmt:message key="jsp.tools.edit-collection.form.button.edit"/>" />
                </td>
            </tr>   
<%  } %>

        </table>
        
        <p>&nbsp;</p>

        <center>
            <table width="70%">
                <tr>
                    <td class="standard">
<%
    if (collection == null)
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="create" value="true" />
                        <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.form.button.create2"/>" />
<%
    }
    else
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                        <input type="hidden" name="create" value="false" />
                        <input type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-collection.form.button.update"/>" />
<% 
    }
%>
                    </td>
                    <td>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COLLECTION %>" /> 
                        <input type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-collection.form.button.cancel"/>" />
                    </td>
                </tr>
            </table>
        </center>
    </form>
</dspace:layout>