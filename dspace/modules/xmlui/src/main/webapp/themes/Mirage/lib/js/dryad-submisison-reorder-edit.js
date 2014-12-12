//  JS behaviors for the "Describe Publication" page
//  /handle/*/*/submit/*.continue
//  These functions enable page-fragment reloading in place of
//  full-page reloads for the metadata fields in the form
//  #aspect_submission_StepTransformer_div_submit-describe-publication
//  The redirect buttons ("Save & Exit" and "Continue to Describe Data")
//  still trigger a full page reload.
jQuery(document).ready(function() {
    var form_ids = [
        'aspect_submission_StepTransformer_div_submit-describe-publication'
      , 'aspect_submission_StepTransformer_div_submit-describe-dataset'
    ]
    , reinterp_timeout  = 500;  // ms, elapse to check for focus on related interp field
    // update the part of the form associated with the input button that was clicked
    // selector: string, jQuery selector to identify the li.ds-form-item element
    //      to be replaced by the update
    // data: string, HTML reponse from server (entire page)
    var update_form_fragment = function(selector,data) {
        var $result_doc, $src, $target, i, file_input;
        try {
            $result_doc = jQuery(document.createElement('div')).append(jQuery(data));
        } catch (e) {
            console.log('Error parsing result to Document: ' + e.toString());
            return;
        }
        // update DOM with fragment selected from response document
        if ($result_doc.length > 0 && $result_doc.find(selector).length > 0) {
            jQuery(selector).replaceWith($result_doc.find(selector));
            submit_describe_publication_binders();
        // on failure to isolate form, load entire response page, which is
        // likely to contain an error message
        } else {
            console.log('No handlable data from returned page.');
            jQuery('html').html(data);
        }
    };
    // need to save off the name of the submit button that was clicked, since
    // there is no way to retrieve that information on the form's submit event
    var clicked_btn_name;
    var watch_clicked = function(e) {
        clicked_btn_name = jQuery(e.target).attr('name');
        return true; // propogate default event (i.e., form submission)
    };
    // function to handle the submit event for the form
    var submit_describe_publication_onsubmit = function(e) {
        var $input      = jQuery('input[name="' + clicked_btn_name +'"]')   // the <input> triggering the submit
          , input_name  = clicked_btn_name          // input name: localize to this scope for the ajax call
          , $form       = jQuery(e.target)          // the entire describe-publication form
          , success     = false                     // unsuccessful ajax call until we receive a 200
          , prevent_default = false                 // whether to continue with form submission/reload
          , ajax_data                               // ajax data, for passing to the updater function
          , form_data;
        // undefine this variable, due to the flurry of onclick events raised by a button
        // click. TODO: determine why multiple button onclick events are raised
        clicked_btn_name = undefined;
        if (input_name !== undefined) {
            // continue with full page reload for these two button click events
            if (input_name === 'submit_cancel' || input_name === 'submit_next') {
                return;
            // File submission is a NON-AJAX form submission (i.e., not a change from previous behavior)
            } else if ( input_name === 'dataset-file' || input_name === 'dc_readme'
                     || input_name === 'submit_remove_dataset' || input_name.match(new RegExp('^submit_dc_readme_remove_'))
            ) {
                // start: from utils.js
                var dataFileIdenTxt = jQuery('input#aspect_submission_StepTransformer_field_datafile_identifier');
                if (dataFileIdenTxt.val() == dataFileIdenTxt.attr('title')) {
                    dataFileIdenTxt.val('');
                }
                var repoNameTxt = jQuery('input#aspect_submission_StepTransformer_field_other_repo_name');
                if (repoNameTxt.val() == repoNameTxt.attr('title')) {
                    repoNameTxt.val('');
                }
                jQuery('select#aspect_submission_StepTransformer_field_dc_type_embargo').removeAttr('disabled');
                // end: from utils.js
                prevent_default = false;

            // do page-fragment reload for other form submission clicks
            } else {
                form_data = $form.serializeArray();
                // jQuery does not add the submission button, which is expected in the
                // request parameters by the DescribePublicationStep.java code; added here manually
                form_data.push({ name: input_name, value: $input.val() });
                prevent_default = true;
                $form.find('input').attr('disabled','disabled');
                try {
                    jQuery.ajax({
                          url         : $form.attr('action')
                        , data        : form_data
                        , type        : "POST"
                        , success     : function(data,textStatus,jqXHR) {
                            if (jqXHR.status === 200) {
                                success   = true;
                                ajax_data = data;
                            } else {
                                console.log('Error: Form "submit-describe-publication" returned non-success status: ' + jsXHR.status);
                            }
                        }
                        , error : function(jqXHR,textStatus,errorThrown) {
                            console.log(textStatus);
                        }
                        , complete : function(jqXHR,textStatus) {
                            // update the page using data associated with the input the user selected
                            if (success === true) {
                                update_form_fragment('#' + $form.attr('id'),ajax_data);
                            }
                        }
                    });
                } catch (e) {
                    console.log('Error: Form "submit-describe-publication" encountered AJAX error: ' + e.toString());
                }
            }
        }
        // prevent default form-submit action (which triggers page reload)
        // TODO: remove this variable once the multiple-event-raising situation has been sorted out
        if (prevent_default) {
            e.preventDefault();
        }
    };
    //
    var reorderOptions = function($select, $row, $table, to) {
        // pull out author data rows, then reorder per user selection
        $row.remove();
        var rows = $table.find('tr.ds-edit-reorder-input-row').remove();
        rows.splice(to-1, 0, $row[0]);
        var max = rows.length;
        // update the row's inputs and select/option data
        jQuery.each(rows, function(i,elt) {
            var $tr = jQuery(elt)
              , ind = (i+1).toString()
              , $input, name;
            $tr.find('select').val(ind);
            $tr.find('option').each(function(j,option){
                $input = jQuery(option);
                if (parseInt($input.text()) > max){
                    $input.remove();
                }
            });
            $tr.find('input').each(function(j,input) {
                $input = jQuery(input);
                name = $input.attr('name');
                if (name.match(new RegExp('^dc_|^dwc_'))) {
                    name = $input.attr('name').replace(new RegExp('_[0-9]+$'), '_'.concat(ind));
                    $input.attr('name', name);
                }
            });
        });
        $table.append(rows);
    };
    // event handler for the on-change event for an author's order changing
    var handleAuthorReorderEvent = function(evt) {
        var from    = jQuery(evt.target).data('prev')
          , to      = parseInt(evt.target.value)
          , $row    = jQuery(evt.target).closest('tr')
          , $table  = $row.closest('table');
        if (from !== to) {
            $table.next('.ds-update-button').removeAttr('disabled');
            reorderOptions(jQuery(evt.target), $row, $table, to);
            submit_describe_publication_binders();
        }
    };
    // update the value of the now-hidden "interp" element
    // after an update of the text field
    var reinterpret_inputs = function($hidden,$shown,interp_input,sep) {
        var newval = '';
        if ($shown.length === 1) {
            newval = $shown.val();
        } else if ($shown.length === 2) {
            newval = $shown.map(function(i,elt) {
                return elt.value;
            }).toArray().join(sep);
            interp_input.value = newval;
        }
        $hidden.text(newval);
        // unhide and rehide editable fields
        $hidden.css('display', '');         // <span>
        $shown.attr('type', 'hidden');      // <input>
    };
    // event handler for the author's Edit button click event
    var handleEdit = function(event) {
        var $row    = jQuery(event.target).closest('tr')
          , $table  = $row.closest('table')
          , $hidden = $row.find('td.ds-reorder-edit-input-col input[type="hidden"]')
                          .not('[class*="authority"]')
                          .not('[name*="authority"]')
                          .not('[value="blank"]')
          , $shown  = $row.find('span.ds-interpreted-field')
          , $button = jQuery(event.target)
          , interp_input, sep = '';
        // don't unhide the 'interpreted' input for composite controls (e.g., an author name)
        if ($hidden.length === 3) {
            interp_input = $hidden.splice(2,1)[0];
            sep = interp_input.value.substring($hidden[0].value.length);
            sep = sep.substring(0,sep.length-$hidden[1].value.length);
        }
        // disable edit button
        $button.attr('disabled','disabled');
        // show lastname/firstname input fields
        // and set focus to the first one
        $hidden.attr('type','text');
        jQuery($hidden[0]).focus();
        // update the interpreted field whenever
        jQuery($hidden).each(function(i,elt) {
            jQuery(elt).blur(function() {
                window.setTimeout(function() {
                    if ($row.find('input:focus[type="text"]').length === 0) {
                        reinterpret_inputs($shown,$hidden,interp_input,sep);
                        $button.removeAttr('disabled');
                    }
                }, reinterp_timeout);
            });
        });
        // hide 'interpreted' span
        $shown.css('display', 'none');
        event.preventDefault();
    };
    // event handler for the author's Delete button click event
    var handleDelete = function(event) {
        var $row = jQuery(event.target).closest('tr')
          , $next = $row.next()
          , $table  = $row.closest('table')
          , max = $table.find('tr').length - 1  // number of rows after delete
          , $event = jQuery(event)
          , $select;
        // set this value for the form submission handler
        jQuery(event.target).closest('.ds-form-content').find('input.ds-delete-button').attr('name');
        $row.remove();
        // reorder the $next row
        if ($next.length > 0) {
            $select = $next.find('select.ds-edit-reorder-order-select');
            var ind = parseInt($select.find('option[selected]').text());
            reorderOptions($select, $next, $table, ind-1);
        }
        $table.find('select.ds-edit-reorder-order-select option').each(function(j,option){
            var $option = jQuery(option);
            if (parseInt($option.text()) > max) {
                $option.remove();
            }
        });
        event.preventDefault();
    };
    var describe_dataset_click = function() {
        jQuery("input[type=radio][name='datafile_type'][value='file']").click();
        enableEmbargo();
    };
    var file_onchange = function(e) {
        jQuery('#aspect_submission_StepTransformer_field_dataset-file-error').remove();
        if (this.id == 'aspect_submission_StepTransformer_field_dataset-file') {
            //Make sure the title gets set with the filename
            var fileName = jQuery(this).val().substr(0, jQuery(this).val().lastIndexOf('.'));
            fileName = fileName.substr(fileName.lastIndexOf('\\') + 1, fileName.length);
            var title_t = jQuery('input#aspect_submission_StepTransformer_field_dc_title').val();
            if (title_t == null || title_t == '')
                jQuery('input#aspect_submission_StepTransformer_field_dc_title').val(fileName);
        }

        // Check the file size.  If greater than 1.3 GB, display a warning and do not
        // auto-submit the form
        var fileSize = getUploadFileSize(this);
        if(fileSize > 1.3 * 1024 * 1024 * 1024) { // 1.3 GB
            console.error("File " + fileSize + " is too big");
            var errorText = jQuery("<span>")
                .attr("id", "aspect_submission_StepTransformer_field_dataset-file-error")
                .text("This data file is too large to upload.  For assistance, please visit ")
                .addClass("error");
            var helpLink = jQuery("<a></a>")
                .attr("href", "http://wiki.datadryad.org/Large_File_Transfer")
                .text("Large file transfer.");
            errorText.append(helpLink);
            jQuery(this).after(errorText);
        } else {
            clicked_btn_name = jQuery(e.target).attr('name');
            //Now find our form
            var form = jQuery(this).closest("form");
            //Now that we have our, indicate that I want it processed BUT NOT TO CONTINUE, just upload
            //We do this by adding a param to the form action
            form.attr('action', form.attr('action') + '?processonly=true');
            //Now we submit our form
            form.submit();
        }
    };
    var datafile_type_radio_change = function(e) {
        //If an external reference is selected we need to disable our embargo
        if (jQuery(this).val() == 'identifier') {
            disableEmbargo();
        } else {
            enableEmbargo();
        }
    };
    var datafile_repo_change = function() {
        if (jQuery(this).val() == 'other') {
            jQuery("input[name='other_repo_name']").show();
        } else {
            jQuery("input[name='other_repo_name']").hide();
        }
    };
    var init_identifier_hints = function() {
        //For our identifier a hint needs to be created
        var dataFileIdenTxt = jQuery('input#aspect_submission_StepTransformer_field_datafile_identifier');
        dataFileIdenTxt.inputHints();
        var repoNameTxt = jQuery('input#aspect_submission_StepTransformer_field_other_repo_name');
        repoNameTxt.inputHints();
        dataFileIdenTxt.blur(function() {
            if (jQuery(this).attr('title') != jQuery(this).val())
                jQuery('input#aspect_submission_StepTransformer_field_dc_title').val(jQuery(this).val());
        });
    };
    // these event handlers need to be registered any time the form is submitted, since the DOM is modified
    var submit_describe_publication_binders = function() {
        for (var i = 0; i < form_ids.length; i++) {
            jQuery('#' + form_ids[i] + ' input.ds-button-field').bind('click', watch_clicked);
            jQuery('#' + form_ids[i] + ' input.ds-file-field'  ).bind('click', watch_clicked);
            jQuery('#' + form_ids[i]).bind('submit', submit_describe_publication_onsubmit);
        }
        jQuery('input.ds-edit-button').bind('click',handleEdit);
        jQuery('input.ds-delete-button').bind('click',handleDelete);
        // bind the onchange event to this function, and also store the current value of
        // the selected option, for use in updating the underlying input data
        jQuery('select.ds-edit-reorder-order-select').each(function(i,elt) {
            jQuery(this).on('change', handleAuthorReorderEvent).data('prev',parseInt(this.value));
        });
        // here to end of function: from Mirage/../utils.js
        jQuery('input#aspect_submission_StepTransformer_field_dataset-file').bind('click', describe_dataset_click);
        jQuery('#aspect_submission_StepTransformer_div_submit-describe-dataset').find(":input[type=file]").bind('change', file_onchange);
        jQuery("input[type='radio'][name='datafile_type']").change(datafile_type_radio_change);
        //Should we have a data file with an external repo then disable the embargo
        if (jQuery("input[name='disabled-embargo']").val()) {
            disableEmbargo();
        }
        jQuery('select#aspect_submission_StepTransformer_field_datafile_repo').bind('change', datafile_repo_change);
        // Hide the other repo name initially
        jQuery("input[name='other_repo_name']").hide();
        init_identifier_hints();
    };
    submit_describe_publication_binders();
});
