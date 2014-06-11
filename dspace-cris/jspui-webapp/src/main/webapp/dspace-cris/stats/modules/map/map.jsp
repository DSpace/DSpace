<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%--  http://www.beginningspatial.com/creating_proportional_symbol_maps_google_maps  --%>
<c:set var="pieType" >location</c:set>
<c:set var="targetDiv" scope="page" >div_${data.jspKey}_${statType}_${objectName}_${pieType}</c:set>

<c:set var="jsDataObjectName" scope="page"> data_${statType}_${objectName}_${pieType}_${pieType}</c:set>
<c:if test="${fn:length(data.resultBean.dataBeans[statType][objectName][pieType].dataTable) gt 0}">
<div class="panel panel-default">
  <div class="panel-heading">
   <h6 class="panel-title"><i class="fa fa-map-marker"></i> <fmt:message key="view.stats.map.title" /></h6>
  </div>
  <div class="panel-body">
	<div id="${targetDiv}" style="width: 100%; height: 300px;"></div>
  </div>
</div>

<script type="text/javascript">
<!--
	var ${jsDataObjectName} = new Array (${fn:length(data.resultBean.dataBeans[statType][objectName][pieType].dataTable)});
	<c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType].dataTable}" var="row" varStatus="status">	
		${jsDataObjectName}[${status.count-1}]= ['<c:out value="${row.latitude}"/>','<c:out value="${row.longitude}"/>','<c:out value="${row.value}"/>',<c:out value="${row.percentage}"/>];
	</c:forEach>	


function initialize_${jsDataObjectName}() {
if (${jsDataObjectName}.length==0) return;
    var latlng_${jsDataObjectName} = new google.maps.LatLng(${jsDataObjectName}[0][0],${jsDataObjectName}[0][1]);
    var myOptions_${jsDataObjectName} = {
      zoom: 4,
      center: latlng_${jsDataObjectName},
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };
	  var map = new google.maps.Map(document.getElementById("${targetDiv}"),myOptions_${jsDataObjectName});
	  	  for(var i = 0; i < ${jsDataObjectName}.length; i++) {
		  var myLatlng = new google.maps.LatLng(${jsDataObjectName}[i][0],${jsDataObjectName}[i][1]);
		  var marker = new google.maps.Marker({
		      position: myLatlng, 
		      map: map, 
		      title:${jsDataObjectName}[i][2]
		  });   
	  }

  }

initialize_${jsDataObjectName}();
-->
</script>
</c:if>