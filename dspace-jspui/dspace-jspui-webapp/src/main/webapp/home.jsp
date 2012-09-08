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
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="gr.ekt.webui.jsptag.TagCloudParameters" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");

    Locale[] supportedLocales = I18nUtil.getSupportedLocales();
    Locale sessionLocale = UIUtil.getSessionLocale(request);
    Config.set(request.getSession(), Config.FMT_LOCALE, sessionLocale);
    String topNews = ConfigurationManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-top.html"));
    String sideNews = ConfigurationManager.readNewsFile(LocaleSupport.getLocalizedMessage(pageContext, "news-side.html"));

    boolean feedEnabled = ConfigurationManager.getBooleanProperty("webui.feed.enable");
    String feedData = "NONE";
    if (feedEnabled)
    {
        feedData = "ALL:" + ConfigurationManager.getProperty("webui.feed.formats");
    }
    
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));
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
    <table class="miscTable" width="95%" align="center">
        <tr>
            <td class="oddRowEvenCol">
               <h3><fmt:message key="jsp.home.com1"/></h3>
                <p><fmt:message key="jsp.home.com2"/></p>


<%
 if (communities.length != 0)
 {
%>
    <table border="0" cellpadding="2">
<% 	                 

    for (int i = 0; i < communities.length; i++)
    {
%>                  <tr>
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
<%                
 }
%>  

            </td>
        </tr>
    </table>
	
<%
	TagCloudParameters tcp = new TagCloudParameters();
	tcp.setLocale(UIUtil.getSessionLocale(request).toString());
	
	String index = ConfigurationManager.getProperty("webui.tagcloud.home.bindex");
	if (index==null)
		index = "subject";
	
	String maxtags = ConfigurationManager.getProperty("webui.tagcloud.home.maxtags");
	if (maxtags==null)
		tcp.setTotalTags("20");
	else
		tcp.setTotalTags(maxtags);

	String showScore = ConfigurationManager.getProperty("webui.tagcloud.home.showscore");
	if (showScore==null)
		tcp.setDisplayScore(false);
	else
		tcp.setDisplayScore(Boolean.parseBoolean(showScore));

	String cutLevel = ConfigurationManager.getProperty("webui.tagcloud.home.cutlevel");
	if (cutLevel==null)
		tcp.setCuttingLevel("5");
	else
		tcp.setCuttingLevel(cutLevel);

	String showCenter = ConfigurationManager.getProperty("webui.tagcloud.home.showcenter");
	if (showCenter==null)
		tcp.setShouldCenter(false);
	else
		tcp.setShouldCenter(Boolean.parseBoolean(showCenter));

	String fontFrom = ConfigurationManager.getProperty("webui.tagcloud.home.fontfrom");
	if (fontFrom==null)
		tcp.setFontFrom("1.3");
	else
		tcp.setFontFrom(fontFrom);

	String fontTo = ConfigurationManager.getProperty("webui.tagcloud.home.fontto");
	if (fontTo==null)
		tcp.setFontTo("2.8");
	else
		tcp.setFontTo(fontTo);

	String tagCase = ConfigurationManager.getProperty("webui.tagcloud.home.tagcase");
	if (tagCase==null)
		tcp.setCloudCase("Case.PRESERVE_CASE");
	else
		tcp.setCloudCase(tagCase);

	String randomColors = ConfigurationManager.getProperty("webui.tagcloud.home.randomcolors");
	if (randomColors==null)
		tcp.setRandomColors(false);
	else
		tcp.setRandomColors(Boolean.parseBoolean(randomColors));

	String tagOrder = ConfigurationManager.getProperty("webui.tagcloud.home.tagorder");
	if (tagOrder==null)
		tcp.setOrdering("Tag.NameComparatorAsc");
	else
		tcp.setOrdering(tagOrder);

	String tagColor1 = ConfigurationManager.getProperty("webui.tagcloud.home.tagcolor1");
	if (tagColor1!=null)
		tcp.setColorLevel1(tagColor1);

	String tagColor2 = ConfigurationManager.getProperty("webui.tagcloud.home.tagcolor2");
	if (tagColor2!=null)
		tcp.setColorLevel2(tagColor2);

	String tagColor3 = ConfigurationManager.getProperty("webui.tagcloud.home.tagcolor3");
	if (tagColor3!=null)
		tcp.setColorLevel3(tagColor3);

	boolean allow = false;
   	String allowStr;
    	
   	if ( ((allowStr = ConfigurationManager.getProperty("webui.tagcloud.home.show"))) != null)
   		allow = Boolean.parseBoolean(allowStr);
	
	if (allow) {
%>
		<dspace:tagcloud parameters='<%= tcp %>' index='<%= index %>'/><br/><br/>
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
    </dspace:sidebar>
</dspace:layout>
