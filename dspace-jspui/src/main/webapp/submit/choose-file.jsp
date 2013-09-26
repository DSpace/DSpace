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


<%
    request.setAttribute("LanguageSwitch", "hide");

    // Obtain DSpace context
    Context context = UIUtil.obtainContext(request);    

	//get submission information object
    SubmissionInfo subInfo = SubmissionController.getSubmissionInfo(context, request);

    boolean withEmbargo = ((Boolean)request.getAttribute("with_embargo")).booleanValue();

 	// Determine whether a file is REQUIRED to be uploaded (default to true)
 	boolean fileRequired = ConfigurationManager.getBooleanProperty("webui.submit.upload.required", true);
    boolean ajaxProgress = ConfigurationManager.getBooleanProperty("webui.submit.upload.ajax", true);

 	Boolean sherpa = (Boolean) request.getAttribute("sherpa");
    boolean bSherpa = sherpa != null?sherpa:false;

    if (ajaxProgress || bSherpa)
    {
%>
<c:set var="dspace.layout.head.last" scope="request">
<%        
     if (bSherpa) { %>

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
	<% } 
	if (ajaxProgress) { %>
	<link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/jquery.fileupload-ui.css">
	<!-- CSS adjustments for browsers with JavaScript disabled -->
	<noscript><link rel="stylesheet" href="<%= request.getContextPath() %>/static/css/jquery.fileupload-ui-noscript.css"></noscript>
    <script type="text/javascript">
    function initProgressBar($){
    	var progressbarArea = $("#progressBarArea");
		progressbarArea.show();
    }
    
    function updateProgressBar($, data){
    	$('#uploadForm').find('input').attr('disabled','disabled');
    	$('#spanFile').button("disable")
    	$('#spanFileCancel').button("disable")
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
			$('<p id="selectedFile">'+$('#tfile').val().replace(/.*(\/|\\)/, '')+'</p>').insertAfter($('#spanFile')).append('&nbsp;');
			var span = $('<span id="spanFileCancel"><fmt:message key="jsp.submit.choose-file.upload-ajax.button.cancel"/></span>');
			span.appendTo($('#selectedFile'));
    		span.button({icons: {primary: "ui-icon ui-icon-cancel"}})
    			.click(function(e){
    				var parent = $('#spanFile').parent();
    				$('#spanFile').remove();
    				$('#selectedFile').remove();
    				$('<input type="file" name="file" id="tfile">').appendTo(parent);
    				$('#tfile').wrap('<span id="spanFile" class="fileinput-button"><fmt:message key="jsp.submit.choose-file.upload-ajax.button.select-file"/></span>');
    		    	$('#spanFile').button({icons: {primary: "ui-icon ui-icon-folder-open"}});
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
    	$('#tfile').wrap('<span id="spanFile" class="fileinput-button"><fmt:message key="jsp.submit.choose-file.upload-ajax.button.select-file"/></span>');
    	$('#spanFile').button({icons: {primary: "ui-icon ui-icon-folder-open"}});
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
   			$('#uploadForm').attr('target','uploadFormIFrame');
   			if ($('#tfile').val() != null && $('#tfile').val() != '') {
	   			initProgressBar($);
	   			setTimeout(function() {
					monitorProgressJSON($);					
				}, 100);
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
   					resultFile = new Object();
	   				resultFile.status = null;
   				}
   				
   	    		if (resultFile.status == <%= UploadStep.STATUS_COMPLETE %> || 
   	    				resultFile.status == <%= UploadStep.STATUS_UNKNOWN_FORMAT %>)
   	    		{
   	    			completeProgressBar($, resultFile.size);
   		           	if (resultFile.status == <%= UploadStep.STATUS_COMPLETE %>)
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
</c:set>
<%  } %>

<dspace:layout locbar="off"
               navbar="off"
               titlekey="jsp.submit.choose-file.title"
               nocache="true">
<% if (ajaxProgress) { %>
	<div style="display:none;" id="uploadError" title="<fmt:message key="jsp.submit.upload-error.title" />">
		<p><fmt:message key="jsp.submit.upload-error.info" /></p>
	</div>
	<div style="display:none;" id="fileRequired" title="<fmt:message key="jsp.submit.choose-file.upload-ajax.fileRequired.title" />">
		<p><fmt:message key="jsp.submit.choose-file.upload-ajax.fileRequired.info" /></p>
	</div>
	<div style="display:none;" id="fileSizeLimitExceeded" title="<fmt:message key="jsp.error.exceeded-size.title" />">
		<p><fmt:message key="jsp.error.exceeded-size.text1">
		<fmt:param><span id="actualSize">&nbsp;</span></fmt:param>
		<fmt:param><span id="limitSize">&nbsp;</span></fmt:param>
		</fmt:message></p>
	</div>
	<div style="display:none;" id="virusFound" title="<fmt:message key="jsp.submit.upload-error.title" />">
		<p><fmt:message key="jsp.submit.virus-error.info" /></p>
	</div>
	<div style="display:none;" id="virusCheckNA" title="<fmt:message key="jsp.submit.upload-error.title" />">
		<p><fmt:message key="jsp.submit.virus-checker-error.info" /></p>
	</div>
    <form style="display:none;" id="uploadFormPostAjax" method="post" action="<%= request.getContextPath() %>/submit" 
    	enctype="multipart/form-data" onkeydown="return disableEnterKey(event);">
    <%= SubmissionController.getSubmissionParameters(context, request) %>    
    </form>
    <iframe id="uploadFormIFrame" name="uploadFormIFrame" style="display: none"> </iframe>
<% } %>
    <form id="uploadForm" <%= bSherpa?"class=\"sherpa\"":"" %> method="post" 
    	action="<%= request.getContextPath() %>/submit" enctype="multipart/form-data" 
    	onkeydown="return disableEnterKey(event);">

		<jsp:include page="/submit/progressbar.jsp"/>
		
		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>

        <%-- <h1>Submit: Upload a File</h1> --%>
		<h1><fmt:message key="jsp.submit.choose-file.heading"/></h1>
    
        <%-- <p>Please enter the name of
        <%= (si.submission.hasMultipleFiles() ? "one of the files" : "the file" ) %> on your
        local hard drive corresponding to your item.  If you click "Browse...", a
        new window will appear in which you can locate and select the file on your
        local hard drive. <object><dspace:popup page="/help/index.html#upload">(More Help...)</dspace:popup></object></p> --%>

		<p><fmt:message key="jsp.submit.choose-file.info1"/>
			<dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.index\") + \"#upload\"%>"><fmt:message key="jsp.morehelp"/></dspace:popup></p>
        
        <%-- FIXME: Collection-specific stuff should go here? --%>
        <%-- <p class="submitFormHelp">Please also note that the DSpace system is
        able to preserve the content of certain types of files better than other
        types.
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\")%>">Information about file types</dspace:popup> and levels of
        support for each are available.</p> --%>
        
		<div class="submitFormHelp"><fmt:message key="jsp.submit.choose-file.info6"/>
        <dspace:popup page="<%= LocaleSupport.getLocalizedMessage(pageContext, \"help.formats\")%>"><fmt:message key="jsp.submit.choose-file.info7"/></dspace:popup>
        </div>

<% if (ajaxProgress)
{
%>
       <div id="progressBarArea" style="display: none;  width: 50%; float: right;">
               <div id="progressBar"></div>
               <p class="progressBarInitMsg">
               			<fmt:message key="jsp.submit.choose-file.upload-ajax.uploadInit"/>
               	</p>
               <p class="progressBarProgressMsg" style="display: none;">
                       <fmt:message key="jsp.submit.choose-file.upload-ajax.uploadInProgress">
                               <fmt:param><span class="percent">&nbsp;</span></fmt:param>
                               <fmt:param><span class="bytesRead">&nbsp;</span></fmt:param>
                               <fmt:param><span class="bytesTotal">&nbsp;</span></fmt:param>
                       </fmt:message></p>
               <p class="progressBarCompleteMsg" style="display: none;">
                       <fmt:message key="jsp.submit.choose-file.upload-ajax.uploadCompleted">
                               <fmt:param><span class="bytesTotal">&nbsp;</span></fmt:param>
                       </fmt:message></p>
       </div>
<% } %>
    
        <table border="0" align="center">
            <tr>
                <td class="submitFormLabel">
                    <%-- Document File: --%>
					<label for="tfile"><fmt:message key="jsp.submit.choose-file.document"/></label>
                </td>
                <td>
                    <input type="file" size="40" name="file" id="tfile" />
                </td>
            </tr>
<%
    if (subInfo.getSubmissionItem().hasMultipleFiles())
    {
%>
            <tr>
                <td colspan="2">&nbsp;</td>
            </tr>
            <tr>
                <td class="submitFormHelp" colspan="2">
                    <%-- Please give a brief description of the contents of this file, for
                    example "Main article", or "Experiment data readings." --%>
					<fmt:message key="jsp.submit.choose-file.info9"/>
                </td>
            </tr>
            <tr>
                <%-- <td class="submitFormLabel">File Description:</td> --%>
				<td class="submitFormLabel"><label for="tdescription"><fmt:message key="jsp.submit.choose-file.filedescr"/></label></td>
                <td><input type="text" name="description" id="tdescription" size="40"/></td>
            </tr>
<%
    }
%>
        </table>

<%
    if (withEmbargo)
    {
%>
        <br/>
        <dspace:access-setting subInfo="<%= subInfo %>" dso="<%= subInfo.getSubmissionItem().getItem() %>" hidden="true" />
        <br/>
<%
    }
%>

		<%-- Hidden fields needed for SubmissionController servlet to know which step is next--%>
        <%= SubmissionController.getSubmissionParameters(context, request) %>
    
        <p>&nbsp;</p>

        <center>
            <table border="0" width="80%">
                <tr>
                    <td width="100%">&nbsp;</td>
               	<%  //if not first step, show "Previous" button
					if(!SubmissionController.isFirstStep(request, subInfo))
					{ %>
                    <td>
                        <input type="submit" name="<%=AbstractProcessingStep.PREVIOUS_BUTTON%>" value="<fmt:message key="jsp.submit.general.previous"/>" />
                    </td>
				<%  } %>
                    <td>
                        <input type="submit" name="<%=UploadStep.SUBMIT_UPLOAD_BUTTON%>" value="<fmt:message key="jsp.submit.general.next"/>" />
                    </td> 
                    <%
                        //if upload is set to optional, or user returned to this page after pressing "Add Another File" button
                    	if (!fileRequired || subInfo.getSubmissionItem().getItem().hasUploadedFiles())
                        {
                    %>
                        	<td>
                                <input type="submit" name="<%=UploadStep.SUBMIT_SKIP_BUTTON%>" value="<fmt:message key="jsp.submit.choose-file.skip"/>" />
                            </td>
                    <%
                        }
                    %>   
                              
                    <td>&nbsp;&nbsp;&nbsp;</td>
                    <td align="right">
                        <input type="submit" name="<%=AbstractProcessingStep.CANCEL_BUTTON%>" value="<fmt:message key="jsp.submit.general.cancel-or-save.button"/>" />
                    </td>
                </tr>
            </table>
        </center>  
    </form>
<%
  if (bSherpa)
      {
%>
  <div id="sherpaBox" class="ui-dialog ui-widget ui-widget-content ui-corner-all ui-front">
  	  <div class="ui-dialog-titlebar ui-widget-header ui-corner-all ui-helper-clearfix">
  		  <span id="ui-id-1" class="ui-dialog-title"><fmt:message key="jsp.sherpa.title" /></span>
  	  </div>
	  <div id="sherpaContent" class="ui-dialog-content ui-widget-content">
	  <fmt:message key="jsp.sherpa.loading">
			<fmt:param value="<%=request.getContextPath()%>" />
	  </fmt:message>  
	  </div>
  </div>
<%
    }
%>
</dspace:layout>
