<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@page import="java.util.Map"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<dspace:layout locbar="link" navbar="admin" style="submission" titlekey="jsp.dspace-admin.doi">
<table width="95%">
    <tr>      
      <td align="left"><h1><fmt:message key="jsp.dspace-admin.doi"/></h1>   
      </td>
    </tr>
</table>
<p><fmt:message key="jsp.dspace-admin.doi.general-description" /></p>
<ul>
<%
	
    Map<String, Integer> doiSignature = (Map<String, Integer>)request.getAttribute("results");
	Boolean showFixLink = (Boolean)request.getAttribute("showFixLink");
	
	for(String keymap : doiSignature.keySet()) { 
	
		String key = "jsp.layout.hku.tool.criteria.doi."+keymap;
		Integer value = doiSignature.get(keymap); 
	%>
						
		<c:set var="messageparam0"><fmt:message key='<%= key%>'/></c:set>	

		<li>
		<% if (value > 0) { %>
			<a href="<%= request.getContextPath() %>/dspace-admin/doifactory/<%= keymap %>?start=0">
		<% } %>	
			<fmt:message key="jsp.layout.hku.tool.link.doi">
				<fmt:param value="${messageparam0}"></fmt:param>
				<fmt:param value="<%= value %>"></fmt:param>
			</fmt:message>
		<% if (value > 0) { %>	 
			</a>
		<% } %>	
	<% } %>
	
</ul>
	
<ul>
<% if(showFixLink) { %>
<li>
	<a href="<%= request.getContextPath() %>/dspace-admin/doifix"><fmt:message key="jsp.layout.hku.tool.link.doi.fixutility"/></a>	 
</li>
<% } %>
<li>
	<a href="<%= request.getContextPath() %>/dspace-admin/doipendings"><fmt:message key="jsp.layout.hku.tool.link.doi.pendings"/></a>
</li>
<li>
	<a href="<%= request.getContextPath() %>/dspace-admin/doiqueued"><fmt:message key="jsp.layout.hku.tool.link.doi.queued"/></a>
</li>
</ul> 
	
</dspace:layout>