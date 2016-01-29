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
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>

<c:set var="dspace.layout.head.last" scope="request">
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/jquery/jquery-1.8.2.min.js'></script>
	<script type='text/javascript' src='<%= request.getContextPath() %>/static/js/jquery/jquery-ui-1.8.24.custom.min.js'></script>
    <script type="text/javascript"><!--

		var j = jQuery;
    j(document).ready(function()
			{
    		j('#addentity').click(function() {
    		  j('#dto').submit();
    		});
			}
    );
		-->
	</script>
    
</c:set>
<dspace:layout locbar="link" style="submission" navbar="admin" titlekey="jsp.dspace-admin.crisconfiguration">
	<h1><fmt:message key="jsp.dspace-admin.crisconfiguration"><fmt:param>${fn:toUpperCase(path)}</fmt:param></fmt:message>
		<a target="_blank" class="pull-right" href='<%=request.getContextPath()%><%=LocaleSupport.getLocalizedMessage(pageContext,
                              "help.site-admin.do")%>'><fmt:message
		key="jsp.help" /></a></h1>

	<c:if test="${!empty error}">
		<span id="errorMessage" class="alert alert-danger"><fmt:message key="jsp.layout.hku.prefix-error-code"/> <fmt:message key="${error}"/></span>
	</c:if>

	<ul class="blank-page">
		<li>
			<a href="<%=request.getContextPath()%>/cris/administrator/${path}/listTabs.htm"><fmt:message
			key="jsp.dspace-admin.hku.jdyna-configuration.listtabs" /></a>
		</li>
		<li>
			<a href="<%=request.getContextPath()%>/cris/administrator/${path}/listEditTabs.htm"><fmt:message
			key="jsp.dspace-admin.hku.jdyna-configuration.listedittabs" /></a>	
		</li>
		<li>
			<a href="<%=request.getContextPath()%>/cris/administrator/${path}/listBoxs.htm"><fmt:message
			key="jsp.dspace-admin.hku.jdyna-configuration.listboxs" /></a>	
		</li>
		<li>
			<a id="addentity" href="#"><fmt:message
			key="jsp.dspace-admin.crisconfiguration.add"><fmt:param>${fn:toUpperCase(path)}</fmt:param></fmt:message></a>	
		
		<div style="display: none; float: right;"><c:set
			var="contextPath"><%=request.getContextPath()%></c:set> <form:form 
			action="${contextPath}/cris/administrator/${path}/add.htm"
			method="post" commandName="dto">
		</form:form>
		</div>
		
		</li>
		<li>
			<a href="<%=request.getContextPath()%>/cris/administrator/${path}/list.htm"><fmt:message
			key="jsp.dspace-admin.crisconfiguration.see"><fmt:param>${fn:toUpperCase(path)}</fmt:param></fmt:message></a>
		</li>
	</ul>
</dspace:layout>
