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


<%
    Collection collection = (Collection) request.getAttribute("collection");
    List<ResourcePolicy> policies =
        (List<ResourcePolicy>) request.getAttribute("policies");
%>

<dspace:layout titlekey="jsp.dspace-admin.authorize-collection-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

  <table width="95%">
    <tr>
      <td align="left">
            <h1><fmt:message key="jsp.dspace-admin.authorize-collection-edit.policies">
            <fmt:param><%= collection.getMetadata("name") %></fmt:param>
            <fmt:param>hdl:<%= collection.getHandle() %></fmt:param>
            <fmt:param><%= collection.getID() %></fmt:param>
        </fmt:message></h1>
      </td>
      <td align="right" class="standard">
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#collectionpolicies\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

 <form action="<%= request.getContextPath() %>/tools/authorize" method="post"> 
    <p align="center">
            <input type="hidden" name="collection_id" value="<%=collection.getID()%>" />
            <input type="submit" name="submit_collection_add_policy" value="<fmt:message key="jsp.dspace-admin.general.addpolicy"/>" />
    </p>
 </form>

<%
    String row = "even";

    for (ResourcePolicy rp : policies)
    {
%>
      <form action="<%= request.getContextPath() %>/tools/authorize" method="post">
        <table class="miscTable" align="center" summary="Collection Policy Edit Form">
            <tr>
               <th class="oddRowOddCol"><strong><fmt:message key="jsp.general.id" /></strong></th>
               <th class="oddRowEvenCol"><strong><fmt:message key="jsp.dspace-admin.general.action"/></strong></th>
               <th class="oddRowOddCol"><strong><fmt:message key="jsp.dspace-admin.general.group"/></strong></th>
               <th class="oddRowEvenCol">&nbsp;</th>
               <th class="oddRowOddCol">&nbsp;</th>
            </tr>

            <tr>
               <td class="<%= row %>RowOddCol"><%= rp.getID() %></td>
               <td class="<%= row %>RowEvenCol">
                    <%= rp.getActionText() %>
               </td>
               <td class="<%= row %>RowOddCol">
                    <%= (rp.getGroup()   == null ? "..." : rp.getGroup().getName() ) %>
               </td>
               <td class="<%= row %>RowEvenCol">
                    <input type="hidden" name="policy_id" value="<%= rp.getID() %>" />
                    <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                    <input type="submit" name="submit_collection_edit_policy" value="<fmt:message key="jsp.dspace-admin.general.edit"/>" />
               </td>
               <td class="<%= row %>RowOddCol">
                    <input type="submit" name="submit_collection_delete_policy" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
               </td>
            </tr>
       </table>
     </form>

<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
</dspace:layout>
