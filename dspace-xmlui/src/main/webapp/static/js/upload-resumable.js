/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

$.ajaxSetup ({
    // Disable caching of AJAX responses
    //  ssss
    cache: false
});

var href = window.location.href;
var params = href.split('/');
console.log(params);


var r = new Resumable({
    target: '/handle/' + params[4] + '/' + params[5] + '/upload?' + params[7],
    chunkSize: 1024 * 1024,
    simultaneousUploads: 1,
    testChunks: true,
    throttleProgressCallbacks: 1,
    method: "multipart"
    //query:{workspace_item_id: id}
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
<div class="resumable-files">\
  <div class="panel panel-default">\
    <div class="panel-heading">Files To Upload</div>\
    <table class="table resumable-list">\
      <thead>\
      <th>#</th>\
      <th>Name</th>\
      <th>Status</th>\
      </thead>\
      <tbody></tbody>\
    </table>\
  </div>\
</div>';

if(r.support) {
    $('#aspect_submission_StepTransformer_div_submit-upload').append(html);

    // Show a place for dropping/selecting files
    $('.resumable-error').hide();
    $('#simple-upload').hide();
    $('.resumable-drop').show();
    $('#resumable-upload').show();

    r.assignDrop($('.resumable-drop')[0]);
    r.assignBrowse($('.resumable-browse')[0]);


    console.log(window.DSpace.theme_path);

    // Handle file add event
    r.on('fileAdded', function(file){
        // Show progress pabr
        $('.resumable-progress, .resumable-files, .resumable-list').show();
        // Show pause, hide resume
        $('#progress-resume-link').hide();
        $('#progress-pause-link').show();
        // Add the file to the list
        $('.resumable-list tbody')
            .append('<tr>')
            .append('<td class="resumable-file-'+file.uniqueIdentifier+'">Uploading</td>')
            .append('<td class="resumable-file-name"></td>')
            .append('<td class="resumable-file-progress"></td>')
            .append('</tr>');
        $('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name').html(file.fileName);
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
        // Reflect that the file upload has completed
        $('.resumable-file-'+file.uniqueIdentifier).html('');
        $('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-progress').html('<span class="glyphicon glyphicon-ok-sign"></span>');
    });

    r.on('fileError', function(file, message){
        // Reflect that the file upload has resulted in error
        $('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-progress').html('<span class="glyphicon glyphicon-exclamation-sign"></span>');
        r.removeFile(file);
        r.upload();
    });

    r.on('fileProgress', function(file){
        // Handle progress for both the file and the overall upload
        $('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-progress').html(Math.floor(file.progress()*100) + '%');
        $('.progress-bar').css({width:Math.floor(r.progress()*100) + '%'});
    });

    function resume() {
        // Show pause, hide resume
        $('#progress-resume-link').hide();
        $('#progress-pause-link').show();
        r.upload();
    }
}
