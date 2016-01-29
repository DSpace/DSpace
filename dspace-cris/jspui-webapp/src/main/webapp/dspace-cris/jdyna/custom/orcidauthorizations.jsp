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
<c:set var="showgrantedidicon" value="false"/>
<c:set var="showgrantedeyesicon" value="false"/>
<c:set var="showgrantedrepeaticon" value="false"/>
<c:set var="showgrantedclouduploadicon" value="false"/>
<c:set var="showmissedidicon" value="false"/>
<c:set var="showmissedeyesicon" value="false"/>
<c:set var="showmissedrepeaticon" value="false"/>
<c:set var="showmissedclouduploadicon" value="false"/>
<c:set var="showauthenticate" value="false"/>
<c:set var="showorcidprofilereadlimited" value="false"/>
<c:set var="showorcidbioupdate" value="false"/>
<c:set var="showorcidworkscreate" value="false"/>
<c:set var="showorcidworksupdate" value="false"/>
<c:set var="showfundingcreate" value="false"/>
<c:set var="showfundingupdate" value="false"/>
<script type="text/javascript">
<!--
j(document).ready(function() {	
	j(".bottomTooltip").popover({
		placement: "bottom",
		trigger: "hover"
	});
});
//-->
</script>
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
			<div class="dynaClear">&nbsp;</div>
            <div class="dynaClear">&nbsp;</div>
            <div class="dynaClear">&nbsp;</div>
			<div class="dynaField"></div>								
			<c:choose>
				<c:when test="${!empty anagraficaObject.anagrafica4view['orcid']}">
					<c:forEach items="${propertiesDefinitionsInHolder[holder.shortName]}" var="tipologiaDaVisualizzare" varStatus="status">
						<c:choose>
						<c:when test="${!empty anagraficaObject.anagrafica4view['system-orcid-token-authenticate']}">
								<c:set var="showgrantedidicon" value="true"/>
								<c:set var="showauthenticate" value="true"/>
						</c:when>
						<c:otherwise>
								<c:set var="showmissedidicon" value="true"/>
						</c:otherwise>
						</c:choose>
						<c:choose>
						<c:when test="${!empty anagraficaObject.anagrafica4view['system-orcid-token-orcid-profile-read-limited']}">
							<c:set var="showgrantedeyesicon" value="true"/>
							<c:set var="showorcidprofilereadlimited" value="true"/>
						</c:when>
						<c:otherwise>
							<c:set var="showmissedeyesicon" value="true"/>
						</c:otherwise>
						</c:choose>
						<c:choose>
						<c:when test="${!empty anagraficaObject.anagrafica4view['system-orcid-token-orcid-bio-update']}">
							<c:set var="showgrantedrepeaticon" value="true"/>
							<c:set var="showorcidbioupdate" value="true"/>
						</c:when>
						<c:otherwise>
							<c:set var="showmissedrepeaticon" value="true"/>
						</c:otherwise>
						</c:choose>
						<c:choose>
						<c:when test="${!empty anagraficaObject.anagrafica4view['system-orcid-token-orcid-works-create']}">
							<c:set var="showgrantedclouduploadicon" value="true"/>
							<c:set var="showorcidworkscreate" value="true"/>
						</c:when>
						<c:otherwise>
							<c:set var="showmissedclouduploadicon" value="true"/>
						</c:otherwise>
						</c:choose>
						<c:choose>
						<c:when test="${!empty anagraficaObject.anagrafica4view['system-orcid-token-orcid-works-update']}">
							<c:set var="showgrantedrepeaticon" value="true"/>
							<c:set var="showorcidworksupdate" value="true"/>
						</c:when>
						<c:otherwise>
							<c:set var="showmissedrepeaticon" value="true"/>
						</c:otherwise>
						</c:choose>						
						<c:choose>
						<c:when test="${!empty anagraficaObject.anagrafica4view['system-orcid-token-funding-update']}">
							<c:set var="showgrantedrepeaticon" value="true"/>
							<c:set var="showfundingupdate" value="true"/>
						</c:when>
						<c:otherwise>
							<c:set var="showmissedrepeaticon" value="true"/>
						</c:otherwise>
						</c:choose>								
						<c:choose>
						<c:when test="${!empty anagraficaObject.anagrafica4view['system-orcid-token-funding-create']}">
							<c:set var="showgrantedclouduploadicon" value="true"/>
							<c:set var="showfundingcreate" value="true"/>
						</c:when>
						<c:otherwise>
							<c:set var="showmissedclouduploadicon" value="true"/>
						</c:otherwise>
						</c:choose>							
						<c:if test="${tipologiaDaVisualizzare.shortName eq 'orcid'}">
							<dyna:display tipologia="${tipologiaDaVisualizzare.real}" hideLabel="false" values="${anagraficaObject.anagrafica4view[tipologiaDaVisualizzare.shortName]}" />
							<div class="dynaClear">&nbsp;</div>
						</c:if>
					</c:forEach>
					<c:choose>
					<c:when test="${showgrantedidicon eq false or showgrantedeyesicon eq false or showgrantedrepeaticon eq false or showgrantedrepeaticon eq false}">
					<div class="col-md-5"><div class="panel panel-default">
  						<div class="panel-heading">
    						<h3 class="panel-title"><fmt:message key="jsp.orcid.custom.box.label.grantedauthorization"/></h3>
  						</div>
  						<div class="panel-body">
							<div class="alert alert-warning"><fmt:message key="jsp.orcid.custom.box.label.grantedauthorization.empty"/></div>    						
						</div>   
					</div></div>
					</c:when>
					<c:otherwise>
					
					<div class="col-md-5"><div class="panel panel-default">
  						<div class="panel-heading">
    						<h3 class="panel-title"><fmt:message key="jsp.orcid.custom.box.label.grantedauthorization"/></h3>
  						</div>
  						<div class="panel-body">
    						<div class="container">	
								<div class="col-md-12">
									<ul class="oauth-icons">
											<c:if test="${showgrantedidicon eq true}">
												<li><span class="mini-orcid-icon oauth-bullet"></span></li>
											</c:if>
											<c:if test="${showgrantedeyesicon eq true}">
												<li><span class="mini-icon glyphicon glyphicon-eye-open green"></span></li>
											</c:if>
											<c:if test="${showgrantedrepeaticon eq true}">
												<li><span class="mini-icon glyphicon glyphicon-repeat green"></span></li>
											</c:if>	 
											<c:if test="${showgrantedclouduploadicon eq true}">
												<li><span class="mini-icon glyphicon glyphicon-cloud-upload green"></span></li>
											</c:if>
									</ul>
								</div>
							</div>
							<div class="container">	
								<div class="col-md-12">
									<ul class="oauth-scopes">			
											<c:if test="${showauthenticate eq true}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showauthenticate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showauthenticate"/></span></li>
											</c:if>
											<c:if test="${showorcidprofilereadlimited eq true}">
												<li><span class="bottompopover" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidprofilereadlimited.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidprofilereadlimited"/></span></li>
											</c:if>											
											<c:if test="${showorcidbioupdate eq true}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidbioupdate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidbioupdate"/></span></li>
											</c:if>
											<c:if test="${showorcidworkscreate eq true}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidworkscreate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidworkscreate"/></span></li>
											</c:if>
											<c:if test="${showorcidworksupdate eq true}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidworksupdate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidworksupdate"/></span></li>
											</c:if>
											<c:if test="${showfundingupdate eq true}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showfundingupdate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showfundingupdate"/></span></li>
											</c:if>											
											<c:if test="${showfundingcreate eq true}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showfundingcreate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showfundingcreate"/></span></li>
											</c:if>
									</ul>
								</div>
							</div>
						</div>   
					</div></div>
					
					</c:otherwise>
					</c:choose>
					<c:choose>
					<c:when test="${showmissedidicon eq true or showmissedeyesicon eq true or showmissedrepeaticon eq true or showmissedrepeaticon eq true}">											
						<div class="col-md-2">
							<div class="row">
							<a href="<%= request.getContextPath() %>/oauth-login">
		      					<div class="bottomTooltip col-md-offset-3" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.button.refresh.auth"/>">
			      					<button class="btn btn-default">		      						
			      						<img src="<%= request.getContextPath() %>/image/orcid_64x64.png" title="ORCID Authentication"/>
							      	</button>
						      	</div>
					      	</a>
							</div>
						</div>			        
					<div class="col-md-5"><div class="panel panel-default">
  						<div class="panel-heading">
						    <h3 class="panel-title"><fmt:message key="jsp.orcid.custom.box.label.missedauthorization"/></h3>
  						</div>
  						<div class="panel-body">
						    <div class="container">	
								<div class="col-md-12">
									<ul class="oauth-icons">
											<c:if test="${showmissedidicon eq true}">
												<li><span class="mini-orcid-icon oauth-bullet"></span></li>
											</c:if>
											<c:if test="${showmissedeyesicon eq true}">
												<li><span class="mini-icon glyphicon glyphicon-eye-open green"></span></li>
											</c:if>
											<c:if test="${showmissedrepeaticon eq true}">
												<li><span class="mini-icon glyphicon glyphicon-repeat green"></span></li>
											</c:if>	 
											<c:if test="${showmissedrepeaticon eq true}">
												<li><span class="mini-icon glyphicon glyphicon-cloud-upload green"></span></li>
											</c:if>
									</ul>
								</div>
  							</div>
							<div class="container">	
								<div class="col-md-12">
									<ul class="oauth-scopes">			
											<c:if test="${showauthenticate eq false}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showauthenticate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showauthenticate"/></span></li>
											</c:if>
											<c:if test="${showorcidprofilereadlimited eq false}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidprofilereadlimited.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidprofilereadlimited"/></span></li>
											</c:if>											
											<c:if test="${showorcidbioupdate eq false}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidbioupdate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidbioupdate"/></span></li>
											</c:if>
											<c:if test="${showorcidworkscreate eq false}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidworkscreate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidworkscreate"/></span></li>
											</c:if>
											<c:if test="${showorcidworksupdate eq false}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidworksupdate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showorcidworksupdate"/></span></li>
											</c:if>
											<c:if test="${showfundingupdate eq false}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showfundingupdate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showfundingupdate"/></span></li>
											</c:if>											
											<c:if test="${showfundingcreate eq false}">
												<li><span class="bottomTooltip" data-toggle="popover" data-container="body" data-content="<fmt:message key="jsp.orcid.custom.box.label.authorization.showfundingcreate.tooltip"/>"><fmt:message key="jsp.orcid.custom.box.label.authorization.showfundingcreate"/></span></li>
											</c:if>
									</ul>
								</div>
							</div>   
						</div>
					</div></div>									
					</c:when>
					<c:otherwise>
						<div class="col-md-2"><div class="row"><a class="col-md-offset-4" href="http://orcid.org"><img src="<%= request.getContextPath() %>/image/orcid_64x64.png" title="ORCID Website"></a></div>
						</div>					
						<div class="col-md-5"><div class="panel panel-default">
  						<div class="panel-heading">
						    <h3 class="panel-title"><fmt:message key="jsp.orcid.custom.box.label.missedauthorization"/></h3>
  						</div>
  						<div class="panel-body">
  							<div class="alert alert-success"><fmt:message key="jsp.orcid.custom.box.label.missedauthorization.empty"/></div>
  						</div>
  						</div>
  						</div>					
					</c:otherwise>
					</c:choose>
				</c:when>
				<c:otherwise>
					<div class="dynaField">
						<div class="dynaFieldValue">
							<a class="label label-warning"><fmt:message key="jsp.orcid.custom.box.label.notyet"/></a>		
						</div>
					</div>				
					<div class="dynaClear">&nbsp;</div>
		            <div class="dynaClear">&nbsp;</div>
		            <div class="dynaClear">&nbsp;</div>
					<div class="dynaField">
		                          
		            </div>
					<div class="dynaClear">
						<div class="dynaField">
							<div class="dynaFieldValue">
								<div class="btn-group" role="group">
								  <a href="<%= request.getContextPath() %>/oauth-login?show-login=false">
			      						<button class="btn btn-default"><fmt:message key="jsp.orcid.custom.box.button.create"/></button>
			      				  </a>								  
								  <span>&nbsp;&nbsp;&nbsp;<img src="<%= request.getContextPath() %>/image/orcid_64x64.png" title="ORCID Authentication">&nbsp;&nbsp;&nbsp;</span>
								  <a href="<%= request.getContextPath() %>/oauth-login?show-login=true">
			      						<button class="btn btn-default"><fmt:message key="jsp.orcid.custom.box.button.connect"/></button>
			      				  </a>
								</div>
							</div>
						</div>
					</div>
					
				</c:otherwise>
			</c:choose>

			</div>
		</div>
	</div>
</div>

