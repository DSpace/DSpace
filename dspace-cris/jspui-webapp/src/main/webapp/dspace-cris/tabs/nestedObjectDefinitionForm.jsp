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
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@page import="it.cilea.osd.jdyna.model.AccessLevelConstants"%>
<%@page import="it.cilea.osd.jdyna.widget.Size"%>
<%@page import="java.util.LinkedList"%>
<%@page import="java.util.List"%>
<%@page import="it.cilea.osd.jdyna.model.AccessLevelConstants"%>
<%@page import="it.cilea.osd.jdyna.utils.SimpleSelectableObject"%>
<%@page import="org.dspace.app.cris.model.jdyna.DynamicObjectType"%>
<%@page import="org.dspace.app.cris.model.CrisConstants"%>
<%@page import="org.dspace.app.cris.model.ResearchObject"%>
<%@page import="org.dspace.app.cris.model.jdyna.DynamicPropertiesDefinition"%>
<%@page import="it.cilea.osd.jdyna.model.PropertiesDefinition"%>
<%@page import="org.dspace.app.cris.model.jdyna.widget.WidgetClassificationTree"%>

<%@ taglib uri="jdynatags" prefix="dyna" %>
<c:set var="dspace.layout.head.last" scope="request">
    <link href="<%= request.getContextPath() %>/css/select2/select2.css" type="text/css" rel="stylesheet" />
    <link href="<%= request.getContextPath() %>/css/select2/select2-bootstrap.css" type="text/css" rel="stylesheet" />
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/select2/select2.min.js"></script>
    <script type="text/javascript"><!--
	
		j(document).ready(function() {
        	jQuery(".jdynadropdown").select2({"dropdownAutoWidth": true});
        	
			var $eventSelectOne = jQuery("#treeObjectType");
			$eventSelectOne.on("change", function (e) { 
				FillDropdown1(jQuery('#treeObjectType').val());
				FillDropdown2(jQuery('#treeObjectType').val());
			});
			
			//var countSelectTwo = 0;
			//var $eventSelectTwo = jQuery("#rootObjectId");			
			//$eventSelectTwo.on("select2-open", function (e) {
			//	if(countSelectTwo == 0) {
			//	FillDropdown1(jQuery('#treeObjectType').val(), jQuery('#rootObjectId option:selected').val(), jQuery('#rootObjectId option:selected').text());
			//	countSelectTwo = countSelectTwo + 1;
			//	}
			//}); 
			
			
			var countSelectThree = 0;
			var $eventSelectThree = jQuery("#metadataBuilder");
			
			$eventSelectThree.on("select2-open", function (e) {
				if(countSelectThree == 0) {
				FillDropdown2(jQuery('#treeObjectType').val(), jQuery('#metadataBuilder option:selected').val(), jQuery('#metadataBuilder option:selected').text());
				countSelectThree = countSelectThree + 1;
				}
			}); 
		});
    
	    var FillDropdown1 = function(selectedVal, optionSelectedId, optionSelectedVal)
	    {
	        if(jQuery.isEmptyObject(selectedVal))
	        {
	            return;
	        }

        	jQuery('#rootObjectId').select2('destroy');
        	jQuery('#rootObjectId').html("");
        	jQuery('#rootObjectId').select2({"dropdownAutoWidth": true, "allowClear": true, "placeholder": "Select one or leave blank"});
        	jQuery("#rootObjectId").append("<option></option>");
        	if(optionSelectedId!=null) {
        		jQuery("#rootObjectId").append("<option value="+optionSelectedId+">"+optionSelectedVal+"</option>");
        		jQuery("#rootObjectId").append("<option disabled></option>");
        	}
        	
	        jQuery.post(
	        		"<%= request.getContextPath() %>/cris/tools/widget/buildClassificationTree.htm",
	                {type: selectedVal, method: "filldropdownwithresearchobject"},
	        function(data)
	        {
	            if(data != null && data.length > 0) {   
	            	
		            jQuery.each(data, function(i, item)
		            {
		            	jQuery("#rootObjectId").append("<option value='"+item['ID']+"'>"+ item['name'] +"</option>");	
		            });
	            }
	        });
	    }
	    
	    
	    var FillDropdown2 = function(selectedVal, optionSelectedId, optionSelectedVal)
	    {
	        if(jQuery.isEmptyObject(selectedVal))
	        {
	            return;
	        }

        	jQuery('#metadataBuilder').select2('destroy');
        	jQuery('#metadataBuilder').html("");
        	jQuery('#metadataBuilder').select2({"dropdownAutoWidth": true, "allowClear": true, "placeholder": "Select one (required)"});
        	jQuery("#metadataBuilder").append("<option></option>");
        	if(optionSelectedId!=null) {
        		jQuery("#metadataBuilder").append("<option value="+optionSelectedId+">"+optionSelectedVal+"</option>");
        		jQuery("#metadataBuilder").append("<option disabled></option>");
        	}
	        jQuery.post(
	        		"<%= request.getContextPath() %>/cris/tools/widget/buildClassificationTree.htm",
	                {type: selectedVal, method: "filldropdownwithmetadatadefinition"},
	        function(data)
	        {
	            if(data != null && data.length > 0) {   

		            jQuery.each(data, function(i, item)
		            {
		            	jQuery("#metadataBuilder").append("<option value='"+item['id']+"'>"+ item['label'] +"</option>");	
		            });
	            }
	        });
	    }
	--></script>
</c:set>
<c:set var="STANDARD_ACCESS" value="<%=  AccessLevelConstants.STANDARD_ACCESS %>"></c:set>
<dspace:layout locbar="link" navbar="admin" style="submission"
	titlekey="jsp.dspace-admin.researchers-list">

	<table width="95%">
		<tr>
			<td align="left">
			<h1><fmt:message
				key="jsp.dspace-admin.edit-propertiesdefinition" /></h1>
			</td>
			<td align="right" class="standard"><a target="_blank"
				href='<%=request.getContextPath()%><%=LocaleSupport.getLocalizedMessage(pageContext,
                                "help.site-admin.rp")%>'><fmt:message
				key="jsp.help" /></a></td>
		</tr>
	</table>



	<form:form commandName="propertiesdefinition" method="post">
		<c:set value="${message}" var="message" scope="request" />
		<c:if test="${!empty message}">
			<div id="authority-message"><fmt:message key="${message}" /></div>
		</c:if>

		<c:if test="${not empty messages}">
			<div class="message" id="successMessages"><c:forEach var="msg"
				items="${messages}">
				<div id="authority-message">${msg}</div>
			</c:forEach></div>
			<c:remove var="messages" scope="session" />
		</c:if>

		<%--  first bind on the object itself to display global errors - if available  --%>
		<spring:bind path="propertiesdefinition">
			<c:forEach items="${status.errorMessages}" var="error">
				<span id="errorMessage"><fmt:message
					key="jsp.layout.hku.prefix-error-code" /> ${error}</span>
				<br>
			</c:forEach>
		</spring:bind>

		<spring:bind path="propertiesdefinition.*">
		<c:if test="${not empty status.errorMessages}">
			<div class="errorMessage"><c:forEach var="error"
				items="${status.errorMessages}">
	               ${error}<br />
			</c:forEach></div>
		</c:if>
		</spring:bind>

		<c:if test="${not empty status.errorMessages}">
		<div class="errorMessage"><c:forEach var="error"
			items="${status.errorMessages}">
                 ${error}<br />
		</c:forEach></div>
		</c:if>

		<fieldset>
		<legend><fmt:message
			key="jsp.layout.hku.label.propertiesdefinition.rendering.nested" /></legend> 


		<dyna:text visibility="false" propertyPath="real.shortName"
				labelKey="jsp.layout.hku.label.propertiesdefinition.shortName" helpKey="help.jdyna.message.shortName"/>
		
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:text visibility="false" propertyPath="real.label" size="50"
				labelKey="jsp.layout.hku.label.propertiesdefinition.label" helpKey="help.jdyna.message.label"/>						
		
		<div class="dynaClear">
			&nbsp;
		</div>

		<spring:bind path="real.accessLevel">
			<c:set var="inputValue">
				<c:out value="${status.value}" escapeXml="true"></c:out>
			</c:set>
			<c:set var="inputName">
				<c:out value="${status.expression}" escapeXml="false"></c:out>
			</c:set>


			<dyna:label propertyPath="${inputName}" labelKey="jsp.layout.hku.label.propertiesdefinition.accessLevel" helpKey="help.jdyna.message.accessLevel"/>
			<div class="dynaClear">
			&nbsp;
			</div>
			<c:forEach items="<%= AccessLevelConstants.getValues() %>" var="item">
			<c:if test="${!(item eq STANDARD_ACCESS)}">
				<input ${disabled} id="${inputName}" name="${inputName}"
					type="radio" value="${item}"
					<c:if test="${inputValue==item}">checked="checked"</c:if> />
				<fmt:message
					key="jsp.layout.hku.label.propertiesdefinition.accessLevel.${item}" />
			</c:if>
			</c:forEach>
			<input ${disabled} name="_${inputName}" id="_${inputName}"
				value="true" type="hidden" />
		</spring:bind>


		<div class="dynaClear">
			&nbsp;
		</div>
		

		
<%-- 		<dyna:boolean propertyPath="propertiesdefinition.real.mandatory"
				labelKey="jsp.layout.hku.label.propertiesdefinition.mandatory" helpKey="help.jdyna.message.mandatory"/>
		
		<div class="dynaClear">
			&nbsp
		</div> --%>
				
		
		<dyna:boolean propertyPath="propertiesdefinition.real.repeatable"
				labelKey="jsp.layout.hku.label.propertiesdefinition.repeatable" helpKey="help.jdyna.message.repeatable"/>
		<div class="dynaClear">
			&nbsp;
		</div>

		<dyna:text propertyPath="propertiesdefinition.real.priority"  helpKey="help.jdyna.message.priority"
				labelKey="jsp.layout.hku.label.propertiesdefinition.priority" size="5" visibility="false"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		
		
		<dyna:boolean propertyPath="propertiesdefinition.real.newline"
				labelKey="jsp.layout.hku.label.propertiesdefinition.newline" helpKey="help.jdyna.message.newline"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:boolean propertyPath="propertiesdefinition.real.inline"
				labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.inline" helpKey="help.jdyna.message.inline"/>
		<div class="dynaClear">
			&nbsp;
		</div>
		</fieldset>
		

						<fieldset><legend><fmt:message
							key="jsp.layout.hku.label.propertiesdefinition.widget" /></legend> <input
							type="hidden" id="renderingparent" name="renderingparent"
							value="${renderingparent}" /> 
														
						<c:forEach
							items="${propertiesdefinition.real.mask}"
							var="subtypo" varStatus="i">

							<fieldset><legend><fmt:message
									key="jsp.layout.hku.label.propertiesdefinition.rendering.${subtypo.rendering.triview}" /></legend> 
							
							<dyna:text visibility="false" propertyPath="real.mask[${i.count - 1}].shortName"
								labelKey="jsp.layout.hku.label.propertiesdefinition.shortName" helpKey="help.jdyna.message.shortName"/>
		
		
							<dyna:text visibility="false" propertyPath="real.mask[${i.count - 1}].label" size="50"
									labelKey="jsp.layout.hku.label.propertiesdefinition.label" helpKey="help.jdyna.message.label"/>						
		
							<div class="dynaClear">
								&nbsp;
							</div>
							
							<spring:bind path="real.mask[${i.count - 1}].accessLevel">
								<c:set var="inputValue">
									<c:out value="${status.value}" escapeXml="true"></c:out>
								</c:set>
								<c:set var="inputName">
									<c:out value="${status.expression}" escapeXml="false"></c:out>
								</c:set>
					
					
								<dyna:label propertyPath="${inputName}" labelKey="jsp.layout.hku.label.propertiesdefinition.accessLevel" helpKey="help.jdyna.message.accessLevel"/>
								<div class="dynaClear">
								&nbsp;
								</div>
								<c:forEach items="<%= AccessLevelConstants.getValues() %>" var="item">
									<input ${disabled} id="${inputName}" name="${inputName}"
										type="radio" value="${item}"
										<c:if test="${inputValue==item}">checked="checked"</c:if> />
									<fmt:message
										key="jsp.layout.hku.label.propertiesdefinition.accessLevel.${item}" />
					
								</c:forEach>
								<input ${disabled} name="_${inputName}" id="_${inputName}"
									value="true" type="hidden" />
							</spring:bind>
							
										
							<div class="dynaClear">
								&nbsp;
							</div>
							<dyna:boolean propertyPath="real.mask[${i.count - 1}].repeatable"
									labelKey="jsp.layout.hku.label.propertiesdefinition.repeatable" helpKey="help.jdyna.message.repeatable"/>
							<div class="dynaClear">
								&nbsp;
							</div>
										<c:if
											test="${subtypo.rendering.triview eq 'link'}">
											<fieldset style="width: 90%;"><dyna:text visibility="false"
												propertyPath="real.mask[${i.count - 1}].rendering.labelHeaderLabel"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.link.labelHeaderLabel"
												helpKey="help.jdyna.message.rendering.link.labelHeaderLabel" />

											<dyna:text visibility="false"
												propertyPath="real.mask[${i.count - 1}].rendering.labelHeaderURL"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.link.labelHeaderURL"
												helpKey="help.jdyna.message.rendering.link.labelHeaderURL" />
											</fieldset>
											<div class="dynaClear">
												&nbsp;
											</div>
										</c:if>
					
										<c:if
											test="${subtypo.rendering.triview eq 'testo'}">
											<fieldset style="width: 90%;"><dyna:text visibility="false"
												propertyPath="real.mask[${i.count - 1}].rendering.dimensione.col"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.text.size" helpKey="help.jdyna.message.rendering.text.size" />
											<div class="dynaClear">
												&nbsp;
											</div>
											<dyna:text visibility="false"
												propertyPath="real.mask[${i.count - 1}].rendering.regex"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.text.regex"
												helpKey="help.jdyna.message.rendering.text.regex" />
											<div class="dynaClear">
												&nbsp;
											</div>			
											<dyna:text visibility="false" propertyPath="real.mask[${i.count - 1}].rendering.displayFormat"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.text.displayFormat" helpKey="help.jdyna.message.rendering.text.displayFormat"/>	
											</fieldset>

										</c:if>
					
										<c:if
											test="${subtypo.rendering.triview eq 'file'}">
											<fieldset style="width: 90%;">
											
											<dyna:boolean propertyPath="real.mask[${i.count - 1}].rendering.showPreview"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.file.showpreview" helpKey="help.jdyna.message.rendering.file.showpreview"/>
											<div class="dynaClear">
												&nbsp;
											</div>
											<dyna:text visibility="false" propertyPath="real.mask[${i.count - 1}].rendering.fileDescription"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.file.fileDescription" helpKey="help.jdyna.message.rendering.file.description"/>
											<div class="dynaClear">
												&nbsp;
											</div>
											<dyna:text visibility="false" propertyPath="real.mask[${i.count - 1}].rendering.labelAnchor"
											labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.file.labelAnchor" helpKey="help.jdyna.message.rendering.file.labelAnchor"/>
																	
											
											</fieldset>

										</c:if>
										
										<c:if
											test="${subtypo.rendering.triview eq 'calendar'}">
											<fieldset>
											<dyna:text visibility="false" propertyPath="real.mask[${i.count - 1}].rendering.minYear"
													labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.calendar.min" helpKey="help.jdyna.message.rendering.calendar.min"/>
					
											<dyna:text visibility="false" propertyPath="real.mask[${i.count - 1}].rendering.maxYear"
													labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.calendar.max" helpKey="help.jdyna.message.rendering.calendar.max"/>
											</fieldset>		
										</c:if>
					
										<c:if test="${subtypo.rendering.triview eq 'pointer'}">
											<fieldset>
											<dyna:text propertyPath="real.mask[${i.count - 1}].rendering.display"  visibility="false"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.display" helpKey="help.jdyna.message.rendering.pointer.display"/>
											<div class="dynaClear">
												&nbsp;
											</div>
											<dyna:text propertyPath="real.mask[${i.count - 1}].rendering.filtro"  visibility="false"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.filter" helpKey="help.jdyna.message.rendering.pointer.filter"/>
												
											<div class="dynaClear">
												&nbsp;
											</div>												
											<c:if test="${dyna:instanceOf(subtypo.rendering,'org.dspace.app.cris.model.jdyna.widget.WidgetPointerDO')}">
							
												<% 
													List<DynamicObjectType> researchobjects = (List<DynamicObjectType>)request.getAttribute("researchobjects");
													List<SimpleSelectableObject>  types = new LinkedList<SimpleSelectableObject>(); 
													for(DynamicObjectType objs : researchobjects) {
													    SimpleSelectableObject sso = new SimpleSelectableObject();
													    sso.setDisplayValue(objs.getLabel());
													    sso.setIdentifyingValue("search.resourcetype:"+ (objs.getId() + CrisConstants.CRIS_DYNAMIC_TYPE_ID_START));
													    types.add(sso);
													}												
												%>
												<dyna:select propertyPath="real.mask[${i.count - 1}].rendering.filterExtended" labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.filterextended" collection="<%= types %>"/>												
											</c:if>
											<div class="dynaClear">
												&nbsp;
											</div>
											<dyna:text visibility="false" propertyPath="real.mask[${i.count - 1}].rendering.indexName"
													labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.indexname" helpKey="help.jdyna.message.rendering.pointer.indexname"/>
											<dyna:text propertyPath="real.mask[${i.count - 1}].rendering.urlPath"  visibility="false"
													labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.path" helpKey="help.jdyna.message.rendering.pointer.path"/>						
											</fieldset>											
										</c:if>
										<c:if test="${subtypo.rendering.triview eq 'boolean'}">
											<fieldset>
											<dyna:boolean propertyPath="real.mask[${i.count - 1}].rendering.checked"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.boolean.checked" helpKey="help.jdyna.message.rendering.boolean.checked"/>
											<div class="dynaClear">
											&nbsp;
											</div>
											<dyna:boolean propertyPath="real.mask[${i.count - 1}].rendering.hideWhenFalse"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.boolean.hideWhenFalse" helpKey="help.jdyna.message.rendering.boolean.hideWhenFalse"/>
											</fieldset>											
										</c:if>
										<c:if test="${subtypo.rendering.triview eq 'checkradio'}">
										<fieldset>
											<dyna:text propertyPath="real.mask[${i.count - 1}].rendering.option4row"  visibility="false"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.checkradio.option4row" helpKey="help.jdyna.message.rendering.checkradio.option4row"/>
											<div class="dynaClear">
											&nbsp;
											</div>
					
											<dyna:text propertyPath="real.mask[${i.count - 1}].rendering.staticValues"  visibility="false"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.checkradio.staticValues" helpKey="help.jdyna.message.rendering.checkradio.staticValues"/>
					 						<div class="dynaClear">
											&nbsp;
											</div>
											<dyna:boolean propertyPath="real.mask[${i.count - 1}].rendering.dropdown"
												labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.checkradio.dropdown" helpKey="help.jdyna.message.rendering.checkradio.dropdown"/>
										</fieldset>											
										</c:if>					
										<c:if test="${subtypo.rendering.triview eq 'classificationTree'}">
										<fieldset>
											
										<% 
												List<DynamicObjectType> researchobjects = (List<DynamicObjectType>)request.getAttribute("researchobjects");
										        PropertiesDefinition subtypo = (PropertiesDefinition)pageContext.getAttribute("subtypo");
												WidgetClassificationTree widgetClassification = (WidgetClassificationTree)subtypo.getRendering();
												List<SimpleSelectableObject>  types = new LinkedList<SimpleSelectableObject>();
												List<SimpleSelectableObject>  typesEmptyA = new LinkedList<SimpleSelectableObject>();
												List<SimpleSelectableObject>  typesEmptyB = new LinkedList<SimpleSelectableObject>();
												if(widgetClassification.getRootResearchObject() != null) {
													SimpleSelectableObject sso = new SimpleSelectableObject();
												    sso.setDisplayValue(widgetClassification.getRootResearchObject().getName());
												    sso.setIdentifyingValue(widgetClassification.getRootResearchObject().getIdentifyingValue());
												    typesEmptyA.add(sso);
												}
												if(widgetClassification.getMetadataBuilderTree() != null) {
													SimpleSelectableObject sso = new SimpleSelectableObject();
												    sso.setDisplayValue(widgetClassification.getMetadataBuilderTree().getLabel());
												    sso.setIdentifyingValue(widgetClassification.getMetadataBuilderTree().getIdentifyingValue());
												    typesEmptyB.add(sso);
												}
												for(DynamicObjectType objs : researchobjects) {
												    SimpleSelectableObject sso = new SimpleSelectableObject();
												    sso.setDisplayValue(objs.getLabel());
												    sso.setIdentifyingValue(objs.getShortName());
												    types.add(sso);
												}												
										%>
										<dyna:dropdown id="treeObjectType" propertyPath="real.mask[${i.count - 1}].rendering.treeObjectType" labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.classificationtree.treeObjectType"  helpKey="help.jdyna.message.rendering.classificationtree.treeObjectType" collection="<%= types %>"/>												
										<div class="dynaClear">
											&nbsp;
										</div>
				
				<%--					
										<dyna:dropdown id="rootObjectId" propertyPath="real.mask[${i.count - 1}].rendering.rootResearchObject" labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.classificationtree.rootObjectId" helpKey="help.jdyna.message.rendering.classificationtree.rootObjectId" collection="<%= typesEmptyA %>"/>												
										<div class="dynaClear">
											&nbsp;
										</div>
				--%>
				
										<dyna:dropdown id="metadataBuilder" propertyPath="real.mask[${i.count - 1}].rendering.metadataBuilderTree" labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.classificationtree.metadataBuilder" helpKey="help.jdyna.message.rendering.classificationtree.metadataBuilder" collection="<%= typesEmptyB %>"/>												
										<div class="dynaClear">
											&nbsp;
										</div>						
																
										<dyna:textarea toolbar="nessuna" propertyPath="real.mask[${i.count - 1}].rendering.display"
											rows="4" cols="60"
											labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.classificationtree.display" 
											helpKey="help.jdyna.message.rendering.classificationtree.display"/>
										<div class="dynaClear">
											&nbsp;
										</div>
										
				<%--					
									<dyna:boolean propertyPath="real.mask[${i.count - 1}].rendering.chooseOnlyLeaves"
											labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.classificationtree.chooseOnlyLeaves" helpKey="help.jdyna.message.rendering.classificationtree.chooseOnlyLeaves"/>
				--%>					
										</fieldset>																		
								</c:if>		
		
		
		<dyna:text propertyPath="real.mask[${i.count - 1}].priority"  helpKey="help.jdyna.message.priority"
								labelKey="jsp.layout.hku.label.propertiesdefinition.priority" size="5" visibility="false"/>
		
		<div class="dynaClear">
			&nbsp;
		</div>
		
		
		<dyna:boolean propertyPath="real.mask[${i.count - 1}].newline"
				labelKey="jsp.layout.hku.label.propertiesdefinition.newline" helpKey="help.jdyna.message.newline"/>
		<div class="dynaClear">
			&nbsp;
		</div>
				
		<dyna:text propertyPath="real.mask[${i.count - 1}].labelMinSize"  helpKey="help.jdyna.message.labelMinSize"
				labelKey="jsp.layout.hku.label.propertiesdefinition.labelMinSize" size="5" visibility="false"/>
		<spring:bind path="real.mask[${i.count - 1}].labelMinSizeUnit">				
		<select name="${status.expression}">
				<% for(String size : Size.getMeasurementUnits()) { %>
					<option <% if(status.getValue() != null && size.equals(status.getValue())) {%>" selected="selected" <% } %> value="<%=size%>"> <%=size%></option>	
				<% } %>
		</select>			
		</spring:bind>	
		<div class="dynaClear">
			&nbsp;
		</div>		
						
		<dyna:text propertyPath="real.mask[${i.count - 1}].fieldMinSize.col"  helpKey="help.jdyna.message.fieldminsize.col"
				labelKey="jsp.layout.hku.label.propertiesdefinition.fieldminsize.col" size="5" visibility="false"/>
		<spring:bind path="real.mask[${i.count - 1}].fieldMinSize.measurementUnitCol">				
		<select name="${status.expression}">
				<% for(String size : Size.getMeasurementUnits()) { %>
					<option <% if(status.getValue() != null && size.equals(status.getValue())) {%>" selected="selected" <% } %> value="<%=size%>"> <%=size%></option>	
				<% } %>
		</select>			
		</spring:bind>	
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:text propertyPath="real.mask[${i.count - 1}].fieldMinSize.row"  helpKey="help.jdyna.message.fieldminsize.row"
				labelKey="jsp.layout.hku.label.propertiesdefinition.fieldminsize.row" size="5" visibility="false"/>
		<spring:bind path="real.mask[${i.count - 1}].fieldMinSize.measurementUnitRow">				
		<select name="${status.expression}">
				<% for(String size : Size.getMeasurementUnits()) { %>
					<option <% if(status.getValue() != null && size.equals(status.getValue())) {%>" selected="selected" <% } %> value="<%=size%>"> <%=size%></option>	
				<% } %>
		</select>			
		</spring:bind>	
		<div class="dynaClear">
			&nbsp;
		</div>		
		
		

		
		
							<a class="jdynaremovebutton"
								title="<fmt:message
				key="jsp.dspace-admin.hku.jdyna-configuration.deletedynamicfield" />"
								href="<%=request.getContextPath()%>/cris/administrator/${specificPartPath}/deleteNestedFieldDefinition.htm?pDId=${subtypo.id}&parentId=${propertiesdefinition.real.id}&boxId=${boxId}&tabId=${tabId}">
							<span class="fa fa-trash" id="remove_${boxed.id}" ></span></a>
							
							
							</fieldset>

						</c:forEach></fieldset>

		<input type="hidden" id="tabId" name="tabId" value="${tabId}" />
		<input type="hidden" id="boxId" name="boxId" value="${boxId}" />
		<input class="btn btn-primary pull-right" type="submit" name="save"
			value="<fmt:message key="jsp.layout.hku.researcher.button.save" />" />
			
						<div class="btn-group dropup pull-right">
						  <button class="btn btn-default"><fmt:message key="jsp.layout.hku.researcher.button.default.option.dropdown" /></button>
  						  <button class="btn btn-default dropdown-toggle" data-toggle="dropdown">
    							<span class="caret"></span>
  						  </button>
  						  <ul class="dropdown-menu">
    						<!-- dropdown menu links -->
    						<li>
							<input class="btn btn-default col-md-12" type="submit" name="text"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.addtextnesteddynamicfield" />" />
							</li><li>
							<input class="btn btn-default col-md-12" type="submit" name="date"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.adddatenesteddynamicfield" />" />
							</li><li>
							<input class="btn btn-default col-md-12" type="submit" name="link"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.addlinknesteddynamicfield" />" />
							</li><li>
							<input class="btn btn-default col-md-12" type="submit" name="file"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.addfilenesteddynamicfield" />" />
							</li><li>	
							<input class="btn btn-default col-md-12" type="submit" name="pointerrp"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.addrppointernesteddynamicfield" />" />
							</li><li>							
							<input class="btn btn-default col-md-12" type="submit" name="pointerpj"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.addpjpointernesteddynamicfield" />" />
							</li><li>
							<input class="btn btn-default col-md-12" type="submit" name="pointerou"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.addoupointernesteddynamicfield" />" />
							</li><li>
							<input class="btn btn-default col-md-12" type="submit" name="pointerdo"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.adddopointernesteddynamicfield" />" />
							</li><li>
							<input class="btn btn-default col-md-12" type="submit" name="boolean"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.addbooleannesteddynamicfield" />" />
							</li><li>	
							<input class="btn btn-default col-md-12" type="submit" name="checkradio"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.adddropdowncheckradioboxnesteddynamicfield" />" />
							</li><li>
							<input class="btn btn-default col-md-12" type="submit" name="classificationtree"
								value="<fmt:message key="jsp.dspace-admin.hku.jdyna-configuration.addclassificationtreenesteddynamicfield" />" />
							</li>
  						</ul>	
						</div>		
						
	</form:form>


</dspace:layout>
