<%--
  - authors.jsp
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
  - Display the results of browsing the author index
  -
  - Attributes to pass in:
  -   community      - pass in if the scope of the browse is a community, or
  -                    a collection within this community
  -   collection     - pass in if the scope of the browse is a collection
  -   browse.info    - the BrowseInfo containing the authors to display
  -   highlight      - Boolean.  If true, the focus point of the browse
  -                    is highlighted
  -   previous.query - The query string to pass to the servlet to get the
  -                    previous page of authors
  -   next.query     - The query string to pass to the servlet to get the next
  -                    page of aitjprs
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.net.URLEncoder" %>
<%@ page import="java.util.List" %>

<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>

<%
    // Get attributes
    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");

    BrowseInfo browseInfo = (BrowseInfo) request.getAttribute("browse.info");

    boolean highlight = ((Boolean) request.getAttribute("highlight")).booleanValue();

    String prevQuery = (String) request.getAttribute("previous.query");
    String nextQuery = (String) request.getAttribute("next.query");

    // Description of what the user is actually browsing
    String scopeName = "All of DSpace";
    if (collection != null)
    {
        scopeName = collection.getMetadata("name");
    }
    else if (community != null)
    {
        scopeName = community.getMetadata("name");
    }
%>

<dspace:layout title="Browse by Author">

    <H2>Browsing <%= scopeName %> by Author</H2>

    <form action="browse-author" method=GET>

        <%-- Browse controls --%>
        <table align=center border=0 bgcolor="#CCCCCC" cellpadding=0>
            <tr>
                <td>
                    <table border=0 bgcolor="#EEEEEE" cellpadding=2>
                        <tr>
                            <td class="browseBar">
                                <span class="browseBarLabel">Jump&nbsp;to:&nbsp;</span>
                                <A HREF="browse-author?starts_with=0">0-9</A>
<%
    for (char c = 'A'; c <= 'Z'; c++)
    {
%>
                                <A HREF="browse-author?starts_with=<%= c %>"><%= c %></A>
<%
    }
%>
                            </td>
                        </tr>
                        <tr>
                            <td class="browseBar" align=center>
                                <span class="browseBarLabel">or enter first few letters:&nbsp;</span>
                                <input type="text" name="starts_with">&nbsp;<input type="submit" value="Go!">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>

        <BR />

        <P align=center>Showing authors <%= browseInfo.getOverallPosition() + 1 %>-<%= browseInfo.getOverallPosition() + browseInfo.getResultCount() %> of <%= browseInfo.getTotal() %>.</P>

        <%-- Previous page/Next page --%>
        <table align=center border=0 width=70%>
            <tr>
                <td class=standard align=left>
<%
    if (prevQuery != null)
    {
%>
                    <A HREF="browse-author?<%= prevQuery %>">Previous page</A>
<%
    }
%>
                </td>
                <td class=standard align=right>
<%
    if (nextQuery != null)
    {
%>
                    <A HREF="browse-author?<%= nextQuery %>">Next page</A>
<%
    }
%>
                </td>
            </tr>
        </table>


        <%-- The authors --%>
        <table align=center class="miscTable">
<%
    // Row: toggles between Odd and Even
    String row = "odd";
    String[] results = browseInfo.getStringResults();

    for (int i = 0; i < results.length; i++)
    {
%>
            <tr>
                <td class="<%= highlight && i==browseInfo.getOffset() ? "highlight" : row %>RowOddCol">
                    <A HREF="items-by-author?author=<%= URLEncoder.encode(results[i]) %>"><%= results[i] %></A>
                </td>
            </tr>
<%
        row = ( row.equals( "odd" ) ? "even" : "odd" );
    }
%>
        </table>


        <%-- Previous page/Next page --%>
        <table align=center border=0 width=70%>
            <tr>
                <td class=standard align=left>
<%
    if (prevQuery != null)
    {
%>
                    <A HREF="browse-author?<%= prevQuery %>">Previous page</A>
<%
    }
%>
                </td>
                <td class=standard align=right>
<%
    if (nextQuery != null)
    {
%>
                    <A HREF="browse-author?<%= nextQuery %>">Next page</A>
<%
    }
%>
                </td>
            </tr>
        </table>


    </form>

</dspace:layout>
