/**
 * Created on : 17-dic-2013, 14:21:13
 * @author David Andrés Maznzano Herrera <damanzano>
 */
// Fix input element click problem for inputs on dropdowns
$(document).ready(function() {
    //Handles menu drop down
    jQuery('.dropdown-menu').find('form').click(function(e) {
        e.stopPropagation();
    });

    jQuery('.collapsible').on('shown.bs.collapse', function(e) {
        // Get Invoker
        var invokerId = $(this).attr('data-invoker');
        jQuery('#' + invokerId).children('span.glyphicon').removeClass("glyphicon-plus").addClass("glyphicon-minus");
        e.stopPropagation();
        //Getlist group item
        jQuery(this).parents('.list-group-item').addClass('active');
    });

    jQuery('.collapsible').on('hidden.bs.collapse', function(e) {
        // Get Invoker
        var invokerId = $(this).attr('data-invoker');
        jQuery('#' + invokerId).children('span.glyphicon').removeClass("glyphicon-minus").addClass("glyphicon-plus");
        e.stopPropagation();
        //Getlist group item
        jQuery(this).parents('.list-group-item').removeClass('active');
    });

});
