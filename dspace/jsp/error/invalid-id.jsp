<%--
  - invalid-id.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
    String typeString = "object";
    if (type != null)
    {
        typeString = Constants.typeText[type.intValue()].toLowerCase();
    }
%>

<dspace:layout locbar="off" title="Invalid Identifier">

    <H1>Invalid Identifier</H1>

    <P>The identifier <%= badID %> does not correspond to a valid
    <%= typeString %> in DSpace.  This may be because of one of the following
    reasons:</P>

    <UL>
        <LI>The URL of the current page is incorrect - if you followed a link
        from outside of DSpace it may be mistyped or corrupt.</LI>
        <LI>You entered an invalid ID into a form - please try again.</LI>
    </UL>
    
    <P>If you're having problems, or you expected the ID to work, feel free to
    contact the site administrators.</P>

    <dspace:include page="/components/contact-info.jsp" />

    <P align=center>
        <A HREF="<%= request.getContextPath() %>/">Go to the DSpace home page</A>
    </P>
	

</dspace:layout>
