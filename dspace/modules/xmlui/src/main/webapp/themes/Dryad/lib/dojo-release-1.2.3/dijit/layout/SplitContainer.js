/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.layout.SplitContainer"]){
dojo._hasResource["dijit.layout.SplitContainer"]=true;
dojo.provide("dijit.layout.SplitContainer");
dojo.require("dojo.cookie");
dojo.require("dijit.layout._LayoutWidget");
dojo.declare("dijit.layout.SplitContainer",dijit.layout._LayoutWidget,{constructor:function(){
dojo.deprecated("dijit.layout.SplitContainer is deprecated","use BorderContainer with splitter instead",2);
},activeSizing:false,sizerWidth:7,orientation:"horizontal",persist:true,baseClass:"dijitSplitContainer",postMixInProperties:function(){
this.inherited("postMixInProperties",arguments);
this.isHorizontal=(this.orientation=="horizontal");
},postCreate:function(){
this.inherited(arguments);
this.sizers=[];
if(dojo.isMozilla){
this.domNode.style.overflow="-moz-scrollbars-none";
}
if(typeof this.sizerWidth=="object"){
try{
this.sizerWidth=parseInt(this.sizerWidth.toString());
}
catch(e){
this.sizerWidth=7;
}
}
var _1=this.virtualSizer=dojo.doc.createElement("div");
_1.style.position="relative";
_1.style.zIndex=10;
_1.className=this.isHorizontal?"dijitSplitContainerVirtualSizerH":"dijitSplitContainerVirtualSizerV";
this.domNode.appendChild(_1);
dojo.setSelectable(_1,false);
},destroy:function(){
delete this.virtualSizer;
dojo.forEach(this._ownconnects,dojo.disconnect);
this.inherited(arguments);
},startup:function(){
if(this._started){
return;
}
dojo.forEach(this.getChildren(),function(_2,i,_4){
this._setupChild(_2);
if(i<_4.length-1){
this._addSizer();
}
},this);
if(this.persist){
this._restoreState();
}
this.inherited(arguments);
},_setupChild:function(_5){
this.inherited(arguments);
_5.domNode.style.position="absolute";
dojo.addClass(_5.domNode,"dijitSplitPane");
},_addSizer:function(){
var i=this.sizers.length;
var _7=this.sizers[i]=dojo.doc.createElement("div");
this.domNode.appendChild(_7);
_7.className=this.isHorizontal?"dijitSplitContainerSizerH":"dijitSplitContainerSizerV";
var _8=dojo.doc.createElement("div");
_8.className="thumb";
_7.appendChild(_8);
var _9=this;
var _a=(function(){
var _b=i;
return function(e){
_9.beginSizing(e,_b);
};
})();
this.connect(_7,"onmousedown",_a);
dojo.setSelectable(_7,false);
},removeChild:function(_d){
if(this.sizers.length){
var i=dojo.indexOf(this.getChildren(),_d);
if(i!=-1){
if(i==this.sizers.length){
i--;
}
dojo._destroyElement(this.sizers[i]);
this.sizers.splice(i,1);
}
}
this.inherited(arguments);
if(this._started){
this.layout();
}
},addChild:function(_f,_10){
this.inherited(arguments);
if(this._started){
var _11=this.getChildren();
if(_11.length>1){
this._addSizer();
}
this.layout();
}
},layout:function(){
this.paneWidth=this._contentBox.w;
this.paneHeight=this._contentBox.h;
var _12=this.getChildren();
if(!_12.length){
return;
}
var _13=this.isHorizontal?this.paneWidth:this.paneHeight;
if(_12.length>1){
_13-=this.sizerWidth*(_12.length-1);
}
var _14=0;
dojo.forEach(_12,function(_15){
_14+=_15.sizeShare;
});
var _16=_13/_14;
var _17=0;
dojo.forEach(_12.slice(0,_12.length-1),function(_18){
var _19=Math.round(_16*_18.sizeShare);
_18.sizeActual=_19;
_17+=_19;
});
_12[_12.length-1].sizeActual=_13-_17;
this._checkSizes();
var pos=0;
var _1b=_12[0].sizeActual;
this._movePanel(_12[0],pos,_1b);
_12[0].position=pos;
pos+=_1b;
if(!this.sizers){
return;
}
dojo.some(_12.slice(1),function(_1c,i){
if(!this.sizers[i]){
return true;
}
this._moveSlider(this.sizers[i],pos,this.sizerWidth);
this.sizers[i].position=pos;
pos+=this.sizerWidth;
_1b=_1c.sizeActual;
this._movePanel(_1c,pos,_1b);
_1c.position=pos;
pos+=_1b;
},this);
},_movePanel:function(_1e,pos,_20){
if(this.isHorizontal){
_1e.domNode.style.left=pos+"px";
_1e.domNode.style.top=0;
var box={w:_20,h:this.paneHeight};
if(_1e.resize){
_1e.resize(box);
}else{
dojo.marginBox(_1e.domNode,box);
}
}else{
_1e.domNode.style.left=0;
_1e.domNode.style.top=pos+"px";
var box={w:this.paneWidth,h:_20};
if(_1e.resize){
_1e.resize(box);
}else{
dojo.marginBox(_1e.domNode,box);
}
}
},_moveSlider:function(_22,pos,_24){
if(this.isHorizontal){
_22.style.left=pos+"px";
_22.style.top=0;
dojo.marginBox(_22,{w:_24,h:this.paneHeight});
}else{
_22.style.left=0;
_22.style.top=pos+"px";
dojo.marginBox(_22,{w:this.paneWidth,h:_24});
}
},_growPane:function(_25,_26){
if(_25>0){
if(_26.sizeActual>_26.sizeMin){
if((_26.sizeActual-_26.sizeMin)>_25){
_26.sizeActual=_26.sizeActual-_25;
_25=0;
}else{
_25-=_26.sizeActual-_26.sizeMin;
_26.sizeActual=_26.sizeMin;
}
}
}
return _25;
},_checkSizes:function(){
var _27=0;
var _28=0;
var _29=this.getChildren();
dojo.forEach(_29,function(_2a){
_28+=_2a.sizeActual;
_27+=_2a.sizeMin;
});
if(_27<=_28){
var _2b=0;
dojo.forEach(_29,function(_2c){
if(_2c.sizeActual<_2c.sizeMin){
_2b+=_2c.sizeMin-_2c.sizeActual;
_2c.sizeActual=_2c.sizeMin;
}
});
if(_2b>0){
var _2d=this.isDraggingLeft?_29.reverse():_29;
dojo.forEach(_2d,function(_2e){
_2b=this._growPane(_2b,_2e);
},this);
}
}else{
dojo.forEach(_29,function(_2f){
_2f.sizeActual=Math.round(_28*(_2f.sizeMin/_27));
});
}
},beginSizing:function(e,i){
var _32=this.getChildren();
this.paneBefore=_32[i];
this.paneAfter=_32[i+1];
this.isSizing=true;
this.sizingSplitter=this.sizers[i];
if(!this.cover){
this.cover=dojo.doc.createElement("div");
this.domNode.appendChild(this.cover);
var s=this.cover.style;
s.position="absolute";
s.zIndex=1;
s.top=0;
s.left=0;
s.width="100%";
s.height="100%";
}else{
this.cover.style.zIndex=1;
}
this.sizingSplitter.style.zIndex=2;
this.originPos=dojo.coords(_32[0].domNode,true);
if(this.isHorizontal){
var _34=e.layerX||e.offsetX||0;
var _35=e.pageX;
this.originPos=this.originPos.x;
}else{
var _34=e.layerY||e.offsetY||0;
var _35=e.pageY;
this.originPos=this.originPos.y;
}
this.startPoint=this.lastPoint=_35;
this.screenToClientOffset=_35-_34;
this.dragOffset=this.lastPoint-this.paneBefore.sizeActual-this.originPos-this.paneBefore.position;
if(!this.activeSizing){
this._showSizingLine();
}
this._ownconnects=[];
this._ownconnects.push(dojo.connect(dojo.doc.documentElement,"onmousemove",this,"changeSizing"));
this._ownconnects.push(dojo.connect(dojo.doc.documentElement,"onmouseup",this,"endSizing"));
dojo.stopEvent(e);
},changeSizing:function(e){
if(!this.isSizing){
return;
}
this.lastPoint=this.isHorizontal?e.pageX:e.pageY;
this.movePoint();
if(this.activeSizing){
this._updateSize();
}else{
this._moveSizingLine();
}
dojo.stopEvent(e);
},endSizing:function(e){
if(!this.isSizing){
return;
}
if(this.cover){
this.cover.style.zIndex=-1;
}
if(!this.activeSizing){
this._hideSizingLine();
}
this._updateSize();
this.isSizing=false;
if(this.persist){
this._saveState(this);
}
dojo.forEach(this._ownconnects,dojo.disconnect);
},movePoint:function(){
var p=this.lastPoint-this.screenToClientOffset;
var a=p-this.dragOffset;
a=this.legaliseSplitPoint(a);
p=a+this.dragOffset;
this.lastPoint=p+this.screenToClientOffset;
},legaliseSplitPoint:function(a){
a+=this.sizingSplitter.position;
this.isDraggingLeft=!!(a>0);
if(!this.activeSizing){
var min=this.paneBefore.position+this.paneBefore.sizeMin;
if(a<min){
a=min;
}
var max=this.paneAfter.position+(this.paneAfter.sizeActual-(this.sizerWidth+this.paneAfter.sizeMin));
if(a>max){
a=max;
}
}
a-=this.sizingSplitter.position;
this._checkSizes();
return a;
},_updateSize:function(){
var pos=this.lastPoint-this.dragOffset-this.originPos;
var _3e=this.paneBefore.position;
var _3f=this.paneAfter.position+this.paneAfter.sizeActual;
this.paneBefore.sizeActual=pos-_3e;
this.paneAfter.position=pos+this.sizerWidth;
this.paneAfter.sizeActual=_3f-this.paneAfter.position;
dojo.forEach(this.getChildren(),function(_40){
_40.sizeShare=_40.sizeActual;
});
if(this._started){
this.layout();
}
},_showSizingLine:function(){
this._moveSizingLine();
dojo.marginBox(this.virtualSizer,this.isHorizontal?{w:this.sizerWidth,h:this.paneHeight}:{w:this.paneWidth,h:this.sizerWidth});
this.virtualSizer.style.display="block";
},_hideSizingLine:function(){
this.virtualSizer.style.display="none";
},_moveSizingLine:function(){
var pos=(this.lastPoint-this.startPoint)+this.sizingSplitter.position;
dojo.style(this.virtualSizer,(this.isHorizontal?"left":"top"),pos+"px");
},_getCookieName:function(i){
return this.id+"_"+i;
},_restoreState:function(){
dojo.forEach(this.getChildren(),function(_43,i){
var _45=this._getCookieName(i);
var _46=dojo.cookie(_45);
if(_46){
var pos=parseInt(_46);
if(typeof pos=="number"){
_43.sizeShare=pos;
}
}
},this);
},_saveState:function(){
if(!this.persist){
return;
}
dojo.forEach(this.getChildren(),function(_48,i){
dojo.cookie(this._getCookieName(i),_48.sizeShare,{expires:365});
},this);
}});
dojo.extend(dijit._Widget,{sizeMin:10,sizeShare:10});
}
