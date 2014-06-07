<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>

<c:set var="dspace.layout.head" scope="request">
	<link href="<%=request.getContextPath() %>/css/misctable.css" type="text/css" rel="stylesheet" />
</c:set>
<c:set var="dspace.cris.navbar" scope="request">
	<c:set var="classcurrent" scope="page">1</c:set>
	<%@ include file="/dspace-cris/_subscription-right.jsp" %>
</c:set>
<dspace:layout titlekey="jsp.statistics.title-subscription-list">

<div id="content">
<div class="title"><h1><fmt:message key="jsp.statistics.title-subscription-list" /></h1></div>

<div class="richeditor">
<div class="top"></div>
<div class="container">
	<%@ include file="/dspace-cris/stats/_subscribeList.jsp" %>
</div>
<div class="bottom"></div>
</div>
<div class="clear"></div>
</div>

</dspace:layout>
