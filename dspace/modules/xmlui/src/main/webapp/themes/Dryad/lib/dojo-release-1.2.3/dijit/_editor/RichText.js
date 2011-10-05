/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._editor.RichText"]){
dojo._hasResource["dijit._editor.RichText"]=true;
dojo.provide("dijit._editor.RichText");
dojo.require("dijit._Widget");
dojo.require("dijit._editor.selection");
dojo.require("dijit._editor.range");
dojo.require("dijit._editor.html");
dojo.require("dojo.i18n");
dojo.requireLocalization("dijit.form","Textarea",null,"zh,ca,pt,da,tr,ru,de,ROOT,sv,ja,he,fi,nb,el,ar,pt-pt,cs,fr,es,ko,nl,zh-tw,pl,th,it,hu,sk,sl");
if(!dojo.config["useXDomain"]||dojo.config["allowXdRichTextSave"]){
if(dojo._postLoad){
(function(){
var _1=dojo.doc.createElement("textarea");
_1.id=dijit._scopeName+"._editor.RichText.savedContent";
var s=_1.style;
s.display="none";
s.position="absolute";
s.top="-100px";
s.left="-100px";
s.height="3px";
s.width="3px";
dojo.body().appendChild(_1);
})();
}else{
try{
dojo.doc.write("<textarea id=\""+dijit._scopeName+"._editor.RichText.savedContent\" "+"style=\"display:none;position:absolute;top:-100px;left:-100px;height:3px;width:3px;overflow:hidden;\"></textarea>");
}
catch(e){
}
}
}
dojo.declare("dijit._editor.RichText",dijit._Widget,{constructor:function(_3){
this.contentPreFilters=[];
this.contentPostFilters=[];
this.contentDomPreFilters=[];
this.contentDomPostFilters=[];
this.editingAreaStyleSheets=[];
this._keyHandlers={};
this.contentPreFilters.push(dojo.hitch(this,"_preFixUrlAttributes"));
if(dojo.isMoz){
this.contentPreFilters.push(this._fixContentForMoz);
this.contentPostFilters.push(this._removeMozBogus);
}
if(dojo.isSafari){
this.contentPostFilters.push(this._removeSafariBogus);
}
this.onLoadDeferred=new dojo.Deferred();
},inheritWidth:false,focusOnLoad:false,name:"",styleSheets:"",_content:"",height:"300px",minHeight:"1em",isClosed:true,isLoaded:false,_SEPARATOR:"@@**%%__RICHTEXTBOUNDRY__%%**@@",onLoadDeferred:null,isTabIndent:false,postCreate:function(){
if("textarea"==this.domNode.tagName.toLowerCase()){
console.warn("RichText should not be used with the TEXTAREA tag.  See dijit._editor.RichText docs.");
}
dojo.publish(dijit._scopeName+"._editor.RichText::init",[this]);
this.open();
this.setupDefaultShortcuts();
},setupDefaultShortcuts:function(){
var _4=dojo.hitch(this,function(_5,_6){
return function(){
return !this.execCommand(_5,_6);
};
});
var _7={b:_4("bold"),i:_4("italic"),u:_4("underline"),a:_4("selectall"),s:function(){
this.save(true);
},m:function(){
this.isTabIndent=!this.isTabIndent;
},"1":_4("formatblock","h1"),"2":_4("formatblock","h2"),"3":_4("formatblock","h3"),"4":_4("formatblock","h4"),"\\":_4("insertunorderedlist")};
if(!dojo.isIE){
_7.Z=_4("redo");
}
for(var _8 in _7){
this.addKeyHandler(_8,true,false,_7[_8]);
}
},events:["onKeyPress","onKeyDown","onKeyUp","onClick"],captureEvents:[],_editorCommandsLocalized:false,_localizeEditorCommands:function(){
if(this._editorCommandsLocalized){
return;
}
this._editorCommandsLocalized=true;
var _9=["div","p","pre","h1","h2","h3","h4","h5","h6","ol","ul","address"];
var _a="",_b,i=0;
while((_b=_9[i++])){
if(_b.charAt(1)!="l"){
_a+="<"+_b+"><span>content</span></"+_b+"><br/>";
}else{
_a+="<"+_b+"><li>content</li></"+_b+"><br/>";
}
}
var _d=dojo.doc.createElement("div");
dojo.style(_d,{position:"absolute",left:"-2000px",top:"-2000px"});
dojo.doc.body.appendChild(_d);
_d.innerHTML=_a;
var _e=_d.firstChild;
while(_e){
dijit._editor.selection.selectElement(_e.firstChild);
dojo.withGlobal(this.window,"selectElement",dijit._editor.selection,[_e.firstChild]);
var _f=_e.tagName.toLowerCase();
this._local2NativeFormatNames[_f]=document.queryCommandValue("formatblock");
this._native2LocalFormatNames[this._local2NativeFormatNames[_f]]=_f;
_e=_e.nextSibling.nextSibling;
}
dojo.body().removeChild(_d);
},open:function(_10){
if((!this.onLoadDeferred)||(this.onLoadDeferred.fired>=0)){
this.onLoadDeferred=new dojo.Deferred();
}
if(!this.isClosed){
this.close();
}
dojo.publish(dijit._scopeName+"._editor.RichText::open",[this]);
this._content="";
if((arguments.length==1)&&(_10["nodeName"])){
this.domNode=_10;
}
var dn=this.domNode;
var _12;
if((dn["nodeName"])&&(dn.nodeName.toLowerCase()=="textarea")){
var ta=this.textarea=dn;
this.name=ta.name;
_12=this._preFilterContent(ta.value);
dn=this.domNode=dojo.doc.createElement("div");
dn.setAttribute("widgetId",this.id);
ta.removeAttribute("widgetId");
dn.cssText=ta.cssText;
dn.className+=" "+ta.className;
dojo.place(dn,ta,"before");
var _14=dojo.hitch(this,function(){
with(ta.style){
display="block";
position="absolute";
left=top="-1000px";
if(dojo.isIE){
this.__overflow=overflow;
overflow="hidden";
}
}
});
if(dojo.isIE){
setTimeout(_14,10);
}else{
_14();
}
if(ta.form){
dojo.connect(ta.form,"onsubmit",this,function(){
ta.value=this.getValue();
});
}
}else{
_12=this._preFilterContent(dijit._editor.getChildrenHtml(dn));
dn.innerHTML="";
}
if(_12==""){
_12="&nbsp;";
}
var _15=dojo.contentBox(dn);
this._oldHeight=_15.h;
this._oldWidth=_15.w;
this.savedContent=_12;
if((dn["nodeName"])&&(dn.nodeName=="LI")){
dn.innerHTML=" <br>";
}
this.editingArea=dn.ownerDocument.createElement("div");
dn.appendChild(this.editingArea);
if(this.name!=""&&(!dojo.config["useXDomain"]||dojo.config["allowXdRichTextSave"])){
var _16=dojo.byId(dijit._scopeName+"._editor.RichText.savedContent");
if(_16.value!=""){
var _17=_16.value.split(this._SEPARATOR),i=0,dat;
while((dat=_17[i++])){
var _1a=dat.split(":");
if(_1a[0]==this.name){
_12=_1a[1];
_17.splice(i,1);
break;
}
}
}
this.connect(window,"onbeforeunload","_saveContent");
}
this.isClosed=false;
if(dojo.isIE||dojo.isSafari||dojo.isOpera){
var _1b=dojo.config["dojoBlankHtmlUrl"]||(dojo.moduleUrl("dojo","resources/blank.html")+"");
var ifr=this.editorObject=this.iframe=dojo.doc.createElement("iframe");
ifr.id=this.id+"_iframe";
ifr.src=_1b;
ifr.style.border="none";
ifr.style.width="100%";
ifr.frameBorder=0;
this.editingArea.appendChild(ifr);
var h=null;
var _1e=dojo.hitch(this,function(){
if(h){
dojo.disconnect(h);
h=null;
}
this.window=ifr.contentWindow;
var d=this.document=this.window.document;
d.open();
d.write(this._getIframeDocTxt(_12));
d.close();
if(this._layoutMode){
ifr.style.height="100%";
}else{
if(dojo.isIE>=7){
if(this.height){
ifr.style.height=this.height;
}
if(this.minHeight){
ifr.style.minHeight=this.minHeight;
}
}else{
ifr.style.height=this.height?this.height:this.minHeight;
}
}
if(dojo.isIE){
this._localizeEditorCommands();
}
this.onLoad();
this.savedContent=this.getValue(true);
});
if(dojo.isIE&&dojo.isIE<=7){
var t=setInterval(function(){
if(ifr.contentWindow.isLoaded){
clearInterval(t);
_1e();
}
},100);
}else{
h=dojo.connect(((dojo.isIE)?ifr.contentWindow:ifr),"onload",_1e);
}
}else{
this._drawIframe(_12);
this.savedContent=this.getValue(true);
}
if(dn.nodeName=="LI"){
dn.lastChild.style.marginTop="-1.2em";
}
if(this.domNode.nodeName=="LI"){
this.domNode.lastChild.style.marginTop="-1.2em";
}
dojo.addClass(this.domNode,"RichTextEditable");
},_local2NativeFormatNames:{},_native2LocalFormatNames:{},_localizedIframeTitles:null,_getIframeDocTxt:function(_21){
var _cs=dojo.getComputedStyle(this.domNode);
if(dojo.isIE||(!this.height&&!dojo.isMoz)){
_21="<div>"+_21+"</div>";
}
var _23=[_cs.fontWeight,_cs.fontSize,_cs.fontFamily].join(" ");
var _24=_cs.lineHeight;
if(_24.indexOf("px")>=0){
_24=parseFloat(_24)/parseFloat(_cs.fontSize);
}else{
if(_24.indexOf("em")>=0){
_24=parseFloat(_24);
}else{
_24="1.0";
}
}
var _25="";
this.style.replace(/(^|;)(line-|font-?)[^;]+/g,function(_26){
_25+=_26.replace(/^;/g,"")+";";
});
return [this.isLeftToRight()?"<html><head>":"<html dir='rtl'><head>",(dojo.isMoz?"<title>"+this._localizedIframeTitles.iframeEditTitle+"</title>":""),"<style>","body,html {","\tbackground:transparent;","\tpadding: 1em 0 0 0;","\tmargin: -1em 0 0 0;","\theight: 100%;","}","body{","\ttop:0px; left:0px; right:0px;","\tfont:",_23,";",((this.height||dojo.isOpera)?"":"position: fixed;"),"\tmin-height:",this.minHeight,";","\tline-height:",_24,"}","p{ margin: 1em 0 !important; }",(this.height?"":"body,html{height:auto;overflow-y:hidden;/*for IE*/} body > div {overflow-x:auto;/*for FF to show vertical scrollbar*/}"),"li > ul:-moz-first-node, li > ol:-moz-first-node{ padding-top: 1.2em; } ","li{ min-height:1.2em; }","</style>",this._applyEditingAreaStyleSheets(),"</head><body style='"+_25+"'>"+_21+"</body></html>"].join("");
},_drawIframe:function(_27){
if(!this.iframe){
var ifr=this.iframe=dojo.doc.createElement("iframe");
ifr.id=this.id+"_iframe";
var _29=ifr.style;
_29.border="none";
_29.lineHeight="0";
_29.verticalAlign="bottom";
this.editorObject=this.iframe;
this._localizedIframeTitles=dojo.i18n.getLocalization("dijit.form","Textarea");
var _2a=dojo.query("label[for=\""+this.id+"\"]");
if(_2a.length){
this._localizedIframeTitles.iframeEditTitle=_2a[0].innerHTML+" "+this._localizedIframeTitles.iframeEditTitle;
}
}
this.iframe.style.width=this.inheritWidth?this._oldWidth:"100%";
if(this._layoutMode){
this.iframe.style.height="100%";
}else{
if(this.height){
this.iframe.style.height=this.height;
}else{
this.iframe.height=this._oldHeight;
}
}
var _2b;
if(this.textarea){
_2b=this.srcNodeRef;
}else{
_2b=dojo.doc.createElement("div");
_2b.style.display="none";
_2b.innerHTML=_27;
this.editingArea.appendChild(_2b);
}
this.editingArea.appendChild(this.iframe);
var _2c=dojo.hitch(this,function(){
if(!this.editNode){
if(!this.document){
try{
if(this.iframe.contentWindow){
this.window=this.iframe.contentWindow;
this.document=this.iframe.contentWindow.document;
}else{
if(this.iframe.contentDocument){
this.window=this.iframe.contentDocument.window;
this.document=this.iframe.contentDocument;
}
}
}
catch(e){
}
if(!this.document){
setTimeout(_2c,50);
return;
}
var _2d=this.document;
_2d.open();
if(dojo.isAIR){
_2d.body.innerHTML=_27;
}else{
_2d.write(this._getIframeDocTxt(_27));
}
_2d.close();
dojo._destroyElement(_2b);
}
if(!this.document.body){
setTimeout(_2c,50);
return;
}
this.onLoad();
}else{
dojo._destroyElement(_2b);
this.editNode.innerHTML=_27;
this.onDisplayChanged();
}
this._preDomFilterContent(this.editNode);
});
_2c();
},_applyEditingAreaStyleSheets:function(){
var _2e=[];
if(this.styleSheets){
_2e=this.styleSheets.split(";");
this.styleSheets="";
}
_2e=_2e.concat(this.editingAreaStyleSheets);
this.editingAreaStyleSheets=[];
var _2f="",i=0,url;
while((url=_2e[i++])){
var _32=(new dojo._Url(dojo.global.location,url)).toString();
this.editingAreaStyleSheets.push(_32);
_2f+="<link rel=\"stylesheet\" type=\"text/css\" href=\""+_32+"\"/>";
}
return _2f;
},addStyleSheet:function(uri){
var url=uri.toString();
if(url.charAt(0)=="."||(url.charAt(0)!="/"&&!uri.host)){
url=(new dojo._Url(dojo.global.location,url)).toString();
}
if(dojo.indexOf(this.editingAreaStyleSheets,url)>-1){
return;
}
this.editingAreaStyleSheets.push(url);
if(this.document.createStyleSheet){
this.document.createStyleSheet(url);
}else{
var _35=this.document.getElementsByTagName("head")[0];
var _36=this.document.createElement("link");
with(_36){
rel="stylesheet";
type="text/css";
href=url;
}
_35.appendChild(_36);
}
},removeStyleSheet:function(uri){
var url=uri.toString();
if(url.charAt(0)=="."||(url.charAt(0)!="/"&&!uri.host)){
url=(new dojo._Url(dojo.global.location,url)).toString();
}
var _39=dojo.indexOf(this.editingAreaStyleSheets,url);
if(_39==-1){
return;
}
delete this.editingAreaStyleSheets[_39];
dojo.withGlobal(this.window,"query",dojo,["link:[href=\""+url+"\"]"]).orphan();
},disabled:true,_mozSettingProps:["styleWithCSS","insertBrOnReturn"],_setDisabledAttr:function(_3a){
if(!this.editNode||"_delayedDisabled" in this){
this._delayedDisabled=_3a;
return;
}
_3a=Boolean(_3a);
if(dojo.isIE||dojo.isSafari||dojo.isOpera){
var _3b=dojo.isIE&&(this.isLoaded||!this.focusOnLoad);
if(_3b){
this.editNode.unselectable="on";
}
this.editNode.contentEditable=!_3a;
if(_3b){
var _3c=this;
setTimeout(function(){
_3c.editNode.unselectable="off";
},0);
}
}else{
if(_3a){
this._mozSettings=[false,this.blockNodeForEnter==="BR"];
}
this.document.designMode=(_3a?"off":"on");
if(!_3a&&this._mozSettingProps){
var ps=this._mozSettingProps;
for(var n in ps){
if(ps.hasOwnProperty(n)){
try{
this.document.execCommand(n,false,ps[n]);
}
catch(e){
}
}
}
}
}
this.disabled=_3a;
},_isResized:function(){
return false;
},onLoad:function(e){
if(!this.window.__registeredWindow){
this.window.__registeredWindow=true;
dijit.registerWin(this.window);
}
if(!dojo.isIE&&(this.height||dojo.isMoz)){
this.editNode=this.document.body;
}else{
this.editNode=this.document.body.firstChild;
var _40=this;
if(dojo.isIE){
var _41=this.tabStop=dojo.doc.createElement("<div tabIndex=-1>");
this.editingArea.appendChild(_41);
this.iframe.onfocus=function(){
_40.editNode.setActive();
};
}
}
this.focusNode=this.editNode;
try{
this.attr("disabled",false);
}
catch(e){
var _42=dojo.connect(this,"onClick",this,function(){
this.attr("disabled",false);
dojo.disconnect(_42);
});
}
this._preDomFilterContent(this.editNode);
var _43=this.events.concat(this.captureEvents);
var ap=(this.iframe)?this.document:this.editNode;
dojo.forEach(_43,function(_45){
this.connect(ap,_45.toLowerCase(),_45);
},this);
if(dojo.isIE){
this.connect(this.document,"onmousedown","_onIEMouseDown");
this.editNode.style.zoom=1;
}
if(this.focusOnLoad){
dojo.addOnLoad(dojo.hitch(this,"focus"));
}
this.onDisplayChanged(e);
if("_delayedDisabled" in this){
var d=this._delayedDisabled;
delete this._delayedDisabled;
this.attr("disabled",d);
}
this.isLoaded=true;
if(this.onLoadDeferred){
this.onLoadDeferred.callback(true);
}
},onKeyDown:function(e){
if(e.keyCode===dojo.keys.TAB&&this.isTabIndent){
dojo.stopEvent(e);
if(this.queryCommandEnabled((e.shiftKey?"outdent":"indent"))){
this.execCommand((e.shiftKey?"outdent":"indent"));
}
}
if(dojo.isIE){
if(e.keyCode==dojo.keys.TAB&&!this.isTabIndent){
if(e.shiftKey&&!e.ctrlKey&&!e.altKey){
this.iframe.focus();
}else{
if(!e.shiftKey&&!e.ctrlKey&&!e.altKey){
this.tabStop.focus();
}
}
}else{
if(e.keyCode===dojo.keys.BACKSPACE&&this.document.selection.type==="Control"){
dojo.stopEvent(e);
this.execCommand("delete");
}else{
if((65<=e.keyCode&&e.keyCode<=90)||(e.keyCode>=37&&e.keyCode<=40)){
e.charCode=e.keyCode;
this.onKeyPress(e);
}
}
}
}else{
if(dojo.isMoz&&!this.isTabIndent){
if(e.keyCode==dojo.keys.TAB&&!e.shiftKey&&!e.ctrlKey&&!e.altKey&&this.iframe){
var _48=dojo.isFF<3?this.iframe.contentDocument:this.iframe;
_48.title=this._localizedIframeTitles.iframeFocusTitle;
this.iframe.focus();
dojo.stopEvent(e);
}else{
if(e.keyCode==dojo.keys.TAB&&e.shiftKey){
if(this.toolbar){
this.toolbar.focus();
}
dojo.stopEvent(e);
}
}
}
}
return true;
},onKeyUp:function(e){
return;
},setDisabled:function(_4a){
dojo.deprecated("dijit.Editor::setDisabled is deprecated","use dijit.Editor::attr(\"disabled\",boolean) instead",2);
this.attr("disabled",_4a);
},_setValueAttr:function(_4b){
this.setValue(_4b);
},onKeyPress:function(e){
var c=(e.keyChar&&e.keyChar.toLowerCase())||e.keyCode;
var _4e=this._keyHandlers[c];
var _4f=arguments;
if(_4e){
dojo.forEach(_4e,function(h){
if((!!h.shift==!!e.shiftKey)&&(!!h.ctrl==!!e.ctrlKey)){
if(!h.handler.apply(this,_4f)){
e.preventDefault();
}
}
},this);
}
if(!this._onKeyHitch){
this._onKeyHitch=dojo.hitch(this,"onKeyPressed");
}
setTimeout(this._onKeyHitch,1);
return true;
},addKeyHandler:function(key,_52,_53,_54){
if(!dojo.isArray(this._keyHandlers[key])){
this._keyHandlers[key]=[];
}
this._keyHandlers[key].push({shift:_53||false,ctrl:_52||false,handler:_54});
},onKeyPressed:function(){
this.onDisplayChanged();
},onClick:function(e){
this.onDisplayChanged(e);
},_onIEMouseDown:function(e){
if(!this._focused&&!this.disabled){
this.focus();
}
},_onBlur:function(e){
this.inherited(arguments);
var _c=this.getValue(true);
if(_c!=this.savedContent){
this.onChange(_c);
this.savedContent=_c;
}
if(dojo.isMoz&&this.iframe){
var _59=dojo.isFF<3?this.iframe.contentDocument:this.iframe;
_59.title=this._localizedIframeTitles.iframeEditTitle;
}
},_initialFocus:true,_onFocus:function(e){
if(dojo.isMoz&&this._initialFocus){
this._initialFocus=false;
if(this.editNode.innerHTML.replace(/^\s+|\s+$/g,"")=="&nbsp;"){
this.placeCursorAtStart();
}
}
this.inherited(arguments);
},blur:function(){
if(!dojo.isIE&&this.window.document.documentElement&&this.window.document.documentElement.focus){
this.window.document.documentElement.focus();
}else{
if(dojo.doc.body.focus){
dojo.doc.body.focus();
}
}
},focus:function(){
if(!dojo.isIE){
dijit.focus(this.iframe);
}else{
if(this.editNode&&this.editNode.focus){
this.iframe.fireEvent("onfocus",document.createEventObject());
}
}
},updateInterval:200,_updateTimer:null,onDisplayChanged:function(e){
if(this._updateTimer){
clearTimeout(this._updateTimer);
}
if(!this._updateHandler){
this._updateHandler=dojo.hitch(this,"onNormalizedDisplayChanged");
}
this._updateTimer=setTimeout(this._updateHandler,this.updateInterval);
},onNormalizedDisplayChanged:function(){
delete this._updateTimer;
},onChange:function(_5c){
},_normalizeCommand:function(cmd){
var _5e=cmd.toLowerCase();
if(_5e=="formatblock"){
if(dojo.isSafari){
_5e="heading";
}
}else{
if(_5e=="hilitecolor"&&!dojo.isMoz){
_5e="backcolor";
}
}
return _5e;
},_qcaCache:{},queryCommandAvailable:function(_5f){
var ca=this._qcaCache[_5f];
if(ca!=undefined){
return ca;
}
return this._qcaCache[_5f]=this._queryCommandAvailable(_5f);
},_queryCommandAvailable:function(_61){
var ie=1;
var _63=1<<1;
var _64=1<<2;
var _65=1<<3;
var _66=1<<4;
var _67=dojo.isSafari;
function isSupportedBy(_68){
return {ie:Boolean(_68&ie),mozilla:Boolean(_68&_63),safari:Boolean(_68&_64),safari420:Boolean(_68&_66),opera:Boolean(_68&_65)};
};
var _69=null;
switch(_61.toLowerCase()){
case "bold":
case "italic":
case "underline":
case "subscript":
case "superscript":
case "fontname":
case "fontsize":
case "forecolor":
case "hilitecolor":
case "justifycenter":
case "justifyfull":
case "justifyleft":
case "justifyright":
case "delete":
case "selectall":
case "toggledir":
_69=isSupportedBy(_63|ie|_64|_65);
break;
case "createlink":
case "unlink":
case "removeformat":
case "inserthorizontalrule":
case "insertimage":
case "insertorderedlist":
case "insertunorderedlist":
case "indent":
case "outdent":
case "formatblock":
case "inserthtml":
case "undo":
case "redo":
case "strikethrough":
case "tabindent":
_69=isSupportedBy(_63|ie|_65|_66);
break;
case "blockdirltr":
case "blockdirrtl":
case "dirltr":
case "dirrtl":
case "inlinedirltr":
case "inlinedirrtl":
_69=isSupportedBy(ie);
break;
case "cut":
case "copy":
case "paste":
_69=isSupportedBy(ie|_63|_66);
break;
case "inserttable":
_69=isSupportedBy(_63|ie);
break;
case "insertcell":
case "insertcol":
case "insertrow":
case "deletecells":
case "deletecols":
case "deleterows":
case "mergecells":
case "splitcell":
_69=isSupportedBy(ie|_63);
break;
default:
return false;
}
return (dojo.isIE&&_69.ie)||(dojo.isMoz&&_69.mozilla)||(dojo.isSafari&&_69.safari)||(_67&&_69.safari420)||(dojo.isOpera&&_69.opera);
},execCommand:function(_6a,_6b){
var _6c;
this.focus();
_6a=this._normalizeCommand(_6a);
if(_6b!=undefined){
if(_6a=="heading"){
throw new Error("unimplemented");
}else{
if((_6a=="formatblock")&&dojo.isIE){
_6b="<"+_6b+">";
}
}
}
if(_6a=="inserthtml"){
_6b=this._preFilterContent(_6b);
_6c=true;
if(dojo.isIE){
var _6d=this.document.selection.createRange();
if(this.document.selection.type.toUpperCase()=="CONTROL"){
var n=_6d.item(0);
while(_6d.length){
_6d.remove(_6d.item(0));
}
n.outerHTML=_6b;
}else{
_6d.pasteHTML(_6b);
}
_6d.select();
}else{
if(dojo.isMoz&&!_6b.length){
this._sCall("remove");
}else{
_6c=this.document.execCommand(_6a,false,_6b);
}
}
}else{
if((_6a=="unlink")&&(this.queryCommandEnabled("unlink"))&&(dojo.isMoz||dojo.isSafari)){
var a=this._sCall("getAncestorElement",["a"]);
this._sCall("selectElement",[a]);
_6c=this.document.execCommand("unlink",false,null);
}else{
if((_6a=="hilitecolor")&&(dojo.isMoz)){
this.document.execCommand("styleWithCSS",false,true);
_6c=this.document.execCommand(_6a,false,_6b);
this.document.execCommand("styleWithCSS",false,false);
}else{
if((dojo.isIE)&&((_6a=="backcolor")||(_6a=="forecolor"))){
_6b=arguments.length>1?_6b:null;
_6c=this.document.execCommand(_6a,false,_6b);
}else{
_6b=arguments.length>1?_6b:null;
if(_6b||_6a!="createlink"){
_6c=this.document.execCommand(_6a,false,_6b);
}
}
}
}
}
this.onDisplayChanged();
return _6c;
},queryCommandEnabled:function(_70){
if(this.disabled){
return false;
}
_70=this._normalizeCommand(_70);
if(dojo.isMoz||dojo.isSafari){
if(_70=="unlink"){
this._sCall("hasAncestorElement",["a"]);
}else{
if(_70=="inserttable"){
return true;
}
}
}
if(dojo.isSafari){
if(_70=="copy"){
_70="cut";
}else{
if(_70=="paste"){
return true;
}
}
}
if(_70=="indent"){
var li=this._sCall("getAncestorElement",["li"]);
var n=li&&li.previousSibling;
while(n){
if(n.nodeType==1){
return true;
}
n=n.previousSibling;
}
return false;
}else{
if(_70=="outdent"){
return this._sCall("hasAncestorElement",["li"]);
}
}
var _73=dojo.isIE?this.document.selection.createRange():this.document;
return _73.queryCommandEnabled(_70);
},queryCommandState:function(_74){
if(this.disabled){
return false;
}
_74=this._normalizeCommand(_74);
return this.document.queryCommandState(_74);
},queryCommandValue:function(_75){
if(this.disabled){
return false;
}
var r;
_75=this._normalizeCommand(_75);
if(dojo.isIE&&_75=="formatblock"){
r=this._native2LocalFormatNames[this.document.queryCommandValue(_75)];
}else{
r=this.document.queryCommandValue(_75);
}
return r;
},_sCall:function(_77,_78){
return dojo.withGlobal(this.window,_77,dijit._editor.selection,_78);
},placeCursorAtStart:function(){
this.focus();
var _79=false;
if(dojo.isMoz){
var _7a=this.editNode.firstChild;
while(_7a){
if(_7a.nodeType==3){
if(_7a.nodeValue.replace(/^\s+|\s+$/g,"").length>0){
_79=true;
this._sCall("selectElement",[_7a]);
break;
}
}else{
if(_7a.nodeType==1){
_79=true;
this._sCall("selectElementChildren",[_7a]);
break;
}
}
_7a=_7a.nextSibling;
}
}else{
_79=true;
this._sCall("selectElementChildren",[this.editNode]);
}
if(_79){
this._sCall("collapse",[true]);
}
},placeCursorAtEnd:function(){
this.focus();
var _7b=false;
if(dojo.isMoz){
var _7c=this.editNode.lastChild;
while(_7c){
if(_7c.nodeType==3){
if(_7c.nodeValue.replace(/^\s+|\s+$/g,"").length>0){
_7b=true;
this._sCall("selectElement",[_7c]);
break;
}
}else{
if(_7c.nodeType==1){
_7b=true;
if(_7c.lastChild){
this._sCall("selectElement",[_7c.lastChild]);
}else{
this._sCall("selectElement",[_7c]);
}
break;
}
}
_7c=_7c.previousSibling;
}
}else{
_7b=true;
this._sCall("selectElementChildren",[this.editNode]);
}
if(_7b){
this._sCall("collapse",[false]);
}
},getValue:function(_7d){
if(this.textarea){
if(this.isClosed||!this.isLoaded){
return this.textarea.value;
}
}
return this._postFilterContent(null,_7d);
},_getValueAttr:function(){
return this.getValue();
},setValue:function(_7e){
if(!this.isLoaded){
this.onLoadDeferred.addCallback(dojo.hitch(this,function(){
this.setValue(_7e);
}));
return;
}
if(this.textarea&&(this.isClosed||!this.isLoaded)){
this.textarea.value=_7e;
}else{
_7e=this._preFilterContent(_7e);
var _7f=this.isClosed?this.domNode:this.editNode;
_7f.innerHTML=_7e;
this._preDomFilterContent(_7f);
}
this.onDisplayChanged();
},replaceValue:function(_80){
if(this.isClosed){
this.setValue(_80);
}else{
if(this.window&&this.window.getSelection&&!dojo.isMoz){
this.setValue(_80);
}else{
if(this.window&&this.window.getSelection){
_80=this._preFilterContent(_80);
this.execCommand("selectall");
if(dojo.isMoz&&!_80){
_80="&nbsp;";
}
this.execCommand("inserthtml",_80);
this._preDomFilterContent(this.editNode);
}else{
if(this.document&&this.document.selection){
this.setValue(_80);
}
}
}
}
},_preFilterContent:function(_81){
var ec=_81;
dojo.forEach(this.contentPreFilters,function(ef){
if(ef){
ec=ef(ec);
}
});
return ec;
},_preDomFilterContent:function(dom){
dom=dom||this.editNode;
dojo.forEach(this.contentDomPreFilters,function(ef){
if(ef&&dojo.isFunction(ef)){
ef(dom);
}
},this);
},_postFilterContent:function(dom,_87){
var ec;
if(!dojo.isString(dom)){
dom=dom||this.editNode;
if(this.contentDomPostFilters.length){
if(_87){
dom=dojo.clone(dom);
}
dojo.forEach(this.contentDomPostFilters,function(ef){
dom=ef(dom);
});
}
ec=dijit._editor.getChildrenHtml(dom);
}else{
ec=dom;
}
if(!dojo.trim(ec.replace(/^\xA0\xA0*/,"").replace(/\xA0\xA0*$/,"")).length){
ec="";
}
dojo.forEach(this.contentPostFilters,function(ef){
ec=ef(ec);
});
return ec;
},_saveContent:function(e){
var _8c=dojo.byId(dijit._scopeName+"._editor.RichText.savedContent");
_8c.value+=this._SEPARATOR+this.name+":"+this.getValue();
},escapeXml:function(str,_8e){
str=str.replace(/&/gm,"&amp;").replace(/</gm,"&lt;").replace(/>/gm,"&gt;").replace(/"/gm,"&quot;");
if(!_8e){
str=str.replace(/'/gm,"&#39;");
}
return str;
},getNodeHtml:function(_8f){
dojo.deprecated("dijit.Editor::getNodeHtml is deprecated","use dijit._editor.getNodeHtml instead",2);
return dijit._editor.getNodeHtml(_8f);
},getNodeChildrenHtml:function(dom){
dojo.deprecated("dijit.Editor::getNodeChildrenHtml is deprecated","use dijit._editor.getChildrenHtml instead",2);
return dijit._editor.getChildrenHtml(dom);
},close:function(_91,_92){
if(this.isClosed){
return false;
}
if(!arguments.length){
_91=true;
}
this._content=this.getValue();
var _93=(this.savedContent!=this._content);
if(this.interval){
clearInterval(this.interval);
}
if(this.textarea){
with(this.textarea.style){
position="";
left=top="";
if(dojo.isIE){
overflow=this.__overflow;
this.__overflow=null;
}
}
this.textarea.value=_91?this._content:this.savedContent;
dojo._destroyElement(this.domNode);
this.domNode=this.textarea;
}else{
this.domNode.innerHTML=_91?this._content:this.savedContent;
}
dojo.removeClass(this.domNode,"RichTextEditable");
this.isClosed=true;
this.isLoaded=false;
delete this.editNode;
if(this.window&&this.window._frameElement){
this.window._frameElement=null;
}
this.window=null;
this.document=null;
this.editingArea=null;
this.editorObject=null;
return _93;
},destroyRendering:function(){
},destroy:function(){
this.destroyRendering();
if(!this.isClosed){
this.close(false);
}
this.inherited("destroy",arguments);
},_removeMozBogus:function(_94){
return _94.replace(/\stype="_moz"/gi,"").replace(/\s_moz_dirty=""/gi,"");
},_removeSafariBogus:function(_95){
return _95.replace(/\sclass="webkit-block-placeholder"/gi,"");
},_fixContentForMoz:function(_96){
return _96.replace(/<(\/)?strong([ \>])/gi,"<$1b$2").replace(/<(\/)?em([ \>])/gi,"<$1i$2");
},_preFixUrlAttributes:function(_97){
return _97.replace(/(?:(<a(?=\s).*?\shref=)("|')(.*?)\2)|(?:(<a\s.*?href=)([^"'][^ >]+))/gi,"$1$4$2$3$5$2 _djrealurl=$2$3$5$2").replace(/(?:(<img(?=\s).*?\ssrc=)("|')(.*?)\2)|(?:(<img\s.*?src=)([^"'][^ >]+))/gi,"$1$4$2$3$5$2 _djrealurl=$2$3$5$2");
}});
}
