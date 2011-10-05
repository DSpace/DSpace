/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._base.scroll"]){
dojo._hasResource["dijit._base.scroll"]=true;
dojo.provide("dijit._base.scroll");
dijit.scrollIntoView=function(_1){
_1=dojo.byId(_1);
var _2=_1.ownerDocument.body;
var _3=_2.parentNode;
if(dojo.isFF==2||_1==_2||_1==_3){
_1.scrollIntoView(false);
return;
}
var _4=!dojo._isBodyLtr();
var _5=dojo.doc.compatMode!="BackCompat";
var _6=(_5&&!dojo.isSafari)?_3:_2;
function addPseudoAttrs(_7){
var _8=_7.parentNode;
var _9=_7.offsetParent;
if(_9==null){
_7=_6;
_9=_3;
_8=null;
}
_7._offsetParent=(_9==_2)?_6:_9;
_7._parent=(_8==_2)?_6:_8;
_7._start={H:_7.offsetLeft,V:_7.offsetTop};
_7._scroll={H:_7.scrollLeft,V:_7.scrollTop};
_7._renderedSize={H:_7.offsetWidth,V:_7.offsetHeight};
var bp=dojo._getBorderExtents(_7);
_7._borderStart={H:bp.l,V:bp.t};
_7._borderSize={H:bp.w,V:bp.h};
_7._clientSize=(_7._offsetParent==_3&&dojo.isSafari&&_5)?{H:_3.clientWidth,V:_3.clientHeight}:{H:_7.clientWidth,V:_7.clientHeight};
_7._scrollBarSize={V:null,H:null};
for(var _b in _7._scrollBarSize){
var _c=_7._renderedSize[_b]-_7._clientSize[_b]-_7._borderSize[_b];
_7._scrollBarSize[_b]=(_7._clientSize[_b]>0&&_c>=15&&_c<=17)?_c:0;
}
_7._isScrollable={V:null,H:null};
for(_b in _7._isScrollable){
var _d=_b=="H"?"V":"H";
_7._isScrollable[_b]=_7==_6||_7._scroll[_b]||_7._scrollBarSize[_d];
}
};
var _e=_1;
while(_e!=null){
addPseudoAttrs(_e);
var _f=_e._parent;
if(_f){
_f._child=_e;
}
_e=_f;
}
for(var dir in _6._renderedSize){
_6._renderedSize[dir]=Math.min(_6._clientSize[dir],_6._renderedSize[dir]);
}
var _11=_1;
while(_11!=_6){
_e=_11._parent;
if(_e.tagName=="TD"){
var _12=_e._parent._parent._parent;
if(_12._offsetParent==_11._offsetParent&&_e._offsetParent!=_11._offsetParent){
_e=_12;
}
}
var _13=_11==_6||(_e._offsetParent!=_11._offsetParent);
for(dir in _11._start){
var _14=dir=="H"?"V":"H";
if(_4&&dir=="H"&&(dojo.isSafari||dojo.isIE)&&_e._clientSize.H>0){
var _15=_e.scrollWidth-_e._clientSize.H;
if(_15>0){
_e._scroll.H-=_15;
}
}
if(dojo.isIE&&_e._offsetParent.tagName=="TABLE"){
_e._start[dir]-=_e._offsetParent._borderStart[dir];
_e._borderStart[dir]=_e._borderSize[dir]=0;
}
if(_e._clientSize[dir]==0){
_e._renderedSize[dir]=_e._clientSize[dir]=_e._child._clientSize[dir];
if(_4&&dir=="H"){
_e._start[dir]-=_e._renderedSize[dir];
}
}else{
_e._renderedSize[dir]-=_e._borderSize[dir]+_e._scrollBarSize[dir];
}
_e._start[dir]+=_e._borderStart[dir];
var _16=_11._start[dir]-(_13?0:_e._start[dir])-_e._scroll[dir];
var _17=_16+_11._renderedSize[dir]-_e._renderedSize[dir];
var _18,_19=(dir=="H")?"scrollLeft":"scrollTop";
var _1a=(dir=="H"&&_4);
var _1b=_1a?-_17:_16;
var _1c=_1a?-_16:_17;
if(_1b<=0){
_18=_1b;
}else{
if(_1c<=0){
_18=0;
}else{
if(_1b<_1c){
_18=_1b;
}else{
_18=_1c;
}
}
}
var _1d=0;
if(_18!=0){
var _1e=_e[_19];
_e[_19]+=_1a?-_18:_18;
_1d=_e[_19]-_1e;
_16-=_1d;
_1c-=_1a?-_1d:_1d;
}
_e._renderedSize[dir]=_11._renderedSize[dir]+_e._scrollBarSize[dir]-((_e._isScrollable[dir]&&_1c>0)?_1c:0);
_e._start[dir]+=(_16>=0||!_e._isScrollable[dir])?_16:0;
}
_11=_e;
}
};
}
