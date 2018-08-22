/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function ($) {


    function publisherVersionVisibility(showPublisherVersion) {
        if (showPublisherVersion) {
            $('.publiserVersionLink').removeClass("hidden");
            $('.nonPublisherViewerLink').addClass("hidden");
            $('a.embeddedViewOpenLink').attr('href',$('a.no_accessThumbnailLinking').attr('href'));
            $('a.embeddedViewOpenLink').removeClass("hidden");

        } else {
            $('.nonPublisherViewerLink').removeClass("hidden");
            // Don't link to the embedded page if the entitlement check has failed
            $('a.embeddedViewOpenLink').addClass("hidden");
            $('a.no_accessThumbnailLinking').removeAttr("href");
        }
    }
    $(document).ready(function () {
        var params;
        var DSpace;
        var entitlementLink;
        var wrapper;
        var showPublisherVersion = false;
        var doiPrefix;

        DSpace = window.DSpace;


        function generateURL(reference) {
            var url = DSpace['elsevier_' + reference + '_url'];

            if(url) {
                    wrapper = $('.' + reference + '-wrapper');
                    entitlementLink = $('.' + reference + '-link');

                url = url.replace('http:', '');
                params = {
                    apiKey: DSpace.elsevier_apikey,
                    httpAccept: 'application/json'
                };

                doiPrefix = DSpace.doi_prefix;

                if (DSpace.item_pii) {
                    showPublisherVersion=true;
                    return url + '/pii/' + DSpace.item_pii;
                } else if (DSpace.item_doi && DSpace.elsevier_doi) {
                    showPublisherVersion=true;
                    return url + '/doi/' + DSpace.item_doi;
                } else if (DSpace.item_scopus_id) {
                    return url + '/scopus_id/' + DSpace.item_scopus_id;
                } else if (DSpace.item_pubmed_id) {
                    return url + '/pubmed_id/' + DSpace.item_pubmed_id;
                } else if (DSpace.item_eid) {
                    return url + '/eid/' + DSpace.item_eid;
                } else if(DSpace.doi_prefix && DSpace.item_doi){
                    renderDoiFallback();
                    publisherVersionVisibility(showPublisherVersion);
                }

                return false;
                }
            }

            function handleSuccess(response) {
                var document = response['entitlement-response']['document-entitlement'];

                if(document != null) {
                    var entitledString = String(document['entitled']);
                    var link = document['link']['@href'];

                    var isAvailable = (entitledString === 'true' || entitledString === 'open_access');
                    if (isAvailable) {
                        setAccessible(entitledString);
                    }
                    else {
                        // This might look like a weird construction, but I only want to do the embargo call when absolutely necessary...
                        // Because it's asynchronous I don't see any other way to do this
                        embargoHasExpired();
                    }
                }
                else {
                    embargoHasExpired();
                }
            }

        function setAccessible(entitledString) {
            // If entitled-> Always show publisher version
            $('.publiserVersionLink').removeClass("hidden");
            $('.nonPublisherViewerLink').removeClass("hidden");
            $('.embeddedViewOpenLink').removeClass("hidden");

            $('#elsevier-embed-wrapper, #elsevier-embed-wrapper-full').find('.noaccess').addClass("hidden");
            var access = $('#elsevier-embed-wrapper, #elsevier-embed-wrapper-full').find('.access');
            access.removeClass("hidden");
            if (entitledString === 'open_access') {
                access.find('.open-access').removeClass("hidden");
                access.find('.full-text-access').addClass("hidden");
            } else {
                access.find('.open-access').addClass("hidden");
                access.find('.full-text-access').removeClass("hidden");
            }
        }

        function setInaccessible() {
            // If not entitled, use "default" visibility
            publisherVersionVisibility(showPublisherVersion);
        }

        function handleError(response) {
            if(response.status == 404 && DSpace.doi_prefix && DSpace.item_doi)
            {
                renderDoiFallback();
            }

            publisherVersionVisibility(showPublisherVersion);
        }

        function pastStartDate(dateString) {
            var q = new Date();
            var m = q.getMonth();
            var d = q.getDate();
            var y = q.getFullYear();

            var today = new Date(y, m, d);

            var startdate = new Date(dateString);
            return today > startdate;
        }

        function embargoHasExpired() {
            $.ajax({
                dataType: 'json',
                url: generateURL("embargo"),
                data: params,
                success: (function (response) {
                    var documentVersions = response['hosting-permission-response']['document-hosting-permission']['hosting-platform']['document-version'];
                    for (var i = 0; i < documentVersions.length; i++) {
                        var hostingAllowed = documentVersions[i]["hosting-allowed"];
                        if (hostingAllowed) {
                            for (var j = 0; j < hostingAllowed.length; j++) {
                                var allowInformation = hostingAllowed[j];
                                if (String(allowInformation["@audience"]) === 'Public' && pastStartDate(String(allowInformation["@start_date"]))) {
                                    setAccessible();
                                    return;
                                }
                            }
                        }
                    }
                })
            });
            setInaccessible();
        }

        // fallback to http://dx.doi.org if a DOI is available
        function renderDoiFallback(){
            showPublisherVersion=true;
            $(".publiserVersionLink").attr("href", DSpace.doi_prefix + DSpace.item_doi);
        }

        var url = generateURL("entitlement");
        if (url) {
            $.ajax({
                dataType: 'json',
                url: url,
                data: params,
                success: handleSuccess,
                error: handleError
            });
        }
    });
})(jQuery);