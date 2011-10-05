/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.rpc.JsonRest"]){
dojo._hasResource["dojox.rpc.JsonRest"]=true;
dojo.provide("dojox.rpc.JsonRest");
dojo.require("dojox.json.ref");
dojo.require("dojox.rpc.Rest");
(function(){
var _1=[];
var _2=dojox.rpc.Rest;
var _3=/(.*?)(#?(\.\w+)|(\[.+))+$/;
var jr=dojox.rpc.JsonRest={commit:function(_5){
var _6;
_5=_5||{};
var _7=[];
var _8={};
var _9=[];
for(var i=0;i<_1.length;i++){
var _b=_1[i];
var _c=_b.object;
var _d=_b.old;
var _e=false;
if(!(_5.service&&(_c||_d)&&(_c||_d).__id.indexOf(_5.service.servicePath))){
if(_c){
if(_d){
while(!(dojox.json&&dojox.json.ref&&dojox.json.ref._useRefs)&&_c.__id.match(_3)){
var _f=_c.__id.match(_3)[1];
_c=_2._index[_f];
}
if(!(_c.__id in _8)){
_8[_c.__id]=_c;
_7.push({method:"put",target:_c,content:_c});
}
}else{
_7.push({method:"post",target:{__id:jr.getServiceAndId(_c.__id).service.servicePath},content:_c});
}
}else{
if(_d){
_7.push({method:"delete",target:_d});
}
}
_9.push(_b);
_1.splice(i--,1);
}
}
var _10;
var _11=dojo.xhr;
_6=_7.length;
var _12;
dojo.xhr=function(_13,_14){
_14.headers=_14.headers||{};
_14.headers["X-Transaction"]=_7.length-1==i?"commit":"open";
if(_12){
_14.headers["Content-Location"]=_12;
}
return _11.apply(dojo,arguments);
};
for(i=0;i<_7.length;i++){
var _15=_7[i];
dojox.rpc.JsonRest._contentId=_15.content&&_15.content.__id;
var _16=_15.method=="post";
_12=_16&&dojox.rpc.JsonRest._contentId;
var _17=jr.getServiceAndId(_15.target.__id);
var _18=_17.service;
var dfd=_15.deferred=_18[_15.method](_17.id.replace(/#/,""),dojox.json.ref.toJson(_15.content,false,_18.servicePath,true));
(function(_1a,dfd,_1c){
dfd.addCallback(function(_1d){
try{
var _1e=dfd.ioArgs.xhr.getResponseHeader("Location");
if(_1e){
var _1f=_1e.match(/(^\w+:\/\/)/)&&_1e.indexOf(_1c.servicePath);
_1e=_1f>0?_1e.substring(_1f):(_1c.servicePath+_1e).replace(/^(.*\/)?(\w+:\/\/)|[^\/\.]+\/\.\.\/|^.*\/(\/)/,"$2$3");
_1a.__id=_1e;
_2._index[_1e]=_1a;
}
_1d=_1d&&dojox.json.ref.resolveJson(_1d,{index:_2._index,idPrefix:_1c.servicePath,idAttribute:jr.getIdAttribute(_1c),schemas:jr.schemas,loader:jr._loader,assignAbsoluteIds:true});
}
catch(e){
}
if(!(--_6)){
if(_5.onComplete){
_5.onComplete.call(_5.scope);
}
}
return _1d;
});
})(_16&&_15.content,dfd,_18);
dfd.addErrback(function(_20){
_6=-1;
var _21=_1;
dirtyObject=_9;
numDirty=0;
jr.revert();
_1=_21;
if(_5.onError){
_5.onError();
}
return _20;
});
}
dojo.xhr=_11;
return _7;
},getDirtyObjects:function(){
return _1;
},revert:function(){
while(_1.length>0){
var d=_1.pop();
if(d.object&&d.old){
for(var i in d.old){
if(d.old.hasOwnProperty(i)){
d.object[i]=d.old[i];
}
}
for(i in d.object){
if(!d.old.hasOwnProperty(i)){
delete d.object[i];
}
}
}
}
},changing:function(_24,_25){
if(!_24.__id){
return;
}
for(var i=0;i<_1.length;i++){
if(_24==_1[i].object){
if(_25){
_1[i].object=false;
}
return;
}
}
var old=_24 instanceof Array?[]:{};
for(i in _24){
if(_24.hasOwnProperty(i)){
old[i]=_24[i];
}
}
_1.push({object:!_25&&_24,old:old});
},deleteObject:function(_28){
this.changing(_28,true);
},getConstructor:function(_29,_2a){
if(typeof _29=="string"){
var _2b=_29;
_29=new dojox.rpc.Rest(_29,true);
this.registerService(_29,_2b,_2a);
}
if(_29._constructor){
return _29._constructor;
}
_29._constructor=function(_2c){
if(_2c){
dojo.mixin(this,_2c);
}
var _2d=jr.getIdAttribute(_29);
_2._index[this.__id=this.__clientId=_29.servicePath+(this[_2d]||(this[_2d]=Math.random().toString(16).substring(2,14)+Math.random().toString(16).substring(2,14)))]=this;
_1.push({object:this});
};
return dojo.mixin(_29._constructor,_29._schema,{load:_29});
},fetch:function(_2e){
var _2f=jr.getServiceAndId(_2e);
return this.byId(_2f.service,_2f.id);
},getIdAttribute:function(_30){
var _31=_30._schema;
var _32;
if(_31){
if(!(_32=_31._idAttr)){
for(var i in _31.properties){
if(_31.properties[i].identity){
_31._idAttr=_32=i;
}
}
}
}
return _32||"id";
},getServiceAndId:function(_34){
var _35=_34.match(/^(.*\/)([^\/]*)$/);
var svc=jr.services[_35[1]]||new dojox.rpc.Rest(_35[1],true);
return {service:svc,id:_35[2]};
},services:{},schemas:{},registerService:function(_37,_38,_39){
_38=_38||_37.servicePath;
_38=_37.servicePath=_38.match(/\/$/)?_38:(_38+"/");
_37._schema=jr.schemas[_38]=_39||_37._schema||{};
jr.services[_38]=_37;
},byId:function(_3a,id){
var _3c,_3d=_2._index[(_3a.servicePath||"")+id];
if(_3d&&!_3d._loadObject){
_3c=new dojo.Deferred();
_3c.callback(_3d);
return _3c;
}
return this.query(_3a,id);
},query:function(_3e,id,_40){
var _41=_3e(id,_40);
_41.addCallback(function(_42){
if(_42.nodeType&&_42.cloneNode){
return _42;
}
return dojox.json.ref.resolveJson(_42,{defaultId:typeof id!="string"||(_40&&(_40.start||_40.count))?undefined:id,index:_2._index,idPrefix:_3e.servicePath,idAttribute:jr.getIdAttribute(_3e),schemas:jr.schemas,loader:jr._loader,assignAbsoluteIds:true});
});
return _41;
},_loader:function(_43){
var _44=jr.getServiceAndId(this.__id);
var _45=this;
jr.query(_44.service,_44.id).addBoth(function(_46){
if(_46==_45){
delete _46.$ref;
delete _46._loadObject;
}else{
_45._loadObject=function(_47){
_47(_46);
};
}
_43(_46);
});
},isDirty:function(_48){
for(var i=0,l=_1.length;i<l;i++){
if(_1[i].object==_48){
return true;
}
}
return false;
}};
})();
}
