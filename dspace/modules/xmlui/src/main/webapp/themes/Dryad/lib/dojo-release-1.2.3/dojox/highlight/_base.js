/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.highlight._base"]){
dojo._hasResource["dojox.highlight._base"]=true;
dojo.provide("dojox.highlight._base");
(function(){
var dh=dojox.highlight,_2="\\b(0x[A-Za-z0-9]+|\\d+(\\.\\d+)?)";
dh.constants={IDENT_RE:"[a-zA-Z][a-zA-Z0-9_]*",UNDERSCORE_IDENT_RE:"[a-zA-Z_][a-zA-Z0-9_]*",NUMBER_RE:"\\b\\d+(\\.\\d+)?",C_NUMBER_RE:_2,APOS_STRING_MODE:{className:"string",begin:"'",end:"'",illegal:"\\n",contains:["escape"],relevance:0},QUOTE_STRING_MODE:{className:"string",begin:"\"",end:"\"",illegal:"\\n",contains:["escape"],relevance:0},BACKSLASH_ESCAPE:{className:"escape",begin:"\\\\.",end:"^",relevance:0},C_LINE_COMMENT_MODE:{className:"comment",begin:"//",end:"$",relevance:0},C_BLOCK_COMMENT_MODE:{className:"comment",begin:"/\\*",end:"\\*/"},HASH_COMMENT_MODE:{className:"comment",begin:"#",end:"$"},C_NUMBER_MODE:{className:"number",begin:_2,end:"^",relevance:0}};
function esc(_3){
return _3.replace(/&/gm,"&amp;").replace(/</gm,"&lt;").replace(/>/gm,"&gt;");
};
function verifyText(_4){
return dojo.every(_4.childNodes,function(_5){
return _5.nodeType==3||String(_5.nodeName).toLowerCase()=="br";
});
};
function blockText(_6){
var _7=[];
dojo.forEach(_6.childNodes,function(_8){
if(_8.nodeType==3){
_7.push(_8.nodeValue);
}else{
if(String(_8.nodeName).toLowerCase()=="br"){
_7.push("\n");
}else{
throw "Complex markup";
}
}
});
return _7.join("");
};
function buildKeywordGroups(_9){
if(!_9.keywordGroups){
for(var _a in _9.keywords){
var kw=_9.keywords[_a];
if(kw instanceof Object){
_9.keywordGroups=_9.keywords;
}else{
_9.keywordGroups={keyword:_9.keywords};
}
break;
}
}
};
function buildKeywords(_c){
if(_c.defaultMode&&_c.modes){
buildKeywordGroups(_c.defaultMode);
dojo.forEach(_c.modes,buildKeywordGroups);
}
};
var _d=function(_e,_f){
this.langName=_e;
this.lang=dh.languages[_e];
this.modes=[this.lang.defaultMode];
this.relevance=0;
this.keywordCount=0;
this.result=[];
if(!this.lang.defaultMode.illegalRe){
this.buildRes();
buildKeywords(this.lang);
}
try{
this.highlight(_f);
this.result=this.result.join("");
}
catch(e){
if(e=="Illegal"){
this.relevance=0;
this.keywordCount=0;
this.partialResult=this.result.join("");
this.result=esc(_f);
}else{
throw e;
}
}
};
dojo.extend(_d,{buildRes:function(){
dojo.forEach(this.lang.modes,function(_10){
if(_10.begin){
_10.beginRe=this.langRe("^"+_10.begin);
}
if(_10.end){
_10.endRe=this.langRe("^"+_10.end);
}
if(_10.illegal){
_10.illegalRe=this.langRe("^(?:"+_10.illegal+")");
}
},this);
this.lang.defaultMode.illegalRe=this.langRe("^(?:"+this.lang.defaultMode.illegal+")");
},subMode:function(_11){
var _12=this.modes[this.modes.length-1].contains;
if(_12){
var _13=this.lang.modes;
for(var i=0;i<_12.length;++i){
var _15=_12[i];
for(var j=0;j<_13.length;++j){
var _17=_13[j];
if(_17.className==_15&&_17.beginRe.test(_11)){
return _17;
}
}
}
}
return null;
},endOfMode:function(_18){
for(var i=this.modes.length-1;i>=0;--i){
var _1a=this.modes[i];
if(_1a.end&&_1a.endRe.test(_18)){
return this.modes.length-i;
}
if(!_1a.endsWithParent){
break;
}
}
return 0;
},isIllegal:function(_1b){
var _1c=this.modes[this.modes.length-1].illegalRe;
return _1c&&_1c.test(_1b);
},langRe:function(_1d,_1e){
var _1f="m"+(this.lang.case_insensitive?"i":"")+(_1e?"g":"");
return new RegExp(_1d,_1f);
},buildTerminators:function(){
var _20=this.modes[this.modes.length-1],_21={};
if(_20.contains){
dojo.forEach(this.lang.modes,function(_22){
if(dojo.indexOf(_20.contains,_22.className)>=0){
_21[_22.begin]=1;
}
});
}
for(var i=this.modes.length-1;i>=0;--i){
var m=this.modes[i];
if(m.end){
_21[m.end]=1;
}
if(!m.endsWithParent){
break;
}
}
if(_20.illegal){
_21[_20.illegal]=1;
}
var t=[];
for(i in _21){
t.push(i);
}
_20.terminatorsRe=this.langRe("("+t.join("|")+")");
},eatModeChunk:function(_26,_27){
var _28=this.modes[this.modes.length-1];
if(!_28.terminatorsRe){
this.buildTerminators();
}
_26=_26.substr(_27);
var _29=_28.terminatorsRe.exec(_26);
if(!_29){
return {buffer:_26,lexeme:"",end:true};
}
return {buffer:_29.index?_26.substr(0,_29.index):"",lexeme:_29[0],end:false};
},keywordMatch:function(_2a,_2b){
var _2c=_2b[0];
if(this.lang.case_insensitive){
_2c=_2c.toLowerCase();
}
for(var _2d in _2a.keywordGroups){
if(_2c in _2a.keywordGroups[_2d]){
return _2d;
}
}
return "";
},buildLexemes:function(_2e){
var _2f={};
dojo.forEach(_2e.lexems,function(_30){
_2f[_30]=1;
});
var t=[];
for(var i in _2f){
t.push(i);
}
_2e.lexemsRe=this.langRe("("+t.join("|")+")",true);
},processKeywords:function(_33){
var _34=this.modes[this.modes.length-1];
if(!_34.keywords||!_34.lexems){
return esc(_33);
}
if(!_34.lexemsRe){
this.buildLexemes(_34);
}
_34.lexemsRe.lastIndex=0;
var _35=[],_36=0,_37=_34.lexemsRe.exec(_33);
while(_37){
_35.push(esc(_33.substr(_36,_37.index-_36)));
var _38=this.keywordMatch(_34,_37);
if(_38){
++this.keywordCount;
_35.push("<span class=\""+_38+"\">"+esc(_37[0])+"</span>");
}else{
_35.push(esc(_37[0]));
}
_36=_34.lexemsRe.lastIndex;
_37=_34.lexemsRe.exec(_33);
}
_35.push(esc(_33.substr(_36,_33.length-_36)));
return _35.join("");
},processModeInfo:function(_39,_3a,end){
var _3c=this.modes[this.modes.length-1];
if(end){
this.result.push(this.processKeywords(_3c.buffer+_39));
return;
}
if(this.isIllegal(_3a)){
throw "Illegal";
}
var _3d=this.subMode(_3a);
if(_3d){
_3c.buffer+=_39;
this.result.push(this.processKeywords(_3c.buffer));
if(_3d.excludeBegin){
this.result.push(_3a+"<span class=\""+_3d.className+"\">");
_3d.buffer="";
}else{
this.result.push("<span class=\""+_3d.className+"\">");
_3d.buffer=_3a;
}
this.modes.push(_3d);
this.relevance+=typeof _3d.relevance=="number"?_3d.relevance:1;
return;
}
var _3e=this.endOfMode(_3a);
if(_3e){
_3c.buffer+=_39;
if(_3c.excludeEnd){
this.result.push(this.processKeywords(_3c.buffer)+"</span>"+_3a);
}else{
this.result.push(this.processKeywords(_3c.buffer+_3a)+"</span>");
}
while(_3e>1){
this.result.push("</span>");
--_3e;
this.modes.pop();
}
this.modes.pop();
this.modes[this.modes.length-1].buffer="";
return;
}
},highlight:function(_3f){
var _40=0;
this.lang.defaultMode.buffer="";
do{
var _41=this.eatModeChunk(_3f,_40);
this.processModeInfo(_41.buffer,_41.lexeme,_41.end);
_40+=_41.buffer.length+_41.lexeme.length;
}while(!_41.end);
if(this.modes.length>1){
throw "Illegal";
}
}});
function replaceText(_42,_43,_44){
if(String(_42.tagName).toLowerCase()=="code"&&String(_42.parentNode.tagName).toLowerCase()=="pre"){
var _45=document.createElement("div"),_46=_42.parentNode.parentNode;
_45.innerHTML="<pre><code class=\""+_43+"\">"+_44+"</code></pre>";
_46.replaceChild(_45.firstChild,_42.parentNode);
}else{
_42.className=_43;
_42.innerHTML=_44;
}
};
function highlightStringLanguage(_47,str){
var _49=new _d(_47,str);
return {result:_49.result,langName:_47,partialResult:_49.partialResult};
};
function highlightLanguage(_4a,_4b){
var _4c=highlightStringLanguage(_4b,blockText(_4a));
replaceText(_4a,_4a.className,_4c.result);
};
function highlightStringAuto(str){
var _4e="",_4f="",_50=2,_51=str;
for(var key in dh.languages){
if(!dh.languages[key].defaultMode){
continue;
}
var _53=new _d(key,_51),_54=_53.keywordCount+_53.relevance,_55=0;
if(!_4e||_54>_55){
_55=_54;
_4e=_53.result;
_4f=_53.langName;
}
}
return {result:_4e,langName:_4f};
};
function highlightAuto(_56){
var _57=highlightStringAuto(blockText(_56));
if(_57.result){
replaceText(_56,_57.langName,_57.result);
}
};
dh.processString=function(str,_59){
return _59?highlightStringLanguage(_59,str):highlightStringAuto(str);
};
dh.init=function(_5a){
if(dojo.hasClass(_5a,"no-highlight")){
return;
}
if(!verifyText(_5a)){
return;
}
var _5b=_5a.className.split(/\s+/),_5c=dojo.some(_5b,function(_5d){
if(_5d.charAt(0)!="_"&&dh.languages[_5d]){
highlightLanguage(_5a,_5d);
return true;
}
return false;
});
if(!_5c){
highlightAuto(_5a);
}
};
dh.Code=function(_5e,_5f){
dh.init(_5f);
};
})();
}
