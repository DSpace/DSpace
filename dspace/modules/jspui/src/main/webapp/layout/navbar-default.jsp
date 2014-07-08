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

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="/WEB-INF/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.List" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="java.util.Map" %>

<%-- damanzano: It was added to enable internationalization on all pages --%>
<%@ page import="java.util.Locale"%>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.JSPManager" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
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
    
    // damanzano it was added to enable internationalization on all pages
    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
%>


       <div class="navbar-header">
         <a class="navbar-brand" href="<%= request.getContextPath() %>/">
             <img height="40px" src="<%= request.getContextPath() %>/image/icesi-logo.svg" onerror="this.src='<%= request.getContextPath() %>/image/icesi-logo.png'" alt="<fmt:message key="jsp.layout.header-default.alt"/>"/>
         </a>
         
         <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".default-navigation">
             <span class="sr-only">Toggle navigation</span>
             <span class="fa fa-bars">
         </button>
       </div>
         
       <!-- navbar collapsible browse content -->
       <nav class="navbar-collapse collapse default-navigation" role="navigation">
           <ul class="nav navbar-nav">
               <li class="<%= currentPage.endsWith("/home.jsp")? "active" : "" %>">
                   <a href="<%= request.getContextPath() %>/"><span class="glyphicon glyphicon-home"></span> <fmt:message key="jsp.layout.navbar-default.home"/></a>
               </li>
               <li class="dropdown">
                   <a class="dropdown-toggle" data-toggle="dropdown" href="#">
                       <span class="icesiicon icesiicon-menu"></span><fmt:message key="jsp.layout.navbar-default.browse"/> <span class="caret"></span>
                   </a>
                   <ul class="dropdown-menu">
                       <li class="navigationBarItem <%= (currentPage.endsWith("/community-list") ? "active" : "")%>">
                           <a href="<%= request.getContextPath()%>/community-list"><fmt:message key="jsp.layout.navbar-default.communities-collections"/></a>
                       </li>
                       <li class="divider"></li>
                       <%-- Insert the dynamic browse indices here --%>
                       <%
                           for (int i = 0; i < bis.length; i++) {
                               BrowseIndex bix = bis[i];
                               String key = "browse.menu." + bix.getName();
                       %>
                       <li class="navigationBarItem <%= (browseCurrent.equals(bix.getName()) ? "active" : "")%>">
                           <a href="<%= request.getContextPath()%>/browse?type=<%= bix.getName()%>"><fmt:message key="<%= key%>"/></a>
                       </li>
                       <%
                           }
                       %>
                       <%-- End of dynamic browse indices --%>
                   </ul>    
               </li>
               <%
               if (supportedLocales != null && supportedLocales.length > 1) {
               %>
               <li class="dropdown">
                   <form method="get" name="repost" action="">
                    <input type ="hidden" name ="locale"/>
                   </form>
                   <a class="dropdown-toggle" data-toggle="dropdown" href="#">
                       <span class="icesiicon icesiicon-language"></span><fmt:message key="jsp.layout.navbar-default.languages"/> <span class="caret"></span>
                   </a>
                   <ul class="dropdown-menu">
                       <%
                           for (int i = supportedLocales.length - 1; i >= 0; i--) {
                       %>
                       <li>
                           <a class ="langChangeOn"
                              onclick="javascript:document.repost.locale.value='<%=supportedLocales[i].toString()%>';document.repost.submit();">
                               <img src="<%= request.getContextPath()%>/image/flags/<%=supportedLocales[i].toString()%>.png" 
                                    alt="<%= supportedLocales[i].getDisplayLanguage(supportedLocales[i])%>" 
                                    title="<%= supportedLocales[i].getDisplayLanguage(supportedLocales[i])%>"/> 
                               <%= supportedLocales[i].getDisplayLanguage(supportedLocales[i])%>

                           </a>
                       </li>
                       <%
                           }
                       %>
                   </ul>
               </li>
           
               <%
               }
               %>
               <li class="dropdown">
               <%
               if (user != null)
               {
               %>
                <a href="#" class="dropdown-toggle" data-toggle="dropdown">
                    <span class="glyphicon glyphicon-user"></span> <fmt:message key="jsp.layout.navbar-default.loggedin">
		      <fmt:param><%= StringUtils.abbreviate(navbarEmail, 20) %></fmt:param>
		  </fmt:message> <b class="caret"></b>
                </a>
		<%
                } else {
		%>
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
               <li class="<%= ( currentPage.endsWith( "/help" ) ? "active" : "" ) %>"><dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") %>"><fmt:message key="jsp.layout.navbar-default.help"/></dspace:popup></li>
           </ul>
           <div class="nav navbar-nav navbar-right">
            <%-- Search Box --%>
            <form method="get" action="<%= request.getContextPath()%>/simple-search" class="navbar-form navbar-right" scope="search">
                <div class="form-group">
                    <input class="form-control" type="text" name="query" id="tequery"  placeholder="<fmt:message key="jsp.layout.navbar-default.search"/>"/>
                    <!--
                    <span class="input-group-btn">
                        <buttom type="submit" name="submit" class="btn btn-default" value="">
                            <span class="fa fa-search fa-flip-horizontal"></span> <fmt:message key="jsp.layout.navbar-default.go"/>
                        </buttom>
                    </span>
                    -->

                </div>
                <button type="submit" class="btn btn-primary"><span class="fa fa-search fa-flip-horizontal"></span></button>
            </form>
            <%--               <br/><a href="<%= request.getContextPath() %>/advanced-search"><fmt:message key="jsp.layout.navbar-default.advanced"/></a>
            <%
                        if (ConfigurationManager.getBooleanProperty("webui.controlledvocabulary.enable"))
                        {
            %>        
            <br/><a href="<%= request.getContextPath() %>/subject-search"><fmt:message key="jsp.layout.navbar-default.subjectsearch"/></a>
            <%
            }
            %>
            --%>
           </div>
           
       </nav><!--/.navbar-collapse -->
       
       
       
