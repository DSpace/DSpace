<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Forgot password token sent message.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.register.password-token-sent.title">
    <%-- <h1>New Password E-mail Sent</h1> --%>
	<h1><fmt:message key="jsp.register.password-token-sent.title"/></h1>

    <%-- <p>You have been sent an e-mail containing a special URL.  When you visit
    this URL, you will be able to set a new password to carry on using DSpace.</p> --%>
	<p><fmt:message key="jsp.register.password-token-sent.info"/></p>

</dspace:layout>
