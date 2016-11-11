<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page contentType="text/html; charset=UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@ taglib uri="jdynatags" prefix="dyna" %>
<%@ taglib uri="researchertags" prefix="researcher"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@page import="java.net.URL"%>
<%@page import="org.dspace.app.cris.model.jdyna.VisibilityTabConstant"%>
<c:set var="dspace.layout.head.last" scope="request">
	<script type="text/javascript">
	<!--
	jQuery(document).ready(function(){
		
	    jQuery('.visibility').click(function(){
	        if(jQuery(this).attr("value")=="<%= VisibilityTabConstant.POLICY %>"){
	            jQuery("#chooserpolicymetadata").show();
	        }
	        else {
	        	jQuery("#chooserpolicymetadata").hide();
	        	jQuery(".policydropdown").val("");
	        }
	    });
	});
	-->
	</script>
</c:set>
    
<dspace:layout locbar="link" navbar="admin" style="submission"
	titlekey="jsp.dspace-admin.edit-tab">

	<table width="95%">
		<tr>
			<td align="left">
			<h1><fmt:message key="jsp.dspace-admin.edit-tab" /></h1>
			</td>
			<td align="right" class="standard"><a target="_blank"
				href='<%=request.getContextPath()%><%=LocaleSupport.getLocalizedMessage(pageContext,
                                "help.site-admin.rp")%>'><fmt:message
				key="jsp.help" /></a></td>
		</tr>
	</table>


	

	

	<form:form commandName="tab" method="post" id="jdynatab" enctype="multipart/form-data">
		<c:set value="${message}" var="message" scope="request" />
		<c:if test="${!empty message}">
			<div id="authority-message"><fmt:message key="${message}" /></div>
		</c:if>

		<c:if test="${not empty messages}">
			<div class="message" id="successMessages"><c:forEach var="msg"
				items="${messages}">
				<div id="authority-message">${msg}</div>
				</c:forEach>
				</div>
			<c:remove var="messages" scope="session" />
		</c:if>
		
		<%--  first bind on the object itself to display global errors - if available  --%>
		<spring:bind path="tab">
			<c:forEach items="${status.errorMessages}" var="error">
				<span id="errorMessage"><fmt:message
					key="jsp.layout.hku.prefix-error-code" /> ${error}</span>
				<br>
			</c:forEach>
		</spring:bind>
		
		<spring:bind path="tab.*">
		<c:if test="${not empty status.errorMessages}">
			<div class="alert alert-danger"><c:forEach var="error"
				items="${status.errorMessages}">
	               ${error}<br />
			</c:forEach></div>
		</c:if>
		</spring:bind>

		<c:if test="${not empty status.errorMessages}">
		<div class="alert alert-danger"><c:forEach var="error"
			items="${status.errorMessages}">
                 ${error}<br />
		</c:forEach></div>
		</c:if>
		
		<dyna:text propertyPath="tab.shortName"  helpKey="help.jdyna.message.tab.shortname"
			labelKey="jsp.layout.hku.label.shortname" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:text propertyPath="tab.title"  helpKey="help.jdyna.message.tab.title"
			labelKey="jsp.layout.hku.label.title" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:text propertyPath="tab.priority"  helpKey="help.jdyna.message.tab.priority"
				labelKey="jsp.layout.hku.label.priority" size="5" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
	
		<c:set var="VISIBILITYCONSTANTS" value="<%= VisibilityTabConstant.getValues() %>"/>
		<c:if test="${!(specificPartPath eq 'rp')}">
			<c:set var="VISIBILITYCONSTANTS" value="<%= VisibilityTabConstant.getLimitedValues() %>"/>
		</c:if>	
		
		<spring:bind path="visibility">
			<c:set var="inputValue">
				<c:out value="${status.value}" escapeXml="true"></c:out>
			</c:set>
			<c:set var="inputName">
				<c:out value="${status.expression}" escapeXml="false"></c:out>
			</c:set>

			<div class="dynaField"><span class="dynaLabel"><label for="${inputName}"><fmt:message
				key="jsp.layout.hku.label.visibility" /></label></span>

			<div class="dynaFieldValue">
			<c:forEach items="${VISIBILITYCONSTANTS}" var="item" varStatus="count">
				<input ${disabled} id="${inputName}_${count.count}" class="${inputName}" name="${inputName}"
					type="radio" value="${item}"
					<c:if test="${inputValue==item}">checked="checked"</c:if> />
				<fmt:message
					key="jsp.layout.hku.label.visibility.${item}" />

			</c:forEach>
			<input ${disabled} name="_${inputName}" id="_${inputName}"
				value="true" type="hidden" />
			</div>
			</div>
		</spring:bind>

		<c:if test="${tab.visibility!=4}">
			<c:set var="chooservisibilityelement" value="style=\"display: none;\""/>
		</c:if>		
		<div id="chooserpolicymetadata" ${chooservisibilityelement}>
		
		<spring:bind path="authorizedSingle">
			<c:set var="inputValue">
				<c:out value="${status.value}" escapeXml="true"></c:out>
			</c:set>
			<c:set var="inputName">
				<c:out value="${status.expression}" escapeXml="false"></c:out>
			</c:set>

			<div class="dynaField"><span class="dynaLabel"><label for="${inputName}"><fmt:message
				key="jsp.layout.hku.label.authorized.eperson" /></label></span>

			<div class="dynaFieldValue">
				<select multiple="multiple" class="form-control policydropdown" id="${inputName}" name="${inputName}">
					<option value=""><fmt:message key="jsp.layout.hku.label.authorized.eperson.select" /></option>									
					<c:if test="${!empty metadataWithPolicySingle}">
					    <c:forEach var="option" items="${metadataWithPolicySingle}" varStatus="loop">	    	
				            <option value="${option.id}"<c:if test="${!empty inputValue && fn:contains(inputValue, option.id)}"> selected="selected"</c:if>>${option.label}(${option.shortName})</option>
				        </c:forEach>        
				    </c:if>
				</select>
			</div>
			</div>
		</spring:bind>
		
		<spring:bind path="authorizedGroup">
			<c:set var="inputValue">
				<c:out value="${status.value}" escapeXml="true"></c:out>
			</c:set>
			<c:set var="inputName">
				<c:out value="${status.expression}" escapeXml="false"></c:out>
			</c:set>

			<div class="dynaField"><span class="dynaLabel"><label for="${inputName}"><fmt:message
				key="jsp.layout.hku.label.authorized.group" /></label></span>

			<div class="dynaFieldValue">
				<select multiple="multiple" class="form-control policydropdown" id="${inputName}" name="${inputName}">
					<option value=""><fmt:message key="jsp.layout.hku.label.authorized.group.select" /></option>				
					<c:if test="${!empty metadataWithPolicyGroup}">
					    <c:forEach var="option" items="${metadataWithPolicyGroup}" varStatus="loop">	    	
				            <option value="${option.id}"<c:if test="${!empty inputValue && fn:contains(inputValue, option.id)}"> selected="selected"</c:if>>${option.label} - Shortname: ${option.shortName}</option>     	
				        </c:forEach>        
				    </c:if>
				</select>
			</div>
			</div>
		</spring:bind>	
		</div>
		<div class="dynaClear">
			&nbsp;
		</div>
		<fieldset>
		<legend><fmt:message key="jsp.layout.hku.label.tab.icon" /></legend>
		<input type="file" size="50%" name="iconFile"/>
		<c:if test="${!empty tab.ext || !empty tab.mime}"><div style="font-size: xx-small"><fmt:message key="jsp.layout.hku.label.deleteicon" /><input type="checkbox" name="deleteIcon" value="true"/></div></c:if>
		</fieldset>		
		<br/><br/>
		<fieldset><legend><fmt:message
					key="jsp.layout.hku.label.boxlist" /></legend>
		
			
		<c:forEach items="${researcher:sortBoxByComparator(boxsList,'org.dspace.app.webui.cris.comparator.CustomBoxComparator')}" var="box">
			
							<c:set var="hasCustomEditJSP" value="false" scope="request" />
							<c:set var="hasCustomDisplayJSP" value="false" scope="request" />
							<c:set var="urljspcustomone"
								value="/dspace-cris/jdyna/custom/edit${box.externalJSP}.jsp" scope="request" />
							<c:set var="urljspcustomtwo"
								value="/dspace-cris/jdyna/custom/${box.externalJSP}.jsp" scope="request" />
								
							<%
							 	URL fileDisplayURL = null;
								URL fileEditURL = null;
								String filePathOne = (String)pageContext.getRequest().getAttribute("urljspcustomone");
								String filePathTwo = (String)pageContext.getRequest().getAttribute("urljspcustomtwo");

							fileEditURL = pageContext.getServletContext().getResource(
												filePathOne);
							fileDisplayURL = pageContext.getServletContext().getResource(
												filePathTwo);
							%>

							<%
								if (fileEditURL != null) {
							%>
								<c:set var="hasCustomEditJSP"
								value="true" scope="request" />
							<%							
								}
							%>
							<%
								if (fileDisplayURL != null) {
							%>
								<c:set var="hasCustomDisplayJSP"
								value="true" scope="request" />
							<%							
								}
							%>					
							
			<div class="mask">
			<c:if test="${!empty tab.id}">	
	
			<a class="jdynaeditbutton" title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.editdynamicfield" />"
			href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/editBox.htm?id=${box.id}&tabId=${tab.id}">		
			<span class="fa fa-edit" id="edit_${box.id}"></span>
					
			</a>
			</c:if>
			
			
			
			<spring:bind path="mask">
				<input id="_${status.expression}" name="_${status.expression}" value="true" type="hidden" />
				<c:set var="checked" value="false" />
				<c:forEach var="ownered" items="${owneredBoxs}" varStatus="i">				
					<c:if test="${ownered.id eq box.id}">
						<c:set var="checked" value="true" />
						<spring:transform value="${ownered.id}" var="optionToCompare" />					
					</c:if>
				</c:forEach>
				<form:label path="mask" for="mask">${box.title}</form:label>
				
				<input class="jdynacontainable" type="checkbox"
				<c:choose>
				<c:when test="${checked == true}">
					value="${optionToCompare}" checked="checked"
				</c:when>
				<c:otherwise>
					value="${box.id}"
				</c:otherwise>
				</c:choose>
							
				 id="mask_${box.id}" name="mask"
			
			 /></spring:bind>		
			 
			 <c:if test="${hasCustomDisplayJSP eq true}">
			
			<img
				src="<%=request.getContextPath()%>/image/authority/jdynahavecustomdisplayjsp.jpg"
				border="0"
				alt="<fmt:message
					key="jsp.dspace-admin.hku.jdyna-configuration.havecustomdisplayjsp" />"
				title="<fmt:message
					key="jsp.dspace-admin.hku.jdyna-configuration.havecustomdisplayjsp" />"
				id="havecustom_${box.id}" />
			</c:if>
			<c:if test="${hasCustomEditJSP eq true}">
			
			<img
				src="<%=request.getContextPath()%>/image/authority/jdynahavecustomeditjsp.jpg"
				border="0"
				alt="<fmt:message
					key="jsp.dspace-admin.hku.jdyna-configuration.havecustomeditjsp" />"
				title="<fmt:message
					key="jsp.dspace-admin.hku.jdyna-configuration.havecustomeditjsp" />"
				id="havecustom_${box.id}" />
			</c:if>
			</div>
		</c:forEach>
		
		</fieldset>


		<input type="submit" class="btn btn-primary pull-right"
			value="<fmt:message key="jsp.layout.hku.researcher.button.save" />" />

	</form:form>
	<c:if test="${!empty tab.id}">
	<div style="padding: 0; margin: 0 10px;"><a class="btn btn-default pull-right"
		href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createBox.htm?tabId=${tab.id}">
	<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.newbox" />
	</a></div>
	</c:if>
	<c:if test="${!empty edittabid}">
	<div style="padding: 0; margin: 0 10px;"><a class="btn btn-default pull-right"
		href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/editEditTabs.htm?id=${edittabid}">
	<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.tabtoedit" />
	</a></div>
	</c:if>

</dspace:layout>
