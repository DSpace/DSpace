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
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@page import="org.dspace.app.cris.model.ws.User"%>


<c:set var="dspace.layout.head.last" scope="request">
	
<style type="text/css">
.error {
	background-color: yellow;
	color: red;
	float: none;
	padding-left: 0.5em;
	vertical-align: top;
}

#enabledDiv {
	padding-top: 1em;
}
</style>	
	
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/jquery.validate.min.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/jquery.showPassword.min.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/additional-methods.min.js"></script>
		
	<script type="text/javascript">
	var j = jQuery.noConflict();
	j(document).ready(function() {
              
        j('#user').validate({
            rules: {
            	"specialAuth.fromIP": {
                    required: true,
                    ipv4: true
                },
                "specialAuth.toIP": {
                    required: false,
                    ipv4: true
                }
            }
        });
        
        j(':password').showPassword();
            
        j(".choosetype").click(function() {
        	if(j("#typeDef1").is(":checked")) {
        		j("#tokenDiv").hide();
        		j("#token").val("");
        		j("#fromIP").val("");
        		j("#toIP").val("");
        		j("#normalDiv").show();
        	}
        	else {
        		j("#tokenDiv").show();        		
        		j("#normalDiv").hide();
        		j("#username").val("");
        		j("#password").val("");
        	}
        	
        });
        
    	if(j("#typeDef1").is(":checked")) {
    		j("#tokenDiv").hide();
    		j("#normalDiv").show();
    	}
    	else {
    		j("#tokenDiv").show();
    		j("#normalDiv").hide();
    	}
    	
	});
    </script>
    
    
</c:set>
<dspace:layout locbar="link" navbar="admin" titlekey="jsp.dspace-admin.userws">

	<table width="95%">
		<tr>
			<td align="left">
			<h1><fmt:message key="jsp.dspace-admin.userws" /></h1>
			</td>
			<td align="right" class="standard"><a target="_blank"
				href='<%=LocaleSupport.getLocalizedMessage(pageContext,
                                "help.site-admin.userws")%>'><fmt:message
				key="jsp.help" /></a></td>
		</tr>
	</table>

	<form:form commandName="user" method="post">
	
		<spring:bind path="user.*">
		<c:if test="${!empty status.errorMessages}">
			<div id="errorMessages">
		</c:if>
		<c:forEach items="${status.errorMessages}" var="error">
			<span class="errorMessage"><fmt:message
				key="jsp.layout.hku.prefix-error-code" /> ${error}</span>
			<br />
		</c:forEach>
		<c:if test="${!empty status.errorMessages}">
			<div id="errorMessages">
		</c:if>
		</spring:bind>
	
		<label for="typeDef1"><fmt:message key="jsp.layout.hku.ws.form.type" /></label>
		
		<form:radiobutton cssClass="choosetype" path="typeDef" value="<%= User.TYPENORMAL %>"/>Normal Authentication
		<form:radiobutton cssClass="choosetype" path="typeDef" value="<%= User.TYPESPECIAL %>"/>Token Authentication
				
		<div id="normalDiv">
			<label for="normalAuth.username"><fmt:message key="jsp.layout.hku.ws.form.user" /></label>
			<form:input cssClass="required" path="normalAuth.username" id="username"/>
			<label for="normalAuth.password"><fmt:message key="jsp.layout.hku.ws.form.pass" /></label>
			<form:password showPassword="true" cssClass="required" path="normalAuth.password" id="password"/>			
		</div>
		<div id="tokenDiv">
			<fmt:message key="jsp.layout.hku.ws.form.tokeninfo" />		
			<label for="specialAuth.token"><fmt:message key="jsp.layout.hku.ws.form.token" /></label>
			<form:input cssClass="required" path="specialAuth.token" id="token"/>
			<div class="ipRange">From:<form:input path="specialAuth.fromIP" id="fromIP"/> &nbsp; To:<form:input path="specialAuth.toIP" id="toIP"/></div>					
		</div>
			
		<div id="enabledDiv">	
			<label for="enabled"><fmt:message key="jsp.layout.hku.ws.form.enabled" /></label><form:checkbox path="enabled"/>
		</div>

		<div id="skipCheckFieldsOnBuildingResponseDiv">	
			<label for="showHiddenMetadata"><fmt:message key="jsp.layout.hku.ws.form.skipCheckFieldsOnBuildingResponse" /></label><form:checkbox path="showHiddenMetadata"/>
		</div>
		<br/>
		<div id="criterias"> 
		<c:forEach items="${user.criteria}" var="crit" varStatus="rowCounter">			
			<div id="criteriadiv_${rowCounter.count-1}" class="singlecriteriadiv">				
				<span id="criteriatypespan"><fmt:message key="jsp.layout.hku.ws.form.criteriatype.${user.criteria[rowCounter.count-1].criteria}" /></span>
				<form:checkbox path="criteria[${rowCounter.count-1}].enabled" id="enabled_${rowCounter.count-1}"/>							
				<br/><label for="criteria[${rowCounter.count-1}].filter"><fmt:message key="jsp.layout.hku.ws.form.criteria" /></label><br/><form:textarea cols="60" path="criteria[${rowCounter.count-1}].filter" id="filter_${rowCounter.count-1}"/>
			</div>
		</c:forEach>		
		</div>	
			
		<input type="submit" class="btn btn-primary" value="<fmt:message key="jsp.layout.hku.researcher.button.save" />" />
		
	</form:form>				 
</dspace:layout>
