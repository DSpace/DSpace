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

<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>

<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.BrowseIndex" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.*" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>


<%
    // Retrieve attributes
    Community community = (Community) request.getAttribute( "community" );
    Collection[] collections =
        (Collection[]) request.getAttribute("collections");
    Community[] subcommunities =
        (Community[]) request.getAttribute("subcommunities");
    
    RecentSubmissions rs = (RecentSubmissions) request.getAttribute("recently.submitted");
    
    Boolean editor_b = (Boolean)request.getAttribute("editor_button");
    boolean editor_button = (editor_b == null ? false : editor_b.booleanValue());
    Boolean add_b = (Boolean)request.getAttribute("add_button");
    boolean add_button = (add_b == null ? false : add_b.booleanValue());
    Boolean remove_b = (Boolean)request.getAttribute("remove_button");
    boolean remove_button = (remove_b == null ? false : remove_b.booleanValue());

	// get the browse indices
    BrowseIndex[] bis = BrowseIndex.getBrowseIndices();

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

    <!-- Community header -->
    <div class="row">
        <%  if (logo != null) { %>
        <div class="col-md-4 col-sm-4 col-xs-4">
            <img class="img-responsive" alt="Logo" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" />
        </div> 
        <% } %>
        
        <div class="col-md-8 col-sm-8 col-xs-8">
            <h2><%= name %>
            <%
                if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
            %>
            <span class="badge"><%= ic.getCount(community) %></span>
            <%
                }
            %>
                <small><fmt:message key="jsp.community-home.heading1"/></small>
            </h2>
        </div>
     </div>
     <!-- End community header -->
                
    <!-- Main data and subcommunities -->               
    <div class="row">
        <div class="col-md-6">
        <% if (StringUtils.isNotBlank(intro)) { %>
            <%= intro %>
        <% } %>
        </div>
        <div class="col-md-6">
            <!-- damanzano : row for communities and collections in this community -->     
            <div class="row">
            <%
               boolean showLogos = ConfigurationManager.getBooleanProperty("jspui.community-home.logos", true);
               if (subcommunities.length != 0)
               {
            %>
                <div class="col-md-12">
                    <h3><fmt:message key="jsp.community-home.heading3"/></h3>

                    <div class="list-group">
                    <%
                    for (int j = 0; j < subcommunities.length; j++)
                    {
                    %>
                        <div class="list-group-item">  
                            <div class="row">
                        <%  
                        Bitstream logoCom = subcommunities[j].getLogo();
                        String titleCols = "";
                        if (showLogos && logoCom != null) 
                        {
                            titleCols="col-md-9";
                        %>
                            <div class="col-md-3">
                                <img alt="Logo" class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logoCom.getID() %>" /> 
                            </div>
                        <% 
                        } else { 
                            titleCols="col-md-12";
                        }  
                        %>		
                            <div class="<%=titleCols%>">
                                <h4 class="list-group-item-heading">
                                    <a href="<%= request.getContextPath() %>/handle/<%= subcommunities[j].getHandle() %>">
                                        <%= subcommunities[j].getMetadata("name") %>
                                    </a>
                                <%
                                if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                                {
                                %>
                                    <span class="badge"><%= ic.getCount(subcommunities[j]) %></span>
                                <%
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
                <div class="col-md-12">
                    <h3><fmt:message key="jsp.community-home.heading2"/></h3>
                    <div class="list-group">
                    <%
                    for (int i = 0; i < collections.length; i++)
                    {
                    %>
                        <div class="list-group-item">
                            <div class="row">
                        <%  
                        Bitstream logoCol = collections[i].getLogo();
                        String collectionTitleCols = "";
                        if (showLogos && logoCol != null) {
                            collectionTitleCols = "col-md-9";
                        %>
                            <div class="col-md-3">
                                <img alt="Logo" class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logoCol.getID() %>" /> 
                            </div>
                                   
                        <% 
                        } else { 
                            collectionTitleCols = "col-md-12";
                        }  
                        %>		
                            <div class="<%=collectionTitleCols%>">
                                <h4 class="list-group-item-heading">
                                    <a href="<%= request.getContextPath() %>/handle/<%= collections[i].getHandle() %>">
                                        <%= collections[i].getMetadata("name") %>
                                    </a>
                                <%
                                if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                                {
                                %>
                                    <span class="badge"><%= ic.getCount(collections[i]) %></span>
                                <%
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
                <input class="btn btn-default col-md-2 col-sm-12 col-xs-12" type="submit" name="submit_browse" value="<fmt:message key="<%= key %>"/>"/>
            </form>
        <%	
            }
        %>
        </div>
    </div>
        

    <!-- Discovery -->        
    <div class="row">
    <%
    	int discovery_panel_cols = 12;
    	int discovery_facet_cols = 4;
    %>
        <%@ include file="discovery/static-sidebar-facet.jsp" %>
    </div>

    <!-- Recent submissions and Sidebar row (useless at this moment)-->
    <div class="row">
    <%
        if (rs != null)
        { 
    %>
        <div class="col-md-12">
            <div class="panel">
                <%-- Recently Submitted items --%>
                <h3><fmt:message key="jsp.community-home.recentsub"/></h3>
                <div id="recent-submissions-carousel" class="carousel slide">
                    <%
                    Item[] items = rs.getRecentSubmissions();
                    boolean first = true;
                    if(items!=null && items.length>0) 
                    { 
                    %>	
                    
                    <!-- Wrapper for slides -->
                    <div class="carousel-inner">
                    <%	
                    for (int i = 0; i < items.length; i++)
                    {
                        DCValue[] dcv = items[i].getMetadata("dc", "title", null, Item.ANY);
                        DCValue[] dcTypes = items[i].getMetadata("dc", "type", null, Item.ANY);
                        DCValue[] dcAuthors = items[i].getMetadata("dc", "creator", null, Item.ANY);
                        DCValue[] dcEditor = items[i].getMetadata("dc", "publisher", null, Item.ANY);
                        DCValue[] dcDateIssued = items[i].getMetadata("dc", "date", "issued", Item.ANY);
                        DCValue[] dcDescription = items[i].getMetadata("dc", "description", null, Item.ANY);
                        String displayTitle = "Untitled";
                        String authorDate ="";
                        String displayDesc="";
                        String displayType = "";
                        String itemCols="";

                        if (dcv != null)
                        {
                            if (dcv.length > 0)
                            {
                                displayTitle = dcv[0].value;
                            }
                        }
                        
                        if(dcAuthors!=null && dcAuthors.length > 0){
                            for (int j=0; j<dcAuthors.length;j++){
                                authorDate+=(dcAuthors[j].value);
                                if (j!=dcAuthors.length-1){
                                    authorDate+=("., ");
                                }
                            }
                        }
                        
                        if((dcEditor!=null && dcEditor.length > 0) || (dcDateIssued!=null && dcDateIssued.length > 0)){
                            authorDate+=" (";
                            if(dcEditor!=null && dcEditor.length > 0){
                                authorDate+=dcEditor[0].value;
                            }
                            if((dcEditor!=null && dcEditor.length > 0) && (dcDateIssued!=null && dcDateIssued.length > 0)){
                                authorDate+=", ";
                            }
                            if(dcDateIssued!=null && dcDateIssued.length > 0){
                                authorDate+=dcDateIssued[0].value;
                            }
                            authorDate+=")";
                            
                        }

                        if (dcDescription != null && dcDescription.length > 0)
                        {
                                displayDesc = dcDescription[0].value;
                        }
                        
                        if (dcTypes != null && dcTypes.length > 0){
                            displayType = dcTypes[0].value;
                            itemCols="col-md-11";
                        }else{
                            itemCols="col-md-12";
                        }
                    %>
                        <div style="padding-bottom: 50px;" class="item <%= first?"active":""%>">
                            <div class="recent-submission row">
                                <% if (displayType != null && !displayType.equals("")){%>
                                    <div class="col-md-1">
                                        <span class="type-icon icesiicon icesiicon-<%= displayType %>"></span>
                                    </div>
                                <% } %>    
                                    <div class="<%= itemCols %>">
                                        <a class="lead" href="<%= request.getContextPath() %>/handle/<%=items[i].getHandle() %>"> 
                                            <%= StringUtils.abbreviate(displayTitle, 70) %> 
                                        </a>
                                    <% if ((authorDate!=null) && !authorDate.equals("")){%>
                                        <p>
                                            <%= authorDate%>
                                        </p>
                                    <%}%>
                                    <% if ((displayDesc!=null) && !displayDesc.equals("")){%>
                                        <p>
                                            <%= StringUtils.abbreviate(displayDesc,300)%>
                                        </p>
                                    <%}%>
                                        
                                    </div>
                            </div>
                        </div>
                    <%
                        first = false;
                    }
                    %>
                    </div>
                    
                    <!-- Controls -->
                    <a class="left carousel-control" href="#recent-submissions-carousel" data-slide="prev">
                        <span class="icon-prev"></span>
                    </a>
                    <a class="right carousel-control" href="#recent-submissions-carousel" data-slide="next">
                        <span class="icon-next"></span>
                    </a>

                    <ol class="carousel-indicators">
                        <li data-target="#recent-submissions-carousel" data-slide-to="0" class="active"></li>
                        <% for (int i = 1; i < rs.count(); i++){ %>
                            <li data-target="#recent-submissions-carousel" data-slide-to="<%= i %>"></li>
                        <% } %>
                    </ol>

                    <%
                    }
                    %>

                </div>
            </div>
        </div>
    <%
        }
    %>
         
    </div>
    <!-- END recent submissions -->
    
    <!-- Additional information -->
    <div class="row">
        <div class="col-md-6">
            <%= sidebar %>
        </div>
        <div class="col-md-6">
            <p class="copyrightText"><%= copyright %></p>
        </div>
    </div>
    
    <div class="row">
        <div class="col-md-12">
        <%
            if (feedEnabled) {
                String[] fmts = feedData.substring(5).split(",");
                String icon = null;
                int width = 0;
                for (int j = 0; j < fmts.length; j++) {
                    if ("rss_1.0".equals(fmts[j])) {
                        icon = "rss1.gif";
                        width = 80;
                    } else if ("rss_2.0".equals(fmts[j])) {
                        icon = "rss2.gif";
                        width = 80;
                    } else {
                        icon = "rss.gif";
                        width = 36;
                    }
        %>
        <a href="<%= request.getContextPath()%>/feed/<%= fmts[j]%>/<%= community.getHandle()%>">
            <img src="<%= request.getContextPath()%>/image/<%= icon%>" alt="RSS Feed" width="<%= width%>" height="15" vspace="3" border="0" />
        </a>
        <%
                }
            }
        %>
        <!-- Statistics button-->
            <a class="statisticsLink btn btn-lg btn-info pull-right" href="<%= request.getContextPath() %>/handle/<%= community.getHandle() %>/statistics">
                <fmt:message key="jsp.community-home.display-statistics"/> <span class="icesiicon icesiicon-statistics"></span>
            </a>
        </div>
    </div>
</div>


<!-- Admin side bar -->
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