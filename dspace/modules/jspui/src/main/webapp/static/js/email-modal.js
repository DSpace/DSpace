
jQuery.noConflict();

/**
 * Handle e-mail dialog form
 */
jQuery(document).ready(function(){
	
	/** User requests sharing **/
	jQuery("#sendEmail").click(function(){
		jQuery("#actionResult").hide();
		jQuery("#submitSend").removeAttr("disabled");
		jQuery("#emailDialog").dialog("open");
		
	});

	jQuery("#closeModal").click(function(){
		
		jQuery("#sendEmailForm")[0].reset();
		jQuery("#emailDialog").dialog("close");
		
	});
	
	/** User requests email sending **/
	jQuery("#submitSend").click(function(){
		jQuery(this).attr("disabled", "disabled");
		jQuery("#urlToShare").val(window.location.href);
		
		jQuery.ajax({
			type: "POST",
			url: "search/shareviaemail",
			data: jQuery("#sendEmailForm").serialize(),
			success: function(data) {
				jQuery("#actionResult").show();
				jQuery("#actionResult").html(data);
				jQuery("#sendEmailForm")[0].reset();
				jQuery("#submitSend").removeAttr("disabled");
			}
		});
		
	});
	
	jQuery("#emailDialog").dialog({modal: false, autoOpen: false, width: 600, closeOnEscape: true, draggable: false, resizable: false});
	
});