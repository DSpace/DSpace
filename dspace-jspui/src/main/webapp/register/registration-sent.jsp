<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Registration token sent message.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.register.registration-sent.title">

     <%-- <h1>Registration E-mail Sent</h1> --%>
	 <h1><fmt:message key="jsp.register.registration-sent.title"/></h1>

    <%-- <p>You have been sent an e-mail containing a special URL, or "token".  When
    you visit this URL, you will need to fill out some simple information.
    After that,	you'll be ready to log into DSpace!</p> --%>
	<p class="alert alert-info"><fmt:message key="jsp.register.registration-sent.info"/></p>

</dspace:layout>
