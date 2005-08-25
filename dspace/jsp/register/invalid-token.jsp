<%--
  - invalid-token.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
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
<%= ConfigurationManager.getProperty("dspace.url") %>/register?token=ABCDEFGHIJK
LMNOP
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
