<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%--fmt:message key="view.${data.jspKey}.data.${statType}.${objectName}.total.description" /--%>
<c:set var="pieType" scope="page">allMonths</c:set>						
<%@include file="../modules/graph/monthsGraph.jsp"%>

<div class="table-responsive">
<table class="table table-bordered dataTable datatable-time">
<thead>
	<tr>
		<th><fmt:message key="view.data.year" /></th>		
		<th><fmt:message key="view.data.jan" /></th>
		<th><fmt:message key="view.data.feb" /></th>
		<th><fmt:message key="view.data.mar" /></th>
		<th><fmt:message key="view.data.apr" /></th>
		<th><fmt:message key="view.data.may" /></th>
		<th><fmt:message key="view.data.jun" /></th>
		<th><fmt:message key="view.data.jul" /></th>
		<th><fmt:message key="view.data.aug" /></th>
		<th><fmt:message key="view.data.sep" /></th>
		<th><fmt:message key="view.data.oct" /></th>
		<th><fmt:message key="view.data.nov" /></th>
		<th><fmt:message key="view.data.dec" /></th>
		<th><fmt:message key="view.data.total" /></th>		
	</tr>
</thead>
<tbody>	
	<c:forEach var="row" items="${researcher:getAllMonthsStats(data.resultBean.dataBeans[statType][objectName]['allMonths'].dataTable)}">
	<c:if test="${row.total > 0}">
	<tr class="evenRowOddCol">
		<td>${row.year}</td>
		<td>${row.jan}</td><td>${row.feb}</td>	
		<td>${row.mar}</td><td>${row.apr}</td><td>${row.may}</td><td>${row.jun}</td>
		<td>${row.jul}</td><td>${row.aug}</td><td>${row.sep}</td><td>${row.oct}</td>
		<td>${row.nov}</td><td>${row.dec}</td>
		<td>${row.total}</td>																
	</tr>
	</c:if>
	</c:forEach>
</tbody>	
<tfoot>
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
</div>
<div class="clearfix">&nbsp;</div>
<script type="text/javascript">
<!--
	
		var j = jQuery;
        j(document).ready(function() {
        	
        	j(".datatable-time").dataTable({
        		dom: "<'pull-right'B>rfrtip",
        		searching: false, 
        		info: false, 
        		paging: false,
        		ordering : true,
                   buttons: [
                             {extend: 'excelHtml5', text: '<i class="fa fa-file-excel-o"></i> Download Excel'}, 
                             {extend: 'csvHtml5', text: '<i class="fa fa-file-text-o"></i> Download CSV'}
                         ]
        	});

        });
-->
</script>

