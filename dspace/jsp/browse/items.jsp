<%--
  - items.jsp
  -
  - Version: $Revision$
  -
  - Date: $Date$
  -
  - Copyright (c) 2001, Hewlett-Packard Company and Massachusetts
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
  -   browse.dates   - Boolean - if True, browsing by date, otherwise by title
  -   community      - pass in if the scope of the browse is a community, or
  -                    a collection within this community
  -   collection     - pass in if the scope of the browse is a collection
  -   browse.info    - the BrowseInfo containing the items to display
  -   handles        - String[] of Handles corresponding to the browse results
  -   highlight      - Boolean.  If true, the focus point of the browse
  -                    is highlighted
  -   previous.query - The query string to pass to the servlet to get the
  -                    previous page of items
  -   next.query     - The query string to pass to the servlet to get the next
  -                    page of items
  -
  - Specific to browsing by date - these can be omitted if browsing titles
  -
  -   oldest.first   - Boolean - if browsing dates, indicates whether oldest
  -                    items are first
  -   flip.ordering.query - the query string for flipping the order of the
  -                         index between oldest first and most recent first
  --%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="org.dspace.browse.BrowseInfo" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>
<%@ page import="org.dspace.content.DCValue" %>
<%@ page import="org.dspace.content.Item" %>

<%
    // Get attributes
    boolean browseDates = ((Boolean) request.getAttribute("browse.dates")).booleanValue();

    Collection collection = (Collection) request.getAttribute("collection");
    Community community = (Community) request.getAttribute("community");

    BrowseInfo browseInfo = (BrowseInfo) request.getAttribute("browse.info");
    String[] handles = (String[]) request.getAttribute("handles");

    boolean highlight = ((Boolean) request.getAttribute("highlight")).booleanValue();

    String prevQuery = (String) request.getAttribute("previous.query");
    String nextQuery = (String) request.getAttribute("next.query");

    // Browse by date only stuff
    boolean oldestFirst = false;
    String flipOrderingQuery = "";
    
    if (browseDates)
    {
        oldestFirst = ((Boolean) request.getAttribute("oldest.first")).booleanValue();
        flipOrderingQuery = (String) request.getAttribute("flip.ordering.query");
    }    

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

    // Title
    String pageTitle = "Browse by " + (browseDates ? "Date" : "Title");
    String link = "browse-" + (browseDates ? "date" : "title");
%>

<dspace:layout title="<%= pageTitle %>">

    <H2>Browsing <%= scopeName %> by <%= browseDates ? "Date" : "Title" %></H2>

    <form action="<%= link %>" method=GET>

 
 <%
    if (browseDates)
    {
%>
        <%-- Date browse controls table --%>

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
        for( int i=1; i<=12; i++ )
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
        for( int i=2001; i>=1990; i-- )
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
                    <a href="<%= link %>?<%= flipOrderingQuery %>">Show Most Recent First</A>
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
                    <a href="<%= link %>?<%= flipOrderingQuery %>order=oldestfirst">Show Oldest First</A>
<%
        }
%>
                </td>
            </tr>
        </table>                
<%
    }
    else
    {
%>
        <%-- Title browse controls table --%>

        <table align=center border=0 bgcolor="#CCCCCC" cellpadding=0>
            <tr>
                <td>
                    <table border=0 bgcolor="#EEEEEE" cellpadding=2>
                        <tr>
                            <td class="browseBar">
                                <span class="browseBarLabel">Jump&nbsp;to:&nbsp;</span>
                                <A HREF="<%= link %>?starts_with=0">0-9</A>
<%
        for (char c = 'A'; c <= 'Z'; c++)
        {
%>
                                <A HREF="<%= link %>?starts_with=<%= c %>"><%= c %></A>
<%
        }
%>
                            </td>
                        </tr>
                        <tr>
                            <td class="browseBar" align=center>
                                <span class="browseBarLabel">or enter first few letters:&nbsp;</span>
                                <input type="text" name="starts_with"/>&nbsp;<input type="submit" value="Go!">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
<%
    }
%>

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
                    <A HREF="<%= link %>?<%= prevQuery %>">Previous page</A>
<%
    }
%>
                </td>
                <td class=standard align=right>
<%
    if (nextQuery != null)
    {
%>
                    <A HREF="<%= link %>?<%= nextQuery %>">Next page</A>
<%
    }
%>
                </td>
            </tr>
        </table>


        <%-- The items --%>
        <table align=center class="miscTable">
<%
    // Row: toggles between Odd and Even
    String row = "odd";

    for (int i = 0; i < browseInfo.getResults().size(); i++)
    {
        // Get the item
        Item item = (Item) browseInfo.getResults().get(i);

        // Title - we just use the first one
        DCValue[] titleArray = item.getDC("title", null, Item.ANY);
        String title = "Untitled";
        if (titleArray.length > 0)
        {
            title = titleArray[0].value;
        }

        // Authors....
        DCValue[] authors = item.getDC("contributor", "author", Item.ANY);

        // Date issued
        DCValue[] dateIssued = item.getDC("date", "issued", Item.ANY);
        DCDate dd = null;
        if(dateIssued.length > 0)
        {
            dd = new DCDate(dateIssued[0].value);
        }

        // Even, odd, or highlight row?
        String rowClass = row;

        if (highlight && i == browseInfo.getOffset())
        {
            rowClass = "highlight";
        }

        // We emphasise the index field
        String beforeDate = (browseDates ? "<strong>" : "");
        String afterDate = (browseDates ? "</strong>" : "");
        String beforeTitle = (browseDates ? "" : "<strong>");
        String afterTitle = (browseDates ? "" : "</strong>");
%>
            <tr>
                <td nowrap class="<%= rowClass %>RowOddCol" align=right>
                    <%= beforeDate %><dspace:date date="<%= dd %>" notime="true" /><%= afterDate %>
                </td>
                <td class="<%= rowClass %>RowEvenCol">
                    <%= beforeTitle %><A HREF="item/<%= handles[i] %>"><%= title %></A><%= afterTitle %>
                </td>
                <td class="<%= rowClass %>RowOddCol">
<%
        for( int j=0; j<authors.length; j++ )
        {
%>
                    <em><%= authors[j].value %></em><% if (j < authors.length - 1) { %>; <% } %>
<%
        }
%>
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
                    <A HREF="<%= link %>?<%= prevQuery %>">Previous page</A>
<%
    }
%>
                </td>
                <td class=standard align=right>
<%
    if (nextQuery != null)
    {
%>
                    <A HREF="<%= link %>?<%= nextQuery %>">Next page</A>
<%
    }
%>
                </td>
            </tr>
        </table>


    </form>

</dspace:layout>
