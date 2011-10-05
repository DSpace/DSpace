/*
 * Common JavaScript functions for working with the search interface's tabs.
 * Tabs are created for externally harvested collections; each tab is a query to
 * solr to get the hit count.  The code that is common across the different tab
 * targets gets put here.
 */


// Trying out syntax that will serve as a workaround for query problems
function solr_query_escape(query, location) {
	// Already a wildcard query
	if (/\*$/.test(query)) return query;
	// Non-Dryad records don't use the Discovery syntax
	else if (location != 'l2') {
		return query.replace(/\s/g, "\\%20") + "*";
	}
	// Else, we have a regular Dryad Discovery record
	else return query.replace(/\s/g, "\\%20") + "\\|*";
}

//Common code for processing the counts for the tabs; we're interacting
//directly with solr rather than going through discovery so we need to
//translate between solr syntax and the URL syntax used by dspace discovery
function clean_count_query(query, location) {
	
	// seeing some variation, so handling both parameter name possibilities...
	query = query.replace(/q=$/, 'q=*:*').replace('q=\&', 'q=*:*&');
	query = query.replace(/query=$/, 'query=*:*').replace('query=\&', 'query=*:*&');

	// clean up/strip what comes out of discovery before sending to solr
	query = query.replace(/\\|\\|\\|/g, '');
	query = query.replace('filter=\*\:\*', '');
	query = query.replace('fq=\*\:\*', '');

	query = query + getExtraFilter(query);
	
	// then we go through and construct phrase searches for our filter queries
	var vars = query.split("&");

	// zero out our query variable so we can build it anew
	query = "";

	for (var index = 0; index < vars.length; index++) {
		  var pair = vars[index].split("=");

		  if (pair[0] == 'fq') {
		    if (pair[1].indexOf(":")) {
		      var parts = pair[1].split(":");

		      // Dates seem to be treated differently, they do resolve as is...
		      if (parts[0] == 'dc.date.issued.year') {
		    	  query = query + pair[0] + "=" + parts[0] + ':'
	      			+ parts[1].replace(/\|\|\|.*/, '') + "&";
		      }
		      else if (parts[0].indexOf('_') != -1) {
		    	  query = query + pair[0] + "=" + parts[0] + ':'
		      			+ solr_query_escape(parts[1].replace(/\|\|\|.*/, ''), location)
		      			+ "&";
		      }
		      else {
		    	  query = query + pair[0] + '=' + pair[1] + '&';
		      }
		    }
		    else {
		      query = query + pair[0] + "=" + pair[1] + "&";
		    }
		  }
		  else if (pair[1] != undefined) {
		    query = query + pair[0] + "=" + pair[1] + "&";
		  }
	}

	return query.substring(0, query.length - 1);
}

// Translate discovery search box syntax to solr syntax
function getExtraFilter(query) {
	var vars = query.split("&");
	var filterType = '';
	var filter = '';
	
	for (var index = 0; index < vars.length; index++) {
		  var pair = vars[index].split("=");
		  
		  if (pair[0] == 'filter') {
			  filter = pair[1];
		  }
		  else if (pair[0] == 'filtertype') {
			  filterType = pair[1];
		  }
	}
	
	if (filter != undefined && filter != '') {
		if (filterType == undefined || filterType == '*') {
			filterType = '';
		}
		
		return '&fq=' + filterType + ':' + filter; 
	}
	
	return '';
}