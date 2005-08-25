<%--
  - internal.jsp
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
  - Page representing an integrity error - an inconsistency or error in the
  - data received from the browser
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page isErrorPage="true" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout titlekey="jsp.error.integrity.title">

      <%-- <h1>System Error: Malformed Request</h1> --%>
      <h1><fmt:message key="jsp.error.integrity.heading"/></h1>

    <%-- <p>There was an inconsistency in the data received from your browser.
    This may be due to one of several things:</p> --%>
    <p><fmt:message key="jsp.error.integrity.text1"/></p>
   
    <ul>
        <%-- <li>Sometimes, if you used your browser's "back" button during an operation like a
        submission, clicking on a button may try and do something that's already
        been done, such as commit the submission to the archive.
        Clicking your browsers "reload" or "refresh" button may have similar
        results.</li> --%>
        <li><fmt:message key="jsp.error.integrity.list1"/></li>
        <%-- <li>If you got here by following a link or bookmark provided by someone
        else, the link may be incorrect or you mistyped the link.  Please check
        the link and try again.</li> --%>
        <li><fmt:message key="jsp.error.integrity.list2"/></li>
        <%-- <li>If you have more than one browser window open with DSpace, this can cause
        a similar problem whereby a button clicked in one window may make a button
        click in the other window invalid.</li> --%>
        <li><fmt:message key="jsp.error.integrity.list3"/></li>
        <%-- <li>Of course, you may have uncovered a problem with the system!  All of
        these errors are logged, and we'll be checking them regularly to see
        if there is a problem.</li> --%>
        <li><fmt:message key="jsp.error.integrity.list4"/></li>
    </ul>

    <%-- <p>If the problem persists, please contact the DSpace site administrators:</p> --%>
    <p><fmt:message key="jsp.error.integrity.text2"/></p>

    <dspace:include page="/components/contact-info.jsp" />

    <p align="center">
        <%-- <a href="<%= request.getContextPath() %>/">Go to the DSpace home page</a> --%>
        <a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.general.gohome"/></a>
    </p>

</dspace:layout>
