<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="java.util.Map"%>
<%@page import="org.dspace.app.webui.servlet.DoiFactoryServlet"%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.List"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="java.net.URLEncoder"            %>
<%@ page import="org.dspace.content.Item"        %>
<%@ page import="org.dspace.sort.SortOption" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Set" %>
<%@page import="org.dspace.eperson.EPerson"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>


<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<style type="text/css">	
	.divdoi {	display: none;  }
	
	.btndoi 
		{ background-position: 189px 50%; }
		
	.strike {
		text-decoration: line-through;
	}
	
	html body.undernavigation main#content div.container div div div.col-md-12 form table.table.table-hover tbody tr th#t5 {
		width: 15em;
	} 
	
	.spandoi {
		display: block;
	}
</style>
<c:set var="root"><%=request.getContextPath()%></c:set>
<dspace:layout locbar="link" navbar="admin" style="submission" titlekey="jsp.dspace-admin.doi.criteriaresult">
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
<%
	EPerson user = (EPerson) request.getAttribute("dspace.current.user");
	String order = (String)request.getAttribute("order");
	String type = (String)request.getAttribute("type");
	SortOption so = (SortOption)request.getAttribute("sortedBy");
	String sortedBy = (so == null) ? null : so.getName();
	Item      [] items       = (Item[])request.getAttribute("items");
	Map<Integer, String> doi2items = (Map<Integer, String>)request.getAttribute("doi2items");
	int pageTotal   = ((Integer)request.getAttribute("pagetotal"  )).intValue();
	int pageCurrent = ((Integer)request.getAttribute("pagecurrent")).intValue();
	int pageLast    = ((Integer)request.getAttribute("pagelast"   )).intValue();
	int pageFirst   = ((Integer)request.getAttribute("pagefirst"  )).intValue();
	int rpp         = ((Integer)request.getAttribute("rpp"  )).intValue();
	int etAl        = ((Integer)request.getAttribute("etAl"  )).intValue();
	int total		= ((Long)request.getAttribute("total"  )).intValue();	
	int start		= ((Integer)request.getAttribute("start"  )).intValue();	
	String prefixDOI = (String)request.getAttribute("prefixDOI");
	String keyH1page = "jsp.dspace-admin.doi.fix." + type;
	if (items!=null && items.length>0) {
%>

<div>


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
                    + "&amp;etal=" + etAl
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
                    + "&amp;etal=" + etAl
                    + "&amp;start=";

    if( q == pageCurrent )
    {
        myLink = "<li><span class=''>" + q + "</span>";
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
<h1><fmt:message key="<%=keyH1page%>"/></h1>
<div>
	<p align="center"><fmt:message key="jsp.dspace-admin.doi.results.search">
        <fmt:param><%=start+1%></fmt:param>
        <fmt:param><%=start+items.length %></fmt:param>
        <fmt:param><%=total%></fmt:param>        
    </fmt:message></p>
<%
if (pageTotal > 1)
{
%>
<%= sb %>
<%
	}
%>
<div class="col-md-12">
<form id="sortform" action="#<%= type %>" method="get">
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
		   <input type="hidden" name="open" value="<%= type %>" />
		   <input id="start" type="hidden" name="start" value="<%= start %>" />
</form>
</div>
<div class="col-md-12">			
<form method="post" action="<%= request.getContextPath() %>/dspace-admin/doifactory/<%= type%>" name="itemform_<%= type%>">			
<dspace:itemlist items="<%= items %>" sortOption="<%= so %>" order="<%= order.toUpperCase() %>" 
	authorLimit="<%= etAl %>" itemStart="1" radioButton="false" inputName="builddoi"/>
       
<div style="display: none;">       
	<input id="submitallreal" type="submit" name="submit" value="<%=DoiFactoryServlet.DOI_ALL%>" />
	<input id="submitanyreal" type="submit" name="submit" value="<%=DoiFactoryServlet.DOI_ANY%>" />
	<input id="submitexcludereal" type="submit" name="submit" value="<%=DoiFactoryServlet.EXCLUDE_ANY%>" />	
</div>

	<input id="submitall" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.all"><fmt:param value="<%= total %>"></fmt:param></fmt:message>" />
	<input id="submitany" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.any"/>" />
	<input id="submitexclude" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.exclude"/>" />
</form>			
</div>
<%-- show pagniation controls at bottom --%>
<%
	if (pageTotal > 1)
	{
%>
<%= sb %>
<%
	}
%>
</div>

</div>


<script type="text/javascript">



	jQuery(document).ready(function() {
		var ctrl = jQuery('<th/>').attr({id:'t5'}).html('Proposed DOI');
		var table = jQuery("html body.undernavigation main#content div.container div div div.col-md-12 form table.table.table-hover tbody tr:first").append(ctrl);;			
	});

	
	var maps = new Object();

	<% 
	
	for(Integer ss : doi2items.keySet()) {		
	%>
		maps[<%= ss%>] = <%= "'"+doi2items.get(ss)+"';"%>
	<%		
	}	
	%>
	
	jQuery('input[name="builddoi"]').each(function(index) {
		
		var ctrl0 = jQuery('<td/>').attr({id:'tddoi_'+jQuery(this).val()});		
		var ctrl4 = jQuery('<span/>').attr({id:'spandoi_'+jQuery(this).val()}).addClass('spandoi').html(maps[jQuery(this).val()]);
			
		var ctrl1 = jQuery('<button/>').attr({ type: 'button', id:"btndoi_"+jQuery(this).val(), value:""}).addClass("btn btn-default btndoi").html('<span class="fa fa-pencil"></span><fmt:message key="jsp.tools.general.modify"/>');
		var ctrl2 = jQuery('<div/>').attr({id:"divdoi_btndoi_"+jQuery(this).val()}).addClass("divdoi");
		var ctrl5 = jQuery('<span/>').attr({id:'spanprefixdoi_'+jQuery(this).val()}).addClass('spanprefixdoi').html('<%= prefixDOI %>');
		var ctrl3 = jQuery('<input/>').attr({ type: 'text', id:'custombuilddoiid_'+jQuery(this).val(), name:'custombuilddoi_'+jQuery(this).val(),value:""}).addClass("doitext");
		
		jQuery(this).parent().parent().append(ctrl0);		
		jQuery("#tddoi_"+jQuery(this).val()).append(ctrl4);
		jQuery("#tddoi_"+jQuery(this).val()).append(ctrl1);
		jQuery("#tddoi_"+jQuery(this).val()).append(ctrl2);

		jQuery("#divdoi_btndoi_"+jQuery(this).val()).append(ctrl5);
		jQuery("#divdoi_btndoi_"+jQuery(this).val()).append(ctrl3);
		
	});


	jQuery('.submitbutton').click(
		function() {
			jQuery('#' + (jQuery(this).attr('id')) + "real").click();
		}		
	);	


	jQuery(window).keydown(function(event) {
		if (event.keyCode == 13) {
			event.preventDefault();
			return false;
		}
	});

	jQuery('.btndoi').click(function() {
		jQuery('#divdoi_' + this.id).toggle();
		var id = this.id;
		var index = id.indexOf("_");
		var suffixID = id.substr(index + 1);
		if (jQuery('#custombuilddoiid_' + suffixID).val().length == 0) {
			jQuery('#spandoi_' + suffixID).removeClass("strike");
		}
	});

	jQuery('.doitext').change(function() {
		var id = this.name;
		var index = id.indexOf("_");
		var suffixID = id.substr(index + 1);
		if (jQuery('#custombuilddoiid_' + suffixID).val().length == 0) {
			jQuery('#spandoi_' + suffixID).removeClass("strike");
		} else {
			jQuery('#spandoi_' + suffixID).addClass("strike");
		}
	});
</script>
<% } else { %>	
	<fmt:message key="jsp.search.doi.form.button.noresult.found"/>
<% } %>
<ul class="clearfix">
	<li>
		<a href="<%= request.getContextPath() %>/dspace-admin/doi"><fmt:message key="jsp.layout.hku.tool.link.doi.home"/></a>
	</li>
	<li>
		<a href="<%= request.getContextPath() %>/dspace-admin/doipendings"><fmt:message key="jsp.layout.hku.tool.link.doi.pendings"/></a>
	</li>
	<li>
		<a href="<%= request.getContextPath() %>/dspace-admin/doiqueued"><fmt:message key="jsp.layout.hku.tool.link.doi.queued"/></a>
	</li>	
</ul> 	

</dspace:layout>