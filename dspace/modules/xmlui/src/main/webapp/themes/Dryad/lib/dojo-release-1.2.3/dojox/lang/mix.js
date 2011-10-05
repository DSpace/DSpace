/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.lang.mix"]){
dojo._hasResource["dojox.lang.mix"]=true;
dojo.provide("dojox.lang.mix");
(function(){
var _1={},_2=dojox.lang.mix;
_2.processProps=function(_3,_4,_5){
if(_3){
var t,i,j,l;
if(_5){
if(dojo.isArray(_5)){
for(j=0,l=_5.length;j<l;++j){
delete _3[_5[j]];
}
}else{
for(var i in _5){
if(_5.hasOwnProperty(i)){
delete _3[i];
}
}
}
}
if(_4){
for(i in _4){
if(_4.hasOwnProperty(i)&&_3.hasOwnProperty(i)){
t=_3[i];
delete _3[i];
_3[_4[i]]=t;
}
}
}
}
return _3;
};
var _a=function(_b,_c,_d){
this.value=_b;
this.rename=_c||_1;
if(_d&&dojo.isArray(_d)){
var p={};
for(j=0,l=_d.length;j<l;++j){
p[_d[j]]=1;
}
this.skip=p;
}else{
this.skip=_d||_1;
}
};
dojo.extend(_a,{filter:function(_f){
if(this.skip.hasOwnProperty(_f)){
return "";
}
return this.rename.hasOwnProperty(_f)?this.rename[_f]:_f;
}});
var _10=function(_11){
this.value=_11;
};
dojo.extend(_10,{process:function(_12,_13){
if(this.value instanceof _10){
this.value.process(_12,_13);
}else{
_12[_13]=this.value;
}
}});
_2.mixer=function(_14,_15){
var dcr=null,flt=null,i,l=arguments.length,_1a,_1b,_1c;
for(i=1,l;i<l;++i){
_15=arguments[i];
if(_15 instanceof _10){
dcr=_15;
_15=dcr.value;
}
if(_15 instanceof _a){
flt=_15;
_15=flt.value;
}
for(_1a in _15){
if(_15.hasOwnProperty(_1a)){
_1b=_15[_1a];
_1c=flt?flt.filter(_1a):_1a;
if(!_1c){
continue;
}
if(_1b instanceof _10){
_1b.process(_14,_1c);
}else{
if(dcr){
dcr.value=_1b;
dcr.process(_14,_1c);
}else{
_14[_1c]=_1b;
}
}
}
}
if(flt){
_15=flt;
flt=null;
}
if(dcr){
dcr.value=_15;
dcr=null;
}
}
return _14;
};
_2.makeFilter=function(_1d){
dojo.declare("dojox.__temp__",_a,_1d||_1);
var t=dojox.__temp__;
delete dojox.__temp__;
return t;
};
_2.createFilter=function(_1f){
var _20=_2.makeFilter(_1f&&{filter:_1f}||_1);
return function(_21){
return new _20(_21);
};
};
_2.makeDecorator=function(_22){
dojo.declare("dojox.__temp__",_10,_22||_1);
var t=dojox.__temp__;
delete dojox.__temp__;
return t;
};
_2.createDecorator=function(_24){
var _25=_2.makeDecorator(_24&&{process:_24}||_1);
return function(_26){
return new _25(_26);
};
};
var _27=_2.makeDecorator({constructor:function(_28,_29){
this.value=_29;
this.context=_28;
},process:function(_2a,_2b){
var old=_2a[_2b],_2d=this.value,_2e=this.context;
_2a[_2b]=function(){
return _2d.call(_2e,this,arguments,_2b,old);
};
}});
dojo.mixin(_2,{filter:_2.createFilter(),augment:_2.createDecorator(function(_2f,_30){
if(!(_30 in _2f)){
_2f[_30]=this.value;
}
}),override:_2.createDecorator(function(_31,_32){
if(_32 in _31){
_31[_32]=this.value;
}
}),replaceContext:function(_33,_34){
return new _27(_33,_34);
},shuffle:_2.createDecorator(function(_35,_36){
if(_36 in _35){
var old=_35[_36],_38=this.value;
_35[_36]=function(){
return old.apply(this,_38.apply(this,arguments));
};
}
}),chainBefore:_2.createDecorator(function(_39,_3a){
if(_3a in _39){
var old=_39[_3a],_3c=this.value;
_39[_3a]=function(){
_3c.apply(this,arguments);
return old.apply(this,arguments);
};
}else{
_39[_3a]=this.value;
}
}),chainAfter:_2.createDecorator(function(_3d,_3e){
if(_3e in _3d){
var old=_3d[_3e],_40=this.value;
_3d[_3e]=function(){
old.apply(this,arguments);
return _40.apply(this,arguments);
};
}else{
_3d[_3e]=this.value;
}
}),before:_2.createDecorator(function(_41,_42){
var old=_41[_42],_44=this.value;
_41[_42]=old?function(){
_44.apply(this,arguments);
return old.apply(this,arguments);
}:function(){
_44.apply(this,arguments);
};
}),around:_2.createDecorator(function(_45,_46){
var old=_45[_46],_48=this.value;
_45[_46]=old?function(){
return _48.call(this,old,arguments);
}:function(){
return _48.call(this,null,arguments);
};
}),afterReturning:_2.createDecorator(function(_49,_4a){
var old=_49[_4a],_4c=this.value;
_49[_4a]=old?function(){
var ret=old.apply(this,arguments);
_4c.call(this,ret);
return ret;
}:function(){
_4c.call(this);
};
}),afterThrowing:_2.createDecorator(function(_4e,_4f){
var old=_4e[_4f],_51=this.value;
if(old){
_4e[_4f]=function(){
var ret;
try{
ret=old.apply(this,arguments);
}
catch(e){
_51.call(this,e);
throw e;
}
return ret;
};
}
}),after:_2.createDecorator(function(_53,_54){
var old=_53[_54],_56=this.value;
_53[_54]=old?function(){
var ret;
try{
ret=old.apply(this,arguments);
}
finally{
_56.call(this);
}
return ret;
}:function(){
_56.call(this);
};
})});
})();
}
