
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

    if (jQuery('#dryadlab-module-list').length > 0) {
        // we're on the DryadLab home page (/pages/dryadlab)
        // loadDryadLabModuleFeed();
        activateModuleKeywords();
    }



/* If the page has separate sidebar boxes, try to align the topmost
* box with the first topmost element on the page, preferably:
*  > a button-bar for intra-page navigation
*  > a main page title
*  > a publication-header box
*  > a featured image
*  > or any .primary box
    var topMainPageElement = jQuery('#ds-body .tab-buttons, #ds-body h1, #ds-body .publication-header, #ds-body .featured-image, #ds-body .primary').eq(0);
    var topSidebarBox = jQuery('#ds-options .simple-box:eq(0)');
    if (topMainPageElement.length && topSidebarBox.length) {
        var mainPageTop = topMainPageElement.offset().top;
        var sidebarBoxTop = topSidebarBox.offset().top;
        var boxNudge = mainPageTop - sidebarBoxTop;
        // ASSUMES that #ds-option has no padding-top!
        jQuery('#ds-options').css('padding-top', boxNudge+"px");
    }
*/

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

        // add 'Open/Close all answers' trigger
        questionBlock.prepend('<a id="all-faq-toggle" href="#" style="float: right;">Open all answers</a>');
        jQuery('#all-faq-toggle').unbind('click').click(function() {
            var toggle = jQuery(this);
            if (toggle.text().indexOf('Open') > -1) {
                // open all answers and update label
                questionBlock.find('li > a').each(function() {
                    if (jQuery(this).text() !== qLinkCloseText) {
                        jQuery(this).click();
                    }
                });
                toggle.text('Close all answers');
            } else {
                // close all answers and update label
                questionBlock.find('li > a').each(function() {
                    if (jQuery(this).text() === qLinkCloseText) {
                        jQuery(this).click();
                    }
                });
                toggle.text('Open all answers');
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
    //console.log("show empty country");
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_country-list').show();
    jQuery(':input[name="country"]').show();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_country-list div').show();
    jQuery(':input[name="country"]').val('');
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-country a').html('Remove country : ');
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-country a').hide();
}
function showEmptyVoucher(){
    //console.log("show empty voucher");
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_voucher-list').show();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-voucher a').html('Remove voucher : ');
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
        jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-country a').html('Remove country : '+obj.country);
    }

}
function showRemoveVoucher(obj){
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_voucher-list').show();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_voucher-list div').hide();
    jQuery(':input[name="country"]').hide();
    jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-voucher a').show();
    if(obj!=null){
        jQuery('#aspect_paymentsystem_ShoppingCartTransformer_item_remove-voucher a').html('Remove voucher : '+obj.voucher);
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


/* Client-side behavior for DryadLab home page */

/*
function loadDryadLabModuleFeed() {
    console.log('START of loadDryadLabModuleFeed...');
    var moduleInfo = null;
    $.get(
        '/feed/atom_1.0/10255/dryad.7871',  // url (TODO: use root-relative URL instead of production!)
        function(data) {
            // 'data' should be an XML root element
            console.log("Got this feed data:");
            console.log(data);
            var howManyEntries = $('entry', data).length;
            console.log(howManyEntries +' entries found');

            // convert to HTML? or render and style as-is?
            var $listHolder = $('#dryadlab-module-list');
            $listHolder.empty();  // clear any stale junk or placeholders

            $('feed entry', data).each( function(i, entry) {
                // add an entry to our list (filters will apply later)
                var $entry = $(entry);
                var oddOrEven =  (i % 2) ? 'even' : 'odd';  // 0 is odd, 1 is even...
                // gather interesting bits from this entry's info
                var moduleDOI = $entry.find('id').text().split('/').slice(-2).join('/');
                    // eg, 'http://datadryad.org/resource/doi:10.5061/dryad.4gk47' ==> 'doi:10.5061/dryad.4gk47'
                var moduleID = 'module-'+ moduleDOI;
                var moduleURL = $entry.find('link').attr('href');
                var moduleTitle = $entry.find('title').html();
                var moduleAuthor = $entry.find('author name').html();
                    // TODO: allow for multiple authors here?
                var modulePublished = $entry.find('published').html(); // ISO dates?
                var moduleLastModified = $entry.find('updated').html();
                var moduleSummary = $entry.find('summary').html();
                // create formatted HTML for this entry
                var $entryListItem = $('<div id="'+ moduleID +'" class="ds-artifact-item '+ oddOrEven +'">');
                var $entryBlock = $('<div xmlns:xlink="http://www.w3.org/TR/xlink/" \
                                          xmlns:mods="http://www.loc.gov/mods/v3" \
                                          xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" \
                                          xmlns:dc="http://purl.org/dc/elements/1.1/" \
                                          xmlns:mets="http://www.loc.gov/METS/" \
                                          xmlns:dri="http://di.tamu.edu/DRI/1.0/" \
                                          style="padding: 6px;" class="artifact-description">');
                $entryListItem.append($entryBlock);
                var $entryLink = $('<a href="'+ moduleURL +'">');
                $entryBlock.append($entryLink);
                $entryLink.append('<span class="author">'+ moduleAuthor +'</span>');
                $entryLink.append('<span class="pub-date">'+ modulePublished +'</span>');
                $entryLink.append('<span class="artifact-title">'+ moduleTitle +'</span>');
                // build a formatted(?) summary and add it
                $entrySummary = $('<div class="dryadlab-module-summary">');
                $entrySummary.append( moduleSummary );
                $entryBlock.append($entrySummary);
                var moduleDOIasURL = 'https://doi.org/'+ moduleDOI.split(':').slice(-1);  // trim 'doi:' if found
                $entryBlock.append('<a href="'+ moduleDOIasURL +'" target="_blank">'+ moduleDOI +'</a>');
                // place the complete HTML for this entry
                $listHolder.append($entryListItem);
            });
        }
    );
    console.log('END of loadDryadLabModuleFeed');
}
*/

function updatedFilteredModuleList() {
    // apply any active filters to show only appropriate DryadLab modules
    var $moduleList = $('#dryadlab-module-list');
    // read the value for each filter (just once)
    var $standardsFilter = $('#filter-by-standard');
    var chosenStandard = $standardsFilter.val();
    var $gradeLevelFilter = $('#filter-by-grade-level');
    var chosenGradeLevel = $gradeLevelFilter.val();
    var $textFilter = $('#filter-by-text');
    // convert search text to LOWER CASE
    var searchText = $textFilter.val().toLowerCase();
    // walk the list and hide/show each module
    var oddOrEven = 'odd';
    $moduleList.find('li.ds-artifact-item').each(function(i) {
        var $module = $(this);
        // test against each filter in turn; must match all to appear!
        var matchesAllFilters = true;
        // convert searchable "fields" in each module's block to LOWER CASE
        var overview = $module.find('strong:contains(Overview)').parent().text().toLowerCase();
        var keywords = $module.find('strong:contains(Keywords)').parent().text().toLowerCase();
        var gradeLevels = $module.find('strong:contains(Level)').parent().text().toLowerCase();
        var standards = $module.find('strong:contains(Standards)').parent().text().toLowerCase();
        var duration = $module.find('strong:contains(Duration)').parent().text().toLowerCase();
        // combine all searchable text as LOWER CASE
        var searchableText = $module.find('span.title').text().toLowerCase();
        searchableText += $module.find('ul.text-list').text().toLowerCase();

        if (chosenStandard === 'CHOOSE ONE') {
            // everything matches
        } else if (standards.indexOf(chosenStandard.toLowerCase()) === -1) {
            // no matching standard found
            matchesAllFilters = false;
        }

        if (chosenGradeLevel === 'CHOOSE ONE') {
            // everything matches
        } else if (gradeLevels.indexOf(chosenGradeLevel.toLowerCase()) === -1) {
            // no matching grade level found
            matchesAllFilters = false;
        }

        if ($.trim(searchText) === '') {
            // everything matches
        } else {
            // search separately for each word, e.g. "bird", "population"
            var searchWords = searchText.split(' ');
            $.each(searchWords, function(i, word) {
                if (searchableText.indexOf(word) === -1) {
                    // text not found anywhere in this module's description or metadata
                    matchesAllFilters = false;
                }
            });
        }
        
        if (matchesAllFilters) {
            // update odd/even class for best display
            if (oddOrEven === 'even') {
                $module.removeClass('odd');
                $module.addClass('even');
                oddOrEven = 'odd';
            } else {
                $module.removeClass('even');
                $module.addClass('odd');
                oddOrEven = 'even';
            }
            $module.show();
        } else {
            $module.hide();
        }

    });
}
function resetAllModuleFilters() {
    // clear all filters and refresh the module list
    var $standardsFilter = $('#filter-by-standard');
    var $gradeLevelFilter = $('#filter-by-grade-level');
    var $textFilter = $('#filter-by-text');
    $standardsFilter.val('CHOOSE ONE');
    $gradeLevelFilter.val('CHOOSE ONE');
    $textFilter.val('');
    updatedFilteredModuleList();
}
function searchModulesForKeyword(kwLink) {
    // clear module filter, then search for the text of this link
    var $standardsFilter = $('#filter-by-standard');
    var $gradeLevelFilter = $('#filter-by-grade-level');
    var $textFilter = $('#filter-by-text');
    $standardsFilter.val('CHOOSE ONE');
    $gradeLevelFilter.val('CHOOSE ONE');
    var newSearchKeyword = $(kwLink).text();
    $textFilter.val( newSearchKeyword );
    updatedFilteredModuleList();
}
function activateModuleKeywords() {
    // Turn each bare (comma-separated) keyword into a links that will search for
    // modules using its text.
    var $keywordLists = $('#dryadlab-module-list .keyword-list');
    $keywordLists.each(function(i, listHolder) {
        var $listHolder = $(listHolder);
        var kwHTML = $listHolder.html();
        var newHTML = '';
        $listHolder.empty();
        var kwList = kwHTML.split(',');
        $.each(kwList, function(i, kw) {
           newHTML += '<a href="#" onclick="searchModulesForKeyword(this); return false;">'+ $.trim(kw) +'</a>';
           if (i < (kwList.length - 1)) {
               newHTML += ', ';
           }
        });
        $listHolder.html(newHTML);
    });
}
