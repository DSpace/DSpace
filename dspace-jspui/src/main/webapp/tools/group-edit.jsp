<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Show contents of a group (name, epeople)
  -
  - Attributes:
  -   group - group to be edited
  -
  - Returns:
  -   cancel - if user wants to cancel
  -   add_eperson - go to group_eperson_select.jsp to choose eperson
  -   change_name - alter name & redisplay
  -   eperson_remove - remove eperson & redisplay
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.eperson.Group"   %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="java.util.List" %>

<%
    Group group = (Group) request.getAttribute("group");
    List<EPerson> epeople = (List<EPerson>) request.getAttribute("members");
    
	List<Group>   groups  = (List<Group>) request.getAttribute("membergroups");
	request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout style="submission" titlekey="jsp.tools.group-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

	<h1><fmt:message key="jsp.tools.group-edit.title"/> : <%=group.getName()%> (id: <%=group.getID()%>)
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\") +\"#groupeditor\"%>"><fmt:message key="jsp.help"/></dspace:popup>
	</h1>
    <form name="epersongroup" method="post" action="">
	<div class="row"><label for="tgroup_name" class="col-md-2">
		<fmt:message key="jsp.tools.group-edit.name"/></label>
	<span class="col-md-10">
		<input class="form-control" name="group_name" id="tgroup_name" value="<%= Utils.addEntities(group.getName()) %>"/>
	</span>
	</div>
	<br/>
    <div class="alert alert-warning"><fmt:message key="jsp.tools.group-edit.heading"/></div>

    <input type="hidden" name="group_id" value="<%=group.getID()%>"/>
    
    <div class="row">
    <div class="col-md-6"> 
	    <label for="eperson_id"><fmt:message key="jsp.tools.group-edit.eperson"/></label>
	    <dspace:selecteperson multiple="true" selected="<%= epeople.toArray(new EPerson[epeople.size()]) %>"/>
    </div>
    
    <div class="col-md-6">
	    <label for="eperson_id"><fmt:message key="jsp.tools.group-edit.group"/></label>
	    <dspace:selectgroup   multiple="true" selected="<%= groups.toArray(new Group[groups.size()])  %>"/>
	</div>
	</div>
	<br/>
    <div class="row"><input class="btn btn-success col-md-2 col-md-offset-5" type="submit" name="submit_group_update" value="<fmt:message key="jsp.tools.group-edit.update.button"/>" onclick="javascript:finishEPerson();finishGroups();"/></div>
   </form>
</dspace:layout>
