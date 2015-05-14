function init_dragNdrop() {
	//
	// This function checks for file drag'n'drop support, and if true
	// than use fileuploadUI api.
	// 
	// Note: Should work for FF, Chrome, Safari.
	// Note: Element id's used from structural.xsl
	//
	jQuery(document).bind('dragenter', ignoreDrag).bind('dragover',
			function(e) {
				ignoreDrag(e);
				e.originalEvent.dataTransfer.dropEffect = 'none';
				return false;
			}).bind('drop', ignoreDrag);

	var dropZone = jQuery(
			"<li><span class='well well-large bold' id=\"drop_zone\" dropzone=\"copy\" style=\"font-size: 32px; display:block; line-height:100px; text-align:center; margin-bottom: 15px; margin-top: -20px; \">Drop file(s) here.</span></li>")
			.bind('dragover', dragOver).bind('drop', drop).bind('dragleave',
					dragLeave);

	if (!!FileReader && 'draggable' in document.createElement('span') && !!window.FormData && "upload" in new XMLHttpRequest) {
		jQuery("#file_upload #aspect_submission_StepTransformer_field_file").parents(
				"li:first").after(dropZone);
	}
} // function init_dragNdrop

function ignoreDrag(e) {
	e.originalEvent.preventDefault();
	e.originalEvent.stopPropagation();
}

function drop(e) {
	ignoreDrag(e);
	var files = e.originalEvent.dataTransfer.files;
	if (files === null || files === undefined || files.length === 0) {
		alert("Can't upload these files.");
		return false;
	}
	processFiles(files);
	e.originalEvent.dataTransfer.files = null;
	return false;
}

function dragOver(e) {
	ignoreDrag(e);
	e.originalEvent.dataTransfer.dropEffect = 'copy';
	jQuery(this).find("#drop_zone").css("background", "#ececef");
	return false;
}

function dragLeave(e) {
	ignoreDrag(e);
	jQuery(this).find("#drop_zone").css("background", "#F9FAFC");
	return false;
}

jQuery(document)
.ready(
		function() {
			init_dragNdrop();
		}
);