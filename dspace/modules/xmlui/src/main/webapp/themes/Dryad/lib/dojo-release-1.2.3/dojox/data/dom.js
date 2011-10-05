/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.dom"]){
dojo._hasResource["dojox.data.dom"]=true;
dojo.provide("dojox.data.dom");
dojo.experimental("dojox.data.dom");
dojox.data.dom.createDocument=function(_1,_2){
var _3=dojo.doc;
if(!_2){
_2="text/xml";
}
if(_1&&dojo.trim(_1)!==""&&(typeof dojo.global["DOMParser"])!=="undefined"){
var _4=new DOMParser();
return _4.parseFromString(_1,_2);
}else{
if((typeof dojo.global["ActiveXObject"])!=="undefined"){
var _5=["MSXML2","Microsoft","MSXML","MSXML3"];
for(var i=0;i<_5.length;i++){
try{
var _7=new ActiveXObject(_5[i]+".XMLDOM");
if(_1){
if(_7){
_7.async=false;
_7.loadXML(_1);
return _7;
}else{

}
}else{
if(_7){
return _7;
}
}
}
catch(e){
}
}
}else{
if((_3.implementation)&&(_3.implementation.createDocument)){
if(_1&&dojo.trim(_1)!==""){
if(_3.createElement){
var _8=_3.createElement("xml");
_8.innerHTML=_1;
var _9=_3.implementation.createDocument("foo","",null);
for(var i=0;i<_8.childNodes.length;i++){
_9.importNode(_8.childNodes.item(i),true);
}
return _9;
}
}else{
return _3.implementation.createDocument("","",null);
}
}
}
}
return null;
};
dojox.data.dom.textContent=function(_a,_b){
if(arguments.length>1){
var _c=_a.ownerDocument||dojo.doc;
dojox.data.dom.replaceChildren(_a,_c.createTextNode(_b));
return _b;
}else{
if(_a.textContent!==undefined){
return _a.textContent;
}
var _d="";
if(_a==null){
return _d;
}
for(var i=0;i<_a.childNodes.length;i++){
switch(_a.childNodes[i].nodeType){
case 1:
case 5:
_d+=dojox.data.dom.textContent(_a.childNodes[i]);
break;
case 3:
case 2:
case 4:
_d+=_a.childNodes[i].nodeValue;
break;
default:
break;
}
}
return _d;
}
};
dojox.data.dom.replaceChildren=function(_f,_10){
var _11=[];
if(dojo.isIE){
for(var i=0;i<_f.childNodes.length;i++){
_11.push(_f.childNodes[i]);
}
}
dojox.data.dom.removeChildren(_f);
for(var i=0;i<_11.length;i++){
dojo._destroyElement(_11[i]);
}
if(!dojo.isArray(_10)){
_f.appendChild(_10);
}else{
for(var i=0;i<_10.length;i++){
_f.appendChild(_10[i]);
}
}
};
dojox.data.dom.removeChildren=function(_13){
var _14=_13.childNodes.length;
while(_13.hasChildNodes()){
_13.removeChild(_13.firstChild);
}
return _14;
};
dojox.data.dom.innerXML=function(_15){
if(_15.innerXML){
return _15.innerXML;
}else{
if(_15.xml){
return _15.xml;
}else{
if(typeof XMLSerializer!="undefined"){
return (new XMLSerializer()).serializeToString(_15);
}
}
}
};
}
