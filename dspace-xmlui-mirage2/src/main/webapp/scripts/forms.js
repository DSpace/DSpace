/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
$(function() {

     // HTML5 input date polyfill
     if(!Modernizr.inputtypes.date){
            $('input[type="date"]').each(function(){
               $(this).datepicker({dateFormat: 'yy-mm-dd'});
            });
     }

    $('a.information').tooltip();


});
