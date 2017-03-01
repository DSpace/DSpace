jQuery(document).ready(function() {

	// Hide the last tile from the set of 5 "recently added" on the collection homepage
	if (jQuery("#aspect_artifactbrowser_CollectionViewer_div_collection-recent-submission > div > div").length > 4)
	{
		jQuery("#aspect_artifactbrowser_CollectionViewer_div_collection-recent-submission > div > div").last().hide();
	}
});