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
        
     // Is the logged in user an admin or community admin or collection admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());
    
    Boolean communityAdmin = (Boolean)request.getAttribute("is.communityAdmin");
    boolean isCommunityAdmin = (communityAdmin == null ? false : communityAdmin.booleanValue());
    
    Boolean collectionAdmin = (Boolean)request.getAttribute("is.collectionAdmin");
    boolean isCollectionAdmin = (collectionAdmin == null ? false : collectionAdmin.booleanValue());
    
    String naviAdmin = "admin";
    String link = "/dspace-admin";
    
    if(!isAdmin && (isCommunityAdmin || isCollectionAdmin))
    {
        naviAdmin = "community-or-collection-admin";
        link = "/tools";
    }
%>

<dspace:layout titlekey="jsp.dspace-admin.upload-logo.title"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="<%= link %>" 
               nocache="true">

    <%-- <h1>Upload Logo</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.upload-logo.title"/></h1>   
    <%-- <p>Select the logo to upload for
	<%= (collection != null ? "collection <strong>" + collection.getName()  + "</strong>"
                                : "community <strong>" + community.getName()  + "</strong>") %>
    </p> --%>    
    	<p>
    	    <%
	    	if (collection != null){
	    %>
	    		<fmt:message key="jsp.dspace-admin.upload-logo.select.col">
                    <fmt:param><%= collection.getName() %></fmt:param>
                </fmt:message>
	    <%	
	    	}
	    	else{
	    %>
	    		<fmt:message key="jsp.dspace-admin.upload-logo.select.com">
                    <fmt:param><%= community.getName() %></fmt:param>
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
