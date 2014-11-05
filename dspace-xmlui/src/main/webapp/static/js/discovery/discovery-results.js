/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {

    /**
     * Function ensures that all the links clicked in our results pass through the internal logging mechanism
     */
    $(document).ready(function() {
        //Retrieve all links with handles attached (comm/coll/item links)
        var urls = $('div#aspect_discovery_SimpleSearch_div_search-results').find('a');

        urls.click(function(){
            var $this = $(this);
            //Instead of redirecting us to the page, first send us to the statistics logger
            //By doing this we ensure that we register the query to the result
            var form = $('form#aspect_discovery_SimpleSearch_div_main-form');
            var saved_action = form.attr('action');
            form.attr('action', $this.attr('href'));
            //Manipulate the fq boxes to all switch to query since the logging doesn't take into account filter queries
            form.find('input[name="fq"]').attr('name', 'query');
            form.submit();
            //restore the original action, that way, if you use the browser's back button later on, the form still works as it should.
            form.attr('action', saved_action);
            return false;
        });

    });

})(jQuery);
