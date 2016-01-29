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

<dspace:layout locbar="link" style="submission" navbar="admin"  titlekey="jsp.dspace-admin.cris">
			<h1><fmt:message key="jsp.dspace-admin.cris" />
				<a class="pull-right" href='<%=request.getContextPath()%><%=LocaleSupport.getLocalizedMessage(pageContext,
                                "help.site-admin.cris")%>'><fmt:message
				key="jsp.help" /></a></h1>


	<c:if test="${!empty error}">
		<span id="errorMessage" class="alert alert-danger"><fmt:message key="jsp.layout.hku.prefix-error-code"/> <fmt:message key="${error}"/></span>
	</c:if>

	<ul class="blank-page">
		<li>
			<a href="<%=request.getContextPath()%>/cris/administrator/rp/index.htm"><fmt:message
				key="jsp.dspace-admin.cris.researcherpage.index" /></a>		
		</li>
		<li>
			<a href="<%=request.getContextPath()%>/cris/administrator/project/index.htm"><fmt:message
				key="jsp.dspace-admin.cris.project.index" /></a>	
		</li>
		<li>
			<a	href="<%=request.getContextPath()%>/cris/administrator/ou/index.htm"><fmt:message
				key="jsp.dspace-admin.cris.organizationunit.index" /></a>	
		</li>
		<li>
			<a href="<%=request.getContextPath()%>/cris/administrator/do/index.htm"><fmt:message
				key="jsp.dspace-admin.cris.dynobj.index" /></a>	
<%--		<ul>
		<c:forEach var="researchobject" items="${researchobjects}">
		
			<li>
				<div style="padding: 0; margin: 0 10px;"><a
					href="<%=request.getContextPath()%>/cris/administrator/${researchobject.shortName}/index.htm"><fmt:message
					key="jsp.dspace-admin.cris.research.index"><fmt:param>${researchobject.label}</fmt:param></fmt:message> </a></div>	
				
			</li>
		
		</c:forEach>
		</ul> --%>
		
		</li>
				
	 		
	<li>
		<a
			href="<%=request.getContextPath()%>/cris/administrator/export.htm"><fmt:message
			key="jsp.layout.navbar-hku.export.researcher" /></a>
		</li>
	
		<li>
		<a
			href="<%=request.getContextPath()%>/cris/administrator/import.htm"><fmt:message
			key="jsp.layout.navbar-hku.import.researcher" /></a>
		</li>
	
	<li>
		<a href="<%=request.getContextPath()%>/cris/administrator/webservices/authorizations.htm"><fmt:message
			key="jsp.layout.navbar-admin.webservices" /></a>
	</li>
			
	</ul>
</dspace:layout>
