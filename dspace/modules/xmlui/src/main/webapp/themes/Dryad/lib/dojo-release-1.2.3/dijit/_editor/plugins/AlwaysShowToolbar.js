/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._editor.plugins.AlwaysShowToolbar"]){
dojo._hasResource["dijit._editor.plugins.AlwaysShowToolbar"]=true;
dojo.provide("dijit._editor.plugins.AlwaysShowToolbar");
dojo.declare("dijit._editor.plugins.AlwaysShowToolbar",dijit._editor._Plugin,{_handleScroll:true,setEditor:function(e){
if(!e.iframe){

return;
}
this.editor=e;
e.onLoadDeferred.addCallback(dojo.hitch(this,this.enable));
},enable:function(d){
this._updateHeight();
this.connect(window,"onscroll","globalOnScrollHandler");
this.connect(this.editor,"onNormalizedDisplayChanged","_updateHeight");
return d;
},_updateHeight:function(){
var e=this.editor;
if(!e.isLoaded){
return;
}
if(e.height){
return;
}
var _4=dojo.marginBox(e.editNode).h;
if(dojo.isOpera){
_4=e.editNode.scrollHeight;
}
if(!_4){
_4=dojo.marginBox(e.document.body).h;
}
if(_4==0){

return;
}
if(_4!=this._lastHeight){
this._lastHeight=_4;
dojo.marginBox(e.iframe,{h:this._lastHeight});
}
},_lastHeight:0,globalOnScrollHandler:function(){
var _5=dojo.isIE<7;
if(!this._handleScroll){
return;
}
var _6=this.editor.toolbar.domNode;
var db=dojo.body;
if(!this._scrollSetUp){
this._scrollSetUp=true;
this._scrollThreshold=dojo._abs(_6,true).y;
}
var _8=dojo._docScroll().y;
var s=_6.style;
if(_8>this._scrollThreshold&&_8<this._scrollThreshold+this._lastHeight){
if(!this._fixEnabled){
var _a=dojo.marginBox(_6);
this.editor.iframe.style.marginTop=_a.h+"px";
if(_5){
s.left=dojo._abs(_6).x;
if(_6.previousSibling){
this._IEOriginalPos=["after",_6.previousSibling];
}else{
if(_6.nextSibling){
this._IEOriginalPos=["before",_6.nextSibling];
}else{
this._IEOriginalPos=["last",_6.parentNode];
}
}
dojo.body().appendChild(_6);
dojo.addClass(_6,"dijitIEFixedToolbar");
}else{
s.position="fixed";
s.top="0px";
}
dojo.marginBox(_6,{w:_a.w});
s.zIndex=2000;
this._fixEnabled=true;
}
var _b=(this.height)?parseInt(this.editor.height):this.editor._lastHeight;
s.display=(_8>this._scrollThreshold+_b)?"none":"";
}else{
if(this._fixEnabled){
this.editor.iframe.style.marginTop="";
s.position="";
s.top="";
s.zIndex="";
s.display="";
if(_5){
s.left="";
dojo.removeClass(_6,"dijitIEFixedToolbar");
if(this._IEOriginalPos){
dojo.place(_6,this._IEOriginalPos[1],this._IEOriginalPos[0]);
this._IEOriginalPos=null;
}else{
dojo.place(_6,this.editor.iframe,"before");
}
}
s.width="";
this._fixEnabled=false;
}
}
},destroy:function(){
this._IEOriginalPos=null;
this._handleScroll=false;
dojo.forEach(this._connects,dojo.disconnect);
if(dojo.isIE<7){
dojo.removeClass(this.editor.toolbar.domNode,"dijitIEFixedToolbar");
}
}});
}
