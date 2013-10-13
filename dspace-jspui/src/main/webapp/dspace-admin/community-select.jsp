<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of communities, with continue and cancel buttons
  -  post method invoked with community_select or community_select_cancel
  -     (community_id contains ID of selected community)
  -
  - Attributes:
  -   communities - a Community [] containing all communities in the system
  - Returns:
  -   submit set to community_select, user has selected a community
  -   submit set to community_select_cancel, return user to main page
  -   community_id - set if user has selected one

  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Community" %>

<%
    Community [] communities =
        (Community[]) request.getAttribute("communities");
        
    request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.community-select.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>communities:</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.community-select.com"/></h1>

    <form method="post" action="">

				<div class="row col-md-4 col-md-offset-4">
                    <select class="form-control" size="12" name="community_id">
                        <%  for (int i = 0; i < communities.length; i++) { %>
                            <option value="<%= communities[i].getID()%>">
                                <%= communities[i].getMetadata("name")%>
                            </option>
                        <%  } %>
                    </select>
				</div>
				<br/>
				<div class="btn-group pull-right col-md-7">
                    <%-- <input type="submit" name="submit_community_select" value="Edit Policies"> --%>

                    	<input class="btn btn-primary" type="submit" name="submit_community_select" value="<fmt:message key="jsp.dspace-admin.general.editpolicy"/>" />
	
                    <%-- <input type="submit" name="submit_community_select_cancel" value="Cancel"> --%>
                    	<input class="btn btn-default" type="submit" name="submit_community_select_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />

				</div>

    </form>
</dspace:layout>
