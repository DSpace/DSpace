<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Default navigation bar
--%>

<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Locale"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.util.LocaleUIHelper" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="java.util.Map" %>
<%
    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");

    // Is the logged in user an admin
    Boolean admin = (Boolean)request.getAttribute("is.admin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    // Get the current page, minus query string
    String currentPage = UIUtil.getOriginalURL(request);
    int c = currentPage.indexOf( '?' );
    if( c > -1 )
    {
        currentPage = currentPage.substring( 0, c );
    }

    // E-mail may have to be truncated
    String navbarEmail = null;

    if (user != null)
    {
        navbarEmail = user.getEmail();
    }
    
    // get the browse indices
    
	BrowseIndex[] bis = BrowseIndex.getBrowseIndices();
    BrowseInfo binfo = (BrowseInfo) request.getAttribute("browse.info");
    String browseCurrent = "";
    if (binfo != null)
    {
        BrowseIndex bix = binfo.getBrowseIndex();
        // Only highlight the current browse, only if it is a metadata index,
        // or the selected sort option is the default for the index
        if (bix.isMetadataIndex() || bix.getSortOption() == binfo.getSortOption())
        {
            if (bix.getName() != null)
    			browseCurrent = bix.getName();
        }
    }
    
    String extraNavbarData = (String)request.getAttribute("dspace.cris.navbar");
 // get the locale languages
    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    boolean isRtl = StringUtils.isNotBlank(LocaleUIHelper.ifLtr(request, "","rtl"));    

    String[] mlinks = new String[0];
    String mlinksConf = ConfigurationManager.getProperty("cris", "navbar.cris-entities");
    if (StringUtils.isNotBlank(mlinksConf)) {
    	mlinks = StringUtils.split(mlinksConf, ",");
    }
    
    boolean showCommList = ConfigurationManager.getBooleanProperty("community-list.show.all",true);
%>

       <div class="navbar-header">
         <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
           <span class="icon-bar"></span>
           <span class="icon-bar"></span>
           <span class="icon-bar"></span>
         </button>
       </div>
       <nav class="collapse navbar-collapse bs-navbar-collapse" role="navigation">
         <ul id="top-menu" class="nav navbar-nav navbar-<%= isRtl ? "right":"left"%>">
           <li class="pull-<%= isRtl ? "right":"left"%>"><a class="navbar-brand" href="<%= request.getContextPath() %>/"><img height="25" src="<%= request.getContextPath() %>/image/dspace-logo-only.png" alt="DSpace logo" /></a></li>
           <li id="home-top-menu" class="pull-<%= isRtl ? "right":"left"%>   <%= currentPage.endsWith("/home.jsp")? 
        		   "active" : "" %>"><a href="<%= request.getContextPath() %>/"><fmt:message key="jsp.layout.navbar-default.home"/></a></li>
		  <% if(showCommList){ %>
		   <li id="communitylist-top-menu" class="<%= currentPage.endsWith("/community-list")? 
        		   "active" : "" %>"><a href="<%= request.getContextPath() %>/community-list"><fmt:message key="jsp.layout.navbar-default.communities-collections"/></a></li>
        		 <% }%> 
           <% for (String mlink : mlinks) { %>
           <c:set var="exploremlink">
           <%= mlink.trim() %>
           </c:set>
           <c:set var="fmtkey">
           jsp.layout.navbar-default.cris.<%= mlink.trim() %>
           </c:set>
           <li id="<%= mlink.trim() %>-top-menu" class="hidden-xs hidden-sm <c:if test="${exploremlink == location}">active</c:if>"><a href="<%= request.getContextPath() %>/cris/explore/<%= mlink.trim() %>"><fmt:message key="${fmtkey}"/></a></li>
           <% } %>
           <li class="dropdown hidden-md hidden-lg">
             <a href="#" class="dropdown-toggle" data-toggle="dropdown"><fmt:message key="jsp.layout.navbar-default.explore"/> <b class="caret"></b></a>
             <ul class="dropdown-menu">
           <% for (String mlink : mlinks) { %>
           <c:set var="exploremlink">
           <%= mlink.trim() %>
           </c:set>
           <c:set var="fmtkey">
           jsp.layout.navbar-default.cris.<%= mlink.trim() %>
           </c:set>
           <li class="<c:if test="${exploremlink == location}">active</c:if>"><a href="<%= request.getContextPath() %>/cris/explore/<%= mlink.trim() %>"><fmt:message key="${fmtkey}"/></a></li>
           <% } %>
           </ul>
           </li>
 <%
 if (extraNavbarData != null)
 {
%>
       <%= extraNavbarData %>
<%
 }
%>
          <li id="help-top-menu" class="<%= ( currentPage.endsWith( "/help" ) ? "active" : "" ) %>"><dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") %>"><fmt:message key="jsp.layout.navbar-default.help"/></dspace:popup></li>
       </ul>

 <%-- if (supportedLocales != null && supportedLocales.length > 1)
     {
 
    <div class="nav navbar-nav navbar-<%= isRtl ? "left" : "right" %>">
	 <ul class="nav navbar-nav navbar-<%= isRtl ? "left" : "right" %>">
      <li id="language-top-menu" class="dropdown">
       <a href="#" class="dropdown-toggle" data-toggle="dropdown"><fmt:message key="jsp.layout.navbar-default.language"/><b class="caret"></b></a>
        <ul class="dropdown-menu">
 <%
    for (int i = supportedLocales.length-1; i >= 0; i--)
     {
 %>
      <li>
        <a onclick="javascript:document.repost.locale.value='<%=supportedLocales[i].toString()%>';
                  document.repost.submit();" href="?locale=<%=supportedLocales[i].toString()%>">
          <%= LocaleSupport.getLocalizedMessage(pageContext, "jsp.layout.navbar-default.language."+supportedLocales[i].toString()) %>                  
     
       </a>
      </li>
 <%
     }
 %>
     </ul>
    </li>
    </ul>
  </div>
 <%
   }
 %>
 --%>
       <div class="nav navbar-nav navbar-<%= isRtl ? "left" : "right" %>">
		<ul class="nav navbar-nav navbar-<%= isRtl ? "left" : "right" %>">
                    <li id="search-top-menu" class="dropdown">
           <a href="#" class="dropdown-toggle" data-toggle="dropdown"><span class="glyphicon glyphicon-search"></span><b class="caret"></b></a>
          <div class="dropdown-menu">
          
	<%-- Search Box --%>
	<form id="formsearch-top-menu" method="get" action="<%= request.getContextPath() %>/global-search" class="navbar-form navbar-<%= isRtl ? "left" : "right" %>" scope="search">		
	    <div class="form-group">
          <input type="text" class="form-control" placeholder="<fmt:message key="jsp.layout.navbar-default.search"/>" name="query" id="tequery" size="25"/>
        </div>
        <button type="submit" class="btn btn-primary"><span class="glyphicon glyphicon-search"></span></button>
<%--               <br/><a href="<%= request.getContextPath() %>/advanced-search"><fmt:message key="jsp.layout.navbar-default.advanced"/></a>
<%
			if (ConfigurationManager.getBooleanProperty("webui.controlledvocabulary.enable"))
			{
%>        
              <br/><a href="<%= request.getContextPath() %>/subject-search"><fmt:message key="jsp.layout.navbar-default.subjectsearch"/></a>
<%
            }
%> --%>
	</form>
	
          </div>
          </li>
         <%
    if (user != null)
    {
		%>
		<li id="userloggedin-top-menu" class="dropdown">
		<a href="#" class="dropdown-toggle <%= isRtl ? "" : "text-right" %>" data-toggle="dropdown"><span class="glyphicon glyphicon-user"></span> <fmt:message key="jsp.layout.navbar-default.loggedin">
		      <fmt:param><%= StringUtils.abbreviate(navbarEmail, 20) %></fmt:param>
		  </fmt:message> <b class="caret"></b></a>
		<%
    } else {
		%>
			<li id="user-top-menu" class="dropdown">
             <a href="#" class="dropdown-toggle" data-toggle="dropdown"><span class="glyphicon glyphicon-user"></span> <fmt:message key="jsp.layout.navbar-default.sign"/> <b class="caret"></b></a>
	<% } %>             
             <ul class="dropdown-menu">
               <li><a href="<%= request.getContextPath() %>/mydspace"><fmt:message key="jsp.layout.navbar-default.users"/></a></li>
               <li><a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.layout.navbar-default.receive"/></a></li>
               <li><a href="<%= request.getContextPath() %>/profile"><fmt:message key="jsp.layout.navbar-default.edit"/></a></li>

		<%
		  if (isAdmin)
		  {
		%>
			   <li class="divider"></li>  
               <li><a href="<%= request.getContextPath() %>/dspace-admin"><fmt:message key="jsp.administer"/></a></li>
		<%
		  }
		  if (user != null) {
		%>
		<li><a href="<%= request.getContextPath() %>/logout"><span class="glyphicon glyphicon-log-out"></span> <fmt:message key="jsp.layout.navbar-default.logout"/></a></li>
		<% } %>
             </ul>
           </li>
          </ul>
	</div>
    </nav>
