/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.dtl.contrib.dijit"]){
dojo._hasResource["dojox.dtl.contrib.dijit"]=true;
dojo.provide("dojox.dtl.contrib.dijit");
dojo.require("dojox.dtl.html");
dojo.require("dojo.parser");
(function(){
var dd=dojox.dtl;
var _2=dd.contrib.dijit;
_2.AttachNode=dojo.extend(function(_3,_4){
this._keys=_3;
this._object=_4;
},{render:function(_5,_6){
if(!this._rendered){
this._rendered=true;
for(var i=0,_8;_8=this._keys[i];i++){
_5.getThis()[_8]=this._object||_6.getParent();
}
}
return _6;
},unrender:function(_9,_a){
if(this._rendered){
this._rendered=false;
for(var i=0,_c;_c=this._keys[i];i++){
if(_9.getThis()[_c]===(this._object||_a.getParent())){
delete _9.getThis()[_c];
}
}
}
return _a;
},clone:function(_d){
return new this.constructor(this._keys,this._object);
}});
_2.EventNode=dojo.extend(function(_e,_f){
this._command=_e;
var _10,_11=_e.split(/\s*,\s*/);
var _12=dojo.trim;
var _13=[];
var fns=[];
while(_10=_11.pop()){
if(_10){
var fn=null;
if(_10.indexOf(":")!=-1){
var _16=_10.split(":");
_10=_12(_16[0]);
fn=_12(_16.slice(1).join(":"));
}else{
_10=_12(_10);
}
if(!fn){
fn=_10;
}
_13.push(_10);
fns.push(fn);
}
}
this._types=_13;
this._fns=fns;
this._object=_f;
this._rendered=[];
},{_clear:false,render:function(_17,_18){
for(var i=0,_1a;_1a=this._types[i];i++){
if(!this._clear&&!this._object){
_18.getParent()[_1a]=null;
}
var fn=this._fns[i];
var _1c;
if(fn.indexOf(" ")!=-1){
if(this._rendered[i]){
dojo.disconnect(this._rendered[i]);
this._rendered[i]=false;
}
_1c=dojo.map(fn.split(" ").slice(1),function(_1d){
return new dd._Filter(_1d).resolve(_17);
});
fn=fn.split(" ",2)[0];
}
if(!this._rendered[i]){
if(!this._object){
this._rendered[i]=_18.addEvent(_17,_1a,fn,_1c);
}else{
this._rendered[i]=dojo.connect(this._object,_1a,_17.getThis(),fn);
}
}
}
this._clear=true;
return _18;
},unrender:function(_1e,_1f){
while(this._rendered.length){
dojo.disconnect(this._rendered.pop());
}
return _1f;
},clone:function(){
return new this.constructor(this._command,this._object);
}});
function cloneNode(n1){
var n2=n1.cloneNode(true);
if(dojo.isIE){
dojo.query("script",n2).forEach("item.text = this[index].text;",dojo.query("script",n1));
}
return n2;
};
_2.DojoTypeNode=dojo.extend(function(_22,_23){
this._node=_22;
this._parsed=_23;
var _24=_22.getAttribute("dojoAttachEvent");
if(_24){
this._events=new _2.EventNode(dojo.trim(_24));
}
var _25=_22.getAttribute("dojoAttachPoint");
if(_25){
this._attach=new _2.AttachNode(dojo.trim(_25).split(/\s*,\s*/));
}
if(!_23){
this._dijit=dojo.parser.instantiate([cloneNode(_22)])[0];
}else{
_22=cloneNode(_22);
var old=_2.widgetsInTemplate;
_2.widgetsInTemplate=false;
this._template=new dd.HtmlTemplate(_22);
_2.widgetsInTemplate=old;
}
},{render:function(_27,_28){
if(this._parsed){
var _29=new dd.HtmlBuffer();
this._template.render(_27,_29);
var _2a=cloneNode(_29.getRootNode());
var div=document.createElement("div");
div.appendChild(_2a);
var _2c=div.innerHTML;
div.removeChild(_2a);
if(_2c!=this._rendered){
this._rendered=_2c;
if(this._dijit){
this._dijit.destroyRecursive();
}
this._dijit=dojo.parser.instantiate([_2a])[0];
}
}
var _2d=this._dijit.domNode;
if(this._events){
this._events._object=this._dijit;
this._events.render(_27,_28);
}
if(this._attach){
this._attach._object=this._dijit;
this._attach.render(_27,_28);
}
return _28.concat(_2d);
},unrender:function(_2e,_2f){
return _2f.remove(this._dijit.domNode);
},clone:function(){
return new this.constructor(this._node,this._parsed);
}});
dojo.mixin(_2,{widgetsInTemplate:true,dojoAttachPoint:function(_30,_31){
return new _2.AttachNode(_31.contents.slice(16).split(/\s*,\s*/));
},dojoAttachEvent:function(_32,_33){
return new _2.EventNode(_33.contents.slice(16));
},dojoType:function(_34,_35){
if(_2.widgetsInTemplate){
var _36=_34.swallowNode();
var _37=false;
if(_35.contents.slice(-7)==" parsed"){
_37=true;
_36.setAttribute("dojoType",_35.contents.slice(0,-7));
}
return new _2.DojoTypeNode(_36,_37);
}
return dd._noOpNode;
},on:function(_38,_39){
var _3a=_39.contents.split();
return new _2.EventNode(_3a[0]+":"+_3a.slice(1).join(" "));
}});
dd.register.tags("dojox.dtl.contrib",{"dijit":["attr:dojoType","attr:dojoAttachPoint",["attr:attach","dojoAttachPoint"],"attr:dojoAttachEvent",[/(attr:)?on(click|key(up))/i,"on"]]});
})();
}
