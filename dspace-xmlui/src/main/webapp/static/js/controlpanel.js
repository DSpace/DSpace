/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
/*global jQuery */
/*jshint globalstrict: true*/
'use strict';
//
//
//
jQuery(document).ready(function () {
    jQuery("#aspect_administrative_ControlPanel_table_users").css(
        "word-break", "break-all");
    jQuery(".toggle-onclick-parent-next4").each(function () {
        // toggle on click
        var how_many = 4;
        jQuery(this).click(function () {
            var o = jQuery(this).parent();
            for (var i = 0; i < how_many; ++i) {
                o = o.next();
                o.toggleClass('hidden');
            }
            return false;
        });
    });

}); // ready
