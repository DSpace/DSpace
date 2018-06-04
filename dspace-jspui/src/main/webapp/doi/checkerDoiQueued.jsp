<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@page import="org.dspace.app.webui.servlet.DoiQueuedServlet"%>
<%@page import="org.apache.commons.lang.StringEscapeUtils"%>
<%@page import="java.util.List"%>
<%@page import="org.dspace.content.Item"%>
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
    	SortOption so = (SortOption)request.getAttribute("sortedBy");
    	String sortedBy = (so == null) ? null : so.getName();
%>

<style type="text/css">	
	.divdoi {	display: none;  }
	
	.btndoi 
		{ width: 16px; height: 16px; background-image: url(../image/ui-icons_222222_256x240.png); background-position: 189px 50%; }
		
	.strike {
		text-decoration: line-through;
	}
	
	html body table tbody tr td.text div table.miscTable tbody tr td form table.miscTable tbody tr th#t5 {
		width: 15em;
	} 
	
	.spandoi {
		display: block;
	}
</style>

<%
	Item[] results = (Item[])request.getAttribute("results");
	Map<Integer, List<String>> doi2items = (Map<Integer, List<String>>)request.getAttribute("doi2items");
	String prefixDOI = (String)request.getAttribute("prefixDOI");
%>
<dspace:layout locbar="link" navbar="admin" style="submission" titlekey="jsp.dspace-admin.doi.queued">
<c:set var="messageproposeddoi"><fmt:message key="jsp.dspace-admin.doi.table.header.proposeddoi"/></c:set>
<c:set var="messagenote"><fmt:message key="jsp.dspace-admin.doi.table.header.note"/></c:set>
<script type="text/javascript">
<!--
var j = jQuery.noConflict();
//-->
</script>
<div class="row">
    <h1><fmt:message key="jsp.dspace-admin.doi.queued"/></h1>   
</div>
<p><fmt:message key="jsp.dspace-admin.doi.general-description" /></p>

<% if(results!=null && results.length>0) {%>
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
</form>
		
<form method="post" action="<%= request.getContextPath() %>/dspace-admin/doiqueued" name="pendingsform">
	<dspace:itemlist items="<%= results %>" itemStart="1" inputName="pendingdoi"
		sortOption="<%= so %>" order="<%= order.toUpperCase() %>" />
	
	<div id="buttons" style="display: none;">
	<div style="display: none;">       
		<input id="submitrequestdeletereal" type="submit" name="submit" value="<%=DoiQueuedServlet.REQUEST_DELETE%>" />
		<input id="submitrequestdeleteandnodoireal" type="submit" name="submit" value="<%=DoiQueuedServlet.REQUEST_DELETE_AND_NODOI%>" />
		<input id="submitnewdoireal" type="submit" name="submit" value="<%=DoiQueuedServlet.REQUEST_NEWDOI%>" />
		<input id="submitcurrentdoireal" type="submit" name="submit" value="<%=DoiQueuedServlet.REQUEST_CURRENTDOI%>" />		
	</div>
	
	<input id="submitrequestdelete" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.delete"/>" />
	<input id="submitrequestdeleteandnodoi" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.deleteandnodoi"/>" />
	<input id="submitnewdoi" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.newdoi"/>" />
	<input id="submitcurrentdoi" type="button" class="submitbutton" value="<fmt:message key="jsp.search.doi.form.button.currentdoi"/>" />
	</div>
</form>
		
<script type="text/javascript">

j(document).ready(function() {
	var ctrl = j('<th/>').attr({id:'t5'}).html('${messageproposeddoi}');
	var ctrl2 = j('<th/>').attr({id:'t6'}).html('${messagenote}');
				   
	var table = j("html body table tbody tr td.text table.miscTable tbody tr:first").append(ctrl);
	var table2 = j("html body table tbody tr td.text table.miscTable tbody tr:first").append(ctrl2);

				
});


var maps = new Object();
var maps2 = new Object();
var maps3 = new Object();

<% 

for(Integer ss : doi2items.keySet()) {		
%>
	maps[<%= ss%>] = <%= "'"+doi2items.get(ss).get(0)+"';"%>
	maps2[<%= ss%>] = <%= "'"+StringEscapeUtils.escapeJavaScript(doi2items.get(ss).get(1))+"';"%>
	maps3[<%= ss%>] = <%= "'"+StringEscapeUtils.escapeJavaScript(doi2items.get(ss).get(2))+"';"%>
<%		
}	
%>


j('input[name="pendingdoi"]').each(function(index) {
	
	var ctrl0 = j('<td/>').attr({id:'tddoi_'+j(this).val()});		
	var ctrl4 = j('<span/>').attr({id:'spandoi_'+j(this).val()}).addClass('spandoi').html(maps[j(this).val()]);
		
	var ctrl1 = j('<input/>').attr({ type: 'button', id:"btndoi_"+j(this).val(), value:""}).addClass("btndoi");	
	var ctrl2 = j('<div/>').attr({id:"divdoi_btndoi_"+j(this).val()}).addClass("divdoi");
	var ctrl5 = j('<span/>').attr({id:'spanprefixdoi_'+j(this).val()}).addClass('spanprefixdoi').html('<%= prefixDOI %>');
	var ctrl3 = j('<input/>').attr({ type: 'text', id:'custombuilddoiid_'+j(this).val(), name:'custombuilddoi_'+j(this).val(),value:""}).addClass("doitext");
	
	j(this).parent().parent().append(ctrl0);		
	j("#tddoi_"+j(this).val()).append(ctrl4);
	j("#tddoi_"+j(this).val()).append(ctrl1);
	j("#tddoi_"+j(this).val()).append(ctrl2);

	j("#divdoi_btndoi_"+j(this).val()).append(ctrl5);
	j("#divdoi_btndoi_"+j(this).val()).append(ctrl3);

	
});

j('input[name="pendingdoi"]').each(function(index) {
	
	var ctrl0 = j('<td/>').attr({id:'tdnote_'+j(this).val()});		
	var ctrl4 = j('<span/>').attr({id:'spannote_'+j(this).val()}).addClass('spannote').html(maps3[j(this).val()] +"- "+ maps2[j(this).val()]);
		
	
	
	j(this).parent().parent().append(ctrl0);		
	j("#tdnote_"+j(this).val()).append(ctrl4);
	
});

foundedWarnOrFailure = false;
j('input[type="checkbox"]').each(function(index) {
	if(index>0) {
	if(maps3[j(this).val()]=="Success"){
		j(this).hide();
		j('#btndoi_'+j(this).val()).hide();	
	}	
	else {
		foundedWarnOrFailure = true;		
	}	
	}
	else{
		j(this).hide();
	}
});

if(foundedWarnOrFailure) {	
	
	j('#buttons').show();
	
}


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

j('.btndoi').click(function() {
	j('#divdoi_'+ this.id).toggle();
	var id = this.id;
	var index = id.indexOf("_");
	var suffixID = id.substr(index+1);
	if(j('#custombuilddoiid_'+ suffixID).val().length == 0) {
		j('#spandoi_'+ suffixID).removeClass("strike");	
	}
});

j('.doitext').change(function() {
	var id = this.name;
	var index = id.indexOf("_");
	var suffixID = id.substr(index+1);		
	if(j('#custombuilddoiid_'+ suffixID).val().length == 0) {
		j('#spandoi_'+ suffixID).removeClass("strike");	
	}
	else { 
		j('#spandoi_'+ suffixID).addClass("strike");
	}
});

</script>	
<% } else { %>	
	<fmt:message key="jsp.search.doi.form.button.nopending.found"/>
<% } %>
<ul>
	<li>
		<a href="<%=request.getContextPath() %>/dspace-admin/doi"><fmt:message key="jsp.layout.hku.tool.link.doi.home"/></a>
	</li>
	<li>
		<a href="<%= request.getContextPath() %>/dspace-admin/doipendings"><fmt:message key="jsp.layout.hku.tool.link.doi.pendings"/></a>
	</li>
</ul> 

</dspace:layout>