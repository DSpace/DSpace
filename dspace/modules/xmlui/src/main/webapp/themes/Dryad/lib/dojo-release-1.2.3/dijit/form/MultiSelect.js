/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.form.MultiSelect"]){
dojo._hasResource["dijit.form.MultiSelect"]=true;
dojo.provide("dijit.form.MultiSelect");
dojo.require("dijit.form._FormWidget");
dojo.declare("dijit.form.MultiSelect",dijit.form._FormWidget,{size:7,templateString:"<select multiple='true' name='${name}' dojoAttachPoint='containerNode,focusNode' dojoAttachEvent='onchange: _onChange'></select>",attributeMap:dojo.mixin(dojo.clone(dijit.form._FormWidget.prototype.attributeMap),{size:"focusNode"}),reset:function(){
this._hasBeenBlurred=false;
this._setValueAttr(this._resetValue,true);
},addSelected:function(_1){
_1.getSelected().forEach(function(n){
this.containerNode.appendChild(n);
if(dojo.isIE){
var s=dojo.getComputedStyle(n);
if(s){
var _4=s.filter;
n.style.filter="alpha(opacity=99)";
n.style.filter=_4;
}
}
this.domNode.scrollTop=this.domNode.offsetHeight;
var _5=_1.domNode.scrollTop;
_1.domNode.scrollTop=0;
_1.domNode.scrollTop=_5;
},this);
},getSelected:function(){
return dojo.query("option",this.containerNode).filter(function(n){
return n.selected;
});
},_getValueAttr:function(){
return this.getSelected().map(function(n){
return n.value;
});
},_multiValue:true,_setValueAttr:function(_8){
dojo.query("option",this.containerNode).forEach(function(n){
n.selected=(dojo.indexOf(_8,n.value)!=-1);
});
},invertSelection:function(_a){
dojo.query("option",this.containerNode).forEach(function(n){
n.selected=!n.selected;
});
this._handleOnChange(this.attr("value"),_a==true);
},_onChange:function(e){
this._handleOnChange(this.attr("value"),true);
},resize:function(_d){
if(_d){
dojo.marginBox(this.domNode,_d);
}
},postCreate:function(){
this._onChange();
}});
}
