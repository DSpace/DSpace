/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.embed.Flash"]){
dojo._hasResource["dojox.embed.Flash"]=true;
dojo.provide("dojox.embed.Flash");
(function(){
var _1,_2;
var _3="dojox-embed-flash-",_4=0;
function prep(_5){
_5=dojo.mixin({expressInstall:false,width:320,height:240,style:null,redirect:null},_5||{});
if(!("path" in _5)){
console.error("dojox.embed.Flash(ctor):: no path reference to a Flash movie was provided.");
return null;
}
if(!("id" in _5)){
_5.id=(_3+_4++);
}
return _5;
};
if(dojo.isIE){
_1=function(_6){
_6=prep(_6);
if(!_6){
return null;
}
var _7=_6.path;
if(_6.vars){
var a=[];
for(var p in _6.vars){
a.push(p+"="+_6.vars[p]);
}
_7+=((_7.indexOf("?")==-1)?"?":"&")+a.join("&");
}
var s="<object id=\""+_6.id+"\" "+"classid=\"clsid:D27CDB6E-AE6D-11cf-96B8-444553540000\" "+"width=\""+_6.width+"\" "+"height=\""+_6.height+"\""+((_6.style)?" style=\""+_6.style+"\"":"")+">"+"<param name=\"movie\" value=\""+_7+"\" />";
if(_6.params){
for(var p in _6.params){
s+="<param name=\""+p+"\" value=\""+_6.params[p]+"\" />";
}
}
s+="</object>";
return {id:_6.id,markup:s};
};
_2=(function(){
var _b=10,_c=null;
while(!_c&&_b>7){
try{
_c=new ActiveXObject("ShockwaveFlash.ShockwaveFlash."+_b--);
}
catch(e){
}
}
if(_c){
var v=_c.GetVariable("$version").split(" ")[1].split(",");
return {major:(v[0]!=null)?parseInt(v[0]):0,minor:(v[1]!=null)?parseInt(v[1]):0,rev:(v[2]!=null)?parseInt(v[2]):0};
}
return {major:0,minor:0,rev:0};
})();
dojo.addOnUnload(function(){
var _f=dojo.query("object");
for(var i=_f.length-1;i>=0;i--){
_f[i].style.display="none";
for(var p in _f[i]){
if(p!="FlashVars"&&dojo.isFunction(_f[i][p])){
_f[i][p]=function(){
};
}
}
}
});
}else{
_1=function(_12){
_12=prep(_12);
if(!_12){
return null;
}
var _13=_12.path;
if(_12.vars){
var a=[];
for(var p in _12.vars){
a.push(p+"="+_12.vars[p]);
}
_13+=((_13.indexOf("?")==-1)?"?":"&")+a.join("&");
}
var s="<embed type=\"application/x-shockwave-flash\" "+"src=\""+_13+"\" "+"id=\""+_12.id+"\" "+"name=\""+_12.id+"\" "+"width=\""+_12.width+"\" "+"height=\""+_12.height+"\""+((_12.style)?" style=\""+_12.style+"\" ":"")+"swLiveConnect=\"true\" "+"allowScriptAccess=\"sameDomain\" "+"pluginspage=\""+window.location.protocol+"//www.adobe.com/go/getflashplayer\" ";
if(_12.params){
for(var p in _12.params){
s+=" "+p+"=\""+_12.params[p]+"\"";
}
}
s+=" />";
return {id:_12.id,markup:s};
};
_2=(function(){
var _17=navigator.plugins["Shockwave Flash"];
if(_17&&_17.description){
var v=_17.description.replace(/([a-zA-Z]|\s)+/,"").replace(/(\s+r|\s+b[0-9]+)/,".").split(".");
return {major:(v[0]!=null)?parseInt(v[0]):0,minor:(v[1]!=null)?parseInt(v[1]):0,rev:(v[2]!=null)?parseInt(v[2]):0};
}
return {major:0,minor:0,rev:0};
})();
}
dojox.embed.Flash=function(_19,_1a){
this.id=null;
this.movie=null;
this.domNode=null;
if(_19&&_1a){
this.init(_19,_1a);
}
};
dojo.extend(dojox.embed.Flash,{onReady:function(_1b){
},onLoad:function(_1c){
},init:function(_1d,_1e){
this.destroy();
_1e=_1e||this.domNode;
if(!_1e){
throw new Error("dojox.embed.Flash: no domNode reference has been passed.");
}
this._poller=null;
this._pollCount=0,this._pollMax=250;
if(dojox.embed.Flash.initialized){
this.id=dojox.embed.Flash.place(_1d,_1e);
this.domNode=_1e;
setTimeout(dojo.hitch(this,function(){
this.movie=(dojo.isIE)?dojo.byId(this.id):document[this.id];
this.onReady(this.movie);
this._poller=setInterval(dojo.hitch(this,function(){
if(this.movie.PercentLoaded()==100||this._pollCount++>this._pollMax){
clearInterval(this._poller);
delete this._poller;
delete this._pollCount;
delete this._pollMax;
this.onLoad(this.movie);
}
}),10);
}),1);
}
},_destroy:function(){
this.domNode.removeChild(this.movie);
this.id=this.movie=this.domNode=null;
},destroy:function(){
if(!this.movie){
return;
}
var _1f=dojo.mixin({},{id:true,movie:true,domNode:true,onReady:true,onLoad:true});
for(var p in this){
if(!_1f[p]){
delete this[p];
}
}
if(this._poller){
dojo.connect(this,"onLoad",this,"_destroy");
}else{
this._destroy();
}
}});
dojo.mixin(dojox.embed.Flash,{minSupported:8,available:_2.major,supported:(_2.major>=8),version:_2,initialized:false,onInitialize:function(){
dojox.embed.Flash.initialized=true;
},__ie_markup__:function(_21){
return _1(_21);
},proxy:function(obj,_23){
dojo.forEach((dojo.isArray(_23)?_23:[_23]),function(_24){
this[_24]=dojo.hitch(this,function(){
return (function(){
return eval(this.movie.CallFunction("<invoke name=\""+_24+"\" returntype=\"javascript\">"+"<arguments>"+dojo.map(arguments,function(_25){
return __flash__toXML(_25);
}).join("")+"</arguments>"+"</invoke>"));
}).apply(this,arguments||[]);
});
},obj);
}});
if(dojo.isIE){
if(dojo._initFired){
var e=document.createElement("script");
e.type="text/javascript";
e.src=dojo.moduleUrl("dojox","embed/IE/flash.js");
document.getElementsByTagName("head")[0].appendChild(e);
}else{
document.write("<scr"+"ipt type=\"text/javascript\" src=\""+dojo.moduleUrl("dojox","embed/IE/flash.js")+"\">"+"</scr"+"ipt>");
}
}else{
dojox.embed.Flash.place=function(_26,_27){
var o=_1(_26);
_27=dojo.byId(_27);
if(!_27){
_27=dojo.doc.createElement("div");
_27.id=o.id+"-container";
dojo.body().appendChild(_27);
}
if(o){
_27.innerHTML=o.markup;
return o.id;
}
return null;
};
dojox.embed.Flash.onInitialize();
}
})();
}
