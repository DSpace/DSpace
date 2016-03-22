/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
(function($) {
    $(function() {
        init_browse_navigation();
        init_sort_options_menu();
    });

    function init_sort_options_menu() {
        $('.sort-options-menu a').click(function() {
            var $this, browse_controls;
            $this = $(this);
            browse_controls = $('#aspect_artifactbrowser_ConfigurableBrowse_div_browse-controls, ' +
                    '#aspect_administrative_WithdrawnItems_div_browse-controls, ' +
                    '#aspect_administrative_PrivateItems_div_browse-controls, '+
                    '#aspect_discovery_SearchFacetFilter_div_browse-controls');
            $('*[name="' +$this.data('name') + '"]', browse_controls).val($this.data('returnvalue'));
            $('.btn', browse_controls).click();
            $this.closest('.open').removeClass('open');
            return false;
        });
    }

    function init_browse_navigation() {
        $('.alphabet-select').change(function() {
            var $this = $(this);
            $this.mouseout();
            window.location = $this.val();
            return false
        });

        $('#aspect_artifactbrowser_ConfigurableBrowse_field_year').change(function() {
            $('#aspect_artifactbrowser_ConfigurableBrowse_field_starts_with').val('');
            $('#aspect_artifactbrowser_ConfigurableBrowse_field_submit').click();
        });

        $('#aspect_administrative_WithdrawnItems_field_year').change(function() {
            $('#aspect_administrative_WithdrawnItems_field_starts_with').val('');
            $('#aspect_administrative_WithdrawnItems_field_submit').click();
        });

        $('#aspect_administrative_PrivateItems_field_year').change(function() {
            $('#aspect_administrative_PrivateItems_field_starts_with').val('');
            $('#aspect_administrative_PrivateItems_field_submit').click();
        });
    }

})(jQuery);