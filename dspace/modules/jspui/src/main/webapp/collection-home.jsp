<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%--
  - Collection home JSP
  -
  - Attributes required:
  -    collection  - Collection to render home page for
  -    community   - Community this collection is in
  -    last.submitted.titles - String[], titles of recent submissions
  -    last.submitted.urls   - String[], corresponding URLs
  -    logged.in  - Boolean, true if a user is logged in
  -    subscribed - Boolean, true if user is subscribed to this collection
  -    admin_button - Boolean, show admin 'edit' button
  -    editor_button - Boolean, show collection editor (edit submitters, item mapping) buttons
  -    show.items - Boolean, show item list
  -    browse.info - BrowseInfo, item list
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.browse.ItemCounter"%>
<%@ page import="org.dspace.content.*"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.eperson.Group"     %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.apache.commons.lang.StringUtils"%>


<%
    // Retrieve attributes
    Collection collection = (Collection) request.getAttribute("collection");
    Community  community  = (Community) request.getAttribute("community");
    Group      submitters = (Group) request.getAttribute("submitters");

    RecentSubmissions rs = (RecentSubmissions) request.getAttribute("recently.submitted");
    
    boolean loggedIn =
        ((Boolean) request.getAttribute("logged.in")).booleanValue();
    boolean subscribed =
        ((Boolean) request.getAttribute("subscribed")).booleanValue();
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());

    Boolean editor_b      = (Boolean)request.getAttribute("editor_button");
    boolean editor_button = (editor_b == null ? false : editor_b.booleanValue());

    Boolean submit_b      = (Boolean)request.getAttribute("can_submit_button");
    boolean submit_button = (submit_b == null ? false : submit_b.booleanValue());

	// get the browse indices
    BrowseIndex[] bis = BrowseIndex.getBrowseIndices();

    // Put the metadata values into guaranteed non-null variables
    String name = collection.getMetadata("name");
    String intro = collection.getMetadata("introductory_text");
    if (intro == null)
    {
        intro = "";
    }
    String copyright = collection.getMetadata("copyright_text");
    if (copyright == null)
    {
        copyright = "";
    }
    String sidebar = collection.getMetadata("side_bar_text");
    if(sidebar == null)
    {
        sidebar = "";
    }

    String communityName = community.getMetadata("name");
    String communityLink = "/handle/" + community.getHandle();

    Bitstream logo = collection.getLogo();
    
    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        feedData = "coll:" + ConfigurationManager.getProperty("webui.feed.formats");
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));

    Boolean showItems = (Boolean)request.getAttribute("show.items");
    boolean show_items = showItems != null ? showItems.booleanValue() : false;
    
    String collectionHandle = "@";
    boolean isImageCollection = false;
    
    if (collection != null){
      collectionHandle = collection.getHandle();
    }
    if(ConfigurationManager.getProperty("image-galleries","webui.imagedisplay").indexOf(collectionHandle) >= 0)
    {
        isImageCollection = true;
    }
%>

<%@page import="org.dspace.app.webui.servlet.MyDSpaceServlet"%>
<dspace:layout locbar="commLink" title="<%= name %>" feedData="<%= feedData %>">
    <div class="well">
        <!-- Collection header -->
        <div class="row">
            <div class="col-md-10">
                <h2><%= name %>
<%
                if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
%>
                    <span class="badge"><%= ic.getCount(collection) %></span>
<%
                }
%>
                    <small><fmt:message key="jsp.collection-home.heading1"/></small>
                    
                </h2>
            </div>
<%  
        if (logo != null) { %>
            <div class="col-md-2">
        	<img class="img-responsive" alt="Logo" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" />
            </div>
<% 	} %>
	</div>
        
        <!-- Main data -->
        <div class="row">
            <div class="col-md-8">
            <%
            if (StringUtils.isNotBlank(intro)) { %>
                <%= intro %>
            <% 	} %>
            
                <p class="copyrightText"><%= copyright %></p>
            </div>
            <div class="col-md-4">
                <button class="visible-xs pull-right" type="button" data-toggle="offcanvas" data-target=".sidebar-section">
                    <fmt:message key="jsp.collection-home.collection-actions"/> <span class="glyphicon glyphicon-arrow-right"></span>
                </button>
            </div>
        </div>
            
        
        <!-- Items -->
 <% 
        if (show_items)
        {
            BrowseInfo bi = (BrowseInfo) request.getAttribute("browse.info");
            BrowseIndex bix = bi.getBrowseIndex();

            // prepare the next and previous links
            String linkBase = request.getContextPath() + "/handle/" + collection.getHandle();

            String next = linkBase;
            String prev = linkBase;

            if (bi.hasNextPage())
            {
                next = next + "?offset=" + bi.getNextOffset();
            }

            if (bi.hasPrevPage())
            {
                prev = prev + "?offset=" + bi.getPrevOffset();
            }

            String bi_name_key = "browse.menu." + bi.getSortOption().getName();
            String so_name_key = "browse.order." + (bi.isAscending() ? "asc" : "desc");
%>
        <%-- give us the top report on what we are looking at --%>
        <fmt:message var="bi_name" key="<%= bi_name_key %>"/>
        <fmt:message var="so_name" key="<%= so_name_key %>"/>
        <div class="browse-range text-center">
            <fmt:message key="jsp.collection-home.content.range">
                <fmt:param value="${bi_name}"/>
                <fmt:param value="${so_name}"/>
                <fmt:param value="<%= Integer.toString(bi.getStart()) %>"/>
                <fmt:param value="<%= Integer.toString(bi.getFinish()) %>"/>
                <fmt:param value="<%= Integer.toString(bi.getTotal()) %>"/>
            </fmt:message>
        </div>

        <%--  do the top previous and next page links --%>
        <div align="center">
<% 
        if (bi.hasPrevPage())
        {
%>
            <a href="<%= prev %>"><fmt:message key="browse.full.prev"/></a>&nbsp;
<%
        }

        if (bi.hasNextPage())
        {
%>
            &nbsp;<a href="<%= next %>"><fmt:message key="browse.full.next"/></a>
<%
        }
%>
        </div>
<%
        if(isImageCollection){
%>
        <div class="row">
            <dspace:imagelist browseInfo="<%= bi %>" />
        </div>
<%
        }else{
%>
        
        <%-- output the results using the browselist tag --%>
<%
            if (bix.isMetadataIndex())
            {
%>
        <dspace:browselist browseInfo="<%= bi %>" emphcolumn="<%= bix.getMetadata() %>" />
<%
            }
            else
            {
%>
        <dspace:browselist browseInfo="<%= bi %>" emphcolumn="<%= bix.getSortOption().getMetadata() %>" />
<%
            }
        }
%>
        <%-- give us the bottom report on what we are looking at --%>
    <div class="browse-range text-center">
        <fmt:message key="jsp.collection-home.content.range">
            <fmt:param value="${bi_name}"/>
            <fmt:param value="${so_name}"/>
            <fmt:param value="<%= Integer.toString(bi.getStart()) %>"/>
            <fmt:param value="<%= Integer.toString(bi.getFinish()) %>"/>
            <fmt:param value="<%= Integer.toString(bi.getTotal()) %>"/>
        </fmt:message>
    </div>

    <%--  do the bottom previous and next page links --%>
    <div align="center">
<% 
      if (bi.hasPrevPage())
      {
%>
      <a href="<%= prev %>"><fmt:message key="browse.full.prev"/></a>&nbsp;
<%
      }

      if (bi.hasNextPage())
      {
%>
      &nbsp;<a href="<%= next %>"><fmt:message key="browse.full.next"/></a>
<%
      }
%>
    </div>

<%
   } // end of if (show_items)
%>

    </div>

  <dspace:sidebar>
<% if(admin_button || editor_button ) { %>
<div class="row">
                 <div class="panel panel-warning">
                 <div class="panel-heading"><fmt:message key="jsp.admintools"/>
                 	<span class="pull-right"><dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.collection-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup></span>
                 </div>
                 <div class="panel-body">              
<% if( editor_button ) { %>
                <form method="post" action="<%=request.getContextPath()%>/tools/edit-communities">
                  <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                  <input type="hidden" name="community_id" value="<%= community.getID() %>" />
                  <input type="hidden" name="action" value="<%= EditCommunitiesServlet.START_EDIT_COLLECTION %>" />
                  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.edit.button"/>" />
                </form>
<% } %>

<% if( admin_button ) { %>
                 <form method="post" action="<%=request.getContextPath()%>/tools/itemmap">
                  <input type="hidden" name="cid" value="<%= collection.getID() %>" />
				  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.collection-home.item.button"/>" />                  
                </form>
<% if(submitters != null) { %>
		      <form method="get" action="<%=request.getContextPath()%>/tools/group-edit">
		        <input type="hidden" name="group_id" value="<%=submitters.getID()%>" />
		        <input class="btn btn-default col-md-12" type="submit" name="submit_edit" value="<fmt:message key="jsp.collection-home.editsub.button"/>" />
		      </form>
<% } %>
<% if( editor_button || admin_button) { %>
                <form method="post" action="<%=request.getContextPath()%>/mydspace">
                  <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                  <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_EXPORT_ARCHIVE %>" />
                  <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.collection"/>" />
                </form>
               <form method="post" action="<%=request.getContextPath()%>/mydspace">
                 <input type="hidden" name="collection_id" value="<%= collection.getID() %>" />
                 <input type="hidden" name="step" value="<%= MyDSpaceServlet.REQUEST_MIGRATE_ARCHIVE %>" />
                 <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.mydspace.request.export.migratecollection"/>" />
               </form>
               <form method="post" action="<%=request.getContextPath()%>/dspace-admin/metadataexport">
                 <input type="hidden" name="handle" value="<%= collection.getHandle() %>" />
                 <input class="btn btn-default col-md-12" type="submit" value="<fmt:message key="jsp.general.metadataexport.button"/>" />
               </form>
               </div>
               </div>
<% } %>
                 
<% } %>
</div>
<%  } %>

<%
	if (rs != null)
	{
%>
	<h3><fmt:message key="jsp.collection-home.recentsub"/></h3>
<%
		Item[] items = rs.getRecentSubmissions();
		for (int i = 0; i < items.length; i++)
		{
			DCValue[] dcv = items[i].getMetadata("dc", "title", null, Item.ANY);
			String displayTitle = "Untitled";
			if (dcv != null)
			{
				if (dcv.length > 0)
				{
					displayTitle = dcv[0].value;
				}
			}
%>
                        <p class="recentItem"><a href="<%= request.getContextPath() %>/handle/<%= items[i].getHandle() %>"><%= displayTitle %></a></p>
<%
		}

      } 
 %>
 <% if (sidebar!=null && !sidebar.equals("")){%>
    <div class="row">
        <div class="well">
                <p><%= sidebar %></p>
        </div>
    </div>
 <% } %>
    
    <!-- Actions -->
    <div class="row">
        <div class="col-md-12 col-sm-12 col-xs-12 panel panel-default">
            <h3><fmt:message key="jsp.collection-home.collection-actions"/></h3>
            
            <!-- Statistics button-->
            <a class="statistics-link btn btn-info col-md-12 col-sm-12 col-xs-12" href="<%= request.getContextPath() %>/handle/<%= collection.getHandle() %>/statistics">
                <fmt:message key="jsp.collection-home.display-statistics"/> <span class="icesiicon icesiicon-statistics"></span>
            </a>
            
            <%  
            if (submit_button)
            { 
%>
                <form class="form-group" action="<%= request.getContextPath() %>/submit" method="post">
                    <input type="hidden" name="collection" value="<%= collection.getID() %>" />
                    <input class="btn btn-success col-md-12 col-sm-12 col-xs-12" type="submit" name="submit" value="<fmt:message key="jsp.collection-home.submit.button"/>" />
                </form>
<%  } %>
                <form  method="get" action="">
<%  
                if (loggedIn && subscribed)
                { 
%>
                    <small>
                        <fmt:message key="jsp.collection-home.subscribed"/> 
                        <a href="<%= request.getContextPath() %>/subscribe">
                            <fmt:message key="jsp.collection-home.info"/>
                        </a>
                    </small>
                    <input class="btn btn-sm btn-warning col-md-12 col-sm-12 col-xs-12" type="submit" name="submit_unsubscribe" value="<fmt:message key="jsp.collection-home.unsub"/>" />
<%              } 
                else 
                { 
%>
                    <small>
                        <fmt:message key="jsp.collection-home.subscribe.msg"/>
                    </small>
                    <input class="btn btn-sm btn-info col-md-12 col-sm-12 col-xs-12" type="submit" name="submit_subscribe" value="<fmt:message key="jsp.collection-home.subscribe"/>" />
<%              
                } 
%>
                </form>
        </div>
    </div>
    <div class="row">
        <div class="col-md-12 col-sm-12 col-xs-12 panel panel-default">
            <!-- RSS -->
            <h3><fmt:message key="jsp.collection-home.feeds"/></h3>
            <% 
            if(feedEnabled)
            { 
                String[] fmts = feedData.substring(5).split(",");
                String icon = null;
                String rssType = null;
                int width = 0;
                for (int j = 0; j < fmts.length; j++)
                {
                    if ("rss_1.0".equals(fmts[j]))
                    {
                       icon = "rss1.gif";
                       width = 80;
                       rssType =  "RSS 1.0";
                    }
                    else if ("rss_2.0".equals(fmts[j]))
                    {
                       icon = "rss2.gif";
                       width = 80;
                        rssType =  "RSS 2.0";
                    }
                    else
                    {
                        icon = "rss.gif";
                        width = 36;
                        rssType =  "RSS";
                    }
%>
            <a class="btn btn-warning col-md-12 col-sm-12 col-xs-12 rss-link" href="<%= request.getContextPath() %>/feed/<%= fmts[j] %>/<%= collection.getHandle() %>">
                <%= rssType %> <span class="fa fa-rss pull-right"></span>
            </a>
<%
                } 
            }
%>
        </div>
    </div>
    
    <div class="row">
    <%-- Browse --%>

    <div class="panel panel-primary">
        <div class="panel-heading">
            <fmt:message key="jsp.general.browse"/>
        </div>
        <div class="panel-body">
        <%-- Insert the dynamic list of browse options --%>
        <%
            for (int i = 0; i < bis.length; i++)
            {
                String key = "browse.menu." + bis[i].getName();
        %>
            <form method="get" action="<%= request.getContextPath() %>/handle/<%= collection.getHandle() %>/browse">
                <input type="hidden" name="type" value="<%= bis[i].getName() %>"/>
                <%-- <input type="hidden" name="collection" value="<%= collection.getHandle() %>" /> --%>
                <input type="submit" class="btn btn-default col-md-12" name="submit_browse" value="<fmt:message key="<%= key %>"/>"/>
            </form>
        <%	
            }
        %>              
        </div>
    </div>
    </div>
    
    <div class="row">
    <%
    	int discovery_panel_cols = 12;
    	int discovery_facet_cols = 12;
    %>
    <%@ include file="discovery/static-sidebar-facet.jsp" %>
        
    </div>
  </dspace:sidebar>

</dspace:layout>

