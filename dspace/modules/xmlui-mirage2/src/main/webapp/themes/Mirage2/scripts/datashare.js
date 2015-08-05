/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

//$.noConflict();

// create latest items on from dspace RSS feed
function createLatestItems(num) {
    if(num === 'undefined'){
        num = 5;
    }

    var div = $('#news_latest_items');

    var url = $($('#ds-feed-option li a')[2]).attr('href');
    var count = 1;

    $.get(url, function(data) {
            div.append('<h3>Latest Items</h3><div id="rss-feed-icon"><a href="/feed/rss_2.0/site"><img src="/static/icons/feed.png" alt="RSS Feed"></a></div>');
            var feed = $(data);
            var list = '<ul>';
            feed.find('entry').each(function(){
                var item = $(this);
                var title = item.find('title').text();
                var link = item.find('link').attr('href');
                var date = item.find('updated').text().substr(0, 10);
                list = list + '<li><a href=' + link + '>' + title + ' (' + date + ')</a></li>';
                if(count === num){
                    return false;
                }
                else {
                    ++count;
                }
            });

        div.append(list);
    });
};

function organiseMetadataGroups(){

    if($('.metadata-group-heading').length > 0){
        // only do this once
        return;
    }

    var map = {
        'Group 1': {
            'id': 'aspect_submission_StepTransformer_field_dc_identifier_govdoc',
            'text': 'Provide an abstract and attach keywords.',
            'items': ['aspect_submission_StepTransformer_field_dc_title_alternative',
                      'aspect_submission_StepTransformer_field_dc_description_abstract',
                      'aspect_submission_StepTransformer_field_dc_subject_classification',
                      'aspect_submission_StepTransformer_field_dc_subject']
        },
        'Group 2': {
            'id': 'aspect_submission_StepTransformer_field_dc_identifier_isbn',
            'text': 'Link to publications and other versions of data item.',
            'items': ['aspect_submission_StepTransformer_field_dc_relation_isversionof',
                      'aspect_submission_StepTransformer_field_dc_relation_isreferencedby',
                      'aspect_submission_StepTransformer_field_dc_relation_replaces',
                      'aspect_submission_StepTransformer_field_dc_relation_isreplacedby']
        },
        'Group 3': {
            'id': 'aspect_submission_StepTransformer_field_dc_identifier_ismn',
            'text': 'Acknowledge sources and describe file structure.',
            'items': ['aspect_submission_StepTransformer_field_dc_source',
                      'aspect_submission_StepTransformer_field_dc_date_embargo',
                      'aspect_submission_StepTransformer_field_dc_description_tableofcontents']
        },
        'Group 4': {
            'id': 'aspect_submission_StepTransformer_field_dc_identifier_issn',
            'text': 'Add geography, dates, and language information.',
            'items': ['aspect_submission_StepTransformer_field_dc_subject_ddc',
                      'aspect_submission_StepTransformer_field_dc_coverage_spatial',
                      'aspect_submission_StepTransformer_field_dc_language_iso',
                      'aspect_submission_StepTransformer_field_dc_coverage_temporal_end_year']
        }

    };

    $.each(map, function(i, group){
        var hide = true;

        console.log(i + " : " + group.id);

        var li = $('#' + group.id).closest('.ds-form-item');

        console.log(li);

        li.addClass('metadata-group');

        var label = li.find('label');

        label.attr('id', i);
        label.text(group.text);

        if(i === 'Group 1'){
            //li.before()
            li.before('<div class="ds-form-item metadata-group-heading"><p>Optimise the \'findability\' of your submission via search engines and link to published papers by adding supplementary information:</p></div>');
        }

        li.click(function(event){
            console.log("=>");
            // click on group
            event.preventDefault();

            var id;
            if($(event.target).get(0).tagName === 'LABEL'){
                id = $(event.target).attr('id');
            }
            else{
                id = $(event.target).find('label').attr('id');
            }

            $.each(map[id].items, function(i, id){
                //$('#' + id).closest('.ds-form-item').slideToggle();
                $('#' + id).closest('.ds-form-item').toggle();
            });

            if($('#' + map[id].items[0]).is(':visible')){
                li.removeClass('metadata-group-expand');
                li.addClass('metadata-group-collapse');
            }
            else{
                li.removeClass('metadata-group-collapse');
                li.addClass('metadata-group-expand');
            }
        });

        $.each(group.items, function(j, id){
            console.log(j + " : " + id);
            var ctrl = $('#' + id);
            var val = ctrl.val();

            if(typeof val != 'undefined'){
                val = $.trim(val);
                if(val.length > 0 ||
                   ctrl.parent().find('.ds-previous-values').length > 0){
                    hide = false;
                    return;
                }
            }
        });

        console.log(hide);

        if(hide){
            li.addClass('metadata-group-expand');

            $.each(group.items, function(j, id){
                $('#' + id).closest('.ds-form-item').hide();
            });
        }
        else{
            li.addClass('metadata-group-collapse');
        }
    });
};

function checkLicense(){
    var toggleRights = function(){
        var LICENSE_URL = "https://creativecommons.org/licenses/by/4.0/";
        var rights = $('#aspect_submission_StepTransformer_field_right-statement');
        var dropdown = $('#aspect_submission_StepTransformer_field_license-options');
        var dropdownHelp = $(dropdown.siblings()[0])

        if($('#aspect_submission_StepTransformer_field_license-options').val() === '5'){
            rights.closest('li').hide();
            dropdownHelp.html('<a href="' + LICENSE_URL + '" target="_blank">What does this open licence mean?</a>');
        }
        else{
            dropdownHelp.text('');
            rights.closest('li').show();
        }
    };

    $('#aspect_submission_StepTransformer_field_license-options').change(function(event){
        toggleRights();
    });

    toggleRights();
};

// hyperlink citation on simple item page
function hyperlinkCitation(){
    var citation = $('.ds-includeSet-table tr:nth-child(4) td:nth-child(2)');
    var text = citation.text().split('http://dx.doi.org');
    if(text.length > 1){
        var url = 'http://dx.doi.org' + text[1].substr(0, text[1].length - 1);
        var link = '<a href="' + url + '">' + url + '.</a>';
        citation.html(text[0] + link);
    }
};

$(document).ready(function(){
    if(window.location.hostname == 'dlib-oxgangs.ucs.ed.ac.uk' || window.location.hostname == 'devel.edina.ac.uk'){
        $('#section-header').css('border', 'red solid 2px');
    }

    // if($('#file_news_div_news').length > 0 ){
    //     createLatestItems(3);
    // }

    if($('#aspect_submission_StepTransformer_list_submit-describe').length > 0 ){
        organiseMetadataGroups();
    }

    if($('#aspect_submission_StepTransformer_list_submit-user-license').length > 0){
        checkLicense();
    }

    if($('#aspect_artifactbrowser_ItemViewer_div_item-view').length > 0){
        if(!document.URL.split('?')[1]){
            // simple item page
            hyperlinkCitation();
        }
    }
});
