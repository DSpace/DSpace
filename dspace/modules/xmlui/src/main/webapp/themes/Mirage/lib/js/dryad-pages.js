
/* JS behaviors for all Dryad pages */
jQuery(document).ready(function() {
    var modal =jQuery("#aspect_eperson_TermsOfService_div_modal-content").val();
    if(modal === undefined)
    {

    }
    else{
        initTermsOfService();
    }

    jQuery('.label-mark').tooltip();
    if(jQuery('#aspect_paymentsystem_ShoppingCartTransformer_field_voucher').length>0){
        shoeShoppingCartWaiver(null);
    }
    jQuery('#aspect_submission_workflow_WorkflowTransformer_field_skip_payment').css('display','none');
    jQuery('#aspect_submission_submit_CheckoutStep_field_skip_payment').css('display','none');
    //if there is error in generate the paypal form or payment is 0 enable the skip button
    var showButton = jQuery('input[name=show_button]');
    if(showButton!='undefined'&&showButton!=null&&showButton.length>0){
        var buttonId = "#aspect_submission_submit_CheckoutStep_field_skip_payment";
        jQuery(buttonId).show();
        jQuery(buttonId).val(showButton.val());
        buttonId = "#aspect_submission_workflow_WorkflowTransformer_field_skip_payment";
        jQuery(buttonId).show();
        jQuery(buttonId).val(showButton.val());
    }



/* If the page has separate sidebar boxes, try to align the topmost
* box with the first topmost element on the page, preferably:
*  > a button-bar for intra-page navigation
*  > a main page title
*  > a publication-header box
*  > a featured image
*  > or any .primary box
*/
    var topMainPageElement = jQuery('#ds-body .tab-buttons, #ds-body h1, #ds-body .publication-header, #ds-body .featured-image, #ds-body .primary').eq(0);
    var topSidebarBox = jQuery('#ds-options .simple-box:eq(0)');
    if (topMainPageElement.length && topSidebarBox.length) {
        var mainPageTop = topMainPageElement.offset().top;
        var sidebarBoxTop = topSidebarBox.offset().top;
        var boxNudge = mainPageTop - sidebarBoxTop;
        // ASSUMES that #ds-option has no padding-top!
        jQuery('#ds-options').css('padding-top', boxNudge+"px");
    }

    jQuery('span.field-help').each(function(entry){
        jQuery(jQuery('span.field-help')[entry]).html(jQuery(jQuery('span.field-help')[entry]).text());
    });

    if(document.URL.indexOf("#advanced")<0){
        jQuery("#aspect_discovery_SimpleSearch_item_search-filter-list").hide();
    }

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

    // General support for simple tabs (styled as buttons)
    // NOTE: This logic supports multiple sets of tabs on a page.
    // NOTE: For now, we're only using this on the Home page!
    if (jQuery('#aspect_discovery_RecentlyAdded_div_Home').length === 1) {
        var jQuerytabButtons = jQuery('.tab-buttons a');
        jQuerytabButtons.unbind('click').click(function() {

            //if click on browse by author or journal redirect
            if(jQuery(this).attr('href').indexOf("#by_")>=0)
            {
                var url ="/search-filter?query=&field=dc.contributor.author_filter&fq=location:l2";
                $(location).attr('href',url);
            }

            //if click on browse by author or journal redirect
            if(jQuery(this).attr('href').indexOf("#by_journal")>=0)
            {
                var url ="/search-filter?query=&field=prism.publicationName_filter&fq=location:l2";
                $(location).attr('href',url);
            }

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
        if(document.URL.indexOf("dc.contributor.author_filter")>=0||jQuery(".choose_browse_by").html()=="dc.contributor.author_filter")
        {
            jQuery('#by_author').click();
            window.location.href = '#by_author';

        }
        else if(document.URL.indexOf("prism.publicationName_filter")>=0||jQuery(".choose_browse_by").html()=="prism.publicationName_filter")
        {
            jQuery('#by_journal').click();
            window.location.href = '#by_journal';
        }
        else
        {
            jQuery('.tab-buttons a:first-child').click();
        }

    }

});

/* JS behaviors for Home page only */
jQuery(document).ready(function() {
    // Show any slides that should be public, based on the publication date for each.
    // By default, this is based on today's date. Upcoming slides can be previewed using the browser's query-string:
    //   EXAMPLE: http://www.datadryad.org/?date=2013-06-02
    // NOTE that the date is in the format YEAR-MONTH-DAY, to the date above is June 2, 2013
    var displayDate = jQuery.datepicker.formatDate('yy-mm-dd', new Date());   // "2013-09-13"
        // this default behavior depends on jQuery UI's datepicker script
    var qsTest = window.location.search.split('?');
    if ((qsTest.length === 2) && (qsTest[1].indexOf('date=') === 0)) {
        // replace today's date with the one specified
        displayDate = qsTest[1].split('=')[1];
    }
    // show any slides that have a publication date of today or earlier
    jQuery('#dryad-home-carousel div.bxslider div').each( function() {
        var $slide = jQuery(this);
        var itsPubDate = $slide.find('.publication-date').text();
        if ((itsPubDate !== '') && (displayDate >= itsPubDate)) {
            $slide.show();
        } else {
            $slide.remove();
        }
    });

    // activate main carousel at top of homepage
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

/* JS behavior (currency conversion) for Membership Form only */
jQuery(document).ready(function() {
    var $currencySelector = jQuery('select[name=org_annual_revenue_currency]');
    if ($currencySelector.length === 1) {
        var amountsByCurrency = {
            'USD': {
                revenueThreshold: '10 million US Dollars',
                smallOrgFee: 'USD$1,000',
                largeOrgFee: 'USD$5,000'
            },
            'EUR': {
                revenueThreshold: '7.8 million Euros',
                smallOrgFee: '&#128;780',  // €
                largeOrgFee: '&#128;3900'
            },
            'GBP': {
                revenueThreshold: '6.6 million GB Pounds',
                smallOrgFee: '&#163;660',  // £
                largeOrgFee: '&#163;3300'
            },
            'CAD': {
                revenueThreshold: '10 million Canadian Dollars',
                smallOrgFee: 'CAD$1,000',
                largeOrgFee: 'CAD$5,000'
            },
            'JPY': {
                revenueThreshold: '950 billion Japanese Yen',
                smallOrgFee: '&#165;95,000', // ¥
                largeOrgFee: '&#165;475,000'
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
            jQuery('#msg-less_than_10_million').html( msgLessThan );
            var msgGreaterThan = 'Greater than {THRESHOLD} per year (annual membership fee {LARGE_ORG_FEE})'
                .replace('{THRESHOLD}', revenueThreshold)
                .replace('{LARGE_ORG_FEE}', largeOrgFee);
           jQuery('#msg-greater_than_10_million').html( msgGreaterThan );
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
// Enable expanding FAQ via JS (else jump to answers)
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
            // transfer its href to the heading, to support inbound links
            var targetID = jQuery(this).attr('href').split('#')[1];
            jQuery('#'+targetID).remove();
            var heading = jQuery(this).parent();
            heading.attr('id', targetID);
            // strip the hyperlink, to leave a simple header
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

        // IF we've just entered the page with a #fragment specified, try to go to it
        var inboundAnswerID = window.location.hash;
        if (inboundAnswerID !== '') {
            var targetAnswer = jQuery( inboundAnswerID ); // eg, '#payment-plans'
            // IF it's a valid target, toggle this answer open and jump to it
            if (targetAnswer.length === 1) {
                questionBlock.find('li a[href$='+ inboundAnswerID +']').click();
                // NOTE: there are now *two* elements with this ID, but the visible clone
                // will always respond to a simple ID selector because it's first in DOM.
                jQuery(document).scrollTop( jQuery( inboundAnswerID ).offset().top );
            }
        }

});
function updateOrder(){
    var transactionId = document.getElementsByName("transactionId")[0].value;
    var country =document.getElementsByName("country")[0].value;
    var currency =document.getElementsByName("currency")[0].value;
    var journal =jQuery("#aspect_submission_StepTransformer_field_prism_publicationName").val();
    if(journal=="undefined")
    {
        journal = "";
    }
    var voucher = jQuery("#aspect_paymentsystem_ShoppingCartTransformer_field_voucher").val();
    var baseUrl = document.getElementsByName("baseUrl")[0].value;
    var searchUrl =baseUrl+"/JSON/transaction/shoppingcart?country="+country+"&currency="+currency+"&transactionId="+transactionId+"&journal="+journal+"&voucher="+voucher;
    jQuery.ajax({
        url: searchUrl,
        beforeSend: function ( xhr ) {
            xhr.overrideMimeType("text/plain; charset=x-user-defined");
        }
    }).done(function ( data ) {
            obj = jQuery.parseJSON(data);
            showShoppingCartMessage(obj);
            shoeShoppingCartWaiver(obj);
        });
}

/* JS behavior (currency conversion) for Pricing and Integrated Journal pages */
jQuery(document).ready(function() {
    var $currencySelector = jQuery('select[name=displayed-currency]');
    // in this case, we just want to update a few displayed values wherever
    // they appear (look for SPANs with marker classes)
    if ($currencySelector.length === 1) {
        var amountsByCurrency = {
            'USD': {
                memberDPC_voucher: '$65',
                nonMemberDPC_voucher: '$70',
                memberDPC_deferred: '$70',
                nonMemberDPC_deferred: '$75',
                memberDPC_subscription: '$25',
                nonMemberDPC_subscription: '$30',
                DPC_pay_on_submission: '$80',  // is this the "base charge"?
                notIntegratedJournalFee: '$10',
                DPC_pay_on_submission_nonintegrated: '$90',  // total of two previous lines
                excessDataStorageFee_first_GB: '$15',
                excessDataStorageFee_per_additional_GB: '$10'
            },
            'EUR': {
                memberDPC_voucher: '&#128;49',  // €
                nonMemberDPC_voucher: '&#128;53',
                memberDPC_deferred: '&#128;53',
                nonMemberDPC_deferred: '&#128;56',
                memberDPC_subscription: '&#128;19',
                nonMemberDPC_subscription: '&#128;23',
                DPC_pay_on_submission: '&#128;60',
                notIntegratedJournalFee: '&#128;8',
                DPC_pay_on_submission_nonintegrated: '&#128;68',  // total of two previous lines
                excessDataStorageFee_first_GB: '&#128;11',
                excessDataStorageFee_per_additional_GB: '&#128;8'
            },
            'GBP': {
                memberDPC_voucher: '&#163;42',  // £
                nonMemberDPC_voucher: '&#163;45',
                memberDPC_deferred: '&#163;45',
                nonMemberDPC_deferred: '&#163;48',
                memberDPC_subscription: '&#163;16',
                nonMemberDPC_subscription: '&#163;20',
                DPC_pay_on_submission: '&#163;53',
                notIntegratedJournalFee: '&#163;7',
                DPC_pay_on_submission_nonintegrated: '&#163;60',  // total of two previous lines
                excessDataStorageFee_first_GB: '&#163;10',
                excessDataStorageFee_per_additional_GB: '&#163;7'
            },
            'CAD': {
                memberDPC_voucher: '$67',
                nonMemberDPC_voucher: '$73',
                memberDPC_deferred: '$73',
                nonMemberDPC_deferred: '$77',
                memberDPC_subscription: '$26',
                nonMemberDPC_subscription: '$30',
                DPC_pay_on_submission: '$83',
                notIntegratedJournalFee: '$10',
                DPC_pay_on_submission_nonintegrated: '$93',  // total of two previous lines
                excessDataStorageFee_first_GB: '$15',
                excessDataStorageFee_per_additional_GB: '$10'
            },
            'JPY': {
                memberDPC_voucher: '&#165;6370',  // ¥
                nonMemberDPC_voucher: '&#165;6860',
                memberDPC_deferred: '&#165;6860',
                nonMemberDPC_deferred: '&#165;7350',
                memberDPC_subscription: '&#165;2450',
                nonMemberDPC_subscription: '&#165;2940',
                DPC_pay_on_submission: '&#165;7840',
                notIntegratedJournalFee: '&#165;980',
                DPC_pay_on_submission_nonintegrated: '&#165;8820',  // total of two previous lines
                excessDataStorageFee_first_GB: '&#165;1470',
                excessDataStorageFee_per_additional_GB: '&#165;980'
            },
            'AUD': {
                memberDPC_voucher: '$72',
                nonMemberDPC_voucher: '$78',
                memberDPC_deferred: '$78',
                nonMemberDPC_deferred: '$84',
                memberDPC_subscription: '$28',
                nonMemberDPC_subscription: '$33',
                DPC_pay_on_submission: '$89',
                notIntegratedJournalFee: '$11',
                DPC_pay_on_submission_nonintegrated: '$100',  // total of two previous lines
                excessDataStorageFee_first_GB: '$17',
                excessDataStorageFee_per_additional_GB: '$11'
            }
        };

        function showPreferredCurrency(currencyCode) {
            // EXAMPLE: showPreferredCurrency('GBP');
            jQuery('.msg-memberDPC_voucher').html( amountsByCurrency[currencyCode].memberDPC_voucher );
            jQuery('.msg-nonMemberDPC_voucher').html( amountsByCurrency[currencyCode].nonMemberDPC_voucher );
            jQuery('.msg-memberDPC_deferred').html( amountsByCurrency[currencyCode].memberDPC_deferred );
            jQuery('.msg-nonMemberDPC_deferred').html( amountsByCurrency[currencyCode].nonMemberDPC_deferred );
            jQuery('.msg-memberDPC_subscription').html( amountsByCurrency[currencyCode].memberDPC_subscription );
            jQuery('.msg-nonMemberDPC_subscription').html( amountsByCurrency[currencyCode].nonMemberDPC_subscription );
            jQuery('.msg-DPC_pay_on_submission').html( amountsByCurrency[currencyCode].DPC_pay_on_submission );
            jQuery('.msg-notIntegratedJournalFee').html( amountsByCurrency[currencyCode].notIntegratedJournalFee );
            jQuery('.msg-DPC_pay_on_submission_nonintegrated').html( amountsByCurrency[currencyCode].DPC_pay_on_submission_nonintegrated );
            jQuery('.msg-excessDataStorageFee_first_GB').html( amountsByCurrency[currencyCode].excessDataStorageFee_first_GB );
            jQuery('.msg-excessDataStorageFee_per_additional_GB').html( amountsByCurrency[currencyCode].excessDataStorageFee_per_additional_GB );
        }

        // choosing a currency should modify the displayed org-revenue threshold and fees
        $currencySelector.unbind('change').change(function() {
            showPreferredCurrency( $(this).val() );
        });

        // show initial values in USD (don't rely on i18n-message text!)
        showPreferredCurrency('USD');
    }
});

function initTermsOfService() {

    $("#aspect_eperson_TermsOfService_div_background").toggleClass("active");
    $("#aspect_eperson_TermsOfService_div_modal-content").toggleClass("active");


    $("#aspect_eperson_TermsOfService_field_submit").click(function(){
        $("#aspect_eperson_TermsOfService_div_background").toggleClass("active");
        $("#aspect_eperson_TermsOfService_div_modal-content").toggleClass("active");

        var popup = document.getElementById("aspect_eperson_TermsOfService_div_modal-content");
        popup.style.display = 'none';

    });

}

function removeVoucher(){
    var transactionId = document.getElementsByName("transactionId")[0].value;
    var country =document.getElementsByName("country")[0].value;
    var currency =document.getElementsByName("currency")[0].value;
    var journal =jQuery("#aspect_submission_StepTransformer_field_prism_publicationName").val();
    if(journal=="undefined")
    {
        journal = "";
    }
    var voucher = "";
    var baseUrl = document.getElementsByName("baseUrl")[0].value;
    var searchUrl =baseUrl+"/JSON/transaction/shoppingcart?country="+country+"&currency="+currency+"&transactionId="+transactionId+"&journal="+journal+"&voucher="+voucher;
    jQuery.ajax({
        url: searchUrl,
        beforeSend: function ( xhr ) {
            xhr.overrideMimeType("text/plain; charset=x-user-defined");
        }
    }).done(function ( data ) {
            obj = jQuery.parseJSON(data);

            showShoppingCartMessage(obj);
            shoeShoppingCartWaiver(obj);


        });
}


function removeCountry(){
    var transactionId = document.getElementsByName("transactionId")[0].value;
    var country ="";
    var currency =document.getElementsByName("currency")[0].value;
    var journal =jQuery("#aspect_submission_StepTransformer_field_prism_publicationName").val();
    if(journal=="undefined")
    {
        journal = "";
    }
    var voucher = jQuery("#aspect_paymentsystem_ShoppingCartTransformer_field_voucher").val();
    var baseUrl = document.getElementsByName("baseUrl")[0].value;
    var searchUrl =baseUrl+"/JSON/transaction/shoppingcart?country="+country+"&currency="+currency+"&transactionId="+transactionId+"&journal="+journal+"&voucher="+voucher;
    jQuery.ajax({
        url: searchUrl,
        beforeSend: function ( xhr ) {
            xhr.overrideMimeType("text/plain; charset=x-user-defined");
        }
    }).done(function ( data ) {
            obj = jQuery.parseJSON(data);

            showShoppingCartMessage(obj);
            shoeShoppingCartWaiver(obj);

        });
}
function showShoppingCartMessage(obj){
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_price div').html(obj.price);
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_total div').html(obj.total);
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_surcharge div').html(obj.surcharge);
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_no-integret div').html(obj.noIntegrateFee);
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_errorMessage').html(obj.errorMessage);
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_waiver-info').html(obj.waiverMessage);
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_payer div').html(obj.payer);
}
function shoeShoppingCartWaiver(obj){
    if(obj==null)
    {
        //initial
        if(jQuery('#aspect_paymentsystem_ShoppingCartTransformer_field_voucher').html().length==0&&jQuery('#aspect_paymentsystem_ShoppingCartTransformer_field_voucher').val().length==0&&jQuery(':input[name="country"]').val()==0)
        {
            showEmptyVoucher();
            showEmptyCountry();
        }
        else
        {
            if(jQuery(':input[name="country"]').val()!=null)
            {
                hideVoucher();
                showRemoveCountry(null);
            }
            if(jQuery('#aspect_paymentsystem_ShoppingCartTransformer_field_voucher').html().length!=0||jQuery('#aspect_paymentsystem_ShoppingCartTransformer_field_voucher').val().length!=0)
            {
                hideCountry();
                showRemoveVoucher(null);
            }
        }
    }
    else
    {
        if(obj.voucher==''&&obj.country=='')
        {

            showEmptyVoucher();
            showEmptyCountry();

        }
        else
        {
            if(obj.voucher!='')
            {
                showRemoveVoucher(obj);
            }
            else
            {
                hideVoucher();
            }
            if(obj.country!='')
            {
                showRemoveCountry(obj);
            }
            else
            {
                hideCountry();
            }

        }
    }
}

function showEmptyCountry(){
    console.log("show empty country");
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_country-list').show();
    jQuery(':input[name="country"]').show();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_country-list div').show();
    jQuery(':input[name="country"]').val('');
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-country a').html('Remove Country : ');
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-country a').hide();
}
function showEmptyVoucher(){
    console.log("show empty voucher");
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_voucher-list').show();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-voucher a').html('Remove Voucher : ');
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_voucher-list div').show();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-voucher a').hide();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_field_voucher').val('');
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_field_voucher').html('');
}
function showRemoveCountry(obj){
    console.log("show country");
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_country-list').show();
    jQuery(':input[name="country"]').show();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_country-list div').hide();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-country a').show();
    if(obj!=null)
    {
        jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-country a').html('Remove Country : '+obj.country);
    }

}
function showRemoveVoucher(obj){
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_voucher-list').show();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_voucher-list div').hide();
    jQuery(':input[name="country"]').hide();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-voucher a').show();
    if(obj!=null){
        jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-voucher a').html('Remove Voucher : '+obj.voucher);
        jQuery('#aspect_paymentsystem_ShoppingCartTransformer_field_voucher').html(obj.voucher);
        jQuery('#aspect_paymentsystem_ShoppingCartTransformer_field_voucher').val(obj.voucher);
    }
}
function hideCountry(){
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_country-list').hide();
    jQuery(':input[name="country"]').hide();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-country a').hide();
}
function hideVoucher(){
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_voucher-list').hide();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-voucher a').hide();
}

function updateCountry(){
    var transactionId = document.getElementsByName("transactionId")[0].value;
    var country =document.getElementsByName("country")[0].value;

    var voucher = jQuery("#aspect_paymentsystem_ShoppingCartTransformer_field_voucher").val();
    var baseUrl = document.getElementsByName("baseUrl")[0].value;
    var searchUrl =baseUrl+"/JSON/transaction/shoppingcart?country="+country+"&transactionId="+transactionId;
    jQuery.ajax({
        url: searchUrl,
        beforeSend: function ( xhr ) {
            xhr.overrideMimeType("text/plain; charset=x-user-defined");
        }
    }).done(function ( data ) {
            obj = jQuery.parseJSON(data);
        });
}

//  JS behaviors for the "Describe Publication" page
//  /handle/*/*/submit/*.continue
//  These functions enable page-fragment reloading in place of 
//  full-page reloads for the metadata fields in the form
//  #aspect_submission_StepTransformer_div_submit-describe-publication
//  The redirect buttons ("Save & Exit" and "Continue to Describe Data")
//  still trigger a full page reload.
jQuery(document).ready(function(){
    var form_selector = '#aspect_submission_StepTransformer_div_submit-describe-publication';
    // update the part of the form associated with the input button that was clicked 
    // selector: string, jQuery selector to identify the li.ds-form-item element
    //      to be replaced by the update
    // data: string, HTML reponse from server (entire page)
    var update_form_fragment = function(selector,data) {
        var $result_doc;
        try {
            $result_doc = jQuery(document.createElement('div')).append(jQuery(data));
        } catch (e) {
            console.log('Error parsing result to Document: ' + e.toString());
            return;
        }
        // update DOM with fragment selected from response document 
        if ($result_doc.length > 0 && $result_doc.find(selector).length > 0) { 
            jQuery(selector).replaceWith($result_doc.find(selector));
            // refresh event bindings on updated page
            submit_describe_publication_binders();
        // on failure to isolate form, load entire response page, which is
        // likely to contain an error message
        } else {
            console.log('No handlable data from returned page.');
            jQuery('html').html(data);
        }
    };
    // need to save off the name of the submit button that was clicked, since
    // there is no way to retrieve that information on the form's submit event
    var clicked_btn_name;
    var watch_clicked = function(e) {
        clicked_btn_name = jQuery(e.target).attr('name');
        return true; // propogate default event (i.e., form submission)
    };
    // function to handle the submit event for the form
    var submit_describe_publication_onsubmit = function(e) {
        var $input      = jQuery('input[name="' + clicked_btn_name +'"]')   // the <input> triggering the submit
          , input_name  = clicked_btn_name                                  // input name: localize to this scope for the ajax call
          , $form       = jQuery(e.target)          // the entire describe-publication form
          , form_data   = $form.serializeArray()    // the form's data
          , success     = false                     // unsuccessful ajax call until we receive a 200
          , ajax_data                               // ajax data, for passing to the updater function
          , prevent_default = false;                // whether to continue with form submission/reload
        // undefine this variable, due to the flurry of onclick events raised by a button 
        // click. TODO: determine why multiple button onclick events are raised
        clicked_btn_name = undefined;
        if (input_name !== undefined) {
            // continue with full page reload for these two button click events
            if (input_name === 'submit_cancel' || input_name === 'submit_next') {
                return;
            // do page-fragment reload for other form submission clicks
            } else if (form_data.length > 0) {
                // jQuery does not add the submission button, which is expected in the 
                // request parameters by the DescribePublicationStep.java code; added here manually
                form_data.push({ name: input_name, value: $input.val() });
                prevent_default = true;
                $form.find('input').prop('disabled',true);
                try {
                    jQuery.ajax({
                          url     : $form.attr('action')
                        , data    : jQuery.param(form_data)
                        , method  : "POST"
                        , success : function(data,textStatus,jqXHR) {
                            if (jqXHR.status === 200) {
                                success   = true;
                                ajax_data = data;
                            } else {
                                console.log('Error: Form "submit-describe-publication" returned non-success status: ' + jsXHR.status);
                            }
                        }
                        , error : function(jqXHR,textStatus,errorThrown) {
                            console.log(textStatus);
                        }
                        , complete : function(jqXHR,textStatus) {
                            // update the page using data associated with the input the user selected
                            if (success === true) {
                                update_form_fragment(form_selector,ajax_data);
                            }
                        }
                    });
                } catch (e) {
                    console.log('Error: Form "submit-describe-publication" encountered AJAX error: ' + e.toString());
                }
            } else {
                console.log('Error: Form "submit-describe-publication" submitted with empty data.');
            }
        }
        // prevent default form-submit action (which triggers page reload)
        // TODO: remove this variable once the multiple-event-raising situation has been sorted out
        if (prevent_default) {
            e.preventDefault();
        }
    };
    // 
    var reorderAuthorOrderOptions = function($select, $row, $table, to) {
        // pull out author data rows, then reorder per user selection
        $row.remove();
        var rows = $table.find('tr.ds-author-input-row').remove();
        rows.splice(to-1, 0, $row[0]);
        // update the row's inputs and select/option data
        jQuery.each(rows, function(i,elt) {
            var $tr             = jQuery(elt)
              , $selected       = $tr.find('input[name="dc_contributor_author_selected"]')
              , $input_first    = $tr.find('input[name*="dc_contributor_author_last_"]')
              , $input_last     = $tr.find('input[name*="dc_contributor_author_first_"]')
              , ind             = (i+1).toString();
            $tr.find('select').val(ind);
            $selected.val(
                $selected.val().replace(new RegExp('_[0-9]+$'), ind)
            );
            $input_first.attr('name',
                $input_first.attr('name').replace(new RegExp('_[0-9]+$'), '_'.concat(ind))
            );
            $input_last.attr('name', 
                $input_last.attr('name').replace(new RegExp('_[0-9]+$'), '_'.concat(ind))
            );
        });
        $table.append(rows);
    };
    // event handler for the on-change event for an author's order changing
    var handleAuthorReorderEvent = function(evt) {
        var from    = jQuery(evt.target).data('prev')
          , to      = parseInt(evt.target.value)
          , $row    = jQuery(evt.target).closest('tr')
          , $table  = $row.closest('table');
        if (from !== to) {
            $table.next('.ds-update-button').removeAttr('disabled');
            reorderAuthorOrderOptions(jQuery(evt.target), $row, $table, to);
            submit_describe_publication_binders();
        }
    };
    // event handler for the author's Edit button click event
    var handleAuthorEdit = function(event) {
        var $row    = jQuery(event.target).closest('tr')
          , $table  = $row.closest('table')
          , $update = $table.next('.ds-update-button')
          , $hidden = $row.find('td.ds-author-input-col input[type="hidden"]')
          , $shown  = $row.find('span.ds-interpreted-field');
        // disable edit button
        jQuery(event.target).attr('disabled','disabled');
        // enable the order input and add event handler
        $row.find('select.ds-author-order-select').removeAttr('disabled');
        // show lastname/firstname input fields
        $hidden.attr('type','text');
        // enable Update button on a change event
        $hidden.bind('change keyup', function(evt) {
            $update.removeAttr('disabled');
        });
        // hide 'interpreted' span
        $shown.attr('hidden', 'hidden');
        event.preventDefault();
    };
    // event handler for the author's Delete button click event
    var handleAuthorDelete = function(event) {
        var $row    = jQuery(event.target).closest('tr')
          , $hidden = $row.find('input[name="dc_contributor_author_selected"]');
          clicked_btn_name = jQuery(event.target).attr('name');
          $hidden.removeAttr('disabled');
    };
    // these event handlers need to be registered any time the form is submitted, since the DOM is modified 
    var submit_describe_publication_binders = function() {
        jQuery(form_selector + ' input.ds-button-field').bind('click', watch_clicked);
        jQuery(form_selector).bind('submit', submit_describe_publication_onsubmit);
        jQuery('input.ds-edit-button').bind('click',handleAuthorEdit);
        jQuery('input.ds-delete-button').bind('click',handleAuthorDelete);
        // bind the onchange event to this function, and also store the current value of
        // the selected option, for use in updating the underlying input data
        jQuery('select.ds-author-order-select').each(function(i,elt) {
            jQuery(this).on('change', handleAuthorReorderEvent).data('prev',parseInt(this.value));
        });
    };
    submit_describe_publication_binders();
});
