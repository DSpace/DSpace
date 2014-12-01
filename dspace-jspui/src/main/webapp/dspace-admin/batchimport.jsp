<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Form to upload a metadata files
--%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.List"            %>
<%@ page import="org.dspace.content.Collection"            %>

<%

	List<String> inputTypes = (List<String>)request.getAttribute("input-types");
	List<Collection> collections = (List<Collection>)request.getAttribute("collections");
	String hasErrorS = (String)request.getAttribute("has-error");
	boolean hasError = (hasErrorS==null) ? false : (Boolean.parseBoolean((String)request.getAttribute("has-error")));
	
	String uploadId = (String)request.getAttribute("uploadId");
	
    String message = (String)request.getAttribute("message");
    
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.batchimport.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin" 
               nocache="true">

    <h1><fmt:message key="jsp.dspace-admin.batchimport.title"/></h1>

	<% if (uploadId != null) { %>
		<div style="color:red">-- <fmt:message key="jsp.dspace-admin.batchimport.resume.info"/> --</div>
		<br/>
	<% } %>
			
<%
	if (hasErrorS == null){
	
	}
	else if (hasError && message!=null){
%>
	<div class="alert alert-warning"><%= message %></div>
<%  
    }
	else if (hasError && message==null){
%>
		<div class="alert alert-warning"><fmt:message key="jsp.dspace-admin.batchmetadataimport.genericerror"/></div>
<%  
	}
	else {
%>
		<div class="batchimport-info alert alert-info">
			<fmt:message key="jsp.dspace-admin.batchimport.info.success">
				<fmt:param><%= request.getContextPath() %>/mydspace</fmt:param>
			</fmt:message>
		</div>
<%  
	}
%>

    <form method="post" action="<%= request.getContextPath() %>/dspace-admin/batchimport" enctype="multipart/form-data">
	
		<div class="form-group">
			<label for="inputType"><fmt:message key="jsp.dspace-admin.batchmetadataimport.selectinputfile"/></label>
	        <select class="form-control" name="inputType" id="import-type">
	        	<option value="safupload"><fmt:message key="jsp.dspace-admin.batchimport.saf.upload"/></option>
				<option value="saf"><fmt:message key="jsp.dspace-admin.batchimport.saf.remote"/></option>
	<% 
	 		for (String inputType : inputTypes){
	%> 			
	 				<option value="<%= inputType %>"><%= inputType %></option>	
	<%
	 		}
	%>      </select>
 		</div>
 		
		<% if (uploadId != null) { %>
			<input type="hidden" name=uploadId value="<%= uploadId %>"/>
		<% } %>
		
		<div class="form-group" id="input-url" style="display:none">
			<label for="collection"><fmt:message key="jsp.dspace-admin.batchmetadataimport.selecturl"/></label><br/>
			<input type="text" name="zipurl" class="form-control"/>
        </div>
        
        <div class="form-group" id="input-file">
			<label for="file"><fmt:message key="jsp.dspace-admin.batchmetadataimport.selectfile"/></label>
            <input class="form-control" type="file" size="40" name="file"/>
        </div>
 
        <div class="form-group">
			<label for="collection">
				<fmt:message key="jsp.dspace-admin.batchmetadataimport.selectowningcollection"/>
				<span id="owning-collection-optional">&nbsp;<fmt:message key="jsp.dspace-admin.batchmetadataimport.selectowningcollection.optional"/></span>
			</label>
			<div id="owning-collection-info"><i for="collection"><fmt:message key="jsp.dspace-admin.batchmetadataimport.selectowningcollection.info"/></i></div>
            <select class="form-control" name="collection" id="owning-collection-select">
				<option value="-1"><fmt:message key="jsp.dspace-admin.batchmetadataimport.select"/></option>
 <% 
 		for (Collection collection : collections){
%> 			
 				<option value="<%= collection.getID() %>"><%= collection.getName() %></option>	
 <%
 		}
 %>           	
            </select>
        </div>
        
        <div class="form-group" id="other-collections-div" style="display:none">
			<label for="collection"><fmt:message key="jsp.dspace-admin.batchmetadataimport.selectothercollections"/></label>
            <select class="form-control" name="collections" multiple style="height:150px" id="other-collections-select">
 <% 
 		for (Collection collection : collections){
%> 			
 				<option value="<%= collection.getID() %>"><%= collection.getName() %></option>	
 <%
 		}
 %>           	
            </select>
        </div>
        
        <input class="btn btn-success" type="submit" name="submit" value="<fmt:message key="jsp.dspace-admin.general.upload"/>" />

    </form>
    
    <script>
	    $( "#import-type" ).change(function() {
	    	var index = $("#import-type").prop("selectedIndex");
	    	if (index == 1){
	    		$( "#input-file" ).hide();
	    		$( "#input-url" ).show();
	    		$( "#owning-collection-info" ).show();
	    		$( "#owning-collection-optional" ).show();
	    	}
	    	else {
	    		$( "#input-file" ).show();
	    		$( "#input-url" ).hide();
	    		$( "#owning-collection-info" ).hide();
	    		$( "#owning-collection-optional" ).hide();
	    	}
	    });
		
		$( "#owning-collection-select" ).change(function() {
	    	var index = $("#owning-collection-select").prop("selectedIndex");
	    	if (index == 0){
	    		$( "#other-collections-div" ).hide();
				$( "#other-collections-select > option" ).attr("selected",false);
	    	}
	    	else {
	    		$( "#other-collections-div" ).show();
	    	}
	    });
    </script>
    
    
</dspace:layout>