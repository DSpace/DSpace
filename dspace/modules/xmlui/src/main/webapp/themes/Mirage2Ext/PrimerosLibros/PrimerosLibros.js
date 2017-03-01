

jQuery(document).ready(function() {

	// register and handle the click to show and hide the "downloads" menu
	jQuery("span.image-gallery-save-link").on("click", function() {

	    var options = jQuery(this).prev();

	    options.slideToggle();
	    options.mouseleave(function() {
	    	options.slideUp();
	    })        
	    
	});
	
	// Hide the last tile from the set of 5 "recently added" on the collection homepage
	// if (jQuery("#aspect_artifactbrowser_CollectionViewer_div_collection-recent-submission > div > div").length > 4) {
	// 	var i = 4;
	// 	while (i < 5) {
	// 		jQuery("#aspect_artifactbrowser_CollectionViewer_div_collection-recent-submission > div > div")[i].hide();
	// 		i++;
	// 	}
	// }

});
