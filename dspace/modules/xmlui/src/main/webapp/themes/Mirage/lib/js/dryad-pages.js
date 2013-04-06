
/* JS behaviors for all Dryad pages */
jQuery(document).ready(function() {

    // If the page has separate sidebar boxes, try to align the topmost
    // box with the first "primary" box on the page.
    var topMainBox = $('#ds-body .primary:eq(0)');
    var topSidebarBox = $('#ds-options .simple-box:eq(0)');
    if (topMainBox.length && topSidebarBox.length) {
        var mainPageBoxTop = topMainBox.offset().top;
        var sidebarBoxTop = topSidebarBox.offset().top;
        var boxNudge = mainPageBoxTop - sidebarBoxTop;
        // ASSUMES that #ds-option has no padding-top!
        $('#ds-options').css('padding-top', boxNudge+"px");
    }

    jQuery('span.field-help').each(function(entry){
        jQuery(jQuery('span.field-help')[entry]).html(jQuery(jQuery('span.field-help')[entry]).text());
    });

    jQuery("#aspect_discovery_SimpleSearch_item_search-filter-list").css("display","none");

    jQuery("#advanced-search").click(function(){
        jQuery("#aspect_discovery_SimpleSearch_item_search-filter-list").toggle();
    });

    jQuery('#main-menu ul.sf-menu')
        .supersubs({
            // all numeric properties are in em
            minWidth: 12,
            maxWidth: 24,
            extraWidth: 1
        })
        .superfish({
            // make the menu snappy (fast+simple animation)
            delay: 0,
            animation: {
                //height: 'show',
                opacity: 'show'
            },
            speed: 0, 
            disableHI: true     // remove menu delay (from hoverIntent)
        })
        .supposition();

    // General support for simple tabs (styled as buttons) in all pages
    // NOTE: This logic supports multiple sets of tabs on a page.
    var jQuerytabButtons = jQuery('.tab-buttons a');
    jQuerytabButtons.unbind('click').click(function() {
        // highlight this button and show its panel
        jQuery(this).addClass('selected');
        var jQuerypanel = jQuery(jQuery(this).attr('href'));
        jQuerypanel.show();
        // dim others and hide their panels
        jQuery(this).siblings().each(function() {
            jQuery(this).removeClass('selected');
            var jQuerypanel = jQuery(jQuery(this).attr('href'));
            jQuerypanel.hide();
        });
        return false;
    });
    // CLick the first (default) tab in each set
    jQuery('.tab-buttons a:first-child').click();

});

/* JS behaviors for Home page only */
jQuery(document).ready(function() {
    // main carousel at top of homepage
    jQuery('#dryad-home-carousel .bxslider').bxSlider({
        auto: true,
        autoHover: true,
        pause: 6000,  // in ms
        speed: 500,   // ms for slide transition
        mode: 'fade',  // can be 'horizontal', 'vertical', 'fade'
        controls: false,
        autoControls: false,
        autoControlsCombine: true
    });

});

/* Membership Form page only */
jQuery(document).ready(function() {
    var $currencySelector = $('select[name=org_annual_revenue_currency]');
    if ($currencySelector.length === 1) {
        var amountsByCurrency = {
            'USD': {
                revenueThreshold: '10 million US Dollars',
                smallOrgFee: 'USD$1,000',
                largeOrgFee: 'USD$5,000'
            },
            'EUR': {
                revenueThreshold: '7.8 million Euros',
                smallOrgFee: '€780',
                largeOrgFee: '€3900'
            },
            'GBP': {
                revenueThreshold: '6.6 million GB Pounds',
                smallOrgFee: '£660',
                largeOrgFee: '£3300'
            },
            'CAD': {
                revenueThreshold: '10 million Canadian Dollars',
                smallOrgFee: 'CAD$1,000',
                largeOrgFee: 'CAD$5,000'
            },
            'JPY': {
                revenueThreshold: '950 billion Japanese Yen',
                smallOrgFee: '¥95,000',
                largeOrgFee: '¥475,000'
            },
            'AUD': {
                revenueThreshold: '9.6 million Australian Dollars',
                smallOrgFee: 'AUD$960',
                largeOrgFee: 'AUD$4,800'
            }
        };

        function showPreferredCurrency(currencyCode) {
            // EXAMPLE: showPreferredCurrency('GBP');
            revenueThreshold = amountsByCurrency[currencyCode].revenueThreshold;
            smallOrgFee = amountsByCurrency[currencyCode].smallOrgFee;
            largeOrgFee = amountsByCurrency[currencyCode].largeOrgFee;
            // build and show display strings in the new currency
            var msgLessThan = 'Less than {THRESHOLD} per year (annual membership fee {SMALL_ORG_FEE})'
                .replace('{THRESHOLD}', revenueThreshold)
                .replace('{SMALL_ORG_FEE}', smallOrgFee);
            $('#msg-less_than_10_million').html( msgLessThan );
            var msgGreaterThan = 'Greater than {THRESHOLD} per year (annual membership fee {LARGE_ORG_FEE})'
                .replace('{THRESHOLD}', revenueThreshold)
                .replace('{LARGE_ORG_FEE}', largeOrgFee);
            $('#msg-greater_than_10_million').html( msgGreaterThan );
        }

        // choosing a currency should modify the displayed org-revenue threshold and fees
        $currencySelector.unbind('change').change(function() {
            showPreferredCurrency( $(this).val() );
        });
        // show initial values in USD (don't rely on i18n-message text!)
        showPreferredCurrency('USD');
    }
});

/* JS behaviors for FAQ page only */
// Enable expanding FAQ via JS (else jump to answers) TODO-FAQ
jQuery(document).ready(function() {
        if (jQuery('.faq-answers').length === 0) {
            // no FAQ on this page, never mind
            return;
        }

        var qLinkCloseText = 'Close';

        // hide all answers (we'll bring them up as needed)
        jQuery('.faq-answers').hide();
        var questionBlock = jQuery('.faq-questions');

        // strip links from section headings
        questionBlock.find('h2 a').each(function() {
            jQuery(this).replaceWith(jQuery(this).html());
        });

        // wire remaining links to toggle w/ answer panel
        questionBlock.find('a').unbind('click').click(function() {
            var qLink = jQuery(this);
            var qListItem = qLink.closest('li');
            // strip off preceding '#' and complete URL, if found
            var aHref = qLink.attr('href');
            if (typeof(aHref) === 'string') {
                aHref = aHref.split('#')[1];
            } else {
                console.log('No HREF found for this link:\n'+ qLink.text());
                return false;
            }
            if (qLink.text() === qLinkCloseText) {
                // hide answer and restore link question
                qLink.html( unescape(qLink.attr('full-question')) );
                qLink.removeClass('question-closer');
                qListItem.find('.answer-panel').remove();

            } else {
                // hide link question and show the matching answer
                qLink.attr('full-question', escape(qLink.html()) );
                qLink.text( qLinkCloseText );
                qLink.addClass('question-closer');
                qListItem.find('.answer-panel').remove(); // just in case
                qListItem.append('<div class="answer-panel"></div>');
                // find and copy the named answer below
                /// IF well-organized in DIVs: qAnswer = jQuery('#'+ aHref).clone(false);
                qAnswer = jQuery('#'+ aHref).nextUntil('h2[id]').andSelf().clone(false);
                qListItem.find('.answer-panel').append( qAnswer );
            }
            return false;
        });
        
        // add 'Open/Close All Answers' trigger
        questionBlock.prepend('<a id="all-faq-toggle" href="#" style="float: right;">Open All Answers</a>');
        jQuery('#all-faq-toggle').unbind('click').click(function() {
            var toggle = jQuery(this);
            if (toggle.text().indexOf('Open') > -1) {
                // open all answers and update label
                questionBlock.find('li > a').each(function() {
                    if (jQuery(this).text() !== qLinkCloseText) {
                        jQuery(this).click();
                    }
                });
                toggle.text('Close All Answers');
            } else {
                // close all answers and update label
                questionBlock.find('li > a').each(function() {
                    if (jQuery(this).text() === qLinkCloseText) {
                        jQuery(this).click();
                    }
                });
                toggle.text('Open All Answers');
            }
        });

});

