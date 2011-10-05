/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid._View"]){
dojo._hasResource["dojox.grid._View"]=true;
dojo.provide("dojox.grid._View");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dojox.html.metrics");
dojo.require("dojox.grid.util");
dojo.require("dojo.dnd.Moveable");
dojo.require("dojo.dnd.Source");
dojo.require("dojo.dnd.Manager");
(function(){
var _1="gridRowIndex";
var _2="gridView";
var _3=function(td){
return td.cellIndex>=0?td.cellIndex:dojo.indexOf(td.parentNode.cells,td);
};
var _5=function(tr){
return tr.rowIndex>=0?tr.rowIndex:dojo.indexOf(tr.parentNode.childNodes,tr);
};
var _7=function(_8,_9){
return _8&&((_8.rows||0)[_9]||_8.childNodes[_9]);
};
var _a=function(_b){
for(var n=_b;n&&n.tagName!="TABLE";n=n.parentNode){
}
return n;
};
var _d=function(_e,_f){
for(var n=_e;n&&_f(n);n=n.parentNode){
}
return n;
};
var _11=function(_12){
var _13=_12.toUpperCase();
return function(_14){
return _14.tagName!=_13;
};
};
var _15=function(_16,_17){
return _16.style.cssText==undefined?_16.getAttribute("style"):_16.style.cssText;
};
var _18=function(_19){
if(_19){
this.view=_19;
this.grid=_19.grid;
}
};
dojo.extend(_18,{view:null,_table:"<table class=\"dojoxGridRowTable\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" role=\"wairole:presentation\"",getTableArray:function(){
var _1a=[this._table];
if(this.view.viewWidth){
_1a.push([" style=\"width:",this.view.viewWidth,";\""].join(""));
}
_1a.push(">");
return _1a;
},generateCellMarkup:function(_1b,_1c,_1d,_1e){
var _1f=[],_20;
if(_1e){
_20=["<th tabIndex=\"-1\" role=\"wairole:columnheader\""];
}else{
_20=["<td tabIndex=\"-1\" role=\"wairole:gridcell\""];
}
_1b.colSpan&&_20.push(" colspan=\"",_1b.colSpan,"\"");
_1b.rowSpan&&_20.push(" rowspan=\"",_1b.rowSpan,"\"");
_20.push(" class=\"dojoxGridCell ");
_1b.classes&&_20.push(_1b.classes," ");
_1d&&_20.push(_1d," ");
_1f.push(_20.join(""));
_1f.push("");
_20=["\" idx=\"",_1b.index,"\" style=\""];
if(_1c&&_1c[_1c.length-1]!=";"){
_1c+=";";
}
_20.push(_1b.styles,_1c||"",_1b.hidden?"display:none;":"");
_1b.unitWidth&&_20.push("width:",_1b.unitWidth,";");
_1f.push(_20.join(""));
_1f.push("");
_20=["\""];
_1b.attrs&&_20.push(" ",_1b.attrs);
_20.push(">");
_1f.push(_20.join(""));
_1f.push("");
_1f.push("</td>");
return _1f;
},isCellNode:function(_21){
return Boolean(_21&&_21!=dojo.doc&&dojo.attr(_21,"idx"));
},getCellNodeIndex:function(_22){
return _22?Number(dojo.attr(_22,"idx")):-1;
},getCellNode:function(_23,_24){
for(var i=0,row;row=_7(_23.firstChild,i);i++){
for(var j=0,_28;_28=row.cells[j];j++){
if(this.getCellNodeIndex(_28)==_24){
return _28;
}
}
}
},findCellTarget:function(_29,_2a){
var n=_29;
while(n&&(!this.isCellNode(n)||(n.offsetParent&&_2 in n.offsetParent.parentNode&&n.offsetParent.parentNode[_2]!=this.view.id))&&(n!=_2a)){
n=n.parentNode;
}
return n!=_2a?n:null;
},baseDecorateEvent:function(e){
e.dispatch="do"+e.type;
e.grid=this.grid;
e.sourceView=this.view;
e.cellNode=this.findCellTarget(e.target,e.rowNode);
e.cellIndex=this.getCellNodeIndex(e.cellNode);
e.cell=(e.cellIndex>=0?this.grid.getCell(e.cellIndex):null);
},findTarget:function(_2d,_2e){
var n=_2d;
while(n&&(n!=this.domNode)&&(!(_2e in n)||(_2 in n&&n[_2]!=this.view.id))){
n=n.parentNode;
}
return (n!=this.domNode)?n:null;
},findRowTarget:function(_30){
return this.findTarget(_30,_1);
},isIntraNodeEvent:function(e){
try{
return (e.cellNode&&e.relatedTarget&&dojo.isDescendant(e.relatedTarget,e.cellNode));
}
catch(x){
return false;
}
},isIntraRowEvent:function(e){
try{
var row=e.relatedTarget&&this.findRowTarget(e.relatedTarget);
return !row&&(e.rowIndex==-1)||row&&(e.rowIndex==row.gridRowIndex);
}
catch(x){
return false;
}
},dispatchEvent:function(e){
if(e.dispatch in this){
return this[e.dispatch](e);
}
},domouseover:function(e){
if(e.cellNode&&(e.cellNode!=this.lastOverCellNode)){
this.lastOverCellNode=e.cellNode;
this.grid.onMouseOver(e);
}
this.grid.onMouseOverRow(e);
},domouseout:function(e){
if(e.cellNode&&(e.cellNode==this.lastOverCellNode)&&!this.isIntraNodeEvent(e,this.lastOverCellNode)){
this.lastOverCellNode=null;
this.grid.onMouseOut(e);
if(!this.isIntraRowEvent(e)){
this.grid.onMouseOutRow(e);
}
}
},domousedown:function(e){
if(e.cellNode){
this.grid.onMouseDown(e);
}
this.grid.onMouseDownRow(e);
}});
var _38=function(_39){
_18.call(this,_39);
};
_38.prototype=new _18();
dojo.extend(_38,{update:function(){
this.prepareHtml();
},prepareHtml:function(){
var _3a=this.grid.get,_3b=this.view.structure.cells;
for(var j=0,row;(row=_3b[j]);j++){
for(var i=0,_3f;(_3f=row[i]);i++){
_3f.get=_3f.get||(_3f.value==undefined)&&_3a;
_3f.markup=this.generateCellMarkup(_3f,_3f.cellStyles,_3f.cellClasses,false);
}
}
},generateHtml:function(_40,_41){
var _42=this.getTableArray(),v=this.view,_44=v.structure.cells,_45=this.grid.getItem(_41);
dojox.grid.util.fire(this.view,"onBeforeRow",[_41,_44]);
for(var j=0,row;(row=_44[j]);j++){
if(row.hidden||row.header){
continue;
}
_42.push(!row.invisible?"<tr>":"<tr class=\"dojoxGridInvisible\">");
for(var i=0,_49,m,cc,cs;(_49=row[i]);i++){
m=_49.markup,cc=_49.customClasses=[],cs=_49.customStyles=[];
m[5]=_49.format(_41,_45);
m[1]=cc.join(" ");
m[3]=cs.join(";");
_42.push.apply(_42,m);
}
_42.push("</tr>");
}
_42.push("</table>");
return _42.join("");
},decorateEvent:function(e){
e.rowNode=this.findRowTarget(e.target);
if(!e.rowNode){
return false;
}
e.rowIndex=e.rowNode[_1];
this.baseDecorateEvent(e);
e.cell=this.grid.getCell(e.cellIndex);
return true;
}});
var _4e=null;
var _4f=function(_50){
_18.call(this,_50);
};
_4f.prototype=new _18();
dojo.extend(_4f,{_skipBogusClicks:false,overResizeWidth:4,minColWidth:1,update:function(){
this.tableMap=new _51(this.view.structure.cells);
},generateHtml:function(_52,_53){
var _54=this.getTableArray(),_55=this.view.structure.cells;
dojox.grid.util.fire(this.view,"onBeforeRow",[-1,_55]);
for(var j=0,row;(row=_55[j]);j++){
if(row.hidden){
continue;
}
_54.push(!row.invisible?"<tr>":"<tr class=\"dojoxGridInvisible\">");
for(var i=0,_59,_5a;(_59=row[i]);i++){
_59.customClasses=[];
_59.customStyles=[];
if(this.view.simpleStructure){
if(_59.headerClasses){
if(_59.headerClasses.indexOf("dojoDndItem")==-1){
_59.headerClasses+=" dojoDndItem";
}
}else{
_59.headerClasses="dojoDndItem";
}
if(_59.attrs){
if(_59.attrs.indexOf("dndType='gridColumn'")==-1){
_59.attrs+=" dndType='gridColumn_"+this.grid.id+"'";
}
}else{
_59.attrs="dndType='gridColumn_"+this.grid.id+"'";
}
}
_5a=this.generateCellMarkup(_59,_59.headerStyles,_59.headerClasses,true);
_5a[5]=(_53!=undefined?_53:_52(_59));
_5a[3]=_59.customStyles.join(";");
_5a[1]=_59.customClasses.join(" ");
_54.push(_5a.join(""));
}
_54.push("</tr>");
}
_54.push("</table>");
return _54.join("");
},getCellX:function(e){
var x=e.layerX;
if(dojo.isMoz){
var n=_d(e.target,_11("th"));
x-=(n&&n.offsetLeft)||0;
var t=e.sourceView.getScrollbarWidth();
if(!dojo._isBodyLtr()&&e.sourceView.headerNode.scrollLeft<t){
x-=t;
}
}
var n=_d(e.target,function(){
if(!n||n==e.cellNode){
return false;
}
x+=(n.offsetLeft<0?0:n.offsetLeft);
return true;
});
return x;
},decorateEvent:function(e){
this.baseDecorateEvent(e);
e.rowIndex=-1;
e.cellX=this.getCellX(e);
return true;
},prepareResize:function(e,mod){
do{
var i=_3(e.cellNode);
e.cellNode=(i?e.cellNode.parentNode.cells[i+mod]:null);
e.cellIndex=(e.cellNode?this.getCellNodeIndex(e.cellNode):-1);
}while(e.cellNode&&e.cellNode.style.display=="none");
return Boolean(e.cellNode);
},canResize:function(e){
if(!e.cellNode||e.cellNode.colSpan>1){
return false;
}
var _64=this.grid.getCell(e.cellIndex);
return !_64.noresize&&!_64.canResize();
},overLeftResizeArea:function(e){
if(dojo._isBodyLtr()){
return (e.cellIndex>0)&&(e.cellX<this.overResizeWidth)&&this.prepareResize(e,-1);
}
var t=e.cellNode&&(e.cellX<this.overResizeWidth);
return t;
},overRightResizeArea:function(e){
if(dojo._isBodyLtr()){
return e.cellNode&&(e.cellX>=e.cellNode.offsetWidth-this.overResizeWidth);
}
return (e.cellIndex>0)&&(e.cellX>=e.cellNode.offsetWidth-this.overResizeWidth)&&this.prepareResize(e,-1);
},domousemove:function(e){
if(!_4e){
var c=(this.overRightResizeArea(e)?"e-resize":(this.overLeftResizeArea(e)?"w-resize":""));
if(c&&!this.canResize(e)){
c="not-allowed";
}
if(dojo.isIE){
var t=e.sourceView.headerNode.scrollLeft;
e.sourceView.headerNode.style.cursor=c||"";
e.sourceView.headerNode.scrollLeft=t;
}else{
e.sourceView.headerNode.style.cursor=c||"";
}
if(c){
dojo.stopEvent(e);
}
}
},domousedown:function(e){
if(!_4e){
if((this.overRightResizeArea(e)||this.overLeftResizeArea(e))&&this.canResize(e)){
this.beginColumnResize(e);
}else{
this.grid.onMouseDown(e);
this.grid.onMouseOverRow(e);
}
}
},doclick:function(e){
if(this._skipBogusClicks){
dojo.stopEvent(e);
return true;
}
},beginColumnResize:function(e){
this.moverDiv=document.createElement("div");
dojo.style(this.moverDiv,{position:"absolute",left:0});
dojo.body().appendChild(this.moverDiv);
var m=_4e=new dojo.dnd.Moveable(this.moverDiv);
var _6f=[],_70=this.tableMap.findOverlappingNodes(e.cellNode);
for(var i=0,_72;(_72=_70[i]);i++){
_6f.push({node:_72,index:this.getCellNodeIndex(_72),width:_72.offsetWidth});
}
var _73=e.sourceView;
var adj=dojo._isBodyLtr()?1:-1;
var _75=e.grid.views.views;
var _76=[];
for(var i=_73.idx+adj,_77;(_77=_75[i]);i=i+adj){
_76.push({node:_77.headerNode,left:window.parseInt(_77.headerNode.style.left)});
}
var _78=_73.headerContentNode.firstChild;
var _79={scrollLeft:e.sourceView.headerNode.scrollLeft,view:_73,node:e.cellNode,index:e.cellIndex,w:dojo.contentBox(e.cellNode).w,vw:dojo.contentBox(_73.headerNode).w,table:_78,tw:dojo.contentBox(_78).w,spanners:_6f,followers:_76};
m.onMove=dojo.hitch(this,"doResizeColumn",_79);
dojo.connect(m,"onMoveStop",dojo.hitch(this,function(){
this.endResizeColumn(_79);
if(_79.node.releaseCapture){
_79.node.releaseCapture();
}
_4e.destroy();
delete _4e;
_4e=null;
}));
_73.convertColPctToFixed();
if(e.cellNode.setCapture){
e.cellNode.setCapture();
}
m.onMouseDown(e);
},doResizeColumn:function(_7a,_7b,_7c){
var _7d=dojo._isBodyLtr();
var _7e=_7d?_7c.l:-_7c.l;
var w=_7a.w+_7e;
var vw=_7a.vw+_7e;
var tw=_7a.tw+_7e;
if(w>=this.minColWidth){
for(var i=0,s,sw;(s=_7a.spanners[i]);i++){
sw=s.width+_7e;
s.node.style.width=sw+"px";
_7a.view.setColWidth(s.index,sw);
}
for(var i=0,f,fl;(f=_7a.followers[i]);i++){
fl=f.left+_7e;
f.node.style.left=fl+"px";
}
_7a.node.style.width=w+"px";
_7a.view.setColWidth(_7a.index,w);
_7a.view.headerNode.style.width=vw+"px";
_7a.view.setColumnsWidth(tw);
if(!_7d){
_7a.view.headerNode.scrollLeft=_7a.scrollLeft+_7e;
}
}
if(_7a.view.flexCells&&!_7a.view.testFlexCells()){
var t=_a(_7a.node);
t&&(t.style.width="");
}
},endResizeColumn:function(_88){
dojo._destroyElement(this.moverDiv);
delete this.moverDiv;
this._skipBogusClicks=true;
var _89=dojo.connect(_88.view,"update",this,function(){
dojo.disconnect(_89);
this._skipBogusClicks=false;
});
setTimeout(dojo.hitch(_88.view,"update"),50);
}});
var _51=function(_8a){
this.mapRows(_8a);
};
dojo.extend(_51,{map:null,mapRows:function(_8b){
var _8c=_8b.length;
if(!_8c){
return;
}
this.map=[];
for(var j=0,row;(row=_8b[j]);j++){
this.map[j]=[];
}
for(var j=0,row;(row=_8b[j]);j++){
for(var i=0,x=0,_91,_92,_93;(_91=row[i]);i++){
while(this.map[j][x]){
x++;
}
this.map[j][x]={c:i,r:j};
_93=_91.rowSpan||1;
_92=_91.colSpan||1;
for(var y=0;y<_93;y++){
for(var s=0;s<_92;s++){
this.map[j+y][x+s]=this.map[j][x];
}
}
x+=_92;
}
}
},dumpMap:function(){
for(var j=0,row,h="";(row=this.map[j]);j++,h=""){
for(var i=0,_9a;(_9a=row[i]);i++){
h+=_9a.r+","+_9a.c+"   ";
}
}
},getMapCoords:function(_9b,_9c){
for(var j=0,row;(row=this.map[j]);j++){
for(var i=0,_a0;(_a0=row[i]);i++){
if(_a0.c==_9c&&_a0.r==_9b){
return {j:j,i:i};
}
}
}
return {j:-1,i:-1};
},getNode:function(_a1,_a2,_a3){
var row=_a1&&_a1.rows[_a2];
return row&&row.cells[_a3];
},_findOverlappingNodes:function(_a5,_a6,_a7){
var _a8=[];
var m=this.getMapCoords(_a6,_a7);
var row=this.map[m.j];
for(var j=0,row;(row=this.map[j]);j++){
if(j==m.j){
continue;
}
var rw=row[m.i];
var n=(rw?this.getNode(_a5,rw.r,rw.c):null);
if(n){
_a8.push(n);
}
}
return _a8;
},findOverlappingNodes:function(_ae){
return this._findOverlappingNodes(_a(_ae),_5(_ae.parentNode),_3(_ae));
}});
dojo.declare("dojox.grid._View",[dijit._Widget,dijit._Templated],{defaultWidth:"18em",viewWidth:"",templateString:"<div class=\"dojoxGridView\">\n\t<div class=\"dojoxGridHeader\" dojoAttachPoint=\"headerNode\">\n\t\t<div dojoAttachPoint=\"headerNodeContainer\" style=\"width:9000em\">\n\t\t\t<div dojoAttachPoint=\"headerContentNode\"></div>\n\t\t</div>\n\t</div>\n\t<input type=\"checkbox\" class=\"dojoxGridHiddenFocus\" dojoAttachPoint=\"hiddenFocusNode\" />\n\t<input type=\"checkbox\" class=\"dojoxGridHiddenFocus\" />\n\t<div class=\"dojoxGridScrollbox\" dojoAttachPoint=\"scrollboxNode\">\n\t\t<div class=\"dojoxGridContent\" dojoAttachPoint=\"contentNode\" hidefocus=\"hidefocus\"></div>\n\t</div>\n</div>\n",themeable:false,classTag:"dojoxGrid",marginBottom:0,rowPad:2,_togglingColumn:-1,postMixInProperties:function(){
this.rowNodes=[];
},postCreate:function(){
this.connect(this.scrollboxNode,"onscroll","doscroll");
dojox.grid.util.funnelEvents(this.contentNode,this,"doContentEvent",["mouseover","mouseout","click","dblclick","contextmenu","mousedown"]);
dojox.grid.util.funnelEvents(this.headerNode,this,"doHeaderEvent",["dblclick","mouseover","mouseout","mousemove","mousedown","click","contextmenu"]);
this.content=new _38(this);
this.header=new _4f(this);
if(!dojo._isBodyLtr()){
this.headerNodeContainer.style.width="";
}
},destroy:function(){
dojo._destroyElement(this.headerNode);
delete this.headerNode;
dojo.forEach(this.rowNodes,dojo._destroyElement);
this.rowNodes=[];
if(this.source){
this.source.destroy();
}
this.inherited(arguments);
},focus:function(){
if(dojo.isSafari||dojo.isOpera){
this.hiddenFocusNode.focus();
}else{
this.scrollboxNode.focus();
}
},setStructure:function(_af){
var vs=this.structure=_af;
if(vs.width&&!isNaN(vs.width)){
this.viewWidth=vs.width+"em";
}else{
this.viewWidth=vs.width||(vs.noscroll?"auto":this.viewWidth);
}
this.onBeforeRow=vs.onBeforeRow;
this.noscroll=vs.noscroll;
if(this.noscroll){
this.scrollboxNode.style.overflow="hidden";
}
this.simpleStructure=Boolean(vs.cells.length==1);
this.testFlexCells();
this.updateStructure();
},testFlexCells:function(){
this.flexCells=false;
for(var j=0,row;(row=this.structure.cells[j]);j++){
for(var i=0,_b4;(_b4=row[i]);i++){
_b4.view=this;
this.flexCells=this.flexCells||_b4.isFlex();
}
}
return this.flexCells;
},updateStructure:function(){
this.header.update();
this.content.update();
},getScrollbarWidth:function(){
var _b5=this.hasVScrollbar();
var _b6=dojo.style(this.scrollboxNode,"overflow");
if(this.noscroll||!_b6||_b6=="hidden"){
_b5=false;
}else{
if(_b6=="scroll"){
_b5=true;
}
}
return (_b5?dojox.html.metrics.getScrollbar().w:0);
},getColumnsWidth:function(){
return this.headerContentNode.firstChild.offsetWidth;
},setColumnsWidth:function(_b7){
this.headerContentNode.firstChild.style.width=_b7+"px";
if(this.viewWidth){
this.viewWidth=_b7+"px";
}
},getWidth:function(){
return this.viewWidth||(this.getColumnsWidth()+this.getScrollbarWidth())+"px";
},getContentWidth:function(){
return Math.max(0,dojo._getContentBox(this.domNode).w-this.getScrollbarWidth())+"px";
},render:function(){
this.scrollboxNode.style.height="";
this.renderHeader();
if(this._togglingColumn>=0){
this.setColumnsWidth(this.getColumnsWidth()-this._togglingColumn);
this._togglingColumn=-1;
}
var _b8=this.grid.layout.cells;
var _b9=dojo.hitch(this,function(_ba,_bb){
var inc=_bb?-1:1;
var idx=this.header.getCellNodeIndex(_ba)+inc;
var _be=_b8[idx];
while(_be&&_be.getHeaderNode()&&_be.getHeaderNode().style.display=="none"){
idx+=inc;
_be=_b8[idx];
}
if(_be){
return _be.getHeaderNode();
}
return null;
});
if(this.grid.columnReordering&&this.simpleStructure){
if(this.source){
this.source.destroy();
}
this.source=new dojo.dnd.Source(this.headerContentNode.firstChild.rows[0],{horizontal:true,accept:["gridColumn_"+this.grid.id],viewIndex:this.index,onMouseDown:dojo.hitch(this,function(e){
this.header.decorateEvent(e);
if((this.header.overRightResizeArea(e)||this.header.overLeftResizeArea(e))&&this.header.canResize(e)&&!_4e){
this.header.beginColumnResize(e);
}else{
if(this.grid.headerMenu){
this.grid.headerMenu.onCancel(true);
}
if(e.button===(dojo.isIE?1:0)){
dojo.dnd.Source.prototype.onMouseDown.call(this.source,e);
}
}
}),_markTargetAnchor:dojo.hitch(this,function(_c0){
var src=this.source;
if(src.current==src.targetAnchor&&src.before==_c0){
return;
}
if(src.targetAnchor&&_b9(src.targetAnchor,src.before)){
src._removeItemClass(_b9(src.targetAnchor,src.before),src.before?"After":"Before");
}
dojo.dnd.Source.prototype._markTargetAnchor.call(src,_c0);
if(src.targetAnchor&&_b9(src.targetAnchor,src.before)){
src._addItemClass(_b9(src.targetAnchor,src.before),src.before?"After":"Before");
}
}),_unmarkTargetAnchor:dojo.hitch(this,function(){
var src=this.source;
if(!src.targetAnchor){
return;
}
if(src.targetAnchor&&_b9(src.targetAnchor,src.before)){
src._removeItemClass(_b9(src.targetAnchor,src.before),src.before?"After":"Before");
}
dojo.dnd.Source.prototype._unmarkTargetAnchor.call(src);
}),destroy:dojo.hitch(this,function(){
dojo.disconnect(this._source_conn);
dojo.unsubscribe(this._source_sub);
dojo.dnd.Source.prototype.destroy.call(this.source);
})});
this._source_conn=dojo.connect(this.source,"onDndDrop",this,"_onDndDrop");
this._source_sub=dojo.subscribe("/dnd/drop/before",this,"_onDndDropBefore");
this.source.startup();
}
},_onDndDropBefore:function(_c3,_c4,_c5){
if(dojo.dnd.manager().target!==this.source){
return;
}
this.source._targetNode=this.source.targetAnchor;
this.source._beforeTarget=this.source.before;
var _c6=this.grid.views.views;
var _c7=_c6[_c3.viewIndex];
var _c8=_c6[this.index];
if(_c8!=_c7){
var s=_c7.convertColPctToFixed();
var t=_c8.convertColPctToFixed();
if(s||t){
setTimeout(function(){
_c7.update();
_c8.update();
},50);
}
}
},_onDndDrop:function(_cb,_cc,_cd){
if(dojo.dnd.manager().target!==this.source){
if(dojo.dnd.manager().source===this.source){
this._removingColumn=true;
}
return;
}
var _ce=function(n){
return n?dojo.attr(n,"idx"):null;
};
var w=dojo.marginBox(_cc[0]).w;
if(_cb.viewIndex!==this.index){
var _d1=this.grid.views.views;
var _d2=_d1[_cb.viewIndex];
var _d3=_d1[this.index];
if(_d2.viewWidth&&_d2.viewWidth!="auto"){
_d2.setColumnsWidth(_d2.getColumnsWidth()-w);
}
if(_d3.viewWidth&&_d3.viewWidth!="auto"){
_d3.setColumnsWidth(_d3.getColumnsWidth());
}
}
var stn=this.source._targetNode;
var stb=this.source._beforeTarget;
var _d6=this.grid.layout;
var idx=this.index;
delete this.source._targetNode;
delete this.source._beforeTarget;
window.setTimeout(function(){
_d6.moveColumn(_cb.viewIndex,idx,_ce(_cc[0]),_ce(stn),stb);
},1);
},renderHeader:function(){
this.headerContentNode.innerHTML=this.header.generateHtml(this._getHeaderContent);
if(this.flexCells){
this.contentWidth=this.getContentWidth();
this.headerContentNode.firstChild.style.width=this.contentWidth;
}
},_getHeaderContent:function(_d8){
var n=_d8.name||_d8.grid.getCellName(_d8);
var ret=["<div class=\"dojoxGridSortNode"];
if(_d8.index!=_d8.grid.getSortIndex()){
ret.push("\">");
}else{
ret=ret.concat([" ",_d8.grid.sortInfo>0?"dojoxGridSortUp":"dojoxGridSortDown","\"><div class=\"dojoxGridArrowButtonChar\">",_d8.grid.sortInfo>0?"&#9650;":"&#9660;","</div><div class=\"dojoxGridArrowButtonNode\"></div>"]);
}
ret=ret.concat([n,"</div>"]);
return ret.join("");
},resize:function(){
this.adaptHeight();
this.adaptWidth();
},hasHScrollbar:function(_db){
if(this._hasHScroll==undefined||_db){
if(this.noscroll){
this._hasHScroll=false;
}else{
var _dc=dojo.style(this.scrollboxNode,"overflow");
if(_dc=="hidden"){
this._hasHScroll=false;
}else{
if(_dc=="scroll"){
this._hasHScroll=true;
}else{
this._hasHScroll=(this.scrollboxNode.offsetWidth<this.contentNode.offsetWidth);
}
}
}
}
return this._hasHScroll;
},hasVScrollbar:function(_dd){
if(this._hasVScroll==undefined||_dd){
if(this.noscroll){
this._hasVScroll=false;
}else{
var _de=dojo.style(this.scrollboxNode,"overflow");
if(_de=="hidden"){
this._hasVScroll=false;
}else{
if(_de=="scroll"){
this._hasVScroll=true;
}else{
this._hasVScroll=(this.scrollboxNode.offsetHeight<this.contentNode.offsetHeight);
}
}
}
}
return this._hasVScroll;
},convertColPctToFixed:function(){
var _df=false;
var _e0=dojo.query("th",this.headerContentNode);
var _e1=dojo.map(_e0,function(c){
var w=c.style.width;
if(w&&w.slice(-1)=="%"){
_df=true;
return dojo.contentBox(c).w;
}else{
if(w&&w.slice(-2)=="px"){
return window.parseInt(w,10);
}
}
return -1;
});
if(_df){
dojo.forEach(this.grid.layout.cells,function(_e4,idx){
if(_e4.view==this){
var _e6=_e4.layoutIndex;
this.setColWidth(idx,_e1[_e6]);
_e0[_e6].style.width=_e4.unitWidth;
}
},this);
return true;
}
return false;
},adaptHeight:function(_e7){
if(!this.grid._autoHeight){
var h=this.domNode.clientHeight;
if(_e7){
h-=dojox.html.metrics.getScrollbar().h;
}
dojox.grid.util.setStyleHeightPx(this.scrollboxNode,h);
}
this.hasVScrollbar(true);
},adaptWidth:function(){
if(this.flexCells){
this.contentWidth=this.getContentWidth();
this.headerContentNode.firstChild.style.width=this.contentWidth;
}
var w=this.scrollboxNode.offsetWidth-this.getScrollbarWidth();
if(!this._removingColumn){
w=Math.max(w,this.getColumnsWidth())+"px";
}else{
w=Math.min(w,this.getColumnsWidth())+"px";
this._removingColumn=false;
}
var cn=this.contentNode;
cn.style.width=w;
this.hasHScrollbar(true);
},setSize:function(w,h){
var ds=this.domNode.style;
var hs=this.headerNode.style;
if(w){
ds.width=w;
hs.width=w;
}
ds.height=(h>=0?h+"px":"");
},renderRow:function(_ef){
var _f0=this.createRowNode(_ef);
this.buildRow(_ef,_f0);
this.grid.edit.restore(this,_ef);
if(this._pendingUpdate){
window.clearTimeout(this._pendingUpdate);
}
this._pendingUpdate=window.setTimeout(dojo.hitch(this,function(){
window.clearTimeout(this._pendingUpdate);
delete this._pendingUpdate;
this.grid._resize();
}),50);
return _f0;
},createRowNode:function(_f1){
var _f2=document.createElement("div");
_f2.className=this.classTag+"Row";
_f2[_2]=this.id;
_f2[_1]=_f1;
this.rowNodes[_f1]=_f2;
return _f2;
},buildRow:function(_f3,_f4){
this.buildRowContent(_f3,_f4);
this.styleRow(_f3,_f4);
},buildRowContent:function(_f5,_f6){
_f6.innerHTML=this.content.generateHtml(_f5,_f5);
if(this.flexCells&&this.contentWidth){
_f6.firstChild.style.width=this.contentWidth;
}
},rowRemoved:function(_f7){
this.grid.edit.save(this,_f7);
delete this.rowNodes[_f7];
},getRowNode:function(_f8){
return this.rowNodes[_f8];
},getCellNode:function(_f9,_fa){
var row=this.getRowNode(_f9);
if(row){
return this.content.getCellNode(row,_fa);
}
},getHeaderCellNode:function(_fc){
if(this.headerContentNode){
return this.header.getCellNode(this.headerContentNode,_fc);
}
},styleRow:function(_fd,_fe){
_fe._style=_15(_fe);
this.styleRowNode(_fd,_fe);
},styleRowNode:function(_ff,_100){
if(_100){
this.doStyleRowNode(_ff,_100);
}
},doStyleRowNode:function(_101,_102){
this.grid.styleRowNode(_101,_102);
},updateRow:function(_103){
var _104=this.getRowNode(_103);
if(_104){
_104.style.height="";
this.buildRow(_103,_104);
}
return _104;
},updateRowStyles:function(_105){
this.styleRowNode(_105,this.getRowNode(_105));
},lastTop:0,firstScroll:0,doscroll:function(_106){
var _107=dojo._isBodyLtr();
if(this.firstScroll<2){
if((!_107&&this.firstScroll==1)||(_107&&this.firstScroll==0)){
var s=dojo.marginBox(this.headerNodeContainer);
if(dojo.isIE){
this.headerNodeContainer.style.width=s.w+this.getScrollbarWidth()+"px";
}else{
if(dojo.isMoz){
this.headerNodeContainer.style.width=s.w-this.getScrollbarWidth()+"px";
this.scrollboxNode.scrollLeft=_107?this.scrollboxNode.clientWidth-this.scrollboxNode.scrollWidth:this.scrollboxNode.scrollWidth-this.scrollboxNode.clientWidth;
}
}
}
this.firstScroll++;
}
this.headerNode.scrollLeft=this.scrollboxNode.scrollLeft;
var top=this.scrollboxNode.scrollTop;
if(top!=this.lastTop){
this.grid.scrollTo(top);
}
},setScrollTop:function(_10a){
this.lastTop=_10a;
this.scrollboxNode.scrollTop=_10a;
return this.scrollboxNode.scrollTop;
},doContentEvent:function(e){
if(this.content.decorateEvent(e)){
this.grid.onContentEvent(e);
}
},doHeaderEvent:function(e){
if(this.header.decorateEvent(e)){
this.grid.onHeaderEvent(e);
}
},dispatchContentEvent:function(e){
return this.content.dispatchEvent(e);
},dispatchHeaderEvent:function(e){
return this.header.dispatchEvent(e);
},setColWidth:function(_10f,_110){
this.grid.setCellWidth(_10f,_110+"px");
},update:function(){
var left=this.scrollboxNode.scrollLeft;
this.content.update();
this.grid.update();
this.scrollboxNode.scrollLeft=left;
this.headerNode.scrollLeft=left;
}});
dojo.declare("dojox.grid._GridAvatar",dojo.dnd.Avatar,{construct:function(){
var dd=dojo.doc;
var a=dd.createElement("table");
a.cellPadding=a.cellSpacing="0";
a.className="dojoxGridDndAvatar";
a.style.position="absolute";
a.style.zIndex=1999;
a.style.margin="0px";
var b=dd.createElement("tbody");
var tr=dd.createElement("tr");
var td=dd.createElement("td");
var img=dd.createElement("td");
tr.className="dojoxGridDndAvatarItem";
img.className="dojoxGridDndAvatarItemImage";
img.style.width="16px";
var _118=this.manager.source,node;
if(_118.creator){
node=_118._normailzedCreator(_118.getItem(this.manager.nodes[0].id).data,"avatar").node;
}else{
node=this.manager.nodes[0].cloneNode(true);
if(node.tagName.toLowerCase()=="tr"){
var _11a=dd.createElement("table"),_11b=dd.createElement("tbody");
_11b.appendChild(node);
_11a.appendChild(_11b);
node=_11a;
}else{
if(node.tagName.toLowerCase()=="th"){
var _11a=dd.createElement("table"),_11b=dd.createElement("tbody"),r=dd.createElement("tr");
_11a.cellPadding=_11a.cellSpacing="0";
r.appendChild(node);
_11b.appendChild(r);
_11a.appendChild(_11b);
node=_11a;
}
}
}
node.id="";
td.appendChild(node);
tr.appendChild(img);
tr.appendChild(td);
dojo.style(tr,"opacity",0.9);
b.appendChild(tr);
a.appendChild(b);
this.node=a;
var m=dojo.dnd.manager();
this.oldOffsetY=m.OFFSET_Y;
m.OFFSET_Y=1;
},destroy:function(){
dojo.dnd.manager().OFFSET_Y=this.oldOffsetY;
this.inherited(arguments);
}});
var _11e=dojo.dnd.manager().makeAvatar;
dojo.dnd.manager().makeAvatar=function(){
var src=this.source;
if(src.viewIndex!==undefined){
return new dojox.grid._GridAvatar(this);
}
return _11e.call(dojo.dnd.manager());
};
})();
}
