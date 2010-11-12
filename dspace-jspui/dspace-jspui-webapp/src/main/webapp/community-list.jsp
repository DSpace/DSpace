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
    boolean showAll = true;
    ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));
%>

<%!
    JspWriter out = null;
    HttpServletRequest request = null;

    void setContext(JspWriter out, HttpServletRequest request)
    { 
        this.out = out;
        this.request = request;
    }

    void showCommunity(Community c) throws ItemCountException, IOException, SQLException
    {
    	ItemCounter ic = new ItemCounter(UIUtil.obtainContext(request));
        out.println( "<li class=\"communityLink\">" );
        out.println( "<strong><a href=\"" + request.getContextPath() + "/handle/" + c.getHandle() + "\">" + c.getMetadata("name") + "</a></strong>");
        if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
        {
            out.println(" <span class=\"communityStrength\">[" + ic.getCount(c) + "]</span>");
        }

        // Get the collections in this community
        Collection[] cols = c.getCollections();
        if (cols.length > 0)
        {
            out.println("<ul>");
            for (int j = 0; j < cols.length; j++)
            {
                out.println("<li class=\"collectionListItem\">");
                out.println("<a href=\"" + request.getContextPath() + "/handle/" + cols[j].getHandle() + "\">" + cols[j].getMetadata("name") +"</a>");
				if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
                    out.println(" [" + ic.getCount(cols[j]) + "]");
                }

                out.println("</li>");
            }
            out.println("</ul>");
        }

        // Get the sub-communities in this community
        Community[] comms = c.getSubcommunities();
        if (comms.length > 0)
        {
            out.println("<ul>");
            for (int k = 0; k < comms.length; k++)
            {
               showCommunity(comms[k]);
            }
            out.println("</ul>"); 
        }
        out.println("<br />");
        out.println("</li>");
    }
%>

<dspace:layout titlekey="jsp.community-list.title">

<%
    if (admin_button)
    {
%>     

<table class="miscTableNoColor" align="center">
        <tr>
        <td>
			<h1><fmt:message key="jsp.community-list.title"/></h1>
			<p><fmt:message key="jsp.community-list.text1"/></p>
        </td>
        <td>
        <table class="miscTable" align="center">
	    <tr>
	        <td class="evenRowEvenCol" colspan="2">
                <table>
                    <tr>
                        <th class="standard" id="t1">
                            <strong><fmt:message key="jsp.admintools"/></strong>
                        </th>
                    </tr>
                    <tr>
                        <td headers="t1" class="standard" align="center">
	                        <form method="post" action="<%=request.getContextPath()%>/dspace-admin/edit-communities">
		                        <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_CREATE_COMMUNITY%>" />
                                    <%--<input type="submit" name="submit" value="Create Top-Level Community...">--%>
									<input type="submit" name="submit" value="<fmt:message key="jsp.community-list.create.button"/>" />
                            </form>
                        </td>
                    </tr>
                    <tr>
                        <td headers="t1" class="standard" align="center">
                            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.site-admin\")%>"><fmt:message key="jsp.adminhelp"/></dspace:popup>
                        </td>
                    </tr>
               </table>
            </td>
        </tr>
        </table>
        </td>
       </tr>
      </table>

<%
    }
    else
    {
%>
	<h1><fmt:message key="jsp.community-list.title"/></h1>
	<p><fmt:message key="jsp.community-list.text1"/></p>
<%
    }
%>
<% if (communities.length != 0)
{
%>
    <ul>
<%
    if (showAll)
    {
        setContext(out, request);
        for (int i = 0; i < communities.length; i++)
        {
            showCommunity(communities[i]);
        }
     }
     else
     {
        for (int i = 0; i < communities.length; i++)
        {
%>		
            <li class="communityLink">
            <%-- HACK: <strong> tags here for broken Netscape 4.x CSS support --%>
            <strong><a href="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><%= communities[i].getMetadata("name") %></a></strong>
	    <ul>
<%
            // Get the collections in this community from the map
            Collection[] cols = (Collection[]) collectionMap.get(
                new Integer(communities[i].getID()));

            for (int j = 0; j < cols.length; j++)
            {
%>
                <li class="collectionListItem">
                <a href="<%= request.getContextPath() %>/handle/<%= cols[j].getHandle() %>"><%= cols[j].getMetadata("name") %></a>
<%
                if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
%>
                    [<%= ic.getCount(cols[j]) %>]
<%
                }
%>

				</li>
<%
            }
%>
            </ul>
	    <ul>
<%
            // Get the sub-communities in this community from the map
            Community[] comms = (Community[]) subcommunityMap.get(
                new Integer(communities[i].getID()));

            for (int k = 0; k < comms.length; k++)
            {
%>
                <li class="communityLink">
                <a href="<%= request.getContextPath() %>/handle/<%= comms[k].getHandle() %>"><%= comms[k].getMetadata("name") %></a>
<%
                if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
%>
                    [<%= ic.getCount(comms[k]) %>]
<%
                }
%>
				</li>
<%
            }
%>
            </ul>
            <br />
        </li>
<%
        }
    }
%>
    </ul>
 
<% }
%>
</dspace:layout>
