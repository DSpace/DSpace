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
<c:set var="root"><%=request.getContextPath()%></c:set>
<c:if test="${!empty anagraficaObject.anagrafica4view['orcid']}">
<div class="panel-group" id="${holder.shortName}">
	<div class="panel panel-default">
    	<div class="panel-heading">
    		<h4 class="panel-title">
        		<a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
          			${holder.title} 
        		</a></h4>
    	</div>
		<div id="collapseOne${holder.shortName}" class="panel-collapse collapse<c:if test="${holder.collapsed==false}"> in</c:if>">
			<div class="panel-body">	
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
							<div class="label label-info"><fmt:message key="jsp.orcid.custom.box.label.preferences.publications.details.tips"/></div>
							<div class="clearfix">&nbsp;</div>
    						<c:choose>	
								<c:when test="${!empty anagraficaObject.anagrafica4view['orcid-publications-prefs'] && anagraficaObject.anagrafica4view['orcid-publications-prefs'][0].value.object==1}">
									<span class="label label-success"><fmt:message key="jsp.orcid.custom.box.label.preferences.publications.sendall"/></span>
								</c:when>
								<c:otherwise>
									<c:choose>	
										<c:when test="${!empty anagraficaObject.anagrafica4view['orcid-publications-prefs'] && anagraficaObject.anagrafica4view['orcid-publications-prefs'][0].value.object==2}">
											<span class="label label-success"><fmt:message key="jsp.orcid.custom.box.label.preferences.publications.sendonlyselected"/></span>
										</c:when>
									   <c:when test="${!empty anagraficaObject.anagrafica4view['orcid-publications-prefs'] && anagraficaObject.anagrafica4view['orcid-publications-prefs'][0].value.object==3}">
											<span class="label label-success"><fmt:message key="jsp.orcid.custom.box.label.preferences.publications.sendmy"/></span>
										</c:when>
										<c:otherwise>
											<span class="label label-default"><fmt:message key="jsp.orcid.custom.box.label.preferences.publications.disable"/></span>
										</c:otherwise>
									</c:choose>
								</c:otherwise>
							</c:choose>
							</div>
						</div>   
					</div></div>

					<div class="col-md-4"><div class="panel panel-default">
  						<div class="panel-heading">
    						<h3 class="panel-title"><fmt:message key="jsp.orcid.custom.box.label.preferences.grant"/></h3>
  						</div>
  						<div class="panel-body">
    						<div class="container">
							<div class="label label-info"><fmt:message key="jsp.orcid.custom.box.label.preferences.projects.details.tips"/></div>
							<div class="clearfix">&nbsp;</div>	
    						<c:choose>	
								<c:when test="${!empty anagraficaObject.anagrafica4view['orcid-projects-prefs'] && anagraficaObject.anagrafica4view['orcid-projects-prefs'][0].value.object==1}">
									<span class="label label-success"><fmt:message key="jsp.orcid.custom.box.label.preferences.projects.sendall"/></span>
								</c:when>
								<c:otherwise>
									<c:choose>	
										<c:when test="${!empty anagraficaObject.anagrafica4view['orcid-projects-prefs'] && anagraficaObject.anagrafica4view['orcid-projects-prefs'][0].value.object==2}">
											<span class="label label-success"><fmt:message key="jsp.orcid.custom.box.label.preferences.projects.sendonlyselected"/></span>
										</c:when>
										<c:when test="${!empty anagraficaObject.anagrafica4view['orcid-projects-prefs'] && anagraficaObject.anagrafica4view['orcid-projects-prefs'][0].value.object==3}">
											<span class="label label-success"><fmt:message key="jsp.orcid.custom.box.label.preferences.projects.sendmy"/></span>
										</c:when>
										<c:otherwise>
											<span class="label label-default"><fmt:message key="jsp.orcid.custom.box.label.preferences.projects.disable"/></span>
										</c:otherwise>
									</c:choose>
								</c:otherwise>
							</c:choose>
							</div>
						</div>   
					</div></div>


					<div class="col-md-4"><div class="panel panel-default">
  						<div class="panel-heading">
    						<h3 class="panel-title"><fmt:message key="jsp.orcid.custom.box.label.preferences.profile"/></h3>
  						</div>
  						<div class="panel-body">
    						<div class="container">
									<div class="label label-info"><fmt:message key="jsp.orcid.custom.box.label.preferences.profile.details.tips"/></div>
									<div class="clearfix">&nbsp;</div>
									<c:forEach items="${propertiesDefinitionsInHolder[holder.shortName]}" var="tipologiaDaVisualizzare" varStatus="status">
										<c:if test="${fn:startsWith(tipologiaDaVisualizzare.shortName, 'orcid-profile-pref-')}">
										<c:choose>
											<c:when test="${!empty anagraficaObject.anagrafica4view[tipologiaDaVisualizzare.shortName]}">										
												<dyna:display tipologia="${tipologiaDaVisualizzare.real}" hideLabel="true" values="${anagraficaObject.anagrafica4view[tipologiaDaVisualizzare.shortName]}" />
											</c:when>
											<c:otherwise>
												<div class="label label-default">${tipologiaDaVisualizzare.label}</div>
											</c:otherwise>
										</c:choose>
										</c:if>
									</c:forEach>
							</div>							
					</div></div>
					</div>
				</div>
			</div>
		</div>
	</div>	
</c:if>