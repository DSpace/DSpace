/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.uuid.generateTimeBasedUuid"]){
dojo._hasResource["dojox.uuid.generateTimeBasedUuid"]=true;
dojo.provide("dojox.uuid.generateTimeBasedUuid");
dojox.uuid.generateTimeBasedUuid=function(_1){
var _2=dojox.uuid.generateTimeBasedUuid._generator.generateUuidString(_1);
return _2;
};
dojox.uuid.generateTimeBasedUuid.isValidNode=function(_3){
var _4=16;
var _5=parseInt(_3,_4);
var _6=dojo.isString(_3)&&_3.length==12&&isFinite(_5);
return _6;
};
dojox.uuid.generateTimeBasedUuid.setNode=function(_7){
dojox.uuid.assert((_7===null)||this.isValidNode(_7));
this._uniformNode=_7;
};
dojox.uuid.generateTimeBasedUuid.getNode=function(){
return this._uniformNode;
};
dojox.uuid.generateTimeBasedUuid._generator=new function(){
this.GREGORIAN_CHANGE_OFFSET_IN_HOURS=3394248;
var _8=null;
var _9=null;
var _a=null;
var _b=0;
var _c=null;
var _d=null;
var _e=16;
function _carry(_f){
_f[2]+=_f[3]>>>16;
_f[3]&=65535;
_f[1]+=_f[2]>>>16;
_f[2]&=65535;
_f[0]+=_f[1]>>>16;
_f[1]&=65535;
dojox.uuid.assert((_f[0]>>>16)===0);
};
function _get64bitArrayFromFloat(x){
var _11=new Array(0,0,0,0);
_11[3]=x%65536;
x-=_11[3];
x/=65536;
_11[2]=x%65536;
x-=_11[2];
x/=65536;
_11[1]=x%65536;
x-=_11[1];
x/=65536;
_11[0]=x;
return _11;
};
function _addTwo64bitArrays(_12,_13){
dojox.uuid.assert(dojo.isArray(_12));
dojox.uuid.assert(dojo.isArray(_13));
dojox.uuid.assert(_12.length==4);
dojox.uuid.assert(_13.length==4);
var _14=new Array(0,0,0,0);
_14[3]=_12[3]+_13[3];
_14[2]=_12[2]+_13[2];
_14[1]=_12[1]+_13[1];
_14[0]=_12[0]+_13[0];
_carry(_14);
return _14;
};
function _multiplyTwo64bitArrays(_15,_16){
dojox.uuid.assert(dojo.isArray(_15));
dojox.uuid.assert(dojo.isArray(_16));
dojox.uuid.assert(_15.length==4);
dojox.uuid.assert(_16.length==4);
var _17=false;
if(_15[0]*_16[0]!==0){
_17=true;
}
if(_15[0]*_16[1]!==0){
_17=true;
}
if(_15[0]*_16[2]!==0){
_17=true;
}
if(_15[1]*_16[0]!==0){
_17=true;
}
if(_15[1]*_16[1]!==0){
_17=true;
}
if(_15[2]*_16[0]!==0){
_17=true;
}
dojox.uuid.assert(!_17);
var _18=new Array(0,0,0,0);
_18[0]+=_15[0]*_16[3];
_carry(_18);
_18[0]+=_15[1]*_16[2];
_carry(_18);
_18[0]+=_15[2]*_16[1];
_carry(_18);
_18[0]+=_15[3]*_16[0];
_carry(_18);
_18[1]+=_15[1]*_16[3];
_carry(_18);
_18[1]+=_15[2]*_16[2];
_carry(_18);
_18[1]+=_15[3]*_16[1];
_carry(_18);
_18[2]+=_15[2]*_16[3];
_carry(_18);
_18[2]+=_15[3]*_16[2];
_carry(_18);
_18[3]+=_15[3]*_16[3];
_carry(_18);
return _18;
};
function _padWithLeadingZeros(_19,_1a){
while(_19.length<_1a){
_19="0"+_19;
}
return _19;
};
function _generateRandomEightCharacterHexString(){
var _1b=Math.floor((Math.random()%1)*Math.pow(2,32));
var _1c=_1b.toString(_e);
while(_1c.length<8){
_1c="0"+_1c;
}
return _1c;
};
this.generateUuidString=function(_1d){
if(_1d){
dojox.uuid.assert(dojox.uuid.generateTimeBasedUuid.isValidNode(_1d));
}else{
if(dojox.uuid.generateTimeBasedUuid._uniformNode){
_1d=dojox.uuid.generateTimeBasedUuid._uniformNode;
}else{
if(!_8){
var _1e=32768;
var _1f=Math.floor((Math.random()%1)*Math.pow(2,15));
var _20=(_1e|_1f).toString(_e);
_8=_20+_generateRandomEightCharacterHexString();
}
_1d=_8;
}
}
if(!_9){
var _21=32768;
var _22=Math.floor((Math.random()%1)*Math.pow(2,14));
_9=(_21|_22).toString(_e);
}
var now=new Date();
var _24=now.valueOf();
var _25=_get64bitArrayFromFloat(_24);
if(!_c){
var _26=_get64bitArrayFromFloat(60*60);
var _27=_get64bitArrayFromFloat(dojox.uuid.generateTimeBasedUuid._generator.GREGORIAN_CHANGE_OFFSET_IN_HOURS);
var _28=_multiplyTwo64bitArrays(_27,_26);
var _29=_get64bitArrayFromFloat(1000);
_c=_multiplyTwo64bitArrays(_28,_29);
_d=_get64bitArrayFromFloat(10000);
}
var _2a=_25;
var _2b=_addTwo64bitArrays(_c,_2a);
var _2c=_multiplyTwo64bitArrays(_2b,_d);
if(now.valueOf()==_a){
_2c[3]+=_b;
_carry(_2c);
_b+=1;
if(_b==10000){
while(now.valueOf()==_a){
now=new Date();
}
}
}else{
_a=now.valueOf();
_b=1;
}
var _2d=_2c[2].toString(_e);
var _2e=_2c[3].toString(_e);
var _2f=_padWithLeadingZeros(_2d,4)+_padWithLeadingZeros(_2e,4);
var _30=_2c[1].toString(_e);
_30=_padWithLeadingZeros(_30,4);
var _31=_2c[0].toString(_e);
_31=_padWithLeadingZeros(_31,3);
var _32="-";
var _33="1";
var _34=_2f+_32+_30+_32+_33+_31+_32+_9+_32+_1d;
_34=_34.toLowerCase();
return _34;
};
}();
}
