<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%--fmt:message key="view.${data.jspKey}.data.${statType}.${objectName}.total.description" /--%>
<c:set var="pieType" scope="page">allMonths</c:set>						
<%@include file="../modules/graph/monthsGraph.jsp"%>


<table class="table table-bordered dataTable datatable-time">
<thead>
	<tr>
		<th><!-- spacer cell --></th>		
		<th><fmt:message key="view.data.total" /></th>
		<th><fmt:message key="view.data.jul" /></th>
		<th><fmt:message key="view.data.aug" /></th>
		<th><fmt:message key="view.data.sep" /></th>
		<th><fmt:message key="view.data.oct" /></th>
		<th><fmt:message key="view.data.nov" /></th>
		<th><fmt:message key="view.data.dec" /></th>
		<th><fmt:message key="view.data.jan" /></th>
		<th><fmt:message key="view.data.feb" /></th>
		<th><fmt:message key="view.data.mar" /></th>
		<th><fmt:message key="view.data.apr" /></th>
		<th><fmt:message key="view.data.may" /></th>
		<th><fmt:message key="view.data.jun" /></th>
	</tr>
	<c:forEach var="row" items="${researcher:getAllMonthsStats(data.resultBean.dataBeans[statType][objectName]['allMonths'].dataTable)}">
	<c:if test="${row.total > 0}">
	<tr class="evenRowOddCol">
		<td>${row.year}</td><td>${row.total}</td>
		<td>${row.jul}</td><td>${row.aug}</td><td>${row.sep}</td><td>${row.oct}</td>
		<td>${row.nov}</td><td>${row.dec}</td>																
		<td>${row.jan}</td><td>${row.feb}</td>	
		<td>${row.mar}</td><td>${row.apr}</td><td>${row.may}</td><td>${row.jun}</td>	
	</tr>
	</c:if>
	</c:forEach>
	<tr>
		<th scope="row" colspan="13" class="text-right"><fmt:message key="view.${data.jspKey}.data.${statType}.${objectName}.total.total" /></th>						
		<th id="totalView">
			${data.resultBean.dataBeans[statType][objectName]['total'].dataTable[0][0]}
		</th>
	</tr>
	<!--tr>
		<th scope="row" colspan="13" align="right"><fmt:message key="view.${data.jspKey}.data.${statType}.${objectName}.total.lastyear" /></th>
		<td>
			${data.resultBean.dataBeans[statType][objectName]['lastYear'].dataTable[0][0]}
		</td>
	</tr-->	
</tfoot>		
</table>
<div class="clearfix">&nbsp;</div>
<script type="text/javascript">
<!--
	renderNA = function ( data, type, row ) {
 	   if(data =='0'){
      		return 'N/A';   
      	   }
      	    return data.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
      	   };
      	   
		var j = jQuery;
        j(document).ready(function() {
        	var num =  j("#totalView").text();
        	var numcomma = num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        	j("#totalView").text(numcomma);
        	
        	var num =  j("#totalItemView").text();
        	var numcomma = num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        	j("#totalItemView").text(numcomma);     	
        	
        	j(".datatable-time").dataTable({
        		dom: "<'pull-right'B>rfrtip",
        		searching: false, 
        		info: false, 
        		paging: false, 
        		"columnDefs": [
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 1 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 2 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 3 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 4 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 5 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 6 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 7 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 8 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 9 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 10 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 11 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 12 },
        	                   {  "render": function ( data, type, row ) { return renderNA( data, type, row ); },"orderable": false, "targets": 13 }
        	                   ],
                   buttons: [
                             {extend: 'excelHtml5', text: '<span class="glyphicon glyphicon-floppy-save"></span> Download Excel'}, 
                             {extend: 'csvHtml5', text: '<span class="glyphicon glyphicon-floppy-save"></span> Download CSV'}
                         ]
        	});
        	j(".datatable-mostviewed").dataTable({
        		dom: "<'pull-right'B>rfrtip",
        		searching: false, 
        		info: false, 
        		paging: false,
        		ordering: false,
                buttons: [
                      {extend: 'excelHtml5', text: '<span class="glyphicon glyphicon-floppy-save"></span> Download Excel'}, 
                      {extend: 'csvHtml5', text: '<span class="glyphicon glyphicon-floppy-save"></span> Download CSV'}
                ],
        		"columnDefs": [
        	                   {  "render": function ( data, type, row ) {
        	                	   if(data =='0'){
        	                		return 'N/A';   
        	                	   }
        	                	    return data.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
        	                	   },"orderable": false, "targets": 1 }
        						]
        	});
        	j(".statstabahref").click(function() {		
    			var d = j('div#' + j(this).attr('id').replace('ahref', 'content'));		
    			d.trigger('redraw');			
    		});
        	
        	
    		j("#statstabs").tabs({
    			"activate": function( event, ui ) {
    				j("li.ui-tabs-active").toggleClass("ui-tabs-active ui-state-active active");
    			},
    			"beforeActivate": function( event, ui ) {
       			 j("li.active").toggleClass("active");
    			},
    	   		"create": function( event, ui ) {
    	               j("div.ui-tabs").toggleClass("ui-tabs ui-widget ui-widget-content ui-corner-all tabbable");
    	               j("ul.ui-tabs-nav").toggleClass("ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all nav nav-tabs");
    	               j("li.ui-tabs-active").toggleClass("ui-state-default ui-corner-top ui-tabs-active ui-state-active active");
    	               j("li.ui-state-default").toggleClass("ui-state-default ui-corner-top");
    	               j("div.ui-tabs-panel").toggleClass("ui-tabs-panel ui-widget-content ui-corner-bottom tab-content with-padding");
    	        }
    		});
        });
-->
</script>
