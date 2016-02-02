/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

$.ajaxSetup ({
    // Disable caching of AJAX responses
    cache: false
});

var href = window.location.href;
var params = href.split('/');
var url = '/handle/' + params[4] + '/' + params[5] + '/upload';
var sid = $('input[name="submit-id"]').val();

var r = new Resumable({
    target: url,
    chunkSize: 1024 * 1024,
    simultaneousUploads: 1,
    testChunks: true,
    throttleProgressCallbacks: 1,
    method: "multipart",
    query:{"submissionId": $('input[name="submit-id"]').val()}
});

var html = '\
<div class="resumable-error">\
  Resumable upload is unsupported in this browser/>\
</div>\
<div id="resumable-upload">\
<div class="resumable-drop col-md-12" ondragenter="jQuery(this).addClass(\'resumable-dragover\');" ondragend="jQuery(this).removeClass(\'resumable-dragover\');" ondrop="jQuery(this).removeClass(\'resumable-dragover\');">\
  <span class="glyphicon glyphicon-upload"></span>\
  <a class="resumable-browse">blah blah</a>\
</div>\
</div>\
<div class="resumable-progress">\
  <table>\
    <tr>\
      <td width="100%"><div class="progress-container"><div class="progress-bar"></div></div></td>\
      <td class="progress-text" nowrap="nowrap"></td>\
      <td class="progress-pause" nowrap="nowrap">\
        <a id="progress-resume-link" href="#" onclick="resume(); return(false);" ><img title="Resume upload" /></a>\
        <a id="progress-pause-link" href="#" onclick="r.pause(); return(false);" ><img title="Pause upload" /></a>\
      </td>\
    </tr>\
  </table>\
</div>\
<div id="dialog-confirm" style="display: none;" title="Delete File?">\
  <p><span class="ui-icon ui-icon-alert" style="float:left; margin:0 7px 20px 0;"></span>Are you sure you want to delete <span id="file-name"></span>?</p>\
</div>';

if(r.support) {
    // file object currently being processed
    var currentFile;

    $('#aspect_submission_StepTransformer_div_submit-file-upload').prepend(html);

    // Show a place for dropping/selecting files
    $('.resumable-error').hide();
    $('#simple-upload').hide();
    $('.resumable-drop').show();
    $('#resumable-upload').show();

    r.assignDrop($('.resumable-drop')[0]);
    r.assignBrowse($('.resumable-browse')[0]);

    var themePath = window.DSpace.theme_path;
    $('#progress-resume-link img').attr('src', themePath + "images/resume.png");
    $('#progress-pause-link img').attr('src', themePath + "images/pause.png");

    // Handle file add event
    r.on('fileAdded', function(file){
        currentFile = file;

        // Show progress pabr
        $('.resumable-progress').show();

        // Show pause, hide resume
        $('#progress-resume-link').hide();
        $('#progress-pause-link').show();

        var tableRow =
            '<tr class="ds-table-row">\
               <td class="ds-table-cell">\
                 <div class="radio"><label><input id="primary-' +  file.uniqueIdentifier + '" type="radio" name="primary_bitstream_id" disabled="disabled"></label></div>\
               </td>\
               <td class="ds-table-cell"><div>' + file.fileName + '</div></td>\
               <td class="ds-table-cell">\
                 <input id="file-description-' + file.uniqueIdentifier + '" class="ds-text-field form-control" type="text" value="" name="description" disabled="disabled"></input>\
               </td>\
               <td id="file-status-' +  file.uniqueIdentifier + '" class="ds-table-cell"></td>\
               <td id="file-info-'   +  file.uniqueIdentifier + '" class="ds-table-cell"></td>\
               <td id="file-delete-' +  file.uniqueIdentifier + '" class="ds-table-cell"></td>\
             </tr>';

        // add the file to the list
        $('#aspect_submission_StepTransformer_table_submit-upload-summary tbody').append(tableRow);

        // start the upload
        r.upload();
    });

    r.on('pause', function(){
        // Show resume, hide pause
        $('#progress-resume-link').show();
        $('#progress-pause-link').hide();

        console.log(arg);
        $('#file-delete-' + currentFile.uniqueIdentifier).attr('class', 'file-delete');
    });

    r.on('complete', function(){
        // Hide pause/resume when the upload has completed
        $('#progress-resume-link, #progress-pause-link').hide();
    });

    r.on('fileSuccess', function(file, message){
        var xml = $.parseXML(message);
        var bId = $($(xml).find("bitstreamId")[0]).text();
        var bytes = $($(xml).find("size")[0]).text();
        var format = $($(xml).find("format")[0]).text();
        var fullChecksum = $($(xml).find("checksum")[0]).text();
        var checksum = fullChecksum.split(':');

        $('#file-description-' + file.uniqueIdentifier).removeAttr('disabled');
        $('#file-description-' + file.uniqueIdentifier).attr('name', 'description-' + bId);
        $('#primary-'          + file.uniqueIdentifier).removeAttr('disabled');
        $('#primary-'          + file.uniqueIdentifier).attr('value', bId);
        $('#file-status-'      + file.uniqueIdentifier).text('');
        $('#file-status-'      + file.uniqueIdentifier).attr('class', 'file-status-success');
        $('#file-info-'        + file.uniqueIdentifier).attr('class', 'file-info');
        $('#file-delete-'      + file.uniqueIdentifier).attr('class', 'file-delete');
        $('#file-delete-'      + file.uniqueIdentifier).parent().attr('id', 'aspect_submission_StepTransformer_cell_delete-' + bId);

        var extra = '<input name="file-extra-bytes" type="hidden" value="' + bytes + '"></input>\
                     <input name="file-extra-format" type="hidden" value="' + format + '"></input>\
                     <input name="file-extra-algorithm" type="hidden" value="' + checksum[0] + '"></input>\
                     <input name="file-extra-checksum" type="hidden" value="' + checksum[1] + '"></input>';
        $('#file-info-' + file.uniqueIdentifier).append(extra);
    });

    r.on('fileError', function(file, message){
        // Reflect that the file upload has resulted in error
        $('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-progress').html('<span class="glyphicon glyphicon-exclamation-sign"></span>');
        r.removeFile(file);
        r.upload();
    });

    r.on('fileProgress', function(file){
        // Handle progress for both the file and the overall upload
        $('#file-status-'+file.uniqueIdentifier).html(Math.floor(file.progress()*100) + '%');
        $('.progress-bar').css({width:Math.floor(r.progress()*100) + '%'});
    });

    $(document).on('click', '.file-delete', function(e){
        var cell = e.target;
        var cellId = $(cell).attr('id');

        var fileCell = $(cell).siblings()[1]
        var infoCell = $(cell).siblings()[4];
        var fileName, param;

        if($(infoCell).hasClass('file-info')){
            // completed upload
            fileName = $(fileCell).find('a').text();
            var bid = cellId.split('-')[1];
            param = "bitstreamId=" + bid;
        }
        else{
            // unfinished
            fileName = $(fileCell).find('div').text();

            var resumableIdentifier = cellId.split('file-delete-')[1];
            param = "resumableIdentifier=" + resumableIdentifier;
        }

        console.log(fileName);
        console.log(param);


        var height = 140;
        var width = 700;
        $("#dialog-confirm").dialog({
            height: height,
            width: width,
            modal: true,
            autoOpen: false,
            buttons: {
                Cancel: function(){
                    $( this ).dialog( "close" );
                },
                "Delete": function(ev) {


                    $.ajax({
                        type: "DELETE",
                        url: url + "?submissionId=" + sid + "&" + param,
                        cache: false,
                        success: $.proxy(function(data){
                            console.log(url)
                            console.log($(cell).parent());
                            //$(row).hide();
                            $(cell).parent().hide();

                            $(this).dialog("close");

                            r.cancel();
                        }, this),
                        error: function(jqXHR, status, error){
                            console.error("Problem deleting " + fileName + " : " +
                                          status + " : " + error);

                        }
                    });

                }
            }
        });
        $('#file-name').text(fileName);

        // for some reason jquery dialog not centring, so do it manually
        var top = $(document).scrollTop() + ($(window).height() / 2) - (height / 2);
        var left = $(document).scrollLeft() + ($(window).width() / 2) - (width / 2);
        $('.ui-dialog').css('top', top + 'px');
        $('.ui-dialog').css('left', left + 'px');

        $("#dialog-confirm").dialog("open");
    });


    $(document).on('click', '.file-info', function(e){
        var cell = e.target;
        var parent = $(cell).parent();
        var cellId = $(cell).attr('id');
        var newRowId = $(parent).attr('id') + '-extra';
        console.log(newRowId);

        console.log($('#' + newRowId).is(":visible"));

        if($('#' + newRowId).is(":visible")){
            $('#' + newRowId).hide();
        }
        else{
            if($('#' + newRowId).length){
                $('#' + newRowId).show();
            }
            else{
                var bytes = $(cell).find("[name=\'file-extra-bytes']").val();
                var format = $(cell).find("[name='file-extra-format']").val();
                var checksum = $(cell).find("[name='file-extra-algorithm']").val() +
                    ":" + $(cell).find("[name='file-extra-checksum']").val();

                var row = '<tr id="' + newRowId + '"><td colspan="6"><div>\
                             <strong>bytes:</strong> ' +  bytes + '&nbsp\
                             <strong>format:</strong> ' + format + '&nbsp;\
                             <strong>checksum:</strong> ' + checksum + '&nbsp;\
                           </div></td></tr>';
                console.log($(cell).find("[name='file-extra-bytes']").val());
                console.log(cell);
                $(parent).after(row);
            }
        }
    });

    function resume() {
        // Show pause, hide resume
        $('#progress-resume-link').hide();
        $('#progress-pause-link').show();

        $('#file-delete-' + currentFile.uniqueIdentifier).removeAttr('class');

        r.upload();
    }

}
