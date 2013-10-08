<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page representing an eperson loginas error
  
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.eperson-main.loginAs.authorize.title">

    <%-- <h1>Authorization Required</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.eperson-main.loginAs.authorize.title"/></h1>

        
    <p><fmt:message key="jsp.dspace-admin.eperson-main.loginAs.authorize.errormsg"/></p>

     
    <p align="center">
        <a href="<%= request.getContextPath() %>/dspace-admin/edit-epeople"><fmt:message key="jsp.dspace-admin.eperson-main.loginAs.backtoeditpeople" /></a>
    </p>

</dspace:layout>

