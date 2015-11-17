<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display hierarchical list of communities and collections
  -
  - Attributes to be passed in:
  -    communities         - array of communities
  -    collections.map  - Map where a keys is a community IDs (Integers) and 
  -                      the value is the array of collections in that community
  -    subcommunities.map  - Map where a keys is a community IDs (Integers) and 
  -                      the value is the array of subcommunities in that community
  -    admin_button - Boolean, show admin 'Create Top-Level Community' button
  --%>

<%@page import="org.dspace.content.Bitstream"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
	
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.browse.ItemCountException" %>
<%@ page import="org.dspace.browse.ItemCounter" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="java.util.Map" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");
    Map collectionMap = (Map) request.getAttribute("collections.map");
    Map subcommunityMap = (Map) request.getAttribute("subcommunities.map");
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));
%>

<%!
    void showCommunity(Community c, JspWriter out, HttpServletRequest request, ItemCounter ic,
    		Map collectionMap, Map subcommunityMap) throws ItemCountException, IOException, SQLException
    {
		boolean showLogos = ConfigurationManager.getBooleanProperty("jspui.community-list.logos", true);
        out.println( "<li>" );
        Collection[] cols = (Collection[]) collectionMap.get(c.getID());
        if (cols != null && cols.length > 0) {
            out.println("<span><i class=\"icon-minus-sign\" style = \"font-size:22px;\"></i></span>");
        }
        Bitstream logo = c.getLogo();
        if (showLogos && logo != null)
        {
        	out.println("<a class=\"pull-left col-md-2\" href=\"" + request.getContextPath() + "/handle/" 
        		+ c.getHandle() + "\"><img class=\"media-object img-responsive\" src=\"" + 
        		request.getContextPath() + "/retrieve/" + logo.getID() + "\" alt=\"community logo\"></a>");
        }
        out.println( "<a href=\"" + request.getContextPath() + "/handle/"
        	+ c.getHandle() + "\">" + c.getMetadata("name") + "</a>");
        if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
        {
            out.println(" <span class=\"badge\">" + ic.getCount(c) + "</span>");
        }
		if (StringUtils.isNotBlank(c.getMetadata("short_description")))
		{
			out.println(c.getMetadata("short_description"));
		}
        // Get the collections in this community

        if (cols != null && cols.length > 0)
        {
            out.println("<ul>");
            for (int j = 0; j < cols.length; j++)
            {
                out.println("<li>");

                Bitstream logoCol = cols[j].getLogo();
                if (showLogos && logoCol != null)
                {
                	out.println("<a class=\"pull-left col-md-2\" href=\"" + request.getContextPath() + "/handle/" 
                		+ cols[j].getHandle() + "\"><img class=\"media-object img-responsive\" src=\"" + 
                		request.getContextPath() + "/retrieve/" + logoCol.getID() + "\" alt=\"collection logo\"></a>");
                }
                out.println("<a href=\"" + request.getContextPath() + "/handle/" + cols[j].getHandle() + "\">" + cols[j].getMetadata("name") +"</a>");

				if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
                    out.println(" <span class=\"badge\">" + ic.getCount(cols[j]) + "</span>");
                }
				if (StringUtils.isNotBlank(cols[j].getMetadata("short_description")))
				{
					out.println(cols[j].getMetadata("short_description"));
				}
                out.println("</li>");
            }
            out.println("</ul>");
        }

        // Get the sub-communities in this community
        Community[] comms = (Community[]) subcommunityMap.get(c.getID());

        if (comms != null && comms.length > 0) {

            out.println("<ul>");
            for (int k = 0; k < comms.length; k++) {
                showCommunity(comms[k], out, request, ic, collectionMap, subcommunityMap);
            }
            out.println("</ul>");
        }
        out.println("</li>");
    }
%>

<dspace:layout titlekey="jsp.community-list.title">

<%
    if (admin_button)
    {
%>     
<dspace:sidebar>
			<div class="panel panel-warning">
			<div class="panel-heading">
				<fmt:message key="jsp.admintools"/>
				<span class="pull-right">
					<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup>
				</span>
			</div>
			<div class="panel-body">
                <form method="post" action="<%=request.getContextPath()%>/dspace-admin/edit-communities">
                    <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_CREATE_COMMUNITY%>" />
					<input class="btn btn-default" type="submit" name="submit" value="<fmt:message key="jsp.community-list.create.button"/>" />
                </form>
            </div>
</dspace:sidebar>
<%
    }
%>
	<h1><fmt:message key="jsp.community-list.title"/></h1>
	<p><fmt:message key="jsp.community-list.text1"/></p>
    <script>
        $(function () {
            $('.tree li:has(ul)').addClass('parent_li').find(' > span').attr('title', 'Collapse this branch');

            $('.tree li.parent_li > .icon-plus-sign').on('click', function (e) {
                var children = $(this).parent('li.parent_li').find(' > ul > li');
                console.log("show");
                children.show('fast');
                $(this).attr('title', 'Collapse this branch').find(' > i').addClass('icon-minus-sign').removeClass('icon-plus-sign');
            }
            $('.tree li.parent_li > .icon-minus-sign').on('click', function (e) {
                var children = $(this).parent('li.parent_li').find(' > ul > li');
                console.log("hide");
                children.hide('fast');
                $(this).attr('title', 'Expand this branch').find(' > i').addClass('icon-plus-sign').removeClass('icon-minus-sign');
            }
            e.stopPropagation();

        });
    </script>
<% if (communities.length != 0)
{
%>
    <div  class = "tree well">
    <ul>
<%
        for (int i = 0; i < communities.length; i++)
        {
            showCommunity(communities[i], out, request, ic, collectionMap, subcommunityMap);
        }
%>
    </ul>
    </div>
<% }
%>
</dspace:layout>
