/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._tree.dndSource"]){
dojo._hasResource["dijit._tree.dndSource"]=true;
dojo.provide("dijit._tree.dndSource");
dojo.require("dijit._tree.dndSelector");
dojo.require("dojo.dnd.Manager");
dojo.declare("dijit._tree.dndSource",dijit._tree.dndSelector,{isSource:true,copyOnly:false,skipForm:false,dragThreshold:0,accept:["text"],constructor:function(_1,_2){
if(!_2){
_2={};
}
dojo.mixin(this,_2);
this.isSource=typeof _2.isSource=="undefined"?true:_2.isSource;
var _3=_2.accept instanceof Array?_2.accept:["text"];
this.accept=null;
if(_3.length){
this.accept={};
for(var i=0;i<_3.length;++i){
this.accept[_3[i]]=1;
}
}
this.isDragging=false;
this.mouseDown=false;
this.targetAnchor=null;
this.targetBox=null;
this.before=true;
this._lastX=0;
this._lastY=0;
this.sourceState="";
if(this.isSource){
dojo.addClass(this.node,"dojoDndSource");
}
this.targetState="";
if(this.accept){
dojo.addClass(this.node,"dojoDndTarget");
}
if(this.horizontal){
dojo.addClass(this.node,"dojoDndHorizontal");
}
this.topics=[dojo.subscribe("/dnd/source/over",this,"onDndSourceOver"),dojo.subscribe("/dnd/start",this,"onDndStart"),dojo.subscribe("/dnd/drop",this,"onDndDrop"),dojo.subscribe("/dnd/cancel",this,"onDndCancel")];
},startup:function(){
},checkAcceptance:function(_5,_6){
return true;
},copyState:function(_7){
return this.copyOnly||_7;
},destroy:function(){
this.inherited("destroy",arguments);
dojo.forEach(this.topics,dojo.unsubscribe);
this.targetAnchor=null;
},markupFactory:function(_8,_9){
_8._skipStartup=true;
return new dijit._tree.dndSource(_9,_8);
},onMouseMove:function(e){
if(this.isDragging&&this.targetState=="Disabled"){
return;
}
this.inherited("onMouseMove",arguments);
var m=dojo.dnd.manager();
if(this.isDragging){
if(this.allowBetween){
var _c=false;
if(this.current){
if(!this.targetBox||this.targetAnchor!=this.current){
this.targetBox={xy:dojo.coords(this.current,true),w:this.current.offsetWidth,h:this.current.offsetHeight};
}
if(this.horizontal){
_c=(e.pageX-this.targetBox.xy.x)<(this.targetBox.w/2);
}else{
_c=(e.pageY-this.targetBox.xy.y)<(this.targetBox.h/2);
}
}
if(this.current!=this.targetAnchor||_c!=this.before){
this._markTargetAnchor(_c);
m.canDrop(!this.current||m.source!=this||!(this.current.id in this.selection));
}
}
}else{
if(this.mouseDown&&this.isSource&&(Math.abs(e.pageX-this._lastX)>=this.dragThreshold||Math.abs(e.pageY-this._lastY)>=this.dragThreshold)){
var n=this.getSelectedNodes();
var _e=[];
for(var i in n){
_e.push(n[i]);
}
if(_e.length){
m.startDrag(this,_e,this.copyState(dojo.dnd.getCopyKeyState(e)));
}
}
}
},onMouseDown:function(e){
this.mouseDown=true;
this.mouseButton=e.button;
this._lastX=e.pageX;
this._lastY=e.pageY;
this.inherited("onMouseDown",arguments);
},onMouseUp:function(e){
if(this.mouseDown){
this.mouseDown=false;
this.inherited("onMouseUp",arguments);
}
},onMouseOver:function(e){
var rt=e.relatedTarget;
while(rt){
if(rt==this.node){
break;
}
try{
rt=rt.parentNode;
}
catch(x){
rt=null;
}
}
if(!rt){
this._changeState("Container","Over");
this.onOverEvent();
}
var n=this._getChildByEvent(e);
if(this.current==n){
return;
}
if(this.current){
this._removeItemClass(this.current,"Over");
}
var m=dojo.dnd.manager();
if(n){
this._addItemClass(n,"Over");
if(this.isDragging){
if(this.checkItemAcceptance(n,m.source)){
m.canDrop(this.targetState!="Disabled"&&(!this.current||m.source!=this||!(n in this.selection)));
}
}
}else{
if(this.isDragging){
m.canDrop(false);
}
}
this.current=n;
},checkItemAcceptance:function(_16,_17){
return true;
},onDndSourceOver:function(_18){
if(this!=_18){
this.mouseDown=false;
if(this.targetAnchor){
this._unmarkTargetAnchor();
}
}else{
if(this.isDragging){
var m=dojo.dnd.manager();
m.canDrop(false);
}
}
},onDndStart:function(_1a,_1b,_1c){
if(this.isSource){
this._changeState("Source",this==_1a?(_1c?"Copied":"Moved"):"");
}
var _1d=this.checkAcceptance(_1a,_1b);
this._changeState("Target",_1d?"":"Disabled");
if(_1d){
dojo.dnd.manager().overSource(this);
}
this.isDragging=true;
},itemCreator:function(_1e){
return dojo.map(_1e,function(_1f){
return {"id":_1f.id,"name":_1f.textContent||_1f.innerText||""};
});
},onDndDrop:function(_20,_21,_22){
if(this.containerState=="Over"){
var _23=this.tree,_24=_23.model,_25=this.current,_26=false;
this.isDragging=false;
var _27=dijit.getEnclosingWidget(_25),_28=(_27&&_27.item)||_23.item;
var _29;
if(_20!=this){
_29=this.itemCreator(_21,_25);
}
dojo.forEach(_21,function(_2a,idx){
if(_20==this){
var _2c=dijit.getEnclosingWidget(_2a),_2d=_2c.item,_2e=_2c.getParent().item;
_24.pasteItem(_2d,_2e,_28,_22);
}else{
_24.newItem(_29[idx],_28);
}
},this);
this.tree._expandNode(_27);
}
this.onDndCancel();
},onDndCancel:function(){
if(this.targetAnchor){
this._unmarkTargetAnchor();
this.targetAnchor=null;
}
this.before=true;
this.isDragging=false;
this.mouseDown=false;
delete this.mouseButton;
this._changeState("Source","");
this._changeState("Target","");
},onOverEvent:function(){
this.inherited("onOverEvent",arguments);
dojo.dnd.manager().overSource(this);
},onOutEvent:function(){
this.inherited("onOutEvent",arguments);
dojo.dnd.manager().outSource(this);
},_markTargetAnchor:function(_2f){
if(this.current==this.targetAnchor&&this.before==_2f){
return;
}
if(this.targetAnchor){
this._removeItemClass(this.targetAnchor,this.before?"Before":"After");
}
this.targetAnchor=this.current;
this.targetBox=null;
this.before=_2f;
if(this.targetAnchor){
this._addItemClass(this.targetAnchor,this.before?"Before":"After");
}
},_unmarkTargetAnchor:function(){
if(!this.targetAnchor){
return;
}
this._removeItemClass(this.targetAnchor,this.before?"Before":"After");
this.targetAnchor=null;
this.targetBox=null;
this.before=true;
},_markDndStatus:function(_30){
this._changeState("Source",_30?"Copied":"Moved");
}});
dojo.declare("dijit._tree.dndTarget",dijit._tree.dndSource,{constructor:function(_31,_32){
this.isSource=false;
dojo.removeClass(this.node,"dojoDndSource");
},markupFactory:function(_33,_34){
_33._skipStartup=true;
return new dijit._tree.dndTarget(_34,_33);
}});
}
