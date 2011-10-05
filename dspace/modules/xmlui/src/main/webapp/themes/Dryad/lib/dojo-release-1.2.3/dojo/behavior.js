/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.behavior"]){
dojo._hasResource["dojo.behavior"]=true;
dojo.provide("dojo.behavior");
dojo.behavior=new function(){
function arrIn(_1,_2){
if(!_1[_2]){
_1[_2]=[];
}
return _1[_2];
};
var _3=0;
function forIn(_4,_5,_6){
var _7={};
for(var x in _4){
if(typeof _7[x]=="undefined"){
if(!_6){
_5(_4[x],x);
}else{
_6.call(_5,_4[x],x);
}
}
}
};
this._behaviors={};
this.add=function(_9){
var _a={};
forIn(_9,this,function(_b,_c){
var _d=arrIn(this._behaviors,_c);
if(typeof _d["id"]!="number"){
_d.id=_3++;
}
var _e=[];
_d.push(_e);
if((dojo.isString(_b))||(dojo.isFunction(_b))){
_b={found:_b};
}
forIn(_b,function(_f,_10){
arrIn(_e,_10).push(_f);
});
});
};
var _11=function(_12,_13,_14){
if(dojo.isString(_13)){
if(_14=="found"){
dojo.publish(_13,[_12]);
}else{
dojo.connect(_12,_14,function(){
dojo.publish(_13,arguments);
});
}
}else{
if(dojo.isFunction(_13)){
if(_14=="found"){
_13(_12);
}else{
dojo.connect(_12,_14,_13);
}
}
}
};
this.apply=function(){
forIn(this._behaviors,function(_15,id){
dojo.query(id).forEach(function(_17){
var _18=0;
var bid="_dj_behavior_"+_15.id;
if(typeof _17[bid]=="number"){
_18=_17[bid];
if(_18==(_15.length)){
return;
}
}
for(var x=_18,_1b;_1b=_15[x];x++){
forIn(_1b,function(_1c,_1d){
if(dojo.isArray(_1c)){
dojo.forEach(_1c,function(_1e){
_11(_17,_1e,_1d);
});
}
});
}
_17[bid]=_15.length;
});
});
};
};
dojo.addOnLoad(dojo.behavior,"apply");
}
