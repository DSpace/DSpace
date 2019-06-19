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

<dspace:layout titlekey="jsp.loginStatistics.title" navbar="admin">

<fmt:message key="jsp.statistics.range.maxresult.all" var="maxresultall"/>

<h1><fmt:message key="jsp.loginStatistics.title"/></h1>
<div>
		<span class="label label-info"><fmt:message key="view.statistics.range.from" /></span> &nbsp; 
			<c:if test="${empty stats_from_date}"><fmt:message key="view.statistics.range.no-start-date" /></c:if>
			${fn:escapeXml(stats_from_date)} &nbsp;&nbsp;&nbsp; 
		<span class="label label-info"><fmt:message key="view.statistics.range.to" /></span> &nbsp; 
			<c:if test="${empty stats_to_date}"><fmt:message key="view.statistics.range.no-end-date" /></c:if>
			${fn:escapeXml(stats_to_date)} &nbsp;&nbsp;&nbsp;
		<span class="label label-info"><fmt:message key="jsp.statistics.range.maxresult" /></span>&nbsp;${viewFilter == '-1'? maxresultall : (empty viewFilter ? '10' : viewFilter)} &nbsp;&nbsp;&nbsp;
		<a class="btn btn-default" data-toggle="modal" data-target="#stats-date-change-dialog"><fmt:message key="view.statistics.change-range" /></a>        
</div>
<table class="table table-striped">
<tr>
<th>&nbsp;</th>
<c:forEach items="${loginStats.matrix}" var="row" varStatus="counter">
	<th>
		<c:out value="${loginStats.colLabels[counter.index]}"/>
	</th>
</c:forEach>
</tr>
<c:forEach items="${loginStats.matrix}" var="row" varStatus="counter">
<tr>
<td>
	<c:out value="${loginStats.rowLabels[counter.index]}"/>
</td>
<c:forEach items="${row}" var="cell" varStatus="rowcounter">
<td>
<c:if test="${rowcounter.index == 0}">
	<c:out value="User"/>
</c:if>
<c:out value="${cell}"/>
</td>
</c:forEach>
</tr>
</c:forEach>
</table>
<div class="modal fade" id="stats-date-change-dialog" tabindex="-1" role="dialog" aria-labelledby="StatsDataChange">
  <div class="modal-dialog" role="document">
    <form class="modal-content" id="formChangeRange" action="loginstats">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
        <h4 class="modal-title" id="StatsDataChange"><fmt:message key="view.statistics.range.change.title" /></h4>
      </div>
      <div class="modal-body">
        <fmt:message key="view.statistics.range.from" /><input class="form-control" type="text" id="stats_from_date" name="stats_from_date" value="${fn:escapeXml(stats_from_date)}"/> <br/>
        <fmt:message key="view.statistics.range.to" /><input class="form-control" type="text" id="stats_to_date" name="stats_to_date" value="${fn:escapeXml(stats_to_date)}"/> <br/>
		<div class="form-group">
		<fmt:message key="jsp.statistics.range.maxresult" />
		<select name="viewFilter" class="form-control">
			<option value="10" ${viewFilter == '10' ? 'selected' :''}>10</option>
			<option value="25" ${viewFilter == '25' ? 'selected' :''}>25</option>
			<option value="50" ${viewFilter == '50' ? 'selected' :''}>50</option>
			<option value="100" ${viewFilter == '100' ? 'selected' :''}>100</option>
			<option value="-1" ${viewFilter == '-1' ? 'selected' :''}><fmt:message key="jsp.statistics.range.maxresult.all"/></option>
		</select>
		</div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="view.statistics.range.change.close" /></button>
        <input type="submit" class="btn btn-primary" id="changeRange" value="<fmt:message key="view.statistics.range.change.submit" />"/>
      </div>
    </form>
  </div>
</div>

<script type="text/javascript">
var j = jQuery;
j(document).ready(function() {    
        j('#stats_from_date').datetimepicker({
        	format: "YYYY-MM-DD"       	
        });
        j('#stats_to_date').datetimepicker({
        	format: "YYYY-MM-DD",
            useCurrent: false //Important! See issue #1075
        });
        j("#stats_from_date").on("dp.change", function (e) {
            j('#stats_to_date').data("DateTimePicker").minDate(e.date);
        });
        j("#stats_to_date").on("dp.change", function (e) {
            j('#stats_from_date').data("DateTimePicker").maxDate(e.date);
        });
        
    	j("#formChangeRange").submit(function(){
    		var sdate= j("#stats_from_date").val();
    		var edate= j("#stats_to_date").val();
    		if(sdate.length ==0){
    			sdate.val("*");
    		}
    		if(edate.length ==0){
    			edate.val("*");
    		}
    	});
});
</script>

</dspace:layout>