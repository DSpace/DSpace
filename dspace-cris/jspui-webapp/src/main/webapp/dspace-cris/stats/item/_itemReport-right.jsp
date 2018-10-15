<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<c:set var="link">${contextPath}/cris/stats/item.html?handle=${data.object.handle}</c:set>
<c:set var="subscribeLink">${contextPath}/cris/tools/stats/subscription/subscribe?uid=${data.object.handle}&amp;type=${fn:escapeXml(data.object.type)}</c:set>
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
	<ul class="nav nav-tabs">
				<c:if test="${type ne 'bitstream'}">
					<c:set var="activeBitstream"></c:set><li class="active"><a href="${link}&amp;type=item"><fmt:message key="view.stats.tab.statistics.view" /></a></li>
					<c:set var="activeProdotto">active</c:set><li><a href="${link}&amp;type=bitstream"><fmt:message key="view.stats.tab.statistics.download" /></a></li>
				</c:if>
				<c:if test="${type eq 'bitstream'}">
					<c:set var="activeBitstream">active</c:set><li><a href="${link}&amp;type=item"><fmt:message key="view.stats.tab.statistics.view" /></a></li>
					<c:set var="activeProdotto"></c:set><li class="active"><a href="${link}&amp;type=bitstream"><fmt:message key="view.stats.tab.statistics.download" /></a></li>
				</c:if>
	</ul>
	<div class="titlestats tab-content with-padding">		
		<div class="btn-group pull-right">
				<a href="#" class="btn btn-default" data-toggle="dropdown"><fmt:message key="view.stats.subscribe.statistics.label" /> <span  class="fa fa-caret-down"></span ></a>

			<ul role="menu" class="dropdown-menu">
			    <li>
					<c:choose>
						<c:when test="${!dailysubscribed}">
							<a href="${subscribeLink}&amp;freq=1${oldsubscription}" title="Subscribe statistics email update"><fmt:message key="view.stats.subscribe.email.daily" /></a>
						</c:when>
						<c:otherwise>
							<a href="${subscribeLink}${fn:replace(oldsubscription,'&amp;freq=1','')}" title="Unsubscribe statistics email update"><fmt:message key="view.stats.unsubscribe.email.daily" /></a>
						</c:otherwise>
					</c:choose>
				</li>
				<li>
					<c:choose>
						<c:when test="${!weeklysubscribed}">
							<a href="${subscribeLink}&amp;freq=7${oldsubscription}" title="Subscribe statistics email update">
								<fmt:message key="view.stats.subscribe.email.weekly" />
							</a>
						</c:when>
						<c:otherwise>
							<a href="${subscribeLink}${fn:replace(oldsubscription,'&amp;freq=7','')}" title="Unsubscribe statistics email update">
								<fmt:message key="view.stats.unsubscribe.email.weekly" />
							</a>
						</c:otherwise>
					</c:choose>
				</li>
				<li>
					<c:choose>
						<c:when test="${!monthlysubscribed}">
							<a href="${subscribeLink}&amp;freq=30${oldsubscription}" title="Subscribe statistics email update">
								<fmt:message key="view.stats.subscribe.email.monthly" />
							</a>
						</c:when>
						<c:otherwise>
							<a href="${subscribeLink}${fn:replace(oldsubscription,'&amp;freq=30','')}" title="Unsubscribe statistics email update">
								<fmt:message key="view.stats.unsubscribe.email.monthly" />
							</a>
						</c:otherwise>
					</c:choose>
				</li>
			</ul>
		</div>
			<div class="btn-group pull-right">
			<a href="#" class="btn btn-default" data-toggle="dropdown"><fmt:message key="view.stats.subscribe.rss.label" /> <span  class="fa fa-caret-down"></span ></a>
			<ul role="menu" class="dropdown-menu">
				<li><a href="${rssLink}daily?uid=${data.object.handle}&amp;type=${fn:escapeXml(data.object.type)}" title="Subscribe to RSS statistics update"><fmt:message key="view.stats.subscribe.rss.daily" /></a></li>
				<li><a href="${rssLink}weekly?uid=${data.object.handle}&amp;type=${fn:escapeXml(data.object.type)}" title="Subscribe to RSS statistics update"><fmt:message key="view.stats.subscribe.rss.weekly" /></a></li>
				<li><a href="${rssLink}monthly?uid=${data.object.handle}&amp;type=${fn:escapeXml(data.object.type)}" title="Subscribe to RSS statistics update"><fmt:message key="view.stats.subscribe.rss.monthly" /></a></li>
			</ul>
			</div>
			<div class="clearfix">&nbsp;</div>
			 
	