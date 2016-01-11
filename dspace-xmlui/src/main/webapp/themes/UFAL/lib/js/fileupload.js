/*jshint multistr: true */
function createRejectedFilesDialog(files, fileUploadDialog) {
	jQuery('#rejected_files').remove();
	var modal_str = '<div class="modal fade" id="rejected_files" tabindex="-1" role="dialog">\
<div class="modal-dialog">\
<div class="modal-content">\
<div class="modal-header">\
<button type="button" class="close" data-dismiss="modal">&times;</button>\
<h4 class="modal-title">' + $.i18n._("Rejected files") + '</h4>\
</div>\
<div class="modal-body" id="rejected_modal_body"><p>' + $.i18n._("The following files are too large for conventional upload (limit is %s)" +
			" or are empty (0 bytes). Please contact %s about how to upload these files.",
					convertBytesToHumanReadableForm(lindat_upload_file_alert_max_file_size), "<a href=\"mailto:" + ufal_help_mail +"\">Help Desk</a>") + '</p>\
</div>\
<div class="modal-footer">\
<button type="button" class="btn btn-primary" id="rejected-ok-button">OK</button>\
</div>\
</div>\
</div>';
	var jModal = jQuery(modal_str);
	var modal_body = jModal.find("#rejected_modal_body");
	for ( var i = 0; i < files.length; i++) {
		var file = files[i];
		modal_body.append("<div id='fileName" + i + "'><b>" + $.i18n._("Filename") + ": </b>"
			+ file.name + "</div>");
		modal_body.append("<div id='fileSize" + i + "'><b>" + $.i18n._("Size") + ": </b>"
			+ convertBytesToHumanReadableForm(file.size) + "</span></div>");
		modal_body.append("<br />");
	}
	jModal.find("#rejected-ok-button").click(function() {
		jModal.modal("toggle");
		if (fileUploadDialog != undefined) {
			fileUploadDialog.modal();
		} else {
			jQuery("#aspect_submission_StepTransformer_field_file")
				.val("");
		}
	});
	return jModal;
}

function convertBytesToHumanReadableForm(b) {
	var units = [ 'bytes', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB' ];
	var exp = b == 0 ? 0 : Math.round(Math.log(b) / Math.log(1024));
	return Math.round((b / Math.pow(1024, exp)) * 100) / 100 + " " + units[exp];
}

function createFileUploadDialog(files) {
	var xhrs = [];

	var fileFieldO = jQuery("#aspect_submission_StepTransformer_field_file");
	var action = fileFieldO.parents("form:first").attr("action");
	fileFieldO.parents("form:first").attr("action", action + "#fl");

	jQuery('#uploaded_files').remove();
	var modal_str = '<div class="modal fade" id="uploaded_files" tabindex="-1" role="dialog">\
<div class="modal-dialog">\
<div class="modal-content">\
<div class="modal-header">\
<button type="button" class="close" data-dismiss="modal">&times;</button>\
<h4 class="modal-title">' + $.i18n._("File Upload") + '</h4>\
</div>\
<div class="modal-body" id="files_modal_body"><p>' + $.i18n._("Please fill in the description(s) and hit the \"Start Upload\" button.\n Then wait till the file(s) are uploaded.") + '</p>\
</div>\
<div class="modal-footer">\
<button type="button" class="btn btn-primary" id="js-su-button">' + $.i18n._("Start Upload") +'</button>\
<button type="button" class="btn btn-primary hidden" id="js-ok-button">OK</button>\
</div>\
</div>\
</div>';
	var jModal = jQuery(modal_str);
	var modal_body = jModal.find("#files_modal_body");

	if ( 3 < files.length ) {
		jModal.addClass("modal-scrollbar");
	}
	for ( var i = 0; i < files.length; i++) {
		var file = files[i];
		modal_body.append("<div id='fileName" + i + "'><b>" + $.i18n._("Filename") + ": </b>"
			+ file.name + "</div>");
		modal_body.append("<div id='fileType" + i + "'><b>" + $.i18n._("Type") + ": </b>"
			+ file.type + "</div>");
		modal_body.append("<div id='fileDescDiv" + i
			+ "'><b>" + $.i18n._("Describe the file") + ": </b><input id='fileDesc" + i
			+ "' type=\"text\"/></div>");
		modal_body.append("<div id='fileSizeProgress" + i
			+ "'><b>" + $.i18n._("Progress") + ": </b><span id='fileSize" + i + "'>0 bytes / "
			+ convertBytesToHumanReadableForm(file.size) + "</span></div>");
		modal_body.append("<div id='fileProgress" + i
			+ "'><progress id='progressBar" + i
			+ "' value='0' max='100' /></div>");
		modal_body.append("<br />");
	}
	jModal.find("#js-su-button").click(function() {
		$(this).prop('disabled', true);
		xhrs = [];
		for ( var i = 0; i < files.length; i++) {
			var file = files[i];
			if (file) {
				var progBarId = '#progressBar' + i;
				var fileSizeId = '#fileSize' + i;
				var fileDescId = '#fileDesc' + i;
				var fileSize = jQuery(fileSizeId);
				var progBar = jQuery(progBarId);
				var fileDesc = jQuery(fileDescId);
				var fd = new FormData();
				fd.append("file", file);
				var description = null;
				description = fileDesc.val();
				fileDesc.prop('disabled', true);
				if (description
					&& 0 !== description.length) {
					fd.append("description",
						description);
				}
				// send
				xhrs.push(jQuery.ajax({
					xhr : function(fs, pb, f) {
						return function() {
							// Upload progress
							var xhr = new window.XMLHttpRequest();
							xhr.upload.addEventListener(
								"progress",
								function(evt) {
									if (evt.lengthComputable) {
										var percentComplete = Math.round(evt.loaded	* 100 / evt.total);
										pb.attr("value", percentComplete.toString());
										fs.html(convertBytesToHumanReadableForm(evt.loaded)	+ " / "	+
											convertBytesToHumanReadableForm(f.size));
									}
								},
								false);
							return xhr;
						}}(fileSize, progBar,file),
					error : function(f) {
						return function(jqXHR, textStatus, errorThrown) {
							// scream on errors but not on abort
							if (textStatus !== 'abort') {
								alert($.i18n._("Upload of %s failed.\n%s\n%s",f.name, textStatus, errorThrown));
							}
						}}(file),
					success : function(fs, pb) {
						return function() {
							// fix for FF
							pb.attr("value","100");
							fs.html($.i18n._("Done."));
						}}(fileSize,progBar),
					url : action,
					data : fd,
					contentType : false,
					processData : false,
					type : 'POST'
				}));
			}
		}
		// wait 4 all xhrs to finish
		$.when.apply($, xhrs).done(function() {
			// success
			jQuery('#js-su-button').hide();
			jQuery('#js-ok-button').show();
			jQuery("#js-ok-button").click();
			xhrs = [];
		}).fail(function() {
			// failure
			jQuery('#js-su-button').hide();
			jQuery('#js-ok-button').show();
		});
	});
	jModal.find("#js-ok-button").click(function() {
		jModal.modal('toggle');
		// location.reload(true);
		jQuery("#aspect_submission_StepTransformer_field_submit_jump_2_1")
			.click();
	});
	jModal.on('hide.bs.modal',function() {
		xhrs.forEach(function(xhr) {
			xhr.abort();
		});
		// remove the selection so the files are not submitted again
		jQuery("#aspect_submission_StepTransformer_field_file")
			.val("");
	});
	return jModal;
}

function filterFiles(files, callback, matching, not_matching) {
	for ( var i = 0; i < files.length; i++) {
		var file = files[i];
		if (callback(file)) {
			matching.push(file);
		}else{
            not_matching.push(file);
        }
	}
}

function processFiles(files) {
	// console.log("processing files");

    var accept = function(f) {
		return f.size > 0 && f.size <= lindat_upload_file_alert_max_file_size;
	};
	var filesToUpload = [];
	var filesToReject = [];
    filterFiles(files, accept, filesToUpload, filesToReject);

	var rejectedFilesDialog;
	var fileUploadDialog;

	if (filesToUpload.length > 0) {
		fileUploadDialog = createFileUploadDialog(filesToUpload);
	}

	if (filesToReject.length > 0) {
		rejectedFilesDialog = createRejectedFilesDialog(filesToReject,
			fileUploadDialog);
	}

	if (rejectedFilesDialog != undefined) {
		rejectedFilesDialog.modal();
	} else if (fileUploadDialog != undefined) {
		fileUploadDialog.modal();
	}

}

// call at the beginning
//
jQuery(document)
	.ready(
	function() {

		jQuery.i18n.load("cs", {
			"Rejected files": "Odmítnuté soubory",
			"The following files are too large for conventional upload (limit is %s) or are empty (0 bytes). Please contact %s about how to upload these files.":
				"Následující soubory jsou příliš velké (limit je %s) pro nahrání běžnou cestou a nebo jsou prázdné (0 bytů). Tyto soubory vám pomůže nahrát %s, prosím kontaktujte jej.",
			"Filename": "Jméno souboru",
			"Size": "Velikost",
			"File Upload": "Odeslání souboru",
			"Please fill in the description(s) and hit the \"Start Upload\" button.\n Then wait till the file(s) are uploaded.":
				"Vyplňte prosím popisky souborů a zmáčkněte tlačítko \"Začít odesílat\".\nPotom vyčkejte, dokud se soubor(y) nenahrají.",
			"Start Upload": "Začít odesílat",
			"Describe the file": "Popište soubor",
			"Progress": "Průběh",
			"Upload of %s failed.\n%s\n%s": "Nahrání souboru %s selhalo.\n%s\n%s",
			"Type": "Typ",
			"Done": "Hotovo",
		});

		var fileFieldO = jQuery("#aspect_submission_StepTransformer_field_file");

		if (!!FileReader && 'draggable' in document.createElement('span')
			&& !!window.FormData && "upload" in new XMLHttpRequest) {

			// dont hide this if we can't auto upload
			jQuery("#aspect_submission_StepTransformer_item_description-field").hide();
			jQuery("#aspect_submission_StepTransformer_field_submit_upload").parents("li:last").hide();

			// fileFieldO.parents("fieldset:first").after(filesToUploadDiv);
			fileFieldO
				.attr("multiple", "multiple")
				.change(
				function() {
					var files = document
						.getElementById("aspect_submission_StepTransformer_field_file").files;
					processFiles(files);
				});
		}


		jQuery(".checksum_cell")
			.each(
			function() {
				jQuery(this).attr("title",
					jQuery(this).text());
				jQuery(this)
					.html(
					"<img src='../../../../themes/UFAL/images/information.png' />")
			});// .html("&sum;")});
		jQuery(".checksum_cell").css("text-align", "center").css(
			"cursor", "pointer").each(function() {
				jQuery(this).click(function() {
					var checksum = jQuery(this).attr("title");
					alert(checksum);
					return false;
				});
			});
		jQuery(".order-box")
			.after(
			"<span class='order-up' style='cursor: pointer;'>&#x25B2;</span><span class='order-down'style='cursor: pointer;'>&#x25BC;</span>");
		jQuery(".order-box").hide();
		jQuery(".order-up").each(
			function() {
				jQuery(this).click(
					function() {
						// console.log(jQuery(this).parents("td:first").find("input"));
						var newOrd = jQuery(this).parents(
							"td:first").find("input")
							.val();
						if (newOrd > 0) {
							var thisRow = jQuery(this)
								.parents("tr:first");
							var prevRow = thisRow.prev();
							prevRow.find(".order-box").val(
								newOrd);
							jQuery(this)
								.parents("td:first")
								.find("input").val(
								--newOrd);
							thisRow.after(prevRow);
						}
						return false;
					});
			});
		jQuery(".order-down").each(
			function() {
				jQuery(this).click(
					function() {
						// console.log(jQuery(this).parents("td:first").find("input"));
						var newOrd = jQuery(this).parents(
							"td:first").find("input")
							.val();
						var lastOrd = jQuery(
							".order-box:last").val();
						if (newOrd < lastOrd) {
							var thisRow = jQuery(this)
								.parents("tr:first");
							var nextRow = thisRow.next();
							nextRow.find(".order-box").val(
								newOrd);
							jQuery(this)
								.parents("td:first")
								.find("input").val(
								++newOrd);
							thisRow.before(nextRow);
						}
						return false;
					});
			});


		var summaryDiv = jQuery("#aspect_submission_StepTransformer_div_summary");

		jQuery("#aspect_submission_StepTransformer_field_submit_remove_selected").attr("class", "hidden");
		jQuery("#aspect_submission_StepTransformer_field_submit_remove_all").click(
			function() {
				jQuery("input[name='remove']", summaryDiv).attr("checked", true);
			}
		);


		jQuery("input[name='remove']", summaryDiv).parent().parent().parent().css("cursor", "pointer");
		jQuery("input[name='remove']", summaryDiv).attr("class", "hidden");

		jQuery("input[name='remove']", summaryDiv).parent().parent().parent().click(function(){
			jQuery("input[name='remove']", summaryDiv).attr("checked", false);
			jQuery("input[name='remove']", this).attr("checked", true);
			jQuery("#aspect_submission_StepTransformer_field_submit_remove_selected").click();
		});

		// css update for local file upload section for admins
		jQuery("#aspect_submission_StepTransformer_list_submit-upload-local li")
			.css("margin", "10px 0px 10px 0px");

		// scrolldown
		var anchor = window.location.hash;
		if (anchor === "#fl") {
			jQuery('html, body').animate(
				{
					scrollTop : jQuery(".lindat-footer-main")
						.offset().top
					- jQuery(window).height()
				}, 200);
		}
	});
