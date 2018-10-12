<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>

<%@ page contentType="text/html;charset=UTF-8" %>


<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="org.dspace.app.webui.components.StatisticsBean"  %>

<dspace:layout titlekey="jsp.workflowStatistics.title" navbar="admin">
<h1><fmt:message key="jsp.workflowStatistics.title"/></h1>
<div>
        <span class="label label-info">from:</span>&nbsp; ${fn:escapeXML(stats_from_date)} &nbsp;&nbsp;&nbsp; <span class="label label-info">to:</span> &nbsp; ${fn:escapeXML(stats_to_date)}
        &nbsp;&nbsp;<span class="label label-info">collection:</span>&nbsp;${stats_collection}
        &nbsp;&nbsp;&nbsp;<span class="label label-info">max:</span>&nbsp;${viewFilter == '-1'? 'all' : viewFilter}
        &nbsp;&nbsp;<a class="btn btn-default" data-toggle="modal" data-target="#stats-date-change-dialog"><fmt:message key="view.statistics.change-range" /></a>
        </div>
<table class="table table-striped">
<tr>
<th>&nbsp;</th>
<c:forEach items="${workflowStats.matrix}" var="row" varStatus="counter">
	<th>
		<fmt:message key="jsp.workflowstatistics.general.${workflowStats.colLabels[counter.index]}"/>
	</th>
</c:forEach>
</tr>
<c:forEach items="${workflowStats.matrix}" var="row" varStatus="counter">
<tr>
<td>
	<c:out value="${workflowStats.rowLabels[counter.index]}"/>
</td>
<c:forEach items="${row}" var="cell" varStatus="rowcounter">
<td>
<c:out value="${cell}"/>
</td>
</c:forEach>
</tr>
</c:forEach>
</table> 



<h3><fmt:message key="jsp.workflowStatistics.owner"/></h3>       
<table class="table table-striped">
<tr>
<th>&nbsp;</th>
<c:forEach items="${ownerStats.matrix}" var="row" varStatus="counter">
	<th>
			<fmt:message key="jsp.workflowstatistics.owner.${workflowStats.colLabels[counter.index]}"/>
	</th>
</c:forEach>
</tr>
<c:forEach items="${ownerStats.matrix}" var="row" varStatus="counter">
<tr>
<td>
	<c:out value="${ownerStats.rowLabels[counter.index]}"/>
</td>
<c:forEach items="${row}" var="cell" varStatus="rowcounter">
<td>
<c:out value="${cell}"/>
</td>
</c:forEach>
</tr>
</c:forEach>
</table>
<h3><fmt:message key="jsp.workflowStatistics.current"/></h3>
<table class="table table-striped">
<th><fmt:message key="jsp.workflowStatistics.current.step"/></th>
<th><fmt:message key="jsp.workflowStatistics.current.count"/></th>
<c:forEach items="${step2count}" var="entry">
	<tr>
		<td>${entry.key}</td>
		<td>${entry.value}</td>
	</tr>
</c:forEach>	
</table>
<div class="modal fade" id="stats-date-change-dialog" tabindex="-1" role="dialog" aria-labelledby="StatsDataChange">
  <div class="modal-dialog" role="document">
    <form class="modal-content" id="formChangeRange" action="workflowstats">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
        <h4 class="modal-title" id="StatsDataChange">Change the range of the report</h4>
      </div>
      <div class="modal-body">
        From: <input class="form-control" type="text" id="stats_from_date" name="stats_from_date"/> <br/>
        To: <input class="form-control" type="text" id="stats_to_date" name="stats_to_date"/> <br/>
		<div class="form-group">
		<fmt:message key="jsp.statistics.viewFilter" />
		<select name="viewFilter" class="form-control">
			<option value="10" ${viewFilter == '10' ? 'selected' :''}>10</option>
			<option value="25" ${viewFilter == '25' ? 'selected' :''}>25</option>
			<option value="50" ${viewFilter == '50' ? 'selected' :''}>50</option>
			<option value="100" ${viewFilter == '100' ? 'selected' :''}>100</option>
			<option value="-1" ${viewFilter == '-1' ? 'selected' :''}><fmt:message key="jsp.statistics.viewFilter.viewAll"/></option>
		</select>
		</div>
		<dspace:selectcollection klass="form-control" name="stats_collection"  id="stats_collection" />
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
        <input type="submit" class="btn btn-primary" id="changeRange"/>
      </div>
    </form>
  </div>
</div>

<script type="text/javascript">
<!--
var j = jQuery;
j(document).ready(function() {
        j("#stats_from_date").datepicker({
                dateFormat: "yy-mm-dd"
        });

        j("#stats_to_date").datepicker({
                dateFormat: "yy-mm-dd"
        });

        j("#formChangeRange").submit(function(){
              /* var sdate= j("#stats_from_date").val();
                var edate= j("#stats_to_date").val();
                
                if(sdate.length ==0){
                        sdate.val("*");
                }
                if(edate.length ==0){
                        edate.val("*");
                }*/
                var colid= j("#stats_collection").val();
                if(colid==-1){
                	 j("#stats_collection").val("*");
                }
        });
});
-->
</script>

</dspace:layout>