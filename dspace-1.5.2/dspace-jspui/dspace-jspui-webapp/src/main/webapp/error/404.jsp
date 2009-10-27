<%--
  - 404.jsp
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
  - Friendly 404 error message page
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page isErrorPage="true" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.core.Context" %>

<%
    Context context = null;

	try
	{
		context = UIUtil.obtainContext(request);
%>

<dspace:layout titlekey="jsp.error.404.title">

    <%-- <h1>Error: Document Not Found</h1> --%>
    <h1><fmt:message key="jsp.error.404.title"/></h1>
    <%-- <p>The document you are trying to access has not been found on the server.</p> --%>
    <p><fmt:message key="jsp.error.404.text1"/></p>
    <ul>
        <%-- <li><p>If you got here by following a link or bookmark provided by someone
        else, the link may be incorrect or you mistyped the link.  Please check
        the link and try again.  If you still get this error, then try going
        to the <a href="<%= request.getContextPath() %>/">DSpace home page</a>
        and looking for what you want from there.</p></li> --%>
		<li><p><fmt:message key="jsp.error.404.text2">
            <fmt:param><%= request.getContextPath() %>/</fmt:param>
        </fmt:message></p></li>
        <%-- <li><p>If you got to this error by clicking in a link on the DSpace site,
        please let us know so we can fix it!</p></li> --%>
		<li><p><fmt:message key="jsp.error.404.text3"/></p></li>
    </ul>

    <dspace:include page="/components/contact-info.jsp" />

    <p align="center">
        <%-- <a href="<%= request.getContextPath() %>/">Go to the DSpace home page</a> --%>
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>

</dspace:layout>
<%
	}
	finally
	{
	    if (context != null && context.isValid())
	        context.abort();
	}
%>