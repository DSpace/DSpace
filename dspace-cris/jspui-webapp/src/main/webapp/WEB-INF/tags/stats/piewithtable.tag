<%@ attribute name="data" required="true" type="java.lang.Object"%>
<%@ attribute name="statType" required="true"%>
<%@ attribute name="objectName" required="true"%>
<%@ attribute name="pieType" required="true"%>
<%@ attribute name="useLocalMap" required="false" %>
<%@ attribute name="useFmt" required="false" %>

<%@ taglib uri="statstags" prefix="stats" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<c:set var="targetDiv" scope="page">div_${data.jspKey}_${statType}_${objectName}_${pieType}_${objectName}</c:set>

<c:choose>
        <c:when test="${fn:length(data.resultBean.dataBeans[statType][objectName][pieType].limitedDataTable)>0}">

    	
	
		<div class="target_stats_button">
				<a id="iestatspng_${targetDiv}" style="display: none">Save as PNG Image</a>
				<a id="statspng_${targetDiv}" href="#" download="stats_${statType}_${objectName}_${pieType}.png">Save as PNG Image</a>
				|
				<a id="statsjpg_${targetDiv}" href="#" download="stats_${statType}_${objectName}_${pieType}.jpg">Save as JPEG Image</a>
				<a id="iestatsjpg_${targetDiv}" style="display: none">Save as JPEG Image</a>
		</div>
			
			
			<div class="target_stats" id="${targetDiv}"></div>


<stats:table data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" useLocalMap="${useLocalMap}" useFmt="${useFmt}" />

<c:set var="jsDataObjectName" scope="page"> data_${statType}_${objectName}_${pieType}_${pieType}</c:set>
<script type="text/javascript"><!--
		var j = jQuery.noConflict();
       

        var ${jsDataObjectName} = new google.visualization.DataTable();
          ${jsDataObjectName}.addColumn('string','${pieType}');
          ${jsDataObjectName}.addColumn('number','Visit');
         	
          <c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType].limitedDataTable}" var="row" varStatus="status">
              ${jsDataObjectName}.addRow( ['<c:out value="${row.label}"/>',<c:out value="${row.value}"/>]);      
          </c:forEach>
		
         
          
          function drawChart_${statType}_${objectName}_${pieType}_${pieType}() {	  
        	  var options = {
        			is3D: true,        			
                    'height': 400,                    
                    'backgroundColor': 'none',
                    'pieSliceText': 'label',
                    'legend': {textStyle: {color: 'white'}}
    	        };
      
               var chart = new google.visualization.PieChart(document.getElementById('${targetDiv}'));               
               
               // set up event handler
               l = google.visualization.events.addListener(chart, 'ready', function() {              
                   
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
            	   google.visualization.events.removeListener(l);
               });
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
<%-- this is the javascript to build the pie --%>
<%--stats:pie data="${data}" statType="${statType}" objectName="${objectName}" pieType="${pieType}" targetDiv="${targetDiv}" /--%>
 
<%--fmt:message key="view.${data.jspKey}.data.${statType}.${objectName}.${pieType}.description" /--%>
</c:when>	
	<c:otherwise>
		<fmt:message key="view.${data.jspKey}.data.${statType}.${objectName}.${pieType}.empty" />
	</c:otherwise>
</c:choose>
<c:set var="useFmt">false</c:set>
<c:set var="useLocalMap">false</c:set>
