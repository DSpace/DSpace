<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

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
  -
  -   admin_button     - If the user is an admin
  --%>

<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="java.net.URLEncoder"            %>
<%@ page import="org.dspace.content.Community"   %>
<%@ page import="org.dspace.content.Collection"  %>
<%@ page import="org.dspace.content.Item"        %>
<%@ page import="org.dspace.search.QueryResults" %>
<%@ page import="org.dspace.sort.SortOption" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Set" %>

<%
    String order = (String)request.getAttribute("order");
    String ascSelected = (SortOption.ASCENDING.equalsIgnoreCase(order)   ? "selected=\"selected\"" : "");
    String descSelected = (SortOption.DESCENDING.equalsIgnoreCase(order) ? "selected=\"selected\"" : "");
    SortOption so = (SortOption)request.getAttribute("sortedBy");
    String sortedBy = (so == null) ? null : so.getName();

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
    int rpp         = qResults.getPageSize();
    int etAl        = qResults.getEtAl();

    // retain scope when navigating result sets
    String searchScope = "";
    if (community == null && collection == null) {
	searchScope = "";
    } else if (collection == null) {
	searchScope = "/handle/" + community.getHandle();
    } else {
	searchScope = "/handle/" + collection.getHandle();
    }

    // Admin user or not
    Boolean admin_b = (Boolean)request.getAttribute("admin_button");
    boolean admin_button = (admin_b == null ? false : admin_b.booleanValue());
%>

<dspace:layout titlekey="jsp.search.results.title">

<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/jquery/jquery-1.6.2.min.js"> </script>
<script type="text/javascript" src="<%= request.getContextPath() %>/static/js/search-results.js"> </script>


    <%-- <h1>Search Results</h1> --%>

<h1><fmt:message key="jsp.search.results.title"/></h1>

    <%-- Controls for a repeat search --%>
    <form action="simple-search" method="get">
        <table class="miscTable" align="center" summary="Table displaying your search results">
            <tr>
                <td class="evenRowEvenCol">
                    <table>
                        <tr>
                            <td>
                                <%-- <strong>Search:</strong>&nbsp;<select name="location"> --%>
                                <label for="tlocation"><strong><fmt:message key="jsp.search.results.searchin"/></strong></label>&nbsp;<select name="location" id="tlocation">
<%
    if (community == null && collection == null)
    {
        // Scope of the search was all of DSpace.  The scope control will list
        // "all of DSpace" and the communities.
%>
                                    <%-- <option selected value="/">All of DSpace</option> --%>
                                    <option selected="selected" value="/"><fmt:message key="jsp.general.genericScope"/></option>
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
                                    <%-- <option value="/">All of DSpace</option> --%>
                                    <option value="/"><fmt:message key="jsp.general.genericScope"/></option>
                                    <option selected="selected" value="<%= community.getHandle() %>"><%= community.getMetadata("name") %></option>
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
                                    <%-- <option value="/">All of DSpace</option> --%>
                                    <option value="/"><fmt:message key="jsp.general.genericScope"/></option>
                                    <option value="<%= community.getHandle() %>"><%= community.getMetadata("name") %></option>
                                    <option selected="selected" value="<%= collection.getHandle() %>"><%= collection.getMetadata("name") %></option>
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

<% if( qResults.getErrorMsg()!=null )
{
    String qError = "jsp.search.error." + qResults.getErrorMsg();
 %>
    <p align="center" class="submitFormWarn"><fmt:message key="<%= qError %>"/></p>
<%
}
else if( qResults.getHitCount() == 0 )
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
        <fmt:param><%=qResults.getStart()+qResults.getHitHandles().size()%></fmt:param>
        <fmt:param><%=qResults.getHitCount()%></fmt:param>
        <fmt:param><%=(float) qResults.getQueryTime() / 1000 %></fmt:param>
    </fmt:message></p>

<% } %>
    <%-- Include a component for modifying sort by, order, results per page, and et-al limit --%>
   <div align="center">
   <form method="get" action="<%= request.getContextPath() + searchScope + "/simple-search" %>">
   <table border="0">
       <tr><td>
           <input type="hidden" name="query" value="<%= StringEscapeUtils.escapeHtml(query) %>" />
           <fmt:message key="search.results.perpage"/>
           <select name="rpp">
<%
               for (int i = 5; i <= 100 ; i += 5)
               {
                   String selected = (i == rpp ? "selected=\"selected\"" : "");
%>
                   <option value="<%= i %>" <%= selected %>><%= i %></option>
<%
               }
%>
           </select>
           &nbsp;|&nbsp;
<%
           Set<SortOption> sortOptions = SortOption.getSortOptions();
           if (sortOptions.size() > 1)
           {
%>
               <fmt:message key="search.results.sort-by"/>
               <select name="sort_by">
                   <option value="0"><fmt:message key="search.sort-by.relevance"/></option>
<%
               for (SortOption sortBy : sortOptions)
               {
                   if (sortBy.isVisible())
                   {
                       String selected = (sortBy.getName().equals(sortedBy) ? "selected=\"selected\"" : "");
                       String mKey = "search.sort-by." + sortBy.getName();
                       %> <option value="<%= sortBy.getNumber() %>" <%= selected %>><fmt:message key="<%= mKey %>"/></option><%
                   }
               }
%>
               </select>
<%
           }
%>
           <fmt:message key="search.results.order"/>
           <select name="order">
               <option value="ASC" <%= ascSelected %>><fmt:message key="search.order.asc" /></option>
               <option value="DESC" <%= descSelected %>><fmt:message key="search.order.desc" /></option>
           </select>
           <fmt:message key="search.results.etal" />
           <select name="etal">
<%
               String unlimitedSelect = "";
               if (qResults.getEtAl() < 1)
               {
                   unlimitedSelect = "selected=\"selected\"";
               }
%>
               <option value="0" <%= unlimitedSelect %>><fmt:message key="browse.full.etal.unlimited"/></option>
<%
               boolean insertedCurrent = false;
               for (int i = 0; i <= 50 ; i += 5)
               {
                   // for the first one, we want 1 author, not 0
                   if (i == 0)
                   {
                       String sel = (i + 1 == qResults.getEtAl() ? "selected=\"selected\"" : "");
                       %><option value="1" <%= sel %>>1</option><%
                   }

                   // if the current i is greated than that configured by the user,
                   // insert the one specified in the right place in the list
                   if (i > qResults.getEtAl() && !insertedCurrent && qResults.getEtAl() > 1)
                   {
                       %><option value="<%= qResults.getEtAl() %>" selected="selected"><%= qResults.getEtAl() %></option><%
                       insertedCurrent = true;
                   }

                   // determine if the current not-special case is selected
                   String selected = (i == qResults.getEtAl() ? "selected=\"selected\"" : "");

                   // do this for all other cases than the first and the current
                   if (i != 0 && i != qResults.getEtAl())
                   {
%>
                       <option value="<%= i %>" <%= selected %>><%= i %></option>
<%
                   }
               }
%>
           </select>
           <%-- add results per page, etc. --%>
           <input type="submit" name="submit_search" value="<fmt:message key="search.update" />" />

<%
    if (admin_button)
    {
        %><input type="submit" name="submit_export_metadata" value="<fmt:message key="jsp.general.metadataexport.button"/>" /><%
    }
%>
           
       </td></tr>
   </table>
   </form>
   </div>

<%
    if(0 < communities.length || 0 < collections.length || 0 < items.length){
%>
<div id="search-results-division">
<% if (communities.length > 0 ) { %>
    <%-- <h3>Community Hits:</h3> --%>
    <h3><fmt:message key="jsp.search.results.comhits"/></h3>
    <dspace:communitylist  communities="<%= communities %>" />
<% } %>

<% if (collections.length > 0 ) { %>
    <br/>
    <%-- <h3>Collection hits:</h3> --%>
    <h3><fmt:message key="jsp.search.results.colhits"/></h3>
    <dspace:collectionlist collections="<%= collections %>" />
<% } %>

<% if (items.length > 0) { %>
    <br/>
    <%-- <h3>Item hits:</h3> --%>
    <h3><fmt:message key="jsp.search.results.itemhits"/></h3>
    <dspace:itemlist items="<%= items %>" sortOption="<%= so %>" authorLimit="<%= qResults.getEtAl() %>" />
<% } %>
</div>
<%
    }
%>

<p align="center">

<%
    // create the URLs accessing the previous and next search result pages
    String prevURL =  request.getContextPath()
                    + searchScope
                    + "/simple-search?query="
                    + URLEncoder.encode(query,"UTF-8")
                    + "&amp;sort_by=" + (so != null ? so.getNumber() : 0)
                    + "&amp;order=" + order
                    + "&amp;rpp=" + rpp
                    + "&amp;etal=" + etAl
                    + "&amp;start=";

    String nextURL = prevURL;

    prevURL = prevURL
            + (pageCurrent-2) * qResults.getPageSize();

    nextURL = nextURL
            + (pageCurrent) * qResults.getPageSize();


if (pageFirst != pageCurrent)
{
    %><a href="<%= prevURL %>"><fmt:message key="jsp.search.general.previous" /></a><%
};


for( int q = pageFirst; q <= pageLast; q++ )
{
    String myLink = "<a href=\""
                    + request.getContextPath()
                    + searchScope
                    + "/simple-search?query="
                    + URLEncoder.encode(query,"UTF-8")
                    + "&amp;sort_by=" + (so != null ? so.getNumber() : 0)
                    + "&amp;order=" + order
                    + "&amp;rpp=" + rpp
                    + "&amp;etal=" + etAl
                    + "&amp;start=";


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

<form id="dso-display" action="<%=request.getContextPath()%>/dso-display" method="post">
    <input type="hidden" name="query"   value="<%=StringEscapeUtils.escapeHtml(query)%>"/>
    <input type="hidden" name="rpp"     value="<%=rpp%>"/>
    <input type="hidden" name="page"   value="<%=pageCurrent%>"/>
    <input type="hidden" name="sort_by" value="<%=(so != null ? so.getNumber() : 0)%>"/>
    <input type="hidden" name="order"   value="<%=order%>"/>
    <input type="hidden" name="scope"   value="<%=collection != null ? collection.getHandle() : (community != null ? community.getHandle() : "")%>"/>
    <input type="hidden" name="redirectUrl"   value=""/>
</form>

</dspace:layout>

