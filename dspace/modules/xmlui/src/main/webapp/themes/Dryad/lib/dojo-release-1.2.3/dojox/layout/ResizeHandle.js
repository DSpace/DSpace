/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.layout.ResizeHandle"]){
dojo._hasResource["dojox.layout.ResizeHandle"]=true;
dojo.provide("dojox.layout.ResizeHandle");
dojo.experimental("dojox.layout.ResizeHandle");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dojo.fx");
dojo.declare("dojox.layout.ResizeHandle",[dijit._Widget,dijit._Templated],{targetId:"",targetContainer:null,resizeAxis:"xy",activeResize:false,activeResizeClass:"dojoxResizeHandleClone",animateSizing:true,animateMethod:"chain",animateDuration:225,minHeight:100,minWidth:100,templateString:"<div dojoAttachPoint=\"resizeHandle\" class=\"dojoxResizeHandle\"><div></div></div>",postCreate:function(){
this.connect(this.resizeHandle,"onmousedown","_beginSizing");
if(!this.activeResize){
this._resizeHelper=dijit.byId("dojoxGlobalResizeHelper");
if(!this._resizeHelper){
var _1=document.createElement("div");
_1.style.display="none";
dojo.body().appendChild(_1);
dojo.addClass(_1,this.activeResizeClass);
this._resizeHelper=new dojox.layout._ResizeHelper({id:"dojoxGlobalResizeHelper"},_1);
this._resizeHelper.startup();
}
}else{
this.animateSizing=false;
}
if(!this.minSize){
this.minSize={w:this.minWidth,h:this.minHeight};
}
this._resizeX=this._resizeY=false;
switch(this.resizeAxis.toLowerCase()){
case "xy":
this._resizeX=this._resizeY=true;
dojo.addClass(this.resizeHandle,"dojoxResizeNW");
break;
case "x":
this._resizeX=true;
dojo.addClass(this.resizeHandle,"dojoxResizeW");
break;
case "y":
this._resizeY=true;
dojo.addClass(this.resizeHandle,"dojoxResizeN");
break;
}
},_beginSizing:function(e){
if(this._isSizing){
return false;
}
this.targetWidget=dijit.byId(this.targetId);
this.targetDomNode=this.targetWidget?this.targetWidget.domNode:dojo.byId(this.targetId);
if(this.targetContainer){
this.targetDomNode=this.targetContainer;
}
if(!this.targetDomNode){
return false;
}
if(!this.activeResize){
var c=dojo.coords(this.targetDomNode,true);
this._resizeHelper.resize({l:c.x,t:c.y,w:c.w,h:c.h});
this._resizeHelper.show();
}
this._isSizing=true;
this.startPoint={"x":e.clientX,"y":e.clientY};
var mb=(this.targetWidget)?dojo.marginBox(this.targetDomNode):dojo.contentBox(this.targetDomNode);
this.startSize={"w":mb.w,"h":mb.h};
this._pconnects=[];
this._pconnects.push(dojo.connect(document,"onmousemove",this,"_updateSizing"));
this._pconnects.push(dojo.connect(document,"onmouseup",this,"_endSizing"));
e.preventDefault();
},_updateSizing:function(e){
if(this.activeResize){
this._changeSizing(e);
}else{
var _6=this._getNewCoords(e);
if(_6===false){
return;
}
this._resizeHelper.resize(_6);
}
e.preventDefault();
},_getNewCoords:function(e){
try{
if(!e.clientX||!e.clientY){
return false;
}
}
catch(e){
return false;
}
this._activeResizeLastEvent=e;
var dx=this.startPoint.x-e.clientX;
var dy=this.startPoint.y-e.clientY;
var _a=(this._resizeX)?this.startSize.w-dx:this.startSize.w;
var _b=(this._resizeY)?this.startSize.h-dy:this.startSize.h;
if(this.minSize){
if(_a<this.minSize.w){
_a=this.minSize.w;
}
if(_b<this.minSize.h){
_b=this.minSize.h;
}
}
return {w:_a,h:_b};
},_changeSizing:function(e){
var _d=this._getNewCoords(e);
if(_d===false){
return;
}
if(this.targetWidget&&typeof this.targetWidget.resize=="function"){
this.targetWidget.resize(_d);
}else{
if(this.animateSizing){
var _e=dojo.fx[this.animateMethod]([dojo.animateProperty({node:this.targetDomNode,properties:{width:{start:this.startSize.w,end:_d.w,unit:"px"}},duration:this.animateDuration}),dojo.animateProperty({node:this.targetDomNode,properties:{height:{start:this.startSize.h,end:_d.h,unit:"px"}},duration:this.animateDuration})]);
_e.play();
}else{
dojo.style(this.targetDomNode,"width",_d.w+"px");
dojo.style(this.targetDomNode,"height",_d.h+"px");
}
}
},_endSizing:function(e){
dojo.forEach(this._pconnects,dojo.disconnect);
if(!this.activeResize){
this._resizeHelper.hide();
this._changeSizing(e);
}
this._isSizing=false;
this.onResize(e);
},onResize:function(e){
}});
dojo.declare("dojox.layout._ResizeHelper",dijit._Widget,{startup:function(){
if(this._started){
return;
}
this.inherited(arguments);
},show:function(){
dojo.fadeIn({node:this.domNode,duration:120,beforeBegin:dojo.hitch(this,function(){
this.domNode.style.display="";
})}).play();
},hide:function(){
dojo.fadeOut({node:this.domNode,duration:250,onEnd:dojo.hitch(this,function(){
this.domNode.style.display="none";
})}).play();
},resize:function(dim){
dojo.marginBox(this.domNode,dim);
}});
}
