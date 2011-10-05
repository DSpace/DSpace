/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.wire.ml.util"]){
dojo._hasResource["dojox.wire.ml.util"]=true;
dojo.provide("dojox.wire.ml.util");
dojo.require("dojox.data.dom");
dojo.require("dojox.wire.Wire");
dojox.wire.ml._getValue=function(_1,_2){
if(!_1){
return undefined;
}
var _3=undefined;
if(_2&&_1.length>=9&&_1.substring(0,9)=="arguments"){
_3=_1.substring(9);
return new dojox.wire.Wire({property:_3}).getValue(_2);
}
var i=_1.indexOf(".");
if(i>=0){
_3=_1.substring(i+1);
_1=_1.substring(0,i);
}
var _5=(dijit.byId(_1)||dojo.byId(_1)||dojo.getObject(_1));
if(!_5){
return undefined;
}
if(!_3){
return _5;
}else{
return new dojox.wire.Wire({object:_5,property:_3}).getValue();
}
};
dojox.wire.ml._setValue=function(_6,_7){
if(!_6){
return;
}
var i=_6.indexOf(".");
if(i<0){
return;
}
var _9=this._getValue(_6.substring(0,i));
if(!_9){
return;
}
var _a=_6.substring(i+1);
new dojox.wire.Wire({object:_9,property:_a}).setValue(_7);
};
dojo.declare("dojox.wire.ml.XmlElement",null,{constructor:function(_b){
if(dojo.isString(_b)){
_b=this._getDocument().createElement(_b);
}
this.element=_b;
},getPropertyValue:function(_c){
var _d=undefined;
if(!this.element){
return _d;
}
if(!_c){
return _d;
}
if(_c.charAt(0)=="@"){
var _e=_c.substring(1);
_d=this.element.getAttribute(_e);
}else{
if(_c=="text()"){
var _f=this.element.firstChild;
if(_f){
_d=_f.nodeValue;
}
}else{
var _10=[];
for(var i=0;i<this.element.childNodes.length;i++){
var _12=this.element.childNodes[i];
if(_12.nodeType===1&&_12.nodeName==_c){
_10.push(new dojox.wire.ml.XmlElement(_12));
}
}
if(_10.length>0){
if(_10.length===1){
_d=_10[0];
}else{
_d=_10;
}
}
}
}
return _d;
},setPropertyValue:function(_13,_14){
if(!this.element){
return;
}
if(!_13){
return;
}
if(_13.charAt(0)=="@"){
var _15=_13.substring(1);
if(_14){
this.element.setAttribute(_15,_14);
}else{
this.element.removeAttribute(_15);
}
}else{
if(_13=="text()"){
while(this.element.firstChild){
this.element.removeChild(this.element.firstChild);
}
if(_14){
var _16=this._getDocument().createTextNode(_14);
this.element.appendChild(_16);
}
}else{
var _17=null;
for(var i=this.element.childNodes.length-1;i>=0;i--){
var _19=this.element.childNodes[i];
if(_19.nodeType===1&&_19.nodeName==_13){
if(!_17){
_17=_19.nextSibling;
}
this.element.removeChild(_19);
}
}
if(_14){
if(dojo.isArray(_14)){
for(var i in _14){
var e=_14[i];
if(e.element){
this.element.insertBefore(e.element,_17);
}
}
}else{
if(_14 instanceof dojox.wire.ml.XmlElement){
if(_14.element){
this.element.insertBefore(_14.element,_17);
}
}else{
var _19=this._getDocument().createElement(_13);
var _16=this._getDocument().createTextNode(_14);
_19.appendChild(_16);
this.element.insertBefore(_19,_17);
}
}
}
}
}
},toString:function(){
var s="";
if(this.element){
var _1c=this.element.firstChild;
if(_1c){
s=_1c.nodeValue;
}
}
return s;
},toObject:function(){
if(!this.element){
return null;
}
var _1d="";
var obj={};
var _1f=0;
for(var i=0;i<this.element.childNodes.length;i++){
var _21=this.element.childNodes[i];
if(_21.nodeType===1){
_1f++;
var o=new dojox.wire.ml.XmlElement(_21).toObject();
var _23=_21.nodeName;
var p=obj[_23];
if(!p){
obj[_23]=o;
}else{
if(dojo.isArray(p)){
p.push(o);
}else{
obj[_23]=[p,o];
}
}
}else{
if(_21.nodeType===3||_21.nodeType===4){
_1d+=_21.nodeValue;
}
}
}
var _25=0;
if(this.element.nodeType===1){
_25=this.element.attributes.length;
for(var i=0;i<_25;i++){
var _26=this.element.attributes[i];
obj["@"+_26.nodeName]=_26.nodeValue;
}
}
if(_1f===0){
if(_25===0){
return _1d;
}
obj["text()"]=_1d;
}
return obj;
},_getDocument:function(){
if(this.element){
return (this.element.nodeType==9?this.element:this.element.ownerDocument);
}else{
return dojox.data.dom.createDocument();
}
}});
}
