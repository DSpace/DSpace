/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._Templated"]){
dojo._hasResource["dijit._Templated"]=true;
dojo.provide("dijit._Templated");
dojo.require("dijit._Widget");
dojo.require("dojo.string");
dojo.require("dojo.parser");
dojo.declare("dijit._Templated",null,{templateNode:null,templateString:null,templatePath:null,widgetsInTemplate:false,_skipNodeCache:false,_stringRepl:function(_1){
var _2=this.declaredClass,_3=this;
return dojo.string.substitute(_1,this,function(_4,_5){
if(_5.charAt(0)=="!"){
_4=_3[_5.substr(1)];
}
if(typeof _4=="undefined"){
throw new Error(_2+" template:"+_5);
}
if(_4==null){
return "";
}
return _5.charAt(0)=="!"?_4:_4.toString().replace(/"/g,"&quot;");
},this);
},buildRendering:function(){
var _6=dijit._Templated.getCachedTemplate(this.templatePath,this.templateString,this._skipNodeCache);
var _7;
if(dojo.isString(_6)){
_7=dijit._Templated._createNodesFromText(this._stringRepl(_6))[0];
}else{
_7=_6.cloneNode(true);
}
this.domNode=_7;
this._attachTemplateNodes(_7);
var _8=this.srcNodeRef;
if(_8&&_8.parentNode){
_8.parentNode.replaceChild(_7,_8);
}
if(this.widgetsInTemplate){
var cw=(this._supportingWidgets=dojo.parser.parse(_7));
this._attachTemplateNodes(cw,function(n,p){
return n[p];
});
}
this._fillContent(_8);
},_fillContent:function(_c){
var _d=this.containerNode;
if(_c&&_d){
while(_c.hasChildNodes()){
_d.appendChild(_c.firstChild);
}
}
},_attachTemplateNodes:function(_e,_f){
_f=_f||function(n,p){
return n.getAttribute(p);
};
var _12=dojo.isArray(_e)?_e:(_e.all||_e.getElementsByTagName("*"));
var x=dojo.isArray(_e)?0:-1;
var _14={};
for(;x<_12.length;x++){
var _15=(x==-1)?_e:_12[x];
if(this.widgetsInTemplate&&_f(_15,"dojoType")){
continue;
}
var _16=_f(_15,"dojoAttachPoint");
if(_16){
var _17,_18=_16.split(/\s*,\s*/);
while((_17=_18.shift())){
if(dojo.isArray(this[_17])){
this[_17].push(_15);
}else{
this[_17]=_15;
}
}
}
var _19=_f(_15,"dojoAttachEvent");
if(_19){
var _1a,_1b=_19.split(/\s*,\s*/);
var _1c=dojo.trim;
while((_1a=_1b.shift())){
if(_1a){
var _1d=null;
if(_1a.indexOf(":")!=-1){
var _1e=_1a.split(":");
_1a=_1c(_1e[0]);
_1d=_1c(_1e[1]);
}else{
_1a=_1c(_1a);
}
if(!_1d){
_1d=_1a;
}
this.connect(_15,_1a,_1d);
}
}
}
var _1f=_f(_15,"waiRole");
if(_1f){
dijit.setWaiRole(_15,_1f);
}
var _20=_f(_15,"waiState");
if(_20){
dojo.forEach(_20.split(/\s*,\s*/),function(_21){
if(_21.indexOf("-")!=-1){
var _22=_21.split("-");
dijit.setWaiState(_15,_22[0],_22[1]);
}
});
}
}
}});
dijit._Templated._templateCache={};
dijit._Templated.getCachedTemplate=function(_23,_24,_25){
var _26=dijit._Templated._templateCache;
var key=_24||_23;
var _28=_26[key];
if(_28){
if(!_28.ownerDocument||_28.ownerDocument==dojo.doc){
return _28;
}
dojo._destroyElement(_28);
}
if(!_24){
_24=dijit._Templated._sanitizeTemplateString(dojo._getText(_23));
}
_24=dojo.string.trim(_24);
if(_25||_24.match(/\$\{([^\}]+)\}/g)){
return (_26[key]=_24);
}else{
return (_26[key]=dijit._Templated._createNodesFromText(_24)[0]);
}
};
dijit._Templated._sanitizeTemplateString=function(_29){
if(_29){
_29=_29.replace(/^\s*<\?xml(\s)+version=[\'\"](\d)*.(\d)*[\'\"](\s)*\?>/im,"");
var _2a=_29.match(/<body[^>]*>\s*([\s\S]+)\s*<\/body>/im);
if(_2a){
_29=_2a[1];
}
}else{
_29="";
}
return _29;
};
if(dojo.isIE){
dojo.addOnWindowUnload(function(){
var _2b=dijit._Templated._templateCache;
for(var key in _2b){
var _2d=_2b[key];
if(!isNaN(_2d.nodeType)){
dojo._destroyElement(_2d);
}
delete _2b[key];
}
});
}
(function(){
var _2e={cell:{re:/^<t[dh][\s\r\n>]/i,pre:"<table><tbody><tr>",post:"</tr></tbody></table>"},row:{re:/^<tr[\s\r\n>]/i,pre:"<table><tbody>",post:"</tbody></table>"},section:{re:/^<(thead|tbody|tfoot)[\s\r\n>]/i,pre:"<table>",post:"</table>"}};
var tn;
dijit._Templated._createNodesFromText=function(_30){
if(tn&&tn.ownerDocument!=dojo.doc){
dojo._destroyElement(tn);
tn=undefined;
}
if(!tn){
tn=dojo.doc.createElement("div");
tn.style.display="none";
dojo.body().appendChild(tn);
}
var _31="none";
var _32=_30.replace(/^\s+/,"");
for(var _33 in _2e){
var map=_2e[_33];
if(map.re.test(_32)){
_31=_33;
_30=map.pre+_30+map.post;
break;
}
}
tn.innerHTML=_30;
if(tn.normalize){
tn.normalize();
}
var tag={cell:"tr",row:"tbody",section:"table"}[_31];
var _36=(typeof tag!="undefined")?tn.getElementsByTagName(tag)[0]:tn;
var _37=[];
while(_36.firstChild){
_37.push(_36.removeChild(_36.firstChild));
}
tn.innerHTML="";
return _37;
};
})();
dojo.extend(dijit._Widget,{dojoAttachEvent:"",dojoAttachPoint:"",waiRole:"",waiState:""});
}
