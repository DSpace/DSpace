/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.form.CheckedMultiSelect"]){
dojo._hasResource["dojox.form.CheckedMultiSelect"]=true;
dojo.provide("dojox.form.CheckedMultiSelect");
dojo.require("dijit.form.CheckBox");
dojo.require("dojox.form._FormSelectWidget");
dojo.declare("dojox.form._CheckedMultiSelectItem",[dijit._Widget,dijit._Templated],{widgetsInTemplate:true,templateString:"<div class=\"dijitReset ${baseClass}\"\n\t><input class=\"${baseClass}Box\" dojoType=\"dijit.form.CheckBox\" dojoAttachPoint=\"checkBox\" \n\t\tdojoAttachEvent=\"_onClick:_changeBox\" type=\"${_type.type}\" baseClass=\"${_type.baseClass}\"\n\t><div class=\"dijitInline ${baseClass}Label\" dojoAttachPoint=\"labelNode\" dojoAttachEvent=\"onmousedown:_onMouse,onmouseover:_onMouse,onmouseout:_onMouse,onclick:_onClick\"></div\n></div>\n",baseClass:"dojoxMultiSelectItem",option:null,parent:null,disabled:false,postMixInProperties:function(){
if(this.parent._multiValue){
this._type={type:"checkbox",baseClass:"dijitCheckBox"};
}else{
this._type={type:"radio",baseClass:"dijitRadio"};
}
this.inherited(arguments);
},postCreate:function(){
this.inherited(arguments);
this.labelNode.innerHTML=this.option.label;
},_changeBox:function(){
if(this.parent._multiValue){
this.option.selected=this.checkBox.attr("value")&&true;
}else{
this.parent.attr("value",this.option.value);
}
this.parent._updateSelection();
this.parent.focus();
},_onMouse:function(e){
this.checkBox._onMouse(e);
},_onClick:function(e){
this.checkBox._onClick(e);
},_updateBox:function(){
this.checkBox.attr("value",this.option.selected);
},_setDisabledAttr:function(_3){
this.checkBox.attr("disabled",_3);
this.disabled=_3;
}});
dojo.declare("dojox.form.CheckedMultiSelect",dojox.form._FormSelectWidget,{templateString:"",templateString:"<div class=\"dijit dijitReset dijitInline\" dojoAttachEvent=\"onmousedown:_mouseDown,onclick:focus\"\n\t><select class=\"${baseClass}Select\" multiple=\"true\" dojoAttachPoint=\"containerNode,focusNode\"></select\n\t><div dojoAttachPoint=\"wrapperDiv\"></div\n></div>\n",baseClass:"dojoxMultiSelect",_mouseDown:function(e){
dojo.stopEvent(e);
},_addOptionItem:function(_5){
this.wrapperDiv.appendChild(new dojox.form._CheckedMultiSelectItem({option:_5,parent:this}).domNode);
},_updateSelection:function(){
this.inherited(arguments);
dojo.forEach(this._getChildren(),function(c){
c._updateBox();
});
},_getChildren:function(){
return dojo.map(this.wrapperDiv.childNodes,function(n){
return dijit.byNode(n);
});
},invertSelection:function(_8){
dojo.forEach(this.options,function(i){
i.selected=!i.selected;
});
this._updateSelection();
},_setDisabledAttr:function(_a){
this.inherited(arguments);
dojo.forEach(this._getChildren(),function(_b){
if(_b&&_b.attr){
_b.attr("disabled",_a);
}
});
}});
}
