<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
	String[] plugins = (String[]) request.getAttribute("plugins");
%>
<dspace:layout titlekey="jsp.tools.import-item.title">

<h2><fmt:message key="jsp.tools.import-item.title" /></h2>

   	<form enctype="multipart/form-data" action="<%=request.getContextPath()%>/tools/import" method="post" class="import" id="import">
	   	<label for="importdata">
	   	  <fmt:message key="jsp.tools.import-item.importdata" />
	   	</label>
   	<textarea rows="10" cols="50" id="importdata" name="importdata"></textarea>
   	
	   	<br /><br />
	   	
	   	<label for="importdata"><fmt:message key="jsp.tools.import-item.importdatafile" /></label>
		<input type="file" size="40" name="importdatafile" id="importdatafile" />
	   	
	   	<br /><br />
	   	
   	<label for="format"><fmt:message key="jsp.tools.import-item.format" /></label>
   	<select name="format" id="format">
   		<option value=""><fmt:message key="jsp.tools.import-item.plugin.firstoption" /></option>
	<c:forEach var="plugin" items="${plugins}">
		<option value="${plugin}"><fmt:message key="jsp.tools.import-item.plugin.${plugin}" /></option>
	</c:forEach> 
   	</select>
	   	
	   	<br /><br />
	   	
   	<input type="submit" value="<fmt:message key="jsp.tools.import-item.submit" />"/>	
	</form>
	
	<hr />
	
	<h3><fmt:message key="jsp.tools.import-item.help" /></h3>
	<fmt:message key="jsp.tools.import-item.help-summary" />
	<c:forEach var="plugin" items="${plugins}">
		<fmt:message key="jsp.tools.import-item.help.plugin.${plugin}" />
	</c:forEach> 


<script type="text/javascript">
var j = jQuery.noConflict();
j(document).ready(
		function()
		{
			j('#import').submit(function() {
				if (j('#format').val().length == 0) {
					 alert('<fmt:message key="jsp.tools.import-item.help.plugin.submit" />');
					 return false;
				}
				return true;
			});
		}
);
</script>		
</dspace:layout>

