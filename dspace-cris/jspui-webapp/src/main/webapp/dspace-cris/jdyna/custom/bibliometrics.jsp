<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>
<c:set var="dspace.layout.head.last" scope="request">
${dspace.layout.head.last}
<script type="text/javascript">
<!--
jQuery(document).ready(function() {
	jQuery('[data-toggle="tooltip"]').tooltip();
});
--></script>
</c:set>
<div class="panel-group ${extraCSS}" id="${holder.shortName}">
  									<div class="panel panel-default">
    										<div class="panel-heading">
      												<h4 class="panel-title">
        												<a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
          													${holder.title}
        												</a>
      												</h4>
    										</div>
										    <div id="collapseOne${holder.shortName}" class="panel-collapse collapse<c:if test="${holder.collapsed==false}"> in</c:if>">
												<div class="panel-body">
<c:set var="root"><%=request.getContextPath()%></c:set>
<c:set var="metrics" value="${extra['metrics']}" />
<div class="row">
<c:forEach var="metricType" items="${extra['metricTypes']}">
<c:set var="metricNameKey">
	jsp.display-cris.citation.${metricType}
</c:set>
<c:set var="metricIconKey">
	jsp.display-cris.citation.${metricType}.icon
</c:set>
<c:if test="${not empty metrics[metricType].counter and metrics[metricType].counter gt 0}">
	<c:if test="${!empty metrics[metricType].moreLink}">
		<script type="text/javascript">
		j(document).ready(function() {
			var obj = JSON.parse('${metrics[metricType].moreLink}');
			j( "div" ).data( "moreLink", obj );
			j( "#metric-counter-${metricType}" ).wrap(function() {
				  return "<a target='_blank' href='" + j( "div" ).data( "moreLink" ).link + "'></a>";
			}).append(" <i class='fa fa-info-circle'></i>");
		});
		</script>
	</c:if>
<div class="col-sm-6 col-xs-12">
<div class="media ${metricType}">
	<div class="media-left">
		<fmt:message key="${metricIconKey}"/>
	</div>
	<div class="media-body text-center">
		<h4 class="media-heading"><fmt:message key="${metricNameKey}"/>
		<c:if test="${not empty metrics[metricType].rankingLev}">
		<span class="pull-right">
		<fmt:message key="jsp.display-item.citation.top" />
		<span class="metric-ranking arc">
			<span class="circle" data-toggle="tooltip" data-placement="bottom" 
				title="<fmt:message key="jsp.display-cris.citation.${metricType}.ranking.tooltip"><fmt:param><fmt:formatNumber value="${metrics[metricType].rankingLev}" type="NUMBER" maxFractionDigits="0" /></fmt:param></fmt:message>">
				<fmt:formatNumber value="${metrics[metricType].rankingLev}" 
					type="NUMBER" maxFractionDigits="0" />
			</span>
		</span>
		</span>
		</c:if>
		</h4>
		<span id="metric-counter-${metricType}" class="metric-counter">
		<c:choose>		
		<c:when test="${!empty metrics[metricType].formatter.type}">
		<c:choose>
		<c:when test="${!(metrics[metricType].formatter.type eq 'PERCENT')}">
			<fmt:formatNumber value="${metrics[metricType].counter}" 
				type="${metrics[metricType].formatter.type}"
				maxFractionDigits="${metrics[metricType].formatter.maxFractionDigits}"
				minFractionDigits="${metrics[metricType].formatter.minFractionDigits}"/>
		</c:when>
		<c:otherwise>
			<fmt:formatNumber value="${metrics[metricType].counter/100}" 
				type="${metrics[metricType].formatter.type}"
				maxFractionDigits="${metrics[metricType].formatter.maxFractionDigits}"
				minFractionDigits="${metrics[metricType].formatter.minFractionDigits}"/>
		</c:otherwise>
		</c:choose>
		</c:when>		
		<c:otherwise>
			<fmt:formatNumber value="${metrics[metricType].counter}" 
				pattern="${metrics[metricType].formatter.pattern}"/>
		</c:otherwise>
		</c:choose>
	</div>
	<c:if test="${not empty metrics[metricType].last1}">
	<div class="row">
		<div class="col-xs-6 text-left">
			<fmt:message key="jsp.display-item.citation.last1" />
			<br/>
				<c:choose>		
					<c:when test="${!empty metrics[metricType].formatter.type}">
					<c:choose>
						<c:when test="${!(metrics[metricType].formatter.type eq 'PERCENT')}">
							<fmt:formatNumber value="${metrics[metricType].last1}" 
								type="${metrics[metricType].formatter.type}"
								maxFractionDigits="${metrics[metricType].formatter.maxFractionDigits}"
								minFractionDigits="${metrics[metricType].formatter.minFractionDigits}"/>
						</c:when>
						<c:otherwise>
							<fmt:formatNumber value="${metrics[metricType].last1/100}" 
								type="${metrics[metricType].formatter.type}"
								maxFractionDigits="${metrics[metricType].formatter.maxFractionDigits}"
								minFractionDigits="${metrics[metricType].formatter.minFractionDigits}"/>
						</c:otherwise>
					</c:choose>
					</c:when>		
					<c:otherwise>
						<fmt:formatNumber value="${metrics[metricType].last1}" 
							pattern="${metrics[metricType].formatter.pattern}"/>
					</c:otherwise>
				</c:choose>		
		</div>
		<div class="col-xs-6 text-right">
			<fmt:message key="jsp.display-item.citation.last2" />
			<br/>
				<c:choose>		
					<c:when test="${!empty metrics[metricType].formatter.type}">
					<c:choose>
						<c:when test="${!(metrics[metricType].formatter.type eq 'PERCENT')}">
							<fmt:formatNumber value="${metrics[metricType].last2}" 
								type="${metrics[metricType].formatter.type}"
								maxFractionDigits="${metrics[metricType].formatter.maxFractionDigits}"
								minFractionDigits="${metrics[metricType].formatter.minFractionDigits}"/>
						</c:when>
						<c:otherwise>
							<fmt:formatNumber value="${metrics[metricType].last2/100}" 
								type="${metrics[metricType].formatter.type}"
								maxFractionDigits="${metrics[metricType].formatter.maxFractionDigits}"
								minFractionDigits="${metrics[metricType].formatter.minFractionDigits}"/>
						</c:otherwise>
					</c:choose>
					</c:when>		
					<c:otherwise>
						<fmt:formatNumber value="${metrics[metricType].last2}" 
							pattern="${metrics[metricType].formatter.pattern}"/>
					</c:otherwise>
				</c:choose>	
		
		</div>
	</div>
	</c:if>
	<div class="row">
		<div class="col-lg-12 text-center small">
			<fmt:message
				key="jsp.display-cris.citation.time">
				<fmt:param value="${metrics[metricType].time}" />
			</fmt:message>
		</div>
	</div>
	</div>
</div>
</c:if>
</c:forEach>
</div>
										        </div>
										  </div>
								   </div>
							</div>
