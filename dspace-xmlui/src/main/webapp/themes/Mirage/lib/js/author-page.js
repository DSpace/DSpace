/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {
    $(document).ready(function () {
     var placeHolder, hideProfile, showProfile;

     placeHolder = $('.details.placeholder');
     hideProfile = placeHolder.html();
     showProfile = $('#AuthorProfileHide').html();
     showElements = $('.author-show');
     hideElements = $('.author-hide');
     placeHolder.click(showDetails);

     function showDetails(event) {
        event.preventDefault();

        if(hideElements.is(':hidden')) {
            placeHolder.html(hideProfile);
            hideElements.show();
            showElements.hide();
        }
        else
        {
            placeHolder.html(showProfile);
            hideElements.hide();
            showElements.show();
        }
        matchHeights();
     }
     matchHeights();
    } )

})(jQuery);
