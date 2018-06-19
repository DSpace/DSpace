<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of Groups, with 'edit' and 'delete' buttons next to them
  -
  - Attributes:
  -
  -   groups - Group [] of groups to work on
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
    
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="java.util.List" %>

<%
    List<Group> groups =
        (List<Group>) request.getAttribute("groups");
        
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

<dspace:layout style="submission" titlekey="jsp.tools.group-list.title"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="<%= link %>"
               nocache="true">

    <%--  <h1>Group Editor</h1> --%>
    <h1><fmt:message key="jsp.tools.group-list.title"/>
    <%-- <dspace:popup page="/help/site-admin.html#groups">Help...</dspace:popup> --%>
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#groups\"%>"><fmt:message key="jsp.help"/></dspace:popup>
    </h1>
    
  	
	<p class="alert alert-info"><fmt:message key="jsp.tools.group-list.note1"/></p>	
	<p class="alert alert-warning"><fmt:message key="jsp.tools.group-list.note2"/></p>
   	
    <form method="post" action="">
        <% if(isAdmin){ %>
            <div class="row col-md-offset-5">
                    <input class="btn btn-success" type="submit" name="submit_add" value="<fmt:message key="jsp.tools.group-list.create.button"/>" />
            </div>
        <% } %>
    </form>
	<br/>
	
    <table class="table" summary="Group data display table">
        <tr>
            <th class="oddRowOddCol"><strong><fmt:message key="jsp.tools.group-list.id" /></strong></th>
			<th class="oddRowEvenCol"><strong><fmt:message key="jsp.tools.group-list.name"/></strong></th>
            <th class="oddRowOddCol">&nbsp;</th>
        </tr>

<%
    String row = "even";
    for (int i = 0; i < groups.size(); i++)
    {
%>
            <tr>
                <td class="<%= row %>RowOddCol"><%= groups.get(i).getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= groups.get(i).getName() %>
                </td>
                <td class="<%= row %>RowOddCol">
<%
	// no edit button for group anonymous
    if (!groups.get(i).getName().equals(Group.ANONYMOUS))
	{
%>                  
                    <form method="post" action="">
                        <input type="hidden" name="group_id" value="<%= groups.get(i).getID() %>"/>
  		        <input class="btn btn-default col-md-6" type="submit" name="submit_edit" value="<fmt:message key="jsp.tools.general.edit"/>" />
                   </form>
<%
	}

	// no delete button for group Anonymous 0 and Administrator 1 to avoid accidental deletion
	if (!groups.get(i).getName().equals(Group.ANONYMOUS) && !groups.get(i).getName().equals(Group.ADMIN))
	{
%>   
                    <form method="post" action="">
                        <input type="hidden" name="group_id" value="<%= groups.get(i).getID() %>"/>
	                <input class="btn btn-danger col-md-6" type="submit" name="submit_group_delete" value="<fmt:message key="jsp.tools.general.delete"/>" />
<%
	}
%>	                
                    </form>
                </td>
            </tr>
<%
        row = (row.equals("odd") ? "even" : "odd");
    }
%>
    </table>
</dspace:layout>
