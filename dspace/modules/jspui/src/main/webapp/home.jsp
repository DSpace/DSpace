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

<%@page import="org.dspace.core.factory.CoreServiceFactory"%>
<%@page import="org.dspace.core.service.NewsService"%>
<%@page import="org.dspace.content.service.CommunityService"%>
<%@page import="org.dspace.content.factory.ContentServiceFactory"%>
<%@page import="org.dspace.content.service.ItemService"%>
<%@page import="org.dspace.core.Utils"%>
<%@page import="org.dspace.content.Bitstream"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.io.File" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Locale"%>
<%@ page import="java.util.List"%>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.app.webui.components.RecentSubmissions" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.services.ConfigurationService" %>
<%@ page import="org.dspace.services.factory.DSpaceServicesFactory" %>

<%
    List<Community> communities = (List<Community>) request.getAttribute("communities");

    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
    NewsService newsService = CoreServiceFactory.getInstance().getNewsService();
    String topNews = newsService.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-top.html"));
    String sideNews = newsService.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-side.html"));

    ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    
    boolean feedEnabled = configurationService.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        // FeedData is expected to be a comma separated list
        String[] formats = configurationService.getArrayProperty("webui.feed.formats");
        String allFormats = StringUtils.join(formats, ",");
        feedData = "ALL:" + allFormats;
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));

    RecentSubmissions submissions = (RecentSubmissions) request.getAttribute("recent.submissions");
    ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
%>
</div>
<dspace:layout locbar="nolink" titlekey="jsp.home.title" feedData="<%= feedData %>">

	<!--<div class="jumbotron">
        <%= topNews %>
	</div>-->
		<%-- Search Box --%>
	<div class="row" style="background-color: #fff; padding-top: 2%;">
    <div class="col-md-3"></div>
		<div class="col-md-6 offset-md-3" align="center">
	    	<form method="get" action="<%= request.getContextPath() %>/simple-search" class="navbar-form"> <!--navbar-right-->
			    <div class="form-group"><!--form-group  Buscar en Repostorio UTM"-->
		          <input type="text" maxlength="70" size="70" class="form-control" placeholder="<fmt:message key="jsp.layout.navbar-default.search"/>" name="query" id="tequery" style="width: 70%"/>
		          <!-- <input type="text" class="form-control" placeholder="<fmt:message key="jsp.layout.navbar-default.search"/>" name="query" id="tequery" size="25" />-->

		          <button type="submit" class="btn btn-primary" style="background-image: none; background-color:#410401; border-color: #240200;"><span class="glyphicon glyphicon-search"></span></button>

		        </div>
		        

		        <div class="row" align="center" style="padding: 2% 2%;">
					<button type="submit" class="btn btn-primary" style="background-image: none; background-color:#6b6b6b; border-color: #414141;">Búsqueda Avanzada</button>
				</div> 
	        </form>
    	</div>
	<div class="col-md-3"></div>
	</div> 



<div class="row">
<%
if (submissions != null && submissions.count() > 0)
{
%>
        <div class="col-md-8">
        <div class="panel panel-primary">        
        <div id="recent-submissions-carousel" class="panel-heading carousel slide">
          <h3><fmt:message key="jsp.collection-home.recentsub"/>
              <%
    if(feedEnabled)
    {
	    	String[] fmts = feedData.substring(feedData.indexOf(':')+1).split(",");
	    	String icon = null;
	    	int width = 0;
	    	for (int j = 0; j < fmts.length; j++)
	    	{
	    		if ("rss_1.0".equals(fmts[j]))
	    		{
	    		   icon = "rss1.gif";
	    		   width = 80;
	    		}
	    		else if ("rss_2.0".equals(fmts[j]))
	    		{
	    		   icon = "rss2.gif";
	    		   width = 80;
	    		}
	    		else
	    	    {
	    	       icon = "rss.gif";
	    	       width = 36;
	    	    }
	%>
	  <!--  <a href="<%= request.getContextPath() %>/feed/<%= fmts[j] %>/site">Transformar a las personas a través de una formación integral de calidad, mediante 
                compromiso, ética, respeto y pertinencia social, con la participación de la comunidad
                universitaria, impulsando el desarrollo regional, estatal y nacional, de manera sustentable,
                mediante investigación de alto nivel que impacte a los diferentes sectores de la sociedad,
                proyectando la imagen de la universidad en el ámbito internacional.<img src="<%= request.getContextPath() %>/image/<%= icon %>" alt="RSS Feed" width="<%= width %>" height="15" style="margin: 3px 0 3px" /></a>-->
	<%
	    	}
	    }
	%>
          </h3>
          
		  <!-- Wrapper for slides -->
		  <div class="carousel-inner">
		    <%
		    boolean first = true;
		    for (Item item : submissions.getRecentSubmissions())
		    {
		        String displayTitle = itemService.getMetadataFirstValue(item, "dc", "title", null, Item.ANY);
		        if (displayTitle == null)
		        {
		        	displayTitle = "Untitled";
		        }
		        String displayAbstract = itemService.getMetadataFirstValue(item, "dc", "description", "abstract", Item.ANY);
		        if (displayAbstract == null)
		        {
		            displayAbstract = "";
		        }
		%>
		    <div style="padding-bottom: 50px; min-height: 200px;" class="item <%= first?"active":""%>">
		      <div style="padding-left: 80px; padding-right: 80px; display: inline-block;"><%= Utils.addEntities(StringUtils.abbreviate(displayTitle, 400)) %> 
		      	<a href="<%= request.getContextPath() %>/handle/<%=item.getHandle() %>" class="btn btn-success">See</a>
                        <p><%= Utils.addEntities(StringUtils.abbreviate(displayAbstract, 500)) %></p>
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
		    <% for (int i = 1; i < submissions.count(); i++){ %>
		    <li data-target="#recent-submissions-carousel" data-slide-to="<%= i %>"></li>
		    <% } %>
	      </ol>
     </div></div></div>
<%
}
%>

<!--<div class="col-md-4">
    <%= sideNews %>
</div>-->

</div>

<div class="row " " style="background-color: #eee;"> <!--container -->
<%
if (communities != null && communities.size() != 0)
{
%>
	<div class="col-md-4"><!--col-sm-5 col-md-6 //orig: col-md-4 -->		
               <!--<h3><fmt:message key="jsp.home.com1"/></h3>
                <p><fmt:message key="jsp.home.com2"/></p>-->
                <div class="row container">
               	<div class="col-md-3"></div>
                <div class="col-md-6">
                <h3 style="padding-bottom: 2%; color: #410401"><fmt:message key="jsp.home.com1"/></h3> <!--comunidades-->
				<div class="list-group" style="display: block;">
<%
	boolean showLogos = configurationService.getBooleanProperty("jspui.home-page.logos", true);
    for (Community com : communities)
    {
%><div class="list-group-item row ">
<%  
		Bitstream logo = com.getLogo();
		if (showLogos && logo != null) { %>
	<div class="col-md-3">
        <img alt="Logo" class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" /> 
	</div>
	<div class="col-md-9">
<% } else { %>
	<div class="col-md-12">
<% }  %>		
		<h4 class="list-group-item-heading"><a href="<%= request.getContextPath() %>/handle/<%= com.getHandle() %>"><%= com.getName() %></a>
<%
        if (configurationService.getBooleanProperty("webui.strengths.show"))
        {
%>
		<span class="badge pull-right"><%= ic.getCount(com) %></span>
<%
        }

%>
		</h4>
		<p style="text-align: justify;"><%= communityService.getMetadata(com, "short_description") %></p>
    </div>
</div> <!--col-md-9-->                           
<%
    }
%>
	</div>
	</div>
<%
}
%>
</div><!---add-->
<!--<div class="col-md-3"></div>-->
</div><!---add-->
	<%
    	int discovery_panel_cols = 8;
    	int discovery_facet_cols = 4;
    %>
	<%@ include file="discovery/static-sidebar-facet.jsp" %>
</div> <!--col-md-4-->

<div class="row">
	<%@ include file="discovery/static-tagcloud-facet.jsp" %>
</div>
	
	<div class="row" >
		<div class="container" style="max-width: 1000px; border-top: 1px solid #6b6b6b;">
			<h4 style="text-align: center; font-size: 14px; color: #4c000e;">Misión del repositorio</h4>
			<p style="color: #6b6b6b; font-size: 12px; text-align: justify;">
				Es una plataforma que emplea estándares internacionales y mecanismos de acceso abierto para albergar publicaciones e información académica, científica y tecnológica generada en nuestra institución. La visibilidad de esta producción se logra a través de la conexión con el Repositorio Nacional (RN) de CONACYT. 

				El RI-UTM funcionará como una memoria institucional, difundiendo y preservando la producción científica evaluada por pares de la comunidad de manera libre, inmediata, gratuita y protegida. Gracias a esta difusión se fomentarán las discusiones académicas, se crearán comunidades de colaboración y se acelerará el desarrollo del conocimiento
	        </p>
        </div>
	</div>

</div>
</dspace:layout>
