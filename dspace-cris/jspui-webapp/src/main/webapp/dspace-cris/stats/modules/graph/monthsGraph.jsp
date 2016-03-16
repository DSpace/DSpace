<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
		<c:set var="targetDiv" scope="page">lastYearsDiv_${statType}_${objectName}_${pieType}</c:set>
		<c:set var="jsDataObjectName" scope="page">splineData_${statType}</c:set>
	

<script type="text/javascript"><!--

j(document).ready(function() {
    	j('div#statstab-content-time').bind('redraw', function() {            	
    		drawVisualization_${statType}_${objectName}_${pieType}();
		});
       google.setOnLoadCallback(drawVisualization_${statType}_${objectName}_${pieType});
});

-->
</script>

	  <script type="text/javascript"> 
      
      
      function drawVisualization_${statType}_${objectName}_${pieType}() {
          var dashboard = new google.visualization.Dashboard(document.getElementById('dashboard_${targetDiv}'));
        
          var control = new google.visualization.ControlWrapper({
              'controlType': 'ChartRangeFilter',
              'containerId': 'control_${targetDiv}',
              'options': {
                // Filter by the date axis.
                'filterColumnIndex': 0,
                'ui': {
                  'chartType': 'LineChart',
                  'chartOptions': {
                    'chartArea': {'width': '90%'},
                    'hAxis': {'baselineColor': 'none'},                    
                    'height': 50,
                    'width': '100%'
                  },
                  // Display a single series that shows the closing value of the stock.
                
                  // 1 day in milliseconds = 24 * 60 * 60 * 1000 = 86,400,000
                  'minRangeSize': 86400000                  
                },                
                'height': 50,
                'width': '100%'                
              }
            });
         
            var chart = new google.visualization.ChartWrapper({
              'chartType': 'LineChart',
              'containerId': 'chart_${targetDiv}',
              'options': {
                // Use the same chart area width as the control for axis alignment.
                'chartArea': {'height': '80%', 'width': '90%'},
                'hAxis': {'slantedText': false, 'format':'MMM, y'//, 
                	//'textStyle': {'color': '#999999', 'fontSize': '11', 'fontName': '"Lucida Grande","Lucida Sans Unicode",Verdana,Arial,Helvetica,sans-serif'}
                },
                //'vAxis': {'textStyle': {'color': '#999999', 'fontSize': '11', 'fontName': '"Lucida Grande","Lucida Sans Unicode",Verdana,Arial,Helvetica,sans-serif'}},
                'legend': {'position': 'none'},
                'pointSize': 5,
                'height': 400,
                'width': '100%',
                'backgroundColor': 'none',
                'series': { //Create 2 separate series to fake what you want. One for the line             and one for the points
                    '0': {
                        'color': '#DDDF0D',
                        'lineWidth': '2'
                    },
                    '1': {
                        'color': '#DDDF0D',
                        'lineWidth': '0',
                        'pointSize': '5'
                    }
                },
                'title': '<fmt:message key="view.${data.jspKey}.data.${statType}.${objectName}.${pieType}.title" />',
                'titlePosition': 'out',
                'titleTextStyle':  {//'color': '#FFFFFF', 
                	'fontSize': '16'}
                
                
              }
         
             
            });
        
        
		var ${jsDataObjectName} = new google.visualization.DataTable();
   	    ${jsDataObjectName}.addColumn('date','Time');
   	  	${jsDataObjectName}.addColumn('number','Hit');
   	  	
   		<c:set var="foundfirstNotZero" scope="page">false</c:set>
   		<c:set var="counter" scope="page">1</c:set>   		 
	 	<c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType].dataTable}" var="row" varStatus="status">
			<c:if test="${row[1]!=null && row[1]!= ''}">
				<c:if test="${row[1]!=0 && foundfirstNotZero==false}">
					<c:set var="foundfirstNotZero" scope="page">true</c:set>
				</c:if>					
				<c:if test="${foundfirstNotZero==true}">
					${jsDataObjectName}.addRow([parseISO8601('<c:out value="${row[0]}"/>'), <c:out value="${row[1]}"/>]);
					<c:set var="counter" scope="page">${counter+1}</c:set>	
				</c:if>
			</c:if>
		</c:forEach>			    
	    
		
		
        dashboard.bind(control, chart);
        
        l = google.visualization.events.addListener(chart, 'ready', function() {              
            
        	if(navigator.userAgent.indexOf("Trident/5")>-1) {
         	   		j('#statspng_${targetDiv}').hide();
        	   		j('#statsjpg_${targetDiv}').hide();   
  	           	    j('#iestatspng_${targetDiv}').show();
         	   		j('#iestatsjpg_${targetDiv}').show();
         	   		
    				j('#iestatspng_${targetDiv}').click(function() {
    					convertToImg(document.getElementById('chart_${targetDiv}'),0);
    				});
    				j('#iestatsjpg_${targetDiv}').click(function() {
    					convertToImg(document.getElementById('chart_${targetDiv}'),1);			
    				});       				
            }
            else {
            	/*For PNG we use new google api features */
              	/* saveAsImg(document.getElementById('chart_${targetDiv}'),0,'statspng_${targetDiv}'); */
              	$("#statspng_${targetDiv}").attr("href", chart.getChart().getImageURI());
              	
            	saveAsImg(document.getElementById('chart_${targetDiv}'),1,'statsjpg_${targetDiv}');   		
            }	
     	   google.visualization.events.removeListener(l);
        });
        dashboard.draw(${jsDataObjectName});
          
      }               
    </script>
        	 
        
 		<div class="pull-right target_stats_button">
				<a id="iestatspng_${targetDiv}" style="display: none"><fmt:message key="jsp.statistics.save-as-png" /></a>
				<a class="btn btn-default" id="statspng_${targetDiv}" href="#" download="stats_${statType}_${objectName}_${pieType}.png"><fmt:message key="jsp.statistics.save-as-png" /></a>
				
				<a class="btn btn-default" id="statsjpg_${targetDiv}" href="#" download="stats_${statType}_${objectName}_${pieType}.jpg"><fmt:message key="jsp.statistics.save-as-jpeg" /></a>
				<a id="iestatsjpg_${targetDiv}" style="display: none"><fmt:message key="jsp.statistics.save-as-jpeg" /></a>
		</div>
		<div class="clearfix"> </div>
      	 <div id="dashboard_${targetDiv}" class="dashboard_stats">      	 	
	        <div class="target_stats" id="chart_${targetDiv}" style="height: 400px;"></div>
        	<div class="control_stats" id="control_${targetDiv}" style="height: 50px;"></div>
    	</div>
    	
<fmt:message
			key="view.${data.jspKey}.data.${statType}.${objectName}.allMonths.description" />
