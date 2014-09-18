<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Form to upload a csv metadata file
--%>

<%@page import="java.io.File"%>
<%@page import="org.dspace.folderimport.dto.ErrorImportRegistry"%>
<%@page import="org.dspace.folderimport.constants.FolderMetadataImportConstants"%>
<%@page import="java.util.Map"%>
<%@ page contentType="text/html;charset=UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace"%>

<%@ page import="java.util.List"%>
<%@ page import="org.dspace.content.Collection"%>

<%
	List<ErrorImportRegistry> parentFolderMapping = (List<ErrorImportRegistry>) request.getAttribute(FolderMetadataImportConstants.ITEMS_WITH_ERROR_ON_IMPORT_KEY);
	String message = (String)request.getAttribute("message");

%>

<div id="errorsContainer">

		<%
			if (message != null){
			%>
				<div class="alert alert-warning">
					<fmt:message key="<%= message %>"></fmt:message>
				</div>
			<%  
	    	}
		%>
		
		<% if(parentFolderMapping != null && !parentFolderMapping.isEmpty())  {%>
		
		         
   	      		<table style="width: 100%" class="table">
   	      			<tr>
   	      				<th><fmt:message key="jsp.dspace-admin.foldermetadataerror.table.number" /> </th>
   	      				<th width="30%"><fmt:message key="jsp.dspace-admin.foldermetadataerror.table.item" /> </th>
   	      				<th><fmt:message key="jsp.dspace-admin.foldermetadataerror.table.type" /> </th>
   	      				<th><fmt:message key="jsp.dspace-admin.foldermetadataerror.table.errordescription"/> </th>
   	      				<th><fmt:message key="jsp.dspace-admin.foldermetadataerror.table.files" /> </th>
   	      			</tr>
		         <% 
		         	int iterationNumber = 0;
		         	for(ErrorImportRegistry currentValue :  parentFolderMapping)  
		         	{ 
		         %>
		         	
       	      			<tr>
       	      				<td>
       	      					<span><%= ++iterationNumber %></span>
       	      				</td>
       	      				<td width="30%">
		         				<%= currentValue.getTitle() %>
       	      				</td>
       	      				<td>
       	      					<fmt:message key="<%= currentValue.getImportErrorType().getI18nKey() %>"></fmt:message>
       	      				</td>
       	      				<td>
       	      					<%
       	      						if(currentValue.getErrorsDescription() != null && !currentValue.getErrorsDescription().isEmpty())
       	      						{
       	      							for(String errorDescription : currentValue.getErrorsDescription())
       	      							{
       	      					%>
											<span><%= errorDescription %></span>
											<br/>       	      							
       	      					<%
       	      							}
       	      						}
       	      					%>
       	      				</td>
       	      				<td>
		         				<% 	int i = 0;
		         					for(Map.Entry<Long, File> currentFile :  currentValue.getItemFiles().entrySet())  { %>
		         						<em>
			        						<% i++; %>
			        						<a href="<%= request.getContextPath() + "/dspace-admin/foldermetadataerror?item=" + currentValue.getInternalIdentifer() + "&file=" + currentFile.getKey() %>"><%= currentFile.getValue().getName() %></a>
			        						<% if(i < currentValue.getItemFiles().size())  { %>
			        							<br/>
			      							<% } %>
		         						</em>
		         				
		         				<% } %>
       	      				</td>
       	      			</tr>
		         
			 	<% } %>
      		</table>
			 
		<% } %>
	

</div>
