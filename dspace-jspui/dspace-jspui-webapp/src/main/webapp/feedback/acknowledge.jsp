<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Feedback received OK acknowledgement
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.feedback.acknowledge.title">

    <%-- <h1>Thank you for your comments!</h1> --%>
    <h1><fmt:message key="jsp.feedback.acknowledge.title"/></h1>

    <%-- <p>Your comments have been received.</p> --%>
    <p><fmt:message key="jsp.feedback.acknowledge.text"/></p>

</dspace:layout>
