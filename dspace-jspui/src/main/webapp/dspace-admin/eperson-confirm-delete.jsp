<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Confirm deletion of an e-person
  -
  - Attributes:
  -    eperson   - eperson we may delete
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.Utils" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");
%>
<dspace:layout style="submission" titlekey="jsp.dspace-admin.eperson-confirm-delete.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <%-- <h1>Delete e-person: <%= Utils.addEntities(eperson.getFullName()) %> (<%= eperson.getEmail() %>)</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.eperson-confirm-delete.heading">
        <fmt:param><%= Utils.addEntities(eperson.getFullName()) %></fmt:param>
        <fmt:param><%= eperson.getEmail() %></fmt:param>
    </fmt:message></h1>
    
    <%-- <p>Are you sure this e-person should be deleted?</p> --%>
    <div class="row col-md-offset-4">
    	<span class="col-md-6"><fmt:message key="jsp.dspace-admin.eperson-confirm-delete.confirm"/></span>
    </div>
    
    <form method="post" action="">
        <input type="hidden" name="eperson_id" value="<%= eperson.getID() %>"/>
		<div class="btn-group col-md-offset-5">
			<%-- <input type="submit" name="submit_confirm_delete" value="Delete"> --%>
        	<input type="submit" class="btn btn-danger col-md-6" name="submit_confirm_delete" value="<fmt:message key="jsp.dspace-admin.general.delete"/>" />
        	<%-- <input type="submit" name="submit_cancel" value="Cancel"> --%>
        	<input type="submit" class="btn btn-default col-md-6" name="submit_cancel" value="<fmt:message key="jsp.dspace-admin.general.cancel"/>" />
        </div>
    </form>
    
</dspace:layout>

