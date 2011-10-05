/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid._Scroller"]){
dojo._hasResource["dojox.grid._Scroller"]=true;
dojo.provide("dojox.grid._Scroller");
(function(){
var _1=function(_2){
var i=0,n,p=_2.parentNode;
while((n=p.childNodes[i++])){
if(n==_2){
return i-1;
}
}
return -1;
};
var _6=function(_7){
if(!_7){
return;
}
var _8=function(_9){
return _9.domNode&&dojo.isDescendant(_9.domNode,_7,true);
};
var ws=dijit.registry.filter(_8);
for(var i=0,w;(w=ws[i]);i++){
w.destroy();
}
delete ws;
};
var _d=function(_e){
var _f=dojo.byId(_e);
return (_f&&_f.tagName?_f.tagName.toLowerCase():"");
};
var _10=function(_11,_12){
var _13=[];
var i=0,n;
while((n=_11.childNodes[i++])){
if(_d(n)==_12){
_13.push(n);
}
}
return _13;
};
var _16=function(_17){
return _10(_17,"div");
};
dojo.declare("dojox.grid._Scroller",null,{constructor:function(_18){
this.setContentNodes(_18);
this.pageHeights=[];
this.pageNodes=[];
this.stack=[];
},rowCount:0,defaultRowHeight:32,keepRows:100,contentNode:null,scrollboxNode:null,defaultPageHeight:0,keepPages:10,pageCount:0,windowHeight:0,firstVisibleRow:0,lastVisibleRow:0,averageRowHeight:0,page:0,pageTop:0,init:function(_19,_1a,_1b){
switch(arguments.length){
case 3:
this.rowsPerPage=_1b;
case 2:
this.keepRows=_1a;
case 1:
this.rowCount=_19;
}
this.defaultPageHeight=this.defaultRowHeight*this.rowsPerPage;
this.pageCount=this._getPageCount(this.rowCount,this.rowsPerPage);
this.setKeepInfo(this.keepRows);
this.invalidate();
if(this.scrollboxNode){
this.scrollboxNode.scrollTop=0;
this.scroll(0);
this.scrollboxNode.onscroll=dojo.hitch(this,"onscroll");
}
},_getPageCount:function(_1c,_1d){
return _1c?(Math.ceil(_1c/_1d)||1):0;
},destroy:function(){
this.invalidateNodes();
delete this.contentNodes;
delete this.contentNode;
delete this.scrollboxNode;
},setKeepInfo:function(_1e){
this.keepRows=_1e;
this.keepPages=!this.keepRows?this.keepRows:Math.max(Math.ceil(this.keepRows/this.rowsPerPage),2);
},setContentNodes:function(_1f){
this.contentNodes=_1f;
this.colCount=(this.contentNodes?this.contentNodes.length:0);
this.pageNodes=[];
for(var i=0;i<this.colCount;i++){
this.pageNodes[i]=[];
}
},getDefaultNodes:function(){
return this.pageNodes[0]||[];
},invalidate:function(){
this.invalidateNodes();
this.pageHeights=[];
this.height=(this.pageCount?(this.pageCount-1)*this.defaultPageHeight+this.calcLastPageHeight():0);
this.resize();
},updateRowCount:function(_21){
this.invalidateNodes();
this.rowCount=_21;
var _22=this.pageCount;
this.pageCount=this._getPageCount(this.rowCount,this.rowsPerPage);
if(this.pageCount<_22){
for(var i=_22-1;i>=this.pageCount;i--){
this.height-=this.getPageHeight(i);
delete this.pageHeights[i];
}
}else{
if(this.pageCount>_22){
this.height+=this.defaultPageHeight*(this.pageCount-_22-1)+this.calcLastPageHeight();
}
}
this.resize();
},pageExists:function(_24){
return Boolean(this.getDefaultPageNode(_24));
},measurePage:function(_25){
var n=this.getDefaultPageNode(_25);
return (n&&n.innerHTML)?n.offsetHeight:0;
},positionPage:function(_27,_28){
for(var i=0;i<this.colCount;i++){
this.pageNodes[i][_27].style.top=_28+"px";
}
},repositionPages:function(_2a){
var _2b=this.getDefaultNodes();
var _2c=0;
for(var i=0;i<this.stack.length;i++){
_2c=Math.max(this.stack[i],_2c);
}
var n=_2b[_2a];
var y=(n?this.getPageNodePosition(n)+this.getPageHeight(_2a):0);
for(var p=_2a+1;p<=_2c;p++){
n=_2b[p];
if(n){
if(this.getPageNodePosition(n)==y){
return;
}
this.positionPage(p,y);
}
y+=this.getPageHeight(p);
}
},installPage:function(_31){
for(var i=0;i<this.colCount;i++){
this.contentNodes[i].appendChild(this.pageNodes[i][_31]);
}
},preparePage:function(_33,_34,_35){
var p=(_35?this.popPage():null);
for(var i=0;i<this.colCount;i++){
var _38=this.pageNodes[i];
var _39=(p===null?this.createPageNode():this.invalidatePageNode(p,_38));
_39.pageIndex=_33;
_39.id=(this._pageIdPrefix||"")+"page-"+_33;
_38[_33]=_39;
}
},renderPage:function(_3a){
var _3b=[];
for(var i=0;i<this.colCount;i++){
_3b[i]=this.pageNodes[i][_3a];
}
for(var i=0,j=_3a*this.rowsPerPage;(i<this.rowsPerPage)&&(j<this.rowCount);i++,j++){
this.renderRow(j,_3b);
}
},removePage:function(_3e){
for(var i=0,j=_3e*this.rowsPerPage;i<this.rowsPerPage;i++,j++){
this.removeRow(j);
}
},destroyPage:function(_41){
for(var i=0;i<this.colCount;i++){
var n=this.invalidatePageNode(_41,this.pageNodes[i]);
if(n){
dojo._destroyElement(n);
}
}
},pacify:function(_44){
},pacifying:false,pacifyTicks:200,setPacifying:function(_45){
if(this.pacifying!=_45){
this.pacifying=_45;
this.pacify(this.pacifying);
}
},startPacify:function(){
this.startPacifyTicks=new Date().getTime();
},doPacify:function(){
var _46=(new Date().getTime()-this.startPacifyTicks)>this.pacifyTicks;
this.setPacifying(true);
this.startPacify();
return _46;
},endPacify:function(){
this.setPacifying(false);
},resize:function(){
if(this.scrollboxNode){
this.windowHeight=this.scrollboxNode.clientHeight;
}
for(var i=0;i<this.colCount;i++){
dojox.grid.util.setStyleHeightPx(this.contentNodes[i],this.height);
}
this.needPage(this.page,this.pageTop);
var _48=(this.page<this.pageCount-1)?this.rowsPerPage:(this.rowCount%this.rowsPerPage);
var _49=this.getPageHeight(this.page);
this.averageRowHeight=(_49>0&&_48>0)?(_49/_48):0;
},calcLastPageHeight:function(){
if(!this.pageCount){
return 0;
}
var _4a=this.pageCount-1;
var _4b=((this.rowCount%this.rowsPerPage)||(this.rowsPerPage))*this.defaultRowHeight;
this.pageHeights[_4a]=_4b;
return _4b;
},updateContentHeight:function(_4c){
this.height+=_4c;
this.resize();
},updatePageHeight:function(_4d){
if(this.pageExists(_4d)){
var oh=this.getPageHeight(_4d);
var h=(this.measurePage(_4d))||(oh);
this.pageHeights[_4d]=h;
if((h)&&(oh!=h)){
this.updateContentHeight(h-oh);
this.repositionPages(_4d);
}
}
},rowHeightChanged:function(_50){
this.updatePageHeight(Math.floor(_50/this.rowsPerPage));
},invalidateNodes:function(){
while(this.stack.length){
this.destroyPage(this.popPage());
}
},createPageNode:function(){
var p=document.createElement("div");
p.style.position="absolute";
p.style[dojo._isBodyLtr()?"left":"right"]="0";
return p;
},getPageHeight:function(_52){
var ph=this.pageHeights[_52];
return (ph!==undefined?ph:this.defaultPageHeight);
},pushPage:function(_54){
return this.stack.push(_54);
},popPage:function(){
return this.stack.shift();
},findPage:function(_55){
var i=0,h=0;
for(var ph=0;i<this.pageCount;i++,h+=ph){
ph=this.getPageHeight(i);
if(h+ph>=_55){
break;
}
}
this.page=i;
this.pageTop=h;
},buildPage:function(_59,_5a,_5b){
this.preparePage(_59,_5a);
this.positionPage(_59,_5b);
this.installPage(_59);
this.renderPage(_59);
this.pushPage(_59);
},needPage:function(_5c,_5d){
var h=this.getPageHeight(_5c),oh=h;
if(!this.pageExists(_5c)){
this.buildPage(_5c,this.keepPages&&(this.stack.length>=this.keepPages),_5d);
h=this.measurePage(_5c)||h;
this.pageHeights[_5c]=h;
if(h&&(oh!=h)){
this.updateContentHeight(h-oh);
}
}else{
this.positionPage(_5c,_5d);
}
return h;
},onscroll:function(){
this.scroll(this.scrollboxNode.scrollTop);
},scroll:function(_60){
this.grid.scrollTop=_60;
if(this.colCount){
this.startPacify();
this.findPage(_60);
var h=this.height;
var b=this.getScrollBottom(_60);
for(var p=this.page,y=this.pageTop;(p<this.pageCount)&&((b<0)||(y<b));p++){
y+=this.needPage(p,y);
}
this.firstVisibleRow=this.getFirstVisibleRow(this.page,this.pageTop,_60);
this.lastVisibleRow=this.getLastVisibleRow(p-1,y,b);
if(h!=this.height){
this.repositionPages(p-1);
}
this.endPacify();
}
},getScrollBottom:function(_65){
return (this.windowHeight>=0?_65+this.windowHeight:-1);
},processNodeEvent:function(e,_67){
var t=e.target;
while(t&&(t!=_67)&&t.parentNode&&(t.parentNode.parentNode!=_67)){
t=t.parentNode;
}
if(!t||!t.parentNode||(t.parentNode.parentNode!=_67)){
return false;
}
var _69=t.parentNode;
e.topRowIndex=_69.pageIndex*this.rowsPerPage;
e.rowIndex=e.topRowIndex+_1(t);
e.rowTarget=t;
return true;
},processEvent:function(e){
return this.processNodeEvent(e,this.contentNode);
},renderRow:function(_6b,_6c){
},removeRow:function(_6d){
},getDefaultPageNode:function(_6e){
return this.getDefaultNodes()[_6e];
},positionPageNode:function(_6f,_70){
},getPageNodePosition:function(_71){
return _71.offsetTop;
},invalidatePageNode:function(_72,_73){
var p=_73[_72];
if(p){
delete _73[_72];
this.removePage(_72,p);
_6(p);
p.innerHTML="";
}
return p;
},getPageRow:function(_75){
return _75*this.rowsPerPage;
},getLastPageRow:function(_76){
return Math.min(this.rowCount,this.getPageRow(_76+1))-1;
},getFirstVisibleRow:function(_77,_78,_79){
if(!this.pageExists(_77)){
return 0;
}
var row=this.getPageRow(_77);
var _7b=this.getDefaultNodes();
var _7c=_16(_7b[_77]);
for(var i=0,l=_7c.length;i<l&&_78<_79;i++,row++){
_78+=_7c[i].offsetHeight;
}
return (row?row-1:row);
},getLastVisibleRow:function(_7f,_80,_81){
if(!this.pageExists(_7f)){
return 0;
}
var _82=this.getDefaultNodes();
var row=this.getLastPageRow(_7f);
var _84=_16(_82[_7f]);
for(var i=_84.length-1;i>=0&&_80>_81;i--,row--){
_80-=_84[i].offsetHeight;
}
return row+1;
},findTopRow:function(_86){
var _87=this.getDefaultNodes();
var _88=_16(_87[this.page]);
for(var i=0,l=_88.length,t=this.pageTop,h;i<l;i++){
h=_88[i].offsetHeight;
t+=h;
if(t>=_86){
this.offset=h-(t-_86);
return i+this.page*this.rowsPerPage;
}
}
return -1;
},findScrollTop:function(_8d){
var _8e=Math.floor(_8d/this.rowsPerPage);
var t=0;
for(var i=0;i<_8e;i++){
t+=this.getPageHeight(i);
}
this.pageTop=t;
this.needPage(_8e,this.pageTop);
var _91=this.getDefaultNodes();
var _92=_16(_91[_8e]);
var r=_8d-this.rowsPerPage*_8e;
for(var i=0,l=_92.length;i<l&&i<r;i++){
t+=_92[i].offsetHeight;
}
return t;
},dummy:0});
})();
}
