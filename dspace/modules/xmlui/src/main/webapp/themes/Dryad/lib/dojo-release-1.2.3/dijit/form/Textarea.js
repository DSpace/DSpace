/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.form.Textarea"]){
dojo._hasResource["dijit.form.Textarea"]=true;
dojo.provide("dijit.form.Textarea");
dojo.require("dijit.form._FormWidget");
dojo.require("dojo.i18n");
dojo.requireLocalization("dijit.form","Textarea",null,"zh,ca,pt,da,tr,ru,de,ROOT,sv,ja,he,fi,nb,el,ar,pt-pt,cs,fr,es,ko,nl,zh-tw,pl,th,it,hu,sk,sl");
dojo.declare("dijit.form.Textarea",dijit.form._FormValueWidget,{attributeMap:dojo.mixin(dojo.clone(dijit.form._FormValueWidget.prototype.attributeMap),{style:"styleNode","class":"styleNode"}),templateString:(dojo.isIE||dojo.isSafari||dojo.isFF)?((dojo.isIE||dojo.isSafari||dojo.isFF>=3)?"<fieldset id=\"${id}\" class=\"dijitInline\" dojoAttachPoint=\"styleNode\" waiRole=\"presentation\"><div dojoAttachPoint=\"editNode,focusNode,eventNode\" dojoAttachEvent=\"onpaste:_changing,oncut:_changing\" waiRole=\"textbox\" waiState=\"multiline-true\" contentEditable=\"true\"></div>":"<span id=\"${id}\" class=\"dijitReset\">"+"<iframe src=\"javascript:<html><head><title>${_iframeEditTitle}</title></head><body><script>var _postCreate=window.frameElement?window.frameElement.postCreate:null;if(_postCreate)_postCreate();</script></body></html>\""+" dojoAttachPoint=\"iframe,styleNode,stateNode\" dojoAttachEvent=\"onblur:_onIframeBlur\" class=\"dijitInline dijitInputField\"></iframe>")+"<textarea name=\"${name}\" value=\"${value}\" dojoAttachPoint=\"formValueNode\" style=\"display:none;\" autocomplete=\"off\"></textarea>"+((dojo.isIE||dojo.isSafari||dojo.isFF>=3)?"</fieldset>":"</span>"):"<textarea id=\"${id}\" name=\"${name}\" value=\"${value}\" dojoAttachPoint=\"formValueNode,editNode,focusNode,styleNode\">"+dojo.isFF+"</textarea>",baseClass:"dijitTextArea",_setDisabledAttr:function(_1){
this.inherited(arguments);
this.formValueNode.disabled=this.disabled;
this._adjustWritable();
},_setReadOnlyAttr:function(_2){
this.readOnly=_2;
this._adjustWritable();
},_adjustWritable:function(){
if(dojo.isIE||dojo.isSafari||dojo.isFF>=3){
this.editNode.contentEditable=(!this.disabled&&!this.readOnly);
}else{
if(dojo.isFF){
this.iframe.contentDocument.designMode=(this.disabled||this.readOnly)?"off":"on";
}
}
},focus:function(){
if(!this.disabled&&!this.readOnly){
this._changing();
}
dijit.focus(this.iframe||this.focusNode);
},_setValueAttr:function(_3,_4){
var _5=this.editNode;
if(typeof _3=="string"){
_5.innerHTML="";
if(_3.split){
var _6=this;
var _7=true;
dojo.forEach(_3.split("\n"),function(_8){
if(_7){
_7=false;
}else{
_5.appendChild(dojo.doc.createElement("BR"));
}
if(_8){
_5.appendChild(dojo.doc.createTextNode(_8));
}
});
}else{
if(_3){
_5.appendChild(dojo.doc.createTextNode(_3));
}
}
if(!dojo.isIE){
_5.appendChild(dojo.doc.createElement("BR"));
}
}else{
_3=_5.innerHTML;
if(this.iframe){
_3=_3.replace(/<div><\/div>\r?\n?$/i,"");
}
_3=_3.replace(/\s*\r?\n|^\s+|\s+$|&nbsp;/g,"").replace(/>\s+</g,"><").replace(/<\/(p|div)>$|^<(p|div)[^>]*>/gi,"").replace(/([^>])<div>/g,"$1\n").replace(/<\/p>\s*<p[^>]*>|<br[^>]*>|<\/div>\s*<div[^>]*>/gi,"\n").replace(/<[^>]*>/g,"").replace(/&amp;/gi,"&").replace(/&lt;/gi,"<").replace(/&gt;/gi,">");
if(!dojo.isIE){
_3=_3.replace(/\n$/,"");
}
}
this.value=this.formValueNode.value=_3;
if(this.iframe){
var _9=dojo.doc.createElement("div");
_5.appendChild(_9);
var _a=_9.offsetTop;
if(_5.scrollWidth>_5.clientWidth){
_a+=16;
}
if(this.lastHeight!=_a){
if(_a==0){
_a=16;
}
dojo.contentBox(this.iframe,{h:_a});
this.lastHeight=_a;
}
_5.removeChild(_9);
}
dijit.form.Textarea.superclass._setValueAttr.call(this,this.attr("value"),_4);
},_getValueAttr:function(){
return this.value.replace(/\r/g,"");
},postMixInProperties:function(){
this.inherited(arguments);
if(this.srcNodeRef&&this.srcNodeRef.innerHTML!=""){
this.value=this.srcNodeRef.innerHTML;
this.srcNodeRef.innerHTML="";
}
if((!this.value||this.value=="")&&this.srcNodeRef&&this.srcNodeRef.value){
this.value=this.srcNodeRef.value;
}
if(!this.value){
this.value="";
}
this.value=this.value.replace(/\r\n/g,"\n").replace(/&gt;/g,">").replace(/&lt;/g,"<").replace(/&amp;/g,"&");
if(dojo.isFF==2){
var _b=dojo.i18n.getLocalization("dijit.form","Textarea");
this._iframeEditTitle=_b.iframeEditTitle;
this._iframeFocusTitle=_b.iframeFocusTitle;
var _c=dojo.query("label[for=\""+this.id+"\"]");
if(_c.length){
this._iframeEditTitle=_c[0].innerHTML+" "+this._iframeEditTitle;
}
this.focusNode=this.editNode=dojo.doc.createElement("BODY");
}
},postCreate:function(){
var _d="";
if(dojo.isIE||dojo.isSafari||dojo.isFF>=3){
this.domNode.style.overflowY="hidden";
}else{
if(dojo.isFF){
var w=this.iframe.contentWindow;
var _f="";
try{
_f=this.iframe.contentDocument.title;
}
catch(e){
}
if(!w||!_f){
this.iframe.postCreate=dojo.hitch(this,this.postCreate);
return;
}
var d=w.document;
d.getElementsByTagName("HTML")[0].replaceChild(this.editNode,d.getElementsByTagName("BODY")[0]);
if(!this.isLeftToRight()){
d.getElementsByTagName("HTML")[0].dir="rtl";
}
this.iframe.style.overflowY="hidden";
this.eventNode=d;
w.addEventListener("resize",dojo.hitch(this,this._changed),false);
dijit.registerWin(w);
_d="margin:0px;padding:0px;border:0px;";
}else{
this.focusNode=this.domNode;
}
}
this.style.replace(/(^|;)(line-|font-?)[^;]+/g,function(_11){
_d+=_11.replace(/^;/g,"")+";";
});
dojo.attr(this.focusNode,"style",_d);
if(this.eventNode){
this.connect(this.eventNode,"keypress",this._onKeyPress);
this.connect(this.eventNode,"mousemove",this._changed);
this.connect(this.eventNode,"focus",this._focusedEventNode);
this.connect(this.eventNode,"blur",this._blurredEventNode);
}
if(this.editNode){
this.connect(this.editNode,"change",this._changed);
}
this.inherited("postCreate",arguments);
},_focusedEventNode:function(e){
this._focused=true;
this._setStateClass();
this._changed(e);
},_blurredEventNode:function(e){
this._focused=false;
this._setStateClass();
this._changed(e,true);
},_onIframeBlur:function(){
this.iframe.contentDocument.title=this._iframeEditTitle;
},_onKeyPress:function(e){
if(e.charOrCode===dojo.keys.TAB&&!e.shiftKey&&!e.ctrlKey&&!e.altKey&&this.iframe){
this.iframe.contentDocument.title=this._iframeFocusTitle;
this.iframe.focus();
dojo.stopEvent(e);
}else{
if(e.charOrCode==dojo.keys.ENTER){
e.stopPropagation();
}else{
if(this.inherited("_onKeyPress",arguments)&&this.iframe){
var te=dojo.doc.createEvent("KeyEvents");
te.initKeyEvent("keypress",true,true,null,e.ctrlKey,e.altKey,e.shiftKey,e.metaKey,e.keyCode,e.charCode);
this.iframe.dispatchEvent(te);
}
}
}
this._changing();
},_changing:function(e){
setTimeout(dojo.hitch(this,"_changed",e,false),1);
},_changed:function(e,_18){
if(this.iframe&&this.iframe.contentDocument.designMode!="on"&&!this.disabled&&!this.readOnly){
this.iframe.contentDocument.designMode="on";
}
this._setValueAttr(null,_18||false);
}});
}
