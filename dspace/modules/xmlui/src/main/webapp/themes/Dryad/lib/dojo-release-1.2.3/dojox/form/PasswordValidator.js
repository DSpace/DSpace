/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.form.PasswordValidator"]){
dojo._hasResource["dojox.form.PasswordValidator"]=true;
dojo.provide("dojox.form.PasswordValidator");
dojo.require("dijit.form._FormWidget");
dojo.require("dijit.form.ValidationTextBox");
dojo.requireLocalization("dojox.form","PasswordValidator",null,"zh,ca,ROOT,pt,da,tr,ru,de,sv,ja,he,fi,nb,el,ar,pt-pt,cs,fr,es,ko,nl,zh-tw,pl,th,it,hu,sk,sl");
dojo.declare("dojox.form._ChildTextBox",dijit.form.ValidationTextBox,{containerWidget:null,type:"password",reset:function(){
dijit.form.ValidationTextBox.prototype._setValueAttr.call(this,"",true);
this._hasBeenBlurred=false;
}});
dojo.declare("dojox.form._OldPWBox",dojox.form._ChildTextBox,{_isPWValid:false,_setValueAttr:function(_1,_2){
if(_1===""){
_1=dojox.form._OldPWBox.superclass.attr.call(this,"value");
}
if(_2!==null){
this._isPWValid=this.containerWidget.pwCheck(_1);
}
this.inherited(arguments);
},isValid:function(_3){
return this.inherited("isValid",arguments)&&this._isPWValid;
},_update:function(e){
if(this._hasBeenBlurred){
this.validate(true);
}
this._onMouse(e);
},_getValueAttr:function(){
if(this.containerWidget._started&&this.containerWidget.isValid()){
return this.inherited(arguments);
}
return "";
}});
dojo.declare("dojox.form._NewPWBox",dojox.form._ChildTextBox,{required:true,onChange:function(){
this.containerWidget._inputWidgets[2].validate(false);
this.inherited(arguments);
}});
dojo.declare("dojox.form._VerifyPWBox",dojox.form._ChildTextBox,{isValid:function(_5){
return this.inherited("isValid",arguments)&&(this.attr("value")==this.containerWidget._inputWidgets[1].attr("value"));
}});
dojo.declare("dojox.form.PasswordValidator",dijit.form._FormValueWidget,{required:true,_inputWidgets:null,oldName:"",templateString:"<div dojoAttachPoint=\"containerNode\">\n\t<input type=\"hidden\" name=\"${name}\" value=\"\" dojoAttachPoint=\"focusNode\" />\n</div>\n",_hasBeenBlurred:false,isValid:function(_6){
return dojo.every(this._inputWidgets,function(i){
if(i&&i._setStateClass){
i._setStateClass();
}
return (!i||i.isValid());
});
},validate:function(_8){
return dojo.every(dojo.map(this._inputWidgets,function(i){
if(i&&i.validate){
i._hasBeenBlurred=(i._hasBeenBlurred||this._hasBeenBlurred);
return i.validate();
}
return true;
},this),"return item;");
},reset:function(){
this._hasBeenBlurred=false;
dojo.forEach(this._inputWidgets,function(i){
if(i&&i.reset){
i.reset();
}
},this);
},_createSubWidgets:function(){
var _b=this._inputWidgets,_c=dojo.i18n.getLocalization("dojox.form","PasswordValidator",this.lang);
dojo.forEach(_b,function(i,_e){
if(i){
var p={containerWidget:this},c;
if(_e===0){
p.name=this.oldName;
p.invalidMessage=_c.badPasswordMessage;
c=dojox.form._OldPWBox;
}else{
if(_e===1){
p.required=this.required;
c=dojox.form._NewPWBox;
}else{
if(_e===2){
p.invalidMessage=_c.nomatchMessage;
c=dojox.form._VerifyPWBox;
}
}
}
_b[_e]=new c(p,i);
}
},this);
},pwCheck:function(_11){
return false;
},postCreate:function(){
this.inherited(arguments);
var _12=this._inputWidgets=[];
dojo.forEach(["old","new","verify"],function(i){
_12.push(dojo.query("input[pwType="+i+"]",this.containerNode)[0]);
},this);
if(!_12[1]||!_12[2]){
throw new Error("Need at least pwType=\"new\" and pwType=\"verify\"");
}
if(this.oldName&&!_12[0]){
throw new Error("Need to specify pwType=\"old\" if using oldName");
}
this._createSubWidgets();
},_setDisabledAttr:function(_14){
this.inherited(arguments);
dojo.forEach(this._inputWidgets,function(i){
if(i&&i.attr){
i.attr("disabled",_14);
}
});
},_setRequiredAttribute:function(_16){
this.required=_16;
dojo.attr(this.focusNode,"required",_16);
dijit.setWaiState(this.focusNode,"required",_16);
this._refreshState();
dojo.forEach(this._inputWidgets,function(i){
if(i&&i.attr){
i.attr("required",_16);
}
});
},_getValueAttr:function(){
if(this.isValid()){
return this._inputWidgets[1].attr("value");
}
return "";
},focus:function(){
var f=false;
dojo.forEach(this._inputWidgets,function(i){
if(i&&!i.isValid()&&!f){
i.focus();
f=true;
}
});
if(!f){
this._inputWidgets[1].focus();
}
}});
}
