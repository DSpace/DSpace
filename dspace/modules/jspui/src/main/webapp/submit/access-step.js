/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function() {
	
	jQuery.noConflict();
	jQuery.datepicker.setDefaults( jQuery.datepicker.regional[ "pt_BR" ] );
    jQuery(document).ready(function($) {
        $("input[name='open_access_radios']").change(function() {
            if ($("#embargo_until_date").attr("disabled") == undefined) {
                $("#embargo_until_date").attr("disabled", "disabled");
                $("#reason").attr("disabled", "disabled");
            } else {
                $("#embargo_until_date").removeAttr("disabled");
                $("#reason").removeAttr("disabled");
            }
        });
        
        $("#embargo_type").change(function() {
        	manageFields();
        });
        
        $("#embargo_until_date").datepicker({
        	dateFormat: "yy-mm-dd",
        	 changeMonth: true,
        	 changeYear: true
        });
        
        manageFields();
    });
    
})();

function manageFields()
{
	var RESTRICTED = 1;
	var EMBARGOED = 2;
	var FREE = 3;
	
	var embargoType = jQuery("#embargo_type").val();
	if(embargoType)
	{
		if(embargoType == RESTRICTED)
		{
			jQuery("#embargo_until_date").attr("disabled", "disabled");
			jQuery("#embargo_until_date").val("");
			
			jQuery("#reason").removeAttr("disabled");
		}
		else if(embargoType == EMBARGOED)
		{
			jQuery("#embargo_until_date").removeAttr("disabled");
			jQuery("#reason").removeAttr("disabled");
		}
		else if(embargoType == FREE)
		{
			jQuery("#embargo_until_date").attr("disabled", "disabled");
			jQuery("#embargo_until_date").val("");
			
			jQuery("#reason").attr("disabled", "disabled");
			jQuery("#reason").val("");
		}
	}
}



