<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<c:set var="link"><%=request.getContextPath() %>/cris/stats/${data.object.publicPath}.html?id=${data.object.uuid}</c:set>
<c:set var="subscribeLink"><%=request.getContextPath() %>/cris/tools/stats/subscription/subscribe?uid=${data.object.uuid}&amp;type=${data.object.type}</c:set>
<c:set var="rssLink"><%=request.getContextPath() %>/cris/stats/rss/</c:set>
<c:set var="rssImgLink"><%=request.getContextPath() %>/image/stats/rss-</c:set>
<c:set var="normalImgLink"><%=request.getContextPath() %>/image/stats/stats-normal.jpg</c:set>
<c:set var="hotImgLink"><%=request.getContextPath() %>/image/stats/stats-hot.jpg</c:set>

<c:set var="oldsubscription">
<c:if test="${dailysubscribed}">&amp;freq=1</c:if>
<c:if test="${weeklysubscribed}">&amp;freq=7</c:if>
<c:if test="${monthlysubscribed}">&amp;freq=30</c:if>
</c:set>
<hr />

<c:forEach items="${data.rightMenu}" var="menu">
	<div class="stats-tab<c:if test="${menu.current}"> stats-tab-current </c:if>"><a href="${link}&type=${menu.type}&mode=${menu.mode}"><fmt:message key="view.stats-crisStatistics.menu.link.${menu.type}.${menu.mode}.${data.object.type}" /></a></div>
</c:forEach>

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
	<li><a href="${rssLink}daily?uid=${data.object.uuid}&amp;type=${data.object.type}" title="Subscribe to RSS statistics update"><img alt="RSS" src="${rssImgLink}daily.jpg" /> Daily</a></li>
	<li><a href="${rssLink}weekly?uid=${data.object.uuid}&amp;type=${data.object.type}" title="Subscribe to RSS statistics update"><img alt="RSS" src="${rssImgLink}weekly.jpg" /> Weekly</a></li>
	<li><a href="${rssLink}monthly?uid=${data.object.uuid}&amp;type=${data.object.type}" title="Subscribe to RSS statistics update"><img alt="RSS" src="${rssImgLink}monthly.jpg" /> Monthly</a></li>
</ul>
</div>
</div>
<hr />