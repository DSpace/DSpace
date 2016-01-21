<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
   - This page lists the current supervisory settings for workspace items
   -
   - Attributes:
   -    supervised  - An array of supervised items
   --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.content.WorkspaceItem" %>
<%@ page import="java.util.List" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<%
    // get the object array out of the request
    List<WorkspaceItem> supervisedItems = (List<WorkspaceItem>) request.getAttribute("supervised");
    request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout 
			   style="submission"
			   titlekey="jsp.dspace-admin.supervise-list.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.supervise-list.heading"/>
<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#supervision\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
</h1>

<p class="help-block"><fmt:message key="jsp.dspace-admin.supervise-list.subheading"/></p>


<table class="table">
    <tr>
        <th class="oddRowOddCol">
            &nbsp;
        </th>
        <th class="oddRowEvenCol">
            <fmt:message key="jsp.dspace-admin.supervise-list.group"/>
        </th>
        <th class="oddRowOddCol">
            <fmt:message key="jsp.dspace-admin.supervise-list.author"/>
        </th>
        <th class="oddRowEvenCol">
            <fmt:message key="jsp.dspace-admin.supervise-list.title"/>
        </th>
        <th class="oddRowOddCol">
            &nbsp;
        </th>
    </tr>
<%
    String row = "even";

    for (int i = 0; i < supervisedItems.size(); i++)
    {
        // get title (or "untitled" if not set), author, and supervisors of 
        // the supervised item
        String title = supervisedItems.get(i).getItem().getName();
//        String title = (titleArray.length > 0 ? titleArray[0].value : "Untitled");
        EPerson submitter = supervisedItems.get(i).getItem().getSubmitter();
        List<Group> supervisors = supervisedItems.get(i).getSupervisorGroups();

        for (int j = 0; j < supervisors.size(); j++)
        {
%>

    <tr>
        <td class="<%= row %>RowOddCol">
            <%-- form to navigate to the item policies --%>
            <form action="<%= request.getContextPath() %>/tools/authorize" method="post">
                <input type="hidden" name="item_id" value="<%=supervisedItems.get(i).getItem().getID() %>"/>
                <input class="btn btn-info" type="submit" name="submit_item_select" value="<fmt:message key="jsp.dspace-admin.supervise-list.policies.button"/>"/>
            </form>
        </td>
        <td class="<%= row %>RowEvenCol">
            <%= supervisors.get(j).getName() %>
        </td>
        <td class="<%= row %>RowOddCol">
            <a href="mailto:<%= submitter.getEmail() %>"><%= Utils.addEntities(submitter.getFullName()) %></a>
        </td>
        <td class="<%= row %>RowEvenCol">
<%
		if (StringUtils.isNotBlank(title))
		{
%>
			<%= title %>
<%
		}
		else
		{
%>
			<fmt:message key="jsp.general.untitled"/>
<%
		}
%>
        </td>
        <td class="<%= row %>RowOddCol">
            <%-- form to request removal of supervisory linking --%>
            <form method="post" action="">
            <input type="hidden" name="gID" value="<%= supervisors.get(j).getID() %>"/>
            <input type="hidden" name="siID" value="<%= supervisedItems.get(i).getID() %>"/>
            <input class="btn btn-danger" type="submit" name="submit_remove" value="<fmt:message key="jsp.dspace-admin.general.remove"/>"/>
            </form>
        </td>
    </tr> 

<%
        row = (row.equals("even") ? "odd" : "even" );
        }
    }
%>

</table>
<div class="pull-right">
<%-- form to navigate to the "add supervisory settings" page --%> 
<form method="post" action="">
    <input class="btn btn-default" type="submit" name="submit_base" value="<fmt:message key="jsp.dspace-admin.supervise-list.back.button"/>"/>
    <input class="btn btn-success" type="submit" name="submit_add" value="<fmt:message key="jsp.dspace-admin.supervise-list.add.button"/>"/>
</form>
</div>
</dspace:layout>
