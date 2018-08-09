/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
var CollReport = function() {
    Report.call(this);
    //If sortable.js is included, uncomment the following
    //this.hasSorttable = function(){return true;}

    //Indicate if Password Authentication is supported
    //this.makeAuthLink = function(){return true;};
    //Indicate if Shibboleth Authentication is supported
    //this.makeShibLink = function(){return true;};

    this.getLangSuffix = function(){
      return "[en]";
    }
    
    this.COLL_LIMIT = 20;
    this.TOOBIG = 10000;
    this.loadId = 0;
    this.THREADS =11;
    this.THREADSP = 11;
    this.ACCIDX_COLL = 1;
    this.ACCIDX_ITEM = 2;
    this.IACCIDX_META = 0;
    this.IACCIDX_BIT  = 1;
    this.IACCIDX_ITEM = 2;
    this.getDefaultParameters = function(){
        return {
            "show_fields[]" : [], 
            "show_fields_bits[]" : [],
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
            "show_fields_bits[]" : this.myBitstreamFields.getShowFieldsBits(),
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
            active: 2
        });
    }
    
    this.myAuth.callback = function(data) {
        self.createCollectionTable();
        $(".showCollections").bind("click", function(){
            self.loadData();
        });
        $("#refresh-fields,#refresh-fields-bits").bind("click", function(){
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
        thn = self.myHtmlUtil.addTh(tr, "Num Filtered").addClass("sorttable_numeric");
        self.myHtmlUtil.makeTotalCol(thn);
        
        self.addCollections();
    }

    this.addCollections = function() {
        var self = this;
        
        $.ajax({
            url: "/rest/hierarchy",
            dataType: "json",
            headers: self.myAuth.getHeaders(),
            success: function(data){
                if (data.community != null) {
                    $.each(data.community, function(index, comm){
                        self.addCommunity(comm, comm);
                    });                    
                }
                self.setCollectionCounts(0);
            },
            error: function(xhr, status, errorThrown) {
                alert("Error in /rest/hierarchy "+ status+ " " + errorThrown);
            }
        });
    };

    this.addCommunity = function(top, comm) {
        var self = this;

        if (comm.collection != null) {
            $.each(comm.collection, function(index, coll){
                self.addCollection(top, coll);
            });                    
        }
        if (comm.community != null) {
            $.each(comm.community, function(index, scomm){
            self.addCommunity(top, scomm);
        });                    
    }
    };

    this.addCollection = function(top, coll) {
        var self = this;

        var tbody = $("#table tbody");
        var index = tbody.find("tr").length;

        var tr = self.myHtmlUtil.addTr(tbody);
        tr.attr("cid", coll.id).attr("index",index).addClass(index % 2 == 0 ? "odd data" : "even data");
        self.myHtmlUtil.addTd(tr, index + 1).addClass("num");
        var parval = self.myHtmlUtil.getAnchor(top.name, self.ROOTPATH + top.handle); 
        
        self.myHtmlUtil.addTd(tr, parval).addClass("title comm");
        self.myHtmlUtil.addTdAnchor(tr, coll.name, self.ROOTPATH + coll.handle).addClass("title");
        var td = self.myHtmlUtil.addTd(tr, "").addClass("num").addClass("link").addClass("numCount");
        td = self.myHtmlUtil.addTd(tr, "").addClass("num").addClass("numFiltered");
    };
    
    
    this.setCollectionCounts = function(offset) {
        var self = this;

        $.ajax({
            url: "/rest/filtered-collections",
            data: {
                limit  : self.COLL_LIMIT,
                offset : offset
             },
            dataType: "json",
            headers: self.myAuth.getHeaders(),
            success: function(data){
                $.each(data, function(index, coll){
                    var id = self.getId(coll);
                    var tr = $("#table tbody").find("tr[cid="+id+"]");
                    var td = tr.find("td.numCount");
                    td.text(coll.numberItems);
                    td.on("click", function(){
                        self.drawItemTable(self.getId(coll),'',0);
                        $("#icollection").val(self.getId(coll));
                        $("#ifilter").val("");
                    });
                });
                
                //cannot assume data returned is full amount in case some items are restricted
                //if (data.length == self.COLL_LIMIT) {
                if (data.length > 0) {
                    self.setCollectionCounts(offset + self.COLL_LIMIT);
                    return;
                }  
                self.myHtmlUtil.totalCol(3);
                $("#table").addClass("sortable");
                  
                  if (self.myFilters.getFilterList() != "") {
                      self.loadData();
                      if ($("#icollection").val() != "") {
                          self.drawItemTable($("#icollection").val(), $("#ifilter").val(), 0);
                      }
                  }
            },
            error: function(xhr, status, errorThrown) {
                alert("Error in /rest/collections "+ status+ " " + errorThrown);
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
                    
                    self.setCellCount(tr, cid, 0, (numItems != numItemsProcessed), itemFilter);
                    self.setFilteredCount(tr, cid, 0, numItems, numItemsProcessed);
                });
                
                tr.removeClass("processing");
                if (!$("#table tr.processing").is("*")) {
                    self.updateSortable();
                    self.totalFilters();
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
    };            
                
    this.updateSortable = function() {
                    if (self.hasSorttable()) {
                        $("#table").removeClass("sortable");
                        $("#table").addClass("sortable");
                        sorttable.makeSortable($("#table")[0]);                        
                    }
    }
    
    this.totalFilters = function() {
                    var colcount = $("#table tr th").length;
                    for(var i=4; i<colcount; i++) {
                        self.myHtmlUtil.totalCol(i);
                    }
                }

    this.updateRow = function(cid, offset) {
        var tr = $("tr[cid="+cid+"]");
        $.ajax({
            url: "/rest/filtered-collections/"+cid,
            data: {
                limit : self.COUNT_LIMIT,
                offset : offset,
                filters : self.myFilters.filterString,
            },
            dataType: "json",
            headers: self.myAuth.getHeaders(),
            success: function(data) {
                var numItems = data.numberItems;
                var numItemsProcessed = data.numberItemsProcessed;
                $.each(data.itemFilters, function(index, itemFilter){
                    self.setCellCount(tr, cid, offset, (numItems != numItemsProcessed + offset),itemFilter);
                });
                self.setFilteredCount(tr, cid, offset, numItems, numItemsProcessed);                
             },
            error: function(xhr, status, errorThrown) {
                alert("Error in /rest/filtered-collections/ " + cid+ status+ " " + errorThrown);
            },
            complete: function(xhr, status) {
                self.spinner.stop();
                  $(".showCollections").attr("disabled", false);
            }
        });
    };            

    this.setFilteredCount = function(tr, cid, offset, numItems, numItemsProcessed) {
        var td = tr.find("td.numFiltered");
        var total = numItemsProcessed + offset; 
        td.text(total);
        td.removeClass("partial");
        td.removeClass("toobig");
        if (numItems != numItemsProcessed + offset) {
            if (offset == 0) {
                td.addClass("button");
                td.off();
                td.on("click", function(){
                    if ($(this).hasClass("toobig")) {
                        if (!confirm("A large number of items are present in this collection.\n\n" + 
                                "If you choose to load this data, it will take some time to load and may impact server performance.")) {
                            return;
                        }
                    }
                    $(this).off();
                    $(this).removeClass("button");
                    self.updateRow(cid, offset + self.COUNT_LIMIT);
                });                                            
            } else {
                self.updateRow(cid, offset + self.COUNT_LIMIT);
            }
            td.addClass("partial");
            var title = "Collection partially processed, item counts are incomplete. ";
            if (numItems >= self.TOOBIG) {
                td.addClass("toobig");
                title+= "\nIt will take significant time to apply this filter to the entire collection."
            }            
            td.attr("title", title);
            return false;
        } else {
            self.totalFilters();
        }
        return true;
    }
    
    this.setCellCount = function(tr, cid, offset, isPartial, itemFilter) {
        var filterName = itemFilter["filter-name"];
        var icount = itemFilter["item-count"];

        var td = tr.find("td."+filterName);
        if (icount == null) {
            icount = 0;
        }
        var cur = parseInt(td.text());
        if (!isNaN(cur)) {
            icount += cur;            
        }
        
        td.removeClass("partial");
        td.removeClass("link");
        td.removeAttr("title");
        td.off();
        td.text(icount);
        if (icount != 0) {
            td.addClass("link");
            if (isPartial) {
                td.addClass("partial");
                td.attr("title", "Collection partially processed, item counts are incomplete");
            }    
            td.on("click", function(){
                self.drawItemTable(cid,filterName,0);
                $("#icollection").val(cid);
                $("#ifilter").val(filterName);
            });                            
        }        
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
        self.myHtmlUtil.addTh(tr, "dc.title"  + self.getLangSuffix()).addClass("title");
        var fields = $("#show-fields select").val();
        if (fields != null) {
            $.each(fields, function(index, field){
                self.myHtmlUtil.addTh(tr, field + self.getLangSuffix());        
            });        
        }
        var bitfields = $("#show-fields-bits select").val();
        if (bitfields != null) {
          $.each(bitfields, function(index, bitf){
            self.myHtmlUtil.addTh(tr, bitf);    
          });   
        }
    
        var expand = "items";
        if (fields != null) {
          expand += ",metadata";
        }
        if (bitfields != null) {
          expand += ",bitstreams";
        }

        var params = {
            expand: expand,
            limit: self.ITEM_LIMIT,
            filters: filter,
            offset: offset,
            "show_fields[]" : fields,
            "show_fields_bits[]" : bitfields,
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
                    self.myHtmlUtil.addTdAnchor(tr, item.handle, self.ROOTPATH + item.handle);
                    self.myHtmlUtil.addTd(tr, item.name).addClass("ititle");
                    if (fields != null) {
                        $.each(fields, function(index, field){
                            var text = "";        
                            var td = self.myHtmlUtil.addTd(tr, "");
                            $.each(item.metadata, function(mindex,mv){
                                if (mv.key == field) {
                                    td.append($("<div>"+mv.value+"</div>"));
                                }
                            });
                        });    
                    }
                    if (bitfields != null) {
                      $.each(bitfields, function(index, bitfield){
                        var td = self.myHtmlUtil.addTd(tr, "");
                        var fieldtext = self.myBitstreamFields.getKeyText(bitfield, item, bitfields);
                        for(var j=0; j<fieldtext.length; j++) {
                          td.append($("<div>"+fieldtext[j]+"</div>"));
                        }
                      });
                    }
                });
                self.displayItems(filter + " Items in " + data.name, 
                    offset,
                    self.ITEM_LIMIT,
                    data.numberItems,
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
                $("#itemResults").accordion("option", "active", self.IACCIDX_ITEM);
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
