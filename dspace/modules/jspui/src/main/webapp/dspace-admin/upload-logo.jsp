<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Form to upload a logo
  -
  - Attributes:
  -    community    - community to upload logo for
  -    collection   - collection to upload logo for - "overrides" community
  -                   if this isn't null
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");
    
%>

<dspace:layout titlekey="jsp.dspace-admin.upload-logo.title"
               navbar="admin"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="/dspace-admin" 
               nocache="true">

    <%-- <h1>Upload Logo</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.upload-logo.title"/></h1>   
    <%-- <p>Select the logo to upload for
	<%= (collection != null ? "collection <strong>" + collection.getMetadata("name") + "</strong>"
                                : "community <strong>" + community.getMetadata("name") + "</strong>") %>
    </p> --%>    
    	<p>
    	    <%
	    	if (collection != null){
	    %>
	    		<fmt:message key="jsp.dspace-admin.upload-logo.select.col">
                    <fmt:param><%= collection.getMetadata("name")%></fmt:param>
                </fmt:message>
	    <%	
	    	}
	    	else{
	    %>
	    		<fmt:message key="jsp.dspace-admin.upload-logo.select.com">
                    <fmt:param><%= community.getMetadata("name")%></fmt:param>
                </fmt:message>
	    <%
	    	}
	    %>
        </p>
    
    <form method="post" enctype="multipart/form-data" action="">
        <p align="center">
            <input type="file" size="40" name="file"/>
        </p>
        
 <input type="hidden" name="community_id" value="<%= community.getID() %>" />

<%  if (collection != null) { %>
        <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
<%  } %>
        <%-- <p align="center"><input type="submit" name="submit" value="Upload"/></p> --%>
        <p align="center"><input type="submit" name="submit" value="<fmt:message key="jsp.dspace-admin.general.upload"/>" /></p>
    </form>
</dspace:layout>
