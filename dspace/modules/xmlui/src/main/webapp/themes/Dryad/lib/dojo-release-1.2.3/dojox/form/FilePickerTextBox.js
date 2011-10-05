/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.form.FilePickerTextBox"]){
dojo._hasResource["dojox.form.FilePickerTextBox"]=true;
dojo.provide("dojox.form.FilePickerTextBox");
dojo.require("dojox.widget.FilePicker");
dojo.require("dijit.form.ValidationTextBox");
dojo.require("dojox.form._HasDropDown");
dojo.declare("dojox.form.FilePickerTextBox",[dijit.form.ValidationTextBox,dojox.form._HasDropDown],{baseClass:"dojoxFilePickerTextBox",templateString:"<div class=\"dijit dijitReset dijitInlineTable dijitLeft\"\n\tid=\"widget_${id}\"\n\tdojoAttachEvent=\"onmouseenter:_onMouse,onmouseleave:_onMouse,onmousedown:_onMouse\" waiRole=\"combobox\" tabIndex=\"-1\"\n\t><div style=\"overflow:hidden;\"\n\t\t><div class='dijitReset dijitRight dijitButtonNode dijitArrowButton dijitDownArrowButton'\n\t\t\tdojoAttachPoint=\"downArrowNode,dropDownNode,popupStateNode\" waiRole=\"presentation\"\n\t\t\t><div class=\"dijitArrowButtonInner\">&thinsp;</div\n\t\t\t><div class=\"dijitArrowButtonChar\">&#9660;</div\n\t\t></div\n\t\t><div class=\"dijitReset dijitValidationIcon\"><br></div\n\t\t><div class=\"dijitReset dijitValidationIconText\">&Chi;</div\n\t\t><div class=\"dijitReset dijitInputField\"\n\t\t\t><input type=\"text\" autocomplete=\"off\" name=\"${name}\" class='dijitReset'\n\t\t\t\tdojoAttachEvent='onfocus:_update,onkeyup:_update,onblur:_onMouse,onkeypress:_onKey' \n\t\t\t\tdojoAttachPoint='textbox,focusNode' waiRole=\"textbox\" waiState=\"haspopup-true,autocomplete-list\"\n\t\t/></div\n\t></div\n></div>\n",searchDelay:500,_stopClickEvents:false,valueItem:null,postMixInProperties:function(){
this.inherited(arguments);
this.dropDown=new dojox.widget.FilePicker(this.constraints);
},postCreate:function(){
this.inherited(arguments);
this.connect(this.dropDown,"onChange",this._onWidgetChange);
this.connect(this.focusNode,"onblur","_focusBlur");
this.connect(this.focusNode,"onfocus","_focusFocus");
this.connect(this.focusNode,"ondblclick",function(){
dijit.selectInputText(this.focusNode);
});
},_setValueAttr:function(_1){
if(!this._searchInProgress){
this.inherited(arguments);
this._skip=true;
this.dropDown.attr("pathValue",_1);
}
},_onWidgetChange:function(_2){
if(!_2&&this.focusNode.value){
this._hasValidPath=false;
}else{
this.valueItem=_2;
var _3=this.dropDown._getPathValueAttr(_2);
if(_3||!this._skipInvalidSet){
if(_3){
this._hasValidPath=true;
}
if(!this._skip){
this.attr("value",_3);
}
delete this._skip;
}
}
this.validate();
},startup:function(){
if(!this.dropDown._started){
this.dropDown.startup();
}
this.inherited(arguments);
},openDropDown:function(){
this.dropDown.domNode.style.width="0px";
this.inherited(arguments);
},toggleDropDown:function(){
this.inherited(arguments);
if(this._opened){
this.dropDown.attr("pathValue",this.attr("value"));
}
},_focusBlur:function(e){
if(e.explicitOriginalTarget==this.focusNode&&!this._allowBlur){
window.setTimeout(dojo.hitch(this,function(){
if(!this._allowBlur){
this.focus();
}
}),1);
}else{
if(this._menuFocus){
this.dropDown._updateClass(this._menuFocus,"Item",{"Hover":false});
delete this._menuFocus;
}
}
},_focusFocus:function(e){
if(this._menuFocus){
this.dropDown._updateClass(this._menuFocus,"Item",{"Hover":false});
}
delete this._menuFocus;
var _6=dijit.getFocus(this);
if(_6&&_6.node){
_6=dijit.byNode(_6.node);
if(_6){
this._menuFocus=_6.domNode;
}
}
if(this._menuFocus){
this.dropDown._updateClass(this._menuFocus,"Item",{"Hover":true});
}
delete this._allowBlur;
},_onBlur:function(){
this._allowBlur=true;
delete this.dropDown._savedFocus;
this.inherited(arguments);
},_setBlurValue:function(){
if(this.dropDown){
this.attr("value",this.focusNode.value);
}
this.inherited(arguments);
},parse:function(_7,_8){
if(this._hasValidPath||this._hasSelection){
return _7;
}
var dd=this.dropDown,_a=dd.topDir,_b=dd.pathSeparator;
var _c=dd.attr("pathValue");
var _d=function(v){
if(_a.length&&v.indexOf(_a)===0){
v=v.substring(_a.length);
}
if(_b&&v[v.length-1]==_b){
v=v.substring(0,v.length-1);
}
return v;
};
_c=_d(_c);
val=_d(_7);
if(val==_c){
return _7;
}
return undefined;
},_startSearchFromInput:function(){
var dd=this.dropDown,fn=this.focusNode;
var val=fn.value,_12=val,_13=dd.topDir;
if(this._hasSelection){
dijit.selectInputText(fn,_12.length);
}
this._hasSelection=false;
if(_13.length&&val.indexOf(_13)===0){
val=val.substring(_13.length);
}
var _14=val.split(dd.pathSeparator);
var _15=dojo.hitch(this,function(idx){
var dir=_14[idx];
var _18=dd.getChildren()[idx];
var _19;
this._searchInProgress=true;
var _1a=dojo.hitch(this,function(){
delete this._searchInProgress;
});
if((dir||_18)&&!this._opened){
this.toggleDropDown();
}
if(dir&&_18){
var fx=dojo.hitch(this,function(){
if(_19){
this.disconnect(_19);
}
delete _19;
var _1c=_18._menu.getChildren();
var _1d=dojo.filter(_1c,function(i){
return i.label==dir;
})[0];
var _1f=dojo.filter(_1c,function(i){
return (i.label.indexOf(dir)===0);
})[0];
if(_1d&&((_14.length>idx+1&&_1d.children)||(!_1d.children))){
idx++;
_18._menu.onItemClick(_1d,{type:"internal",stopPropagation:function(){
},preventDefault:function(){
}});
if(_14[idx]){
_15(idx);
}else{
_1a();
}
}else{
_18._setSelected(null);
if(_1f&&_14.length===idx+1){
dd._setInProgress=true;
dd._removeAfter(_18);
delete dd._setInProgress;
var _21=_1f.label;
if(_1f.children){
_21+=dd.pathSeparator;
}
_21=_21.substring(dir.length);
window.setTimeout(function(){
dijit.scrollIntoView(_1f.domNode);
},1);
fn.value=_12+_21;
dijit.selectInputText(fn,_12.length);
this._hasSelection=true;
try{
_1f.focusNode.focus();
}
catch(e){
}
}else{
if(this._menuFocus){
this.dropDown._updateClass(this._menuFocus,"Item",{"Hover":false,"Focus":false});
}
delete this._menuFocus;
}
_1a();
}
});
if(!_18.isLoaded){
_19=this.connect(_18,"onLoad",fx);
}else{
fx();
}
}else{
if(_18){
_18._setSelected(null);
dd._setInProgress=true;
dd._removeAfter(_18);
delete dd._setInProgress;
}
_1a();
}
});
_15(0);
},_onKey:function(e){
if(this.disabled||this.readOnly){
return;
}
var dk=dojo.keys;
var c=e.charOrCode;
if(c==dk.DOWN_ARROW){
this._allowBlur=true;
}
if(c==dk.ENTER&&this._opened){
this.dropDown.onExecute();
dijit.selectInputText(this.focusNode,this.focusNode.value.length);
this._hasSelection=false;
dojo.stopEvent(e);
return;
}
if((c==dk.RIGHT_ARROW||c==dk.LEFT_ARROW||c==dk.TAB)&&this._hasSelection){
this._startSearchFromInput();
dojo.stopEvent(e);
return;
}
this.inherited(arguments);
var _25=false;
if((c==dk.BACKSPACE||c==dk.DELETE)&&this._hasSelection){
this._hasSelection=false;
}else{
if(c==dk.BACKSPACE||c==dk.DELETE||c==" "){
_25=true;
}else{
_25=e.keyChar!=="";
}
}
if(this._searchTimer){
window.clearTimeout(this._searchTimer);
}
delete this._searchTimer;
if(_25){
this._hasValidPath=false;
this._hasSelection=false;
this._searchTimer=window.setTimeout(dojo.hitch(this,"_startSearchFromInput"),this.searchDelay+1);
}
}});
}
