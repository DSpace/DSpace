<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Community home JSP
  -
  - Attributes required:
  -    community             - Community to render home page for
  -    collections           - array of Collections in this community
  -    subcommunities        - array of Sub-communities in this community
  -    last.submitted.titles - String[] of titles of recently submitted items
  -    last.submitted.urls   - String[] of URLs of recently submitted items
  -    admin_button - Boolean, show admin 'edit' button
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>
<%@ page import="org.dspace.app.webui.components.MostViewedBean"%>
<%@ page import="org.dspace.app.webui.components.MostViewedItem"%>
<%@ page import="org.dspace.discovery.SearchUtils"%>
<%@ page import="org.dspace.discovery.IGlobalSearchResult"%>
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.*" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Utils" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.util.List"%>

<%
    Boolean isAdminB = (Boolean) request.getAttribute("is.admin");
    Boolean isAdmin = (isAdminB != null?isAdminB.booleanValue():false);

    // Retrieve attributes
    Community community = (Community) request.getAttribute( "community" );
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");
    Community[] subcommunities =
        (Community[]) request.getAttribute("subcommunities");
    
    RecentSubmissions submissions = (RecentSubmissions) request.getAttribute("recently.submitted");
    List<Integer> commSubscribed = (List<Integer>) request.getAttribute("subscription_communities");
    List<Integer> collSubscribed = (List<Integer>) request.getAttribute("subscription_collections");
    MostViewedBean mostViewedItem = (MostViewedBean) request.getAttribute("mostViewedItem");
    boolean loggedIn =
          ((Boolean) request.getAttribute("logged.in")).booleanValue();
    boolean subscribed =
          ((Boolean) request.getAttribute("subscribed")).booleanValue();
    
    Boolean editor_b = (Boolean)request.getAttribute("editor_button");
    boolean editor_button = (editor_b == null ? false : editor_b.booleanValue());
    Boolean add_b = (Boolean)request.getAttribute("add_button");
    boolean add_button = (add_b == null ? false : add_b.booleanValue());
    Boolean remove_b = (Boolean)request.getAttribute("remove_button");
    boolean remove_button = (remove_b == null ? false : remove_b.booleanValue());

	// get the browse indices
    BrowseIndex[] bis = BrowseIndex.getBrowseCommunityIndices();

    // Put the metadata values into guaranteed non-null variables
    String name = community.getMetadata("name");
    String intro = community.getMetadata("introductory_text");
    String copyright = community.getMetadata("copyright_text");
    String sidebar = community.getMetadata("side_bar_text");
    Bitstream logo = community.getLogo();
    
    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        feedData = "comm:" + ConfigurationManager.getProperty("webui.feed.formats");
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));
%>

<%@page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>
<dspace:layout locbar="commLink" title="<%= name %>" feedData="<%= feedData %>">
<div class="well">
<div class="row">
	<div class="col-md-8">
        <h2><%= name %>
        <%
            if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
            {
%>
                : [<%= ic.getCount(community) %>]
<%
            }
%>
		<small><fmt:message key="jsp.community-home.heading1"/></small>
        <a class="statisticsLink btn btn-info" href="<%= request.getContextPath() %>/cris/stats/community.html?handle=<%= community.getHandle() %>&type=selected"><fmt:message key="jsp.community-home.display-statistics"/></a>
		</h2>
	</div>
<%  if (logo != null) { %>
     <div class="col-md-4">
     	<img class="img-responsive" alt="Logo" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" />
     </div> 
<% } %>
 </div>

<% if (StringUtils.isNotBlank(intro)) { %>
  <%= intro %>
<% } %>
</div>
<p class="copyrightText"><%= copyright %></p>
 <form class="well" method="get" action="">
<%  if (loggedIn && subscribed)
    { %>
                <small><fmt:message key="jsp.collection-home.subscribed"/> <a href="<%= request.getContextPath() %>/subscribe"><fmt:message key="jsp.collection-home.info"/></a></small>
           		<input class="btn btn-sm btn-warning" type="submit" name="submit_unsubscribe" value="<fmt:message key="jsp.collection-home.unsub"/>" />
<%  } else { %>
                <small>
            		  <fmt:message key="jsp.collection-home.subscribe.msg"/>
                </small>
				<input class="btn btn-sm btn-info" type="submit" name="submit_subscribe" value="<fmt:message key="jsp.collection-home.subscribe"/>" />
<%  } %>
</form>

	<div class="row">
	<div class="col-md-4">
	<%@ include file="components/recent-submissions.jsp" %>
		    </div>
	<div class="col-md-4">
	<%@ include file="components/most-viewed.jsp" %>
	<%-- @ include file="components/most-downloaded.jsp" --%>
		</div>
	<div class="col-md-4">
    	<%= sidebar %>
	</div>
</div>	

<%-- Browse --%>
<div class="panel panel-primary">
	<div class="panel-heading"><fmt:message key="jsp.general.browse"/></div>
	<div class="panel-body">
   				<%-- Insert the dynamic list of browse options --%>
<%
	for (int i = 0; i < bis.length; i++)
	{
		String key = "browse.menu." + bis[i].getName();
%>
	<form method="get" action="<%= request.getContextPath() %>/handle/<%= community.getHandle() %>/browse">
		<input type="hidden" name="type" value="<%= bis[i].getName() %>"/>
		<%-- <input type="hidden" name="community" value="<%= community.getHandle() %>" /> --%>
		<input class="btn btn-default col-md-3" type="submit" name="submit_browse" value="<fmt:message key="<%= key %>"/>"/>
	</form>
<%	
	}
%>
			
	</div>
</div>

<div class="row">

    <%
    	int discovery_panel_cols = 12;
    	int discovery_facet_cols = 4;
    	Map<String, List<FacetResult>> mapFacetes = (Map<String, List<FacetResult>>) request.getAttribute("discovery.fresults");
    	List<DiscoverySearchFilterFacet> facetsConf = (List<DiscoverySearchFilterFacet>) request.getAttribute("facetsConfig");
    	String processorSidebar = (String) request.getAttribute("processorSidebar");
    
    if(processorSidebar!=null && processorSidebar.equals("sidebar")) {
	%>
	<%@ include file="discovery/static-sidebar-facet.jsp" %>
	<% } %>	
</div>

<div class="row">
	<%@ include file="discovery/static-tagcloud-facet.jsp" %>
</div>
	
<div class="row">
<%
	boolean showLogos = ConfigurationManager.getBooleanProperty("jspui.community-home.logos", true);
	if (subcommunities.length != 0)
    {
%>
	<div class="col-md-6">

		<h3><fmt:message key="jsp.community-home.heading3"/></h3>
   
        <div class="list-group">
<%
        for (int j = 0; j < subcommunities.length; j++)
        {
%>
			<div class="list-group-item row">  
<%  
		Bitstream logoCom = subcommunities[j].getLogo();
		if (showLogos && logoCom != null) { %>
			<div class="col-md-3">
		        <img alt="Logo" class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logoCom.getID() %>" /> 
			</div>
			<div class="col-md-9">
<% } else { %>
			<div class="col-md-12">
<% }  %>		

	      <h4 class="list-group-item-heading"><a href="<%= request.getContextPath() %>/handle/<%= subcommunities[j].getHandle() %>">
	                <%= subcommunities[j].getMetadata("name") %></a>
<%
                if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
%>
                    [<%= ic.getCount(subcommunities[j]) %>]
<%
                }
			if(isAdmin || !ConfigurationManager.getBooleanProperty("solr-statistics","authorization.admin")) { %>
					<a href="<%= request.getContextPath() %>/cris/stats/community.html?handle=<%= subcommunities[j].getHandle() %>&type=selected"><img src="<%= request.getContextPath() %>/image/stats/chart_curve.png" border="0" title="usage statistics"/></a>
				&nbsp;
		 <% } %>
        
		<a href="<%= request.getContextPath() %>/feed/rss_2.0/<%= subcommunities[j].getHandle() %>"><img src="<%= request.getContextPath() %>/image/stats/feed.png" border="0" title="Content update: RSS feed"/></a>
		&nbsp;<a href=<%= request.getContextPath() %>/handle/<%= subcommunities[j].getHandle() %>?handle=<%= subcommunities[j].getHandle() %>&submit_<%
		    if (commSubscribed!=null && commSubscribed.contains(subcommunities[j].getID()))
		    { // subscribed
		        %>unsubscribe=unsubscribe"><img src="<%= request.getContextPath() %>/image/stats/stop-bell.png" border="0" title="Content update: Email subscription"/></a><%
		    }
		    else
		    { // not yet subscribed
		        %>subscribe=subscribe"><img src="<%= request.getContextPath() %>/image/stats/start-bell.png" border="0" title="Content update: Email subscription"/></a><%
		    }
    	%>
	    		<% if (remove_button) { %>
	                <form class="btn-group" method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
			          <input type="hidden" name="parent_community_id" value="<%= community.getID() %>" />
			          <input type="hidden" name="community_id" value="<%= subcommunities[j].getID() %>" />
			          <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_DELETE_COMMUNITY%>" />
	                  <button type="submit" class="btn btn-xs btn-danger"><span class="glyphicon glyphicon-trash"></span></button>
	                </form>
	    		<% } %>
			    </h4>
                <p class="collectionDescription"><%= subcommunities[j].getMetadata("short_description") %></p>
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
    if (collections.length != 0)
    {
%>
	<div class="col-md-6">

        <%-- <h2>Collections in this community</h2> --%>
		<h3><fmt:message key="jsp.community-home.heading2"/></h3>
		<div class="list-group">
<%
        for (int i = 0; i < collections.length; i++)
        {
%>
			<div class="list-group-item row">  
<%  
		Bitstream logoCol = collections[i].getLogo();
		if (showLogos && logoCol != null) { %>
			<div class="col-md-3">
		        <img alt="Logo" class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logoCol.getID() %>" /> 
			</div>
			<div class="col-md-9">
<% } else { %>
			<div class="col-md-12">
<% }  %>		

	      <h4 class="list-group-item-heading"><a href="<%= request.getContextPath() %>/handle/<%= collections[i].getHandle() %>">
	      <%= collections[i].getMetadata("name") %></a>
<%
            if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
            {
%>
                [<%= ic.getCount(collections[i]) %>]
<%
            }
			if(isAdmin || !ConfigurationManager.getBooleanProperty("solr-statistics","authorization.admin")) { %>
					<a href="<%= request.getContextPath() %>/cris/stats/collection.html?handle=<%= collections[i].getHandle() %>&type=selected"><img src="<%= request.getContextPath() %>/image/stats/chart_curve.png" border="0" title="usage statistics"/></a>
				&nbsp;
		 <% } %>
        
		<a href="<%= request.getContextPath() %>/feed/rss_2.0/<%= collections[i].getHandle() %>"><img src="<%= request.getContextPath() %>/image/stats/feed.png" border="0" title="Content update: RSS feed"/></a>
		&nbsp;<a href=<%= request.getContextPath() %>/handle/<%= collections[i].getHandle() %>?handle=<%= collections[i].getHandle() %>&submit_<%
		    if (collSubscribed!=null && collSubscribed.contains(collections[i].getID()))
		    { // subscribed
		        %>unsubscribe=unsubscribe"><img src="<%= request.getContextPath() %>/image/stats/stop-bell.png" border="0" title="Content update: Email subscription"/></a><%
		    }
		    else
		    { // not yet subscribed
		        %>subscribe=subscribe"><img src="<%= request.getContextPath() %>/image/stats/start-bell.png" border="0" title="Content update: Email subscription"/></a><%
		    }
    	%>
			
	    <% if (remove_button) { %>
	      <form class="btn-group" method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
	          <input type="hidden" name="parent_community_id" value="<%= community.getID() %>" />
	          <input type="hidden" name="community_id" value="<%= community.getID() %>" />
	          <input type="hidden" name="collection_id" value="<%= collections[i].getID() %>" />
	          <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_DELETE_COLLECTION%>" />
	          <button type="submit" class="btn btn-xs btn-danger"><span class="glyphicon glyphicon-trash"></span></button>
	      </form>
	    <% } %>
		</h4>
      <p class="collectionDescription"><%= collections[i].getMetadata("short_description") %></p>
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
</div>
    <% if(editor_button || add_button)  // edit button(s)
    { %>
    <dspace:sidebar>
		 <div class="panel panel-warning">
             <div class="panel-heading">
             	<fmt:message key="jsp.admintools"/>
             	<span class="pull-right">
             		<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup>
             	</span>
             	</div>
             <div class="panel-body">
             <% if(editor_button) { %>
	            <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
		          <input type="hidden" name="community_id" value="<%= community.getID() %>" />
		          <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_EDIT_COMMUNITY%>" />
                  <%--<input type="submit" value="Edit..." />--%>
                  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
             <% } %>
             <% if(add_button) { %>

				<form method="post" action="<%=request.getContextPath()%>/tools/collection-wizard">
		     		<input type="hidden" name="community_id" value="<%= community.getID() %>" />
                    <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.community-home.create1.button"/>" />
                </form>
                
                <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                    <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_CREATE_COMMUNITY%>" />
                    <input type="hidden" name="parent_community_id" value="<%= community.getID() %>" />
                    <%--<input type="submit" name="submit" value="Create Sub-community" />--%>
                    <input class="btn btn-default col-md-12" type="submit" name="submit" value="<fmt:message key="jsp.community-home.create2.button"/>" />
                 </form>
             <% } %>
            <% if( editor_button ) { %>
                <form method="post" action="<%=request.getContextPath()%>/mydspace">
                  <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                  <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.community"/>" />
                </form>
              <form method="post" action="<%=request.getContextPath()%>/mydspace">
                <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.migratecommunity"/>" />
              </form>
               <form method="post" action="<%=request.getContextPath()%>/dspace-admin/metadataexport">
                 <input type="hidden" name="handle" value="<%= community.getHandle() %>" />
                 <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
               </form>
			<% } %>
			</div>
		</div>
  </dspace:sidebar>
    <% } %>
</dspace:layout>
