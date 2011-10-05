/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.widget.Dialog"]){
dojo._hasResource["dojox.widget.Dialog"]=true;
dojo.provide("dojox.widget.Dialog");
dojo.experimental("dojox.widget.Dialog");
dojo.require("dijit.Dialog");
dojo.require("dojox.fx");
dojo.declare("dojox.widget.Dialog",dijit.Dialog,{templateString:"<div class=\"dojoxDialog\" tabindex=\"-1\" waiRole=\"dialog\" waiState=\"labelledby-${id}_title\">\n\t<div dojoAttachPoint=\"titleBar\" class=\"dojoxDialogTitleBar\">\n\t\t<span dojoAttachPoint=\"titleNode\" class=\"dojoxDialogTitle\" id=\"${id}_title\">${title}</span>\n\t</div>\n\t<div dojoAttachPoint=\"dojoxDialogWrapper\">\n\t\t<div dojoAttachPoint=\"containerNode\" class=\"dojoxDialogPaneContent\"></div>\n\t</div>\n\t<div dojoAttachPoint=\"closeButtonNode\" class=\"dojoxDialogCloseIcon\" dojoAttachEvent=\"onclick: onCancel\">\n\t\t\t<span dojoAttachPoint=\"closeText\" class=\"closeText\">x</span>\n\t</div>\n</div>\n",fixedSize:false,viewportPadding:35,dimensions:null,easing:null,sizeDuration:dijit._defaultDuration,sizeMethod:"chain",showTitle:false,draggable:false,constructor:function(_1,_2){
this.easing=_1.easing||dojo._defaultEasing;
this.dimensions=_1.dimensions||[300,300];
},_setup:function(){
this.inherited(arguments);
if(!this._alreadyInitialized){
this.connect(this._underlay.domNode,"onclick","onCancel");
this._navIn=dojo.fadeIn({node:this.closeButtonNode});
this._navOut=dojo.fadeOut({node:this.closeButtonNode});
if(!this.showTitle){
dojo.addClass(this.domNode,"dojoxDialogNoTitle");
}
}
},layout:function(e){
this._setSize();
this.inherited(arguments);
},_setSize:function(){
this._vp=dijit.getViewport();
var tc=this.containerNode;
var _5=this.fixedSize;
this._displaysize={w:_5?tc.scrollWidth:this.dimensions[0],h:_5?tc.scrollHeight:this.dimensions[1]};
},show:function(){
this._setSize();
dojo.style(this.closeButtonNode,"opacity",0);
dojo.style(this.domNode,{overflow:"hidden",opacity:0,width:"1px",height:"1px"});
dojo.style(this.containerNode,{opacity:0,overflow:"hidden"});
this.inherited(arguments);
this._modalconnects.push(dojo.connect(this.domNode,"onmouseenter",this,"_handleNav"));
this._modalconnects.push(dojo.connect(this.domNode,"onmouseleave",this,"_handleNav"));
},_handleNav:function(e){
var _7="_navOut";
var _8="_navIn";
var _9=(e.type=="mouseout"?_8:_7);
var _a=(e.type=="mouseout"?_7:_8);
this[_9].stop();
this[_a].play();
},_position:function(){
if(this._sizing){
this._sizing.stop();
this.disconnect(this._sizingConnect);
}
this.inherited(arguments);
if(!this.open){
dojo.style(this.containerNode,"opacity",0);
}
var _b=this.viewportPadding*2;
var _c={node:this.domNode,duration:this.sizeDuration||dijit._defaultDuration,easing:this.easing,method:this.sizeMethod};
var ds=this._displaysize;
_c["width"]=ds.w=(ds.w+_b>=this._vp.w||this.fixedSize)?this._vp.w-_b:ds.w;
_c["height"]=ds.h=(ds.h+_b>=this._vp.h||this.fixedSize)?this._vp.h-_b:ds.h;
this._sizing=dojox.fx.sizeTo(_c);
this._sizingConnect=this.connect(this._sizing,"onEnd","_showContent");
this._sizing.play();
},_showContent:function(e){
var _f=this.containerNode;
dojo.style(this.domNode,"overflow","visible");
dojo.style(_f,{height:this._displaysize.h+"px",width:this._displaysize.w+"px",overflow:"auto"});
dojo.anim(_f,{opacity:1});
}});
}
