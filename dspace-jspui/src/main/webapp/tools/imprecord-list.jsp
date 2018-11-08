<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page language="java" pageEncoding="UTF-8" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.net.URLEncoder" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<%@ page import="org.apache.commons.lang3.StringUtils"%>
<%@ page import="org.dspace.app.cris.batch.bte.ImpRecordItem"%>
<%@ page import="org.dspace.app.cris.batch.bte.ImpRecordMetadata"%>
<%@ page import="org.dspace.core.ConfigurationManager"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%
	List<ImpRecordItem> results = (List<ImpRecordItem>)request.getAttribute("results");
	String crisID = (String)request.getAttribute("crisid");
	String sourceRef = (String)request.getAttribute("sourceref");
	
	String tableConfiguration = ConfigurationManager.getProperty("cris", "webui.recordlist.columns");
	String[] columns = tableConfiguration.split(",");
%>

<dspace:layout titlekey="jsp.dspace.imprecord-list.title">
<style>
.list-group-item {
	border: none;
}

.dt-buttons {
	float: right !important;
}
</style>
<h3><fmt:message key="jsp.dspace.imprecord-list.title.info-heading" /></h3>

<fmt:message key="jsp.dspace.imprecord-list.title.info" />

<form method="post">

<table id="mytable" class="table table-bordered table-condensed display">
<thead>
	<tr>
		<th>&nbsp;</th>
		<% for (String column : columns) { %>	
			<c:set var="messagekey">itemlist.<%= column %></c:set>
			<th><fmt:message key="${messagekey}"/></th>
		<% } %>
	</tr>
</thead>
<tbody>
<%
    for (ImpRecordItem result : results)
        {
%>        
		<tr>
			<td id="checkbox_<%= result.getSourceId() %>">
				<div class="data" data-id="<%= result.getSourceId() %>">&nbsp;</div>
				<input type="hidden" name="identifier_<%= result.getSourceId() %>" value="<%= result.getSourceId() + "_" + result.getSourceRef() %>"/>
			</td>
       	 	<% for (String column : columns) { %> 	
            <% 
            	Set<ImpRecordMetadata> valueSet = result.getMetadata().get(column);
            %>
            	<td>
            <%  
            	if(valueSet!=null) {
            	    int idxSep = 0;
            	for(ImpRecordMetadata valueMetadata : valueSet) { %>
            	<%= valueMetadata.getValue() %><% if(idxSep < valueSet.size()-1) { %><fmt:message key="jsp.dspace.imprecord-list.value.separator" /><% } %>            	            		
			<%  idxSep++;} } %>
				</td> 			
        	<% } %>
		</tr>        	
<%
    	}
%>  		
</tbody>
<tfoot>
</tfoot>
</table>
	<div class="row col-md-12 pull-right">
		<input type="hidden" name="sourceRef" value="<%= sourceRef %>" />
		<input class="btn btn-primary pull-right col-md-3" type="submit" name="submit_import" value="<fmt:message key="jsp.tools.general.import"/>" />
		<input class="btn btn-warning pull-right col-md-3" type="submit" name="submit_discard" value="<fmt:message key="jsp.tools.general.discard"/>" />
		<input class="btn btn-default pull-right col-md-3" type="submit" name="submit_cancel" value="<fmt:message key="jsp.tools.general.cancel"/>" />
	</div>	
<script type="text/javascript">
j(document).ready(function() {
	
    var table = j('#mytable').DataTable( {
		dom: 'Blfrtip',
        buttons: [
            'selectAll',
            'selectNone'
        ],
        language: {
            buttons: {
                selectAll: "Select all items",
                selectNone: "Select none"
            }
        },
        columnDefs: [ 
        {
            orderable: false,
            className: 'select-checkbox',
            targets:   0
        }
        ],
        select: {
            style:    'os',
            selector: 'td:first-child'
        },
        order: [[ 1, 'asc' ]]
    } );
    
    j(".hidden-select-checkbox").removeClass();
    
	j( "form input[type=submit]" ).click(function( event ) {
		var buttonPressed = this.getAttribute("name");
		if((buttonPressed=='submit_import' || buttonPressed=='submit_discard')) {
			var successExit = false;			
			table.rows('.selected').data().each( function ( i ) {
					successExit = true;
					var htmlid = j(i[0]).attr("data-id");
					j('<input>').attr({
					    type: 'hidden',
					    id: 'selectedId' + htmlid,
					    name: 'selectedId',
					    value: htmlid
					}).appendTo('form');
					return false;
            } );
			if(!successExit) {
				return false;
			}
		}

	});
});
</script>
</form>
</dspace:layout>
