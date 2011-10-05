/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid.compat._grid.scroller"]){
dojo._hasResource["dojox.grid.compat._grid.scroller"]=true;
dojo.provide("dojox.grid.compat._grid.scroller");
dojo.declare("dojox.grid.scroller.base",null,{constructor:function(){
this.pageHeights=[];
this.stack=[];
},rowCount:0,defaultRowHeight:10,keepRows:100,contentNode:null,scrollboxNode:null,defaultPageHeight:0,keepPages:10,pageCount:0,windowHeight:0,firstVisibleRow:0,lastVisibleRow:0,page:0,pageTop:0,init:function(_1,_2,_3){
switch(arguments.length){
case 3:
this.rowsPerPage=_3;
case 2:
this.keepRows=_2;
case 1:
this.rowCount=_1;
}
this.defaultPageHeight=this.defaultRowHeight*this.rowsPerPage;
this.pageCount=Math.ceil(this.rowCount/this.rowsPerPage);
this.setKeepInfo(this.keepRows);
this.invalidate();
if(this.scrollboxNode){
this.scrollboxNode.scrollTop=0;
this.scroll(0);
this.scrollboxNode.onscroll=dojo.hitch(this,"onscroll");
}
},setKeepInfo:function(_4){
this.keepRows=_4;
this.keepPages=!this.keepRows?this.keepRows:Math.max(Math.ceil(this.keepRows/this.rowsPerPage),2);
},invalidate:function(){
this.invalidateNodes();
this.pageHeights=[];
this.height=(this.pageCount?(this.pageCount-1)*this.defaultPageHeight+this.calcLastPageHeight():0);
this.resize();
},updateRowCount:function(_5){
this.invalidateNodes();
this.rowCount=_5;
oldPageCount=this.pageCount;
this.pageCount=Math.ceil(this.rowCount/this.rowsPerPage);
if(this.pageCount<oldPageCount){
for(var i=oldPageCount-1;i>=this.pageCount;i--){
this.height-=this.getPageHeight(i);
delete this.pageHeights[i];
}
}else{
if(this.pageCount>oldPageCount){
this.height+=this.defaultPageHeight*(this.pageCount-oldPageCount-1)+this.calcLastPageHeight();
}
}
this.resize();
},pageExists:function(_7){
},measurePage:function(_8){
},positionPage:function(_9,_a){
},repositionPages:function(_b){
},installPage:function(_c){
},preparePage:function(_d,_e,_f){
},renderPage:function(_10){
},removePage:function(_11){
},pacify:function(_12){
},pacifying:false,pacifyTicks:200,setPacifying:function(_13){
if(this.pacifying!=_13){
this.pacifying=_13;
this.pacify(this.pacifying);
}
},startPacify:function(){
this.startPacifyTicks=new Date().getTime();
},doPacify:function(){
var _14=(new Date().getTime()-this.startPacifyTicks)>this.pacifyTicks;
this.setPacifying(true);
this.startPacify();
return _14;
},endPacify:function(){
this.setPacifying(false);
},resize:function(){
if(this.scrollboxNode){
this.windowHeight=this.scrollboxNode.clientHeight;
}
dojox.grid.setStyleHeightPx(this.contentNode,this.height);
},calcLastPageHeight:function(){
if(!this.pageCount){
return 0;
}
var _15=this.pageCount-1;
var _16=((this.rowCount%this.rowsPerPage)||(this.rowsPerPage))*this.defaultRowHeight;
this.pageHeights[_15]=_16;
return _16;
},updateContentHeight:function(_17){
this.height+=_17;
this.resize();
},updatePageHeight:function(_18){
if(this.pageExists(_18)){
var oh=this.getPageHeight(_18);
var h=(this.measurePage(_18))||(oh);
this.pageHeights[_18]=h;
if((h)&&(oh!=h)){
this.updateContentHeight(h-oh);
this.repositionPages(_18);
}
}
},rowHeightChanged:function(_1b){
this.updatePageHeight(Math.floor(_1b/this.rowsPerPage));
},invalidateNodes:function(){
while(this.stack.length){
this.destroyPage(this.popPage());
}
},createPageNode:function(){
var p=document.createElement("div");
p.style.position="absolute";
p.style[dojo._isBodyLtr()?"left":"right"]="0";
return p;
},getPageHeight:function(_1d){
var ph=this.pageHeights[_1d];
return (ph!==undefined?ph:this.defaultPageHeight);
},pushPage:function(_1f){
return this.stack.push(_1f);
},popPage:function(){
return this.stack.shift();
},findPage:function(_20){
var i=0,h=0;
for(var ph=0;i<this.pageCount;i++,h+=ph){
ph=this.getPageHeight(i);
if(h+ph>=_20){
break;
}
}
this.page=i;
this.pageTop=h;
},buildPage:function(_24,_25,_26){
this.preparePage(_24,_25);
this.positionPage(_24,_26);
this.installPage(_24);
this.renderPage(_24);
this.pushPage(_24);
},needPage:function(_27,_28){
var h=this.getPageHeight(_27),oh=h;
if(!this.pageExists(_27)){
this.buildPage(_27,this.keepPages&&(this.stack.length>=this.keepPages),_28);
h=this.measurePage(_27)||h;
this.pageHeights[_27]=h;
if(h&&(oh!=h)){
this.updateContentHeight(h-oh);
}
}else{
this.positionPage(_27,_28);
}
return h;
},onscroll:function(){
this.scroll(this.scrollboxNode.scrollTop);
},scroll:function(_2b){
this.startPacify();
this.findPage(_2b);
var h=this.height;
var b=this.getScrollBottom(_2b);
for(var p=this.page,y=this.pageTop;(p<this.pageCount)&&((b<0)||(y<b));p++){
y+=this.needPage(p,y);
}
this.firstVisibleRow=this.getFirstVisibleRow(this.page,this.pageTop,_2b);
this.lastVisibleRow=this.getLastVisibleRow(p-1,y,b);
if(h!=this.height){
this.repositionPages(p-1);
}
this.endPacify();
},getScrollBottom:function(_30){
return (this.windowHeight>=0?_30+this.windowHeight:-1);
},processNodeEvent:function(e,_32){
var t=e.target;
while(t&&(t!=_32)&&t.parentNode&&(t.parentNode.parentNode!=_32)){
t=t.parentNode;
}
if(!t||!t.parentNode||(t.parentNode.parentNode!=_32)){
return false;
}
var _34=t.parentNode;
e.topRowIndex=_34.pageIndex*this.rowsPerPage;
e.rowIndex=e.topRowIndex+dojox.grid.indexInParent(t);
e.rowTarget=t;
return true;
},processEvent:function(e){
return this.processNodeEvent(e,this.contentNode);
},dummy:0});
dojo.declare("dojox.grid.scroller",dojox.grid.scroller.base,{constructor:function(){
this.pageNodes=[];
},renderRow:function(_36,_37){
},removeRow:function(_38){
},getDefaultNodes:function(){
return this.pageNodes;
},getDefaultPageNode:function(_39){
return this.getDefaultNodes()[_39];
},positionPageNode:function(_3a,_3b){
_3a.style.top=_3b+"px";
},getPageNodePosition:function(_3c){
return _3c.offsetTop;
},repositionPageNodes:function(_3d,_3e){
var _3f=0;
for(var i=0;i<this.stack.length;i++){
_3f=Math.max(this.stack[i],_3f);
}
var n=_3e[_3d];
var y=(n?this.getPageNodePosition(n)+this.getPageHeight(_3d):0);
for(var p=_3d+1;p<=_3f;p++){
n=_3e[p];
if(n){
if(this.getPageNodePosition(n)==y){
return;
}
this.positionPage(p,y);
}
y+=this.getPageHeight(p);
}
},invalidatePageNode:function(_44,_45){
var p=_45[_44];
if(p){
delete _45[_44];
this.removePage(_44,p);
dojox.grid.cleanNode(p);
p.innerHTML="";
}
return p;
},preparePageNode:function(_47,_48,_49){
var p=(_48===null?this.createPageNode():this.invalidatePageNode(_48,_49));
p.pageIndex=_47;
p.id=(this._pageIdPrefix||"")+"page-"+_47;
_49[_47]=p;
},pageExists:function(_4b){
return Boolean(this.getDefaultPageNode(_4b));
},measurePage:function(_4c){
var p=this.getDefaultPageNode(_4c);
var h=p.offsetHeight;
if(!this._defaultRowHeight){
if(p){
this._defaultRowHeight=8;
var fr=p.firstChild;
if(fr){
var _50=dojo.doc.createTextNode("T");
fr.appendChild(_50);
this._defaultRowHeight=fr.offsetHeight;
fr.removeChild(_50);
}
}
}
return (this.rowsPerPage==h)?(h*this._defaultRowHeight):h;
},positionPage:function(_51,_52){
this.positionPageNode(this.getDefaultPageNode(_51),_52);
},repositionPages:function(_53){
this.repositionPageNodes(_53,this.getDefaultNodes());
},preparePage:function(_54,_55){
this.preparePageNode(_54,(_55?this.popPage():null),this.getDefaultNodes());
},installPage:function(_56){
this.contentNode.appendChild(this.getDefaultPageNode(_56));
},destroyPage:function(_57){
var p=this.invalidatePageNode(_57,this.getDefaultNodes());
dojox.grid.removeNode(p);
},renderPage:function(_59){
var _5a=this.pageNodes[_59];
for(var i=0,j=_59*this.rowsPerPage;(i<this.rowsPerPage)&&(j<this.rowCount);i++,j++){
this.renderRow(j,_5a);
}
},removePage:function(_5d){
for(var i=0,j=_5d*this.rowsPerPage;i<this.rowsPerPage;i++,j++){
this.removeRow(j);
}
},getPageRow:function(_60){
return _60*this.rowsPerPage;
},getLastPageRow:function(_61){
return Math.min(this.rowCount,this.getPageRow(_61+1))-1;
},getFirstVisibleRowNodes:function(_62,_63,_64,_65){
var row=this.getPageRow(_62);
var _67=dojox.grid.divkids(_65[_62]);
for(var i=0,l=_67.length;i<l&&_63<_64;i++,row++){
_63+=_67[i].offsetHeight;
}
return (row?row-1:row);
},getFirstVisibleRow:function(_6a,_6b,_6c){
if(!this.pageExists(_6a)){
return 0;
}
return this.getFirstVisibleRowNodes(_6a,_6b,_6c,this.getDefaultNodes());
},getLastVisibleRowNodes:function(_6d,_6e,_6f,_70){
var row=this.getLastPageRow(_6d);
var _72=dojox.grid.divkids(_70[_6d]);
for(var i=_72.length-1;i>=0&&_6e>_6f;i--,row--){
_6e-=_72[i].offsetHeight;
}
return row+1;
},getLastVisibleRow:function(_74,_75,_76){
if(!this.pageExists(_74)){
return 0;
}
return this.getLastVisibleRowNodes(_74,_75,_76,this.getDefaultNodes());
},findTopRowForNodes:function(_77,_78){
var _79=dojox.grid.divkids(_78[this.page]);
for(var i=0,l=_79.length,t=this.pageTop,h;i<l;i++){
h=_79[i].offsetHeight;
t+=h;
if(t>=_77){
this.offset=h-(t-_77);
return i+this.page*this.rowsPerPage;
}
}
return -1;
},findScrollTopForNodes:function(_7e,_7f){
var _80=Math.floor(_7e/this.rowsPerPage);
var t=0;
for(var i=0;i<_80;i++){
t+=this.getPageHeight(i);
}
this.pageTop=t;
this.needPage(_80,this.pageTop);
var _83=dojox.grid.divkids(_7f[_80]);
var r=_7e-this.rowsPerPage*_80;
for(var i=0,l=_83.length;i<l&&i<r;i++){
t+=_83[i].offsetHeight;
}
return t;
},findTopRow:function(_86){
return this.findTopRowForNodes(_86,this.getDefaultNodes());
},findScrollTop:function(_87){
return this.findScrollTopForNodes(_87,this.getDefaultNodes());
},dummy:0});
dojo.declare("dojox.grid.scroller.columns",dojox.grid.scroller,{constructor:function(_88){
this.setContentNodes(_88);
},setContentNodes:function(_89){
this.contentNodes=_89;
this.colCount=(this.contentNodes?this.contentNodes.length:0);
this.pageNodes=[];
for(var i=0;i<this.colCount;i++){
this.pageNodes[i]=[];
}
},getDefaultNodes:function(){
return this.pageNodes[0]||[];
},scroll:function(_8b){
if(this.colCount){
dojox.grid.scroller.prototype.scroll.call(this,_8b);
}
},resize:function(){
if(this.scrollboxNode){
this.windowHeight=this.scrollboxNode.clientHeight;
}
for(var i=0;i<this.colCount;i++){
dojox.grid.setStyleHeightPx(this.contentNodes[i],this.height);
}
},positionPage:function(_8d,_8e){
for(var i=0;i<this.colCount;i++){
this.positionPageNode(this.pageNodes[i][_8d],_8e);
}
},preparePage:function(_90,_91){
var p=(_91?this.popPage():null);
for(var i=0;i<this.colCount;i++){
this.preparePageNode(_90,p,this.pageNodes[i]);
}
},installPage:function(_94){
for(var i=0;i<this.colCount;i++){
this.contentNodes[i].appendChild(this.pageNodes[i][_94]);
}
},destroyPage:function(_96){
for(var i=0;i<this.colCount;i++){
dojox.grid.removeNode(this.invalidatePageNode(_96,this.pageNodes[i]));
}
},renderPage:function(_98){
var _99=[];
for(var i=0;i<this.colCount;i++){
_99[i]=this.pageNodes[i][_98];
}
for(var i=0,j=_98*this.rowsPerPage;(i<this.rowsPerPage)&&(j<this.rowCount);i++,j++){
this.renderRow(j,_99);
}
}});
}
