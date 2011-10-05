/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.embed.Quicktime"]){
dojo._hasResource["dojox.embed.Quicktime"]=true;
dojo.provide("dojox.embed.Quicktime");
(function(){
var _1,_2,_3,_4={width:320,height:240,redirect:null};
var _5="dojox-embed-quicktime-",_6=0;
var _7=dojo.moduleUrl("dojox","embed/resources/version.mov");
function prep(_8){
_8=dojo.mixin(dojo.clone(_4),_8||{});
if(!("path" in _8)){
console.error("dojox.embed.Quicktime(ctor):: no path reference to a QuickTime movie was provided.");
return null;
}
if(!("id" in _8)){
_8.id=(_5+_6++);
}
return _8;
};
var _9="This content requires the <a href=\"http://www.apple.com/quicktime/download/\" title=\"Download and install QuickTime.\">QuickTime plugin</a>.";
if(dojo.isIE){
_2=0;
_3=(function(){
try{
var o=new ActiveXObject("QuickTimeCheckObject.QuickTimeCheck.1");
if(o!==undefined){
var v=o.QuickTimeVersion.toString(16);
_2={major:parseInt(v.substring(0,1),10)||0,minor:parseInt(v.substring(1,2),10)||0,rev:parseInt(v.substring(2,3),10)||0};
return o.IsQuickTimeAvailable(0);
}
}
catch(e){
}
return false;
})();
_1=function(_c){
if(!_3){
return {id:null,markup:_9};
}
_c=prep(_c);
if(!_c){
return null;
}
var s="<object classid=\"clsid:02BF25D5-8C17-4B23-BC80-D3488ABDDC6B\" "+"codebase=\"http://www.apple.com/qtactivex/qtplugin.cab#version=6,0,2,0\" "+"id=\""+_c.id+"\" "+"width=\""+_c.width+"\" "+"height=\""+_c.height+"\">"+"<param name=\"src\" value=\""+_c.path+"\" />";
if(_c.params){
for(var p in _c.params){
s+="<param name=\""+p+"\" value=\""+_c.params[p]+"\" />";
}
}
s+="</object>";
return {id:_c.id,markup:s};
};
}else{
_3=(function(){
for(var i=0,l=navigator.plugins.length;i<l;i++){
if(navigator.plugins[i].name.indexOf("QuickTime")>-1){
return true;
}
}
return false;
})();
_1=function(_11){
if(!_3){
return {id:null,markup:_9};
}
_11=prep(_11);
if(!_11){
return null;
}
var s="<embed type=\"video/quicktime\" src=\""+_11.path+"\" "+"id=\""+_11.id+"\" "+"name=\""+_11.id+"\" "+"pluginspage=\"www.apple.com/quicktime/download\" "+"enablejavascript=\"true\" "+"width=\""+_11.width+"\" "+"height=\""+_11.height+"\"";
if(_11.params){
for(var p in _11.params){
s+=" "+p+"=\""+_11.params[p]+"\"";
}
}
s+="></embed>";
return {id:_11.id,markup:s};
};
}
dojox.embed.Quicktime=function(_14,_15){
return dojox.embed.Quicktime.place(_14,_15);
};
dojo.mixin(dojox.embed.Quicktime,{minSupported:6,available:_3,supported:_3,version:_2,initialized:false,onInitialize:function(){
dojox.embed.Quicktime.initialized=true;
},place:function(_16,_17){
var o=_1(_16);
_17=dojo.byId(_17);
if(!_17){
_17=dojo.doc.createElement("div");
_17.id=o.id+"-container";
dojo.body().appendChild(_17);
}
if(o){
_17.innerHTML=o.markup;
if(o.id){
return (dojo.isIE)?dojo.byId(o.id):document[o.id];
}
}
return null;
}});
if(!dojo.isIE){
_2=dojox.embed.Quicktime.version={major:0,minor:0,rev:0};
var o=_1({path:_7,width:4,height:4});
function qtInsert(){
if(!dojo._initFired){
var s="<div style=\"top:0;left:0;width:1px;height:1px;;overflow:hidden;position:absolute;\" id=\"-qt-version-test\">"+o.markup+"</div>";
document.write(s);
}else{
var n=document.createElement("div");
n.id="-qt-version-test";
n.style.cssText="top:0;left:0;width:1px;height:1px;overflow:hidden;position:absolute;";
dojo.body().appendChild(n);
n.innerHTML=o.markup;
}
};
function qtGetInfo(mv){
var qt,n,v=[0,0,0];
if(mv){
qt=mv,n=qt.parentNode;
}else{
if(o.id){
qtInsert();
if(!dojo.isOpera){
setTimeout(function(){
qtGetInfo(document[o.id]);
},50);
}else{
var fn=function(){
setTimeout(function(){
qtGetInfo(document[o.id]);
},50);
};
if(!dojo._initFired){
dojo.addOnLoad(fn);
}else{
dojo.connect(document[o.id],"onload",fn);
}
}
}
return;
}
if(qt){
try{
v=qt.GetQuickTimeVersion().split(".");
_2={major:parseInt(v[0]||0),minor:parseInt(v[1]||0),rev:parseInt(v[2]||0)};
}
catch(e){
_2={major:0,minor:0,rev:0};
}
}
dojox.embed.Quicktime.supported=v[0];
dojox.embed.Quicktime.version=_2;
if(dojox.embed.Quicktime.supported){
dojox.embed.Quicktime.onInitialize();
}else{

}
try{
if(!mv){
dojo.body().removeChild(n);
}
}
catch(e){
}
};
qtGetInfo();
}else{
if(dojo.isIE&&_3){
dojox.embed.Quicktime.onInitialize();
}
}
})();
}
