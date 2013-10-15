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

<%
    Group[] groups =
        (Group[]) request.getAttribute("groups");
%>

<dspace:layout style="submission" titlekey="jsp.tools.group-list.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

    <%--  <h1>Group Editor</h1> --%>
    <h1><fmt:message key="jsp.tools.group-list.title"/>
    <%-- <dspace:popup page="/help/site-admin.html#groups">Help...</dspace:popup> --%>
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#groups\"%>"><fmt:message key="jsp.help"/></dspace:popup>
    </h1>
    
  	
	<p class="alert alert-info"><fmt:message key="jsp.tools.group-list.note1"/></p>	
	<p class="alert alert-warning"><fmt:message key="jsp.tools.group-list.note2"/></p>
   	
    <form method="post" action="">
        <div class="row col-md-offset-5">
	    	<input class="btn btn-success" type="submit" name="submit_add" value="<fmt:message key="jsp.tools.group-list.create.button"/>" />
        </div>
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
    for (int i = 0; i < groups.length; i++)
    {
%>
            <tr>
                <td class="<%= row %>RowOddCol"><%= groups[i].getID() %></td>
                <td class="<%= row %>RowEvenCol">
                    <%= groups[i].getName() %>
                </td>
                <td class="<%= row %>RowOddCol">
<%
	// no edit button for group anonymous
	if (groups[i].getID() > 0 )
	{
%>                  
                    <form method="post" action="">
                        <input type="hidden" name="group_id" value="<%= groups[i].getID() %>"/>
  		        <input class="btn btn-default col-md-6" type="submit" name="submit_edit" value="<fmt:message key="jsp.tools.general.edit"/>" />
                   </form>
<%
	}

	// no delete button for group Anonymous 0 and Administrator 1 to avoid accidental deletion
	if (groups[i].getID() > 1 )
	{
%>   
                    <form method="post" action="">
                        <input type="hidden" name="group_id" value="<%= groups[i].getID() %>"/>
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
