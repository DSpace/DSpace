/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
$(function() {

    $("#aspect_submission_StepTransformer_field_embargo_until_date").datepicker({dateFormat: 'yy-mm-dd'});
    $("#aspect_submission_StepTransformer_field_embargo_until_date").datepicker({dateFormat: 'yy-mm-dd'});

    $( "#aspect_submission_StepTransformer_field_embargo_until_date").datepicker();

    $(function() {
        $( "#aspect_submission_StepTransformer_field_embargo_until_date").datepicker();
        initAccessSubmissionForm();
    });


    $('input[name|="open_access_radios"]').change(function(){
        // Visible
        if ($('input[name|="open_access_radios"]:checked').val() == '0'){
            disableFields();
        }
        // Embargoed
        else if ($('input[name|="open_access_radios"]:checked').val() == '1'){
            enableFields()
        }

    });


    function initAccessSubmissionForm() {
        if ($('input[name|="open_access_radios"]').length >0){
            if ($('input[name|="open_access_radios"]:checked').val() == '0'){
                disableFields();
            }
            // Embargoed
            else if ($('input[name|="open_access_radios"]:checked').val() == '1'){
                enableFields()
            }
        }
    }

    function enableFields() {
        $("#aspect_submission_StepTransformer_field_reason").removeAttr("disabled");
        $("#aspect_submission_StepTransformer_field_embargo_until_date").removeAttr("disabled");

    }

    function disableFields() {
        $("#aspect_submission_StepTransformer_field_reason").attr("disabled", "disabled");
        $("#aspect_submission_StepTransformer_field_embargo_until_date").attr("disabled", "disabled");
    }
});
