/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
$(function() {
    $( "#aspect_statisticsGoogleAnalytics_StatisticsGoogleAnalyticsTransformer_field_startDate" ).datepicker({
        changeMonth: true,
        changeYear: true,
        defaultDate: "-1y",
        numberOfMonths: 1,
        dateFormat: "yy-mm-dd",
        onClose: function( selectedDate ) {
            $( "#aspect_statisticsGoogleAnalytics_StatisticsGoogleAnalyticsTransformer_field_endDate" ).datepicker( "option", "minDate", selectedDate );
        }
    });
    $( "#aspect_statisticsGoogleAnalytics_StatisticsGoogleAnalyticsTransformer_field_endDate" ).datepicker({
        changeMonth: true,
        changeYear: true,
        numberOfMonths: 1,
        dateFormat: "yy-mm-dd",
        onClose: function( selectedDate ) {
            $( "#aspect_statisticsGoogleAnalytics_StatisticsGoogleAnalyticsTransformer_field_startDate" ).datepicker( "option", "maxDate", selectedDate );
        }
    });
});