function createRejectedFilesDialog(files, fileUploadDialog) {
	jQuery('#rejected_files').remove();
	var filesToRejectDiv = jQuery("<div id=\"rejected_files\" class=\"well well-light \"><p>The following files are too large for conventional upload (limit is "
			+ convertBytesToHumanReadableForm(lindat_upload_file_alert_max_file_size)
			+ "). Please contact <a href=\"mailto:"
			+ ufal_help_mail
			+ "\">Help Desk</a> about how to upload these files.</p></div>");
	filesToRejectDiv.attr('class', 'modal-body');

	for ( var i = 0; i < files.length; i++) {
		var file = files[i];
		filesToRejectDiv.append("<div id='fileName" + i + "'><b>Filename: </b>"
				+ file.name + "</div>");
		filesToRejectDiv.append("<div id='fileSize" + i + "'><b>Size: </b>"
				+ convertBytesToHumanReadableForm(file.size) + "</span></div>");
		filesToRejectDiv.append("<br />");
	}

	return filesToRejectDiv.dialog({
		modal : true,
		title : "Rejected files",
		resizable : true,
		autoOpen : false,
		width : 600,
		dialogClass : "modal well well-light",
		buttons : [ {
			text : "OK",
			class : "btn btn-repository",
			click : function() {
				jQuery(this).dialog("close");
				if (fileUploadDialog != undefined) {
					fileUploadDialog.dialog("open");
				} else {
					jQuery("#aspect_submission_StepTransformer_field_file")
							.val("");
				}
			}
		} ],
	});
}

function convertBytesToHumanReadableForm(b) {
	var units = [ 'bytes', 'kB', 'MB', 'GB', 'TB', 'PB', 'EB', 'ZB', 'YB' ];
	var exp = Math.round(Math.log(b) / Math.log(1024));
	return Math.round((b / Math.pow(1024, exp)) * 100) / 100 + " " + units[exp];
}

function createFileUploadDialog(files) {
	var xhrs = [];

	var fileFieldO = jQuery("#aspect_submission_StepTransformer_field_file");
	var action = fileFieldO.parents("form:first").attr("action");
	fileFieldO.parents("form:first").attr("action", action + "#fl");

	jQuery('#uploaded_files').remove();
	var filesToUploadDiv = jQuery("<div id=\"uploaded_files\" class=\"well well-light\"><p>Please fill in the description(s) and hit the \"Start Upload\" button.\n Then wait till the file(s) are uploaded.</p></div>");
	filesToUploadDiv.attr('class', 'modal-body');

	for ( var i = 0; i < files.length; i++) {
		var file = files[i];
		filesToUploadDiv.append("<div id='fileName" + i + "'><b>Filename: </b>"
				+ file.name + "</div>");
		filesToUploadDiv.append("<div id='fileType" + i + "'><b>Type: </b>"
				+ file.type + "</div>");
		filesToUploadDiv.append("<div id='fileDescDiv" + i
				+ "'><b>Describe the file: </b><input id='fileDesc" + i
				+ "' type=\"text\"/></div>");
		filesToUploadDiv.append("<div id='fileSizeProgress" + i
				+ "'><b>Progress: </b><span id='fileSize" + i + "'>0 bytes / "
				+ convertBytesToHumanReadableForm(file.size) + "</span></div>");
		filesToUploadDiv.append("<div id='fileProgress" + i
				+ "'><progress id='progressBar" + i
				+ "' value='0' max='100' /></div>");
		filesToUploadDiv.append("<br />");
	}

	return filesToUploadDiv
			.dialog({
				modal : true,
				title : "File upload",
				resizable : true,
				closeOnEscape : false,
				autoOpen : false,
				width : 600,
				dialogClass : "modal well well-light",
				buttons : [
						{
							text : "OK",
							class : "btn btn-repository",
							click : function() {
								jQuery(this).dialog("close");
								// location.reload(true);
								jQuery("input[type='submit'][value='Upload']")
										.click();
							},
							id : "js-ok-button"
						},
						{
							text : "Start Upload",
							class : "btn btn-repository",
							id : "js-su-button",
							click : function() {
								jQuery('#js-su-button').button('disable');
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
										xhrs
												.push(jQuery
														.ajax({
															xhr : function(fs,
																	pb, f) {
																return function() {
																	// Upload
																	// progress
																	var xhr = new window.XMLHttpRequest();
																	xhr.upload
																			.addEventListener(
																					"progress",
																					function(
																							evt) {
																						if (evt.lengthComputable) {
																							var percentComplete = Math
																									.round(evt.loaded
																											* 100
																											/ evt.total);
																							pb
																									.attr(
																											"value",
																											percentComplete
																													.toString());
																							fs
																									.html(convertBytesToHumanReadableForm(evt.loaded)
																											+ " / "
																											+ convertBytesToHumanReadableForm(f.size));
																						}
																					},
																					false);
																	return xhr;
																}
															}(fileSize,
																	progBar,
																	file),
															error : function(f) {
																return function(
																		jqXHR,
																		textStatus,
																		errorThrown) {
																	// scream on
																	// errors
																	// but not
																	// on abort
																	if (textStatus != 'abort') {
																		alert("Upload of "
																				+ f.name
																				+ " failed.\n"
																				+ textStatus
																				+ "\n"
																				+ errorThrown);
																	}
																}
															}(file),
															success : function(
																	fs, pb) {
																return function() {
																	// fix for
																	// FF
																	pb
																			.attr(
																					"value",
																					"100");
																	fs
																			.html("Done.");
																}
															}
																	(fileSize,
																			progBar),
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
									// filesToUploadDiv.parent().find(".ui-dialog-buttonpane
									// button:contains('OK')").attr("disabled",
									// false);
									// alert("Is this called at all?");
								}).fail(function() {
									// failure
									jQuery('#js-su-button').hide();
									jQuery('#js-ok-button').show();
								});
							}
						} ],
				beforeClose : function() {
					xhrs.forEach(function(xhr) {
						xhr.abort();
					});
					// remove the selection so the files are not submitted again
					jQuery("#aspect_submission_StepTransformer_field_file")
							.val("");
				},
				open : function() {
					jQuery("#js-ok-button").hide();
				}
			});
}

function filterFiles(files, callback) {
	var res = [];
	for ( var i = 0; i < files.length; i++) {
		var file = files[i];
		if (callback(file)) {
			res.push(file);
		}
	}
	return res;
}

function processFiles(files) {
	// console.log("processing files");

	var filesToUpload = filterFiles(files, function(f) {
		return f.size <= lindat_upload_file_alert_max_file_size;
	});
	var filesToReject = filterFiles(files, function(f) {
		return f.size > lindat_upload_file_alert_max_file_size;
	});

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
		rejectedFilesDialog.dialog("open");
	} else if (fileUploadDialog != undefined) {
		fileUploadDialog.dialog("open");
	}

}

// call at the beginning
//
jQuery(document)
		.ready(
				function() {
					
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
