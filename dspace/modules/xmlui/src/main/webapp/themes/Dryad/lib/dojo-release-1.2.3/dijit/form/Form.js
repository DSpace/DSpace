/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.form.Form"]){
dojo._hasResource["dijit.form.Form"]=true;
dojo.provide("dijit.form.Form");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.declare("dijit.form._FormMixin",null,{reset:function(){
dojo.forEach(this.getDescendants(),function(_1){
if(_1.reset){
_1.reset();
}
});
},validate:function(){
var _2=false;
return dojo.every(dojo.map(this.getDescendants(),function(_3){
_3._hasBeenBlurred=true;
var _4=_3.disabled||!_3.validate||_3.validate();
if(!_4&&!_2){
dijit.scrollIntoView(_3.containerNode||_3.domNode);
_3.focus();
_2=true;
}
return _4;
}),function(_5){
return _5;
});
},setValues:function(_6){
dojo.deprecated(this.declaredClass+"::setValues() is deprecated. Use attr('value', val) instead.","","2.0");
return this.attr("value",_6);
},_setValueAttr:function(_7){
var _8={};
dojo.forEach(this.getDescendants(),function(_9){
if(!_9.name){
return;
}
var _a=_8[_9.name]||(_8[_9.name]=[]);
_a.push(_9);
});
for(var _b in _8){
if(!_8.hasOwnProperty(_b)){
continue;
}
var _c=_8[_b],_d=dojo.getObject(_b,false,_7);
if(_d===undefined){
continue;
}
if(!dojo.isArray(_d)){
_d=[_d];
}
if(typeof _c[0].checked=="boolean"){
dojo.forEach(_c,function(w,i){
w.attr("value",dojo.indexOf(_d,w.value)!=-1);
});
}else{
if(_c[0]._multiValue){
_c[0].attr("value",_d);
}else{
dojo.forEach(_c,function(w,i){
w.attr("value",_d[i]);
});
}
}
}
},getValues:function(){
dojo.deprecated(this.declaredClass+"::getValues() is deprecated. Use attr('value') instead.","","2.0");
return this.attr("value");
},_getValueAttr:function(){
var obj={};
dojo.forEach(this.getDescendants(),function(_13){
var _14=_13.name;
if(!_14||_13.disabled){
return;
}
var _15=_13.attr("value");
if(typeof _13.checked=="boolean"){
if(/Radio/.test(_13.declaredClass)){
if(_15!==false){
dojo.setObject(_14,_15,obj);
}
}else{
var ary=dojo.getObject(_14,false,obj);
if(!ary){
ary=[];
dojo.setObject(_14,ary,obj);
}
if(_15!==false){
ary.push(_15);
}
}
}else{
dojo.setObject(_14,_15,obj);
}
});
return obj;
},isValid:function(){
this._invalidWidgets=[];
return dojo.every(this.getDescendants(),function(_17){
var _18=_17.disabled||!_17.isValid||_17.isValid();
if(!_18){
this._invalidWidgets.push(_17);
}
return _18;
},this);
},onValidStateChange:function(_19){
},_widgetChange:function(_1a){
var _1b=this._lastValidState;
if(!_1a||this._lastValidState===undefined){
_1b=this.isValid();
if(this._lastValidState===undefined){
this._lastValidState=_1b;
}
}else{
if(_1a.isValid){
this._invalidWidgets=dojo.filter(this._invalidWidgets||[],function(w){
return (w!=_1a);
},this);
if(!_1a.isValid()&&!_1a.attr("disabled")){
this._invalidWidgets.push(_1a);
}
_1b=(this._invalidWidgets.length===0);
}
}
if(_1b!==this._lastValidState){
this._lastValidState=_1b;
this.onValidStateChange(_1b);
}
},connectChildren:function(){
dojo.forEach(this._changeConnections,dojo.hitch(this,"disconnect"));
var _1d=this;
var _1e=this._changeConnections=[];
dojo.forEach(dojo.filter(this.getDescendants(),function(_1f){
return _1f.validate;
}),function(_20){
_1e.push(_1d.connect(_20,"validate",dojo.hitch(_1d,"_widgetChange",_20)));
_1e.push(_1d.connect(_20,"_setDisabledAttr",dojo.hitch(_1d,"_widgetChange",_20)));
});
this._widgetChange(null);
},startup:function(){
this.inherited(arguments);
this._changeConnections=[];
this.connectChildren();
}});
dojo.declare("dijit.form.Form",[dijit._Widget,dijit._Templated,dijit.form._FormMixin],{name:"",action:"",method:"",encType:"","accept-charset":"",accept:"",target:"",templateString:"<form dojoAttachPoint='containerNode' dojoAttachEvent='onreset:_onReset,onsubmit:_onSubmit' name='${name}'></form>",attributeMap:dojo.mixin(dojo.clone(dijit._Widget.prototype.attributeMap),{action:"",method:"",encType:"","accept-charset":"",accept:"",target:""}),execute:function(_21){
},onExecute:function(){
},_setEncTypeAttr:function(_22){
this.encType=_22;
dojo.attr(this.domNode,"encType",_22);
if(dojo.isIE){
this.domNode.encoding=_22;
}
},postCreate:function(){
if(dojo.isIE&&this.srcNodeRef&&this.srcNodeRef.attributes){
var _23=this.srcNodeRef.attributes.getNamedItem("encType");
if(_23&&!_23.specified&&(typeof _23.value=="string")){
this.attr("encType",_23.value);
}
}
this.inherited(arguments);
},onReset:function(e){
return true;
},_onReset:function(e){
var _26={returnValue:true,preventDefault:function(){
this.returnValue=false;
},stopPropagation:function(){
},currentTarget:e.currentTarget,target:e.target};
if(!(this.onReset(_26)===false)&&_26.returnValue){
this.reset();
}
dojo.stopEvent(e);
return false;
},_onSubmit:function(e){
var fp=dijit.form.Form.prototype;
if(this.execute!=fp.execute||this.onExecute!=fp.onExecute){
dojo.deprecated("dijit.form.Form:execute()/onExecute() are deprecated. Use onSubmit() instead.","","2.0");
this.onExecute();
this.execute(this.getValues());
}
if(this.onSubmit(e)===false){
dojo.stopEvent(e);
}
},onSubmit:function(e){
return this.isValid();
},submit:function(){
if(!(this.onSubmit()===false)){
this.containerNode.submit();
}
}});
}
