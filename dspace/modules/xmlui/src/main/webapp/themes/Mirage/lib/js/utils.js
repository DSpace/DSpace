jQuery(document).ready(function() {
    //initjQueryTooltips();
    initCiteMe();
    initFirstSubmissionForm();
});

function disableEmbargo() {
    var embargoSelect = jQuery('select#aspect_submission_StepTransformer_field_dc_type_embargo');
    embargoSelect.find('option:selected').removeAttr('selected');
    //Select the publish immediately option
    embargoSelect.find("option[value='none']").attr('selected', 'selected');
    embargoSelect.attr('disabled', 'disabled');
}

function enableEmbargo() {
    var embargoSelect = jQuery('select#aspect_submission_StepTransformer_field_dc_type_embargo');
    embargoSelect.removeAttr('disabled');
}

function initjQueryTooltips() {

    jQuery('fieldset#aspect_submission_StepTransformer_list_submit-select-publication *').tooltip();
    jQuery('fieldset#aspect_submission_StepTransformer_list_submit-describe-dataset *').tooltip();
}

function initCiteMe() {
    jQuery('#citemediv').hide();
    jQuery('#sharemediv').hide();

    jQuery('#cite').click(function() {
        jQuery('#citemediv').toggle();
        return false;
    });

    jQuery('#share').click(function() {
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
    this.focus(
        function() {
            if (jQuery(this).val() == jQuery(this).attr('title')) {
                jQuery(this).val('');
                jQuery(this).removeClass('inputhint');
                //Select the correct radio button
                jQuery("input[type=radio][name='datafile_type'][value='identifier']").click();
                if (this.name == 'datafile_identifier')
                    disableEmbargo();
            }
        }).blur(function() {
            if (jQuery(this).val() == '') {
                jQuery(this).val(jQuery(this).attr('title'));
                jQuery(this).addClass('inputhint');
            }
        });
};


function initFirstSubmissionForm() {

    enableJournalPublished();
    // if I am in the first page
    if (jQuery("#aspect_submission_StepTransformer_div_submit-select-publication").length > 0) {

        // Status (onLoad of the page): STATUS_ACCEPTED
        if (jQuery('input[name|="article_status"]:checked').val()=='1') {

            jQuery("#status_other_than_published").show();
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#manu").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#journalIDStatusInReview").hide();

            jQuery("#prism_publicationName").show();


            var journal = jQuery("#aspect_submission_StepTransformer_field_prism_publicationName").val();
            var integrated = false;
            var journal_integrated_length = jQuery("#aspect_submission_StepTransformer_field_journalIDStatusIntegrated option").length;

            for (var i=0; i<journal_integrated_length; i++)
            {
                var integratedJournal = document.getElementById('aspect_submission_StepTransformer_field_journalIDStatusIntegrated').options[i].value;
                //alert(integrated);

                if(integratedJournal == journal)
                {
                    //alert('found');
                    integrated = true;
                    break;
                }
            }
            if(journal==""){
                jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
                jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            }
            else if (integrated==true) {
                jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").show();
                jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            }
            else {
                jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").show();
                jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            }

            enableNextButton();
        }

        // Status (onLoad of the page): STATUS_PUBLISHED
        else if (jQuery('input[name|="article_status"]:checked').val()=='0') {
            jQuery("#prism_publicationName").hide();
            jQuery("#manu").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#manu").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            jQuery("#journalIDStatusInReview").hide();

            jQuery("#aspect_submission_StepTransformer_list_doi").show();
            jQuery("#status_other_than_published").hide();

            enableNextButton();

        }

        // Status (onLoad of the page): STATUS_IN_REVIEW
        else if (jQuery('input[name|="article_status"]:checked').val()=='2') {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#prism_publicationName").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();

            jQuery("#manu").show();
            jQuery("#journalIDStatusInReview").show();
            jQuery("#status_other_than_published").show();
            enableNextButton();
        }

//        // Status (onLoad of the page): STATUS_NOT_YET_DEPLOYED
//        else if (jQuery("'#xmlui_submit_publication_article_status_not_yet_submitted':checked").val()) {
//            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
//            jQuery("#journalID").hide();
//            jQuery("#manu").hide();
//            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
//            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
//            jQuery("#journalIDStatusInReview").hide();
//
//            jQuery("#status_other_than_published").show();
//            jQuery("#journalIDStatusNotYetSubmitted").show();
//
//            enableNextButton();
//        }

        // Status (onLoad of the page): STATUS_NULL
        else {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#prism_publicationName").hide();
            jQuery("#manu").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            jQuery("#journalIDStatusInReview").hide();
            jQuery("#status_other_than_published").hide();

            enableNextButton();
        }

        // Click: status_published
        jQuery('#xmlui_submit_publication_article_status_published').click(function () {
            jQuery("#prism_publicationName").hide();
            jQuery("#manu").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
            jQuery("#journalIDStatusInReview").hide();

            jQuery("#status_other_than_published").hide();
            jQuery("#aspect_submission_StepTransformer_list_doi").show();

            enableNextButton();
        });

        // Click: status_in_review
        jQuery('#xmlui_submit_publication_article_status_in_review').click(function () {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#prism_publicationName").hide();
            jQuery("#aspect_submission_StepTransformer_item_new-manu-comp-acc").hide();
            jQuery("#aspect_submission_StepTransformer_item_new-manu-acc").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();

            jQuery("#status_other_than_published").show();
            jQuery("#manu").show();
            jQuery("#journalIDStatusInReview").show();
            enableNextButton();
        });

        // Click: status_accepted
        jQuery('#xmlui_submit_publication_article_status_accepted').click(function () {
            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
            jQuery("#manu").hide();
            jQuery("#journalIDStatusNotYetSubmitted").hide();
            jQuery("#journalIDStatusInReview").hide();

            jQuery("#status_other_than_published").show();
            jQuery("#prism_publicationName").show();
            enableNextButton();
        });

//        // Click: status_not_yet_published
//        jQuery('#xmlui_submit_publication_article_status_not_yet_submitted').click(function () {
//            jQuery("#aspect_submission_StepTransformer_list_doi").hide();
//            jQuery("#journalID").hide();
//            jQuery("#manu").hide();
//            jQuery("#journalIDStatusNotYetSubmitted").hide();
//            jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
//            jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
//            jQuery("#journalIDStatusInReview").hide();
//
//            jQuery("#status_other_than_published").show();
//            jQuery("#journalIDStatusNotYetSubmitted").show();
//            enableNextButton();
//        });

        //change: prism_publicationName
        jQuery("#aspect_submission_StepTransformer_field_prism_publicationName").focusout(function() {



            if (jQuery('input[name|="article_status"]:checked').val()=='1') {
                var journal = jQuery("#aspect_submission_StepTransformer_field_prism_publicationName").val();


                var journal_integrated_length = jQuery("#aspect_submission_StepTransformer_field_journalIDStatusIntegrated option").length;
                // alert(journal_integrated_length);
                var integrated = false;

                for (var i=0; i<journal_integrated_length; i++)
                {
                    var integrated = document.getElementById('aspect_submission_StepTransformer_field_journalIDStatusIntegrated').options[i].value;
                    //alert(integrated);

                    if(integrated == journal)
                    {
                        //alert('found');
                        integrated = true;
                        break;
                    }
                }

                if(journal==""){
                    jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
                    jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
                }
                //else if (journal.indexOf('*') != -1) {
                else if(integrated == true){
                    jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").show();
                    jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").hide();
                }
                else {
                    jQuery("#aspect_submission_StepTransformer_item_manu_accepted-cb").show();
                    jQuery("#aspect_submission_StepTransformer_item_manu-number-status-accepted").hide();
                    //update the order form after enter journal name
                    updateOrder();
                }
            }
            enableNextButton();
        });

        jQuery('input[name|="license_accept"]').change(function() {
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
        jQuery('input[name|="manu_accepted-cb"]').change(function() {
            enableNextButton();
        });
        jQuery('input[name|="unknown_doi"]').keyup(function() {
            enableJournalPublished();
        });
        jQuery('input[name|="article_doi"]').keyup(function() {
            enableJournalPublished();
        });

    }
}

function enableJournalPublished(){
    //console.log(jQuery('#unknown-doi-panel span.field-help'));
    if(jQuery('input[name|="article_doi"]').val()!="")
    {
        jQuery('input[name|="unknown_doi"]').attr("disabled", "disabled");
        jQuery('input[name|="unknown_doi"]').css("background-color","#E3E3E3");
        jQuery('#unknown-doi-panel span.field-help').attr("style","color:grey");
        jQuery('label.ds-form-label-select-publication').css("color","");
    }
    else
    {
        jQuery('input[name|="unknown_doi"]').removeAttr("disabled");
        jQuery('input[name|="unknown_doi"]').css("background-color","");
        jQuery('#unknown-doi-panel span.field-help').attr("style","color:black");
        jQuery('label.ds-form-label-select-publication').css("color","grey");
    }
    if(jQuery('input[name|="unknown_doi"]').val()!="")
    {
        jQuery('input[name|="article_doi"]').attr("disabled", "disabled");
        jQuery('input[name|="article_doi"]').css("background-color","#E3E3E3");
        jQuery('label.ds-form-label-select-publication').css("color","grey");
        jQuery('#unknown-doi-panel span.field-help').attr("style","color:black");
    }
    else
    {
        jQuery('input[name|="article_doi"]').removeAttr("disabled");
        jQuery('input[name|="article_doi"]').css("background-color","");
        jQuery('label.ds-form-label-select-publication').css("color","");
        jQuery('#unknown-doi-panel span.field-help').attr("style","color:grey");
    }
}
function enableNextButton() {

    // note: JOURNAL_ID is always present because the default value!!

    // license must be cheched



    if (jQuery('input[name|="license_accept"]:checked').val()) {

        // Status: status_accepted ==> must have JOURNAL_ID && (MANUSCRIPT_NUMBER || CB_This manuscript has been accepted)
        if (jQuery('input[name|="article_status"]:checked').val()=='1') {
            var journal = jQuery("#aspect_submission_StepTransformer_field_prism_publicationName").val();

            if (jQuery('input[name|="manu_accepted-cb"]:checked').val()) {
                jQuery("#aspect_submission_StepTransformer_field_submit_next").removeAttr("disabled");
            }
            else {
                if(jQuery('input[name="manu-number-status-accepted"]').val())
                    jQuery("#aspect_submission_StepTransformer_field_submit_next").removeAttr("disabled");
            }

            return;
        }

        // Status: status_published ==> must have DOI || CB_unknown_doi
        else if (jQuery('input[name|="article_status"]:checked').val()=='0') {

            var val = jQuery("#aspect_submission_StepTransformer_field_article_doi").val();
            var jol =  jQuery('input[name|="journal_name"]').val();
            if (val != "" || jol != "") {

                //alert("article_doi || unknown_doi ok: enable next!");

                jQuery("#aspect_submission_StepTransformer_field_submit_next").removeAttr("disabled");
                return;
            }
        }

        // Status: status_in_review ==> must have JOURNAL_ID (MANUSCRIPT_NUMBER is optional)
        else if (jQuery('input[name|="article_status"]:checked').val()=='2') {
            jQuery("#aspect_submission_StepTransformer_field_submit_next").removeAttr("disabled");
            return;
        }

        // Status status_not_yet_submitted ==> must have JOURNAL_ID
//    else if (jQuery("'#xmlui_submit_publication_article_status_not_yet_submitted':checked").val()) {
//        jQuery("#aspect_submission_StepTransformer_field_submit_next").removeAttr("disabled");
//        return;
//    }

    }
    jQuery("#aspect_submission_StepTransformer_field_submit_next").attr("disabled", "disabled");
}

// Very simple email validation, must contain an @ sign and at least one dot (.). 
// Also, the @ must not be the first character of the email address, and the
// last dot must be present after the @ sign, and minimum 2 characters before the end:
function validateEmailAddress(email) {
    var atpos=email.indexOf("@");
    var dotpos=email.lastIndexOf(".");
    if (atpos < 1 || dotpos < atpos+2 || dotpos+2 >= email.length) {
        return false;
    } else {
        return true;
    }
}

// Mailing list subscription form on homepage, submits to /subscribe via ajax
function subscribeMailingList(form) {
    $('#file_news_div_mailing_list_input_email_error').remove();
    var emailField = jQuery('#file_news_div_mailing_list_input_email');
    emailField.removeClass("error");
    if(validateEmailAddress(emailField[0].value) == false) {
        var errorText = jQuery("<span>")
            .attr("id", "file_news_div_mailing_list_input_email_error")
            .text("Please enter a valid email address.")
            .addClass("error");
        errorText.insertAfter(emailField);
        emailField.addClass("error");
    } else {
        var subscribeButton = jQuery('#file_news_div_mailing_list_input_subscribe');
        subscribeButton.attr("disabled", "disabled").attr("value","Subscribing...");
        var subscribeURL = '/subscribe';
        jQuery.ajax({
          url: subscribeURL,
          data: jQuery(form).serialize()
        }).done(function(data) {
            // Remove the email input and replace with thank you text
            var emailField = jQuery('#file_news_div_mailing_list_input_email');
            emailField.parent().text("Thank you for signing up!")
            emailField.remove();
            var subscribeButton = jQuery('#file_news_div_mailing_list_input_subscribe');
            subscribeButton.attr("disabled", "disabled").attr("value","Subscribed!");    
        }).fail(function(data) {
            // Remove the email input and replace with thank you text
            var errorText = jQuery("<span>")
                .attr("id", "file_news_div_mailing_list_input_email_error")
                .text("An error occurred, please try again.")
                .addClass("error");
            errorText.insertAfter(emailField);
            emailField.addClass("error");
            var subscribeButton = jQuery('#file_news_div_mailing_list_input_subscribe');
            subscribeButton.removeAttr("disabled");
            subscribeButton.attr("value","Subscribe");    
        });
    }
    return false;
}

// Adapted from http://stackoverflow.com/questions/3717793/javascript-file-upload-size-validation
function getUploadFileSize(fileInputElement) {
    if (!window.FileReader) {
        // The FileReader API (http://www.w3.org/TR/FileAPI/) is required to do this
        // calculation.  If it is not present in the browser, we can't check the file
        // size locally
        console.error("Browser does not support FileAPI");
        return 0;
    }
    
    if (!fileInputElement) {
        console.error("No file input element provided");
        return 0;
    }
    else if (!fileInputElement.files) {
        console.error("This browser doesn't seem to support the `files` property of file inputs.");
        return 0;
    }
    else if (!fileInputElement.files[0]) {
        console.error("No file selected");
        return 0;
    }
    else {
        file = fileInputElement.files[0];
        return file.size; // size in bytes
    }
}


/////// TESTING PROGRESS BAR /////////////////
//function fileSelected() {
//    var file = document.getElementById('aspect_submission_StepTransformer_field_dataset-file').files[0];
//    if (file) {
//        var fileSize = 0;
//        if (file.size > 1024 * 1024)
//            fileSize = (Math.round(file.size * 100 / (1024 * 1024)) / 100).toString() + 'MB';
//        else
//            fileSize = (Math.round(file.size * 100 / 1024) / 100).toString() + 'KB';
//
//        document.getElementById('fileName').innerHTML = 'Name: ' + file.name;
//        document.getElementById('fileSize').innerHTML = 'Size: ' + fileSize;
//        document.getElementById('fileType').innerHTML = 'Type: ' + file.type;
//    }
//}
//
//function uploadFile() {
//    var fd = new FormData();
//    fd.append("aspect_submission_StepTransformer_field_dataset-file", document.getElementById('aspect_submission_StepTransformer_field_dataset-file').files[0]);
//    var xhr = new XMLHttpRequest();
//    xhr.upload.addEventListener("progress", uploadProgress, false);
//    xhr.addEventListener("load", uploadComplete, false);
//    xhr.addEventListener("error", uploadFailed, false);
//    xhr.addEventListener("abort", uploadCanceled, false);
//    //xhr.open("POST", "UploadMinimal.aspx");
//    xhr.send(fd);
//}
//
//function uploadProgress(evt) {
//    if (evt.lengthComputable) {
//        var percentComplete = Math.round(evt.loaded * 100 / evt.total);
//        document.getElementById('progressNumber').innerHTML = percentComplete.toString() + '%';
//    }
//    else {
//        document.getElementById('progressNumber').innerHTML = 'unable to compute';
//    }
//}
//
//function uploadComplete(evt) {
//    /* This event is raised when the server send back a response */
//    alert(evt.target.responseText);
//}
//
//function uploadFailed(evt) {
//    alert("There was an error attempting to upload the file.");
//}
//
//function uploadCanceled(evt) {
//    alert("The upload has been canceled by the user or the browser dropped the connection.");
//}
/////// END TESTING PROGRESS BAR /////////////////

function DryadEditMetadataAndPropagate(metadataFieldName) {
  // adds a hidden input field, like
  // <input type="hidden" name="metadata_field_name" value="dc.author">
  // before submitting the form
  var hiddenElement = jQuery('<input type="hidden" name="propagate_md_field_name">');
  hiddenElement.attr('value',metadataFieldName);
  jQuery('form').append(hiddenElement);
  // must click the submit_update button for administrative.js to interpret as editing
  jQuery('form input[name="submit_update"]:first').click();
}

function DryadShowPropagateMetadata(serverUrl, metadataFieldName, packageDoi) {
  var link = serverUrl + '?metadata_field_name=' + metadataFieldName + '&package_doi=' + packageDoi;

  var width = 680;
  var height = 480;
  var left;
  var top;
  var cOffset = 0;
  if (window.screenX == null) {
      left = window.screenLeft + cOffset.left - (width / 2);
      top = window.screenTop + cOffset.top - (height / 2);
  } else {
      left = window.screenX + cOffset.left - (width / 2);
      top = window.screenY + cOffset.top - (height / 2);
  }
  if (left < 0) left = 0;
  if (top < 0) top = 0;
  var pw = window.open(link, 'ignoreme',
          'width=' + width + ',height=' + height + ',left=' + left + ',top=' + top +
                  ',toolbar=no,menubar=no,location=no,status=no,resizable');
  if (window.focus) pw.focus();
  return false;
}

function DryadClosePropagate() {
  window.close();
  return false;
}
