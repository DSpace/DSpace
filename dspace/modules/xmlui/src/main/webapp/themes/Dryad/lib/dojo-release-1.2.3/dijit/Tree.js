/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.Tree"]){
dojo._hasResource["dijit.Tree"]=true;
dojo.provide("dijit.Tree");
dojo.require("dojo.fx");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dijit._Container");
dojo.require("dojo.cookie");
dojo.declare("dijit._TreeNode",[dijit._Widget,dijit._Templated,dijit._Container,dijit._Contained],{item:null,isTreeNode:true,label:"",isExpandable:null,isExpanded:false,state:"UNCHECKED",templateString:"<div class=\"dijitTreeNode\" waiRole=\"presentation\"\n\t><div dojoAttachPoint=\"rowNode\" class=\"dijitTreeRow\" waiRole=\"presentation\"\n\t\t><img src=\"${_blankGif}\" alt=\"\" dojoAttachPoint=\"expandoNode\" class=\"dijitTreeExpando\" waiRole=\"presentation\"\n\t\t><span dojoAttachPoint=\"expandoNodeText\" class=\"dijitExpandoText\" waiRole=\"presentation\"\n\t\t></span\n\t\t><span dojoAttachPoint=\"contentNode\" dojoAttachEvent=\"onmouseenter:_onMouseEnter, onmouseleave:_onMouseLeave\"\n\t\t\tclass=\"dijitTreeContent\" waiRole=\"presentation\">\n\t\t\t<img src=\"${_blankGif}\" alt=\"\" dojoAttachPoint=\"iconNode\" class=\"dijitTreeIcon\" waiRole=\"presentation\"\n\t\t\t><span dojoAttachPoint=\"labelNode\" class=\"dijitTreeLabel\" wairole=\"treeitem\" tabindex=\"-1\" waiState=\"selected-false\" dojoAttachEvent=\"onfocus:_onNodeFocus\"></span>\n\t\t</span\n\t></div>\n\t<div dojoAttachPoint=\"containerNode\" class=\"dijitTreeContainer\" waiRole=\"presentation\" style=\"display: none;\"></div>\n</div>\n",postCreate:function(){
this.setLabelNode(this.label);
this._setExpando();
this._updateItemClasses(this.item);
if(this.isExpandable){
dijit.setWaiState(this.labelNode,"expanded",this.isExpanded);
}
},markProcessing:function(){
this.state="LOADING";
this._setExpando(true);
},unmarkProcessing:function(){
this._setExpando(false);
},_updateItemClasses:function(_1){
var _2=this.tree,_3=_2.model;
if(_2._v10Compat&&_1===_3.root){
_1=null;
}
this.iconNode.className="dijitTreeIcon "+_2.getIconClass(_1,this.isExpanded);
this.labelNode.className="dijitTreeLabel "+_2.getLabelClass(_1,this.isExpanded);
},_updateLayout:function(){
var _4=this.getParent();
if(!_4||_4.rowNode.style.display=="none"){
dojo.addClass(this.domNode,"dijitTreeIsRoot");
}else{
dojo.toggleClass(this.domNode,"dijitTreeIsLast",!this.getNextSibling());
}
},_setExpando:function(_5){
var _6=["dijitTreeExpandoLoading","dijitTreeExpandoOpened","dijitTreeExpandoClosed","dijitTreeExpandoLeaf"];
var _7=["*","-","+","*"];
var _8=_5?0:(this.isExpandable?(this.isExpanded?1:2):3);
dojo.forEach(_6,function(s){
dojo.removeClass(this.expandoNode,s);
},this);
dojo.addClass(this.expandoNode,_6[_8]);
this.expandoNodeText.innerHTML=_7[_8];
},expand:function(){
if(this.isExpanded){
return;
}
this._wipeOut&&this._wipeOut.stop();
this.isExpanded=true;
dijit.setWaiState(this.labelNode,"expanded","true");
dijit.setWaiRole(this.containerNode,"group");
this.contentNode.className="dijitTreeContent dijitTreeContentExpanded";
this._setExpando();
this._updateItemClasses(this.item);
if(!this._wipeIn){
this._wipeIn=dojo.fx.wipeIn({node:this.containerNode,duration:dijit.defaultDuration});
}
this._wipeIn.play();
},collapse:function(){
if(!this.isExpanded){
return;
}
this._wipeIn&&this._wipeIn.stop();
this.isExpanded=false;
dijit.setWaiState(this.labelNode,"expanded","false");
this.contentNode.className="dijitTreeContent";
this._setExpando();
this._updateItemClasses(this.item);
if(!this._wipeOut){
this._wipeOut=dojo.fx.wipeOut({node:this.containerNode,duration:dijit.defaultDuration});
}
this._wipeOut.play();
},setLabelNode:function(_a){
this.labelNode.innerHTML="";
this.labelNode.appendChild(dojo.doc.createTextNode(_a));
},setChildItems:function(_b){
var _c=this.tree,_d=_c.model;
this.getChildren().forEach(function(_e){
dijit._Container.prototype.removeChild.call(this,_e);
},this);
this.state="LOADED";
if(_b&&_b.length>0){
this.isExpandable=true;
dojo.forEach(_b,function(_f){
var id=_d.getIdentity(_f),_11=_c._itemNodeMap[id],_12=(_11&&!_11.getParent())?_11:this.tree._createTreeNode({item:_f,tree:_c,isExpandable:_d.mayHaveChildren(_f),label:_c.getLabel(_f)});
this.addChild(_12);
_c._itemNodeMap[id]=_12;
if(this.tree.persist){
if(_c._openedItemIds[id]){
_c._expandNode(_12);
}
}
},this);
dojo.forEach(this.getChildren(),function(_13,idx){
_13._updateLayout();
});
}else{
this.isExpandable=false;
}
if(this._setExpando){
this._setExpando(false);
}
if(this==_c.rootNode){
var fc=this.tree.showRoot?this:this.getChildren()[0],_16=fc?fc.labelNode:this.domNode;
_16.setAttribute("tabIndex","0");
_c.lastFocused=fc;
}
},removeChild:function(_17){
this.inherited(arguments);
var _18=this.getChildren();
if(_18.length==0){
this.isExpandable=false;
this.collapse();
}
dojo.forEach(_18,function(_19){
_19._updateLayout();
});
},makeExpandable:function(){
this.isExpandable=true;
this._setExpando(false);
},_onNodeFocus:function(evt){
var _1b=dijit.getEnclosingWidget(evt.target);
this.tree._onTreeFocus(_1b);
},_onMouseEnter:function(evt){
dojo.addClass(this.contentNode,"dijitTreeNodeHover");
},_onMouseLeave:function(evt){
dojo.removeClass(this.contentNode,"dijitTreeNodeHover");
}});
dojo.declare("dijit.Tree",[dijit._Widget,dijit._Templated],{store:null,model:null,query:null,label:"",showRoot:true,childrenAttr:["children"],openOnClick:false,templateString:"<div class=\"dijitTreeContainer\" waiRole=\"tree\"\n\tdojoAttachEvent=\"onclick:_onClick,onkeypress:_onKeyPress\">\n</div>\n",isExpandable:true,isTree:true,persist:true,dndController:null,dndParams:["onDndDrop","itemCreator","onDndCancel","checkAcceptance","checkItemAcceptance","dragThreshold"],onDndDrop:null,itemCreator:null,onDndCancel:null,checkAcceptance:null,checkItemAcceptance:null,dragThreshold:0,_publish:function(_1e,_1f){
dojo.publish(this.id,[dojo.mixin({tree:this,event:_1e},_1f||{})]);
},postMixInProperties:function(){
this.tree=this;
this._itemNodeMap={};
if(!this.cookieName){
this.cookieName=this.id+"SaveStateCookie";
}
},postCreate:function(){
if(this.persist){
var _20=dojo.cookie(this.cookieName);
this._openedItemIds={};
if(_20){
dojo.forEach(_20.split(","),function(_21){
this._openedItemIds[_21]=true;
},this);
}
}
if(!this.model){
this._store2model();
}
this.connect(this.model,"onChange","_onItemChange");
this.connect(this.model,"onChildrenChange","_onItemChildrenChange");
this.connect(this.model,"onDelete","_onItemDelete");
this._load();
this.inherited(arguments);
if(this.dndController){
if(dojo.isString(this.dndController)){
this.dndController=dojo.getObject(this.dndController);
}
var _22={};
for(var i=0;i<this.dndParams.length;i++){
if(this[this.dndParams[i]]){
_22[this.dndParams[i]]=this[this.dndParams[i]];
}
}
this.dndController=new this.dndController(this,_22);
}
},_store2model:function(){
this._v10Compat=true;
dojo.deprecated("Tree: from version 2.0, should specify a model object rather than a store/query");
var _24={id:this.id+"_ForestStoreModel",store:this.store,query:this.query,childrenAttrs:this.childrenAttr};
if(this.params.mayHaveChildren){
_24.mayHaveChildren=dojo.hitch(this,"mayHaveChildren");
}
if(this.params.getItemChildren){
_24.getChildren=dojo.hitch(this,function(_25,_26,_27){
this.getItemChildren((this._v10Compat&&_25===this.model.root)?null:_25,_26,_27);
});
}
this.model=new dijit.tree.ForestStoreModel(_24);
this.showRoot=Boolean(this.label);
},_load:function(){
this.model.getRoot(dojo.hitch(this,function(_28){
var rn=this.rootNode=this.tree._createTreeNode({item:_28,tree:this,isExpandable:true,label:this.label||this.getLabel(_28)});
if(!this.showRoot){
rn.rowNode.style.display="none";
}
this.domNode.appendChild(rn.domNode);
this._itemNodeMap[this.model.getIdentity(_28)]=rn;
rn._updateLayout();
this._expandNode(rn);
}),function(err){
console.error(this,": error loading root: ",err);
});
},mayHaveChildren:function(_2b){
},getItemChildren:function(_2c,_2d){
},getLabel:function(_2e){
return this.model.getLabel(_2e);
},getIconClass:function(_2f,_30){
return (!_2f||this.model.mayHaveChildren(_2f))?(_30?"dijitFolderOpened":"dijitFolderClosed"):"dijitLeaf";
},getLabelClass:function(_31,_32){
},_onKeyPress:function(e){
if(e.altKey){
return;
}
var dk=dojo.keys;
var _35=dijit.getEnclosingWidget(e.target);
if(!_35){
return;
}
var key=e.charOrCode;
if(typeof key=="string"){
if(!e.altKey&&!e.ctrlKey&&!e.shiftKey&&!e.metaKey){
this._onLetterKeyNav({node:_35,key:key.toLowerCase()});
dojo.stopEvent(e);
}
}else{
var map=this._keyHandlerMap;
if(!map){
map={};
map[dk.ENTER]="_onEnterKey";
map[this.isLeftToRight()?dk.LEFT_ARROW:dk.RIGHT_ARROW]="_onLeftArrow";
map[this.isLeftToRight()?dk.RIGHT_ARROW:dk.LEFT_ARROW]="_onRightArrow";
map[dk.UP_ARROW]="_onUpArrow";
map[dk.DOWN_ARROW]="_onDownArrow";
map[dk.HOME]="_onHomeKey";
map[dk.END]="_onEndKey";
this._keyHandlerMap=map;
}
if(this._keyHandlerMap[key]){
this[this._keyHandlerMap[key]]({node:_35,item:_35.item});
dojo.stopEvent(e);
}
}
},_onEnterKey:function(_38){
this._publish("execute",{item:_38.item,node:_38.node});
this.onClick(_38.item,_38.node);
},_onDownArrow:function(_39){
var _3a=this._getNextNode(_39.node);
if(_3a&&_3a.isTreeNode){
this.focusNode(_3a);
}
},_onUpArrow:function(_3b){
var _3c=_3b.node;
var _3d=_3c.getPreviousSibling();
if(_3d){
_3c=_3d;
while(_3c.isExpandable&&_3c.isExpanded&&_3c.hasChildren()){
var _3e=_3c.getChildren();
_3c=_3e[_3e.length-1];
}
}else{
var _3f=_3c.getParent();
if(!(!this.showRoot&&_3f===this.rootNode)){
_3c=_3f;
}
}
if(_3c&&_3c.isTreeNode){
this.focusNode(_3c);
}
},_onRightArrow:function(_40){
var _41=_40.node;
if(_41.isExpandable&&!_41.isExpanded){
this._expandNode(_41);
}else{
if(_41.hasChildren()){
_41=_41.getChildren()[0];
if(_41&&_41.isTreeNode){
this.focusNode(_41);
}
}
}
},_onLeftArrow:function(_42){
var _43=_42.node;
if(_43.isExpandable&&_43.isExpanded){
this._collapseNode(_43);
}else{
var _44=_43.getParent();
if(_44&&_44.isTreeNode&&!(!this.showRoot&&_44===this.rootNode)){
this.focusNode(_44);
}
}
},_onHomeKey:function(){
var _45=this._getRootOrFirstNode();
if(_45){
this.focusNode(_45);
}
},_onEndKey:function(_46){
var _47=this;
while(_47.isExpanded){
var c=_47.getChildren();
_47=c[c.length-1];
}
if(_47&&_47.isTreeNode){
this.focusNode(_47);
}
},_onLetterKeyNav:function(_49){
var _4a=_49.node,_4b=_4a,key=_49.key;
do{
_4a=this._getNextNode(_4a);
if(!_4a){
_4a=this._getRootOrFirstNode();
}
}while(_4a!==_4b&&(_4a.label.charAt(0).toLowerCase()!=key));
if(_4a&&_4a.isTreeNode){
if(_4a!==_4b){
this.focusNode(_4a);
}
}
},_onClick:function(e){
var _4e=e.target;
var _4f=dijit.getEnclosingWidget(_4e);
if(!_4f||!_4f.isTreeNode){
return;
}
if((this.openOnClick&&_4f.isExpandable)||(_4e==_4f.expandoNode||_4e==_4f.expandoNodeText)){
if(_4f.isExpandable){
this._onExpandoClick({node:_4f});
}
}else{
this._publish("execute",{item:_4f.item,node:_4f});
this.onClick(_4f.item,_4f);
this.focusNode(_4f);
}
dojo.stopEvent(e);
},_onExpandoClick:function(_50){
var _51=_50.node;
this.focusNode(_51);
if(_51.isExpanded){
this._collapseNode(_51);
}else{
this._expandNode(_51);
}
},onClick:function(_52,_53){
},onOpen:function(_54,_55){
},onClose:function(_56,_57){
},_getNextNode:function(_58){
if(_58.isExpandable&&_58.isExpanded&&_58.hasChildren()){
return _58.getChildren()[0];
}else{
while(_58&&_58.isTreeNode){
var _59=_58.getNextSibling();
if(_59){
return _59;
}
_58=_58.getParent();
}
return null;
}
},_getRootOrFirstNode:function(){
return this.showRoot?this.rootNode:this.rootNode.getChildren()[0];
},_collapseNode:function(_5a){
if(_5a.isExpandable){
if(_5a.state=="LOADING"){
return;
}
_5a.collapse();
this.onClose(_5a.item,_5a);
if(this.persist&&_5a.item){
delete this._openedItemIds[this.model.getIdentity(_5a.item)];
this._saveState();
}
}
},_expandNode:function(_5b){
if(!_5b.isExpandable){
return;
}
var _5c=this.model,_5d=_5b.item;
switch(_5b.state){
case "LOADING":
return;
case "UNCHECKED":
_5b.markProcessing();
var _5e=this;
_5c.getChildren(_5d,function(_5f){
_5b.unmarkProcessing();
_5b.setChildItems(_5f);
_5e._expandNode(_5b);
},function(err){
console.error(_5e,": error loading root children: ",err);
});
break;
default:
_5b.expand();
this.onOpen(_5b.item,_5b);
if(this.persist&&_5d){
this._openedItemIds[_5c.getIdentity(_5d)]=true;
this._saveState();
}
}
},blurNode:function(){
var _61=this.lastFocused;
if(!_61){
return;
}
var _62=_61.labelNode;
dojo.removeClass(_62,"dijitTreeLabelFocused");
_62.setAttribute("tabIndex","-1");
dijit.setWaiState(_62,"selected",false);
this.lastFocused=null;
},focusNode:function(_63){
_63.labelNode.focus();
},_onBlur:function(){
this.inherited(arguments);
if(this.lastFocused){
var _64=this.lastFocused.labelNode;
dojo.removeClass(_64,"dijitTreeLabelFocused");
}
},_onTreeFocus:function(_65){
if(_65){
if(_65!=this.lastFocused){
this.blurNode();
}
var _66=_65.labelNode;
_66.setAttribute("tabIndex","0");
dijit.setWaiState(_66,"selected",true);
dojo.addClass(_66,"dijitTreeLabelFocused");
this.lastFocused=_65;
}
},_onItemDelete:function(_67){
var _68=this.model.getIdentity(_67);
var _69=this._itemNodeMap[_68];
if(_69){
var _6a=_69.getParent();
if(_6a){
_6a.removeChild(_69);
}
delete this._itemNodeMap[_68];
_69.destroyRecursive();
}
},_onItemChange:function(_6b){
var _6c=this.model,_6d=_6c.getIdentity(_6b),_6e=this._itemNodeMap[_6d];
if(_6e){
_6e.setLabelNode(this.getLabel(_6b));
_6e._updateItemClasses(_6b);
}
},_onItemChildrenChange:function(_6f,_70){
var _71=this.model,_72=_71.getIdentity(_6f),_73=this._itemNodeMap[_72];
if(_73){
_73.setChildItems(_70);
}
},_onItemDelete:function(_74){
var _75=this.model,_76=_75.getIdentity(_74),_77=this._itemNodeMap[_76];
if(_77){
_77.destroyRecursive();
delete this._itemNodeMap[_76];
}
},_saveState:function(){
if(!this.persist){
return;
}
var ary=[];
for(var id in this._openedItemIds){
ary.push(id);
}
dojo.cookie(this.cookieName,ary.join(","),{expires:365});
},destroy:function(){
if(this.rootNode){
this.rootNode.destroyRecursive();
}
if(this.dndController&&!dojo.isString(this.dndController)){
this.dndController.destroy();
}
this.rootNode=null;
this.inherited(arguments);
},destroyRecursive:function(){
this.destroy();
},_createTreeNode:function(_7a){
return new dijit._TreeNode(_7a);
}});
dojo.declare("dijit.tree.TreeStoreModel",null,{store:null,childrenAttrs:["children"],labelAttr:"",root:null,query:null,constructor:function(_7b){
dojo.mixin(this,_7b);
this.connects=[];
var _7c=this.store;
if(!_7c.getFeatures()["dojo.data.api.Identity"]){
throw new Error("dijit.Tree: store must support dojo.data.Identity");
}
if(_7c.getFeatures()["dojo.data.api.Notification"]){
this.connects=this.connects.concat([dojo.connect(_7c,"onNew",this,"_onNewItem"),dojo.connect(_7c,"onDelete",this,"_onDeleteItem"),dojo.connect(_7c,"onSet",this,"_onSetItem")]);
}
},destroy:function(){
dojo.forEach(this.connects,dojo.disconnect);
},getRoot:function(_7d,_7e){
if(this.root){
_7d(this.root);
}else{
this.store.fetch({query:this.query,onComplete:dojo.hitch(this,function(_7f){
if(_7f.length!=1){
throw new Error(this.declaredClass+": query "+dojo.toJson(this.query)+" returned "+_7f.length+" items, but must return exactly one item");
}
this.root=_7f[0];
_7d(this.root);
}),onError:_7e});
}
},mayHaveChildren:function(_80){
return dojo.some(this.childrenAttrs,function(_81){
return this.store.hasAttribute(_80,_81);
},this);
},getChildren:function(_82,_83,_84){
var _85=this.store;
var _86=[];
for(var i=0;i<this.childrenAttrs.length;i++){
var _88=_85.getValues(_82,this.childrenAttrs[i]);
_86=_86.concat(_88);
}
var _89=0;
dojo.forEach(_86,function(_8a){
if(!_85.isItemLoaded(_8a)){
_89++;
}
});
if(_89==0){
_83(_86);
}else{
var _8b=function _8b(_8c){
if(--_89==0){
_83(_86);
}
};
dojo.forEach(_86,function(_8d){
if(!_85.isItemLoaded(_8d)){
_85.loadItem({item:_8d,onItem:_8b,onError:_84});
}
});
}
},getIdentity:function(_8e){
return this.store.getIdentity(_8e);
},getLabel:function(_8f){
if(this.labelAttr){
return this.store.getValue(_8f,this.labelAttr);
}else{
return this.store.getLabel(_8f);
}
},newItem:function(_90,_91){
var _92={parent:_91,attribute:this.childrenAttrs[0]};
return this.store.newItem(_90,_92);
},pasteItem:function(_93,_94,_95,_96){
var _97=this.store,_98=this.childrenAttrs[0];
if(_94){
dojo.forEach(this.childrenAttrs,function(_99){
if(_97.containsValue(_94,_99,_93)){
if(!_96){
var _9a=dojo.filter(_97.getValues(_94,_99),function(x){
return x!=_93;
});
_97.setValues(_94,_99,_9a);
}
_98=_99;
}
});
}
if(_95){
_97.setValues(_95,_98,_97.getValues(_95,_98).concat(_93));
}
},onChange:function(_9c){
},onChildrenChange:function(_9d,_9e){
},onDelete:function(_9f,_a0){
},_onNewItem:function(_a1,_a2){
if(!_a2){
return;
}
this.getChildren(_a2.item,dojo.hitch(this,function(_a3){
this.onChildrenChange(_a2.item,_a3);
}));
},_onDeleteItem:function(_a4){
this.onDelete(_a4);
},_onSetItem:function(_a5,_a6,_a7,_a8){
if(dojo.indexOf(this.childrenAttrs,_a6)!=-1){
this.getChildren(_a5,dojo.hitch(this,function(_a9){
this.onChildrenChange(_a5,_a9);
}));
}else{
this.onChange(_a5);
}
}});
dojo.declare("dijit.tree.ForestStoreModel",dijit.tree.TreeStoreModel,{rootId:"$root$",rootLabel:"ROOT",query:null,constructor:function(_aa){
this.root={store:this,root:true,id:_aa.rootId,label:_aa.rootLabel,children:_aa.rootChildren};
},mayHaveChildren:function(_ab){
return _ab===this.root||this.inherited(arguments);
},getChildren:function(_ac,_ad,_ae){
if(_ac===this.root){
if(this.root.children){
_ad(this.root.children);
}else{
this.store.fetch({query:this.query,onComplete:dojo.hitch(this,function(_af){
this.root.children=_af;
_ad(_af);
}),onError:_ae});
}
}else{
this.inherited(arguments);
}
},getIdentity:function(_b0){
return (_b0===this.root)?this.root.id:this.inherited(arguments);
},getLabel:function(_b1){
return (_b1===this.root)?this.root.label:this.inherited(arguments);
},newItem:function(_b2,_b3){
if(_b3===this.root){
this.onNewRootItem(_b2);
return this.store.newItem(_b2);
}else{
return this.inherited(arguments);
}
},onNewRootItem:function(_b4){
},pasteItem:function(_b5,_b6,_b7,_b8){
if(_b6===this.root){
if(!_b8){
this.onLeaveRoot(_b5);
}
}
dijit.tree.TreeStoreModel.prototype.pasteItem.call(this,_b5,_b6===this.root?null:_b6,_b7===this.root?null:_b7);
if(_b7===this.root){
this.onAddToRoot(_b5);
}
},onAddToRoot:function(_b9){

},onLeaveRoot:function(_ba){

},_requeryTop:function(){
var _bb=this.root.children||[];
this.store.fetch({query:this.query,onComplete:dojo.hitch(this,function(_bc){
this.root.children=_bc;
if(_bb.length!=_bc.length||dojo.some(_bb,function(_bd,idx){
return _bc[idx]!=_bd;
})){
this.onChildrenChange(this.root,_bc);
}
})});
},_onNewItem:function(_bf,_c0){
this._requeryTop();
this.inherited(arguments);
},_onDeleteItem:function(_c1){
if(dojo.indexOf(this.root.children,_c1)!=-1){
this._requeryTop();
}
this.inherited(arguments);
}});
}
