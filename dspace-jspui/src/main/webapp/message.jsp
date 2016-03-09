<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>
<c:set var="dspace.layout.head.last" scope="request">
<style>

a {
	color: #2373ad;
}

</style>
</c:set>
<dspace:layout locbar="nolink" title="Waiting for email">

		<div class="alert alert-info">
			<fmt:message key="jsp.references.email.info.success"/>
		</div>
			
</dspace:layout>
