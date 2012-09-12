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
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>

<%
	//////////////////////////////////////////////////////////////////////////////////////////
	System.out.println("anna");
	//Community [] communityArray = (Community[] )request.getAttribute("communities");
	String query1 			= request.getParameter("query1") == null ? "" : request.getParameter("query1");
	String query2 			= request.getParameter("query2") == null ? "" : request.getParameter("query2");
	String query3 			= request.getParameter("query3") == null ? "" : request.getParameter("query3");
	String query4 			= request.getParameter("query4") == null ? "" : request.getParameter("query4");
	String query5 			= request.getParameter("query5") == null ? "" : request.getParameter("query5");
	String query6 			= request.getParameter("query6") == null ? "" : request.getParameter("query6");

	String field1 			= request.getParameter("field1") == null ? "ANY" : request.getParameter("field1");
	String field2 			= request.getParameter("field2") == null ? "ANY" : request.getParameter("field2");
	String field3 			= request.getParameter("field3") == null ? "ANY" : request.getParameter("field3");
	String field4 			= request.getParameter("field4") == null ? "ANY" : request.getParameter("field4");
	String field5 			= request.getParameter("field5") == null ? "ANY" : request.getParameter("field5");
	String field6 			= request.getParameter("field6") == null ? "ANY" : request.getParameter("field6");

	String conjunction1 	= request.getParameter("conjunction1") == null ? "AND" : request.getParameter("conjunction1");
	String conjunction2 	= request.getParameter("conjunction2") == null ? "AND" : request.getParameter("conjunction2");
	
	//Read the configuration to find out the search indices dynamically
	ArrayList<String> dateSearchIndices = new ArrayList<String>();
	int dateIdx = 1;
	String dateIndexConfig;
	while ( ((dateIndexConfig = ConfigurationManager.getProperty("search.index.date." + dateIdx))) != null){
		dateSearchIndices.add(dateIndexConfig);
		dateIdx++;
	}
	
	int idx = 1;
	String definition;
	ArrayList<String> searchIndices = new ArrayList<String>();
	String dateIndexString = "";

	while ( ((definition = ConfigurationManager.getProperty("search.index." + idx))) != null){
	        
		String index = definition.substring(0, definition.indexOf(":"));
		searchIndices.add(index);
		if (dateSearchIndices.contains(index)){
			if (dateIndexString.equals(""))
				dateIndexString += String.valueOf(idx);
			else
				dateIndexString += "_"+String.valueOf(idx);
		}
	    idx++;
	}
	//////////////////////////////////////////////////////////////////////////////////////////

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

    <%-- <h1>Search Results</h1> --%>

<h1><fmt:message key="jsp.search.results.title"/></h1>

<!--//////////////////////////////////////////////////////////-->
<form action="<%= request.getContextPath() %>/simple-search" method="get">
<input type="hidden" name="advanced" value="true"/>
<table class="miscTable" align="center" summary="Search DSpace Form">
    <tr>
      <td class="oddRowEvenCol" align="center">	
      	<p><strong><fmt:message key="jsp.search.advanced.search"/></strong>&nbsp;
		<select name="location">
			<option selected="selected" value="/"><fmt:message key="jsp.general.genericScope"/></option>

<%
        for (int i = 0; i < communityArray.length; i++)
        {
%>
			<option value="<%= communityArray[i].getHandle() %>"><%= communityArray[i].getMetadata("name") %></option>
<%
        }
%>
		</select>
              </p>
         <table cellspacing="2" border="0" width="80%">
		  <tr>
                <td class="evenRowEvenCol">
                    <table border="0">
            <tr>
            <td width="12%" align="left" valign="top"></td>
              <td width="20%" align="left" valign="top" nowrap="nowrap">
                <%-- Search type: <br> --%>
                <label for="tfield1"><fmt:message key="jsp.search.advanced.type"/></label> <br/>
                  <select onchange="onFieldChange('<fmt:message key="jsp.search.advanced.searchfor"/>', '<%= dateIndexString %>');" name="field1" id="tfield1">
                    <option value="ANY" <%= field1.equals("ANY") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.any"/></option>
					<%
						for (String index : searchIndices)
						{
							String key = "jsp.search.advanced.type." + index;
					%>
							<option value="<%= index %>" <%= field1.equals(index) ? "selected=\"selected\"" : "" %>><fmt:message key="<%= key %>"/></option>
					<%
						}
					%>
                  </select>
            </td>
			
			<td><table border="0" cellpadding="0" cellspacing="0">
            <tr id="date_row" height="25px">
               <%
                String displayType = "none"; 
                String displayType_2 = "none";
                if (field1.equals("date")){
                	displayType = "block";
                	if (field4 != null && field4.equals("between")){
                		displayType_2 = "block";
                	}
                }
             %>
            <td id="tfield4_column" width="100px" align="left" valign="top" nowrap="nowrap" style="display:<%= displayType%>">
                <%-- Search type: <br> --%>
                
                <label id="tfield4_label" for="tfield4" style="display:<%= displayType%>; font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.option"/></label>
                  <select onchange="onOptionChange('<fmt:message key="jsp.search.advanced.option.from"/>','<fmt:message key="jsp.search.advanced.searchfor"/>');" name="field4" id="tfield4" style="display:<%= displayType%>; font-family:Arial, Helvetica, sans-serif;font-size:12px;height:20px;">
                  	<option value="equal" <%= field4.equals("equal") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.equal"/></option>
                    <option value="before" <%= field4.equals("before") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.before"/></option>
                    <option value="after" <%= field4.equals("after") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.after"/></option>
                  	<option value="between" <%= field4.equals("between") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.between"/></option>
                  </select>
            </td>

			<td align="left" valign="top" nowrap="nowrap" width="68%">
                <%-- Search for: <br> --%>
                <% if (field1 != null && field4!=null && field1.equals("date") && field4.equals("between")){ %>
              	<label id="tquery1_label" for="tquery1"><fmt:message key="jsp.search.advanced.option.from"/></label> <br/>
              	<%} else {%>
              	<label id="tquery1_label" for="tquery1"><fmt:message key="jsp.search.advanced.searchfor"/></label> <br/>
              	<%}%>
                <input onkeyup="validateDateNumber(true, '<%= dateIndexString %>');" type="text" name="query1" id="tquery1" value="<%=StringEscapeUtils.escapeHtml(query1)%>" size="30" />
                <br/>
                <label id="tquery4_label" for="tquery4" style="display:<%= displayType_2%> ;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.to"/></label>
                <input onkeyup="validateDateNumber(true, '<%= dateIndexString %>');" type="text" name="query4" id="tquery4"  value="<%=StringEscapeUtils.escapeHtml(query4)%>" size="30" style="display:<%= displayType_2%>;"/>
              <p/>
            </td>

			</tr>
          </table>
            </td>
          </tr>
          <tr>
            <td width="12%" align="left" valign="top">
              <select name="conjunction1">
                <option value="AND" <%= conjunction1.equals("AND") ? "selected=\"selected\"" : "" %>> <fmt:message key="jsp.search.advanced.logical.and" /> </option>
		<option value="OR" <%= conjunction1.equals("OR") ? "selected=\"selected\"" : "" %>> <fmt:message key="jsp.search.advanced.logical.or" /> </option>
                <option value="NOT" <%= conjunction1.equals("NOT") ? "selected=\"selected\"" : "" %>> <fmt:message key="jsp.search.advanced.logical.not" /> </option>
              </select>
            </td>
            <td width="20%" align="left" valign="top" nowrap="nowrap">
                  <select onchange="onFieldChange2('<%= dateIndexString %>');" name="field2" id="tfield2">
                    <option value="ANY" <%= field2.equals("ANY") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.any"/></option>
                    <%
						for (String index : searchIndices)
						{
							String key = "jsp.search.advanced.type." + index;
					%>
							<option value="<%= index %>" <%= field2.equals(index) ? "selected=\"selected\"" : "" %>><fmt:message key="<%= key %>"/></option>
					<%
						}
					%>
                  </select>
           </td>

		   <td>
           <table border="0" cellpadding="0" cellspacing="0">
            <tr id="date_row2" height="25px">
              <%
                String displayType2 = "none"; 
                String displayType2_2 = "none";
                if (field2.equals("date")){
                	displayType2 = "block";
                	if (field5 != null && field5.equals("between")){
                		displayType2_2 = "block";
                	}
                }
             %>
            <td id="tfield5_column" width="100px" align="left" valign="top" nowrap="nowrap" style="display:<%= displayType2%>">
                <%-- Search type: <br> --%>
                <label id="tfield5_label" for="tfield5" style="display:none; font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.option"/></label>
                  <select onchange="onOptionChange2('<fmt:message key="jsp.search.advanced.option.from"/>','<fmt:message key="jsp.search.advanced.searchfor"/>');" name="field5" id="tfield5" style="display:<%= displayType2%> ;font-family:Arial, Helvetica, sans-serif;font-size:12px;height:20px;">
                  	<option value="equal" <%= field5.equals("equal") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.equal"/></option>
                    <option value="before" <%= field5.equals("before") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.before"/></option>
                    <option value="after" <%= field5.equals("after") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.after"/></option>
                  	<option value="between" <%= field5.equals("between") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.between"/></option>
                  </select>
            </td>

            <td align="left" valign="top" nowrap="nowrap" width="68%">
                <% if (field2 != null && field5!=null && field2.equals("date") && field5.equals("between")){ %>
              	<label id="tquery2_label" for="tquery2" style="display:block;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.from"/></label>
              	<%} else {%>
              	<label id="tquery2_label" for="tquery2" style="display:none;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.searchfor"/></label>
              	<%}%>
              <input onkeyup="validateDateNumber2(true, '<%= dateIndexString %>');" type="text" name="query2" id="tquery2" value="<%=StringEscapeUtils.escapeHtml(query2)%>" size="30"/>
               <br/>
               <label id="tquery5_label" for="tquery5" style="display:<%= displayType2_2%> ;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.to"/></label>
               <input onkeyup="validateDateNumber2(true, '<%= dateIndexString %>');" type="text" name="query5" id="tquery5" style="display:<%= displayType2_2%>" value="<%=StringEscapeUtils.escapeHtml(query5)%>" size="30" style="font-family:Arial, Helvetica, sans-serif;font-size:12px;height:15px;"/>
              <p/>
            </td>
			</tr>
          </table>
            </td>
          </tr>
          <tr>
            <td width="12%" align="left" valign="top">
              <select name="conjunction2">
                <option value="AND" <%= conjunction2.equals("AND") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.logical.and" /> </option>
                <option value="OR" <%= conjunction2.equals("OR") ? "selected=\"selected\"" : "" %>> <fmt:message key="jsp.search.advanced.logical.or" /> </option>
                <option value="NOT" <%= conjunction2.equals("NOT") ? "selected=\"selected\""  : "" %>> <fmt:message key="jsp.search.advanced.logical.not" /> </option>
              </select>
            </td>
            <td width="20%" align="left" valign="top" nowrap="nowrap">

                  <select onchange="onFieldChange3('<%= dateIndexString %>');" name="field3" id="tfield3">
                    <option value="ANY" <%= field3.equals("ANY") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.type.any"/></option>
                    <%
						for (String index : searchIndices)
						{
							String key = "jsp.search.advanced.type." + index;
					%>
							<option value="<%= index %>" <%= field3.equals(index) ? "selected=\"selected\"" : "" %>><fmt:message key="<%= key %>"/></option>
					<%
						}
					%>
                  </select>
                  <br/>
            </td>
			<td>
           <table border="0" cellpadding="0" cellspacing="0">
            <tr id="date_row3" height="25px">
               <%
                String displayType3 = "none"; 
                String displayType3_2 = "none";
                if (field3.equals("date")){
                	displayType = "block";
                	if (field6 != null && field6.equals("between")){
                		displayType3_2 = "block";
                	}
                }
             %>
            <td id="tfield6_column" width="100px" align="left" valign="top" nowrap="nowrap" style="display:<%= displayType3%>">
                <%-- Search type: <br> --%>
                <label id="tfield6_label" for="tfield6" style="display:none; font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.option"/></label>
                  <select onchange="onOptionChange3('<fmt:message key="jsp.search.advanced.option.from"/>','<fmt:message key="jsp.search.advanced.searchfor"/>');" name="field6" id="tfield6" style="display:<%= displayType3%> ;font-family:Arial, Helvetica, sans-serif;font-size:12px;height:20px;">
                  	<option value="equal" <%= field6.equals("equal") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.equal"/></option>
                    <option value="before" <%= field6.equals("before") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.before"/></option>
                    <option value="after" <%= field6.equals("after") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.after"/></option>
                  	<option value="between" <%= field6.equals("between") ? "selected=\"selected\"" : "" %>><fmt:message key="jsp.search.advanced.option.between"/></option>
                  </select>
            </td>
            <td align="left" valign="top" nowrap="nowrap" width="68%">
                 <% if (field3 != null && field6!=null && field3.equals("date") && field6.equals("between")){ %>
              	<label id="tquery3_label" for="tquery3" style="display:block;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.from"/></label>
              	<%} else {%>
              	<label id="tquery3_label" for="tquery3" style="display:none;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.searchfor"/></label>
              	<%}%>           
               <input onkeyup="validateDateNumber3(true, '<%= dateIndexString %>');" type="text" name="query3" id="tquery3" value="<%=StringEscapeUtils.escapeHtml(query3)%>" size="30"/>
               <br/>
                <label id="tquery6_label" for="tquery6" style="display:<%= displayType3_2%>;font-family: "verdana","Arial","Helvetica",sans-serif; font-size: 12pt; font-weight: none; color: #000000"><fmt:message key="jsp.search.advanced.option.to"/></label>
                <input onkeyup="validateDateNumber3(true, '<%= dateIndexString %>');" type="text" name="query6" id="tquery6" style="display:<%= displayType3_2%>" value="<%=StringEscapeUtils.escapeHtml(query6)%>" size="30" style="font-family:Arial, Helvetica, sans-serif;font-size:12px;height:15px;"/>
              <p/>
            </td>
			</tr>
		  </table>
        </td>
  </tr>
  </table>
</td>
</tr>
  <tr>
    <td valign="bottom" align="right" nowrap="nowrap">
      &nbsp; &nbsp; &nbsp;
      <%-- <input type="submit" name="submit" value="Search"> --%>
	  <div id="error_log" style="color:red; display:none" align="left"></div>
      <input type="submit" name="submit" value="<fmt:message key="jsp.search.advanced.search2"/>" />
            &nbsp;  &nbsp; &nbsp;
      <%-- <input type="reset" name="reset" value=" Clear "> --%>
      <input type="reset" name="reset" value=" <fmt:message key="jsp.search.advanced.clear"/>" />
    </td>
  </tr>
</table>
</td>
</tr>
</table>
</form>

<!---------------------------------------------------->

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

</dspace:layout>

