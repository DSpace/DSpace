/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {

AjaxSolr.AutocompleteWidget = AjaxSolr.AbstractFacetWidget.extend({

    afterRequest: function () {

    $(this.target).find("input[type='text']").val('');

    var self = this;

    var callback = function (response) {
      var list = [];
      var source = [];
      var counts = [];
      for (var field in response.facet_counts.facet_fields) {
        for (var facet in response.facet_counts.facet_fields[field]) {
	  var text = facet + ' (' + response.facet_counts.facet_fields[field][facet] + ')';
          list[text] = {
            field: field,
            value: facet,
            text: text,
          };
	  source.push(text);
          counts[text] = response.facet_counts.facet_fields[field][facet]; 
        }
      }

      self.requestSent = false;
      $(self.target).find("input[type='text']").typeahead({
		source: source,
		updater: function(item){
		  console.log("Processing " + item);
		  return "\"" + list[item].value + "\"";
		},
		matcher: function(item){
			var value = item.substring(0,item.lastIndexOf("(") - 1); //-1 for space in front of '('
			//default matcher but changed value
			return ~value.toLowerCase().indexOf(this.query.toLowerCase());
		},
		sorter: function(items){
			return items.sort(function(i1, i2){
				return counts[i2] - counts[i1];
			})
		},
        });
    }; // end callback

    var params = [ 'q=' + query + '&facet=true&facet.limit=-1&facet.sort=count&facet.mincount=1&json.nl=map' ];
    for (var i = 0; i < this.fields.length; i++) {
      params.push('facet.field=' + this.fields[i]);
    }
    var fqs = $("input[name='fq']");
    for(var j = 0; j < fqs.length; j ++){
        params.push('fq=' + encodeURIComponent($(fqs[j]).val()));
    }
    //Attempt to add our scope !
    var scope = $("input[name='discovery-json-scope']").val();
    if(scope != undefined){
        params.push("scope=" + scope);
    }


    


//    jQuery.getJSON(this.manager.solrUrl + 'select?' + params.join('&') + '&wt=json&json.wrf=?', {}, callback);
    jQuery.getJSON(this.manager.solrUrl + '?' + params.join('&') + '&wt=json&json.wrf=?', {}, callback);
}

});

})(jQuery);
