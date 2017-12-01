<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Renders a page containing a statistical summary of the repository usage
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
    String navbar = (String) request.getAttribute("navbar");
%>
<dspace:layout style="submission" navbar="<%=  navbar %>" titlekey="jsp.statistics.no-report.title">

<p><fmt:message key="jsp.statistics.no-report.info1"/></p>

</dspace:layout>
