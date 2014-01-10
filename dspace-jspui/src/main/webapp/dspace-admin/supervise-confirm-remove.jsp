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
   -    wsItem  - An item that is going to be removed
   -    group   - the group supervising the item
   --%>
   
   

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group" %>
<%@ page import="org.dspace.content.WorkspaceItem" %>
<%@ page import="org.dspace.core.Utils" %>

<%
    // get item and group out of the request
    WorkspaceItem wsItem = (WorkspaceItem) request.getAttribute("wsItem");
    Group group = (Group) request.getAttribute("group");
    request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout style="submission"
			   titlekey="jsp.dspace-admin.supervise-confirm-remove.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.heading"/></h1>

<h3><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.subheading"/></h3>

<%
        DCValue[] titleArray = wsItem.getItem().getDC("title", null, Item.ANY);
//        String title = (titleArray.length > 0 ? titleArray[0].value : "Untitled");
        EPerson submitter = wsItem.getItem().getSubmitter();
%>
<div class="row">
<label class="col-md-2"><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.titleheader"/>:</label>
<span class="col-md-3">
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
</span>
</div>
<div class="row">
<label class="col-md-2"><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.authorheader"/>:</label>
<span class="col-md-3">
<a href="mailto:<%= submitter.getEmail() %>"><%= Utils.addEntities(submitter.getFullName()) %></a>
</span>
</div>
<div class="row">
<label class="col-md-2"><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.supervisorgroupheader"/>:</label>
<span class="col-md-3">
<%= group.getName() %>
</span>
</div>

<div class="row">
<label class="col-md-5"><fmt:message key="jsp.dspace-admin.supervise-confirm-remove.confirm"/></label>
</div>

<%-- form to request removal of supervisory linking --%>
<form method="post" action="">
    <input type="hidden" name="gID" value="<%= group.getID() %>"/>
    <input type="hidden" name="siID" value="<%= wsItem.getID() %>"/>
    <input class="btn btn-default" type="submit" name="submit_doremove" value="<fmt:message key="jsp.dspace-admin.general.remove"/>"/>
    <input class="btn btn-default" type="submit" name="submit_base" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>"/>
</form>


</dspace:layout>
