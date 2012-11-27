/*
 * This code gets the solr query from the class attribute (where it's put by
 * the DryadSearch XSLT code); it's performs the Solr query and gets the hit
 * count back and then puts the hit count in the text node with the name of
 * the externally harvested collection.
 */

var query = clean_count_query(jQuery('#dryadCount').attr('class'), 'l2');

jQuery.get("/solr/search/select/?indent=on&rows=1&fq=location:l2&q=DSpaceStatus:Archived&" + query,
	function(xml){
		var count = jQuery(xml).find('result').attr('numFound');
		jQuery('#dryadCount').append(document.createTextNode('(' + count + ')'));
		
		// we change the link in tabs that have hit counts of zero so that the
		// link just sends the user back to the same page (instead of actually
		// performing the search)
		if (count == 0) {
			jQuery('#dryadResultsLink').attr("href", "#");
		}
	});