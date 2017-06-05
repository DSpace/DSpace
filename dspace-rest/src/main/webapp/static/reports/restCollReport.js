/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
var CollReport = function() {
	Report.call(this);
	this.getId = function(obj) {return obj.id;}
	//If sortable.js is included, uncomment the following
	//this.hasSorttable = function(){return true;}
	
	this.COLL_LIMIT = 25;
	this.loadId = 0;
	this.THREADS =11;
	this.THREADSP = 11;
	this.ACCIDX_COLL = 1;
	this.ACCIDX_ITEM = 2;
	this.getDefaultParameters = function(){
		return {
	    	"show_fields[]" : [],
		    filters       : "",
		    limit         : this.COUNT_LIMIT,
		    offset        : 0,
		    icollection   : "",
		    ifilter       : "",
	    };
	}
	this.getCurrentParameters = function(){
		return {
			"show_fields[]" : this.myMetadataFields.getShowFields(),
			filters         : this.myFilters.getFilterList(),
			limit           : this.myReportParameters.getLimit(),
			offset          : this.myReportParameters.getOffset(),
		    icollection    : $("#icollection").val(),
		    ifilter        : $("#ifilter").val(),
	    };
	}
	var self = this;

	this.init = function() {
	    this.baseInit();	
		$("#icollection").val(self.myReportParameters.params.icollection);
		$("#ifilter").val(self.myReportParameters.params.ifilter);
		$("#itemResults").accordion({
	        heightStyle: "content",
	        collapsible: true,
	        active: 1
	    });
	}
	
	this.myAuth.callback = function(data) {
		self.createCollectionTable();		
		$(".showCollections").bind("click", function(){
			self.loadData();
		});
		$("#refresh-fields").bind("click", function(){
			self.drawItemTable($("#icollection").val(), $("#ifilter").val(), 0);
		});
	}

	this.createCollectionTable = function() {
		var self = this;
		var tbl = $("<table/>");
		tbl.attr("id","table");
		$("#report").replaceWith(tbl);

		var thead = $("<thead/>");
		tbl.append(thead);
		var tbody = $("<tbody/>");
		tbl.append(tbody);
		var tr = self.myHtmlUtil.addTr(thead).addClass("header");
		self.myHtmlUtil.addTh(tr, "Num").addClass("num").addClass("sorttable_numeric");
		self.myHtmlUtil.addTh(tr, "Community").addClass("title");
		self.myHtmlUtil.addTh(tr, "Collection").addClass("title");
		var thn = self.myHtmlUtil.addTh(tr, "Num Items").addClass("sorttable_numeric");
		self.myHtmlUtil.makeTotalCol(thn);
		
		self.addCollectionRows(tbl, 0);
	}

	this.addCollectionRows = function(tbl, offset) {
		var self = this;

		$.ajax({
			url: "/rest/filtered-collections",
			data: {
				limit  : self.COLL_LIMIT,
				expand : "topCommunity",
				offset : offset,
 			},
			dataType: "json",
			headers: self.myAuth.getHeaders(),
			success: function(data){
				$.each(data, function(index, coll){
					var tr = self.myHtmlUtil.addTr($("#table tbody"));
					tr.attr("cid", self.getId(coll)).attr("index",index + offset).addClass(index % 2 == 0 ? "odd data" : "even data");
					self.myHtmlUtil.addTd(tr, index + offset + 1).addClass("num");
					var parval = ""; 
					
					if ("topCommunity" in coll) {
						var par = coll.topCommunity;
						parval = par ? self.myHtmlUtil.getAnchor(par.name, "/handle/" + par.handle) : "";					
					} else if ("parCommunityList" in coll) {
						var par = coll.parentCommunityList[coll.parentCommunityList.length-1];
						parval = par ? self.myHtmlUtil.getAnchor(par.name, "/handle/" + par.handle) : "";
					}
					self.myHtmlUtil.addTd(tr, parval).addClass("title comm");
					self.myHtmlUtil.addTdAnchor(tr, coll.name, "/handle/" + coll.handle).addClass("title");
					var td = self.myHtmlUtil.addTd(tr, coll.numberItems).addClass("num").addClass("link");
					td.on("click", function(){
						self.drawItemTable(self.getId(coll),'',0);
						$("#icollection").val(self.getId(coll));
						$("#ifilter").val("");
					});
				});
				
				if (data.length == self.COLL_LIMIT) {
					self.addCollectionRows(tbl, offset + self.COLL_LIMIT);
					return;
				}  
				self.myHtmlUtil.totalCol(3);
				$("#table").addClass("sortable");
				if (self.hasSorttable()) {
					sorttable.makeSortable($("#table")[0]);					
				}
		  		
		  		if (self.myFilters.getFilterList() != "") {
		  			self.loadData();
		  			if ($("#icollection").val() != "") {
		  				self.drawItemTable($("#icollection").val(), $("#ifilter").val(), 0);
		  			}
		  		}
			},
			error: function(xhr, status, errorThrown) {
				alert("Error in /rest/filtered-collections "+ status+ " " + errorThrown);
			},
			complete: function(xhr, status) {
				self.spinner.stop();
		  		$(".showCollections").attr("disabled", false);
			}
		});	
	}
	
	this.loadData = function() {
		self.spinner.spin($("h1")[0]);
		$(".showCollections").attr("disabled", true);
		$("#metadatadiv").accordion("option", "active", self.ACCIDX_COLL);
		self.loadId++;
		$("td.datacol,th.datacol").remove();
		$("#table tr.data").addClass("processing");
		self.myFilters.filterString = self.myFilters.getFilterList();
		self.doRow(0, self.THREADS, self.loadId);
	}
	
	this.doRow = function(row, threads, curLoadId) {
		if (self.loadId != curLoadId) return;
		var tr = $("tr[index="+row+"]");
		if (!tr.is("*")){
			return; 
		}
			
		var cid = tr.attr("cid");
		$.ajax({
			url: "/rest/filtered-collections/"+cid,
			data: {
				limit : self.COUNT_LIMIT,
				filters : self.myFilters.filterString,
			},
			dataType: "json",
			headers: self.myAuth.getHeaders(),
			success: function(data) {
				var numItems = data.numberItems;
				var numItemsProcessed = data.numberItemsProcessed;
				$.each(data.itemFilters, function(index, itemFilter){
					if (self.loadId != curLoadId) {
						return;
					}
					var trh = $("#table tr.header");
					var filterName = itemFilter["filter-name"];
					var filterTitle = itemFilter.title == null ? filterName : itemFilter.title;
					var icount = itemFilter["item-count"];
					if (!trh.find("th."+filterName).is("*")) {
						var th = self.myHtmlUtil.addTh(trh, filterTitle);
						th.addClass(filterName).addClass("datacol").addClass("sorttable_numeric");
						self.myHtmlUtil.makeTotalCol(th);
						
						if (itemFilter.description != null) {
							th.attr("title", itemFilter.description);											
						}

						$("tr.data").each(function(){
							var td = self.myHtmlUtil.addTd($(this), "");
							td.addClass(filterName).addClass("num").addClass("datacol");
						});
					}
					
                    var td = tr.find("td."+filterName);
                    if (icount == null) {
                    	icount = "0";
                    }
                    td.text(icount);
					if (icount != "0") {
						td.addClass("link");
						td.on("click", function(){
							self.drawItemTable(cid,filterName,0);
							$("#icollection").val(cid);
							$("#ifilter").val(filterName);
						});
						if (numItems != numItemsProcessed) {
							td.addClass("partial");
							td.attr("title", "Collection partially processed, item counts are incomplete");
						}
					}
					
					
				});
				
				tr.removeClass("processing");
				if (!$("#table tr.processing").is("*")) {
					if (self.hasSorttable()) {
						$("#table").removeClass("sortable");
						$("#table").addClass("sortable");
						sorttable.makeSortable($("#table")[0]);						
					}
					var colcount = $("#table tr th").length;
					for(var i=4; i<colcount; i++) {
						self.myHtmlUtil.totalCol(i);
					}
					self.spinner.stop();
	  	  		    $(".showCollections").attr("disabled", false);
					return;
				}
				if (row % threads == 0 || threads == 1) {
					for(var i=1; i<=threads; i++) {
						self.doRow(row+i, threads, curLoadId);
					}					
				}
	 		},
			error: function(xhr, status, errorThrown) {
				alert("Error in /rest/filtered-collections "+ status+ " " + errorThrown);
			},
			complete: function(xhr, status) {
				self.spinner.stop();
		  		$(".showCollections").attr("disabled", false);
			}
		});
	}			
				
	this.drawItemTable = function(cid, filter, offset) {
		self = this;
		self.spinner.spin($("h1")[0]);
		$("#itemtable").replaceWith($('<table id="itemtable" class="sortable"></table>'));
		var itbl = $("#itemtable");
		//itbl.find("tr").remove("*");
		var tr = self.myHtmlUtil.addTr(itbl).addClass("header");
		self.myHtmlUtil.addTh(tr, "Num").addClass("num").addClass("sorttable_numeric");
		self.myHtmlUtil.addTh(tr, "id");
		self.myHtmlUtil.addTh(tr, "Handle");
		self.myHtmlUtil.addTh(tr, "dc.title").addClass("title");
		var fields = $("#show-fields select").val();
		if (fields != null) {
			$.each(fields, function(index, field){
				self.myHtmlUtil.addTh(tr, field);		
			});		
		}

		var params = {
			expand: fields == null ? "items" : "items,metadata",
			limit: self.ITEM_LIMIT,
			filters: filter,
			offset: offset,
			"show_fields[]" : fields,
		}
		
		$.ajax({
			url: "/rest/filtered-collections/"+cid,
			data: params,
			dataType: "json",
			headers: self.myAuth.getHeaders(),
			success: function(data){
				var source = filter == "" ? data.items : data.itemFilters[0].items;
				
				$.each(source, function(index, item){
					var tr = self.myHtmlUtil.addTr(itbl);
					tr.addClass(index % 2 == 0 ? "odd data" : "even data");
					self.myHtmlUtil.addTd(tr, offset+index+1).addClass("num");
					self.myHtmlUtil.addTd(tr, self.getId(item));
					self.myHtmlUtil.addTdAnchor(tr, item.handle, "/handle/" + item.handle);
					self.myHtmlUtil.addTd(tr, item.name).addClass("ititle");
					if (fields != null) {
						$.each(fields, function(index, field){
							var text = "";		
							$.each(item.metadata, function(mindex,mv){
								if (mv.key == field) {
									if (text != "") {
										text += "<hr/>";
									}
									text += mv.value;
								}
							});
							self.myHtmlUtil.addTd(tr, text);
						});		
					}
				});
				self.displayItems(filter + " Items in " + data.name, 
					offset,
					self.ITEM_LIMIT,
					function(){self.drawItemTable(cid, filter, (offset - self.ITEM_LIMIT < 0) ? 0 : offset - self.ITEM_LIMIT);},
					function(){self.drawItemTable(cid, filter, offset + self.ITEM_LIMIT);}
				);
				
				if (self.hasSorttable()){
					sorttable.makeSortable(itbl[0]);					
				}
				$("#metadatadiv").accordion("option", "active", self.ACCIDX_ITEM); 
			},
			error: function(xhr, status, errorThrown) {
				alert("Error in /rest/filtered-collections "+ status+ " " + errorThrown);
			},
			complete: function(xhr, status) {
				self.spinner.stop();
	  			$(".showCollections").attr("disabled", false);
			}
		});
	}

	//Ignore the first column containing a row number and the item handle
	this.exportCol = function(colnum, col) {
		var data = "";
		if (colnum == 0) return "";
		if (colnum == 2) return "";
		data += (colnum == 1) ? "" : ",";
		data += self.exportCell(col);
		return data;
	}
}
CollReport.prototype = Object.create(Report.prototype);

$(document).ready(function(){
	var myReport=new CollReport();
	myReport.init();
});
