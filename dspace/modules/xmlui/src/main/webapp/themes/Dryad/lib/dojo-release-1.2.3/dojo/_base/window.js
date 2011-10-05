/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.window"]){
dojo._hasResource["dojo._base.window"]=true;
dojo.provide("dojo._base.window");
dojo.doc=window["document"]||null;
dojo.body=function(){
return dojo.doc.body||dojo.doc.getElementsByTagName("body")[0];
};
dojo.setContext=function(_1,_2){
dojo.global=_1;
dojo.doc=_2;
};
dojo._fireCallback=function(_3,_4,_5){
if(_4&&dojo.isString(_3)){
_3=_4[_3];
}
return _3.apply(_4,_5||[]);
};
dojo.withGlobal=function(_6,_7,_8,_9){
var _a;
var _b=dojo.global;
var _c=dojo.doc;
try{
dojo.setContext(_6,_6.document);
_a=dojo._fireCallback(_7,_8,_9);
}
finally{
dojo.setContext(_b,_c);
}
return _a;
};
dojo.withDoc=function(_d,_e,_f,_10){
var _11;
var _12=dojo.doc;
try{
dojo.doc=_d;
_11=dojo._fireCallback(_e,_f,_10);
}
finally{
dojo.doc=_12;
}
return _11;
};
}
