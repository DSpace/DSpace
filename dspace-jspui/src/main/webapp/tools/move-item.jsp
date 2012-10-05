<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
	
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditItemServlet" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
	Collection [] notLinkedCollections = (Collection[] )request.getAttribute("notLinkedCollections");
	Collection [] linkedCollections = (Collection[] )request.getAttribute("linkedCollections");
	
	Item item = (Item)request.getAttribute("item");
%>

<dspace:layout titlekey="jsp.tools.move-item.title">

   	<form action="<%=request.getContextPath()%>/tools/edit-item" method="post">
   		
	  <table class="miscTable" align="center">
        <tr>
          <td class="evenRowEvenCol" colspan="2">
            <table>
              <tr>
                <td class="standard">
				  <small><strong><fmt:message key="jsp.tools.move-item.item.name.msg"/></strong></small>
			    </td>
			    <td class="standard">
				  <font color="#FF0000"><%=item.getMetadata("dc", "title", null, Item.ANY)[0].value%></font>
				</td>
			  </tr>
			  <tr>
				<td class="standard">
					<small><strong><fmt:message key="jsp.tools.move-item.collection.from.msg"/></strong></small>
				</td>
				<td class="standard">
				<select name="collection_from_id">
<%
        for (int i = 0; i < linkedCollections.length; i++)
        {
%>
            <option value="<%= linkedCollections[i].getID() %>"><%= linkedCollections[i].getMetadata("name") %></option>
<%
        }
%>
				</select>
				</td>
			  </tr>
			  <tr>
				<td class="standard">
					<small><strong><fmt:message key="jsp.tools.move-item.collection.to.msg"/></strong></small>
				</td>
				<td class="standard">
				<select name="collection_to_id">
<%
		//Later on find a away to display in a tree format with the linked one disabled?
        for (int i = 0; i < notLinkedCollections.length; i++)
        {
%>
            <option value="<%= notLinkedCollections[i].getID() %>"><%= notLinkedCollections[i].getMetadata("name") %></option>
<%
        }
%>
				</select>
			</td>
         </tr>
         <tr>
            <td class="standard"><small><strong><fmt:message key="jsp.tools.move-item.inheritpolicies"/></strong></small></td>
            <td class="standard"><input type="checkbox" name="inheritpolicies" /></td>
         </tr>
		 <tr>
       		<td class="standard"></td>
       		<td class="standard">
				<input type="submit" name="submit" value="<fmt:message key="jsp.tools.move-item.button"/>"/>
			</td>
         </tr>
        </table>
        </td>
      </tr>
     </table>
      <input type="hidden" name="action" value="<%=EditItemServlet.CONFIRM_MOVE_ITEM%>" />
      <input type="hidden" name="item_id" value="<%=item.getID() %>"/> 
    </form>


</dspace:layout>
