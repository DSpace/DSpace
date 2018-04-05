<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Show policies for an item, allowing you to modify, delete
  -  or add to them
  -
  - Attributes:
  -  item          - Item being modified
  -  item_policies - ResourcePolicy List of policies for the item
  -  bundles            - [] of Bundle objects
  -  bundle_policies    - Map of (ID, List of policies)
  -  bitstream_policies - Map of (ID, List of policies)
  -
  - Returns:
  -  submit value item_add_policy         to add a policy 
  -  submit value item_edit_policy        to edit policy for item, bundle, or bitstream
  -  submit value item_delete_policy      to delete policy for item, bundle, or bitstream
  -
  -  submit value bundle_add_policy       add policy
  -
  -  submit value bitstream_add_policy    to add a policy
  -
  -  policy_id - ID of policy to edit, delete
  -  item_id
  -  bitstream_id
  -  bundle_id
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.Iterator" %>
<%@ page import="java.util.List"     %>
<%@ page import="java.util.Map"      %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.content.Item"             %>
<%@ page import="org.dspace.content.Bundle"           %>
<%@ page import="org.dspace.content.Bitstream"        %>
<%@ page import="org.dspace.core.Constants"           %>
<%@ page import="org.dspace.eperson.EPerson"          %>
<%@ page import="org.dspace.eperson.Group"            %>
<%@ page import="org.dspace.authorize.factory.AuthorizeServiceFactory" %>
<%@ page import="org.dspace.authorize.service.ResourcePolicyService" %>


<%
    // get item and list of policies
    Item item = (Item) request.getAttribute("item");
    List<ResourcePolicy> item_policies =
        (List<ResourcePolicy>) request.getAttribute("item_policies");

    // get bitstreams and corresponding policy lists
    List<Bundle> bundles      = (List<Bundle>)request.getAttribute("bundles");
    Map bundle_policies    = (Map)request.getAttribute("bundle_policies"   );
    Map bitstream_policies = (Map)request.getAttribute("bitstream_policies");
    
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

<dspace:layout style="submission" titlekey="jsp.dspace-admin.authorize-item-edit.title"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="<%= link %>"
               nocache="true">


	<h1><fmt:message key="jsp.dspace-admin.authorize-item-edit.policies">
        <fmt:param><%= item.getHandle() %></fmt:param>
        <fmt:param><%= item.getID() %></fmt:param>
    </fmt:message>
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#itempolicies\"%>"><fmt:message key="jsp.help"/></dspace:popup>
    </h1>

  <p class="help-block"><fmt:message key="jsp.dspace-admin.authorize-item-edit.text1"/></p>
  <p class="help-block"><fmt:message key="jsp.dspace-admin.authorize-item-edit.text2"/></p>

  <div class="panel panel-primary">
  <div class="panel-heading"><fmt:message key="jsp.dspace-admin.authorize-item-edit.item"/></div>
  <div class="panel-body">
    <form method="post" action="">
      <div class="row col-md-offset-4">
          <input type="hidden" name="item_id" value="<%=item.getID()%>" />
          <input class="btn btn-success col-md-4" type="submit" name="submit_item_add_policy" value="<fmt:message key="jsp.dspace-admin.general.addpolicy"/>" />
      </div>
    </form>
    <br/>

    <table class="table" summary="Item Policy Edit Form">
        <tr>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.general.id" /></strong></th>
            <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.action"/></strong></th>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.dspace-admin.authorize-item-edit.eperson"/></strong></th>
            <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.group"/></strong></th>
            <th class="oddRowOddCol">&nbsp;</th>
        </tr>
<%
    ResourcePolicyService resourcePolicyService = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
    String row = "even";
    for (ResourcePolicy rp : item_policies)
    {
%>
        <tr>
            <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
            <td class="<%= row %>RowEvenCol">
                    <%= resourcePolicyService.getActionText(rp) %>
            </td>
            <td class="<%= row %>RowOddCol">
                    <%= (rp.getEPerson() == null ? "..." : rp.getEPerson().getEmail() ) %>  
            </td>
            <td class="<%= row %>RowEvenCol">
                    <%= (rp.getGroup()   == null ? "..." : rp.getGroup().getName() ) %>  
            </td>
            <td class="<%= row %>RowOddCol">
                 <form method="post" action=""> 
                     <input type="hidden" name="policy_id" value="<%= rp.getID() %>" />
                     <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                     <input class="btn btn-primary col-md-4" type="submit" name="submit_item_edit_policy" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                     <input class="btn btn-danger col-md-4 col-md-offset-1" type="submit" name="submit_item_delete_policy" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                </form>  
            </td>
        </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
    </div>
 </div>
<%
    for( int b = 0; b < bundles.size(); b++ )
    {
        Bundle myBun = bundles.get(b);
        List<ResourcePolicy> myPolicies = (List<ResourcePolicy>)bundle_policies.get(myBun.getID());

        // display add policy
        // display bundle header w/ID

%>
	<div class="panel panel-info">
  		<div class="panel-heading">    
        <fmt:message key="jsp.dspace-admin.authorize-item-edit.bundle">
            <fmt:param><%=myBun.getName()%></fmt:param>
            <fmt:param><%=myBun.getID()%></fmt:param>
        </fmt:message></div>
		<div class="panel-body">
        <form method="post" action="">
      		<div class="row col-md-offset-4">
                <input type="hidden" name="item_id" value="<%=item.getID()%>" />
                <input type="hidden" name="bundle_id" value="<%=myBun.getID()%>" />
                <input class="btn btn-success col-md-4" type="submit" name="submit_bundle_add_policy" value="<fmt:message key="jsp.dspace-admin.general.addpolicy"/>" />
            </div>
        </form>
        <br/> 
    <table class="table" summary="Bundle Policy Edit Form">
        <tr>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.general.id" /></strong></th>
            <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.action"/></strong></th>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.dspace-admin.general.eperson" /></strong></th>
            <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.group"/></strong></th>
            <th class="oddRowOddCol">&nbsp;</th>
        </tr>

<%
    row = "even";
    for (ResourcePolicy rp : myPolicies)
    {
%>
        <tr>
            <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
            <td class="<%= row %>RowEvenCol">
                    <%= resourcePolicyService.getActionText(rp) %>
            </td>
            <td class="<%= row %>RowOddCol">
                    <%= (rp.getEPerson() == null ? "..." : rp.getEPerson().getEmail() ) %>  
            </td>
            <td class="<%= row %>RowEvenCol">
                    <%= (rp.getGroup()   == null ? "..." : rp.getGroup().getName() ) %>  
            </td>
            <td class="<%= row %>RowOddCol">
                <form method="post" action="">
                    <input type="hidden" name="policy_id" value="<%= rp.getID() %>" />
                    <input type="hidden" name="item_id" value="<%= item.getID() %>" />
                    <input type="hidden" name="bundle_id" value="<%= myBun.getID() %>" />
                    <input class="btn btn-primary col-md-4" type="submit" name="submit_item_edit_policy" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                    <input class="btn btn-danger col-md-4 col-md-offset-1" type="submit" name="submit_item_delete_policy" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                </form>
            </td>
         </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
<%
        List<Bitstream> bitstreams = myBun.getBitstreams();
                
        for( int s = 0; s < bitstreams.size(); s++ )
        {
            Bitstream myBits = bitstreams.get(s);
            myPolicies  = (List)bitstream_policies.get(myBits.getID());

            // display bitstream header w/ID, filename
            // 'add policy'
            // display bitstream's policies
%>                        
	<div class="panel panel-success">
  		<div class="panel-heading">  
            <fmt:message key="jsp.dspace-admin.authorize-item-edit.bitstream">
                <fmt:param><%=myBits.getID()%></fmt:param>
                <fmt:param><%=myBits.getName()%></fmt:param>
            </fmt:message></div>
            
        <div class="panel-body">    
            <form method="post" action="">
                <div class="row col-md-offset-4">
                    <input type="hidden" name="item_id"value="<%=item.getID()%>" />
                    <input type="hidden" name="bitstream_id" value="<%=myBits.getID()%>" />
                    <input class="btn btn-success col-md-4" type="submit" name="submit_bitstream_add_policy" value="<fmt:message key="jsp.dspace-admin.general.addpolicy"/>" />
                </div>
            </form>
            <br/>
            <table class="table" summary="This table displays the bitstream data">
            <tr>
                <th class="oddRowOddCol"><strong><fmt:message key="jsp.general.id" /></strong></th>
                <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.action"/></strong></th>
                <th class="oddRowOddCol"><strong><fmt:message key="jsp.dspace-admin.authorize-item-edit.eperson" /></strong></th>
                <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.group"/></strong></th>
                <th class="oddRowOddCol">&nbsp;</th>
            </tr>
<%
    row = "even";

    for (ResourcePolicy rp : myPolicies)
    {
%>
        <tr>
            <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
            <td class="<%= row %>RowEvenCol">
                    <%= resourcePolicyService.getActionText(rp) %>
            </td>
            <td class="<%= row %>RowOddCol">
                    <%= (rp.getEPerson() == null ? "..." : rp.getEPerson().getEmail() ) %>  
            </td>
            <td class="<%= row %>RowEvenCol">
                    <%= (rp.getGroup()   == null ? "..." : rp.getGroup().getName() ) %>  
            </td>
            <td class="<%= row %>RowOddCol">
                <form method="post" action="">
                    <input type="hidden" name="policy_id" value="<%= rp.getID()     %>" />
                    <input type="hidden" name="item_id" value="<%= item.getID()   %>" />
                    <input type="hidden" name="bitstream_id" value="<%= myBits.getID() %>" />
                    <input class="btn btn-primary col-md-4" type="submit" name="submit_item_edit_policy" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                    <input class="btn btn-danger col-md-4 col-md-offset-1" type="submit" name="submit_item_delete_policy" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                 </form>  
            </td>
        </tr>
<%
                row = (row.equals("odd") ? "even" : "odd");
            }
%>
    </table>
    </div>
  </div>  
<%

        }
        %>
   </div>
</div>        
        <%
    }
%>
</dspace:layout>
