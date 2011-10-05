/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.form.NumberTextBox"]){
dojo._hasResource["dijit.form.NumberTextBox"]=true;
dojo.provide("dijit.form.NumberTextBox");
dojo.require("dijit.form.ValidationTextBox");
dojo.require("dojo.number");
dojo.declare("dijit.form.NumberTextBoxMixin",null,{regExpGen:dojo.number.regexp,editOptions:{pattern:"#.######"},_onFocus:function(){
this._setValueAttr(this.attr("value"),false);
this.inherited(arguments);
},_formatter:dojo.number.format,format:function(_1,_2){
if(typeof _1=="string"){
return _1;
}
if(isNaN(_1)){
return "";
}
if(this.editOptions&&this._focused){
_2=dojo.mixin(dojo.mixin({},this.editOptions),this.constraints);
}
return this._formatter(_1,_2);
},parse:dojo.number.parse,filter:function(_3){
return (_3===null||_3===""||_3===undefined)?NaN:this.inherited(arguments);
},serialize:function(_4,_5){
return (typeof _4!="number"||isNaN(_4))?"":this.inherited(arguments);
},_getValueAttr:function(){
var v=this.inherited(arguments);
if(isNaN(v)&&this.textbox.value!==""){
return undefined;
}
return v;
},value:NaN});
dojo.declare("dijit.form.NumberTextBox",[dijit.form.RangeBoundTextBox,dijit.form.NumberTextBoxMixin],{});
}
