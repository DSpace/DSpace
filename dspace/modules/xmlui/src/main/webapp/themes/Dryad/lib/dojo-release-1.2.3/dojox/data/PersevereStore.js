/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.PersevereStore"]){
dojo._hasResource["dojox.data.PersevereStore"]=true;
dojo.provide("dojox.data.PersevereStore");
dojo.require("dojox.data.JsonRestStore");
dojo.require("dojox.rpc.Client");
if(dojox.rpc.OfflineRest){
dojo.require("dojox.json.query");
}
dojox.json.ref._useRefs=true;
dojox.json.ref.serializeFunctions=true;
dojo.declare("dojox.data.PersevereStore",dojox.data.JsonRestStore,{_toJsonQuery:function(_1){
if(_1.query&&typeof _1.query=="object"){
var _2="[?(",_3=true;
for(var i in _1.query){
if(_1.query[i]!="*"){
_2+=(_3?"":"&")+"@["+dojo._escapeString(i)+"]="+dojox.json.ref.toJson(_1.query[i]);
_3=false;
}
}
if(!_3){
_2+=")]";
}else{
_2="";
}
_1.queryStr=_2.replace(/\\"|"/g,function(t){
return t=="\""?"'":t;
});
}else{
if(!_1.query||_1.query=="*"){
_1.query="";
}
}
var _6=_1.sort;
if(_6){
_1.queryStr=_1.queryStr||(typeof _1.query=="string"?_1.query:"");
_3=true;
for(i=0;i<_6.length;i++){
_1.queryStr+=(_3?"[":",")+(_6[i].descending?"\\":"/")+"@["+dojo._escapeString(_6[i].attribute)+"]";
_3=false;
}
if(!_3){
_1.queryStr+="]";
}
}
if(typeof _1.queryStr=="string"){
_1.queryStr=_1.queryStr.replace(/\\"|"/g,function(t){
return t=="\""?"'":t;
});
return _1.queryStr;
}
return _1.query;
},fetch:function(_8){
this._toJsonQuery(_8);
return this.inherited(arguments);
},isUpdateable:function(){
if(!dojox.json.query){
return this.inherited(arguments);
}
return true;
},matchesQuery:function(_9,_a){
if(!dojox.json.query){
return this.inherited(arguments);
}
_a._jsonQuery=_a._jsonQuery||dojox.json.query(this._toJsonQuery(_a));
return _a._jsonQuery([_9]).length;
},clientSideFetch:function(_b,_c){
if(!dojox.json.query){
return this.inherited(arguments);
}
_b._jsonQuery=_b._jsonQuery||dojox.json.query(this._toJsonQuery(_b));
return _b._jsonQuery(_c);
},querySuperSet:function(_d,_e){
if(!dojox.json.query){
return this.inherited(arguments);
}
if(!_d.query){
return _e.query;
}
return this.inherited(arguments);
}});
dojox.data.PersevereStore.getStores=function(_f,_10){
_f=(_f&&(_f.match(/\/$/)?_f:(_f+"/")))||"/";
if(_f.match(/^\w*:\/\//)){
dojo.require("dojox.io.xhrWindowNamePlugin");
dojox.io.xhrWindowNamePlugin(_f,dojox.io.xhrPlugins.fullHttpAdapter,true);
}
var _11=dojox.rpc.Rest(_f,true);
var _12=dojox.rpc._sync;
dojox.rpc._sync=_10;
var dfd=_11("root");
var _14;
dfd.addBoth(function(_15){
for(var i in _15){
if(typeof _15[i]=="object"){
_15[i]=new dojox.data.PersevereStore({target:new dojo._Url(_f,i)+"",schema:_15[i]});
}
}
return (_14=_15);
});
dojox.rpc._sync=_12;
return _10?_14:dfd;
};
dojox.data.PersevereStore.addProxy=function(){
dojo.require("dojox.io.xhrPlugins");
dojox.io.xhrPlugins.addProxy("/proxy/");
};
}
