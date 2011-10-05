/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.form.Button"]){
dojo._hasResource["dijit.form.Button"]=true;
dojo.provide("dijit.form.Button");
dojo.require("dijit.form._FormWidget");
dojo.require("dijit._Container");
dojo.declare("dijit.form.Button",dijit.form._FormWidget,{label:"",showLabel:true,iconClass:"",type:"button",baseClass:"dijitButton",templateString:"<span class=\"dijit dijitReset dijitLeft dijitInline\"\n\tdojoAttachEvent=\"ondijitclick:_onButtonClick,onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\"\n\t><span class=\"dijitReset dijitRight dijitInline\"\n\t\t><span class=\"dijitReset dijitInline dijitButtonNode\"\n\t\t\t><button class=\"dijitReset dijitStretch dijitButtonContents\"\n\t\t\t\tdojoAttachPoint=\"titleNode,focusNode\" \n\t\t\t\tname=\"${name}\" type=\"${type}\" waiRole=\"button\" waiState=\"labelledby-${id}_label\"\n\t\t\t\t><span class=\"dijitReset dijitInline\" dojoAttachPoint=\"iconNode\" \n\t\t\t\t\t><span class=\"dijitReset dijitToggleButtonIconChar\">&#10003;</span \n\t\t\t\t></span \n\t\t\t\t><span class=\"dijitReset dijitInline dijitButtonText\" \n\t\t\t\t\tid=\"${id}_label\"  \n\t\t\t\t\tdojoAttachPoint=\"containerNode\"\n\t\t\t\t></span\n\t\t\t></button\n\t\t></span\n\t></span\n></span>\n",attributeMap:dojo.mixin(dojo.clone(dijit.form._FormWidget.prototype.attributeMap),{label:{node:"containerNode",type:"innerHTML"},iconClass:{node:"iconNode",type:"class"}}),_onClick:function(e){
if(this.disabled||this.readOnly){
return false;
}
this._clicked();
return this.onClick(e);
},_onButtonClick:function(e){
if(e.type!="click"){
dojo.stopEvent(e);
}
if(this._onClick(e)===false){
e.preventDefault();
}else{
if(this.type=="submit"&&!this.focusNode.form){
for(var _3=this.domNode;_3.parentNode;_3=_3.parentNode){
var _4=dijit.byNode(_3);
if(_4&&typeof _4._onSubmit=="function"){
_4._onSubmit(e);
break;
}
}
}
}
},_fillContent:function(_5){
if(_5&&!("label" in this.params)){
this.attr("label",_5.innerHTML);
}
},postCreate:function(){
if(this.showLabel==false){
dojo.addClass(this.containerNode,"dijitDisplayNone");
}
dojo.setSelectable(this.focusNode,false);
this.inherited(arguments);
},onClick:function(e){
return true;
},_clicked:function(e){
},setLabel:function(_8){
dojo.deprecated("dijit.form.Button.setLabel() is deprecated.  Use attr('label', ...) instead.","","2.0");
this.attr("label",_8);
},_setLabelAttr:function(_9){
this.containerNode.innerHTML=this.label=_9;
this._layoutHack();
if(this.showLabel==false&&!this.params.title){
this.titleNode.title=dojo.trim(this.containerNode.innerText||this.containerNode.textContent||"");
}
}});
dojo.declare("dijit.form.DropDownButton",[dijit.form.Button,dijit._Container],{baseClass:"dijitDropDownButton",templateString:"<span class=\"dijit dijitReset dijitLeft dijitInline\"\n\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse,onclick:_onDropDownClick,onkeydown:_onDropDownKeydown,onblur:_onDropDownBlur,onkeypress:_onKey\"\n\t><span class='dijitReset dijitRight dijitInline'\n\t\t><span class='dijitReset dijitInline dijitButtonNode'\n\t\t\t><button class=\"dijitReset dijitStretch dijitButtonContents\" \n\t\t\t\ttype=\"${type}\" name=\"${name}\"\n\t\t\t\tdojoAttachPoint=\"focusNode,titleNode\" \n\t\t\t\twaiRole=\"button\" waiState=\"haspopup-true,labelledby-${id}_label\"\n\t\t\t\t><span class=\"dijitReset dijitInline\" \n\t\t\t\t\tdojoAttachPoint=\"iconNode\"\n\t\t\t\t></span\n\t\t\t\t><span class=\"dijitReset dijitInline dijitButtonText\"  \n\t\t\t\t\tdojoAttachPoint=\"containerNode,popupStateNode\" \n\t\t\t\t\tid=\"${id}_label\"\n\t\t\t\t></span\n\t\t\t\t><span class=\"dijitReset dijitInline dijitArrowButtonInner\">&thinsp;</span\n\t\t\t\t><span class=\"dijitReset dijitInline dijitArrowButtonChar\">&#9660;</span\n\t\t\t></button\n\t\t></span\n\t></span\n></span>\n",_fillContent:function(){
if(this.srcNodeRef){
var _a=dojo.query("*",this.srcNodeRef);
dijit.form.DropDownButton.superclass._fillContent.call(this,_a[0]);
this.dropDownContainer=this.srcNodeRef;
}
},startup:function(){
if(this._started){
return;
}
if(!this.dropDown){
var _b=dojo.query("[widgetId]",this.dropDownContainer)[0];
this.dropDown=dijit.byNode(_b);
delete this.dropDownContainer;
}
dijit.popup.prepare(this.dropDown.domNode);
this.inherited(arguments);
},destroyDescendants:function(){
if(this.dropDown){
this.dropDown.destroyRecursive();
delete this.dropDown;
}
this.inherited(arguments);
},_onArrowClick:function(e){
if(this.disabled||this.readOnly){
return;
}
this._toggleDropDown();
},_onDropDownClick:function(e){
var _e=dojo.isFF&&dojo.isFF<3&&navigator.appVersion.indexOf("Macintosh")!=-1;
if(!_e||e.detail!=0||this._seenKeydown){
this._onArrowClick(e);
}
this._seenKeydown=false;
},_onDropDownKeydown:function(e){
this._seenKeydown=true;
},_onDropDownBlur:function(e){
this._seenKeydown=false;
},_onKey:function(e){
if(this.disabled||this.readOnly){
return;
}
if(e.charOrCode==dojo.keys.DOWN_ARROW){
if(!this.dropDown||this.dropDown.domNode.style.visibility=="hidden"){
dojo.stopEvent(e);
this._toggleDropDown();
}
}
},_onBlur:function(){
this._closeDropDown();
this.inherited(arguments);
},_toggleDropDown:function(){
if(this.disabled||this.readOnly){
return;
}
dijit.focus(this.popupStateNode);
var _12=this.dropDown;
if(!_12){
return;
}
if(!this._opened){
if(_12.href&&!_12.isLoaded){
var _13=this;
var _14=dojo.connect(_12,"onLoad",function(){
dojo.disconnect(_14);
_13._openDropDown();
});
_12._loadCheck(true);
return;
}else{
this._openDropDown();
}
}else{
this._closeDropDown();
}
},_openDropDown:function(){
var _15=this.dropDown;
var _16=_15.domNode.style.width;
var _17=this;
dijit.popup.open({parent:this,popup:_15,around:this.domNode,orient:this.isLeftToRight()?{"BL":"TL","BR":"TR","TL":"BL","TR":"BR"}:{"BR":"TR","BL":"TL","TR":"BR","TL":"BL"},onExecute:function(){
_17._closeDropDown(true);
},onCancel:function(){
_17._closeDropDown(true);
},onClose:function(){
_15.domNode.style.width=_16;
_17.popupStateNode.removeAttribute("popupActive");
_17._opened=false;
}});
if(this.domNode.offsetWidth>_15.domNode.offsetWidth){
var _18=null;
if(!this.isLeftToRight()){
_18=_15.domNode.parentNode;
var _19=_18.offsetLeft+_18.offsetWidth;
}
dojo.marginBox(_15.domNode,{w:this.domNode.offsetWidth});
if(_18){
_18.style.left=_19-this.domNode.offsetWidth+"px";
}
}
this.popupStateNode.setAttribute("popupActive","true");
this._opened=true;
if(_15.focus){
_15.focus();
}
},_closeDropDown:function(_1a){
if(this._opened){
dijit.popup.close(this.dropDown);
if(_1a){
this.focus();
}
this._opened=false;
}
}});
dojo.declare("dijit.form.ComboButton",dijit.form.DropDownButton,{templateString:"<table class='dijit dijitReset dijitInline dijitLeft'\n\tcellspacing='0' cellpadding='0' waiRole=\"presentation\"\n\t><tbody waiRole=\"presentation\"><tr waiRole=\"presentation\"\n\t\t><td class=\"dijitReset dijitStretch dijitButtonContents dijitButtonNode\"\n\t\t\tdojoAttachEvent=\"ondijitclick:_onButtonClick,onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\"  dojoAttachPoint=\"titleNode\"\n\t\t\twaiRole=\"button\" waiState=\"labelledby-${id}_label\"\n\t\t\t><div class=\"dijitReset dijitInline\" dojoAttachPoint=\"iconNode\" waiRole=\"presentation\"></div\n\t\t\t><div class=\"dijitReset dijitInline dijitButtonText\" id=\"${id}_label\" dojoAttachPoint=\"containerNode\" waiRole=\"presentation\"></div\n\t\t></td\n\t\t><td class='dijitReset dijitRight dijitButtonNode dijitArrowButton dijitDownArrowButton'\n\t\t\tdojoAttachPoint=\"popupStateNode,focusNode\"\n\t\t\tdojoAttachEvent=\"ondijitclick:_onArrowClick, onkeypress:_onKey,onmouseenter:_onMouse,onmouseleave:_onMouse\"\n\t\t\tstateModifier=\"DownArrow\"\n\t\t\ttitle=\"${optionsTitle}\" name=\"${name}\"\n\t\t\twaiRole=\"button\" waiState=\"haspopup-true\"\n\t\t\t><div class=\"dijitReset dijitArrowButtonInner\" waiRole=\"presentation\">&thinsp;</div\n\t\t\t><div class=\"dijitReset dijitArrowButtonChar\" waiRole=\"presentation\">&#9660;</div\n\t\t></td\n\t></tr></tbody\n></table>\n",attributeMap:dojo.mixin(dojo.clone(dijit.form.Button.prototype.attributeMap),{id:"",name:"",tabIndex:["focusNode","titleNode"]}),optionsTitle:"",baseClass:"dijitComboButton",_focusedNode:null,postCreate:function(){
this.inherited(arguments);
this._focalNodes=[this.titleNode,this.popupStateNode];
dojo.forEach(this._focalNodes,dojo.hitch(this,function(_1b){
if(dojo.isIE){
this.connect(_1b,"onactivate",this._onNodeFocus);
this.connect(_1b,"ondeactivate",this._onNodeBlur);
}else{
this.connect(_1b,"onfocus",this._onNodeFocus);
this.connect(_1b,"onblur",this._onNodeBlur);
}
}));
},focusFocalNode:function(_1c){
this._focusedNode=_1c;
dijit.focus(_1c);
},hasNextFocalNode:function(){
return this._focusedNode!==this.getFocalNodes()[1];
},focusNext:function(){
this._focusedNode=this.getFocalNodes()[this._focusedNode?1:0];
dijit.focus(this._focusedNode);
},hasPrevFocalNode:function(){
return this._focusedNode!==this.getFocalNodes()[0];
},focusPrev:function(){
this._focusedNode=this.getFocalNodes()[this._focusedNode?0:1];
dijit.focus(this._focusedNode);
},getFocalNodes:function(){
return this._focalNodes;
},_onNodeFocus:function(evt){
this._focusedNode=evt.currentTarget;
var fnc=this._focusedNode==this.focusNode?"dijitDownArrowButtonFocused":"dijitButtonContentsFocused";
dojo.addClass(this._focusedNode,fnc);
},_onNodeBlur:function(evt){
var fnc=evt.currentTarget==this.focusNode?"dijitDownArrowButtonFocused":"dijitButtonContentsFocused";
dojo.removeClass(evt.currentTarget,fnc);
},_onBlur:function(){
this.inherited(arguments);
this._focusedNode=null;
}});
dojo.declare("dijit.form.ToggleButton",dijit.form.Button,{baseClass:"dijitToggleButton",checked:false,attributeMap:dojo.mixin(dojo.clone(dijit.form.Button.prototype.attributeMap),{checked:"focusNode"}),_clicked:function(evt){
this.attr("checked",!this.checked);
},_setCheckedAttr:function(_22){
this.checked=_22;
dojo.attr(this.focusNode||this.domNode,"checked",_22);
dijit.setWaiState(this.focusNode||this.domNode,"pressed",_22);
this._setStateClass();
this._handleOnChange(_22,true);
},setChecked:function(_23){
dojo.deprecated("setChecked("+_23+") is deprecated. Use attr('checked',"+_23+") instead.","","2.0");
this.attr("checked",_23);
},reset:function(){
this._hasBeenBlurred=false;
this.attr("checked",this.params.checked||false);
}});
}
