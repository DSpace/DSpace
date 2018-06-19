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

<%@page import="org.dspace.content.factory.ContentServiceFactory"%>
<%@page import="org.dspace.content.service.CommunityService"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.UUID" %> 
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
	CommunityService comServ = ContentServiceFactory.getInstance().getCommunityService();
    Community community = (Community) request.getAttribute("community");
	Community parentCommunity = (Community) request.getAttribute("parent");
    UUID parentID = (parentCommunity != null ? parentCommunity.getID() : null);
    
    // Is the logged in user an admin or community admin or collection admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    
    Boolean communityAdmin = (Boolean)request.getAttribute("is.communityAdmin");
    boolean isCommunityAdmin = (communityAdmin == null ? false : communityAdmin.booleanValue());
    
    Boolean collectionAdmin = (Boolean)request.getAttribute("is.collectionAdmin");
    boolean isCollectionAdmin = (collectionAdmin == null ? false : collectionAdmin.booleanValue());
    
    String naviAdmin = "admin";
    String link = "/dspace-admin";
    
    if(!isAdmin && (isCommunityAdmin || isCollectionAdmin))
    {
        naviAdmin = "community-or-collection-admin";
        link = "/tools";
    }
    
    Boolean adminCreateGroup = (Boolean)request.getAttribute("admin_create_button");
    boolean bAdminCreateGroup = (adminCreateGroup == null ? false : adminCreateGroup.booleanValue());

    Boolean adminRemoveGroup = (Boolean)request.getAttribute("admin_remove_button");
    boolean bAdminRemoveGroup = (adminRemoveGroup == null ? false : adminRemoveGroup.booleanValue());
    
    Boolean policy = (Boolean)request.getAttribute("policy_button");
    boolean bPolicy = (policy == null ? false : policy.booleanValue());

    Boolean delete = (Boolean)request.getAttribute("delete_button");
    boolean bDelete = (delete == null ? false : delete.booleanValue());

    Boolean adminCommunity = (Boolean)request.getAttribute("admin_community");
    boolean bAdminCommunity = (adminCommunity == null ? false : adminCommunity.booleanValue());
    String name = "";
    String shortDesc = "";
    String intro = "";
    String copy = "";
    String side = "";
    Group admins = null;

    Bitstream logo = null;
    
    if (community != null)
    {
        name = comServ.getMetadata(community, "name");
        shortDesc = comServ.getMetadata(community, "short_description");
        intro = comServ.getMetadata(community, "introductory_text");
        copy = comServ.getMetadata(community, "copyright_text");
        side = comServ.getMetadata(community, "side_bar_text");
        logo = community.getLogo();
        admins = community.getAdministrators();
    }
%>

<dspace:layout style="submission" titlekey="jsp.tools.edit-community.title"
		       navbar="<%= naviAdmin %>"
		       locbar="link"
		       parentlink="<%= link %>"
		       parenttitlekey="jsp.administer" nocache="true">

<div class="row">
<%
    if (community == null)
    {
%>
    <h3 class="col-md-12"><fmt:message key="jsp.tools.edit-community.heading1"/>
    	<span>
        	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#editcommunity\"%>"><fmt:message key="jsp.help"/></dspace:popup>
        </span>
    </h3>
<%
    }
    else
    {
%>
    <h3 class="col-md-8"><fmt:message key="jsp.tools.edit-community.heading2">
        <fmt:param><%= community.getHandle() %></fmt:param>
        </fmt:message>
        <span>
        	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#editcommunity\"%>"><fmt:message key="jsp.help"/></dspace:popup>
        </span>	
    </h3>
    <% if(bDelete) { %>
              <form class="col-md-4" method="post" action="">
                <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_DELETE_COMMUNITY %>" />
                <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                <input class="col-md-12 btn btn-danger" type="submit" name="submit_delete" value="<fmt:message key="jsp.tools.edit-community.button.delete"/>" />
              </form>
    <% } %>
<%
    }
%>
</div>
  
<form method="post" action="">  
<div class="row">
	<div class="col-md-<%= community != null?"8":"12" %>">
	<div class="panel panel-primary">
		<div class="panel-heading"><fmt:message key="jsp.tools.edit-community.form.basic-metadata"/></div>
    
        <div class="panel-body">
<%-- ===========================================================
     Basic metadata
     =========================================================== --%>
            <div class="row">    
                <label for="name" class="col-md-3"><fmt:message key="jsp.tools.edit-community.form.label1"/></label>
                <span class="col-md-9"><input class="form-control" type="text" name="name" value="<%= Utils.addEntities(name) %>" size="50" /></span>
            </div><br/>
            <div class="row">
                <label for="short_description" class="col-md-3"><fmt:message key="jsp.tools.edit-community.form.label2"/></label>
                <span class="col-md-9"><input class="form-control" type="text" name="short_description" value="<%= Utils.addEntities(shortDesc) %>" size="50" />
                </span>
            </div><br/>
            <div class="row">
                <label class="col-md-3" for="introductory_text"><fmt:message key="jsp.tools.edit-community.form.label3"/></label>
                <span class="col-md-9"><textarea class="form-control" name="introductory_text" rows="6" cols="50"><%= Utils.addEntities(intro) %></textarea>
                </span>
            </div><br/>
            <div class="row">
                <label class="col-md-3" for="copyright_text"><fmt:message key="jsp.tools.edit-community.form.label4"/></label>
                <span class="col-md-9">
                    <textarea class="form-control" name="copyright_text" rows="6" cols="50"><%= Utils.addEntities(copy) %></textarea>
                </span>
            </div><br/>
            <div class="row">
                <label class="col-md-3"><fmt:message key="jsp.tools.edit-community.form.label5"/></label>
                <span class="col-md-9">
                    <textarea class="form-control" name="side_bar_text" rows="6" cols="50"><%= Utils.addEntities(side) %></textarea>
                </span>
            </div><br/>
<%-- ===========================================================
     Logo
     =========================================================== --%>
            <div class="row">
                <label class="col-md-3"><fmt:message key="jsp.tools.edit-community.form.label6"/></label>
                    <div class="col-md-9">

<%  if (logo != null) { %>
                        <span class="col-md-6">
                            <img class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" alt="logo" />
                        </span>
                        <input class="col-md-3 btn btn-default" type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-community.form.button.add-logo"/>" />
                        <input class="col-md-3 btn btn-danger" type="submit" name="submit_delete_logo" value="<fmt:message key="jsp.tools.edit-community.form.button.delete-logo"/>" />
<%  } else { %>
                    <input class="col-md-12 btn btn-success" type="submit" name="submit_set_logo" value="<fmt:message key="jsp.tools.edit-community.form.button.set-logo"/>" />
<%  } %>
                    </div>
			</div>
            
        </div>
     </div>
 </div>
 <% if (community != null) { %>
 <div class="col-md-4">
 	<div class="panel panel-default">
		<div class="panel-heading"><fmt:message key="jsp.tools.edit-community.form.community-settings" /></div>
		<div class="panel-body">
<% if(bAdminCreateGroup || (admins != null && bAdminRemoveGroup)) { %>
 <%-- ===========================================================
     Community Administrators
     =========================================================== --%>
            <div class="row">
                <label class="col-md-6" for="submit_admins_create"><fmt:message key="jsp.tools.edit-community.form.label8"/></label>
                <span class="col-md-6 btn-group">
			<%  if (admins == null) {
					if (bAdminCreateGroup) {
			%>
                    <input class="btn btn-success col-md-12" type="submit" name="submit_admins_create" value="<fmt:message key="jsp.tools.edit-community.form.button.create"/>" />
			<%  	}
				} 
				else 
				{ 
					if (bAdminCreateGroup) { %>
                    <input class="btn btn-default col-md-6" type="submit" name="submit_admins_edit" value="<fmt:message key="jsp.tools.edit-community.form.button.edit"/>" />
				<%  }
					if (bAdminRemoveGroup) { %>
					<input class="btn btn-danger col-md-6" type="submit" name="submit_admins_remove" value="<fmt:message key="jsp.tools.edit-community.form.button.remove"/>" />
			<%  	}
				}
			%>                    
                </span>
            </div>   
    
	<% }
    	
    if (bPolicy) { 
    
    %>

<%-- ===========================================================
     Edit community's policies
     =========================================================== --%>
            <div class="row">
                <label class="col-md-6" for="submit_authorization_edit"><fmt:message key="jsp.tools.edit-community.form.label7"/></label>
                <span class="col-md-6 btn-group">
                    <input class="col-md-12 btn btn-success" type="submit" name="submit_authorization_edit" value="<fmt:message key="jsp.tools.edit-community.form.button.edit"/>" />
                </span>
            </div>   
    <% }

    if (bAdminCommunity) {
%> 
<%-- ===========================================================
     Curate Community
     =========================================================== --%>
            <div class="row">
                <label for="submit_curate_community" class="col-md-6"><fmt:message key="jsp.tools.edit-community.form.label9"/></label>
                <span class="col-md-6">
                    <input class="col-md-12 btn btn-success" type="submit" name="submit_curate_community" value="<fmt:message key="jsp.tools.edit-community.form.button.curate"/>" />
                </span>
            </div>   
    <% } %>
	</div>
	</div>
</div>
<% } %>
</div>	

<div class="row">
<div class="btn-group col-md-12">                        
<%
    if (community == null)
    {
%>
                        <input type="hidden" name="parent_community_id" value="<%= parentID %>" />
                        <input type="hidden" name="create" value="true" />
                        <input class="col-md-6 btn btn-success" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-community.form.button.create"/>" />
                        
                        <input type="hidden" name="parent_community_id" value="<%= parentID %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COMMUNITY %>" />
                        <input class="col-md-6 btn btn-warning" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-community.form.button.cancel"/>" />
<%
    }
    else
    {
%>
                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="create" value="false" />
                        <input class="col-md-6 btn btn-success" type="submit" name="submit" value="<fmt:message key="jsp.tools.edit-community.form.button.update"/>" />

                        <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                        <input type="hidden" name="action" value="<%= EditCommunitiesServlet.CONFIRM_EDIT_COMMUNITY %>" />
                        <input class="col-md-6 btn btn-warning" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.edit-community.form.button.cancel"/>" />
<%
    }
%>
            </div>
        </div>
    </form>
</dspace:layout>
