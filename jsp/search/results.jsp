<%--
  - results.jsp
  -
  - Version: $Revision: 1.13 $
  -
  - Date: $Date: 2005/03/04 02:37:10 $
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
  - Display the results of a simple search
  -
  - Attributes to pass in:
  -
  -   community        - pass in if the scope of the search was a community
  -                      or a collection in this community
  -   collection       - pass in if the scope of the search was a collection
  -   community.array  - if the scope of the search was "all of DSpace", pass
  -                      in all the communities in DSpace as an array to
  -                      display in a drop-down box
  -   collection.array - if the scope of a search was a community, pass in an
  -                      array of the collections in the community to put in
  -                      the drop-down box
  -   items            - the results.  An array of Items, most relevant first
  -   communities      - results, Community[]
  -   collections      - results, Collection[]
  -
  -   query            - The original query
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="java.net.URLEncoder"            %>
<%@ page import="org.dspace.content.Community"   %>
<%@ page import="org.dspace.content.Collection"  %>
<%@ page import="org.dspace.content.Item"        %>
<%@ page import="org.dspace.search.QueryResults" %>

<%
    // Get the attributes
    Community   community        = (Community   ) request.getAttribute("community" );
    Collection  collection       = (Collection  ) request.getAttribute("collection");
    Community[] communityArray   = (Community[] ) request.getAttribute("community.array");
    Collection[] collectionArray = (Collection[]) request.getAttribute("collection.array");
    
    Item      [] items       = (Item[]      )request.getAttribute("items");
    Community [] communities = (Community[] )request.getAttribute("communities");
    Collection[] collections = (Collection[])request.getAttribute("collections");

    String query = (String) request.getAttribute("query");

    QueryResults qResults = (QueryResults)request.getAttribute("queryresults");

    int pageTotal   = ((Integer)request.getAttribute("pagetotal"  )).intValue();
    int pageCurrent = ((Integer)request.getAttribute("pagecurrent")).intValue();
    int pageLast    = ((Integer)request.getAttribute("pagelast"   )).intValue();
    int pageFirst   = ((Integer)request.getAttribute("pagefirst"  )).intValue();
%>

<dspace:layout title="Search Results">

    <H1>Search Results</H1>
    
    <%-- Controls for a repeat search --%>
    <form action="simple-search" method=GET>
        <table class=miscTable align=center>
            <tr>
                <td class="evenRowEvenCol">
                    <table>
                        <tr>
                            <td>
                                <strong>Search:</strong>&nbsp;<select name="location">
<%
    if (community == null && collection == null)
    {
        // Scope of the search was all of DSpace.  The scope control will list
        // "all of DSpace" and the communities.
%>
                                    <option selected value="/">All of DSpace</option>
<%
        for (int i = 0; i < communityArray.length; i++)
        {
%>
                                    <option value="<%= communityArray[i].getHandle() %>"><%= communityArray[i].getMetadata("name") %></option>
<%
        }
    }
    else if (collection == null)
    {
        // Scope of the search was within a community.  Scope control will list
        // "all of DSpace", the community, and the collections within the community.
%>
                                    <option value="/">All of DSpace</option>
                                    <option selected value="<%= community.getHandle() %>"><%= community.getMetadata("name") %></option>
<%
        for (int i = 0; i < collectionArray.length; i++)
        {
%>
                                    <option value="<%= collectionArray[i].getHandle() %>"><%= collectionArray[i].getMetadata("name") %></option>
<%
        }
    }
    else
    {
        // Scope of the search is a specific collection
%>
                                    <option value="/">All of DSpace</option>
                                    <option value="<%= community.getHandle() %>"><%= community.getMetadata("name") %></option>
                                    <option selected value="<%= collection.getHandle() %>"><%= collection.getMetadata("name") %></option>
<%
    }
%>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <td align=center>
                                for&nbsp;<input type="text" name="query" value='<%= (query==null ? "" : query) %>'>&nbsp;<input type="submit" value="Go">
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </form>

<% if( qResults.getErrorMsg()!=null )
{
 %>
    <P align=center class="submitFormWarn"><%= qResults.getErrorMsg() %></P>
<%
}
else if( qResults.getHitCount() == 0 )
{
 %>
    <P align=center>Search produced no results.</P>
<%
}
else
{
%>
    <P align=center>Results <%=qResults.getStart()+1%>-<%=qResults.getStart()+qResults.getHitHandles().size()%> of
    <%=qResults.getHitCount()%>. </P>

<% } %>

<% if (communities.length > 0 ) { %>
    <h3>Community Hits:</h3>
    <dspace:communitylist  communities="<%= communities %>" />
<% } %>

<% if (collections.length > 0 ) { %>   
    <br>
    <h3>Collection hits:</h3>
    <dspace:collectionlist collections="<%= collections %>" />
<% } %>

<% if (items.length > 0) { %>
    <br>
    <h3>Item hits:</h3>
    <dspace:itemlist items="<%= items %>" />
<% } %>

<P align="center">


<%
    // retain scope when navigating result sets
    String searchScope = ""; 
    if (community == null && collection == null) {
	searchScope = "";
    } else if (collection == null) {
	searchScope = "/handle/" + community.getHandle();
    } else {
	searchScope = "/handle/" + collection.getHandle();
    } 

    String prevLink =  "<A HREF=\""
                    + request.getContextPath()
                    + searchScope 
                    + "/simple-search?query="
                    + URLEncoder.encode(query)
                    + "&start=";

    String nextLink = prevLink;

    prevLink = prevLink
            + (pageCurrent-2) * qResults.getPageSize()
            + "\">"
            + "previous"
            + "</A>";

    nextLink = nextLink
            + (pageCurrent) * qResults.getPageSize()
            + "\">"
            + "next"
            + "</A>";
    
    
    
%>


<%= (pageFirst != pageCurrent) ? prevLink : "" %>

<% for( int q = pageFirst; q <= pageLast; q++ )
{
    String myLink = "<A HREF=\""
                    + request.getContextPath()
                    + searchScope 
                    + "/simple-search?query="
                    + URLEncoder.encode(query)
                    + "&start=";


    if( q == pageCurrent )
    {
        myLink = "" + q;
    }
    else
    {
        myLink = myLink
            + (q-1) * qResults.getPageSize()
            + "\">"
            + q
            + "</A>";
    }
%>

<%= myLink %>

<%
}
%>

<%= ((pageTotal > pageCurrent) ? nextLink : "") %>

</P>

</dspace:layout>

