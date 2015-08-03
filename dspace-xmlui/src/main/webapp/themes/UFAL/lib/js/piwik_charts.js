jQuery(document).ready(function (){

	jQuery("#visits_over_time_chart").each(function(){

		var targetDiv = jQuery(this);
		var reportURL = targetDiv.attr("data-url");

		jQuery.ajax(
				{
					url : reportURL,
					dataType : 'json',
					beforeSend: function() {
        				jQuery("#piwik-loading").css("display", "block");
    				}					
				}
			)
			.done(
					function(data) {
					
						jQuery("#piwik-loading").css("display", "none");
					
						var nb_views = [];
						var nb_downloads = [];
						var dates = Object.keys(data);
						dates.sort(dateSorter);
						var total_views = 0;
						var total_unique_views = 0;
						var total_downloads = 0;
						var total_unique_downloads = 0;
						var total_visits = 0;
						var total_unique_visits = 0;
						for(var i=0;i<dates.length;i++) {
                            var values = data[dates[i]];
                            nb_views[i] = [dates[i], values['nb_pageviews']||0];
                            nb_downloads[i] = [dates[i], values['nb_downloads']||0];
                            total_views += nb_views[i][1];
                            total_unique_views += values['nb_uniq_pageviews']||0;
                            total_downloads += nb_downloads[i][1];
                            total_unique_downloads += values['nb_uniq_downloads']||0;
                            total_visits += values['nb_visits']||0;
                            total_unique_visits += values['nb_uniq_visitors']||0;
						}
		jQuery('#visits_summary_report .views').html("<strong>" + total_views + "</strong> pageviews, <strong>" + total_unique_views + "</strong> unique pageviews.");
		jQuery('#visits_summary_report .visits').html("<strong>" + total_visits + "</strong> visits, <strong>" + total_unique_visits + "</strong> unique visitors.");
		jQuery('#visits_summary_report .downloads').html("<strong>" + total_downloads + "</strong> downloads, <strong>" + total_unique_downloads + "</strong> unique downloads.");

                        var visitsPlot = $.jqplot ('visits_over_time_chart', [nb_views, nb_downloads], {
                        		axes:{
                        			xaxis:{
                        				renderer:$.jqplot.DateAxisRenderer,
                        				tickOptions:{
                        					formatString:'%a %#d %b',
								showGridline: false,
                        				},
                        				tickInterval: '1 day',
                        				min: dates[0],
                        				max: dates[dates.length-1],
                        			},

                        			yaxis:{
                        				min: 0,
                        				numberTicks: 3
                        			}
                        		},

                        		seriesDefaults: {
                        			lineWidth:1,
                        			markerOptions: {
                        				size: 6
                        			}
                        		},

                        		grid: {background: '#FFFFFF', borderWidth: 0, shadow: false},

                        		seriesColors: [ "#d4291f", "#1f78b4", "#ff7f00", "#33a02c", "#6a3d9a", "#b15928", "#fdbf6f", "#cab2d6" ],

					series: [
						{highlighter: {formatString: "%s<BR/><strong style='font-size: 14px;'>%s</strong> Visits"}},
						{highlighter: {formatString: "%s<BR/><strong style='font-size: 14px;'>%s</strong> Downloads"}}
					],

                        		highlighter: {
                        			show: true,
                        			sizeAdjust: 7.5,
                        			tooltipAxes: "both",
                        		},

					legend: {
						renderer: $.jqplot.EnhancedLegendRenderer,
					        show: true,
						placement: 'outsideGrid',
						location: 'n',
						showSwatches: true,
                    				rendererOptions: {
                        				numberColumns: 2,
                    				},
						seriesToggle: true,
						labels: ['Views', 'Downloads'],
					},

                        });

                        jQuery(window).resize(function(){
                        	visitsPlot.replot();
                        });

					}
			)
			.fail(
					function(data) {
					}
			);

	});
	
	var dateRE = /^(\d{2})[\/\- ](\d{2})[\/\- ](\d{4})/;

    function dateSorter(a, b) {
        a = a.replace(dateRE, "$3$2$1");
        b = b.replace(dateRE, "$3$2$1");
        if (a > b) return 1;
        if (a < b) return -1;
        return 0;
    };

	jQuery(".jqplot-to-picture").click(function() {
		var a = jQuery(this);
		var t = jQuery(a.attr("target-div"));
		var imgData = t.jqplotToImageStr({});
		var imgElem = jQuery('#jqplot-save-as-picture img').attr('src',imgData);
		imgElem.css('display', 'block');
		jQuery("#jqplot-save-as-picture").modal();
	});

});
