<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
	
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditItemServlet" %>
<%@ page import="java.util.List" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
	List<Collection> notLinkedCollections = (List<Collection> )request.getAttribute("notLinkedCollections");
	List<Collection> linkedCollections = (List<Collection>)request.getAttribute("linkedCollections");
	
	Item item = (Item)request.getAttribute("item");
%>

<dspace:layout style="submission" titlekey="jsp.tools.move-item.title">
	<div class="container">
   	<form class="form-horizontal" action="<%=request.getContextPath()%>/tools/edit-item" method="post">
			<div class="form-group">   		
				  <label><fmt:message key="jsp.tools.move-item.item.name.msg"/></label>
			      <%=item.getName()%></font>
			</div>
		  <div class="form-group">
		  	<div class="input-group">
		  	<span class="input-group-addon">
				<label for="collection_from_id"><fmt:message key="jsp.tools.move-item.collection.from.msg"/></label>
			</span>
				<select class="form-control" name="collection_from_id">
<%
        for (int i = 0; i < linkedCollections.size(); i++)
        {
%>
            <option value="<%= linkedCollections.get(i).getID() %>"><%= linkedCollections.get(i).getName() %></option>
<%
        }
%>
				</select>
				</div>
		</div>
		<div class="form-group">
			<div class="input-group">
		  	<span class="input-group-addon">
				<label for="collection_to_id"><fmt:message key="jsp.tools.move-item.collection.to.msg"/></label>
			</span>
			<select class="form-control" name="collection_to_id">
<%
		//Later on find a away to display in a tree format with the linked one disabled?
        for (int i = 0; i < notLinkedCollections.size(); i++)
        {
%>
            <option value="<%= notLinkedCollections.get(i).getID() %>"><%= notLinkedCollections.get(i).getName() %></option>
<%
        }
%>
				</select>
			</div>
		</div>
		<div class="form-group">
			<div class="input-group">
		  		<span class="input-group-addon">
            		<input type="checkbox" name="inheritpolicies" />
            	</span>
				<span class="form-control"><fmt:message key="jsp.tools.move-item.inheritpolicies"/></span>            	
            </div>
         </div>
		 <div class="col-md-offset-5">
				<input class="btn btn-success col-md-4" type="submit" name="submit" value="<fmt:message key="jsp.tools.move-item.button"/>"/>
		</div>
      <input type="hidden" name="action" value="<%=EditItemServlet.CONFIRM_MOVE_ITEM%>" />
      <input type="hidden" name="item_id" value="<%=item.getID() %>"/> 
    </form>

</div>
</dspace:layout>
