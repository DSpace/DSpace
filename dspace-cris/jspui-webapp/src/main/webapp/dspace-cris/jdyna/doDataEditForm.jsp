<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/sql" prefix="sql"%>
<%@ taglib uri="researchertags" prefix="researcher"%>
<%@ taglib uri="jdynatags" prefix="dyna"%>
<%@ taglib uri="http://ajaxtags.org/tags/ajax" prefix="ajax"%>

<%@page import="java.util.List"%>
<%@ page import="java.util.Locale"%>
<%@ page import="org.dspace.app.webui.util.UIUtil" %>
<%@page import="java.util.ArrayList"%>
<%@page import="it.cilea.osd.jdyna.model.PropertiesDefinition"%>
<%@page
	import="it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition"%>
<%@page
	import="org.dspace.app.cris.model.jdyna.DecoratorRestrictedField"%>
	
<%@page import="it.cilea.osd.jdyna.model.AccessLevelConstants"%>
<%@ page import="java.net.URL"%>
<%@ page import="org.dspace.eperson.EPerson" %>

<%
    // Is anyone logged in?
    EPerson user = (EPerson) request.getAttribute("dspace.current.user");

    // Is the logged in user an admin
    Boolean admin = (Boolean)request.getAttribute("isAdmin");
    boolean isAdmin = (admin == null ? false : admin.booleanValue());

    Locale sessionLocale = UIUtil.getSessionLocale(request);
	String currLocale = null;
	if (sessionLocale != null) {
		currLocale = sessionLocale.toString();
	}
%>
<c:set var="root"><%=request.getContextPath()%></c:set>
<c:set var="admin"><%=isAdmin%></c:set>
<c:set var="HIGH_ACCESS"><%=AccessLevelConstants.HIGH_ACCESS%></c:set>
<c:set var="ADMIN_ACCESS"><%=AccessLevelConstants.ADMIN_ACCESS%></c:set>
<c:set var="LOW_ACCESS"><%=AccessLevelConstants.LOW_ACCESS%></c:set>
<c:set var="STANDARD_ACCESS"><%=AccessLevelConstants.STANDARD_ACCESS%></c:set>
<c:set var="tabId" value="${anagraficadto.tabId}" />

<c:forEach items="${tabList}" var="areaIter" varStatus="rowCounter">
	<c:if test="${areaIter.id == tabId}">
	<c:set var="currTabIdx" scope="request" value="${rowCounter.count}" />
	</c:if>
</c:forEach>
	
	
<c:set var="commandObject" value="${anagraficadto}" scope="request" />

<c:set var="simpleNameAnagraficaObject"
	value="${simpleNameAnagraficaObject}" scope="page" />

<c:set var="disabledfield" value=" readonly='readonly'"></c:set>

<c:set var="dspace.layout.head.last" scope="request">	
    <style type="text/css">@import url(<%=request.getContextPath()%>/js/jscalendar/calendar-blue.css );</style>
	<link rel="stylesheet" href="<%= request.getContextPath() %>/css/commons-edit-jquery-for-cris.css" type="text/css" />    
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/researcher.css" type="text/css" />
    <link rel="stylesheet" href="<%= request.getContextPath() %>/css/jdyna.css" type="text/css" />    
    <link href="<%= request.getContextPath() %>/css/select2/select2.css" type="text/css" rel="stylesheet" />
    <link href="<%= request.getContextPath() %>/css/select2/select2-bootstrap.css" type="text/css" rel="stylesheet" />
    <link href="<%= request.getContextPath() %>/css/jstree/themes/default/style.min.css" type="text/css" rel="stylesheet" />
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/jscalendar/calendar.js"> </script>	
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/jscalendar/lang/calendar-en.js"> </script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/jscalendar/calendar-setup.js"> </script>
	<script type="text/javascript" src="<%= request.getContextPath() %>/js/jquery.form.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/select2/select2.min.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/jstree/jstree.min.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/jstree/thirdparty/_makeTree.js"></script>
	<script type="text/javascript" src="<%=request.getContextPath()%>/js/jstree/thirdparty/_queryTreeSort.js"></script>
		
	<style>
    .ui-autocomplete-loading {
        background: white url('../../../image/jdyna/indicator.gif') right center no-repeat;
    }    
    .ui-autocomplete {
        max-height: 100px;
        overflow-y: auto;
        /* prevent horizontal scrollbar */
        overflow-x: hidden;
    }
    /* IE 6 doesn't support max-height
     * we use height instead, but this forces the menu to always be this tall
     */
    * html .ui-autocomplete {
        height: 100px;
    }
    </style>
    
	  <script type="text/javascript"><!--

		var j = jQuery.noConflict();
  	
	    // Setup the ajax indicator
	    
	    j('#ajaxBusy').css({
		    display:"none",
		    margin:"0px",
		    paddingLeft:"0px",
		    paddingRight:"0px",
		    paddingTop:"0px",
		    paddingBottom:"0px",
		    position:"absolute",
		    right:"3px",
		    top:"3px",
		    width:"auto"
	    });

	    j.ajaxSetup({
	        beforeSend:function(){
	            // show gif here, eg:
	        	j('#ajaxBusy').show();
	        },
	        complete:function(){
	            // hide gif here, eg:
	            j("#ajaxBusy").hide();
	        }
	    });
  	
	var activeTab = function(){
		j(".box:not(.expanded)").accordion({
			autoHeight: false,
			navigation: true,
			collapsible: true,
			active: 0,
			heightStyle: "content"
		});
		j(".box.expanded").accordion({
			autoHeight: false,
			navigation: true,
			collapsible: true,
			active: 0,
			heightStyle: "content"
		});
		
		var ajaxurlrelations = "<%=request.getContextPath()%>/cris/${specificPartPath}/viewNested.htm";
		j('.nestedinfo').each(function(){
			var id = j(this).html();
			j.ajax( {
				url : ajaxurlrelations,
				data : {																			
					"parentID" : ${anagraficadto.objectId},
					"typeNestedID" : id,
					"pageCurrent": j('#nested_'+id+"_pageCurrent").html(),
					"limit": j('#nested_'+id+"_limit").html(),
					"editmode": true,
					"totalHit": j('#nested_'+id+"_totalHit").html(),
					"admin": ${admin}
				},
				success : function(data) {																										
					j('#viewnested_'+id).html(data);
					var ajaxFunction = function(page){
						j.ajax( {
							url : ajaxurlrelations,
							data : {																			
								"parentID" : ${anagraficadto.objectId},
								"typeNestedID" : id,													
								"pageCurrent": page,
								"limit": j('#nested_'+id+"_limit").html(),
								"editmode": true,
								"totalHit": j('#nested_'+id+"_totalHit").html(),
								"admin": ${admin}
							},
							success : function(data) {									
								j('#viewnested_'+id).html(data);
								jQuery( "#ui-accordion" ).accordion( "resize" );
								postfunction();
							},
							error : function(data) {
							}
						});		
					};
					var postfunction = function(){
						j('#viewnested_'+id+' .nested_edit_button').parent().parent().mouseover(function(){
							j(this).toggleClass('ui-state-hover');
						});
						j('#viewnested_'+id+' .nested_edit_button').parent().parent().mouseout(function(){
							j(this).toggleClass('ui-state-hover');
						});
						j('#viewnested_'+id+' .nested_edit_button').click(function(){
							var ajaxurleditnested = 
								"<%= request.getContextPath() %>/cris/tools/${specificPartPath}/editNested.htm";
								j.ajax( {
									url : ajaxurleditnested,
									data : {
										"elementID": j(this).attr('id').substr(('nested_'+id+'_edit_').length),
										"parentID" : ${anagraficadto.objectId},
										"typeNestedID" : id,
										"editmode" : true,
										"admin": ${admin}
									},
									success : function(data) {
										j('#nested_edit_dialog').html(data);
										j('#nested_edit_dialog input:submit').button();
										var options = { 
										        target: '#viewnested_'+id,   // target element(s) to be updated with server response 
										        success: function(){ // post-submit callback
										        	j('#nested_edit_dialog').dialog("close");        
										        	postfunction();
									        	}
										};
										j('#nested_edit_form').ajaxForm(options); 
										j('#nested_edit_dialog').dialog("option",{title: 'Edit: '+j('#viewnested_'+id+' span.dynaLabel').html()});
										j('#nested_edit_dialog').dialog("open");
									},
									error : function(data) {
									}
								});	
						});
						j('#viewnested_'+id+' .nested_delete_button').click(function(){
							var ajaxurldeletenested = 
								"<%= request.getContextPath() %>/cris/tools/${specificPartPath}/deleteNested.htm";
								j.ajax( {
									url : ajaxurldeletenested,
									data : {
										"elementID": j(this).attr('id').substr(('nested_'+id+'_delete_').length),
										"parentID" : ${anagraficadto.objectId},
										"typeNestedID" : id,
										"editmode" : true,
										"admin": ${admin}
									},
									success : function(data) {
										j('#viewnested_'+id).html(data);
										postfunction();
									},
									error : function(data) {
									}
								});	
						});
						j('#viewnested_'+id+' .nested_preferred_button').click(function(){
							var ajaxurlpreferrednested = 
								"<%= request.getContextPath() %>/cris/tools/${specificPartPath}/preferredNested.htm";
								j.ajax( {
									url : ajaxurlpreferrednested,
									data : {
										"elementID": j(this).attr('id').substr(('nested_'+id+'_preferred_').length),
										"parentID" : ${anagraficadto.objectId},
										"typeNestedID" : id,
										"editmode" : true,
										"preferred" : true,
										"admin": ${admin}
									},
									success : function(data) {
										j('#viewnested_'+id).html(data);
										postfunction();
									},
									error : function(data) {
									}
								});	
						});
						j('#viewnested_'+id+' .nested_notpreferred_button').click(function(){
							var ajaxurlnotpreferrednested = 
								"<%= request.getContextPath() %>/cris/tools/${specificPartPath}/notPreferredNested.htm";
								j.ajax( {
									url : ajaxurlnotpreferrednested,
									data : {
										"elementID": j(this).attr('id').substr(('nested_'+id+'_notpreferred_').length),
										"parentID" : ${anagraficadto.objectId},
										"typeNestedID" : id,
										"editmode" : true,
										"preferred" : false,
										"admin": ${admin}
									},
									success : function(data) {
										j('#viewnested_'+id).html(data);
										postfunction();
									},
									error : function(data) {
									}
								});	
						});
						j('#nested_'+id+'_addbutton').click(function(){
							var ajaxurladdnested = 
								"<%= request.getContextPath() %>/cris/tools/${specificPartPath}/addNested.htm";
								j.ajax( {
									url : ajaxurladdnested,
									data : {			
										"parentID" : ${anagraficadto.objectId},
										"typeNestedID" : id,
										"admin": ${admin}
									},
									success : function(data) {
										j('#nested_edit_dialog').html(data);
										j('#nested_edit_dialog input:submit').button();
										var options = { 
										        target: '#viewnested_'+id,   // target element(s) to be updated with server response 
										        success: function(){ // post-submit callback
										        	j('#nested_edit_dialog').dialog("close");        
										        	postfunction();
									        	}
										};
										j('#nested_edit_form').ajaxForm(options); 
										j('#nested_edit_dialog').dialog("option",{title: 'Add new: '+j('#viewnested_'+id+' span.dynaLabel').html()});
										j('#nested_edit_dialog').dialog("open");
									},
									error : function(data) {
									}
								});	
						});
						j('#nested_'+id+'_next').click(
								function() {
							    	ajaxFunction(parseInt(j('#nested_'+id+"_pageCurrent").html())+1);
									
						});
						j('#nested_'+id+'_prev').click(
								function() {
									ajaxFunction(parseInt(j('#nested_'+id+"_pageCurrent").html())-1);
						});
						j('.nested_'+id+'_nextprev').click(
								function(){
									ajaxFunction(j(this).attr('id').substr(('nested_'+id+'_nextprev_').length));
						});
					};
					postfunction();
				},
				error : function(data) {
				}
			});
		});
	};
	
	j(document).ready(function()
	{
		j('.jdynadropdown').select2();
		
		j('#nested_edit_dialog').dialog({
			autoOpen: false,
			modal: true,
			width: 720
		});			
		j('input:submit').button();
		j('#delete').button();
		j("#tabs").tabs({
			active: ${currTabIdx-1},
			"activate": function( event, ui ) {
				j("li.ui-tabs-active").toggleClass("ui-tabs-active ui-state-active active");
				j('#newTabId').val(j(ui.newTab).attr('id').substr(8));
				j('#submit_save').click();
			},			"beforeActivate": function( event, ui ) {
   			 j("li.active").toggleClass("active");
			},
	   		"create": function( event, ui ) {
	               j("div.ui-tabs").toggleClass("ui-tabs ui-widget ui-widget-content ui-corner-all tabbable");
	               j("ul.ui-tabs-nav").toggleClass("ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all nav nav-tabs");
	               j("li.ui-tabs-active").toggleClass("ui-state-default ui-corner-top ui-tabs-active ui-state-active active");
	               j("li.ui-state-default").toggleClass("ui-state-default ui-corner-top");
	               j("div.ui-tabs-panel").toggleClass("ui-tabs-panel ui-widget-content ui-corner-bottom tab-content with-padding");
	        }
		});
		
		j('.navigation-tabs:not(.expanded)').accordion({
			collapsible: true,
			active: 0,
			event: "click mouseover"
		});
		j('.navigation-tabs.expanded').accordion({
			collapsible: true,
			active: 0,
			event: "click mouseover"
		});
		
		activeTab();
		activePointer();
		activeCustomPointer();
		activeTree();
	});

	
	function updateSelectedPointer( id, count, repeatable, displayvalue, identifiervalue ) {
		if(identifiervalue!=null) {
        	if (!repeatable){
        		j("#pointer_"+id+"_selected").html(' ');
        		count = 0;
        	}
			var div = j('<div id="pointer_'+id+'_selected_'+count+'" class="jdyna-pointer-value">');
        	var img = j('<img class="jdyna-icon jdyna-action-icon jdyna-delete-button" src="<%= request.getContextPath() %>/image/jdyna/delete_icon.gif">');
			var path = j('#pointer_'+id+'_path').html();
			var input = j( "<input type='hidden' id='"+path+"["+count+"]"+"' name='"+path+"["+count+"]"+"'>" ).val(identifiervalue);
        	var display = j("<span>").text(displayvalue);
        	var selectedDiv = j("#pointer_"+id+"_selected");
        	selectedDiv.append(div);
        	div.append(input);
        	div.append(display);
        	div.append("&nbsp;")
        	div.append(img);
        	div.effect('highlight');
        	j('#pointer_'+id+'_tot').html(count+1);
        	img.click(function(){
            	if (!repeatable){
            		selectedDiv.html(' ');
            		var _input = j( "<input type='hidden' id='_"+path+"[0]"+"' name='_"+path+"[0]"+"'>" ).val('true');
            		selectedDiv.append(_input);
            	}
            	else
            	{
            		j('#pointer_'+id+'_selected_'+count).remove();
            	}
        	});
        	if (!repeatable){
        		var _input = j( "<input type='hidden' id='_"+path+"[0]"+"' name='_"+path+"[0]"+"'>" ).val('true');
        		selectedDiv.append(_input);
        	}            	
		}
    }
	
	var activePointer = function() {
				 			
		 j(".pointerinfo").each(function(){
			 var id = j(this).html();
			 j('#pointer_'+id+'_selected div img').click(
					 function(){
				j(this).parent().remove();		 
			 });
			 var repeatable = j('#pointer_'+id+'_repeatable').html() == 'true';
			 j("#searchboxpointer_"+id).autocomplete({
				delay: 500,
	            source: function( request, response ) {	
	                j.ajax({
	                    url: "searchPointer.htm",
	                    dataType: "json", 
	                    data : {																			
							"elementID" : id,								
							"query":  request.term						
						},                  
	                    success: function( data ) {
	                        response( j.map( data.pointers, function( item ) {
	                            return {
	                                label: item.display,
	                                value: item.id
	                            }
	                        }));
	                    }
	                });
	            },		            
	            minLength: 2,
	            select: function( event, ui ) {
	            	if (ui == null || ui.item == null) return false;
	            	updateSelectedPointer( id, j('#pointer_'+id+'_tot').html(), repeatable, 
	                		ui.item.label, ui.item.value);
	            	j('#searchboxpointer_'+id).val('');
	            	return false;
	            },
	            open: function() {
	                j( this ).removeClass( "ui-corner-all" ).addClass( "ui-corner-top" );
	            },
	            close: function() {
	                j( this ).removeClass( "ui-corner-top" ).addClass( "ui-corner-all" );
	            }
	        });
	});
}

	var activeTree = function() {
		 j(".classificationtreeinfo").each(function(){
			 var id = j(this).html();
			 var treeObjectType = j('#classificationtree_'+id+'_treeObjectType').html();
			 var rootResearchObject = j('#classificationtree_'+id+'_rootResearchObject').html();
			 var metadataBuilderTree = j('#classificationtree_'+id+'_metadataBuilderTree').html();
			 var chooseOnlyLeaves = j('#classificationtree_'+id+'_chooseOnlyLeaves').html();
			 var repeatable = j('#classificationtree_'+id+'_repeatable').html();
			 var propertyPath = j('#classificationtree_'+id+'_propertyPath').html();
			
			 j('#classificationtree_'+id+'_selected div img').click(
					 function(){
						 j(this).parent().remove();
			 });
			 j('#classificationtree_'+id+'_btn').click(
			 function(event){
		     j('#classificationtree_modal').modal();	 
			 event.preventDefault()
			 j('#classificationtree_modal .modal-body').html('');
			 j('#classificationtree_modal .modal-footer').html('');
			 
			 j('#classificationtree_modal .modal-body').append("<div id=\"jstree_div\"></div>")
			 j('#classificationtree_modal .modal-footer').append("<button type=\"button\" id=\"btn-close-modal-classificationtree\" class=\"btn btn-default\" data-dismiss=\"modal\">Close</button>")
			 j('#classificationtree_modal .modal-footer').append("<button type=\"button\" id=\"button-save-classificationtree_"+propertyPath+"\" class=\"btn btn-primary btn-classificationtree-save\">Done</button>")
			 				
			j('.btn-classificationtree-save').click(
					 function(event){
						  var propertyPath = j(this).attr("id");
						  var realPP = propertyPath.replace('button-save-classificationtree_anagraficadto.','');
						  var n = j('#classificationtree_modal .modal-body').find(".jstree-clicked");
						  if(n.length>0) {
							  if(n.length>1) {									  
								 j("#classificationtree_"+id+"_selected").html('');
								 j.each(n, function( index, value ) {
									 var idValue = j(this).parent().attr("id");
									 var labelValue = j(this).text();				
									 var div = j("<div id=\"classificationtree_"+ id +"_selected_"+index+"\">");
									 var input = j("<input type=\"hidden\" id=\""+realPP+"["+index+"]\" name=\""+realPP+"["+index+"]\">").val(idValue);										 
									 var label = j("<span>").text(labelValue);
									 div.append(input);										 
									 div.append(label);
									 j("#classificationtree_"+id+"_selected").append(div);
									 
						           	 var img = j("<img class=\"jdyna-icon jdyna-action-icon jdyna-delete-button\" src=\"<%=request.getContextPath()%>/image/jdyna/delete_icon.gif\">");
						             j("#classificationtree_"+id+"_selected_"+index).append(img);
					                 img.click(function(){                     	
					                 	j("#classificationtree_"+id+"_selected_"+index).html('');
					                 	var _input = j( "<input type='hidden' id='_"+realPP+"["+index+"]"+"' name='_"+realPP+"["+index+"]"+"'>" ).val('true');
					                 	j("#classificationtree_"+id+"_selected_"+index).append(_input);             
					                 });
								 });
							  }
							  else {
								var idValue = j(n).parent().attr("id");
								var labelValue = j(n).text();
								j("#classificationtree_"+id+"_selected").html('');
								var div = j("<div id=\"classificationtree_"+ id +"_selected_0\">");
								var input = j("<input type=\"hidden\" id=\""+realPP+"[0]\" name=\""+realPP+"[0]\">").val(idValue);
								
								var label = j("<span>").text(labelValue);
								div.append(input);									
								div.append(label);
								j("#classificationtree_"+id+"_selected").append(div);
				            	var img = j("<img class=\"jdyna-icon jdyna-action-icon jdyna-delete-button\" src=\"<%=request.getContextPath()%>/image/jdyna/delete_icon.gif\">");
				            	j("#classificationtree_"+id+"_selected").append(img);
			                     img.click(function(){                     	
			                        j("#classificationtree_"+id+"_selected").html('');
			                        var _input = j( "<input type='hidden' id='_"+realPP+"[0]"+"' name='_"+realPP+"[0]"+"'>" ).val('true');
			                      	j("#classificationtree_"+id+"_selected").append(_input);                     	
			                  	 });
							  }
						  }
						  
						  j("#classificationtree_modal").modal("hide");
					 }
					 
			);

			 j('#jstree_div').jstree({
				 'core' : {
				   'data' : {
				     'url': 'buildClassificationTree.htm?method=buildtree&id='+rootResearchObject+'&type='+treeObjectType+'&builder='+metadataBuilderTree,						 
				   	}
				  },
			 	  "checkbox" : {
			      	"keep_selected_style" : false
			 	  },
		    	  "types" : {
		    	 	"default" : {
		    	 		"icon" : "fa fa-flash"
		    	 	}
		    	  },			    	  
	    	 	  "plugins" : [ "checkbox", "types"]		    	 	 
			});

		 });
	 });
		 
	}
	
	function updateSelectedCustomPointer( id, count, repeatable, displayvalue, identifiervalue ) {
		if(identifiervalue!=null) {
        	if (!repeatable){
        		j("#custompointer_"+id+"_selected").html(' ');
        		count = 0;
        	}
			var div = j('<div id="custompointer_'+id+'_selected_'+count+'" class="jdyna-pointer-value">');
        	var img = j('<img class="jdyna-icon jdyna-action-icon jdyna-delete-button" src="<%= request.getContextPath() %>/image/jdyna/delete_icon.gif">');
			var path = j('#custompointer_'+id+'_path').html();
			var input = j( "<input type='hidden' id='"+path+"["+count+"]"+"' name='"+path+"["+count+"]"+"'>" ).val(identifiervalue);
        	var display = j("<span>").text(displayvalue);
        	var selectedDiv = j("#custompointer_"+id+"_selected");
        	selectedDiv.append(div);
        	div.append(input);
        	div.append(display);
        	div.append("&nbsp;")
        	div.append(img);
        	div.effect('highlight');
        	j('#custompointer_'+id+'_tot').html(count+1);
        	img.click(function(){
            	if (!repeatable){
            		selectedDiv.html(' ');
            		var _input = j( "<input type='hidden' id='_"+path+"[0]"+"' name='_"+path+"[0]"+"'>" ).val('true');
            		selectedDiv.append(_input);
            	}
            	else
            	{
            		j('#custompointer_'+id+'_selected_'+count).remove();
            	}
        	});
        	if (!repeatable){
        		var _input = j( "<input type='hidden' id='_"+path+"[0]"+"' name='_"+path+"[0]"+"'>" ).val('true');
        		selectedDiv.append(_input);
        	}            	
		}
    }
	
	var activeCustomPointer = function() {
			
		 j(".custompointerinfo").each(function(){
			 var id = j(this).html();
			 j('#custompointer_'+id+'_selected div img').click(
					 function(){
				j(this).parent().remove();		 
			 });
			 var repeatable = j('#custompointer_'+id+'_repeatable').html() == 'true';
			 var type = j('#custompointer_'+id+'_type').html();
			 j("#searchboxcustompointer_"+id).autocomplete({
				delay: 500,
	            source: function( request, response ) {	
	                j.ajax({
	                    url: "searchCustomPointer.htm",
	                    dataType: "json", 
	                    data : {																			
							"elementID" : id,								
							"query":  request.term,
							"type": type
						},                  
	                    success: function( data ) {
	                        response( j.map( data.pointers, function( item ) {
	                            return {
	                                label: item.display,
	                                identifier: item.identifyingValue
	                            }
	                        }));
	                    }
	                });
	            },		            
	            minLength: 2,
	            select: function( event, ui ) {
	            	if (ui == null || ui.item == null) return false;
	            	updateSelectedCustomPointer( id, j('#custompointer_'+id+'_tot').html(), repeatable, 
	                		ui.item.label, ui.item.identifier);
	            	j('#searchboxcustompointer_'+id).val('');
	            	return false;
	            },
	            open: function() {
	                j( this ).removeClass( "ui-corner-all" ).addClass( "ui-corner-top" );
	            },
	            close: function() {
	                j( this ).removeClass( "ui-corner-top" ).addClass( "ui-corner-all" );
	            }
	        });
	});

	}
		-->
	</script>
	
</c:set>
<dspace:layout title="${entity.typo.label}" style="submission" navbar="off">

<h1>${entity.name} <c:if test="${admin}"> <a id="delete" href="delete.htm?id=${entity.id}"><span class="fa fa-trash"></span></a></c:if></h1>


<c:if test="${not empty messages}">
	<div class="message" id="successMessages"><c:forEach var="msg"
		items="${messages}">
		<div id="authority-message" class="alert alert-success">${msg}</div>
	</c:forEach></div>
	<c:remove var="messages" scope="session" />
</c:if>

<div id="ajaxBusy" style="display:none;height:100%;width:100%;z-index: 2001;" class="ui-widget-overlay"><p><img alt="Loading..." style="position: fixed;left: 50%;top: 35%;" src="<%= request.getContextPath() %>/image/jdyna/indicator.gif"/></p></div>
<div id="researcher">
<form:form commandName="anagraficadto"
	action="" method="post" enctype="multipart/form-data">
	<%-- if you need to display all errors (both global and all field errors,
		 use wildcard (*) in place of the property name --%>
	<spring:bind path="anagraficadto.*">
		<c:if test="${!empty status.errorMessages}">
			<div id="errorMessages">
		</c:if>
		<c:forEach items="${status.errorMessages}" var="error">
			<span class="errorMessage alert alert-danger"><fmt:message
				key="jsp.layout.hku.prefix-error-code" /> ${error}</span>
			<br />
		</c:forEach>
		<c:if test="${!empty status.errorMessages}">
			</div>
		</c:if>
	</spring:bind>

<div class="panel panel-default">
<div class="container">
	<div class="col-md-6">
		<div class="cris-edit-status">
		<spring:bind path="status">
			<c:set var="inputValue">
				<c:out value="${status.value}" escapeXml="true"></c:out>
			</c:set>
			<c:set var="inputName">
				<c:out value="${status.expression}" escapeXml="false"></c:out>
			</c:set>

			<span class="cris-record-info-status"><b><fmt:message
				key="jsp.layout.hku.label.status" /></b>

			
			
			<input id="${inputName}" name="${inputName}"
					type="radio" value="false"
					<c:if test="${inputValue==false}">checked="checked"</c:if> />
				<fmt:message
					key="jsp.layout.hku.label.status.0" />
			<input id="${inputName}" name="${inputName}"
					type="radio" value="true"
					<c:if test="${inputValue==true}">checked="checked"</c:if> />
				<fmt:message
					key="jsp.layout.hku.label.status.1" />
			
			<input name="_${inputName}" id="_${inputName}"
				value="true" type="hidden" />
			</span>
		</spring:bind>
		</div>
		</div>
		<div class="col-md-6">
		
		<div class="cris-edit-record-info">
		<c:set var="disabled" value=" readonly='readonly'"/>
		<c:choose>
		<c:when test="${admin}">
			<dyna:text labelKey="jsp.cris.detail.info.sourceid" propertyPath="anagraficadto.sourceID" visibility="false"/>
			<div class="dynaClear">&nbsp;</div>			
			<dyna:text labelKey="jsp.cris.detail.info.sourceref" propertyPath="anagraficadto.sourceRef" visibility="false"/>
			<div class="dynaClear">&nbsp;</div>
		</c:when>
		<c:otherwise>
			<span class="cris-record-info-sourceid"><b><fmt:message key="jsp.cris.detail.info.sourceid" /></b> ${!empty anagraficadto.sourceID?anagraficadto.sourceID:i18nnone}</span>
			<span class="cris-record-info-sourceref"><b><fmt:message key="jsp.cris.detail.info.sourceref" /></b> ${!empty anagraficadto.sourceRef?anagraficadto.sourceRef:i18nnone}</span>
		</c:otherwise>
		</c:choose>
			<span class="cris-record-info-created"><b><fmt:message key="jsp.cris.detail.info.created" /></b> ${anagraficadto.timeStampCreated}</span>
			<span class="cris-record-info-updated"><b><fmt:message key="jsp.cris.detail.info.updated" /></b> ${anagraficadto.timeStampModified}</span>
		</div>
		</div>
	</div>
	</div>
	<dyna:hidden propertyPath="anagraficadto.objectId" />
	<input type="hidden" id="newTabId" name="newTabId" />
	
	
	<p style="color: red; text-decoration: underline; font-weight: bold; text-align: center;"><fmt:message key='jsp.rp.edit-tips'/></p>

				<div id="tabs">
		<ul>
					<c:forEach items="${tabList}" var="area" varStatus="rowCounter">
			<li id="bar-tab-${area.id}">
				<a href="#tab-${area.id}">
				<c:if test="${!empty area.ext}">
				<img style="width: 16px;vertical-align: middle;" border="0" 
					src="<%=request.getContextPath()%>/cris/researchertabimage/${area.id}" alt="icon" />
				</c:if>	
				${area.title}</a>
			</li>
					</c:forEach>
		</ul>

<c:forEach items="${tabList}" var="area" varStatus="rowCounter">
	<c:if test="${area.id != tabId}">
	<div id="tab-${area.id}">
	Saving data... tab data will be shown soon
	</div>
	</c:if>
</c:forEach>		
		<div id="tab-${tabId}">
	
				<c:forEach items="${propertiesHolders}" var="holder">
				
				<c:set value="${researcher:isThereMetadataNoEditable(holder.shortName, holder.class)}" var="isThereMetadataNoEditable"></c:set>
					
					
							<%!public URL fileURL;%>

							<c:set var="urljspcustom"
								value="/dspace-cris/jdyna/custom/edit${holder.externalJSP}.jsp" scope="request" />
								
							<%
								String filePath = (String)pageContext.getRequest().getAttribute("urljspcustom");

										fileURL = pageContext.getServletContext().getResource(
												filePath);
							%>

							<%
								if (fileURL != null) {
							%>
				
											
				
				
						<c:set var="holder" value="${holder}" scope="request"/>
						<c:set var="isThereMetadataNoEditable" value="${isThereMetadataNoEditable}" scope="request"/>												
						<c:import url="${urljspcustom}" />
					
					
							<%
								} else {
							%>
					
						<div id="hidden_first${holder.shortName}">&nbsp;</div>
						<div id="${holder.shortName}" class="box ${holder.collapsed?"":"expanded"}">
						  <h3><a href="#">${holder.title}</a></h3>
						  <div>
						<c:forEach
							items="${propertiesDefinitionsInHolder[holder.shortName]}"
							var="tipologiaDaVisualizzareNoI18n">
							<c:set var="tipologiaDaVisualizzare" value="${researcher:getPropertyDefinitionI18N(tipologiaDaVisualizzareNoI18n,currLocale)}" />
							<c:set var="hideLabel">${fn:length(propertiesDefinitionsInHolder[holder.shortName]) le 1}</c:set>
							<c:set var="disabled" value=" readonly='readonly'"/>
							
							<c:set var="show" value="true" />
							<c:choose>							
							<c:when
								test="${admin or (tipologiaDaVisualizzare.accessLevel eq HIGH_ACCESS)}">
								<c:set var="disabled" value="" />
								<c:set var="visibility" value="true" />
							</c:when>
							<c:when 
								test="${(tipologiaDaVisualizzare.accessLevel eq LOW_ACCESS)}">
								<c:set var="disabled" value="${disabledfield}" />
								<c:set var="visibility" value="false" />
							</c:when>
							<c:when 
								test="${(tipologiaDaVisualizzare.accessLevel eq STANDARD_ACCESS)}">
								<c:set var="disabled" value="${disabledfield}" />
								<c:set var="visibility" value="true" />
							</c:when>
							<c:otherwise>
								<c:set var="show" value="false" />
							</c:otherwise>
							</c:choose>	
							<c:if
								test="${show && dyna:instanceOf(tipologiaDaVisualizzare,'it.cilea.osd.jdyna.model.ADecoratorTypeDefinition')}">

								<c:set var="totalHit" value="0"/>
								<c:set var="limit" value="5"/>
								<c:set var="offset" value="0"/>											
								<c:set var="pageCurrent" value="0"/>	
								<c:set var="editmode" value="true"/>
								
								<div
									id="viewnested_${tipologiaDaVisualizzare.real.id}" class="viewnested">
										<img src="<%=request.getContextPath()%>/image/cris/bar-loader.gif" class="loader" />
											<fmt:message key="jsp.jdyna.nestedloading" />
								<span class="spandatabind nestedinfo">${tipologiaDaVisualizzare.real.id}</span>
								<span id="nested_${tipologiaDaVisualizzare.real.id}_totalHit" class="spandatabind">0</span>
								<span id="nested_${tipologiaDaVisualizzare.real.id}_limit" class="spandatabind">5</span>
								<span id="nested_${tipologiaDaVisualizzare.real.id}_pageCurrent" class="spandatabind">0</span>
								<span id="nested_${tipologiaDaVisualizzare.real.id}_editmode" class="spandatabind">false</span>
								</div>
							</c:if>


							<c:if
								test="${show && (dyna:instanceOf(tipologiaDaVisualizzare,'it.cilea.osd.jdyna.model.ADecoratorPropertiesDefinition'))}">
								
								<%
								List<String> parameters = new ArrayList<String>();
												parameters.add(pageContext.getAttribute(
														"simpleNameAnagraficaObject").toString());
												parameters
														.add(((ADecoratorPropertiesDefinition) pageContext
																.getAttribute("tipologiaDaVisualizzare"))
																.getShortName());
												pageContext.setAttribute("parameters", parameters);
								%>
								<dyna:edit tipologia="${tipologiaDaVisualizzare.object}" disabled="${disabled}"
									propertyPath="anagraficadto.anagraficaProperties[${tipologiaDaVisualizzare.shortName}]"
									ajaxValidation="validateAnagraficaProperties" hideLabel="${hideLabel}"
									validationParams="${parameters}" visibility="${visibility}"/>
									
							</c:if>

						</c:forEach>
		</div>	
</div>	
				
<% } %>
				</c:forEach>
<br/>
<div class="jdyna-form-button">
				<input id="submit_save" class="btn btn-primary" type="submit"
					value="<fmt:message key="jsp.layout.hku.researcher.button.save"/>" />
				<input type="submit" class="btn btn-default" name="cancel"
					value="<fmt:message key="jsp.layout.hku.researcher.button.cancel"/>" />
					</div>
</div>						
</div>				
				
</form:form>
</div>
<div id="nested_edit_dialog">&nbsp;</div>
<div id="classificationtree_modal" class="modal fade">
  <div class="modal-dialog">
    <div class="modal-content">
      <div class="modal-header">
        <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
        <h4 class="modal-title"></h4>
      </div>
      <div class="modal-body">
		<div id="jstree_div"></div>         
      </div>
      <div class="modal-footer">
        
      </div>
    </div><!-- /.modal-content -->
  </div><!-- /.modal-dialog -->
</div><!-- /.modal -->
</dspace:layout>
