/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {


    $(document).ready(function () {
        var url;
        var params;
        var DSpace;
        var entitlementLink;
        var wrapper;

        DSpace = window.DSpace;
        url = DSpace.elsevier_entitlement_url; // is undefined if entitlement checks are disabled
        if(url) {
            wrapper = $('.entitlement-wrapper');
            entitlementLink = $('.entitlement-link');

            url = url.replace('http:', '');
            params = {
                apiKey: DSpace.elsevier_apikey,
                httpAccept: 'application/json'
            };

            var doCall = false;

            if (DSpace.item_pii) {
                doCall = true;
                url += '/pii/' + DSpace.item_pii;
            } else if (DSpace.item_doi) {
                doCall = true;
                url += '/doi/' + DSpace.item_doi;
            }

            if (doCall) {

                function handleSuccess(response) {
                    var document = response['entitlement-response']['document-entitlement'];
                    var entitledString = String(document['entitled']);
                    var link = document['link']['@href'];

                    function insertLink() {
                        wrapper.removeClass('hidden');
                        entitlementLink.attr('href', link);
                        entitlementLink.attr('target', '_blank');
                        entitlementLink.text(link);
                    }

                    if (entitledString === 'false') {
                        insertLink();
                    }

                    if (entitledString === 'true' || entitledString === 'open_access') {
                        insertLink();
                        wrapper.append(' (Accessible) ');
                    }
                }

                $.ajax({
                    dataType: 'json',
                    url: url,
                    data: params,
                    success: handleSuccess
                });
            }
        }

    });
})(jQuery);