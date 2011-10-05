/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid.compat._data.dijitEditors"]){
dojo._hasResource["dojox.grid.compat._data.dijitEditors"]=true;
dojo.provide("dojox.grid.compat._data.dijitEditors");
dojo.require("dojox.grid.compat._data.editors");
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
dojo.declare("dojox.grid.editors.Dijit",dojox.grid.editors.base,{editorClass:"dijit.form.TextBox",constructor:function(_1){
this.editor=null;
this.editorClass=dojo.getObject(this.cell.editorClass||this.editorClass);
},format:function(_2,_3){
this.needFormatNode(_2,_3);
return "<div></div>";
},getValue:function(_4){
return this.editor.getValue();
},setValue:function(_5,_6){
if(this.editor&&this.editor.setValue){
this.editor.setValue(_6);
}else{
this.inherited(arguments);
}
},getEditorProps:function(_7){
return dojo.mixin({},this.cell.editorProps||{},{constraints:dojo.mixin({},this.cell.constraint)||{},value:_7});
},createEditor:function(_8,_9,_a){
return new this.editorClass(this.getEditorProps(_9),_8);
},attachEditor:function(_b,_c,_d){
_b.appendChild(this.editor.domNode);
this.setValue(_d,_c);
},formatNode:function(_e,_f,_10){
if(!this.editorClass){
return _f;
}
if(!this.editor){
this.editor=this.createEditor.apply(this,arguments);
}else{
this.attachEditor.apply(this,arguments);
}
this.sizeEditor.apply(this,arguments);
this.cell.grid.rowHeightChanged(_10);
this.focus();
},sizeEditor:function(_11,_12,_13){
var p=this.cell.getNode(_13),box=dojo.contentBox(p);
dojo.marginBox(this.editor.domNode,{w:box.w});
},focus:function(_16,_17){
if(this.editor){
setTimeout(dojo.hitch(this.editor,function(){
dojox.grid.fire(this,"focus");
}),0);
}
},_finish:function(_18){
this.inherited(arguments);
dojox.grid.removeNode(this.editor.domNode);
}});
dojo.declare("dojox.grid.editors.ComboBox",dojox.grid.editors.Dijit,{editorClass:"dijit.form.ComboBox",getEditorProps:function(_19){
var _1a=[];
dojo.forEach(this.cell.options,function(o){
_1a.push({name:o,value:o});
});
var _1c=new dojo.data.ItemFileReadStore({data:{identifier:"name",items:_1a}});
return dojo.mixin({},this.cell.editorProps||{},{value:_19,store:_1c});
},getValue:function(){
var e=this.editor;
e.setDisplayedValue(e.getDisplayedValue());
return e.getValue();
}});
dojo.declare("dojox.grid.editors.DateTextBox",dojox.grid.editors.Dijit,{editorClass:"dijit.form.DateTextBox",setValue:function(_1e,_1f){
if(this.editor){
this.editor.setValue(new Date(_1f));
}else{
this.inherited(arguments);
}
},getEditorProps:function(_20){
return dojo.mixin(this.inherited(arguments),{value:new Date(_20)});
}});
dojo.declare("dojox.grid.editors.CheckBox",dojox.grid.editors.Dijit,{editorClass:"dijit.form.CheckBox",getValue:function(){
return this.editor.checked;
},setValue:function(_21,_22){
if(this.editor&&this.editor.setAttribute){
this.editor.setAttribute("checked",_22);
}else{
this.inherited(arguments);
}
},sizeEditor:function(_23,_24,_25){
return;
}});
dojo.declare("dojox.grid.editors.Editor",dojox.grid.editors.Dijit,{editorClass:"dijit.Editor",getEditorProps:function(_26){
return dojo.mixin({},this.cell.editorProps||{},{height:this.cell.editorHeight||"100px"});
},createEditor:function(_27,_28,_29){
var _2a=new this.editorClass(this.getEditorProps(_28),_27);
dojo.connect(_2a,"onLoad",dojo.hitch(this,"populateEditor"));
return _2a;
},formatNode:function(_2b,_2c,_2d){
this.content=_2c;
this.inherited(arguments);
if(dojo.isMoz){
var e=this.editor;
e.open();
if(this.cell.editorToolbar){
dojo.place(e.toolbar.domNode,e.editingArea,"before");
}
}
},populateEditor:function(){
this.editor.setValue(this.content);
this.editor.placeCursorAtEnd();
}});
}
