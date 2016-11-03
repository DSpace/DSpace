<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ page contentType="text/html;charset=UTF-8" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>

<%@ page import="org.dspace.app.webui.cris.servlet.DTODCValue"%>
<%@ page import="org.dspace.app.webui.cris.web.tag.DeduplicationTagLibraryFunctions"%>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport" %>
<%@ page import="javax.servlet.jsp.PageContext" %>

<%@ page import="org.dspace.content.MetadataField" %>
<%@ page import="org.dspace.app.webui.servlet.admin.AuthorizeAdminServlet" %>
<%@ page import="org.dspace.app.webui.servlet.admin.EditItemServlet" %>

<%@ page import="org.dspace.content.Collection" %>
<%@ page import="org.dspace.content.DCDate" %>

<%@ page import="org.dspace.core.ConfigurationManager" %>

<%@ page import="org.dspace.core.Utils" %>
<%@ page import="org.dspace.content.authority.MetadataAuthorityManager" %>
<%@ page import="org.dspace.content.authority.ChoiceAuthorityManager" %>
<%@ page import="org.dspace.content.authority.Choices" %>

<%@ page import="org.dspace.app.util.DCInput" %>

<%@ page import="java.util.Map"%>
<%@ page import="java.util.List"%>
<%@	page import="java.util.HashMap"%>
<%@	page import="org.dspace.content.Item"%>
<%@ page import="org.dspace.content.MetadataField" %>
<%@ page import="org.dspace.content.Bitstream" %>
<%@ page import="org.dspace.content.BitstreamFormat" %>
<%@ page import="org.dspace.content.Bundle" %>
<%@ page import="org.dspace.content.Metadatum" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="org.dspace.eperson.EPerson" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>
<c:set var="dspace.layout.head.last" scope="request">	
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/jquery.tablednd_0_5.js"></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/jquery.contextmenu.js"></script>
    <script type="text/javascript" src="<%= request.getContextPath() %>/js/jquery.textarearesizer.compressed.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/jquery.pretty-text-diff.js"></script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/diff_match_patch.js"></script>
</c:set>
<%
	Boolean mergeByUser = (Boolean)request.getAttribute("mergeByUser");	
	String navbar = "admin";
	if(mergeByUser != null && mergeByUser) { 
	    navbar="off";
	}
%>
<dspace:layout style="submission" locbar="link" titlekey="jsp.dspace-admin.deduplication" navbar="<%=navbar%>">
   <div class="row-fluid">
    	<h1><fmt:message key="jsp.dspace-admin.deduplication.signature"><fmt:param><fmt:message key="jsp.tools.deduplicate.dd.${signatureType}"/></fmt:param></fmt:message></h1>   
   </div>
<style type="text/css">

.alertLink{
	font-size:1.5em;
}

div.grippie {
	background: #EEEEEE url(../images/grippie.png) no-repeat scroll center 2px;
	border-color: #DDDDDD;
	border-style: solid;
	border-width: 0pt 1px 1px;
	margin: 0px;
	cursor: s-resize;
	height: 9px;
	overflow: hidden;	
}

.collapsed-item {
	display: none;
}

.hidebutton,
.hideAll {
	display: none;
}

.onpreview {
	border-color: black;
    border-width: 3px;
}

pre {
    white-space: -moz-pre-wrap; /* Mozilla, supported since 1999 */
    white-space: -pre-wrap; /* Opera */
    white-space: -o-pre-wrap; /* Opera */
    white-space: pre-wrap; /* CSS3 - Text module (Candidate Recommendation) http://www.w3.org/TR/css3-text/#white-space */
    word-wrap: break-word; /* IE 5.5+ */
}

.ui-icon {
	background-image: url("../css/redmond/images/ui-icons_469bdd_256x240.png")
		!important;
}

.unknownField{
	display: none;
}
/*
.showDragHandle {
	background-image: url("../images/updown.gif");
	background-position: center center;
	background-repeat: no-repeat;
	cursor: move;
}


.dragHandle {
	min-width: 50px;
	width: 50px;
}
*/

.tDnD_whileDrag {
	background-color: #f1b881 !important;
}

#table-selected tr:hover {
	background-color: #FCEFA1;
}

.example-target {
	background: url("../images/help_icon.gif") no-repeat scroll 3px center
		#0F67A1;
	color: white;
	cursor: help;
	padding: 4px 10px 4px 35px;
}

.new {
	background: url("../images/new_icon48.gif") no-repeat right;
	min-width: 50px;
	width: 50px;
}
.center{
	text-align: center;
}
#table-selected .nomargin{
	margin: 0px;
	padding: 0px;
}

.maxwidth{
	width: 100%;
}

#table-selected .strikeout span{
	text-decoration: line-through;
	color: red;
}

.diffItem{
	font-size: large;
}

.itemIdButton{
	cursor: pointer;
	cursor: hand;
}

.previewMode{
	display: none;
}
</style>


<%
	Map<Integer, String> citations =(Map<Integer, String>) request.getAttribute("citations");
	List<DTODCValue> dcv = (List<DTODCValue>) request.getAttribute("dtodcvalues");
	Map<Integer, DCInput> dcinputs = (Map<Integer, DCInput>)request.getAttribute("dcinputs");
	List<Item> items = (List<Item>) request.getAttribute("items");    
	List<Bitstream> bitstreams = (List<Bitstream>) request.getAttribute("bitstreams");
	Item item = (Item) request.getAttribute("target");		
    MetadataField[] dcTypes = (MetadataField[])  request.getAttribute("dcTypes");
    List<String> blockedTypes = (List<String>)  request.getAttribute("blockedMetadata");
    Map<Integer, String> metadataFields = (HashMap<Integer, String>) request.getAttribute("metadataFields");    
    Map<Integer, String> legenda = (HashMap<Integer, String>) request.getAttribute("legenda");
    Map<String, Integer> metadataSourceInfo = (Map<String, Integer>)request.getAttribute("metadataSourceInfo");
    Map<Integer, List<DTODCValue>> metadataExtraSourceInfo = (Map<Integer, List<DTODCValue>>)request.getAttribute("metadataExtraSourceInfo");
    Map<Collection, Boolean[]> collections = (Map<Collection, Boolean[]>)request.getAttribute("collections");
    Boolean noowningcollection = (Boolean)request.getAttribute("noowningcollection");
%>

<script type="text/javascript">

function openMetadata() {
	j("#menu1").modal('show');
} 
j(document).ready(function() {
	
	j("#menu1").modal('hide');

    <c:forEach var="type" items="${dcTypes}">
	j(".modalbox_${type.fieldID}").click(function() {
		j("#menu1").modal('hide');
        j("#modalbox_metadata_${type.fieldID}").modal( "show" );
    	j("#modalbox_metadata_${type.fieldID}").prettyTextDiff({
    		cleanup: true,
    		debug: true,
            originalContainer: '.firstItem',
            changedContainer: '.secondItem',
            diffContainer: '.diffItem'
    	});
    });
	

	</c:forEach>
	
	/*	j( "#modalbox_metadata_${type.fieldID}" ).dialog({
	autoOpen: false,
	height: "auto",
	width: "auto",
	modal: true
});*/
	
    j('.hidededup').click(function () {
    	var attr = j(this).attr("id");
		var splitted = attr.substring(5);
		var tr = "#"+ splitted;
		var new_attr = "#"+ splitted + " td textarea";
		var name = j(new_attr).attr("name");
		j(new_attr).attr("name", "hidden_"+name);
		j(tr).toggleClass("collapsed-item");
		var new_attr1 = "#onpreview_"+ splitted;
		var new_attr2 = "#show_"+ splitted;
		var new_attr3 = "#edih_"+ splitted;
    	j(new_attr1).toggleClass("onpreview");
    	j(new_attr2).toggleClass("hidebutton");
    	j(new_attr3).toggleClass("hidebutton");
    });
    
    j('.showdedup').click(function () {
    	var attr = j(this).attr("id");
		var splitted = attr.substring(5);
		var tr = "#"+ splitted;
		var new_attr = "#"+ splitted + " td textarea";
		var name = j(new_attr).attr("name");
		name = name.substring(7);
		j(new_attr).attr("name", name);
    	j(tr).toggleClass("collapsed-item");    	
    	var new_attr1 = "#onpreview_"+ splitted;
		var new_attr2 = "#show_"+ splitted;
		var new_attr3 = "#edih_"+ splitted;
    	j(new_attr1).toggleClass("onpreview");
    	j(new_attr2).toggleClass("hidebutton");
    	j(new_attr3).toggleClass("hidebutton");
    });
    
    j('#showall').click(function () {
    	
    	j('.collapsed-item').each(function(index) {
    	     j(this).toggleClass("collapsed-item");    	    
    	});     	
    	
    	j('td[id^="onpreview"][class=""]').each(function(index) {
   	    	 j(this).toggleClass("onpreview");    	    
   		});
    	
    	j('textarea[id^="value"][name^="hidden"]').each(function(index) {
    		var attr = j(this).attr("name");
    		var splitted = attr.substring(7);
    		j(this).attr("name", splitted);  	    
   		});   
    	
    	j('.removed-item').each(function(index) {
   	  	   j(this).toggleClass("collapsed-item");    	    
 	 	});
    	
    	j('.hidebutton').each(function(index) {
   	    	 j(this).toggleClass("hidebutton");    	    
   		});
    	
    	j('.showdedup').each(function(index) {
  	    	 j(this).toggleClass("hidebutton");    	    
  		});
    	    	
    });
    
    
	/* Manage sortable table with id table-selected*/
/*	j("#table-selected").tableDnD();
	
	j("#table-selected tr").hover(function() {			
        j(this.cells[0]).addClass("showDragHandle");
  	}, function() {
        j(this.cells[0]).removeClass("showDragHandle");
  	});
*/	
	j('#submitmerge').click(function () {
		j("#table_values").val(j("#table-selected").tableDnDSerialize());			
	});
	
	j('#duplicate').click(function() { j(this).toggleClass("duplicate");});
	
    j('#showToggle').click(function () {
    	j('.showAll').each(function(index) {
    	     j(this).toggleClass("hideAll");    	    
    	});
    });
    
    j('#showSuccessField').click(function(){
    	j('#table-selected').find("tr").each(function(index) {
    		if (j(this).hasClass('previewMode')){
    			j(this).removeClass("previewMode");
    		} else if (j(this).hasClass('alert-danger')){
    			j(this).addClass("previewMode");
    		}
    	})
    })
    
    j('#showDiffField').click(function(){
    	j('#table-selected').find("tr").each(function(index) {
    		if (j(this).attr('id').indexOf("_group") != -1){
    			datagroup = j(this).data("group");
    			j('#table-selected').find("[data-group='" + datagroup + "']").each(function(index) {
    				if (j(this).find('alert-danger')){
    					disableGroup(datagroup);
    				}
    			})
    		}
    	})
    })


    
    
    j('.actionButton').click(function(event){
    	if (j(this).hasClass('delete')){
	    	datagroup = j(this).data("group");
	    	j('#table-selected').find("[data-group='" + datagroup + "']").each(function(index) {
	    		if (j(this).attr('id') != undefined){
	    			if (j(this).attr('id').indexOf("_group") != -1){
	    				j(this).find('.labelField div').addClass('strikeout');
	    			} else {
	    				if (!j(this).hasClass('removed-item')){
	    					j(this).switchClass("alert-success", "alert-danger");
	    					//j(this).find('.dataIco i').switchClass('fa-check','fa-ban');
	    					j(this).addClass('deleted-item');
	    					if (j(this).hasClass('alert-success')) j(this).addClass('restore-item');
	    					j(this).toggleClass("collapsed-item");
	    	        		var attr = j(this).attr("id");
	    	    			var tr = "#"+ attr;
	    	    			var new_attr = "#"+ attr + " td textarea";
	    	    			var name = j(new_attr).attr("name");
	    	    			j(new_attr).attr("name", "hidden_"+name);
	    				}
	    			}
	    		}
	    	})
	    	event.preventDefault();
	    	j(this).removeClass('delete');
	    	j(this).addClass('restore');
	    	j(this).html('<i class=\"icon-undo2\"></i><fmt:message key="jsp.deduplication.fieldRestore"/>');
    	} else if (j(this).addClass('restore')){
        	datagroup = j(this).data("group");
        	j('#table-selected').find("[data-group='" + datagroup + "']").each(function(index) {
        		if (j(this).attr('id') != undefined){
        			if (j(this).attr('id').indexOf("_group") != -1){
        				j(this).find('.labelField div').removeClass('strikeout');
        			} else {
        				if (!j(this).hasClass('removed-item')){
        					if (j(this).hasClass('restore-item')){
        						j(this).switchClass("alert-danger", "alert-success");
        						j(this).removeClass('restore-item');
        					}
        					//j(this).find('.dataIco i').switchClass('fa-ban','fa-check');
	    					j(this).removeClass('deleted-item');
	    					j(this).toggleClass("collapsed-item");
	    	        		var attr = j(this).attr("id");
	    	    			var tr = "#"+ attr;
	    	    			var new_attr = "#"+ attr + " td textarea";
	    	    			var name = j(new_attr).attr("name");
	    	    			name = name.substring(7);
	    	    			j(new_attr).attr("name", name);
        				}
	    			}
        		}
        	})
        	event.preventDefault();
        	j(this).removeClass('restore');
        	j(this).addClass('delete');
        	j(this).html('<i class=\"fa fa-trash-o fa-lg\"></i><fmt:message key="jsp.deduplication.fieldDeleteLabel"/>');    		
    	}
    })
	itemCount = ${items.size()};
    j('.selectItem').click(function(){
    	if (j(this).data("repeatable") == false && itemCount < 3){
    		datagroup = j(this).data("group");
    		j('.'+datagroup).each(function(index) {
    			if (j(this).hasClass('removed-item')){
    				return;
				}
    			if (j(this).hasClass('alert-success')){
    				j(this).switchClass("alert-success", "alert-danger");
    				j(this).find('.dataIco i').switchClass('fa-check','fa-ban');
    	        	var attr = j(this).attr("id");
    	    		var tr = "#"+ attr;
    	    		var new_attr = "#"+ attr + " td textarea";
    	    		var name = j(new_attr).attr("name");
    	    		j(new_attr).attr("name", "hidden_"+name);
    			} else if (j(this).hasClass('alert-danger')){
    				j(this).switchClass("alert-danger", "alert-success");
    				j(this).find('.dataIco i').switchClass('fa-ban','fa-check');
    	        	var attr = j(this).attr("id");
    	    		var tr = "#"+ attr;
    	    		var new_attr = "#"+ attr + " td textarea";
    	    		var name = j(new_attr).attr("name");
    	    		name = name.substring(7);
    	    		j(new_attr).attr("name", name);
    			}
			});
    	} else {
    		if (j(this).hasClass('alert-success')){
				j(this).switchClass("alert-success", "alert-danger");
				j(this).find('.dataIco i').switchClass('fa-check','fa-ban');
	        	var attr = j(this).attr("id");
	    		var tr = "#"+ attr;
	    		var new_attr = "#"+ attr + " td textarea";
	    		var name = j(new_attr).attr("name");
	    		j(new_attr).attr("name", "hidden_"+name);    			
    		} else if (j(this).hasClass('alert-danger')){
				j(this).switchClass("alert-danger", "alert-success");
				j(this).find('.dataIco i').switchClass('fa-ban','fa-check');
	        	var attr = j(this).attr("id");
	    		var tr = "#"+ attr;
	    		var new_attr = "#"+ attr + " td textarea";
	    		var name = j(new_attr).attr("name");
	    		name = name.substring(7);
	    		j(new_attr).attr("name", name);    	
			}
    	}
    });
});


//===== Default datatable =====//
$(document).ready(function() {
	/*
	oTable = $('#table-selected').dataTable({
		"bJQueryUI": false,
		"bAutoWidth": false,
		"sPaginationType": "full_numbers",
		"sDom": '<"datatable-header"fl><"datatable-scroll"t><"datatable-footer"ip>',
		"oLanguage": {
			"sSearch": "<span>Filter:</span> _INPUT_",
			"sLengthMenu": "<span>Show entries:</span> _MENU_",
			"oPaginate": { "sFirst": "First", "sLast": "Last", "sNext": ">", "sPrevious": "<" }
		}
	});
	*/
});
</script>
<div class="modal fade" id="menu1" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
  <div class="modal-dialog">
    <div class="modal-content">
     	<div class="modal-header">
     	   <p class="modal-title" id="myModalLabel"><fmt:message key="jsp.deduplication.metadata.select"/></p>
      	</div>
        <div class="modal-body with-padding">
		    <ul>
		         <% 
		         for(MetadataField type : dcTypes) {
		        	boolean blocked = false;
		        	for(String block : blockedTypes) {
		        	    if(block!=null && block.equalsIgnoreCase(metadataFields.get(type.getFieldID()).trim())) {
		        			blocked = true;
		        			break;
		        		}
		        	}
		         	if(!blocked && metadataExtraSourceInfo.get(type.getFieldID()).size()>0) { %>
					<li><a class="modalbox_<%= type.getFieldID()%>" title="Show" href="javascript:void(0);"><%= metadataFields.get(type.getFieldID()) %></a></li>
		         <% } 
		  	 
		         }%>
			</ul>
        </div>
      <div class="modal-footer">
        <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
      </div>
    </div>
  </div>
</div>
<%!
     StringBuffer doAuthority(MetadataAuthorityManager mam, ChoiceAuthorityManager cam,
            PageContext pageContext,
            String contextPath, String fieldName, String idx,
            DTODCValue dtodcv){
		Metadatum dcv = dtodcv.getDcValue();
		//int collectionID = dtodcv.getOwnerCollectionID();
        StringBuffer sb = new StringBuffer();
        if (cam.isChoicesConfigured(fieldName)){
            boolean authority = mam.isAuthorityControlled(fieldName);
            boolean required = authority && mam.isAuthorityRequired(fieldName);
           
            String fieldIdIdx = (dtodcv.isHidden() && dtodcv.isRemoved())?("hidden_value_" + fieldName + "_" + idx):("value_" + fieldName + "_" + idx);
            String fieldNameIdx = dtodcv.isHidden()?("hidden_value_" + fieldName + "_" + idx):("value_" + fieldName + "_" + idx);
            String authorityName = "choice_" + fieldName + "_authority_" + idx;
            String confidenceName = "choice_" + fieldName + "_confidence_" + idx;
            sb.append("<div style=\"font-size: 1.3em\">")
              .append(dcv.value)
     	      .append("<textarea class=\"hide form-control\" readonly=\"readonly\" id=\"").append(fieldIdIdx).append("\" name=\"").append(fieldNameIdx)
 	          .append("\" rows=\"1\" cols=\"50\">")
 	          .append(dcv.value).append("</textarea>\n")
              .append("</div>");
                if (authority){
                    String confidenceSymbol = Choices.getConfidenceText(dcv.confidence).toLowerCase();
                    sb.append("<div>")
                    .append("Authority key: <input readonly=\"readonly\" class=\"form-control\" type=\"hidden\" value=\"")
                    .append(dcv.authority != null ? dcv.authority : "")
                    .append("\" id=\"").append(authorityName)
                    .append("\" name=\"").append(authorityName).append("\" />")
                    .append(dcv.authority != null ? dcv.authority : "")
					.append("</div>")
                    .append("<input type=\"hidden\" value=\"").append(confidenceSymbol).append("\" id=\"").append(confidenceName)
                    .append("\" name=\"").append(confidenceName)
                    .append("\" class=\"ds-authority-confidence-input\"/>");
                }
        }
        return sb;
    }
%>


<div class="row-fluid">
<form action="<%=request.getContextPath()%>/tools/duplicate" name="duplicateForm" method="post">
<table class="table table-bordered table-condensed display">
	<thead>
		<tr><td><fmt:message key="jsp.tools.edit-item-form.fieldset.legend"/></td><td>Identifier</td></tr>
	</thead>
	<tbody>
	<% 
	String statusClass = "info";
	String labelClass = "label-default";
	String iconItemClass = "icon-stack";
	for (Item myItem : items){
		if (item.getID() == myItem.getID()){
			statusClass = "info";
			labelClass = "label-info";
		} else {
			statusClass = "default";
			labelClass = "label-default";
		}
	%>
		<tr class="alert">
			<td>
				<div>	
					<div><%=citations.get(myItem.getID())%></div>
					<%
					if (myItem.getOwningCollection() != null){
					%>
					<div><fmt:message key="jsp.tools.deduplication.fieldset.collection"/><%=myItem.getOwningCollection().getName() %></div>
					<div><fmt:message key="jsp.tools.deduplication.fieldset.handle"/><%=myItem.getHandle() %></div>
					<%
					}
					%>
				</div>
			</td>
			<td>
				<div class="itemIdButton label <%=labelClass %>" title="visualizza il prodotto" onclick="window.open('<%=request.getContextPath()%>/tools/edit-item?item_id=<%=myItem.getID()%>');">
					<i class="<%=iconItemClass %>">
						<%=myItem.getID()%>
					</i>
				</div>
			</td>
		</tr>
	<%}%>	
	</tbody>
</table>

<br/>
<!--  div id="collectionDiv">
<fieldset id="previewcollections"><legend><fmt:message key="jsp.tools.edit-item-form.fieldset.choosecollection"/></legend>
	<div class="table-responsive">
	<table class="table table-condensed table-bordered">
	<thead>
	<tr>
	<th><fmt:message key="jsp.tools.edit-item-form.dedup.collectioninfo.handle"/></th>
	<th><fmt:message key="jsp.tools.edit-item-form.dedup.collectioninfo.name"/></th>
	<th><fmt:message key="jsp.tools.edit-item-form.dedup.collectioninfo.ownercollection"/></th>
	<% if(noowningcollection==null || !noowningcollection) { %>
	<th><fmt:message key="jsp.tools.edit-item-form.dedup.collectioninfo.othercollections"/></th>
	<% } %>
	
	</tr>
	</thead>
	
	<tbody>
	
	<% for(Collection collection : collections.keySet()) { %>	
	<tr>
	<td><%= collection.getHandle()%></td>
	<td><%= collection.getName() %></td>
    <td align="center"><input type="radio"  id="ownercollection" name="collectionOwner" value="<%= collection.getID() %>" <% if(collections.get(collection)[0]) {%> checked="checked" <% } %></td>
    <% if(noowningcollection==null || !noowningcollection) { %>
	<td align="center"><input type="checkbox"  id="othercollections" name="collectionOthers" value="<%= collection.getID() %>" <% if(collections.get(collection)[1]) {%> checked="checked" <% } %></td>
	<% } %>
    
	</tr>
		
	<% } %>
	
	</tbody>
	</table>
	</div>
</fieldset>

</div -->

<p>&nbsp;</p>
<% String row = "even"; //ONLY used on bitstream table 
  if (bitstreams.size() > 0) { %>
        <%-- <h2>Bitstreams</h2> --%>
		<fieldset><legend><fmt:message key="jsp.tools.edit-item-form.heading"/></legend>
		<div class="table-responsive">
        <table class="table table-bordered table-condensed" summary="Bitstream data table">
            <tr>
	   <%-- <th class="oddRowEvenCol"><strong>Primary<br>Bitstream</strong></th>
                <th class="oddRowOddCol"><strong>Name</strong></th>
                <th class="oddRowEvenCol"><strong>Source</strong></th>
                <th class="oddRowOddCol"><strong>Description</strong></th>
                <th class="oddRowEvenCol"><strong>Format</strong></th>
                <th class="oddRowOddCol"><strong>User&nbsp;Format&nbsp;Description</strong></th> --%>
                <th class="oddRowEvenCol"><fmt:message key="jsp.deduplication.itemid"/></th>
                <th class="oddRowOddCol"><fmt:message key="jsp.tools.edit-item-form.select"/></th>
		        <th class="oddRowEvenCol"><fmt:message key="jsp.tools.edit-item-form.elem7"/></th>
                <th class="oddRowOddCol"><fmt:message key="jsp.tools.edit-item-form.elem12"/></th>
                <th class="oddRowEvenCol"><fmt:message key="jsp.tools.edit-item-form.elem13"/></th>                
                <th class="oddRowOddCol"><fmt:message key="jsp.deduplication.actions"/></th>
            </tr>
<%
    
    row = "even";

        for (Bitstream bits : bitstreams)
        {
            // Parameter names will include the bundle and bitstream ID
            // e.g. "bitstream_14_18_desc" is the description of bitstream 18 in bundle 14
            Bundle bnd = bits.getBundles()[0];
            String key = bnd.getID() + "_" + bits.getID();
            BitstreamFormat bf = bits.getFormat();
%>
		   <tr>
		    	<td headers="t18" class="<%= row %>RowEvenCol" align="center">
                	<span class="label label-<%= legenda.get(bnd.getParentObject().getID())%>"><%= bnd.getParentObject().getID()%></span>
                </td>
                <td headers="t11" class="<%= row %>RowEvenCol" align="center">
                    <input type="checkbox" name="bitstream_id" value="<%= bits.getID() %>" checked="checked"/>
                </td>
                <td headers="t12" class="<%= row %>RowOddCol">
                    <%= (bits.getName() == null ? "" : Utils.addEntities(bits.getName())) %>
                </td>
                <td headers="t13" class="<%= row %>RowEvenCol">
                    <%= bits.getSize()%>
                </td>
                <td headers="t14" class="<%= row %>RowOddCol">
                    <%= (bits.getChecksum() == null ? "" : Utils.addEntities(bits.getChecksum())) %>
                </td>                
                <td headers="t17" class="<%= row %>RowEvenCol">
                    <%-- <a target="_blank" href="<%= request.getContextPath() %>/retrieve/<%= bits.getID() %>">View</a>&nbsp;<input type="submit" name="submit_delete_bitstream_<%= key %>" value="Remove"> --%>
                      <a target="_blank" href="<%= request.getContextPath() %>/retrieve/<%= bits.getID() %>"><fmt:message key="jsp.tools.general.view"/></a>&nbsp;     
                </td>
  			</tr>
		<% 
			 row = (row.equals("odd") ? "even" : "odd");
        }
%>
        </table>  
        </div>
        </fieldset>
<% } %>	
<div class="text-center">
	<!-- button type="button" class="btn btn-default" id="showall" ><fmt:message key="jsp.deduplication.showAll"/></button> 
	<button type="button" class="btn btn-default" id="addMetadata" onclick="openMetadata();"><fmt:message key="jsp.deduplication.addMetadata"/></button -->
	<button type="button" class="btn btn-default" id="showToggle"><fmt:message key="jsp.deduplication.showHideValues"/></button>
	<button type="button" class="btn btn-default" id="showSuccessField"><fmt:message key="jsp.deduplication.showHideFinalObject"/></button>
	<button type="submit" class="btn btn-primary" name="submitmerge" id="submitmerge" ><fmt:message key="jsp.tools.general.merge"/></button>
</div>
<br/>
<!--  div class="table-responsive datatable-tasks" -->
<div class="table-responsive">
<table id="table-selected" class="table table-bordered table-condensed display">
<thead>
	<tr>
		<th colspan="2"><fmt:message key="jsp.deduplication.field"/></th>
		<th><fmt:message key="jsp.deduplication.identifier"/></th>
	</tr>
</thead>
<tbody>
	
<%	
	MetadataAuthorityManager mam = MetadataAuthorityManager.getManager();
	ChoiceAuthorityManager cam = ChoiceAuthorityManager.getManager();
 	Map dcCounter = new HashMap();
 	HashMap traceFieldValue = new HashMap();
 	//String row = "even"; //ONLY used on bitstream table
 	String oldkey = "";
    for (int i = 0; i < dcv.size(); i++){
        // Find out how many values with this element/qualifier we've found

        String key = ChoiceAuthorityManager.makeFieldKey(dcv.get(i).getSchema(), dcv.get(i).getElement(), dcv.get(i).getQualifier());
		if(!key.equals(oldkey)) { 
			oldkey = key;
		}
		
        Integer count = (Integer) dcCounter.get(key);
        if (count == null){
            count = new Integer(0);
        }
        
        // Increment counter in map
        dcCounter.put(key, new Integer(count.intValue() + 1));

        // We will use two digits to represent the counter number in the parameter names.
        // This means a string sort can be used to put things in the correct order even
        // if there are >= 10 values for a particular element/qualifier.  Increase this to 
        // 3 digits if there are ever >= 100 for a single element/qualifer! :)
        String sequenceNumber = count.toString();
        
        while (sequenceNumber.length() < 2){
            sequenceNumber = "0" + sequenceNumber;
        }
        
 		String trClass = "";
 		String trFieldClass = "";
		String iconClass = "";
		String groupParent = "";
		String groupRepeatable = "";
		String identifierCompared = "";
		boolean collapsedItem = false;
		boolean firstRow = true; 
		
//				if(dcv.get(i).isHidden()) { trClass = "collapsed-item"; collapsedItem = true; } 
				//if(dcv.get(i).isRemoved()) { trClass += collapsedItem?" removed-item":"removed-item";}
		
		groupParent = dcv.get(i).getSchema() + dcv.get(i).getElement() + (dcv.get(i).getQualifier() == null ? "" : dcv.get(i).getQualifier());

		if(dcv.get(i).isRemoved()) { 
			trClass = "collapsed-item removed-item";
		} else {
			if (traceFieldValue.containsValue(groupParent+(dcv.get(i).getValue()).replaceAll(" ", "")+dcv.get(i).getAuthority())){
				trClass = "collapsed-item removed-item";
			} else {
				traceFieldValue.put(i, groupParent+(dcv.get(i).getValue()).replaceAll(" ", "")+dcv.get(i).getAuthority());
			}
		}
		
		List IDContentEquality = DeduplicationTagLibraryFunctions.getIDContentEquality(metadataExtraSourceInfo.get(dcv.get(i).getMetadataFieldId()), dcv.get(i).getValue(), dcv.get(i));
		if (IDContentEquality.size() > 0) {
			//identifierCompared = (IDContentEquality.get(0)).toString();
			for (int k = 0; k < IDContentEquality.size(); k++){
				if (((IDContentEquality.get(k)).toString()).equals(String.valueOf(item.getID()))){
					identifierCompared += "<div class=\"label label-info\"><i class=\"icon-stack\">" + (IDContentEquality.get(k)).toString() + "</i></div>&nbsp;";
				} else {
					identifierCompared += "<div class=\"label label-default\"><i class=\"icon-stack\">" + (IDContentEquality.get(k)).toString() + "</i></div>&nbsp;";
				}
			}
		}
		//if(dcv.get(i).isHidden() || item.getID()!=dcv.get(i).getOwner()) { 
		if(dcv.get(i).isHidden()) {
			trClass += " alert alert-danger";
			iconClass = "fa fa-ban";
		} else { 
			trClass += " alert alert-success";
			iconClass = "fa fa-check";
		}

      	Integer metaFieldId=dcv.get(i).getMetadataFieldId();
      	DCInput dcinput = dcinputs.get(metaFieldId);
      	
      	String label = null;
      	if(dcinput != null){
      		//if (IDContentEquality.size() < 1) {
      			trClass += " selectItem";
      		//}
      		label = dcinput.getLabel();
      		groupRepeatable = (dcinput.getRepeatable() == true) ? "true" : "false";
      	} else {
      		//field not in input form, but saved automatically
      		trFieldClass = "unknownField ";
      	}
		if(dcv.get(i).isBlocked()){
			trFieldClass += "blockedField ";
		}
          	if(sequenceNumber.equals("00") && firstRow) {
          		firstRow = false;
           		//dcinput.getRepeatable()
%>
		<tr data-group="<%= groupParent %>" class="<%= trFieldClass %>alert-block alert-warning" id="<%= dcv.get(i).getOwner() %>_<%= key %>_group">
            <td colspan="2" class="labelField">
            	<div style="color: #333;font-size: 1.3em;text-transform: uppercase; float: left;">
            		<i class="fa fa-expand" style="padding-top: 2px;"></i>
            		<span style="margin-left: 10px;">
           				<% if(label!=null) {%>
           				<%=label %> (
           				<%} %>
           				<%=dcv.get(i).getSchema() %>.<%= dcv.get(i).getElement() %>.<%= (dcv.get(i).getQualifier() == null ? "" : dcv.get(i).getQualifier()) %>
           				<% if(label!=null) {%>
           				)
           				<%} %>
					</span>
            	</div>
            	<div class="showAll" style="float: right;">
            	<% if (groupRepeatable == "false" && (items.size() < 3) && (IDContentEquality.size() == 1) && (metadataExtraSourceInfo.get(dcv.get(i).getMetadataFieldId())).size() > 1 ){ %>
            		<a style="cursor: pointer; cursor: hand;" class="modalbox_<%= dcv.get(i).getMetadataFieldId()%>" title="Show" href="javascript:void(0);">
            			<button class="btn btn-warning" type="button"><fmt:message key="jsp.deduplication.fieldShowDiff"/></button>
           			</a>
           		<%} %>
           			<button data-group="<%= groupParent %>" type="button" class="actionButton delete btn btn-danger"><i class="fa fa-trash-o fa-lg"></i><fmt:message key="jsp.deduplication.fieldDeleteLabel"/></button>
           		</div>
           	</td>
           	<td class="nomargin center"></td>
		</tr>
		<tr data-group="<%= groupParent %>" data-repeatable="<%= groupRepeatable %>" id="<%= dcv.get(i).getOwner() %>_<%= key %>_<%= sequenceNumber %>" class="<%= trFieldClass %><%= trClass %> <%= groupParent %> showAll">
			<td class="dataIco"><i class="<%= iconClass %>"></i></td>
			<td>
				<div>
                   <%
					if (cam.isChoicesConfigured(key)){
                   %>                   
                   <%=
						doAuthority(mam, cam, pageContext, request.getContextPath(), key, sequenceNumber,
                                dcv.get(i)).toString()
                   %>
                    <% } else { %>
                    <div style="font-size: 1.3em;height:75px;overflow: auto;padding: 5px; resize: vertical; text-align: justify;">
                       	<textarea class="hide form-control" readonly="readonly" id="<% if(dcv.get(i).isHidden() && dcv.get(i).isRemoved()) { %>hidden_<% } %>value_<%= key %>_<%= sequenceNumber %>" name="<% if(dcv.get(i).isHidden()) { %>hidden_<% } %>value_<%= key %>_<%= sequenceNumber %>" rows="1" cols="50"><%= dcv.get(i).getValue() %></textarea>
                       	<span class="txtCompare"><%= dcv.get(i).getValue() %></span>
					</div>                        
                    <% } %>
                    	<!-- div>
							<input type="hidden" class="form-control" readonly="readonly" name="language_<%= key %>_<%= sequenceNumber %>" value="<%= (dcv.get(i).getLanguage() == null ? "" : dcv.get(i).getLanguage()) %>" size="5"/>
							Language: <%= (dcv.get(i).getLanguage() == null ? "" : dcv.get(i).getLanguage()) %>
						</div -->
		            </div>		            
				</td>
				<td>
					<div style="text-align: center">
						<%= identifierCompared %>
					</div>
				</td>
			</tr>
            <% } else {%>
            <tr data-group="<%= groupParent %>" data-repeatable="<%= groupRepeatable %>" id="<%= dcv.get(i).getOwner() %>_<%= key %>_<%= sequenceNumber %>" class="<%= trFieldClass %><%= trClass %> <%= groupParent %> showAll">
            	<td class="dataIco"><i class="<%= iconClass %>"></i></td>
				<td>
					<div>
                    <%
                        if (cam.isChoicesConfigured(key))
                        {
                    %>                   
                    <%=
                        doAuthority(mam, cam, pageContext, request.getContextPath(), key, sequenceNumber,
                                dcv.get(i)).toString()
                    %>
                    <% } else { %>
                    	<div style="font-size: 1.3em;height:75px;overflow: auto;padding: 5px; resize: vertical; text-align: justify;">
                        	<textarea class="hide form-control" readonly="readonly" id="<% if(dcv.get(i).isHidden() && dcv.get(i).isRemoved()) { %>hidden_<% } %>value_<%= key %>_<%= sequenceNumber %>" name="<% if(dcv.get(i).isHidden()) { %>hidden_<% } %>value_<%= key %>_<%= sequenceNumber %>" rows="1" cols="50"><%= dcv.get(i).getValue() %></textarea>
                        	<span class="txtCompare"><%= dcv.get(i).getValue() %></span>
                        </div>                        
                    <% } %>
						<!-- div>
                    		<input type="hidden" class="form-control" readonly="readonly" name="language_<%= key %>_<%= sequenceNumber %>" value="<%= (dcv.get(i).getLanguage() == null ? "" : dcv.get(i).getLanguage()) %>" size="5"/>
                    		Language: <%= (dcv.get(i).getLanguage() == null ? "" : dcv.get(i).getLanguage()) %>
                		</div -->
		            </div>		            
				</td>
				<td>
					<div style="text-align: center">
						<%= identifierCompared %>
					</div>
				</td>
			</tr>
            <% } %>
	<%
	if(!key.equals(oldkey)) { %>
		<!-- /tbody -->	
	<% } 	
    } %>
</tbody>
</table>
</div>


<% 
dcCounter = new HashMap();

String trDiffClass = "";
String divClass = "";
for(int i = 0; i<dcTypes.length; i++) {
%>
<div id="modalbox_metadata_<%= dcTypes[i].getFieldID() %>" class="modalbox_metadata modal fade in" role="dialog" tabindex="-1" aria-hidden="false">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header">
				<button aria-hidden="true" data-dismiss="modal" class="close" type="button">×</button>
				<h4 class="modal-title"><%= metadataFields.get(dcTypes[i].getFieldID()) %></h4>
			</div>
			<div class="modal-body with-padding">
				<div class="tableDiff">
	   		<% 			 
	   			 List<DTODCValue> otherdcv = DeduplicationTagLibraryFunctions.groupDeduplication(metadataExtraSourceInfo.get(dcTypes[i].getFieldID()),item.getID());
	   			 
				 for(int j = 0; j<otherdcv.size(); j++) {
				        
					 	String key = MetadataField.formKey(otherdcv.get(j).getSchema(), otherdcv.get(j).getElement(), otherdcv.get(j).getQualifier());
	
				        Integer count = (Integer) dcCounter.get(key);
				        if (count == null)
				        {
				            count = new Integer(0);
				        }
				        
				        // Increment counter in map
				        dcCounter.put(key, new Integer(count.intValue() + 1));
	
				        // We will use two digits to represent the counter number in the parameter names.
				        // This means a string sort can be used to put things in the correct order even
				        // if there are >= 10 values for a particular element/qualifier.  Increase this to 
				        // 3 digits if there are ever >= 100 for a single element/qualifer! :)
				        String sequenceNumber = count.toString();
				        
				        while (sequenceNumber.length() < 2)
				        {
				            sequenceNumber = "0" + sequenceNumber;
				        }
				        trDiffClass = "secondItem";
				        divClass = "alert-danger";
						if(!sequenceNumber.equals("00")) {
							trDiffClass = "firstItem";
							divClass = "alert-success";
						}
	
			 %>
					
					<% if(otherdcv.get(j).isMasterDuplicate()==null || otherdcv.get(j).isMasterDuplicate()==true) { %>
					
					<div class="alert alert-block <%= divClass %> fade in block-inner">
						<h6><i class="icon-command"></i><%= otherdcv.get(j).getOwner()%></h6>
						<% if(!(otherdcv.get(j).getDuplicates().isEmpty())) { %>  <% for(Integer v : otherdcv.get(j).getDuplicates()) { %> <span class="label label-<%= legenda.get(v)%>"><%= v %></span><br/><% }%> <%} %>
						<hr>
						<p class="<%= trDiffClass %>"><%= otherdcv.get(j).getValue()%></p>
					</div>
					<% } %>
			<%	 					
				 }		
			%>
					<div class="alert alert-block alert-warning fade in">
						<h6><i class="icon-command"></i> <fmt:message key="jsp.deduplication.diff"/></h6>
						<hr>
						<p class="diffItem"></p>
					</div>
				</div>
			</div>
		</div>
	</div>
</div>

<% 
} 
%>
	<input type="hidden" name="mergeByUser" value="<%= mergeByUser%>"/>
	<input type="hidden" name="rule" value="${rule}"/>
	<input type="hidden" name="scope" value="${scope}"/>
	<input type="hidden" name="signatureType" value="${signatureType}"/>
	<input type="hidden" name="page" value="<%= StringUtils.isNotBlank(request.getParameter("page")) ? request.getParameter("page") : "" %>"/>
	<input type="hidden" name="handle" value="<%= StringUtils.isNotBlank(request.getParameter("handle")) ? request.getParameter("handle") : "" %>"/>
	<input type="hidden" id="table_values" name="table_values" value=""/>
	<input type="hidden" name="item_id" value="<%= item.getID() %>"/>
	<% for(Item hiddenItem : items) { %>
		<input type="hidden" name="itemremove_id" value="<%= hiddenItem.getID() %>"/>
	<% } %>
</form>			
</div>
</dspace:layout>
<br/>
<br/>