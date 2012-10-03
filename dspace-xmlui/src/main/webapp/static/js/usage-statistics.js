/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {

    /**
     * Function ensures that when a new time filter is selected the form is submitted
     */
    $(document).ready(function() {
        $('select[name="time_filter"]').change(function(){
            $(this).parents('form:first').submit();

        });

    });
})(jQuery);
