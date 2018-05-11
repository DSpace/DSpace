<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - main page for authorization editing
  -
  - Attributes:
  -   none
  -
  - Returns:
  -   submit_community
  -   submit_collection
  -   submit_item
  -       item_handle
  -       item_id
  -   submit_advanced
  -
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="org.dspace.content.Collection" %>

<% request.setAttribute("LanguageSwitch", "hide"); %>

<%
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

<dspace:layout style="submission" titlekey="jsp.dspace-admin.authorize-main.title"
               navbar="<%= naviAdmin %>"
               locbar="link"
               parenttitlekey="jsp.administer"
               parentlink="<%= link %>">

    <%-- <h1>Administer Authorization Policies</h1> --%>
    <h1><fmt:message key="jsp.dspace-admin.authorize-main.adm"/>
          <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\") + \"#authorize\"%>"><fmt:message key="jsp.help"/></dspace:popup>
    </h1>
  
          <%-- <h3>Choose a resource to manage policies for:</h3> --%>
		  <h3><fmt:message key="jsp.dspace-admin.authorize-main.choose"/></h3>
  
  
  
    
    
    <form method="post" action="">    

				<div class="btn-group col-md-offset-5">
                    <% if(isAdmin){ %>
					<div class="row">
                    <%-- <input type="submit" name="submit_community" value="Manage a Community's Policies"> --%>
                    	<input class="btn btn-default col-md-12" type="submit" name="submit_community" value="<fmt:message key="jsp.dspace-admin.authorize-main.manage1"/>" />
					</div>
                    <% } %>
					<div class="row">
                    <%-- <input type="submit" name="submit_collection" value="Manage Collection's Policies"> --%>
                    	<input class="btn btn-default col-md-12" type="submit" name="submit_collection" value="<fmt:message key="jsp.dspace-admin.authorize-main.manage2"/>" />
					</div>
					<div class="row">
                    <%-- <input type="submit" name="submit_item" value="Manage An Item's Policies"> --%>
                    	<input class="btn btn-default col-md-12" type="submit" name="submit_item" value="<fmt:message key="jsp.dspace-admin.authorize-main.manage3"/>" />
					</div>
                    <% if(isAdmin){ %>
					<div class="row">
                    <%-- <input type="submit" name="submit_advanced" value="Advanced/Item Wildcard Policy Admin Tool"> --%>
                    	<input class="btn btn-default col-md-12" type="submit" name="submit_advanced" value="<fmt:message key="jsp.dspace-admin.authorize-main.advanced"/>" />
                    </div>
                    <% } %>
     			</div>

    </form>
</dspace:layout>
