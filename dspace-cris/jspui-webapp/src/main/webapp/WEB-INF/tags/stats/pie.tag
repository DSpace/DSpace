<%@ attribute name="data" required="true" type="java.lang.Object"%>
<%@ attribute name="statType" required="true"%>
<%@ attribute name="objectName" required="true"%>
<%@ attribute name="pieType" required="true"%>
<%@ attribute name="targetDiv" required="true"%>
<%@ taglib uri="statstags" prefix="stats" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<c:set var="targetDiv" scope="page">div_${data.jspKey}_${statType}_${objectName}_${pieType}_${objectName}</c:set>
<c:set var="jsDataObjectName" scope="page"> data_${statType}_${objectName}_${pieType}_${pieType}</c:set>

		<div class="target_stats_button">
				<a id="iestatspng_${targetDiv}" style="display: none">Save as PNG Image</a>
				<a id="statspng_${targetDiv}" href="#" download="stats_${statType}_${objectName}_${pieType}.png">Save as PNG Image</a>
				|
				<a id="statsjpg_${targetDiv}" href="#" download="stats_${statType}_${objectName}_${pieType}.jpg">Save as JPEG Image</a>
				<a id="iestatsjpg_${targetDiv}" style="display: none">Save as JPEG Image</a>
		</div>
<div class="row">		
<div class="target_stats" id="${targetDiv}"></div>
</div>
<script type="text/javascript"><!--

              

        var ${jsDataObjectName} = new google.visualization.DataTable();
          ${jsDataObjectName}.addColumn('string','${pieType}');
          ${jsDataObjectName}.addColumn('number','Visit');
         	
          <c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType].limitedDataTable}" var="row" varStatus="status">
              ${jsDataObjectName}.addRow( ['<c:out value="${row.label}"/>',<c:out value="${row.value}"/>]);      
          </c:forEach>
          
          function onReady_${statType}_${objectName}_${pieType}() {
              if(navigator.userAgent.indexOf("Trident/5")>-1) {
           	   j('#statspng_${targetDiv}').hide();
          	   		j('#statsjpg_${targetDiv}').hide();   
	           	    j('#iestatspng_${targetDiv}').show();
           	   	j('#iestatsjpg_${targetDiv}').show();            	   
      				j('#iestatspng_${targetDiv}').click(function() {
      					convertToImg(document.getElementById('${targetDiv}'),0);			
      				});
      				j('#iestatsjpg_${targetDiv}').click(function() {
      					convertToImg(document.getElementById('${targetDiv}'),1);			
      				});       				
              }
              else {
               	saveAsImg(document.getElementById('${targetDiv}'),0,'statspng_${targetDiv}');
               	saveAsImg(document.getElementById('${targetDiv}'),1,'statsjpg_${targetDiv}');   		
              }	 
          }
          
          function drawChart_${statType}_${objectName}_${pieType}_${pieType}() {	  
        	  var options = {
        			is3D: true,        			
                    'height': 400,
                    'legend': {'position': 'bottom'},
                    'backgroundColor': 'none',
                    'pieSliceText': 'label'
    	        };
      
               var chart = new google.visualization.PieChart(document.getElementById('${targetDiv}'));
               google.visualization.events.addListener(chart, 'ready', onReady_${statType}_${objectName}_${pieType});
               chart.draw(${jsDataObjectName}, options);              	
             }

       j(document).ready(function() {
    	   j('div#statstab-content-${pieType}').bind('redraw', function() {   
    		   setInterval(function(){drawChart_${statType}_${objectName}_${pieType}_${pieType}()},500);          		
       	   });
           google.setOnLoadCallback(drawChart_${statType}_${objectName}_${pieType}_${pieType});        	
       });
	           
	                	
        	        


-->
</script>