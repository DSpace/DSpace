<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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
