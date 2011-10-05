/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._tree.dndContainer"]){
dojo._hasResource["dijit._tree.dndContainer"]=true;
dojo.provide("dijit._tree.dndContainer");
dojo.require("dojo.dnd.common");
dojo.require("dojo.dnd.Container");
dojo.declare("dijit._tree.dndContainer",null,{constructor:function(_1,_2){
this.tree=_1;
this.node=_1.domNode;
dojo.mixin(this,_2);
this.map={};
this.current=null;
this.containerState="";
dojo.addClass(this.node,"dojoDndContainer");
if(!(_2&&_2._skipStartup)){
this.startup();
}
this.events=[dojo.connect(this.node,"onmouseover",this,"onMouseOver"),dojo.connect(this.node,"onmouseout",this,"onMouseOut"),dojo.connect(this.node,"ondragstart",dojo,"stopEvent"),dojo.connect(this.node,"onselectstart",dojo,"stopEvent")];
},getItem:function(_3){
return this.selection[_3];
},destroy:function(){
dojo.forEach(this.events,dojo.disconnect);
this.node=this.parent=this.current;
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
if(n){
this._addItemClass(n,"Over");
}
this.current=n;
},onMouseOut:function(e){
for(var n=e.relatedTarget;n;){
if(n==this.node){
return;
}
try{
n=n.parentNode;
}
catch(x){
n=null;
}
}
if(this.current){
this._removeItemClass(this.current,"Over");
this.current=null;
}
this._changeState("Container","");
this.onOutEvent();
},_changeState:function(_9,_a){
var _b="dojoDnd"+_9;
var _c=_9.toLowerCase()+"State";
dojo.removeClass(this.node,_b+this[_c]);
dojo.addClass(this.node,_b+_a);
this[_c]=_a;
},_getChildByEvent:function(e){
var _e=e.target;
if(_e){
for(var _f=_e.parentNode;_f;_e=_f,_f=_e.parentNode){
if(dojo.hasClass(_e,"dijitTreeContent")){
return _e;
}
}
}
return null;
},markupFactory:function(_10,_11){
_11._skipStartup=true;
return new dijit._tree.dndContainer(_10,_11);
},_addItemClass:function(_12,_13){
dojo.addClass(_12,"dojoDndItem"+_13);
},_removeItemClass:function(_14,_15){
dojo.removeClass(_14,"dojoDndItem"+_15);
},onOverEvent:function(){
},onOutEvent:function(){
}});
}
