/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.io.iframe"]){
dojo._hasResource["dojo.io.iframe"]=true;
dojo.provide("dojo.io.iframe");
dojo.io.iframe={create:function(_1,_2,_3){
if(window[_1]){
return window[_1];
}
if(window.frames[_1]){
return window.frames[_1];
}
var _4=null;
var _5=_3;
if(!_5){
if(dojo.config["useXDomain"]&&!dojo.config["dojoBlankHtmlUrl"]){
console.warn("dojo.io.iframe.create: When using cross-domain Dojo builds,"+" please save dojo/resources/blank.html to your domain and set djConfig.dojoBlankHtmlUrl"+" to the path on your domain to blank.html");
}
_5=(dojo.config["dojoBlankHtmlUrl"]||dojo.moduleUrl("dojo","resources/blank.html"));
}
var _6=dojo.isIE?"<iframe name=\""+_1+"\" src=\""+_5+"\" onload=\""+_2+"\">":"iframe";
_4=dojo.doc.createElement(_6);
with(_4){
name=_1;
setAttribute("name",_1);
id=_1;
}
dojo.body().appendChild(_4);
window[_1]=_4;
with(_4.style){
if(dojo.isSafari<3){
position="absolute";
}
left=top="1px";
height=width="1px";
visibility="hidden";
}
if(!dojo.isIE){
this.setSrc(_4,_5,true);
_4.onload=new Function(_2);
}
return _4;
},setSrc:function(_7,_8,_9){
try{
if(!_9){
if(dojo.isSafari){
_7.location=_8;
}else{
frames[_7.name].location=_8;
}
}else{
var _a;
if(dojo.isIE||dojo.isSafari>2){
_a=_7.contentWindow.document;
}else{
if(dojo.isSafari){
_a=_7.document;
}else{
_a=_7.contentWindow;
}
}
if(!_a){
_7.location=_8;
return;
}else{
_a.location.replace(_8);
}
}
}
catch(e){

}
},doc:function(_b){
var _c=_b.contentDocument||(((_b.name)&&(_b.document)&&(document.getElementsByTagName("iframe")[_b.name].contentWindow)&&(document.getElementsByTagName("iframe")[_b.name].contentWindow.document)))||((_b.name)&&(document.frames[_b.name])&&(document.frames[_b.name].document))||null;
return _c;
},send:function(_d){
if(!this["_frame"]){
this._frame=this.create(this._iframeName,dojo._scopeName+".io.iframe._iframeOnload();");
}
var _e=dojo._ioSetArgs(_d,function(_f){
_f.canceled=true;
_f.ioArgs._callNext();
},function(dfd){
var _11=null;
try{
var _12=dfd.ioArgs;
var dii=dojo.io.iframe;
var ifd=dii.doc(dii._frame);
var _15=_12.handleAs;
_11=ifd;
if(_15!="html"){
if(_15=="xml"){
if(dojo.isIE){
dojo.query("a",dii._frame.contentWindow.document.documentElement).orphan();
var _16=(dii._frame.contentWindow.document).documentElement.innerText;
_16=_16.replace(/>\s+</g,"><");
if(!this._ieXmlDom){
for(var i=0,a=["MSXML2","Microsoft","MSXML","MSXML3"],l=a.length;i<l;i++){
try{
var _1a=new ActiveXObject(a[i]+".XmlDom");
this._ieXmlDom=a[i]+".XmlDom";
break;
}
catch(e){
}
}
if(!this._ieXmlDom){
throw new Error("dojo.io.iframe.send (return handler): your copy of Internet Explorer does not support XML documents.");
}
}
var _1b=new ActiveXObject(this._ieXmlDom);
_1b.async=false;
_1b.loadXML(_16);
_11=_1b;
}
}else{
_11=ifd.getElementsByTagName("textarea")[0].value;
if(_15=="json"){
_11=dojo.fromJson(_11);
}else{
if(_15=="javascript"){
_11=dojo.eval(_11);
}
}
}
}
}
catch(e){
_11=e;
}
finally{
_12._callNext();
}
return _11;
},function(_1c,dfd){
dfd.ioArgs._hasError=true;
dfd.ioArgs._callNext();
return _1c;
});
_e.ioArgs._callNext=function(){
if(!this["_calledNext"]){
this._calledNext=true;
dojo.io.iframe._currentDfd=null;
dojo.io.iframe._fireNextRequest();
}
};
this._dfdQueue.push(_e);
this._fireNextRequest();
dojo._ioWatch(_e,function(dfd){
return !dfd.ioArgs["_hasError"];
},function(dfd){
return (!!dfd.ioArgs["_finished"]);
},function(dfd){
if(dfd.ioArgs._finished){
dfd.callback(dfd);
}else{
dfd.errback(new Error("Invalid dojo.io.iframe request state"));
}
});
return _e;
},_currentDfd:null,_dfdQueue:[],_iframeName:dojo._scopeName+"IoIframe",_fireNextRequest:function(){
try{
if((this._currentDfd)||(this._dfdQueue.length==0)){
return;
}
var dfd=this._currentDfd=this._dfdQueue.shift();
var _22=dfd.ioArgs;
var _23=_22.args;
_22._contentToClean=[];
var fn=dojo.byId(_23["form"]);
var _25=_23["content"]||{};
if(fn){
if(_25){
for(var x in _25){
if(!fn[x]){
var tn;
if(dojo.isIE){
tn=dojo.doc.createElement("<input type='hidden' name='"+x+"'>");
}else{
tn=dojo.doc.createElement("input");
tn.type="hidden";
tn.name=x;
}
tn.value=_25[x];
fn.appendChild(tn);
_22._contentToClean.push(x);
}else{
fn[x].value=_25[x];
}
}
}
var _28=fn.getAttributeNode("action");
var _29=fn.getAttributeNode("method");
var _2a=fn.getAttributeNode("target");
if(_23["url"]){
_22._originalAction=_28?_28.value:null;
if(_28){
_28.value=_23.url;
}else{
fn.setAttribute("action",_23.url);
}
}
if(!_29||!_29.value){
if(_29){
_29.value=(_23["method"])?_23["method"]:"post";
}else{
fn.setAttribute("method",(_23["method"])?_23["method"]:"post");
}
}
_22._originalTarget=_2a?_2a.value:null;
if(_2a){
_2a.value=this._iframeName;
}else{
fn.setAttribute("target",this._iframeName);
}
fn.target=this._iframeName;
fn.submit();
}else{
var _2b=_23.url+(_23.url.indexOf("?")>-1?"&":"?")+_22.query;
this.setSrc(this._frame,_2b,true);
}
}
catch(e){
dfd.errback(e);
}
},_iframeOnload:function(){
var dfd=this._currentDfd;
if(!dfd){
this._fireNextRequest();
return;
}
var _2d=dfd.ioArgs;
var _2e=_2d.args;
var _2f=dojo.byId(_2e.form);
if(_2f){
var _30=_2d._contentToClean;
for(var i=0;i<_30.length;i++){
var key=_30[i];
if(dojo.isSafari<3){
for(var j=0;j<_2f.childNodes.length;j++){
var _34=_2f.childNodes[j];
if(_34.name==key){
dojo._destroyElement(_34);
break;
}
}
}else{
dojo._destroyElement(_2f[key]);
_2f[key]=null;
}
}
if(_2d["_originalAction"]){
_2f.setAttribute("action",_2d._originalAction);
}
if(_2d["_originalTarget"]){
_2f.setAttribute("target",_2d._originalTarget);
_2f.target=_2d._originalTarget;
}
}
_2d._finished=true;
}};
}
