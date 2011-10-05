/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid.cells.dijit"]){
dojo._hasResource["dojox.grid.cells.dijit"]=true;
dojo.provide("dojox.grid.cells.dijit");
dojo.require("dojox.grid.cells");
dojo.require("dijit.form.DateTextBox");
dojo.require("dijit.form.TimeTextBox");
dojo.require("dijit.form.ComboBox");
dojo.require("dojo.data.ItemFileReadStore");
dojo.require("dijit.form.CheckBox");
dojo.require("dijit.form.TextBox");
dojo.require("dijit.form.NumberSpinner");
dojo.require("dijit.form.NumberTextBox");
dojo.require("dijit.form.CurrencyTextBox");
dojo.require("dijit.form.Slider");
dojo.require("dijit.Editor");
(function(){
var _1=dojox.grid.cells;
dojo.declare("dojox.grid.cells._Widget",_1._Base,{widgetClass:"dijit.form.TextBox",constructor:function(_2){
this.widget=null;
if(typeof this.widgetClass=="string"){
this.widgetClass=dojo.getObject(this.widgetClass);
}
},formatEditing:function(_3,_4){
this.needFormatNode(_3,_4);
return "<div></div>";
},getValue:function(_5){
return this.widget.attr("value");
},setValue:function(_6,_7){
if(this.widget&&this.widget.setValue){
this.widget.setValue(_7);
}else{
this.inherited(arguments);
}
},getWidgetProps:function(_8){
return dojo.mixin({},this.widgetProps||{},{constraints:dojo.mixin({},this.constraint)||{},value:_8});
},createWidget:function(_9,_a,_b){
return new this.widgetClass(this.getWidgetProps(_a),_9);
},attachWidget:function(_c,_d,_e){
_c.appendChild(this.widget.domNode);
this.setValue(_e,_d);
},formatNode:function(_f,_10,_11){
if(!this.widgetClass){
return _10;
}
if(!this.widget){
this.widget=this.createWidget.apply(this,arguments);
}else{
this.attachWidget.apply(this,arguments);
}
this.sizeWidget.apply(this,arguments);
this.grid.rowHeightChanged(_11);
this.focus();
},sizeWidget:function(_12,_13,_14){
var p=this.getNode(_14),box=dojo.contentBox(p);
dojo.marginBox(this.widget.domNode,{w:box.w});
},focus:function(_17,_18){
if(this.widget){
setTimeout(dojo.hitch(this.widget,function(){
dojox.grid.util.fire(this,"focus");
}),0);
}
},_finish:function(_19){
this.inherited(arguments);
dojox.grid.util.removeNode(this.widget.domNode);
}});
_1._Widget.markupFactory=function(_1a,_1b){
_1._Base.markupFactory(_1a,_1b);
var d=dojo;
var _1d=d.trim(d.attr(_1a,"widgetProps")||"");
var _1e=d.trim(d.attr(_1a,"constraint")||"");
var _1f=d.trim(d.attr(_1a,"widgetClass")||"");
if(_1d){
_1b.widgetProps=d.fromJson(_1d);
}
if(_1e){
_1b.constraint=d.fromJson(_1e);
}
if(_1f){
_1b.widgetClass=d.getObject(_1f);
}
};
dojo.declare("dojox.grid.cells.ComboBox",_1._Widget,{widgetClass:"dijit.form.ComboBox",getWidgetProps:function(_20){
var _21=[];
dojo.forEach(this.options,function(o){
_21.push({name:o,value:o});
});
var _23=new dojo.data.ItemFileReadStore({data:{identifier:"name",items:_21}});
return dojo.mixin({},this.widgetProps||{},{value:_20,store:_23});
},getValue:function(){
var e=this.widget;
e.attr("displayedValue",e.attr("displayedValue"));
return e.attr("value");
}});
_1.ComboBox.markupFactory=function(_25,_26){
_1._Widget.markupFactory(_25,_26);
var d=dojo;
var _28=d.trim(d.attr(_25,"options")||"");
if(_28){
var o=_28.split(",");
if(o[0]!=_28){
_26.options=o;
}
}
};
dojo.declare("dojox.grid.cells.DateTextBox",_1._Widget,{widgetClass:"dijit.form.DateTextBox",setValue:function(_2a,_2b){
if(this.widget){
this.widget.setValue(new Date(_2b));
}else{
this.inherited(arguments);
}
},getWidgetProps:function(_2c){
return dojo.mixin(this.inherited(arguments),{value:new Date(_2c)});
}});
_1.DateTextBox.markupFactory=function(_2d,_2e){
_1._Widget.markupFactory(_2d,_2e);
};
dojo.declare("dojox.grid.cells.CheckBox",_1._Widget,{widgetClass:"dijit.form.CheckBox",getValue:function(){
return this.widget.checked;
},setValue:function(_2f,_30){
if(this.widget&&this.widget.setAttribute){
this.widget.setAttribute("checked",_30);
}else{
this.inherited(arguments);
}
},sizeWidget:function(_31,_32,_33){
return;
}});
_1.CheckBox.markupFactory=function(_34,_35){
_1._Widget.markupFactory(_34,_35);
};
dojo.declare("dojox.grid.cells.Editor",_1._Widget,{widgetClass:"dijit.Editor",getWidgetProps:function(_36){
return dojo.mixin({},this.widgetProps||{},{height:this.widgetHeight||"100px"});
},createWidget:function(_37,_38,_39){
var _3a=new this.widgetClass(this.getWidgetProps(_38),_37);
dojo.connect(_3a,"onLoad",dojo.hitch(this,"populateEditor"));
return _3a;
},formatNode:function(_3b,_3c,_3d){
this.content=_3c;
this.inherited(arguments);
if(dojo.isMoz){
var e=this.widget;
e.open();
if(this.widgetToolbar){
dojo.place(e.toolbar.domNode,e.editingArea,"before");
}
}
},populateEditor:function(){
this.widget.setValue(this.content);
this.widget.placeCursorAtEnd();
}});
_1.Editor.markupFactory=function(_3f,_40){
_1._Widget.markupFactory(_3f,_40);
var d=dojo;
var h=dojo.trim(dojo.attr(_3f,"widgetHeight")||"");
if(h){
if((h!="auto")&&(h.substr(-2)!="em")){
h=parseInt(w)+"px";
}
_40.widgetHeight=h;
}
};
})();
}
