/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.rpc.OfflineRest"]){
dojo._hasResource["dojox.rpc.OfflineRest"]=true;
dojo.provide("dojox.rpc.OfflineRest");
dojo.require("dojox.data.ClientFilter");
dojo.require("dojox.rpc.Rest");
dojo.require("dojox.storage");
(function(){
var _1=dojox.rpc.Rest;
var _2="dojox_rpc_OfflineRest";
var _3;
var _4=_1._index;
dojox.storage.manager.addOnLoad(function(){
_3=dojox.storage.manager.available;
for(var i in _4){
saveObject(_4[i],i);
}
});
var _6;
function getStorageKey(_7){
return _7.replace(/[^0-9A-Za-z_]/g,"_");
};
function saveObject(_8,id){
if(_3&&!_6&&(id||(_8&&_8.__id))){
dojox.storage.put(getStorageKey(id||_8.__id),typeof _8=="object"?dojox.json.ref.toJson(_8):_8,function(){
},_2);
}
};
function isNetworkError(_a){
return _a instanceof Error&&(_a.status==503||_a.status>12000||!_a.status);
};
function sendChanges(){
if(_3){
var _b=dojox.storage.get("dirty",_2);
if(_b){
for(var _c in _b){
commitDirty(_c,_b);
}
}
}
};
var _d;
function sync(){
_d.sendChanges();
_d.downloadChanges();
};
var _e=setInterval(sync,15000);
dojo.connect(document,"ononline",sync);
_d=dojox.rpc.OfflineRest={turnOffAutoSync:function(){
clearInterval(_e);
},sync:sync,sendChanges:sendChanges,downloadChanges:function(){
},addStore:function(_f,_10){
_d.stores.push(_f);
_f.fetch({queryOptions:{cache:true},query:_10,onComplete:function(_11,_12){
_f._localBaseResults=_11;
_f._localBaseFetch=_12;
}});
}};
_d.stores=[];
var _13=_1._get;
_1._get=function(_14,id){
try{
sendChanges();
if(window.navigator&&navigator.onLine===false){
throw new Error();
}
var dfd=_13(_14,id);
}
catch(e){
dfd=new dojo.Deferred();
dfd.errback(e);
}
var _17=dojox.rpc._sync;
dfd.addCallback(function(_18){
saveObject(_18,_14.servicePath+id);
return _18;
});
dfd.addErrback(function(_19){
if(_3){
if(isNetworkError(_19)){
var _1a={};
var _1b=function(id,_1d){
if(_1a[id]){
return _1d;
}
var _1e=dojo.fromJson(dojox.storage.get(getStorageKey(id),_2))||_1d;
_1a[id]=_1e;
for(var i in _1e){
var val=_1e[i];
if(val&&val.$ref){
_1e[i]=_1b(val.$ref,val);
}
}
if(_1e instanceof Array){
for(i=0;i<_1e.length;i++){
if(_1e[i]===undefined){
_1e.splice(i--,1);
}
}
}
return _1e;
};
_6=true;
var _21=_1b(_14.servicePath+id);
if(!_21){
return _19;
}
_6=false;
return _21;
}else{
return _19;
}
}else{
if(_17){
return new Error("Storage manager not loaded, can not continue");
}
dfd=new dojo.Deferred();
dfd.addCallback(arguments.callee);
dojox.storage.manager.addOnLoad(function(){
dfd.callback();
});
return dfd;
}
});
return dfd;
};
var _22=_1._change;
_1._change=function(_23,_24,id,_26){
if(!_3){
return _22.apply(this,arguments);
}
var _27=_24.servicePath+id;
if(_23=="delete"){
dojox.storage.remove(getStorageKey(_27),_2);
}else{
dojox.storage.put(getStorageKey(dojox.rpc.JsonRest._contentId),_26,function(){
},_2);
}
var _28=_24._store;
if(_28){
_28.updateResultSet(_28._localBaseResults,_28._localBaseFetch);
dojox.storage.put(getStorageKey(_24.servicePath+_28._localBaseFetch.query),dojox.json.ref.toJson(_28._localBaseResults),function(){
},_2);
}
var _29=dojox.storage.get("dirty",_2)||{};
if(_23=="put"||_23=="delete"){
var _2a=_27;
}else{
_2a=0;
for(var i in _29){
if(!isNaN(parseInt(i))){
_2a=i;
}
}
_2a++;
}
_29[_2a]={method:_23,id:_27,content:_26};
return commitDirty(_2a,_29);
};
function commitDirty(_2c,_2d){
var _2e=_2d[_2c];
var _2f=dojox.rpc.JsonRest.getServiceAndId(_2e.id);
var _30=_22(_2e.method,_2f.service,_2f.id,_2e.content);
_2d[_2c]=_2e;
dojox.storage.put("dirty",_2d,function(){
},_2);
_30.addBoth(function(_31){
if(isNetworkError(_31)){
return null;
}
var _32=dojox.storage.get("dirty",_2)||{};
delete _32[_2c];
dojox.storage.put("dirty",_32,function(){
},_2);
return _31;
});
return _30;
};
dojo.connect(_4,"onLoad",saveObject);
dojo.connect(_4,"onUpdate",saveObject);
})();
}
