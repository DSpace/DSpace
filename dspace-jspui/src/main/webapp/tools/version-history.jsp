<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Version history table with functionalities
  -
  - Attributes:
   --%>

<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="org.dspace.core.Context"%>
<%@page import="org.dspace.content.Item"%>
<%@page import="org.dspace.eperson.EPerson"%>
<%@page import="org.dspace.versioning.Version"%>
<%@page import="org.dspace.app.webui.util.VersionUtil"%>
<%@page import="org.dspace.versioning.VersionHistory"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%
    Integer itemID = (Integer)request.getAttribute("itemID");
	String versionID = (String)request.getAttribute("versionID");
	Item item = (Item) request.getAttribute("item");
	Boolean removeok = UIUtil.getBoolParameter(request, "delete");
	Context context = UIUtil.obtainContext(request);
	
	
	
	request.setAttribute("LanguageSwitch", "hide");
%>
<c:set var="messagedeleteconfirmtitle"><fmt:message key="jsp.version.history.delete.warning.head1"/></c:set>
<c:set var="messagedeleteconfirmpar1"><fmt:message key="jsp.version.history.delete.warning.para1"/></c:set>
<c:set var="messagedeleteconfirmpar2"><fmt:message key="jsp.version.history.delete.warning.para2"/></c:set>
<c:set var="dspace.layout.head.last" scope="request">
<script type="text/javascript">
var j = jQuery.noConflict();
j(document).ready(function() {
	j("#fake_submit_delete").click(function() {
		var checked = j('.remove').is(':checked');
		if(checked) {
			var oksubmitdelete = confirm('${messagedeleteconfirmtitle}${messagedeleteconfirmpar1}${messagedeleteconfirmpar2}');
			if(oksubmitdelete==true) {
				j("#submit_delete").click();
			}
		}
	});	
});
</script>
</c:set>
<dspace:layout titlekey="jsp.version.history.title">
	
    <h1><fmt:message key="jsp.dspace-admin.version-summary.heading"/></h1>
		
	<% if(removeok) { %><fmt:message key="jsp.dspace-admin.version-summary.heading"/><% } %>
 <form action="<%= request.getContextPath() %>/tools/history" method="post">
	<input type="hidden" name="itemID" value="<%= itemID %>" />
	<input type="hidden" name="versionID" value="<%= versionID %>" />                
    <%-- Versioning table --%>
<%
                
	VersionHistory history = VersionUtil.retrieveVersionHistory(context, item);
						 
%>
	<div id="versionHistory">
	<h2><fmt:message key="jsp.version.history.head2" /></h2>
	
	
	<table class="miscTable">
		<tr>
			<th id="t0"></th>
			<th id="t1" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column1"/></th>
			<th 			
				id="t2" class="oddRowOddCol"><fmt:message key="jsp.version.history.column2"/></th>
			<th 
				id="t3" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column3"/></th>
			<th 				
				id="t4" class="oddRowOddCol"><fmt:message key="jsp.version.history.column4"/></th>
			<th 
				id="t5" class="oddRowEvenCol"><fmt:message key="jsp.version.history.column5"/> </th>
			<th 
				id="t6" class="oddRowOddCol"><fmt:message key="jsp.version.history.column6"/> </th>
		</tr>
		
		<% for(Version versRow : history.getVersions()) {  
		
			EPerson versRowPerson = versRow.getEperson();
			String[] identifierPath = VersionUtil.addItemIdentifier(item, versRow);

		%>	
		<tr>
			<td headers="t0"><input type="checkbox" class="remove" name="remove" value="<%=versRow.getVersionId()%>"/></td>			
			<td headers="t1" class="oddRowEvenCol"><%= versRow.getVersionNumber() %></td>
			<td headers="t2" class="oddRowOddCol"><a href="<%= request.getContextPath() + identifierPath[0] %>"><%= identifierPath[1] %></a><%= item.getID()==versRow.getItemID()?"*":""%></td>
			<td headers="t3" class="oddRowEvenCol"><a href="mailto:<%= versRowPerson.getEmail() %>"><%=versRowPerson.getFullName() %></a></td>
			<td headers="t4" class="oddRowOddCol"><%= versRow.getVersionDate() %></td>
			<td headers="t5" class="oddRowEvenCol"><%= versRow.getSummary() %></td>
			<td headers="t6" class="oddRowOddCol"><a href="<%= request.getContextPath() %>/tools/version?itemID=<%= versRow.getItemID()%>&versionID=<%= versRow.getVersionId() %>&submit_update_version"><fmt:message key="jsp.version.history.update"/></a></td>
		</tr>
		<% } %>
		<tr>
			<td></td>
			<td colspan="2"><input type="button" id="fake_submit_delete" value="<fmt:message key="jsp.version.history.delete"/>"/> <input type="submit" value="<fmt:message key="jsp.version.history.return"/>" name="submit_cancel"/></td>
			<td colspan="5"></td>
		</tr>
	</table>
	<p><fmt:message key="jsp.version.history.legend"/></p>
	</div>
    <div style="display: none">
    	<input type="submit" value="<fmt:message key="jsp.version.history.delete"/>" name="submit_delete" id="submit_delete"/>
    </div>    
</form>


</dspace:layout>
