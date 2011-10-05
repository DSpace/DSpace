/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.xhr"]){
dojo._hasResource["dojo._base.xhr"]=true;
dojo.provide("dojo._base.xhr");
dojo.require("dojo._base.Deferred");
dojo.require("dojo._base.json");
dojo.require("dojo._base.lang");
dojo.require("dojo._base.query");
(function(){
var _d=dojo;
function setValue(_2,_3,_4){
var _5=_2[_3];
if(_d.isString(_5)){
_2[_3]=[_5,_4];
}else{
if(_d.isArray(_5)){
_5.push(_4);
}else{
_2[_3]=_4;
}
}
};
dojo.formToObject=function(_6){
var _7={};
var _8="file|submit|image|reset|button|";
_d.forEach(dojo.byId(_6).elements,function(_9){
var _a=_9.name;
var _b=(_9.type||"").toLowerCase();
if(_a&&_b&&_8.indexOf(_b)==-1&&!_9.disabled){
if(_b=="radio"||_b=="checkbox"){
if(_9.checked){
setValue(_7,_a,_9.value);
}
}else{
if(_9.multiple){
_7[_a]=[];
_d.query("option",_9).forEach(function(_c){
if(_c.selected){
setValue(_7,_a,_c.value);
}
});
}else{
setValue(_7,_a,_9.value);
if(_b=="image"){
_7[_a+".x"]=_7[_a+".y"]=_7[_a].x=_7[_a].y=0;
}
}
}
}
});
return _7;
};
dojo.objectToQuery=function(_d){
var _e=encodeURIComponent;
var _f=[];
var _10={};
for(var _11 in _d){
var _12=_d[_11];
if(_12!=_10[_11]){
var _13=_e(_11)+"=";
if(_d.isArray(_12)){
for(var i=0;i<_12.length;i++){
_f.push(_13+_e(_12[i]));
}
}else{
_f.push(_13+_e(_12));
}
}
}
return _f.join("&");
};
dojo.formToQuery=function(_15){
return _d.objectToQuery(_d.formToObject(_15));
};
dojo.formToJson=function(_16,_17){
return _d.toJson(_d.formToObject(_16),_17);
};
dojo.queryToObject=function(str){
var ret={};
var qp=str.split("&");
var dec=decodeURIComponent;
_d.forEach(qp,function(_1c){
if(_1c.length){
var _1d=_1c.split("=");
var _1e=dec(_1d.shift());
var val=dec(_1d.join("="));
if(_d.isString(ret[_1e])){
ret[_1e]=[ret[_1e]];
}
if(_d.isArray(ret[_1e])){
ret[_1e].push(val);
}else{
ret[_1e]=val;
}
}
});
return ret;
};
dojo._blockAsync=false;
dojo._contentHandlers={"text":function(xhr){
return xhr.responseText;
},"json":function(xhr){
return _d.fromJson(xhr.responseText||null);
},"json-comment-filtered":function(xhr){
if(!dojo.config.useCommentedJson){
console.warn("Consider using the standard mimetype:application/json."+" json-commenting can introduce security issues. To"+" decrease the chances of hijacking, use the standard the 'json' handler and"+" prefix your json with: {}&&\n"+"Use djConfig.useCommentedJson=true to turn off this message.");
}
var _23=xhr.responseText;
var _24=_23.indexOf("/*");
var _25=_23.lastIndexOf("*/");
if(_24==-1||_25==-1){
throw new Error("JSON was not comment filtered");
}
return _d.fromJson(_23.substring(_24+2,_25));
},"javascript":function(xhr){
return _d.eval(xhr.responseText);
},"xml":function(xhr){
var _28=xhr.responseXML;
if(_d.isIE&&(!_28||_28.documentElement==null)){
_d.forEach(["MSXML2","Microsoft","MSXML","MSXML3"],function(_29){
try{
var dom=new ActiveXObject(_29+".XMLDOM");
dom.async=false;
dom.loadXML(xhr.responseText);
_28=dom;
}
catch(e){
}
});
}
return _28;
}};
dojo._contentHandlers["json-comment-optional"]=function(xhr){
var _2c=_d._contentHandlers;
if(xhr.responseText&&xhr.responseText.indexOf("/*")!=-1){
return _2c["json-comment-filtered"](xhr);
}else{
return _2c["json"](xhr);
}
};
dojo._ioSetArgs=function(_2d,_2e,_2f,_30){
var _31={args:_2d,url:_2d.url};
var _32=null;
if(_2d.form){
var _33=_d.byId(_2d.form);
var _34=_33.getAttributeNode("action");
_31.url=_31.url||(_34?_34.value:null);
_32=_d.formToObject(_33);
}
var _35=[{}];
if(_32){
_35.push(_32);
}
if(_2d.content){
_35.push(_2d.content);
}
if(_2d.preventCache){
_35.push({"dojo.preventCache":new Date().valueOf()});
}
_31.query=_d.objectToQuery(_d.mixin.apply(null,_35));
_31.handleAs=_2d.handleAs||"text";
var d=new _d.Deferred(_2e);
d.addCallbacks(_2f,function(_37){
return _30(_37,d);
});
var ld=_2d.load;
if(ld&&_d.isFunction(ld)){
d.addCallback(function(_39){
return ld.call(_2d,_39,_31);
});
}
var err=_2d.error;
if(err&&_d.isFunction(err)){
d.addErrback(function(_3b){
return err.call(_2d,_3b,_31);
});
}
var _3c=_2d.handle;
if(_3c&&_d.isFunction(_3c)){
d.addBoth(function(_3d){
return _3c.call(_2d,_3d,_31);
});
}
d.ioArgs=_31;
return d;
};
var _3e=function(dfd){
dfd.canceled=true;
var xhr=dfd.ioArgs.xhr;
var _at=typeof xhr.abort;
if(_at=="function"||_at=="object"||_at=="unknown"){
xhr.abort();
}
var err=dfd.ioArgs.error;
if(!err){
err=new Error("xhr cancelled");
err.dojoType="cancel";
}
return err;
};
var _43=function(dfd){
var ret=_d._contentHandlers[dfd.ioArgs.handleAs](dfd.ioArgs.xhr);
return (typeof ret=="undefined")?null:ret;
};
var _46=function(_47,dfd){

return _47;
};
var _49=null;
var _4a=[];
var _4b=function(){
var now=(new Date()).getTime();
if(!_d._blockAsync){
for(var i=0,tif;i<_4a.length&&(tif=_4a[i]);i++){
var dfd=tif.dfd;
var _50=function(){
if(!dfd||dfd.canceled||!tif.validCheck(dfd)){
_4a.splice(i--,1);
}else{
if(tif.ioCheck(dfd)){
_4a.splice(i--,1);
tif.resHandle(dfd);
}else{
if(dfd.startTime){
if(dfd.startTime+(dfd.ioArgs.args.timeout||0)<now){
_4a.splice(i--,1);
var err=new Error("timeout exceeded");
err.dojoType="timeout";
dfd.errback(err);
dfd.cancel();
}
}
}
}
};
if(dojo.config.isDebug){
_50.call(this);
}else{
try{
_50.call(this);
}
catch(e){
dfd.errback(e);
}
}
}
}
if(!_4a.length){
clearInterval(_49);
_49=null;
return;
}
};
dojo._ioCancelAll=function(){
try{
_d.forEach(_4a,function(i){
try{
i.dfd.cancel();
}
catch(e){
}
});
}
catch(e){
}
};
if(_d.isIE){
_d.addOnWindowUnload(_d._ioCancelAll);
}
_d._ioWatch=function(dfd,_54,_55,_56){
if(dfd.ioArgs.args.timeout){
dfd.startTime=(new Date()).getTime();
}
_4a.push({dfd:dfd,validCheck:_54,ioCheck:_55,resHandle:_56});
if(!_49){
_49=setInterval(_4b,50);
}
_4b();
};
var _57="application/x-www-form-urlencoded";
var _58=function(dfd){
return dfd.ioArgs.xhr.readyState;
};
var _5a=function(dfd){
return 4==dfd.ioArgs.xhr.readyState;
};
var _5c=function(dfd){
var xhr=dfd.ioArgs.xhr;
if(_d._isDocumentOk(xhr)){
dfd.callback(dfd);
}else{
var err=new Error("Unable to load "+dfd.ioArgs.url+" status:"+xhr.status);
err.status=xhr.status;
err.responseText=xhr.responseText;
dfd.errback(err);
}
};
dojo._ioAddQueryToUrl=function(_60){
if(_60.query.length){
_60.url+=(_60.url.indexOf("?")==-1?"?":"&")+_60.query;
_60.query=null;
}
};
dojo.xhr=function(_61,_62,_63){
var dfd=_d._ioSetArgs(_62,_3e,_43,_46);
dfd.ioArgs.xhr=_d._xhrObj(dfd.ioArgs.args);
if(_63){
if("postData" in _62){
dfd.ioArgs.query=_62.postData;
}else{
if("putData" in _62){
dfd.ioArgs.query=_62.putData;
}
}
}else{
_d._ioAddQueryToUrl(dfd.ioArgs);
}
var _65=dfd.ioArgs;
var xhr=_65.xhr;
xhr.open(_61,_65.url,_62.sync!==true,_62.user||undefined,_62.password||undefined);
if(_62.headers){
for(var hdr in _62.headers){
if(hdr.toLowerCase()==="content-type"&&!_62.contentType){
_62.contentType=_62.headers[hdr];
}else{
xhr.setRequestHeader(hdr,_62.headers[hdr]);
}
}
}
xhr.setRequestHeader("Content-Type",_62.contentType||_57);
if(!_62.headers||!_62.headers["X-Requested-With"]){
xhr.setRequestHeader("X-Requested-With","XMLHttpRequest");
}
if(dojo.config.isDebug){
xhr.send(_65.query);
}else{
try{
xhr.send(_65.query);
}
catch(e){
dfd.ioArgs.error=e;
dfd.cancel();
}
}
_d._ioWatch(dfd,_58,_5a,_5c);
xhr=null;
return dfd;
};
dojo.xhrGet=function(_68){
return _d.xhr("GET",_68);
};
dojo.rawXhrPost=dojo.xhrPost=function(_69){
return _d.xhr("POST",_69,true);
};
dojo.rawXhrPut=dojo.xhrPut=function(_6a){
return _d.xhr("PUT",_6a,true);
};
dojo.xhrDelete=function(_6b){
return _d.xhr("DELETE",_6b);
};
})();
}
