/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.dtl._HtmlTemplated"]){
dojo._hasResource["dojox.dtl._HtmlTemplated"]=true;
dojo.provide("dojox.dtl._HtmlTemplated");
dojo.require("dijit._Templated");
dojo.require("dojox.dtl.html");
dojo.require("dojox.dtl.render.html");
dojo.require("dojox.dtl.contrib.dijit");
dojox.dtl._HtmlTemplated={prototype:{_dijitTemplateCompat:false,buildRendering:function(){
this.domNode=this.srcNodeRef;
if(!this._render){
var _1=dojox.dtl.contrib.dijit;
var _2=_1.widgetsInTemplate;
_1.widgetsInTemplate=this.widgetsInTemplate;
this._template=this._getCachedTemplate(this.templatePath,this.templateString);
this._render=new dojox.dtl.render.html.Render(this.domNode,this._template);
_1.widgetsInTemplate=_2;
}
var _3=this;
this._rendering=setTimeout(function(){
_3.render();
},10);
},setTemplate:function(_4,_5){
if(dojox.dtl.text._isTemplate(_4)){
this._template=this._getCachedTemplate(null,_4);
}else{
this._template=this._getCachedTemplate(_4);
}
this.render(_5);
},render:function(_6,_7){
if(this._rendering){
clearTimeout(this._rendering);
delete this._rendering;
}
if(_7){
this._template=_7;
}
this._render.render(this._getContext(_6),this._template);
},_getContext:function(_8){
if(!(_8 instanceof dojox.dtl.Context)){
_8=false;
}
_8=_8||new dojox.dtl.Context(this);
_8.setThis(this);
return _8;
},_getCachedTemplate:function(_9,_a){
if(!this._templates){
this._templates={};
}
var _b=_a||_9.toString();
var _c=this._templates;
if(_c[_b]){
return _c[_b];
}
return (_c[_b]=new dojox.dtl.HtmlTemplate(dijit._Templated.getCachedTemplate(_9,_a,true)));
}}};
}
