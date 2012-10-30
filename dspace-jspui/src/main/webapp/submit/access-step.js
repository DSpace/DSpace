(function() {
    jQuery.noConflict();
    jQuery(document).ready(function($) {
        $("input[name='open_access_radios']").change(function() {
            if ($("#embargo_until_date").attr("disabled") == undefined) {
                $("#embargo_until_date").attr("disabled", "disabled");
                $("#reason").attr("disabled", "disabled");
            } else {
                $("#embargo_until_date").removeAttr("disabled");
                $("#reason").removeAttr("disabled");
            }
        });
    });
})();
