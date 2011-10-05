/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.validate.isbn"]){
dojo._hasResource["dojox.validate.isbn"]=true;
dojo.provide("dojox.validate.isbn");
dojox.validate.isValidIsbn=function(_1){
var _2,_3,_4;
if(typeof _1!="string"){
_1=String(_1);
}
_1=_1.replace(/[- ]/g,"");
_2=_1.length;
_3=0;
if(_2==10){
_4=10;
for(var i=0;i<9;i++){
_3+=parseInt(_1.charAt(i))*_4;
_4--;
}
var t=_1.charAt(9).toUpperCase();
_3+=t=="X"?10:parseInt(t);
return _3%11==0;
}else{
if(_2==13){
_4=-1;
for(var i=0;i<_2;i++){
_3+=parseInt(_1.charAt(i))*(2+_4);
_4*=-1;
}
return _3%10==0;
}else{
return false;
}
}
};
}
