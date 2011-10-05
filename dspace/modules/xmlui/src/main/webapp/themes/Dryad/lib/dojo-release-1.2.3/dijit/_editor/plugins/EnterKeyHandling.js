/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._editor.plugins.EnterKeyHandling"]){
dojo._hasResource["dijit._editor.plugins.EnterKeyHandling"]=true;
dojo.provide("dijit._editor.plugins.EnterKeyHandling");
dojo.declare("dijit._editor.plugins.EnterKeyHandling",dijit._editor._Plugin,{blockNodeForEnter:"BR",constructor:function(_1){
if(_1){
dojo.mixin(this,_1);
}
},setEditor:function(_2){
this.editor=_2;
if(this.blockNodeForEnter=="BR"){
if(dojo.isIE){
_2.contentDomPreFilters.push(dojo.hitch(this,"regularPsToSingleLinePs"));
_2.contentDomPostFilters.push(dojo.hitch(this,"singleLinePsToRegularPs"));
_2.onLoadDeferred.addCallback(dojo.hitch(this,"_fixNewLineBehaviorForIE"));
}else{
_2.onLoadDeferred.addCallback(dojo.hitch(this,function(d){
try{
this.editor.document.execCommand("insertBrOnReturn",false,true);
}
catch(e){
}
return d;
}));
}
}else{
if(this.blockNodeForEnter){
dojo["require"]("dijit._editor.range");
var h=dojo.hitch(this,this.handleEnterKey);
_2.addKeyHandler(13,0,0,h);
_2.addKeyHandler(13,0,1,h);
this.connect(this.editor,"onKeyPressed","onKeyPressed");
}
}
},connect:function(o,f,tf){
if(!this._connects){
this._connects=[];
}
this._connects.push(dojo.connect(o,f,this,tf));
},destroy:function(){
dojo.forEach(this._connects,dojo.disconnect);
this._connects=[];
},onKeyPressed:function(e){
if(this._checkListLater){
if(dojo.withGlobal(this.editor.window,"isCollapsed",dijit)){
var _9=dojo.withGlobal(this.editor.window,"getAncestorElement",dijit._editor.selection,["LI"]);
if(!_9){
dijit._editor.RichText.prototype.execCommand.call(this.editor,"formatblock",this.blockNodeForEnter);
var _a=dojo.withGlobal(this.editor.window,"getAncestorElement",dijit._editor.selection,[this.blockNodeForEnter]);
if(_a){
_a.innerHTML=this.bogusHtmlContent;
if(dojo.isIE){
var r=this.editor.document.selection.createRange();
r.move("character",-1);
r.select();
}
}else{
alert("onKeyPressed: Can not find the new block node");
}
}else{
if(dojo.isMoz){
if(_9.parentNode.parentNode.nodeName=="LI"){
_9=_9.parentNode.parentNode;
}
}
var fc=_9.firstChild;
if(fc&&fc.nodeType==1&&(fc.nodeName=="UL"||fc.nodeName=="OL")){
_9.insertBefore(fc.ownerDocument.createTextNode(" "),fc);
var _d=dijit.range.create();
_d.setStart(_9.firstChild,0);
var _e=dijit.range.getSelection(this.editor.window,true);
_e.removeAllRanges();
_e.addRange(_d);
}
}
}
this._checkListLater=false;
}
if(this._pressedEnterInBlock){
if(this._pressedEnterInBlock.previousSibling){
this.removeTrailingBr(this._pressedEnterInBlock.previousSibling);
}
delete this._pressedEnterInBlock;
}
},bogusHtmlContent:"&nbsp;",blockNodes:/^(?:P|H1|H2|H3|H4|H5|H6|LI)$/,handleEnterKey:function(e){
if(!this.blockNodeForEnter){
return true;
}
var _10,_11,_12,doc=this.editor.document,br;
if(e.shiftKey||this.blockNodeForEnter=="BR"){
var _15=dojo.withGlobal(this.editor.window,"getParentElement",dijit._editor.selection);
var _16=dijit.range.getAncestor(_15,this.blockNodes);
if(_16){
if(!e.shiftKey&&_16.tagName=="LI"){
return true;
}
_10=dijit.range.getSelection(this.editor.window);
_11=_10.getRangeAt(0);
if(!_11.collapsed){
_11.deleteContents();
}
if(dijit.range.atBeginningOfContainer(_16,_11.startContainer,_11.startOffset)){
if(e.shiftKey){
br=doc.createElement("br");
_12=dijit.range.create();
_16.insertBefore(br,_16.firstChild);
_12.setStartBefore(br.nextSibling);
_10.removeAllRanges();
_10.addRange(_12);
}else{
dojo.place(br,_16,"before");
}
}else{
if(dijit.range.atEndOfContainer(_16,_11.startContainer,_11.startOffset)){
_12=dijit.range.create();
br=doc.createElement("br");
if(e.shiftKey){
_16.appendChild(br);
_16.appendChild(doc.createTextNode(" "));
_12.setStart(_16.lastChild,0);
}else{
dojo.place(br,_16,"after");
_12.setStartAfter(_16);
}
_10.removeAllRanges();
_10.addRange(_12);
}else{
return true;
}
}
}else{
dijit._editor.RichText.prototype.execCommand.call(this.editor,"inserthtml","<br>");
}
return false;
}
var _17=true;
_10=dijit.range.getSelection(this.editor.window);
_11=_10.getRangeAt(0);
if(!_11.collapsed){
_11.deleteContents();
}
var _18=dijit.range.getBlockAncestor(_11.endContainer,null,this.editor.editNode);
var _19=_18.blockNode;
if((this._checkListLater=(_19&&(_19.nodeName=="LI"||_19.parentNode.nodeName=="LI")))){
if(dojo.isMoz){
this._pressedEnterInBlock=_19;
}
if(/^(?:\s|&nbsp;)$/.test(_19.innerHTML)){
_19.innerHTML="";
}
return true;
}
if(!_18.blockNode||_18.blockNode===this.editor.editNode){
dijit._editor.RichText.prototype.execCommand.call(this.editor,"formatblock",this.blockNodeForEnter);
_18={blockNode:dojo.withGlobal(this.editor.window,"getAncestorElement",dijit._editor.selection,[this.blockNodeForEnter]),blockContainer:this.editor.editNode};
if(_18.blockNode){
if(!(_18.blockNode.textContent||_18.blockNode.innerHTML).replace(/^\s+|\s+$/g,"").length){
this.removeTrailingBr(_18.blockNode);
return false;
}
}else{
_18.blockNode=this.editor.editNode;
}
_10=dijit.range.getSelection(this.editor.window);
_11=_10.getRangeAt(0);
}
var _1a=doc.createElement(this.blockNodeForEnter);
_1a.innerHTML=this.bogusHtmlContent;
this.removeTrailingBr(_18.blockNode);
if(dijit.range.atEndOfContainer(_18.blockNode,_11.endContainer,_11.endOffset)){
if(_18.blockNode===_18.blockContainer){
_18.blockNode.appendChild(_1a);
}else{
dojo.place(_1a,_18.blockNode,"after");
}
_17=false;
_12=dijit.range.create();
_12.setStart(_1a,0);
_10.removeAllRanges();
_10.addRange(_12);
if(this.editor.height){
_1a.scrollIntoView(false);
}
}else{
if(dijit.range.atBeginningOfContainer(_18.blockNode,_11.startContainer,_11.startOffset)){
dojo.place(_1a,_18.blockNode,_18.blockNode===_18.blockContainer?"first":"before");
if(_1a.nextSibling&&this.editor.height){
_1a.nextSibling.scrollIntoView(false);
}
_17=false;
}else{
if(dojo.isMoz){
this._pressedEnterInBlock=_18.blockNode;
}
}
}
return _17;
},removeTrailingBr:function(_1b){
var _1c=/P|DIV|LI/i.test(_1b.tagName)?_1b:dijit._editor.selection.getParentOfType(_1b,["P","DIV","LI"]);
if(!_1c){
return;
}
if(_1c.lastChild){
if((_1c.childNodes.length>1&&_1c.lastChild.nodeType==3&&/^[\s\xAD]*$/.test(_1c.lastChild.nodeValue))||(_1c.lastChild&&_1c.lastChild.tagName=="BR")){
dojo._destroyElement(_1c.lastChild);
}
}
if(!_1c.childNodes.length){
_1c.innerHTML=this.bogusHtmlContent;
}
},_fixNewLineBehaviorForIE:function(d){
if(this.editor.document.__INSERTED_EDITIOR_NEWLINE_CSS===undefined){
var _1e="p{margin:0 !important;}";
var _1f=function(_20,doc,URI){
if(!_20){
return null;
}
if(!doc){
doc=document;
}
var _23=doc.createElement("style");
_23.setAttribute("type","text/css");
var _24=doc.getElementsByTagName("head")[0];
if(!_24){

return null;
}else{
_24.appendChild(_23);
}
if(_23.styleSheet){
var _25=function(){
try{
_23.styleSheet.cssText=_20;
}
catch(e){

}
};
if(_23.styleSheet.disabled){
setTimeout(_25,10);
}else{
_25();
}
}else{
var _26=doc.createTextNode(_20);
_23.appendChild(_26);
}
return _23;
};
_1f(_1e,this.editor.document);
this.editor.document.__INSERTED_EDITIOR_NEWLINE_CSS=true;
return d;
}
return null;
},regularPsToSingleLinePs:function(_27,_28){
function wrapLinesInPs(el){
function wrapNodes(_2a){
var _2b=_2a[0].ownerDocument.createElement("p");
_2a[0].parentNode.insertBefore(_2b,_2a[0]);
dojo.forEach(_2a,function(_2c){
_2b.appendChild(_2c);
});
};
var _2d=0;
var _2e=[];
var _2f;
while(_2d<el.childNodes.length){
_2f=el.childNodes[_2d];
if(_2f.nodeType==3||(_2f.nodeType==1&&_2f.nodeName!="BR"&&dojo.style(_2f,"display")!="block")){
_2e.push(_2f);
}else{
var _30=_2f.nextSibling;
if(_2e.length){
wrapNodes(_2e);
_2d=(_2d+1)-_2e.length;
if(_2f.nodeName=="BR"){
dojo._destroyElement(_2f);
}
}
_2e=[];
}
_2d++;
}
if(_2e.length){
wrapNodes(_2e);
}
};
function splitP(el){
var _32=null;
var _33=[];
var _34=el.childNodes.length-1;
for(var i=_34;i>=0;i--){
_32=el.childNodes[i];
if(_32.nodeName=="BR"){
var _36=_32.ownerDocument.createElement("p");
dojo.place(_36,el,"after");
if(_33.length==0&&i!=_34){
_36.innerHTML="&nbsp;";
}
dojo.forEach(_33,function(_37){
_36.appendChild(_37);
});
dojo._destroyElement(_32);
_33=[];
}else{
_33.unshift(_32);
}
}
};
var _38=[];
var ps=_27.getElementsByTagName("p");
dojo.forEach(ps,function(p){
_38.push(p);
});
dojo.forEach(_38,function(p){
if((p.previousSibling)&&(p.previousSibling.nodeName=="P"||dojo.style(p.previousSibling,"display")!="block")){
var _3c=p.parentNode.insertBefore(this.document.createElement("p"),p);
_3c.innerHTML=_28?"":"&nbsp;";
}
splitP(p);
},this.editor);
wrapLinesInPs(_27);
return _27;
},singleLinePsToRegularPs:function(_3d){
function getParagraphParents(_3e){
var ps=_3e.getElementsByTagName("p");
var _40=[];
for(var i=0;i<ps.length;i++){
var p=ps[i];
var _43=false;
for(var k=0;k<_40.length;k++){
if(_40[k]===p.parentNode){
_43=true;
break;
}
}
if(!_43){
_40.push(p.parentNode);
}
}
return _40;
};
function isParagraphDelimiter(_45){
if(_45.nodeType!=1||_45.tagName!="P"){
return dojo.style(_45,"display")=="block";
}else{
if(!_45.childNodes.length||_45.innerHTML=="&nbsp;"){
return true;
}
}
return false;
};
var _46=getParagraphParents(_3d);
for(var i=0;i<_46.length;i++){
var _48=_46[i];
var _49=null;
var _4a=_48.firstChild;
var _4b=null;
while(_4a){
if(_4a.nodeType!="1"||_4a.tagName!="P"){
_49=null;
}else{
if(isParagraphDelimiter(_4a)){
_4b=_4a;
_49=null;
}else{
if(_49==null){
_49=_4a;
}else{
if((!_49.lastChild||_49.lastChild.nodeName!="BR")&&(_4a.firstChild)&&(_4a.firstChild.nodeName!="BR")){
_49.appendChild(this.editor.document.createElement("br"));
}
while(_4a.firstChild){
_49.appendChild(_4a.firstChild);
}
_4b=_4a;
}
}
}
_4a=_4a.nextSibling;
if(_4b){
dojo._destroyElement(_4b);
_4b=null;
}
}
}
return _3d;
}});
}
