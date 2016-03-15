<div class="modal fade" id="stats-date-change-dialog" tabindex="-1" role="dialog" aria-labelledby="StatsDataChange">
  <div class="modal-dialog" role="document">
    <form class="modal-content" id="formChangeRange">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
        <h4 class="modal-title" id="StatsDataChange"><fmt:message key="view.statistics.range.change.title" /></h4>
      </div>
      <div class="modal-body">
			<fmt:message key="view.statistics.range.from" /> <input class="form-control" type="text" id="stats_from_date" name="stats_from_date" value="${data.stats_from_date}" /> <br/>
			<fmt:message key="view.statistics.range.to" /> <input class="form-control" type="text" id="stats_to_date" name="stats_to_date" value="${data.stats_to_date}" /> <br/>
			<input type="hidden" name="type" value="${type}"/>
			<input type="hidden" name="handle" value="${data.object.handle}" />
      </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal"><fmt:message key="view.statistics.range.change.close" /></button>
        <input type="submit" class="btn btn-primary" id="changeRange" value="<fmt:message key="view.statistics.range.change.submit"/>"/>
      </div>
    </form>
  </div>
</div>

<script type="text/javascript">
<!--
var j = jQuery;
j(document).ready(function() {
	j("#stats_from_date").datepicker({
		dateFormat: "yy-mm-dd",
		defaultDate: "-6m",
		changeMonth: true,
		changeYear: true,
		showOtherMonths: true,
	    selectOtherMonths: true,
		onClose: function( selectedDate ) {
	        j( "#stats_to_date" ).datepicker( "option", "minDate", selectedDate );
	      }
	});

	j("#stats_to_date").datepicker({
		dateFormat: "yy-mm-dd",
		changeMonth: true,
		changeYear: true,
		showOtherMonths: true,
	    selectOtherMonths: true,
		onClose: function( selectedDate ) {
	        j( "#stats_from_date" ).datepicker( "option", "maxDate", selectedDate );
	      }
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
-->
</script>