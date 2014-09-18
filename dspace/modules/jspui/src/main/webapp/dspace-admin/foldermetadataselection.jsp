<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Form to upload a csv metadata file
--%>

<%@page import="org.dspace.folderimport.constants.FolderMetadataImportConstants"%>
<%@page import="java.util.Map"%>
<%@ page contentType="text/html;charset=UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>

<%@ page import="java.util.List"%>
<%@ page import="org.dspace.content.Collection"%>

<%
	
	Map<Long, String> userDataSelection = (Map<Long, String>) request.getSession().getAttribute(FolderMetadataImportConstants.USER_DATA_READBLE_KEY_ROOT);
	
	String hasErrorS = String.valueOf(request.getAttribute("has-error"));
	boolean hasError = (request.getAttribute("has-error") != null ? Boolean.valueOf(request.getAttribute("has-error").toString()) : Boolean.FALSE);
	String message = (String)request.getAttribute("message");

%>

<dspace:layout style="submission"
	titlekey="jsp.dspace-admin.foldermetadataimport.title" navbar="admin"
	locbar="link" parenttitlekey="jsp.administer"
	parentlink="/dspace-admin" nocache="true">

	<h1>
		<fmt:message key="jsp.dspace-admin.foldermetadataimport.title" />
	</h1>

	<%
	if (hasError && message!=null){
	%>
		<div class="alert alert-warning">
			<fmt:message key="<%= message %>"></fmt:message>
		</div>
	<%  
    }
	%>
	<form method="post" action="<%= request.getContextPath() %>/dspace-admin/foldermetadataimport">
		
		
		<%
			if(userDataSelection != null) 
			{
		%>
			<div class="form-group">
				<label for="folderSelection">
					<fmt:message key="jsp.dspace-admin.foldermetadataselection.folder" />
				</label> 
				
				
				
				<select class="form-control" name="selectedFolder" id="folderSelection">
				<% 
			 		for (Map.Entry<Long, String> folder : userDataSelection.entrySet()){
				%>
						<option value="<%= folder.getKey() %>"><%= folder.getValue() %></option>
				<%
				 		}
				 %>
				</select>
			</div>
			
		<%
			}
		%>

		<%
			if(userDataSelection != null) 
			{
		%>
			<input class="btn btn-primary" type="submit" name="submit_selection"
				value="<fmt:message key="jsp.search.general.next"/>" />
		<%
			}
		%>

	</form>

</dspace:layout>