/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.Color"]){
dojo._hasResource["dojo._base.Color"]=true;
dojo.provide("dojo._base.Color");
dojo.require("dojo._base.array");
dojo.require("dojo._base.lang");
dojo.Color=function(_1){
if(_1){
this.setColor(_1);
}
};
dojo.Color.named={black:[0,0,0],silver:[192,192,192],gray:[128,128,128],white:[255,255,255],maroon:[128,0,0],red:[255,0,0],purple:[128,0,128],fuchsia:[255,0,255],green:[0,128,0],lime:[0,255,0],olive:[128,128,0],yellow:[255,255,0],navy:[0,0,128],blue:[0,0,255],teal:[0,128,128],aqua:[0,255,255]};
dojo.extend(dojo.Color,{r:255,g:255,b:255,a:1,_set:function(r,g,b,a){
var t=this;
t.r=r;
t.g=g;
t.b=b;
t.a=a;
},setColor:function(_7){
var d=dojo;
if(d.isString(_7)){
d.colorFromString(_7,this);
}else{
if(d.isArray(_7)){
d.colorFromArray(_7,this);
}else{
this._set(_7.r,_7.g,_7.b,_7.a);
if(!(_7 instanceof d.Color)){
this.sanitize();
}
}
}
return this;
},sanitize:function(){
return this;
},toRgb:function(){
var t=this;
return [t.r,t.g,t.b];
},toRgba:function(){
var t=this;
return [t.r,t.g,t.b,t.a];
},toHex:function(){
var _b=dojo.map(["r","g","b"],function(x){
var s=this[x].toString(16);
return s.length<2?"0"+s:s;
},this);
return "#"+_b.join("");
},toCss:function(_e){
var t=this,rgb=t.r+", "+t.g+", "+t.b;
return (_e?"rgba("+rgb+", "+t.a:"rgb("+rgb)+")";
},toString:function(){
return this.toCss(true);
}});
dojo.blendColors=function(_11,end,_13,obj){
var d=dojo,t=obj||new dojo.Color();
d.forEach(["r","g","b","a"],function(x){
t[x]=_11[x]+(end[x]-_11[x])*_13;
if(x!="a"){
t[x]=Math.round(t[x]);
}
});
return t.sanitize();
};
dojo.colorFromRgb=function(_18,obj){
var m=_18.toLowerCase().match(/^rgba?\(([\s\.,0-9]+)\)/);
return m&&dojo.colorFromArray(m[1].split(/\s*,\s*/),obj);
};
dojo.colorFromHex=function(_1b,obj){
var d=dojo,t=obj||new d.Color(),_1f=(_1b.length==4)?4:8,_20=(1<<_1f)-1;
_1b=Number("0x"+_1b.substr(1));
if(isNaN(_1b)){
return null;
}
d.forEach(["b","g","r"],function(x){
var c=_1b&_20;
_1b>>=_1f;
t[x]=_1f==4?17*c:c;
});
t.a=1;
return t;
};
dojo.colorFromArray=function(a,obj){
var t=obj||new dojo.Color();
t._set(Number(a[0]),Number(a[1]),Number(a[2]),Number(a[3]));
if(isNaN(t.a)){
t.a=1;
}
return t.sanitize();
};
dojo.colorFromString=function(str,obj){
var a=dojo.Color.named[str];
return a&&dojo.colorFromArray(a,obj)||dojo.colorFromRgb(str,obj)||dojo.colorFromHex(str,obj);
};
}
