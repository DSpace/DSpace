/*global jQuery, google */
/*jshint globalstrict: true*/
'use strict';

var ufal = ufal || {};
google.load('visualization', '1', {'packages': ['geochart']});

jQuery(document).ready(function () {
    ufal.statistics.init_maps();
});


//
// GA
//

ufal.statistics = {

    init_maps: function () {
        var data_map = {};
        var header = false;
        var totals = 0;
        var jtable = jQuery('#cz_cuni_mff_ufal_dspace_app_xmlui_aspect_statistics_GAStatisticsTransformer_table_ga-countries tr');
        if (0 === jtable.length) {
            totals = 'not available';
        }

        jtable.each(function () {
            if (!header) {
                header = true;
                return;
            }
            var state = null;
            jQuery('td', this).each(function () {
                var tt = jQuery(this).text();
                if (state === null) {
                    state = tt;
                    return;
                }
                if (!isNaN(tt)) {
                    var num = parseInt(tt);
                    totals += num;
                    if ( state in data_map ) {
                        data_map[state] += num;
                    }else {
                        data_map[state] = num;
                    }
                }
            });
        });

        // set totals
        jQuery('#cz_cuni_mff_ufal_dspace_app_xmlui_aspect_statistics_GAStatisticsTransformer_table_ga-info .totalvisits td.even').text(totals);

        var data_arr = [ ["Country", "# of views from this country"] ];
        for (var key in data_map) {
            if ( data_map.hasOwnProperty(key)) {
                data_arr.push( [key, data_map[key]] );
            }
        }

        google.setOnLoadCallback(drawRegionsMap);
        function drawRegionsMap() {
            var data = google.visualization.arrayToDataTable(data_arr);
            var options = {};
            var chart = new google.visualization.GeoChart(document.getElementById(
                'cz_cuni_mff_ufal_dspace_app_xmlui_aspect_statistics_GAStatisticsTransformer_div_chart_id'));
            chart.draw(data, options);
        }
    }
};
