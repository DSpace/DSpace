<%--
  - community-list.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2002, Hewlett-Packard Company and Massachusetts
  - Institute of Technology.  All rights reserved.
  -
  - Redistribution and use in source and binary forms, with or without
  - modification, are permitted provided that the following conditions are
  - met:
  -
  - - Redistributions of source code must retain the above copyright
  - notice, this list of conditions and the following disclaimer.
  -
  - - Redistributions in binary form must reproduce the above copyright
  - notice, this list of conditions and the following disclaimer in the
  - documentation and/or other materials provided with the distribution.
  -
  - - Neither the name of the Hewlett-Packard Company nor the name of the
  - Massachusetts Institute of Technology nor the names of their
  - contributors may be used to endorse or promote products derived from
  - this software without specific prior written permission.
  -
  - THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  - ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  - LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  - A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  - HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  - INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  - BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  - OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  - ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  - TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  - USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  - DAMAGE.
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

<%@ page import="java.util.Map" %>
<%@ page import="java.io.IOException" %>
<%@ page import="java.sql.SQLException" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditCommunitiesServlet" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%
    Community[] communities = (Community[]) request.getAttribute("communities");
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
    <UL>
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
            <LI class="communityLink">
            <%-- HACK: <strong> tags here for broken Netscape 4.x CSS support --%>
            <strong><A HREF="<%= request.getContextPath() %>/handle/<%= communities[i].getHandle() %>"><%= communities[i].getMetadata("name") %></A></strong>
	    <UL>
<%
            // Get the collections in this community from the map
            Collection[] cols = (Collection[]) collectionMap.get(
                new Integer(communities[i].getID()));

            for (int j = 0; j < cols.length; j++)
            {
%>
                <LI class="collectionListItem">
                <A HREF="<%= request.getContextPath() %>/handle/<%= cols[j].getHandle() %>"><%= cols[j].getMetadata("name") %></A>
<%
                if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
%>
                    [<%= cols[j].countItems() %>]
<%
                }
%>

				</LI>
<%
            }
%>
            </UL>
	    <UL>
<%
            // Get the sub-communities in this community from the map
            Community[] comms = (Community[]) subcommunityMap.get(
                new Integer(communities[i].getID()));

            for (int k = 0; k < comms.length; k++)
            {
%>
                <LI class="communityLink">
                <A HREF="<%= request.getContextPath() %>/handle/<%= comms[k].getHandle() %>"><%= comms[k].getMetadata("name") %></A>
<%
                if (ConfigurationManager.getBooleanProperty("webui.strengths.show"))
                {
%>
                    [<%= comms[k].countItems() %>]
<%
                }
%>
				</LI>
<%
            }
%>
            </UL>
            <BR>
        </LI>
<%
        }
    }
%>
    </UL>
 
</dspace:layout>
