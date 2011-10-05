/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._Container"]){
dojo._hasResource["dijit._Container"]=true;
dojo.provide("dijit._Container");
dojo.declare("dijit._Contained",null,{getParent:function(){
for(var p=this.domNode.parentNode;p;p=p.parentNode){
var id=p.getAttribute&&p.getAttribute("widgetId");
if(id){
var _3=dijit.byId(id);
return _3.isContainer?_3:null;
}
}
return null;
},_getSibling:function(_4){
var _5=this.domNode;
do{
_5=_5[_4+"Sibling"];
}while(_5&&_5.nodeType!=1);
if(!_5){
return null;
}
var id=_5.getAttribute("widgetId");
return dijit.byId(id);
},getPreviousSibling:function(){
return this._getSibling("previous");
},getNextSibling:function(){
return this._getSibling("next");
},getIndexInParent:function(){
var p=this.getParent();
if(!p||!p.getIndexOfChild){
return -1;
}
return p.getIndexOfChild(this);
}});
dojo.declare("dijit._Container",null,{isContainer:true,buildRendering:function(){
this.inherited(arguments);
if(!this.containerNode){
this.containerNode=this.domNode;
}
},addChild:function(_8,_9){
var _a=this.containerNode;
if(_9&&typeof _9=="number"){
var _b=dojo.query("> [widgetId]",_a);
if(_b&&_b.length>=_9){
_a=_b[_9-1];
_9="after";
}
}
dojo.place(_8.domNode,_a,_9);
if(this._started&&!_8._started){
_8.startup();
}
},removeChild:function(_c){
if(typeof _c=="number"&&_c>0){
_c=this.getChildren()[_c];
}
if(!_c||!_c.domNode){
return;
}
var _d=_c.domNode;
_d.parentNode.removeChild(_d);
},_nextElement:function(_e){
do{
_e=_e.nextSibling;
}while(_e&&_e.nodeType!=1);
return _e;
},_firstElement:function(_f){
_f=_f.firstChild;
if(_f&&_f.nodeType!=1){
_f=this._nextElement(_f);
}
return _f;
},getChildren:function(){
return dojo.query("> [widgetId]",this.containerNode).map(dijit.byNode);
},hasChildren:function(){
return !!this._firstElement(this.containerNode);
},destroyDescendants:function(_10){
dojo.forEach(this.getChildren(),function(_11){
_11.destroyRecursive(_10);
});
},_getSiblingOfChild:function(_12,dir){
var _14=_12.domNode;
var _15=(dir>0?"nextSibling":"previousSibling");
do{
_14=_14[_15];
}while(_14&&(_14.nodeType!=1||!dijit.byNode(_14)));
return _14?dijit.byNode(_14):null;
},getIndexOfChild:function(_16){
var _17=this.getChildren();
for(var i=0,c;c=_17[i];i++){
if(c==_16){
return i;
}
}
return -1;
}});
dojo.declare("dijit._KeyNavContainer",[dijit._Container],{_keyNavCodes:{},connectKeyNavHandlers:function(_1a,_1b){
var _1c=this._keyNavCodes={};
var _1d=dojo.hitch(this,this.focusPrev);
var _1e=dojo.hitch(this,this.focusNext);
dojo.forEach(_1a,function(_1f){
_1c[_1f]=_1d;
});
dojo.forEach(_1b,function(_20){
_1c[_20]=_1e;
});
this.connect(this.domNode,"onkeypress","_onContainerKeypress");
this.connect(this.domNode,"onfocus","_onContainerFocus");
},startupKeyNavChildren:function(){
dojo.forEach(this.getChildren(),dojo.hitch(this,"_startupChild"));
},addChild:function(_21,_22){
dijit._KeyNavContainer.superclass.addChild.apply(this,arguments);
this._startupChild(_21);
},focus:function(){
this.focusFirstChild();
},focusFirstChild:function(){
this.focusChild(this._getFirstFocusableChild());
},focusNext:function(){
if(this.focusedChild&&this.focusedChild.hasNextFocalNode&&this.focusedChild.hasNextFocalNode()){
this.focusedChild.focusNext();
return;
}
var _23=this._getNextFocusableChild(this.focusedChild,1);
if(_23.getFocalNodes){
this.focusChild(_23,_23.getFocalNodes()[0]);
}else{
this.focusChild(_23);
}
},focusPrev:function(){
if(this.focusedChild&&this.focusedChild.hasPrevFocalNode&&this.focusedChild.hasPrevFocalNode()){
this.focusedChild.focusPrev();
return;
}
var _24=this._getNextFocusableChild(this.focusedChild,-1);
if(_24.getFocalNodes){
var _25=_24.getFocalNodes();
this.focusChild(_24,_25[_25.length-1]);
}else{
this.focusChild(_24);
}
},focusChild:function(_26,_27){
if(_26){
if(this.focusedChild&&_26!==this.focusedChild){
this._onChildBlur(this.focusedChild);
}
this.focusedChild=_26;
if(_27&&_26.focusFocalNode){
_26.focusFocalNode(_27);
}else{
_26.focus();
}
}
},_startupChild:function(_28){
if(_28.getFocalNodes){
dojo.forEach(_28.getFocalNodes(),function(_29){
dojo.attr(_29,"tabindex",-1);
this._connectNode(_29);
},this);
}else{
var _2a=_28.focusNode||_28.domNode;
if(_28.isFocusable()){
dojo.attr(_2a,"tabindex",-1);
}
this._connectNode(_2a);
}
},_connectNode:function(_2b){
this.connect(_2b,"onfocus","_onNodeFocus");
this.connect(_2b,"onblur","_onNodeBlur");
},_onContainerFocus:function(evt){
if(evt.target===this.domNode){
this.focusFirstChild();
}
},_onContainerKeypress:function(evt){
if(evt.ctrlKey||evt.altKey){
return;
}
var _2e=this._keyNavCodes[evt.charOrCode];
if(_2e){
_2e();
dojo.stopEvent(evt);
}
},_onNodeFocus:function(evt){
dojo.attr(this.domNode,"tabindex",-1);
var _30=dijit.getEnclosingWidget(evt.target);
if(_30&&_30.isFocusable()){
this.focusedChild=_30;
}
dojo.stopEvent(evt);
},_onNodeBlur:function(evt){
if(this.tabIndex){
dojo.attr(this.domNode,"tabindex",this.tabIndex);
}
dojo.stopEvent(evt);
},_onChildBlur:function(_32){
},_getFirstFocusableChild:function(){
return this._getNextFocusableChild(null,1);
},_getNextFocusableChild:function(_33,dir){
if(_33){
_33=this._getSiblingOfChild(_33,dir);
}
var _35=this.getChildren();
for(var i=0;i<_35.length;i++){
if(!_33){
_33=_35[(dir>0)?0:(_35.length-1)];
}
if(_33.isFocusable()){
return _33;
}
_33=this._getSiblingOfChild(_33,dir);
}
return null;
}});
}
