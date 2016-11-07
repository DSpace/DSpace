<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Page representing an authorization error
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core"
    prefix="c" %>
    
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<fmt:message key="jsp.explore.title" var="title">
<fmt:param>${handle}</fmt:param><fmt:param>${filename}</fmt:param>
</fmt:message>
<dspace:layout title="${title}">
<h3><fmt:message key="jsp.explore.error"/></h3>
	<div class="row">
		<div class="col-md-12">
			<a href="<%= request.getContextPath()%>/handle/${handle}" class="btn btn-default">
				<fmt:message key="jsp.explore.back" ><fmt:param>${itemTitle}</fmt:param></fmt:message>
			</a>
		
		</div>
	</div>

	
</dspace:layout>
