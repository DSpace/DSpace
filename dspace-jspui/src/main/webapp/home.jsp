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

<%@page import="org.dspace.app.webui.util.UIUtil"%>
<%@page import="org.dspace.browse.ItemCounter"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager"%>
<%@ page import="org.dspace.core.NewsManager"%>
<%@ page import="org.springframework.context.ApplicationContext" %>
<%@ page import="org.springframework.web.servlet.support.RequestContextUtils" %>
<%@ page import="ua.edu.sumdu.essuir.statistics.EssuirStatistics" %>
<%@ page import="ua.edu.sumdu.essuir.statistics.StatisticData" %>
<%@ page import="ua.edu.sumdu.essuir.utils.EssuirUtils" %>
<%@ page import="javax.servlet.jsp.jstl.core.Config" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.util.Locale" %>


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
					java.util.Hashtable<String, Long> types = EssuirUtils.getTypesCount();
					java.util.TreeMap<String, String> typesLocale = new java.util.TreeMap<String, String>();

					for (String type : types.keySet()) {
						typesLocale.put(EssuirUtils.getTypeLocalized(type, sessionLocale.toString()), type);
					}



					int i = 0;
					for (String typeLocale : typesLocale.keySet())
					{
				%>
				<td class="standard" width="25%">
					<a href="<%= request.getContextPath() %>/simple-search?query=&filtername=type&filtertype=equals&filterquery=<%= typesLocale.get(typeLocale) %>&rpp=20&sort_by=dc.date.issued_dt&order=desc"><%= typeLocale %></a>
					<%
					%>
					<span class="badge"><%= types.get(typesLocale.get(typeLocale)) %></span>
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

	<div class="jumbotron">
		<table class="miscTable" width="100%" align="center">
			<tr>
				<td class="oddRowEvenCol">
					<h3><fmt:message key="jsp.home.com1"/></h3>
					<!--                  <p><fmt:message key="jsp.home.com2"/></p> -->


					<%
						if (communities.length != 0)
						{
					%>
					<table border="0" cellpadding="2">
						<%

							for (i = 0; i < communities.length; i++)
							{
						%>                  <tr>
						<td class="standard">
							<a href="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><%= communities[i].getMetadata("name") %></a>
							<%
								if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
								{
							%>
							<span class="badge"><%= ic.getCount(communities[i]) %></span>
							<%
								}

							%>
						</td>
					</tr>
						<%
							}
						%>
					</table>
					<%
						}
					%>
				</td>
			</tr>
		</table>
	</div>
</dspace:layout>
