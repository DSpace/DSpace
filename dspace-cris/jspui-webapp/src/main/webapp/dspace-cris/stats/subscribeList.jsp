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
<dspace:layout style="submission" locbar="link" titlekey="jsp.statistics.title-subscription-list">

    			<div class="btn-group pull-right">
			    <button type="button" class="btn btn-sm btn-default dropdown-toggle" data-toggle="dropdown">
    				<fmt:message key="jsp.mydspace.subscriptions.button.seealso"/> <span class="fa fa-caret-down"></span>	
  				</button>
				<ul class="dropdown-menu dropdown-menu-right" role="menu">
					<li><a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.layout.navbar-hku.item-subscription"/></a></li>
				</ul>
				</div>


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
