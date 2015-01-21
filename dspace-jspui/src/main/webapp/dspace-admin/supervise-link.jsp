<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
   - This page provides the form to link groups with workspace items for
   - supervision.  You may also specify default policies for the group to use
   -
   - Attributes:
   -    groups  - An array of all the epersongroups in the database
   -    wsItems - An array of all the workspace items on the system (using
   -                EULWorkspaceItem
   --%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.content.Metadatum" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.content.WorkspaceItem" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="org.dspace.eperson.Supervisor" %>
<%@ page import="org.dspace.core.Utils" %>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>

<%
    // get objects from request
    Group[] groups = (Group[]) request.getAttribute("groups");
    WorkspaceItem[] workspaceItems = (WorkspaceItem[]) request.getAttribute("wsItems");
    request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout style="submission"
			   titlekey="jsp.dspace-admin.supervise-link.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.supervise-link.heading"/>
<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#supervision\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
</h1>

<p class="help-block"><fmt:message key="jsp.dspace-admin.supervise-link.choose"/></p>

<form method="post" action="">

<div class="input-group">
<%-- Select the group to supervise --%>
    
            <label class="input-group-addon"><fmt:message key="jsp.dspace-admin.supervise-link.group"/></label> 
            <select class="form-control" name="TargetGroup">
<%
    for (int i = 0; i < groups.length; i++)
    {
%>
                <option value="<%= groups[i].getID() %>"><%= groups[i].getName() %></option>
<%
    }
%>
            </select>


<%-- Select the defaul policy type --%>

            <label class="input-group-addon"><fmt:message key="jsp.dspace-admin.supervise-link.policy"/></label>
            <select class="form-control" name="PolicyType">
                <option value="<%= Supervisor.POLICY_NONE %>" selected="selected"><fmt:message key="jsp.dspace-admin.supervise-link.policynone"/></option>
                <option value="<%= Supervisor.POLICY_EDITOR %>"><fmt:message key="jsp.dspace-admin.supervise-link.policyeditor"/></option>
                <option value="<%= Supervisor.POLICY_OBSERVER %>"><fmt:message key="jsp.dspace-admin.supervise-link.policyobserver"/></option>
            </select>
</div>
<%-- Select the workspace item to be supervised --%>
<br/>
<div>
            <p><b><fmt:message key="jsp.dspace-admin.supervise-link.workspace"/></b></p>
            
            <table class="table">
                <tr>
                    <th class="odRowOddCol"><fmt:message key="jsp.dspace-admin.supervise-link.id"/></th>
                    <th class="oddRowEvenCol"><fmt:message key="jsp.dspace-admin.supervise-link.submittedby"/></th>
                    <th class="oddRowOddCol"><fmt:message key="jsp.dspace-admin.supervise-link.title"/></th>
                    <th class="oddRowEvenCol"><fmt:message key="jsp.dspace-admin.supervise-link.submittedto"/></th>
                    <th class="oddRowOddCol"><fmt:message key="jsp.dspace-admin.supervise-link.select"/></th>
                </tr>
<%
    String row = "even";

    for (int i = 0; i < workspaceItems.length; i++)
    {
        // get title (or "untitled" if none) and submitter of workspace item
        Metadatum[] titleArray = workspaceItems[i].getItem().getDC("title", null, Item.ANY);
//        String title = (titleArray.length > 0 ? titleArray[0].value : "Untitled");
        EPerson submitter = workspaceItems[i].getItem().getSubmitter();
%>
                <tr>
                    <td class="<%= row %>RowOddCol">
                        <%= workspaceItems[i].getID() %>
                    </td>
                    <td class="<%= row %>RowEvenCol">
                        <a href="mailto:<%= submitter.getEmail() %>"><%= Utils.addEntities(submitter.getFullName()) %></a>
                    </td>
                    <td class="<%= row %>RowOddCol">
<%
					if (titleArray.length > 0)
					{
%>
						<%= titleArray[0].value %>
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
                    <td class="<%= row %>RowEvenCol">
                        <%= workspaceItems[i].getCollection().getMetadata("name") %>
                    </td>
                    <td class="<%= row %>RowOddCol" align="center">
                        <input type="radio" name="TargetWSItem" value="<%= workspaceItems[i].getID() %>"/>
                    </td>
                </tr>
<%
    row = (row.equals("even") ? "odd" : "even" );
    }
%>
            </table>
</div>
<div class="pull-right">
  	<input class="btn btn-default" type="submit" name="submit_base" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>"/>
	<input class="btn btn-success" type="submit" name="submit_link" value="<fmt:message key="jsp.dspace-admin.supervise-link.submit.button"/>"/>
</div>
</form>

</dspace:layout>
