jQuery.noConflict();

/**
 * Handle e-mail dialog form
 */
jQuery(document).ready(function(){
	
	/** User requests sharing **/
	jQuery("#tabContainer").tabs();
	
	jQuery("#submit_import").click(function(){
		jQuery(this).attr("disabled", "disabled");
		jQuery("#folder_form").submit();
	});
	
});