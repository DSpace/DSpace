/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._base.focus"]){
dojo._hasResource["dijit._base.focus"]=true;
dojo.provide("dijit._base.focus");
dojo.mixin(dijit,{_curFocus:null,_prevFocus:null,isCollapsed:function(){
var _1=dojo.doc;
if(_1.selection){
var s=_1.selection;
if(s.type=="Text"){
return !s.createRange().htmlText.length;
}else{
return !s.createRange().length;
}
}else{
var _3=dojo.global;
var _4=_3.getSelection();
if(dojo.isString(_4)){
return !_4;
}else{
return _4.isCollapsed||!_4.toString();
}
}
},getBookmark:function(){
var _5,_6=dojo.doc.selection;
if(_6){
var _7=_6.createRange();
if(_6.type.toUpperCase()=="CONTROL"){
if(_7.length){
_5=[];
var i=0,_9=_7.length;
while(i<_9){
_5.push(_7.item(i++));
}
}else{
_5=null;
}
}else{
_5=_7.getBookmark();
}
}else{
if(window.getSelection){
_6=dojo.global.getSelection();
if(_6){
_7=_6.getRangeAt(0);
_5=_7.cloneRange();
}
}else{
console.warn("No idea how to store the current selection for this browser!");
}
}
return _5;
},moveToBookmark:function(_a){
var _b=dojo.doc;
if(_b.selection){
var _c;
if(dojo.isArray(_a)){
_c=_b.body.createControlRange();
dojo.forEach(_a,function(n){
_c.addElement(n);
});
}else{
_c=_b.selection.createRange();
_c.moveToBookmark(_a);
}
_c.select();
}else{
var _e=dojo.global.getSelection&&dojo.global.getSelection();
if(_e&&_e.removeAllRanges){
_e.removeAllRanges();
_e.addRange(_a);
}else{
console.warn("No idea how to restore selection for this browser!");
}
}
},getFocus:function(_f,_10){
return {node:_f&&dojo.isDescendant(dijit._curFocus,_f.domNode)?dijit._prevFocus:dijit._curFocus,bookmark:!dojo.withGlobal(_10||dojo.global,dijit.isCollapsed)?dojo.withGlobal(_10||dojo.global,dijit.getBookmark):null,openedForWindow:_10};
},focus:function(_11){
if(!_11){
return;
}
var _12="node" in _11?_11.node:_11,_13=_11.bookmark,_14=_11.openedForWindow;
if(_12){
var _15=(_12.tagName.toLowerCase()=="iframe")?_12.contentWindow:_12;
if(_15&&_15.focus){
try{
_15.focus();
}
catch(e){
}
}
dijit._onFocusNode(_12);
}
if(_13&&dojo.withGlobal(_14||dojo.global,dijit.isCollapsed)){
if(_14){
_14.focus();
}
try{
dojo.withGlobal(_14||dojo.global,dijit.moveToBookmark,null,[_13]);
}
catch(e){
}
}
},_activeStack:[],registerWin:function(_16){
if(!_16){
_16=window;
}
dojo.connect(_16.document,"onmousedown",function(evt){
dijit._justMouseDowned=true;
setTimeout(function(){
dijit._justMouseDowned=false;
},0);
dijit._onTouchNode(evt.target||evt.srcElement);
});
var doc=_16.document;
if(doc){
if(dojo.isIE){
doc.attachEvent("onactivate",function(evt){
if(evt.srcElement.tagName.toLowerCase()!="#document"){
dijit._onFocusNode(evt.srcElement);
}
});
doc.attachEvent("ondeactivate",function(evt){
dijit._onBlurNode(evt.srcElement);
});
}else{
doc.addEventListener("focus",function(evt){
dijit._onFocusNode(evt.target);
},true);
doc.addEventListener("blur",function(evt){
dijit._onBlurNode(evt.target);
},true);
}
}
doc=null;
},_onBlurNode:function(_1d){
dijit._prevFocus=dijit._curFocus;
dijit._curFocus=null;
if(dijit._justMouseDowned){
return;
}
if(dijit._clearActiveWidgetsTimer){
clearTimeout(dijit._clearActiveWidgetsTimer);
}
dijit._clearActiveWidgetsTimer=setTimeout(function(){
delete dijit._clearActiveWidgetsTimer;
dijit._setStack([]);
dijit._prevFocus=null;
},100);
},_onTouchNode:function(_1e){
if(dijit._clearActiveWidgetsTimer){
clearTimeout(dijit._clearActiveWidgetsTimer);
delete dijit._clearActiveWidgetsTimer;
}
var _1f=[];
try{
while(_1e){
if(_1e.dijitPopupParent){
_1e=dijit.byId(_1e.dijitPopupParent).domNode;
}else{
if(_1e.tagName&&_1e.tagName.toLowerCase()=="body"){
if(_1e===dojo.body()){
break;
}
_1e=dijit.getDocumentWindow(_1e.ownerDocument).frameElement;
}else{
var id=_1e.getAttribute&&_1e.getAttribute("widgetId");
if(id){
_1f.unshift(id);
}
_1e=_1e.parentNode;
}
}
}
}
catch(e){
}
dijit._setStack(_1f);
},_onFocusNode:function(_21){
if(!_21){
return;
}
if(_21.nodeType==9){
return;
}
if(_21.nodeType==9){
var _22=dijit.getDocumentWindow(_21).frameElement;
if(!_22){
return;
}
_21=_22;
}
dijit._onTouchNode(_21);
if(_21==dijit._curFocus){
return;
}
if(dijit._curFocus){
dijit._prevFocus=dijit._curFocus;
}
dijit._curFocus=_21;
dojo.publish("focusNode",[_21]);
},_setStack:function(_23){
var _24=dijit._activeStack;
dijit._activeStack=_23;
for(var _25=0;_25<Math.min(_24.length,_23.length);_25++){
if(_24[_25]!=_23[_25]){
break;
}
}
for(var i=_24.length-1;i>=_25;i--){
var _27=dijit.byId(_24[i]);
if(_27){
_27._focused=false;
_27._hasBeenBlurred=true;
if(_27._onBlur){
_27._onBlur();
}
if(_27._setStateClass){
_27._setStateClass();
}
dojo.publish("widgetBlur",[_27]);
}
}
for(i=_25;i<_23.length;i++){
_27=dijit.byId(_23[i]);
if(_27){
_27._focused=true;
if(_27._onFocus){
_27._onFocus();
}
if(_27._setStateClass){
_27._setStateClass();
}
dojo.publish("widgetFocus",[_27]);
}
}
}});
dojo.addOnLoad(dijit.registerWin);
}
