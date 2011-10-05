/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.uuid.generateRandomUuid"]){
dojo._hasResource["dojox.uuid.generateRandomUuid"]=true;
dojo.provide("dojox.uuid.generateRandomUuid");
dojox.uuid.generateRandomUuid=function(){
var _1=16;
function _generateRandomEightCharacterHexString(){
var _2=Math.floor((Math.random()%1)*Math.pow(2,32));
var _3=_2.toString(_1);
while(_3.length<8){
_3="0"+_3;
}
return _3;
};
var _4="-";
var _5="4";
var _6="8";
var a=_generateRandomEightCharacterHexString();
var b=_generateRandomEightCharacterHexString();
b=b.substring(0,4)+_4+_5+b.substring(5,8);
var c=_generateRandomEightCharacterHexString();
c=_6+c.substring(1,4)+_4+c.substring(4,8);
var d=_generateRandomEightCharacterHexString();
var _b=a+_4+b+_4+c+d;
_b=_b.toLowerCase();
return _b;
};
}
