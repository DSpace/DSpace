<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://displaytag.sf.net" prefix="display"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@page import="org.dspace.app.importer.ImporterException"%>

<%	
	ImporterException exc = (ImporterException) request.getAttribute("error");
	String error = "jsp.tools.import-item.error." + exc.getPluginName();
%>

<dspace:layout titlekey="jsp.tools.import-item.results.title">


	 <center>
		<fieldset><legend><fmt:message key="jsp.tools.import-item.error.legend"><fmt:param><%= exc.getPluginName()%></fmt:param></fmt:message></legend>		
			<strong><fmt:message key="<%= error %>"><fmt:param><%= exc.getLimit()%></fmt:param><fmt:param><%= exc.getTotal()%></fmt:param></fmt:message></strong>
		</fieldset>
	</center>	

<p>
<fmt:message key="jsp.tools.import-item.results.endpage">
	<fmt:param><%= request.getContextPath() %>/mydspace</fmt:param>
</fmt:message>
</p>
</dspace:layout>
