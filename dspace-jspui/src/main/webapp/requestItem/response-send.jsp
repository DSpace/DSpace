<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - response-send received OK acknowledgement
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<style type="text/css">
<!--
.style1 {color: #0000AE}
-->
</style>


<dspace:layout locbar="off" navbar="default" titlekey="jsp.request.item.response-send.title">

    <h1><fmt:message key="jsp.request.item.response-send.info1"/></h1>

    <p><fmt:message key="jsp.request.item.response-send.info2"/> 
	<a href="<%= request.getContextPath() %>/handle/${handle}"><fmt:message key="jsp.request.item.return-item"/></a></p>

</dspace:layout>
