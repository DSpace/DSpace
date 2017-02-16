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
%>

<c:set value="${researchertarget.dynamicField}" var="anagraficaObject"></c:set>
			
	
			
			
			<div id="relationdivtwice">	
			
			<c:choose>
			<c:when test="${typo=='dept'}">		
				<c:if test="${depth==1}">
					<div class="relationlist">
						<h3 id="spanmemberof"><fmt:message key="network.profilefragment.memberofdept"/></h3>
					</div>				
				</c:if> 
			</c:when>
			<c:otherwise>			
			
			<div class="boxrelationlist">
			<div class="relationlistarrowup"><img src="../image/arrow_relation_up.png"></div>
			<div class="relationlist">
			<ul>
			<c:forEach var="relation" items="${relations}">
			
				<li><a class="relationspantwice" id="relation_${relation.key}"><fmt:message key="network.profilefragment.number.${relation.key}"/> ${relation.value}</a></li>
					
			</c:forEach>
			</ul>
			</div>
			<div class="relationlistarrowdown"><img src="../image/arrow_relation_down.png"></div>
			</div>
			</c:otherwise>
			</c:choose>
			</div>
		
			<div id="profiletargettwiceminimized" class="profile">		
				
						<div class="imagemin">

									<a target="_blank" href="<%=request.getContextPath()%>/cris/rp/${authoritytarget}">
											<img title="A preview ${authoritytarget} picture"
												src="researcherimage/${authoritytarget}"
												alt="${authoritytarget} picture" name="picture" id="picture" onError="this.onerror=null;this.src='<%=request.getContextPath() %>/image/cris/photo_not_available.png'" /> </a>
																			
						</div>
					<div class="otherminimized">
					<span class="header4" id="headermin">
					<c:if test="${fn:length(researchertarget.anagrafica4view['honorific'])>0 && researchertarget.anagrafica4view['honorific'][0].visibility==1}">
						researchertarget.anagrafica4view['honorific'][0].value
					</c:if>
					
						${researchertarget.fullName}</span>
  				<span class="network-degree">
				${depth}
				<c:if test="${depth==1}">
				<sup>st</sup>
				</c:if>
				<c:if test="${depth==2}">
				<sup>nd</sup>
				</c:if>
				<c:if test="${depth==3}">
				<sup>rd</sup>
				</c:if>
				<c:if test="${depth>3}">
				<sup>th</sup>
				</c:if>
				</span>
					<c:if
							test="${researchertarget.translatedName.visibility==1}">
							<br/>
							<span class="header4" id="headermin">${researchertarget.translatedName.value}</span> 
					</c:if>
					<c:if test="${researchertarget.internalRP}"><a target="_blank" href="<%= request.getContextPath() %>/cris/rp/${authoritytarget}"><span id="profileTitle"><img src='../image/profile.png' alt="<fmt:message key='jsp.network.label.profile.title'/>" title="<fmt:message key='jsp.network.label.profile.title'/>"/></span></a></c:if>
					<div id="titlemin">
					
						<c:forEach
							items="${researchertarget.anagrafica4view['title']}" var="title" varStatus="counter">
							<c:if test="${title.visibility==1 && counter.count==1}">
								<div class="ulmin"><strong> ${title.value}</strong></div>
							</c:if>
						</c:forEach>
											
					</div>
					
		
				
				</div>
		
		
					<div id="deptmin">
					
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
													<span> <input type="checkbox" class="deptvisualizationprofiletwice" id="deptvisualizationprofiletwice" value="${researcher:encode(dept.value,'UTF-8')}"/><label
														for="deptvisualizationprofiletwice">See DGraph</label> </span>						
													<%--a target="_blank" href="<%=request.getContextPath()%>/dnetwork/graph?dept="><span class="icon-network"><img src='../image/wheel-icon2.jpg' alt="<fmt:message key='jsp.network.label.link.network.dept'/>" title="<fmt:message key='jsp.network.label.link.network.dept'/>"/></span></a>
													<a target="_blank" href="<%=request.getContextPath()%>/network/${authoritytarget}"><span class="icon-network"><img src='../image/wheel-icon1.jpg' alt="<fmt:message key='jsp.layout.hku.network.researcher.link'><fmt:param value='<%= researchertarget.getFullName()%>'/></fmt:message>" title="<fmt:message key='jsp.layout.hku.network.researcher.link'><fmt:param value='<%= researchertarget.getFullName()%>'/></fmt:message>"/></span></a--%>
												</li>
						</c:if>
						</c:forEach>
					
					</div>
					
							
				<div class="viewnetworkvisualizationtd">			
						
						
						
					<c:if test="${researchertarget.internalRP}"><a href="<%= request.getContextPath() %>/cris/network/${authoritytarget}"><fmt:message	key="jsp.layout.hku.network.researcher.link"><fmt:param>							
						<c:if test="${fn:length(researchertarget.anagrafica4view['honorific'])>0 && researchertarget.anagrafica4view['honorific'][0].visibility==1}">
							${researchertarget.fullName}
						</c:if>
						 ${researchertarget.fullName}</fmt:param></fmt:message></a></c:if>
					
				</div>
		
				</div>
<script type="text/javascript">




j( ".deptvisualizationprofiletwice" ).button({ 
	icons: {
    	primary: "icon-network"
	},
	text: false
});


j(".deptvisualizationprofiletwice").click(function(){

	location.href = "<%=request.getContextPath()%>/dnetwork/graph?dept=" + this.value;
	return true;		
	
});

		
	j(".relationspantwice")
			.click(
					function() {						
					
						
						j("#log").dialog("open");
						Log.write("Loading...");
												
						
						var parameterId = this.id;
						var ajaxurlrelations = "<%= request.getContextPath() %>/networkdatarelations/${authority}"
						j.ajax( {
							url : ajaxurlrelations,
							data : {
								"relation" : parameterId,
								"with" : "${authoritytarget}"
							},
							success : function(data) {
								j("#log").dialog("close");
								j('#relationfragmenttwice').modal("show");	
								j("#relationfragmenttwicecontenttitle").html("${fullname} / ${researchertarget.fullName}");
								j('#relationfragmenttwicecontent').html(data);
								
							},
							error : function(data) {
								j("#log").dialog("close");
								j('#relationfragmenttwice').modal("hide");
								j("#log").dialog("open");
								Log.write(data.statusText);
								
							}
						});

					});
	
</script>