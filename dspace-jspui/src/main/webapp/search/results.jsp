<%--
  - results.jsp
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

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="org.dspace.content.Community" %>
<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.search.QueryResults" %>

<%
    // Get the attributes
    Community community = (Community) request.getAttribute("community" );
    Collection collection = (Collection) request.getAttribute("collection");
    Community[] communityArray = (Community[]) request.getAttribute("community.array");
    Collection[] collectionArray = (Collection[]) request.getAttribute("collection.array");

    Item[] items = (Item[]) request.getAttribute("items");
    Community[] communities = (Community[] )request.getAttribute("communities");
    Collection[] collections = (Collection[])request.getAttribute("collections");

    String query = (String) request.getAttribute("query");

    QueryResults qResults = (QueryResults)request.getAttribute("queryresults");

    int pageTotal = ((Integer)request.getAttribute("pagetotal")).intValue();
    int pageCurrent = ((Integer)request.getAttribute("pagecurrent")).intValue();
    int pageLast = ((Integer)request.getAttribute("pagelast")).intValue();
    int pageFirst = ((Integer)request.getAttribute("pagefirst")).intValue();
%>

<dspace:layout titlekey="jsp.search.results.title">

<h1><fmt:message key="jsp.search.results.title"/></h1>

    <%-- Controls for a repeat search --%>
    <form action="simple-search" method="get">
        <table class="miscTable" align="center" summary="Table displaying your search results">
            <tr>
                <td class="evenRowEvenCol">
                    <table>
                        <tr>
                            <td>
                                <label for="tlocation"><strong><fmt:message key="jsp.search.results.searchin"/></strong></label>&nbsp;<select name="location" id="tlocation">
<%
    if (community == null && collection == null)
    {
        // Scope of the search was all of DSpace.  The scope control will list
        // "all of DSpace" and the communities.
%>
                                    <option selected="selected" value="/"><fmt:message key="jsp.general.genericScope"/></option>
<%
        for (int i = 0; i < communityArray.length; i++)
        {
%>
                                    <option value="<%= communityArray[i].getIdentifier().getCanonicalForm() %>"><%= communityArray[i].getMetadata("name") %></option>
<%
        }
    }
    else if (collection == null)
    {
        // Scope of the search was within a community.  Scope control will list
        // "all of DSpace", the community, and the collections within the community.
%>
                                    <option value="/"><fmt:message key="jsp.general.genericScope"/></option>
                                    <option selected="selected" value="<%= community.getIdentifier().getCanonicalForm() %>"><%= community.getMetadata("name") %></option>
<%
        for (int i = 0; i < collectionArray.length; i++)
        {
%>
                                    <option value="<%= collectionArray[i].getIdentifier().getCanonicalForm() %>"><%= collectionArray[i].getMetadata("name") %></option>
<%
        }
    }
    else
    {
        // Scope of the search is a specific collection
%>
                                    <option value="/"><fmt:message key="jsp.general.genericScope"/></option>
                                    <option value="<%= community.getIdentifier().getCanonicalForm() %>"><%= community.getMetadata("name") %></option>
                                    <option selected="selected" value="<%= collection.getIdentifier().getCanonicalForm() %>"><%= collection.getMetadata("name") %></option>
<%
    }
%>
                                </select>
                            </td>
                        </tr>
                        <tr>
                            <td align="center">
                                <fmt:message key="jsp.search.results.searchfor"/>&nbsp;<input type="text" name="query" value="<%= (query==null ? "" : StringEscapeUtils.escapeHtml(query)) %>"/>&nbsp;<input type="submit" value="<fmt:message key="jsp.general.go"/>" />
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </form>

<%
if (qResults.getErrorMsg() != null)
{
 %>
    <p align="center" class="submitFormWarn"><%= qResults.getErrorMsg() %></p>
<%
}
else if (qResults.getHitCount() == 0)
{
 %>
    <p align="center"><fmt:message key="jsp.search.general.noresults"/></p>
<%
}
else
{
%>
	<p align="center"><fmt:message key="jsp.search.results.results">
        <fmt:param><%=qResults.getStart()+1%></fmt:param>
        <fmt:param><%=qResults.getStart()+qResults.getHitURIs().size()%></fmt:param>
        <fmt:param><%=qResults.getHitCount()%></fmt:param>
    </fmt:message></p>

<%
}
%>

<%
if (communities.length > 0)
{
%>
    <h3><fmt:message key="jsp.search.results.comhits"/></h3>
    <dspace:communitylist  communities="<%= communities %>" />
<%
}
if (collections.length > 0)
{
%>
    <br/>
    <h3><fmt:message key="jsp.search.results.colhits"/></h3>
    <dspace:collectionlist collections="<%= collections %>" />
<%
}
if (items.length > 0)
{
%>
    <br/>
    <h3><fmt:message key="jsp.search.results.itemhits"/></h3>
    <dspace:itemlist items="<%= items %>" />
<%
}
%>

<p align="center">

<%
    // retain scope when navigating result sets
    String searchScope = "";
    if (community == null && collection == null)
    {
	    searchScope = "";
    }
    else if (collection == null)
    {
	    searchScope = community.getIdentifier().getURL().toString();
    }
    else
    {
	    searchScope = collection.getIdentifier().getURL().toString();
    }

    // create the URLs accessing the previous and next search result pages
    String prevURL =  request.getContextPath()
                    + searchScope
                    + "/simple-search?query="
                    + URLEncoder.encode(query)
                    + "&amp;start=";

    String nextURL = prevURL;

    prevURL = prevURL
            + (pageCurrent-2) * qResults.getPageSize();

    nextURL = nextURL
            + (pageCurrent) * qResults.getPageSize();


if (pageFirst != pageCurrent)
{
    %><a href="<%= prevURL %>"><fmt:message key="jsp.search.general.previous" /></a><%
}


for (int q = pageFirst; q <= pageLast; q++)
{
    String myLink = "<a href=\""
                    + request.getContextPath()
                    + searchScope
                    + "/simple-search?query="
                    + URLEncoder.encode(query)
                    + "&amp;start=";


    if (q == pageCurrent)
    {
        myLink = "" + q;
    }
    else
    {
        myLink = myLink
            + (q-1) * qResults.getPageSize()
            + "\">"
            + q
            + "</a>";
    }
%>

<%= myLink %>

<%
}

if (pageTotal > pageCurrent)
{
    %><a href="<%= nextURL %>"><fmt:message key="jsp.search.general.next" /></a><%
}
%>

</p>

</dspace:layout>

