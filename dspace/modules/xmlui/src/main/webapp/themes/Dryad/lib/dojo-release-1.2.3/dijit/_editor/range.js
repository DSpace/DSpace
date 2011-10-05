/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._editor.range"]){
dojo._hasResource["dijit._editor.range"]=true;
dojo.provide("dijit._editor.range");
dijit.range={};
dijit.range.getIndex=function(_1,_2){
var _3=[],_4=[];
var _5=_2;
var _6=_1;
var _7,n;
while(_1!=_5){
var i=0;
_7=_1.parentNode;
while((n=_7.childNodes[i++])){
if(n===_1){
--i;
break;
}
}
if(i>=_7.childNodes.length){
dojo.debug("Error finding index of a node in dijit.range.getIndex");
}
_3.unshift(i);
_4.unshift(i-_7.childNodes.length);
_1=_7;
}
if(_3.length>0&&_6.nodeType==3){
n=_6.previousSibling;
while(n&&n.nodeType==3){
_3[_3.length-1]--;
n=n.previousSibling;
}
n=_6.nextSibling;
while(n&&n.nodeType==3){
_4[_4.length-1]++;
n=n.nextSibling;
}
}
return {o:_3,r:_4};
};
dijit.range.getNode=function(_a,_b){
if(!dojo.isArray(_a)||_a.length==0){
return _b;
}
var _c=_b;
dojo.every(_a,function(i){
if(i>=0&&i<_c.childNodes.length){
_c=_c.childNodes[i];
}else{
_c=null;

return false;
}
return true;
});
return _c;
};
dijit.range.getCommonAncestor=function(n1,n2){
var _10=function(n){
var as=[];
while(n){
as.unshift(n);
if(n.nodeName!="BODY"){
n=n.parentNode;
}else{
break;
}
}
return as;
};
var _13=_10(n1);
var _14=_10(n2);
var m=Math.min(_13.length,_14.length);
var com=_13[0];
for(var i=1;i<m;i++){
if(_13[i]===_14[i]){
com=_13[i];
}else{
break;
}
}
return com;
};
dijit.range.getAncestor=function(_18,_19,_1a){
_1a=_1a||_18.ownerDocument.body;
while(_18&&_18!==_1a){
var _1b=_18.nodeName.toUpperCase();
if(_19.test(_1b)){
return _18;
}
_18=_18.parentNode;
}
return null;
};
dijit.range.BlockTagNames=/^(?:P|DIV|H1|H2|H3|H4|H5|H6|ADDRESS|PRE|OL|UL|LI|DT|DE)$/;
dijit.range.getBlockAncestor=function(_1c,_1d,_1e){
_1e=_1e||_1c.ownerDocument.body;
_1d=_1d||dijit.range.BlockTagNames;
var _1f=null,_20;
while(_1c&&_1c!==_1e){
var _21=_1c.nodeName.toUpperCase();
if(!_1f&&_1d.test(_21)){
_1f=_1c;
}
if(!_20&&(/^(?:BODY|TD|TH|CAPTION)$/).test(_21)){
_20=_1c;
}
_1c=_1c.parentNode;
}
return {blockNode:_1f,blockContainer:_20||_1c.ownerDocument.body};
};
dijit.range.atBeginningOfContainer=function(_22,_23,_24){
var _25=false;
var _26=(_24==0);
if(!_26&&_23.nodeType==3){
if(dojo.trim(_23.nodeValue.substr(0,_24))==0){
_26=true;
}
}
if(_26){
var _27=_23;
_25=true;
while(_27&&_27!==_22){
if(_27.previousSibling){
_25=false;
break;
}
_27=_27.parentNode;
}
}
return _25;
};
dijit.range.atEndOfContainer=function(_28,_29,_2a){
var _2b=false;
var _2c=(_2a==(_29.length||_29.childNodes.length));
if(!_2c&&_29.nodeType==3){
if(dojo.trim(_29.nodeValue.substr(_2a))==0){
_2c=true;
}
}
if(_2c){
var _2d=_29;
_2b=true;
while(_2d&&_2d!==_28){
if(_2d.nextSibling){
_2b=false;
break;
}
_2d=_2d.parentNode;
}
}
return _2b;
};
dijit.range.adjacentNoneTextNode=function(_2e,_2f){
var _30=_2e;
var len=(0-_2e.length)||0;
var _32=_2f?"nextSibling":"previousSibling";
while(_30){
if(_30.nodeType!=3){
break;
}
len+=_30.length;
_30=_30[_32];
}
return [_30,len];
};
dijit.range._w3c=Boolean(window["getSelection"]);
dijit.range.create=function(){
if(dijit.range._w3c){
return dojo.doc.createRange();
}else{
return new dijit.range.W3CRange;
}
};
dijit.range.getSelection=function(win,_34){
if(dijit.range._w3c){
return win.getSelection();
}else{
var s=new dijit.range.ie.selection(win);
if(!_34){
s._getCurrentSelection();
}
return s;
}
};
if(!dijit.range._w3c){
dijit.range.ie={cachedSelection:{},selection:function(win){
this._ranges=[];
this.addRange=function(r,_38){
this._ranges.push(r);
if(!_38){
r._select();
}
this.rangeCount=this._ranges.length;
};
this.removeAllRanges=function(){
this._ranges=[];
this.rangeCount=0;
};
var _39=function(){
var r=win.document.selection.createRange();
var _3b=win.document.selection.type.toUpperCase();
if(_3b=="CONTROL"){
return new dijit.range.W3CRange(dijit.range.ie.decomposeControlRange(r));
}else{
return new dijit.range.W3CRange(dijit.range.ie.decomposeTextRange(r));
}
};
this.getRangeAt=function(i){
return this._ranges[i];
};
this._getCurrentSelection=function(){
this.removeAllRanges();
var r=_39();
if(r){
this.addRange(r,true);
}
};
},decomposeControlRange:function(_3e){
var _3f=_3e.item(0),_40=_3e.item(_3e.length-1);
var _41=_3f.parentNode,_42=_40.parentNode;
var _43=dijit.range.getIndex(_3f,_41).o;
var _44=dijit.range.getIndex(_40,_42).o+1;
return [[_41,_43],[_42,_44]];
},getEndPoint:function(_45,end){
var _47=_45.duplicate();
_47.collapse(!end);
var _48="EndTo"+(end?"End":"Start");
var _49=_47.parentElement();
var _4a,_4b,_4c;
if(_49.childNodes.length>0){
dojo.every(_49.childNodes,function(_4d,i){
var _4f;
if(_4d.nodeType!=3){
_47.moveToElementText(_4d);
if(_47.compareEndPoints(_48,_45)>0){
_4a=_4d.previousSibling;
if(_4c&&_4c.nodeType==3){
_4a=_4c;
_4f=true;
}else{
_4a=_49;
_4b=i;
return false;
}
}else{
if(i==_49.childNodes.length-1){
_4a=_49;
_4b=_49.childNodes.length;
return false;
}
}
}else{
if(i==_49.childNodes.length-1){
_4a=_4d;
_4f=true;
}
}
if(_4f&&_4a){
var _50=dijit.range.adjacentNoneTextNode(_4a)[0];
if(_50){
_4a=_50.nextSibling;
}else{
_4a=_49.firstChild;
}
var _51=dijit.range.adjacentNoneTextNode(_4a);
_50=_51[0];
var _52=_51[1];
if(_50){
_47.moveToElementText(_50);
_47.collapse(false);
}else{
_47.moveToElementText(_49);
}
_47.setEndPoint(_48,_45);
_4b=_47.text.length-_52;
return false;
}
_4c=_4d;
return true;
});
}else{
_4a=_49;
_4b=0;
}
if(!end&&_4a.nodeType!=3&&_4b==_4a.childNodes.length){
if(_4a.nextSibling&&_4a.nextSibling.nodeType==3){
_4a=_4a.nextSibling;
_4b=0;
}
}
return [_4a,_4b];
},setEndPoint:function(_53,_54,_55){
var _56=_53.duplicate(),_57,len;
if(_54.nodeType!=3){
_56.moveToElementText(_54);
_56.collapse(true);
if(_55==_54.childNodes.length){
if(_55>0){
_57=_54.lastChild;
len=0;
while(_57&&_57.nodeType==3){
len+=_57.length;
_54=_57;
_57=_57.previousSibling;
}
if(_57){
_56.moveToElementText(_57);
}
_56.collapse(false);
_55=len;
}else{
_56.moveToElementText(_54);
_56.collapse(true);
}
}else{
if(_55>0){
_57=_54.childNodes[_55-1];
if(_57.nodeType==3){
_54=_57;
_55=_57.length;
}else{
_56.moveToElementText(_57);
_56.collapse(false);
}
}
}
}
if(_54.nodeType==3){
var _59=dijit.range.adjacentNoneTextNode(_54);
var _5a=_59[0];
len=_59[1];
if(_5a){
_56.moveToElementText(_5a);
_56.collapse(false);
if(_5a.contentEditable!="inherit"){
len++;
}
}else{
_56.moveToElementText(_54.parentNode);
_56.collapse(true);
}
_55+=len;
if(_55>0){
if(_56.move("character",_55)!=_55){
console.error("Error when moving!");
}
}
}
return _56;
},decomposeTextRange:function(_5b){
var _5c=dijit.range.ie.getEndPoint(_5b);
var _5d=_5c[0],_5e=_5c[1];
var _5f=_5c[0],_60=_5c[1];
if(_5b.htmlText.length){
if(_5b.htmlText==_5b.text){
_60=_5e+_5b.text.length;
}else{
_5c=dijit.range.ie.getEndPoint(_5b,true);
_5f=_5c[0],_60=_5c[1];
}
}
return [[_5d,_5e],[_5f,_60]];
},setRange:function(_61,_62,_63,_64,_65,_66){
var _67=dijit.range.ie.setEndPoint(_61,_62,_63);
_61.setEndPoint("StartToStart",_67);
var _68=_67;
if(!_66){
_68=dijit.range.ie.setEndPoint(_61,_64,_65);
}
_61.setEndPoint("EndToEnd",_68);
return _61;
}};
dojo.declare("dijit.range.W3CRange",null,{constructor:function(){
if(arguments.length>0){
this.setStart(arguments[0][0][0],arguments[0][0][1]);
this.setEnd(arguments[0][1][0],arguments[0][1][1]);
}else{
this.commonAncestorContainer=null;
this.startContainer=null;
this.startOffset=0;
this.endContainer=null;
this.endOffset=0;
this.collapsed=true;
}
},_simpleSetEndPoint:function(_69,_6a,end){
var r=(this._body||_69.ownerDocument.body).createTextRange();
if(_69.nodeType!=1){
r.moveToElementText(_69.parentNode);
}else{
r.moveToElementText(_69);
}
r.collapse(true);
_6a.setEndPoint(end?"EndToEnd":"StartToStart",r);
},_updateInternal:function(){
if(this.startContainer!==this.endContainer){
this.commonAncestorContainer=dijit.range.getCommonAncestor(this.startContainer,this.endContainer);
}else{
this.commonAncestorContainer=this.startContainer;
}
this.collapsed=(this.startContainer===this.endContainer)&&(this.startOffset==this.endOffset);
},setStart:function(_6d,_6e){
_6e=parseInt(_6e);
if(this.startContainer===_6d&&this.startOffset==_6e){
return;
}
delete this._cachedBookmark;
this.startContainer=_6d;
this.startOffset=_6e;
if(!this.endContainer){
this.setEnd(_6d,_6e);
}else{
this._updateInternal();
}
},setEnd:function(_6f,_70){
_70=parseInt(_70);
if(this.endContainer===_6f&&this.endOffset==_70){
return;
}
delete this._cachedBookmark;
this.endContainer=_6f;
this.endOffset=_70;
if(!this.startContainer){
this.setStart(_6f,_70);
}else{
this._updateInternal();
}
},setStartAfter:function(_71,_72){
this._setPoint("setStart",_71,_72,1);
},setStartBefore:function(_73,_74){
this._setPoint("setStart",_73,_74,0);
},setEndAfter:function(_75,_76){
this._setPoint("setEnd",_75,_76,1);
},setEndBefore:function(_77,_78){
this._setPoint("setEnd",_77,_78,0);
},_setPoint:function(_79,_7a,_7b,ext){
var _7d=dijit.range.getIndex(_7a,_7a.parentNode).o;
this[_79](_7a.parentNode,_7d.pop()+ext);
},_getIERange:function(){
var r=(this._body||this.endContainer.ownerDocument.body).createTextRange();
dijit.range.ie.setRange(r,this.startContainer,this.startOffset,this.endContainer,this.endOffset,this.collapsed);
return r;
},getBookmark:function(_7f){
this._getIERange();
return this._cachedBookmark;
},_select:function(){
var r=this._getIERange();
r.select();
},deleteContents:function(){
var r=this._getIERange();
r.pasteHTML("");
this.endContainer=this.startContainer;
this.endOffset=this.startOffset;
this.collapsed=true;
},cloneRange:function(){
var r=new dijit.range.W3CRange([[this.startContainer,this.startOffset],[this.endContainer,this.endOffset]]);
r._body=this._body;
return r;
},detach:function(){
this._body=null;
this.commonAncestorContainer=null;
this.startContainer=null;
this.startOffset=0;
this.endContainer=null;
this.endOffset=0;
this.collapsed=true;
}});
}
}
