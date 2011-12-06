jQuery(document).ready(function(){
    initdatasetsubmissionfile();
    initjQueryTooltips();
    initCiteMe();
    initFirstSubmissionForm();
});


function initdatasetsubmissionfile() {
    //For each file I find make sure that the form gets auto submitted
    jQuery('input#aspect_submission_StepTransformer_field_dataset-file').bind('click', function(){
        jQuery("input[type=radio][name='datafile_type'][value='file']").click();
        enableEmbargo();
    });

    jQuery('#aspect_submission_StepTransformer_div_submit-describe-dataset').find(":input[type=file]").bind('change', function(){
        if(this.id == 'aspect_submission_StepTransformer_field_dataset-file'){
            //Make sure the title gets set with the filename
            var fileName = jQuery(this).val().substr(0, jQuery(this).val().lastIndexOf('.'));
            fileName = fileName.substr(fileName.lastIndexOf('\\') + 1, fileName.length);


            var title_t = jQuery('input#aspect_submission_StepTransformer_field_dc_title').val();
	        if(title_t==null || title_t=='')
		        jQuery('input#aspect_submission_StepTransformer_field_dc_title').val(fileName);
        }
        //Now find our form
        var form = jQuery(this).closest("form");
        //Now that we have our, indicate that I want it processed BUT NOT TO CONTINUE, just upload
        //We do this by adding a param to the form action
        form.attr('action', form.attr('action') + '?processonly=true');
        //Now we submit our form
        form.submit();
    });

    jQuery("input[type='radio'][name='datafile_type']").change(function(){
        //If an external reference is selected we need to disable our embargo
        if(jQuery(this).val() == 'identifier'){
            disableEmbargo();
        }else{
            enableEmbargo();
        }

    });

    //Should we have a data file with an external repo then disable the embargo
    if(jQuery("input[name='disabled-embargo']").val()){
        disableEmbargo();
    }
    jQuery('select#aspect_submission_StepTransformer_field_datafile_repo').bind('change', function(){
        if(jQuery(this).val() == 'other'){
            jQuery("input[name='other_repo_name']").show();
        }else{
            jQuery("input[name='other_repo_name']").hide();
        }
    });
    

    //For our identifier a hint needs to be created
    var dataFileIdenTxt = jQuery('input#aspect_submission_StepTransformer_field_datafile_identifier');
    dataFileIdenTxt.inputHints();
    var repoNameTxt = jQuery('input#aspect_submission_StepTransformer_field_other_repo_name');
    repoNameTxt.inputHints();
    dataFileIdenTxt.blur(function(){
        if(jQuery(this).attr('title') != jQuery(this).val())
            jQuery('input#aspect_submission_StepTransformer_field_dc_title').val(jQuery(this).val());
    });

    jQuery('form#aspect_submission_StepTransformer_div_submit-describe-dataset').submit(function() {
        var dataFileIdenTxt = jQuery('input#aspect_submission_StepTransformer_field_datafile_identifier');
        if(dataFileIdenTxt.val() == dataFileIdenTxt.attr('title'))
            dataFileIdenTxt.val('');

        var repoNameTxt = jQuery('input#aspect_submission_StepTransformer_field_other_repo_name');
        if(repoNameTxt.val() == repoNameTxt.attr('title'))
            repoNameTxt.val('');


        jQuery('select#aspect_submission_StepTransformer_field_dc_type_embargo').removeAttr('disabled');

        return true;
    });

}

function disableEmbargo(){
    var embargoSelect = jQuery('select#aspect_submission_StepTransformer_field_dc_type_embargo');
    embargoSelect.find('option:selected').removeAttr('selected');
    //Select the publish immediately option
    embargoSelect.find("option[value='none']").attr('selected', 'selected');
    embargoSelect.attr('disabled', 'disabled');
}

function enableEmbargo(){
    var embargoSelect = jQuery('select#aspect_submission_StepTransformer_field_dc_type_embargo');
    embargoSelect.removeAttr('disabled');
}

function initjQueryTooltips(){

    jQuery('fieldset#aspect_submission_StepTransformer_list_submit-select-publication *').tooltip();
    jQuery('fieldset#aspect_submission_StepTransformer_list_submit-describe-dataset *').tooltip();
}

function initCiteMe() {
    jQuery('#citemediv').hide();
    jQuery('#sharemediv').hide();
    
    jQuery('#cite').click(function(){
    	jQuery('#citemediv').toggle();
    	return false;
    });
    
    jQuery('#share').click(function(){
    	jQuery('#sharemediv').toggle();
    	return false;
    });
}


jQuery.fn.inputHints = function() {
   // hides the input display text stored in the title on focus
   // and sets it on blur if the user hasn't changed it.

   // show the display text
   this.each(function(i) {
       jQuery(this).val(jQuery(this).attr('title'));
       jQuery(this).addClass('inputhint');
   });

   // hook up the blur & focus
   this.focus(function() {
       if (jQuery(this).val() == jQuery(this).attr('title')){
           jQuery(this).val('');
           jQuery(this).removeClass('inputhint');
           //Select the correct radio button
           jQuery("input[type=radio][name='datafile_type'][value='identifier']").click();
           if(this.name == 'datafile_identifier')
                disableEmbargo();
       }
   }).blur(function() {
       if (jQuery(this).val() == ''){
           jQuery(this).val(jQuery(this).attr('title'));
           jQuery(this).addClass('inputhint');
       }
   });
};


function initFirstSubmissionForm() {

    // if I am in the first page
    if(jQuery("#aspect_submission_StepTransformer_div_submit-select-publication").length > 0){

        // Status (onLoad of the page): status_accepted
        if (jQuery("'#xmlui_submit_publication_article_status_accepted':checked").val()) {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#journalID").show();
            jQuery("#manu").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            enableNextButton();
        }

        // Status (onLoad of the page): status_published
        else if (jQuery("'#xmlui_submit_publication_article_status_published':checked").val()) {
            jQuery("#aspect_submission_StepTransformer_list_doi").show();
            jQuery("#journalID").hide();
            jQuery("#manu").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#manu").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            enableNextButton();

        }

        // Status (onLoad of the page): status_in_review
        else if (jQuery("'#xmlui_submit_publication_article_status_in_review':checked").val()) {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#journalID").show();
            jQuery("#manu").show();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            enableNextButton();
        }

        // Status (onLoad of the page): status_not_yet_submitted
        else if (jQuery("'#xmlui_submit_publication_article_status_not_yet_submitted':checked").val()) {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#journalID").hide();
            jQuery("#journalIDStatusNotYetSubmitted").show();
            jQuery("#manu").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            enableNextButton();
        }

        else{
           jQuery("#aspect_submission_StepTransformer_list_doi").hide();
           jQuery("#journalID").hide();
           jQuery("#manu").hide();
           jQuery("#journalIDStatusNotYetSubmitted").hide();
           jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
           jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
           enableNextButton();
        }

        // Click: status_published
        jQuery('#xmlui_submit_publication_article_status_published').click(function () {
            jQuery("#aspect_submission_StepTransformer_list_doi").show();
            jQuery("#journalID").hide();
            jQuery("#manu").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            enableNextButton();
        });

        // Click: status_in_review
        jQuery('#xmlui_submit_publication_article_status_in_review').click(function () {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#journalID").show();
            jQuery("#manu").show();
            jQuery("#aspect_submission_StepTransformer_item_new-manu-comp-acc").hide();
            jQuery("#aspect_submission_StepTransformer_item_new-manu-acc").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            enableNextButton();
        });

        // Click: status_accepted
        jQuery('#xmlui_submit_publication_article_status_accepted').click(function () {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#journalID").show();
            jQuery("#manu").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            enableNextButton();
        });

        // Click: status_not_yet_published
        jQuery('#xmlui_submit_publication_article_status_not_yet_submitted').click(function () {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#journalID").hide();
            jQuery("#manu").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#journalIDStatusNotYetSubmitted").show();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            enableNextButton();
        });

        //change: journalID
        jQuery("#journalID").change(function() {
            if (jQuery("'#xmlui_submit_publication_article_status_accepted':checked").val()){
                jQuery('#aspect_submission_StepTransformer_div_submit-select-publication').submit();
            }
        });


        jQuery('input[name|="license_accept"]').change(function() {
            enableNextButton();
        });

        jQuery('input[name|="unknown_doi"]').change(function() {
            enableNextButton();
        });

        jQuery("#aspect_submission_StepTransformer_field_manu-number-status-accepted").blur(function() {
            enableNextButton();
        });

        jQuery("#aspect_submission_StepTransformer_field_manu").blur(function() {
            enableNextButton();
        });

        jQuery("#aspect_submission_StepTransformer_field_article_doi").blur(function() {
            enableNextButton();
        });

        jQuery('input[name|="manu_acc"]').change(function() {
            enableNextButton();
        });
    }
}

function enableNextButton() {
    
    // note: JOURNAL_ID is always present because the default value!!
    
    // license must be cheched
    if (jQuery('input[name|="license_accept"]:checked').val()) {
	//alert("license_accept: checked");
	
	// Status: status_accepted ==> must have JOURNAL_ID && (MANUSCRIPT_NUMBER || CB_This manuscript has been accepted)
	if (jQuery("'#xmlui_submit_publication_article_status_accepted':checked").val()) {
	    
	    //alert("accepted: checked");	    
	    //alert("manu-number ? empty: " + (jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").is(":empty")));
	        
	    var val = jQuery("#aspect_submission_StepTransformer_field_manu-number-status-accepted").val();
	    if(val!=null && val != '' || jQuery('input[name|="manu_acc"]:checked').val() ){
		jQuery("#aspect_submission_StepTransformer_field_submit_next").removeAttr("disabled");
		return;
	    }	    
	}
	
	// Status: status_published ==> must have DOI || CB_unknown_doi
	else if (jQuery("'#xmlui_submit_publication_article_status_published':checked").val()) {
	    
	    //alert("published: checked");	    
	    //alert("article_doi:" + ( jQuery("#aspect_submission_StepTransformer_field_article_doi").val()) );	    
	    
	    var val = jQuery("#aspect_submission_StepTransformer_field_article_doi").val();
	    if(val!="" || jQuery('input[name|="unknown_doi"]:checked').val()){
		
		//alert("article_doi || unknown_doi ok: enable next!");
		
		jQuery("#aspect_submission_StepTransformer_field_submit_next").removeAttr("disabled");
		return;
	    }	    
	}
	
	// Status: status_in_review ==> must have JOURNAL_ID && MANUSCRIPT_NUMBER
	else if (jQuery("'#xmlui_submit_publication_article_status_in_review':checked").val()) {
	    //var val = jQuery("#aspect_submission_StepTransformer_field_manu").val();
	    //if(val!=""){
	        jQuery("#aspect_submission_StepTransformer_field_submit_next").removeAttr("disabled");
		return;
	    //}
	}
	
	// Status status_not_yet_submitted ==> must have JOURNAL_ID
	else if (jQuery("'#xmlui_submit_publication_article_status_not_yet_submitted':checked").val()) {
	     jQuery("#aspect_submission_StepTransformer_field_submit_next").removeAttr("disabled");
	     return;
	}
	
    }    
    jQuery("#aspect_submission_StepTransformer_field_submit_next").attr("disabled", "disabled");    
}

