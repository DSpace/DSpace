/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.back"]){
dojo._hasResource["dojo.back"]=true;
dojo.provide("dojo.back");
(function(){
var _1=dojo.back;
function getHash(){
var h=window.location.hash;
if(h.charAt(0)=="#"){
h=h.substring(1);
}
return dojo.isMozilla?h:decodeURIComponent(h);
};
function setHash(h){
if(!h){
h="";
}
window.location.hash=encodeURIComponent(h);
_4=history.length;
};
if(dojo.exists("tests.back-hash")){
_1.getHash=getHash;
_1.setHash=setHash;
}
var _5=(typeof (window)!=="undefined")?window.location.href:"";
var _6=(typeof (window)!=="undefined")?getHash():"";
var _7=null;
var _8=null;
var _9=null;
var _a=null;
var _b=[];
var _c=[];
var _d=false;
var _e=false;
var _4;
function handleBackButton(){
var _f=_c.pop();
if(!_f){
return;
}
var _10=_c[_c.length-1];
if(!_10&&_c.length==0){
_10=_7;
}
if(_10){
if(_10.kwArgs["back"]){
_10.kwArgs["back"]();
}else{
if(_10.kwArgs["backButton"]){
_10.kwArgs["backButton"]();
}else{
if(_10.kwArgs["handle"]){
_10.kwArgs.handle("back");
}
}
}
}
_b.push(_f);
};
_1.goBack=handleBackButton;
function handleForwardButton(){
var _11=_b.pop();
if(!_11){
return;
}
if(_11.kwArgs["forward"]){
_11.kwArgs.forward();
}else{
if(_11.kwArgs["forwardButton"]){
_11.kwArgs.forwardButton();
}else{
if(_11.kwArgs["handle"]){
_11.kwArgs.handle("forward");
}
}
}
_c.push(_11);
};
_1.goForward=handleForwardButton;
function createState(url,_13,_14){
return {"url":url,"kwArgs":_13,"urlHash":_14};
};
function getUrlQuery(url){
var _16=url.split("?");
if(_16.length<2){
return null;
}else{
return _16[1];
}
};
function loadIframeHistory(){
var url=(dojo.config["dojoIframeHistoryUrl"]||dojo.moduleUrl("dojo","resources/iframe_history.html"))+"?"+(new Date()).getTime();
_d=true;
if(_a){
dojo.isSafari?_a.location=url:window.frames[_a.name].location=url;
}else{
}
return url;
};
function checkLocation(){
if(!_e){
var hsl=_c.length;
var _19=getHash();
if((_19===_6||window.location.href==_5)&&(hsl==1)){
handleBackButton();
return;
}
if(_b.length>0){
if(_b[_b.length-1].urlHash===_19){
handleForwardButton();
return;
}
}
if((hsl>=2)&&(_c[hsl-2])){
if(_c[hsl-2].urlHash===_19){
handleBackButton();
return;
}
}
if(dojo.isSafari&&dojo.isSafari<3){
var _1a=history.length;
if(_1a>_4){
handleForwardButton();
}else{
if(_1a<_4){
handleBackButton();
}
}
_4=_1a;
}
}
};
_1.init=function(){
if(dojo.byId("dj_history")){
return;
}
var src=dojo.config["dojoIframeHistoryUrl"]||dojo.moduleUrl("dojo","resources/iframe_history.html");
document.write("<iframe style=\"border:0;width:1px;height:1px;position:absolute;visibility:hidden;bottom:0;right:0;\" name=\"dj_history\" id=\"dj_history\" src=\""+src+"\"></iframe>");
};
_1.setInitialState=function(_1c){
_7=createState(_5,_1c,_6);
};
_1.addToHistory=function(_1d){
_b=[];
var _1e=null;
var url=null;
if(!_a){
if(dojo.config["useXDomain"]&&!dojo.config["dojoIframeHistoryUrl"]){
console.warn("dojo.back: When using cross-domain Dojo builds,"+" please save iframe_history.html to your domain and set djConfig.dojoIframeHistoryUrl"+" to the path on your domain to iframe_history.html");
}
_a=window.frames["dj_history"];
}
if(!_9){
_9=document.createElement("a");
dojo.body().appendChild(_9);
_9.style.display="none";
}
if(_1d["changeUrl"]){
_1e=""+((_1d["changeUrl"]!==true)?_1d["changeUrl"]:(new Date()).getTime());
if(_c.length==0&&_7.urlHash==_1e){
_7=createState(url,_1d,_1e);
return;
}else{
if(_c.length>0&&_c[_c.length-1].urlHash==_1e){
_c[_c.length-1]=createState(url,_1d,_1e);
return;
}
}
_e=true;
setTimeout(function(){
setHash(_1e);
_e=false;
},1);
_9.href=_1e;
if(dojo.isIE){
url=loadIframeHistory();
var _20=_1d["back"]||_1d["backButton"]||_1d["handle"];
var tcb=function(_22){
if(getHash()!=""){
setTimeout(function(){
setHash(_1e);
},1);
}
_20.apply(this,[_22]);
};
if(_1d["back"]){
_1d.back=tcb;
}else{
if(_1d["backButton"]){
_1d.backButton=tcb;
}else{
if(_1d["handle"]){
_1d.handle=tcb;
}
}
}
var _23=_1d["forward"]||_1d["forwardButton"]||_1d["handle"];
var tfw=function(_25){
if(getHash()!=""){
setHash(_1e);
}
if(_23){
_23.apply(this,[_25]);
}
};
if(_1d["forward"]){
_1d.forward=tfw;
}else{
if(_1d["forwardButton"]){
_1d.forwardButton=tfw;
}else{
if(_1d["handle"]){
_1d.handle=tfw;
}
}
}
}else{
if(!dojo.isIE){
if(!_8){
_8=setInterval(checkLocation,200);
}
}
}
}else{
url=loadIframeHistory();
}
_c.push(createState(url,_1d,_1e));
};
_1._iframeLoaded=function(evt,_27){
var _28=getUrlQuery(_27.href);
if(_28==null){
if(_c.length==1){
handleBackButton();
}
return;
}
if(_d){
_d=false;
return;
}
if(_c.length>=2&&_28==getUrlQuery(_c[_c.length-2].url)){
handleBackButton();
}else{
if(_b.length>0&&_28==getUrlQuery(_b[_b.length-1].url)){
handleForwardButton();
}
}
};
})();
}
