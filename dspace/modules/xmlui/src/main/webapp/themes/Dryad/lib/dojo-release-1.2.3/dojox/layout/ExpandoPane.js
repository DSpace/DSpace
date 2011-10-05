/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.layout.ExpandoPane"]){
dojo._hasResource["dojox.layout.ExpandoPane"]=true;
dojo.provide("dojox.layout.ExpandoPane");
dojo.experimental("dojox.layout.ExpandoPane");
dojo.require("dijit.layout.ContentPane");
dojo.require("dijit._Templated");
dojo.require("dijit._Container");
dojo.declare("dojox.layout.ExpandoPane",[dijit.layout.ContentPane,dijit._Templated,dijit._Contained],{maxHeight:"",maxWidth:"",splitter:"",templateString:"<div class=\"dojoxExpandoPane\" dojoAttachEvent=\"ondblclick:toggle\" >\n\t<div dojoAttachPoint=\"titleWrapper\" class=\"dojoxExpandoTitle\">\n\t\t<div class=\"dojoxExpandoIcon\" dojoAttachPoint=\"iconNode\" dojoAttachEvent=\"onclick:toggle\"><span class=\"a11yNode\">X</span></div>\t\t\t\n\t\t<span class=\"dojoxExpandoTitleNode\" dojoAttachPoint=\"titleNode\">${title}</span>\n\t</div>\n\t<div class=\"dojoxExpandoWrapper\" dojoAttachPoint=\"cwrapper\" dojoAttachEvent=\"ondblclick:_trap\">\n\t\t<div class=\"dojoxExpandoContent\" dojoAttachPoint=\"containerNode\"></div>\n\t</div>\n</div>\n",_showing:true,easeOut:"dojo._DefaultEasing",easeIn:"dojo._DefaultEasing",duration:420,startExpanded:true,baseClass:"dijitExpandoPane",postCreate:function(){
this.inherited(arguments);
this._animConnects=[];
this._isHorizontal=true;
if(dojo.isString(this.easeOut)){
this.easeOut=dojo.getObject(this.easeOut);
}
if(dojo.isString(this.easeIn)){
this.easeIn=dojo.getObject(this.easeIn);
}
var _1="";
if(this.region){
switch(this.region){
case "right":
_1="Right";
break;
case "left":
_1="Left";
break;
case "top":
_1="Top";
break;
case "bottom":
_1="Bottom";
break;
}
dojo.addClass(this.domNode,"dojoxExpando"+_1);
this._isHorizontal=/top|bottom/.test(this.region);
}
dojo.style(this.domNode,{overflow:"hidden",padding:0});
},startup:function(){
this.inherited(arguments);
this._container=this.getParent();
this._closedSize=this._titleHeight=dojo.marginBox(this.titleWrapper).h;
if(this.splitter){
var _2=this.id;
dijit.registry.filter(function(w){
return w&&w.child&&w.child.id==_2;
}).forEach(dojo.hitch(this,function(w){
this.connect(w,"_stopDrag","_afterResize");
}));
}
this._currentSize=dojo.marginBox(this.domNode);
this._showSize=this._currentSize[(this._isHorizontal?"h":"w")];
this._setupAnims();
if(this.startExpanded){
this._showing=true;
}else{
this._hideWrapper();
this._hideAnim.gotoPercent(99,true);
}
},_afterResize:function(e){
var _6=this._currentSize;
this._currentSize=dojo.marginBox(this.domNode);
var n=this._currentSize[(this._isHorizontal?"h":"w")];
if(n>this._titleHeight){
if(!this._showing){
this._showing=!this._showing;
this._showEnd();
}
this._showSize=n;
this._setupAnims();
}else{
this._showSize=_6[(this._isHorizontal?"h":"w")];
this._showing=false;
this._hideWrapper();
this._hideAnim.gotoPercent(89,true);
}
},_setupAnims:function(){
dojo.forEach(this._animConnects,dojo.disconnect);
var _8={node:this.domNode,duration:this.duration};
var _9=this._isHorizontal;
var _a={};
var _b={};
var _c=_9?"height":"width";
_a[_c]={end:this._showSize,unit:"px"};
_b[_c]={end:this._closedSize,unit:"px"};
this._showAnim=dojo.animateProperty(dojo.mixin(_8,{easing:this.easeIn,properties:_a}));
this._hideAnim=dojo.animateProperty(dojo.mixin(_8,{easing:this.easeOut,properties:_b}));
this._animConnects=[dojo.connect(this._showAnim,"onEnd",this,"_showEnd"),dojo.connect(this._hideAnim,"onEnd",this,"_hideEnd")];
},toggle:function(){
if(this._showing){
this._hideWrapper();
this._showAnim&&this._showAnim.stop();
this._hideAnim.play();
}else{
this._hideAnim&&this._hideAnim.stop();
this._showAnim.play();
}
this._showing=!this._showing;
},_hideWrapper:function(){
dojo.addClass(this.domNode,"dojoxExpandoClosed");
dojo.style(this.cwrapper,{visibility:"hidden",opacity:"0",overflow:"hidden"});
},_showEnd:function(){
dojo.style(this.cwrapper,{opacity:0,visibility:"visible"});
dojo.fadeIn({node:this.cwrapper,duration:227}).play(1);
dojo.removeClass(this.domNode,"dojoxExpandoClosed");
setTimeout(dojo.hitch(this._container,"layout"),15);
},_hideEnd:function(){
setTimeout(dojo.hitch(this._container,"layout"),15);
},resize:function(){
var _d=dojo.marginBox(this.domNode),h=_d.h-this._titleHeight;
dojo.style(this.containerNode,"height",h+"px");
this.inherited(arguments);
},_trap:function(e){
dojo.stopEvent(e);
}});
}
