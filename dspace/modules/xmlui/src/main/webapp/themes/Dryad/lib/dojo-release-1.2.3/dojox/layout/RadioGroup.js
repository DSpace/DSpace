/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.layout.RadioGroup"]){
dojo._hasResource["dojox.layout.RadioGroup"]=true;
dojo.provide("dojox.layout.RadioGroup");
dojo.experimental("dojox.layout.RadioGroup");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dijit._Container");
dojo.require("dijit.layout.StackContainer");
dojo.require("dojo.fx.easing");
dojo.declare("dojox.layout.RadioGroup",[dijit.layout.StackContainer,dijit._Templated],{duration:750,hasButtons:false,buttonClass:"dojox.layout._RadioButton",templateString:"<div class=\"dojoxRadioGroup\">"+" \t<div dojoAttachPoint=\"buttonHolder\" style=\"display:none;\">"+"\t\t<table class=\"dojoxRadioButtons\"><tbody><tr class=\"dojoxRadioButtonRow\" dojoAttachPoint=\"buttonNode\"></tr></tbody></table>"+"\t</div>"+"\t<div class=\"dojoxRadioView\" dojoAttachPoint=\"containerNode\"></div>"+"</div>",startup:function(){
this.inherited(arguments);
this._children=this.getChildren();
this._buttons=this._children.length;
this._size=dojo.coords(this.containerNode);
if(this.hasButtons){
dojo.style(this.buttonHolder,"display","block");
dojo.forEach(this._children,this._makeButton,this);
}
},_makeButton:function(_1){
dojo.style(_1.domNode,"position","absolute");
var _2=dojo.doc.createElement("td");
this.buttonNode.appendChild(_2);
var _3=_2.appendChild(dojo.doc.createElement("div"));
var _4=dojo.getObject(this.buttonClass);
var _5=new _4({label:_1.title,page:_1},_3);
dojo.mixin(_1,{_radioButton:_5});
_5.startup();
},addChild:function(_6){
this.inherited(arguments);
if(this.hasButtons){
this._makeButton(_6);
}
},removeChild:function(_7){
if(this.hasButtons&&_7._radioButton){
_7._radioButton.destroy();
delete _7._radioButton;
}
this.inherited(arguments);
},_transition:function(_8,_9){
this._showChild(_8);
if(_9){
this._hideChild(_9);
}
if(this.doLayout&&_8.resize){
_8.resize(this._containerContentBox||this._contentBox);
}
},_showChild:function(_a){
var _b=this.getChildren();
_a.isFirstChild=(_a==_b[0]);
_a.isLastChild=(_a==_b[_b.length-1]);
_a.selected=true;
_a.domNode.style.display="";
if(_a._loadCheck){
_a._loadCheck();
}
if(_a.onShow){
_a.onShow();
}
},_hideChild:function(_c){
_c.selected=false;
_c.domNode.style.display="none";
if(_c.onHide){
_c.onHide();
}
}});
dojo.declare("dojox.layout.RadioGroupFade",dojox.layout.RadioGroup,{_hideChild:function(_d){
dojo.fadeOut({node:_d.domNode,duration:this.duration,onEnd:dojo.hitch(this,"inherited",arguments)}).play();
},_showChild:function(_e){
this.inherited(arguments);
dojo.style(_e.domNode,"opacity",0);
dojo.fadeIn({node:_e.domNode,duration:this.duration}).play();
}});
dojo.declare("dojox.layout.RadioGroupSlide",dojox.layout.RadioGroup,{easing:"dojo.fx.easing.backOut",zTop:99,constructor:function(){
if(dojo.isString(this.easing)){
this.easing=dojo.getObject(this.easing);
}
},startup:function(){
this.inherited(arguments);
dojo.forEach(this._children,this._positionChild,this);
},_positionChild:function(_f){
var rA=true,rB=true;
switch(_f.slideFrom){
case "bottom":
rB=!rB;
break;
case "right":
rA=!rA;
rB=!rB;
break;
case "top":
break;
case "left":
rA=!rA;
break;
default:
rA=Math.round(Math.random());
rB=Math.round(Math.random());
break;
}
var _12=rA?"top":"left";
var val=(rB?"-":"")+this._size[rA?"h":"w"]+"px";
dojo.style(_f.domNode,_12,val);
},_showChild:function(_14){
var _15=this.getChildren();
_14.isFirstChild=(_14==_15[0]);
_14.isLastChild=(_14==_15[_15.length-1]);
_14.selected=true;
dojo.style(_14.domNode,{display:"",zIndex:this.zTop});
if(this._anim&&this._anim.status()=="playing"){
this._anim.gotoPercent(100,true);
}
this._anim=dojo.animateProperty({node:_14.domNode,properties:{left:0,top:0},duration:this.duration,easing:this.easing,onEnd:dojo.hitch(_14,function(){
if(this.onShow){
this.onShow();
}
if(this._loadCheck){
this._loadCheck();
}
})});
this._anim.play();
},_hideChild:function(_16){
if(this._tmpConnect){
dojo.disconnect(this._tmpConnect);
}
_16.selected=false;
_16.domNode.style.zIndex=this.zTop-1;
if(_16.onHide){
_16.onHide();
}
this._tmpConnect=dojo.connect(this._anim,"onEnd",dojo.hitch(this,"_positionChild",_16));
},addChild:function(_17){
this.inherited(arguments);
this._positionChild(_17);
}});
dojo.declare("dojox.layout._RadioButton",[dijit._Widget,dijit._Templated,dijit._Contained],{label:"",page:null,templateString:"<div dojoAttachPoint=\"focusNode\" class=\"dojoxRadioButton\"><span dojoAttachPoint=\"titleNode\" class=\"dojoxRadioButtonLabel\">${label}</span></div>",startup:function(){
this.connect(this.domNode,"onmouseover","_onMouse");
},_onMouse:function(e){
this.getParent().selectChild(this.page);
this._clearSelected();
dojo.addClass(this.domNode,"dojoxRadioButtonSelected");
},_clearSelected:function(){
dojo.query(".dojoxRadioButtonSelected",this.domNode.parentNode.parentNode).forEach(function(n){
dojo.removeClass(n,"dojoxRadioButtonSelected");
});
}});
dojo.extend(dijit._Widget,{slideFrom:"random"});
}
