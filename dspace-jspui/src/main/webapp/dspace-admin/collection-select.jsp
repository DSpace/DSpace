<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display list of collections, with continue and cancel buttons
  -  post method invoked with collection_select or collection_select_cancel
  -     (collection_id contains ID of selected collection)
  -
  - Attributes:
  -   collections - a Collection [] containing all collections in the system
  - Returns:
  -   submit set to collection_select, user has selected a collection
  -   submit set to collection_select_cancel, return user to main page
  -   collection_id - set if user has selected one

  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Collection" %>

<%
    Collection [] collections =
        (Collection[]) request.getAttribute("collections");
       
    request.setAttribute("LanguageSwitch", "hide");
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.collection-select.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Collections:</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.collection-select.col"/></h1>

    <form method="post" action="">
				<div class="row col-md-4 col-md-offset-4">
                    <select class="form-control" size="12" name="collection_id">
                        <%  for (int i = 0; i < collections.length; i++) { %>
                            <option value="<%= collections[i].getID()%>">
                                <%= collections[i].getMetadata("name")%>
                            </option>
                        <%  } %>
                    </select>
                </div>
                <br/>
				<div class="btn-group pull-right col-md-7">
                    <%-- <input type="submit" name="submit_collection_select" value="Edit Policies"> --%>
                    <input class="btn btn-primary" type="submit" name="submit_collection_select" value="<fmt:message key="jsp.dspace-admin.general.editpolicy"/>" />
				
                    <%-- <input type="submit" name="submit_collection_select_cancel" value="Cancel"> --%>
                    <input class="btn btn-default" type="submit" name="submit_collection_select_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
     			</div>

    </form>
</dspace:layout>
