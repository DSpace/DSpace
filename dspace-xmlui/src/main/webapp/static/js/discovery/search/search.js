/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
var Manager;
var query;
var defaultFacets = new Array();

(function ($) {
    $(function () {
        var searchUrl = $("input[name='discovery-json-search-url']").val();
        Manager = new AjaxSolr.Manager({
            solrUrl: searchUrl
        });

        //Retrieve our filterSelect, which contains all the types to be sorted on
        var filterSelect = $("tr[class=' search-filter'] select[id^='aspect_discovery_SimpleSearch_field_filtertype']");
        //Get our filters
        /*
        var filterOptions = filterSelect.find('option');
        //Get all the 
        for (var index = 1; index < filterOptions.length; index++){
            //We skip the first one (for the moment)
            defaultFacets[index - 1] = filterOptions[index].value;
        }
        */
        //As a default facet we use the selected value
        defaultFacets[0] = filterSelect.find('option:selected').val();
        
        var widget = Manager.addWidget(new AjaxSolr.AutocompleteWidget({
            id: 'text',
            target: 'tr[class=" search-filter"]',
            field: 'allText',
            fields: defaultFacets
        }));

        Manager.init();

        query = $('input#aspect_discovery_SimpleSearch_field_query').val();
        if(query == null || query == '')
            query = '*:*';

        Manager.store.addByValue('q', query);
        //Retrieve our filter queries
        var fqs = $("tr[class=' search-filter used-filter hidden']");
        for(var j = 0; j < fqs.length; j ++){
        	var selectedRow = fqs[j];
        	var filterType = $("select[name^='filtertype_']", selectedRow);
        	var filterOpr = $("select[name^='filter_relational_operator_']", selectedRow);
        	var filterVal = $("input[name^='filter_']", selectedRow);
            Manager.store.addByValue(filterType.attr('name'), filterType.val());
            Manager.store.addByValue(filterOpr.attr('name'), filterOpr.val());
            Manager.store.addByValue(filterVal.attr('name'), filterVal.val());
        }
        Manager.store.addByValue('facet.sort', 'count');


        var params = {
            facet: true,
            'facet.field': defaultFacets,
            'facet.limit': 20,
            'facet.mincount': 1,
            'f.topics.facet.limit': 50,
            'json.nl': 'map'
        };
        for (var name in params) {
            Manager.store.addByValue(name, params[name]);
        }

        //Attempt to add our scope !
        var scope = $("input[name='discovery-json-scope']").val();
        if(scope != undefined){
            Manager.store.addByValue("scope", scope);
        }

        Manager.doRequest();

        filterSelect.change(function() {
//            TODO: this is dirty, but with lack of time the best I could do
            var oldInput = $('tr[class=" search-filter"] input[id^="aspect_discovery_SimpleSearch_field_filter"]');
            var newInput = oldInput.clone(false);
            
//            newInput.val(oldInput.val());
            newInput.appendTo(oldInput.parent());
            oldInput.remove();

            //Remove any results lists we may still have standing
            $("div#discovery_autocomplete_div").remove();
            //Put the field in which our facet is going to facet into the widget
            var facetFields;

            if($(this).val() != '*'){
                var facetVal = $(this).val();
                //Only facet on autocomplete fields
//                if(!facetVal.match(/.year$/)){
//                    facetVal += '_ac';
//                }
                facetFields = [facetVal];
            } else {
                facetFields = defaultFacets;
            }

            Manager.widgets.text.fields = facetFields;
            Manager.initialized = false;
            
			var opr = $('tr[class=" search-filter"] select[id^="aspect_discovery_SimpleSearch_field_filter_relational_operator"]').val();
			if(opr=='notavailable') {
				newInput.value = "[* TO *]";
				return;
			}            

            Manager.init();
//TODO: does this need to happen twice ?
            Manager.store.addByValue('q', query);
            //Retrieve our filter queries
            var fqs = $("tr[class=' search-filter used-filter hidden']");
            for(var j = 0; j < fqs.length; j ++){
            	var selectedRow = fqs[j];
            	var filterType = $("select[name^='filtertype_']", selectedRow);
            	var filterOpr = $("select[name^='filter_relational_operator_']", selectedRow);
            	var filterVal = $("input[name^='filter_']", selectedRow);
                Manager.store.addByValue(filterType.attr('name'), filterType.val());
                Manager.store.addByValue(filterOpr.attr('name'), filterOpr.val());
                Manager.store.addByValue(filterVal.attr('name'), filterVal.val());
            }
            Manager.store.addByValue('facet.sort', 'count');

            var params = {
                facet: true,
                'facet.field': facetFields,
                'facet.limit': 20,
                'facet.mincount': 1,
                'f.topics.facet.limit': 50,
                'json.nl': 'map'
            };
            for (var name in params) {
                Manager.store.addByValue(name, params[name]);
            }

            //Attempt to add our scope !
            var scope = $("input[name='discovery-json-scope']").val();
            if(scope != undefined){
                Manager.store.addByValue("scope", scope);
            }
            Manager.doRequest();
        });
    });
})(jQuery);
