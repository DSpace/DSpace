<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of groups, with continue and cancel buttons
  -
  - Attributes:
  -   group  - group we're working on
  -   groups - all groups we can select from
  - Returns:
  -   submit set to add_group_add, user has selected at least one group
  -   submit set to add_group_cancel, user has cancelled operation
  -   group_id  - group we're working on
  -   groups_id - groups user has selected

  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ page import="org.dspace.eperson.Group"   %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="java.util.List" %>

<%
    Group group = (Group) request.getAttribute("group");
    List<Group> groups =
        (List<Group>) request.getAttribute("groups");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.group-group-select.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin"
               nocache="true">

    <%--<h1>Select Group to Add to Group <%= group.getID() %></h1>--%>
	<h1><fmt:message key="jsp.dspace-admin.group-group-select.heading">
        <fmt:param><%= group.getID() %></fmt:param>
    </fmt:message></h1>

    <form method="post" action="">

   
                <input type="hidden" name="group_id" value="<%=group.getID()%>" />
   				<div class="row col-md-4 col-md-offset-4">
                    <select class="form-control" size="15" name="groups_id" multiple="multiple">
                        <%  for (int i = 0; i < groups.size(); i++) { %>
                            <option value="<%= groups.get(i).getID()%>">
                                <%= Utils.addEntities(groups.get(i).getName())%>
                            </option>
                        <%  } %>
                </select>
                </div>
				<br/>
				<div class="btn-group pull-right col-md-7">
                	<%--<input type="submit" name="submit_add_group_add" value="Add Group" />--%>
					<input class="btn btn-primary" type="submit" name="submit_add_group_add" value="<fmt:message key="jsp.dspace-admin.group-group-select.add"/>" />
                
                    <%--<input type="submit" name="submit_add_group_cancel" value="Cancel" />--%>
					<input class="btn btn-default" type="submit" name="submit_add_group_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
                </div>     

    </form>
</dspace:layout>
