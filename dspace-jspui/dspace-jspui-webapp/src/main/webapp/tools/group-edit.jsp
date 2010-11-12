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

<%
    Group group = (Group) request.getAttribute("group");
    EPerson [] epeople = (EPerson []) request.getAttribute("members");
    
	Group   [] groups  = (Group   []) request.getAttribute("membergroups");
	request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout titlekey="jsp.tools.group-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

  <table width="95%">
    <tr>
      <td align="left">
	<h1><fmt:message key="jsp.tools.group-edit.title"/> : <%=group.getName()%> (id: <%=group.getID()%>)</h1>
      </td>
      <td align="right" class="standard">
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\") +\"#groupeditor\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

  <center>
    <form name="epersongroup" method="post" action="">
	<p><label for="tgroup_name"><fmt:message key="jsp.tools.group-edit.name"/></label><input name="group_name" id="tgroup_name" value="<%= Utils.addEntities(group.getName()) %>"/></p>
   	    <h3><fmt:message key="jsp.tools.group-edit.heading"/></h3>

        <input type="hidden" name="group_id" value="<%=group.getID()%>"/>
        <table>
          <tr>
            <td align="center"><strong><fmt:message key="jsp.tools.group-edit.eperson"/></strong><br/>
              <dspace:selecteperson multiple="true" selected="<%= epeople %>"/> 
            </td>
            <td align="center"><strong><fmt:message key="jsp.tools.group-edit.group"/></strong><br/>
              <dspace:selectgroup   multiple="true" selected="<%= groups  %>"/>
            </td>
		  </tr>
        </table>

        <br/>

        <p><input type="submit" name="submit_group_update" value="<fmt:message key="jsp.tools.group-edit.update.button"/>" onclick="javascript:finishEPerson();finishGroups();"/></p>
   </form>
  </center>
</dspace:layout>
