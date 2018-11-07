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
<%@ taglib uri="researchertags" prefix="researcher"%>
<%@ page import="java.util.Locale"%>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>

<%
Locale sessionLocale = UIUtil.getSessionLocale(request);
String currLocale = null;
if (sessionLocale != null) {
	currLocale = sessionLocale.toString();
}

%>
<c:set var="root"><%=request.getContextPath()%></c:set>
<div class="panel-group" id="${holder.shortName}">
	<div class="panel panel-default">
    	<div class="panel-heading">
    		<h4 class="panel-title">
        		<a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
          			${holder.title} 
        		</a></h4>
    	</div>
		<div id="collapseOne${holder.shortName}" class="panel-collapse collapse in">
			<div class="panel-body">
				<div class="col-md-12">	
					<div class="panel panel-default">
						<div class="panel-heading">
							<h3 class="panel-title">
								<fmt:message
									key="jsp.orcid.custom.box.label.preferences.pushmode.title" />
							</h3>
						</div>
						<div class="panel-body">
							<div class="container">
								<div class="alert alert-info" role="alert">
									<fmt:message
										key="jsp.orcid.custom.box.label.preferences.pushmode" />
								</div>
								<div class="col-md-12">
									<c:forEach
										items="${propertiesDefinitionsInHolder[holder.shortName]}"
										var="tipologiaDaVisualizzareNoI18n">
										<c:set var="tipologiaDaVisualizzare" value="${researcher:getPropertyDefinitionI18N(tipologiaDaVisualizzareNoI18n,currLocale)}" />
										<c:if
											test="${tipologiaDaVisualizzare.shortName eq 'orcid-push-manual'}">
											<dyna:edit tipologia="${tipologiaDaVisualizzare.object}"
												disabled="${disabled}"
												propertyPath="anagraficadto.anagraficaProperties[${tipologiaDaVisualizzare.shortName}]"
												ajaxValidation="validateAnagraficaProperties"
												hideLabel="${hideLabel}" validationParams="${parameters}"
												visibility="${visibility}" lock="true" />
										</c:if>
									</c:forEach>
								</div>
							</div>
						</div>
					</div>
				</div>
			<div class="clearfix">&nbsp;</div>
			<div class="dynaClear">&nbsp;</div>
            <div class="dynaClear">&nbsp;</div>
            <div class="dynaClear">&nbsp;</div>
			<div class="dynaField"></div>								
					
					<div class="col-md-4"><div class="panel panel-default">
  						<div class="panel-heading">
    						<h3 class="panel-title"><fmt:message key="jsp.orcid.custom.box.label.preferences.publications"/></h3>
  						</div>
  						<div class="panel-body">
    						<div class="container">
    							<div class="label label-info"><fmt:message key="jsp.orcid.custom.box.label.preferences.publications.tips"/></div>
    							<div class="clearfix"></div>
								<c:forEach
									items="${propertiesDefinitionsInHolder[holder.shortName]}"
									var="tipologiaDaVisualizzareNoI18n">			
									<c:set var="tipologiaDaVisualizzare" value="${researcher:getPropertyDefinitionI18N(tipologiaDaVisualizzareNoI18n,currLocale)}" />						
									<c:if test="${tipologiaDaVisualizzare.shortName eq 'orcid-publications-prefs'}">
										<dyna:edit tipologia="${tipologiaDaVisualizzare.object}" disabled="${disabled}"
											propertyPath="anagraficadto.anagraficaProperties[${tipologiaDaVisualizzare.shortName}]"
											ajaxValidation="validateAnagraficaProperties" hideLabel="${hideLabel}"
											validationParams="${parameters}" visibility="${visibility}" lock="true"/>																
									</c:if>
								</c:forEach>
							</div>
						</div>   
					</div></div>

					<div class="col-md-4"><div class="panel panel-default">
  						<div class="panel-heading">
    						<h3 class="panel-title"><fmt:message key="jsp.orcid.custom.box.label.preferences.grant"/></h3>
  						</div>
  						<div class="panel-body">
    						<div class="container">
    							<div class="label label-info"><fmt:message key="jsp.orcid.custom.box.label.preferences.projects.tips"/></div>
    							<div class="clearfix"></div>
								<c:forEach
									items="${propertiesDefinitionsInHolder[holder.shortName]}"
									var="tipologiaDaVisualizzareNoI18n">
									<c:set var="tipologiaDaVisualizzare" value="${researcher:getPropertyDefinitionI18N(tipologiaDaVisualizzareNoI18n,currLocale)}" />
									<c:if test="${tipologiaDaVisualizzare.shortName eq 'orcid-projects-prefs'}">
										<dyna:edit tipologia="${tipologiaDaVisualizzare.object}" disabled="${disabled}"
											propertyPath="anagraficadto.anagraficaProperties[${tipologiaDaVisualizzare.shortName}]"
											ajaxValidation="validateAnagraficaProperties" hideLabel="${hideLabel}"
											validationParams="${parameters}" visibility="${visibility}" lock="true"/>																
									</c:if>
								</c:forEach>
								  </div>  
    						</div>
						</div>   
					</div>


					<div class="col-md-4"><div class="panel panel-default">
  						<div class="panel-heading">
    						<h3 class="panel-title"><fmt:message key="jsp.orcid.custom.box.label.preferences.profile"/></h3>
  						</div>
  						<div class="panel-body">
    						<div class="container">
    							<div class="label label-info"><fmt:message key="jsp.orcid.custom.box.label.preferences.profile.tips"/></div>
    							<div class="clearfix"></div>		
								<c:forEach
									items="${propertiesDefinitionsInHolder[holder.shortName]}"
									var="tipologiaDaVisualizzareNoI18n">
									<c:set var="tipologiaDaVisualizzare" value="${researcher:getPropertyDefinitionI18N(tipologiaDaVisualizzareNoI18n,currLocale)}" />
									<c:if test="${fn:startsWith(tipologiaDaVisualizzare.shortName, 'orcid-profile-pref-')}">
										<dyna:edit tipologia="${tipologiaDaVisualizzare.object}" disabled="${disabled}"
											propertyPath="anagraficadto.anagraficaProperties[${tipologiaDaVisualizzare.shortName}]"
											ajaxValidation="validateAnagraficaProperties" hideLabel="${hideLabel}"
											validationParams="${parameters}" visibility="${visibility}" lock="true"/>																
									</c:if>
		
								</c:forEach>
								</div>
							</div>
		
   						 </div>							
						</div>   
					</div></div></div>
</div>