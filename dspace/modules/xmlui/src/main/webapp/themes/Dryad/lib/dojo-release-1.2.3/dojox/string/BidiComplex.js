/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.string.BidiComplex"]){
dojo._hasResource["dojox.string.BidiComplex"]=true;
dojo.provide("dojox.string.BidiComplex");
dojo.experimental("dojox.string.BidiComplex");
dojox.string.BidiComplex.attachInput=function(_1,_2){
dojox.string.BidiComplex._ce_type=_2;
_1.alt=dojox.string.BidiComplex._ce_type;
if((document.dir=="rtl")||(document.body.dir=="rtl")){
_1.style.textAlign="right";
}
if(dojo.isIE){
_1.onkeydown=new Function("dojox.string.BidiComplex._ceKeyDown(event);");
_1.onkeyup=new Function("dojox.string.BidiComplex._ceKeyUp(event);");
}else{
_1.onkeyup=dojox.string.BidiComplex._ceKeyUp;
_1.onkeydown=dojox.string.BidiComplex._ceKeyDown;
}
_1.oncut=dojox.string.BidiComplex._fOnCut;
_1.oncopy=dojox.string.BidiComplex._fOnCopy;
_1.value=dojox.string.BidiComplex._insertMarkers(_1.value,_1.alt);
};
dojox.string.BidiComplex.createDisplayString=function(_3,_4){
return dojox.string.BidiComplex._insertMarkers(_3,_4);
};
dojox.string.BidiComplex.stripSpecialCharacters=function(_5){
return _5.replace(/[\u200E\u200F\u202A-\u202E]/g,"");
};
dojox.string.BidiComplex._segmentsPointers=[];
dojox.string.BidiComplex._ce_type=null;
dojox.string.BidiComplex._PATH=null;
dojox.string.BidiComplex._insertAlways=false;
dojox.string.BidiComplex._fOnCut=new Function("dojox.string.BidiComplex._ceCutText(this)");
dojox.string.BidiComplex._fOnCopy=new Function("dojox.string.BidiComplex._ceCopyText(this);");
dojox.string.BidiComplex._ceKeyDown=function(_6){
obj=dojo.isIE?_6.srcElement:_6.target;
str0=obj.value;
};
dojox.string.BidiComplex._ceKeyUp=function(_7){
var _8="‎";
obj=dojo.isIE?_7.srcElement:_7.target;
str1=obj.value;
if(obj.alt!=""){
dojox.string.BidiComplex._ce_type=obj.alt;
}
ieKey=_7.keyCode;
if((ieKey==dojo.keys.HOME)||(ieKey==dojo.keys.END)||(ieKey==dojo.keys.SHIFT)){
return;
}
var _9,_a;
var _b=dojox.string.BidiComplex._getCaretPos(_7,obj);
if(_b){
_9=_b[0];
_a=_b[1];
}
if(dojo.isIE){
var _c=_9,_d=_a;
if(ieKey==dojo.keys.LEFT_ARROW){
if((str1.charAt(_a-1)==_8)&&(_9==_a)){
dojox.string.BidiComplex._setSelectedRange(obj,_9-1,_a-1);
}
return;
}
if(ieKey==dojo.keys.RIGHT_ARROW){
if(str1.charAt(_a-1)==_8){
_d=_a+1;
if(_9==_a){
_c=_9+1;
}
}
dojox.string.BidiComplex._setSelectedRange(obj,_c,_d);
return;
}
}else{
if(ieKey==dojo.keys.LEFT_ARROW){
if(str1.charAt(_a-1)==_8){
dojox.string.BidiComplex._setSelectedRange(obj,_9-1,_a-1);
}
return;
}
if(ieKey==dojo.keys.RIGHT_ARROW){
if(str1.charAt(_a-1)==_8){
dojox.string.BidiComplex._setSelectedRange(obj,_9+1,_a+1);
}
return;
}
}
str2=dojox.string.BidiComplex._insertMarkers(str1,obj.alt);
if(str1!=str2){
window.status=str1+" c="+_a;
obj.value=str2;
if((ieKey==dojo.keys.DELETE)&&(str2.charAt(_a)==_8)){
obj.value=str2.substring(0,_a)+str2.substring(_a+2,str2.length);
}
if(ieKey==dojo.keys.DELETE){
setSelectedRange(obj,_9,_a);
}else{
if(ieKey==dojo.keys.BACKSPACE){
if(str0.charAt(_a-1)==_8){
dojox.string.BidiComplex._setSelectedRange(obj,_9-1,_a-1);
}else{
dojox.string.BidiComplex._setSelectedRange(obj,_9,_a);
}
}else{
if(obj.value.charAt(_a)!=_8){
dojox.string.BidiComplex._setSelectedRange(obj,_9+1,_a+1);
}
}
}
}
};
dojox.string.BidiComplex._processCopy=function(_e,_f,_10){
if(_f==null){
if(dojo.isIE){
range=document.selection.createRange();
_f=range.text;
}else{
_f=_e.value.substring(_e.selectionStart,_e.selectionEnd);
}
}
var _11=dojox.string.BidiComplex.stripSpecialCharacters(_f);
if(dojo.isIE){
window.clipboardData.setData("Text",_11);
return true;
}else{
try{
return window.SignedJs.processCopy(_11);
}
catch(e){
return false;
}
}
};
dojox.string.BidiComplex._ceCopyText=function(obj){
if(dojo.isIE){
event.returnValue=false;
}
return dojox.string.BidiComplex._processCopy(obj,null,false);
};
dojox.string.BidiComplex._ceCutText=function(obj){
var ret=dojox.string.BidiComplex._processCopy(obj,null,false);
if(!ret){
return false;
}
if(dojo.isIE){
range=document.selection.clear();
}else{
var _15=obj.selectionStart;
obj.value=obj.value.substring(0,_15)+obj.value.substring(obj.selectionEnd);
obj.setSelectionRange(_15,_15);
}
return true;
};
dojox.string.BidiComplex._getCaretPos=function(_16,obj){
if(dojo.isIE){
var _18=0,_19=document.selection.createRange().duplicate(),_1a=_19.duplicate(),_1b=_19.text.length;
if(obj.type=="textarea"){
_1a.moveToElementText(obj);
}else{
_1a.expand("textedit");
}
while(_19.compareEndPoints("StartToStart",_1a)>0){
_19.moveStart("character",-1);
++_18;
}
return [_18,_18+_1b];
}
return [_16.target.selectionStart,_16.target.selectionEnd];
};
dojox.string.BidiComplex._setSelectedRange=function(obj,_1d,_1e){
if(dojo.isIE){
var _1f=obj.createTextRange();
if(_1f){
if(obj.type=="textarea"){
_1f.moveToElementText(obj);
}else{
_1f.expand("textedit");
}
_1f.collapse();
_1f.moveEnd("character",_1e);
_1f.moveStart("character",_1d);
_1f.select();
}
}else{
obj.selectionStart=_1d;
obj.selectionEnd=_1e;
}
};
dojox.string.BidiComplex._isBidiChar=function(c){
if(c>="0"&&c<="9"){
return true;
}
return c>"ÿ";
};
dojox.string.BidiComplex._isLatinChar=function(c){
return (c>="A"&&c<="Z")||(c>="a"&&c<="z");
};
dojox.string.BidiComplex._isCharBeforeBiDiChar=function(_22,i,_24){
if(dojox.string.BidiComplex._insertAlways){
return true;
}
while(i>0){
if(i==_24){
return false;
}
i--;
if(dojox.string.BidiComplex._isBidiChar(_22.charAt(i))){
return true;
}
if(dojox.string.BidiComplex._isLatinChar(_22.charAt(i))){
return false;
}
}
return false;
};
dojox.string.BidiComplex._parse=function(str,_26){
var i,i1;
var _29;
var _2a=-1;
if(dojox.string.BidiComplex._segmentsPointers!=null){
for(i=0;i<dojox.string.BidiComplex._segmentsPointers.length;i++){
dojox.string.BidiComplex._segmentsPointers[i]=null;
}
}
var _2b=0;
if(_26=="FILE_PATH"){
_29="/\\:.";
for(i=0;i<str.length;i++){
if((_29.indexOf(str.charAt(i))>=0)&&dojox.string.BidiComplex._isCharBeforeBiDiChar(str,i,_2a)){
_2a=i;
dojox.string.BidiComplex._segmentsPointers[_2b++]=i;
}
}
}else{
if(_26=="URL"){
var _2c=str.length;
_29="/:.?=&#";
for(i=0;i<_2c;i++){
if((_29.indexOf(str.charAt(i))>=0)&&dojox.string.BidiComplex._isCharBeforeBiDiChar(str,i,_2a)){
_2a=i;
dojox.string.BidiComplex._segmentsPointers[_2b]=i;
_2b++;
}
}
}else{
if(_26=="EMAIL"){
_29="<>@.,;";
var _2d=false;
for(i=0;i<str.length;i++){
if(str.charAt(i)=="\""){
if(dojox.string.BidiComplex._isCharBeforeBiDiChar(str,i,_2a)){
_2a=i;
dojox.string.BidiComplex._segmentsPointers[_2b]=i;
_2b++;
}
i++;
i1=str.indexOf("\"",i);
if(i1>=i){
i=i1;
}
if(dojox.string.BidiComplex._isCharBeforeBiDiChar(str,i,_2a)){
_2a=i;
dojox.string.BidiComplex._segmentsPointers[_2b]=i;
_2b++;
}
}
if((_29.indexOf(str.charAt(i))>=0)&&dojox.string.BidiComplex._isCharBeforeBiDiChar(str,i,_2a)){
_2a=i;
dojox.string.BidiComplex._segmentsPointers[_2b]=i;
_2b++;
}
}
}else{
if(_26=="XPATH"){
_29="/\\:.<>=[]";
for(i=0;i<str.length;i++){
if((_29.indexOf(str.charAt(i))>=0)&&dojox.string.BidiComplex._isCharBeforeBiDiChar(str,i,_2a)){
_2a=i;
dojox.string.BidiComplex._segmentsPointers[_2b]=i;
_2b++;
}
}
}
}
}
}
return dojox.string.BidiComplex._segmentsPointers;
};
dojox.string.BidiComplex._insertMarkers=function(str,_2f){
str=dojox.string.BidiComplex.stripSpecialCharacters(str);
dojox.string.BidiComplex._segmentsPointers=dojox.string.BidiComplex._parse(str,_2f);
var buf="‪"+str;
var _31=1;
var n;
for(i=0;i<dojox.string.BidiComplex._segmentsPointers.length;i++){
n=dojox.string.BidiComplex._segmentsPointers[i];
if(n!=null){
preStr=buf.substring(0,n+_31);
postStr=buf.substring(n+_31,buf.length);
buf=preStr+"‎"+postStr;
_31++;
}
}
return buf;
};
}
