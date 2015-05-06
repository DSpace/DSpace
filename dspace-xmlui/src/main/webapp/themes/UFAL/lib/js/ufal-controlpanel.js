/*global jQuery */
/*jshint globalstrict: true*/
'use strict';
var ufal = ufal || {};

String.prototype.endsWith = function (suffix) {
    return this.indexOf(suffix, this.length - suffix.length) !== -1;
};
String.prototype.startsWith = function (str){
    return this.indexOf(str) === 0;
};
jQuery.fn.exists = function () {
    return this.length > 0;
};

ufal.controlpanel = {

    MAIN_CONTROL_CHECK : "MainControlCheck",
    POST : "POST:",

    // fnc
    all_checks : function () {
        return jQuery('tr.this-is-check td:nth-child(2)');
    },

    all_checks_headers : function () {
        return jQuery('tr.this-is-check td:nth-child(1)');
    },


    run_check_async: function (index_id, url, url_this, type, form_data) {
        // do the ajax
        jQuery.ajax({
            url: url,
            type: type,
            data: form_data,
            timeout: 120000,
            context: document.body,
            beforeSend: function (xhr) {
                url_this.addClass('running');
            }
        }).done(function (data) {
            url_this.removeClass('running');
            var jdata = jQuery(data);
            if (jQuery('.alert-error', jdata).exists()) {
                //jQuery('div#'+index_id).fadeIn().text( "Check" + url + ".. failed!!" ).addClass('notok');
                url_this.addClass('notok');
                var title = url;
                if(form_data !== ""){
                    title += " "+ form_data.curate_task;
                }
                var jerrors = jQuery('.control_check', jdata);
                var errors = "";
                if(!jerrors.exists()){
                    errors = jQuery('.alert-error', jdata).parent().html();
                }else{
                    errors = jerrors.html();
                }
                jQuery('div#' + index_id).fadeIn().html(
                    "<h2>Check " + title + " .. has errors!</h2><hr /><br />" + errors
                ).addClass('notok');
            } else {
                //jQuery('div#'+index_id).fadeIn().text( "Check" + url + ".. success!!" ).addClass('ok');
                url_this.addClass('ok');
            }
        }).fail(function (data) {
            url_this.removeClass('running');
            var jdata = jQuery(data);
            url_this.addClass('notok');
            jQuery('div#' + index_id).fadeIn().html(
                "<h2>Check " + url + ".. failed to complete!!</h2><hr /><br />" + jdata.html()
            ).addClass('notok');
        });
    },

    start_checking : function () {
        var check = ufal.controlpanel.all_checks();
        if (check.exists()) {
            // reset state
            jQuery('div.result').remove();

            // run it
            var check_table = jQuery('div#aspect_administrative_ControlPanel_div_control-button');
            check.each(function (index) {
                var url = jQuery(this).text();
                var url_this = jQuery(this);
                if (url.endsWith(ufal.controlpanel.MAIN_CONTROL_CHECK)) {
                    return;
                }
                
                var type = "GET";
                var data = "";
                
                // reset
                url_this.removeClass('notok ok');

                if (!jQuery(':checkbox', jQuery(this).prev()).attr('checked')) {
                    return;
                }

                if (url.startsWith(ufal.controlpanel.POST)){
                    var task = url.replace(ufal.controlpanel.POST,"");
                    var site_handle = jQuery("input[name='site_handle']").val();
                    var administrative_continue = "";
                    jQuery.ajax({url : 'curate', async: false}).done(function(data){
                        var jdata = jQuery(data);
                        administrative_continue = jQuery("input[name='administrative-continue']", jdata).val();
                    });	
                    data = { identifier : site_handle, curate_task : task, submit_curate_task : "Perform", 'administrative-continue': administrative_continue};
                    url = 'curate';
                    type = 'POST';
                }

                // add result frame
                var index_id = 'result' + index;
                check_table.after('<div id="' + index_id + '" class="result well well-white">Result...</div>');
                ufal.controlpanel.run_check_async(index_id, url, url_this, type, data);
            }); // each
        }
    }, // start_checking

    make_list_files_clickable : function () {
        var but = jQuery('ul.file-chooser-list').prev();
        if (but) {
            but.attr('btn btn-default');
            but.append(' <span class="caret"></span>');

            var but_lf = but;
            but_lf.next().hide();
            but_lf.click(function () {
                but_lf.next().toggle();
                return false;
            });
        }
    },

    make_logins_hideable : function () {
        if (jQuery('table.table_hideable').exists()) {
            jQuery('table.table_hideable tr').each(function (index) {
                jQuery(this).has('td').hide();
                jQuery(this).has('th').css('cursor', 'pointer')
                jQuery(this).click(function () {
                    jQuery(this).siblings().each(function (index){
                        jQuery(this).toggle();
                    });
                });
            });
        }
    },

    replace_br: function (cls) {
        jQuery(cls).each(function () {
            jQuery(this).html(jQuery(this).html().split("\n").join("<br/>"));
        });
    },

    add_check_boxes_and_button : function () {
        var checks = ufal.controlpanel.all_checks_headers();
        if (checks.exists()) {
            checks.each(function (index) {
                if (!jQuery(this).text().endsWith(ufal.controlpanel.MAIN_CONTROL_CHECK)) {
                    jQuery(this).prepend(' <input type="checkbox" checked class="checkbox"/>');
                }
            });
            checks.each(function (index) {
                if (jQuery(this).text().indexOf(ufal.controlpanel.MAIN_CONTROL_CHECK) !== -1)
                {
                    jQuery(this).addClass('btn btn-link');
                    jQuery(this).toggle(function () {
                        var checkboxes = jQuery('.checkbox');
                        checkboxes.attr('checked', 'checked');
                    }, function () {
                        var checkboxes = jQuery('.checkbox');
                        checkboxes.removeAttr('checked');
                    });
                }
            });
        }
        var but = jQuery('div.run-checks');
        if (but.exists()) {
            var but_rc = but;
            but_rc.click(function () {
                ufal.controlpanel.start_checking();
                return false;
            });
        }
    },

    add_ip_link: function () {
        var link = 'http://www.infosniper.net/index.php?map_source=1&overview_map=1&lang=1&map_type=1&zoom_level=7&ip_address=';
        jQuery('#aspect_administrative_ControlPanel_table_users td:nth-child(3)').each(function (index) {
            var t = jQuery(this).text();
            jQuery(this).html('<a href="' + link + t + '" target="_blank">' + t + '</a>');
        });
    },
}; // namespace


//
//
//
jQuery(document).ready(function () {
    // Control panel by jm
    ufal.controlpanel.make_list_files_clickable();
    ufal.controlpanel.make_logins_hideable();

    // add checkboxes + button to run checks
    ufal.controlpanel.add_check_boxes_and_button();

    // current activity
    ufal.controlpanel.add_ip_link();
    // replace \n instead of br
    ufal.controlpanel.replace_br('.programs-result');
    ufal.controlpanel.replace_br('.replace_br');

    var valid_res_p = jQuery("#aspect_administrative_ControlPanel_div_oval-validation-result p");
    if (valid_res_p.exists()) {
        valid_res_p.html("<pre>" + valid_res_p.html() + "</pre>");
    }

    jQuery(".unpublished_items").attr("id", "aspect_submission_Submissions_div_submissions");
    jQuery(".itemlist").css("margin-bottom", "30px");

    jQuery(".toggle-onclick-parent-next4").each(function () {
        // toggle on click
        var how_many = 4;
        jQuery(this).click(function () {
            var o = jQuery(this).parent();
            for (var i = 0; i < how_many; ++i) {
                o = o.next();
                o.toggleClass('hidden');
            }
            return false;
        });
    });

}); // ready
