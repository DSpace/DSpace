<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@page import="java.net.URLEncoder"%>
<%@page import="java.util.Map"%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="org.dspace.app.cris.model.ResearcherPage"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="researchertags" prefix="researcher"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>

<%
	
	ResearcherPage researchertarget = (ResearcherPage) request.getAttribute("researchertarget");
	String authoritytarget = (String) request.getAttribute("authoritytarget");
	String authority = (String) request.getAttribute("authority");
	Map<String,Integer> relations = (Map<String,Integer>) request.getAttribute("relations");
	String depth = (String) request.getAttribute("depth");
	String typo = (String) request.getAttribute("typo");
	String fullname = (String) request.getAttribute("fullname");
	
%>


<c:set value="${researchertarget.dynamicField}" var="anagraficaObject"></c:set>
<c:set var="depthOld" value="${depth}"/>



<div id="innerprofiletarget">



	<c:choose>		
			<c:when test="${depthOld==1 && typo=='dept'}">		
					
					<div class="target-separator"><fmt:message key="network.profilefragment.memberofdept"/></div>
					
			</c:when>
			<c:otherwise>
					<div class="target-separator"><fmt:message key="network.profilefragment.targetseparator.and"/></div>				
			</c:otherwise>
	</c:choose>

	<div id="target-rp">

			<div class="rp-label"><fmt:message
								key="jsp.network.label.title.targetprofile" /></div>
					<div class="rp-header">
						<div class="rp-image">
						
										<a target="_blank" href="<%=request.getContextPath()%>/cris/rp/${authoritytarget}">
											<img title="A preview ${authoritytarget} picture"
												src="researcherimage/${authoritytarget}"
												alt="${authoritytarget} picture" name="picture" id="picture" onError="this.onerror=null;this.src='<%=request.getContextPath() %>/image/cris/photo_not_available.png'" /> </a>
						
					</div>
					<div class="rp-content">
							<div class="rp-name"><a target="_blank" href="<%= request.getContextPath() %>/cris/rp/${authoritytarget}">
							<c:if test="${fn:length(researchertarget.anagrafica4view['honorific'])>0 && researchertarget.anagrafica4view['honorific'][0].visibility==1}">
								researchertarget.anagrafica4view['honorific'][0].value
							</c:if>
							${researchertarget.fullName}</a></div>
							<c:forEach
							items="${researchertarget.anagrafica4view['title']}" var="title" varStatus="counter">
							<c:if test="${title.visibility==1 && counter.count==1}">
								<div class="rp-title"><strong> ${title.value}</strong></div>
							</c:if>
							</c:forEach>							
							<div class="rp-dept rp-dept-main">

<c:set value="false" var="deptvisibility"></c:set>
<c:forEach items="${researchertarget.orgUnit}" var="deptvisibilityitem">
        <c:if test="${deptvisibilityitem.visibility==1}">
                <c:set value="true" var="deptvisibility"></c:set>
        </c:if>
</c:forEach>
<c:if test="${!empty researchertarget.orgUnit && deptvisibility==true}">													
						<span><fmt:message
								key="jsp.layout.hku.detail.researcher.department" />
						</span>
						<ul>
<c:forEach var="dept" items="${researchertarget.orgUnit}" varStatus="counter">
<c:if test="${dept.visibility==1}">
							<li>
							
							<c:url var="deptSearch" value="/cris/ou/"/>
							<a target="_blank" href="${deptSearch}${dept.authority}">${dept.value}</a>        											
							<%--a target="_blank" href="<%=request.getContextPath()%>/dnetwork/graph?dept="><span class="icon-network"><img src='../image/wheel-icon2.jpg' alt="<fmt:message key='jsp.network.label.link.network.dept'/>" title="<fmt:message key='jsp.network.label.link.network.dept'/>"/></span></a>
							<a target="_blank" href="<%=request.getContextPath()%>/network/${authoritytarget}"><span class="icon-network"><img src='../image/wheel-icon1.jpg' alt="<fmt:message key='jsp.layout.hku.network.researcher.link'><fmt:param value='<%= researchertarget.getFullName()%>'/></fmt:message>" title="<fmt:message key='jsp.layout.hku.network.researcher.link'><fmt:param value='<%= researchertarget.getFullName()%>'/></fmt:message>"/></span></a--%>
						</li>
</c:if>
</c:forEach>
						</ul>
						
					</c:if>
					
							</div>
						
						</div>
					</div>
				
		</div>

		<c:choose>		
			<c:when test="${typo=='dept'}">		

			</c:when>
			<c:otherwise>
			<div class="target-separator"><fmt:message key="network.profilefragment.targetseparator.share"/></div>

			<div id="target-common">
					<c:choose>
						<c:when test="${depthOld==1 && typo=='dept'}">
						
						</c:when>
						<c:otherwise>
							<div style="font-size: 1.2em;" class="rp-label"><fmt:message key="network.profilefragment.title.targetcommon"/></div>
<c:choose>
	<c:when test="${fn:length(relations) == 0}">
								<div style="text-align: center;"><fmt:message key="network.nothing.in.common.yet" /></div>
	</c:when>
	<c:otherwise>
										<c:forEach var="relation" items="${relations}">
									<ul>

											<li><a class="relationspan"
												id="relation_${relation.key}"><fmt:message
														key="network.profilefragment.number.${relation.key}" />
													${relation.value}</a>
											</li>
									</ul>
										</c:forEach>
	</c:otherwise>
</c:choose>
						</c:otherwise>
					</c:choose>
			</div>

		</c:otherwise>
		</c:choose>


<script type="text/javascript">
	


	
	j(".relationspan")
			.click(
					function() {						
								
						
						j("#log").dialog("open");
						Log.write("Loading...");
											
						var parameterId = this.id;
						var servletpathcaller = "<%= request.getServletPath() %>";
						var ajaxurlrelations = "<%= request.getContextPath() %>/networkdatarelations/${authority}";
						j.ajax( {
							url : ajaxurlrelations,
							data : {
								"servletpathcaller" : servletpathcaller,
								"relation" : parameterId,
								"with" : "${authoritytarget}"
							},
							success : function(data) {
								j("#log").dialog("close");
								j('#relationfragment').modal("show");
								j("#relationfragmentcontenttitle").html("${fullname} / ${researchertarget.fullName}");
								j('#relationfragmentcontent').html(data);

							},
							error : function(data) {
								j("#log").dialog("close");
								j('#relationfragment').modal("hide");
								j("#log").dialog("open");
								Log.write(data.statusText);
								
							}
						});

					});
</script>
