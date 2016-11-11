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
<dspace:layout locbar="link" style="submission" navbar="admin"
	titlekey="jsp.dspace-admin.researchers-list">
<h1><fmt:message key="jsp.dspace-admin.edit-box" />
<a target="_blank"
	href='<%=request.getContextPath()%><%=LocaleSupport.getLocalizedMessage(pageContext,
                             "help.site-admin.rp")%>'><fmt:message
	key="jsp.help" /></a></h1>

		
							<c:set var="hasCustomEditJSP" value="false" scope="request" />
							<c:set var="hasCustomDisplayJSP" value="false" scope="request" />
							<c:set var="urljspcustomone"
								value="/authority/jdyna/custom/edit${box.externalJSP}.jsp" scope="request" />
							<c:set var="urljspcustomtwo"
								value="/authority/jdyna/custom/${box.externalJSP}.jsp" scope="request" />
								
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
								<div id="authority-message">
								<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.havecustomeditjsp" />
								</div>
							<%							
								}
							%>
							<%
								if (fileDisplayURL != null) {
							%>
							<div id="authority-message">
								<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.havecustomdisplayjsp" />
								</div>
							<%							
								}
							%>					

	<form:form commandName="box" method="post">
		<c:set value="${message}" var="message" scope="request" />
		<c:if test="${!empty message}">
			<div class="alert alert-default"><fmt:message key="${message}" /></div>
		</c:if>

		<c:if test="${not empty messages}">
			<div class="message" id="successMessages"><c:forEach var="msg"
				items="${messages}">
				<div class="alert alert-success">${msg}</div>
			</c:forEach></div>
			<c:remove var="messages" scope="session" />
		</c:if>

		<%--  first bind on the object itself to display global errors - if available  --%>
		<spring:bind path="box">
			<c:forEach items="${status.errorMessages}" var="error">
				<span id="errorMessage" class="alert alert-danger"><fmt:message
					key="jsp.layout.hku.prefix-error-code" /> ${error}</span>
				<br>
			</c:forEach>
		</spring:bind>
		
				
		<spring:bind path="box.*">
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
		
		<dyna:text propertyPath="box.shortName"  helpKey="help.jdyna.message.box.shortname"
			labelKey="jdyna.message.box.shortname" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:text propertyPath="box.title"  helpKey="help.jdyna.message.box.title"
			labelKey="jdyna.message.box.title" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:text propertyPath="box.priority"  helpKey="help.jdyna.message.box.priority"
				labelKey="jdyna.message.box.priority" size="5" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:boolean propertyPath="box.collapsed"  helpKey="help.jdyna.message.box.collapsed"
				labelKey="jdyna.message.box.collapsed"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:boolean propertyPath="box.unrelevant"  helpKey="help.jdyna.message.box.unrelevant"
				labelKey="jdyna.message.box.unrelevant"/>
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
				<input ${disabled} id="${inputName}_${count.count}" name="${inputName}" class="${inputName}"
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

		<c:if test="${box.visibility!=4}">
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
		<dyna:text propertyPath="box.externalJSP"  helpKey="help.jdyna.message.box.externalJSP"
			labelKey="jdyna.message.box.externalJSP" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
				
		<br/><br/>
		
		<c:set var="sortedAllContainablesList" value="${researcher:sortBoxByComparator(containablesList,'org.dspace.app.webui.cris.comparator.CustomContainableComparator')}" />
		<c:set var="count" value="0" />
		<fieldset><legend><fmt:message
			key="jsp.layout.hku.label.containableslist" /></legend> 
			
			<c:forEach
			items="${sortedAllContainablesList}" var="boxed">
			
			<c:if test="${boxed.class.simpleName eq 'DecoratorRestrictedField'}">
				<div class="mask">
				${boxed.shortName}	<input class="jdynacontainable" type="checkbox" disabled="disabled" checked="checked"/>
				</div>
				<c:set var="count" value="${count+1}" />
			</c:if>
			<c:if test="${!empty box.id}">			
				<c:if
					test="${dyna:instanceOf(boxed,'it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition')}">
					
					<c:forEach var="ownered" items="${owneredContainables}"
							varStatus="i">
							<c:if test="${ownered.id eq boxed.id}">
							<c:set var="checked" value="true" />
						<spring:transform value="${ownered.id}" var="optionToCompare" />
						</c:if>
					</c:forEach>
					
					<c:if test="${checked == true}">
					<c:set var="count" value="${count+1}" />
					<div class="mask">
					<c:if test="${boxed.real.rendering.triview == 'testo'}">
						<c:set var="controller" value="Text" />
					</c:if>
				    <c:if test="${boxed.real.rendering.triview == 'boolean'}">
						<c:set var="controller" value="Boolean" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'calendar'}">
						<c:set var="controller" value="Date" />
					</c:if>					
					<c:if test="${boxed.real.rendering.triview == 'link'}">
						<c:set var="controller" value="Link" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'file'}">
						<c:set var="controller" value="File" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'classificationTree'}">
						<c:set var="controller" value="ClassificationTree" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'checkradio'}">
						<c:set var="controller" value="CheckRadio" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'custompointer'}">
						<c:if test="${boxed.real.rendering.type == 7}">
							<c:set var="controller" value="EPerson" />
						</c:if>
						<c:if test="${boxed.real.rendering.type == 6}">
							<c:set var="controller" value="Group" />
						</c:if>
					</c:if>					
					<c:if test="${boxed.real.rendering.triview == 'pointer'}">
						<c:set var="controller" value="${boxed.real.rendering.valoreClass.simpleName}" />
					</c:if>
					<a class="jdynaremovebutton"
						title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.deletedynamicfield" />"
						href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/deleteFieldDefinition.htm?pDId=${boxed.real.id}&boxId=${box.id}&tabId=${tabId}">
					<span class="fa fa-trash" id="remove_${boxed.id}" ></span> </a>
						<a class="jdynaeditbutton"
						title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.editdynamicfield" />"
						href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/edit${controller}DynamicField.htm?pDId=${boxed.real.id}&boxId=${box.id}&tabId=${tabId}">
					<span class="fa fa-edit" id="edit_${boxed.id}" ></span> </a>

					<spring:bind path="mask">
						<input id="_${status.expression}" name="_${status.expression}"
							value="true" type="hidden" />
						<c:set var="checked" value="false" />
					
						<form:label path="mask" for="mask">${boxed.label} [${boxed.shortName}][<fmt:message key="jsp.layout.hku.label.propertiesdefinition.priority"/>:${boxed.priority}]</form:label>

						<input class="jdynacontainable" type="checkbox"	value="${optionToCompare}" checked="checked" id="mask_${boxed.id}" name="mask" />
					</spring:bind>
					</div>
					</c:if>
				</c:if>
				<c:if
					test="${dyna:instanceOf(boxed,'it.cilea.osd.jdyna.model.ADecoratorTypeDefinition')}">
					<c:forEach var="ownered" items="${owneredContainables}"
							varStatus="i">
							<c:if test="${ownered.id eq boxed.id}">
							<c:set var="checked" value="true" />
						<spring:transform value="${ownered.id}" var="optionToCompare" />
						</c:if>
					</c:forEach>
					
					<c:if test="${checked == true}">
					<c:set var="controller" value="Nested" />
					<div class="mask">
				<a class="jdynaremovebutton"
						title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.deletedynamicfield" />"
						href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/deleteNestedField.htm?pDId=${boxed.real.id}&boxId=${box.id}&tabId=${tabId}">
					<span class="fa fa-trash" id="remove_${boxed.id}" ></span> </a>
					<a class="jdynaeditbutton"
						title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.editdynamicfield" />"
						href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/edit${controller}DynamicField.htm?pDId=${boxed.real.id}&boxId=${box.id}&tabId=${tabId}">
					<span class="fa fa-edit" id="edit_${boxed.id}" ></span> </a>
					<spring:bind path="mask">
						<input id="_${status.expression}" name="_${status.expression}"
							value="true" type="hidden" />
						<c:set var="checked" value="false" />
					
						<form:label path="mask" for="mask">${boxed.label} [${boxed.shortName}][<fmt:message key="jsp.layout.hku.label.propertiesdefinition.priority"/>:${boxed.priority}]</form:label>

						<input class="jdynacontainable" type="checkbox"	value="${optionToCompare}" checked="checked" id="mask_${boxed.id}" name="mask" />
					</spring:bind>
					</div>				
					</c:if>	
				</c:if>
			</c:if>
		   
		    
		</c:forEach>
		
		<c:if test="${fn:length(containablesList) > count}"> 
		<a class="show" id="show" href="#" title="Show other"><fmt:message key="jsp.dspace-admin.edit-box.showotherpdef" /><i class="fa fa-toggle-down"></i></a>
		
		<div id="othermetadata" style="display: none">
		<c:forEach
			items="${sortedAllContainablesList}" var="boxed">
					
			
			<c:if test="${!empty box.id}">
			
				<c:if
					test="${dyna:instanceOf(boxed,'it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition')}">
					<c:set var="checked" value="false" />
					<c:forEach var="ownered" items="${owneredContainables}"
							varStatus="i">
							<c:if test="${ownered.id eq boxed.id}">
								<c:set var="checked" value="true" />
								<spring:transform value="${ownered.id}" var="optionToCompare" />
							</c:if>
					</c:forEach>
					
					<c:if test="${checked eq false}">
					
					<div class="mask">
					<c:if test="${boxed.real.rendering.triview == 'testo'}">
						<c:set var="controller" value="Text" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'boolean'}">
						<c:set var="controller" value="Boolean" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'calendar'}">
						<c:set var="controller" value="Date" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'file'}">
						<c:set var="controller" value="File" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'link'}">
						<c:set var="controller" value="Link" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'classificationTree'}">
						<c:set var="controller" value="ClassificationTree" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'checkradio'}">
						<c:set var="controller" value="CheckRadio" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'pointer'}">
						<c:set var="controller" value="${boxed.real.rendering.valoreClass.simpleName}" />
					</c:if>
					<c:if test="${boxed.real.rendering.triview == 'custompointer'}">
						<c:if test="${boxed.real.rendering.type == 7}">
							<c:set var="controller" value="EPerson" />
						</c:if>
						<c:if test="${boxed.real.rendering.type == 6}">
							<c:set var="controller" value="Group" />
						</c:if>
					</c:if>
					<a class="jdynaremovebutton"
						title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.deletedynamicfield" />"
						href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/deleteFieldDefinition.htm?pDId=${boxed.real.id}&boxId=${box.id}&tabId=${tabId}">
					<span class="fa fa-trash" id="remove_${boxed.id}" ></span> </a>
					 </a>
					<a class="jdynaeditbutton"
						title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.editdynamicfield" />"
						href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/edit${controller}DynamicField.htm?pDId=${boxed.real.id}&boxId=${box.id}&tabId=${tabId}">
					<span class="fa fa-edit" id="edit_${boxed.id}" ></span> </a>

					<spring:bind path="mask">
						<input id="_${status.expression}" name="_${status.expression}"
							value="true" type="hidden" />
						<c:set var="checked" value="false" />
					
						<form:label path="mask" for="mask">${boxed.label} [${boxed.shortName}][<fmt:message key="jsp.layout.hku.label.propertiesdefinition.priority"/>:${boxed.priority}]</form:label>

						<input class="jdynacontainable" type="checkbox"	value="${boxed.id}" id="mask_${boxed.id}" name="mask" />
					</spring:bind>
					</div>
					</c:if>
				</c:if>
				
				
				<c:if
					test="${dyna:instanceOf(boxed,'it.cilea.osd.jdyna.model.ADecoratorTypeDefinition')}">
					<c:set var="checked" value="false" />
					<c:forEach var="ownered" items="${owneredContainables}"
							varStatus="i">
							<c:if test="${ownered.id eq boxed.id}">
								<c:set var="checked" value="true" />
								<spring:transform value="${ownered.id}" var="optionToCompare" />
							</c:if>
					</c:forEach>
					
					<c:if test="${checked eq false}">
					
					<div class="mask">
					
					<c:set var="controller" value="Nested" />
					
					<a class="jdynaremovebutton"
						title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.deletedynamicfield" />"
						href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/deleteNestedField.htm?pDId=${boxed.real.id}&boxId=${box.id}&tabId=${tabId}">
					<span class="fa fa-trash" id="remove_${boxed.id}" ></span> </a>
					<a class="jdynaeditbutton"
						title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.editdynamicfield" />"
						href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/edit${controller}DynamicField.htm?pDId=${boxed.real.id}&boxId=${box.id}&tabId=${tabId}">
					<span class="fa fa-edit" id="edit_${boxed.id}" ></span> </a>

					<spring:bind path="mask">
						<input id="_${status.expression}" name="_${status.expression}"
							value="true" type="hidden" />
						<c:set var="checked" value="false" />
					
						<form:label path="mask" for="mask">${boxed.label} [${boxed.shortName}][<fmt:message key="jsp.layout.hku.label.propertiesdefinition.priority"/>:${boxed.priority}]</form:label>

						<input class="jdynacontainable" type="checkbox"	value="${boxed.id}" id="mask_${boxed.id}" name="mask" />
					</spring:bind>
					</div>
					</c:if>
				</c:if>
			</c:if>
		   
		    
		</c:forEach>
		</div>
		</c:if>
		
		</fieldset>

		<input type="hidden" id="tabId" name="tabId" value="${tabId}" />
		<input type="submit" class="btn btn-primary pull-right"
			value="<fmt:message key="jsp.layout.hku.researcher.button.save" />" />
		
		<c:if test="${!empty box.id}">
	  <div class="btn-group dropup pull-right">
      <a class="btn btn-default dropdown-toggle" data-toggle="dropdown" href="#">
        <fmt:message key="jsp.layout.hku.researcher.button.default.option.dropdown" />
        <span class="caret"></span>
      </a>
      <ul class="dropdown-menu">
        <!-- dropdown menu links -->
        	<li><a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createDateDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newdatedynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createTextDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newtextdynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createNestedDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newnesteddynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createLinkDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newlinkdynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createFileDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newfiledynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createRPPointerDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newrppointerdynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createProjectPointerDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newprojectpointerdynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createOUPointerDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newoupointerdynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createDOPointerDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newdopointerdynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createBooleanDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newbooleandynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createCheckRadioDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newcheckradiodynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createClassificationTreeDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newclassificationtreedynamicfield" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createEPersonDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newepersonwidget" />
			</a></li><li>
			<a 
				href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/createGroupDynamicField.htm?boxId=${box.id}&tabId=${tabId}">
			<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.newgroupwidget" />
			</a></li>
        
      	</ul>
    </div>
		</c:if>

	</form:form>
	
	<script type="text/javascript">
	<!--
	var j = jQuery;
    j(document).ready(function() {	
		j( "#show" ).click(function() {
			j( "#othermetadata" ).toggle();
		});		
    });
	-->
	</script>
</dspace:layout>
