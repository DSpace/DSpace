<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="java.util.Map" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<script>
<!--
	j(function () {
		j('[data-toggle="testtool"]').tooltip();
	})
	
	
	function validateBeforeSubmit(){
		var input = $('#itemid_list').val();
		var res = input.split(",");
		for (i=0; i<res.length; i++) { 
			if (isNaN(res[i].trim()) || res[i].trim() == ''){
				$( "#dialog-confirm" ).modal("show");
				return false;
			}
		}
		if (res.length < 2){
			$( "#dialog-confirm" ).modal("show");
			return false;			
		}
		document.formItemIdList.submit();
	}
	
-->
</script>

	<div class="modal fade" id="dialog-confirm" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
	  <div class="modal-dialog">
	    <div class="modal-content">
	      <div class="modal-header">
	        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
	        <h4 class="modal-title"><fmt:message key="jsp.tools.deduplicate.itemid.description"/></h4>
	      </div>
	      <div class="modal-body with-padding">
	      	<div class="clearfix">
	      		&nbsp;
	      	</div>
	        <p><fmt:message key="jsp.tools.deduplication.itemsubmit.alert"/></p>
	      </div>
	      <div class="modal-footer">
	        <button type="button" class="btn btn-default" data-dismiss="modal" id="button-confirm"><fmt:message key="jsp.tools.deduplication.confirm.alert.ok"/></button>
	      </div>
	    </div>
	  </div>
	</div>

<dspace:layout style="submission" navbar="admin" locbar="link" titlekey="jsp.dspace-admin.deduplication">

<h1><fmt:message key="jsp.dspace-admin.deduplication"/></h1>

<div class="well block">
	  	<ul class="nav nav-tabs" role="tablist">
	  		
	  		<c:if test="${!empty duplicatessignatureall}">
    			<li role="presentation" class="active"><a href="#home" aria-controls="home" role="tab" data-toggle="tab"><fmt:message key='jsp.tools.deduplicate.tab.title.all'/></a></li>
    		</c:if>
    		<li role="presentation" <c:if test="${empty duplicatessignatureall}">class="active"</c:if>><a href="#compare" aria-controls="compare" role="tab" data-toggle="tab"><fmt:message key="jsp.tools.deduplicate.tab.title.compare"/></a></li>
    		<%-- <li role="presentation"><a href="#search" aria-controls="search" role="tab" data-toggle="tab"><fmt:message key="jsp.tools.deduplicate.tab.title.search"/></a></li> --%>
    		<c:if test="${!empty duplicatessignatureonlyreported}">
    			<li role="presentation"><a target="_blank" href="<%= request.getContextPath() %>/tools/duplicate?submitcheck=0&scope=2&start=0&rows=10" aria-controls="onlyreported" role="tab" data-toggle="tab"><fmt:message key='jsp.tools.deduplicate.tab.title.onlyreported'/></a></li>
    		</c:if>
  		</ul>
  		
		<div class="tab-content">
			<c:if test="${!empty duplicatessignatureall}">
			<div role="tabpanel" class="tabsignature tab-pane fade in active" id="home">

			<div class="row">
						
			<c:forEach var="entry" items="${duplicatessignatureall}">
			<c:set var="key"><c:out value="${entry.key}"/></c:set>
			<c:set var="value"><c:out value="${entry.value}"/></c:set>
			<c:set var="signatureNameKey">
				jsp.tools.deduplicate.dd.${key}
			</c:set>
			<c:set var="signatureIconKey">
				jsp.tools.deduplicate.dd.${key}.icon
			</c:set>
					<div
						class="col-md-3 col-sm-6 col-xs-12 box-${key}">
						<div class="media ${key}">
							<div class="media-left">
								<fmt:message key="${signatureIconKey}" />
							</div>
							<div class="media-body text-center">
								<h4 class="media-heading" data-toggle="testtool" title="<fmt:message key="jsp.tools.deduplicate.description.${key}"/>">
									<fmt:message key="${signatureNameKey}" />
								</h4>
								<span id="signature-counter-${key}" class="metric-counter">
								<c:choose><c:when test="${!empty duplicatessignatureonlywf[key]}">
									<a data-toggle="testtool" title="<fmt:message key="jsp.tools.deduplicate.only.popup.wf"/>" target="_blank" href="<%= request.getContextPath() %>/tools/duplicate?submitcheck=-1&signatureType=${key}&rule=2&start=0&rows=10"><fmt:formatNumber
														value="${!empty duplicatessignatureonlywf[key]?duplicatessignatureonlywf[key]:0}" type="NUMBER"
															maxFractionDigits="0" /></a>
									</c:when><c:otherwise><span data-toggle="testtool" title="<fmt:message key="jsp.tools.deduplicate.only.popup.wf"/>">0</span></c:otherwise></c:choose>
								</span>
							</div>

							<div class="row">
								<div data-toggle="testtool" title="<fmt:message key="jsp.tools.deduplicate.only.popup.ws"/>" class="col-xs-6 text-left">
									<fmt:message key="jsp.tools.deduplicate.only.ws" />
									<br/>
									<c:choose><c:when test="${!empty duplicatessignatureonlyws[key]}">									
										<a target="_blank" href="<%= request.getContextPath() %>/tools/duplicate?submitcheck=-1&signatureType=${key}&rule=1&start=0&rows=10"><fmt:formatNumber value="${!empty duplicatessignatureonlyws[key]?duplicatessignatureonlyws[key]:0}" type="NUMBER" maxFractionDigits="0" /></a>
									</c:when><c:otherwise>0</c:otherwise></c:choose>
								</div>
								<div data-toggle="testtool" title="<fmt:message key="jsp.tools.deduplicate.only.popup.admin"/>" class="col-xs-6 text-right">
									<fmt:message key="jsp.tools.deduplicate.only.admin" />
									<br/>									
										<a target="_blank" href="<%= request.getContextPath() %>/tools/duplicate?submitcheck=0&signatureType=${key}&start=0&rows=10"><fmt:formatNumber
										value="${value}" type="NUMBER"
										maxFractionDigits="0" /></a>										
								</div>
							</div>
							
						</div>
					</div>
				</c:forEach>
			</div>
		</div>
		</c:if>

		<div role="tabpanel" class="tab-pane fade <c:if test="${empty duplicatessignatureall}">in active</c:if>" id="compare">
		
			<div id="compareItemId">
				<form id="form-submission-itemid_list" name="formItemIdList" role="form" method="post" action="<%= request.getContextPath() %>/tools/duplicate" class="form-horizontal form-bordered">
					<input type="hidden" value="0" name="scope" />
					<input type="hidden" value="submitcheck" name="submitcheck" />
					<div class="panel-body">
						<div class="form-group">
							<label class="col-sm-2 control-label"><fmt:message key="jsp.tools.deduplicate.descriptionById"/></label>
							<div class="col-sm-10">
								<textarea id="itemid_list" name="itemid_list" rows="5" cols="5" class="form-control submission-lookup-itemid_list"></textarea>
							</div>
						</div>
						<div class="form-actions text-right">
	                   		<input type="button" id="lookup_itemid_list" onclick="validateBeforeSubmit()" class="btn btn-primary" value="<fmt:message key="jsp.dspace-admin.deduplication.alt-button" />">
						</div>
				    </div>
				</form>
			</div>
		</div>

<!--	<div class="tab-pane fade" id="search">
		
				<div class="search">
					...
				</div>
		
		</div> -->
		
		<c:if test="${!empty duplicatessignatureonlyreported}">
		<div role="tabpanel" class="tabsignature tab-pane fade" id="onlyreported">
			<div class="row">

			</div>
		</div>
		</c:if>
		
</div>

</dspace:layout>
