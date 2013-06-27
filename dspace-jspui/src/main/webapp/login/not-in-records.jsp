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
    <h1><fmt:message key="jsp.login.not-in-records.title"/></h1>

    <%-- <p>You have a valid Web certficate, but the DSpace system does not have a
    record of you.  You will need to <a href="<%= request.getContextPath() %>/register">
    register with DSpace</a> before using those areas of the system that
    require a user account.</p> --%>
    <p><fmt:message key="jsp.login.not-in-records.text">
        <fmt:param><%= request.getContextPath() %>/register</fmt:param>
    </fmt:message></p>

    <%-- <p><a href="<%= request.getContextPath() %>/register">Register wth DSpace</a></p> --%>
    <p><a href="<%= request.getContextPath() %>/register"><fmt:message key="jsp.login.not-in-records.register"/></a></p>
</dspace:layout>
