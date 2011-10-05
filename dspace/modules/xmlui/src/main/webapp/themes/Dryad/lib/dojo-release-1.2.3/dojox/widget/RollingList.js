/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.widget.RollingList"]){
dojo._hasResource["dojox.widget.RollingList"]=true;
dojo.provide("dojox.widget.RollingList");
dojo.experimental("dojox.widget.RollingList");
dojo.require("dijit._Templated");
dojo.require("dijit.layout.ContentPane");
dojo.require("dijit.Menu");
dojo.require("dojox.html.metrics");
dojo.require("dojo.i18n");
dojo.requireLocalization("dojox.widget","RollingList",null,"ROOT");
dojo.declare("dojox.widget._RollingListPane",[dijit.layout.ContentPane,dijit._Templated,dijit._Contained],{templateString:"<div class=\"dojoxRollingListPane\"><table><tbody><tr><td dojoAttachPoint=\"containerNode\"></td></tr></tbody></div>",parentWidget:null,parentPane:null,store:null,items:null,query:null,queryOptions:null,_focusByNode:true,_setContentAndScroll:function(_1){
this._setContent(_1);
this.parentWidget.scrollIntoView(this);
},startup:function(){
if(this._started){
return;
}
if(this.store&&this.store.getFeatures()["dojo.data.api.Notification"]){
window.setTimeout(dojo.hitch(this,function(){
this.connect(this.store,"onSet","_onSetItem");
this.connect(this.store,"onNew","_onNewItem");
this.connect(this.store,"onDelete","_onDeleteItem");
}),1);
}
this.connect(this.focusNode||this.domNode,"onkeypress","_focusKey");
this.parentWidget._updateClass(this.domNode,"Pane");
this.inherited(arguments);
},_focusKey:function(e){
if(e.charOrCode==dojo.keys.BACKSPACE){
dojo.stopEvent(e);
return;
}else{
if(e.charOrCode==dojo.keys.LEFT_ARROW&&this.parentPane){
this.parentPane.focus();
this.parentWidget.scrollIntoView(this.parentPane);
}else{
if(e.charOrCode==dojo.keys.ENTER){
this.parentWidget.onExecute();
}
}
}
},focus:function(_3){
if(this.parentWidget._focusedPane!=this){
this.parentWidget._focusedPane=this;
this.parentWidget.scrollIntoView(this);
if(this._focusByNode&&(!this.parentWidget._savedFocus||_3)){
try{
(this.focusNode||this.domNode).focus();
}
catch(e){
}
}
}
},_loadCheck:function(_4){
if(!this._started){
var c=this.connect(this,"startup",function(){
this.disconnect(c);
this._loadCheck(_4);
});
}
var _6=this.domNode&&this._isShown();
if((this.store||this.items)&&(_4||(this.refreshOnShow&&_6)||(!this.isLoaded&&_6))){
this._doQuery();
}
},_doQuery:function(){
this.isLoaded=false;
if(this.items){
var _7=0,_8=this.store,_9=this.items;
dojo.forEach(_9,function(_a){
if(!_8.isItemLoaded(_a)){
_7++;
}
});
if(_7===0){
this.onItems();
}else{
var _b=dojo.hitch(this,function(_c){
_7--;
if((_7)===0){
this.onItems();
}
});
this._setContentAndScroll(this.onLoadStart());
dojo.forEach(_9,function(_d){
if(!_8.isItemLoaded(_d)){
_8.loadItem({item:_d,onItem:_b});
}
});
}
}else{
this._setContentAndScroll(this.onFetchStart());
this.store.fetch({query:this.query,onComplete:function(_e){
this.items=_e;
this.onItems();
},onError:function(e){
this._onError("Fetch",e);
},scope:this});
}
},_hasItem:function(_10){
var _11=this.items||[];
for(var i=0,_13;(_13=_11[i]);i++){
if(this.parentWidget._itemsMatch(_13,_10)){
return true;
}
}
return false;
},_onSetItem:function(_14,_15,_16,_17){
if(this._hasItem(_14)){
this._loadCheck(true);
}
},_onNewItem:function(_18,_19){
var sel;
if((!_19&&!this.parentPane)||(_19&&this.parentPane&&this.parentPane._hasItem(_19.item)&&(sel=this.parentPane._getSelected())&&this.parentWidget._itemsMatch(sel.item,_19.item))){
this.items.push(_18);
this._loadCheck(true);
}else{
if(_19&&this.parentPane&&this._hasItem(_19.item)){
this._loadCheck(true);
}
}
},_onDeleteItem:function(_1b){
if(this._hasItem(_1b)){
this.items=dojo.filter(this.items,function(i){
return (i!=_1b);
});
this._loadCheck(true);
}
},onFetchStart:function(){
return this.loadingMessage;
},onFetchError:function(_1d){
return this.errorMessage;
},onLoadStart:function(){
return this.loadingMessage;
},onLoadError:function(_1e){
return this.errorMessage;
},onItems:function(){
this._onLoadHandler();
}});
dojo.declare("dojox.widget._RollingListGroupPane",[dojox.widget._RollingListPane],{templateString:"<div><div dojoAttachPoint=\"containerNode\"></div>"+"<div dojoAttachPoint=\"menuContainer\">"+"<div dojoAttachPoint=\"menuNode\"></div>"+"</div></div>",_menu:null,_loadCheck:function(_1f){
var _20=this._isShown();
if((this.store||this.items)&&(_1f||(this.refreshOnShow&&_20)||(!this.isLoaded&&_20))){
this._doQuery();
}
},_setContent:function(_21){
if(!this._menu){
this.inherited(arguments);
}
},onItems:function(){
var _22,_23=false;
if(this._menu){
_22=this._getSelected();
this._menu.destroyRecursive();
}
this._menu=this._getMenu();
var _24,_25;
if(this.items.length){
dojo.forEach(this.items,function(_26){
_24=this.parentWidget._getMenuItemForItem(_26,this);
if(_24){
if(_22&&this.parentWidget._itemsMatch(_24.item,_22.item)){
_25=_24;
}
this._menu.addChild(_24);
}
},this);
}else{
_24=this.parentWidget._getMenuItemForItem(null,this);
if(_24){
this._menu.addChild(_24);
}
}
if(_25){
this._setSelected(_25);
if((_22&&!_22.children&&_25.children)||(_22&&_22.children&&!_25.children)){
var _27=this.parentWidget._getPaneForItem(_25.item,this,_25.children);
if(_27){
this.parentWidget.addChild(_27,this.getIndexInParent()+1);
}else{
this.parentWidget._removeAfter(this);
this.parentWidget._onItemClick(null,this,_25.item,_25.children);
}
}
}else{
if(_22){
this.parentWidget._removeAfter(this);
}
}
this.containerNode.innerHTML="";
this.containerNode.appendChild(this._menu.domNode);
this.parentWidget.scrollIntoView(this);
this.inherited(arguments);
},startup:function(){
this.inherited(arguments);
this.parentWidget._updateClass(this.domNode,"GroupPane");
},focus:function(_28){
if(this._menu){
if(this._pendingFocus){
this.disconnect(this._pendingFocus);
}
delete this._pendingFocus;
var _29=this._menu.focusedChild;
if(!_29){
var _2a=dojo.query(".dojoxRollingListItemSelected",this.domNode)[0];
if(_2a){
_29=dijit.byNode(_2a);
}
}
if(!_29){
_29=this._menu.getChildren()[0]||this._menu;
}
this._focusByNode=false;
if(_29.focusNode){
if(!this.parentWidget._savedFocus||_28){
try{
_29.focusNode.focus();
}
catch(e){
}
}
window.setTimeout(function(){
try{
dijit.scrollIntoView(_29.focusNode);
}
catch(e){
}
},1);
}else{
if(_29.focus){
if(!this.parentWidget._savedFocus||_28){
_29.focus();
}
}else{
this._focusByNode=true;
}
}
this.inherited(arguments);
}else{
if(!this._pendingFocus){
this._pendingFocus=this.connect(this,"onItems","focus");
}
}
},_getMenu:function(){
var _2b=this;
var _2c=new dijit.Menu({parentMenu:this.parentPane?this.parentPane._menu:null,onCancel:function(_2d){
if(_2b.parentPane){
_2b.parentPane.focus(true);
}
},_moveToPopup:function(evt){
if(this.focusedChild&&!this.focusedChild.disabled){
this.focusedChild._onClick(evt);
}
}},this.menuNode);
this.connect(_2c,"onItemClick",function(_2f,evt){
if(_2f.disabled){
return;
}
evt.alreadySelected=dojo.hasClass(_2f.domNode,"dojoxRollingListItemSelected");
if(evt.alreadySelected&&((evt.type=="keypress"&&evt.charOrCode!=dojo.keys.ENTER)||(evt.type=="internal"))){
var p=this.parentWidget.getChildren()[this.getIndexInParent()+1];
if(p){
p.focus(true);
this.parentWidget.scrollIntoView(p);
}
}else{
this._setSelected(_2f,_2c);
this.parentWidget._onItemClick(evt,this,_2f.item,_2f.children);
if(evt.type=="keypress"&&evt.charOrCode==dojo.keys.ENTER){
this.parentWidget.onExecute();
}
}
});
if(!_2c._started){
_2c.startup();
}
return _2c;
},_getSelected:function(_32){
if(!_32){
_32=this._menu;
}
if(_32){
var _33=this._menu.getChildren();
for(var i=0,_35;(_35=_33[i]);i++){
if(dojo.hasClass(_35.domNode,"dojoxRollingListItemSelected")){
return _35;
}
}
}
return null;
},_setSelected:function(_36,_37){
if(!_37){
_37=this._menu;
}
if(_37){
dojo.forEach(_37.getChildren(),function(i){
this.parentWidget._updateClass(i.domNode,"Item",{"Selected":(_36&&(i==_36&&!i.disabled))});
},this);
}
}});
dojo.declare("dojox.widget.RollingList",[dijit._Widget,dijit._Templated,dijit._Container],{templateString:"<div class=\"dojoxRollingList ${className}\" dojoAttachPoint=\"containerNode\" dojoAttachEvent=\"onkeypress:_onKey\"></div>",className:"",store:null,query:null,queryOptions:null,childrenAttrs:["children"],parentAttr:"",value:null,_itemsMatch:function(_39,_3a){
if(!_39&&!_3a){
return true;
}else{
if(!_39||!_3a){
return false;
}
}
return (_39==_3a||(this._isIdentity&&this.store.getIdentity(_39)==this.store.getIdentity(_3a)));
},_removeAfter:function(idx){
if(typeof idx!="number"){
idx=this.getIndexOfChild(idx);
}
if(idx>=0){
dojo.forEach(this.getChildren(),function(c,i){
if(i>idx){
this.removeChild(c);
c.destroyRecursive();
}
},this);
}
var _3e=this.getChildren(),_3f=_3e[_3e.length-1];
var _40=null;
while(_3f&&!_40){
var val=_3f._getSelected?_3f._getSelected():null;
if(val){
_40=val.item;
}
_3f=_3f.parentPane;
}
if(!this._setInProgress){
this._setValue(_40);
}
},addChild:function(_42,_43){
if(_43>0){
this._removeAfter(_43-1);
}
this.inherited(arguments);
if(!_42._started){
_42.startup();
}
this.layout();
if(!this._savedFocus){
_42.focus();
}
},_updateClass:function(_44,_45,_46){
if(!this._declaredClasses){
this._declaredClasses=("dojoxRollingList "+this.className).split(" ");
}
dojo.forEach(this._declaredClasses,function(c){
if(c){
dojo.addClass(_44,c+_45);
for(var k in _46||{}){
dojo.toggleClass(_44,c+_45+k,_46[k]);
}
dojo.toggleClass(_44,c+_45+"FocusSelected",(dojo.hasClass(_44,c+_45+"Focus")&&dojo.hasClass(_44,c+_45+"Selected")));
dojo.toggleClass(_44,c+_45+"HoverSelected",(dojo.hasClass(_44,c+_45+"Hover")&&dojo.hasClass(_44,c+_45+"Selected")));
}
});
},scrollIntoView:function(_49){
if(this._scrollingTimeout){
window.clearTimeout(this._scrollingTimeout);
}
delete this._scrollingTimeout;
this._scrollingTimeout=window.setTimeout(dojo.hitch(this,function(){
if(_49.domNode){
dijit.scrollIntoView(_49.domNode);
}
delete this._scrollingTimeout;
return;
}),1);
},resize:function(_4a){
dijit.layout._LayoutWidget.prototype.resize.call(this,_4a);
},layout:function(){
var _4b=this.getChildren();
if(this._contentBox){
var _4c=this._contentBox.h-dojox.html.metrics.getScrollbar().h;
dojo.forEach(_4b,function(c){
dojo.marginBox(c.domNode,{h:_4c});
});
}
if(this._focusedPane){
var foc=this._focusedPane;
delete this._focusedPane;
if(!this._savedFocus){
foc.focus();
}
}else{
if(_4b&&_4b.length){
if(!this._savedFocus){
_4b[0].focus();
}
}
}
},_onChange:function(_4f){
this.onChange(_4f);
},_setValue:function(_50){
delete this._setInProgress;
if(!this._itemsMatch(this.value,_50)){
this.value=_50;
this._onChange(_50);
}
},_setValueAttr:function(_51){
if(this._itemsMatch(this.value,_51)&&!_51){
return;
}
if(this._setInProgress&&this._setInProgress===_51){
return;
}
this._setInProgress=_51;
if(!_51||!this.store.isItem(_51)){
var _52=this.getChildren()[0];
_52._setSelected(null);
this._onItemClick(null,_52,null,null);
return;
}
var _53=dojo.hitch(this,function(_54,_55){
var _56=this.store,id;
if(this.parentAttr&&_56.getFeatures()["dojo.data.api.Identity"]&&((id=this.store.getValue(_54,this.parentAttr))||id==="")){
var cb=function(i){
if(_56.getIdentity(i)==_56.getIdentity(_54)){
_55(null);
}else{
_55([i]);
}
};
if(id===""){
_55(null);
}else{
if(typeof id=="string"){
_56.fetchItemByIdentity({identity:id,onItem:cb});
}else{
if(_56.isItem(id)){
cb(id);
}
}
}
}else{
var _5a=this.childrenAttrs.length;
var _5b=[];
dojo.forEach(this.childrenAttrs,function(_5c){
var q={};
q[_5c]=_54;
_56.fetch({query:q,scope:this,onComplete:function(_5e){
if(this._setInProgress!==_51){
return;
}
_5b=_5b.concat(_5e);
_5a--;
if(_5a===0){
_55(_5b);
}
}});
},this);
}
});
var _5f=dojo.hitch(this,function(_60,idx){
var set=_60[idx];
var _63=this.getChildren()[idx];
var _64;
if(set&&_63){
var fx=dojo.hitch(this,function(){
if(_64){
this.disconnect(_64);
}
delete _64;
if(this._setInProgress!==_51){
return;
}
var _66=dojo.filter(_63._menu.getChildren(),function(i){
return this._itemsMatch(i.item,set);
},this)[0];
if(_66){
idx++;
_63._menu.onItemClick(_66,{type:"internal",stopPropagation:function(){
},preventDefault:function(){
}});
if(_60[idx]){
_5f(_60,idx);
}else{
this._setValue(set);
this.onItemClick(set,_63,this.getChildItems(set));
}
}
});
if(!_63.isLoaded){
_64=this.connect(_63,"onLoad",fx);
}else{
fx();
}
}else{
if(idx===0){
this.attr("value",null);
}
}
});
var _68=[];
var _69=dojo.hitch(this,function(_6a){
if(_6a&&_6a.length){
_68.push(_6a[0]);
_53(_6a[0],_69);
}else{
if(!_6a){
_68.pop();
}
_68.reverse();
_5f(_68,0);
}
});
var ns=this.domNode.style;
if(ns.display=="none"||ns.visibility=="hidden"){
this._setValue(_51);
}else{
if(!this._itemsMatch(_51,this._visibleItem)){
_69([_51]);
}
}
},_onItemClick:function(evt,_6d,_6e,_6f){
if(evt){
var _70=this._getPaneForItem(_6e,_6d,_6f);
var _71=(evt.type=="click"&&evt.alreadySelected);
if(_71&&_70){
this._removeAfter(_6d.getIndexInParent()+1);
var _72=_6d.getNextSibling();
if(_72&&_72._setSelected){
_72._setSelected(null);
}
this.scrollIntoView(_72);
}else{
if(_70){
this.addChild(_70,_6d.getIndexInParent()+1);
if(this._savedFocus){
_70.focus(true);
}
}else{
this._removeAfter(_6d);
this.scrollIntoView(_6d);
}
}
}else{
if(_6d){
this._removeAfter(_6d);
this.scrollIntoView(_6d);
}
}
if(!evt||evt.type!="internal"){
this._setValue(_6e);
this.onItemClick(_6e,_6d,_6f);
}
this._visibleItem=_6e;
},_getPaneForItem:function(_73,_74,_75){
var ret=this.getPaneForItem(_73,_74,_75);
ret.store=this.store;
ret.parentWidget=this;
ret.parentPane=_74||null;
if(!_73){
ret.query=this.query;
ret.queryOptions=this.queryOptions;
}else{
if(_75){
ret.items=_75;
}else{
ret.items=[_73];
}
}
return ret;
},_getMenuItemForItem:function(_77,_78){
var _79=this.store;
if(!_77||!_79&&!_79.isItem(_77)){
var i=new dijit.MenuItem({label:dojo.i18n.getLocalization("dojox.widget","RollingList",this.lang).empty,disabled:true,iconClass:"dojoxEmpty",focus:function(){
}});
this._updateClass(i.domNode,"Item");
return i;
}else{
var _7b=this.getChildItems(_77);
var _7c;
if(_7b){
_7c=this.getMenuItemForItem(_77,_78,_7b);
_7c.children=_7b;
this._updateClass(_7c.domNode,"Item",{"Expanding":true});
if(!_7c._started){
var c=_7c.connect(_7c,"startup",function(){
this.disconnect(c);
dojo.style(this.arrowWrapper,"display","");
});
}else{
dojo.style(_7c.arrowWrapper,"display","");
}
}else{
_7c=this.getMenuItemForItem(_77,_78,null);
this._updateClass(_7c.domNode,"Item",{"Single":true});
}
_7c.store=this.store;
_7c.item=_77;
if(!_7c.label){
_7c.attr("label",this.store.getLabel(_77));
}
if(_7c.focusNode){
var _7e=this;
_7c.focus=function(){
if(!this.disabled){
try{
this.focusNode.focus();
}
catch(e){
}
}
};
_7c.connect(_7c.focusNode,"onmouseenter",function(){
_7e._updateClass(this.domNode,"Item",{"Hover":true});
});
_7c.connect(_7c.focusNode,"onmouseleave",function(){
_7e._updateClass(this.domNode,"Item",{"Hover":false});
});
_7c.connect(_7c.focusNode,"blur",function(){
_7e._updateClass(this.domNode,"Item",{"Focus":false});
});
_7c.connect(_7c.focusNode,"focus",function(){
_7e._updateClass(this.domNode,"Item",{"Focus":true});
_7e._focusedPane=_78;
});
_7c.connect(_7c.focusNode,"ondblclick",function(){
_7e.onExecute();
});
}
return _7c;
}
},_setStore:function(_7f){
if(_7f===this.store&&this._started){
return;
}
this.store=_7f;
this._isIdentity=_7f.getFeatures()["dojo.data.api.Identity"];
var _80=this._getPaneForItem();
this.addChild(_80,0);
},_onKey:function(e){
if(e.charOrCode==dojo.keys.BACKSPACE){
dojo.stopEvent(e);
return;
}else{
if(e.charOrCode==dojo.keys.ESCAPE&&this._savedFocus){
try{
dijit.focus(this._savedFocus);
}
catch(e){
}
dojo.stopEvent(e);
return;
}else{
if(e.charOrCode==dojo.keys.LEFT_ARROW||e.charOrCode==dojo.keys.RIGHT_ARROW){
dojo.stopEvent(e);
return;
}
}
}
},focus:function(){
var _82=this._savedFocus;
this._savedFocus=dijit.getFocus(this);
if(!this._savedFocus.node){
delete this._savedFocus;
}
if(!this._focusedPane){
var _83=this.getChildren()[0];
if(_83&&!_82){
_83.focus(true);
}
}else{
this._savedFocus=dijit.getFocus(this);
var foc=this._focusedPane;
delete this._focusedPane;
if(!_82){
foc.focus(true);
}
}
},handleKey:function(e){
if(e.charOrCode==dojo.keys.DOWN_ARROW){
delete this._savedFocus;
this.focus();
return false;
}else{
if(e.charOrCode==dojo.keys.ESCAPE){
this.onCancel();
return false;
}
}
return true;
},startup:function(){
if(this._started){
return;
}
if(!this.getParent||!this.getParent()){
this.resize();
this.connect(dojo.global,"onresize","resize");
}
this._setStore(this.store);
this.inherited(arguments);
},getChildItems:function(_86){
var _87,_88=this.store;
dojo.forEach(this.childrenAttrs,function(_89){
var _8a=_88.getValues(_86,_89);
if(_8a&&_8a.length){
_87=(_87||[]).concat(_8a);
}
});
return _87;
},getMenuItemForItem:function(_8b,_8c,_8d){
return new dijit.MenuItem({});
},getPaneForItem:function(_8e,_8f,_90){
if(!_8e||_90){
return new dojox.widget._RollingListGroupPane({});
}else{
return null;
}
},onItemClick:function(_91,_92,_93){
},onExecute:function(){
},onCancel:function(){
},onChange:function(_94){
}});
}
