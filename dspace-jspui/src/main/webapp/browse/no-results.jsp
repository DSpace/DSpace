<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Message indicating that there are no entries in the browse index.
  -
  - Attributes required:
  -    community      - community the browse was in, or null
  -    collection     - collection the browse was in, or null
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page  import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>

<%
    request.setAttribute("LanguageSwitch", "hide");

	String layoutNavbar = "default";
	if (request.getAttribute("browseWithdrawn") != null || request.getAttribute("browsePrivate") != null)
	{
	    layoutNavbar = "admin";
            
            // Is the logged in user an admin or community admin or collection admin
            Boolean admin = (Boolean)request.getAttribute("is.admin");
            boolean isAdmin = (admin == null ? false : admin.booleanValue());
    
            Boolean communityAdmin = (Boolean)request.getAttribute("is.communityAdmin");
            boolean isCommunityAdmin = (communityAdmin == null ? false : communityAdmin.booleanValue());

            Boolean collectionAdmin = (Boolean)request.getAttribute("is.collectionAdmin");
            boolean isCollectionAdmin = (collectionAdmin == null ? false : collectionAdmin.booleanValue());
            
            if(!isAdmin && (isCommunityAdmin || isCollectionAdmin)){
                layoutNavbar = "community-or-collection-admin";
            }
	}

	// get the BrowseInfo object
	BrowseInfo bi = (BrowseInfo) request.getAttribute("browse.info");

    // Retrieve community and collection if necessary
    Community community = null;
    Collection collection = null;

    if (bi.inCommunity())
    {
    	community = (Community) bi.getBrowseContainer();
    }
    
    if (bi.inCollection())
    {
    	collection = (Collection) bi.getBrowseContainer();
    }
    
    // FIXME: this is not using the i18n
    // Description of what the user is actually browsing, and where link back
    String linkText = LocaleSupport.getLocalizedMessage(pageContext, "jsp.general.home");
    String linkBack = "/";

    if (collection != null)
    {
        linkText = collection.getName();
        linkBack = "/handle/" + collection.getHandle();
    }
    else if (community != null)
    {
        linkText = community.getName();
        linkBack = "/handle/" + community.getHandle();
    }
%>

<dspace:layout titlekey="browse.no-results.title" navbar="<%= layoutNavbar %>">

    <h1><fmt:message key="browse.no-results.title"/></h1>

<p>
    <%
	    if (collection != null)
	    {
   %>
            	<fmt:message key="browse.no-results.col">
                    <fmt:param><%= collection.getName() %></fmt:param>
                </fmt:message>
   <%
	    }
	    else if (community != null)
	    {
   %>
   		<fmt:message key="browse.no-results.com">
            <fmt:param><%= community.getName() %></fmt:param>
        </fmt:message>
   <%
 	    }
 	    else
 	    {
   %>
   		<fmt:message key="browse.no-results.genericScope"/>
   <%
   	    }
   %>
 </p>
   
    <p><a href="<%= request.getContextPath() %><%= linkBack %>"><%= linkText %></a></p>

    <%-- dump the results for debug (uncomment to enable) --%>
    <%--
	<!-- <%= bi.toString() %> -->
	--%>

</dspace:layout>
