jQuery(document).ready(function() {
      //amend DOI text field in single item submission workflow if needed
      jQuery("input[name='submit_dc_identifier_add'][value='Add']").click( function() {
          //alert( jQuery( "#aspect_submission_StepTransformer_field_dc_identifier_qualifier" ).val() );
          //alert( jQuery( "#aspect_submission_StepTransformer_field_dc_identifier_value" ).val() );        
         var value = jQuery( "#aspect_submission_StepTransformer_field_dc_identifier_value" ).val().toLowerCase();
         var qualifier = jQuery( "#aspect_submission_StepTransformer_field_dc_identifier_qualifier" ).val().toLowerCase();
         var prefix = qualifier + ":";
          if( value.search(qualifier) >= 0 ) {
            //alert("MATCH " + prefix);
            jQuery( "#aspect_submission_StepTransformer_field_dc_identifier_value" ).val(
                  value.replace(prefix, '')
            );
          }
      });
})