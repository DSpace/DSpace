/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid.compat.VirtualGrid"]){
dojo._hasResource["dojox.grid.compat.VirtualGrid"]=true;
dojo.provide("dojox.grid.compat.VirtualGrid");
dojo.require("dojox.grid.compat._grid.lib");
dojo.require("dojox.grid.compat._grid.scroller");
dojo.require("dojox.grid.compat._grid.view");
dojo.require("dojox.grid.compat._grid.views");
dojo.require("dojox.grid.compat._grid.layout");
dojo.require("dojox.grid.compat._grid.rows");
dojo.require("dojox.grid.compat._grid.focus");
dojo.require("dojox.grid.compat._grid.selection");
dojo.require("dojox.grid.compat._grid.edit");
dojo.require("dojox.grid.compat._grid.rowbar");
dojo.require("dojox.grid.compat._grid.publicEvents");
dojo.declare("dojox.VirtualGrid",[dijit._Widget,dijit._Templated],{templateString:"<div class=\"dojoxGrid\" hidefocus=\"hidefocus\" role=\"wairole:grid\">\n\t<div class=\"dojoxGrid-master-header\" dojoAttachPoint=\"viewsHeaderNode\"></div>\n\t<div class=\"dojoxGrid-master-view\" dojoAttachPoint=\"viewsNode\"></div>\n\t<span dojoAttachPoint=\"lastFocusNode\" tabindex=\"0\"></span>\n</div>\n",classTag:"dojoxGrid",get:function(_1){
},rowCount:5,keepRows:75,rowsPerPage:25,autoWidth:false,autoHeight:false,autoRender:true,defaultHeight:"15em",structure:"",elasticView:-1,singleClickEdit:false,_click:null,sortInfo:0,themeable:true,buildRendering:function(){
this.inherited(arguments);
if(this.get==dojox.VirtualGrid.prototype.get){
this.get=null;
}
if(!this.domNode.getAttribute("tabIndex")){
this.domNode.tabIndex="0";
}
this.createScroller();
this.createLayout();
this.createViews();
this.createManagers();
dojox.grid.initTextSizePoll();
this.connect(dojox.grid,"textSizeChanged","textSizeChanged");
dojox.grid.funnelEvents(this.domNode,this,"doKeyEvent",dojox.grid.keyEvents);
this.connect(this,"onShow","renderOnIdle");
},postCreate:function(){
this.styleChanged=this._styleChanged;
this.setStructure(this.structure);
this._click=[];
},destroy:function(){
this.domNode.onReveal=null;
this.domNode.onSizeChange=null;
this.edit.destroy();
this.views.destroyViews();
this.inherited(arguments);
},styleChanged:function(){
this.setStyledClass(this.domNode,"");
},_styleChanged:function(){
this.styleChanged();
this.update();
},textSizeChanged:function(){
setTimeout(dojo.hitch(this,"_textSizeChanged"),1);
},_textSizeChanged:function(){
if(this.domNode){
this.views.forEach(function(v){
v.content.update();
});
this.render();
}
},sizeChange:function(){
dojox.grid.jobs.job(this.id+"SizeChange",50,dojo.hitch(this,"update"));
},renderOnIdle:function(){
setTimeout(dojo.hitch(this,"render"),1);
},createManagers:function(){
this.rows=new dojox.grid.rows(this);
this.focus=new dojox.grid.focus(this);
this.selection=new dojox.grid.selection(this);
this.edit=new dojox.grid.edit(this);
},createScroller:function(){
this.scroller=new dojox.grid.scroller.columns();
this.scroller._pageIdPrefix=this.id+"-";
this.scroller.renderRow=dojo.hitch(this,"renderRow");
this.scroller.removeRow=dojo.hitch(this,"rowRemoved");
},createLayout:function(){
this.layout=new dojox.grid.layout(this);
},createViews:function(){
this.views=new dojox.grid.views(this);
this.views.createView=dojo.hitch(this,"createView");
},createView:function(_3){
if(dojo.isAIR){
var _4=window;
var _5=_3.split(".");
for(var i=0;i<_5.length;i++){
if(typeof _4[_5[i]]=="undefined"){
var _7=_5[0];
for(var j=1;j<=i;j++){
_7+="."+_5[j];
}
throw new Error(_7+" is undefined");
}
_4=_4[_5[i]];
}
var c=_4;
}else{
var c=eval(_3);
}
var _a=new c({grid:this});
this.viewsNode.appendChild(_a.domNode);
this.viewsHeaderNode.appendChild(_a.headerNode);
this.views.addView(_a);
return _a;
},buildViews:function(){
for(var i=0,vs;(vs=this.layout.structure[i]);i++){
this.createView(vs.type||dojox._scopeName+".GridView").setStructure(vs);
}
this.scroller.setContentNodes(this.views.getContentNodes());
},setStructure:function(_d){
this.views.destroyViews();
this.structure=_d;
if((this.structure)&&(dojo.isString(this.structure))){
this.structure=dojox.grid.getProp(this.structure);
}
if(!this.structure){
this.structure=window["layout"];
}
if(!this.structure){
return;
}
this.layout.setStructure(this.structure);
this._structureChanged();
},_structureChanged:function(){
this.buildViews();
if(this.autoRender){
this.render();
}
},hasLayout:function(){
return this.layout.cells.length;
},resize:function(_e){
this._sizeBox=_e;
this._resize();
this.sizeChange();
},_getPadBorder:function(){
this._padBorder=this._padBorder||dojo._getPadBorderExtents(this.domNode);
return this._padBorder;
},_resize:function(){
if(!this.domNode.parentNode||this.domNode.parentNode.nodeType!=1||!this.hasLayout()){
return;
}
var _f=this._getPadBorder();
if(this.autoHeight){
this.domNode.style.height="auto";
this.viewsNode.style.height="";
}else{
if(this.flex>0){
}else{
if(this.domNode.clientHeight<=_f.h){
if(this.domNode.parentNode==document.body){
this.domNode.style.height=this.defaultHeight;
}else{
this.fitTo="parent";
}
}
}
}
if(this._sizeBox){
dojo.contentBox(this.domNode,this._sizeBox);
}else{
if(this.fitTo=="parent"){
var h=dojo._getContentBox(this.domNode.parentNode).h;
dojo.marginBox(this.domNode,{h:Math.max(0,h)});
}
}
var h=dojo._getContentBox(this.domNode).h;
if(h==0&&!this.autoHeight){
this.viewsHeaderNode.style.display="none";
}else{
this.viewsHeaderNode.style.display="block";
}
this.adaptWidth();
this.adaptHeight();
this.scroller.defaultRowHeight=this.rows.getDefaultHeightPx()+1;
this.postresize();
},adaptWidth:function(){
var w=this.autoWidth?0:this.domNode.clientWidth||(this.domNode.offsetWidth-this._getPadBorder().w);
vw=this.views.arrange(1,w);
this.views.onEach("adaptWidth");
if(this.autoWidth){
this.domNode.style.width=vw+"px";
}
},adaptHeight:function(){
var vns=this.viewsHeaderNode.style,t=vns.display=="none"?0:this.views.measureHeader();
vns.height=t+"px";
this.views.normalizeHeaderNodeHeight();
var h=(this.autoHeight?-1:Math.max(this.domNode.clientHeight-t,0)||0);
this.views.onEach("setSize",[0,h]);
this.views.onEach("adaptHeight");
this.scroller.windowHeight=h;
},render:function(){
if(!this.domNode){
return;
}
if(!this.hasLayout()){
this.scroller.init(0,this.keepRows,this.rowsPerPage);
return;
}
this.update=this.defaultUpdate;
this.scroller.init(this.rowCount,this.keepRows,this.rowsPerPage);
this.prerender();
this.setScrollTop(0);
this.postrender();
},prerender:function(){
this.keepRows=this.autoHeight?0:this.constructor.prototype.keepRows;
this.scroller.setKeepInfo(this.keepRows);
this.views.render();
this._resize();
},postrender:function(){
this.postresize();
this.focus.initFocusView();
dojo.setSelectable(this.domNode,false);
},postresize:function(){
if(this.autoHeight){
this.viewsNode.style.height=this.views.measureContent()+"px";
}
},renderRow:function(_15,_16){
this.views.renderRow(_15,_16);
},rowRemoved:function(_17){
this.views.rowRemoved(_17);
},invalidated:null,updating:false,beginUpdate:function(){
if(this.invalidated==null){
this.invalidated={rows:[],count:1,all:false,rowCount:undefined};
}else{
this.invalidated.count++;
}
this.updating=true;
},endUpdate:function(){
var i=this.invalidated;
if(--i.count===0){
this.updating=false;
if(i.rows.length>0){
for(r in i.rows){
this.updateRow(Number(r));
}
this.invalidated.rows=[];
}
if(i.rowCount!=undefined){
this.updateRowCount(i.rowCount);
i.rowCount=undefined;
}
if(i.all){
this.update();
i.all=false;
}
}
},defaultUpdate:function(){
if(!this.domNode){
return;
}
if(this.updating){
this.invalidated.all=true;
return;
}
this.prerender();
this.scroller.invalidateNodes();
this.setScrollTop(this.scrollTop);
this.postrender();
},update:function(){
this.render();
},updateRow:function(_19){
_19=Number(_19);
if(this.updating){
this.invalidated.rows[_19]=true;
}else{
this.views.updateRow(_19,this.rows.getHeight(_19));
this.scroller.rowHeightChanged(_19);
}
},updateRowCount:function(_1a){
if(this.updating){
this.invalidated.rowCount=_1a;
}else{
this.rowCount=_1a;
if(this.layout.cells.length){
this.scroller.updateRowCount(_1a);
this.setScrollTop(this.scrollTop);
}
this._resize();
}
},updateRowStyles:function(_1b){
this.views.updateRowStyles(_1b);
},rowHeightChanged:function(_1c){
this.views.renormalizeRow(_1c);
this.scroller.rowHeightChanged(_1c);
},fastScroll:true,delayScroll:false,scrollRedrawThreshold:(dojo.isIE?100:50),scrollTo:function(_1d){
if(!this.fastScroll){
this.setScrollTop(_1d);
return;
}
var _1e=Math.abs(this.lastScrollTop-_1d);
this.lastScrollTop=_1d;
if(_1e>this.scrollRedrawThreshold||this.delayScroll){
this.delayScroll=true;
this.scrollTop=_1d;
this.views.setScrollTop(_1d);
dojox.grid.jobs.job("dojoxGrid-scroll",200,dojo.hitch(this,"finishScrollJob"));
}else{
this.setScrollTop(_1d);
}
},finishScrollJob:function(){
this.delayScroll=false;
this.setScrollTop(this.scrollTop);
},setScrollTop:function(_1f){
this.scrollTop=this.views.setScrollTop(_1f);
this.scroller.scroll(this.scrollTop);
},scrollToRow:function(_20){
this.setScrollTop(this.scroller.findScrollTop(_20)+1);
},styleRowNode:function(_21,_22){
if(_22){
this.rows.styleRowNode(_21,_22);
}
},getCell:function(_23){
return this.layout.cells[_23];
},setCellWidth:function(_24,_25){
this.getCell(_24).unitWidth=_25;
},getCellName:function(_26){
return "Cell "+_26.index;
},canSort:function(_27){
},sort:function(){
},getSortAsc:function(_28){
_28=_28==undefined?this.sortInfo:_28;
return Boolean(_28>0);
},getSortIndex:function(_29){
_29=_29==undefined?this.sortInfo:_29;
return Math.abs(_29)-1;
},setSortIndex:function(_2a,_2b){
var si=_2a+1;
if(_2b!=undefined){
si*=(_2b?1:-1);
}else{
if(this.getSortIndex()==_2a){
si=-this.sortInfo;
}
}
this.setSortInfo(si);
},setSortInfo:function(_2d){
if(this.canSort(_2d)){
this.sortInfo=_2d;
this.sort();
this.update();
}
},doKeyEvent:function(e){
e.dispatch="do"+e.type;
this.onKeyEvent(e);
},_dispatch:function(m,e){
if(m in this){
return this[m](e);
}
},dispatchKeyEvent:function(e){
this._dispatch(e.dispatch,e);
},dispatchContentEvent:function(e){
this.edit.dispatchEvent(e)||e.sourceView.dispatchContentEvent(e)||this._dispatch(e.dispatch,e);
},dispatchHeaderEvent:function(e){
e.sourceView.dispatchHeaderEvent(e)||this._dispatch("doheader"+e.type,e);
},dokeydown:function(e){
this.onKeyDown(e);
},doclick:function(e){
if(e.cellNode){
this.onCellClick(e);
}else{
this.onRowClick(e);
}
},dodblclick:function(e){
if(e.cellNode){
this.onCellDblClick(e);
}else{
this.onRowDblClick(e);
}
},docontextmenu:function(e){
if(e.cellNode){
this.onCellContextMenu(e);
}else{
this.onRowContextMenu(e);
}
},doheaderclick:function(e){
if(e.cellNode){
this.onHeaderCellClick(e);
}else{
this.onHeaderClick(e);
}
},doheaderdblclick:function(e){
if(e.cellNode){
this.onHeaderCellDblClick(e);
}else{
this.onHeaderDblClick(e);
}
},doheadercontextmenu:function(e){
if(e.cellNode){
this.onHeaderCellContextMenu(e);
}else{
this.onHeaderContextMenu(e);
}
},doStartEdit:function(_3b,_3c){
this.onStartEdit(_3b,_3c);
},doApplyCellEdit:function(_3d,_3e,_3f){
this.onApplyCellEdit(_3d,_3e,_3f);
},doCancelEdit:function(_40){
this.onCancelEdit(_40);
},doApplyEdit:function(_41){
this.onApplyEdit(_41);
},addRow:function(){
this.updateRowCount(this.rowCount+1);
},removeSelectedRows:function(){
this.updateRowCount(Math.max(0,this.rowCount-this.selection.getSelected().length));
this.selection.clear();
}});
dojo.mixin(dojox.VirtualGrid.prototype,dojox.grid.publicEvents);
}
