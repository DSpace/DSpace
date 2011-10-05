/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.gfx"]){
dojo._hasResource["dojox.gfx"]=true;
dojo.provide("dojox.gfx");
dojo.require("dojox.gfx.matrix");
dojo.require("dojox.gfx._base");
dojo.loadInit(function(){
var _1=dojo.getObject("dojox.gfx",true),sl,_3,_4;
if(!_1.renderer){
var _5=(typeof dojo.config.gfxRenderer=="string"?dojo.config.gfxRenderer:"svg,vml,silverlight,canvas").split(",");
var ua=navigator.userAgent,_7=0,_8=0;
if(dojo.isSafari>=3){
if(ua.indexOf("iPhone")>=0||ua.indexOf("iPod")>=0){
_4=ua.match(/Version\/(\d(\.\d)?(\.\d)?)\sMobile\/([^\s]*)\s?/);
if(_4){
_7=parseInt(_4[4].substr(0,3),16);
}
}
if(!_7){
_4=ua.match(/Android\s+(\d+\.\d+)/);
if(_4){
_8=parseFloat(_4[1]);
}
}
}
for(var i=0;i<_5.length;++i){
switch(_5[i]){
case "svg":
if(!dojo.isIE&&(!_7||_7>=1521)&&!_8){
dojox.gfx.renderer="svg";
}
break;
case "vml":
if(dojo.isIE){
dojox.gfx.renderer="vml";
}
break;
case "silverlight":
try{
if(dojo.isIE){
sl=new ActiveXObject("AgControl.AgControl");
if(sl&&sl.IsVersionSupported("1.0")){
_3=true;
}
}else{
if(navigator.plugins["Silverlight Plug-In"]){
_3=true;
}
}
}
catch(e){
_3=false;
}
finally{
sl=null;
}
if(_3){
dojox.gfx.renderer="silverlight";
}
break;
case "canvas":
if(!dojo.isIE){
dojox.gfx.renderer="canvas";
}
break;
}
if(dojox.gfx.renderer){
break;
}
}
if(dojo.config.isDebug){

}
}
});
dojo.requireIf(dojox.gfx.renderer=="svg","dojox.gfx.svg");
dojo.requireIf(dojox.gfx.renderer=="vml","dojox.gfx.vml");
dojo.requireIf(dojox.gfx.renderer=="silverlight","dojox.gfx.silverlight");
dojo.requireIf(dojox.gfx.renderer=="canvas","dojox.gfx.canvas");
}
