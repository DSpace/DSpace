/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {
    $(document).ready(function () {
        var initialMinimalBiography, initialFullBiography, aditionalContent, detailsWrapper, showButton, hideButton, initHeight, buttons, stats;
        detailsWrapper = $('.author-details-wrapper');
        initialFullBiography = $('.author-biography-full');
        initialMinimalBiography = $('.author-biography-minimal');
        buttons = $('.author-details-button');
        showButton = $('#author-details-show-button');
        hideButton = $('#author-details-hide-button');
        stats =$('.discovery-stats-section-wrapper');
        aditionalContent = $('.author-details-full');

        initHeight = detailsWrapper.outerHeight();
        showButton.click(function (event) {
            showButton.hide();
            hideButton.removeClass('hidden').show();
            var content;
            event.preventDefault();
            content = initialFullBiography.html();
            initialFullBiography.html(initialMinimalBiography.html());
            detailsWrapper.css('max-height', initHeight);
            initialMinimalBiography.html(content);
            aditionalContent.removeClass('hidden').show();
            detailsWrapper.animate({"max-height": $(window).height()}, 200, function () {
                detailsWrapper.removeAttr('style');
            });
        })

        hideButton.click(function (event) {
            showButton.show();
            hideButton.hide();
            var content;
            event.preventDefault();
            content = initialFullBiography.html();
            initialFullBiography.html(initialMinimalBiography.html());
            buttons.show();
            $(buttons[0]).hide();
            detailsWrapper.css('max-height', detailsWrapper.outerHeight());
            aditionalContent.hide();
            detailsWrapper.animate({"max-height": initHeight}, 200, function () {
                initialMinimalBiography.html(content);
                detailsWrapper.removeAttr('style');

            });

        })


        $('button.show-stats-filter').click(function (event) {
            event.preventDefault();
            $(this).mouseout();
            if(stats.hasClass('hidden')){
                stats.hide().removeClass('hidden').slideDown(300);
            }else{
                stats.slideUp(150, function() {
                    $(this).addClass('hidden').removeAttr('style');
                });
            }
        });

    })
})(jQuery);
