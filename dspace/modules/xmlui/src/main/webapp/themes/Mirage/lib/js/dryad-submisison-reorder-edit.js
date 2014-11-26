//  JS behaviors for the "Describe Publication" page
//  /handle/*/*/submit/*.continue
//  These functions enable page-fragment reloading in place of
//  full-page reloads for the metadata fields in the form
//  #aspect_submission_StepTransformer_div_submit-describe-publication
//  The redirect buttons ("Save & Exit" and "Continue to Describe Data")
//  still trigger a full page reload.
jQuery(document).ready(function(){
    var form_selector = '#aspect_submission_StepTransformer_div_submit-describe-publication';
    // update the part of the form associated with the input button that was clicked
    // selector: string, jQuery selector to identify the li.ds-form-item element
    //      to be replaced by the update
    // data: string, HTML reponse from server (entire page)
    var update_form_fragment = function(selector,data) {
        var $result_doc;
        try {
            $result_doc = jQuery(document.createElement('div')).append(jQuery(data));
        } catch (e) {
            console.log('Error parsing result to Document: ' + e.toString());
            return;
        }
        // update DOM with fragment selected from response document
        if ($result_doc.length > 0 && $result_doc.find(selector).length > 0) {
            jQuery(selector).replaceWith($result_doc.find(selector));
            // refresh event bindings on updated page
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
          , input_name  = clicked_btn_name                                  // input name: localize to this scope for the ajax call
          , $form       = jQuery(e.target)          // the entire describe-publication form
          , form_data   = $form.serializeArray()    // the form's data
          , success     = false                     // unsuccessful ajax call until we receive a 200
          , ajax_data                               // ajax data, for passing to the updater function
          , prevent_default = false;                // whether to continue with form submission/reload
        // undefine this variable, due to the flurry of onclick events raised by a button
        // click. TODO: determine why multiple button onclick events are raised
        clicked_btn_name = undefined;
        if (input_name !== undefined) {
            // continue with full page reload for these two button click events
            if (input_name === 'submit_cancel' || input_name === 'submit_next') {
                return;
            // do page-fragment reload for other form submission clicks
            } else if (form_data.length > 0) {
                // jQuery does not add the submission button, which is expected in the
                // request parameters by the DescribePublicationStep.java code; added here manually
                form_data.push({ name: input_name, value: $input.val() });
                prevent_default = true;
                $form.find('input').attr('disabled','disabled');
                try {
                    jQuery.ajax({
                          url     : $form.attr('action')
                        , data    : jQuery.param(form_data)
                        , method  : "POST"
                        , success : function(data,textStatus,jqXHR) {
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
                                update_form_fragment(form_selector,ajax_data);
                            }
                        }
                    });
                } catch (e) {
                    console.log('Error: Form "submit-describe-publication" encountered AJAX error: ' + e.toString());
                }
            } else {
                console.log('Error: Form "submit-describe-publication" submitted with empty data.');
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
    // event handler for the author's Edit button click event
    var handleEdit = function(event) {
        var $row    = jQuery(event.target).closest('tr')
          , $table  = $row.closest('table')
          , $update = $table.next('.ds-update-button')
          , $hidden = $row.find('td.ds-reorder-edit-input-col input[type="hidden"]').not('.ds-authority-confidence-input')
          , $shown  = $row.find('span.ds-interpreted-field');
        // todo: handle this more cleanly; don't unhide the 'interpreted'
        // input for composite controls (e.g., an author name)
        if ($hidden.length === 4) $hidden.splice(2,1);
        // disable edit button
        jQuery(event.target).attr('disabled','disabled');
        // show lastname/firstname input fields
        $hidden.attr('type','text');
        // enable Update button on a change event
        $hidden.bind('change keyup', function(evt) {
            $update.removeAttr('disabled');
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
          , $select;
        // set this value for the form submission handler
        clicked_btn_name = jQuery(event.target).closest('.ds-form-content').find('.ds-delete-button').attr('name');
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

        var e = jQuery.Event('click');
        e.target = jQuery(form_selector);
        submit_describe_publication_onsubmit(e);
        submit_describe_publication_binders();
        event.preventDefault();
    };
    var disableEditAuthority = function() {
        jQuery(form_selector + ' input.ds-authority-confidence-input').each(function(i,elt){
            debugger;
            console.log(elt);
        });
    };
    // these event handlers need to be registered any time the form is submitted, since the DOM is modified
    var submit_describe_publication_binders = function() {
        jQuery(form_selector + ' input.ds-button-field').bind('click', watch_clicked);
        jQuery(form_selector).bind('submit', submit_describe_publication_onsubmit);
        jQuery('input.ds-edit-button').bind('click',handleEdit);
        jQuery('input.ds-delete-button').bind('click',handleDelete);
        // bind the onchange event to this function, and also store the current value of
        // the selected option, for use in updating the underlying input data
        jQuery('select.ds-edit-reorder-order-select').each(function(i,elt) {
            jQuery(this).on('change', handleAuthorReorderEvent).data('prev',parseInt(this.value));
        });
    };
    submit_describe_publication_binders();
});