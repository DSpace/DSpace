/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.charting.axis2d.Default"]){
dojo._hasResource["dojox.charting.axis2d.Default"]=true;
dojo.provide("dojox.charting.axis2d.Default");
dojo.require("dojox.charting.scaler.linear");
dojo.require("dojox.charting.axis2d.common");
dojo.require("dojox.charting.axis2d.Base");
dojo.require("dojo.colors");
dojo.require("dojo.string");
dojo.require("dojox.gfx");
dojo.require("dojox.lang.functional");
dojo.require("dojox.lang.utils");
(function(){
var dc=dojox.charting,df=dojox.lang.functional,du=dojox.lang.utils,g=dojox.gfx,_5=dc.scaler.linear,_6=4;
dojo.declare("dojox.charting.axis2d.Default",dojox.charting.axis2d.Base,{defaultParams:{vertical:false,fixUpper:"none",fixLower:"none",natural:false,leftBottom:true,includeZero:false,fixed:true,majorLabels:true,minorTicks:true,minorLabels:true,microTicks:false,htmlLabels:true},optionalParams:{min:0,max:1,from:0,to:1,majorTickStep:4,minorTickStep:2,microTickStep:1,labels:[],stroke:{},majorTick:{},minorTick:{},font:"",fontColor:""},constructor:function(_7,_8){
this.opt=dojo.clone(this.defaultParams);
du.updateWithObject(this.opt,_8);
du.updateWithPattern(this.opt,_8,this.optionalParams);
},dependOnData:function(){
return !("min" in this.opt)||!("max" in this.opt);
},clear:function(){
delete this.scaler;
delete this.ticks;
this.dirty=true;
return this;
},initialized:function(){
return "scaler" in this&&!(this.dirty&&this.dependOnData());
},setWindow:function(_9,_a){
this.scale=_9;
this.offset=_a;
return this.clear();
},getWindowScale:function(){
return "scale" in this?this.scale:1;
},getWindowOffset:function(){
return "offset" in this?this.offset:0;
},calculate:function(_b,_c,_d,_e){
if(this.initialized()){
return this;
}
this.labels="labels" in this.opt?this.opt.labels:_e;
this.scaler=_5.buildScaler(_b,_c,_d,this.opt);
if("scale" in this){
this.opt.from=this.scaler.bounds.lower+this.offset;
this.opt.to=(this.scaler.bounds.upper-this.scaler.bounds.lower)/this.scale+this.opt.from;
if(!isFinite(this.opt.from)||isNaN(this.opt.from)||!isFinite(this.opt.to)||isNaN(this.opt.to)||this.opt.to-this.opt.from>=this.scaler.bounds.upper-this.scaler.bounds.lower){
delete this.opt.from;
delete this.opt.to;
delete this.scale;
delete this.offset;
}else{
if(this.opt.from<this.scaler.bounds.lower){
this.opt.to+=this.scaler.bounds.lower-this.opt.from;
this.opt.from=this.scaler.bounds.lower;
}else{
if(this.opt.to>this.scaler.bounds.upper){
this.opt.from+=this.scaler.bounds.upper-this.opt.to;
this.opt.to=this.scaler.bounds.upper;
}
}
this.offset=this.opt.from-this.scaler.bounds.lower;
}
this.scaler=_5.buildScaler(_b,_c,_d,this.opt);
if(this.scale==1&&this.offset==0){
delete this.scale;
delete this.offset;
}
}
var _f=0,ta=this.chart.theme.axis,_11="font" in this.opt?this.opt.font:ta.font,_12=_11?g.normalizedLength(g.splitFontString(_11).size):0;
if(this.vertical){
if(_12){
_f=_12+_6;
}
}else{
if(_12){
var _13,i;
if(this.labels){
_13=df.foldl(df.map(this.labels,function(_15){
return dojox.gfx._base._getTextBox(_15.text,{font:_11}).w;
}),"Math.max(a, b)",0);
}else{
var _16=Math.ceil(Math.log(Math.max(Math.abs(this.scaler.bounds.from),Math.abs(this.scaler.bounds.to)))/Math.LN10),t=[];
if(this.scaler.bounds.from<0||this.scaler.bounds.to<0){
t.push("-");
}
t.push(dojo.string.rep("9",_16));
var _18=Math.floor(Math.log(this.scaler.bounds.to-this.scaler.bounds.from)/Math.LN10);
if(_18>0){
t.push(".");
for(i=0;i<_18;++i){
t.push("9");
}
}
_13=dojox.gfx._base._getTextBox(t.join(""),{font:_11}).w;
}
_f=_13+_6;
}
}
this.scaler.minMinorStep=_f;
this.ticks=_5.buildTicks(this.scaler,this.opt);
return this;
},getScaler:function(){
return this.scaler;
},getTicks:function(){
return this.ticks;
},getOffsets:function(){
var _19={l:0,r:0,t:0,b:0},_1a,a,b,c,d,gtb=dojox.gfx._base._getTextBox,gl=dc.scaler.common.getNumericLabel,_21=0,ta=this.chart.theme.axis,_23="font" in this.opt?this.opt.font:ta.font,_24="majorTick" in this.opt?this.opt.majorTick:ta.majorTick,_25="minorTick" in this.opt?this.opt.minorTick:ta.minorTick,_26=_23?g.normalizedLength(g.splitFontString(_23).size):0,s=this.scaler;
if(!s){
return _19;
}
if(this.vertical){
if(_26){
if(this.labels){
_1a=df.foldl(df.map(this.labels,function(_28){
return dojox.gfx._base._getTextBox(_28.text,{font:_23}).w;
}),"Math.max(a, b)",0);
}else{
a=gtb(gl(s.major.start,s.major.prec,this.opt),{font:_23}).w;
b=gtb(gl(s.major.start+s.major.count*s.major.tick,s.major.prec,this.opt),{font:_23}).w;
c=gtb(gl(s.minor.start,s.minor.prec,this.opt),{font:_23}).w;
d=gtb(gl(s.minor.start+s.minor.count*s.minor.tick,s.minor.prec,this.opt),{font:_23}).w;
_1a=Math.max(a,b,c,d);
}
_21=_1a+_6;
}
_21+=_6+Math.max(_24.length,_25.length);
_19[this.opt.leftBottom?"l":"r"]=_21;
_19.t=_19.b=_26/2;
}else{
if(_26){
_21=_26+_6;
}
_21+=_6+Math.max(_24.length,_25.length);
_19[this.opt.leftBottom?"b":"t"]=_21;
if(_26){
if(this.labels){
_1a=df.foldl(df.map(this.labels,function(_29){
return dojox.gfx._base._getTextBox(_29.text,{font:_23}).w;
}),"Math.max(a, b)",0);
}else{
a=gtb(gl(s.major.start,s.major.prec,this.opt),{font:_23}).w;
b=gtb(gl(s.major.start+s.major.count*s.major.tick,s.major.prec,this.opt),{font:_23}).w;
c=gtb(gl(s.minor.start,s.minor.prec,this.opt),{font:_23}).w;
d=gtb(gl(s.minor.start+s.minor.count*s.minor.tick,s.minor.prec,this.opt),{font:_23}).w;
_1a=Math.max(a,b,c,d);
}
_19.l=_19.r=_1a/2;
}
}
return _19;
},render:function(dim,_2b){
if(!this.dirty){
return this;
}
var _2c,_2d,_2e,_2f,_30,_31,ta=this.chart.theme.axis,_33="stroke" in this.opt?this.opt.stroke:ta.stroke,_34="majorTick" in this.opt?this.opt.majorTick:ta.majorTick,_35="minorTick" in this.opt?this.opt.minorTick:ta.minorTick,_36="font" in this.opt?this.opt.font:ta.font,_37="fontColor" in this.opt?this.opt.fontColor:ta.fontColor,_38=Math.max(_34.length,_35.length),_39=_36?g.normalizedLength(g.splitFontString(_36).size):0;
if(this.vertical){
_2c={y:dim.height-_2b.b};
_2d={y:_2b.t};
_2e={x:0,y:-1};
if(this.opt.leftBottom){
_2c.x=_2d.x=_2b.l;
_2f={x:-1,y:0};
_31="end";
}else{
_2c.x=_2d.x=dim.width-_2b.r;
_2f={x:1,y:0};
_31="start";
}
_30={x:_2f.x*(_38+_6),y:_39*0.4};
}else{
_2c={x:_2b.l};
_2d={x:dim.width-_2b.r};
_2e={x:1,y:0};
_31="middle";
if(this.opt.leftBottom){
_2c.y=_2d.y=dim.height-_2b.b;
_2f={x:0,y:1};
_30={y:_38+_6+_39};
}else{
_2c.y=_2d.y=_2b.t;
_2f={x:0,y:-1};
_30={y:-_38-_6};
}
_30.x=0;
}
this.cleanGroup();
try{
var s=this.group,c=this.scaler,t=this.ticks,_3d,f=_5.getTransformerFromModel(this.scaler),_3f=dojox.gfx.renderer=="canvas",_40=_3f||this.opt.htmlLabels&&!dojo.isIE&&!dojo.isOpera?"html":"gfx",dx=_2f.x*_34.length,dy=_2f.y*_34.length;
s.createLine({x1:_2c.x,y1:_2c.y,x2:_2d.x,y2:_2d.y}).setStroke(_33);
dojo.forEach(t.major,function(_43){
var _44=f(_43.value),_45,x=_2c.x+_2e.x*_44,y=_2c.y+_2e.y*_44;
s.createLine({x1:x,y1:y,x2:x+dx,y2:y+dy}).setStroke(_34);
if(_43.label){
_45=dc.axis2d.common.createText[_40](this.chart,s,x+_30.x,y+_30.y,_31,_43.label,_36,_37);
if(_40=="html"){
this.htmlElements.push(_45);
}
}
},this);
dx=_2f.x*_35.length;
dy=_2f.y*_35.length;
_3d=c.minMinorStep<=c.minor.tick*c.bounds.scale;
dojo.forEach(t.minor,function(_48){
var _49=f(_48.value),_4a,x=_2c.x+_2e.x*_49,y=_2c.y+_2e.y*_49;
s.createLine({x1:x,y1:y,x2:x+dx,y2:y+dy}).setStroke(_35);
if(_3d&&_48.label){
_4a=dc.axis2d.common.createText[_40](this.chart,s,x+_30.x,y+_30.y,_31,_48.label,_36,_37);
if(_40=="html"){
this.htmlElements.push(_4a);
}
}
},this);
dojo.forEach(t.micro,function(_4d){
var _4e=f(_4d.value),_4f,x=_2c.x+_2e.x*_4e,y=_2c.y+_2e.y*_4e;
s.createLine({x1:x,y1:y,x2:x+dx,y2:y+dy}).setStroke(_35);
},this);
}
catch(e){
}
this.dirty=false;
return this;
}});
})();
}
