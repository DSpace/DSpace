jQuery(document).ready(function (){

	var endDate = moment();
	var startDate = moment().subtract(6, "days");

	jQuery('input[name="daterange"]').daterangepicker(
		{

ranges: {
           'Last 7 Days': [moment().subtract(6, 'days'), moment()],
           'Last 30 Days': [moment().subtract(29, 'days'), moment()],
           'This Month': [moment().startOf('month'), moment()],
           'Last Month': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')],
	   'This Year': [moment().startOf('year'), moment()]
        },
			locale: {
				format: 'YYYY-MM-DD'
			},
			startDate: startDate,
			endDate: endDate
		},
		function(start, end, label){
			startDate = start;
			endDate = end;
			$('.nav-tabs a[href="#views"]').tab('show');
			jQuery("#piwik-charts").each(loadContents)
		}
	);

	jQuery("#piwik-charts").each(loadContents);

	function loadContents() {
		var targetDiv = jQuery(this);
		var reportURL = targetDiv.attr("data-url");
		var tickInterval = targetDiv.attr("interval");

		var ticks = endDate.diff(startDate, "day");
		if(ticks >= 6) {
			tickInterval = "".concat(ticks / 6).concat(" day");
		} else {
			tickInterval = "".concat(1).concat(" day");
		}

		var visitsPlot;
		var downloadPlot;

		if(reportURL.indexOf("?")!=-1) {
			reportURL = reportURL.concat("&date=", startDate.format("YYYY-MM-DD"), ",", endDate.format("YYYY-MM-DD"));
		}else{
			reportURL = reportURL.concat("?date=", startDate.format("YYYY-MM-DD"), ",", endDate.format("YYYY-MM-DD"));
		}

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
						var dates = Object.keys(data[0]);
						dates.sort(dateSorter);
						var total_views = 0;
						var total_unique_views = 0;
						var total_downloads = 0;
						var total_unique_downloads = 0;
						var total_visits = 0;
						var total_unique_visits = 0;
						for(var i=0;i<dates.length;i++) {
                            var values0 = data[0][dates[i]];
                            var values1 = data[1][dates[i]];
                            nb_views[i] = [dates[i], values0['nb_pageviews']||0];
                            nb_downloads[i] = [dates[i], values1['nb_pageviews']||0];
                            total_views += nb_views[i][1];
                            total_unique_views += values0['nb_uniq_pageviews']||0;
                            total_downloads += nb_downloads[i][1];
                            total_unique_downloads += values1['nb_uniq_pageviews']||0;
                            total_visits += values0['nb_visits']||0;
                            total_unique_visits += values0['nb_uniq_visitors']||0;
						}
		jQuery('#visits_summary_report .views').html("<strong>" + total_views + "</strong> pageviews, <strong>" + total_unique_views + "</strong> unique pageviews.");
		jQuery('#visits_summary_report .visits').html("<strong>" + total_visits + "</strong> visits, <strong>" + total_unique_visits + "</strong> unique visitors.");
		jQuery('#visits_summary_report .downloads').html("<strong>" + total_downloads + "</strong> downloads, <strong>" + total_unique_downloads + "</strong> unique downloads.");
		jQuery('#views_tab_count').html("<strong>" + total_views + "</strong>");
		jQuery('#downloads_tab_count').html("<strong>" + total_downloads + "</strong>");
		jQuery('#period').html(dates[0] + " to " + dates[dates.length-1]);

 			 jQuery('#visits_over_time_chart').html("");
                         visitsPlot = $.jqplot ('visits_over_time_chart', [nb_views, nb_views], {
                        		axes:{
                        			xaxis:{
                        				renderer:$.jqplot.DateAxisRenderer,
                        				tickOptions:{
                        					formatString:'%Y %a %#d %b',
								showGridline: false,
                        				},
                        				tickInterval: tickInterval,
                        				min: dates[0],
                        				max: dates[dates.length-1],
                        			},

                        			yaxis:{
                        				min: 0,
                        				numberTicks: 3
                        			}
                        		},

                          		seriesDefaults: {
                          			lineWidth:3,
                          			shadow:false,
                          			markerOptions: {
                          				size: 6
                          			},
                          			highlighter: {formatString: "<div style='color: #FFFFFF;'>%s<BR/><strong style='font-size: 14px;'>%s</strong> Visits</div>"}
                          		},

                        		grid: {background: '#F0F0F0', borderWidth: 0, shadow: false},

                        		seriesColors: [ "#bee89c", "#60a22a" ],

					series: [
					        {fill: [true, false]}
					],

                        		highlighter: {
                        			show: true,
                        			sizeAdjust: 7.5,
                        			tooltipAxes: "both",
                        		},

                        });

                        $('a[data-toggle="tab"]').on('shown.bs.tab', function (e) {
                        	  //e.target // newly activated tab
                        	  //e.relatedTarget // previous active tab
                        	  var target = e.target.getAttribute("aria-controls");
                        	  if(target=='views') {
					jQuery('#visits_over_time_chart').html("");
                        		visitsPlot = $.jqplot ('visits_over_time_chart', [nb_views, nb_views], {
                              		axes:{
                              			xaxis:{
                              				renderer:$.jqplot.DateAxisRenderer,
                              				tickOptions:{
                              					formatString:'%Y %a %#d %b',
      								showGridline: false,
                              				},
                              				tickInterval: tickInterval,
                              				min: dates[0],
                              				max: dates[dates.length-1],
                              			},

                              			yaxis:{
                              				min: 0,
                              				numberTicks: 3
                              			}
                              		},

                                		seriesDefaults: {
                                			lineWidth:3,
                                			shadow:false,
                                			markerOptions: {
                                				size: 6
                                			},
                                			highlighter: {formatString: "<div style='color: #FFFFFF;'>%s<BR/><strong style='font-size: 14px;'>%s</strong> Visits</div>"}
                                		},

                              		grid: {background: '#F0F0F0', borderWidth: 0, shadow: false},

                              		seriesColors: [ "#bee89c", "#60a22a" ],

      					series: [
      					        {fill: [true, false]}
      					],

                              		highlighter: {
                              			show: true,
                              			sizeAdjust: 7.5,
                              			tooltipAxes: "both",
                              		},

                              });
                        	  } else
                        	  if(target=='downloads'){
				  jQuery('#downloads_over_time_chart').html("");
                                  downloadPlot = $.jqplot ('downloads_over_time_chart', [nb_downloads, nb_downloads], {
                              		axes:{
                              			xaxis:{
                              				renderer:$.jqplot.DateAxisRenderer,
                              				tickOptions:{
                              					formatString:'%Y %a %#d %b',
      								showGridline: false,
                              				},
                              				tickInterval: tickInterval,
                              				min: dates[0],
                              				max: dates[dates.length-1],
                              			},

                              			yaxis:{
                              				min: 0,
                              				numberTicks: 3
                              			}
                              		},

                              		seriesDefaults: {
                              			lineWidth:3,
                              			shadow:false,
                              			markerOptions: {
                              				size: 6
                              			},
                              			highlighter: {formatString: "<div style='color: #FFFFFF;'>%s<BR/><strong style='font-size: 14px;'>%s</strong> Downloads</div>"}
                              		},

                              		grid: {background: '#F0F0F0', borderWidth: 0, shadow: false},

                              		seriesColors: ["#94c7ea", "#1f78b4"],

      					series: [
      						{fill: [true, false]}
      					],

                              		highlighter: {
                              			show: true,
                              			sizeAdjust: 7.5,
                              			tooltipAxes: "both",
                              		},

                              });
                        	  }
                        })

                        jQuery(window).resize(function(){
                        	visitsPlot.replot();
                        	downloadPlot.replot();
                        });

					}
			)
			.fail(
					function(data) {
					}
			);
	}

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
