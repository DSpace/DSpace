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

    <p><strong>Any UM Faculty member can make digital works
    permanently accessible and available across the Internet with
    DRUM.</strong><a href="<%= request.getContextPath() %>/help/about_submitting.jsp">  Find out
    more about depositing your work.</a></p>

    <table class="standard" width="95%" align="center">
        <tr>
            <td class="standard">
                <dspace:include page="/components/home-links.jsp" />
            </td>
        </tr>
    </table>

    <br>

    <table class="standard" width="95%" align="center">
        <tr>
            <td class="standard">
              <h2>The following communities of digital works are available:</h2>

            </td>
        </tr>
    </table>

    <br>


<%
    for (int k = 0; k < groups.length; k++) 
    {
%>
    <table class="standard" width="95%" align="center">
        <tr>
            <td class="standard">
                <p><b><%=groups[k].getName()%></b></p> 
                <table border=0 cellpadding=8>
<%
                    Community[] communities = 
		       (Community[]) communityMap.get(
		         new Integer(groups[k].getID()));

                    for (int i = 0; i < communities.length; i++)
                    {
%>                 
		    <tr>
                        <td class="standard">
                            <A HREF="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><%= communities[i].getMetadata("name") %> </A>
<%
        if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
        {
%>
            [<%= communities[i].countItems() %>]
<%
        }

%>
                        </td>
                    </tr>
<%
                    }
%>

                </table>

            </tr>
        </td>
    </table>
    <br>
<%
    }
%>



<dspace:sidebar>
<%= sideNews %>
</dspace:sidebar>
</dspace:layout>
