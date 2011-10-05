/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.fx"]){
dojo._hasResource["dojo._base.fx"]=true;
dojo.provide("dojo._base.fx");
dojo.require("dojo._base.Color");
dojo.require("dojo._base.connect");
dojo.require("dojo._base.declare");
dojo.require("dojo._base.lang");
dojo.require("dojo._base.html");
(function(){
var d=dojo;
dojo._Line=function(_2,_3){
this.start=_2;
this.end=_3;
this.getValue=function(n){
return ((this.end-this.start)*n)+this.start;
};
};
d.declare("dojo._Animation",null,{constructor:function(_5){
d.mixin(this,_5);
if(d.isArray(this.curve)){
this.curve=new d._Line(this.curve[0],this.curve[1]);
}
},duration:350,repeat:0,rate:10,_percent:0,_startRepeatCount:0,_fire:function(_6,_7){
if(this[_6]){
if(dojo.config.isDebug){
this[_6].apply(this,_7||[]);
}else{
try{
this[_6].apply(this,_7||[]);
}
catch(e){
console.error("exception in animation handler for:",_6);
console.error(e);
}
}
}
return this;
},play:function(_8,_9){
var _t=this;
if(_9){
_t._stopTimer();
_t._active=_t._paused=false;
_t._percent=0;
}else{
if(_t._active&&!_t._paused){
return _t;
}
}
_t._fire("beforeBegin");
var de=_8||_t.delay;
var _p=dojo.hitch(_t,"_play",_9);
if(de>0){
setTimeout(_p,de);
return _t;
}
_p();
return _t;
},_play:function(_d){
var _t=this;
_t._startTime=new Date().valueOf();
if(_t._paused){
_t._startTime-=_t.duration*_t._percent;
}
_t._endTime=_t._startTime+_t.duration;
_t._active=true;
_t._paused=false;
var _f=_t.curve.getValue(_t._percent);
if(!_t._percent){
if(!_t._startRepeatCount){
_t._startRepeatCount=_t.repeat;
}
_t._fire("onBegin",[_f]);
}
_t._fire("onPlay",[_f]);
_t._cycle();
return _t;
},pause:function(){
this._stopTimer();
if(!this._active){
return this;
}
this._paused=true;
this._fire("onPause",[this.curve.getValue(this._percent)]);
return this;
},gotoPercent:function(_10,_11){
this._stopTimer();
this._active=this._paused=true;
this._percent=_10;
if(_11){
this.play();
}
return this;
},stop:function(_12){
if(!this._timer){
return this;
}
this._stopTimer();
if(_12){
this._percent=1;
}
this._fire("onStop",[this.curve.getValue(this._percent)]);
this._active=this._paused=false;
return this;
},status:function(){
if(this._active){
return this._paused?"paused":"playing";
}
return "stopped";
},_cycle:function(){
var _t=this;
if(_t._active){
var _14=new Date().valueOf();
var _15=(_14-_t._startTime)/(_t._endTime-_t._startTime);
if(_15>=1){
_15=1;
}
_t._percent=_15;
if(_t.easing){
_15=_t.easing(_15);
}
_t._fire("onAnimate",[_t.curve.getValue(_15)]);
if(_t._percent<1){
_t._startTimer();
}else{
_t._active=false;
if(_t.repeat>0){
_t.repeat--;
_t.play(null,true);
}else{
if(_t.repeat==-1){
_t.play(null,true);
}else{
if(_t._startRepeatCount){
_t.repeat=_t._startRepeatCount;
_t._startRepeatCount=0;
}
}
}
_t._percent=0;
_t._fire("onEnd");
_t._stopTimer();
}
}
return _t;
}});
var ctr=0;
var _17=[];
var _18={run:function(){
}};
var _19=null;
dojo._Animation.prototype._startTimer=function(){
if(!this._timer){
this._timer=d.connect(_18,"run",this,"_cycle");
ctr++;
}
if(!_19){
_19=setInterval(d.hitch(_18,"run"),this.rate);
}
};
dojo._Animation.prototype._stopTimer=function(){
if(this._timer){
d.disconnect(this._timer);
this._timer=null;
ctr--;
}
if(ctr<=0){
clearInterval(_19);
_19=null;
ctr=0;
}
};
var _1a=(d.isIE)?function(_1b){
var ns=_1b.style;
if(!ns.width.length&&d.style(_1b,"width")=="auto"){
ns.width="auto";
}
}:function(){
};
dojo._fade=function(_1d){
_1d.node=d.byId(_1d.node);
var _1e=d.mixin({properties:{}},_1d);
var _1f=(_1e.properties.opacity={});
_1f.start=!("start" in _1e)?function(){
return Number(d.style(_1e.node,"opacity"));
}:_1e.start;
_1f.end=_1e.end;
var _20=d.animateProperty(_1e);
d.connect(_20,"beforeBegin",d.partial(_1a,_1e.node));
return _20;
};
dojo.fadeIn=function(_21){
return d._fade(d.mixin({end:1},_21));
};
dojo.fadeOut=function(_22){
return d._fade(d.mixin({end:0},_22));
};
dojo._defaultEasing=function(n){
return 0.5+((Math.sin((n+1.5)*Math.PI))/2);
};
var _24=function(_25){
this._properties=_25;
for(var p in _25){
var _27=_25[p];
if(_27.start instanceof d.Color){
_27.tempColor=new d.Color();
}
}
this.getValue=function(r){
var ret={};
for(var p in this._properties){
var _2b=this._properties[p];
var _2c=_2b.start;
if(_2c instanceof d.Color){
ret[p]=d.blendColors(_2c,_2b.end,r,_2b.tempColor).toCss();
}else{
if(!d.isArray(_2c)){
ret[p]=((_2b.end-_2c)*r)+_2c+(p!="opacity"?_2b.units||"px":"");
}
}
}
return ret;
};
};
dojo.animateProperty=function(_2d){
_2d.node=d.byId(_2d.node);
if(!_2d.easing){
_2d.easing=d._defaultEasing;
}
var _2e=new d._Animation(_2d);
d.connect(_2e,"beforeBegin",_2e,function(){
var pm={};
for(var p in this.properties){
if(p=="width"||p=="height"){
this.node.display="block";
}
var _31=this.properties[p];
_31=pm[p]=d.mixin({},(d.isObject(_31)?_31:{end:_31}));
if(d.isFunction(_31.start)){
_31.start=_31.start();
}
if(d.isFunction(_31.end)){
_31.end=_31.end();
}
var _32=(p.toLowerCase().indexOf("color")>=0);
function getStyle(_33,p){
var v=({height:_33.offsetHeight,width:_33.offsetWidth})[p];
if(v!==undefined){
return v;
}
v=d.style(_33,p);
return (p=="opacity")?Number(v):(_32?v:parseFloat(v));
};
if(!("end" in _31)){
_31.end=getStyle(this.node,p);
}else{
if(!("start" in _31)){
_31.start=getStyle(this.node,p);
}
}
if(_32){
_31.start=new d.Color(_31.start);
_31.end=new d.Color(_31.end);
}else{
_31.start=(p=="opacity")?Number(_31.start):parseFloat(_31.start);
}
}
this.curve=new _24(pm);
});
d.connect(_2e,"onAnimate",d.hitch(d,"style",_2e.node));
return _2e;
};
dojo.anim=function(_36,_37,_38,_39,_3a,_3b){
return d.animateProperty({node:_36,duration:_38||d._Animation.prototype.duration,properties:_37,easing:_39,onEnd:_3a}).play(_3b||0);
};
})();
}
