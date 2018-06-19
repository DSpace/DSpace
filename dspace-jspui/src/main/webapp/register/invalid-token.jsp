<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.core.ConfigurationManager" %>


<dspace:layout titlekey="jsp.register.invalid-token.title">

<%--
  - Invalid token sent message.
  --%>


    <%-- <h1>Invalid Token</h1> --%>
	<h1><fmt:message key="jsp.register.invalid-token.title"/></h1>

    <%-- <p>The registration or forgotten password "token" in the URL is invalid.
    This may be because of one of the following reason:</p> --%>
	<p><fmt:message key="jsp.register.invalid-token.info1"/></p>

    <ul>
        <%--  <li>The token might be incorrectly copied into the URL.  Some e-mail
        programs will "wrap" long lines of text in an email, so maybe it split
        your special URL up into two lines, like this: --%>
		<li><fmt:message key="jsp.register.invalid-token.info2"/>
        <pre>
<%= ConfigurationManager.getProperty("dspace.url") %>/register?token=ABCDEFGHIJKLMNOP
        </pre>

        <%-- If it has, you should copy and paste the first line into your browser's
        address bar, then copy the second line, and paste into the address bar
        just on the end of the first line, making sure there are no spaces.  The
        address bar should then contain something like: --%>
		<li><fmt:message key="jsp.register.invalid-token.info3"/>

        <pre>
<%= ConfigurationManager.getProperty("dspace.url") %>/register?token=ABCDEFGHIJKLMNOP
        </pre>

        <%-- Then press return in the address bar, and the URL should work fine.</li> --%>
		<fmt:message key="jsp.register.invalid-token.info4"/></li>
    </ul>

    <%-- <p>If you're still having trouble, please contact us.</p> --%>
	<p><fmt:message key="jsp.register.invalid-token.info5"/></p>
    
    <dspace:include page="/components/contact-info.jsp" />
</dspace:layout>
