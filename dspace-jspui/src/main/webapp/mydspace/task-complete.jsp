<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Task complete message page
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<dspace:layout style="submission" locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.mydspace.task-complete.title">

<%-- <h1>Thank You</h1> --%>
<h1><fmt:message key="jsp.mydspace.task-complete.title"/></h1>

    <%-- <p>The task is complete, and notification has been sent to the appropriate people.</p> --%>
    <p><fmt:message key="jsp.mydspace.task-complete.text1"/></p>
 
    <%-- <p align="center"><a href="<%= request.getContextPath() %>/mydspace">Return to My DSpace </a></p> --%>
    <p align="center"><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.mydspace.general.returnto-mydspace"/> </a></p>
 
</dspace:layout>
