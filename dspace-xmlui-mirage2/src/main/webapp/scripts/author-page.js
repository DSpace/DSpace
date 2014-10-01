/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {
    $(document).ready(function () {
        var initialMinimalContent, initialFullContent, detailsWrapper, showButton, hideButton, initHeight, buttons, stats;
        detailsWrapper = $('.author-details-wrapper');
        initialFullContent = $('.author-details-full');
        initialMinimalContent = $('.author-details-minimal');
        buttons = $('.author-details-button');
        showButton = $('#author-details-show-button');
        hideButton = $('#author-details-hide-button');
        stats =$('.discovery-stats-section-wrapper');

        initHeight = detailsWrapper.outerHeight();
        showButton.click(function (event) {
            showButton.hide();
            hideButton.removeClass('hidden').show();
            var content;
            event.preventDefault();
            content = initialFullContent.html();
            initialFullContent.html(initialMinimalContent.html());
            detailsWrapper.css('max-height', initHeight);
            initialMinimalContent.html(content);
            detailsWrapper.animate({"max-height": $(window).height()}, 200, function () {
                detailsWrapper.removeAttr('style');
            });
        })

        hideButton.click(function (event) {
            showButton.show();
            hideButton.hide();
            var content;
            event.preventDefault();
            content = initialFullContent.html();
            initialFullContent.html(initialMinimalContent.html());
            buttons.show();
            $(buttons[0]).hide();
            detailsWrapper.css('max-height', detailsWrapper.outerHeight());
            detailsWrapper.animate({"max-height": initHeight}, 200, function () {
                initialMinimalContent.html(content);
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
