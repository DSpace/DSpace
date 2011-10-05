/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.presentation._base"]){
dojo._hasResource["dojox.presentation._base"]=true;
dojo.provide("dojox.presentation._base");
dojo.experimental("dojox.presentation");
dojo.require("dijit._Widget");
dojo.require("dijit._Container");
dojo.require("dijit._Templated");
dojo.require("dijit.layout.StackContainer");
dojo.require("dijit.layout.ContentPane");
dojo.require("dojo.fx");
dojo.declare("dojox.presentation.Deck",[dijit.layout.StackContainer,dijit._Templated],{fullScreen:true,useNav:true,navDuration:250,noClick:false,setHash:true,templateString:null,templateString:"<div class=\"dojoShow\" dojoAttachPoint=\"showHolder\">\n\t<div class=\"dojoShowNav\" dojoAttachPoint=\"showNav\" dojoAttachEvent=\"onmouseover: _showNav, onmouseout: _hideNav\">\n\t<div class=\"dojoShowNavToggler\" dojoAttachPoint=\"showToggler\">\n\t\t<img dojoAttachPoint=\"prevNode\" src=\"${prevIcon}\" dojoAttachEvent=\"onclick:previousSlide\">\n\t\t<select dojoAttachEvent=\"onchange:_onEvent\" dojoAttachPoint=\"select\">\n\t\t\t<option dojoAttachPoint=\"_option\">Title</option>\n\t\t</select>\n\t\t<img dojoAttachPoint=\"nextNode\" src=\"${nextIcon}\" dojoAttachEvent=\"onclick:nextSlide\">\n\t</div>\n\t</div>\n\t<div dojoAttachPoint=\"containerNode\"></div>\n</div>\n",nextIcon:dojo.moduleUrl("dojox.presentation","resources/icons/next.png"),prevIcon:dojo.moduleUrl("dojox.presentation","resources/icons/prev.png"),_navOpacMin:0,_navOpacMax:0.85,_slideIndex:0,_slides:[],_navShowing:true,_inNav:false,startup:function(){
this.inherited(arguments);
if(this.useNav){
this._hideNav();
}else{
this.showNav.style.display="none";
}
this.connect(dojo.doc,"onclick","_onEvent");
this.connect(dojo.doc,"onkeypress","_onEvent");
this.connect(window,"onresize","_resizeWindow");
this._resizeWindow();
this._updateSlides();
this._readHash();
this._setHash();
},moveTo:function(_1){
var _2=_1-1;
if(_2<0){
_2=0;
}
if(_2>this._slides.length-1){
_2=this._slides.length-1;
}
this._gotoSlide(_2);
},onMove:function(_3){
},nextSlide:function(_4){
if(this.selectedChildWidget&&!this.selectedChildWidget.isLastChild){
this._gotoSlide(this._slideIndex+1);
}
if(_4){
_4.stopPropagation();
}
},previousSlide:function(_5){
if(this.selectedChildWidget&&!this.selectedChildWidget.isFirstChild){
this._gotoSlide(this._slideIndex-1);
}else{
this.selectedChildWidget&&this.selectedChildWidget._reset();
}
if(_5){
_5.stopPropagation();
}
},getHash:function(id){
return this.id+"_SlideNo_"+id;
},_hideNav:function(_7){
if(this._navAnim){
this._navAnim.stop();
}
this._navAnim=dojo.animateProperty({node:this.showNav,duration:this.navDuration,properties:{opacity:this._navOpacMin}}).play();
},_showNav:function(_8){
if(this._navAnim){
this._navAnim.stop();
}
this._navAnim=dojo.animateProperty({node:this.showNav,duration:this.navDuration,properties:{opacity:this._navOpacMax}}).play();
},_handleNav:function(_9){
_9.stopPropagation();
},_updateSlides:function(){
this._slides=this.getChildren();
if(this.useNav){
dojo.forEach(this._slides,dojo.hitch(this,function(_a,i){
var _c=this._option.cloneNode(true);
_c.text=_a.title+" ("+i+") ";
this._option.parentNode.insertBefore(_c,this._option);
}));
if(this._option.parentNode){
this._option.parentNode.removeChild(this._option);
}
}
},_onEvent:function(_d){
var _e=_d.target,_f=_d.type,_10=dojo.keys;
if(_f=="click"||_f=="change"){
if(_e.index&&_e.parentNode==this.select){
this._gotoSlide(_e.index);
}else{
if(_e==this.select){
this._gotoSlide(_e.selectedIndex);
}else{
if(this.noClick||this.selectedChildWidget.noClick||this._isUnclickable(_d)){
return;
}
this.selectedChildWidget._nextAction(_d);
}
}
}else{
if(_f=="keydown"||_f=="keypress"){
switch(_d.charOrCode){
case _10.DELETE:
case _10.BACKSPACE:
case _10.LEFT_ARROW:
case _10.UP_ARROW:
case _10.PAGE_UP:
case 80:
this.previousSlide(_d);
break;
case _10.ENTER:
case _10.SPACE:
case _10.RIGHT_ARROW:
case _10.DOWN_ARROW:
case _10.PAGE_DOWN:
case 78:
this.selectedChildWidget._nextAction(_d);
break;
case _10.HOME:
this._gotoSlide(0);
}
}
}
this._resizeWindow();
_d.stopPropagation();
},_gotoSlide:function(_11){
this.selectChild(this._slides[_11]);
this.selectedChildWidget._reset();
this._slideIndex=_11;
if(this.useNav){
this.select.selectedIndex=_11;
}
if(this.setHash){
this._setHash();
}
this.onMove(this._slideIndex+1);
},_isUnclickable:function(evt){
var _13=evt.target.nodeName.toLowerCase();
switch(_13){
case "a":
case "input":
case "textarea":
return true;
break;
}
return false;
},_readHash:function(){
var th=window.location.hash;
if(th.length&&this.setHash){
var _15=(""+window.location).split(this.getHash(""));
if(_15.length>1){
this._gotoSlide(parseInt(_15[1])-1);
}
}
},_setHash:function(){
if(this.setHash){
var _16=this._slideIndex+1;
window.location.href="#"+this.getHash(_16);
}
},_resizeWindow:function(evt){
dojo.body().style.height="auto";
var wh=dijit.getViewport();
var h=Math.max(dojo.doc.documentElement.scrollHeight||dojo.body().scrollHeight,wh.h);
var w=wh.w;
dojo.style(this.selectedChildWidget.domNode,{height:h+"px",width:w+"px"});
},_transition:function(_1b,_1c){
var _1d=[];
if(_1c){
this._hideChild(_1c);
}
if(_1b){
this._showChild(_1b);
_1b._reset();
}
}});
dojo.declare("dojox.presentation.Slide",[dijit.layout.ContentPane,dijit._Contained,dijit._Container,dijit._Templated],{templateString:"<div dojoAttachPoint=\"showSlide\" class=\"dojoShowPrint dojoShowSlide\">\n\t<h1 class=\"showTitle\" dojoAttachPoint=\"slideTitle\"><span class=\"dojoShowSlideTitle\" dojoAttachPoint=\"slideTitleText\">${title}</span></h1>\n\t<div class=\"dojoShowBody\" dojoAttachPoint=\"containerNode\"></div>\n</div>\n",title:"",refreshOnShow:true,preLoad:false,doLayout:true,parseContent:true,noClick:false,_parts:[],_actions:[],_actionIndex:0,_runningDelay:false,startup:function(){
this.inherited(arguments);
this.slideTitleText.innerHTML=this.title;
var _1e=this.getChildren();
this._actions=[];
dojo.forEach(_1e,function(_1f){
var _20=_1f.declaredClass.toLowerCase();
switch(_20){
case "dojox.presentation.part":
this._parts.push(_1f);
break;
case "dojox.presentation.action":
this._actions.push(_1f);
break;
}
},this);
},_nextAction:function(evt){
var _22=this._actions[this._actionIndex]||0;
if(_22){
if(_22.on=="delay"){
this._runningDelay=setTimeout(dojo.hitch(_22,"_runAction"),_22.delay);
}else{
_22._runAction();
}
var _23=this._getNextAction();
this._actionIndex++;
if(_23.on=="delay"){
setTimeout(dojo.hitch(_23,"_runAction"),_23.delay);
}
}else{
this.getParent().nextSlide(evt);
}
},_getNextAction:function(){
return this._actions[this._actionIndex+1]||0;
},_reset:function(){
this._actionIndex=[0];
dojo.forEach(this._parts,function(_24){
_24._reset();
},this);
}});
dojo.declare("dojox.presentation.Part",[dijit._Widget,dijit._Contained],{as:"",startVisible:false,_isShowing:false,postCreate:function(){
this._reset();
},_reset:function(){
this._isShowing=!this.startVisible;
this._quickToggle();
},_quickToggle:function(){
var _25=dojo.partial(dojo.style,this.domNode);
if(this._isShowing){
_25({display:"none",visibility:"hidden",opacity:0});
}else{
_25({display:"",visibility:"visible",opacity:1});
}
this._isShowing=!this._isShowing;
}});
dojo.declare("dojox.presentation.Action",[dijit._Widget,dijit._Contained],{on:"click",forSlide:"",toggle:"fade",delay:0,duration:1000,_attached:[],_nullAnim:false,_runAction:function(){
var _26=[];
dojo.forEach(this._attached,function(_27){
var _28=dojo.fadeIn({node:_27.domNode,duration:this.duration,beforeBegin:dojo.hitch(_27,"_quickToggle")});
_26.push(_28);
},this);
var _29=dojo.fx.combine(_26);
if(_29){
_29.play();
}
},_getSiblingsByType:function(_2a){
var _2b=dojo.filter(this.getParent().getChildren(),function(_2c){
return _2c.declaredClass==_2a;
});
return _2b;
},postCreate:function(){
this.inherited(arguments);
dojo.style(this.domNode,"display","none");
var _2d=this._getSiblingsByType("dojox.presentation.Part");
this._attached=[];
dojo.forEach(_2d,function(_2e){
if(this.forSlide==_2e.as){
this._attached.push(_2e);
}
},this);
}});
}
