/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


(function(){
if(!this["console"]){
this.console={};
}
var cn=["assert","count","debug","dir","dirxml","error","group","groupEnd","info","profile","profileEnd","time","timeEnd","trace","warn","log"];
var i=0,tn;
while((tn=cn[i++])){
if(!console[tn]){
(function(){
var _4=tn+"";
console[_4]=("log" in console)?function(){
var a=Array.apply({},arguments);
a.unshift(_4+":");
console["log"](a.join(" "));
}:function(){
};
})();
}
}
if(typeof dojo=="undefined"){
this.dojo={_scopeName:"dojo",_scopePrefix:"",_scopePrefixArgs:"",_scopeSuffix:"",_scopeMap:{},_scopeMapRev:{}};
}
var d=dojo;
if(typeof dijit=="undefined"){
this.dijit={_scopeName:"dijit"};
}
if(typeof dojox=="undefined"){
this.dojox={_scopeName:"dojox"};
}
if(!d._scopeArgs){
d._scopeArgs=[dojo,dijit,dojox];
}
d.global=this;
d.config={isDebug:false,debugAtAllCosts:false};
if(typeof djConfig!="undefined"){
for(var _7 in djConfig){
d.config[_7]=djConfig[_7];
}
}
var _8=["Browser","Rhino","Spidermonkey","Mobile"];
var t;
while((t=_8.shift())){
d["is"+t]=false;
}
dojo.locale=d.config.locale;
var _a="$Rev$".match(/\d+/);
dojo.version={major:1,minor:2,patch:3,flag:"",revision:_a?+_a[0]:999999,toString:function(){
with(d.version){
return major+"."+minor+"."+patch+flag+" ("+revision+")";
}
}};
if(typeof OpenAjax!="undefined"){
OpenAjax.hub.registerLibrary(dojo._scopeName,"http://dojotoolkit.org",d.version.toString());
}
dojo._mixin=function(_b,_c){
var _d={};
for(var x in _c){
if(_d[x]===undefined||_d[x]!=_c[x]){
_b[x]=_c[x];
}
}
if(d["isIE"]&&_c){
var p=_c.toString;
if(typeof p=="function"&&p!=_b.toString&&p!=_d.toString&&p!="\nfunction toString() {\n    [native code]\n}\n"){
_b.toString=_c.toString;
}
}
return _b;
};
dojo.mixin=function(obj,_11){
for(var i=1,l=arguments.length;i<l;i++){
d._mixin(obj,arguments[i]);
}
return obj;
};
dojo._getProp=function(_14,_15,_16){
var obj=_16||d.global;
for(var i=0,p;obj&&(p=_14[i]);i++){
if(i==0&&this._scopeMap[p]){
p=this._scopeMap[p];
}
obj=(p in obj?obj[p]:(_15?obj[p]={}:undefined));
}
return obj;
};
dojo.setObject=function(_1a,_1b,_1c){
var _1d=_1a.split("."),p=_1d.pop(),obj=d._getProp(_1d,true,_1c);
return obj&&p?(obj[p]=_1b):undefined;
};
dojo.getObject=function(_20,_21,_22){
return d._getProp(_20.split("."),_21,_22);
};
dojo.exists=function(_23,obj){
return !!d.getObject(_23,false,obj);
};
dojo["eval"]=function(_25){
return d.global.eval?d.global.eval(_25):eval(_25);
};
d.deprecated=d.experimental=function(){
};
})();
