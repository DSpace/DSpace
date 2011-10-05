/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.cometd._base"]){
dojo._hasResource["dojox.cometd._base"]=true;
dojo.provide("dojox.cometd._base");
dojo.require("dojo.AdapterRegistry");
dojox.cometd={Connection:function(_1){
dojo.mixin(this,{"DISCONNECTED":"DISCONNECTED","CONNECTING":"CONNECTING","CONNECTED":"CONNECTED","DISCONNECTING":"DISCONNECING",prefix:_1,_initialized:false,_connected:false,_polling:false,_handshook:false,expectedNetworkDelay:10000,connectTimeout:0,version:"1.0",minimumVersion:"0.9",clientId:null,messageId:0,batch:0,_isXD:false,handshakeReturn:null,currentTransport:null,url:null,lastMessage:null,_messageQ:[],handleAs:"json",_advice:{},_backoffInterval:0,_backoffIncrement:1000,_backoffMax:60000,_deferredSubscribes:{},_deferredUnsubscribes:{},_subscriptions:[],_extendInList:[],_extendOutList:[]});
this.state=function(){
return this._initialized?(this._connected?"CONNECTED":"CONNECTING"):(this._connected?"DISCONNECTING":"DISCONNECTED");
};
this.init=function(_2,_3,_4){
_3=_3||{};
_3.version=this.version;
_3.minimumVersion=this.minimumVersion;
_3.channel="/meta/handshake";
_3.id=""+this.messageId++;
this.url=_2||dojo.config["cometdRoot"];
if(!this.url){
throw "no cometd root";
return null;
}
var _5="^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?$";
var _6=(""+window.location).match(new RegExp(_5));
if(_6[4]){
var _7=_6[4].split(":");
var _8=_7[0];
var _9=_7[1]||"80";
_6=this.url.match(new RegExp(_5));
if(_6[4]){
_7=_6[4].split(":");
var _a=_7[0];
var _b=_7[1]||"80";
this._isXD=((_a!=_8)||(_b!=_9));
}
}
if(!this._isXD){
_3.supportedConnectionTypes=dojo.map(dojox.cometd.connectionTypes.pairs,"return item[0]");
}
_3=this._extendOut(_3);
var _c={url:this.url,handleAs:this.handleAs,content:{"message":dojo.toJson([_3])},load:dojo.hitch(this,function(_d){
this._backon();
this._finishInit(_d);
}),error:dojo.hitch(this,function(e){
this._backoff();
this._finishInit([{}]);
}),timeout:this.expectedNetworkDelay};
if(_4){
dojo.mixin(_c,_4);
}
this._props=_3;
for(var _f in this._subscriptions){
for(var sub in this._subscriptions[_f]){
if(this._subscriptions[_f][sub].topic){
dojo.unsubscribe(this._subscriptions[_f][sub].topic);
}
}
}
this._messageQ=[];
this._subscriptions=[];
this._initialized=true;
this.batch=0;
this.startBatch();
var r;
if(this._isXD){
_c.callbackParamName="jsonp";
r=dojo.io.script.get(_c);
}else{
r=dojo.xhrPost(_c);
}
return r;
};
this.publish=function(_12,_13,_14){
var _15={data:_13,channel:_12};
if(_14){
dojo.mixin(_15,_14);
}
this._sendMessage(_15);
};
this.subscribe=function(_16,_17,_18,_19){
_19=_19||{};
if(_17){
var _1a=_1+_16;
var _1b=this._subscriptions[_1a];
if(!_1b||_1b.length==0){
_1b=[];
_19.channel="/meta/subscribe";
_19.subscription=_16;
this._sendMessage(_19);
var _ds=this._deferredSubscribes;
if(_ds[_16]){
_ds[_16].cancel();
delete _ds[_16];
}
_ds[_16]=new dojo.Deferred();
}
for(var i in _1b){
if(_1b[i].objOrFunc===_17&&(!_1b[i].funcName&&!_18||_1b[i].funcName==_18)){
return null;
}
}
var _1e=dojo.subscribe(_1a,_17,_18);
_1b.push({topic:_1e,objOrFunc:_17,funcName:_18});
this._subscriptions[_1a]=_1b;
}
var ret=this._deferredSubscribes[_16]||{};
ret.args=dojo._toArray(arguments);
return ret;
};
this.unsubscribe=function(_20,_21,_22,_23){
if((arguments.length==1)&&(!dojo.isString(_20))&&(_20.args)){
return this.unsubscribe.apply(this,_20.args);
}
var _24=_1+_20;
var _25=this._subscriptions[_24];
if(!_25||_25.length==0){
return null;
}
var s=0;
for(var i in _25){
var sb=_25[i];
if((!_21)||(sb.objOrFunc===_21&&(!sb.funcName&&!_22||sb.funcName==_22))){
dojo.unsubscribe(_25[i].topic);
delete _25[i];
}else{
s++;
}
}
if(s==0){
_23=_23||{};
_23.channel="/meta/unsubscribe";
_23.subscription=_20;
delete this._subscriptions[_24];
this._sendMessage(_23);
this._deferredUnsubscribes[_20]=new dojo.Deferred();
if(this._deferredSubscribes[_20]){
this._deferredSubscribes[_20].cancel();
delete this._deferredSubscribes[_20];
}
}
return this._deferredUnsubscribes[_20];
};
this.disconnect=function(){
for(var _29 in this._subscriptions){
for(var sub in this._subscriptions[_29]){
if(this._subscriptions[_29][sub].topic){
dojo.unsubscribe(this._subscriptions[_29][sub].topic);
}
}
}
this._subscriptions=[];
this._messageQ=[];
if(this._initialized&&this.currentTransport){
this._initialized=false;
this.currentTransport.disconnect();
}
if(!this._polling){
this._connected=false;
this._publishMeta("connect",false);
}
this._initialized=false;
this._handshook=false;
this._publishMeta("disconnect",true);
};
this.subscribed=function(_2b,_2c){
};
this.unsubscribed=function(_2d,_2e){
};
this.tunnelInit=function(_2f,_30){
};
this.tunnelCollapse=function(){
};
this._backoff=function(){
if(!this._advice){
this._advice={reconnect:"retry",interval:0};
}else{
if(!this._advice.interval){
this._advice.interval=0;
}
}
if(this._backoffInterval<this._backoffMax){
this._backoffInterval+=this._backoffIncrement;
}
};
this._backon=function(){
this._backoffInterval=0;
};
this._interval=function(){
var i=this._backoffInterval+(this._advice?(this._advice.interval?this._advice.interval:0):0);
if(i>0){

}
return i;
};
this._publishMeta=function(_32,_33,_34){
try{
var _35={cometd:this,action:_32,successful:_33,state:this.state()};
if(_34){
dojo.mixin(_35,_34);
}
dojo.publish(this.prefix+"/meta",[_35]);
}
catch(e){

}
};
this._finishInit=function(_36){
_36=_36[0];
this.handshakeReturn=_36;
if(_36["advice"]){
this._advice=_36.advice;
}
var _37=_36.successful?_36.successful:false;
if(_36.version<this.minimumVersion){
if(console.log){

}
_37=false;
this._advice.reconnect="none";
}
if(_37){
this.currentTransport=dojox.cometd.connectionTypes.match(_36.supportedConnectionTypes,_36.version,this._isXD);
var _38=this.currentTransport;
_38._cometd=this;
_38.version=_36.version;
this.clientId=_36.clientId;
this.tunnelInit=_38.tunnelInit&&dojo.hitch(_38,"tunnelInit");
this.tunnelCollapse=_38.tunnelCollapse&&dojo.hitch(_38,"tunnelCollapse");
_38.startup(_36);
}
this._publishMeta("handshake",_37,{reestablish:_37&&this._handshook});
if(_37){
this._handshook=true;
}else{

if(!this._advice||this._advice["reconnect"]!="none"){
setTimeout(dojo.hitch(this,"init",this.url,this._props),this._interval());
}
}
};
this._extendIn=function(_39){
dojo.forEach(dojox.cometd._extendInList,function(f){
_39=f(_39)||_39;
});
return _39;
};
this._extendOut=function(_3b){
dojo.forEach(dojox.cometd._extendOutList,function(f){
_3b=f(_3b)||_3b;
});
return _3b;
};
this.deliver=function(_3d){
dojo.forEach(_3d,this._deliver,this);
return _3d;
};
this._deliver=function(_3e){
_3e=this._extendIn(_3e);
if(!_3e["channel"]){
if(_3e["success"]!==true){
return;
}
}
this.lastMessage=_3e;
if(_3e.advice){
this._advice=_3e.advice;
}
var _3f=null;
if((_3e["channel"])&&(_3e.channel.length>5)&&(_3e.channel.substr(0,5)=="/meta")){
switch(_3e.channel){
case "/meta/connect":
if(_3e.successful&&!this._connected){
this._connected=this._initialized;
this.endBatch();
}else{
if(!this._initialized){
this._connected=false;
}
}
if(this._initialized){
this._publishMeta("connect",_3e.successful);
}
break;
case "/meta/subscribe":
_3f=this._deferredSubscribes[_3e.subscription];
try{
if(!_3e.successful){
if(_3f){
_3f.errback(new Error(_3e.error));
}
this.currentTransport.cancelConnect();
return;
}
if(_3f){
_3f.callback(true);
}
this.subscribed(_3e.subscription,_3e);
}
catch(e){
log.warn(e);
}
break;
case "/meta/unsubscribe":
_3f=this._deferredUnsubscribes[_3e.subscription];
try{
if(!_3e.successful){
if(_3f){
_3f.errback(new Error(_3e.error));
}
this.currentTransport.cancelConnect();
return;
}
if(_3f){
_3f.callback(true);
}
this.unsubscribed(_3e.subscription,_3e);
}
catch(e){
log.warn(e);
}
break;
default:
if(_3e.successful&&!_3e.successful){
this.currentTransport.cancelConnect();
return;
}
}
}
this.currentTransport.deliver(_3e);
if(_3e.data){
try{
var _40=[_3e];
var _41=_1+_3e.channel;
var _42=_3e.channel.split("/");
var _43=_1;
for(var i=1;i<_42.length-1;i++){
dojo.publish(_43+"/**",_40);
_43+="/"+_42[i];
}
dojo.publish(_43+"/**",_40);
dojo.publish(_43+"/*",_40);
dojo.publish(_41,_40);
}
catch(e){

}
}
};
this._sendMessage=function(_45){
if(this.currentTransport&&!this.batch){
return this.currentTransport.sendMessages([_45]);
}else{
this._messageQ.push(_45);
return null;
}
};
this.startBatch=function(){
this.batch++;
};
this.endBatch=function(){
if(--this.batch<=0&&this.currentTransport&&this._connected){
this.batch=0;
var _46=this._messageQ;
this._messageQ=[];
if(_46.length>0){
this.currentTransport.sendMessages(_46);
}
}
};
this._onUnload=function(){
dojo.addOnUnload(dojox.cometd,"disconnect");
};
this._connectTimeout=function(){
var _47=0;
if(this._advice&&this._advice.timeout&&this.expectedNetworkDelay>0){
_47=this._advice.timeout+this.expectedNetworkDelay;
}
if(this.connectTimeout>0&&this.connectTimeout<_47){
return this.connectTimeout;
}
return _47;
};
},connectionTypes:new dojo.AdapterRegistry(true)};
dojox.cometd.Connection.call(dojox.cometd,"/cometd");
dojo.addOnUnload(dojox.cometd,"_onUnload");
}
