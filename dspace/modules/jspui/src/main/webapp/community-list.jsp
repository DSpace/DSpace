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
  -    groups      - CommunityGroup[] all groups
  -    communities.map - Map where a key is a group ID (Integer) and
  -                      the value is the arrary communities in that group
  -    collections.map  - Map where a keys is a community IDs (Integers) and 
  -                      the value is the array of collections in that community
  -    subcommunities.map  - Map where a keys is a community IDs (Integers) and 
  -                      the value is the array of subcommunities in that community
  -    admin_button - Boolean, show admin 'Create Top-Level Community' button
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ page import="java.util.Map" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.CommunityGroup" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    CommunityGroup[] groups = (CommunityGroup[]) request.getAttribute("groups");
    Map communityMap = (Map) request.getAttribute("communities.map");
    Map collectionMap = (Map) request.getAttribute("collections.map");
    Map subcommunityMap = (Map) request.getAttribute("subcommunities.map");
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
    boolean showAll = true;
%>

<%!
    JspWriter out = null;
    HttpServletRequest request = null;

    void setContext(JspWriter out, HttpServletRequest request)
    { 
        this.out = out;
        this.request = request;
    }

    void showCommunity(Community c) throws IOException, SQLException
    {
        out.println( "<LI class=\"communityLink\">" );
        out.println( "<strong><A HREF=\"" + request.getContextPath() + "/handle/" + c.getHandle() + "\">" + c.getMetadata("name") + "</A></strong>");

        // Get the collections in this community
        Collection[] cols = c.getCollections();
        if (cols.length > 0)
        {
            out.println("<UL>");
            for (int j = 0; j < cols.length; j++)
            {
                out.println("<LI class=\"collectionListItem\">");
                out.println("<A HREF=\"" + request.getContextPath() + "/handle/" + cols[j].getHandle() + "\">" + cols[j].getMetadata("name") +"</A>");
				if(ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
                    out.println(" [" + cols[j].countItems() + "]");
                }

                out.println("</LI>");
            }
            out.println("</UL>");
        }

        // Get the sub-communities in this community
        Community[] comms = c.getSubcommunities();
        if (comms.length > 0)
        {
            out.println("<UL>");
            for (int k = 0; k < comms.length; k++)
            {
               showCommunity(comms[k]);
            }
            out.println("</UL>");
 
        }
        out.println("<BR>");
    }
%>

<dspace:layout title="Communities and Collections">

<%
    if (admin_button)
    {
%>
      <table class=miscTableNoColor align=center>
        <tr>
        <td>
            <H1>Communities and Collections</H1>

            <P>Shown below is a list of communities and the collections and sub-communities within them.
            Click on a name to view that community or collection home page.</P>
        </td>
        <td>
        <table class=miscTable align=center>
            <tr>
                <td class="evenRowEvenCol" colspan=2>
                    <table>
                        <tr>
                            <th class="standard">
                                <strong>Admin Tools</strong>
                            </th>
                        </tr>
                        <tr>
                            <td class="standard" align="center">
                                <form method=POST action="<%=request.getContextPath()%>/dspace-admin/edit-communities">
                                    <input type="hidden" name="action" value="<%=EditCommunitiesServlet.START_CREATE_COMMUNITY%>">
                                    <input type="submit" name="submit" value="Create Top-Level Community...">
                                </form>
                            </td>
                        </tr>
                        <tr>
                            <td class="standard" align="center">
                            <dspace:popup page="/help/site-admin.html">Admin Help...</dspace:popup>
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
    <H1>Communities and Collections</H1>

    <P>Shown below is a list of communities and the collections and sub-communities within them.
    Click on a name to view that community or collection home page.</P>
<%
    }
%>
    <table class="standard" width="95%" align="center">
<%
    setContext(out, request);
    
    for (int k = 0; k < groups.length; k++) 
    {
       out.println("<tr><td><b>" + groups[k].getName() + "</b>");
       Community[] communities = 
         (Community[]) communityMap.get(
            new Integer(groups[k].getID()));
       out.println("<ul>");
       for (int i = 0; i < communities.length; i++)
       {
           showCommunity(communities[i]);
       }
       out.println("</ul></td></tr>");
    }
%>
    </table> 
</dspace:layout>
