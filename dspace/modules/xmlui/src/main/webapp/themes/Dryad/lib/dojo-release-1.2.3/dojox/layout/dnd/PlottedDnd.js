/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.layout.dnd.PlottedDnd"]){
dojo._hasResource["dojox.layout.dnd.PlottedDnd"]=true;
dojo.provide("dojox.layout.dnd.PlottedDnd");
dojo.require("dojo.dnd.Source");
dojo.require("dojo.dnd.Manager");
dojo.require("dojox.layout.dnd.Avatar");
dojo.declare("dojox.layout.dnd.PlottedDnd",[dojo.dnd.Source],{GC_OFFSET_X:dojo.dnd.manager().OFFSET_X,GC_OFFSET_Y:dojo.dnd.manager().OFFSET_Y,constructor:function(_1,_2){
this.childBoxes=null;
this.dropIndicator=new dojox.layout.dnd.DropIndicator("dndDropIndicator","div");
this.withHandles=_2.withHandles;
this.handleClasses=_2.handleClasses;
this.opacity=_2.opacity;
this.allowAutoScroll=_2.allowAutoScroll;
this.dom=_2.dom;
this.singular=true;
this.skipForm=true;
this._over=false;
this.defaultHandleClass="GcDndHandle";
this.isDropped=false;
this._timer=null;
this.isOffset=(_2.isOffset)?true:false;
this.offsetDrag=(_2.offsetDrag)?_2.offsetDrag:{x:0,y:0};
this.hideSource=_2.hideSource?_2.hideSource:true;
this._drop=this.dropIndicator.create();
},_calculateCoords:function(_3){
dojo.forEach(this.node.childNodes,dojo.hitch(this,function(_4){
_4.coords={xy:dojo.coords(_4,true),w:_4.offsetWidth/2,h:_4.offsetHeight/2};
if(_3){
_4.coords.mh=dojo.marginBox(_4).h;
}
}));
},_legalMouseDown:function(e){
if(!this.withHandles){
return true;
}
for(var _6=(e.target);_6&&_6!=this.node;_6=_6.parentNode){
if(dojo.hasClass(_6,this.defaultHandleClass)){
return true;
}
}
return false;
},setDndItemSelectable:function(_7,_8){
for(var _9=_7;_9&&_7!=this.node;_9=_9.parentNode){
if(dojo.hasClass(_9,"dojoDndItem")){
dojo.setSelectable(_9,_8);
return;
}
}
},getDraggedWidget:function(_a){
var _b=_a;
while(_b&&_b.nodeName.toLowerCase()!="body"&&!dojo.hasClass(_b,"dojoDndItem")){
_b=_b.parentNode;
}
return (_b)?dijit.byNode(_b):null;
},isAccepted:function(_c){
var _d=(_c)?_c.getAttribute("dndtype"):null;
if(_d&&_d in this.accept){
return true;
}else{
return false;
}
},onDndStart:function(_e,_f,_10){
if(_e==this){
this.firstIndicator=true;
}else{
this.firstIndicator=false;
}
this._calculateCoords(true);
if(_f[0].coords){
this._drop.style.height=_f[0].coords.mh+"px";
}else{
var m=dojo.dnd.manager();
this._drop.style.height=m.avatar.node.clientHeight+"px";
}
this.dndNodes=_f;
dojox.layout.dnd.PlottedDnd.superclass.onDndStart.call(this,_e,_f,_10);
if(_e==this){
if(this.hideSource){
for(var i=0;i<_f.length;i++){
dojo.style(_f[i],"display","none");
}
}
}
},onDndCancel:function(){
var m=dojo.dnd.manager();
if(m.source==this&&this.hideSource){
var _14=this.getSelectedNodes();
for(var i=0;i<_14.length;i++){
if(_14[i]){
dojo.style(_14[i],"display","");
}
}
}
dojox.layout.dnd.PlottedDnd.superclass.onDndCancel.call(this);
this.deleteDashedZone();
},onDndDrop:function(_16,_17,_18,_19){
try{
if(!this.isAccepted(_17[0])){
this.onDndCancel();
}else{
if(_16==this&&this._over&&this.dropObject){
this.current=this.dropObject.c;
}
dojox.layout.dnd.PlottedDnd.superclass.onDndDrop.call(this,_16,_17,_18,_19);
this._calculateCoords(true);
}
}
catch(error){

}
},onMouseDown:function(e){
if(this.current==null){
this.selection={};
}else{
if(this.current==this.anchor){
this.anchor=null;
}
}
if(this.current!==null){
this.current.coords={xy:dojo.coords(this.current,true),w:this.current.offsetWidth/2,h:this.current.offsetHeight/2,mh:dojo.marginBox(this.current).h};
this._drop.style.height=this.current.coords.mh+"px";
if(this.isOffset){
if(this.offsetDrag.x==0&&this.offsetDrag.y==0){
var _1b=true;
var _1c=dojo.coords(this._getChildByEvent(e));
this.offsetDrag.x=_1c.x-e.pageX;
this.offsetDrag.y=_1c.y-e.clientY;
}
if(this.offsetDrag.y<16&&this.current!=null){
this.offsetDrag.y=this.GC_OFFSET_Y;
}
var m=dojo.dnd.manager();
m.OFFSET_X=this.offsetDrag.x;
m.OFFSET_Y=this.offsetDrag.y;
if(_1b){
this.offsetDrag.x=0;
this.offsetDrag.y=0;
}
}
}
if(dojo.dnd.isFormElement(e)){
this.setDndItemSelectable(e.target,true);
}else{
this.containerSource=true;
var _1e=this.getDraggedWidget(e.target);
if(_1e&&_1e.dragRestriction){
dragRestriction=true;
}else{
dojox.layout.dnd.PlottedDnd.superclass.onMouseDown.call(this,e);
}
}
},onMouseUp:function(e){
dojox.layout.dnd.PlottedDnd.superclass.onMouseUp.call(this,e);
this.containerSource=false;
if(!dojo.isIE&&this.mouseDown){
this.setDndItemSelectable(e.target,true);
}
var m=dojo.dnd.manager();
m.OFFSET_X=this.GC_OFFSET_X;
m.OFFSET_Y=this.GC_OFFSET_Y;
},onMouseMove:function(e){
var m=dojo.dnd.manager();
if(this.isDragging){
var _23=false;
if(this.current!=null||(this.current==null&&!this.dropObject)){
if(this.isAccepted(m.nodes[0])||this.containerSource){
_23=this.setIndicatorPosition(e);
}
}
if(this.current!=this.targetAnchor||_23!=this.before){
this._markTargetAnchor(_23);
m.canDrop(!this.current||m.source!=this||!(this.current.id in this.selection));
}
if(this.allowAutoScroll){
this._checkAutoScroll(e);
}
}else{
if(this.mouseDown&&this.isSource){
var _24=this.getSelectedNodes();
if(_24.length){
m.startDrag(this,_24,this.copyState(dojo.dnd.getCopyKeyState(e)));
}
}
if(this.allowAutoScroll){
this._stopAutoScroll();
}
}
},_markTargetAnchor:function(_25){
if(this.current==this.targetAnchor&&this.before==_25){
return;
}
this.targetAnchor=this.current;
this.targetBox=null;
this.before=_25;
},_unmarkTargetAnchor:function(){
if(!this.targetAnchor){
return;
}
this.targetAnchor=null;
this.targetBox=null;
this.before=true;
},setIndicatorPosition:function(e){
var _27=false;
if(this.current){
if(!this.current.coords||this.allowAutoScroll){
this.current.coords={xy:dojo.coords(this.current,true),w:this.current.offsetWidth/2,h:this.current.offsetHeight/2};
}
if(this.horizontal){
_27=(e.pageX-this.current.coords.xy.x)<this.current.coords.w;
}else{
_27=(e.pageY-this.current.coords.xy.y)<this.current.coords.h;
}
this.insertDashedZone(_27);
}else{
if(!this.dropObject){
this.insertDashedZone(false);
}
}
return _27;
},onOverEvent:function(){
this._over=true;
dojox.layout.dnd.PlottedDnd.superclass.onOverEvent.call(this);
if(this.isDragging){
var m=dojo.dnd.manager();
if(!this.current&&!this.dropObject&&this.getSelectedNodes()[0]&&this.isAccepted(m.nodes[0])){
this.insertDashedZone(false);
}
}
},onOutEvent:function(){
this._over=false;
this.containerSource=false;
dojox.layout.dnd.PlottedDnd.superclass.onOutEvent.call(this);
if(this.dropObject){
this.deleteDashedZone();
}
},deleteDashedZone:function(){
this._drop.style.display="none";
var _29=this._drop.nextSibling;
while(_29!=null){
_29.coords.xy.y-=parseInt(this._drop.style.height);
_29=_29.nextSibling;
}
delete this.dropObject;
},insertDashedZone:function(_2a){
if(this.dropObject){
if(_2a==this.dropObject.b&&((this.current&&this.dropObject.c==this.current.id)||(!this.current&&!this.dropObject.c))){
return;
}else{
this.deleteDashedZone();
}
}
this.dropObject={n:this._drop,c:(this.current)?this.current.id:null,b:_2a};
if(this.current){
dojo.place(this._drop,this.current,(_2a)?"before":"after");
if(!this.firstIndicator){
var _2b=this._drop.nextSibling;
while(_2b!=null){
_2b.coords.xy.y+=parseInt(this._drop.style.height);
_2b=_2b.nextSibling;
}
}else{
this.firstIndicator=false;
}
}else{
this.node.appendChild(this._drop);
}
this._drop.style.display="";
},insertNodes:function(_2c,_2d,_2e,_2f){
if(this.dropObject){
dojo.style(this.dropObject.n,"display","none");
dojox.layout.dnd.PlottedDnd.superclass.insertNodes.call(this,true,_2d,true,this.dropObject.n);
this.deleteDashedZone();
}else{
return dojox.layout.dnd.PlottedDnd.superclass.insertNodes.call(this,_2c,_2d,_2e,_2f);
}
var _30=dijit.byId(_2d[0].getAttribute("widgetId"));
if(_30){
dojox.layout.dnd._setGcDndHandle(_30,this.withHandles,this.handleClasses);
if(this.hideSource){
dojo.style(_30.domNode,"display","");
}
}
},_checkAutoScroll:function(e){
if(this._timer){
clearTimeout(this._timer);
}
this._stopAutoScroll();
var _32=this.dom;
var y=this._sumAncestorProperties(_32,"offsetTop");
if((e.pageY-_32.offsetTop+30)>_32.clientHeight){
autoScrollActive=true;
this._autoScrollDown(_32);
}else{
if((_32.scrollTop>0)&&(e.pageY-y)<30){
autoScrollActive=true;
this._autoScrollUp(_32);
}
}
},_autoScrollUp:function(_34){
if(autoScrollActive&&_34.scrollTop>0){
_34.scrollTop-=30;
this._timer=setTimeout(dojo.hitch(this,function(){
this._autoScrollUp(_34);
}),"100");
}
},_autoScrollDown:function(_35){
if(autoScrollActive&&(_35.scrollTop<(_35.scrollHeight-_35.clientHeight))){
_35.scrollTop+=30;
this._timer=setTimeout(dojo.hitch(this,function(){
this._autoScrollDown(_35);
}),"100");
}
},_stopAutoScroll:function(){
this.autoScrollActive=false;
},_sumAncestorProperties:function(_36,_37){
_36=dojo.byId(_36);
if(!_36){
return 0;
}
var _38=0;
while(_36){
var val=_36[_37];
if(val){
_38+=val-0;
if(_36==dojo.body()){
break;
}
}
_36=_36.parentNode;
}
return _38;
}});
dojox.layout.dnd._setGcDndHandle=function(_3a,_3b,_3c,_3d){
if(!_3d){
dojo.query(".GcDndHandle",_3a.domNode).removeClass("GcDndHandle");
}
if(!_3b){
dojo.addClass(_3a.domNode,"GcDndHandle");
}else{
var _3e=false;
for(var i=_3c.length-1;i>=0;i--){
var _40=dojo.query("."+_3c[i],_3a.domNode)[0];
if(_40){
_3e=true;
if(_3c[i]!="GcDndHandle"){
var _41=dojo.query(".GcDndHandle",_3a.domNode);
if(_41.length==0){
dojo.removeClass(_3a.domNode,"GcDndHandle");
}else{
_41.removeClass("GcDndHandle");
}
dojo.addClass(_40,"GcDndHandle");
}
}
}
if(!_3e){
dojo.addClass(_3a.domNode,"GcDndHandle");
}
}
};
dojo.declare("dojox.layout.dnd.DropIndicator",null,{constructor:function(cn,tag){
this.tag=tag||"div";
this.style=cn||null;
},isInserted:function(){
return (this.node.parentNode&&this.node.parentNode.nodeType==1);
},create:function(){
if(this.node&&this.isInserted()){
return this.node;
}
var h="90px";
var el=document.createElement(this.tag);
if(this.style){
el.className=this.style;
el.style.height=h;
}else{
with(el.style){
position="relative";
border="1px dashed #F60";
margin="2px";
height=h;
}
}
this.node=el;
return el;
},destroy:function(){
if(!this.node||!this.isInserted()){
return;
}
this.node.parentNode.removeChild(this.node);
this.node=null;
}});
dojo.extend(dojo.dnd.Manager,{canDrop:function(_46){
var _47=this.target&&_46;
if(this.canDropFlag!=_47){
this.canDropFlag=_47;
if(this.avatar){
this.avatar.update();
}
}
},makeAvatar:function(){
if(this.source.declaredClass=="dojox.layout.dnd.PlottedDnd"){
return new dojox.layout.dnd.Avatar(this,this.source.opacity);
}else{
return new dojo.dnd.Avatar(this);
}
}});
if(dojo.isIE){
dojox.layout.dnd.handdleIE=[dojo.subscribe("/dnd/start",null,function(){
IEonselectstart=document.body.onselectstart;
document.body.onselectstart=function(e){
return false;
};
}),dojo.subscribe("/dnd/cancel",null,function(){
document.body.onselectstart=IEonselectstart;
}),dojo.subscribe("/dnd/drop",null,function(){
document.body.onselectstart=IEonselectstart;
})];
dojo.addOnWindowUnload(function(){
dojo.forEach(dojox.layout.dnd.handdleIE,dojo.unsubscribe);
});
}
}
