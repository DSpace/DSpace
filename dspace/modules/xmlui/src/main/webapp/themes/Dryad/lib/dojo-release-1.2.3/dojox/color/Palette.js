/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.color.Palette"]){
dojo._hasResource["dojox.color.Palette"]=true;
dojo.provide("dojox.color.Palette");
dojo.require("dojox.color");
(function(){
var _1=dojox.color;
_1.Palette=function(_2){
this.colors=[];
if(_2 instanceof dojox.color.Palette){
this.colors=_2.colors.slice(0);
}else{
if(_2 instanceof dojox.color.Color){
this.colors=[null,null,_2,null,null];
}else{
if(dojo.isArray(_2)){
this.colors=dojo.map(_2.slice(0),function(_3){
if(dojo.isString(_3)){
return new dojox.color.Color(_3);
}
return _3;
});
}else{
if(dojo.isString(_2)){
this.colors=[null,null,new dojox.color.Color(_2),null,null];
}
}
}
}
};
function tRGBA(p,_5,_6){
var _7=new dojox.color.Palette();
_7.colors=[];
dojo.forEach(p.colors,function(_8){
var r=(_5=="dr")?_8.r+_6:_8.r,g=(_5=="dg")?_8.g+_6:_8.g,b=(_5=="db")?_8.b+_6:_8.b,a=(_5=="da")?_8.a+_6:_8.a;
_7.colors.push(new dojox.color.Color({r:Math.min(255,Math.max(0,r)),g:Math.min(255,Math.max(0,g)),b:Math.min(255,Math.max(0,b)),a:Math.min(1,Math.max(0,a))}));
});

return _7;
};
function tCMY(p,_e,_f){
var ret=new dojox.color.Palette();
ret.colors=[];
dojo.forEach(p.colors,function(_11){
var o=_11.toCmy(),c=(_e=="dc")?o.c+_f:o.c,m=(_e=="dm")?o.m+_f:o.m,y=(_e=="dy")?o.y+_f:o.y;
ret.colors.push(dojox.color.fromCmy(Math.min(100,Math.max(0,c)),Math.min(100,Math.max(0,m)),Math.min(100,Math.max(0,y))));
});
return ret;
};
function tCMYK(p,_17,val){
var ret=new dojox.color.Palette();
ret.colors=[];
dojo.forEach(p.colors,function(_1a){
var o=_1a.toCmyk(),c=(_17=="dc")?o.c+val:o.c,m=(_17=="dm")?o.m+val:o.m,y=(_17=="dy")?o.y+val:o.y,k=(_17=="dk")?o.b+val:o.b;
ret.colors.push(dojox.color.fromCmyk(Math.min(100,Math.max(0,c)),Math.min(100,Math.max(0,m)),Math.min(100,Math.max(0,y)),Math.min(100,Math.max(0,k))));
});
return ret;
};
function tHSL(p,_21,val){
var ret=new dojox.color.Palette();
ret.colors=[];
dojo.forEach(p.colors,function(_24){
var o=_24.toHsl(),h=(_21=="dh")?o.h+val:o.h,s=(_21=="ds")?o.s+val:o.s,l=(_21=="dl")?o.l+val:o.l;
ret.colors.push(dojox.color.fromHsl(h%360,Math.min(100,Math.max(0,s)),Math.min(100,Math.max(0,l))));
});
return ret;
};
function tHSV(p,_2a,val){
var ret=new dojox.color.Palette();
ret.colors=[];
dojo.forEach(p.colors,function(_2d){
var o=_2d.toHsv(),h=(_2a=="dh")?o.h+val:o.h,s=(_2a=="ds")?o.s+val:o.s,v=(_2a=="dv")?o.v+val:o.v;
ret.colors.push(dojox.color.fromHsv(h%360,Math.min(100,Math.max(0,s)),Math.min(100,Math.max(0,v))));
});
return ret;
};
function rangeDiff(val,low,_34){
return _34-((_34-val)*((_34-low)/_34));
};
dojo.extend(_1.Palette,{transform:function(_35){
var fn=tRGBA;
if(_35.use){
var use=_35.use.toLowerCase();
if(use.indexOf("hs")==0){
if(use.charAt(2)=="l"){
fn=tHSL;
}else{
fn=tHSV;
}
}else{
if(use.indexOf("cmy")==0){
if(use.charAt(3)=="k"){
fn=tCMYK;
}else{
fn=tCMY;
}
}
}
}else{
if("dc" in _35||"dm" in _35||"dy" in _35){
if("dk" in _35){
fn=tCMYK;
}else{
fn=tCMY;
}
}else{
if("dh" in _35||"ds" in _35){
if("dv" in _35){
fn=tHSV;
}else{
fn=tHSL;
}
}
}
}
var _38=this;
for(var p in _35){
if(p=="use"){
continue;
}
_38=fn(_38,p,_35[p]);
}
return _38;
},clone:function(){
return new _1.Palette(this);
}});
dojo.mixin(_1.Palette,{generators:{analogous:function(_3a){
var _3b=_3a.high||60,low=_3a.low||18,_3d=dojo.isString(_3a.base)?new dojox.color.Color(_3a.base):_3a.base,hsv=_3d.toHsv();
var h=[(hsv.h+low+360)%360,(hsv.h+Math.round(low/2)+360)%360,hsv.h,(hsv.h-Math.round(_3b/2)+360)%360,(hsv.h-_3b+360)%360];
var s1=Math.max(10,(hsv.s<=95)?hsv.s+5:(100-(hsv.s-95))),s2=(hsv.s>1)?hsv.s-1:21-hsv.s,v1=(hsv.v>=92)?hsv.v-9:Math.max(hsv.v+9,20),v2=(hsv.v<=90)?Math.max(hsv.v+5,20):(95+Math.ceil((hsv.v-90)/2)),s=[s1,s2,hsv.s,s1,s1],v=[v1,v2,hsv.v,v1,v2];
return new _1.Palette(dojo.map(h,function(hue,i){
return dojox.color.fromHsv(hue,s[i],v[i]);
}));
},monochromatic:function(_48){
var _49=dojo.isString(_48.base)?new dojox.color.Color(_48.base):_48.base,hsv=_49.toHsv();
var s1=(hsv.s-30>9)?hsv.s-30:hsv.s+30,s2=hsv.s,v1=rangeDiff(hsv.v,20,100),v2=(hsv.v-20>20)?hsv.v-20:hsv.v+60,v3=(hsv.v-50>20)?hsv.v-50:hsv.v+30;
return new _1.Palette([dojox.color.fromHsv(hsv.h,s1,v1),dojox.color.fromHsv(hsv.h,s2,v3),_49,dojox.color.fromHsv(hsv.h,s1,v3),dojox.color.fromHsv(hsv.h,s2,v2)]);
},triadic:function(_50){
var _51=dojo.isString(_50.base)?new dojox.color.Color(_50.base):_50.base,hsv=_51.toHsv();
var h1=(hsv.h+57+360)%360,h2=(hsv.h-157+360)%360,s1=(hsv.s>20)?hsv.s-10:hsv.s+10,s2=(hsv.s>90)?hsv.s-10:hsv.s+10,s3=(hsv.s>95)?hsv.s-5:hsv.s+5,v1=(hsv.v-20>20)?hsv.v-20:hsv.v+20,v2=(hsv.v-30>20)?hsv.v-30:hsv.v+30,v3=(hsv.v-30>70)?hsv.v-30:hsv.v+30;
return new _1.Palette([dojox.color.fromHsv(h1,s1,hsv.v),dojox.color.fromHsv(hsv.h,s2,v2),_51,dojox.color.fromHsv(h2,s2,v1),dojox.color.fromHsv(h2,s3,v3)]);
},complementary:function(_5b){
var _5c=dojo.isString(_5b.base)?new dojox.color.Color(_5b.base):_5b.base,hsv=_5c.toHsv();
var h1=((hsv.h*2)+137<360)?(hsv.h*2)+137:Math.floor(hsv.h/2)-137,s1=Math.max(hsv.s-10,0),s2=rangeDiff(hsv.s,10,100),s3=Math.min(100,hsv.s+20),v1=Math.min(100,hsv.v+30),v2=(hsv.v>20)?hsv.v-30:hsv.v+30;
return new _1.Palette([dojox.color.fromHsv(hsv.h,s1,v1),dojox.color.fromHsv(hsv.h,s2,v2),_5c,dojox.color.fromHsv(h1,s3,v2),dojox.color.fromHsv(h1,hsv.s,hsv.v)]);
},splitComplementary:function(_64){
var _65=dojo.isString(_64.base)?new dojox.color.Color(_64.base):_64.base,_66=_64.da||30,hsv=_65.toHsv();
var _68=((hsv.h*2)+137<360)?(hsv.h*2)+137:Math.floor(hsv.h/2)-137,h1=(_68-_66+360)%360,h2=(_68+_66)%360,s1=Math.max(hsv.s-10,0),s2=rangeDiff(hsv.s,10,100),s3=Math.min(100,hsv.s+20),v1=Math.min(100,hsv.v+30),v2=(hsv.v>20)?hsv.v-30:hsv.v+30;
return new _1.Palette([dojox.color.fromHsv(h1,s1,v1),dojox.color.fromHsv(h1,s2,v2),_65,dojox.color.fromHsv(h2,s3,v2),dojox.color.fromHsv(h2,hsv.s,hsv.v)]);
},compound:function(_70){
var _71=dojo.isString(_70.base)?new dojox.color.Color(_70.base):_70.base,hsv=_71.toHsv();
var h1=((hsv.h*2)+18<360)?(hsv.h*2)+18:Math.floor(hsv.h/2)-18,h2=((hsv.h*2)+120<360)?(hsv.h*2)+120:Math.floor(hsv.h/2)-120,h3=((hsv.h*2)+99<360)?(hsv.h*2)+99:Math.floor(hsv.h/2)-99,s1=(hsv.s-40>10)?hsv.s-40:hsv.s+40,s2=(hsv.s-10>80)?hsv.s-10:hsv.s+10,s3=(hsv.s-25>10)?hsv.s-25:hsv.s+25,v1=(hsv.v-40>10)?hsv.v-40:hsv.v+40,v2=(hsv.v-20>80)?hsv.v-20:hsv.v+20,v3=Math.max(hsv.v,20);
return new _1.Palette([dojox.color.fromHsv(h1,s1,v1),dojox.color.fromHsv(h1,s2,v2),_71,dojox.color.fromHsv(h2,s3,v3),dojox.color.fromHsv(h3,s2,v2)]);
},shades:function(_7c){
var _7d=dojo.isString(_7c.base)?new dojox.color.Color(_7c.base):_7c.base,hsv=_7d.toHsv();
var s=(hsv.s==100&&hsv.v==0)?0:hsv.s,v1=(hsv.v-50>20)?hsv.v-50:hsv.v+30,v2=(hsv.v-25>=20)?hsv.v-25:hsv.v+55,v3=(hsv.v-75>=20)?hsv.v-75:hsv.v+5,v4=Math.max(hsv.v-10,20);
return new _1.Palette([new dojox.color.fromHsv(hsv.h,s,v1),new dojox.color.fromHsv(hsv.h,s,v2),_7d,new dojox.color.fromHsv(hsv.h,s,v3),new dojox.color.fromHsv(hsv.h,s,v4)]);
}},generate:function(_84,_85){
if(dojo.isFunction(_85)){
return _85({base:_84});
}else{
if(_1.Palette.generators[_85]){
return _1.Palette.generators[_85]({base:_84});
}
}
throw new Error("dojox.color.Palette.generate: the specified generator ('"+_85+"') does not exist.");
}});
})();
}
