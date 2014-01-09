/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function() {
    jQuery.noConflict();
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
        $("#embargo_until_date").datepicker({
        	dateFormat: "yy-mm-dd"    	 
        });
    });
})();
