/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
var Report = function() {
    var self = this;
    this.COLL_LIMIT = 500;
    this.COUNT_LIMIT = 500;
    this.ITEM_LIMIT = 100;

    //set default to work on demo.dspace.org
    this.ROOTPATH = "/xmlui/handle/"
    //this.ROOTPATH = "/jspui/handle/"
    //this.ROOTPATH = "/handle/"
    
    //disable this setting if Password Authentication is not supported
    this.makeAuthLink = function(){return false;};

    //Override this to return obj.id for DSpace 5 versions
    this.getId = function(obj) {
        return obj.uuid;
    }
    
    //Override this method is sortable.js has been included
    this.hasSorttable = function() {
        return false;
    }
    
    this.getDefaultParameters = function(){
        return {};
    }
    this.getCurrentParameters = function(){
        return {};
    }
    
    this.saveUrl = function() {
        this.myReportParameters.saveAsUrl(this.getCurrentParameters());
    }
    
    this.getLoginPayload = function() {
        //Placeholder to allow a customized report to prompt for email/password
        //If not enabled, the authenticaton callback will be called immediately
        var email = $("#restemail").val();
        var pass = $("#restpass").val();
        if (email == "" || pass == "") {
          return undefined;  
        } else if (email == null || pass == null) {
           return undefined;  
        } else {
           return {email: email, password: pass};      
        }
    }
    this.getLangSuffix = function(){
        return "";
    }
    this.myAuth = new Auth(this);
    this.myAuth.authStat();
    this.myAuth.callback = function(data) {
        self.spinner.stop();
    }
    this.myHtmlUtil = new HtmlUtil();
    this.spinner = new Spinner({
        lines: 13, // The number of lines to draw
        length: 20, // The length of each line
        width: 10, // The line thickness
        radius: 30, // The radius of the inner circle
        corners: 1, // Corner roundness (0..1)
        rotate: 0, // The rotation offset
        direction: 1, // 1: clockwise, -1: counterclockwise
        color: '#000', // #rgb or #rrggbb or array of colors
        speed: 1, // Rounds per second
        trail: 60, // Afterglow percentage
        shadow: false, // Whether to render a shadow
        hwaccel: false, // Whether to use hardware acceleration
        className: 'spinner', // The CSS class to assign to the spinner
        zIndex: 2e9, // The z-index (defaults to 2000000000)
        top:  '400px', // Top position relative to parent
        left: '600px' // Left position relative to parent        
    });
    
    this.displayItems = function(itemsTitle, offset, limit, total, funcdec, funcinc) {
        var count = $("#itemtable tr.data").length;
        
        var last = offset + limit;
        var suff = "";
        
        if (total == null) {
            last = offset + count;
            suff = (count == limit) ? " of " + last + "+ " : " of " + last;
        } else if (limit == total) {
            //total may only be accurate to page size
            suff = " of " + total + "+ ";
        } else {
            last = (last > total) ? total : last;
            suff = " of " + total;
        }
        suff += " unfiltered; displaying " + count + " filtered" ;
        
        itemsTitle += " (" + (offset+1) + " - " + last + suff + ")";
        $("#prev,#next").attr("disabled",true);
        $("#itemdiv h3").text(itemsTitle);
        if (offset > 0) $("#prev").attr("disabled", false);
        $("#prev").off("click").on("click", funcdec);
        //in case of filters, always allow next
        
        if (total == null) {
            $("#next").attr("disabled", false);                
        } else if (offset + limit  < total) {
            $("#next").attr("disabled", false);            
        } else if (limit == total) {
            //total may only be accurate to one page
            $("#next").attr("disabled", false);            
        }
        $("#next").off("click").on("click", funcinc);
    }
    
    this.myReportParameters = undefined;
    this.myFilters = undefined;
    this.myMetadataFields = undefined;
    
    this.initMetadataFields = function() {
        this.myMetadataFields = new MetadataFields(self);
        this.myMetadataFields.load();        
    }
    
    this.baseInit = function() {
        this.myReportParameters = new ReportParameters(
                this.getDefaultParameters(),
                window.location.search.substr(1)
        );
        this.spinner.spin($("h1")[0]);
        this.myFilters = new Filters(this.myReportParameters.params["filters"]);
        this.initMetadataFields();
        $("#metadatadiv").accordion({
            heightStyle: "content",
            collapsible: true,
            active: $("#metadatadiv > h3").length - 2
        });
        $("#export").click(function(){
            self.export($("#itemtable tr"));
        });
        $("a.this-search").on("click",function(){
            self.saveUrl();
        });
        this.myFilters.createFilterTable(this.myReportParameters.params.filters);
        this.myAuth.init();
    }

    this.export = function(rows) {
        var itemdata = "data:text/csv;charset=utf-8,";
        rows.each(function(rownum, row){
            itemdata += (rownum == 0) ? "" : "\r\n";
            $(row).find("td,th").each(function(colnum, col){
                itemdata += self.exportCol(colnum, col);
            });
        });
        var encodedUri = encodeURI(itemdata);
        window.open(encodedUri);        
    }
    
    //this is meant to be overridden for each report
    this.exportCol = function(colnum, col) {
        var data = "";
        data += (colnum == 0) ? "" : ",";
        data += self.exportCell(col);
        return data;
    }
    
    this.exportCell = function(col) {
        data = "\"";
        $(col).contents().each(function(i, node){
            if ($(node).is("hr")) {
                data += "||";
            } else {
                data += $(node).text().replace(/\n/g," ").replace(/"/g,"\"\"");
                if ($(node).is("div:not(:last-child)")) {
                    data += "||";
                }
            }        
        });
        data += "\"";
        return data;
    }
    
    this.init = function() {
        this.baseInit();    
    }
    
}

var Auth = function(report) {
    this.report = report;
    this.TOKEN = undefined;
    this.callback = function(data) {
    };
    this.saveToken = function(data) {
        this.TOKEN = data;
    }
    this.init = function() {
        var loginPayload = report.getLoginPayload();
        if (loginPayload == undefined) {
            this.callback();
            return;
        }

        var self = this;
        $.ajax({
            url : "/rest/login",
            contentType : "application/x-www-form-urlencoded",
            accepts : "application/json",
            type : "POST",
            data : loginPayload,
            success : function(data){
                self.saveToken(data);
               },
            error: function(xhr, status, errorThrown) {
                alert("Error in /rest/login "+ status+ " " + errorThrown);
            },
            complete: function(xhr, status) {
                self.authStat();
                self.callback();
            }
        });        
    }
    this.authStat = function() {
        var self = this;
        $.ajax({
            url : "/rest/status",
            dataType : "json",
            error: function(xhr, status, errorThrown) {
                alert("Error in /rest/status "+ status+ " " + errorThrown);
            },
            success: function(data) {
              var user = "";
              if (data.email != undefined) {
                user = data.email;                  
              } else {
                user = "You are not logged in.  Some items may be excluded from reports.";
              }
              var anchor = $("<a/>").text(user);
              if (self.report.makeAuthLink()) {
                  anchor.attr("href","javascript:window.open('authenticate.html','Authenticate (Password Auth Only)','height=200,width=500')");
              }
              $("#currentUser").empty().append("<b>Current User: </b>").append(anchor);
            }
        });     
    }
    this.logout = function() {
        var self = this;
        $.ajax({
            url : "/rest/logout",
            error: function(xhr, status, errorThrown) {
                alert("Error in /rest/logout "+ status+ " " + errorThrown);
            },
            complete: function(xhr, status) {
                self.authStat();
            }
        });     
    }
    this.getHeaders = function() {
        var HEADERS = {};
        if (this.TOKEN != null) {
            HEADERS['rest-dspace-token'] = this.TOKEN;
        }
        return HEADERS;
    }
}

var ReportParameters = function(defaultParams, prmstr) {
    this.params = defaultParams;

    if (prmstr == null) prmstr = "";
    var prmarr = prmstr.split("&");
    for ( var i = 0; i < prmarr.length; i++) {
        var tmparr = prmarr[i].split("=");
        var field = tmparr[0];
        var val = decodeURIComponent(tmparr[1]);
        var pval = this.params[field];
              
        if ($.isArray(pval)) {
            pval[pval.length] = val;          
        } else {
            this.params[field] = val;
        }
    }
    $("#limit").val(this.params.limit);  
    $("#offset").val(this.params.offset);
    this.limit = this.params.limit;
    this.offset = this.params.offset;

    this.getOffset = function() {
        var offset = $("#offset").val();
        return $.isNumeric(offset) ? Number(offset) : this.offset;
    }

    this.getNextOffset = function() {
        return this.getOffset() + this.getLimit();
    }

    this.getPrevOffset = function() {
        var v = this.getOffset() - this.getLimit();
        return v < 0 ? 0 : v;
    }

    this.getLimit = function() {
        var limit = $("#limit").val();
        return $.isNumeric(limit) ? Number(limit) : this.limit;
    }

    this.updateOffset = function(increment) {
        var val = $("#offset").val();
        var lim = $("#limit").val();
        if ($.isNumeric(val) && $.isNumeric(lim)) {
            if (increment) {
                $("#offset").val(this.getNextOffset());                
            } else {
                $("#offset").val(this.getPrevOffset());                
            }
        }        
    }

    this.saveAsUrl = function(params) {
        var pstr = $.param(params).replace(/%5B%5D/g,"[]");
        window.location.search = pstr;
    }
}


var Filters = function() {
    this.createFilterTable = function(filterList) {
        self = this;
        var paramFilterSel = filterList == null ? new Array() : filterList.split(",");
        var categories = new Array();
        self.addFilter("", categories, "General", "None", "De-select all filters", "none").click(
            function(){
                $("input.filter,input.all").attr("checked",false);
                $("#filter-reload").attr("disabled", false);
            }
        );
        self.addFilter("all", categories, "General", "All", "Show all filters", "all").click(
            function(){
                $("input.filter,input.none").attr("checked",false);
                $("#filter-reload").attr("disabled", false);
            }
        );
        
        $.getJSON(
            "/rest/filters",
            function(data){
                $.each(data, function(index, filter){
                    var checkbox = self.addFilter(filter["filter-name"], categories, filter.category, filter.title, filter.description, "filter").click(
                        function(){
                            $("input.none,input.all").attr("checked",false);
                            $("#filter-reload").attr("disabled", false);
                        }
                    );
                    $.each(paramFilterSel, function(index, filtername){
                        if (filtername == filter["filter-name"]) {
                            checkbox.attr("checked", true);
                        }
                    });
                });
            }
        );
    }

    this.addFilter = function(val, categories, category, title, description, cname) {
        var catdiv = null;
        for(var i=0; i<categories.length; i++) {
            if (categories[i].name == category) {
                catdiv = categories[i].div;
                break;
            }
        }
        if (catdiv == null) {
            catdiv = $("<fieldset class='catdiv'/>");
            catdiv.append($("<legend>"+category+"</legend>"));
            $("#filterdiv").append(catdiv);
            categories[categories.length] = {name: category, div: catdiv};
        }
        var div = $("<div/>");
        var input = $("<input name='filters[]' type='checkbox'/>");
        input.attr("id",val);
        input.val(val);
        input.addClass(cname);
        div.append(input);
        var ftitle = (title == null) ? val : title;
        var label = $("<label>" + ftitle + "</label>");
        label.attr("title", description);
        div.append(label);
        catdiv.append(div);
        return input;
    }

    this.getFilterList = function() {
        var list="";
        $("input:checked[name='filters[]']").each(
            function(){
                if (list != "") {
                    list += ",";
                }
                list += $(this).val();
            }
        );
        if (list == "") {
            list = "none";
        }
        return list;
    }    
}

var MetadataFields = function(report) {
    this.metadataSchemas = undefined;
    var self = this;
    
    this.load = function(){
        $.ajax({
            url: "/rest/registries/schema",
            dataType: "json",
            success: function(data){
                self.initFields(data, report);
            },
            error: function(xhr, status, errorThrown) {
                alert("Error in /rest/registries/schema "+ status+ " " + errorThrown);
            },
            complete: function(xhr, status) {
            }
        });        
    }
    
    this.initFields = function(data, report) {
        var params = report.myReportParameters.params;
        self.metadataSchemas = data;
        self.drawShowFields(params["show_fields[]"]);
    }
    
    this.getShowFields = function(){
        var val = $("#show-fields select").val();
        return val == null ? Array() : val;
    }

    this.drawShowFields = function(pfields) {
        if (pfields == null) return;
        var self = this;
        var sel = $("<select name='show_fields'/>").attr("multiple","true").attr("size","8").appendTo("#show-fields");
        $.each(this.metadataSchemas, function(index, schema){
            if (schema.prefix == 'eperson') {
                return;
            }
            $.each(schema.fields, function(findex, field) {
                var name = field.name;
                var opt = $("<option/>");
                opt.attr("value",name).text(name);
                for(var i=0; i<pfields.length; i++) {
                    if (pfields[i] == name) {
                        opt.attr("selected", true);
                    }
                }
                sel.append(opt);
            });
        });
    }
    
    this.initQueries = function(){};
}

var HtmlUtil = function() {
    this.addTr = function(tbl) {
        var tr = $("<tr/>");
        tbl.append(tr);
        return tr;
    }

    this.addTd = function(tr, val) {
        var td = $("<td/>");
        if (val != null) {
            td.append(val);
        }
        tr.append(td);
        return td;
    }

    this.addTh = function(tr, val) {
        var th = $("<th/>");
        if (val != null) {
            th.append(val);
        }
        tr.append(th);
        return th;
    }


    this.addTdAnchor = function(tr, val, href) {
        return this.addTd(tr, this.getAnchor(val, href));
    }

    this.getAnchor = function(val, href) {
        var a = $("<a/>");
        a.append(val);
        a.attr("href", href);
        a.attr("target", "_blank");
        return a;
    }

    this.createOpt = function(name, val) {
        var opt = $("<option/>");
        opt.attr("value", val).text(name);
        return opt;
    }

    this.addOpt = function(sel, name, val) {
        var opt = this.createOpt(name, val);
        sel.append(opt);
        return opt;
    }

    this.addDisabledOpt = function(sel, name, val) {
        var opt = this.createOpt(name, val).attr("disabled",true);
        sel.append(opt);
        return opt;
    }

    this.makeTotalCol = function(th) {
        th.append($("<hr><span class='num'>-</span>"));
    }

    this.totalCol = function(index){
        var total = 0;
        $("#table tr.data").each(function(){
            var val = $($(this).find("td")[index]).text();
            if ($.isNumeric(val)) {
                total += Number(val);
            }
        });
        $($("#table tr.header th")[index]).find("span.num").text(total);
    }

}

var CommunitySelector = function(report, parent, paramCollSel) {
    var self = this;
    $("#collSel,#collSel option").remove();
    var collSel = $("<select/>").attr("id","collSel").attr("name","collSel").attr("multiple", true).attr("size",15);
    parent.append(collSel);
    report.myHtmlUtil.addOpt(collSel, "Whole Repository", "");
    
    $.ajax({
        url: "/rest/hierarchy",
        dataType: "json",
        headers: report.myAuth.getHeaders(),
        success: function(data){
            var collSel = $("#collSel");
            if (data.community != null) {
                $.each(data.community, function(index, comm){
                    self.addCommLabel(collSel, comm, 0, paramCollSel);
                });
            }
        },
        error: function(xhr, status, errorThrown) {
            alert("Error in /rest/communities "+ status+ " " + errorThrown);
        },
        complete: function(xhr, status) {
        }
    });    

    this.addCommLabel = function(collSel, comm, indent, paramCollSel) {
        var prefix = "";
        for(var i=0; i<indent; i++) {
            prefix += "--";
        }
        report.myHtmlUtil.addDisabledOpt(collSel, prefix + comm.name, comm.id);
        if (comm.collection != null) {
            $.each(comm.collection, function(index, coll) {
                var opt = report.myHtmlUtil.addOpt(collSel, prefix + "--" + coll.name, coll.id);
                $.each(paramCollSel, function(index, collid){
                    if (collid == coll.id) {
                        opt.attr("selected", true);
                    }
                });
            });        
        }
        if (comm.community != null) {
            $.each(comm.community, function(index, scomm) {
                self.addCommLabel(collSel, scomm, indent + 1, paramCollSel);
            });        
        }
    }
}
