/* JS behaviors for all Dryad pages */
jQuery(document).ready(function() {
    var showButton = jQuery('input[name=show_button]');

    if(showButton!='undefined'&&showButton!=null&&showButton.length>0)
    {
        jQuery(parent.top.document.getElementById('aspect_submission_workflow_WorkflowTransformer_field_skip_payment')).show();
        jQuery(parent.top.document.getElementById('aspect_submission_workflow_WorkflowTransformer_field_skip_payment')).val(showButton.val());
        jQuery(parent.top.document.getElementById('aspect_submission_submit_CheckoutStep_field_skip_payment')).show();
        jQuery(parent.top.document.getElementById('aspect_submission_submit_CheckoutStep_field_skip_payment')).val(showButton.val());
    }
});
