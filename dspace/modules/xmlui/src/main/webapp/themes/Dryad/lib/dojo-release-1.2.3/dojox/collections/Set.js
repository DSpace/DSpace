/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.collections.Set"]){
dojo._hasResource["dojox.collections.Set"]=true;
dojo.provide("dojox.collections.Set");
dojo.require("dojox.collections.ArrayList");
(function(){
var _1=dojox.collections;
_1.Set=new (function(){
function conv(_2){
if(_2.constructor==Array){
return new dojox.collections.ArrayList(_2);
}
return _2;
};
this.union=function(_3,_4){
_3=conv(_3);
_4=conv(_4);
var _5=new dojox.collections.ArrayList(_3.toArray());
var e=_4.getIterator();
while(!e.atEnd()){
var _7=e.get();
if(!_5.contains(_7)){
_5.add(_7);
}
}
return _5;
};
this.intersection=function(_8,_9){
_8=conv(_8);
_9=conv(_9);
var _a=new dojox.collections.ArrayList();
var e=_9.getIterator();
while(!e.atEnd()){
var _c=e.get();
if(_8.contains(_c)){
_a.add(_c);
}
}
return _a;
};
this.difference=function(_d,_e){
_d=conv(_d);
_e=conv(_e);
var _f=new dojox.collections.ArrayList();
var e=_d.getIterator();
while(!e.atEnd()){
var _11=e.get();
if(!_e.contains(_11)){
_f.add(_11);
}
}
return _f;
};
this.isSubSet=function(_12,_13){
_12=conv(_12);
_13=conv(_13);
var e=_12.getIterator();
while(!e.atEnd()){
if(!_13.contains(e.get())){
return false;
}
}
return true;
};
this.isSuperSet=function(_15,_16){
_15=conv(_15);
_16=conv(_16);
var e=_16.getIterator();
while(!e.atEnd()){
if(!_15.contains(e.get())){
return false;
}
}
return true;
};
})();
})();
}
