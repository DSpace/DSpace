/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

var href = window.location.href;
var params = href.split('/');
var url = '/handle/' + params[4] + '/' + params[5] + '/upload';

// get submission id
var sId = $('input[name="submit-id"]').val();

// number of bytes left in item for uploads
var itemSpace = parseInt($('input[name=item-space]').val());

var doResumable = false;
var r;

if(sId > 0){
    r = new Resumable({
        target: url,
        chunkSize: 1024 * 1024,
        simultaneousUploads: 1,
        testChunks: true,
        throttleProgressCallbacks: 1,

        // remove, for testing
        xhrTimeout: 60000,
        //xhrTimeout: 5000,

        maxChunkRetries: 5,
        //maxChunkRetries: 3,

        chunkRetryInterval: 5000,
        method: "multipart",
        query:{"submissionId": sId}
    });


    if(r.support){
        doResumable = true;
    }
}

if(doResumable){
    $.ajaxSetup({
        // Disable caching of AJAX responses
        cache: false
    });

    var html = '\
      <div id="dialog-confirm" style="display: none;" title="Delete File(s)?">\
        <p><span id="delete-icon" class="ui-icon ui-icon-alert" ></span><span id="delete-text">?</span></p>\
      </div>';

    $('#aspect_submission_StepTransformer_div_submit-file-upload').append(html);

    // file object currently being processed
    var currentFile;

    // place for dropping/selecting files
    var rdId = 'aspect_submission_StepTransformer_div_resumable-drop';
    $('#' + rdId + ' p:first').addClass('glyphicon glyphicon-upload');
    r.assignDrop($('#' + rdId)[0]);
    r.assignBrowse($('#' + rdId)[0]);

    // set up progress bar and button
    var themePath = window.DSpace.theme_path;
    $('#aspect_submission_StepTransformer_div_progress-button').append('<a id="progress-resume-link"><img title="Resume upload" /></a><a id="progress-pause-link"><img title="Pause upload" /></a>');
    $('#progress-resume-link img').attr('src', themePath + "images/resume.png");
    $('#progress-pause-link img').attr('src', themePath + "images/pause.png");
    $('#progress-resume-link, #progress-pause-link').hide();

    $('.file-primary').attr('title', $('input[name=text-primary-help]').val())
    $('.file-select').attr('title', $('input[name=text-select-help]').val())
    $('.file-info').attr('title', $('input[name=text-info-help]').val())

    $('#' + rdId).on('dragenter', function(e){
        $(this).addClass('resumable-dragover');
    });
    $('#' + rdId).on('dragleave', function(e){
        $(this).removeClass('resumable-dragover');
    });
    $('#' + rdId).on('dragdrop', function(e){
        $(this).removeClass('resumable-dragover');
    });

    $('#aspect_submission_StepTransformer_div_resumable-upload').after(
        '<div id="switch-upload" title="This can be used to switch upload interface"><a href="#">Switch Interface</a></div>'
    );

    if(localStorage.getItem('resumable') === '0'){
        $('#aspect_submission_StepTransformer_div_resumable-upload').hide();
    }
    else{
        // resumable is supported hide standard form
        $('#aspect_submission_StepTransformer_div_submit-upload').hide();
    }

    // Handle file add event
    r.on('fileAdded', function(file){
        currentFile = file;
        hideError();

        // check enough space left in item for upload
        if(r.getSize() > itemSpace){
            console.debug("Total upload size is " + r.getSize());
            console.debug("Space left on item is " + itemSpace);
            showError('no-space');
            r.cancel();
        }
        else{
            // Show pause, hide resume
            $('#progress-resume-link').hide();
            $('#progress-pause-link').show();

            // build HTML for dynamic table row
            var tableRow =
                '<tr class="ds-table-row">\
                   <td class="ds-table-cell">\
                     <div class="radio"><label><input id="primary-' +  file.uniqueIdentifier + '" type="radio" name="primary_bitstream_id" disabled="disabled"></label></div>\
                   </td>\
                   <td class="ds-table-cell">\
                   <div class="checkbox"><label><input id="select-' +  file.uniqueIdentifier + '" type="checkbox" name="select" disabled="disabled"></label></div>\
                   </td>\
                   <td class="ds-table-cell">\
                   <div id="file-name-' + file.uniqueIdentifier + '">' + file.fileName + '</div></td>\
                   <td class="ds-table-cell">\
                   <input id="file-description-' + file.uniqueIdentifier + '" class="ds-text-field form-control" type="text" value="" name="description" disabled="disabled"></input>\
</td>\
                   <td id="file-status-' +  file.uniqueIdentifier + '" class="file-uploading ds-table-cell"><div></div></td>\
                   <td id="file-info-'   +  file.uniqueIdentifier + '" class="ds-table-cell"></td>\
                   <td id="file-delete-' +  file.uniqueIdentifier + '" class="ds-table-cell"></td>\
                 </tr>';

            // add the file to the list
            $('#aspect_submission_StepTransformer_table_resumable-upload-summary tbody').append(tableRow);

            // start the upload
            r.upload();
        }
    });

    r.on('fileProgress', function(file){
        // Handle progress for both the file and the overall upload
        $('#file-status-' + file.uniqueIdentifier + ' div').html(Math.floor(file.progress()*100) + '%');
        $('#aspect_submission_StepTransformer_div_progress-bar').css({width:Math.floor(r.progress()*100) + '%'});
    });

    r.on('pause', function(){
        // Show resume, hide pause
        $('#progress-resume-link').show();
        $('#progress-pause-link').hide();

        $('#file-delete-' + currentFile.uniqueIdentifier).attr('class', 'file-delete');
    });

    r.on('complete', function(){
        var filesWithError = false;

        // check if there are any errors
        for(var i = 0; i < r.files.length; i++){
            if(r.files[i].retryDueToError){
                filesWithError = true;
                break;
            }
        }

        if(filesWithError){
            $('#progress-pause-link').hide();
            $('#progress-resume-link').show();
        }
        else{
            // Hide pause/resume when the upload has completed
            $('#progress-resume-link, #progress-pause-link').hide();
        }

        $('#aspect_submission_StepTransformer_div_progress-bar').css('width', '0%');
    });

    r.on('cancel', function(){
        $('#progress-resume-link, #progress-pause-link').hide();
        $('#aspect_submission_StepTransformer_div_progress-bar').css('width', '0%');
    });

    r.on('fileSuccess', function(file, message){
        var TIMEOUT = 300000; // 5 mins
        var INTERVAL = 3000;  // 3 seconds
        var attempts = 0;

        // last chunk has been up loaded fetch details of completed bistream,
        // for large files this can take sometime
        var intId = setInterval(function(){
            $.ajax({
                type: "GET",
                url: url + "?submissionId=" + sId + "&complete=true&resumableIdentifier=" + file.uniqueIdentifier,
                cache: false,
                success: function(data){
                    var xml = $.parseXML(data);

                    var bId = $($(xml).find("bitstreamId")[0]).text();
                    if(bId.length > 0){
                        clearInterval(intId);

                        var bytes = $($(xml).find("size")[0]).text();
                        var format = $($(xml).find("format")[0]).text();
                        var fullChecksum = $($(xml).find("checksum")[0]).text();
                        var sequenceId = $($(xml).find("sequenceId")[0]).text();
                        var checksum = fullChecksum.split(':');
                        var href = '/bitstream/item/' + sId + '/' + file.fileName + '?sequence=' + sequenceId;
                        var fAnchor = '<a href="' + href + '">' + file.fileName + '</a>';

                        $('#primary-'          + file.uniqueIdentifier).removeAttr('disabled');
                        $('#primary-'          + file.uniqueIdentifier).attr('value', bId);
                        $('#select-'           + file.uniqueIdentifier).removeAttr('disabled');
                        $('#select-'           + file.uniqueIdentifier).attr('value', bId);
                        $('#file-name-'        + file.uniqueIdentifier).empty();
                        $('#file-name-'        + file.uniqueIdentifier).append(fAnchor);
                        $('#file-description-' + file.uniqueIdentifier).removeAttr('disabled');
                        $('#file-description-' + file.uniqueIdentifier).attr('name', 'description-' + bId);
                        $('#file-status-'      + file.uniqueIdentifier).text('');
                        $('#file-status-'      + file.uniqueIdentifier).attr('class', 'file-status-success');
                        $('#file-info-'        + file.uniqueIdentifier).attr('class', 'file-info');
                        $('#file-delete-'      + file.uniqueIdentifier).attr('class', 'file-delete');

                        // give the successful upload a new id
                        $('#file-delete-' + file.uniqueIdentifier).parent().attr(
                            'id', 'aspect_submission_StepTransformer_row_bitstream-' + bId);

                        var extra = '<input name="file-extra-bytes-' + bId + '" type="hidden" value="' + bytes + '"></input>\
                                     <input name="file-extra-format" type="hidden" value="' + format + '"></input>\
                                     <input name="file-extra-algorithm" type="hidden" value="' + checksum[0] + '"></input>\
                                     <input name="file-extra-checksum" type="hidden" value="' + checksum[1] + '"></input>';
                        $('#file-info-' + file.uniqueIdentifier).append(extra);

                        itemSpace -= bytes;
                    }
                    else{
                        ++attempts;
                        if(attempts > (TIMEOUT / INTERVAL)){
                            console.warn("Creation of file timed out after " + TIMEOUT / 1000 + " seconds");
                            clearInterval(intId);
                            showError('create-failed');
                        }
                    }
                },
                error: function(jqXHR, status, error){
                    console.error(error);
                    clearInterval(intId);
                    $('#file-delete-' + file.uniqueIdentifier).attr('class', 'file-delete');
                    showError('upload-failed');
                }
            });

        }, INTERVAL);
    });

    r.on('fileError', function(file, message){
        // flag file to be retried
        file.retryDueToError = true;
        $('#file-delete-' + file.uniqueIdentifier).attr('class', 'file-delete');

        for(var i = 0; i < r.files.length; i++){
            if(!r.files[i].isComplete()){
                // stop any other file uploads
                // note: abort did not stop any remaining
                // files uploading hence bootstrap
                r.files[i].bootstrap();

                $('#file-delete-' + r.files[i].uniqueIdentifier).attr('class', 'file-delete');
            }
        }

        showError('upload-failed');
    });

    $(document).on('click', '.file-delete', function(e){
        var cell = e.target;
        var cellId = $(cell).attr('id');

        var fileCell = $(cell).siblings()[2]
        var infoCell = $(cell).siblings()[5];
        var fileName, param;
        var toBeDeleted = [];
        var text = $('input[name=text-delete-msg]').val() + " ";

        if($(infoCell).hasClass('file-info')){
            $.each($('tr input[name=select]:checked'), function(i, entry){
                toBeDeleted.push($(entry).val());
            });

            if(toBeDeleted.length === 0){
                // no files have been selected just delete row click on
                var bid = $(cell).parent().attr('id').split('-')[1];
                toBeDeleted.push(bid);

                text += $(fileCell).find('a').text() + "?";
            }
            else if(toBeDeleted.length === 1){
                // just check selected file is same as row delete click
                var bid = $(cell).parent().attr('id').split('-')[1];
                if(bid !== toBeDeleted[0]){
                    // there is a mismatch, make actual delete to be row click
                    toBeDeleted[0] = bid;
                    text = $('input[name=text-delete-unmatch]').val();
                }
                else{
                    text += $(fileCell).find('a').text() + "?";
                }
            }
            else{
                text += toBeDeleted.length + " " + $('input[name=text-delete-sf]').val() + "?";
            }
        }
        else{
            text += $(fileCell).find('div').text() + "?";
        }

        var deleteFile = function(param){
            var deferred = $.Deferred();

            $.ajax({
                type: "DELETE",
                url: url + "?submissionId=" + sId + "&" + param,
                cache: false,
                success: $.proxy(function(data){
                    deferred.resolve();
                }, this),
                error: function(jqXHR, status, error){
                    var error = "Problem deleting file with " + param + " : " +
                                  status + " : " + error
                    console.error(error);
                    deferred.reject(error);
                }
            });

            return deferred.promise();
        };

        var height = 180;
        var width = 500;
        $("#dialog-confirm").dialog({
            height: height,
            width: width,
            modal: true,
            autoOpen: false,
            buttons: {
                Cancel: function(){
                    $(this).dialog( "close" );
                },
                "Delete": function(ev) {
                    if(toBeDeleted.length > 0){
                        $.each(toBeDeleted, function(i, bId){
                            var promise = deleteFile("bitstreamId=" + bId);
                            promise.done(function(){
                                var bytes = $("input[name='file-extra-bytes-" + bId + "']").val();
                                itemSpace += parseInt(bytes);

                                var row = $('#aspect_submission_StepTransformer_row_bitstream-' + bId);
                                var next = row.next();
                                if(next.find('td').length === 1){
                                    // next element is an info row remove it too
                                    next.remove();
                                }
                                row.remove();
                            });
                            promise.fail(function(err) {
                                // ?
                            });
                        });
                    }
                    else{
                        // must be unfinished upload
                        var resumableIdentifier = cellId.split('file-delete-')[1];
                        var promise = deleteFile("resumableIdentifier=" + resumableIdentifier);

                        promise.done(function(){
                            $(cell).parent().remove();

                            // cancel upload with resumablejs
                            r.cancel();
                        });
                        promise.fail(function(err) {
                            // ?
                        });

                    }

                    $(this).dialog("close");
                }
            }
        });

        $('#delete-text').text(text);

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

        if($('#' + newRowId).is(":visible")){
            $('#' + newRowId).hide();
        }
        else{
            if($('#' + newRowId).length){
                $('#' + newRowId).show();
            }
            else{
                var bytes = $(cell).find("[name^='file-extra-bytes']").val();
                var format = $(cell).find("[name='file-extra-format']").val();
                var checksum = $(cell).find("[name='file-extra-algorithm']").val() +
                    ":" + $(cell).find("[name='file-extra-checksum']").val();

                var row = '<tr id="' + newRowId + '"><td colspan="7"><div>\
                             <strong>bytes:</strong> ' +  bytes + '&nbsp\
                             <strong>format:</strong> ' + format + '&nbsp;\
                             <strong>checksum:</strong> ' + checksum + '&nbsp;\
                           </div></td></tr>';
                $(parent).after(row);
            }
        }
    });

    $('#progress-resume-link').on('click', function(e){
        // Show pause, hide resume
        $('#progress-resume-link').hide();
        $('#progress-pause-link').show();

        $('#file-delete-' + currentFile.uniqueIdentifier).removeAttr('class');

        for(var i = 0; i < r.files.length; i++){
            // retry any previous failed
            if(r.files[i].retryDueToError){
                r.files[i].retry();
                r.files[i].retryDueToError = false;
                $('#file-delete-' + r.files[i].uniqueIdentifier).removeAttr('class');
            }
        }

        // remove any error message
        hideError();

        r.upload();
    });

    $('#progress-pause-link').on('click', function(e){
        r.pause();
    });

    $('#switch-upload').on('click', function(e){
        if($('#aspect_submission_StepTransformer_div_resumable-upload').is(':visible')){
            localStorage.setItem('resumable', 0);
        }
        else{
            localStorage.setItem('resumable', 1);
        }

        $('#aspect_submission_StepTransformer_div_resumable-upload').toggle();
        $('#aspect_submission_StepTransformer_div_submit-upload').toggle();
    });


    var showError = function(name){
        $('#aspect_submission_StepTransformer_p_' + name).removeClass('hide');
    };

    var hideError = function(){
        $('.alert').addClass('hide');
    };
}
else{
    // resumable not supported hide resumable form
    $('#aspect_submission_StepTransformer_div_resumable-upload').hide();
}
