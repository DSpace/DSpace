<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<div class="modal fade" id="stats-date-change-dialog" tabindex="-1" role="dialog" aria-labelledby="StatsDataChange">
  <div class="modal-dialog" role="document">
    <form class="modal-content" id="formChangeRange">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
        <h4 class="modal-title" id="StatsDataChange"><fmt:message key="view.statistics.range.change.title" /></h4>
      </div>
      <div class="modal-body">
      <div class="row">
					<div class='col-md-6'>
						<div class="form-group">
								<fmt:message key="view.statistics.range.from" /> <input class="form-control" type="text" id="stats_from_date" name="stats_from_date" value="${fn:escapeXml(data.stats_from_date)}" /> 
						</div>
					</div>
					<div class='col-md-6'>
						<div class="form-group">
								<fmt:message key="view.statistics.range.to" /> <input class="form-control" type="text" id="stats_to_date" name="stats_to_date" value="${fn:escapeXml(data.stats_to_date)}" /> 
						</div>
					</div>
			<input type="hidden" name="type" value="${data.type}"/>
			<c:choose>
			<c:when test="${data.object.type < 9}">
				<input type="hidden" name="handle" value="${data.object.handle}" />
			</c:when>
			<c:otherwise>
				<input type="hidden" name="id" value="${data.object.handle}" />
			</c:otherwise>
			</c:choose>						
	  </div>
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="view.statistics.range.change.close" /></button>
        <input type="submit" class="btn btn-primary" id="changeRange" value="<fmt:message key="view.statistics.range.change.submit"/>"/>
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
