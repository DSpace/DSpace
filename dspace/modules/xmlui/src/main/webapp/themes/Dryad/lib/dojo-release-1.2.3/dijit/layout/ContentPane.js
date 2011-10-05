/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.layout.ContentPane"]){
dojo._hasResource["dijit.layout.ContentPane"]=true;
dojo.provide("dijit.layout.ContentPane");
dojo.require("dijit._Widget");
dojo.require("dijit.layout._LayoutWidget");
dojo.require("dojo.parser");
dojo.require("dojo.string");
dojo.require("dojo.html");
dojo.requireLocalization("dijit","loading",null,"zh,ca,pt,da,tr,ru,ROOT,de,sv,ja,he,fi,nb,el,ar,pt-pt,cs,fr,es,ko,nl,zh-tw,pl,th,it,hu,sk,sl");
dojo.declare("dijit.layout.ContentPane",dijit._Widget,{href:"",extractContent:false,parseOnLoad:true,preventCache:false,preload:false,refreshOnShow:false,loadingMessage:"<span class='dijitContentPaneLoading'>${loadingState}</span>",errorMessage:"<span class='dijitContentPaneError'>${errorState}</span>",isLoaded:false,baseClass:"dijitContentPane",doLayout:true,_isRealContent:true,postMixInProperties:function(){
this.inherited(arguments);
var _1=dojo.i18n.getLocalization("dijit","loading",this.lang);
this.loadingMessage=dojo.string.substitute(this.loadingMessage,_1);
this.errorMessage=dojo.string.substitute(this.errorMessage,_1);
},buildRendering:function(){
this.inherited(arguments);
if(!this.containerNode){
this.containerNode=this.domNode;
}
},postCreate:function(){
this.domNode.title="";
if(!dijit.hasWaiRole(this.domNode)){
dijit.setWaiRole(this.domNode,"group");
}
dojo.addClass(this.domNode,this.baseClass);
},startup:function(){
if(this._started){
return;
}
if(this.doLayout!="false"&&this.doLayout!==false){
this._checkIfSingleChild();
if(this._singleChild){
this._singleChild.startup();
}
}
this._loadCheck();
this.inherited(arguments);
},_checkIfSingleChild:function(){
var _2=dojo.query(">",this.containerNode),_3=_2.filter(function(_4){
return dojo.hasAttr(_4,"dojoType")||dojo.hasAttr(_4,"widgetId");
}),_5=dojo.filter(_3.map(dijit.byNode),function(_6){
return _6&&_6.domNode&&_6.resize;
});
if(_2.length==_3.length&&_5.length==1){
this.isContainer=true;
this._singleChild=_5[0];
}else{
delete this.isContainer;
delete this._singleChild;
}
},refresh:function(){
return this._prepareLoad(true);
},setHref:function(_7){
dojo.deprecated("dijit.layout.ContentPane.setHref() is deprecated.\tUse attr('href', ...) instead.","","2.0");
return this.attr("href",_7);
},_setHrefAttr:function(_8){
this.href=_8;
if(this._created){
return this._prepareLoad();
}
},setContent:function(_9){
dojo.deprecated("dijit.layout.ContentPane.setContent() is deprecated.  Use attr('content', ...) instead.","","2.0");
this.attr("content",_9);
},_setContentAttr:function(_a){
this.href="";
this.cancel();
this._setContent(_a||"");
this._isDownloaded=false;
if(this.doLayout!="false"&&this.doLayout!==false){
this._checkIfSingleChild();
if(this._singleChild&&this._singleChild.resize){
this._singleChild.startup();
var cb=this._contentBox||dojo.contentBox(this.containerNode);
this._singleChild.resize({w:cb.w,h:cb.h});
}
}
},_getContentAttr:function(){
return this.containerNode.innerHTML;
},cancel:function(){
if(this._xhrDfd&&(this._xhrDfd.fired==-1)){
this._xhrDfd.cancel();
}
delete this._xhrDfd;
},destroyRecursive:function(_c){
if(this._beingDestroyed){
return;
}
this._beingDestroyed=true;
this.inherited(arguments);
},resize:function(_d){
dojo.marginBox(this.domNode,_d);
var _e=this.containerNode,mb=dojo.mixin(dojo.marginBox(_e),_d||{});
var cb=this._contentBox=dijit.layout.marginBox2contentBox(_e,mb);
if(this._singleChild&&this._singleChild.resize){
this._singleChild.resize({w:cb.w,h:cb.h});
}
},_prepareLoad:function(_11){
this.cancel();
this.isLoaded=false;
this._loadCheck(_11);
},_isShown:function(){
if("open" in this){
return this.open;
}else{
var _12=this.domNode;
return (_12.style.display!="none")&&(_12.style.visibility!="hidden");
}
},_loadCheck:function(_13){
var _14=this._isShown();
if(this.href&&(_13||(this.preload&&!this.isLoaded&&!this._xhrDfd)||(this.refreshOnShow&&_14&&!this._xhrDfd)||(!this.isLoaded&&_14&&!this._xhrDfd))){
this._downloadExternalContent();
}
},_downloadExternalContent:function(){
this._setContent(this.onDownloadStart(),true);
var _15=this;
var _16={preventCache:(this.preventCache||this.refreshOnShow),url:this.href,handleAs:"text"};
if(dojo.isObject(this.ioArgs)){
dojo.mixin(_16,this.ioArgs);
}
var _17=this._xhrDfd=(this.ioMethod||dojo.xhrGet)(_16);
_17.addCallback(function(_18){
try{
_15._isDownloaded=true;
_15._setContent(_18,false);
_15.onDownloadEnd();
}
catch(err){
_15._onError("Content",err);
}
delete _15._xhrDfd;
return _18;
});
_17.addErrback(function(err){
if(!_17.cancelled){
_15._onError("Download",err);
}
delete _15._xhrDfd;
return err;
});
},_onLoadHandler:function(_1a){
this.isLoaded=true;
try{
this.onLoad(_1a);
}
catch(e){
console.error("Error "+this.widgetId+" running custom onLoad code");
}
},_onUnloadHandler:function(){
this.isLoaded=false;
try{
this.onUnload();
}
catch(e){
console.error("Error "+this.widgetId+" running custom onUnload code");
}
},destroyDescendants:function(){
if(this._isRealContent){
this._onUnloadHandler();
}
var _1b=this._contentSetter;
if(_1b){
_1b.empty();
}else{
this.inherited(arguments);
dojo.html._emptyNode(this.containerNode);
}
},_setContent:function(_1c,_1d){
this.destroyDescendants();
this._isRealContent=!_1d;
var _1e=this._contentSetter;
if(!(_1e&&_1e instanceof dojo.html._ContentSetter)){
_1e=this._contentSetter=new dojo.html._ContentSetter({node:this.containerNode,_onError:dojo.hitch(this,this._onError),onContentError:dojo.hitch(this,function(e){
var _20=this.onContentError(e);
try{
this.containerNode.innerHTML=_20;
}
catch(e){
console.error("Fatal "+this.id+" could not change content due to "+e.message,e);
}
})});
}
var _21=dojo.mixin({cleanContent:this.cleanContent,extractContent:this.extractContent,parseContent:this.parseOnLoad},this._contentSetterParams||{});
dojo.mixin(_1e,_21);
_1e.set((dojo.isObject(_1c)&&_1c.domNode)?_1c.domNode:_1c);
delete this._contentSetterParams;
if(!_1d){
this._onLoadHandler(_1c);
}
},_onError:function(_22,err,_24){
var _25=this["on"+_22+"Error"].call(this,err);
if(_24){
console.error(_24,err);
}else{
if(_25){
this._setContent(_25,true);
}
}
},_createSubWidgets:function(){
try{
dojo.parser.parse(this.containerNode,true);
}
catch(e){
this._onError("Content",e,"Couldn't create widgets in "+this.id+(this.href?" from "+this.href:""));
}
},onLoad:function(_26){
},onUnload:function(){
},onDownloadStart:function(){
return this.loadingMessage;
},onContentError:function(_27){
},onDownloadError:function(_28){
return this.errorMessage;
},onDownloadEnd:function(){
}});
}
