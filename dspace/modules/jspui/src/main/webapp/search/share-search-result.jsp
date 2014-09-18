<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%
	boolean hasError = (request.getAttribute("has-error") != null ? Boolean.valueOf(request.getAttribute("has-error").toString()) : Boolean.FALSE);
	String message = (String) request.getAttribute("message");

%>

<%
if (hasError && message != null){
%>
	<div class="alert alert-warning">
		<fmt:message key="<%= message %>"></fmt:message>
	</div>
<%  
   }
%>

<%
if (!hasError && message != null){
%>
	<div class="alert alert-info">
		<fmt:message key="<%= message %>"></fmt:message>
	</div>
<%  
}
%>