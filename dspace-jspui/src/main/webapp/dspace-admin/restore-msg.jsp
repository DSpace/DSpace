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

	String snapshotname = (String)request.getAttribute("snapshotname");
	String message = (String)request.getAttribute("message");
	String hasErrorS = (String)request.getAttribute("has-error");
	boolean hasError = (hasErrorS==null) ? true : (Boolean.parseBoolean((String)request.getAttribute("has-error")));
%>

<dspace:layout style="submission" titlekey="jsp.dspace-admin.restore.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin" 
               nocache="true">

    <h1><fmt:message key="jsp.dspace-admin.restore.title"/></h1>
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
		<div class="alert alert-warning"><fmt:message key="jsp.dspace-admin.batchmetadataimport.genericerror"/></div>
<%  
	}
	else {
%>
		<div class="alert alert-info"><fmt:message key="jsp.dspace-admin.batchmetadataimport.success"/></div>
    		<label value="<%= snapshotname %>">Restoring with Snapshot Name: <%= snapshotname %></label>	
<%  
	}
%>
    
</dspace:layout>
