/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.xml.DomParser"]){
dojo._hasResource["dojox.xml.DomParser"]=true;
dojo.provide("dojox.xml.DomParser");
dojox.xml.DomParser=new (function(){
var _1={ELEMENT:1,ATTRIBUTE:2,TEXT:3,CDATA_SECTION:4,PROCESSING_INSTRUCTION:7,COMMENT:8,DOCUMENT:9};
var _2=/<([^>\/\s+]*)([^>]*)>([^<]*)/g;
var _3=/([^=]*)=(("([^"]*)")|('([^']*)'))/g;
var _4=/<!ENTITY\s+([^"]*)\s+"([^"]*)">/g;
var _5=/<!\[CDATA\[([\u0001-\uFFFF]*?)\]\]>/g;
var _6=/<!--([\u0001-\uFFFF]*?)-->/g;
var _7=/^\s+|\s+$/g;
var _8=/\s+/g;
var _9=/\&gt;/g;
var _a=/\&lt;/g;
var _b=/\&quot;/g;
var _c=/\&apos;/g;
var _d=/\&amp;/g;
var _e="_def_";
function _doc(){
return new (function(){
var _f={};
this.nodeType=_1.DOCUMENT;
this.nodeName="#document";
this.namespaces={};
this._nsPaths={};
this.childNodes=[];
this.documentElement=null;
this._add=function(obj){
if(typeof (obj.id)!="undefined"){
_f[obj.id]=obj;
}
};
this._remove=function(id){
if(_f[id]){
delete _f[id];
}
};
this.byId=this.getElementById=function(id){
return _f[id];
};
this.byName=this.getElementsByTagName=byName;
this.byNameNS=this.getElementsByTagNameNS=byNameNS;
this.childrenByName=childrenByName;
})();
};
function byName(_13){
function __(_14,_15,arr){
dojo.forEach(_14.childNodes,function(c){
if(c.nodeType==_1.ELEMENT){
if(_15=="*"){
arr.push(c);
}else{
if(c.nodeName==_15){
arr.push(c);
}
}
__(c,_15,arr);
}
});
};
var a=[];
__(this,_13,a);
return a;
};
function byNameNS(_19,ns){
function __(_1b,_1c,ns,arr){
dojo.forEach(_1b.childNodes,function(c){
if(c.nodeType==_1.ELEMENT){
if(_1c=="*"&&c.ownerDocument._nsPaths[ns]==c.namespace){
arr.push(c);
}else{
if(c.localName==_1c&&c.ownerDocument._nsPaths[ns]==c.namespace){
arr.push(c);
}
}
__(c,_1c,ns,arr);
}
});
};
if(!ns){
ns=_e;
}
var a=[];
__(this,_19,ns,a);
return a;
};
function childrenByName(_21){
var a=[];
dojo.forEach(this.childNodes,function(c){
if(c.nodeType==_1.ELEMENT){
if(_21=="*"){
a.push(c);
}else{
if(c.nodeName==_21){
a.push(c);
}
}
}
});
return a;
};
function _createTextNode(v){
return {nodeType:_1.TEXT,nodeName:"#text",nodeValue:v.replace(_8," ").replace(_9,">").replace(_a,"<").replace(_c,"'").replace(_b,"\"").replace(_d,"&")};
};
function getAttr(_25){
for(var i=0;i<this.attributes.length;i++){
if(this.attributes[i].nodeName==_25){
return this.attributes[i].nodeValue;
}
}
return null;
};
function getAttrNS(_27,ns){
for(var i=0;i<this.attributes.length;i++){
if(this.ownerDocument._nsPaths[ns]==this.attributes[i].namespace&&this.attributes[i].localName==_27){
return this.attributes[i].nodeValue;
}
}
return null;
};
function setAttr(_2a,val){
var old=null;
for(var i=0;i<this.attributes.length;i++){
if(this.attributes[i].nodeName==_2a){
old=this.attributes[i].nodeValue;
this.attributes[i].nodeValue=val;
break;
}
}
if(_2a=="id"){
if(old!=null){
this.ownerDocument._remove(old);
}
this.ownerDocument._add(this);
}
};
function setAttrNS(_2e,val,ns){
for(var i=0;i<this.attributes.length;i++){
if(this.ownerDocument._nsPaths[ns]==this.attributes[i].namespace&&this.attributes[i].localName==_2e){
this.attributes[i].nodeValue=val;
return;
}
}
};
function prev(){
var p=this.parentNode;
if(p){
for(var i=0;i<p.childNodes.length;i++){
if(p.childNodes[i]==this&&i>0){
return p.childNodes[i-1];
}
}
}
return null;
};
function next(){
var p=this.parentNode;
if(p){
for(var i=0;i<p.childNodes.length;i++){
if(p.childNodes[i]==this&&(i+1)<p.childNodes.length){
return p.childNodes[i+1];
}
}
}
return null;
};
this.parse=function(str){
var _37=_doc();
if(str==null){
return _37;
}
if(str.length==0){
return _37;
}
if(str.indexOf("<!ENTITY")>0){
var _38,eRe=[];
if(_4.test(str)){
_4.lastIndex=0;
while((_38=_4.exec(str))!=null){
eRe.push({entity:"&"+_38[1].replace(_7,"")+";",expression:_38[2]});
}
for(var i=0;i<eRe.length;i++){
str=str.replace(new RegExp(eRe[i].entity,"g"),eRe[i].expression);
}
}
}
var _3b=[],_3c;
while((_3c=_5.exec(str))!=null){
_3b.push(_3c[1]);
}
for(var i=0;i<_3b.length;i++){
str=str.replace(_3b[i],i);
}
var _3d=[],_3e;
while((_3e=_6.exec(str))!=null){
_3d.push(_3e[1]);
}
for(i=0;i<_3d.length;i++){
str=str.replace(_3d[i],i);
}
var res,obj=_37;
while((res=_2.exec(str))!=null){
if(res[2].charAt(0)=="/"&&res[2].replace(_7,"").length>1){
if(obj.parentNode){
obj=obj.parentNode;
}
var _41=(res[3]||"").replace(_7,"");
if(_41.length>0){
obj.childNodes.push(_createTextNode(_41));
}
}else{
if(res[1].length>0){
if(res[1].charAt(0)=="?"){
var _42=res[1].substr(1);
var _43=res[2].substr(0,res[2].length-2);
obj.childNodes.push({nodeType:_1.PROCESSING_INSTRUCTION,nodeName:_42,nodeValue:_43});
}else{
if(res[1].charAt(0)=="!"){
if(res[1].indexOf("![CDATA[")==0){
var val=parseInt(res[1].replace("![CDATA[","").replace("]]",""));
obj.childNodes.push({nodeType:_1.CDATA_SECTION,nodeName:"#cdata-section",nodeValue:_3b[val]});
}else{
if(res[1].substr(0,3)=="!--"){
var val=parseInt(res[1].replace("!--","").replace("--",""));
obj.childNodes.push({nodeType:_1.COMMENT,nodeName:"#comment",nodeValue:_3d[val]});
}
}
}else{
var _42=res[1].replace(_7,"");
var o={nodeType:_1.ELEMENT,nodeName:_42,localName:_42,namespace:_e,ownerDocument:_37,attributes:[],parentNode:null,childNodes:[]};
if(_42.indexOf(":")>-1){
var t=_42.split(":");
o.namespace=t[0];
o.localName=t[1];
}
o.byName=o.getElementsByTagName=byName;
o.byNameNS=o.getElementsByTagNameNS=byNameNS;
o.childrenByName=childrenByName;
o.getAttribute=getAttr;
o.getAttributeNS=getAttrNS;
o.setAttribute=setAttr;
o.setAttributeNS=setAttrNS;
o.previous=o.previousSibling=prev;
o.next=o.nextSibling=next;
var _47;
while((_47=_3.exec(res[2]))!=null){
if(_47.length>0){
var _42=_47[1].replace(_7,"");
var val=(_47[4]||_47[6]||"").replace(_8," ").replace(_9,">").replace(_a,"<").replace(_c,"'").replace(_b,"\"").replace(_d,"&");
if(_42.indexOf("xmlns")==0){
if(_42.indexOf(":")>0){
var ns=_42.split(":");
_37.namespaces[ns[1]]=val;
_37._nsPaths[val]=ns[1];
}else{
_37.namespaces[_e]=val;
_37._nsPaths[val]=_e;
}
}else{
var ln=_42;
var ns=_e;
if(_42.indexOf(":")>0){
var t=_42.split(":");
ln=t[1];
ns=t[0];
}
o.attributes.push({nodeType:_1.ATTRIBUTE,nodeName:_42,localName:ln,namespace:ns,nodeValue:val});
if(ln=="id"){
o.id=val;
}
}
}
}
_37._add(o);
if(obj){
obj.childNodes.push(o);
o.parentNode=obj;
if(res[2].charAt(res[2].length-1)!="/"){
obj=o;
}
}
var _41=res[3];
if(_41.length>0){
obj.childNodes.push(_createTextNode(_41));
}
}
}
}
}
}
for(var i=0;i<_37.childNodes.length;i++){
var e=_37.childNodes[i];
if(e.nodeType==_1.ELEMENT){
_37.documentElement=e;
break;
}
}
return _37;
};
})();
}
