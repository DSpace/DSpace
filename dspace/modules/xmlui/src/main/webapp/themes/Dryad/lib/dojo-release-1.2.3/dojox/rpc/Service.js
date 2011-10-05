/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.rpc.Service"]){
dojo._hasResource["dojox.rpc.Service"]=true;
dojo.provide("dojox.rpc.Service");
dojo.require("dojo.AdapterRegistry");
dojo.declare("dojox.rpc.Service",null,{constructor:function(_1,_2){
var _3;
var _4=this;
function processSmd(_5){
_5._baseUrl=new dojo._Url(location.href,_3||".")+"";
_4._smd=_5;
for(var _6 in _4._smd.services){
var _7=_6.split(".");
var _8=_4;
for(var i=0;i<_7.length-1;i++){
_8=_8[_7[i]]||(_8[_7[i]]={});
}
_8[_7[_7.length-1]]=_4._generateService(_6,_4._smd.services[_6]);
}
};
if(_1){
if((dojo.isString(_1))||(_1 instanceof dojo._Url)){
if(_1 instanceof dojo._Url){
_3=_1+"";
}else{
_3=_1;
}
var _a=dojo._getText(_3);
if(!_a){
throw new Error("Unable to load SMD from "+_1);
}else{
processSmd(dojo.fromJson(_a));
}
}else{
processSmd(_1);
}
}
this._options=(_2?_2:{});
this._requestId=0;
},_generateService:function(_b,_c){
if(this[_c]){
throw new Error("WARNING: "+_b+" already exists for service. Unable to generate function");
}
_c.name=_b;
var _d=dojo.hitch(this,"_executeMethod",_c);
var _e=dojox.rpc.transportRegistry.match(_c.transport||this._smd.transport);
if(_e.getExecutor){
_d=_e.getExecutor(_d,_c,this);
}
var _f=_c.returns||(_c._schema={});
var _10="/"+_b+"/";
_f._service=_d;
_d.servicePath=_10;
_d._schema=_f;
_d.id=dojox.rpc.Service._nextId++;
return _d;
},_getRequest:function(_11,_12){
var smd=this._smd;
var _14=dojox.rpc.envelopeRegistry.match(_11.envelope||smd.envelope||"NONE");
if(_14.namedParams){
if((_12.length==1)&&dojo.isObject(_12[0])){
_12=_12[0];
}else{
var _15={};
for(var i=0;i<_11.parameters.length;i++){
if(typeof _12[i]!="undefined"||!_11.parameters[i].optional){
_15[_11.parameters[i].name]=_12[i];
}
}
_12=_15;
}
var _17=(_11.parameters||[]).concat(smd.parameters||[]);
if(_11.strictParameters||smd.strictParameters){
for(i in _12){
var _18=false;
for(j=0;j<_17.length;j++){
if(_17[i].name==i){
_18=true;
}
}
if(!_18){
delete _12[i];
}
}
}
for(i=0;i<_17.length;i++){
var _19=_17[i];
if(!_19.optional&&_19.name&&!_12[_19.name]){
if(_19["default"]){
_12[_19.name]=_19["default"];
}else{
if(!(_19.name in _12)){
throw new Error("Required parameter "+_19.name+" was omitted");
}
}
}
}
}else{
if(_11.parameters&&_11.parameters[0]&&_11.parameters[0].name&&(_12.length==1)&&dojo.isObject(_12[0])){
if(_14.namedParams===false){
_12=dojox.rpc.toOrdered(_11,_12);
}else{
_12=_12[0];
}
}
}
if(dojo.isObject(this._options)){
_12=dojo.mixin(_12,this._options);
}
var _1a=_11._schema||_11.returns;
var _1b=_14.serialize.apply(this,[smd,_11,_12]);
_1b._envDef=_14;
var _1c=(_11.contentType||smd.contentType||_1b.contentType);
return dojo.mixin(_1b,{sync:dojox.rpc._sync,contentType:_1c,headers:{},target:_1b.target||dojox.rpc.getTarget(smd,_11),transport:_11.transport||smd.transport||_1b.transport,envelope:_11.envelope||smd.envelope||_1b.envelope,timeout:_11.timeout||smd.timeout,callbackParamName:_11.callbackParamName||smd.callbackParamName,schema:_1a,handleAs:_1b.handleAs||"auto",preventCache:_11.preventCache||smd.preventCache,frameDoc:this._options.frameDoc||undefined});
},_executeMethod:function(_1d){
var _1e=[];
var i;
for(i=1;i<arguments.length;i++){
_1e.push(arguments[i]);
}
var _20=this._getRequest(_1d,_1e);
var _21=dojox.rpc.transportRegistry.match(_20.transport).fire(_20);
_21.addBoth(function(_22){
return _20._envDef.deserialize.call(this,_22);
});
return _21;
}});
dojox.rpc.getTarget=function(smd,_24){
var _25=smd._baseUrl;
if(smd.target){
_25=new dojo._Url(_25,smd.target)+"";
}
if(_24.target){
_25=new dojo._Url(_25,_24.target)+"";
}
return _25;
};
dojox.rpc.toOrdered=function(_26,_27){
if(dojo.isArray(_27)){
return _27;
}
var _28=[];
for(var i=0;i<_26.parameters.length;i++){
_28.push(_27[_26.parameters[i].name]);
}
return _28;
};
dojox.rpc.transportRegistry=new dojo.AdapterRegistry(true);
dojox.rpc.envelopeRegistry=new dojo.AdapterRegistry(true);
dojox.rpc.envelopeRegistry.register("URL",function(str){
return str=="URL";
},{serialize:function(smd,_2c,_2d){
var d=dojo.objectToQuery(_2d);
return {data:d,transport:"POST"};
},deserialize:function(_2f){
return _2f;
},namedParams:true});
dojox.rpc.envelopeRegistry.register("JSON",function(str){
return str=="JSON";
},{serialize:function(smd,_32,_33){
var d=dojo.toJson(_33);
return {data:d,handleAs:"json",contentType:"application/json"};
},deserialize:function(_35){
return _35;
}});
dojox.rpc.envelopeRegistry.register("PATH",function(str){
return str=="PATH";
},{serialize:function(smd,_38,_39){
var i;
var _3b=dojox.rpc.getTarget(smd,_38);
if(dojo.isArray(_39)){
for(i=0;i<_39.length;i++){
_3b+="/"+_39[i];
}
}else{
for(i in _39){
_3b+="/"+i+"/"+_39[i];
}
}
return {data:"",target:_3b};
},deserialize:function(_3c){
return _3c;
}});
dojox.rpc.transportRegistry.register("POST",function(str){
return str=="POST";
},{fire:function(r){
r.url=r.target;
r.postData=r.data;
return dojo.rawXhrPost(r);
}});
dojox.rpc.transportRegistry.register("GET",function(str){
return str=="GET";
},{fire:function(r){
r.url=r.target+(r.data?"?"+r.data:"");
return dojo.xhrGet(r);
}});
dojox.rpc.transportRegistry.register("JSONP",function(str){
return str=="JSONP";
},{fire:function(r){
r.url=r.target+((r.target.indexOf("?")==-1)?"?":"&")+r.data;
r.callbackParamName=r.callbackParamName||"callback";
return dojo.io.script.get(r);
}});
dojox.rpc.Service._nextId=1;
dojo._contentHandlers.auto=function(xhr){
var _44=dojo._contentHandlers;
var _45=xhr.getResponseHeader("Content-Type");
results=!_45?_44.text(xhr):_45.match(/\/.*json/)?_44.json(xhr):_45.match(/\/javascript/)?_44.javascript(xhr):_45.match(/\/xml/)?_44.xml(xhr):_44.text(xhr);
return results;
};
}
