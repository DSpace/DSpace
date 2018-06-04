<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="java.util.List"%>
<%@page import="org.dspace.content.Item"%>
<%@page import="org.dspace.app.webui.servlet.DoiPendingServlet"%>
<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@page import="java.util.Map"%>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@ page import="org.dspace.sort.SortOption" %>
<%@ page import="java.util.Set" %>
<%
        String order = (String)request.getAttribute("order");
		if (order == null)
		{
			order = "ASC";
		}
    	SortOption so = (SortOption)request.getAttribute("sort_by");
    	String sortedBy = (so == null) ? null : so.getName();
    	int pageTotal   = ((Integer)request.getAttribute("pagetotal"  )).intValue();
    	int pageCurrent = ((Integer)request.getAttribute("pagecurrent")).intValue();
    	int pageLast    = ((Integer)request.getAttribute("pagelast"   )).intValue();
    	int pageFirst   = ((Integer)request.getAttribute("pagefirst"  )).intValue();
    	int rpp         = ((Integer)request.getAttribute("rpp"  )).intValue();
    	int total		= ((Long)request.getAttribute("total"  )).intValue();	
    	int start		= ((Integer)request.getAttribute("start"  )).intValue();    	
%>

<style type="text/css">	
	.spandoi {
		display: block;
	}
</style>
<%
	Item[] results = (Item[])request.getAttribute("results");
	Map<Integer, List<String>> doi2items = (Map<Integer, List<String>>)request.getAttribute("doi2items");
%>
<dspace:layout locbar="link" navbar="admin" style="submission" titlekey="jsp.dspace-admin.doi.pendings">
<c:set var="messageproposeddoi"><fmt:message key="jsp.dspace-admin.doi.table.header.proposeddoi"/></c:set>
<c:set var="messagenote"><fmt:message key="jsp.dspace-admin.doi.table.header.note"/></c:set>

<script type="text/javascript">
<!--
var j = jQuery.noConflict();
//-->

function sortBy(pos, ord){
	document.getElementById("sort_by").value=pos;
	document.getElementById("order").value=ord;
	document.getElementById("sortform").submit();
	
}
</script>
<!-- prepare pagination controls -->
<%
    // create the URLs accessing the previous and next search result pages
    StringBuilder sb = new StringBuilder();
	sb.append("<div><ul class=\"pagination\">");
	sb.append("<li><span>Result pages:</span></li>");
	
    String prevURL =  "?"
                    + "&amp;sort_by=" + (so != null ? so.getNumber() : 0)
                    + "&amp;order=" + order
                    + "&amp;rpp=" + rpp
                    + "&amp;start=";

    String nextURL = prevURL;

    prevURL = prevURL
            + (pageCurrent-2) * rpp;

    nextURL = nextURL
            + (pageCurrent) * rpp;


if (pageFirst != pageCurrent) {
  sb.append("<li><a class=\"\" href=\"");
  sb.append(prevURL);
  sb.append("\">previous</a></li>");
}

for( int q = pageFirst; q <= pageLast; q++ )
{
    String myLink = "<li><a class='' href=\""
    				+ "?"
                    + "sort_by=" + (so != null ? so.getNumber() : 0)
                    + "&amp;order=" + order
                    + "&amp;rpp=" + rpp
                    + "&amp;start=";

    if( q == pageCurrent )
    {
        myLink = "<li><span>" + q + "</span>";
    }
    else
    {
        myLink = myLink
            + (q-1) * rpp
            + "\">"
            + q
            + "</a>";
    }
    sb.append(" " + myLink+"</li>");
} // for

if (pageTotal > pageCurrent) {
  sb.append("<li><a class=\"\" href=\"");
  sb.append(nextURL);
  sb.append("\">next</a></li>");
}

sb.append("</ul></div>");

%>

<div class="row">
      <h1><fmt:message key="jsp.dspace-admin.doi.pendings"/></h1>
<% if(results!=null && results.length>0) {%>
	<p align="center"><fmt:message key="jsp.dspace-admin.doi.results.search">
        <fmt:param><%=start+1%></fmt:param>
        <fmt:param><%=start+results.length %></fmt:param>
        <fmt:param><%=total%></fmt:param>        
    </fmt:message></p>
<% }
if (pageTotal > 1)
{
%>
<%= sb %>
<%
	}
%>      
</div>
<p><fmt:message key="jsp.dspace-admin.doi.general-description" /></p>
<form id="sortform" action="" method="get">
<input id="sort_by" type="hidden" name="sort_by"
<%
			Set<SortOption> sortOptions = SortOption.getSortOptions();
			if (sortOptions.size() > 1)
			{
               for (SortOption sortBy : sortOptions)
               {
                   if (sortBy.isVisible())
                   {
                       String selected = (sortBy.getName().equals(sortedBy) ? "value=\""+ sortBy.getName()+"\"" : "");
                   }
               }
			}
%>
/>

           <input id="order" type="hidden" name="order" value="<%= order %>" />
           <input id="start" type="hidden" name="start" value="<%= start %>" />
</form>

<% if(results!=null && results.length>0) {%>
<form method="post" action="<%= request.getContextPath() %>/dspace-admin/doipendings" name="pendingsform">			

<dspace:itemlist items="<%= results %>" itemStart="1" radioButton="false" inputName="pendingdoi" 
	sortOption="<%= so %>" order="<%= order.toUpperCase() %>" />
       
<div style="display: none;">       
	<input id="submitallreal" type="submit" name="submit" value="<%=DoiPendingServlet.EXCLUDE_ALL%>" />
	<input id="submitanyreal" type="submit" name="submit" value="<%=DoiPendingServlet.EXCLUDE_ANY%>" />		
</div>
	
	<input id="submitall" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.pendingall"><fmt:param value="<%= total %>"></fmt:param></fmt:message>" />
	<input id="submitany" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.pendingany"/>" />

</form>		
<script type="text/javascript">

j(document).ready(function() {
	var ctrl = j('<th/>').attr({id:'t5'}).html('${messageproposeddoi}');
	var ctrl2 = j('<th/>').attr({id:'t5'}).html('${messagenote}');
				   
	var table = j("html body.undernavigation main#content div.container form table.table.table-hover tbody tr:first").append(ctrl);
	var table2 = j("html body.undernavigation main#content div.container form table.table.table-hover tbody tr:first").append(ctrl2);
});


var maps = new Object();
var maps2 = new Object();

<% 

for(Integer ss : doi2items.keySet()) {		
%>
	maps[<%= ss%>] = <%= "'"+doi2items.get(ss).get(0)+"';"%>
	maps2[<%= ss%>] = <%= "'"+StringEscapeUtils.escapeJavaScript(doi2items.get(ss).get(1))+"';"%>
<%		
}	
%>


j('input[name="pendingdoi"]').each(function(index) {
	
	var ctrl0 = j('<td/>').attr({id:'tddoi_'+j(this).val()});		
	var ctrl4 = j('<span/>').attr({id:'spandoi_'+j(this).val()}).addClass('spandoi').html(maps[j(this).val()]);
		
	
	
	j(this).parent().parent().append(ctrl0);		
	j("#tddoi_"+j(this).val()).append(ctrl4);
	
});

j('input[name="pendingdoi"]').each(function(index) {
	
	var ctrl0 = j('<td/>').attr({id:'tdnote_'+j(this).val()});		
	var ctrl4 = j('<span/>').attr({id:'spannote_'+j(this).val()}).addClass('spannote').html(maps2[j(this).val()]);
		
	
	
	j(this).parent().parent().append(ctrl0);		
	j("#tdnote_"+j(this).val()).append(ctrl4);
	
});

	j('.submitbutton').click(
		function() {
			j('#' + (j(this).attr('id')) + "real").click();
		}		
	);

	j(window).keydown(function(event) {
		if (event.keyCode == 13) {
			event.preventDefault();
			return false;
		}
	});
</script>	
<% } else { %>	
	<fmt:message key="jsp.search.doi.form.button.nopending.found"/>
<% } %>
<ul class="clearfix">
	<li>
		<a href="<%=request.getContextPath() %>/dspace-admin/doi"><fmt:message key="jsp.layout.hku.tool.link.doi.home"/></a>
	</li>
	<li>
		<a href="<%= request.getContextPath() %>/dspace-admin/doiqueued"><fmt:message key="jsp.layout.hku.tool.link.doi.queued"/></a>
	</li>
</ul> 	


</dspace:layout>