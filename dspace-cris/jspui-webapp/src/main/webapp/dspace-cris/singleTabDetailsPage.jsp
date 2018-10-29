<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ page import="java.net.URL"%>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="java.util.Locale"%>
<%@ page import="java.util.List"%>
<%@ page import="java.util.Map"%>
<%@ page import="java.util.HashMap"%>
<%@ page import="it.cilea.osd.jdyna.web.Box"%>

<%@ taglib uri="jdynatags" prefix="dyna"%>
<%@ taglib uri="researchertags" prefix="researcher"%>
<%
    Locale sessionLocale = UIUtil.getSessionLocale(request);
	String currLocale = null;
	if (sessionLocale != null) {
		currLocale = sessionLocale.toString();
	}
    List<Box> boxs = (List<Box>)(request.getAttribute("propertiesHolders"));
    Map<String, Long> mapCount = new HashMap<String, Long>();
   	for(Box box : boxs) {
	 	if(box!=null) {
		 	   String nameBoxCountAttribute = "count" + box.getShortName(); 
		 	   Long objnameBoxCountAttribute = (Long)request.getAttribute(nameBoxCountAttribute);
			   mapCount.put(box.getShortName(), objnameBoxCountAttribute);
 		} 
	}
%>
<c:set var="currLocale"><%=currLocale %></c:set>
	<div id="tab-${area.id}">
		<div class="row">
					<c:forEach items="${propertiesHolders}" var="holder">
					<c:set var="holdershortname" value="${holder.shortName}" scope="page"/>
					<c:set var="extraCSS">
						<c:choose>
							<c:when test="${holder.priority % 10 == 2}">col-md-6</c:when>
							<c:otherwise>col-md-12</c:otherwise>
						</c:choose>
					</c:set>
					
					<% Long counterBoxMap = (Long)mapCount.get(pageContext.getAttribute("holdershortname")); 
						if(counterBoxMap==null) {
					%>
						<c:set
							value="${researcher:isBoxHidden(entity,holder.shortName)}"
							var="invisibleBox"></c:set>
					<% } else {	%>
						<c:set
							value="<%= counterBoxMap>0?false:true %>"
							var="invisibleBox"></c:set>
					<% } %>		
						<c:if test="${invisibleBox==false}">

							<%!public URL fileURL;%>

							<c:set var="urljspcustom"
								value="/dspace-cris/jdyna/custom/${holder.externalJSP}.jsp" scope="request" />
								
							<%
								String filePath = (String)pageContext.getRequest().getAttribute("urljspcustom");

										fileURL = pageContext.getServletContext().getResource(
												filePath);
							%>
							<%
								if (fileURL == null) {
							%>
							<div class="panel-group ${extraCSS}" id="${holder.shortName}">
  									<div class="panel panel-default">
    										<div class="panel-heading">
      												<h4 class="panel-title">
        												<a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
          													<spring:message code="${entity.class.simpleName}.box.${holder.shortName}.label" text="${holder.title}"></spring:message>
        												</a>
      												</h4>
    										</div>
										    <div id="collapseOne${holder.shortName}" class="panel-collapse collapse in">
												<div class="panel-body">
											      <c:set var="hideLabel">${fn:length(propertiesDefinitionsInHolder[holder.shortName]) le 1}</c:set>
													<c:forEach
														items="${propertiesDefinitionsInHolder[holder.shortName]}"
														var="tipologiaDaVisualizzareNoI18n" varStatus="status">
														<c:set var="tipologiaDaVisualizzare" value="${researcher:getPropertyDefinitionI18N(tipologiaDaVisualizzareNoI18n,currLocale)}" />
														<%!public URL fileFieldURL;%>
							
														<c:set var="urljspcustomfield"
															value="/dspace-cris/jdyna/custom/field/${tipologiaDaVisualizzare.shortName}.jsp" scope="request" />
															
														<%
															String fileFieldPath = (String)pageContext.getRequest().getAttribute("urljspcustomfield");
							
																	fileFieldURL = pageContext.getServletContext().getResource(
																			fileFieldPath);
														%>
														<%
															if (fileFieldURL == null) {
														%>
														<c:if
															test="${dyna:instanceOf(tipologiaDaVisualizzare,'it.cilea.osd.jdyna.model.ADecoratorTypeDefinition')}">
															
																<c:set var="totalHit" value="0"/>
																<c:set var="limit" value="5"/>
																<c:set var="offset" value="0"/>											
																<c:set var="pageCurrent" value="0"/>	
																<c:set var="editmode" value="false"/>
																
																<div
																	id="viewnested_${tipologiaDaVisualizzare.real.id}" class="viewnested">
																		<img src="<%=request.getContextPath()%>/image/cris/bar-loader.gif" class="loader" />
																			<fmt:message key="jsp.jdyna.nestedloading" />
																<span class="spandatabind nestedinfo">${tipologiaDaVisualizzare.real.id}</span>
																<span id="nested_${tipologiaDaVisualizzare.real.id}_totalHit" class="spandatabind">0</span>
																<span id="nested_${tipologiaDaVisualizzare.real.id}_limit" class="spandatabind">5</span>
																<span id="nested_${tipologiaDaVisualizzare.real.id}_pageCurrent" class="spandatabind">0</span>
																<span id="nested_${tipologiaDaVisualizzare.real.id}_editmode" class="spandatabind">false</span>
																<span id="nested_${tipologiaDaVisualizzare.real.id}_externalJSP" class="spandatabind">${tipologiaDaVisualizzare.externalJSP}</span>
																</div>
														</c:if>
														<c:if
															test="${dyna:instanceOf(tipologiaDaVisualizzare,'it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition')}">
															<dyna:display tipologia="${tipologiaDaVisualizzare.real}"
																hideLabel="${hideLabel}"
																values="${anagraficaObject.anagrafica4view[tipologiaDaVisualizzare.shortName]}" />
														</c:if>
														<% } else { %>
															<c:set var="tipologiaDaVisualizzare" value="${tipologiaDaVisualizzare}" scope="request" />
															<c:import url="${urljspcustomfield}" />
														<% } %>
													</c:forEach>		
										        </div>
										  </div>
								   </div>
							</div>
							
							
							<%
								} else {
							%>
							<c:set var="extraCSS" value="${extraCSS}" scope="request" />
							<c:set var="holder" value="${holder}" scope="request" />							
							<c:import url="${urljspcustom}" />

							<%
								}
							%>

						</c:if>
					</c:forEach>
			</div>
	</div>
