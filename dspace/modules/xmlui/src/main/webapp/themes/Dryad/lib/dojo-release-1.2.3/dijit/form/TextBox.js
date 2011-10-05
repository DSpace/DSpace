/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.form.TextBox"]){
dojo._hasResource["dijit.form.TextBox"]=true;
dojo.provide("dijit.form.TextBox");
dojo.require("dijit.form._FormWidget");
dojo.declare("dijit.form.TextBox",dijit.form._FormValueWidget,{trim:false,uppercase:false,lowercase:false,propercase:false,maxLength:"",templateString:"<input class=\"dijit dijitReset dijitLeft\" dojoAttachPoint='textbox,focusNode' name=\"${name}\"\n\tdojoAttachEvent='onmouseenter:_onMouse,onmouseleave:_onMouse,onfocus:_onMouse,onblur:_onMouse,onkeypress:_onKeyPress'\n\tautocomplete=\"off\" type=\"${type}\"\n\t/>\n",baseClass:"dijitTextBox",attributeMap:dojo.mixin(dojo.clone(dijit.form._FormValueWidget.prototype.attributeMap),{maxLength:"focusNode"}),_getValueAttr:function(){
return this.parse(this.attr("displayedValue"),this.constraints);
},_setValueAttr:function(_1,_2,_3){
var _4;
if(_1!==undefined){
_4=this.filter(_1);
if(_4!==null&&((typeof _4!="number")||!isNaN(_4))){
if(typeof _3!="string"){
_3=this.format(_4,this.constraints);
}
}else{
_3="";
}
}
if(_3!=null&&_3!=undefined){
this.textbox.value=_3;
}
dijit.form.TextBox.superclass._setValueAttr.call(this,_4,_2);
},displayedValue:"",getDisplayedValue:function(){
dojo.deprecated(this.declaredClass+"::getDisplayedValue() is deprecated. Use attr('displayedValue') instead.","","2.0");
return this.attr("displayedValue");
},_getDisplayedValueAttr:function(){
return this.filter(this.textbox.value);
},setDisplayedValue:function(_5){
dojo.deprecated(this.declaredClass+"::setDisplayedValue() is deprecated. Use attr('displayedValue', ...) instead.","","2.0");
this.attr("displayedValue",_5);
},_setDisplayedValueAttr:function(_6){
this.textbox.value=_6;
this._setValueAttr(this.attr("value"));
},format:function(_7,_8){
return ((_7==null||_7==undefined)?"":(_7.toString?_7.toString():_7));
},parse:function(_9,_a){
return _9;
},postCreate:function(){
this.textbox.setAttribute("value",this.textbox.value);
this.inherited(arguments);
this._layoutHack();
},filter:function(_b){
if(typeof _b!="string"){
return _b;
}
if(this.trim){
_b=dojo.trim(_b);
}
if(this.uppercase){
_b=_b.toUpperCase();
}
if(this.lowercase){
_b=_b.toLowerCase();
}
if(this.propercase){
_b=_b.replace(/[^\s]+/g,function(_c){
return _c.substring(0,1).toUpperCase()+_c.substring(1);
});
}
return _b;
},_setBlurValue:function(){
this._setValueAttr(this.attr("value"),(this.isValid?this.isValid():true));
},_onBlur:function(){
this._setBlurValue();
this.inherited(arguments);
}});
dijit.selectInputText=function(_d,_e,_f){
var _10=dojo.global;
var _11=dojo.doc;
_d=dojo.byId(_d);
if(isNaN(_e)){
_e=0;
}
if(isNaN(_f)){
_f=_d.value?_d.value.length:0;
}
_d.focus();
if(_11["selection"]&&dojo.body()["createTextRange"]){
if(_d.createTextRange){
var _12=_d.createTextRange();
with(_12){
collapse(true);
moveStart("character",_e);
moveEnd("character",_f);
select();
}
}
}else{
if(_10["getSelection"]){
var _13=_10.getSelection();
if(_d.setSelectionRange){
_d.setSelectionRange(_e,_f);
}
}
}
};
}
