/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._Widget"]){
dojo._hasResource["dijit._Widget"]=true;
dojo.provide("dijit._Widget");
dojo.require("dijit._base");
dojo.connect(dojo,"connect",function(_1,_2){
if(_1&&dojo.isFunction(_1._onConnect)){
_1._onConnect(_2);
}
});
dijit._connectOnUseEventHandler=function(_3){
};
(function(){
var _4={};
var _5=function(dc){
if(!_4[dc]){
var r=[];
var _8;
var _9=dojo.getObject(dc).prototype;
for(var _a in _9){
if(dojo.isFunction(_9[_a])&&(_8=_a.match(/^_set([a-zA-Z]*)Attr$/))&&_8[1]){
r.push(_8[1].charAt(0).toLowerCase()+_8[1].substr(1));
}
}
_4[dc]=r;
}
return _4[dc]||[];
};
dojo.declare("dijit._Widget",null,{id:"",lang:"",dir:"","class":"",style:"",title:"",srcNodeRef:null,domNode:null,containerNode:null,attributeMap:{id:"",dir:"",lang:"","class":"",style:"",title:""},_deferredConnects:{onClick:"",onDblClick:"",onKeyDown:"",onKeyPress:"",onKeyUp:"",onMouseMove:"",onMouseDown:"",onMouseOut:"",onMouseOver:"",onMouseLeave:"",onMouseEnter:"",onMouseUp:""},onClick:dijit._connectOnUseEventHandler,onDblClick:dijit._connectOnUseEventHandler,onKeyDown:dijit._connectOnUseEventHandler,onKeyPress:dijit._connectOnUseEventHandler,onKeyUp:dijit._connectOnUseEventHandler,onMouseDown:dijit._connectOnUseEventHandler,onMouseMove:dijit._connectOnUseEventHandler,onMouseOut:dijit._connectOnUseEventHandler,onMouseOver:dijit._connectOnUseEventHandler,onMouseLeave:dijit._connectOnUseEventHandler,onMouseEnter:dijit._connectOnUseEventHandler,onMouseUp:dijit._connectOnUseEventHandler,_blankGif:(dojo.config.blankGif||dojo.moduleUrl("dojo","resources/blank.gif")),postscript:function(_b,_c){
this.create(_b,_c);
},create:function(_d,_e){
this.srcNodeRef=dojo.byId(_e);
this._connects=[];
this._deferredConnects=dojo.clone(this._deferredConnects);
for(var _f in this.attributeMap){
delete this._deferredConnects[_f];
}
for(_f in this._deferredConnects){
if(this[_f]!==dijit._connectOnUseEventHandler){
delete this._deferredConnects[_f];
}
}
if(this.srcNodeRef&&(typeof this.srcNodeRef.id=="string")){
this.id=this.srcNodeRef.id;
}
if(_d){
this.params=_d;
dojo.mixin(this,_d);
}
this.postMixInProperties();
if(!this.id){
this.id=dijit.getUniqueId(this.declaredClass.replace(/\./g,"_"));
}
dijit.registry.add(this);
this.buildRendering();
if(this.domNode){
this._applyAttributes();
for(_f in this.params){
this._onConnect(_f);
}
}
if(this.domNode){
this.domNode.setAttribute("widgetId",this.id);
}
this.postCreate();
if(this.srcNodeRef&&!this.srcNodeRef.parentNode){
delete this.srcNodeRef;
}
this._created=true;
},_applyAttributes:function(){
var _10=function(_11,_12){
if((_12.params&&_11 in _12.params)||_12[_11]){
_12.attr(_11,_12[_11]);
}
};
for(var _13 in this.attributeMap){
_10(_13,this);
}
dojo.forEach(_5(this.declaredClass),function(a){
if(!(a in this.attributeMap)){
_10(a,this);
}
},this);
},postMixInProperties:function(){
},buildRendering:function(){
this.domNode=this.srcNodeRef||dojo.doc.createElement("div");
},postCreate:function(){
},startup:function(){
this._started=true;
},destroyRecursive:function(_15){
this.destroyDescendants(_15);
this.destroy(_15);
},destroy:function(_16){
this.uninitialize();
dojo.forEach(this._connects,function(_17){
dojo.forEach(_17,dojo.disconnect);
});
dojo.forEach(this._supportingWidgets||[],function(w){
if(w.destroy){
w.destroy();
}
});
this.destroyRendering(_16);
dijit.registry.remove(this.id);
},destroyRendering:function(_19){
if(this.bgIframe){
this.bgIframe.destroy(_19);
delete this.bgIframe;
}
if(this.domNode){
if(!_19){
dojo._destroyElement(this.domNode);
}
delete this.domNode;
}
if(this.srcNodeRef){
if(!_19){
dojo._destroyElement(this.srcNodeRef);
}
delete this.srcNodeRef;
}
},destroyDescendants:function(_1a){
dojo.forEach(this.getDescendants(),function(_1b){
if(_1b.destroy){
_1b.destroy(_1a);
}
});
},uninitialize:function(){
return false;
},onFocus:function(){
},onBlur:function(){
},_onFocus:function(e){
this.onFocus();
},_onBlur:function(){
this.onBlur();
},_onConnect:function(_1d){
if(_1d in this._deferredConnects){
var _1e=this[this._deferredConnects[_1d]||"domNode"];
this.connect(_1e,_1d.toLowerCase(),this[_1d]);
delete this._deferredConnects[_1d];
}
},_setClassAttr:function(_1f){
var _20=this[this.attributeMap["class"]||"domNode"];
dojo.removeClass(_20,this["class"]);
this["class"]=_1f;
dojo.addClass(_20,_1f);
},_setStyleAttr:function(_21){
var _22=this[this.attributeMap["style"]||"domNode"];
if(_22.style.cssText){
_22.style.cssText+="; "+_21;
}else{
_22.style.cssText=_21;
}
this["style"]=_21;
},setAttribute:function(_23,_24){
dojo.deprecated(this.declaredClass+"::setAttribute() is deprecated. Use attr() instead.","","2.0");
this.attr(_23,_24);
},_attrToDom:function(_25,_26){
var _27=this.attributeMap[_25];
dojo.forEach(dojo.isArray(_27)?_27:[_27],function(_28){
var _29=this[_28.node||_28||"domNode"];
var _2a=_28.type||"attribute";
switch(_2a){
case "attribute":
if(dojo.isFunction(_26)){
_26=dojo.hitch(this,_26);
}
if(/^on[A-Z][a-zA-Z]*$/.test(_25)){
_25=_25.toLowerCase();
}
dojo.attr(_29,_25,_26);
break;
case "innerHTML":
_29.innerHTML=_26;
break;
case "class":
dojo.removeClass(_29,this[_25]);
dojo.addClass(_29,_26);
break;
}
},this);
this[_25]=_26;
},attr:function(_2b,_2c){
var _2d=arguments.length;
if(_2d==1&&!dojo.isString(_2b)){
for(var x in _2b){
this.attr(x,_2b[x]);
}
return this;
}
var _2f=this._getAttrNames(_2b);
if(_2d==2){
if(this[_2f.s]){
return this[_2f.s](_2c)||this;
}else{
if(_2b in this.attributeMap){
this._attrToDom(_2b,_2c);
}
this[_2b]=_2c;
}
return this;
}else{
if(this[_2f.g]){
return this[_2f.g]();
}else{
return this[_2b];
}
}
},_attrPairNames:{},_getAttrNames:function(_30){
var apn=this._attrPairNames;
if(apn[_30]){
return apn[_30];
}
var uc=_30.charAt(0).toUpperCase()+_30.substr(1);
return apn[_30]={n:_30+"Node",s:"_set"+uc+"Attr",g:"_get"+uc+"Attr"};
},toString:function(){
return "[Widget "+this.declaredClass+", "+(this.id||"NO ID")+"]";
},getDescendants:function(){
if(this.containerNode){
var _33=dojo.query("[widgetId]",this.containerNode);
return _33.map(dijit.byNode);
}else{
return [];
}
},nodesWithKeyClick:["input","button"],connect:function(obj,_35,_36){
var d=dojo;
var dco=d.hitch(d,"connect",obj);
var _39=[];
if(_35=="ondijitclick"){
if(!this.nodesWithKeyClick[obj.nodeName]){
var m=d.hitch(this,_36);
_39.push(dco("onkeydown",this,function(e){
if(!d.isFF&&e.keyCode==d.keys.ENTER){
return m(e);
}else{
if(e.keyCode==d.keys.SPACE){
d.stopEvent(e);
}
}
}),dco("onkeyup",this,function(e){
if(e.keyCode==d.keys.SPACE){
return m(e);
}
}));
if(d.isFF){
_39.push(dco("onkeypress",this,function(e){
if(e.keyCode==d.keys.ENTER){
return m(e);
}
}));
}
}
_35="onclick";
}
_39.push(dco(_35,this,_36));
this._connects.push(_39);
return _39;
},disconnect:function(_3e){
for(var i=0;i<this._connects.length;i++){
if(this._connects[i]==_3e){
dojo.forEach(_3e,dojo.disconnect);
this._connects.splice(i,1);
return;
}
}
},isLeftToRight:function(){
return dojo._isBodyLtr();
},isFocusable:function(){
return this.focus&&(dojo.style(this.domNode,"display")!="none");
},placeAt:function(_40,_41){
if(_40["declaredClass"]&&_40["addChild"]){
_40.addChild(this,_41);
}else{
dojo.place(this.domNode,_40,_41);
}
return this;
}});
})();
}
