/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
/* 
  The contents of this file are subject to the license and copyright
   detailed in the LICENSE and NOTICE files at the root of the source
   tree and available online at
   
   https://github.com/CILEA/dspace-cris/wiki/License

*/
var pubTable = null;
var currSection = "active";
var my_aData = null;
function getJQueryObjectTrPublication(element)
	{
		return j("#publicationTable"+" tbody tr."+element);
	}
	/* Return an array of ids (this ids are the visible element on table-<element> (e.g. table-active) )*/
	function getAllNotCollapsedItemUtil(element) {
		//tableselected.fnFilter( '' );
		var object1 = getJQueryObjectTrPublication(element);
		var values = new Array();
		for ( var i = 0; i < object1.length; i++) {
			var v = object1[i].id;
			values.push(v);
		}
		return values;
	}
	
	/* Utility method to mantain counters and show info message for each container */
	function utilCount(element) {
		
		var values = getJQueryObjectTrPublication(element);
		j(".control"+element+"h3").html(values.length + " items");
		return values.length;
	}
	
	function internalDrop(context, element) {
		j(context).addClass(element);
		j(context).addClass("filtered");
		j(context).removeClass(currSection);
		utilCount(currSection);
		var count = utilCount(element);
		if ("selected" == element)
		{
			j(context).removeClass("nodrop");
			j(context.cells[0]).html(count);
			j(context.cells[1]).removeClass("filtered");
			j(context).hover(function() {			
		          j(this.cells[1]).addClass("showDragHandle");
		    }, function() {
		          j(this.cells[1]).removeClass("showDragHandle");
		    });

		}
		else
		{
			j(context).addClass("nodrop");
			j(context.cells[1]).addClass("filtered");
			j(context).unbind("hover");
		}
	}
	
	function countSelectedRowBefore(index)
	{
		return j("#publicationTable tr:lt("+index+")").filter(".selected").length;		
	}
	
	/* Change status of the item 'ui */
	function utilDrop(ui, prefix_element) {
		var context = j(ui.draggable.context);
		internalDrop(context, prefix_element);
	}

	function showLoading()
	{
		j("#publicationTable_filter").addClass("invisible");
		pubTable.addClass("invisible");
		j("#processing").removeClass("invisible");
	}
	
	function loadComplete()
	{
		j("#processing").addClass("invisible");
		pubTable.removeClass("invisible");
	}
	
	/* Generic method to control container and central elements hidden/showed */
	function controlContainer(container) {
		j("."+currSection).addClass("filtered");
		j(".drop"+currSection).removeClass("filtered");
		j("#droppable"+currSection).removeClass("collapsed-container");
		j("."+container).removeClass("filtered");
		j(".drop"+container).addClass("filtered");
		j("#droppable"+container).addClass("collapsed-container");

		if (container != "selected")
		{
			j("#publicationTable_filter").removeClass("invisible");
			j("#publicationTable tr th").removeClass("nosort");
			j(j("#publicationTable tr th")[1]).addClass("filtered");
			pubTable.fnSort([[2,'asc']]);
		}
		else
		{
			j("#publicationTable tr th").addClass("nosort");
			j(j("#publicationTable tr th")[1]).removeClass("filtered");
			pubTable.fnSort([[0,'asc']]);
		}
		
		currSection = container;
	}
	
/* Set hidden input data to send server*/
function setDataFormUtil(element) {		

	var values = getAllNotCollapsedItemUtil(element);			
	var rpMPForm = j("#rpMPForm");
	
	for ( var i = 0; i < values.length; i++) {
		var v = values[i];
		var splitted = v.split("_");
		var child = document.createElement("input");
		child.setAttribute('type', "hidden");
		child.setAttribute('name', element);
		child.setAttribute('value', splitted[1]);
		j(child).appendTo(rpMPForm);
	}
}	

function initializeRIAForm()
{
	/* Manage sortable table with id table-selected*/
	j("#publicationTable").tableDnD({								
		onDrop: function(table, row) {
				j("#message").removeClass("invisible");
//				var newPos = countSelectedRowBefore(row.rowIndex);
//				j(row.cells[0]).html(newPos +1);
//				var tmp = newPos+1;
//				j("#publicationTable tr.selected:gt("+newPos+")").each(
				var tmp = 0;
				j("#publicationTable tr.selected").each(
						function()
						{
							j(this.cells[0]).html(++tmp);
						});
		}			
	});
	
	my_aData = new Array(j("#publicationTable tbody tr").lenght);
	/* Create an array with the values of all the input boxes in a column */
	j.fn.dataTableExt.afnSortData['dom-text'] = function  ( oSettings, iColumn )
	{	

//		j( 'td:eq('+iColumn+')', oSettings.oApi._fnGetTrNodes(oSettings) ).each( function () {
//			aData.push( j(this).html());
//		} );
		var tmp = 0;
		j(oSettings.oApi._fnGetTrNodes(oSettings)).each( function () {
		my_aData[tmp++] = j(this.cells[0]).html();
		});
		return my_aData;
	};


	pubTable = j("#publicationTable").dataTable(
			{
				"oLanguage": {"sSearch":       "Filter:"},
				"aaSorting": [[2,'asc']],
				"bPaginate": false,
				"bLengthChange": false,
				"bFilter": true,
				"bSort": true,
				"bInfo": false,
				"bAutoWidth": false,
				"aoColumnDefs": [
				                 {"sSortDataType": "dom-text",
				                	 "aTargets": [0], 
				                	 "sType": "numeric" },
				                 {"bSortable": false, "aTargets": [6]},
				                 {"bSortable": false, "aTargets": [1]}
				                ]
			}
			);

	utilCount("selected");
	utilCount("hided");
	utilCount("active");
	utilCount("unlinked");
	
	j("#publicationTable tr.selected").hover(function() {			
        j(this.cells[1]).addClass("showDragHandle");
	}, function() {
        j(this.cells[1]).removeClass("showDragHandle");
	});

	/* Manage cancel click to show message or not */
	j("#cancel").click(function() {
		var message = jQuery.trim(j("#message").text());
		if(message.length > 0) { 
			var c = confirm("Exit from this page?\nYou have entered new data on this page, if you navigate away from this page without first saving your data, the changes will be lost.");
			if(c==false) {
				return false;
			}
		}
	});

	/* Manage submit to insert data to send server */
	j("#bsubmit").click(function() {
		pubTable.fnFilter( '' );
		setDataFormUtil("active");
		setDataFormUtil("selected");
		setDataFormUtil("hided");
		setDataFormUtil("unlinked");
	});

	/* Manage show/hide central box and right container elements */
	j("#droppableactive").click(function() {
		pubTable.fnFilter( '' );
		showLoading();
		setTimeout(function(){
			controlContainer("active");
			loadComplete();
		}, 50);
	});

	/* Manage show/hide central box and right container elements */
	j("#droppableselected").click(function() {
		pubTable.fnFilter( '' );
		showLoading();
		setTimeout(function(){
			controlContainer("selected");
			loadComplete();
		}, 50);
	});

	/* Manage show/hide central box and rights container elements */
	j("#droppablehided").click(function() {
		pubTable.fnFilter( '' );
		showLoading();
		setTimeout(function(){
			controlContainer("hided");
			loadComplete();
		}, 50);
	});

	/* Manage show/hide central box and rights container elements */
	j("#droppableunlinked").click(function() {
		pubTable.fnFilter( '' );
		showLoading();
		setTimeout(function(){
			controlContainer("unlinked");
			loadComplete();
		}, 5);
	});

	/* Manage central box draggable elements */
/*	j(".draggable").draggable( {
		cursor : "crosshair",
		revert: "invalid",
		helper: function(event){
		return j('<div class="drag-row-item"><span class="dragrowmessage">Drop this publication on the new button state</span><table></table></div>').find('table').append(j(event.target).closest('tr').clone()).end().appendTo('body');
		}						
	});*/

	
	/* Manage right container droppable element */
/*	j("#droppableactive").droppable( {
		activeClass: "ui-state-hover",
		hoverClass: "ui-state-active",
		tolerance: 'pointer',
		drop : function(event, ui) {
			utilDrop(ui, "active");
		}
	});*/

	/* Manage right container droppable element */
/*	j("#droppablehided").droppable( {
		activeClass: "ui-state-hover",
		hoverClass: "ui-state-active",
		tolerance: 'pointer',
		drop : function(event, ui) {
			utilDrop(ui, "hided");
		}
	});*/

	/* Manage right container droppable element */
/*	j("#droppableselected").droppable( {
		activeClass: "ui-state-hover",
		hoverClass: "ui-state-active",
		tolerance: 'pointer',
		drop : function(event, ui) {
			utilDrop(ui, "selected");
		}
	});*/

	/* Manage right container droppable element */
/*	j("#droppableunlinked").droppable( {
		activeClass: "ui-state-hover",
		hoverClass: "ui-state-active",
		tolerance: 'pointer',
		drop : function(event, ui) {								
			utilDrop(ui, "unlinked");
		}
	});*/
	

	j(".dropselected").click(function() {
		pubTable.fnFilter( '' );
		internalDrop(this.parentNode.parentNode, "selected");
		var options = {};	
		j("#droppableselected").effect( "highlight", options, "normal");
		return false;
	});
	j(".dropactive").click(function() {
		pubTable.fnFilter( '' );
		internalDrop(this.parentNode.parentNode, "active");
		var options = {};
		j("#droppableactive").effect( "highlight", options, "normal");
		return false;
	});
	j(".dropunlinked").click(function() {
		pubTable.fnFilter( '' );
		internalDrop(this.parentNode.parentNode, "unlinked");
		var options = {};
		j("#droppableunlinked").effect( "highlight", options, "normal");
		return false;
	});
	j(".drophided").click(function() {
		pubTable.fnFilter( '' );
		internalDrop(this.parentNode.parentNode, "hided");
		var options = {};
		j("#droppablehide").effect( "highlight", options, "normal");
		return false;
	});
	
	loadComplete();
}