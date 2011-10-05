/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.sketch.Figure"]){
dojo._hasResource["dojox.sketch.Figure"]=true;
dojo.provide("dojox.sketch.Figure");
dojo.experimental("dojox.sketch");
dojo.require("dojox.gfx");
dojo.require("dojox.sketch.UndoStack");
(function(){
var ta=dojox.sketch;
ta.tools={};
ta.registerTool=function(_2,fn){
ta.tools[_2]=fn;
};
ta.Figure=function(){
var _4=this;
var _5=1;
this.shapes=[];
this.image=null;
this.imageSrc=null;
this.size={w:0,h:0};
this.surface=null;
this.group=null;
this.node=null;
this.zoomFactor=1;
this.tools=null;
this.nextKey=function(){
return _5++;
};
this.obj={};
this.initUndoStack();
this.selected=[];
this.hasSelections=function(){
return this.selected.length>0;
};
this.isSelected=function(_6){
for(var i=0;i<_4.selected.length;i++){
if(_4.selected[i]==_6){
return true;
}
}
return false;
};
this.select=function(_8){
if(!_4.isSelected(_8)){
_4.clearSelections();
_4.selected=[_8];
}
_8.setMode(ta.Annotation.Modes.View);
_8.setMode(ta.Annotation.Modes.Edit);
};
this.deselect=function(_9){
var _a=-1;
for(var i=0;i<_4.selected.length;i++){
if(_4.selected[i]==_9){
_a=i;
break;
}
}
if(_a>-1){
_9.setMode(ta.Annotation.Modes.View);
_4.selected.splice(_a,1);
}
return _9;
};
this.clearSelections=function(){
for(var i=0;i<_4.selected.length;i++){
_4.selected[i].setMode(ta.Annotation.Modes.View);
}
_4.selected=[];
};
this.replaceSelection=function(n,o){
if(!_4.isSelected(o)){
_4.select(n);
return;
}
var _f=-1;
for(var i=0;i<_4.selected.length;i++){
if(_4.selected[i]==o){
_f=i;
break;
}
}
if(_f>-1){
_4.selected.splice(_f,1,n);
}
};
this._c=null;
this._ctr=null;
this._lp=null;
this._action=null;
this._prevState=null;
this._startPoint=null;
this._ctool=null;
this._start=null;
this._end=null;
this._absEnd=null;
this._cshape=null;
this._click=function(e){
if(_4._c){
dojo.stopEvent(e);
return;
}
var o=_4._fromEvt(e);
if(!o){
_4.clearSelections();
dojo.stopEvent(e);
}else{
if(!o.setMode){
}else{
_4.select(o);
}
}
};
this._dblclick=function(e){
var o=_4._fromEvt(e);
if(o){
_4.onDblClickShape(o,e);
}
};
this._keydown=function(e){
var _16=false;
if(e.ctrlKey){
if(e.keyCode==90){
_4.undo();
_16=true;
}else{
if(e.keyCode==89){
_4.redo();
_16=true;
}
}
}
if(e.keyCode==46||e.keyCode==8){
_4._delete(_4.selected);
_16=true;
}
if(_16){
dojo.stopEvent(e);
}
};
this._md=function(e){
var o=_4._fromEvt(e);
_4._startPoint={x:e.pageX,y:e.pageY};
var win=dijit.getDocumentWindow(_4.node.ownerDocument);
_4._ctr=dojo._abs(_4.node);
var _1a=dojo.withGlobal(win,dojo._docScroll);
_4._ctr={x:_4._ctr.x-_1a.x,y:_4._ctr.y-_1a.y};
var X=e.clientX-_4._ctr.x,Y=e.clientY-_4._ctr.y;
_4._lp={x:X,y:Y};
_4._start={x:X,y:Y};
_4._end={x:X,y:Y};
_4._absEnd={x:X,y:Y};
if(!o){
_4.clearSelections();
_4._ctool.onMouseDown(e);
}else{
if(o.type&&o.type()!="Anchor"){
_4.select(o);
}
o.beginEdit();
_4._c=o;
}
};
this._mm=function(e){
if(!_4._ctr){
return;
}
var x=e.clientX-_4._ctr.x;
var y=e.clientY-_4._ctr.y;
var dx=x-_4._lp.x;
var dy=y-_4._lp.y;
_4._absEnd={x:x,y:y};
if(_4._c){
_4._c.doChange({dx:Math.round(dx/_4.zoomFactor),dy:Math.round(dy/_4.zoomFactor)});
_4._c.setBinding({dx:Math.round(dx/_4.zoomFactor),dy:Math.round(dy/_4.zoomFactor)});
_4._lp={x:x,y:y};
}else{
_4._end={x:dx,y:dy};
var _22={x:Math.min(_4._start.x,_4._absEnd.x),y:Math.min(_4._start.y,_4._absEnd.y),width:Math.abs(_4._start.x-_4._absEnd.x),height:Math.abs(_4._start.y-_4._absEnd.y)};
_4._ctool.onMouseMove(e,_22);
}
};
this._mu=function(e){
if(_4._c){
_4._c.endEdit();
}else{
_4._ctool.onMouseUp(e);
}
_4._c=_4._ctr=_4._lp=_4._action=_4._prevState=_4._startPoint=null;
_4._cshape=_4._start=_4._end=_4._absEnd=null;
};
this._delete=function(arr,_25){
for(var i=0;i<arr.length;i++){
if(!_25){
arr[i].remove();
}
arr[i].setMode(ta.Annotation.Modes.View);
arr[i].destroy();
_4.remove(arr[i]);
_4._remove(arr[i]);
}
arr.splice(0,arr.length);
};
};
var p=ta.Figure.prototype;
p.initUndoStack=function(){
this.history=new ta.UndoStack(this);
};
p.setTool=function(t){
this._ctool=t;
};
p.onDblClickShape=function(_29,e){
if(_29["onDblClick"]){
_29.onDblClick(e);
}
};
p.onCreateShape=function(_2b){
};
p.onBeforeCreateShape=function(_2c){
};
p.initialize=function(_2d){
this.node=_2d;
this.surface=dojox.gfx.createSurface(_2d,this.size.w,this.size.h);
this.surface.createRect({x:0,y:0,width:this.size.w,height:this.size.h}).setFill("white");
this.group=this.surface.createGroup();
this._cons=[];
this._cons.push(dojo.connect(this.node,"ondragstart",dojo,"stopEvent"));
this._cons.push(dojo.connect(this.node,"onselectstart",dojo,"stopEvent"));
this._cons.push(dojo.connect(this.surface.getEventSource(),"onmousedown",this._md));
this._cons.push(dojo.connect(this.surface.getEventSource(),"onmousemove",this._mm));
this._cons.push(dojo.connect(this.surface.getEventSource(),"onmouseup",this._mu));
this._cons.push(dojo.connect(this.surface.getEventSource(),"ondblclick",this._dblclick));
this._cons.push(dojo.connect(this.surface.getEventSource().ownerDocument,"onkeydown",this._keydown));
this.group.createRect({x:0,y:0,width:this.size.w,height:this.size.h});
this.image=this.group.createImage({width:this.size.w,height:this.size.h,src:this.imageSrc});
};
p.destroy=function(_2e){
if(!this.node){
return;
}
if(!_2e){
if(this.history){
this.history.destroy();
}
if(this._subscribed){
dojo.unsubscribe(this._subscribed);
delete this._subscribed;
}
}
dojo.forEach(this._cons,dojo.disconnect);
this._cons=[];
this.node.removeChild(this.surface.getEventSource());
this.group=this.surface=null;
this.obj={};
this.shapes=[];
};
p.draw=function(){
};
p.zoom=function(pct){
this.zoomFactor=pct/100;
var w=this.size.w*this.zoomFactor;
var h=this.size.h*this.zoomFactor;
this.surface.setDimensions(w,h);
this.group.setTransform(dojox.gfx.matrix.scale(this.zoomFactor,this.zoomFactor));
if(dojo.isIE){
this.image.rawNode.style.width=Math.max(w,this.size.w);
this.image.rawNode.style.height=Math.max(h,this.size.h);
}
};
p.getFit=function(){
var wF=(this.node.parentNode.clientWidth-5)/this.size.w;
var hF=(this.node.parentNode.clientHeight-5)/this.size.h;
return Math.min(wF,hF)*100;
};
p.unzoom=function(){
this.zoomFactor=1;
this.surface.setDimensions(this.size.w,this.size.h);
this.group.setTransform();
};
p._add=function(obj){
this.obj[obj._key]=obj;
};
p._remove=function(obj){
if(this.obj[obj._key]){
delete this.obj[obj._key];
}
};
p._get=function(key){
if(key&&key.indexOf("bounding")>-1){
key=key.replace("-boundingBox","");
}
return this.obj[key];
};
p._fromEvt=function(e){
var key=e.target.id+"";
if(key.length==0){
var p=e.target.parentNode;
var _3a=this.surface.getEventSource();
while(p&&p.id.length==0&&p!=_3a){
p=p.parentNode;
}
key=p.id;
}
return this._get(key);
};
p.add=function(_3b){
for(var i=0;i<this.shapes.length;i++){
if(this.shapes[i]==_3b){
return true;
}
}
this.shapes.push(_3b);
return true;
};
p.remove=function(_3d){
var idx=-1;
for(var i=0;i<this.shapes.length;i++){
if(this.shapes[i]==_3d){
idx=i;
break;
}
}
if(idx>-1){
this.shapes.splice(idx,1);
}
return _3d;
};
p.get=function(id){
for(var i=0;i<this.shapes.length;i++){
if(this.shapes[i].id==id){
return this.shapes[i];
}
}
return null;
};
p.convert=function(ann,t){
var _44=t+"Annotation";
if(!ta[_44]){
return;
}
var _45=ann.type(),id=ann.id,_47=ann.label,_48=ann.mode,_49=ann.tokenId;
var _4a,end,_4c,_4d;
switch(_45){
case "Preexisting":
case "Lead":
_4d={dx:ann.transform.dx,dy:ann.transform.dy};
_4a={x:ann.start.x,y:ann.start.y};
end={x:ann.end.x,y:ann.end.y};
var cx=end.x-((end.x-_4a.x)/2);
var cy=end.y-((end.y-_4a.y)/2);
_4c={x:cx,y:cy};
break;
case "SingleArrow":
case "DoubleArrow":
_4d={dx:ann.transform.dx,dy:ann.transform.dy};
_4a={x:ann.start.x,y:ann.start.y};
end={x:ann.end.x,y:ann.end.y};
_4c={x:ann.control.x,y:ann.control.y};
break;
case "Underline":
_4d={dx:ann.transform.dx,dy:ann.transform.dy};
_4a={x:ann.start.x,y:ann.start.y};
_4c={x:_4a.x+50,y:_4a.y+50};
end={x:_4a.x+100,y:_4a.y+100};
break;
case "Brace":
}
var n=new ta[_44](this,id);
if(n.type()=="Underline"){
n.transform={dx:_4d.dx+_4a.x,dy:_4d.dy+_4a.y};
}else{
if(n.transform){
n.transform=_4d;
}
if(n.start){
n.start=_4a;
}
}
if(n.end){
n.end=end;
}
if(n.control){
n.control=_4c;
}
n.label=_47;
n.token=dojo.lang.shallowCopy(ann.token);
n.initialize();
this.replaceSelection(n,ann);
this._remove(ann);
this.remove(ann);
ann.destroy();
n.setMode(_48);
};
p.setValue=function(_51){
var obj=dojox.xml.DomParser.parse(_51);
var _53=this.node;
this.load(obj,_53);
this.zoom(this.zoomFactor*100);
};
p.load=function(obj,n){
if(this.surface){
this.destroy(true);
}
var _56=obj.documentElement;
this.size={w:parseFloat(_56.getAttribute("width"),10),h:parseFloat(_56.getAttribute("height"),10)};
var g=_56.childrenByName("g")[0];
var img=g.childrenByName("image")[0];
this.imageSrc=img.getAttribute("xlink:href");
this.initialize(n);
var ann=g.childrenByName("g");
for(var i=0;i<ann.length;i++){
this._loadAnnotation(ann[i]);
}
if(this._loadDeferred){
this._loadDeferred.callback(this);
this._loadDeferred=null;
}
this.onLoad();
};
p.onLoad=function(){
};
p._loadAnnotation=function(obj){
var _5c=obj.getAttribute("dojoxsketch:type")+"Annotation";
if(ta[_5c]){
var a=new ta[_5c](this,obj.id);
a.initialize(obj);
this.nextKey();
a.setMode(ta.Annotation.Modes.View);
this._add(a);
return a;
}
return null;
};
p.onUndo=function(){
};
p.onBeforeUndo=function(){
};
p.onRedo=function(){
};
p.onBeforeRedo=function(){
};
p.undo=function(){
if(this.history){
this.onBeforeUndo();
this.history.undo();
this.onUndo();
}
};
p.redo=function(){
if(this.history){
this.onBeforeRedo();
this.history.redo();
this.onRedo();
}
};
p.serialize=function(){
var s="<svg xmlns=\"http://www.w3.org/2000/svg\" "+"xmlns:xlink=\"http://www.w3.org/1999/xlink\" "+"xmlns:dojoxsketch=\"http://dojotoolkit.org/dojox/sketch\" "+"width=\""+this.size.w+"\" height=\""+this.size.h+"\">"+"<g>"+"<image xlink:href=\""+this.imageSrc+"\" x=\"0\" y=\"0\" width=\""+this.size.w+"\" height=\""+this.size.h+"\" />";
for(var i=0;i<this.shapes.length;i++){
s+=this.shapes[i].serialize();
}
s+="</g></svg>";
return s;
};
p.getValue=p.serialize;
})();
}
