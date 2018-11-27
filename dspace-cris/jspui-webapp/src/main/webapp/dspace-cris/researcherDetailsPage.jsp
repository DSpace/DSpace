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
<%@ page import="org.dspace.app.cris.integration.RPAuthority"%>
<%@ page import="org.dspace.app.cris.network.NetworkPlugin"%>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.eperson.EPerson" %>

<%@ taglib uri="jdynatags" prefix="dyna"%>
<%@ taglib uri="researchertags" prefix="researcher"%>
<c:set var="root" scope="request"><%=request.getContextPath()%></c:set>
<c:set var="entity" value="${researcher}" scope="request" />
<c:choose>
<c:when test="${param.onlytab}">
<c:forEach items="${tabList}" var="areaIter" varStatus="rowCounter">
	<c:if test="${areaIter.id == tabId}">
	<c:set var="area" scope="request" value="${areaIter}"></c:set>
	<c:set var="currTabIdx" scope="request" value="${rowCounter.count}" />
	<jsp:include page="singleTabDetailsPage.jsp"></jsp:include>
	</c:if>
</c:forEach>
</c:when>
<c:otherwise>
<c:forEach items="${tabList}" var="areaIter" varStatus="rowCounter">
	<c:if test="${areaIter.id == tabId}">
	<c:set var="currTabIdx" scope="request" value="${rowCounter.count}" />
	</c:if>
</c:forEach>
<%

    String subscribe = request.getParameter("subscribe");
	boolean showSubMsg = false;
	boolean showUnSubMsg = false;
	if (subscribe != null && subscribe.equalsIgnoreCase("true"))
	{
	    showSubMsg = true;
	}
	if (subscribe != null && subscribe.equalsIgnoreCase("false"))
	{
	    showUnSubMsg = true;
	}
	
    // Is the logged in user an admin
    Boolean admin = (Boolean)request.getAttribute("isAdmin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    // Can the logged in user edit
    Boolean bEdit = (Boolean)request.getAttribute("canEdit");
    boolean canEdit = (bEdit == null ? false : bEdit.booleanValue());
    
    // Get the current page, minus query string
    String currentPage = UIUtil.getOriginalURL(request);
    int c = currentPage.indexOf( '?' );
    if( c > -1 )
    {
        currentPage = currentPage.substring( 0, c );
    }
    
    boolean networkModuleEnabled = ConfigurationManager.getBooleanProperty(NetworkPlugin.CFG_MODULE,"network.enabled");
    boolean changeStatusAdmin = ConfigurationManager.getBooleanProperty("cris","rp.changestatus.admin");
    boolean claimEnabled = ConfigurationManager.getBooleanProperty("cris","rp.claim.enabled");
%>
<c:set var="admin" scope="request"><%=isAdmin%></c:set>
<c:set var="statusAdmin" scope="request"><%=changeStatusAdmin%></c:set>
<c:set var="claim" scope="request"><%=claimEnabled%></c:set>
<c:set var="req" value="${pageContext.request}" />
<c:set var="baseURL" value="${fn:replace(req.requestURL, fn:substring(req.requestURI, 0, fn:length(req.requestURI)), req.contextPath)}" />
<c:set var="metaprofilename"><c:choose><c:when test="${!empty entity.preferredName.value}">${entity.preferredName.value}</c:when><c:otherwise>${entity.fullName}</c:otherwise></c:choose></c:set>

<c:set var="dspace.cris.navbar" scope="request">

</c:set>
<c:set var="dspace.layout.head" scope="request">		
	<meta property="title" content="${metaprofilename}" />
	<meta property="og:title" content="${metaprofilename}" />	
	<meta property="og:type" content="profile" />
	<meta property="og:url" content="${baseURL}/cris/uuid/${entity.uuid}" />
</c:set>
<c:set var="dspace.layout.head.last" scope="request">
	
	<script type="application/ld+json">
    {
      "@context": "http://schema.org",
      "@type": "Person",
      "name": "${metaprofilename}",
      "url": "${baseURL}/cris/uuid/${entity.uuid}"
    }
    </script>
    
    <script type="text/javascript"><!--

	    var activeTab = function(){
    		var ajaxurlrelations = "<%=request.getContextPath()%>/cris/${specificPartPath}/viewNested.htm";
		   	j('.nestedinfo').each(function(){
				var id = j(this).html();
				j.ajax( {
					url : ajaxurlrelations,
					data : {																			
						"parentID" : ${entity.id},
						"typeNestedID" : id,
						"pageCurrent": j('#nested_'+id+"_pageCurrent").html(),
						"limit": j('#nested_'+id+"_limit").html(),
						"editmode": j('#nested_'+id+"_editmode").html(),
						"totalHit": j('#nested_'+id+"_totalHit").html(),
						"admin": ${admin},
						"externalJSP": j('#nested_'+id+"_externalJSP").html()
					},
					success : function(data) {																										
						j('#viewnested_'+id).html(data);
						var ajaxFunction = function(page){
							j.ajax( {
								url : ajaxurlrelations,
								data : {																			
									"parentID" : ${entity.id},
									"typeNestedID" : id,													
									"pageCurrent": page,
									"limit": j('#nested_'+id+"_limit").html(),
									"editmode": j('#nested_'+id+"_editmode").html(),
									"totalHit": j('#nested_'+id+"_totalHit").html(),
									"admin": ${admin},
									"externalJSP": j('#nested_'+id+"_externalJSP").html()
								},
								success : function(data) {									
									j('#viewnested_'+id).html(data);
									postfunction();
								},
								error : function(data) {
								}
							});		
						};
						var postfunction = function(){
							j('#nested_'+id+'_next').click(
									function() {
								    	ajaxFunction(parseInt(j('#nested_'+id+"_pageCurrent").html())+1);
										
							});
							j('#nested_'+id+'_prev').click(
									function() {
										ajaxFunction(parseInt(j('#nested_'+id+"_pageCurrent").html())-1);
							});
							j('.nested_'+id+'_nextprev').click(
									function(){
										ajaxFunction(j(this).attr('id').substr(('nested_'+id+'_nextprev_').length));
							});
						};
						postfunction();
					},
					error : function(data) {
					}
				});
			});
    	};
    	
		j(document).ready(function()
		{
			j('#claimrp-modal-close').on('click',function(){
				j('#claimrp-modal').hide();
			});
			
			j('#claim-rp').on('click',function(){
				j('#claimrp-validation').val('');
				j('#label-success').remove();
				j('#label-error').remove();
         		j('#claimrp-modal').show();				
			});
			
			j('#claimrp-button').on('click', function(){
				j('#label-success').remove();
				j('#label-error').remove();				
				j.ajax({
					url: "<%= request.getContextPath() %>/json/claimrp",
					data: {
						"rpKey": j('#claimrp-rpkey').val(),
						"mailUser" : j('#claimrp-validation').val()
					},
					success : function(data) {
						send = data.result;
						if(send==-1){
							j('#claimrp-result').append('<span id="label-error" class="label label-warning"><fmt:message key="jsp.cris.detail.claimrp.error-1" /></span>');	
						}else if(send==-2){
							j('#claimrp-result').append('<span id="label-error" class="label label-warning"><fmt:message key="jsp.cris.detail.claimrp.error-2" /></span>');
						}
						else{
							j('#claimrp-result').append('<span id="label-success" class="label label-success"><fmt:message key="jsp.cris.detail.claimrp.success" /></span>');
							
						}
					}
				});
				
			});
			
			j('#self-claim-rp').on('click', function(){
				j('#label-success').remove();
				j('#label-error').remove();				
				j.ajax({
					url: "<%= request.getContextPath() %>/json/claimrp",
					data: {
						"rpKey": j('#self-claimrp-rpkey').val()
					},
					success : function(data) {
						send = data.result;
						if(send==-1){
							j('#selfclaimrp-result').append('<span id="label-error" class="label label-warning"><fmt:message key="jsp.cris.detail.selfclaimrp.error-1"><fmt:param value="${baseURL}/cris/rp/${entity.crisID}"/></fmt:message></span>');	
						}
						else{
							j('#selfclaimrp-result').append('<span id="label-success" class="label label-success"><fmt:message key="jsp.cris.detail.selfclaimrp.success"><fmt:param value="${baseURL}/cris/rp/${entity.crisID}"/></fmt:message></span>');
							
						}
					}
				});
				
			});
			
			j("#tabs").tabs({
				cache: true,
				active: ${currTabIdx-1},
				load: function(event, ui){
					activeTab();
				},
				"activate": function( event, ui ) {
					j("li.ui-tabs-active").toggleClass("ui-tabs-active ui-state-active active");
					if(history!=undefined) {
						history.replaceState(null, null, "${root}/cris/rp/${entity.crisID}/" + j(ui.newTab[0]).data("tabname")+".html");	
					}					
				},
				"beforeActivate": function( event, ui ) {
	   			 j("li.active").toggleClass("active");
				},
		   		"create": function( event, ui ) {
		               j("div.ui-tabs").toggleClass("ui-tabs ui-widget ui-widget-content ui-corner-all tabbable");
		               j("ul.ui-tabs-nav").toggleClass("ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all nav nav-tabs");
		               j("li.ui-tabs-active").toggleClass("ui-state-default ui-corner-top ui-tabs-active ui-state-active active");
		               j("li.ui-state-default").toggleClass("ui-state-default ui-corner-top");
		               j("div.ui-tabs-panel").toggleClass("ui-tabs-panel ui-widget-content ui-corner-bottom tab-content with-padding");
		        }
			});

			activeTab();
		});
		-->
	</script>
    
</c:set>

<dspace:layout title="${metaprofilename}">

<div id="content">
<div class="row">
	<div class="col-lg-12">
		<div class="form-inline">
	         <div class="form-group">
			 	<h1>
				 	<fmt:message key="jsp.layout.detail.title-first" />
				 	<c:choose>
						<c:when test="${!empty entity.preferredName.value}">
							${entity.preferredName.value}
						</c:when>
						<c:otherwise>
							${entity.fullName}
						</c:otherwise>
					</c:choose>
				</h1>
				<%
		    	if (isAdmin) {
				%>
				<fmt:message key="jsp.cris.detail.info.sourceid.none" var="i18nnone" />
				<div class="row cris-record-info">
					<div class="col-sm-6">
					<span class="cris-record-info-sourceid"><b><fmt:message key="jsp.cris.detail.info.sourceid" /></b> ${!empty researcher.sourceID?researcher.sourceID:i18nnone}</span><br/>
					<span class="cris-record-info-sourceref"><b><fmt:message key="jsp.cris.detail.info.sourceref" /></b> ${!empty researcher.sourceRef?researcher.sourceRef:i18nnone}</span>
					</div>
					<div class="col-sm-6">
					<span class="cris-record-info-created"><b><fmt:message key="jsp.cris.detail.info.created" /></b> <fmt:message key="jsp.display-cris.entity.created"><fmt:param value="${researcher.timeStampInfo.timestampCreated.timestamp}" /></fmt:message></span><br/>
					<span class="cris-record-info-updated"><b><fmt:message key="jsp.cris.detail.info.updated" /></b> <fmt:message key="jsp.display-cris.entity.updated"><fmt:param value="${researcher.timeStampInfo.timestampLastModified.timestamp}" /></fmt:message></span>
					</div>
				</div>
			 	<%
		    	}
				%>
				
			 </div>
			 <div class="form-group pull-right" style="margin-top:1.5em;">
				<div class="btn-group">
					<% if(networkModuleEnabled) { %>
					  <a class="btn btn-default" href="<%= request.getContextPath() %>/cris/network/${researcher.crisID}"><i class="fa fa-globe"></i> <fmt:message key="jsp.cris.detail.link.network" /> </a>
					<% } %>
					<a class="btn btn-default"  href="<%= request.getContextPath() %>/cris/stats/rp.html?id=${researcher.uuid}"><i class="fa fa-bar-chart-o"></i> <fmt:message key="jsp.cris.detail.link.statistics" /> </a>
	  				<c:choose>
        					<c:when test="${!subscribed}">
                				<a class="btn btn-default" href="<%= request.getContextPath() %>/cris/tools/subscription/subscribe?uuid=${researcher.uuid}"><i class="fa fa-bell"></i> <fmt:message key="jsp.cris.detail.link.email.alert" /></a>
        					</c:when>
        					<c:otherwise>
                				<a class="btn btn-default" href="<%= request.getContextPath() %>/cris/tools/subscription/unsubscribe?uuid=${researcher.uuid}"><i class="fa fa-stop"></i> <fmt:message key="jsp.cris.detail.link.email.alert.remove" /> </a>
        					</c:otherwise>
					</c:choose>
	  				<a class="btn btn-default" href="<%= request.getContextPath() %>/open-search?query=author_authority:${authority}&amp;format=rss"><i class="fa fa-rss"></i> <fmt:message key="jsp.cris.detail.link.rssfeed" /></a>
				</div>
				<c:if test="${(researcher_page_menu || canEdit) && !empty researcher}">
				<div class="btn-group">
						<c:if test="${!empty addModeType && addModeType=='display'}">
							<a class="btn btn-default" href="<%= request.getContextPath() %>/cris/tools/rp/editDynamicData.htm?id=${researcher.id}&anagraficaId=${researcher.dynamicField.id}<c:if test='${!empty tabIdForRedirect}'>&tabId=${tabIdForRedirect}</c:if>"><i class="fa fa-edit"></i> <fmt:message key="jsp.layout.navbar-hku.staff-mode.edit.primary-data"/></a>
						 </c:if>
						<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown">
		    				<i class="fa fa-cog"></i> <i class="fa fa-caret-down"></i>
		  				</button>
						<ul class="dropdown-menu" role="menu">
							<c:if test="${!empty addModeType && addModeType=='display'}">
						    <li>
								<a href="<%= request.getContextPath() %>/cris/tools/rp/editDynamicData.htm?id=${researcher.id}&anagraficaId=${researcher.dynamicField.id}<c:if test='${!empty tabIdForRedirect}'>&tabId=${tabIdForRedirect}</c:if>"><i class="fa fa-pencil-square-o"></i> <fmt:message key="jsp.layout.navbar-hku.staff-mode.edit.primary-data"/></a>
							</li>
							<c:if test="${researcher_page_menu && !empty researcher}">
							<li>
								<a href="${root}/cris/uuid/${researcher.uuid}/relMgmt/publications"><i class="fa fa-book"></i> <fmt:message key="jsp.layout.navbar-hku.staff-mode.manage-publication"/></a>								
							</li>
							<li>
								<a href="${root}/cris/uuid/${researcher.uuid}/relMgmt/projects"><i class="fa fa-book"></i> <fmt:message key="jsp.layout.navbar-hku.staff-mode.manage-project"/></a>								
							</li>							
							</c:if>
							</c:if>
							<c:if test="${admin}">				
								<li>
									<a href="${root}/cris/tools/rp/rebindItemsToRP.htm?id=${researcher.id}"><i class="fa fa-search"></i> <fmt:message key="jsp.layout.navbar-hku.staff-mode.bind.items"/></a>
								</li>
							</c:if>
							<li>
								<a href="${root}/cris/tools/rp/rebindItemsToRP.htm?id=${researcher.id}&operation=list"><i class="fa fa-search"></i> <fmt:message key="jsp.authority-claim.choice.list.items"/></a>
							</li>							
						</ul>
					</div> 
					
<%-- 					<div class="btn-group">
						<a class="btn btn-default" href="${root}/cris/uuid/${researcher.uuid}/relMgmt/publications"><i class="fa fa-book"></i> <fmt:message key="jsp.layout.navbar-hku.staff-mode.manage-publication"/></a>
					</div> --%>
				</c:if>

				
				<c:if test="${claim && !researcher_page_menu && empty researcher.epersonID && !userHasRP}" >
				<div class="btn-group">				
				<c:choose>				
					<c:when test="${selfClaimRP}">
						<span id="self-claim-rp" class="btn btn-primary"><i class="fa fa-user"></i>&nbsp;<fmt:message key="jsp.cris.detail.info.claimrp"/></span>
					</c:when>
					<c:when test="${!selfClaimRP && (empty researcher.email.value && empty anagraficaObject.anagrafica4view['orcid'])}">
						<a class="btn btn-primary" href="<%= request.getContextPath() %>/feedback?claimProfile=${researcher.crisID}"><i class="fa fa-user"></i>&nbsp;<fmt:message key="jsp.cris.detail.info.claimrp"/></a>
					</c:when>
					<c:when test="${!selfClaimRP && isLoggedIn && empty anagraficaObject.anagrafica4view['orcid']}">
						<a class="btn btn-primary" href="<%= request.getContextPath() %>/feedback?claimProfile=${researcher.crisID}"><i class="fa fa-user"></i>&nbsp;<fmt:message key="jsp.cris.detail.info.claimrp"/></a>
					</c:when>										
					<c:otherwise>
						<span id="claim-rp" class="btn btn-primary"><i class="fa fa-user"></i>&nbsp;<fmt:message key="jsp.cris.detail.info.claimrp"/></span>							
					</c:otherwise>
				</c:choose>
				</div>
				</c:if>
			 </div>
		</div>
	</div>
</div>
	<c:if test="${(!entity.status && !statusAdmin) or (!entity.status && admin)}">
		<p class="warning">
			<fmt:message
				key="jsp.layout.hku.detail.researcher-disabled" /><a				
				href="<%= request.getContextPath() %>/cris/tools/rp/editDynamicData.htm?id=${researcher.id}&anagraficaId=${researcher.dynamicField.id}<c:if test='${!empty tabIdForRedirect}'>&tabId=${tabIdForRedirect}</c:if>"><fmt:message
				key="jsp.layout.hku.detail.researcher-disabled.fixit" /></a>
		</p>
	</c:if>
	
	
	<c:if test="${!empty infoPendingImpRecord && researcher_page_menu}">
		<c:forEach var="entry" items="${infoPendingImpRecord}">
			<c:if test="${entry.value>0}">
    	    <p class="warning pending">
    	    	<c:choose>				
					<c:when test="${admin}">
						<a href="<%=request.getContextPath()%>/tools/importrecord?crisid=${researcher.crisID}&sourceref=${entry.key}"><fmt:message key="jsp.cris.detail.imprecord.result-match.${entry.key}"><fmt:param>${entry.value}</fmt:param></fmt:message></a>
	    	    	</c:when>
	    	    	<c:otherwise>
						<a href="<%=request.getContextPath()%>/tools/importrecord?sourceref=${entry.key}"><fmt:message key="jsp.cris.detail.imprecord.result-match.${entry.key}"><fmt:param>${entry.value}</fmt:param></fmt:message></a>    	    	
	    	    	</c:otherwise>
    	    	</c:choose>
    	    </p>
    	  	</c:if>
		</c:forEach>
	</c:if>

	<c:if test="${pendingItems > 0 && publicationSelfClaimRP}">
		<p class="warning pending">
			<fmt:message
				key="jsp.layout.hku.detail.researcher-pending-items">
				<fmt:param>${pendingItems}</fmt:param>
			</fmt:message> <fmt:message
				key="jsp.layout.hku.detail.researcher-goto-pending-items">
                <fmt:param><%=request.getContextPath()%>/tools/authority?authority=<%=RPAuthority.RP_AUTHORITY_NAME%>&key=${authority_key}</fmt:param>
			</fmt:message>
		</p>	
	</c:if>
	
	<h4 id="selfclaimrp-result"></h4>
	<input type="hidden" value="${requestScope.authority}" id="self-claimrp-rpkey"/>
	
	<c:if test="${not empty messages}">
	<div class="message" id="successMessages">
		<c:forEach var="msg" items="${messages}">
				<div id="authority-message" class="alert alert-info">${msg}</div>
		</c:forEach>
	</div>
	<c:remove var="messages" scope="session" />
	</c:if>
	
		<div id="researcher">
			<jsp:include page="commonDetailsPage.jsp"></jsp:include>
		</div>
</div>

<div id="claimrp-modal" class="modal" tabindex="-1" role="dialog" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
    	<div class="modal-header">
    		<%-- <button type="button" class="close" data-target="claimrp-modal" data-dismiss="modal" aria-hidden="true">
    			<span aria-hidden="true">&times;</span><span class="sr-only">Close</span></button>--%>
			<h4 class="modal-title"><fmt:message key="jsp.cris.detail.claimrp.title" /></h4>
		</div>
		<div class="modal-body">
            <c:if test="${!empty researcher.email.value && !isLoggedIn}">
			<p><fmt:message key="jsp.cris.detail.claimrp.text1" /></p>
			<p><fmt:message key="jsp.cris.detail.claimrp.title.emails" />
		 	<c:set var="indexAt" value="${fn:indexOf(researcher.email.value,'@')}" />
			<c:set var="len" value="${fn:length(researcher.email.value)}" />
              	<%-- <c:forEach var="chr" items="${researcher.email.value}" varStatus="i">--%>
              	<c:forEach begin="0" end="${len}"  var="i">
                        	<c:choose>
                                <c:when test="${i le 3}">
                                         <c:out  value="${fn:substring(entity.email.value, i, i + 1)}" />
                                </c:when>
                                <c:when test="${i eq indexAt }">
                                        <c:out  value="${fn:substring(entity.email.value, i, i + 1)}" />
                                </c:when>
                                <c:when test="${i ge (len - 4)}">
                                        <c:out  value="${fn:substring(entity.email.value, i, i + 1)}" />
                                </c:when>
                                <c:otherwise>
                                        <c:out value="*" />
                                </c:otherwise>
                        </c:choose>
                        
                </c:forEach>
			</p>
   			<div class="input-group">
	      		<input type="text" class="form-control" id="claimrp-validation" />
      			<input type="hidden" value="${requestScope.authority}" id="claimrp-rpkey"/>
      			<span class="input-group-btn">
	        		<span href="" class="btn btn-success" id="claimrp-button"><fmt:message key="jsp.cris.detail.claimrp.button.text" /></span>
      			</span>
    		</div>
     		<h4 id="claimrp-result"></h4>
            </c:if>
	     	<c:if test="${!empty anagraficaObject.anagrafica4view['orcid']}">
     		<c:if test="${!empty researcher.email.value && !isLoggedIn}"><hr /></c:if>
		     	<div class="col-md-12">
		     		<h4><fmt:message key="jsp.cris.detail.claimrp.orcid"/></h4>
		     		      <a href="<%= request.getContextPath() %>/oauth-login">
      						<button class="btn btn-default">ORCID Login 
						      	<img src="<%= request.getContextPath() %>/image/orcid_64x64.png" title="ORCID Authentication">
      						</button>
      					</a>
	     		</div>
     		</c:if>
     		<c:if test="${!empty researcher.email.value || !empty anagraficaObject.anagrafica4view['orcid']}"><hr /></c:if>
     		<div>
     		<h4><fmt:message key="jsp.cris.detail.claimrp.send.email"/></h4>
     		<div class="col-md-12">
     			<fmt:message key="jsp.cris.detail.claimrp.feedback.form"/><a class="btn btn-primary" href="<%= request.getContextPath() %>/feedback?claimProfile=${researcher.crisID}"><i class="fa fa-envelope-o"></i>&nbsp;</a>
     		</div>
     </div>
      <div class="modal-footer">
         <button type="button" class="btn btn-default" data-dismiss="modal" id="claimrp-modal-close">Close</button>
      </div>
     
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->

</dspace:layout>
</c:otherwise>
</c:choose>
