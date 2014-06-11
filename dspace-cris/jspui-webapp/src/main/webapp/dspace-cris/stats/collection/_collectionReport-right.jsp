<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<c:set var="link">${contextPath}/cris/stats/collection.html?handle=${data.object.handle}</c:set>
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
<hr />
<div class="stats-tab<c:if test="${type ne 'item' && type ne 'bitstream'}"> stats-tab-current </c:if>"><a href="${link}"><fmt:message key="view.stats-collection.selectedObject.page.title" /></a></div>
<div class="stats-tab<c:if test="${type eq 'item'}"> stats-tab-current </c:if>"><a href="${link}&amp;type=item"><fmt:message key="view.stats-collection.top.item.page.title" /></a></div>
<div class="stats-tab<c:if test="${type eq 'bitstream'}"> stats-tab-current </c:if>"><a href="${link}&amp;type=bitstream"><fmt:message key="view.stats-collection.top.bitstream.page.title" /></a></div>
<hr />
<div class="box">
<div class="box-label"><fmt:message key="view.stats.subscribe.statistics.label" /></div>
<div class="box-content">
<ul>
	<li>
<c:choose>
	<c:when test="${!dailysubscribed}">
		<a href="${subscribeLink}&amp;freq=1${oldsubscription}" title="Subscribe statistics email update"><img alt="Email alert" src="${normalImgLink}" /> Daily</a>
	</c:when>
	<c:otherwise>
		<a href="${subscribeLink}${fn:replace(oldsubscription,'&amp;freq=1','')}" title="Unsubscribe statistics email update"><img alt="Email alert" src="${hotImgLink}" /> Daily</a>
	</c:otherwise>
</c:choose>
	</li>
	<li>
<c:choose>
	<c:when test="${!weeklysubscribed}">
		<a href="${subscribeLink}&amp;freq=7${oldsubscription}" title="Subscribe statistics email update">
			<img alt="Email alert" src="${normalImgLink}" /> Weekly
		</a>
	</c:when>
	<c:otherwise>
		<a href="${subscribeLink}${fn:replace(oldsubscription,'&amp;freq=7','')}" title="Unsubscribe statistics email update">
			<img alt="Email alert" src="${hotImgLink}" /> Weekly
		</a>
	</c:otherwise>
</c:choose>
	</li>
	<li>
<c:choose>
	<c:when test="${!monthlysubscribed}">
		<a href="${subscribeLink}&amp;freq=30${oldsubscription}" title="Subscribe statistics email update">
			<img alt="Email alert" src="${normalImgLink}" /> Monthly
		</a>
	</c:when>
	<c:otherwise>
		<a href="${subscribeLink}${fn:replace(oldsubscription,'&amp;freq=30','')}" title="Unsubscribe statistics email update">
			<img alt="Email alert" src="${hotImgLink}" /> Monthly
		</a>
	</c:otherwise>
</c:choose>
	</li>
</ul>
</div>
</div>
<div class="box">
<div class="box-label"><fmt:message key="view.stats.subscribe.rss.label" /></div>
<div class="box-content">
<ul>
	<li><a href="${rssLink}daily?uid=${data.object.handle}&amp;type=${data.object.type}" title="Subscribe to RSS statistics update"><img alt="RSS" src="${rssImgLink}daily.jpg" /> Daily</a></li>
	<li><a href="${rssLink}weekly?uid=${data.object.handle}&amp;type=${data.object.type}" title="Subscribe to RSS statistics update"><img alt="RSS" src="${rssImgLink}weekly.jpg" /> Weekly</a></li>
	<li><a href="${rssLink}monthly?uid=${data.object.handle}&amp;type=${data.object.type}" title="Subscribe to RSS statistics update"><img alt="RSS" src="${rssImgLink}monthly.jpg" /> Monthly</a></li>
</ul>
</div>
</div>
