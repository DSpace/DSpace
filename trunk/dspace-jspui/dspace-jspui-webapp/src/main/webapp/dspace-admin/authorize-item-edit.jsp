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


<%
    // get item and list of policies
    Item item = (Item) request.getAttribute("item");
    List<ResourcePolicy> item_policies =
        (List<ResourcePolicy>) request.getAttribute("item_policies");

    // get bitstreams and corresponding policy lists
    Bundle [] bundles      = (Bundle [])request.getAttribute("bundles");
    Map bundle_policies    = (Map)request.getAttribute("bundle_policies"   );
    Map bitstream_policies = (Map)request.getAttribute("bitstream_policies");
%>

<dspace:layout titlekey="jsp.dspace-admin.authorize-item-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

  <table width="95%">
    <tr>
      <td align="left">
	<h1><fmt:message key="jsp.dspace-admin.authorize-item-edit.policies">
        <fmt:param><%= item.getHandle() %></fmt:param>
        <fmt:param><%= item.getID() %></fmt:param>
    </fmt:message></h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#itempolicies\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

  <p><fmt:message key="jsp.dspace-admin.authorize-item-edit.text1"/></p>
  <p><fmt:message key="jsp.dspace-admin.authorize-item-edit.text2"/></p>

  <h3><fmt:message key="jsp.dspace-admin.authorize-item-edit.item"/></h3>
    <form method="post" action="">
      <p align="center">
          <input type="hidden" name="item_id" value="<%=item.getID()%>" />
          <input type="submit" name="submit_item_add_policy" value="<fmt:message key="jsp.dspace-admin.general.addpolicy"/>" />
      </p>
    </form>

    <table class="miscTable" align="center" summary="Item Policy Edit Form">
        <tr>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.general.id" /></strong></th>
            <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.action"/></strong></th>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.dspace-admin.authorize-item-edit.eperson"/></strong></th>
            <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.group"/></strong></th>
            <th class="oddRowOddCol">&nbsp;</th>
        </tr>
<%
    String row = "even";
    for (ResourcePolicy rp : item_policies)
    {
%>
        <tr>
            <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
            <td class="<%= row %>RowEvenCol">
                    <%= rp.getActionText() %>
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
                     <input type="submit" name="submit_item_edit_policy" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                     <input type="submit" name="submit_item_delete_policy" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                </form>  
            </td>
        </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
<%
    for( int b = 0; b < bundles.length; b++ )
    {
        Bundle myBun = bundles[b];
        List<ResourcePolicy> myPolicies = (List<ResourcePolicy>)bundle_policies.get(new Integer(myBun.getID()));

        // display add policy
        // display bundle header w/ID

%>                
        <h3><fmt:message key="jsp.dspace-admin.authorize-item-edit.bundle">
            <fmt:param><%=myBun.getName()%></fmt:param>
            <fmt:param><%=myBun.getID()%></fmt:param>
        </fmt:message></h3>

        <form method="post" action="">
            <p align="center">
                <input type="hidden" name="item_id" value="<%=item.getID()%>" />
                <input type="hidden" name="bundle_id" value="<%=myBun.getID()%>" />
                <input type="submit" name="submit_bundle_add_policy" value="<fmt:message key="jsp.dspace-admin.general.addpolicy"/>" />
            </p>
        </form>      
        
    <table class="miscTable" align="center" summary="Bundle Policy Edit Form">
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
                    <%= rp.getActionText() %>
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
                    <input type="submit" name="submit_item_edit_policy" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                    <input type="submit" name="submit_item_delete_policy" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                </form>
            </td>
         </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>

<%
        Bitstream [] bitstreams = myBun.getBitstreams();
                
        for( int s = 0; s < bitstreams.length; s++ )
        {
            Bitstream myBits = bitstreams[s];
            myPolicies  = (List)bitstream_policies.get(new Integer(myBits.getID()));

            // display bitstream header w/ID, filename
            // 'add policy'
            // display bitstream's policies
%>                        
            <p><fmt:message key="jsp.dspace-admin.authorize-item-edit.bitstream">
                <fmt:param><%=myBits.getID()%></fmt:param>
                <fmt:param><%=myBits.getName()%></fmt:param>
            </fmt:message></p>
            <form method="post" action="">
                <p align="center">
                    <input type="hidden" name="item_id"value="<%=item.getID()%>" />
                    <input type="hidden" name="bitstream_id" value="<%=myBits.getID()%>" />
                    <input type="submit" name="submit_bitstream_add_policy" value="<fmt:message key="jsp.dspace-admin.general.addpolicy"/>" />
                </p>
            </form>
            <table class="miscTable" align="center" summary="This table displays the bitstream data">
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
                    <%= rp.getActionText() %>
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
                    <input type="submit" name="submit_item_edit_policy" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
                    <input type="submit" name="submit_item_delete_policy" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
                 </form>  
            </td>
        </tr>
<%
                row = (row.equals("odd") ? "even" : "odd");
            }
%>
    </table>
<%

        }
    }
%>
</dspace:layout>
