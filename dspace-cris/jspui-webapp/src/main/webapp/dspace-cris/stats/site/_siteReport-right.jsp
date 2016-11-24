<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<c:set var="link">${contextPath}/cris/stats/site.html?handle=${data.object.handle}&stats_from_date=${data.stats_from_date}&stats_to_date=${data.stats_to_date}</c:set>
<c:set var="subscribeLink">${contextPath}/cris/tools/stats/subscription/subscribe?uid=${data.object.handle}&amp;type=${data.object.type}</c:set>
<c:set var="rssLink">${contextPath}/cris/stats/rss/</c:set>
<c:set var="rssImgLink">${contextPath}/image/stats/rss-</c:set>
<c:set var="normalImgLink">${contextPath}/image/stats/stats-normal.jpg</c:set>
<c:set var="hotImgLink">${contextPath}/image/stats/stats-hot.jpg</c:set>
<c:set var="oldsubscription">
<c:if test="${dailysubscribed}">&amp;freq=1</c:if>
<c:if test="${weeklysubscribed}">&amp;freq=7</c:if>
<c:if test="${monthlysubscribed}">&amp;freq=30</c:if>
</c:set>
 <div style="margin-top:2.3em;" class="form-group">
	<div class="col-md-12">
		<div>
		<ul class="nav nav-tabs">
			<li class="<c:if test="${type ne 'upload' && type ne 'community' && type ne 'collection' && type ne 'bitstream'}">active</c:if>"><a class="ui-tabs-anchor" href="${link}&amp;type=item"><fmt:message key="view.stats-site.top.item.page.title" /></a></li>
			<li class="<c:if test="${type eq 'bitstream'}">active</c:if>"><a class="ui-tabs-anchor" href="${link}&amp;type=bitstream"><fmt:message key="view.stats-site.top.bitstream.page.title" /></a></li>
			<c:if test="${data.seeUpload}">			
				<li class="<c:if test="${type eq 'upload'}">active</c:if>"><a class="ui-tabs-anchor" href="${link}&amp;type=upload"><fmt:message key="view.stats-site.upload.page.title" /></a></li>
			</c:if>
<%-- 			<li class="<c:if test="${type eq 'community'}">active</c:if>"><a class="ui-tabs-anchor" href="${link}&amp;type=community"><fmt:message key="view.stats-site.top.community.page.title" /></a></li>
			<li class="<c:if test="${type eq 'collection'}">active</c:if>"><a class="ui-tabs-anchor" href="${link}&amp;type=collection"><fmt:message key="view.stats-site.top.collection.page.title" /></a></li> --%>		
		</ul>

<div class="clearfix">&nbsp;</div>