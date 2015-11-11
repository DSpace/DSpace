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

<%@page import="org.dspace.core.Utils"%>
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
<%@ page import="org.dspace.content.Metadatum" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="ua.edu.sumdu.essuir.statistics.EssuirStatistics" %>
<%@ page import="ua.edu.sumdu.essuir.statistics.StatisticData" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");

    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
    String topNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-top.html"));
    String sideNews = NewsManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-side.html"));

	org.dspace.core.Context context = org.dspace.app.webui.util.UIUtil.obtainContext(request);
	StatisticData sd = EssuirStatistics.getTotalStatistic(context);

	topNews = String.format(topNews, sd.getTotalCount(), sd.getLastUpdate());

    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        feedData = "ALL:" + ConfigurationManager.getProperty("webui.feed.formats");
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));

    RecentSubmissions submissions = (RecentSubmissions) request.getAttribute("recent.submissions");
%>

<dspace:layout locbar="nolink" titlekey="jsp.home.title" feedData="<%= feedData %>">

	<table width="100%" style="margin-bottom:20px">
		<tr>
			<td class="jumbotron" width="75%">
			<%= topNews %>
		</td>
		<td width="20px"/><td valign="top" class="jumbotron">
			<p align="center" style="margin-bottom:22px"><a href="http://sumdu.edu.ua"><img src="/image/sumdu-logo-tr.gif" style="margin-top: 38px;"></a></p>
			<%= sideNews %>
		</td></tr>
	</table>


	<div class="jumbotron">
		<h3><fmt:message key="jsp.home.type"/></h3>

		<table border="0" cellpadding="2" width="100%">
			<tr>
				<%

					java.util.Hashtable<String, Long> types = ua.edu.sumdu.essuir.utils.EssuirUtils.getTypesCount();
					java.util.TreeMap<String, String> typesLocale = new java.util.TreeMap<String, String>();

					for (String type : types.keySet()) {
						typesLocale.put(ua.edu.sumdu.essuir.utils.EssuirUtils.getTypeLocalized(type, sessionLocale.toString()), type);
					}



					int i = 0;
					for (String typeLocale : typesLocale.keySet())
					{
						String type = typesLocale.get(typeLocale);
						String query = "";
						java.util.StringTokenizer tokens = new java.util.StringTokenizer(type);

						while (tokens.hasMoreTokens()) {
							query += "+" + tokens.nextToken();
						}

						if (query.length() > 0)
							query = query.substring(1);

				%>
				<td class="standard" width="25%">
					<a href="<%= request.getContextPath() %>/simple-search?query=&filtername=type&filtertype=equals&filterquery=<%= query %>&rpp=20&sort_by=dc.date.issued_dt&order=desc"><%= typeLocale %></a>
					<%
					%>
					<span class="badge"><%= types.get(type) %></span>
					<%
					%>
				</td>
				<%
					if (i++ % 4 == 3) {
				%>
			</tr>
			<tr>
				<%
						}

					}
				%>		<td></td><td></td><td></td>
			</tr>
		</table>
	</div>

<div class="container row">
<%
if (communities != null && communities.length != 0)
{
%>
	<div class="col-md-4">		
               <h3><fmt:message key="jsp.home.com1"/></h3>
                <p><fmt:message key="jsp.home.com2"/></p>
				<div class="list-group">
<%
	boolean showLogos = ConfigurationManager.getBooleanProperty("jspui.home-page.logos", true);
    for (int j = 0; j < communities.length; j++)
    {
%><div class="list-group-item row">
<%  
		Bitstream logo = communities[j].getLogo();
		if (showLogos && logo != null) { %>
	<div class="col-md-3">
        <img alt="Logo" class="img-responsive" src="<%= request.getContextPath() %>/retrieve/<%= logo.getID() %>" /> 
	</div>
	<div class="col-md-9">
<% } else { %>
	<div class="col-md-12">
<% }  %>		
		<h4 class="list-group-item-heading"><a href="<%= request.getContextPath() %>/handle/<%= communities[j].getHandle() %>"><%= communities[j].getMetadata("name") %></a>
<%
        if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
        {
%>
		<span class="badge pull-right"><%= ic.getCount(communities[j]) %></span>
<%
        }

%>
		</h4>
		<p><%= communities[j].getMetadata("short_description") %></p>
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

<div class="row">
	<%@ include file="discovery/static-tagcloud-facet.jsp" %>
</div>
	
</div>
</dspace:layout>
