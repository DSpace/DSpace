/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.dtl.contrib.data"]){
dojo._hasResource["dojox.dtl.contrib.data"]=true;
dojo.provide("dojox.dtl.contrib.data");
dojo.require("dojox.dtl._base");
(function(){
var dd=dojox.dtl;
var _2=dd.contrib.data;
var _3=true;
_2._BoundItem=dojo.extend(function(_4,_5){
this.item=_4;
this.store=_5;
},{get:function(_6){
var _7=this.store;
var _8=this.item;
if(_6=="getLabel"){
return _7.getLabel(_8);
}else{
if(_6=="getAttributes"){
return _7.getAttributes(_8);
}else{
if(_6=="getIdentity"){
if(_7.getIdentity){
return _7.getIdentity(_8);
}
return "Store has no identity API";
}else{
if(!_7.hasAttribute(_8,_6)){
if(_6.slice(-1)=="s"){
if(_3){
_3=false;
dojo.deprecated("You no longer need an extra s to call getValues, it can be figured out automatically");
}
_6=_6.slice(0,-1);
}
if(!_7.hasAttribute(_8,_6)){
return;
}
}
var _9=_7.getValues(_8,_6);
if(!_9){
return;
}
if(!dojo.isArray(_9)){
return new _2._BoundItem(_9,_7);
}
_9=dojo.map(_9,function(_a){
if(dojo.isObject(_a)&&_7.isItem(_a)){
return new _2._BoundItem(_a,_7);
}
return _a;
});
_9.get=_2._get;
return _9;
}
}
}
}});
_2.BindDataNode=dojo.extend(function(_b,_c,_d){
this.items=new dd._Filter(_b);
this.store=new dd._Filter(_c);
this.alias=_d;
},{render:function(_e,_f){
var _10=this.items.resolve(_e);
var _11=this.store.resolve(_e);
if(!_11){
throw new Error("data_bind didn't receive a store");
}
var _12=[];
if(_10){
for(var i=0,_14;_14=_10[i];i++){
_12.push(new _2._BoundItem(_14,_11));
}
}
_e[this.alias]=_12;
return _f;
},unrender:function(_15,_16){
return _16;
},clone:function(){
return this;
}});
dojo.mixin(_2,{_get:function(key){
if(this.length){
return this[0].get(key);
}
},bind_data:function(_18,_19){
var _1a=_19.contents.split();
if(_1a[2]!="to"||_1a[4]!="as"||!_1a[5]){
throw new Error("data_bind expects the format: 'data_bind items to store as varName'");
}
return new _2.BindDataNode(_1a[1],_1a[3],_1a[5]);
}});
dd.register.tags("dojox.dtl.contrib",{"data":["bind_data"]});
})();
}
