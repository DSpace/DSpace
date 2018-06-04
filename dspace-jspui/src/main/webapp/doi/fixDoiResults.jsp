<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="org.dspace.app.webui.servlet.DoiFixUtilityCheckerServlet"%>
<%@page import="org.dspace.content.Item"%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@page import="java.util.Map"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>

<%
	Map<String, Item[]> results = (Map<String, Item[]>)request.getAttribute("mapitems");
	Map<Integer, String> doi2items = (Map<Integer, String>)request.getAttribute("doi2items");
%>
<dspace:layout locbar="link" navbar="admin" style="submission" titlekey="jsp.dspace-admin.doi.fix">
<c:set var="messageproposeddoi"><fmt:message key="jsp.dspace-admin.doi.table.header.proposeddoi"/></c:set>
<script type="text/javascript">
<!--
var j = jQuery.noConflict();
//-->
</script>
<table width="95%">
    <tr>      
      <td align="left"><h1><fmt:message key="jsp.dspace-admin.doi.fix"/></h1>   
      </td>
    </tr>
</table>
<p><fmt:message key="jsp.dspace-admin.doi.fix.general-description" /></p>

<% if(results!=null) { 

for(String type : results.keySet()) {
	
	Item[] items = results.get(type);
	
	String key = "jsp.dspace-admin.doi.fix."+type;
	
	%>
	<br/>	
	<fieldset><legend><fmt:message key="<%= key%>"/></legend>
	
	<%
		if(items!=null && items.length>0) {
	%>

<form method="post" action="<%= request.getContextPath() %>/dspace-admin/doifix/<%= type%>" name="itemform_<%= type%>">			
<dspace:itemlist items="<%= items %>" itemStart="1" radioButton="false" inputName="builddoi"/>
       
<div style="display: none;">       
	<input id="submitallreal" type="submit" name="submit" value="<%=DoiFixUtilityCheckerServlet.DOI_ALL%>" />
	<input id="submitanyreal" type="submit" name="submit" value="<%=DoiFixUtilityCheckerServlet.DOI_ANY%>" />		
</div>

	<input id="submitall" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.all"><fmt:param value="<%= items.length %>"></fmt:param></fmt:message>" />
	<input id="submitany" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.any"/>" />
	
</form>				
	<% } else { %>	
		<fmt:message key="jsp.search.doi.form.button.noitemtofix.found.for"><fmt:param value="<%= type %>"></fmt:param></fmt:message>
	<% } %>
</fieldset>	
<% } %>


<script type="text/javascript">

j(document).ready(function() {
	
	
	<% if(results!=null) { 

		for(String type : results.keySet()) {		%>
			var ctrl_<%= type%> = j('<th/>').attr({id:'t5'}).html('${messageproposeddoi}');		
			j('form[name="itemform_<%= type%>"] table.miscTable tbody tr:first').append(ctrl_<%= type%>);;
	<%}}%>
});


var maps = new Object();

<% 

for(Integer ss : doi2items.keySet()) {		
%>
	maps[<%= ss%>] = <%= "'"+doi2items.get(ss)+"';"%>
<%		
}	
%>

j('input[name="builddoi"]').each(function(index) {
	
	var ctrl0 = j('<td/>').attr({id:'tddoi_'+j(this).val()});		
	var ctrl4 = j('<span/>').attr({id:'spandoi_'+j(this).val()}).addClass('spandoi').html(maps[j(this).val()]);
		
	
	
	j(this).parent().parent().append(ctrl0);		
	j("#tddoi_"+j(this).val()).append(ctrl4);
	
});


	j('.submitbutton').click(
		function() {
			j('#' + (j(this).attr('id')) + "real").click();
		}		
	);

	j(window).keydown(function(event) {
		if (event.keyCode == 13) {
			event.preventDefault();
			return false;
		}
	});
</script>	
<% } else { %>	
	<fmt:message key="jsp.search.doi.form.button.noitemtofix.found"/>
<% } %>



</dspace:layout>