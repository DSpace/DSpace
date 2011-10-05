/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.Menu"]){
dojo._hasResource["dijit.Menu"]=true;
dojo.provide("dijit.Menu");
dojo.require("dijit._Widget");
dojo.require("dijit._Container");
dojo.require("dijit._Templated");
dojo.declare("dijit.Menu",[dijit._Widget,dijit._Templated,dijit._KeyNavContainer],{constructor:function(){
this._bindings=[];
},templateString:"<table class=\"dijit dijitMenu dijitReset dijitMenuTable\" waiRole=\"menu\" dojoAttachEvent=\"onkeypress:_onKeyPress\">"+"<tbody class=\"dijitReset\" dojoAttachPoint=\"containerNode\"></tbody>"+"</table>",targetNodeIds:[],contextMenuForWindow:false,leftClickToOpen:false,parentMenu:null,popupDelay:500,_contextMenuWithMouse:false,postCreate:function(){
if(this.contextMenuForWindow){
this.bindDomNode(dojo.body());
}else{
dojo.forEach(this.targetNodeIds,this.bindDomNode,this);
}
this.connectKeyNavHandlers([dojo.keys.UP_ARROW],[dojo.keys.DOWN_ARROW]);
},startup:function(){
if(this._started){
return;
}
dojo.forEach(this.getChildren(),function(_1){
_1.startup();
});
this.startupKeyNavChildren();
this.inherited(arguments);
},onExecute:function(){
},onCancel:function(_2){
},_moveToPopup:function(_3){
if(this.focusedChild&&this.focusedChild.popup&&!this.focusedChild.disabled){
this.focusedChild._onClick(_3);
}
},_onKeyPress:function(_4){
if(_4.ctrlKey||_4.altKey){
return;
}
switch(_4.charOrCode){
case dojo.keys.RIGHT_ARROW:
this._moveToPopup(_4);
dojo.stopEvent(_4);
break;
case dojo.keys.LEFT_ARROW:
if(this.parentMenu){
this.onCancel(false);
}else{
dojo.stopEvent(_4);
}
break;
}
},onItemHover:function(_5){
this.focusChild(_5);
if(this.focusedChild.popup&&!this.focusedChild.disabled&&!this.hover_timer){
this.hover_timer=setTimeout(dojo.hitch(this,"_openPopup"),this.popupDelay);
}
},_onChildBlur:function(_6){
dijit.popup.close(_6.popup);
_6._blur();
this._stopPopupTimer();
},onItemUnhover:function(_7){
},_stopPopupTimer:function(){
if(this.hover_timer){
clearTimeout(this.hover_timer);
this.hover_timer=null;
}
},_getTopMenu:function(){
for(var _8=this;_8.parentMenu;_8=_8.parentMenu){
}
return _8;
},onItemClick:function(_9,_a){
if(_9.disabled){
return false;
}
if(_9.popup){
if(!this.is_open){
this._openPopup();
}
}else{
this.onExecute();
_9.onClick(_a);
}
},_iframeContentWindow:function(_b){
var _c=dijit.getDocumentWindow(dijit.Menu._iframeContentDocument(_b))||dijit.Menu._iframeContentDocument(_b)["__parent__"]||(_b.name&&dojo.doc.frames[_b.name])||null;
return _c;
},_iframeContentDocument:function(_d){
var _e=_d.contentDocument||(_d.contentWindow&&_d.contentWindow.document)||(_d.name&&dojo.doc.frames[_d.name]&&dojo.doc.frames[_d.name].document)||null;
return _e;
},bindDomNode:function(_f){
_f=dojo.byId(_f);
var win=dijit.getDocumentWindow(_f.ownerDocument);
if(_f.tagName.toLowerCase()=="iframe"){
win=this._iframeContentWindow(_f);
_f=dojo.withGlobal(win,dojo.body);
}
var cn=(_f==dojo.body()?dojo.doc:_f);
_f[this.id]=this._bindings.push([dojo.connect(cn,(this.leftClickToOpen)?"onclick":"oncontextmenu",this,"_openMyself"),dojo.connect(cn,"onkeydown",this,"_contextKey"),dojo.connect(cn,"onmousedown",this,"_contextMouse")]);
},unBindDomNode:function(_12){
var _13=dojo.byId(_12);
if(_13){
var bid=_13[this.id]-1,b=this._bindings[bid];
dojo.forEach(b,dojo.disconnect);
delete this._bindings[bid];
}
},_contextKey:function(e){
this._contextMenuWithMouse=false;
if(e.keyCode==dojo.keys.F10){
dojo.stopEvent(e);
if(e.shiftKey&&e.type=="keydown"){
var _e={target:e.target,pageX:e.pageX,pageY:e.pageY};
_e.preventDefault=_e.stopPropagation=function(){
};
window.setTimeout(dojo.hitch(this,function(){
this._openMyself(_e);
}),1);
}
}
},_contextMouse:function(e){
this._contextMenuWithMouse=true;
},_openMyself:function(e){
if(this.leftClickToOpen&&e.button>0){
return;
}
dojo.stopEvent(e);
var x,y;
if(dojo.isSafari||this._contextMenuWithMouse){
x=e.pageX;
y=e.pageY;
}else{
var _1c=dojo.coords(e.target,true);
x=_1c.x+10;
y=_1c.y+10;
}
var _1d=this;
var _1e=dijit.getFocus(this);
function closeAndRestoreFocus(){
dijit.focus(_1e);
dijit.popup.close(_1d);
};
dijit.popup.open({popup:this,x:x,y:y,onExecute:closeAndRestoreFocus,onCancel:closeAndRestoreFocus,orient:this.isLeftToRight()?"L":"R"});
this.focus();
this._onBlur=function(){
this.inherited("_onBlur",arguments);
dijit.popup.close(this);
};
},onOpen:function(e){
this.isShowingNow=true;
},onClose:function(){
this._stopPopupTimer();
this.parentMenu=null;
this.isShowingNow=false;
this.currentPopup=null;
if(this.focusedChild){
this._onChildBlur(this.focusedChild);
this.focusedChild=null;
}
},_openPopup:function(){
this._stopPopupTimer();
var _20=this.focusedChild;
var _21=_20.popup;
if(_21.isShowingNow){
return;
}
_21.parentMenu=this;
var _22=this;
dijit.popup.open({parent:this,popup:_21,around:_20.domNode,orient:this.isLeftToRight()?{"TR":"TL","TL":"TR"}:{"TL":"TR","TR":"TL"},onCancel:function(){
dijit.popup.close(_21);
_20.focus();
_22.currentPopup=null;
}});
this.currentPopup=_21;
if(_21.focus){
_21.focus();
}
},uninitialize:function(){
dojo.forEach(this.targetNodeIds,this.unBindDomNode,this);
this.inherited(arguments);
}});
dojo.declare("dijit.MenuItem",[dijit._Widget,dijit._Templated,dijit._Contained],{templateString:"<tr class=\"dijitReset dijitMenuItem\" dojoAttachPoint=\"focusNode\" waiRole=\"menuitem\" tabIndex=\"-1\""+"dojoAttachEvent=\"onmouseenter:_onHover,onmouseleave:_onUnhover,ondijitclick:_onClick\">"+"<td class=\"dijitReset\" waiRole=\"presentation\"><div class=\"dijitMenuItemIcon\" dojoAttachPoint=\"iconNode\"></div></td>"+"<td class=\"dijitReset dijitMenuItemLabel\" dojoAttachPoint=\"containerNode\"></td>"+"<td class=\"dijitReset dijitMenuArrowCell\" waiRole=\"presentation\">"+"<div dojoAttachPoint=\"arrowWrapper\" style=\"display: none\">"+"<div class=\"dijitMenuExpand\"></div>"+"<span class=\"dijitMenuExpandA11y\">+</span>"+"</div>"+"</td>"+"</tr>",attributeMap:dojo.mixin(dojo.clone(dijit._Widget.prototype.attributeMap),{label:{node:"containerNode",type:"innerHTML"},iconClass:{node:"iconNode",type:"class"}}),label:"",iconClass:"",disabled:false,_fillContent:function(_23){
if(_23&&!("label" in this.params)){
this.attr("label",_23.innerHTML);
}
},postCreate:function(){
dojo.setSelectable(this.domNode,false);
dojo.attr(this.containerNode,"id",this.id+"_text");
dijit.setWaiState(this.domNode,"labelledby",this.id+"_text");
},_onHover:function(){
this.getParent().onItemHover(this);
},_onUnhover:function(){
this.getParent().onItemUnhover(this);
},_onClick:function(evt){
this.getParent().onItemClick(this,evt);
dojo.stopEvent(evt);
},onClick:function(evt){
},focus:function(){
dojo.addClass(this.domNode,"dijitMenuItemHover");
try{
dijit.focus(this.focusNode);
}
catch(e){
}
},_blur:function(){
dojo.removeClass(this.domNode,"dijitMenuItemHover");
},setLabel:function(_26){
dojo.deprecated("dijit.MenuItem.setLabel() is deprecated.  Use attr('label', ...) instead.","","2.0");
this.attr("label",_26);
},setDisabled:function(_27){
dojo.deprecated("dijit.Menu.setDisabled() is deprecated.  Use attr('disabled', bool) instead.","","2.0");
this.attr("disabled",_27);
},_setDisabledAttr:function(_28){
this.disabled=_28;
dojo[_28?"addClass":"removeClass"](this.domNode,"dijitMenuItemDisabled");
dijit.setWaiState(this.focusNode,"disabled",_28?"true":"false");
}});
dojo.declare("dijit.PopupMenuItem",dijit.MenuItem,{_fillContent:function(){
if(this.srcNodeRef){
var _29=dojo.query("*",this.srcNodeRef);
dijit.PopupMenuItem.superclass._fillContent.call(this,_29[0]);
this.dropDownContainer=this.srcNodeRef;
}
},startup:function(){
if(this._started){
return;
}
this.inherited(arguments);
if(!this.popup){
var _2a=dojo.query("[widgetId]",this.dropDownContainer)[0];
this.popup=dijit.byNode(_2a);
}
dojo.body().appendChild(this.popup.domNode);
this.popup.domNode.style.display="none";
dojo.style(this.arrowWrapper,"display","");
dijit.setWaiState(this.focusNode,"haspopup","true");
},destroyDescendants:function(){
if(this.popup){
this.popup.destroyRecursive();
delete this.popup;
}
this.inherited(arguments);
}});
dojo.declare("dijit.MenuSeparator",[dijit._Widget,dijit._Templated,dijit._Contained],{templateString:"<tr class=\"dijitMenuSeparator\"><td colspan=3>"+"<div class=\"dijitMenuSeparatorTop\"></div>"+"<div class=\"dijitMenuSeparatorBottom\"></div>"+"</td></tr>",postCreate:function(){
dojo.setSelectable(this.domNode,false);
},isFocusable:function(){
return false;
}});
dojo.declare("dijit.CheckedMenuItem",dijit.MenuItem,{templateString:"<tr class=\"dijitReset dijitMenuItem\" dojoAttachPoint=\"focusNode\" waiRole=\"menuitemcheckbox\" tabIndex=\"-1\""+"dojoAttachEvent=\"onmouseenter:_onHover,onmouseleave:_onUnhover,ondijitclick:_onClick\">"+"<td class=\"dijitReset\" waiRole=\"presentation\"><div class=\"dijitMenuItemIcon dijitCheckedMenuItemIcon\" dojoAttachPoint=\"iconNode\">"+"<div class=\"dijitCheckedMenuItemIconChar\">&#10003;</div>"+"</div></td>"+"<td class=\"dijitReset dijitMenuItemLabel\" dojoAttachPoint=\"containerNode,labelNode\"></td>"+"<td class=\"dijitReset dijitMenuArrowCell\" waiRole=\"presentation\">"+"<div dojoAttachPoint=\"arrowWrapper\" style=\"display: none\">"+"<div class=\"dijitMenuExpand\"></div>"+"<span class=\"dijitMenuExpandA11y\">+</span>"+"</div>"+"</td>"+"</tr>",checked:false,_setCheckedAttr:function(_2b){
dojo.toggleClass(this.iconNode,"dijitCheckedMenuItemIconChecked",_2b);
dijit.setWaiState(this.domNode,"checked",_2b);
this.checked=_2b;
},onChange:function(_2c){
},_onClick:function(e){
if(!this.disabled){
this.attr("checked",!this.checked);
this.onChange(this.checked);
}
this.inherited(arguments);
}});
}
