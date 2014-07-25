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
  -    groups      - CommunityGroup[] all groups
  -    communities.map - Map where a key is a group ID (Integer) and
  -                      the value is the arrary communities in that group
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.io.File" %>
<%@ page import="java.util.Enumeration"%>
<%@ page import="java.util.Locale"%>
<%@ page import="java.util.Map" %>
<%@ page import="javax.servlet.jsp.jstl.core.*" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="org.dspace.core.I18nUtil" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.CommunityGroup" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.browse.ItemCounter" %>

<%
    CommunityGroup[] groups = (CommunityGroup[]) request.getAttribute("groups");
    Map communityMap = (Map) request.getAttribute("communities.map");

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

<dspace:layout locbar="nolink" titlekey="jsp.home.title" feedData="<%= feedData %>" style="default">

    <h1>Welcome to the repository for University of Maryland research.</h1>

    <p>The Digital Repository at the University of Maryland (DRUM) collects, preserves, and provides public access to the scholarly output of the university. Faculty and researchers can upload research products for rapid dissemination, global visibility and impact, and long-term preservation.</p>
    <p>You can use DRUM to share and preserve <a href="http://drum.lib.umd.edu/help/scope_of_drum_content.jsp">a wide range of research products</a>, such as:</p>
    <ul>
    	<li>Articles, papers, books, and technical reports</li>
    	<li>Data and code</li>
    	<li>Supplemental material for journal articles</li>
    	<li>Presentations and posters</li>
    	<li>Theses and dissertations</li>
    </ul>
    <p>You can track views and downloads of your research, and everything in DRUM is indexed by Google and Google Scholar. You receive a permanent DOI for your items, making it easy for other researchers to cite your work.</p>
    <p>Depositing research in DRUM can help you satisfy data management and sharing requirements from the NSF, NIH, and other funding agencies and journals.</p>
    <button type="button"><a href="http://drum.lib.umd.edu/mydspace">Submit</a></button> 
            
    <br>




<dspace:sidebar>
<%= sideNews %>
</dspace:sidebar>
</dspace:layout>
