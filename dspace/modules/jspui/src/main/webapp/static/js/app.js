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

    jQuery('.community-toggler').click(function(e) {
        e.preventDefault();
        var collapse = jQuery(this).closest('');
    });
});
