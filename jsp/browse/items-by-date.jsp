<%--
  - items-by-date.jsp
  -
  - Version: $Revision: 1.8 $
  -
  - Date: $Date: 2003/12/17 15:42:54 $
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
  - Display the results of browsing a title or date index
  -
  - Attributes to pass in:
  -   community      - pass in if the scope of the browse is a community, or
  -                    a collection within this community
  -   collection     - pass in if the scope of the browse is a collection
  -   browse.info    - the BrowseInfo containing the items to display
  -   highlight      - Boolean.  If true, the focus point of the browse
  -                    is highlighted
  -   previous.query - The query string to pass to the servlet to get the
  -                    previous page of items
  -   next.query     - The query string to pass to the servlet to get the next
  -                    page of items
  -   oldest.first   - Boolean - if browsing dates, indicates whether oldest
  -                    items are first
  -   flip.ordering.query - the query string for flipping the order of the
  -                         index between oldest first and most recent first
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

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

    // Browse by date only stuff
    boolean oldestFirst = ((Boolean) request.getAttribute("oldest.first")).booleanValue();
    String flipOrderingQuery = (String) request.getAttribute("flip.ordering.query");

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

<dspace:layout title="Browsing by Date">

    <H2>Browsing <%= scopeName %> by Date</H2>

    <%-- Date browse controls table --%>
    <form action="browse-date" method=GET>
 
<%
    if (oldestFirst)
    {
        // Remember ordering when using browse controls
%>
        <input type=hidden name="order" value="oldestfirst">
<%
    }
%>
        <table align=center border=0 bgcolor="#CCCCCC" cellpadding=0>
            <tr>
                <td>
                    <table border=0 bgcolor="#EEEEEE" cellpadding=2>
                        <tr>
                            <td class="browseBar">
                                <span class="browseBarLabel">Jump to a point in the index: </span>
                                <select name=month>
                                    <option selected value="-1">(Choose month)</option>
<%
    for (int i = 1; i <= 12; i++)
    {
%>
                                    <option value=<%= i %>><%= DCDate.getMonthName(i) %></option>
<%
    }
%>
                                </select>

                                <select name=year>
                                    <option selected value="-1">(Choose year)</option>
<%
    int thisYear = DCDate.getCurrent().getYear();
    for (int i = thisYear; i >= 1990; i--)
    {
%>
                                    <option><%= i %></option>
<%
    }
%>
                                    <option>1985</option>
                                    <option>1980</option>
                                    <option>1975</option>
                                    <option>1970</option>
                                    <option>1960</option>
                                    <option>1950</option>
                                </select>
                            </td>
                            <td class="browseBar" rowspan=2>
                                <input type=submit value="Go">
                            </td>
                        </tr>
                        <tr>
                            <%-- HACK:  Shouldn't use align here --%>
                            <td class="browseBar" align=center>
                                <span class="browseBarLabel">Or type in a year:</span>
                                <input type=text name=starts_with size=4 maxlength=4>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>


        <%-- Flip the ordering controls --%>

        <table border=0 width=70% align=center>
            <tr>
                <td align=left class=standard>
<%
    if (oldestFirst)
    {
%>
                    <a href="browse-date?<%= flipOrderingQuery %>">Show Most Recent First</A>
<%
    }
    else
    {
%>
                    <strong>Ordering With Most Recent First</strong>
<%
    }
%>
                </td>
                <td align=right class=standard>
<%
    if( oldestFirst )
    {
%>
                    <strong>Ordering With Oldest First</strong>
<%
    }
    else
    {
%>
                    <a href="browse-date?<%= flipOrderingQuery %>order=oldestfirst">Show Oldest First</A>
<%
    }
%>
                </td>
            </tr>
        </table>                
    </form>

    <BR>

    <P align=center>Showing items <%= browseInfo.getOverallPosition()+1 %>-<%= browseInfo.getOverallPosition()+browseInfo.getResultCount() %> of <%= browseInfo.getTotal() %>.</P>

    <%-- Previous page/Next page --%>
    <table align=center border=0 width=70%>
        <tr>
            <td class=standard align=left>
<%
    if (prevQuery != null)
    {
%>
                <A HREF="browse-date?<%= prevQuery %>">Previous page</A>
<%
    }
%>
            </td>
            <td class=standard align=right>
<%
    if (nextQuery != null)
    {
%>
                <A HREF="browse-date?<%= nextQuery %>">Next page</A>
<%
    }
%>
            </td>
        </tr>
    </table>


<%
    String highlightAttribute = "-1";
    if (highlight)
    {
        highlightAttribute = String.valueOf(browseInfo.getOffset());
    }    
%>
    <dspace:itemlist items="<%= browseInfo.getItemResults() %>" emphcolumn="date" highlightrow="<%= highlightAttribute %>" />


    <%-- Previous page/Next page --%>
    <table align=center border=0 width=70%>
        <tr>
            <td class=standard align=left>
<%
    if (prevQuery != null)
    {
%>
                <A HREF="browse-date?<%= prevQuery %>">Previous page</A>
<%
    }
%>
            </td>
            <td class=standard align=right>
<%
    if (nextQuery != null)
    {
%>
                <A HREF="browse-date?<%= nextQuery %>">Next page</A>
<%
    }
%>
            </td>
        </tr>
    </table>

</dspace:layout>
