/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.secure.DOM"]){
dojo._hasResource["dojox.secure.DOM"]=true;
dojo.provide("dojox.secure.DOM");
dojo.require("dojox.lang.observable");
dojox.secure.DOM=function(_1){
function safeNode(_2){
if(!_2){
return _2;
}
var _3=_2;
do{
if(_3==_1){
return wrap(_2);
}
}while((_3=_3.parentNode));
return null;
};
function wrap(_4){
if(_4){
if(_4.nodeType){
var _5=nodeObserver(_4);
if(_4.nodeType==1&&typeof _5.style=="function"){
_5.style=styleObserver(_4.style);
_5.ownerDocument=safeDoc;
_5.childNodes={__get__:function(i){
return wrap(_4.childNodes[i]);
},length:0};
}
return _5;
}
if(_4&&typeof _4=="object"){
if(_4.__observable){
return _4.__observable;
}
_5=_4 instanceof Array?[]:{};
_4.__observable=_5;
for(var i in _4){
if(i!="__observable"){
_5[i]=wrap(_4[i]);
}
}
_5.data__=_4;
return _5;
}
if(typeof _4=="function"){
var _8=function(_9){
if(typeof _9=="function"){
return function(){
for(var i=0;i<arguments.length;i++){
arguments[i]=wrap(arguments[i]);
}
return _8(_9.apply(wrap(this),arguments));
};
}
return dojox.secure.unwrap(_9);
};
return function(){
if(_4.safetyCheck){
_4.safetyCheck.apply(_8(this),arguments);
}
for(var i=0;i<arguments.length;i++){
arguments[i]=_8(arguments[i]);
}
return wrap(_4.apply(_8(this),arguments));
};
}
}
return _4;
};
unwrap=dojox.secure.unwrap;
function safeCSS(_c){
_c+="";
if(_c.match(/behavior:|content:|javascript:|binding|expression|\@import/)){
throw new Error("Illegal CSS");
}
var id=_1.id||(_1.id="safe"+(""+Math.random()).substring(2));
return _c.replace(/(\}|^)\s*([^\{]*\{)/g,function(t,a,b){
return a+" #"+id+" "+b;
});
};
function safeURL(url){
if(url.match(/:/)&&!url.match(/^(http|ftp|mailto)/)){
throw new Error("Unsafe URL "+url);
}
};
function safeElement(el){
if(el&&el.nodeType==1){
if(el.tagName.match(/script/i)){
var src=el.src;
if(src&&src!=""){
el.parentNode.removeChild(el);
dojo.xhrGet({url:src,secure:true}).addCallback(function(_14){
safeDoc.evaluate(_14);
});
}else{
var _15=el.innerHTML;
el.parentNode.removeChild(el);
wrap.evaluate(_15);
}
}
if(el.tagName.match(/link/i)){
throw new Error("illegal tag");
}
if(el.tagName.match(/style/i)){
var _16=function(_17){
if(el.styleSheet){
el.styleSheet.cssText=_17;
}else{
var _18=doc.createTextNode(_17);
if(el.childNodes[0]){
el.replaceChild(_18,el.childNodes[0]);
}else{
el.appendChild(_18);
}
}
};
src=el.src;
if(src&&src!=""){
alert("src"+src);
el.src=null;
dojo.xhrGet({url:src,secure:true}).addCallback(function(_19){
_16(safeCSS(_19));
});
}
_16(safeCSS(el.innerHTML));
}
if(el.style){
safeCSS(el.style.cssText);
}
if(el.href){
safeURL(el.href);
}
if(el.src){
safeURL(el.src);
}
var _1a,i=0;
while((_1a=el.attributes[i++])){
if(_1a.name.substring(0,2)=="on"&&_1a.value!="null"&&_1a.value!=""){
throw new Error("event handlers not allowed in the HTML, they must be set with element.addEventListener");
}
}
var _1c=el.childNodes;
for(i=0,l=_1c.length;i<l;i++){
safeElement(_1c[i]);
}
}
};
function safeHTML(_1d){
var div=document.createElement("div");
if(_1d.match(/<object/i)){
throw new Error("The object tag is not allowed");
}
div.innerHTML=_1d;
safeElement(div);
return div;
};
var doc=_1.ownerDocument;
var _20={getElementById:function(id){
return safeNode(doc.getElementById(id));
},createElement:function(_22){
return wrap(doc.createElement(_22));
},createTextNode:function(_23){
return wrap(doc.createTextNode(_23));
},write:function(str){
var div=safeHTML(str);
while(div.childNodes.length){
_1.appendChild(div.childNodes[0]);
}
}};
_20.open=_20.close=function(){
};
var _26={innerHTML:function(_27,_28){

_27.innerHTML=safeHTML(_28).innerHTML;
}};
_26.outerHTML=function(_29,_2a){
throw new Error("Can not set this property");
};
function domChanger(_2b,_2c){
return function(_2d,_2e){
safeElement(_2e[_2c]);
return _2d[_2b](_2e[0]);
};
};
var _2f={appendChild:domChanger("appendChild",0),insertBefore:domChanger("insertBefore",0),replaceChild:domChanger("replaceChild",1),cloneNode:function(_30,_31){
return _30.cloneNode(_31[0]);
},addEventListener:function(_32,_33){
dojo.connect(_32,"on"+_33[0],this,function(_34){
_34=_35(_34||window.event);
_33[1].call(this,_34);
});
}};
_2f.childNodes=_2f.style=_2f.ownerDocument=function(){
};
function makeObserver(_36){
return dojox.lang.makeObservable(function(_37,_38){
var _39;
return _37[_38];
},_36,function(_3a,_3b,_3c,_3d){
for(var i=0;i<_3d.length;i++){
_3d[i]=unwrap(_3d[i]);
}
if(_2f[_3c]){
return wrap(_2f[_3c].call(_3a,_3b,_3d));
}
return wrap(_3b[_3c].apply(_3b,_3d));
},_2f);
};
var _35=makeObserver(function(_3f,_40,_41){
if(_26[_40]){
_26[_40](_3f,_41);
}
_3f[_40]=_41;
});
var _42={behavior:1,MozBinding:1};
var _43=makeObserver(function(_44,_45,_46){
if(!_42[_45]){
_44[_45]=safeCSS(_46);
}
});
wrap.safeHTML=safeHTML;
wrap.safeCSS=safeCSS;
return wrap;
};
dojox.secure.unwrap=function unwrap(_47){
return (_47&&_47.data__)||_47;
};
}
