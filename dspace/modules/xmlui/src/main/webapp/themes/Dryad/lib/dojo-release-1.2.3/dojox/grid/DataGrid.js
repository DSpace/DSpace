/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.grid.DataGrid"]){
dojo._hasResource["dojox.grid.DataGrid"]=true;
dojo.provide("dojox.grid.DataGrid");
dojo.require("dojox.grid._Grid");
dojo.require("dojox.grid.DataSelection");
dojo.declare("dojox.grid.DataGrid",dojox.grid._Grid,{store:null,query:null,queryOptions:null,fetchText:"...",items:null,_store_connects:null,_by_idty:null,_by_idx:null,_cache:null,_pages:null,_pending_requests:null,_bop:-1,_eop:-1,_requests:0,rowCount:0,_isLoaded:false,_isLoading:false,postCreate:function(){
this._pages=[];
this._store_connects=[];
this._by_idty={};
this._by_idx=[];
this._cache=[];
this._pending_requests={};
this._setStore(this.store);
this.inherited(arguments);
},createSelection:function(){
this.selection=new dojox.grid.DataSelection(this);
},get:function(_1,_2){
return (!_2?this.defaultValue:(!this.field?this.value:this.grid.store.getValue(_2,this.field)));
},_onSet:function(_3,_4,_5,_6){
var _7=this.getItemIndex(_3);
if(_7>-1){
this.updateRow(_7);
}
},_addItem:function(_8,_9,_a){
var _b=this._hasIdentity?this.store.getIdentity(_8):dojo.toJson(this.query)+":idx:"+_9+":sort:"+dojo.toJson(this.getSortProps());
var o={idty:_b,item:_8};
this._by_idty[_b]=this._by_idx[_9]=o;
if(!_a){
this.updateRow(_9);
}
},_onNew:function(_d,_e){
this.updateRowCount(this.rowCount+1);
this._addItem(_d,this.rowCount-1);
this.showMessage();
},_onDelete:function(_f){
var idx=this._getItemIndex(_f,true);
if(idx>=0){
var o=this._by_idx[idx];
this._by_idx.splice(idx,1);
delete this._by_idty[o.idty];
this.updateRowCount(this.rowCount-1);
if(this.rowCount===0){
this.showMessage(this.noDataMessage);
}
}
},_onRevert:function(){
this._refresh();
},setStore:function(_12,_13,_14){
this._setQuery(_13,_14);
this._setStore(_12);
this._refresh(true);
},setQuery:function(_15,_16){
this._setQuery(_15,_16);
this._refresh(true);
},setItems:function(_17){
this.items=_17;
this._setStore(this.store);
this._refresh(true);
},_setQuery:function(_18,_19){
this.query=_18||this.query;
this.queryOptions=_19||this.queryOptions;
},_setStore:function(_1a){
if(this.store&&this._store_connects){
dojo.forEach(this._store_connects,function(arr){
dojo.forEach(arr,dojo.disconnect);
});
}
this.store=_1a;
if(this.store){
var f=this.store.getFeatures();
var h=[];
this._canEdit=!!f["dojo.data.api.Write"]&&!!f["dojo.data.api.Identity"];
this._hasIdentity=!!f["dojo.data.api.Identity"];
if(!!f["dojo.data.api.Notification"]&&!this.items){
h.push(this.connect(this.store,"onSet","_onSet"));
h.push(this.connect(this.store,"onNew","_onNew"));
h.push(this.connect(this.store,"onDelete","_onDelete"));
}
if(this._canEdit){
h.push(this.connect(this.store,"revert","_onRevert"));
}
this._store_connects=h;
}
},_onFetchBegin:function(_1e,req){
if(this.rowCount!=_1e){
if(req.isRender){
this.scroller.init(_1e,this.keepRows,this.rowsPerPage);
this.prerender();
}
this.updateRowCount(_1e);
}
},_onFetchComplete:function(_20,req){
if(_20&&_20.length>0){
dojo.forEach(_20,function(_22,idx){
this._addItem(_22,req.start+idx,true);
},this);
this.updateRows(req.start,_20.length);
if(req.isRender){
this.setScrollTop(0);
this.postrender();
}else{
if(this._lastScrollTop){
this.setScrollTop(this._lastScrollTop);
}
}
}
delete this._lastScrollTop;
if(!this._isLoaded){
this._isLoading=false;
this._isLoaded=true;
if(!_20||!_20.length){
this.showMessage(this.noDataMessage);
}else{
this.showMessage();
}
}
this._pending_requests[req.start]=false;
},_onFetchError:function(err,req){

delete this._lastScrollTop;
if(!this._isLoaded){
this._isLoading=false;
this._isLoaded=true;
this.showMessage(this.errorMessage);
}
this.onFetchError(err,req);
},onFetchError:function(err,req){
},_fetch:function(_28,_29){
var _28=_28||0;
if(this.store&&!this._pending_requests[_28]){
if(!this._isLoaded&&!this._isLoading){
this._isLoading=true;
this.showMessage(this.loadingMessage);
}
this._pending_requests[_28]=true;
try{
if(this.items){
var _2a=this.items;
var _2b=this.store;
this.rowsPerPage=_2a.length;
var req={start:_28,count:this.rowsPerPage,isRender:_29};
this._onFetchBegin(_2a.length,req);
var _2d=0;
dojo.forEach(_2a,function(i){
if(!_2b.isItemLoaded(i)){
_2d++;
}
});
if(_2d===0){
this._onFetchComplete(_2a,req);
}else{
var _2f=function(_30){
_2d--;
if(_2d===0){
this._onFetchComplete(_2a,req);
}
};
dojo.forEach(_2a,function(i){
if(!_2b.isItemLoaded(i)){
_2b.loadItem({item:i,onItem:_2f,scope:this});
}
},this);
}
}else{
this.store.fetch({start:_28,count:this.rowsPerPage,query:this.query,sort:this.getSortProps(),queryOptions:this.queryOptions,isRender:_29,onBegin:dojo.hitch(this,"_onFetchBegin"),onComplete:dojo.hitch(this,"_onFetchComplete"),onError:dojo.hitch(this,"_onFetchError")});
}
}
catch(e){
this._onFetchError(e);
}
}
},_clearData:function(){
this.updateRowCount(0);
this._by_idty={};
this._by_idx=[];
this._pages=[];
this._bop=this._eop=-1;
this._isLoaded=false;
this._isLoading=false;
},getItem:function(idx){
var _33=this._by_idx[idx];
if(!_33||(_33&&!_33.item)){
this._preparePage(idx);
return null;
}
return _33.item;
},getItemIndex:function(_34){
return this._getItemIndex(_34,false);
},_getItemIndex:function(_35,_36){
if(!_36&&!this.store.isItem(_35)){
return -1;
}
var _37=this._hasIdentity?this.store.getIdentity(_35):null;
for(var i=0,l=this._by_idx.length;i<l;i++){
var d=this._by_idx[i];
if(d&&((_37&&d.idty==_37)||(d.item===_35))){
return i;
}
}
return -1;
},filter:function(_3b,_3c){
this.query=_3b;
if(_3c){
this._clearData();
}
this._fetch();
},_getItemAttr:function(idx,_3e){
var _3f=this.getItem(idx);
return (!_3f?this.fetchText:this.store.getValue(_3f,_3e));
},_render:function(){
if(this.domNode.parentNode){
this.scroller.init(this.rowCount,this.keepRows,this.rowsPerPage);
this.prerender();
this._fetch(0,true);
}
},_requestsPending:function(_40){
return this._pending_requests[_40];
},_rowToPage:function(_41){
return (this.rowsPerPage?Math.floor(_41/this.rowsPerPage):_41);
},_pageToRow:function(_42){
return (this.rowsPerPage?this.rowsPerPage*_42:_42);
},_preparePage:function(_43){
if(_43<this._bop||_43>=this._eop){
var _44=this._rowToPage(_43);
this._needPage(_44);
this._bop=_44*this.rowsPerPage;
this._eop=this._bop+(this.rowsPerPage||this.rowCount);
}
},_needPage:function(_45){
if(!this._pages[_45]){
this._pages[_45]=true;
this._requestPage(_45);
}
},_requestPage:function(_46){
var row=this._pageToRow(_46);
var _48=Math.min(this.rowsPerPage,this.rowCount-row);
if(_48>0){
this._requests++;
if(!this._requestsPending(row)){
setTimeout(dojo.hitch(this,"_fetch",row,false),1);
}
}
},getCellName:function(_49){
return _49.field;
},_refresh:function(_4a){
this._clearData();
this._fetch(0,_4a);
},sort:function(){
this._lastScrollTop=this.scrollTop;
this._refresh();
},canSort:function(){
return (!this._isLoading);
},getSortProps:function(){
var c=this.getCell(this.getSortIndex());
if(!c){
return null;
}else{
var _4c=c["sortDesc"];
var si=!(this.sortInfo>0);
if(typeof _4c=="undefined"){
_4c=si;
}else{
_4c=si?!_4c:_4c;
}
return [{attribute:c.field,descending:_4c}];
}
},styleRowState:function(_4e){
if(this.store&&this.store.getState){
var _4f=this.store.getState(_4e.index),c="";
for(var i=0,ss=["inflight","error","inserting"],s;s=ss[i];i++){
if(_4f[s]){
c=" dojoxGridRow-"+s;
break;
}
}
_4e.customClasses+=c;
}
},onStyleRow:function(_54){
this.styleRowState(_54);
this.inherited(arguments);
},canEdit:function(_55,_56){
return this._canEdit;
},_copyAttr:function(idx,_58){
var row={};
var _5a={};
var src=this.getItem(idx);
return this.store.getValue(src,_58);
},doStartEdit:function(_5c,_5d){
if(!this._cache[_5d]){
this._cache[_5d]=this._copyAttr(_5d,_5c.field);
}
this.onStartEdit(_5c,_5d);
},doApplyCellEdit:function(_5e,_5f,_60){
this.store.fetchItemByIdentity({identity:this._by_idx[_5f].idty,onItem:dojo.hitch(this,function(_61){
this.store.setValue(_61,_60,_5e);
this.onApplyCellEdit(_5e,_5f,_60);
})});
},doCancelEdit:function(_62){
var _63=this._cache[_62];
if(_63){
this.updateRow(_62);
delete this._cache[_62];
}
this.onCancelEdit.apply(this,arguments);
},doApplyEdit:function(_64,_65){
var _66=this._cache[_64];
this.onApplyEdit(_64);
},removeSelectedRows:function(){
if(this._canEdit){
this.edit.apply();
var _67=this.selection.getSelected();
if(_67.length){
dojo.forEach(_67,this.store.deleteItem,this.store);
this.selection.clear();
}
}
}});
dojox.grid.DataGrid.markupFactory=function(_68,_69,_6a,_6b){
return dojox.grid._Grid.markupFactory(_68,_69,_6a,function(_6c,_6d){
var _6e=dojo.trim(dojo.attr(_6c,"field")||"");
if(_6e){
_6d.field=_6e;
}
_6d.field=_6d.field||_6d.name;
if(_6b){
_6b(_6c,_6d);
}
});
};
}
