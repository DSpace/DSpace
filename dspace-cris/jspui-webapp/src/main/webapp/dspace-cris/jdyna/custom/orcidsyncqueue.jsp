<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>
<c:set var="root"><%=request.getContextPath()%></c:set>
<c:if test="${!empty anagraficaObject.anagrafica4view['orcid']}">

<script type="text/javascript">

fnServerObjectToArray = function ()
{
	
	return function ( sSource, aoData, fnCallback ) {
		
    	j.ajax( {
            "dataType": 'json',
            "type": "POST",
            "url": "<%= request.getContextPath() %>/json/orcidqueue",
            "data" : {
	            "id" : "${entity.crisID}"
	          },
            "error": function(data){
            	json = new Object();
        		json.aaData = [];
                fnCallback(json);
            	return;
            },
            "success": function (json) {
            	if (json == null || json.error)
            	{
            		json = new Object();
            		json.aaData = [];
	                fnCallback(json);
            		return;
            	}
            	fnCallback(json);
            }
        } );
    };
};

var oTable;


function showPleaseWait() {  
    j("#pleaseWaitDialog").modal("show");
}

function hidePleaseWait() {
    j("#pleaseWaitDialog").modal("hide");
}

jQuery(document).ready(function() {
	
	drawOrcidDatatable = function() { 
		jQuery('#orcidQueueTable').dataTable( {
		"processing": true,
		"bServerSide": true,
		"sAjaxSource": "<%= request.getContextPath() %>/json/orcidqueue",
		"fnServerData": fnServerObjectToArray(),
		"sAjaxDataProp": "result",
		"bDestroy": true,
	    "aoColumns": [
			{ "mData": "ttext", 
	            "bSortable": true,
	            "sWidth": "3%",
	            "mRender": function(data, type, full) {
	            	if(data=='crisrp')
	                	return '<i class="fa fa-user"></i>';
	            	if(data=='crispj')
	            		return '<i class="fa fa-university"></i>';
	            	if(data=='item')
	                	return '<i class="fa fa-archive"></i>';
	             }	
			},
	        { "mData": "name" , 
	            "bSortable": true,
	            "mRender": function(data, type, full) {
	            	if(full.ttext=='crisrp' || full.ttext=='crispj') {
	            		return '<a href="<%= request.getContextPath() %>/cris/uuid/'+full.uuid+'">' + data + '</a>';
	            	}
	            	else {
	            		 return '<a href="<%= request.getContextPath() %>/handle/'+full.uuid+'">' + data + '</a>';
	            	}
	             }	
			},
	        {
	            "mData": "uuid",
	            "sWidth": "10%",
	            "bSortable": false,
	            "mRender": function(data, type, full) {
	                return '<a class="btn btn-warning" data-operation="3" data-ttext="'+full.ttext+'" data-uuid="'+data+'" data-owner="'+full.owner+'">' + j('#orcidmanualsend').text() + '</a>';
	             }
	        }
	    ]
		} );
	};
	
	oTable = drawOrcidDatatable();
	
	j('#orcidQueueTable tbody').on('click', 'a', function() {
		var operation = j(this).data("operation");
		if(operation == 3) {
			var uuid = j(this).data("uuid");
			var owner = j(this).data("owner");
			var ttext = j(this).data("ttext");
			showPleaseWait();
			j.ajax({
				  type: "POST", //or GET
				  url: "<%= request.getContextPath() %>/json/orcidqueue/rest/"+ttext,
				  data: {
		            "id" : "${entity.crisID}",
		            "uuid" : uuid,
		            "owner": owner
		          },
				  success: function(response){
					  if(response.status==false) {
						  hidePleaseWait();
						  j("#modalerrors").modal('show');  
					  }
					  else {
						  hidePleaseWait();
						  j("#modalsuccess").modal('show');
					  }
					  oTable = drawOrcidDatatable();
				  }
			} );	
		}
	});

} );




</script>
<div class="panel-group" id="${holder.shortName}">
	<div class="panel panel-default">
    	<div class="panel-heading">
    		<h4 class="panel-title">
        		<a data-toggle="collapse" data-parent="#${holder.shortName}" href="#collapseOne${holder.shortName}">
          			${holder.title} 
        		</a></h4>
    	</div>
		<div id="collapseOne${holder.shortName}" class="panel-collapse collapse in">
			<div class="panel-body">	
			<div class="dynaClear">&nbsp;</div>
            <div class="dynaClear">&nbsp;</div>
            <div class="dynaClear">&nbsp;</div>
			<div class="dynaField"></div>								
					
					<div class="col-md-12">
    						<div class="container">

							<c:choose>	
								<c:when test="${!empty anagraficaObject.anagrafica4view['orcid-push-manual'] && anagraficaObject.anagrafica4view['orcid-push-manual'][0].value.object==1}">
									<span class="label label-warning"><fmt:message key="jsp.orcid.custom.box.label.preferences.manual"/></span>
								</c:when>
								<c:otherwise>
									<span class="label label-info"><fmt:message key="jsp.orcid.custom.box.label.preferences.batch"/></span>											
								</c:otherwise>
							</c:choose>							
							<hr/>
							<span><fmt:message key="jsp.orcid.custom.box.label.preferences.warning.v2"/></span>
							<hr/>
							<div class="clearfix">&nbsp;</div>
							<table id="orcidQueueTable" class="table table-striped table-bordered">
					        <thead>
					            <tr>
					                <th></th>
					                <th>Name</th>
					                <th></th>
					            </tr>
					        </thead>
						    </table>
					</div></div>
			</div>
		</div>
	</div>
</div>

<span id="orcidmanualsend" style="display: none;"><fmt:message key="jsp.orcid.custom.box.label.preferences.button.manual"/></span>

 <!-- Modal -->
  <div class="modal fade" id="modalsuccess" tabindex="-1" role="dialog" aria-labelledby="myModalSuccessLabel" aria-hidden="true">
   <div class="modal-dialog">
      <div class="modal-content">
       <div class="modal-header">
         <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
         <h4 class="modal-title" id="myModalSuccessLabel"><fmt:message key="jsp.orcid.custom.box.label.send.success.title"/></h4>
       </div>
       <div class="modal-body" id="errorSuccess" style="overflow-x: scroll;">
			<fmt:message key="jsp.orcid.custom.box.label.send.success"/>
       </div>
    </div>
   </div>
 </div>
 <div class="modal fade" id="modalerrors" tabindex="-1" role="dialog" aria-labelledby="myModalErrorLabel" aria-hidden="true">
   <div class="modal-dialog">
      <div class="modal-content">
       <div class="modal-header">
         <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
         <h4 class="modal-title" id="myModalErrorLabel"><fmt:message key="jsp.orcid.custom.box.label.send.error.title"/></h4>
       </div>
       <div class="modal-body" id="errorMessage" style="overflow-x: scroll;">
			<fmt:message key="jsp.orcid.custom.box.label.send.error"/>
       </div>
    </div>
   </div>
 </div>
<div class="modal" id="pleaseWaitDialog" data-backdrop="static" data-keyboard="false" role="dialog">
	<div class="modal-dialog">
      <div class="modal-content">
      	<div class="modal-header">
      		<fmt:message key="jsp.orcid.custom.box.label.plaisewait"/>
      	</div>
	  </div>
    </div>
</div> 
</c:if>