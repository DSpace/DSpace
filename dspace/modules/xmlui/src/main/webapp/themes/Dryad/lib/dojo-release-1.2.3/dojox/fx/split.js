/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.fx.split"]){
dojo._hasResource["dojox.fx.split"]=true;
dojo.provide("dojox.fx.split");
dojo.require("dojo.fx");
dojo.require("dojo.fx.easing");
dojo.mixin(dojox.fx,{_split:function(_1){
var _2=_1.node=dojo.byId(_1.node),s=_2.style,_4=dojo.coords(_2,true);
_1.rows=_1.rows||3;
_1.columns=_1.columns||3;
_1.duration=_1.duration||1000;
var _5=_4.h/_1.rows;
var _6=_4.w/_1.columns;
var _7=dojo.doc.createElement("div");
dojo.style(_7,{position:"absolute",top:parseFloat(_4.t)+"px",left:parseFloat(_4.l)+"px",border:"none",height:parseFloat(_4.h)+"px",width:parseFloat(_4.w)+"px",overflow:_1.crop?"hidden":"visible"});
_2.parentNode.appendChild(_7);
var _8=[];
for(var y=0;y<_1.rows;y++){
for(var x=0;x<_1.columns;x++){
var _b=document.createElement("DIV");
var _c=_2.cloneNode(true);
var _d=_1.duration;
dojo.style(_b,{position:"absolute",top:(_5*y)+"px",left:(_6*x)+"px",padding:"0",margin:"0",border:"none",overflow:"hidden",height:_5+"px",width:_6+"px"});
_b.onmouseover=undefined;
_b.onmouseout=undefined;
_b.onclick=undefined;
_b.ondblclick=undefined;
_b.oncontextmenu=undefined;
dojo.style(_c,{opacity:"",position:"relative",top:0-(y*_5)+"px",left:0-(x*_6)+"px"});
_b.appendChild(_c);
_7.appendChild(_b);
var _e=_1.pieceAnimation(_b,x,y,_4);
if(dojo.isArray(_e)){
_8=_8.concat(_e);
}else{
_8[_8.length]=_e;
}
}
}
var _f=dojo.fx.combine(_8);
dojo.connect(_f,"onEnd",_f,function(){
_2.parentNode.removeChild(_7);
});
if(_1.onPlay){
dojo.connect(_f,"onPlay",_f,_1.onPlay);
}
if(_1.onEnd){
dojo.connect(_f,"onEnd",_f,_1.onEnd);
}
return _f;
},explode:function(_10){
var _11=_10.node=dojo.byId(_10.node);
var s=_11.style;
_10.rows=_10.rows||3;
_10.columns=_10.columns||3;
_10.distance=_10.distance||1;
_10.duration=_10.duration||1000;
_10.random=_10.random||0;
if(!_10.fade){
_10.fade=true;
}
if(typeof (_10.sync)=="undefined"){
_10.sync=true;
}
_10.random=Math.abs(_10.random);
_10.pieceAnimation=function(_13,x,y,_16){
var _17=_16.h/_10.rows,_18=_16.w/_10.columns,_19=_10.distance*2,_1a=_10.duration,_1b=parseFloat(_13.style.top),_1c=parseFloat(_13.style.left),_1d=0,_1e=0,_1f=0;
if(_10.random){
var _20=(Math.random()*_10.random)+Math.max(1-_10.random,0);
_19*=_20;
_1a*=_20;
_1d=((_10.unhide&&_10.sync)||(!_10.unhide&&!_10.sync))?(_10.duration-_1a):0;
_1e=Math.random()-0.5;
_1f=Math.random()-0.5;
}
var _21=((_16.h-_17)/2-_17*y),_22=((_16.w-_18)/2-_18*x),_23=Math.sqrt(Math.pow(_22,2)+Math.pow(_21,2)),_24=_1b-_21*_19+_23*_1f,_25=_1c-_22*_19+_23*_1e;
var _26=dojo.animateProperty({node:_13,duration:_1a,delay:_1d,easing:(_10.easing||(_10.unhide?dojo.fx.easing.sinOut:dojo.fx.easing.circOut)),beforeBegin:(_10.unhide?function(){
if(_10.fade){
_13.style.opacity=0;
}
_13.style.top=_24+"px";
_13.style.left=_25+"px";
}:undefined),properties:{top:(_10.unhide?{start:_24,end:_1b}:{start:_1b,end:_24}),left:(_10.unhide?{start:_25,end:_1c}:{start:_1c,end:_25})}});
if(_10.fade){
var _27=dojo.animateProperty({node:_13,duration:_1a,delay:_1d,easing:(_10.fadeEasing||dojo.fx.easing.quadOut),properties:{opacity:(_10.unhide?{start:0,end:1}:{end:0})}});
return (_10.unhide?[_27,_26]:[_26,_27]);
}else{
return _26;
}
};
var _28=dojox.fx._split(_10);
if(_10.unhide){
dojo.connect(_28,"onEnd",_28,function(){
dojo.style(_11,"opacity",1);
});
}else{
dojo.connect(_28,"onPlay",_28,function(){
dojo.style(_11,"opacity",0);
});
}
return _28;
},converge:function(_29){
_29.unhide=true;
return dojox.fx.explode(_29);
},disintegrate:function(_2a){
var _2b=_2a.node=dojo.byId(_2a.node);
var s=_2b.style;
_2a.rows=_2a.rows||5;
_2a.columns=_2a.columns||5;
_2a.duration=_2a.duration||1500;
_2a.interval=_2a.interval||_2a.duration/(_2a.rows+_2a.columns*2);
_2a.distance=_2a.distance||1.5;
_2a.random=_2a.random||0;
var _2d=Math.abs(_2a.random);
if(typeof (_2a.fade)=="undefined"){
_2a.fade=true;
}
var _2e=_2a.duration-(_2a.rows+_2a.columns)*_2a.interval;
_2a.pieceAnimation=function(_2f,x,y,_32){
var _33=_32.h/_2a.rows;
var _34=_32.w/_2a.columns;
var _35=Math.random()*(_2a.rows+_2a.columns)*_2a.interval;
var _36=(_2a.reverseOrder||_2a.distance<0)?((x+y)*_2a.interval):(((_2a.rows+_2a.columns)-(x+y))*_2a.interval);
var _37=_35*_2d+Math.max(1-_2d,0)*_36;
var _38={};
if(_2a.unhide){
_38.top={start:(parseFloat(_2f.style.top)-_32.h*_2a.distance),end:parseFloat(_2f.style.top)};
if(_2a.fade){
_38.opacity={start:0,end:1};
}
}else{
_38.top={end:(parseFloat(_2f.style.top)+_32.h*_2a.distance)};
if(_2a.fade){
_38.opacity={end:0};
}
}
var _39=dojo.animateProperty({node:_2f,duration:_2e,delay:_37,easing:(_2a.easing||(_2a.unhide?dojo.fx.easing.sinIn:dojo.fx.easing.circIn)),properties:_38,beforeBegin:(_2a.unhide?function(){
if(_2a.fade){
dojo.style(_2f,"opacity",0);
}
_2f.style.top=_38.top.start+"px";
}:undefined)});
return _39;
};
var _3a=dojox.fx._split(_2a);
if(_2a.unhide){
dojo.connect(_3a,"onEnd",_3a,function(){
dojo.style(_2b,"opacity",1);
});
}else{
dojo.connect(_3a,"onPlay",_3a,function(){
dojo.style(_2b,"opacity",0);
});
}
return _3a;
},build:function(_3b){
_3b.unhide=true;
return dojox.fx.disintegrate(_3b);
},shear:function(_3c){
var _3d=_3c.node=dojo.byId(_3c.node);
var s=_3d.style;
_3c.rows=_3c.rows||6;
_3c.columns=_3c.columns||6;
_3c.duration=_3c.duration||1000;
_3c.interval=_3c.interval||0;
_3c.distance=_3c.distance||1;
_3c.random=_3c.random||0;
if(typeof (_3c.fade)=="undefined"){
_3c.fade=true;
}
var _3f=Math.abs(_3c.random);
var _40=(_3c.duration-(_3c.rows+_3c.columns)*Math.abs(_3c.interval));
_3c.pieceAnimation=function(_41,x,y,_44){
var _45=_44.h/_3c.rows;
var _46=_44.w/_3c.columns;
var _47=!(x%2);
var _48=!(y%2);
var _49=Math.random()*_40;
var _4a=(_3c.reverseOrder)?(((_3c.rows+_3c.columns)-(x+y))*_3c.interval):((x+y)*_3c.interval);
var _4b=_49*_3f+Math.max(1-_3f,0)*_4a;
var _4c={};
if(_3c.fade){
_4c.opacity=(_3c.unhide?{start:0,end:1}:{end:0});
}
if(_3c.columns==1){
_47=_48;
}else{
if(_3c.rows==1){
_48=!_47;
}
}
var _4d=parseFloat(_41.style.left);
var top=parseFloat(_41.style.top);
var _4f=_3c.distance*_44.w;
var _50=_3c.distance*_44.h;
if(_3c.unhide){
if(_47==_48){
_4c.left=_47?{start:(_4d-_4f),end:_4d}:{start:(_4d+_4f),end:_4d};
}else{
_4c.top=_47?{start:(top+_50),end:top}:{start:(top-_50),end:top};
}
}else{
if(_47==_48){
_4c.left=_47?{end:(_4d-_4f)}:{end:(_4d+_4f)};
}else{
_4c.top=_47?{end:(top+_50)}:{end:(top-_50)};
}
}
var _51=dojo.animateProperty({node:_41,duration:_40,delay:_4b,easing:(_3c.easing||dojo.fx.easing.sinInOut),properties:_4c,beforeBegin:(_3c.unhide?function(){
if(_3c.fade){
_41.style.opacity=0;
}
if(_47==_48){
_41.style.left=_4c.left.start+"px";
}else{
_41.style.top=_4c.top.start+"px";
}
}:undefined)});
return _51;
};
var _52=dojox.fx._split(_3c);
if(_3c.unhide){
dojo.connect(_52,"onEnd",_52,function(){
dojo.style(_3d,"opacity",1);
});
}else{
dojo.connect(_52,"onPlay",_52,function(){
dojo.style(_3d,"opacity",0);
});
}
return _52;
},unShear:function(_53){
_53.unhide=true;
return dojox.fx.shear(_53);
},pinwheel:function(_54){
var _55=_54.node=dojo.byId(_54.node);
var s=_55.style;
_54.rows=_54.rows||4;
_54.columns=_54.columns||4;
_54.duration=_54.duration||1000;
_54.interval=_54.interval||0;
_54.distance=_54.distance||1;
_54.random=_54.random||0;
if(typeof (_54.fade)=="undefined"){
_54.fade=true;
}
var _57=Math.abs(_54.random);
var _58=(_54.duration-(_54.rows+_54.columns)*Math.abs(_54.interval));
_54.pieceAnimation=function(_59,x,y,_5c){
var _5d=_5c.h/_54.rows,_5e=_5c.w/_54.columns,_5f=!(x%2),_60=!(y%2),_61=Math.random()*_58,_62=(_54.interval<0)?(((_54.rows+_54.columns)-(x+y))*_54.interval*-1):((x+y)*_54.interval),_63=_61*_54.random+Math.max(1-_54.random,0)*_62,_64={};
if(_54.fade){
_64.opacity=(_54.unhide?{start:0,end:1}:{end:0});
}
if(_54.columns==1){
_5f=!_60;
}else{
if(_54.rows==1){
_60=_5f;
}
}
var _65=parseFloat(_59.style.left);
var top=parseFloat(_59.style.top);
if(_5f){
if(_60){
_64.top=_54.unhide?{start:top+_5d*_54.distance,end:top}:{start:top,end:top+_5d*_54.distance};
}else{
_64.left=_54.unhide?{start:_65+_5e*_54.distance,end:_65}:{start:_65,end:_65+_5e*_54.distance};
}
}
if(_5f!=_60){
_64.width=_54.unhide?{start:_5e*(1-_54.distance),end:_5e}:{start:_5e,end:_5e*(1-_54.distance)};
}else{
_64.height=_54.unhide?{start:_5d*(1-_54.distance),end:_5d}:{start:_5d,end:_5d*(1-_54.distance)};
}
var _67=dojo.animateProperty({node:_59,duration:_58,delay:_63,easing:(_54.easing||dojo.fx.easing.sinInOut),properties:_64,beforeBegin:(_54.unhide?function(){
if(_54.fade){
dojo.style(_59,"opacity",0);
}
if(_5f){
if(_60){
_59.style.top=(top+_5d*(1-_54.distance))+"px";
}else{
_59.style.left=(_65+_5e*(1-_54.distance))+"px";
}
}else{
_59.style.left=_65+"px";
_59.style.top=top+"px";
}
if(_5f!=_60){
_59.style.width=(_5e*(1-_54.distance))+"px";
}else{
_59.style.height=(_5d*(1-_54.distance))+"px";
}
}:undefined)});
return _67;
};
var _68=dojox.fx._split(_54);
if(_54.unhide){
dojo.connect(_68,"onEnd",_68,function(){
dojo.style(_55,"opacity",1);
});
}else{
dojo.connect(_68,"onPlay",_68,function(){
dojo.style(_55,"opacity",0);
});
}
return _68;
},unPinwheel:function(_69){
_69.unhide=true;
return dojox.fx.pinwheel(_69);
},blockFadeOut:function(_6a){
var _6b=_6a.node=dojo.byId(_6a.node);
var s=_6b.style;
_6a.rows=_6a.rows||5;
_6a.columns=_6a.columns||5;
_6a.duration=_6a.duration||1000;
_6a.interval=_6a.interval||_6a.duration/(_6a.rows+_6a.columns*2);
_6a.random=_6a.random||0;
var _6d=Math.abs(_6a.random);
var _6e=_6a.duration-(_6a.rows+_6a.columns)*_6a.interval;
_6a.pieceAnimation=function(_6f,x,y,_72){
var _73=Math.random()*_6a.duration;
var _74=(_6a.reverseOrder)?(((_6a.rows+_6a.columns)-(x+y))*Math.abs(_6a.interval)):((x+y)*_6a.interval);
var _75=_73*_6d+Math.max(1-_6d,0)*_74;
var _76=dojo.animateProperty({node:_6f,duration:_6e,delay:_75,easing:(_6a.easing||dojo.fx.easing.sinInOut),properties:{opacity:(_6a.unhide?{start:0,end:1}:{end:0})},beforeBegin:(_6a.unhide?function(){
dojo.style(_6f,"opacity",0);
}:undefined)});
return _76;
};
var _77=dojox.fx._split(_6a);
if(_6a.unhide){
dojo.connect(_77,"onEnd",_77,function(){
dojo.style(_6b,"opacity",1);
});
}else{
dojo.connect(_77,"onPlay",_77,function(){
dojo.style(_6b,"opacity",0);
});
}
return _77;
},blockFadeIn:function(_78){
_78.unhide=true;
return dojox.fx.blockFadeOut(_78);
}});
}
