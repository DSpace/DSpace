<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Show policies for a collection, allowing you to modify, delete
  -  or add to them
  -
  - Attributes:
  -  collection - Collection being modified
  -  policies - ResourcePolicy [] of policies for the collection
  - Returns:
  -  submit value collection_addpolicy    to add a policy
  -  submit value collection_editpolicy   to edit policy
  -  submit value collection_deletepolicy to delete policy
  -
  -  policy_id - ID of policy to edit, delete
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>


<%@ page import="java.util.List"     %>
<%@ page import="java.util.Iterator" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Collection"       %>
<%@ page import="org.dspace.core.Constants"           %>
<%@ page import="org.dspace.eperson.EPerson"          %>
<%@ page import="org.dspace.eperson.Group"            %>
<%@ page import="org.dspace.authorize.factory.AuthorizeServiceFactory" %>
<%@ page import="org.dspace.authorize.service.ResourcePolicyService" %>


<%
    Collection collection = (Collection) request.getAttribute("collection");
    List<ResourcePolicy> policies =
        (List<ResourcePolicy>) request.getAttribute("policies");
    
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
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.authorize-collection-edit.title"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="<%= link %>"
               nocache="true">

		<h1><fmt:message key="jsp.dspace-admin.authorize-collection-edit.policies">
            <fmt:param><%= collection.getName() %></fmt:param>
            <fmt:param>hdl:<%= collection.getHandle() %></fmt:param>
            <fmt:param><%= collection.getID() %></fmt:param>
        </fmt:message>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#collectionpolicies\"%>"><fmt:message key="jsp.help"/></dspace:popup>
       	</h1>


 <form action="<%= request.getContextPath() %>/tools/authorize" method="post"> 
	<div class="row"> 
            <input type="hidden" name="collection_id" value="<%=collection.getID()%>" />
            <input class="btn btn-success col-md-2 col-md-offset-5"  type="submit" name="submit_collection_add_policy" value="<fmt:message key="jsp.dspace-admin.general.addpolicy"/>" />
    </div>
 </form>
 <br/>
         <table class="table" summary="Collection Policy Edit Form">
            <tr>
               <th class="oddRowOddCol"><strong><fmt:message key="jsp.general.id" /></strong></th>
               <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.action"/></strong></th>
               <th class="oddRowOddCol"><strong><fmt:message key="jsp.dspace-admin.general.group"/></strong></th>
               <th class="oddRowEvenCol">&nbsp;</th>
               <th class="oddRowOddCol">&nbsp;</th>
            </tr>
 
<%
    String row = "even";

    ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    for (ResourcePolicy rp : policies)
    {
%>
            <tr>
               <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
               <td class="<%= row %>RowEvenCol">
                    <%= resourcePolicyService.getActionText(rp) %>
               </td>
               <td class="<%= row %>RowOddCol">
                    <%= (rp.getGroup()   == null ? "..." : rp.getGroup().getName() ) %>
               </td>
               <td class="<%= row %>RowEvenCol">
               <form action="<%= request.getContextPath() %>/tools/authorize" method="post">
                    <input type="hidden" name="policy_id" value="<%= rp.getID() %>" />
                    <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                    <input class="btn btn-primary" type="submit" name="submit_collection_edit_policy" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
               </form>
               </td>
               <td class="<%= row %>RowOddCol">
               <form action="<%= request.getContextPath() %>/tools/authorize" method="post">
                    <input type="hidden" name="policy_id" value="<%= rp.getID() %>" />
                    <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                    <input class="btn btn-danger" type="submit" name="submit_collection_delete_policy" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
               </form>
               </td>
            </tr>

<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
	</table>
</dspace:layout>
