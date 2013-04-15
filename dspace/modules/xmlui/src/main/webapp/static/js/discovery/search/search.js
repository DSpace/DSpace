var Manager;
var query;
var defaultFacets = new Array();

(function (jQuery) {
    jQuery(function () {
        var searchUrl = jQuery("input[name='solr-search-url']").val();
        Manager = new AjaxSolr.Manager({
            solrUrl: searchUrl
        });

        //Retrieve our filterSelect, which contains all the types to be sorted on
        //var filterSelect = jQuery("select[id='aspect_discovery_SimpleSearch_field_filtertype']");
        //Get our filters
        //var filterOptions = filterSelect.find('#aspect_discovery_SimpleSearch_field_filtertype option');
        //Get all the
        //for (var index = 1; index < filterOptions.length; index++){
        //this is the right code to get the options value in jquery 1.9.1
        var index = 0;
        jQuery('#aspect_discovery_SimpleSearch_field_filtertype option').each(function(){
            //We skip the first one (for the moment)
            defaultFacets[index] = jQuery(this).val();//filterOptions[index].value;
            index++;
        });

        var widget = Manager.addWidget(new AjaxSolr.AutocompleteWidget({
            id: 'text',
            target: 'li#aspect_discovery_SimpleSearch_item_search-filter-list',
            field: 'allText',
            fields: defaultFacets
        }));

        Manager.init();

        query = jQuery('input#aspect_discovery_SimpleSearch_field_query').val();
        if(query == '')
            query = '*:*';

        Manager.store.addByValue('q', query);
        //Retrieve our filter queries
        var fqs = jQuery("input[name='fq']");
        for(var j = 0; j < fqs.length; j ++){
            Manager.store.addByValue('fq', jQuery(fqs[j]).val() + '*');
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
        Manager.doRequest();





        filterSelect.change(function() {
//            TODO: this is dirty, but with lack of time the best I could do
            var oldInput = jQuery('input#aspect_discovery_SimpleSearch_field_filter');
            var newInput = oldInput.clone(false);

//            newInput.val(oldInput.val());
            newInput.appendTo(oldInput.parent());
            oldInput.remove();

            //Remove any results lists we may still have standing
            jQuery("div#discovery_autocomplete_div").remove();
            //Put the field in which our facet is going to facet into the widget
            var facetFields;

            if(jQuery(this).val() != '*'){
                facetFields = [jQuery(this).val()];
            } else {
                facetFields = defaultFacets;
            }

            Manager.widgets.text.fields = facetFields;
            Manager.initialized = false;

            Manager.init();
//TODO: does this need to happen twice ?
            Manager.store.addByValue('q', query);
            //Retrieve our filter queries
            var fqs = jQuery("input[name='fq']");
            for(var j = 0; j < fqs.length; j ++){
                Manager.store.addByValue('fq', jQuery(fqs[j]).val() + '*');
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

            Manager.doRequest();
        });


    });
})(jQuery);
