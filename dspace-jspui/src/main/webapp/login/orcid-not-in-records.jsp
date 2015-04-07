<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Display message indicating that the user's certificate was valid
  - but there is no EPerson record for them
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.login.not-in-records.title">

    <%-- <h1>No User Record Available</h1> --%>
    <h1><fmt:message key="jsp.login.orcid-not-in-records.title"/></h1>

    
    <p><fmt:message key="jsp.login.orcid-not-in-records.text"/></p>

    <%-- <p><a href="<%= request.getContextPath() %>/register">Register wth DSpace</a></p> --%>
    <p><a href="<%= request.getContextPath() %>/register"><fmt:message key="jsp.login.not-in-records.register"/></a></p>
</dspace:layout>
