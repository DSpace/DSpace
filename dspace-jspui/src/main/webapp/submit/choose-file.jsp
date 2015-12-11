<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt"
    prefix="fmt" %>

 <%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
    
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>

<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>

<%@ page import="java.util.List" %>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<%@ page import="org.dspace.core.Context" %>
<%@ page import="org.dspace.app.webui.servlet.SubmissionController" %>
<%@ page import="org.dspace.authorize.ResourcePolicy" %>
<%@ page import="org.dspace.submit.AbstractProcessingStep" %>
<%@ page import="org.dspace.submit.step.UploadStep" %>
<%@ page import="org.dspace.app.util.DCInputSet" %>
<%@ page import="org.dspace.app.util.DCInputsReader" %>
<%@ page import="org.dspace.app.util.SubmissionInfo" %>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@ page import="org.dspace.content.service.ItemService" %>
<%@ page import="org.dspace.content.factory.ContentServiceFactory" %>


<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);
    request.setAttribute("submission.info", subInfo);

    boolean withEmbargo = ((Boolean)request.getAttribute("with_embargo")).booleanValue();

    // Determine whether a file is REQUIRED to be uploaded (default to true)
    boolean fileRequired = ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);
    boolean ajaxProgress = ConfigurationManager.getBooleanProperty("webui.submit.upload.ajax", true);
    boolean html5Upload = ConfigurationManager.getBooleanProperty("webui.submit.upload.html5", true);

 	Boolean sherpa = (Boolean) request.getAttribute("sherpa");
    boolean bSherpa = sherpa != null?sherpa:false;
%>


<% if (ajaxProgress || bSherpa || html5Upload) { %>
    <c:set var="dspace.layout.head.last" scope="request">

        <% if (bSherpa) { %>
            <link rel="stylesheet" href="<%=request.getContextPath()%>/sherpa/css/sherpa.css" type="text/css" />
            <script type="text/javascript">
                jQuery(document).ready(function(html){
                    jQuery.ajax({
                        url: '<%= request.getContextPath() + "/tools/sherpaPolicy" %>', 
                        data: {item_id: <%= subInfo.getSubmissionItem().getItem().getID() %>}})
                            .done(function(html) {
                                jQuery('#sherpaContent').html(html);
                    });
                });
            </script>
        <% } %>

        <% if (ajaxProgress) { %>
            <link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/jquery.fileupload-ui.css">
            <!-- CSS adjustments for browsers with JavaScript disabled -->
            <noscript><link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/jquery.fileupload-ui-noscript.css"></noscript>
            <script type="text/javascript">
                var bootstrapButton = $.fn.button.noConflict(); // return $.fn.button to previously assigned value
                $.fn.bootstrapBtn = bootstrapButton;            // give $().bootstrapBtn the Bootstrap functionality

            function initProgressBar($){
                var progressbarArea = $("#progressBarArea");
                progressbarArea.show();
            }

            function updateProgressBar($, data){
                $('#uploadForm').find('input').attr('disabled','disabled');
                $('#spanFile').attr('disabled','disabled');
                $('#spanFileCancel').attr('disabled','disabled');
                var percent = parseInt(data.loaded / data.total * 100, 10);
                var progressbarArea = $("#progressBarArea");
                var progressbar = $("#progressBar");
                progressbar.progressbar({ value: data.loaded, max: data.total});
                progressbarArea.find('p.progressBarInitMsg').hide();
                progressbarArea.find('p.progressBarProgressMsg').show();
                progressbarArea.find('p.progressBarCompleteMsg').hide();
                progressbarArea.find('span.bytesRead').html(data.loaded);
                progressbarArea.find('span.bytesTotal').html(data.total);
                progressbarArea.find('span.percent').html(percent);
            }

            function completeProgressBar($, total){
                var progressbarArea = $("#progressBarArea");
                var progressbar = $("#progressBar");
                progressbar.progressbar({ value: total, max: total});
                progressbarArea.find('p.progressBarInitMsg').hide();
                progressbarArea.find('p.progressBarProgressMsg').hide();
                progressbarArea.find('p.progressBarCompleteMsg').show();
                progressbarArea.find('span.bytesTotal').html(total);
            }

            function monitorProgressJSON($){
                $.ajax({
                    cache: false,
                    url: '<%= request.getContextPath() %>/json/uploadProgress'})
                .done(function(progress) {
                    var data = {loaded: progress.readBytes, total: progress.totalBytes};
                    updateProgressBar($, data);
                    setTimeout(function() {										
                        monitorProgressJSON($);					
                    }, 250);
                });					
            }

            function decorateFileInputChangeEvent($) {
                if ($('#selectedFile').length > 0) {
                    $('#selectedFile').html($('#tfile').val().replace(/.*(\/|\\)/, '')).append('&nbsp;');
                }
                else {
                    $('<span id="selectedFile">&nbsp;'+$('#tfile').val().replace(/.*(\/|\\)/, '')+'</span>').insertAfter($('#spanFile')).append('&nbsp;');
                    var span = $('<span id="spanFileCancel" class="btn btn-danger"><span class="glyphicon glyphicon-ban-circle"></span></span>');
                    span.appendTo($('#selectedFile'));
                    span.click(function(e){
                            var parent = $('#spanFile').parent();
                            $('#spanFile').remove();
                            $('#selectedFile').remove();
                            $('<input type="file" name="file" id="tfile">').appendTo(parent);
                            $('#tfile').wrap('<span id="spanFile" class="fileinput-button btn btn-success col-md-2"></span>');
                            $('#spanFile').prepend('&nbsp;&nbsp;<fmt:message key="jsp.submit.choose-file.upload-ajax.button.select-file"/>');
                            $('#spanFile').prepend('<span class="glyphicon glyphicon-folder-open"></span>');
                            $('#tfile').on('change', function(){
                                 decorateFileInputChangeEvent($);
                            });
                    });
                }
            }    

            function setupAjaxUpload($, data){
                var progressbarArea = $("#progressBarArea");
                var progressbar = $("#progressBar");
                progressbar.progressbar({ value: false});
                progressbarArea.find('p.progressBarInitMsg').show();
                progressbarArea.find('p.progressBarProgressMsg').hide();
                progressbarArea.find('p.progressBarCompleteMsg').hide();
                progressbarArea.hide();

                $('#tfile').wrap('<span id="spanFile" class="fileinput-button btn btn-success col-md-2"></span>');
                $('#spanFile').prepend('&nbsp;&nbsp;<fmt:message key="jsp.submit.choose-file.upload-ajax.button.select-file"/>');
                $('#spanFile').prepend('<span class="glyphicon glyphicon-folder-open"></span>');
                $('#tfile').on('change', function(){
                    decorateFileInputChangeEvent($);
                });

                // the skip button should not send any files
                $('input[name="<%=UploadStep.SUBMIT_SKIP_BUTTON%>"]').on('click', function(){
                    $('#tfile').val('');
                });
                $('#uploadForm').append('<input type="hidden" id="ajaxUpload" name="ajaxUpload" value="true" />');
                // track the upload progress for all the submit buttons other than the skip
                $('input[type="submit"]').not(":disabled")
                .on('click', function(e){   			
                    if ($('#tfile').val() != null && $('#tfile').val() != '') {
                        $('#uploadForm').attr('target','uploadFormIFrame');
                        initProgressBar($);
                        setTimeout(function() {
                            monitorProgressJSON($);					
                        }, 100);
                    }
                    else
                    {
                        $('#ajaxUpload').val(false);
                    }
                    $('#uploadFormIFrame').on('load',function(){
                        var resultFile = null;
                        try {
                            var jsonResult = $.parseJSON($('#uploadFormIFrame').contents().find('body').text());
                            if (jsonResult.fileSizeLimitExceeded) {
                                $('#actualSize').html(jsonResult.fileSizeLimitExceeded.actualSize);
                                $('#limitSize').html(jsonResult.fileSizeLimitExceeded.permittedSize);
                                $('#fileSizeLimitExceeded').dialog("open");
                                return true;
                            }
                            resultFile = jsonResult.files[0];
                        } catch (err) {
                            /*
                             * A file has been uploaded, the return answer is HTML instead of JSON because it comes from
                             * a different step. Just ignore the target step and reload the upload list screen we
                             * must let the user know that the file has been uploaded.
                             */
                            resultFile = new Object();
                            resultFile.status = null;
                        }

                        if (resultFile.status == null || resultFile.status == <%= UploadStep.STATUS_COMPLETE %> || 
                                resultFile.status == <%= UploadStep.STATUS_UNKNOWN_FORMAT %>)
                        {
                            completeProgressBar($, resultFile.size);
                            if (resultFile.status == null || 
                                    resultFile.status == <%= UploadStep.STATUS_COMPLETE %>)
                            {
                                $('#uploadFormPostAjax').removeAttr('enctype')
                                    .append('<input type="hidden" name="<%= UploadStep.SUBMIT_UPLOAD_BUTTON %>" value="1">');
                            }
                            else
                            {
                                $('#uploadFormPostAjax')
                                    .append('<input type="hidden" name="submit_format_'+resultFile.bitstreamID+'" value="1">')
                                    .append('<input type="hidden" name="bitstream_id" value="'+resultFile.bitstreamID+'">');
                            }

                            $('#uploadFormPostAjax').submit();	
                        }
                        else {
                            if (resultFile.status == <%= UploadStep.STATUS_NO_FILES_ERROR %>) {
                                $('#fileRequired').dialog("open");
                            }
                            else if (resultFile.status == <%= UploadStep.STATUS_VIRUS_CHECKER_UNAVAILABLE %>) {
                                completeProgressBar($, resultFile.size);
                                $('#virusCheckNA').dialog("open");
                            }
                            else if (resultFile.status == <%= UploadStep.STATUS_CONTAINS_VIRUS %>) {
                                completeProgressBar($, resultFile.size);
                                $('#virusFound').dialog("open");				
                            }
                            else {
                                $('#uploadError').dialog("open");
                            }
                        }    		
                        });
                });   		
            }


            jQuery(document).ready(function($){
                setupAjaxUpload($);

                $('#uploadError').dialog({modal: true, autoOpen: false, width: 600, buttons: {
                    '<fmt:message key="jsp.submit.choose-file.upload-ajax.dialog.close"/>': function() {
                        $(this).dialog("close");
                        $('#uploadFormPostAjax')
                            .append('<input type="hidden" name="<%= UploadStep.SUBMIT_MORE_BUTTON %>" value="1">');
                        $('#uploadFormPostAjax').submit();
                }
                }});

                $('#fileRequired').dialog({modal: true, autoOpen: false, width: 600, buttons: {
                    '<fmt:message key="jsp.submit.choose-file.upload-ajax.dialog.close"/>': function() {
                        $(this).dialog("close");
                        $('#uploadFormPostAjax')
                            .append('<input type="hidden" name="<%= UploadStep.SUBMIT_MORE_BUTTON %>" value="1">');
                        $('#uploadFormPostAjax').submit();
                }
                }});

                $('#fileSizeLimitExceeded').dialog({modal: true, autoOpen: false, width: 600, buttons: {
                    '<fmt:message key="jsp.submit.choose-file.upload-ajax.dialog.close"/>': function() {
                        $(this).dialog("close");
                        $('#uploadFormPostAjax')
                            .append('<input type="hidden" name="<%= UploadStep.SUBMIT_MORE_BUTTON %>" value="1">');
                        $('#uploadFormPostAjax').submit();
                }
                }});

                $('#virusFound').dialog({modal: true, autoOpen: false, width: 600, buttons: {
                    '<fmt:message key="jsp.submit.choose-file.upload-ajax.dialog.close"/>': function() {
                        $('#uploadFormPostAjax')
                            .append('<input type="hidden" name="<%= UploadStep.SUBMIT_MORE_BUTTON %>" value="1">');
                        $('#uploadFormPostAjax').submit();
                        $(this).dialog("close");
                }
                }});

                $('#virusCheckNA').dialog({modal: true, autoOpen:false, width: 600, buttons: {
                    '<fmt:message key="jsp.submit.choose-file.upload-ajax.dialog.close"/>': function() {
                        $('#uploadFormPostAjax')
                            .append('<input type="hidden" name="<%= UploadStep.SUBMIT_MORE_BUTTON %>" value="1">');
                        $('#uploadFormPostAjax').submit();
                        $(this).dialog("close");
                    }
                }});
            });
            </script>
        <% } %>

        <% if (html5Upload) { %>
            <link rel="stylesheet" href="<%=request.getContextPath()%>/static/css/resumable-upload.css" type="text/css" />
            <script src="<%=request.getContextPath()%>/static/js/resumable.js"></script>
        <% } %>

    </c:set>
<% } %>

<dspace:layout style="submission"
			   locbar="off"
               navbar="off"
               titlekey="jsp.submit.choose-file.title"
               nocache="true">
    
    <% if (ajaxProgress) { %>
        <div style="display:none;" id="uploadError" title="<fmt:message key="jsp.submit.upload-error.title" />">
            <p>
                <fmt:message key="jsp.submit.upload-error.info" />
            </p>
        </div>
        <div style="display:none;" id="fileRequired" title="<fmt:message key="jsp.submit.choose-file.upload-ajax.fileRequired.title" />">
            <p>
                <fmt:message key="jsp.submit.choose-file.upload-ajax.fileRequired.info" />
            </p>
        </div>
        <div style="display:none;" id="fileSizeLimitExceeded" title="<fmt:message key="jsp.error.exceeded-size.title" />">
            <p>
                <fmt:message key="jsp.error.exceeded-size.text1">
                    <fmt:param><span id="actualSize">&nbsp;</span></fmt:param>
                    <fmt:param><span id="limitSize">&nbsp;</span></fmt:param>
                </fmt:message>
            </p>
        </div>
        <div style="display:none;" id="virusFound" title="<fmt:message key="jsp.submit.upload-error.title" />">
            <p>
                <fmt:message key="jsp.submit.virus-error.info" />
            </p>
        </div>
        <div style="display:none;" id="virusCheckNA" title="<fmt:message key="jsp.submit.upload-error.title" />">
            <p>
                <fmt:message key="jsp.submit.virus-checker-error.info" />
            </p>
        </div>
        <form style="display:none;" id="uploadFormPostAjax" method="post" action="<%= request.getContextPath() %>/submit" 
            enctype="multipart/form-data" onkeydown="return disableEnterKey(event);">
            <%= SubmissionController.getSubmissionParameters(context, request) %>    
        </form>
        <iframe id="uploadFormIFrame" name="uploadFormIFrame" style="display: none"> </iframe>
    <% } %>
    
    <form id="uploadForm" <%= bSherpa?"class=\"sherpa col-md-8\"":"" %> method="post" 
    	action="<%= request.getContextPath() %>/submit" enctype="multipart/form-data" 
    	onkeydown="return disableEnterKey(event);">

		<jsp:include page="/submit/progressbar.jsp"/>
		
		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

        <%-- <h1>Submit: Upload a File</h1> --%>
		<h1>
            <fmt:message key="jsp.submit.choose-file.heading"/>
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#upload\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup>
        </h1>
    
        <%-- <p>Please enter the name of
        <%= (si.submission.hasMultipleFiles() ? "one of the files" : "the file" ) %> on your
        local hard drive corresponding to your item.  If you click "Browse...", a
        new window will appear in which you can locate and select the file on your
        local hard drive.</p> --%>

		<p>
            <fmt:message key="jsp.submit.choose-file.info1"/>
        </p>
        
        <%-- FIXME: Collection-specific stuff should go here? --%>
        <%-- <p class="submitFormHelp">Please also note that the DSpace system is
        able to preserve the content of certain types of files better than other
        types.
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\")%>">Information about file types</dspace:popup> and levels of
        support for each are available.</p> --%>
        
		<div class="submitFormHelp">
            <fmt:message key="jsp.submit.choose-file.info6"/>
            <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\")%>"><fmt:message key="jsp.submit.choose-file.info7"/></dspace:popup>
        </div>
		<br/>
        
        <div class="row container">
    		<div class="row">
                    <%-- Document File: --%>
                    <div class="simple-upload">
                        <label class="col-md-<%= bSherpa?"3":"2" %>" for="tfile"><fmt:message key="jsp.submit.choose-file.document"/></label>
                        <input type="file" size="40" name="file" id="tfile" />
                    </div>
            </div>
            <br/>
            
            
            <% if (html5Upload) {%>
                <div class="resumable-error">
                    <fmt:message key="jsp.submit.choose-file.upload-resumable.unsupported"/>
                </div>
                <div id="resumable-upload">
                      <div class="resumable-drop col-md-12" ondragenter="jQuery(this).addClass('resumable-dragover');" ondragend="jQuery(this).removeClass('resumable-dragover');" ondrop="jQuery(this).removeClass('resumable-dragover');">
                          <span class="glyphicon glyphicon-upload"></span>
                          <a class="resumable-browse"><fmt:message key="jsp.submit.choose-file.upload-resumable.button.select-file"/></a>
                      </div>
                </div>
            
                <div class="resumable-progress">
                    <table>
                        <tr>
                            <td width="100%">
                                <div class="progress-container">
                                    <div class="progress-bar"></div>
                                </div>
                            </td>
                            <td class="progress-text" nowrap="nowrap"></td>
                            <td class="progress-pause" nowrap="nowrap">
                                <a href="#" onclick="resume(); return(false);" class="progress-resume-link"><img src="image/submit/resume.png" title="Resume upload" /></a>
                                <a href="#" onclick="r.pause(); return(false);" class="progress-pause-link"><img src="image/submit/pause.png" title="Pause upload" /></a>
                            </td>
                        </tr>
                    </table>
                </div>
      
                <div class="resumable-files">
                    <div class="panel panel-default">
                        <div class="panel-heading">
                            Files To Upload
                        </div>

                        <table class="table resumable-list">
                            <thead>
                                <th>#</th>
                                <th>Name</th>
                                <th class="text-center">Status</th>
                                <th>Description</th>
                            </thead>
                            <tbody></tbody>
                        </table>
                    </div>
                </div>
                <script>
                    $(document).ready(function(){
                        var r = new Resumable({
                            target:'submit',
                            chunkSize:1024*1024,
                            simultaneousUploads:1,
                            testChunks: true,
                            throttleProgressCallbacks:1,
                            method: "multipart",
                            <%
                            if (subInfo.isInWorkflow())
                            {
                            %>
                                query:{workflow_id:'<%= subInfo.getSubmissionItem().getID()%>'}
                            <%
                            } else {
                            %>
                                query:{workspace_item_id:'<%= subInfo.getSubmissionItem().getID()%>'}
                            <%}%>
                        });
                        // Resumable.js isn't supported, fall back on a different method

                        if(r.support) {
                            // Show a place for dropping/selecting files
                            $('.resumable-error').hide();
                            $('.simple-upload').hide();
                            $('.resumable-drop').show();
                            $('#resumable-upload').show(); // done
                            r.assignDrop($('.resumable-drop')[0]);
                            r.assignBrowse($('.resumable-browse')[0]);

                            // Handle file add event
                            r.on('fileAdded', function(file){
                                // Show progress pabr
                                $('.resumable-progress, .resumable-files, .resumable-list').show();
                                // Show pause, hide resume
                                $('.resumable-progress .progress-resume-link').hide();
                                $('.resumable-progress .progress-pause-link').show();
                                // Add the file to the list
                                $('.resumable-list tbody')
                                    .append(
                                        '<tr>' +
                                        '<td class="resumable-file-'+file.uniqueIdentifier+'">Uploading</td>' +
                                        '<td class="resumable-file-name"></td>' +
                                        '<td class="resumable-file-progress text-center"></td>' +
                                        '<td class="resumable-file-description-'+file.uniqueIdentifier+'"><input class="form-control" type="text" name="description[' + file.fileName + ']" id="tdescription"></td>' +
                                        '</tr>'
                                    );
                                $('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name').html(file.fileName);
                                // Actually start the upload
                                r.upload();
                            });
                                r.on('pause', function(){
                                // Show resume, hide pause
                                $('.resumable-progress .progress-resume-link').show();
                                $('.resumable-progress .progress-pause-link').hide();
                            });
                            r.on('complete', function(){
                                // Hide pause/resume when the upload has completed
                                $('.resumable-progress .progress-resume-link, .resumable-progress .progress-pause-link').hide();
                            });
                            r.on('fileSuccess', function(file,message){
                                // Reflect that the file upload has completed
                                $('.resumable-file-'+file.uniqueIdentifier).html('');
                                //$('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-description + .resumable-file-progress').html('<span class="glyphicon glyphicon-ok-sign"></span>');
                                $('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-progress').html('<span class="glyphicon glyphicon-ok-sign"></span>');
                            });
                            r.on('fileError', function(file, message){
                                // Reflect that the file upload has resulted in error
                                $('.resumable-file-'+file.uniqueIdentifier+' + .resumable-file-name + .resumable-file-progress').html('<span class="glyphicon glyphicon-exclamation-sign"></span>');              
                                //'+message+')');
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
                                $('.resumable-progress .progress-resume-link').hide();
                                $('.resumable-progress .progress-pause-link').show();
                                r.upload();
                            }
                        }
                    });
                </script>
            <% } %>
            
            <% if (ajaxProgress) { %>
                <div id="progressBarArea" class="row">
                    <div id="progressBar"></div>
                    <p class="progressBarInitMsg">
                        <fmt:message key="jsp.submit.choose-file.upload-ajax.uploadInit"/>
                    </p>
                    <p class="progressBarProgressMsg" style="display: none;">
                        <fmt:message key="jsp.submit.choose-file.upload-ajax.uploadInProgress">
                                <fmt:param><span class="percent">&nbsp;</span></fmt:param>
                                <fmt:param><span class="bytesRead">&nbsp;</span></fmt:param>
                                <fmt:param><span class="bytesTotal">&nbsp;</span></fmt:param>
                        </fmt:message>
                    </p>
                    <p class="progressBarCompleteMsg" style="display: none;">
                        <fmt:message key="jsp.submit.choose-file.upload-ajax.uploadCompleted">
                                <fmt:param><span class="bytesTotal">&nbsp;</span></fmt:param>
                        </fmt:message>
                    </p>
                </div>
                <br/>
            <% } %>

            <% if (subInfo.getSubmissionItem().hasMultipleFiles()) { %>
                <%-- Please give a brief description of the contents of this file, for
                example "Main article", or "Experiment data readings." --%>
                <div class="help-block simple-upload"><fmt:message key="jsp.submit.choose-file.info9"/></div>
                <%-- <td class="submitFormLabel">File Description:</td> --%>
                <div class="row simple-upload">
					<label for="tdescription" class="col-md-<%= bSherpa?"3":"2" %>"><fmt:message key="jsp.submit.choose-file.filedescr"/></label>
                	<span class="col-md-<%= bSherpa?"9":"10" %> row"><input class="form-control" type="text" name="description" id="tdescription" size="40"/></span>
                </div>
            <% } %>

            <% if (withEmbargo) { %>
                <br/>
                <dspace:access-setting subInfo="<%= subInfo %>" dso="<%= subInfo.getSubmissionItem().getItem() %>" hidden="true" />
                <br/>
            <% } %>
        </div>
	
        <br/>
        
		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
        <%
            ItemService itemService = ContentServiceFactory.getInstance().getItemService();
            int col = 0; 
            if(!SubmissionController.isFirstStep(request, subInfo))
            {
                    col++;
            }
            if (!fileRequired || itemService.hasUploadedFiles(subInfo.getSubmissionItem().getItem()))
            {
                col++;
            }
            %>

            <div class="pull-right btn-group col-md-<%= (bSherpa?2:1) * col*2 + 4 %>">
               	<%  //if not first step, show "Previous" button
					if(!SubmissionController.isFirstStep(request, subInfo))
					{ %>
                        <input class="btn btn-default col-md-<%= 12 / (col + 2) %>" type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
				<%  } %>
                        <input class="btn btn-default col-md-<%= 12 / (col + 2) %>" type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                    <%
                        //if upload is set to optional, or user returned to this page after pressing "Add Another File" button
                    	if (!fileRequired || itemService.hasUploadedFiles(subInfo.getSubmissionItem().getItem()))
                        {
                    %>
                                <input class="btn btn-warning col-md-<%= 12 / (col + 2) %>" type="submit" name="<%=UploadStep.SUBMIT_SKIP_BUTTON%>" value="<fmt:message key="jsp.submit.choose-file.skip"/>" />
                    <%
                        }
                    %>   
                        <input class="btn btn-primary col-md-<%= 12 / (col + 2) %>" type="submit" name="<%=UploadStep.SUBMIT_UPLOAD_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
            </div> 
        </div>
    </form>
    <% if (bSherpa) { %>
        <div class="col-md-4">
            <div id="sherpaBox" class="panel panel-info">
                <div class="panel-heading">
                    <span id="ui-id-1"><fmt:message key="jsp.sherpa.title" /></span>
                </div>
                <div id="sherpaContent" class="panel-body">
                    <fmt:message key="jsp.sherpa.loading">
                          <fmt:param value="<%=request.getContextPath()%>" />
                    </fmt:message>  
                </div>
            </div>
        </div>
    <% } %>
</dspace:layout>
