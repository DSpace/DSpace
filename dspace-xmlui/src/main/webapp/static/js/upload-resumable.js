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
console.log("**");


// console.log($('input[name="submit-id"]'));
// console.log($('input[name="submit-id"]').val());
var sid = $('input[name="submit-id"]').val();
//console.log($('input[name="submit-id"]').value());

var r = new Resumable({
    target: '/handle/' + params[4] + '/' + params[5] + '/upload',
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
  <a class="resumable-browse"><fmt:message key="jsp.submit.choose-file.upload-resumable.button.select-file"/></a>\
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

/*
<div class="resumable-files">\
  <div class="panel panel-default">\
    <div class="panel-heading">Files To Upload</div>\
    <table class="table resumable-list">\
      <thead>\
      <th>Primary</th>\
      <th>File</th>\
      <th>Size</th>\
      <th>Description</th>\
      <th>Format</th>\
      <th>Status</th>\
      </thead>\
      <tbody></tbody>\
    </table>\
  </div>\
</div>';
*/



if(r.support) {
    $('#aspect_submission_StepTransformer_div_submit-file-upload').prepend(html);

    // Show a place for dropping/selecting files
    $('.resumable-error').hide();
    $('#simple-upload').hide();
    $('.resumable-drop').show();
    $('#resumable-upload').show();

    r.assignDrop($('.resumable-drop')[0]);
    r.assignBrowse($('.resumable-browse')[0]);

    console.log(window.DSpace.theme_path);
    var themePath = window.DSpace.theme_path;
    $('#progress-resume-link img').attr('src', themePath + "images/resume.png");
    $('#progress-pause-link img').attr('src', themePath + "images/pause.png");

    // Handle file add event
    r.on('fileAdded', function(file){
        // Show progress pabr
        //$('.resumable-progress, .resumable-files, .resumable-list').show();
        $('.resumable-progress').show();

        // Show pause, hide resume
        $('#progress-resume-link').hide();
        $('#progress-pause-link').show();

        console.log(file);
        var tableRow =
            '<tr class="ds-table-row">\
               <td class="ds-table-cell"></td>\
               <td class="ds-table-cell">' + file.fileName + '</td>\
               <td class="ds-table-cell">\
                 <input id="file-description-' + file.uniqueIdentifier + '" class="ds-text-field form-control" type="text" value="" name="description" disabled="disabled"></input>\
               </td>\
               <td id="file-status-' +  file.uniqueIdentifier + '" class="ds-table-cell"></td>\
               <td class="ds-table-cell"></td>\
              </tr>';

        // Add the file to the list
        $('#aspect_submission_StepTransformer_table_submit-upload-summary tbody').append(tableRow);

            // .append('')
            // .append('</tr>');
        //$('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name').html(file.fileName);

        // Actually start the upload
        r.upload();
    });

    r.on('pause', function(){
        // Show resume, hide pause
        $('#progress-resume-link').show();
        $('#progress-pause-link').hide();
    });

    r.on('complete', function(){
        // Hide pause/resume when the upload has completed
        $('#progress-resume-link, #progress-pause-link').hide();
    });

    r.on('fileSuccess', function(file,message){
        console.log(file);
        console.log(message);
        // Reflect that the file upload has completed
        //$('.resumable-file-'+file.uniqueIdentifier).html('');
        //$('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-progress').html('<span class="glyphicon glyphicon-ok-sign"></span>');


        var xml = $.parseXML(message);
        console.log(xml);
        var bId = $($(xml).find("bitstreamId")[0]).text();
        console.log(bId);

        $('#file-description-' + file.uniqueIdentifier).removeAttr('disabled');
        $('#file-description-' + file.uniqueIdentifier).attr('name', 'description-' + bId);


        // console.log(bId[0]);
        // console.log($(bId[0]).text());
    });

    r.on('fileError', function(file, message){
        // Reflect that the file upload has resulted in error
        $('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-progress').html('<span class="glyphicon glyphicon-exclamation-sign"></span>');
        r.removeFile(file);
        r.upload();
    });

    r.on('fileProgress', function(file){
        // Handle progress for both the file and the overall upload
        //$('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-progress').html(Math.floor(file.progress()*100) + '%');
        $('#file-status-'+file.uniqueIdentifier).html(Math.floor(file.progress()*100) + '%');

        $('.progress-bar').css({width:Math.floor(r.progress()*100) + '%'});
    });

    //$('header').hide();

    $('.file-delete').click(function(e){
        console.log("=>");
        console.log($(e.target).attr('id'));

        var height = 140;
        var width = 700;
        $("#dialog-confirm").dialog({
            height: height,
            width: width,
            modal: true,

            buttons: {
                Cancel: function() {
                    $( this ).dialog( "close" );
                },
                "Delete": function() {
                    $( this ).dialog( "close" );
                }
            }
        });


        // for some reason jquery dialog not centring, so do it manually
        var top = ($(window).height() / 2) - (height / 2);
        var left = ($(window).width() / 2) - (width / 2);
        $('.ui-dialog').css('top', top + 'px');
        $('.ui-dialog').css('left', left + 'px');
    });

    function resume() {
        // Show pause, hide resume
        $('#progress-resume-link').hide();
        $('#progress-pause-link').show();
        r.upload();
    }

}
