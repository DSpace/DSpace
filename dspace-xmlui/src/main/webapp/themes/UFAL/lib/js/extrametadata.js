/*global jQuery */
/*jshint globalstrict: true*/
'use strict';
var ufal = ufal || {};

// helper function
String.prototype.trim = function() {
    return this.replace(/^\s+|\s+$/g,"");
};

ufal.extrametadata = {

    improve_funding_type_ui: function () {
        //Make funding type prettier
        var fundingTypeID = "#aspect_submission_StepTransformer_field_metashare_ResourceInfo\\#ResourceCreationInfo\\#FundingInfo\\#ProjectInfo_fundingType";
        var fundingTypeObj = jQuery(fundingTypeID);
        var fundingTypeDiv = fundingTypeObj.parent();
        fundingTypeObj.hide();
        fundingTypeDiv.prepend("<select id=\"fake_fundingType\" class=\"ds-select-field submit-select\">" +
                                   "<option value=\"\">N/A</option>" +
                                   "<option value=\"euFunds\">EU</option>" +
                                   "<option value=\"ownFunds\">Own funds</option>" +
                                   "<option value=\"nationalFunds\">National</option>" +
                                   "<option value=\"other\">Other</option></select>");
        jQuery("#fake_fundingType").change(function () {
            fundingTypeObj.val(jQuery("#fake_fundingType").find(":selected").val());
        });
    },

    improve_licence_info_ui: function() {
        // autoselect
        var licenses = jQuery("[name='metashare_ResourceInfo\\#DistributionInfo\\#LicenseInfo_license_selected']");
        if (licenses.length === 1) {
            ufal.extrametadata.fillout_licence_info();
        }
    },
    
    improve_accordion_ui: function() {
    	$('.accordion-heading h3').prepend('<i class="fa fa-chevron-circle-right fw" style="margin-right: 1em;"></i>');	       	    
    },

    fillout_licence_info: function() {
        //We have a license that means "restrictions"
        var licenseText = jQuery("input + span.ds-interpreted-field").text();
        if (licenseText.match(/CC/)) {
            var parent = jQuery("#aspect_submission_StepTransformer_field_metashare_ResourceInfo\\#DistributionInfo\\#LicenseInfo_restrictionsOfUse");
            //CC licenses
            if (licenseText.match(/CC BY/)) {
                //attribution
                parent.find("option[value=attribution]").attr('selected','selected');
            }
            if (licenseText.match(/-NC/)) {
                //non comerc
                parent.find("option[value=academic-nonCommercialUse]").attr('selected','selected');
            }
            if (licenseText.match(/-SA/)) {
                //share alike
                parent.find("option[value=shareAlike]").attr('selected','selected');
            }
            if (licenseText.match(/-ND/)) {
                //no derivs
                parent.find("option[value=noDerivatives]").attr('selected','selected');
            }
        }
    },

    handle_repeatable: function() {

        function get_value_num( value ) {
            var arr = value.split('_');
            return arr[arr.length-1];
        }

        // get repeteable components
        jQuery('input[type="hidden"]').each(function (index) {
            var name = jQuery(this).attr('name');
            var start_id = 'start_repeatable_';

            var component = null;
            if ( name.indexOf(start_id) === 0 ) 
            {
                // rename it
                //
                jQuery(this).parent().parent().next().find("span.ds-interpreted-field").each( function() {
                    var comp_text = jQuery(this).text();
                    if ( comp_text.indexOf('#') === 0 ) {
                        comp_text = comp_text.replace( /#(\d+)-/g, "[Component $1]-" );
                        jQuery(this).text(comp_text);
                    }
                });

                component = name.substring(start_id.length);
                // we  have a repeteable component - then remove all
                // buttons for not REALLY repeatable components
                jQuery('input[type="submit"]').each(function(index) {
                    var name = jQuery(this).attr('name');
                    if (name.indexOf('submit_' + component + "_") === 0 &&
                        (name.match('_add$') || name.match('_delete$')))
                    {
                        jQuery(this).hide();
                    }
                });

                jQuery('input[type="checkbox"]').each(function(index) {
                    var name = jQuery(this).attr('name');
                    if (name.indexOf(component + "_") === 0 && name.match('_selected$')) {
                        // master click
                        jQuery(this).click( function () {
                            var clicked_value = get_value_num( jQuery(this).attr('value') );
                            var checked = jQuery(this).attr('checked');
                            jQuery('input[type="checkbox"]').each(function(index) {
                                var name = jQuery(this).attr( 'name' );
                                var value = get_value_num( jQuery(this).attr('value') );
                                if (name.indexOf( component + "_" ) === 0 && name.match('_selected$')) {
                                    if (value === clicked_value) {
                                        jQuery(this).attr('checked', checked);
                                    }
                                }
                            }); // for all checkboxes
                        }); // click on checkbox
                    }
                });
            }
        });
    },

    verify_repeatable_all_filled: function() {
        // add the modal dialog html before first add
        jQuery("[name^=submit_component_]").filter('[value*="Add"]').first().parent().parent().append(
            '<div class="modal hide fade" id="extrametadata-modal-notallfilled"><div class="modal-header alert alert-warning">' +
                '<h3>Warning</h3></div><div class="modal-body"><p>Not all fields of the component have been properly filled out. Fill them and try again.</p>' +
                '</div><div class="modal-footer"><a href="#" data-dismiss="modal" class="btn">Close</a></div></div>'
        );
        jQuery("#extrametadata-modal-notallfilled").modal( { 'show' : false } );

        // Naive component check (all filled) #703
        jQuery("[name^=submit_component_]").filter('[value*="Add"]').each(function () {
            jQuery(this).click(function (event) {
                var fields = jQuery(this).parents().filter(
                    "fieldset:first").find("input").filter("[type!=hidden]").filter("[type!=submit]");
                var missing = false;
                var length = fields.length;
                for (var i = 0; i < length; ++i) {
                    var field = jQuery(fields[i]);
                    var value = field.val();
                    if (!value || 0 === value.length) {
                        //One of them not filled
                        missing = true;
                        break;
                    }
                }
                // show modal dialog if one of the component fields have not been filled out
                if (missing) {
                    event.preventDefault();
                    jQuery("#extrametadata-modal-notallfilled").modal('show');
                }
            });
        });
    },

    // :)
    handle_collapses: function() {    	
        jQuery('.accordion-heading').each(function () {
        	jQuery("h3", this).css("cursor", "pointer");
            jQuery(this).attr('data-target', jQuery(this).next().attr('id'));
            jQuery(this).on('click', function () {
                jQuery(this).siblings(".accordion-body").collapse('toggle');                
                jQuery(this).find('h3 i').toggleClass('fa-chevron-circle-right').toggleClass('fa-chevron-circle-down');
            });
        });
    },
    
    handle_scrolling: function() {    	
		var name = jQuery('input[name="jump_to"]').val();		
		if(name != "") {   
			var target = jQuery('input[name="'+name+'"]').closest('li');
			var accordionHeading = target.closest('.accordion-body').prev();
			if( accordionHeading.length > 0) {
				accordionHeading.trigger('click');							
			}
			var topOffset= target.offset().top;
			jQuery('html, body').delay(100).animate({scrollTop: topOffset}, 200);
		}    	
    },
    
    handle_datepickers: function() {
    	if(jQuery('#aspect_submission_StepTransformer_field_local_embargo_termslift').length > 0) {    	
	    	jQuery('#aspect_submission_StepTransformer_field_local_embargo_termslift').datepicker({
	    		autoclose: true,
	    		format: 'yyyy-mm-dd',
	    		forceParse: false
	    	});
    	}
    }
    
};


jQuery(document).ready(function () {	

    // metadata fiddling
    ufal.extrametadata.improve_funding_type_ui();
    ufal.extrametadata.improve_licence_info_ui();

    // make repeatable components
    ufal.extrametadata.handle_repeatable();
    ufal.extrametadata.verify_repeatable_all_filled();
    
    // improve accordion ui
    ufal.extrametadata.improve_accordion_ui();

    // collapsibles
    ufal.extrametadata.handle_collapses();
    
    // date pickers
    ufal.extrametadata.handle_datepickers();
    
    // scroll to expected place after submitting if returning to the same page
    ufal.extrametadata.handle_scrolling();
    
    
});
