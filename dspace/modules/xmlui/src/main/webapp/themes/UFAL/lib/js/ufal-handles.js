/*global jQuery */
/*jshint globalstrict: true*/
'use strict';
var ufal = ufal || {};

ufal.handles = {

    init : function () {
    	if($('[name="handle_id"]:checked').length == 0) {
    		$('[name="submit_edit"]').prop( "disabled", true );
    		$('[name="submit_delete"]').prop( "disabled", true );
    	}
    	$('[name="handle_id"]').one('change', function (e) {
    		$('[name="submit_edit"]').prop( "disabled", false );
        	$('[name="submit_delete"]').prop( "disabled", false );
    	});
    }

};

jQuery(document).ready(function () {
    ufal.handles.init();    
}); // ready
