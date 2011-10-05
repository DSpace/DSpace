/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.math.curves"]){
dojo._hasResource["dojox.math.curves"]=true;
dojo.provide("dojox.math.curves");
dojo.mixin(dojox.math.curves,{Line:function(_1,_2){
this.start=_1;
this.end=_2;
this.dimensions=_1.length;
for(var i=0;i<_1.length;i++){
_1[i]=Number(_1[i]);
}
for(var i=0;i<_2.length;i++){
_2[i]=Number(_2[i]);
}
this.getValue=function(n){
var _5=new Array(this.dimensions);
for(var i=0;i<this.dimensions;i++){
_5[i]=((this.end[i]-this.start[i])*n)+this.start[i];
}
return _5;
};
return this;
},Bezier:function(_7){
this.getValue=function(_8){
if(_8>=1){
return this.p[this.p.length-1];
}
if(_8<=0){
return this.p[0];
}
var _9=new Array(this.p[0].length);
for(var k=0;j<this.p[0].length;k++){
_9[k]=0;
}
for(var j=0;j<this.p[0].length;j++){
var C=0;
var D=0;
for(var i=0;i<this.p.length;i++){
C+=this.p[i][j]*this.p[this.p.length-1][0]*dojox.math.bernstein(_8,this.p.length,i);
}
for(var l=0;l<this.p.length;l++){
D+=this.p[this.p.length-1][0]*dojox.math.bernstein(_8,this.p.length,l);
}
_9[j]=C/D;
}
return _9;
};
this.p=_7;
return this;
},CatmullRom:function(_10,c){
this.getValue=function(_12){
var _13=_12*(this.p.length-1);
var _14=Math.floor(_13);
var _15=_13-_14;
var i0=_14-1;
if(i0<0){
i0=0;
}
var i=_14;
var i1=_14+1;
if(i1>=this.p.length){
i1=this.p.length-1;
}
var i2=_14+2;
if(i2>=this.p.length){
i2=this.p.length-1;
}
var u=_15;
var u2=_15*_15;
var u3=_15*_15*_15;
var _1d=new Array(this.p[0].length);
for(var k=0;k<this.p[0].length;k++){
var x1=(-this.c*this.p[i0][k])+((2-this.c)*this.p[i][k])+((this.c-2)*this.p[i1][k])+(this.c*this.p[i2][k]);
var x2=(2*this.c*this.p[i0][k])+((this.c-3)*this.p[i][k])+((3-2*this.c)*this.p[i1][k])+(-this.c*this.p[i2][k]);
var x3=(-this.c*this.p[i0][k])+(this.c*this.p[i1][k]);
var x4=this.p[i][k];
_1d[k]=x1*u3+x2*u2+x3*u+x4;
}
return _1d;
};
if(!c){
this.c=0.7;
}else{
this.c=c;
}
this.p=_10;
return this;
},Arc:function(_23,end,ccw){
function translate(a,b){
var c=new Array(a.length);
for(var i=0;i<a.length;i++){
c[i]=a[i]+b[i];
}
return c;
};
function invert(a){
var b=new Array(a.length);
for(var i=0;i<a.length;i++){
b[i]=-a[i];
}
return b;
};
var _2d=dojox.math.midpoint(_23,end);
var _2e=translate(invert(_2d),_23);
var rad=Math.sqrt(Math.pow(_2e[0],2)+Math.pow(_2e[1],2));
var _30=dojox.math.radiansToDegrees(Math.atan(_2e[1]/_2e[0]));
if(_2e[0]<0){
_30-=90;
}else{
_30+=90;
}
dojox.math.curves.CenteredArc.call(this,_2d,rad,_30,_30+(ccw?-180:180));
},CenteredArc:function(_31,_32,_33,end){
this.center=_31;
this.radius=_32;
this.start=_33||0;
this.end=end;
this.getValue=function(n){
var _36=new Array(2);
var _37=dojox.math.degreesToRadians(this.start+((this.end-this.start)*n));
_36[0]=this.center[0]+this.radius*Math.sin(_37);
_36[1]=this.center[1]-this.radius*Math.cos(_37);
return _36;
};
return this;
},Circle:function(_38,_39){
dojox.math.curves.CenteredArc.call(this,_38,_39,0,360);
return this;
},Path:function(){
var _3a=[];
var _3b=[];
var _3c=[];
var _3d=0;
this.add=function(_3e,_3f){
if(_3f<0){
console.error("dojox.math.curves.Path.add: weight cannot be less than 0");
}
_3a.push(_3e);
_3b.push(_3f);
_3d+=_3f;
computeRanges();
};
this.remove=function(_40){
for(var i=0;i<_3a.length;i++){
if(_3a[i]==_40){
_3a.splice(i,1);
_3d-=_3b.splice(i,1)[0];
break;
}
}
computeRanges();
};
this.removeAll=function(){
_3a=[];
_3b=[];
_3d=0;
};
this.getValue=function(n){
var _43=false,_44=0;
for(var i=0;i<_3c.length;i++){
var r=_3c[i];
if(n>=r[0]&&n<r[1]){
var _47=(n-r[0])/r[2];
_44=_3a[i].getValue(_47);
_43=true;
break;
}
}
if(!_43){
_44=_3a[_3a.length-1].getValue(1);
}
for(var j=0;j<i;j++){
_44=dojox.math.points.translate(_44,_3a[j].getValue(1));
}
return _44;
};
function computeRanges(){
var _49=0;
for(var i=0;i<_3b.length;i++){
var end=_49+_3b[i]/_3d;
var len=end-_49;
_3c[i]=[_49,end,len];
_49=end;
}
};
return this;
}});
}
