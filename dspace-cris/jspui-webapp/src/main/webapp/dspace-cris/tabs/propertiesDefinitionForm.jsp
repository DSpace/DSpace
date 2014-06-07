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
<%@page import="it.cilea.osd.jdyna.widget.Size"%>
<%@page import="java.util.LinkedList"%>
<%@page import="java.util.List"%>
<%@page import="it.cilea.osd.jdyna.model.AccessLevelConstants"%>
<%@page import="it.cilea.osd.jdyna.utils.SimpleSelectableObject"%>
<%@page import="org.dspace.app.cris.model.jdyna.DynamicObjectType"%>
<%@page import="org.dspace.app.cris.model.CrisConstants"%>
<%@ taglib uri="jdynatags" prefix="dyna" %>

<dspace:layout locbar="link" navbar="admin"
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
			key="jsp.layout.hku.label.propertiesdefinition.rendering.${propertiesdefinition.rendering.triview}" /></legend> 


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

		<c:if test="${empty renderingparent}">
								
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
				
		<dyna:boolean propertyPath="propertiesdefinition.real.mandatory"
				labelKey="jsp.layout.hku.label.propertiesdefinition.mandatory" helpKey="help.jdyna.message.mandatory"/>
		
		<div class="dynaClear">
			&nbsp
		</div>
				
		</c:if>
				
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
		
		<dyna:text propertyPath="propertiesdefinition.real.labelMinSize"  helpKey="help.jdyna.message.labelMinSize"
				labelKey="jsp.layout.hku.label.propertiesdefinition.labelMinSize" size="5" visibility="false"/>
		<spring:bind path="real.labelMinSizeUnit">				
		<select name="${status.expression}">
				<% for(String size : Size.getMeasurementUnits()) { %>
					<option <% if(status.getValue() != null && size.equals(status.getValue())) {%>" selected="selected" <% } %> value="<%=size%>"> <%=size%></option>	
				<% } %>
		</select>			
		</spring:bind>	
		<div class="dynaClear">
			&nbsp;
		</div>		
						
		<dyna:text propertyPath="propertiesdefinition.real.fieldMinSize.col"  helpKey="help.jdyna.message.fieldminsize.col"
				labelKey="jsp.layout.hku.label.propertiesdefinition.fieldminsize.col" size="5" visibility="false"/>
		
		<spring:bind path="real.fieldMinSize.measurementUnitCol">				
		<select name="${status.expression}">
				<% for(String size : Size.getMeasurementUnits()) { %>
					<option <% if(status.getValue() != null && size.equals(status.getValue())) {%>" selected="selected" <% } %> value="<%=size%>"> <%=size%></option>	
				<% } %>
		</select>			
		</spring:bind>	
		<div class="dynaClear">
			&nbsp;
		</div>
		
		<dyna:text propertyPath="propertiesdefinition.real.fieldMinSize.row"  helpKey="help.jdyna.message.fieldminsize.row"
				labelKey="jsp.layout.hku.label.propertiesdefinition.fieldminsize.row" size="5" visibility="false"/>
		<spring:bind path="real.fieldMinSize.measurementUnitRow">				
		<select name="${status.expression}">
				<% for(String size : Size.getMeasurementUnits()) { %>
					<option <% if(status.getValue() != null && size.equals(status.getValue())) {%>" selected="selected" <% } %> value="<%=size%>"> <%=size%></option>	
				<% } %>
		</select>			
		</spring:bind>	
		<div class="dynaClear">
			&nbsp;
		</div>				
		</fieldset>
		
		<c:choose>

			<c:when test="${propertiesdefinition.rendering.triview == 'testo'}">
				<fieldset>
					<dyna:text visibility="false" propertyPath="real.rendering.dimensione.col"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.text.size" helpKey="help.jdyna.message.rendering.text.size"/>
					<div class="dynaClear">
					&nbsp;
					</div>			
					<dyna:text visibility="false" propertyPath="real.rendering.regex"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.text.regex" helpKey="help.jdyna.message.rendering.text.regex"/>
											
				</fieldset>
			</c:when>
			<c:otherwise>
				<c:choose>
					<c:when
						test="${propertiesdefinition.rendering.triview eq 'calendar'}">
						<fieldset>
						<dyna:text visibility="false" propertyPath="real.rendering.minYear"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.calendar.min" helpKey="help.jdyna.message.rendering.calendar.min"/>

						<dyna:text visibility="false" propertyPath="real.rendering.maxYear"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.calendar.max" helpKey="help.jdyna.message.rendering.calendar.max"/>
						</fieldset>		
					</c:when>
					<c:otherwise>
					<c:choose>
					<c:when
						test="${propertiesdefinition.rendering.triview eq 'link'}">
						<fieldset>
						<dyna:text visibility="false" propertyPath="real.rendering.labelHeaderLabel"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.link.labelHeaderLabel" helpKey="help.jdyna.message.rendering.link.labelHeaderLabel"/>

						<dyna:text visibility="false" propertyPath="real.rendering.labelHeaderURL"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.link.labelHeaderURL" helpKey="help.jdyna.message.rendering.link.labelHeaderURL"/>
						</fieldset>
					</c:when>
					<c:otherwise>
					<c:choose>
					<c:when test="${propertiesdefinition.rendering.triview eq 'file'}">
						<fieldset>
						<dyna:boolean propertyPath="real.rendering.showPreview"
							labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.file.showpreview" helpKey="help.jdyna.message.rendering.file.showpreview"/>
						<div class="dynaClear">
							&nbsp;
						</div>
						<dyna:text visibility="false" propertyPath="real.rendering.fileDescription"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.file.fileDescription" helpKey="help.jdyna.message.rendering.file.description"/>
						<div class="dynaClear">
							&nbsp;
						</div>
						<dyna:text visibility="false" propertyPath="real.rendering.labelAnchor"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.file.labelAnchor" helpKey="help.jdyna.message.rendering.file.labelAnchor"/>
						<div class="dynaClear">
							&nbsp;
						</div>						
						<dyna:boolean propertyPath="real.rendering.useInStatistics"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.file.useInStatistics" helpKey="help.jdyna.message.rendering.file.useInStatistics"/>		
								
						</fieldset>											
					</c:when>
					<c:otherwise>
						<c:choose>
					<c:when test="${propertiesdefinition.rendering.triview eq 'pointer'}">
						<fieldset>
						<legend><fmt:message key="jsp.dspace-admin.cris.jdyna.pointer.${propertiesdefinition.rendering.valoreClass.simpleName}" /></legend>
						<dyna:textarea toolbar="nessuna" propertyPath="real.rendering.display"
							rows="4" cols="60"
							labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.display" 
							helpKey="help.jdyna.message.rendering.pointer.display"/>
						<div class="dynaClear">
							&nbsp;
						</div>

						<dyna:text propertyPath="real.rendering.filtro"  visibility="false"
							labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.filter" helpKey="help.jdyna.message.rendering.pointer.filter"/>						

						<div class="dynaClear">
							&nbsp;
						</div>
												
						<c:if test="${dyna:instanceOf(propertiesdefinition.rendering,'org.dspace.app.cris.model.jdyna.widget.WidgetPointerDO')}">
							
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
							<dyna:select propertyPath="real.rendering.filterExtended" labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.filterextended" collection="<%= types %>"/>												
						</c:if>
						
																
						<div class="dynaClear">
							&nbsp;
						</div>
						<dyna:text visibility="false" propertyPath="real.rendering.indexName"
								labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.indexname" helpKey="help.jdyna.message.rendering.pointer.indexname"/>
						<div class="dynaClear">
						&nbsp;
						</div>								
						<dyna:text propertyPath="real.rendering.urlPath"  visibility="false"
							labelKey="jsp.layout.hku.label.propertiesdefinition.rendering.pointer.path" helpKey="help.jdyna.message.rendering.pointer.path"/>						
						</fieldset>											
					</c:when>
					<c:otherwise>
					<%-- nothing --%>
					</c:otherwise>
					</c:choose>
					</c:otherwise>
					</c:choose>
					</c:otherwise>
					</c:choose>
					</c:otherwise>
				</c:choose>
				
			</c:otherwise>
		</c:choose>

		<input type="hidden" id="tabId" name="tabId" value="${tabId}" />
		<input type="hidden" id="boxId" name="boxId" value="${boxId}" />
		<input type="submit" name="save"
			value="<fmt:message key="jsp.layout.hku.researcher.button.save" />" />

	</form:form>


</dspace:layout>
