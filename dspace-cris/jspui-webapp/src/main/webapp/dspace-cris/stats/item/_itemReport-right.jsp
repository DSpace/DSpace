<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<c:set var="link">${contextPath}/cris/stats/item.html?handle=${data.object.handle}</c:set>
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

 <div style="margin-top:1.5em;" class="form-group pull-right">
	<div class="btn-group">
		<c:if test="${type ne 'bitstream'}">
			<a class="btn btn-default" href="${link}&amp;type=bitstream"><fmt:message key="view.stats-item.top.bitstream.page.title" /></a>
		</c:if>
		<c:if test="${type eq 'bitstream'}">
			<a class="btn btn-default" href="${link}"><fmt:message key="view.stats-item.selectedObject.page.title" /></a>
		</c:if>
	</div>
	<div class="btn-group">
			<a href="#" class="btn btn-default" data-toggle="dropdown"><fmt:message key="view.stats.subscribe.statistics.label" /></a>
			<button data-toggle="dropdown" class="btn btn-default dropdown-toggle" type="button">
   				<i class="fa fa-cog"></i> <i class="fa fa-caret-down"></i>
 				</button>
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
		<div class="btn-group">
		<a href="#" class="btn btn-default" data-toggle="dropdown"><fmt:message key="view.stats.subscribe.rss.label" /></a>
		<button data-toggle="dropdown" class="btn btn-default dropdown-toggle" type="button">
  				<i class="fa fa-cog"></i> <i class="fa fa-caret-down"></i>
				</button>
		<ul role="menu" class="dropdown-menu">
			<li><a href="${rssLink}daily?uid=${data.object.handle}&amp;type=${data.object.type}" title="Subscribe to RSS statistics update"><fmt:message key="view.stats.subscribe.rss.daily" /></a></li>
			<li><a href="${rssLink}weekly?uid=${data.object.handle}&amp;type=${data.object.type}" title="Subscribe to RSS statistics update"><fmt:message key="view.stats.subscribe.rss.weekly" /></a></li>
			<li><a href="${rssLink}monthly?uid=${data.object.handle}&amp;type=${data.object.type}" title="Subscribe to RSS statistics update"><fmt:message key="view.stats.subscribe.rss.monthly" /></a></li>
		</ul>
	</div>
 </div>
		 
			 
	