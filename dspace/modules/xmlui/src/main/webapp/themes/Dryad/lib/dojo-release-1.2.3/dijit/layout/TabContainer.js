/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.layout.TabContainer"]){
dojo._hasResource["dijit.layout.TabContainer"]=true;
dojo.provide("dijit.layout.TabContainer");
dojo.require("dijit.layout.StackContainer");
dojo.require("dijit._Templated");
dojo.requireLocalization("dijit","common",null,"zh,ca,pt,da,tr,ru,de,sv,ja,he,fi,nb,el,ar,ROOT,pt-pt,cs,fr,es,ko,nl,zh-tw,pl,th,it,hu,sk,sl");
dojo.declare("dijit.layout.TabContainer",[dijit.layout.StackContainer,dijit._Templated],{tabPosition:"top",baseClass:"dijitTabContainer",tabStrip:false,templateString:null,templateString:"<div class=\"dijitTabContainer\">\n\t<div dojoAttachPoint=\"tablistNode\"></div>\n\t<div dojoAttachPoint=\"tablistSpacer\" class=\"dijitTabSpacer ${baseClass}-spacer\"></div>\n\t<div class=\"dijitTabPaneWrapper ${baseClass}-container\" dojoAttachPoint=\"containerNode\"></div>\n</div>\n",_controllerWidget:"dijit.layout.TabController",postMixInProperties:function(){
this.baseClass+=this.tabPosition.charAt(0).toUpperCase()+this.tabPosition.substr(1).replace(/-.*/,"");
this.inherited(arguments);
},postCreate:function(){
this.inherited(arguments);
var _1=dojo.getObject(this._controllerWidget);
this.tablist=new _1({id:this.id+"_tablist",tabPosition:this.tabPosition,doLayout:this.doLayout,containerId:this.id,"class":this.baseClass+"-tabs"+(this.doLayout?"":" dijitTabNoLayout")},this.tablistNode);
if(this.tabStrip){
dojo.addClass(this.tablist.domNode,this.baseClass+"Strip");
}
},_setupChild:function(_2){
dojo.addClass(_2.domNode,"dijitTabPane");
this.inherited(arguments);
return _2;
},startup:function(){
if(this._started){
return;
}
this.tablist.startup();
this.inherited(arguments);
},layout:function(){
if(!this.doLayout){
return;
}
var _3=this.tabPosition.replace(/-h/,"");
var _4=[{domNode:this.tablist.domNode,layoutAlign:_3},{domNode:this.tablistSpacer,layoutAlign:_3},{domNode:this.containerNode,layoutAlign:"client"}];
dijit.layout.layoutChildren(this.domNode,this._contentBox,_4);
this._containerContentBox=dijit.layout.marginBox2contentBox(this.containerNode,_4[2]);
if(this.selectedChildWidget){
this._showChild(this.selectedChildWidget);
if(this.doLayout&&this.selectedChildWidget.resize){
this.selectedChildWidget.resize(this._containerContentBox);
}
}
},destroy:function(){
if(this.tablist){
this.tablist.destroy();
}
this.inherited(arguments);
}});
dojo.declare("dijit.layout.TabController",dijit.layout.StackController,{templateString:"<div wairole='tablist' dojoAttachEvent='onkeypress:onkeypress'></div>",tabPosition:"top",doLayout:true,buttonWidget:"dijit.layout._TabButton",_rectifyRtlTabList:function(){
if(0>=this.tabPosition.indexOf("-h")){
return;
}
if(!this.pane2button){
return;
}
var _5=0;
for(var _6 in this.pane2button){
var ow=this.pane2button[_6].innerDiv.scrollWidth;
_5=Math.max(_5,ow);
}
for(_6 in this.pane2button){
this.pane2button[_6].innerDiv.style.width=_5+"px";
}
}});
dojo.declare("dijit.layout._TabButton",dijit.layout._StackButton,{baseClass:"dijitTab",templateString:"<div waiRole=\"presentation\" dojoAttachEvent='onclick:onClick,onmouseenter:_onMouse,onmouseleave:_onMouse'>\n    <div waiRole=\"presentation\" class='dijitTabInnerDiv' dojoAttachPoint='innerDiv'>\n        <div waiRole=\"presentation\" class='dijitTabContent' dojoAttachPoint='tabContent'>\n\t        <span dojoAttachPoint='containerNode,focusNode' class='tabLabel'>${!label}</span><img class =\"dijitTabButtonSpacer\" src=\"${_blankGif}\" />\n\t        <div class=\"dijitInline closeNode\" dojoAttachPoint='closeNode' dojoAttachEvent='onclick:onClickCloseButton'>\n\t        \t<img src=\"${_blankGif}\" alt=\"\" dojoAttachPoint='closeButtonNode' class='closeImage' dojoAttachEvent='onmouseenter:_onMouse, onmouseleave:_onMouse' stateModifier='CloseButton' waiRole=\"presentation\"/>\n\t            <span dojoAttachPoint='closeText' class='closeText'>x</span>\n\t        </div>\n        </div>\n    </div>\n</div>\n",_scroll:false,postCreate:function(){
if(this.closeButton){
dojo.addClass(this.innerDiv,"dijitClosable");
var _8=dojo.i18n.getLocalization("dijit","common");
if(this.closeNode){
dojo.attr(this.closeNode,"title",_8.itemClose);
dojo.attr(this.closeButtonNode,"title",_8.itemClose);
}
}else{
this.closeNode.style.display="none";
}
this.inherited(arguments);
dojo.setSelectable(this.containerNode,false);
}});
}
