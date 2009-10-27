<%--
  - registered.jsp
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

<%--
  - Registered OK message
  -
  - Displays a message indicating that the user has registered OK.
  - 
  - Attributes to pass in:
  -   eperson - eperson who's just registered
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
	
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.servlet.RegisterServlet" %>
<%@ page import="org.dspace.eperson.EPerson" %>

<%
    EPerson eperson = (EPerson) request.getAttribute("eperson");
%>

<dspace:layout titlekey="jsp.register.registered.title">

    <%-- <h1>Registration Complete</h1> --%>
	<h1><fmt:message key="jsp.register.registered.title"/></h1>
    
    <%-- <p>Thank you <%= eperson.getFirstName() %>,</p> --%>
	<p><fmt:message key="jsp.register.registered.thank">
        <fmt:param><%= eperson.getFirstName() %></fmt:param>
    </fmt:message></p>

    <%-- <p>You're now registered to use the DSpace system.  You can subscribe to
    collections to receive e-mail updates about new items.</p> --%>
	<p><fmt:message key="jsp.register.registered.info"/></p>
    
    <%-- <p><a href="<%= request.getContextPath() %>/">Return to DSpace Home</a></p> --%>
	<p><a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.register.general.return-home"/></a></p>
</dspace:layout>
