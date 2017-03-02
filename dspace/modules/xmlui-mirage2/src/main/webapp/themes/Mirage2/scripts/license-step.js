jQuery(document).ready(function(){
   // License step foo

   // Hide everything until the user makes a choice
   jQuery(document).concealLicenseText();
   jQuery("#aspect_submission_StepTransformer_list_submit-proxy-document").hide();
   jQuery("#aspect_submission_StepTransformer_div_submit-license-inner #aspect_submission_StepTransformer_list_submit-review").hide();


   jQuery("input[name='license-selector'][value='default']").click( function() {
       jQuery(document).concealLicenseText();
       jQuery("#aspect_submission_StepTransformer_div_submit-license-standard-text").show();
       jQuery("#aspect_submission_StepTransformer_div_submit-license-inner #aspect_submission_StepTransformer_list_submit-review").show();
   });


   jQuery("input[name='license-selector'][value='proxy']").click( function() {
       jQuery(document).concealLicenseText();
       jQuery("#aspect_submission_StepTransformer_div_submit-license-proxy-text").show();
       jQuery("#aspect_submission_StepTransformer_list_submit-proxy-document").show();
       jQuery("#aspect_submission_StepTransformer_div_submit-license-inner #aspect_submission_StepTransformer_list_submit-review").show();
   });

   jQuery("input[name='license-selector'][value='open']").click( function() {
       jQuery(document).concealLicenseText();
       jQuery("#aspect_submission_StepTransformer_div_submit-license-open-text").show();
       jQuery("#aspect_submission_StepTransformer_div_submit-license-inner #aspect_submission_StepTransformer_list_submit-review").show();
   });
      
});

jQuery.fn.extend({
   concealLicenseText: function() {
      jQuery("#aspect_submission_StepTransformer_list_submit-proxy-document").hide();
      jQuery("#aspect_submission_StepTransformer_div_submit-license-standard-text").hide();
      jQuery("#aspect_submission_StepTransformer_div_submit-license-proxy-text").hide();
      jQuery("#aspect_submission_StepTransformer_div_submit-license-open-text").hide();
   }
});