<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Message informing MIT user they need to use a certificate to log in
  - FIXME: MIT-specific message
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.error.require-certificate.title">

    <%-- <h1>Certificate Required</h1> --%>
    <h1><fmt:message key="jsp.error.require-certificate.title"/></h1>

    <%-- <p>The configuration of this DSpace site means that you need a valid
    Web certificate to log in.  If you are having problems with this,
    please contact us.</p> --%>
    <p><fmt:message key="jsp.error.require-certificate.text"/></p>

    <dspace:include page="/components/contact-info.jsp" />

</dspace:layout>
