/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.html"]){
dojo._hasResource["dojo.html"]=true;
dojo.provide("dojo.html");
dojo.require("dojo.parser");
(function(){
var _1=0;
dojo.html._secureForInnerHtml=function(_2){
return _2.replace(/(?:\s*<!DOCTYPE\s[^>]+>|<title[^>]*>[\s\S]*?<\/title>)/ig,"");
};
dojo.html._emptyNode=function(_3){
while(_3.firstChild){
dojo._destroyElement(_3.firstChild);
}
};
dojo.html._setNodeContent=function(_4,_5,_6){
if(_6){
dojo.html._emptyNode(_4);
}
if(typeof _5=="string"){
var _7="",_8="",_9=0,_a=_4.nodeName.toLowerCase();
switch(_a){
case "tr":
_7="<tr>";
_8="</tr>";
_9+=1;
case "tbody":
case "thead":
_7="<tbody>"+_7;
_8+="</tbody>";
_9+=1;
case "table":
_7="<table>"+_7;
_8+="</table>";
_9+=1;
break;
}
if(_9){
var n=_4.ownerDocument.createElement("div");
n.innerHTML=_7+_5+_8;
do{
n=n.firstChild;
}while(--_9);
dojo.forEach(n.childNodes,function(n){
_4.appendChild(n.cloneNode(true));
});
}else{
_4.innerHTML=_5;
}
}else{
if(_5.nodeType){
_4.appendChild(_5);
}else{
dojo.forEach(_5,function(n){
_4.appendChild(n.cloneNode(true));
});
}
}
return _4;
};
dojo.declare("dojo.html._ContentSetter",null,{node:"",content:"",id:"",cleanContent:false,extractContent:false,parseContent:false,constructor:function(_e,_f){
dojo.mixin(this,_e||{});
_f=this.node=dojo.byId(this.node||_f);
if(!this.id){
this.id=["Setter",(_f)?_f.id||_f.tagName:"",_1++].join("_");
}
if(!(this.node||_f)){
new Error(this.declaredClass+": no node provided to "+this.id);
}
},set:function(_10,_11){
if(undefined!==_10){
this.content=_10;
}
if(_11){
this._mixin(_11);
}
this.onBegin();
this.setContent();
this.onEnd();
return this.node;
},setContent:function(){
var _12=this.node;
if(!_12){
console.error("setContent given no node");
}
try{
_12=dojo.html._setNodeContent(_12,this.content);
}
catch(e){
var _13=this.onContentError(e);
try{
_12.innerHTML=_13;
}
catch(e){
console.error("Fatal "+this.declaredClass+".setContent could not change content due to "+e.message,e);
}
}
this.node=_12;
},empty:function(){
if(this.parseResults&&this.parseResults.length){
dojo.forEach(this.parseResults,function(w){
if(w.destroy){
w.destroy();
}
});
delete this.parseResults;
}
dojo.html._emptyNode(this.node);
},onBegin:function(){
var _15=this.content;
if(dojo.isString(_15)){
if(this.cleanContent){
_15=dojo.html._secureForInnerHtml(_15);
}
if(this.extractContent){
var _16=_15.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);
if(_16){
_15=_16[1];
}
}
}
this.empty();
this.content=_15;
return this.node;
},onEnd:function(){
if(this.parseContent){
this._parse();
}
return this.node;
},tearDown:function(){
delete this.parseResults;
delete this.node;
delete this.content;
},onContentError:function(err){
return "Error occured setting content: "+err;
},_mixin:function(_18){
var _19={},key;
for(key in _18){
if(key in _19){
continue;
}
this[key]=_18[key];
}
},_parse:function(){
var _1b=this.node;
try{
this.parseResults=dojo.parser.parse(_1b,true);
}
catch(e){
this._onError("Content",e,"Error parsing in _ContentSetter#"+this.id);
}
},_onError:function(_1c,err,_1e){
var _1f=this["on"+_1c+"Error"].call(this,err);
if(_1e){
console.error(_1e,err);
}else{
if(_1f){
dojo.html._setNodeContent(this.node,_1f,true);
}
}
}});
dojo.html.set=function(_20,_21,_22){
if(undefined==_21){
console.warn("dojo.html.set: no cont argument provided, using empty string");
_21="";
}
if(!_22){
return dojo.html._setNodeContent(_20,_21,true);
}else{
var op=new dojo.html._ContentSetter(dojo.mixin(_22,{content:_21,node:_20}));
return op.set();
}
};
})();
}
