/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.Dialog"]){
dojo._hasResource["dijit.Dialog"]=true;
dojo.provide("dijit.Dialog");
dojo.require("dojo.dnd.move");
dojo.require("dojo.dnd.TimedMoveable");
dojo.require("dojo.fx");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dijit.layout.ContentPane");
dojo.require("dijit.form.Form");
dojo.requireLocalization("dijit","common",null,"zh,ca,pt,da,tr,ru,de,sv,ja,he,fi,nb,el,ar,ROOT,pt-pt,cs,fr,es,ko,nl,zh-tw,pl,th,it,hu,sk,sl");
dojo.declare("dijit.DialogUnderlay",[dijit._Widget,dijit._Templated],{templateString:"<div class='dijitDialogUnderlayWrapper' id='${id}_wrapper'><div class='dijitDialogUnderlay ${class}' id='${id}' dojoAttachPoint='node'></div></div>",attributeMap:{},postCreate:function(){
dojo.body().appendChild(this.domNode);
this.bgIframe=new dijit.BackgroundIframe(this.domNode);
},layout:function(){
var _1=dijit.getViewport();
var is=this.node.style,os=this.domNode.style;
os.top=_1.t+"px";
os.left=_1.l+"px";
is.width=_1.w+"px";
is.height=_1.h+"px";
var _4=dijit.getViewport();
if(_1.w!=_4.w){
is.width=_4.w+"px";
}
if(_1.h!=_4.h){
is.height=_4.h+"px";
}
},show:function(){
this.domNode.style.display="block";
this.layout();
if(this.bgIframe.iframe){
this.bgIframe.iframe.style.display="block";
}
},hide:function(){
this.domNode.style.display="none";
if(this.bgIframe.iframe){
this.bgIframe.iframe.style.display="none";
}
},uninitialize:function(){
if(this.bgIframe){
this.bgIframe.destroy();
}
}});
dojo.declare("dijit._DialogMixin",null,{attributeMap:dijit._Widget.prototype.attributeMap,execute:function(_5){
},onCancel:function(){
},onExecute:function(){
},_onSubmit:function(){
this.onExecute();
this.execute(this.attr("value"));
},_getFocusItems:function(_6){
var _7=dijit._getTabNavigable(dojo.byId(_6));
this._firstFocusItem=_7.lowest||_7.first||_6;
this._lastFocusItem=_7.last||_7.highest||this._firstFocusItem;
if(dojo.isMoz&&this._firstFocusItem.tagName.toLowerCase()=="input"&&dojo.attr(this._firstFocusItem,"type").toLowerCase()=="file"){
dojo.attr(_6,"tabindex","0");
this._firstFocusItem=_6;
}
}});
dojo.declare("dijit.Dialog",[dijit.layout.ContentPane,dijit._Templated,dijit.form._FormMixin,dijit._DialogMixin],{templateString:null,templateString:"<div class=\"dijitDialog\" tabindex=\"-1\" waiRole=\"dialog\" waiState=\"labelledby-${id}_title\">\n\t<div dojoAttachPoint=\"titleBar\" class=\"dijitDialogTitleBar\">\n\t<span dojoAttachPoint=\"titleNode\" class=\"dijitDialogTitle\" id=\"${id}_title\"></span>\n\t<span dojoAttachPoint=\"closeButtonNode\" class=\"dijitDialogCloseIcon\" dojoAttachEvent=\"onclick: onCancel\" title=\"${buttonCancel}\">\n\t\t<span dojoAttachPoint=\"closeText\" class=\"closeText\" title=\"${buttonCancel}\">x</span>\n\t</span>\n\t</div>\n\t\t<div dojoAttachPoint=\"containerNode\" class=\"dijitDialogPaneContent\"></div>\n</div>\n",attributeMap:dojo.mixin(dojo.clone(dijit._Widget.prototype.attributeMap),{title:[{node:"titleNode",type:"innerHTML"},{node:"titleBar",type:"attribute"}]}),open:false,duration:dijit.defaultDuration,refocus:true,autofocus:true,_firstFocusItem:null,_lastFocusItem:null,doLayout:false,draggable:true,postMixInProperties:function(){
var _8=dojo.i18n.getLocalization("dijit","common");
dojo.mixin(this,_8);
this.inherited(arguments);
},postCreate:function(){
var s=this.domNode.style;
s.visibility="hidden";
s.position="absolute";
s.display="";
s.top="-9999px";
dojo.body().appendChild(this.domNode);
this.inherited(arguments);
this.connect(this,"onExecute","hide");
this.connect(this,"onCancel","hide");
this._modalconnects=[];
},onLoad:function(){
this._position();
this.inherited(arguments);
},_endDrag:function(e){
if(e&&e.node&&e.node===this.domNode){
var vp=dijit.getViewport();
var p=e._leftTop||dojo.coords(e.node,true);
this._relativePosition={t:p.t-vp.t,l:p.l-vp.l};
}
},_setup:function(){
var _d=this.domNode;
if(this.titleBar&&this.draggable){
this._moveable=(dojo.isIE==6)?new dojo.dnd.TimedMoveable(_d,{handle:this.titleBar}):new dojo.dnd.Moveable(_d,{handle:this.titleBar,timeout:0});
dojo.subscribe("/dnd/move/stop",this,"_endDrag");
}else{
dojo.addClass(_d,"dijitDialogFixed");
}
this._underlay=new dijit.DialogUnderlay({id:this.id+"_underlay","class":dojo.map(this["class"].split(/\s/),function(s){
return s+"_underlay";
}).join(" ")});
var _f=this._underlay;
this._fadeIn=dojo.fadeIn({node:_d,duration:this.duration,onBegin:dojo.hitch(_f,"show")});
this._fadeOut=dojo.fadeOut({node:_d,duration:this.duration,onEnd:function(){
_d.style.visibility="hidden";
_d.style.top="-9999px";
_f.hide();
}});
},uninitialize:function(){
if(this._fadeIn&&this._fadeIn.status()=="playing"){
this._fadeIn.stop();
}
if(this._fadeOut&&this._fadeOut.status()=="playing"){
this._fadeOut.stop();
}
if(this._underlay){
this._underlay.destroy();
}
if(this._moveable){
this._moveable.destroy();
}
},_size:function(){
var mb=dojo.marginBox(this.domNode);
var _11=dijit.getViewport();
if(mb.w>=_11.w||mb.h>=_11.h){
dojo.style(this.containerNode,{width:Math.min(mb.w,Math.floor(_11.w*0.75))+"px",height:Math.min(mb.h,Math.floor(_11.h*0.75))+"px",overflow:"auto",position:"relative"});
}
},_position:function(){
if(!dojo.hasClass(dojo.body(),"dojoMove")){
var _12=this.domNode;
var _13=dijit.getViewport();
var p=this._relativePosition;
var mb=p?null:dojo.marginBox(_12);
dojo.style(_12,{left:Math.floor(_13.l+(p?p.l:(_13.w-mb.w)/2))+"px",top:Math.floor(_13.t+(p?p.t:(_13.h-mb.h)/2))+"px"});
}
},_onKey:function(evt){
if(evt.charOrCode){
var dk=dojo.keys;
var _18=evt.target;
if(evt.charOrCode===dk.TAB){
this._getFocusItems(this.domNode);
}
var _19=(this._firstFocusItem==this._lastFocusItem);
if(_18==this._firstFocusItem&&evt.shiftKey&&evt.charOrCode===dk.TAB){
if(!_19){
dijit.focus(this._lastFocusItem);
}
dojo.stopEvent(evt);
}else{
if(_18==this._lastFocusItem&&evt.charOrCode===dk.TAB&&!evt.shiftKey){
if(!_19){
dijit.focus(this._firstFocusItem);
}
dojo.stopEvent(evt);
}else{
while(_18){
if(_18==this.domNode){
if(evt.charOrCode==dk.ESCAPE){
this.onCancel();
}else{
return;
}
}
_18=_18.parentNode;
}
if(evt.charOrCode!==dk.TAB){
dojo.stopEvent(evt);
}else{
if(!dojo.isOpera){
try{
this._firstFocusItem.focus();
}
catch(e){
}
}
}
}
}
}
},show:function(){
if(this.open){
return;
}
if(!this._alreadyInitialized){
this._setup();
this._alreadyInitialized=true;
}
if(this._fadeOut.status()=="playing"){
this._fadeOut.stop();
}
this._modalconnects.push(dojo.connect(window,"onscroll",this,"layout"));
this._modalconnects.push(dojo.connect(window,"onresize",this,"layout"));
this._modalconnects.push(dojo.connect(dojo.doc.documentElement,"onkeypress",this,"_onKey"));
dojo.style(this.domNode,{opacity:0,visibility:""});
this.open=true;
this._loadCheck();
this._size();
this._position();
this._fadeIn.play();
this._savedFocus=dijit.getFocus(this);
if(this.autofocus){
this._getFocusItems(this.domNode);
setTimeout(dojo.hitch(dijit,"focus",this._firstFocusItem),50);
}
},hide:function(){
if(!this._alreadyInitialized){
return;
}
if(this._fadeIn.status()=="playing"){
this._fadeIn.stop();
}
this._fadeOut.play();
if(this._scrollConnected){
this._scrollConnected=false;
}
dojo.forEach(this._modalconnects,dojo.disconnect);
this._modalconnects=[];
if(this.refocus){
this.connect(this._fadeOut,"onEnd",dojo.hitch(dijit,"focus",this._savedFocus));
}
if(this._relativePosition){
delete this._relativePosition;
}
this.open=false;
},layout:function(){
if(this.domNode.style.visibility!="hidden"){
this._underlay.layout();
this._position();
}
},destroy:function(){
dojo.forEach(this._modalconnects,dojo.disconnect);
if(this.refocus&&this.open){
setTimeout(dojo.hitch(dijit,"focus",this._savedFocus),25);
}
this.inherited(arguments);
}});
dojo.declare("dijit.TooltipDialog",[dijit.layout.ContentPane,dijit._Templated,dijit.form._FormMixin,dijit._DialogMixin],{title:"",doLayout:false,autofocus:true,"class":"dijitTooltipDialog",_firstFocusItem:null,_lastFocusItem:null,templateString:null,templateString:"<div waiRole=\"presentation\">\n\t<div class=\"dijitTooltipContainer\" waiRole=\"presentation\">\n\t\t<div class =\"dijitTooltipContents dijitTooltipFocusNode\" dojoAttachPoint=\"containerNode\" tabindex=\"-1\" waiRole=\"dialog\"></div>\n\t</div>\n\t<div class=\"dijitTooltipConnector\" waiRole=\"presentation\"></div>\n</div>\n",postCreate:function(){
this.inherited(arguments);
this.connect(this.containerNode,"onkeypress","_onKey");
this.containerNode.title=this.title;
},orient:function(_1a,_1b,_1c){
this.domNode.className=this["class"]+" dijitTooltipAB"+(_1c.charAt(1)=="L"?"Left":"Right")+" dijitTooltip"+(_1c.charAt(0)=="T"?"Below":"Above");
},onOpen:function(pos){
this.orient(this.domNode,pos.aroundCorner,pos.corner);
this._loadCheck();
if(this.autofocus){
this._getFocusItems(this.containerNode);
dijit.focus(this._firstFocusItem);
}
},_onKey:function(evt){
var _1f=evt.target;
var dk=dojo.keys;
if(evt.charOrCode===dk.TAB){
this._getFocusItems(this.containerNode);
}
var _21=(this._firstFocusItem==this._lastFocusItem);
if(evt.charOrCode==dk.ESCAPE){
this.onCancel();
dojo.stopEvent(evt);
}else{
if(_1f==this._firstFocusItem&&evt.shiftKey&&evt.charOrCode===dk.TAB){
if(!_21){
dijit.focus(this._lastFocusItem);
}
dojo.stopEvent(evt);
}else{
if(_1f==this._lastFocusItem&&evt.charOrCode===dk.TAB&&!evt.shiftKey){
if(!_21){
dijit.focus(this._firstFocusItem);
}
dojo.stopEvent(evt);
}else{
if(evt.charOrCode===dk.TAB){
evt.stopPropagation();
}
}
}
}
}});
}
