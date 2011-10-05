/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.io.xhrPlugins"]){
dojo._hasResource["dojox.io.xhrPlugins"]=true;
dojo.provide("dojox.io.xhrPlugins");
dojo.require("dojo.AdapterRegistry");
dojo.require("dojo._base.xhr");
(function(){
var _1;
var _2;
dojox.io.xhrPlugins.register=function(){
if(!_1){
_1=new dojo.AdapterRegistry();
_2=dojox.io.xhrPlugins.plainXhr=dojo._defaultXhr||dojo.xhr;
dojo[dojo._defaultXhr?"_defaultXhr":"xhr"]=function(_3,_4,_5){
return _1.match.apply(_1,arguments);
};
_1.register("xhr",function(_6,_7){
if(!_7.url.match(/^\w*:\/\//)){
return true;
}
var _8=window.location.href.match(/^.*?\/\/.*?\//)[0];
return _7.url.substring(0,_8.length)==_8;
},_2);
}
return _1.register.apply(_1,arguments);
};
dojox.io.xhrPlugins.addProxy=function(_9){
dojox.io.xhrPlugins.register("proxy",function(_a,_b){
return true;
},function(_c,_d,_e){
_d.url=_9+encodeURIComponent(_d.url);
return _2.call(dojo,_c,_d,_e);
});
};
var _f;
dojox.io.xhrPlugins.addCrossSiteXhr=function(url,_11){
if(_f===undefined&&window.XMLHttpRequest){
try{
var xhr=new XMLHttpRequest();
xhr.open("GET","http://fnadkfna.com",true);
_f=true;
}
catch(e){
_f=false;
}
}
dojox.io.xhrPlugins.register("cs-xhr",function(_13,_14){
return (_f||(window.XDomainRequest&&_14.sync!==true&&(_13=="GET"||_13=="POST"||_11)))&&(_14.url.substring(0,url.length)==url);
},_f?_2:function(){
var _15=dojo._xhrObj;
dojo._xhrObj=function(){
var xdr=new XDomainRequest();
xdr.readyState=1;
xdr.setRequestHeader=function(){
};
xdr.getResponseHeader=function(_17){
return _17=="Content-Type"?xdr.contentType:null;
};
function handler(_18,_19){
return function(){
xdr.readyState=_19;
xdr.status=_18;
};
};
xdr.onload=handler(200,4);
xdr.onprogress=handler(200,3);
xdr.onerror=handler(404,4);
return xdr;
};
var dfd=(_11?_11(_2):_2).apply(dojo,arguments);
dojo._xhrObj=_15;
return dfd;
});
};
dojox.io.xhrPlugins.fullHttpAdapter=function(_1b,_1c){
return function(_1d,_1e,_1f){
var _20={};
var _21={};
if(!(_1d=="GET"||_1d=="POST")){
_21["http-method"]=_1d;
_1d="POST";
if(_1e.putData&&_1c){
_20["http-content"]=_1e.putData;
delete _1e.putData;
_1f=false;
}
}else{
if(_1d=="POST"&&_1e.postData&&_1c){
_20["http-content"]=_1e.postData;
delete _1e.postData;
_1f=false;
}
}
for(var i in _1e.headers){
var _23=i.match(/^X-/)?i.substring(2).replace(/-/g,"_").toLowerCase():("http-"+i);
_21[_23]=_1e.headers[i];
}
_1e.query=dojo.objectToQuery(_21);
dojo._ioAddQueryToUrl(_1e);
_1e.content=dojo.mixin(_1e.content||{},_20);
return _1b.call(dojo,_1d,_1e,_1f);
};
};
})();
}
