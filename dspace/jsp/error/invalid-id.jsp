<%--
  - invalid-id.jsp
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
  - Page representing an invalid ID error
  -
  - Attributes:
  -    bad.id   - Optional.  The ID that is invalid.
  -    bad.type - Optional.  The type of the ID (or the type the system thought
  -               is was!) from org.dspace.core.Constants.
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page isErrorPage="true" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.core.Constants" %>

<%
    String badID = (String) request.getAttribute("bad.id");
    Integer type = (Integer) request.getAttribute("bad.type");

    // Make sure badID isn't null
    if (badID == null)
    {
        badID = "";
    }

    // Get text for the type
    //String typeString = "object";
   // if (type != null)
   // {
   //     typeString = Constants.typeText[type.intValue()].toLowerCase();
   // }


    String typeString = "constants.object";
    if (type != null)
    {
        typeString = "constants.type" + type.intValue();
    }


%>

<dspace:layout locbar="off" titlekey="jsp.error.invalid-id.title">
    <%-- <h1>Invalid Identifier</h1> --%>
    <h1><fmt:message key="jsp.error.invalid-id.title"/></h1>
    <%-- <p>The identifier <%= badID %> does not correspond to a valid
    reasons:</p> --%>
	<p><fmt:message key="jsp.error.invalid-id.text1">
        <fmt:param><%= badID %></fmt:param>
        <fmt:param><%= typeString %></fmt:param>
    </fmt:message></p>

    <ul>
        <%-- <li>The URL of the current page is incorrect - if you followed a link
        from outside of DSpace it may be mistyped or corrupt.</li> --%>
        <li><fmt:message key="jsp.error.invalid-id.list1"/></li>
        <%-- <li>You entered an invalid ID into a form - please try again.</li> --%>
        <li><fmt:message key="jsp.error.invalid-id.list2"/></li>
    </ul>
    
    <%-- <p>If you're having problems, or you expected the ID to work, feel free to
    contact the site administrators.</p> --%>
    <p><fmt:message key="jsp.error.invalid-id.text2"/></p>

    <dspace:include page="/components/contact-info.jsp" />

    <p align="center">
        <%-- <a href="<%= request.getContextPath() %>/">Go to the DSpace home page</a> --%>
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>
	
</dspace:layout>
