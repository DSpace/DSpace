/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.cometd.RestChannels"]){
dojo._hasResource["dojox.cometd.RestChannels"]=true;
dojo.provide("dojox.cometd.RestChannels");
dojo.require("dojox.rpc.Client");
if(dojox.data&&dojox.data.JsonRestStore){
dojo.require("dojox.data.restListener");
}
(function(){
dojo.declare("dojox.cometd.RestChannels",null,{constructor:function(_1){
dojo.mixin(this,_1);
if(dojox.rpc.Rest&&this.autoSubscribeRoot){
var _2=dojox.rpc.Rest._get;
var _3=this;
dojox.rpc.Rest._get=function(_4,id){
var _6=dojo.xhrGet;
dojo.xhrGet=function(r){
var _8=_3.autoSubscribeRoot;
return (_8&&r.url.substring(0,_8.length)==_8)?_3.get(r.url,r):_6(r);
};
var _9=_2.apply(this,arguments);
dojo.xhrGet=_6;
return _9;
};
}
if(dojox.data&&dojox.data.restListener){
this.receive=dojox.data.restListener;
}
},absoluteUrl:function(_a,_b){
return new dojo._Url(_a,_b)+"";
},acceptType:"x-application/rest+json,application/http;q=0.9,*/*;q=0.7",subscriptions:{},subCallbacks:{},autoReconnectTime:3000,sendAsJson:false,url:"/channels",autoSubscribeRoot:"/",open:function(){
if(!this.connected){
this.connectionId=dojox._clientId;
var _c=this.started?"X-Client-Id":"X-Create-Client-Id";
var _d={Accept:this.acceptType};
_d[_c]=this.connectionId;
var _e=dojo.xhrPost({headers:_d,url:this.url,noStatus:true});
var _f=this;
this.lastIndex=0;
var _10,_11=function(_12){
if(typeof dojo=="undefined"){
return null;
}
_12=_12.substring(_f.lastIndex);
var _13=xhr&&(xhr.contentType||xhr.getResponseHeader("Content-Type"));
_f.started=true;
var _15=_f.onprogress(xhr,_12,_13);
if(_15){
if(_10()){
return new Error(_15);
}
}
if(!xhr||xhr.readyState==4){
xhr=null;
if(_f.connected){
_f.connected=false;
_f.open();
}
}
return _12;
};
_10=function(_16){
if(xhr&&xhr.status==409){

_f.disconnected();
return null;
}
if(_f.started){
_f.started=false;
_f.connected=false;
var _17=_f.subscriptions;
_f.subscriptions={};
for(var i in _17){
_f.subscribe(i,{since:_17[i]});
}
}else{
_f.disconnected();
}
return _16;
};
_e.addCallbacks(_11,_10);
var xhr=_e.ioArgs.xhr;
if(xhr){
xhr.onreadystatechange=function(){
var _19;
try{
if(xhr.readyState==3){
_f.readyState=3;
_19=xhr.responseText;
}
}
catch(e){
}
if(typeof _19=="string"){
_11(_19);
}
};
}
if(window.attachEvent){
attachEvent("onunload",function(){
_f.connected=false;
if(xhr){
xhr.abort();
}
});
}
this.connected=true;
}
},_send:function(_1a,_1b,_1c){
if(this.sendAsJson){
_1b.postBody=dojo.toJson({target:_1b.url,method:_1a,content:_1c,params:_1b.content,subscribe:headers["X-Subscribe"]});
_1b.url=this.url;
_1a="POST";
}else{
_1b.postData=dojo.toJson(_1c);
}
return dojo.xhr(_1a,_1b,_1b.postBody);
},subscribe:function(_1d,_1e){
_1e=_1e||{};
_1e.url=this.absoluteUrl(this.url,_1d);
if(_1e.headers){
delete _1e.headers.Range;
}
var _1f=this.subscriptions[_1d];
var _20=_1e.method||"HEAD";
var _21=_1e.since;
var _22=_1e.callback;
var _23=_1e.headers||(_1e.headers={});
this.subscriptions[_1d]=_21||_1f||0;
var _24=this.subCallbacks[_1d];
if(_22){
this.subCallbacks[_1d]=_24?function(m){
_24(m);
_22(m);
}:_22;
}
if(!this.connected){
this.open();
}
if(_1f===undefined||_1f!=_21){
_23["Cache-Control"]="max-age=0";
_21=typeof _21=="number"?new Date(_21).toUTCString():_21;
if(_21){
_23["X-Subscribe-Since"]=_21;
}
_23["X-Subscribe"]=_1e.unsubscribe?"none":"*";
var dfd=this._send(_20,_1e);
var _27=this;
dfd.addBoth(function(_28){
var xhr=dfd.ioArgs.xhr;
if(!(_28 instanceof Error)){
if(_1e.confirmation){
_1e.confirmation();
}
}
if(xhr&&xhr.getResponseHeader("X-Subscribed")=="OK"){
var _2a=xhr.getResponseHeader("Last-Modified");
if(xhr.responseText){
_27.subscriptions[_1d]=_2a||new Date().toUTCString();
}else{
return null;
}
}else{
if(xhr){
delete _27.subscriptions[_1d];
}
}
if(!(_28 instanceof Error)){
var _2b={responseText:xhr&&xhr.responseText,channel:_1d,getResponseHeader:function(_2c){
return xhr.getResponseHeader(_2c);
},getAllResponseHeaders:function(){
return xhr.getAllResponseHeaders();
},result:_28};
if(_27.subCallbacks[_1d]){
_27.subCallbacks[_1d](_2b);
}
}else{
if(_27.subCallbacks[_1d]){
_27.subCallbacks[_1d](xhr);
}
}
return _28;
});
return dfd;
}
return null;
},publish:function(_2d,_2e){
return this._send("POST",{url:_2d,contentType:"application/json"},_2e);
},_processMessage:function(_2f){
_2f.event=_2f.event||_2f.getResponseHeader("X-Event");
if(_2f.event=="connection-conflict"){
return "conflict";
}
try{
_2f.result=_2f.result||dojo.fromJson(_2f.responseText);
}
catch(e){
}
var _30=this;
var loc=_2f.channel=new dojo._Url(this.url,_2f.source||_2f.getResponseHeader("Content-Location"))+"";
if(loc in this.subscriptions&&_2f.getResponseHeader){
this.subscriptions[loc]=_2f.getResponseHeader("Last-Modified");
}
if(this.subCallbacks[loc]){
setTimeout(function(){
_30.subCallbacks[loc](_2f);
},0);
}
this.receive(_2f);
return null;
},onprogress:function(xhr,_33,_34){
if(!_34||_34.match(/application\/rest\+json/)){
var _35=_33.length;
_33=_33.replace(/^\s*[,\[]?/,"[").replace(/[,\]]?\s*$/,"]");
try{
var _36=dojo.fromJson(_33);
this.lastIndex+=_35;
}
catch(e){
}
}else{
if(dojox.io&&dojox.io.httpParse&&_34.match(/application\/http/)){
var _37="";
if(xhr&&xhr.getAllResponseHeaders){
_37=xhr.getAllResponseHeaders();
}
_36=dojox.io.httpParse(_33,_37,xhr.readyState!=4);
}
}
if(_36){
for(var i=0;i<_36.length;i++){
if(this._processMessage(_36[i])){
return "conflict";
}
}
return null;
}
if(!xhr){
return "error";
}
if(xhr.readyState!=4){
return null;
}
if(xhr.__proto__){
xhr={channel:"channel",__proto__:xhr};
}
return this._processMessage(xhr);
},get:function(_39,_3a){
(_3a=_3a||{}).method="GET";
return this.subscribe(_39,_3a);
},receive:function(_3b){
},disconnected:function(){
var _3c=this;
if(this.connected){
setTimeout(function(){
_3c.open();
},this.autoReconnectTime);
}
this.connected=false;
},unsubscribe:function(_3d,_3e){
_3e=_3e||{};
_3e.unsubscribe=true;
this.subscribe(_3d,_3e);
},disconnect:function(){
this.connected=false;
this.xhr.abort();
}});
var _3f=dojox.cometd.RestChannels.defaultInstance=new dojox.cometd.RestChannels();
if(dojox.cometd.connectionTypes){
_3f.startup=function(_40){
_3f.open();
this._cometd._deliver({channel:"/meta/connect",successful:true});
};
_3f.check=function(_41,_42,_43){
for(var i=0;i<_41.length;i++){
if(_41[i]=="rest-channels"){
return !_43;
}
}
return false;
};
_3f.deliver=function(_45){
};
dojo.connect(this,"receive",null,function(_46){
_46.data=_46.result;
this._cometd._deliver(_46);
});
_3f.sendMessages=function(_47){
for(var i=0;i<_47.length;i++){
var _49=_47[i];
var _4a=_49.channel;
var _4b=this._cometd;
var _4c={confirmation:function(){
_4b._deliver({channel:_4a,successful:true});
}};
if(_4a=="/meta/subscribe"){
this.subscribe(_49.subscription,_4c);
}else{
if(_4a=="/meta/unsubscribe"){
this.unsubscribe(_49.subscription,_4c);
}else{
if(_4a=="/meta/connect"){
_4c.confirmation();
}else{
if(_4a=="/meta/disconnect"){
_3f.disconnect();
_4c.confirmation();
}else{
if(_4a.substring(0,6)!="/meta/"){
this.publish(_4a,_49.data);
}
}
}
}
}
}
};
dojox.cometd.connectionTypes.register("rest-channels",_3f.check,_3f,false,true);
}
})();
}
