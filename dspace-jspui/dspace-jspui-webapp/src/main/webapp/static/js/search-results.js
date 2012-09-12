/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
$.noConflict();
(function ($) {

    /**
     * Function ensures that all the links clicked in our results pass through the internal logging mechanism
     */
    $(document).ready(function() {


        //Retrieve all links with handles attached (comm/coll/item links)
        var urls = $('div#search-results-division').find('a');

        urls.click(function(){
            var $this = $(this);
            //Instead of redirecting us to the page, first send us to the statistics logger
            //By doing this we ensure that we register the query to the result
            var form = $('form#dso-display');
            form.find('input[name="redirectUrl"]').val($this.attr('href'));
            form.submit();
            return false;
        });
    });


})(jQuery);
