<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%--
  - Display the results of a subject search
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

<%@ page import="java.net.URLEncoder"            %>
<%@ page import="java.util.List"                 %>
<%@ page import="org.dspace.content.Community"   %>
<%@ page import="org.dspace.content.Collection"  %>
<%@ page import="org.dspace.content.Item"        %>
<%@ page import="org.dspace.discovery.DiscoverResult" %>

<%
    // Get the attributes
    Community   community        = (Community   ) request.getAttribute("community" );
    Collection  collection       = (Collection  ) request.getAttribute("collection");

    List<Item>      items       = (List<Item>      )request.getAttribute("items");
    List<Community> communities = (List<Community> )request.getAttribute("communities");
    List<Collection>collections = (List<Collection>)request.getAttribute("collections");

    String query = (String) request.getAttribute("query");

    DiscoverResult qResults = (DiscoverResult)request.getAttribute("queryresults");

    long pageTotal   = ((Long)request.getAttribute("pagetotal"  )).longValue();
    long pageCurrent = ((Long)request.getAttribute("pagecurrent")).longValue();
    long pageLast    = ((Long)request.getAttribute("pagelast"   )).longValue();
    long pageFirst   = ((Long)request.getAttribute("pagefirst"  )).longValue();
%>

<dspace:layout titlekey="jsp.search.results.title">

    <%-- <h1>Search Results</h1> --%>
    
<h1><fmt:message key="jsp.search.results.title"/></h1>
    
  
<% if( qResults.getTotalSearchResults() == 0 )
{
 %>
    <%-- <p align="center">Search produced no results.</p> --%>
    <p align="center"><fmt:message key="jsp.search.general.noresults"/></p>
<%
}
else
{
%>
    <%-- <p align="center">Results <//%=qResults.getStart()+1%>-<//%=qResults.getStart()+qResults.getHitHandles().size()%> of --%>
	<p align="center"><fmt:message key="jsp.search.results.results">
        <fmt:param><%=qResults.getStart()+1%></fmt:param>
        <fmt:param><%=qResults.getStart()+qResults.getMaxResults()%></fmt:param>
        <fmt:param><%=qResults.getTotalSearchResults()%></fmt:param>
    </fmt:message></p>

<% } %>

<% if (communities.size() > 0 ) { %>
    <%-- <h3>Community Hits:</h3> --%>
    <h3><fmt:message key="jsp.search.results.comhits"/></h3>
    <dspace:communitylist  communities="<%= communities %>" />
<% } %>

<% if (collections.size() > 0 ) { %>   
    <br/>
    <%-- <h3>Collection hits:</h3> --%>
    <h3><fmt:message key="jsp.search.results.colhits"/></h3>
    <dspace:collectionlist collections="<%= collections %>" />
<% } %>

<% if (items.size() > 0) { %>
    <br/>
    <%-- <h3>Item hits:</h3> --%>
    <h3><fmt:message key="jsp.search.results.itemhits"/></h3>
    <dspace:itemlist items="<%= items %>" />
<% } %>

<p align="center">

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

    // create the URLs accessing the previous and next search result pages
    String prevURL =  request.getContextPath()
                    + searchScope 
                    + "/simple-search?query="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&amp;start=";

    String nextURL = prevURL;

    prevURL = prevURL
            + (pageCurrent-2) * qResults.getMaxResults();

    nextURL = nextURL
            + (pageCurrent) * qResults.getMaxResults();
    
    
if (pageFirst != pageCurrent)
{
    %><a href="<%= prevURL %>"><fmt:message key="jsp.search.general.previous" /></a><%
};


for( long q = pageFirst; q <= pageLast; q++ )
{
    String myLink = "<a href=\""
                    + request.getContextPath()
                    + searchScope 
                    + "/simple-search?query="
                    + URLEncoder.encode(query, "UTF-8")
                    + "&amp;start=";


    if( q == pageCurrent )
    {
        myLink = "" + q;
    }
    else
    {
        myLink = myLink
            + (q-1) * qResults.getMaxResults()
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

<object>
 <div align="center">
  <a href="<%= request.getContextPath() %>/subject-search">
  	<fmt:message key="jsp.controlledvocabulary.results.newsearch"/>
  </a>
 </div>
</object>
</p>

</dspace:layout>

