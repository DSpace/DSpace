/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.dtl.contrib.html"]){
dojo._hasResource["dojox.dtl.contrib.html"]=true;
dojo.provide("dojox.dtl.contrib.html");
dojo.require("dojox.dtl.html");
(function(){
var dd=dojox.dtl;
var _2=dd.contrib.html;
_2.StyleNode=dojo.extend(function(_3){
this.contents={};
this._styles=_3;
for(var _4 in _3){
this.contents[_4]=new dd.Template(_3[_4]);
}
},{render:function(_5,_6){
for(var _7 in this.contents){
dojo.style(_6.getParent(),_7,this.contents[_7].render(_5));
}
return _6;
},unrender:function(_8,_9){
return _9;
},clone:function(_a){
return new this.constructor(this._styles);
}});
_2.BufferNode=dojo.extend(function(_b,_c){
this.nodelist=_b;
this.options=_c;
},{_swap:function(_d,_e){
if(!this.swapped&&this.parent.parentNode){
if(_d=="node"){
if((_e.nodeType==3&&!this.options.text)||(_e.nodeType==1&&!this.options.node)){
return;
}
}else{
if(_d=="class"){
if(_d!="class"){
return;
}
}
}
this.onAddNode&&dojo.disconnect(this.onAddNode);
this.onRemoveNode&&dojo.disconnect(this.onRemoveNode);
this.onChangeAttribute&&dojo.disconnect(this.onChangeAttribute);
this.onChangeData&&dojo.disconnect(this.onChangeData);
this.swapped=this.parent.cloneNode(true);
this.parent.parentNode.replaceChild(this.swapped,this.parent);
}
},render:function(_f,_10){
this.parent=_10.getParent();
if(this.options.node){
this.onAddNode=dojo.connect(_10,"onAddNode",dojo.hitch(this,"_swap","node"));
this.onRemoveNode=dojo.connect(_10,"onRemoveNode",dojo.hitch(this,"_swap","node"));
}
if(this.options.text){
this.onChangeData=dojo.connect(_10,"onChangeData",dojo.hitch(this,"_swap","node"));
}
if(this.options["class"]){
this.onChangeAttribute=dojo.connect(_10,"onChangeAttribute",dojo.hitch(this,"_swap","class"));
}
_10=this.nodelist.render(_f,_10);
if(this.swapped){
this.swapped.parentNode.replaceChild(this.parent,this.swapped);
dojo._destroyElement(this.swapped);
}else{
this.onAddNode&&dojo.disconnect(this.onAddNode);
this.onRemoveNode&&dojo.disconnect(this.onRemoveNode);
this.onChangeAttribute&&dojo.disconnect(this.onChangeAttribute);
this.onChangeData&&dojo.disconnect(this.onChangeData);
}
delete this.parent;
delete this.swapped;
return _10;
},unrender:function(_11,_12){
return this.nodelist.unrender(_11,_12);
},clone:function(_13){
return new this.constructor(this.nodelist.clone(_13),this.options);
}});
dojo.mixin(_2,{buffer:function(_14,_15){
var _16=_15.contents.split().slice(1);
var _17={};
var _18=false;
for(var i=_16.length;i--;){
_18=true;
_17[_16[i]]=true;
}
if(!_18){
_17.node=true;
}
var _1a=_14.parse(["endbuffer"]);
_14.next_token();
return new _2.BufferNode(_1a,_17);
},html:function(_1b,_1c){
dojo.deprecated("{% html someVariable %}","Use {{ someVariable|safe }} instead");
return _1b.create_variable_node(_1c.contents.slice(5)+"|safe");
},tstyle:function(_1d,_1e){
var _1f={};
_1e=_1e.contents.replace(/^tstyle\s+/,"");
var _20=_1e.split(/\s*;\s*/g);
for(var i=0,_22;_22=_20[i];i++){
var _23=_22.split(/\s*:\s*/g);
var key=_23[0];
var _25=_23[1];
if(_25.indexOf("{{")==0){
_1f[key]=_25;
}
}
return new _2.StyleNode(_1f);
}});
dd.register.tags("dojox.dtl.contrib",{"html":["html","attr:tstyle","buffer"]});
})();
}
