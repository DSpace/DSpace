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
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<dspace:layout style="submission" locbar="link"
               parentlink="/mydspace"
               parenttitlekey="jsp.mydspace"
               titlekey="jsp.statistics.title-subscription-list">

<h1><fmt:message key="jsp.mydspace.subscriptions.title"/>
	<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") +\"#subscribe\" %>"><fmt:message key="jsp.help"/></dspace:popup>
</h1>

<div id="content">
<ul id="tabs" class="nav nav-tabs" data-tabs="tabs">
<li><a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.layout.navbar-hku.item-subscription"/></a></li>
<li class="active"><a href="<%= request.getContextPath() %>/cris/tools/stats/subscription/list.htm"><fmt:message key="jsp.layout.navbar-hku.stat-subscription"/></a></li>
</ul>
<div id="my-tab-content" class="tab-content">
<div class="tab-pane active" id="contentsubscription">

</div>
</div>
<div class="tab-pane" id="statisticssubscription">

	<%@ include file="/dspace-cris/stats/_subscribeList.jsp" %>

</div>
</div>
</div>


</dspace:layout>

