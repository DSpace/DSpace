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

<dspace:layout locbar="nolink" titlekey="jsp.home.title" feedData="<%= feedData %>">

    <table  width="95%" align="center">
      <tr align="right">
        <td align="right">						
<% if (supportedLocales != null && supportedLocales.length > 1)
{
%>
        <form method="get" name="repost" action="">
          <input type ="hidden" name ="locale"/>
        </form>
<%
for (int i = supportedLocales.length-1; i >= 0; i--)
{
%>
        <a class ="langChangeOn"
                  onclick="javascript:document.repost.locale.value='<%=supportedLocales[i].toString()%>';
                  document.repost.submit();">
                 <%= supportedLocales[i].getDisplayLanguage(supportedLocales[i])%>
        </a> &nbsp;
<%
}
}
%>
        </td>
      </tr>
      <tr>
            <td class="oddRowEvenCol"><%= topNews %></td>
        </tr>
    </table>
    <br/>
    <form action="<%= request.getContextPath() %>/simple-search" method="get">
        <table class="miscTable" width="95%" align="center">
            <tr>
                <td class="oddRowEvenCol">
                  <h3><fmt:message key="jsp.home.search1"/></h3>
                      <p><label for="tquery"><fmt:message key="jsp.home.search2"/></label></p>
                      <p><input type="text" name="query" size="20" id="tquery" />&nbsp;
                         <input type="submit" name="submit" value="<fmt:message key="jsp.general.search.button"/>" /></p>
                </td>
            </tr>
        </table>
    </form>

<%
if (communities != null && communities.length != 0)
{
%>
    <br/>
    <table class="miscTable" width="95%" align="center">
        <tr>
            <td class="oddRowEvenCol">
               <h3><fmt:message key="jsp.home.com1"/></h3>
                <p><fmt:message key="jsp.home.com2"/></p>


                <table border="0" cellpadding="2">
<%

    for (int i = 0; i < communities.length; i++)
    {
%>
                    <tr>
                        <td class="standard">
                            <a href="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><%= communities[i].getMetadata("name") %></a>
<%
        if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
        {
%>
            [<%= ic.getCount(communities[i]) %>]
<%
        }

%>
                        </td>
                    </tr>
<%
    }
%>
                </table>
            </td>
        </tr>
    </table>
<%
}
%>

<%
if (submissions != null && submissions.count() > 0)
{
%>
    <br/>
    <table class="miscTable" width="95%" align="center">
        <tr>
            <td class="oddRowEvenCol">
                <h3><fmt:message key="jsp.collection-home.recentsub"/></h3>
                <table border="0" cellpadding="2">
<%
    for (Item item : submissions.getRecentSubmissions())
    {
        DCValue[] dcv = item.getMetadata("dc", "title", null, Item.ANY);
        String displayTitle = "Untitled";
        if (dcv != null & dcv.length > 0)
        {
            displayTitle = dcv[0].value;
        }
%>
                    <tr>
                        <td class="standard10">
                            <a href="<%= request.getContextPath() %>/handle/<%=item.getHandle() %>"><%= displayTitle%> </a>
                        </td>
                    </tr>
<%
     }
%>
                </table>
             </td>
         </tr>
     </table>
<%
}
%>
    <dspace:sidebar>
    <%= sideNews %>
    <%
    if(feedEnabled)
    {
	%>
	    <center>
	    <h4><fmt:message key="jsp.home.feeds"/></h4>
	<%
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
	    <a href="<%= request.getContextPath() %>/feed/<%= fmts[j] %>/site"><img src="<%= request.getContextPath() %>/image/<%= icon %>" alt="RSS Feed" width="<%= width %>" height="15" vspace="3" border="0" /></a>
	<%
	    	}
	%>
	    </center>
	<%
	    }
	%>
	<%@ include file="discovery/static-sidebar-facet.jsp" %>
    </dspace:sidebar>
</dspace:layout>
