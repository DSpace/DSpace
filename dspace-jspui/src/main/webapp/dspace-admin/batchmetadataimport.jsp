<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Form to upload a csv metadata file
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
	boolean hasError = (hasErrorS==null) ? true : (Boolean.parseBoolean((String)request.getAttribute("has-error")));
	
    String message = (String)request.getAttribute("message");
%>

<dspace:layout titlekey="jsp.dspace-admin.batchmetadataimport.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin" 
               nocache="true">

    <h1><fmt:message key="jsp.dspace-admin.batchmetadataimport.title"/></h1>

<%
	if (hasErrorS == null){
	
	}
	else if (hasError && message!=null){
%>
	<%= message %>
<%  
    }
	else if (hasError && message==null){
%>
		<fmt:message key="jsp.dspace-admin.batchmetadataimport.genericerror"/>
<%  
	}
	else {
%>
		<fmt:message key="jsp.dspace-admin.batchmetadataimport.success"/>
<%  
	}
%>

    <form method="post" enctype="multipart/form-data" action="">
	

		<p align="center">
			<fmt:message key="jsp.dspace-admin.batchmetadataimport.selectfile"/>
		</p>
		
        <p align="center">
            <input type="file" size="40" name="file"/>
        </p>
        
        <p align="center">
			<fmt:message key="jsp.dspace-admin.batchmetadataimport.selectinputfile"/>
		</p>
		
        <p align="center">
            <select name="inputType">
 <% 
 		for (String inputType : inputTypes){
%> 			
 				<option value="<%= inputType %>"><%= inputType %></option>	
 <%
 		}
 %>           	
            </select>
        </p>
        
        <p align="center">
			<fmt:message key="jsp.dspace-admin.batchmetadataimport.selectcollection"/>
		</p>
        
        <p align="center">
            <select name="collection">
 <% 
 		for (Collection collection : collections){
%> 			
 				<option value="<%= collection.getID() %>"><%= collection.getName() %></option>	
 <%
 		}
 %>           	
            </select>
        </p>
        
        <p align="center">
            <input type="submit" name="submit" value="<fmt:message key="jsp.dspace-admin.general.upload"/>" />
        </p>

    </form>
    
</dspace:layout>