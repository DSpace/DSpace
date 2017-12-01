<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page representing an eperson reset password error
  
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page isErrorPage="true" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.eperson-main.ResetPassword.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin">

    <h1><fmt:message key="jsp.dspace-admin.eperson-main.ResetPassword.head" /></h1>

    
    <p><fmt:message key="jsp.dspace-admin.eperson-main.ResetPassword-error.errormsg"/></p>

     
    <p align="center">
        <a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople"><fmt:message key="jsp.dspace-admin.eperson-main.ResetPassword.returntoedit" /></a>
    </p>

</dspace:layout>

