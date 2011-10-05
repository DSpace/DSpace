/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.layout.StackContainer"]){
dojo._hasResource["dijit.layout.StackContainer"]=true;
dojo.provide("dijit.layout.StackContainer");
dojo.require("dijit._Templated");
dojo.require("dijit.layout._LayoutWidget");
dojo.require("dijit.form.Button");
dojo.require("dijit.Menu");
dojo.requireLocalization("dijit","common",null,"zh,ca,pt,da,tr,ru,de,sv,ja,he,fi,nb,el,ar,ROOT,pt-pt,cs,fr,es,ko,nl,zh-tw,pl,th,it,hu,sk,sl");
dojo.declare("dijit.layout.StackContainer",dijit.layout._LayoutWidget,{doLayout:true,baseClass:"dijitStackContainer",_started:false,postCreate:function(){
this.inherited(arguments);
dijit.setWaiRole(this.containerNode,"tabpanel");
this.connect(this.domNode,"onkeypress",this._onKeyPress);
},startup:function(){
if(this._started){
return;
}
var _1=this.getChildren();
dojo.forEach(_1,this._setupChild,this);
dojo.some(_1,function(_2){
if(_2.selected){
this.selectedChildWidget=_2;
}
return _2.selected;
},this);
var _3=this.selectedChildWidget;
if(!_3&&_1[0]){
_3=this.selectedChildWidget=_1[0];
_3.selected=true;
}
if(_3){
this._showChild(_3);
}
dojo.publish(this.id+"-startup",[{children:_1,selected:_3}]);
this.inherited(arguments);
},_setupChild:function(_4){
this.inherited(arguments);
_4.domNode.style.display="none";
_4.domNode.style.position="relative";
_4.domNode.title="";
return _4;
},addChild:function(_5,_6){
this.inherited(arguments);
if(this._started){
dojo.publish(this.id+"-addChild",[_5,_6]);
this.layout();
if(!this.selectedChildWidget){
this.selectChild(_5);
}
}
},removeChild:function(_7){
this.inherited(arguments);
if(this._beingDestroyed){
return;
}
if(this._started){
dojo.publish(this.id+"-removeChild",[_7]);
this.layout();
}
if(this.selectedChildWidget===_7){
this.selectedChildWidget=undefined;
if(this._started){
var _8=this.getChildren();
if(_8.length){
this.selectChild(_8[0]);
}
}
}
},selectChild:function(_9){
_9=dijit.byId(_9);
if(this.selectedChildWidget!=_9){
this._transition(_9,this.selectedChildWidget);
this.selectedChildWidget=_9;
dojo.publish(this.id+"-selectChild",[_9]);
}
},_transition:function(_a,_b){
if(_b){
this._hideChild(_b);
}
this._showChild(_a);
if(this.doLayout&&_a.resize){
_a.resize(this._containerContentBox||this._contentBox);
}
},_adjacent:function(_c){
var _d=this.getChildren();
var _e=dojo.indexOf(_d,this.selectedChildWidget);
_e+=_c?1:_d.length-1;
return _d[_e%_d.length];
},forward:function(){
this.selectChild(this._adjacent(true));
},back:function(){
this.selectChild(this._adjacent(false));
},_onKeyPress:function(e){
dojo.publish(this.id+"-containerKeyPress",[{e:e,page:this}]);
},layout:function(){
if(this.doLayout&&this.selectedChildWidget&&this.selectedChildWidget.resize){
this.selectedChildWidget.resize(this._contentBox);
}
},_showChild:function(_10){
var _11=this.getChildren();
_10.isFirstChild=(_10==_11[0]);
_10.isLastChild=(_10==_11[_11.length-1]);
_10.selected=true;
_10.domNode.style.display="";
if(_10._loadCheck){
_10._loadCheck();
}
if(_10.onShow){
_10.onShow();
}
},_hideChild:function(_12){
_12.selected=false;
_12.domNode.style.display="none";
if(_12.onHide){
_12.onHide();
}
},closeChild:function(_13){
var _14=_13.onClose(this,_13);
if(_14){
this.removeChild(_13);
_13.destroyRecursive();
}
},destroy:function(){
this._beingDestroyed=true;
this.inherited(arguments);
}});
dojo.declare("dijit.layout.StackController",[dijit._Widget,dijit._Templated,dijit._Container],{templateString:"<span wairole='tablist' dojoAttachEvent='onkeypress' class='dijitStackController'></span>",containerId:"",buttonWidget:"dijit.layout._StackButton",postCreate:function(){
dijit.setWaiRole(this.domNode,"tablist");
this.pane2button={};
this.pane2handles={};
this.pane2menu={};
this._subscriptions=[dojo.subscribe(this.containerId+"-startup",this,"onStartup"),dojo.subscribe(this.containerId+"-addChild",this,"onAddChild"),dojo.subscribe(this.containerId+"-removeChild",this,"onRemoveChild"),dojo.subscribe(this.containerId+"-selectChild",this,"onSelectChild"),dojo.subscribe(this.containerId+"-containerKeyPress",this,"onContainerKeyPress")];
},onStartup:function(_15){
dojo.forEach(_15.children,this.onAddChild,this);
this.onSelectChild(_15.selected);
},destroy:function(){
for(var _16 in this.pane2button){
this.onRemoveChild(_16);
}
dojo.forEach(this._subscriptions,dojo.unsubscribe);
this.inherited(arguments);
},onAddChild:function(_17,_18){
var _19=dojo.doc.createElement("span");
this.domNode.appendChild(_19);
var cls=dojo.getObject(this.buttonWidget);
var _1b=new cls({label:_17.title,closeButton:_17.closable},_19);
this.addChild(_1b,_18);
this.pane2button[_17]=_1b;
_17.controlButton=_1b;
var _1c=[];
_1c.push(dojo.connect(_1b,"onClick",dojo.hitch(this,"onButtonClick",_17)));
if(_17.closable){
_1c.push(dojo.connect(_1b,"onClickCloseButton",dojo.hitch(this,"onCloseButtonClick",_17)));
var _1d=dojo.i18n.getLocalization("dijit","common");
var _1e=new dijit.Menu({targetNodeIds:[_1b.id],id:_1b.id+"_Menu"});
var _1f=new dijit.MenuItem({label:_1d.itemClose});
_1c.push(dojo.connect(_1f,"onClick",dojo.hitch(this,"onCloseButtonClick",_17)));
_1e.addChild(_1f);
this.pane2menu[_17]=_1e;
}
this.pane2handles[_17]=_1c;
if(!this._currentChild){
_1b.focusNode.setAttribute("tabIndex","0");
this._currentChild=_17;
}
if(!this.isLeftToRight()&&dojo.isIE&&this._rectifyRtlTabList){
this._rectifyRtlTabList();
}
},onRemoveChild:function(_20){
if(this._currentChild===_20){
this._currentChild=null;
}
dojo.forEach(this.pane2handles[_20],dojo.disconnect);
delete this.pane2handles[_20];
var _21=this.pane2menu[_20];
if(_21){
_21.destroyRecursive();
delete this.pane2menu[_20];
}
var _22=this.pane2button[_20];
if(_22){
_22.destroy();
delete this.pane2button[_20];
}
},onSelectChild:function(_23){
if(!_23){
return;
}
if(this._currentChild){
var _24=this.pane2button[this._currentChild];
_24.attr("checked",false);
_24.focusNode.setAttribute("tabIndex","-1");
}
var _25=this.pane2button[_23];
_25.attr("checked",true);
this._currentChild=_23;
_25.focusNode.setAttribute("tabIndex","0");
var _26=dijit.byId(this.containerId);
dijit.setWaiState(_26.containerNode,"labelledby",_25.id);
},onButtonClick:function(_27){
var _28=dijit.byId(this.containerId);
_28.selectChild(_27);
},onCloseButtonClick:function(_29){
var _2a=dijit.byId(this.containerId);
_2a.closeChild(_29);
var b=this.pane2button[this._currentChild];
if(b){
dijit.focus(b.focusNode||b.domNode);
}
},adjacent:function(_2c){
if(!this.isLeftToRight()&&(!this.tabPosition||/top|bottom/.test(this.tabPosition))){
_2c=!_2c;
}
var _2d=this.getChildren();
var _2e=dojo.indexOf(_2d,this.pane2button[this._currentChild]);
var _2f=_2c?1:_2d.length-1;
return _2d[(_2e+_2f)%_2d.length];
},onkeypress:function(e){
if(this.disabled||e.altKey){
return;
}
var _31=null;
if(e.ctrlKey||!e._djpage){
var k=dojo.keys;
switch(e.charOrCode){
case k.LEFT_ARROW:
case k.UP_ARROW:
if(!e._djpage){
_31=false;
}
break;
case k.PAGE_UP:
if(e.ctrlKey){
_31=false;
}
break;
case k.RIGHT_ARROW:
case k.DOWN_ARROW:
if(!e._djpage){
_31=true;
}
break;
case k.PAGE_DOWN:
if(e.ctrlKey){
_31=true;
}
break;
case k.DELETE:
if(this._currentChild.closable){
this.onCloseButtonClick(this._currentChild);
}
dojo.stopEvent(e);
break;
default:
if(e.ctrlKey){
if(e.charOrCode===k.TAB){
this.adjacent(!e.shiftKey).onClick();
dojo.stopEvent(e);
}else{
if(e.charOrCode=="w"){
if(this._currentChild.closable){
this.onCloseButtonClick(this._currentChild);
}
dojo.stopEvent(e);
}
}
}
}
if(_31!==null){
this.adjacent(_31).onClick();
dojo.stopEvent(e);
}
}
},onContainerKeyPress:function(_33){
_33.e._djpage=_33.page;
this.onkeypress(_33.e);
}});
dojo.declare("dijit.layout._StackButton",dijit.form.ToggleButton,{tabIndex:"-1",postCreate:function(evt){
dijit.setWaiRole((this.focusNode||this.domNode),"tab");
this.inherited(arguments);
},onClick:function(evt){
dijit.focus(this.focusNode);
},onClickCloseButton:function(evt){
evt.stopPropagation();
}});
dojo.extend(dijit._Widget,{title:"",selected:false,closable:false,onClose:function(){
return true;
}});
}
