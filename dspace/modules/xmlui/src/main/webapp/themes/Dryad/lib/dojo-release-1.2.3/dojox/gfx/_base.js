/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.gfx._base"]){
dojo._hasResource["dojox.gfx._base"]=true;
dojo.provide("dojox.gfx._base");
(function(){
var g=dojox.gfx,b=g._base;
g._hasClass=function(_3,_4){
return ((" "+_3.getAttribute("className")+" ").indexOf(" "+_4+" ")>=0);
};
g._addClass=function(_5,_6){
var _7=_5.getAttribute("className");
if((" "+_7+" ").indexOf(" "+_6+" ")<0){
_5.setAttribute("className",_7+(_7?" ":"")+_6);
}
};
g._removeClass=function(_8,_9){
_8.setAttribute("className",_8.getAttribute("className").replace(new RegExp("(^|\\s+)"+_9+"(\\s+|$)"),"$1$2"));
};
b._getFontMeasurements=function(){
var _a={"1em":0,"1ex":0,"100%":0,"12pt":0,"16px":0,"xx-small":0,"x-small":0,"small":0,"medium":0,"large":0,"x-large":0,"xx-large":0};
if(dojo.isIE){
dojo.doc.documentElement.style.fontSize="100%";
}
var _b=dojo.doc.createElement("div");
_b.style.position="absolute";
_b.style.left="-100px";
_b.style.top="0";
_b.style.width="30px";
_b.style.height="1000em";
_b.style.border="0";
_b.style.margin="0";
_b.style.padding="0";
_b.style.outline="0";
_b.style.lineHeight="1";
_b.style.overflow="hidden";
dojo.body().appendChild(_b);
for(var p in _a){
_b.style.fontSize=p;
_a[p]=Math.round(_b.offsetHeight*12/16)*16/12/1000;
}
dojo.body().removeChild(_b);
_b=null;
return _a;
};
var _d=null;
b._getCachedFontMeasurements=function(_e){
if(_e||!_d){
_d=b._getFontMeasurements();
}
return _d;
};
var _f=null,_10={};
b._getTextBox=function(_11,_12,_13){
var m;
if(!_f){
m=_f=dojo.doc.createElement("div");
m.style.position="absolute";
m.style.left="-10000px";
m.style.top="0";
dojo.body().appendChild(m);
}else{
m=_f;
}
m.className="";
m.style.border="0";
m.style.margin="0";
m.style.padding="0";
m.style.outline="0";
if(arguments.length>1&&_12){
for(var i in _12){
if(i in _10){
continue;
}
m.style[i]=_12[i];
}
}
if(arguments.length>2&&_13){
m.className=_13;
}
m.innerHTML=_11;
return dojo.marginBox(m);
};
var _16=0;
b._getUniqueId=function(){
var id;
do{
id=dojo._scopeName+"Unique"+(++_16);
}while(dojo.byId(id));
return id;
};
})();
dojo.mixin(dojox.gfx,{defaultPath:{type:"path",path:""},defaultPolyline:{type:"polyline",points:[]},defaultRect:{type:"rect",x:0,y:0,width:100,height:100,r:0},defaultEllipse:{type:"ellipse",cx:0,cy:0,rx:200,ry:100},defaultCircle:{type:"circle",cx:0,cy:0,r:100},defaultLine:{type:"line",x1:0,y1:0,x2:100,y2:100},defaultImage:{type:"image",x:0,y:0,width:0,height:0,src:""},defaultText:{type:"text",x:0,y:0,text:"",align:"start",decoration:"none",rotated:false,kerning:true},defaultTextPath:{type:"textpath",text:"",align:"start",decoration:"none",rotated:false,kerning:true},defaultStroke:{type:"stroke",color:"black",style:"solid",width:1,cap:"butt",join:4},defaultLinearGradient:{type:"linear",x1:0,y1:0,x2:100,y2:100,colors:[{offset:0,color:"black"},{offset:1,color:"white"}]},defaultRadialGradient:{type:"radial",cx:0,cy:0,r:100,colors:[{offset:0,color:"black"},{offset:1,color:"white"}]},defaultPattern:{type:"pattern",x:0,y:0,width:0,height:0,src:""},defaultFont:{type:"font",style:"normal",variant:"normal",weight:"normal",size:"10pt",family:"serif"},normalizeColor:function(_18){
return (_18 instanceof dojo.Color)?_18:new dojo.Color(_18);
},normalizeParameters:function(_19,_1a){
if(_1a){
var _1b={};
for(var x in _19){
if(x in _1a&&!(x in _1b)){
_19[x]=_1a[x];
}
}
}
return _19;
},makeParameters:function(_1d,_1e){
if(!_1e){
return dojo.clone(_1d);
}
var _1f={};
for(var i in _1d){
if(!(i in _1f)){
_1f[i]=dojo.clone((i in _1e)?_1e[i]:_1d[i]);
}
}
return _1f;
},formatNumber:function(x,_22){
var val=x.toString();
if(val.indexOf("e")>=0){
val=x.toFixed(4);
}else{
var _24=val.indexOf(".");
if(_24>=0&&val.length-_24>5){
val=x.toFixed(4);
}
}
if(x<0){
return val;
}
return _22?" "+val:val;
},makeFontString:function(_25){
return _25.style+" "+_25.variant+" "+_25.weight+" "+_25.size+" "+_25.family;
},splitFontString:function(str){
var _27=dojo.clone(dojox.gfx.defaultFont);
var t=str.split(/\s+/);
do{
if(t.length<5){
break;
}
_27.style=t[0];
_27.varian=t[1];
_27.weight=t[2];
var i=t[3].indexOf("/");
_27.size=i<0?t[3]:t[3].substring(0,i);
var j=4;
if(i<0){
if(t[4]=="/"){
j=6;
break;
}
if(t[4].substr(0,1)=="/"){
j=5;
break;
}
}
if(j+3>t.length){
break;
}
_27.size=t[j];
_27.family=t[j+1];
}while(false);
return _27;
},cm_in_pt:72/2.54,mm_in_pt:7.2/2.54,px_in_pt:function(){
return dojox.gfx._base._getCachedFontMeasurements()["12pt"]/12;
},pt2px:function(len){
return len*dojox.gfx.px_in_pt();
},px2pt:function(len){
return len/dojox.gfx.px_in_pt();
},normalizedLength:function(len){
if(len.length==0){
return 0;
}
if(len.length>2){
var _2e=dojox.gfx.px_in_pt();
var val=parseFloat(len);
switch(len.slice(-2)){
case "px":
return val;
case "pt":
return val*_2e;
case "in":
return val*72*_2e;
case "pc":
return val*12*_2e;
case "mm":
return val*dojox.gfx.mm_in_pt*_2e;
case "cm":
return val*dojox.gfx.cm_in_pt*_2e;
}
}
return parseFloat(len);
},pathVmlRegExp:/([A-Za-z]+)|(\d+(\.\d+)?)|(\.\d+)|(-\d+(\.\d+)?)|(-\.\d+)/g,pathSvgRegExp:/([A-Za-z])|(\d+(\.\d+)?)|(\.\d+)|(-\d+(\.\d+)?)|(-\.\d+)/g,equalSources:function(a,b){
return a&&b&&a==b;
}});
}
