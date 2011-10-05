/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.secure.sandbox"]){
dojo._hasResource["dojox.secure.sandbox"]=true;
dojo.provide("dojox.secure.sandbox");
dojo.require("dojox.secure.DOM");
dojo.require("dojox.secure.capability");
dojo.require("dojo.NodeList-fx");
(function(){
var _1=setTimeout;
var _2=setInterval;
if({}.__proto__){
var _3=function(_4){
var _5=Array.prototype[_4];
if(_5&&!_5.fixed){
(Array.prototype[_4]=function(){
if(this==window){
throw new TypeError("Called with wrong this");
}
return _5.apply(this,arguments);
}).fixed=true;
}
};
_3("concat");
_3("reverse");
_3("sort");
_3("slice");
_3("forEach");
_3("filter");
_3("reduce");
_3("reduceRight");
_3("every");
_3("map");
_3("some");
}
var _6=function(){
return dojo.xhrGet.apply(dojo,arguments);
};
dojox.secure.sandbox=function(_7){
var _8=dojox.secure.DOM(_7);
_7=_8(_7);
var _9=_7.ownerDocument;
var _a=dojox.secure._safeDojoFunctions(_7,_8);
var _b=[];
var _c=["isNaN","isFinite","parseInt","parseFloat","escape","unescape","encodeURI","encodeURIComponent","decodeURI","decodeURIComponent","alert","confirm","prompt","Error","EvalError","RangeError","ReferenceError","SyntaxError","TypeError","Date","RegExp","Number","Object","Array","String","Math","setTimeout","setInterval","clearTimeout","clearInterval","dojo","get","set","forEach","load","evaluate"];
for(var i in _a){
_c.push(i);
_b.push("var "+i+"=dojo."+i);
}
eval(_b.join(";"));
function get(_e,_f){
_f=""+_f;
if(dojox.secure.badProps.test(_f)){
throw new Error("bad property access");
}
if(_e.__get__){
return _e.__get__(_f);
}
return _e[_f];
};
function set(obj,_11,_12){
_11=""+_11;
get(obj,_11);
if(obj.__set){
return obj.__set(_11);
}
obj[_11]=_12;
return _12;
};
function forEach(obj,fun){
if(typeof fun!="function"){
throw new TypeError();
}
if("length" in obj){
if(obj.__get__){
var len=obj.__get__("length");
for(var i=0;i<len;i++){
if(i in obj){
fun.call(obj,obj.__get__(i),i,obj);
}
}
}else{
len=obj.length;
for(i=0;i<len;i++){
if(i in obj){
fun.call(obj,obj[i],i,obj);
}
}
}
}else{
for(i in obj){
fun.call(obj,get(obj,i),i,obj);
}
}
};
function Class(_17,_18,_19){
var _1a,_1b,_1c;
var arg;
for(var i=0,l=arguments.length;typeof (arg=arguments[i])=="function"&&i<l;i++){
if(_1a){
mixin(_1a,arg.prototype);
}else{
_1b=arg;
F=function(){
};
F.prototype=arg.prototype;
_1a=new F;
}
}
if(arg){
for(var j in arg){
var _21=arg[j];
if(typeof _21=="function"){
arg[j]=function(){
if(this instanceof Class){
return arguments.callee.__rawMethod__.apply(this,arguments);
}
throw new Error("Method called on wrong object");
};
arg[j].__rawMethod__=_21;
}
}
if(arg.hasOwnProperty("constructor")){
_1c=arg.constructor;
}
}
_1a=_1a?mixin(_1a,arg):arg;
function Class(){
if(_1b){
_1b.apply(this,arguments);
}
if(_1c){
_1c.apply(this,arguments);
}
};
mixin(Class,arguments[i]);
_1a.constructor=Class;
Class.prototype=_1a;
return Class;
};
function checkString(_22){
if(typeof _22!="function"){
throw new Error("String is not allowed in setTimeout/setInterval");
}
};
function setTimeout(_23,_24){
checkString(_23);
return _1(_23,_24);
};
function setInterval(_25,_26){
checkString(_25);
return _2(_25,_26);
};
function evaluate(_27){
return _8.evaluate(_27);
};
var _28=_8.load=function(url){
if(url.match(/^[\w\s]*:/)){
throw new Error("Access denied to cross-site requests");
}
return _6({url:(new _a._Url(_8.rootUrl,url))+"",secure:true});
};
_8.evaluate=function(_2a){
dojox.secure.capability.validate(_2a,_c,{document:1,element:1});
if(_2a.match(/^\s*[\[\{]/)){
var _2b=eval("("+_2a+")");
}else{
eval(_2a);
}
};
return {loadJS:function(url){
_8.rootUrl=url;
return _6({url:url,secure:true}).addCallback(function(_2d){
evaluate(_2d,_7);
});
},loadHTML:function(url){
_8.rootUrl=url;
return _6({url:url,secure:true}).addCallback(function(_2f){
_7.innerHTML=_2f;
});
},evaluate:function(_30){
return _8.evaluate(_30);
}};
};
})();
dojox.secure._safeDojoFunctions=function(_31,_32){
var _33=["mixin","require","isString","isArray","isFunction","isObject","isArrayLike","isAlien","hitch","delegate","partial","trim","disconnect","subscribe","unsubscribe","Deferred","toJson","style","attr"];
var doc=_31.ownerDocument;
var _35=dojox.secure.unwrap;
dojo.NodeList.prototype.addContent.safetyCheck=function(_36){
_32.safeHTML(_36);
};
dojo.NodeList.prototype.style.safetyCheck=function(_37,_38){
if(_37=="behavior"){
throw new Error("Can not set behavior");
}
_32.safeCSS(_38);
};
dojo.NodeList.prototype.attr.safetyCheck=function(_39,_3a){
if(_3a&&(_39=="src"||_39=="href"||_39=="style")){
throw new Error("Illegal to set "+_39);
}
};
var _3b={query:function(_3c,_3d){
return _32(dojo.query(_3c,_35(_3d||_31)));
},connect:function(el,_3f){
var obj=el;
arguments[0]=_35(el);
if(obj!=arguments[0]&&_3f.substring(0,2)!="on"){
throw new Error("Invalid event name for element");
}
return dojo.connect.apply(dojo,arguments);
},body:function(){
return _31;
},byId:function(id){
return _31.ownerDocument.getElementById(id);
},fromJson:function(str){
dojox.secure.capability.validate(str,[],{});
return dojo.fromJson(str);
}};
for(var i=0;i<_33.length;i++){
_3b[_33[i]]=dojo[_33[i]];
}
return _3b;
};
}
