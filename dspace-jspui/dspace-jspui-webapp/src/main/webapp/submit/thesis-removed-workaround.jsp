<%--
  - thesis-removed-workaround.jsp
  -
  - Version: $Revision: 3705 $
  -
  - Date: $Date: 2009-04-11 13:02:24 -0400 (Sat, 11 Apr 2009) $
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
  - Submission removed message - this is displayed when the user has checked
  - the "This is a thesis" option on the first submission page, had their
  - submission removed, used the back button to go back to the submission
  - page, and tried carry on.  Normally this would result in an integrity error
  - since the workspace ID is no longer valid but in this special case we
  - will display a decent message.
  -
  - This page displays a message informing the user that theses are not
  - presently accepted in DSpace, and that their submission has been removed.
  - FIXME: MIT-SPECIFIC
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<dspace:layout titlekey="jsp.submit.thesis-removed-workaround.title">
    <%-- <h1>Submission Stopped: Theses Not Accepted in DSpace</h1> --%>
	<h1><fmt:message key="jsp.submit.thesis-removed-workaround.heading"/></h1>

    <%-- <p>Since DSpace does not accept theses, your submission has been stopped.
    To start submitting something else click below.</p> --%>
	<p><fmt:message key="jsp.submit.thesis-removed-workaround.info"/></p>

    <p align="center"><strong><a href="<%= request.getContextPath() %>/submit">
    <%-- Start a new submission</a></p> --%>
	<fmt:message key="jsp.submit.thesis-removed-workaround.link"/></a></p>

</dspace:layout>
