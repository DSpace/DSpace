/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid.compat._grid.builder"]){
dojo._hasResource["dojox.grid.compat._grid.builder"]=true;
dojo.provide("dojox.grid.compat._grid.builder");
dojo.require("dojox.grid.compat._grid.drag");
dojo.declare("dojox.grid.Builder",null,{constructor:function(_1){
this.view=_1;
this.grid=_1.grid;
},view:null,_table:"<table class=\"dojoxGrid-row-table\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" role=\"wairole:presentation\">",generateCellMarkup:function(_2,_3,_4,_5){
var _6=[],_7;
if(_5){
_7=["<th tabIndex=\"-1\" role=\"wairole:columnheader\""];
}else{
_7=["<td tabIndex=\"-1\" role=\"wairole:gridcell\""];
}
_2.colSpan&&_7.push(" colspan=\"",_2.colSpan,"\"");
_2.rowSpan&&_7.push(" rowspan=\"",_2.rowSpan,"\"");
_7.push(" class=\"dojoxGrid-cell ");
_2.classes&&_7.push(_2.classes," ");
_4&&_7.push(_4," ");
_6.push(_7.join(""));
_6.push("");
_7=["\" idx=\"",_2.index,"\" style=\""];
_7.push(_2.styles,_3||"");
_2.unitWidth&&_7.push("width:",_2.unitWidth,";");
_6.push(_7.join(""));
_6.push("");
_7=["\""];
_2.attrs&&_7.push(" ",_2.attrs);
_7.push(">");
_6.push(_7.join(""));
_6.push("");
_6.push("</td>");
return _6;
},isCellNode:function(_8){
return Boolean(_8&&_8.getAttribute&&_8.getAttribute("idx"));
},getCellNodeIndex:function(_9){
return _9?Number(_9.getAttribute("idx")):-1;
},getCellNode:function(_a,_b){
for(var i=0,_d;_d=dojox.grid.getTr(_a.firstChild,i);i++){
for(var j=0,_f;_f=_d.cells[j];j++){
if(this.getCellNodeIndex(_f)==_b){
return _f;
}
}
}
},findCellTarget:function(_10,_11){
var n=_10;
while(n&&(!this.isCellNode(n)||(dojox.grid.gridViewTag in n.offsetParent.parentNode&&n.offsetParent.parentNode[dojox.grid.gridViewTag]!=this.view.id))&&(n!=_11)){
n=n.parentNode;
}
return n!=_11?n:null;
},baseDecorateEvent:function(e){
e.dispatch="do"+e.type;
e.grid=this.grid;
e.sourceView=this.view;
e.cellNode=this.findCellTarget(e.target,e.rowNode);
e.cellIndex=this.getCellNodeIndex(e.cellNode);
e.cell=(e.cellIndex>=0?this.grid.getCell(e.cellIndex):null);
},findTarget:function(_14,_15){
var n=_14;
while(n&&(n!=this.domNode)&&(!(_15 in n)||(dojox.grid.gridViewTag in n&&n[dojox.grid.gridViewTag]!=this.view.id))){
n=n.parentNode;
}
return (n!=this.domNode)?n:null;
},findRowTarget:function(_17){
return this.findTarget(_17,dojox.grid.rowIndexTag);
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
dojo.declare("dojox.grid.contentBuilder",dojox.grid.Builder,{update:function(){
this.prepareHtml();
},prepareHtml:function(){
var _1f=this.grid.get,_20=this.view.structure.rows;
for(var j=0,row;(row=_20[j]);j++){
for(var i=0,_24;(_24=row[i]);i++){
_24.get=_24.get||(_24.value==undefined)&&_1f;
_24.markup=this.generateCellMarkup(_24,_24.cellStyles,_24.cellClasses,false);
}
}
},generateHtml:function(_25,_26){
var _27=[this._table],v=this.view,obr=v.onBeforeRow,_2a=v.structure.rows;
obr&&obr(_26,_2a);
for(var j=0,row;(row=_2a[j]);j++){
if(row.hidden||row.header){
continue;
}
_27.push(!row.invisible?"<tr>":"<tr class=\"dojoxGrid-invisible\">");
for(var i=0,_2e,m,cc,cs;(_2e=row[i]);i++){
m=_2e.markup,cc=_2e.customClasses=[],cs=_2e.customStyles=[];
m[5]=_2e.format(_25);
m[1]=cc.join(" ");
m[3]=cs.join(";");
_27.push.apply(_27,m);
}
_27.push("</tr>");
}
_27.push("</table>");
return _27.join("");
},decorateEvent:function(e){
e.rowNode=this.findRowTarget(e.target);
if(!e.rowNode){
return false;
}
e.rowIndex=e.rowNode[dojox.grid.rowIndexTag];
this.baseDecorateEvent(e);
e.cell=this.grid.getCell(e.cellIndex);
return true;
}});
dojo.declare("dojox.grid.headerBuilder",dojox.grid.Builder,{bogusClickTime:0,overResizeWidth:4,minColWidth:1,_table:"<table class=\"dojoxGrid-row-table\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\" role=\"wairole:presentation\"",update:function(){
this.tableMap=new dojox.grid.tableMap(this.view.structure.rows);
},generateHtml:function(_33,_34){
var _35=[this._table],_36=this.view.structure.rows;
if(this.view.viewWidth){
_35.push([" style=\"width:",this.view.viewWidth,";\""].join(""));
}
_35.push(">");
dojox.grid.fire(this.view,"onBeforeRow",[-1,_36]);
for(var j=0,row;(row=_36[j]);j++){
if(row.hidden){
continue;
}
_35.push(!row.invisible?"<tr>":"<tr class=\"dojoxGrid-invisible\">");
for(var i=0,_3a,_3b;(_3a=row[i]);i++){
_3a.customClasses=[];
_3a.customStyles=[];
_3b=this.generateCellMarkup(_3a,_3a.headerStyles,_3a.headerClasses,true);
_3b[5]=(_34!=undefined?_34:_33(_3a));
_3b[3]=_3a.customStyles.join(";");
_3b[1]=_3a.customClasses.join(" ");
_35.push(_3b.join(""));
}
_35.push("</tr>");
}
_35.push("</table>");
return _35.join("");
},getCellX:function(e){
var x=e.layerX;
if(dojo.isMoz){
var n=dojox.grid.ascendDom(e.target,dojox.grid.makeNotTagName("th"));
x-=(n&&n.offsetLeft)||0;
var t=e.sourceView.getScrollbarWidth();
if(!dojo._isBodyLtr()&&e.sourceView.headerNode.scrollLeft<t){
x-=t;
}
}
var n=dojox.grid.ascendDom(e.target,function(){
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
var i=dojox.grid.getTdIndex(e.cellNode);
e.cellNode=(i?e.cellNode.parentNode.cells[i+mod]:null);
e.cellIndex=(e.cellNode?this.getCellNodeIndex(e.cellNode):-1);
return Boolean(e.cellNode);
},canResize:function(e){
if(!e.cellNode||e.cellNode.colSpan>1){
return false;
}
var _45=this.grid.getCell(e.cellIndex);
return !_45.noresize&&!_45.isFlex();
},overLeftResizeArea:function(e){
if(dojo._isBodyLtr()){
return (e.cellIndex>0)&&(e.cellX<this.overResizeWidth)&&this.prepareResize(e,-1);
}
return t=e.cellNode&&(e.cellX<this.overResizeWidth);
},overRightResizeArea:function(e){
if(dojo._isBodyLtr()){
return e.cellNode&&(e.cellX>=e.cellNode.offsetWidth-this.overResizeWidth);
}
return (e.cellIndex>0)&&(e.cellX>=e.cellNode.offsetWidth-this.overResizeWidth)&&this.prepareResize(e,-1);
},domousemove:function(e){
var c=(this.overRightResizeArea(e)?"e-resize":(this.overLeftResizeArea(e)?"w-resize":""));
if(c&&!this.canResize(e)){
c="not-allowed";
}
e.sourceView.headerNode.style.cursor=c||"";
if(c){
dojo.stopEvent(e);
}
},domousedown:function(e){
if(!dojox.grid.drag.dragging){
if((this.overRightResizeArea(e)||this.overLeftResizeArea(e))&&this.canResize(e)){
this.beginColumnResize(e);
}else{
this.grid.onMouseDown(e);
this.grid.onMouseOverRow(e);
}
}
},doclick:function(e){
if(new Date().getTime()<this.bogusClickTime){
dojo.stopEvent(e);
return true;
}
},beginColumnResize:function(e){
dojo.stopEvent(e);
var _4d=[],_4e=this.tableMap.findOverlappingNodes(e.cellNode);
for(var i=0,_50;(_50=_4e[i]);i++){
_4d.push({node:_50,index:this.getCellNodeIndex(_50),width:_50.offsetWidth});
}
var _51={scrollLeft:e.sourceView.headerNode.scrollLeft,view:e.sourceView,node:e.cellNode,index:e.cellIndex,w:e.cellNode.clientWidth,spanners:_4d};
dojox.grid.drag.start(e.cellNode,dojo.hitch(this,"doResizeColumn",_51),dojo.hitch(this,"endResizeColumn",_51),e);
},doResizeColumn:function(_52,_53){
var _54=dojo._isBodyLtr();
if(_54){
var w=_52.w+_53.deltaX;
}else{
var w=_52.w-_53.deltaX;
}
if(w>=this.minColWidth){
for(var i=0,s,sw;(s=_52.spanners[i]);i++){
if(_54){
sw=s.width+_53.deltaX;
}else{
sw=s.width-_53.deltaX;
}
s.node.style.width=sw+"px";
_52.view.setColWidth(s.index,sw);
}
_52.node.style.width=w+"px";
_52.view.setColWidth(_52.index,w);
if(!_54){
_52.view.headerNode.scrollLeft=(_52.scrollLeft-_53.deltaX);
}
}
if(_52.view.flexCells&&!_52.view.testFlexCells()){
var t=dojox.grid.findTable(_52.node);
t&&(t.style.width="");
}
},endResizeColumn:function(_5a){
this.bogusClickTime=new Date().getTime()+30;
setTimeout(dojo.hitch(_5a.view,"update"),50);
}});
dojo.declare("dojox.grid.tableMap",null,{constructor:function(_5b){
this.mapRows(_5b);
},map:null,mapRows:function(_5c){
var _5d=_5c.length;
if(!_5d){
return;
}
this.map=[];
for(var j=0,row;(row=_5c[j]);j++){
this.map[j]=[];
}
for(var j=0,row;(row=_5c[j]);j++){
for(var i=0,x=0,_62,_63,_64;(_62=row[i]);i++){
while(this.map[j][x]){
x++;
}
this.map[j][x]={c:i,r:j};
_64=_62.rowSpan||1;
_63=_62.colSpan||1;
for(var y=0;y<_64;y++){
for(var s=0;s<_63;s++){
this.map[j+y][x+s]=this.map[j][x];
}
}
x+=_63;
}
}
},dumpMap:function(){
for(var j=0,row,h="";(row=this.map[j]);j++,h=""){
for(var i=0,_6b;(_6b=row[i]);i++){
h+=_6b.r+","+_6b.c+"   ";
}

}
},getMapCoords:function(_6c,_6d){
for(var j=0,row;(row=this.map[j]);j++){
for(var i=0,_71;(_71=row[i]);i++){
if(_71.c==_6d&&_71.r==_6c){
return {j:j,i:i};
}
}
}
return {j:-1,i:-1};
},getNode:function(_72,_73,_74){
var row=_72&&_72.rows[_73];
return row&&row.cells[_74];
},_findOverlappingNodes:function(_76,_77,_78){
var _79=[];
var m=this.getMapCoords(_77,_78);
var row=this.map[m.j];
for(var j=0,row;(row=this.map[j]);j++){
if(j==m.j){
continue;
}
with(row[m.i]){
var n=this.getNode(_76,r,c);
if(n){
_79.push(n);
}
}
}
return _79;
},findOverlappingNodes:function(_7e){
return this._findOverlappingNodes(dojox.grid.findTable(_7e),dojox.grid.getTrIndex(_7e.parentNode),dojox.grid.getTdIndex(_7e));
}});
dojox.grid.rowIndexTag="gridRowIndex";
dojox.grid.gridViewTag="gridView";
}
