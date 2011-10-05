/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid._FocusManager"]){
dojo._hasResource["dojox.grid._FocusManager"]=true;
dojo.provide("dojox.grid._FocusManager");
dojo.require("dojox.grid.util");
dojo.declare("dojox.grid._FocusManager",null,{constructor:function(_1){
this.grid=_1;
this.cell=null;
this.rowIndex=-1;
this._connects=[];
this._connects.push(dojo.connect(this.grid.domNode,"onfocus",this,"doFocus"));
this._connects.push(dojo.connect(this.grid.domNode,"onblur",this,"doBlur"));
this._connects.push(dojo.connect(this.grid.lastFocusNode,"onfocus",this,"doLastNodeFocus"));
this._connects.push(dojo.connect(this.grid.lastFocusNode,"onblur",this,"doLastNodeBlur"));
},destroy:function(){
dojo.forEach(this._connects,dojo.disconnect);
delete this.grid;
delete this.cell;
},_colHeadNode:null,tabbingOut:false,focusClass:"dojoxGridCellFocus",focusView:null,initFocusView:function(){
this.focusView=this.grid.views.getFirstScrollingView();
this._initColumnHeaders();
},isFocusCell:function(_2,_3){
return (this.cell==_2)&&(this.rowIndex==_3);
},isLastFocusCell:function(){
return (this.rowIndex==this.grid.rowCount-1)&&(this.cell.index==this.grid.layout.cellCount-1);
},isFirstFocusCell:function(){
return (this.rowIndex==0)&&(this.cell.index==0);
},isNoFocusCell:function(){
return (this.rowIndex<0)||!this.cell;
},isNavHeader:function(){
return (!!this._colHeadNode);
},getHeaderIndex:function(){
if(this._colHeadNode){
return dojo.indexOf(this._findHeaderCells(),this._colHeadNode);
}else{
return -1;
}
},_focusifyCellNode:function(_4){
var n=this.cell&&this.cell.getNode(this.rowIndex);
if(n){
dojo.toggleClass(n,this.focusClass,_4);
if(_4){
var sl=this.scrollIntoView();
try{
if(!this.grid.edit.isEditing()){
dojox.grid.util.fire(n,"focus");
if(sl){
this.cell.view.scrollboxNode.scrollLeft=sl;
}
}
}
catch(e){
}
}
}
},_initColumnHeaders:function(){
this._connects.push(dojo.connect(this.grid.viewsHeaderNode,"onblur",this,"doBlurHeader"));
var _7=this._findHeaderCells();
for(var i=0;i<_7.length;i++){
this._connects.push(dojo.connect(_7[i],"onfocus",this,"doColHeaderFocus"));
this._connects.push(dojo.connect(_7[i],"onblur",this,"doColHeaderBlur"));
}
},_findHeaderCells:function(){
var _9=dojo.query("th",this.grid.viewsHeaderNode);
var _a=[];
for(var i=0;i<_9.length;i++){
var _c=_9[i];
var _d=dojo.hasAttr(_c,"tabindex");
var _e=dojo.attr(_c,"tabindex");
if(_d&&_e<0){
_a.push(_c);
}
}
return _a;
},scrollIntoView:function(){
var _f=(this.cell?this._scrollInfo(this.cell):null);
if(!_f){
return null;
}
var rt=this.grid.scroller.findScrollTop(this.rowIndex);
if(_f.n.offsetLeft+_f.n.offsetWidth>_f.sr.l+_f.sr.w){
_f.s.scrollLeft=_f.n.offsetLeft+_f.n.offsetWidth-_f.sr.w;
}else{
if(_f.n.offsetLeft<_f.sr.l){
_f.s.scrollLeft=_f.n.offsetLeft;
}
}
if(rt+_f.r.offsetHeight>_f.sr.t+_f.sr.h){
this.grid.setScrollTop(rt+_f.r.offsetHeight-_f.sr.h);
}else{
if(rt<_f.sr.t){
this.grid.setScrollTop(rt);
}
}
return _f.s.scrollLeft;
},_scrollInfo:function(_11,_12){
if(_11){
var cl=_11,sbn=cl.view.scrollboxNode,_15={w:sbn.clientWidth,l:sbn.scrollLeft,t:sbn.scrollTop,h:sbn.clientHeight},rn=cl.view.getRowNode(this.rowIndex);
return {c:cl,s:sbn,sr:_15,n:(_12?_12:_11.getNode(this.rowIndex)),r:rn};
}
return null;
},_scrollHeader:function(_17){
var _18=null;
if(this._colHeadNode){
_18=this._scrollInfo(this.grid.getCell(_17),this._colHeadNode);
}
if(_18){
if(_18.n.offsetLeft+_18.n.offsetWidth>_18.sr.l+_18.sr.w){
_18.s.scrollLeft=_18.n.offsetLeft+_18.n.offsetWidth-_18.sr.w;
}else{
if(_18.n.offsetLeft<_18.sr.l){
_18.s.scrollLeft=_18.n.offsetLeft;
}
}
}
},styleRow:function(_19){
return;
},setFocusIndex:function(_1a,_1b){
this.setFocusCell(this.grid.getCell(_1b),_1a);
},setFocusCell:function(_1c,_1d){
if(_1c&&!this.isFocusCell(_1c,_1d)){
this.tabbingOut=false;
this._colHeadNode=null;
this.focusGridView();
this._focusifyCellNode(false);
this.cell=_1c;
this.rowIndex=_1d;
this._focusifyCellNode(true);
}
if(dojo.isOpera){
setTimeout(dojo.hitch(this.grid,"onCellFocus",this.cell,this.rowIndex),1);
}else{
this.grid.onCellFocus(this.cell,this.rowIndex);
}
},next:function(){
var row=this.rowIndex,col=this.cell.index+1,cc=this.grid.layout.cellCount-1,rc=this.grid.rowCount-1;
if(col>cc){
col=0;
row++;
}
if(row>rc){
col=cc;
row=rc;
}
this.setFocusIndex(row,col);
},previous:function(){
var row=(this.rowIndex||0),col=(this.cell.index||0)-1;
if(col<0){
col=this.grid.layout.cellCount-1;
row--;
}
if(row<0){
row=0;
col=0;
}
this.setFocusIndex(row,col);
},move:function(_24,_25){
if(this.isNavHeader()){
var _26=this._findHeaderCells();
var _27=dojo.indexOf(_26,this._colHeadNode);
_27+=_25;
if((_27>=0)&&(_27<_26.length)){
this._colHeadNode=_26[_27];
this._colHeadNode.focus();
this._scrollHeader(_27);
}
}else{
var rc=this.grid.rowCount-1,cc=this.grid.layout.cellCount-1,r=this.rowIndex,i=this.cell.index,row=Math.min(rc,Math.max(0,r+_24)),col=Math.min(cc,Math.max(0,i+_25));
this.setFocusIndex(row,col);
if(_24){
this.grid.updateRow(r);
}
}
},previousKey:function(e){
if(!this.isNavHeader()){
this.focusHeader();
dojo.stopEvent(e);
}else{
if(this.grid.edit.isEditing()){
dojo.stopEvent(e);
this.previous();
}else{
this.tabOut(this.grid.domNode);
}
}
},nextKey:function(e){
if(e.target===this.grid.domNode){
this.focusHeader();
dojo.stopEvent(e);
}else{
if(this.isNavHeader()){
this._colHeadNode=null;
if(this.isNoFocusCell()){
this.setFocusIndex(0,0);
}else{
if(this.cell){
this.focusGrid();
}
}
}else{
if(this.grid.edit.isEditing()){
dojo.stopEvent(e);
this.next();
}else{
this.tabOut(this.grid.lastFocusNode);
}
}
}
},tabOut:function(_30){
this.tabbingOut=true;
_30.focus();
},focusGridView:function(){
dojox.grid.util.fire(this.focusView,"focus");
},focusGrid:function(_31){
this.focusGridView();
this._focusifyCellNode(true);
},focusHeader:function(){
var _32=this._findHeaderCells();
if(this.isNoFocusCell()){
this._colHeadNode=_32[0];
}else{
this._colHeadNode=_32[this.cell.index];
}
if(this._colHeadNode){
this._colHeadNode.focus();
this._focusifyCellNode(false);
}
},doFocus:function(e){
if(e&&e.target!=e.currentTarget){
dojo.stopEvent(e);
return;
}
if(!this.tabbingOut){
this.focusHeader();
}
this.tabbingOut=false;
dojo.stopEvent(e);
},doBlur:function(e){
dojo.stopEvent(e);
},doBlurHeader:function(e){
dojo.stopEvent(e);
},doLastNodeFocus:function(e){
if(this.tabbingOut){
this._focusifyCellNode(false);
}else{
this._focusifyCellNode(true);
}
this.tabbingOut=false;
dojo.stopEvent(e);
},doLastNodeBlur:function(e){
dojo.stopEvent(e);
},doColHeaderFocus:function(e){
dojo.toggleClass(e.target,this.focusClass,true);
},doColHeaderBlur:function(e){
dojo.toggleClass(e.target,this.focusClass,false);
}});
}
