<%--
  - Show contents of a unit
  -
  - Attributes:
  -   unit - unit to be edited
  -
  - Returns:
  -   cancel - if user wants to cancel
  -   change_name - alter name & redisplay
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.eperson.Group"   %>
<%@ page import="org.dspace.eperson.Unit"   %>
<%@ page import="org.dspace.core.Utils" %>

<%
    Unit unit = (Unit) request.getAttribute("unit");
    Group[] groups  = (Group[]) request.getAttribute("groups");
    request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout titlekey="jsp.tools.unit-edit.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

  <table width="95%">
    <tr>
      <td align="left">
	<h1><fmt:message key="jsp.tools.unit-edit.title"/> : <%=unit.getName()%> (id: <%=unit.getID()%>)</h1>
      </td>
      <td align="right" class="standard">
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\") +\"#uniteditor\"%>"><fmt:message key="jsp.help"/></dspace:popup>
      </td>
    </tr>
  </table>

  <center>
    <form name="epersongroup" method="post" action="">
	<p><label for="tunit_name"><fmt:message key="jsp.tools.unit-edit.name"/></label><input name="unit_name" id="tunit_name" value="<%= Utils.addEntities(unit.getName()) %>" size="50"/></p>
        <p>
          <label for="tfaculty_only">Faculty Only: </label>
          <input type="checkbox" name="faculty_only" id="tfaculty_only" value="true"<%= unit.getFacultyOnly() ? " checked=\"checked\"" : "" %> />
        </p>
   	    <h3><fmt:message key="jsp.tools.unit-edit.heading"/></h3>

        <input type="hidden" name="unit_id" value="<%=unit.getID()%>"/>
        <table>
          <tr>
            <td align="center"><strong><fmt:message key="jsp.tools.unit-edit.group"/></strong><br/>
              <dspace:selectgroup multiple="true" selected="<%= groups %>"/>
            </td>
          </tr>
        </table>

        <br/>

        <p><input type="submit" name="submit_unit_update" value="<fmt:message key="jsp.tools.unit-edit.update.button"/>"  onclick="javascript:finishGroups();"/></p>
   </form>
  </center>
</dspace:layout>
