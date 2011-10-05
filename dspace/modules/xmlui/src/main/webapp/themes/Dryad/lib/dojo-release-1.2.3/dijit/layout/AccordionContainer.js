/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.layout.AccordionContainer"]){
dojo._hasResource["dijit.layout.AccordionContainer"]=true;
dojo.provide("dijit.layout.AccordionContainer");
dojo.require("dojo.fx");
dojo.require("dijit._Container");
dojo.require("dijit._Templated");
dojo.require("dijit.layout.StackContainer");
dojo.require("dijit.layout.ContentPane");
dojo.declare("dijit.layout.AccordionContainer",dijit.layout.StackContainer,{duration:dijit.defaultDuration,_verticalSpace:0,baseClass:"dijitAccordionContainer",postCreate:function(){
this.domNode.style.overflow="hidden";
this.inherited(arguments);
dijit.setWaiRole(this.domNode,"tablist");
},startup:function(){
if(this._started){
return;
}
this.inherited(arguments);
if(this.selectedChildWidget){
var _1=this.selectedChildWidget.containerNode.style;
_1.display="";
_1.overflow="auto";
this.selectedChildWidget._setSelectedState(true);
}
},_getTargetHeight:function(_2){
var cs=dojo.getComputedStyle(_2);
return Math.max(this._verticalSpace-dojo._getPadBorderExtents(_2,cs).h,0);
},layout:function(){
var _4=0;
var _5=this.selectedChildWidget;
dojo.forEach(this.getChildren(),function(_6){
_4+=_6.getTitleHeight();
});
var _7=this._contentBox;
this._verticalSpace=_7.h-_4;
if(_5){
_5.containerNode.style.height=this._getTargetHeight(_5.containerNode)+"px";
}
},_setupChild:function(_8){
return _8;
},_transition:function(_9,_a){
if(this._inTransition){
return;
}
this._inTransition=true;
var _b=[];
var _c=this._verticalSpace;
if(_9){
_9.setSelected(true);
var _d=_9.containerNode;
_d.style.display="";
_c=this._getTargetHeight(_9.containerNode);
_b.push(dojo.animateProperty({node:_d,duration:this.duration,properties:{height:{start:1,end:_c}},onEnd:function(){
_d.style.overflow="auto";
}}));
}
if(_a){
_a.setSelected(false);
var _e=_a.containerNode;
_e.style.overflow="hidden";
_c=this._getTargetHeight(_a.containerNode);
_b.push(dojo.animateProperty({node:_e,duration:this.duration,properties:{height:{start:_c,end:"1"}},onEnd:function(){
_e.style.display="none";
}}));
}
this._inTransition=false;
dojo.fx.combine(_b).play();
},_onKeyPress:function(e){
if(this.disabled||e.altKey||!(e._dijitWidget||e.ctrlKey)){
return;
}
var k=dojo.keys;
var _11=e._dijitWidget;
switch(e.charOrCode){
case k.LEFT_ARROW:
case k.UP_ARROW:
if(_11){
this._adjacent(false)._onTitleClick();
dojo.stopEvent(e);
}
break;
case k.PAGE_UP:
if(e.ctrlKey){
this._adjacent(false)._onTitleClick();
dojo.stopEvent(e);
}
break;
case k.RIGHT_ARROW:
case k.DOWN_ARROW:
if(_11){
this._adjacent(true)._onTitleClick();
dojo.stopEvent(e);
}
break;
case k.PAGE_DOWN:
if(e.ctrlKey){
this._adjacent(true)._onTitleClick();
dojo.stopEvent(e);
}
break;
default:
if(e.ctrlKey&&e.charOrCode===k.TAB){
this._adjacent(e._dijitWidget,!e.shiftKey)._onTitleClick();
dojo.stopEvent(e);
}
}
}});
dojo.declare("dijit.layout.AccordionPane",[dijit.layout.ContentPane,dijit._Templated,dijit._Contained],{templateString:"<div waiRole=\"presentation\"\n\t><div dojoAttachPoint='titleNode,focusNode' dojoAttachEvent='ondijitclick:_onTitleClick,onkeypress:_onTitleKeyPress,onfocus:_handleFocus,onblur:_handleFocus,onmouseenter:_onTitleEnter,onmouseleave:_onTitleLeave'\n\t\tclass='dijitAccordionTitle' wairole=\"tab\" waiState=\"expanded-false\"\n\t\t><span class='dijitInline dijitAccordionArrow' waiRole=\"presentation\"></span\n\t\t><span class='arrowTextUp' waiRole=\"presentation\">+</span\n\t\t><span class='arrowTextDown' waiRole=\"presentation\">-</span\n\t\t><span waiRole=\"presentation\" dojoAttachPoint='titleTextNode' class='dijitAccordionText'></span></div\n\t><div waiRole=\"presentation\"><div dojoAttachPoint='containerNode' style='overflow: hidden; height: 1px; display: none'\n\t\tclass='dijitAccordionBody' wairole=\"tabpanel\"\n\t></div></div>\n</div>\n",attributeMap:dojo.mixin(dojo.clone(dijit.layout.ContentPane.prototype.attributeMap),{title:{node:"titleTextNode",type:"innerHTML"}}),baseClass:"dijitAccordionPane",postCreate:function(){
this.inherited(arguments);
dojo.setSelectable(this.titleNode,false);
this.setSelected(this.selected);
dojo.attr(this.titleTextNode,"id",this.domNode.id+"_title");
dijit.setWaiState(this.focusNode,"labelledby",dojo.attr(this.titleTextNode,"id"));
},getTitleHeight:function(){
return dojo.marginBox(this.titleNode).h;
},_onTitleClick:function(){
var _12=this.getParent();
if(!_12._inTransition){
_12.selectChild(this);
dijit.focus(this.focusNode);
}
},_onTitleEnter:function(){
dojo.addClass(this.focusNode,"dijitAccordionTitle-hover");
},_onTitleLeave:function(){
dojo.removeClass(this.focusNode,"dijitAccordionTitle-hover");
},_onTitleKeyPress:function(evt){
evt._dijitWidget=this;
return this.getParent()._onKeyPress(evt);
},_setSelectedState:function(_14){
this.selected=_14;
dojo[(_14?"addClass":"removeClass")](this.titleNode,"dijitAccordionTitle-selected");
dijit.setWaiState(this.focusNode,"expanded",_14);
this.focusNode.setAttribute("tabIndex",_14?"0":"-1");
},_handleFocus:function(e){
dojo[(e.type=="focus"?"addClass":"removeClass")](this.focusNode,"dijitAccordionFocused");
},setSelected:function(_16){
this._setSelectedState(_16);
if(_16){
this.onSelected();
this._loadCheck();
}
},onSelected:function(){
}});
}
