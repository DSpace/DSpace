/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
var QueryReport = function() {
    Report.call(this);
    
    //If sortable.js is included, uncomment the following
    //this.hasSorttable = function(){return true;}
    
    this.getDefaultParameters = function(){
        return {
            "collSel[]"     : [],
            "query_field[]" : [],
            "query_op[]"    : [],
            "query_val[]"   : [],
            "show_fields[]" : [],
            "filters"       : "",
            "limit"         : this.ITEM_LIMIT,
            "offset"        : 0,          
        };
    }
    this.getCurrentParameters = function(){
        var params = {
            "query_field[]" : [],
            "query_op[]"    : [],
            "query_val[]"   : [],
            "collSel[]"     : ($("#collSel").val() == null) ? [""] : $("#collSel").val(),
            limit           : this.myReportParameters.getLimit(),
            offset          : this.myReportParameters.getOffset(),
            "expand"        : "parentCollection,metadata",
            filters         : this.myFilters.getFilterList(),
            "show_fields[]" : this.myMetadataFields.getShowFields(),
        };
        $("select.query-tool,input.query-tool").each(function() {
            var paramArr = params[$(this).attr("name")];
            paramArr[paramArr.length] = $(this).val();
        });
        return params;
    }
    var self = this;

    this.init = function() {
        this.baseInit();    
    }
    
    this.initMetadataFields = function() {
        this.myMetadataFields = new QueryableMetadataFields(self);
        this.myMetadataFields.load();        
    }
    this.myAuth.callback = function(data) {
        var communitySelector = new CommunitySelector(self, $("#collSelector"), self.myReportParameters.params["collSel[]"]);
        $(".query-button").click(function(){self.runQuery();})
    }

    this.runQuery = function() {
        this.spinner.spin($("body")[0]);
        $("button").attr("disabled", true);
        $.ajax({
            url: "/rest/filtered-items", 
            data: this.getCurrentParameters(), 
            dataType: "json",
            headers: self.myAuth.getHeaders(),
            success: function(data){
                data.metadata = $("#show-fields select").val();
                self.drawItemFilterTable(data);
                self.spinner.stop();
                  $("button").not("#next,#prev").attr("disabled", false);
            },
            error: function(xhr, status, errorThrown) {
                alert("Error in /rest/filtered-items "+ status+ " " + errorThrown);
            },
            complete: function(xhr, status, errorThrown) {
                self.spinner.stop();
                  $("button").not("#next,#prev").attr("disabled", false);
            }
        });
    }
    
    this.drawItemFilterTable = function(data) {
        $("#itemtable").replaceWith($('<table id="itemtable" class="sortable"></table>'));
        var itbl = $("#itemtable");
        var tr = self.myHtmlUtil.addTr(itbl).addClass("header");
        self.myHtmlUtil.addTh(tr, "Num").addClass("num").addClass("sorttable_numeric");
        self.myHtmlUtil.addTh(tr, "id");
        self.myHtmlUtil.addTh(tr, "collection");
        self.myHtmlUtil.addTh(tr, "Item Handle");
        self.myHtmlUtil.addTh(tr, "dc.title" + self.getLangSuffix());
        
        var mdCols = [];
        if (data.metadata) {
            $.each(data.metadata, function(index, field) {
                if (field != "") {
                    self.myHtmlUtil.addTh(tr,field + self.getLangSuffix()).addClass("returnFields");
                    mdCols[mdCols.length] = field;            
                }
            });            
        }
        
        $.each(data.items, function(index, item){
            var tr = self.myHtmlUtil.addTr(itbl);
            tr.addClass(index % 2 == 0 ? "odd data" : "even data");
            self.myHtmlUtil.addTd(tr, self.myReportParameters.getOffset()+index+1).addClass("num");
            self.myHtmlUtil.addTd(tr, self.getId(item));
            if (item.parentCollection == null) {
                self.myHtmlUtil.addTd(tr, "--");            
            } else {
                self.myHtmlUtil.addTdAnchor(tr, item.parentCollection.name, self.ROOTPATH + item.parentCollection.handle);
            }
            self.myHtmlUtil.addTdAnchor(tr, item.handle, self.ROOTPATH + item.handle);
            self.myHtmlUtil.addTd(tr, item.name);
            
            for(var i=0; i<mdCols.length; i++) {
                var key =  mdCols[i];
                var td = self.myHtmlUtil.addTd(tr, "");
                $.each(item.metadata, function(colindex, metadata) {
                    if (metadata.key == key) {
                        if (metadata.value != null) {
                            var div = $("<div>"+metadata.value+"</div>");
                            td.append(div);
                        }
                    }
                });
            }
        });
        
        this.displayItems(data["query-annotation"],
            this.myReportParameters.getOffset(),
            this.myReportParameters.getLimit(),
            data["unfiltered-item-count"],
            function(){
                self.myReportParameters.updateOffset(false);
                self.runQuery();
            }, 
            function(){
                self.myReportParameters.updateOffset(true);
                self.runQuery();
            }
        );
        
        if (this.hasSorttable()) {
            sorttable.makeSortable(itbl[0]);            
        }
        $("#metadatadiv").accordion("option", "active", $("#metadatadiv > h3").length - 1); 
    }
    
    //Ignore the first column containing a row number and the item handle, get handle for the collection
    this.exportCol = function(colnum, col) {
        var data = "";
        if (colnum == 0) return "";
        if (colnum == 3) return "";
        data += (colnum == 1) ? "" : ",";
        
        if (colnum == 2) {
            var anchor = $(col).find("a");
            var href = anchor.is("a") ? anchor.attr("href").replace(self.ROOTPATH,"") : $(col).text();
            data += "\"" + href + "\"";
        } else {
            data += self.exportCell(col);        }
        return data;
    }
}
QueryReport.prototype = Object.create(Report.prototype);

$(document).ready(function(){
    var myReport=new QueryReport();
    myReport.init();
});

var QueryableMetadataFields = function(report) {
    MetadataFields.call(this, report);
    var self = this;
    
    this.initFields = function(data, report) {
        self.metadataSchemas = data;
        var params = report.myReportParameters.params;
        var fields = params["query_field[]"];
        var ops = params["query_op[]"];
        var vals = params["query_val[]"];
        if (fields && ops && vals) {
            if (fields.length == 0) {
                self.drawFilterQuery("*","exists","");
            } else {
                for(var i=0; i<fields.length; i++) {
                    var op = ops.length > i ? ops[i] : "";
                    var val = vals.length > i ? vals[i] : "";
                    self.drawFilterQuery(fields[i],op,val);
                } 
            }                
        }
        self.drawShowFields(params["show_fields[]"]);
        self.initQueries();        
        report.spinner.stop();
        $(".query-button").attr("disabled", false);
    }
    
    this.initQueries = function() {
        $("#predefselect")
          .append($("<option value='new'>New Query</option>"))
          .append($("<option value='q1'>Has No Title</option>"))
          .append($("<option value='q2'>Has No dc.identifier.uri</option>"))
          .append($("<option value='q3'>Has compound subject</option>"))
          .append($("<option value='q4'>Has compound dc.contributor.author</option>"))
          .append($("<option value='q5'>Has compound dc.creator</option>"))
          .append($("<option value='q6'>Has URL in dc.description</option>"))
          .append($("<option value='q7'>Has full text in dc.description.provenance</option>"))
          .append($("<option value='q8'>Has non-full text in dc.description.provenance</option>"))
          .append($("<option value='q9'>Has empty metadata</option>"))
          .append($("<option value='q10'>Has unbreaking metadata in description</option>"))
          .append($("<option value='q12'>Has XML entity in metadata</option>"))
          .append($("<option value='q13'>Has non-ascii character in metadata</option>"))
          .on("change",function(){
              $("div.metadata").remove();
              var val = $("#predefselect").val();
              if (val ==  'new') {
                  self.drawFilterQuery("","","");            
              } else if (val ==  'q1') {
                  self.drawFilterQuery("dc.title","doesnt_exist","");                        
              } else if (val ==  'q2') {
                  self.drawFilterQuery("dc.identifier.uri","doesnt_exist","");                        
              } else if (val ==  'q3') {
                  self.drawFilterQuery("dc.subject.*","like","%;%");                        
              } else if (val ==  'q4') {
                  self.drawFilterQuery("dc.contributor.author","like","% and %");                        
              } else if (val ==  'q5') {
                  self.drawFilterQuery("dc.creator","like","% and %");                        
              } else if (val ==  'q6') {
                  self.drawFilterQuery("dc.description","matches","^.*(http://|https://|mailto:).*$");                        
              } else if (val ==  'q7') {
                  self.drawFilterQuery("dc.description.provenance","matches","^.*No\\. of bitstreams(.|\\r|\\n|\\r\\n)*\\.(PDF|pdf|DOC|doc|PPT|ppt|DOCX|docx|PPTX|pptx).*$");                        
              } else if (val ==  'q8') {
                  self.drawFilterQuery("dc.description.provenance","doesnt_match","^.*No\\. of bitstreams(.|\\r|\\n|\\r\\n)*\\.(PDF|pdf|DOC|doc|PPT|ppt|DOCX|docx|PPTX|pptx).*$");                        
              } else if (val ==  'q9') {
                  self.drawFilterQuery("*","matches","^\\s*$");                        
              } else if (val ==  'q10') {
                  self.drawFilterQuery("dc.description.*","matches","^.*[^\\s]{50,}.*$");                        
              } else if (val ==  'q12') {
                  self.drawFilterQuery("*","matches","^.*&#.*$");                        
              } else if (val ==  'q13') {
                  self.drawFilterQuery("*","matches","^.*[^[:ascii:]].*$");                        
              }
          });
    }

    this.drawFilterQuery = function(pField, pOp, pVal) {
        var div = $("<div class='metadata'/>").appendTo("#queries");
        var sel = $("<select class='query-tool' name='query_field[]'/>");
        var opt = $("<option value='*'>Any Field</option>");
        sel.append(opt);
        $.each(self.metadataSchemas, function(index, schema){
            if (schema.prefix == 'eperson') {
                return;
            }
            $.each(schema.fields, function(findex, field) {
                var name = field.name;
                var parts = name.match(/^([^\.]+)\.([^\.]+)\.([^\.]+)$/);
                if (parts == null) {
                    var wildname = name + ".*";
                    var opt = $("<option/>");
                    opt.attr("value",wildname).text(wildname);
                    sel.append(opt);                
                }
                var opt = $("<option/>");
                opt.attr("value",name).text(name);
                sel.append(opt);
            });
        });
        sel.val(pField);
        div.append(sel);
        var opsel = $("<select class='query-tool' name='query_op[]'/>");
        $("<option>exists</option>").val("exists").appendTo(opsel);
        $("<option>does not exist</option>").val("doesnt_exist").appendTo(opsel);
        $("<option selected>equals</option>").val("equals").appendTo(opsel);
        $("<option>does not equal</option>").val("not_equals").appendTo(opsel);
        $("<option>like</option>").val("like").appendTo(opsel);
        $("<option>not like</option>").val("not_like").appendTo(opsel);
        $("<option>contains</option>").val("contains").appendTo(opsel);
        $("<option>does not contain</option>").val("doesnt_contain").appendTo(opsel);
        $("<option>matches</option>").val("matches").appendTo(opsel);
        $("<option>does not match</option>").val("doesnt_match").appendTo(opsel);
        opsel.val(pOp);
        opsel.change(function(){
            self.valField($(this));
        });
        div.append(opsel);
        var input = $("<input class='query-tool' name='query_val[]'/>");
        div.append(input);
        input.val(pVal);
        self.valField(opsel);
        $("<button class='field_plus'>+</button>").appendTo(div).click(function(){
            self.drawFilterQuery();
            self.queryButtons();
        });
        $("<button class='field_minus'>-</button>").appendTo(div).click(function(){
            $(this).parent("div.metadata").remove();
            self.queryButtons();
        });
        self.queryButtons();
    }

    this.valField = function(valop) {
        var val = valop.val();
        var disableval = (val == "exists" || val == "not_exists");
        var valinput = valop.parent("div.metadata").find("input[name='query_val[]']");
        valinput.attr("readonly",disableval);
        if (disableval) {
            valinput.val("");        
        }
    }

    this.queryButtons = function() {
        $("button.field_plus").attr("disabled",true);
        $("button.field_plus:last").attr("disabled",false);
        $("button.field_minus").attr("disabled",false);
        if ($("button.field_minus").length == 1) {
            $("button.field_minus").attr("disabled",true);                
        }
    }
}
QueryableMetadataFields.prototype = Object.create(MetadataFields.prototype);
