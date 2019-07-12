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

<dspace:layout titlekey="jsp.login.orcid-different-profile.title">

    <%-- <h1>No User Record Available</h1> --%>
    <h1><fmt:message key="jsp.login.orcid-different-profile.title"/></h1>

    
    <p><fmt:message key="jsp.login.orcid-different-profile.text">
        	<fmt:param><%= request.getAttribute("orcid.standalone.error") %></fmt:param>
    		<fmt:param><%= request.getAttribute("orcid.standalone.ticket") %></fmt:param>
	</fmt:message></p>

</dspace:layout>
