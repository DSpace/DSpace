<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
   - This page displays an error message if the administrator attempts to add
   - a supervisor setting which already exists
   --%>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
    
<dspace:layout 
			   style="submission"
		       titlekey="jsp.dspace-admin.supervise-duplicate.title"
               navbar="admin"
               locbar="link"
               parentlink="/dspace-admin"
               parenttitlekey="jsp.administer">

<h1><fmt:message key="jsp.dspace-admin.supervise-duplicate.heading"/></h1>

<p><fmt:message key="jsp.dspace-admin.supervise-duplicate.errormsg"/></p>

<div align="center">
<a href="<%= request.getContextPath() %>/dspace-admin/supervise"><fmt:message key="jsp.dspace-admin.supervise-duplicate.return"/></a>
</div>

</dspace:layout>