<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Home page JSP
  -
  - Attributes:
  -    communities - Community[] all communities in DSpace
  -    recent.submissions - RecetSubmissions
  --%>

<%@page import="org.dspace.content.Bitstream"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.io.File" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Locale"%>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.NewsManager" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");

    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
    String topNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-top.html"));
    String sideNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-side.html"));

    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        feedData = "ALL:" + ConfigurationManager.getProperty("webui.feed.formats");
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));

    RecentSubmissions submissions = (RecentSubmissions) request.getAttribute("recent.submissions");
%>

<dspace:layout locbar="off" titlekey="jsp.home.title" feedData="<%= feedData %>">

	<div class="jumbotron">
       <%= topNews %>
	</div>

<div class="row">
    <div class="col-md-12 col-sm-12">
        <form action="<%= request.getContextPath()%>/simple-search" method="get">
            <div class="input-group input-group-lg">
                <input type="text" class="form-control" name="query" id="tquery" placeholder="<fmt:message key="jsp.home.search2"/>"/>
                <span class="input-group-btn">
                    <button type="submit" class="btn btn-default" name="submit"><span class="fa fa-search fa-flip-horizontal"></span> <fmt:message key="jsp.general.search.button"/></button>
                </span>
            </div>
        </form>
        <%
        if (ConfigurationManager.getBooleanProperty("webui.controlledvocabulary.enable"))
        {
        %>        
        <br/><a href="<%= request.getContextPath() %>/subject-search"><fmt:message key="jsp.layout.navbar-default.subjectsearch"/></a>
        <%
        }
        %>
    </div>
</div>
<!--                
<div class="row">
    <div class="col-md-4">
        <%= sideNews %>
    </div>
</div>
-->
<div class="container-fluid row home-browse-panel">
    <div class="well">
        <div class="row">
<%
if (communities != null && communities.length != 0)
{
%>
	<div class="col-md-4 home-communities-panel">
            <h3><fmt:message key="jsp.home.com1"/></h3>
            <p><fmt:message key="jsp.home.com2"/></p>
            <div class="list-group">
<%
    boolean showLogos = ConfigurationManager.getBooleanProperty("jspui.home-page.logos", true);
    for (int i = 0; i < communities.length; i++)
    {
%>
                <div class="list-group-item row">
<%  
        Bitstream logo = communities[i].getLogo();
        String md_cols = "col-md-12";
        if (showLogos && logo != null) {
            md_cols="col-md-9";
%>
                    <div class="col-md-3">
                        <img alt="Logo" class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" /> 
                    </div>
               
<%      } %>
                    <div class="<%=md_cols%>">
	
                        <h4 class="list-group-item-heading">
                            <a href="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><%= communities[i].getMetadata("name") %></a>
<%
        if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
        {
%>
                            <span class="badge pull-right"><%= ic.getCount(communities[i]) %></span>
<%
        }

%>
                        </h4>
                        <p><%= communities[i].getMetadata("short_description") %></p>
                    </div>
                </div>                            
<%
    }
%>
            </div>
	</div>
<%
}
%>
	<%
    	int discovery_panel_cols = 8;
    	int discovery_facet_cols = 4;
    %>
	<%@ include file="discovery/static-sidebar-facet.jsp" %>
    </div>
    </div>
</div>
</dspace:layout>
