/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.parser"]){
dojo._hasResource["dojo.parser"]=true;
dojo.provide("dojo.parser");
dojo.require("dojo.date.stamp");
dojo.parser=new function(){
var d=dojo;
var _2=d._scopeName+"Type";
var _3="["+_2+"]";
function val2type(_4){
if(d.isString(_4)){
return "string";
}
if(typeof _4=="number"){
return "number";
}
if(typeof _4=="boolean"){
return "boolean";
}
if(d.isFunction(_4)){
return "function";
}
if(d.isArray(_4)){
return "array";
}
if(_4 instanceof Date){
return "date";
}
if(_4 instanceof d._Url){
return "url";
}
return "object";
};
function str2obj(_5,_6){
switch(_6){
case "string":
return _5;
case "number":
return _5.length?Number(_5):NaN;
case "boolean":
return typeof _5=="boolean"?_5:!(_5.toLowerCase()=="false");
case "function":
if(d.isFunction(_5)){
_5=_5.toString();
_5=d.trim(_5.substring(_5.indexOf("{")+1,_5.length-1));
}
try{
if(_5.search(/[^\w\.]+/i)!=-1){
_5=d.parser._nameAnonFunc(new Function(_5),this);
}
return d.getObject(_5,false);
}
catch(e){
return new Function();
}
case "array":
return _5?_5.split(/\s*,\s*/):[];
case "date":
switch(_5){
case "":
return new Date("");
case "now":
return new Date();
default:
return d.date.stamp.fromISOString(_5);
}
case "url":
return d.baseUrl+_5;
default:
return d.fromJson(_5);
}
};
var _7={};
function getClassInfo(_8){
if(!_7[_8]){
var _9=d.getObject(_8);
if(!d.isFunction(_9)){
throw new Error("Could not load class '"+_8+"'. Did you spell the name correctly and use a full path, like 'dijit.form.Button'?");
}
var _a=_9.prototype;
var _b={};
for(var _c in _a){
if(_c.charAt(0)=="_"){
continue;
}
var _d=_a[_c];
_b[_c]=val2type(_d);
}
_7[_8]={cls:_9,params:_b};
}
return _7[_8];
};
this._functionFromScript=function(_e){
var _f="";
var _10="";
var _11=_e.getAttribute("args");
if(_11){
d.forEach(_11.split(/\s*,\s*/),function(_12,idx){
_f+="var "+_12+" = arguments["+idx+"]; ";
});
}
var _14=_e.getAttribute("with");
if(_14&&_14.length){
d.forEach(_14.split(/\s*,\s*/),function(_15){
_f+="with("+_15+"){";
_10+="}";
});
}
return new Function(_f+_e.innerHTML+_10);
};
this.instantiate=function(_16){
var _17=[];
d.forEach(_16,function(_18){
if(!_18){
return;
}
var _19=_18.getAttribute(_2);
if((!_19)||(!_19.length)){
return;
}
var _1a=getClassInfo(_19);
var _1b=_1a.cls;
var ps=_1b._noScript||_1b.prototype._noScript;
var _1d={};
var _1e=_18.attributes;
for(var _1f in _1a.params){
var _20=_1e.getNamedItem(_1f);
if(!_20||(!_20.specified&&(!dojo.isIE||_1f.toLowerCase()!="value"))){
continue;
}
var _21=_20.value;
switch(_1f){
case "class":
_21=_18.className;
break;
case "style":
_21=_18.style&&_18.style.cssText;
}
var _22=_1a.params[_1f];
_1d[_1f]=str2obj(_21,_22);
}
if(!ps){
var _23=[],_24=[];
d.query("> script[type^='dojo/']",_18).orphan().forEach(function(_25){
var _26=_25.getAttribute("event"),_19=_25.getAttribute("type"),nf=d.parser._functionFromScript(_25);
if(_26){
if(_19=="dojo/connect"){
_23.push({event:_26,func:nf});
}else{
_1d[_26]=nf;
}
}else{
_24.push(nf);
}
});
}
var _28=_1b["markupFactory"];
if(!_28&&_1b["prototype"]){
_28=_1b.prototype["markupFactory"];
}
var _29=_28?_28(_1d,_18,_1b):new _1b(_1d,_18);
_17.push(_29);
var _2a=_18.getAttribute("jsId");
if(_2a){
d.setObject(_2a,_29);
}
if(!ps){
d.forEach(_23,function(_2b){
d.connect(_29,_2b.event,null,_2b.func);
});
d.forEach(_24,function(_2c){
_2c.call(_29);
});
}
});
d.forEach(_17,function(_2d){
if(_2d&&_2d.startup&&!_2d._started&&(!_2d.getParent||!_2d.getParent())){
_2d.startup();
}
});
return _17;
};
this.parse=function(_2e){
var _2f=d.query(_3,_2e);
var _30=this.instantiate(_2f);
return _30;
};
}();
(function(){
var _31=function(){
if(dojo.config["parseOnLoad"]==true){
dojo.parser.parse();
}
};
if(dojo.exists("dijit.wai.onload")&&(dijit.wai.onload===dojo._loaders[0])){
dojo._loaders.splice(1,0,_31);
}else{
dojo._loaders.unshift(_31);
}
})();
dojo.parser._anonCtr=0;
dojo.parser._anon={};
dojo.parser._nameAnonFunc=function(_32,_33){
var jpn="$joinpoint";
var nso=(_33||dojo.parser._anon);
if(dojo.isIE){
var cn=_32["__dojoNameCache"];
if(cn&&nso[cn]===_32){
return _32["__dojoNameCache"];
}
}
var ret="__"+dojo.parser._anonCtr++;
while(typeof nso[ret]!="undefined"){
ret="__"+dojo.parser._anonCtr++;
}
nso[ret]=_32;
return ret;
};
}
