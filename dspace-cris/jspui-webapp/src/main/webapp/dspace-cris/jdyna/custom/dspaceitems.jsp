<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@page import="org.apache.commons.lang.StringUtils"%>
<%@page import="java.util.List"%>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="java.net.URLEncoder"            %>
<%@ page import="org.dspace.sort.SortOption" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Map" %>
<%@page import="org.dspace.eperson.EPerson"%>
<%@ page import="org.dspace.content.Item"        %>
<%@page import="org.dspace.app.webui.cris.dto.ComponentInfoDTO"%>
<%@page import="it.cilea.osd.jdyna.web.Box"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>
<c:set var="root"><%=request.getContextPath()%></c:set>
<c:set var="info" value="${componentinfomap}" scope="page" />
<%
	
	Box holder = (Box)request.getAttribute("holder");
	ComponentInfoDTO info = ((Map<String, ComponentInfoDTO>)(request.getAttribute("componentinfomap"))).get(holder.getShortName());
	List<String[]> subLinks = (List<String[]>) request
            .getAttribute("activeTypes"+info.getRelationName());
            		
	if (info.getItems().length > 0) {
%>
<c:set var="info" value="<%= info %>" scope="request" />
<div class="panel-group" id="${holder.shortName}">
	<div class="panel panel-default">
    	<div class="panel-heading">
    		<h4 class="panel-title">
        		<a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
          			${holder.title} <fmt:message
				key="jsp.layout.dspace.detail.fieldset-legend.component.boxtitle.${info[holder.shortName].type}"/>
        		</a>
        		<% if(subLinks!=null && subLinks.size()>1) {%>
        		<div class="btn-group">
			    <button type="button" class="btn btn-sm btn-default dropdown-toggle" data-toggle="dropdown">
    				<fmt:message key="jsp.components.button.seealso"/> <span class="fa fa-caret-down"></span>	
  				</button>
				<ul class="dropdown-menu dropdown-menu-right" role="menu">
				<% for (String[] sub : subLinks ) { %>
					<li><a href="?open=<%= sub[0] %>"><%= sub[1] %></a></li>
				<% } %>
				</ul>
				</div>
				<% } %>	
        	</h4>
    	</div>
	<div id="collapseOne${holder.shortName}" class="panel-collapse collapse in">
		<div class="panel-body">	
	<p>


<!-- prepare pagination controls -->
<%
    // create the URLs accessing the previous and next search result pages
    StringBuilder sb = new StringBuilder();
	sb.append("<div align=\"center\">");
	sb.append("Result pages:");
	
    String prevURL = info.buildPrevURL(); 
    String nextURL = info.buildNextURL();


if (info.getPagefirst() != info.getPagecurrent()) {
  sb.append(" <a class=\"pagination\" href=\"");
  sb.append(prevURL);
  sb.append("\">previous</a>");
};

for( int q = info.getPagefirst(); q <= info.getPagelast(); q++ )
{
   	String myLink = info.buildMyLink(q);
    sb.append(" " + myLink);
} // for

if (info.getPagetotal() > info.getPagecurrent()) {
  sb.append(" <a class=\"pagination\" href=\"");
  sb.append(nextURL);
  sb.append("\">next</a>");
}

sb.append("</div>");

%>


<div align="center" class="browse_range">

	<p align="center"><fmt:message key="jsp.search.results.results">
        <fmt:param><%=info.getStart()+1%></fmt:param>
        <fmt:param><%=info.getStart()+info.getItems().length%></fmt:param>
        <fmt:param><%=info.getTotal()%></fmt:param>
        <fmt:param><%=(float)info.getSearchTime() / 1000%></fmt:param>
    </fmt:message></p>

</div>
<%
if (info.getPagetotal() > 1)
{
%>
<%= sb %>
<%
	}
%>
			
<form id="sortform<%= info.getType() %>" action="#<%= info.getType() %>" method="get">
	   <input id="sort_by<%= info.getType() %>" type="hidden" name="sort_by<%= info.getType() %>" value=""/>
       <input id="order<%= info.getType() %>" type="hidden" name="order<%= info.getType() %>" value="<%= info.getOrder() %>" />
	   <input type="hidden" name="open" value="<%= info.getType() %>" />
</form>

<dspace:itemlist items="<%= (Item[])info.getItems() %>" sortOption="<%= info.getSo() %>" authorLimit="<%= info.getEtAl() %>" order="<%= info.getOrder() %>"/>

<script type="text/javascript"><!--
    function sortBy(sort_by, order) {
        j('#sort_by<%= info.getType() %>').val(sort_by);
        j('#order<%= info.getType() %>').val(order);
        j('#sortform<%= info.getType() %>').submit();        
    }
--></script>

<%-- show pagination controls at bottom --%>
<%
	if (info.getPagetotal() > 1)
	{
%>
<%= sb %>
<%
	}
%>


</p>
</div>
										  </div>
								   </div>
							</div>

<% } %>
