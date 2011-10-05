/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.fx.text"]){
dojo._hasResource["dojox.fx.text"]=true;
dojo.provide("dojox.fx.text");
dojo.require("dojo.fx");
dojo.require("dojo.fx.easing");
dojox.fx.text._split=function(_1){
var _2=_1.node=dojo.byId(_1.node),s=_2.style,cs=dojo.getComputedStyle(_2),_5=dojo.coords(_2,true);
_1.duration=_1.duration||1000;
_1.words=_1.words||false;
var _6=(_1.text&&typeof (_1.text)=="string")?_1.text:_2.innerHTML,_7=s.height,_8=s.width,_9=[];
dojo.style(_2,{height:cs.height,width:cs.width});
var _a=/(<\/?\w+((\s+\w+(\s*=\s*(?:".*?"|'.*?'|[^'">\s]+))?)+\s*|\s*)\/?>)/g;
var _b=(_1.words?/(<\/?\w+((\s+\w+(\s*=\s*(?:".*?"|'.*?'|[^'">\s]+))?)+\s*|\s*)\/?>)\s*|([^\s<]+\s*)/g:/(<\/?\w+((\s+\w+(\s*=\s*(?:".*?"|'.*?'|[^'">\s]+))?)+\s*|\s*)\/?>)\s*|([^\s<]\s*)/g);
var _c=(typeof _1.text=="string")?_1.text.match(_b):_2.innerHTML.match(_b);
var _d="";
var _e=0;
var _f=0;
for(var i=0;i<_c.length;i++){
var _11=_c[i];
if(!_11.match(_a)){
_d+="<span>"+_11+"</span>";
_e++;
}else{
_d+=_11;
}
}
_2.innerHTML=_d;
function animatePieces(_12){
var _13=_12.nextSibling;
if(_12.tagName=="SPAN"&&_12.childNodes.length==1&&_12.firstChild.nodeType==3){
var _14=dojo.coords(_12,true);
_f++;
dojo.style(_12,{padding:0,margin:0,top:(_1.crop?"0px":_14.t+"px"),left:(_1.crop?"0px":_14.l+"px"),display:"inline"});
var _15=_1.pieceAnimation(_12,_14,_5,_f,_e);
if(dojo.isArray(_15)){
_9=_9.concat(_15);
}else{
_9[_9.length]=_15;
}
}else{
if(_12.firstChild){
animatePieces(_12.firstChild);
}
}
if(_13){
animatePieces(_13);
}
};
animatePieces(_2.firstChild);
var _16=dojo.fx.combine(_9);
dojo.connect(_16,"onEnd",_16,function(){
_2.innerHTML=_6;
dojo.style(_2,{height:_7,width:_8});
});
if(_1.onPlay){
dojo.connect(_16,"onPlay",_16,_1.onPlay);
}
if(_1.onEnd){
dojo.connect(_16,"onEnd",_16,_1.onEnd);
}
return _16;
};
dojox.fx.text.explode=function(_17){
var _18=_17.node=dojo.byId(_17.node);
var s=_18.style;
_17.distance=_17.distance||1;
_17.duration=_17.duration||1000;
_17.random=_17.random||0;
if(typeof (_17.fade)=="undefined"){
_17.fade=true;
}
if(typeof (_17.sync)=="undefined"){
_17.sync=true;
}
_17.random=Math.abs(_17.random);
_17.pieceAnimation=function(_1a,_1b,_1c,_1d,_1e){
var _1f=_1b.h;
var _20=_1b.w;
var _21=_17.distance*2;
var _22=_17.duration;
var _23=parseFloat(_1a.style.top);
var _24=parseFloat(_1a.style.left);
var _25=0;
var _26=0;
var _27=0;
if(_17.random){
var _28=(Math.random()*_17.random)+Math.max(1-_17.random,0);
_21*=_28;
_22*=_28;
_25=((_17.unhide&&_17.sync)||(!_17.unhide&&!_17.sync))?(_17.duration-_22):0;
_26=Math.random()-0.5;
_27=Math.random()-0.5;
}
var _29=((_1c.h-_1f)/2-(_1b.y-_1c.y));
var _2a=((_1c.w-_20)/2-(_1b.x-_1c.x));
var _2b=Math.sqrt(Math.pow(_2a,2)+Math.pow(_29,2));
var _2c=_23-_29*_21+_2b*_27;
var _2d=_24-_2a*_21+_2b*_26;
var _2e=dojo.animateProperty({node:_1a,duration:_22,delay:_25,easing:(_17.easing||(_17.unhide?dojo.fx.easing.sinOut:dojo.fx.easing.circOut)),beforeBegin:(_17.unhide?function(){
if(_17.fade){
dojo.style(_1a,"opacity",0);
}
_1a.style.position=_17.crop?"relative":"absolute";
_1a.style.top=_2c+"px";
_1a.style.left=_2d+"px";
}:function(){
_1a.style.position=_17.crop?"relative":"absolute";
}),properties:{top:(_17.unhide?{start:_2c,end:_23}:{start:_23,end:_2c}),left:(_17.unhide?{start:_2d,end:_24}:{start:_24,end:_2d})}});
if(_17.fade){
var _2f=dojo.animateProperty({node:_1a,duration:_22,delay:_25,easing:(_17.fadeEasing||dojo.fx.easing.quadOut),properties:{opacity:(_17.unhide?{start:0,end:1}:{end:0})}});
return (_17.unhide?[_2f,_2e]:[_2e,_2f]);
}else{
return _2e;
}
};
var _30=dojox.fx.text._split(_17);
return _30;
};
dojox.fx.text.converge=function(_31){
_31.unhide=true;
return dojox.fx.text.explode(_31);
};
dojox.fx.text.disintegrate=function(_32){
var _33=_32.node=dojo.byId(_32.node);
var s=_33.style;
_32.duration=_32.duration||1500;
_32.distance=_32.distance||1.5;
_32.random=_32.random||0;
if(!_32.fade){
_32.fade=true;
}
var _35=Math.abs(_32.random);
_32.pieceAnimation=function(_36,_37,_38,_39,_3a){
var _3b=_37.h;
var _3c=_37.w;
var _3d=_32.interval||(_32.duration/(1.5*_3a));
var _3e=(_32.duration-_3a*_3d);
var _3f=Math.random()*_3a*_3d;
var _40=(_32.reverseOrder||_32.distance<0)?(_39*_3d):((_3a-_39)*_3d);
var _41=_3f*_35+Math.max(1-_35,0)*_40;
var _42={};
if(_32.unhide){
_42.top={start:(parseFloat(_36.style.top)-_38.h*_32.distance),end:parseFloat(_36.style.top)};
if(_32.fade){
_42.opacity={start:0,end:1};
}
}else{
_42.top={end:(parseFloat(_36.style.top)+_38.h*_32.distance)};
if(_32.fade){
_42.opacity={end:0};
}
}
var _43=dojo.animateProperty({node:_36,duration:_3e,delay:_41,easing:(_32.easing||(_32.unhide?dojo.fx.easing.sinIn:dojo.fx.easing.circIn)),properties:_42,beforeBegin:(_32.unhide?function(){
if(_32.fade){
dojo.style(_36,"opacity",0);
}
_36.style.position=_32.crop?"relative":"absolute";
_36.style.top=_42.top.start+"px";
}:function(){
_36.style.position=_32.crop?"relative":"absolute";
})});
return _43;
};
var _44=dojox.fx.text._split(_32);
return _44;
};
dojox.fx.text.build=function(_45){
_45.unhide=true;
return dojox.fx.text.disintegrate(_45);
};
dojox.fx.text.blockFadeOut=function(_46){
var _47=_46.node=dojo.byId(_46.node);
var s=_47.style;
_46.duration=_46.duration||1000;
_46.random=_46.random||0;
var _49=Math.abs(_46.random);
_46.pieceAnimation=function(_4a,_4b,_4c,_4d,_4e){
var _4f=_46.interval||(_46.duration/(1.5*_4e));
var _50=(_46.duration-_4e*_4f);
var _51=Math.random()*_4e*_4f;
var _52=(_46.reverseOrder)?((_4e-_4d)*_4f):(_4d*_4f);
var _53=_51*_49+Math.max(1-_49,0)*_52;
var _54=dojo.animateProperty({node:_4a,duration:_50,delay:_53,easing:(_46.easing||dojo.fx.easing.sinInOut),properties:{opacity:(_46.unhide?{start:0,end:1}:{end:0})},beforeBegin:(_46.unhide?function(){
dojo.style(_4a,"opacity",0);
}:undefined)});
return _54;
};
var _55=dojox.fx.text._split(_46);
return _55;
};
dojox.fx.text.blockFadeIn=function(_56){
_56.unhide=true;
return dojox.fx.text.blockFadeOut(_56);
};
dojox.fx.text.backspace=function(_57){
var _58=_57.node=dojo.byId(_57.node);
var s=_58.style;
_57.words=false;
_57.duration=_57.duration||2000;
_57.random=_57.random||0;
var _5a=Math.abs(_57.random);
var _5b=10;
_57.pieceAnimation=function(_5c,_5d,_5e,_5f,_60){
var _61=_57.interval||(_57.duration/(1.5*_60));
var _62=_5c.textContent;
var _63=_62.match(/\s/g);
if(typeof (_57.wordDelay)=="undefined"){
_57.wordDelay=_61*2;
}
if(!_57.unhide){
_5b=(_60-_5f-1)*_61;
}
var _64,_65;
if(_57.fixed){
if(_57.unhide){
var _64=function(){
dojo.style(_5c,"opacity",0);
};
}
}else{
if(_57.unhide){
var _64=function(){
_5c.style.display="none";
};
var _65=function(){
_5c.style.display="inline";
};
}else{
var _65=function(){
_5c.style.display="none";
};
}
}
var _66=dojo.animateProperty({node:_5c,duration:1,delay:_5b,easing:(_57.easing||dojo.fx.easing.sinInOut),properties:{opacity:(_57.unhide?{start:0,end:1}:{end:0})},beforeBegin:_64,onEnd:_65});
if(_57.unhide){
var _67=Math.random()*_62.length*_61;
var _68=_67*_5a/2+Math.max(1-_5a/2,0)*_57.wordDelay;
_5b+=_67*_5a+Math.max(1-_5a,0)*_61*_62.length+(_68*(_63&&_62.lastIndexOf(_63[_63.length-1])==_62.length-1));
}
return _66;
};
var _69=dojox.fx.text._split(_57);
return _69;
};
dojox.fx.text.type=function(_6a){
_6a.unhide=true;
return dojox.fx.text.backspace(_6a);
};
}
