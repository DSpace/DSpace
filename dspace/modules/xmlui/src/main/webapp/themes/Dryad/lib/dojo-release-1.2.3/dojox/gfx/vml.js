/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.gfx.vml"]){
dojo._hasResource["dojox.gfx.vml"]=true;
dojo.provide("dojox.gfx.vml");
dojo.require("dojox.gfx._base");
dojo.require("dojox.gfx.shape");
dojo.require("dojox.gfx.path");
dojo.require("dojox.gfx.arc");
dojox.gfx.vml.xmlns="urn:schemas-microsoft-com:vml";
dojox.gfx.vml.text_alignment={start:"left",middle:"center",end:"right"};
dojox.gfx.vml._parseFloat=function(_1){
return _1.match(/^\d+f$/i)?parseInt(_1)/65536:parseFloat(_1);
};
dojox.gfx.vml._bool={"t":1,"true":1};
dojo.extend(dojox.gfx.Shape,{setFill:function(_2){
if(!_2){
this.fillStyle=null;
this.rawNode.filled="f";
return this;
}
if(typeof _2=="object"&&"type" in _2){
var i,f,fo,a,s;
switch(_2.type){
case "linear":
var _8=this._getRealMatrix(),m=dojox.gfx.matrix;
s=[];
f=dojox.gfx.makeParameters(dojox.gfx.defaultLinearGradient,_2);
a=f.colors;
this.fillStyle=f;
dojo.forEach(a,function(v,i,a){
a[i].color=dojox.gfx.normalizeColor(v.color);
});
if(a[0].offset>0){
s.push("0 "+a[0].color.toHex());
}
for(i=0;i<a.length;++i){
s.push(a[i].offset.toFixed(8)+" "+a[i].color.toHex());
}
i=a.length-1;
if(a[i].offset<1){
s.push("1 "+a[i].color.toHex());
}
fo=this.rawNode.fill;
fo.colors.value=s.join(";");
fo.method="sigma";
fo.type="gradient";
var _d=_8?m.multiplyPoint(_8,f.x1,f.y1):{x:f.x1,y:f.y1},_e=_8?m.multiplyPoint(_8,f.x2,f.y2):{x:f.x2,y:f.y2};
fo.angle=(m._radToDeg(Math.atan2(_e.x-_d.x,_e.y-_d.y))+180)%360;
fo.on=true;
break;
case "radial":
f=dojox.gfx.makeParameters(dojox.gfx.defaultRadialGradient,_2);
this.fillStyle=f;
var l=parseFloat(this.rawNode.style.left),t=parseFloat(this.rawNode.style.top),w=parseFloat(this.rawNode.style.width),h=parseFloat(this.rawNode.style.height),c=isNaN(w)?1:2*f.r/w;
a=[];
if(f.colors[0].offset>0){
a.push({offset:1,color:dojox.gfx.normalizeColor(f.colors[0].color)});
}
dojo.forEach(f.colors,function(v,i){
a.push({offset:1-v.offset*c,color:dojox.gfx.normalizeColor(v.color)});
});
i=a.length-1;
while(i>=0&&a[i].offset<0){
--i;
}
if(i<a.length-1){
var q=a[i],p=a[i+1];
p.color=dojo.blendColors(q.color,p.color,q.offset/(q.offset-p.offset));
p.offset=0;
while(a.length-i>2){
a.pop();
}
}
i=a.length-1,s=[];
if(a[i].offset>0){
s.push("0 "+a[i].color.toHex());
}
for(;i>=0;--i){
s.push(a[i].offset.toFixed(8)+" "+a[i].color.toHex());
}
fo=this.rawNode.fill;
fo.colors.value=s.join(";");
fo.method="sigma";
fo.type="gradientradial";
if(isNaN(w)||isNaN(h)||isNaN(l)||isNaN(t)){
fo.focusposition="0.5 0.5";
}else{
fo.focusposition=((f.cx-l)/w).toFixed(8)+" "+((f.cy-t)/h).toFixed(8);
}
fo.focussize="0 0";
fo.on=true;
break;
case "pattern":
f=dojox.gfx.makeParameters(dojox.gfx.defaultPattern,_2);
this.fillStyle=f;
fo=this.rawNode.fill;
fo.type="tile";
fo.src=f.src;
if(f.width&&f.height){
fo.size.x=dojox.gfx.px2pt(f.width);
fo.size.y=dojox.gfx.px2pt(f.height);
}
fo.alignShape="f";
fo.position.x=0;
fo.position.y=0;
fo.origin.x=f.width?f.x/f.width:0;
fo.origin.y=f.height?f.y/f.height:0;
fo.on=true;
break;
}
this.rawNode.fill.opacity=1;
return this;
}
this.fillStyle=dojox.gfx.normalizeColor(_2);
this.rawNode.fill.method="any";
this.rawNode.fill.type="solid";
this.rawNode.fillcolor=this.fillStyle.toHex();
this.rawNode.fill.opacity=this.fillStyle.a;
this.rawNode.filled=true;
return this;
},setStroke:function(_18){
if(!_18){
this.strokeStyle=null;
this.rawNode.stroked="f";
return this;
}
if(typeof _18=="string"||dojo.isArray(_18)||_18 instanceof dojo.Color){
_18={color:_18};
}
var s=this.strokeStyle=dojox.gfx.makeParameters(dojox.gfx.defaultStroke,_18);
s.color=dojox.gfx.normalizeColor(s.color);
var rn=this.rawNode;
rn.stroked=true;
rn.strokecolor=s.color.toCss();
rn.strokeweight=s.width+"px";
if(rn.stroke){
rn.stroke.opacity=s.color.a;
rn.stroke.endcap=this._translate(this._capMap,s.cap);
if(typeof s.join=="number"){
rn.stroke.joinstyle="miter";
rn.stroke.miterlimit=s.join;
}else{
rn.stroke.joinstyle=s.join;
}
rn.stroke.dashstyle=s.style=="none"?"Solid":s.style;
}
return this;
},_capMap:{butt:"flat"},_capMapReversed:{flat:"butt"},_translate:function(_1b,_1c){
return (_1c in _1b)?_1b[_1c]:_1c;
},_applyTransform:function(){
if(this.fillStyle&&this.fillStyle.type=="linear"){
this.setFill(this.fillStyle);
}
var _1d=this._getRealMatrix();
if(!_1d){
return this;
}
var _1e=this.rawNode.skew;
if(typeof _1e=="undefined"){
for(var i=0;i<this.rawNode.childNodes.length;++i){
if(this.rawNode.childNodes[i].tagName=="skew"){
_1e=this.rawNode.childNodes[i];
break;
}
}
}
if(_1e){
_1e.on="f";
var mt=_1d.xx.toFixed(8)+" "+_1d.xy.toFixed(8)+" "+_1d.yx.toFixed(8)+" "+_1d.yy.toFixed(8)+" 0 0",_21=Math.floor(_1d.dx).toFixed()+"px "+Math.floor(_1d.dy).toFixed()+"px",s=this.rawNode.style,l=parseFloat(s.left),t=parseFloat(s.top),w=parseFloat(s.width),h=parseFloat(s.height);
if(isNaN(l)){
l=0;
}
if(isNaN(t)){
t=0;
}
if(isNaN(w)){
w=1;
}
if(isNaN(h)){
h=1;
}
var _27=(-l/w-0.5).toFixed(8)+" "+(-t/h-0.5).toFixed(8);
_1e.matrix=mt;
_1e.origin=_27;
_1e.offset=_21;
_1e.on=true;
}
return this;
},setRawNode:function(_28){
_28.stroked="f";
_28.filled="f";
this.rawNode=_28;
},_moveToFront:function(){
this.rawNode.parentNode.appendChild(this.rawNode);
return this;
},_moveToBack:function(){
var r=this.rawNode,p=r.parentNode,n=p.firstChild;
p.insertBefore(r,n);
if(n.tagName=="rect"){
n.swapNode(r);
}
return this;
},_getRealMatrix:function(){
return this.parentMatrix?new dojox.gfx.Matrix2D([this.parentMatrix,this.matrix]):this.matrix;
}});
dojo.declare("dojox.gfx.Group",dojox.gfx.Shape,{constructor:function(){
dojox.gfx.vml.Container._init.call(this);
},_applyTransform:function(){
var _2c=this._getRealMatrix();
for(var i=0;i<this.children.length;++i){
this.children[i]._updateParentMatrix(_2c);
}
return this;
}});
dojox.gfx.Group.nodeType="group";
dojo.declare("dojox.gfx.Rect",dojox.gfx.shape.Rect,{setShape:function(_2e){
var _2f=this.shape=dojox.gfx.makeParameters(this.shape,_2e);
this.bbox=null;
var _30=this.rawNode.style;
_30.left=_2f.x.toFixed();
_30.top=_2f.y.toFixed();
_30.width=(typeof _2f.width=="string"&&_2f.width.indexOf("%")>=0)?_2f.width:_2f.width.toFixed();
_30.height=(typeof _2f.width=="string"&&_2f.height.indexOf("%")>=0)?_2f.height:_2f.height.toFixed();
var r=Math.min(1,(_2f.r/Math.min(parseFloat(_2f.width),parseFloat(_2f.height)))).toFixed(8);
var _32=this.rawNode.parentNode,_33=null;
if(_32){
if(_32.lastChild!=this.rawNode){
for(var i=0;i<_32.childNodes.length;++i){
if(_32.childNodes[i]==this.rawNode){
_33=_32.childNodes[i+1];
break;
}
}
}
_32.removeChild(this.rawNode);
}
this.rawNode.arcsize=r;
if(_32){
if(_33){
_32.insertBefore(this.rawNode,_33);
}else{
_32.appendChild(this.rawNode);
}
}
return this.setTransform(this.matrix).setFill(this.fillStyle).setStroke(this.strokeStyle);
}});
dojox.gfx.Rect.nodeType="roundrect";
dojo.declare("dojox.gfx.Ellipse",dojox.gfx.shape.Ellipse,{setShape:function(_35){
var _36=this.shape=dojox.gfx.makeParameters(this.shape,_35);
this.bbox=null;
var _37=this.rawNode.style;
_37.left=(_36.cx-_36.rx).toFixed();
_37.top=(_36.cy-_36.ry).toFixed();
_37.width=(_36.rx*2).toFixed();
_37.height=(_36.ry*2).toFixed();
return this.setTransform(this.matrix);
}});
dojox.gfx.Ellipse.nodeType="oval";
dojo.declare("dojox.gfx.Circle",dojox.gfx.shape.Circle,{setShape:function(_38){
var _39=this.shape=dojox.gfx.makeParameters(this.shape,_38);
this.bbox=null;
var _3a=this.rawNode.style;
_3a.left=(_39.cx-_39.r).toFixed();
_3a.top=(_39.cy-_39.r).toFixed();
_3a.width=(_39.r*2).toFixed();
_3a.height=(_39.r*2).toFixed();
return this;
}});
dojox.gfx.Circle.nodeType="oval";
dojo.declare("dojox.gfx.Line",dojox.gfx.shape.Line,{constructor:function(_3b){
if(_3b){
_3b.setAttribute("dojoGfxType","line");
}
},setShape:function(_3c){
var _3d=this.shape=dojox.gfx.makeParameters(this.shape,_3c);
this.bbox=null;
this.rawNode.path.v="m"+_3d.x1.toFixed()+" "+_3d.y1.toFixed()+"l"+_3d.x2.toFixed()+" "+_3d.y2.toFixed()+"e";
return this.setTransform(this.matrix);
}});
dojox.gfx.Line.nodeType="shape";
dojo.declare("dojox.gfx.Polyline",dojox.gfx.shape.Polyline,{constructor:function(_3e){
if(_3e){
_3e.setAttribute("dojoGfxType","polyline");
}
},setShape:function(_3f,_40){
if(_3f&&_3f instanceof Array){
this.shape=dojox.gfx.makeParameters(this.shape,{points:_3f});
if(_40&&this.shape.points.length){
this.shape.points.push(this.shape.points[0]);
}
}else{
this.shape=dojox.gfx.makeParameters(this.shape,_3f);
}
this.bbox=null;
var _41=[],p=this.shape.points;
if(p.length>0){
_41.push("m");
var k=1;
if(typeof p[0]=="number"){
_41.push(p[0].toFixed());
_41.push(p[1].toFixed());
k=2;
}else{
_41.push(p[0].x.toFixed());
_41.push(p[0].y.toFixed());
}
if(p.length>k){
_41.push("l");
for(var i=k;i<p.length;++i){
if(typeof p[i]=="number"){
_41.push(p[i].toFixed());
}else{
_41.push(p[i].x.toFixed());
_41.push(p[i].y.toFixed());
}
}
}
}
_41.push("e");
this.rawNode.path.v=_41.join(" ");
return this.setTransform(this.matrix);
}});
dojox.gfx.Polyline.nodeType="shape";
dojo.declare("dojox.gfx.Image",dojox.gfx.shape.Image,{constructor:function(_45){
if(_45){
_45.setAttribute("dojoGfxType","image");
}
},getEventSource:function(){
return this.rawNode?this.rawNode.firstChild:null;
},setShape:function(_46){
var _47=this.shape=dojox.gfx.makeParameters(this.shape,_46);
this.bbox=null;
this.rawNode.firstChild.src=_47.src;
return this.setTransform(this.matrix);
},_setDimensions:function(s,w,h){
if(w||h){
s.width=w+"px";
s.height=h+"px";
}
},_resetImage:function(){
var s=this.rawNode.firstChild.style,_4c=this.shape;
s.left="0px";
s.top="0px";
this._setDimensions(s,_4c.width,_4c.height);
},_applyTransform:function(){
var _4d=this._getRealMatrix(),img=this.rawNode.firstChild,s=img.style,_50=this.shape;
if(_4d){
_4d=dojox.gfx.matrix.multiply(_4d,{dx:_50.x,dy:_50.y});
}else{
_4d=dojox.gfx.matrix.normalize({dx:_50.x,dy:_50.y});
}
if(_4d.xy==0&&_4d.yx==0&&_4d.xx>0&&_4d.yy>0){
this.rawNode.style.filter="";
s.left=Math.floor(_4d.dx)+"px";
s.top=Math.floor(_4d.dy)+"px";
this._setDimensions(s,Math.floor(_4d.xx*_50.width),Math.floor(_4d.yy*_50.height));
}else{
this._resetImage();
var f=this.rawNode.filters["DXImageTransform.Microsoft.Matrix"];
if(f){
f.M11=_4d.xx;
f.M12=_4d.xy;
f.M21=_4d.yx;
f.M22=_4d.yy;
f.Dx=_4d.dx;
f.Dy=_4d.dy;
}else{
this.rawNode.style.filter="progid:DXImageTransform.Microsoft.Matrix(M11="+_4d.xx+", M12="+_4d.xy+", M21="+_4d.yx+", M22="+_4d.yy+", Dx="+_4d.dx+", Dy="+_4d.dy+")";
}
}
return this;
}});
dojox.gfx.Image.nodeType="div";
dojo.declare("dojox.gfx.Text",dojox.gfx.shape.Text,{constructor:function(_52){
if(_52){
_52.setAttribute("dojoGfxType","text");
}
this.fontStyle=null;
},_alignment:{start:"left",middle:"center",end:"right"},setShape:function(_53){
this.shape=dojox.gfx.makeParameters(this.shape,_53);
this.bbox=null;
var r=this.rawNode,s=this.shape,x=s.x,y=s.y.toFixed();
switch(s.align){
case "middle":
x-=5;
break;
case "end":
x-=10;
break;
}
this.rawNode.path.v="m"+x.toFixed()+","+y+"l"+(x+10).toFixed()+","+y+"e";
var p=null,t=null,c=r.childNodes;
for(var i=0;i<c.length;++i){
var tag=c[i].tagName;
if(tag=="path"){
p=c[i];
if(t){
break;
}
}else{
if(tag=="textpath"){
t=c[i];
if(p){
break;
}
}
}
}
if(!p){
p=this.rawNode.ownerDocument.createElement("v:path");
r.appendChild(p);
}
if(!t){
t=this.rawNode.ownerDocument.createElement("v:textpath");
r.appendChild(t);
}
p.textPathOk=true;
t.on=true;
var a=dojox.gfx.vml.text_alignment[s.align];
t.style["v-text-align"]=a?a:"left";
t.style["text-decoration"]=s.decoration;
t.style["v-rotate-letters"]=s.rotated;
t.style["v-text-kern"]=s.kerning;
t.string=s.text;
return this.setTransform(this.matrix);
},_setFont:function(){
var f=this.fontStyle,c=this.rawNode.childNodes;
for(var i=0;i<c.length;++i){
if(c[i].tagName=="textpath"){
c[i].style.font=dojox.gfx.makeFontString(f);
break;
}
}
this.setTransform(this.matrix);
},_getRealMatrix:function(){
var _61=dojox.gfx.Shape.prototype._getRealMatrix.call(this);
if(_61){
_61=dojox.gfx.matrix.multiply(_61,{dy:-dojox.gfx.normalizedLength(this.fontStyle?this.fontStyle.size:"10pt")*0.35});
}
return _61;
},getTextWidth:function(){
var _62=this.rawNode,_63=_62.style.display;
_62.style.display="inline";
var _64=dojox.gfx.pt2px(parseFloat(_62.currentStyle.width));
_62.style.display=_63;
return _64;
}});
dojox.gfx.Text.nodeType="shape";
dojox.gfx.path._calcArc=function(_65){
var _66=Math.cos(_65),_67=Math.sin(_65),p2={x:_66+(4/3)*(1-_66),y:_67-(4/3)*_66*(1-_66)/_67};
return {s:{x:_66,y:-_67},c1:{x:p2.x,y:-p2.y},c2:p2,e:{x:_66,y:_67}};
};
dojo.declare("dojox.gfx.Path",dojox.gfx.path.Path,{constructor:function(_69){
if(_69&&!_69.getAttribute("dojoGfxType")){
_69.setAttribute("dojoGfxType","path");
}
this.vmlPath="";
this.lastControl={};
},_updateWithSegment:function(_6a){
var _6b=dojo.clone(this.last);
dojox.gfx.Path.superclass._updateWithSegment.apply(this,arguments);
var _6c=this[this.renderers[_6a.action]](_6a,_6b);
if(typeof this.vmlPath=="string"){
this.vmlPath+=_6c.join("");
this.rawNode.path.v=this.vmlPath+" r0,0 e";
}else{
Array.prototype.push.apply(this.vmlPath,_6c);
}
},setShape:function(_6d){
this.vmlPath=[];
this.lastControl.type="";
dojox.gfx.Path.superclass.setShape.apply(this,arguments);
this.vmlPath=this.vmlPath.join("");
this.rawNode.path.v=this.vmlPath+" r0,0 e";
return this;
},_pathVmlToSvgMap:{m:"M",l:"L",t:"m",r:"l",c:"C",v:"c",qb:"Q",x:"z",e:""},renderers:{M:"_moveToA",m:"_moveToR",L:"_lineToA",l:"_lineToR",H:"_hLineToA",h:"_hLineToR",V:"_vLineToA",v:"_vLineToR",C:"_curveToA",c:"_curveToR",S:"_smoothCurveToA",s:"_smoothCurveToR",Q:"_qCurveToA",q:"_qCurveToR",T:"_qSmoothCurveToA",t:"_qSmoothCurveToR",A:"_arcTo",a:"_arcTo",Z:"_closePath",z:"_closePath"},_addArgs:function(_6e,_6f,_70,_71){
var n=_6f instanceof Array?_6f:_6f.args;
for(var i=_70;i<_71;++i){
_6e.push(" ",n[i].toFixed());
}
},_adjustRelCrd:function(_74,_75,_76){
var n=_75 instanceof Array?_75:_75.args,l=n.length,_79=new Array(l),i=0,x=_74.x,y=_74.y;
if(typeof x!="number"){
_79[0]=x=n[0];
_79[1]=y=n[1];
i=2;
}
if(typeof _76=="number"&&_76!=2){
var j=_76;
while(j<=l){
for(;i<j;i+=2){
_79[i]=x+n[i];
_79[i+1]=y+n[i+1];
}
x=_79[j-2];
y=_79[j-1];
j+=_76;
}
}else{
for(;i<l;i+=2){
_79[i]=(x+=n[i]);
_79[i+1]=(y+=n[i+1]);
}
}
return _79;
},_adjustRelPos:function(_7e,_7f){
var n=_7f instanceof Array?_7f:_7f.args,l=n.length,_82=new Array(l);
for(var i=0;i<l;++i){
_82[i]=(_7e+=n[i]);
}
return _82;
},_moveToA:function(_84){
var p=[" m"],n=_84 instanceof Array?_84:_84.args,l=n.length;
this._addArgs(p,n,0,2);
if(l>2){
p.push(" l");
this._addArgs(p,n,2,l);
}
this.lastControl.type="";
return p;
},_moveToR:function(_88,_89){
return this._moveToA(this._adjustRelCrd(_89,_88));
},_lineToA:function(_8a){
var p=[" l"],n=_8a instanceof Array?_8a:_8a.args;
this._addArgs(p,n,0,n.length);
this.lastControl.type="";
return p;
},_lineToR:function(_8d,_8e){
return this._lineToA(this._adjustRelCrd(_8e,_8d));
},_hLineToA:function(_8f,_90){
var p=[" l"],y=" "+_90.y.toFixed(),n=_8f instanceof Array?_8f:_8f.args,l=n.length;
for(var i=0;i<l;++i){
p.push(" ",n[i].toFixed(),y);
}
this.lastControl.type="";
return p;
},_hLineToR:function(_96,_97){
return this._hLineToA(this._adjustRelPos(_97.x,_96),_97);
},_vLineToA:function(_98,_99){
var p=[" l"],x=" "+_99.x.toFixed(),n=_98 instanceof Array?_98:_98.args,l=n.length;
for(var i=0;i<l;++i){
p.push(x," ",n[i].toFixed());
}
this.lastControl.type="";
return p;
},_vLineToR:function(_9f,_a0){
return this._vLineToA(this._adjustRelPos(_a0.y,_9f),_a0);
},_curveToA:function(_a1){
var p=[],n=_a1 instanceof Array?_a1:_a1.args,l=n.length,lc=this.lastControl;
for(var i=0;i<l;i+=6){
p.push(" c");
this._addArgs(p,n,i,i+6);
}
lc.x=n[l-4];
lc.y=n[l-3];
lc.type="C";
return p;
},_curveToR:function(_a7,_a8){
return this._curveToA(this._adjustRelCrd(_a8,_a7,6));
},_smoothCurveToA:function(_a9,_aa){
var p=[],n=_a9 instanceof Array?_a9:_a9.args,l=n.length,lc=this.lastControl,i=0;
if(lc.type!="C"){
p.push(" c");
this._addArgs(p,[_aa.x,_aa.y],0,2);
this._addArgs(p,n,0,4);
lc.x=n[0];
lc.y=n[1];
lc.type="C";
i=4;
}
for(;i<l;i+=4){
p.push(" c");
this._addArgs(p,[2*_aa.x-lc.x,2*_aa.y-lc.y],0,2);
this._addArgs(p,n,i,i+4);
lc.x=n[i];
lc.y=n[i+1];
}
return p;
},_smoothCurveToR:function(_b0,_b1){
return this._smoothCurveToA(this._adjustRelCrd(_b1,_b0,4),_b1);
},_qCurveToA:function(_b2){
var p=[],n=_b2 instanceof Array?_b2:_b2.args,l=n.length,lc=this.lastControl;
for(var i=0;i<l;i+=4){
p.push(" qb");
this._addArgs(p,n,i,i+4);
}
lc.x=n[l-4];
lc.y=n[l-3];
lc.type="Q";
return p;
},_qCurveToR:function(_b8,_b9){
return this._qCurveToA(this._adjustRelCrd(_b9,_b8,4));
},_qSmoothCurveToA:function(_ba,_bb){
var p=[],n=_ba instanceof Array?_ba:_ba.args,l=n.length,lc=this.lastControl,i=0;
if(lc.type!="Q"){
p.push(" qb");
this._addArgs(p,[lc.x=_bb.x,lc.y=_bb.y],0,2);
lc.type="Q";
this._addArgs(p,n,0,2);
i=2;
}
for(;i<l;i+=2){
p.push(" qb");
this._addArgs(p,[lc.x=2*_bb.x-lc.x,lc.y=2*_bb.y-lc.y],0,2);
this._addArgs(p,n,i,i+2);
}
return p;
},_qSmoothCurveToR:function(_c1,_c2){
return this._qSmoothCurveToA(this._adjustRelCrd(_c2,_c1,2),_c2);
},_arcTo:function(_c3,_c4){
var p=[],n=_c3.args,l=n.length,_c8=_c3.action=="a";
for(var i=0;i<l;i+=7){
var x1=n[i+5],y1=n[i+6];
if(_c8){
x1+=_c4.x;
y1+=_c4.y;
}
var _cc=dojox.gfx.arc.arcAsBezier(_c4,n[i],n[i+1],n[i+2],n[i+3]?1:0,n[i+4]?1:0,x1,y1);
for(var j=0;j<_cc.length;++j){
p.push(" c");
var t=_cc[j];
this._addArgs(p,t,0,t.length);
}
_c4.x=x1;
_c4.y=y1;
}
this.lastControl.type="";
return p;
},_closePath:function(){
this.lastControl.type="";
return ["x"];
}});
dojox.gfx.Path.nodeType="shape";
dojo.declare("dojox.gfx.TextPath",dojox.gfx.Path,{constructor:function(_cf){
if(_cf){
_cf.setAttribute("dojoGfxType","textpath");
}
this.fontStyle=null;
if(!("text" in this)){
this.text=dojo.clone(dojox.gfx.defaultTextPath);
}
if(!("fontStyle" in this)){
this.fontStyle=dojo.clone(dojox.gfx.defaultFont);
}
},setText:function(_d0){
this.text=dojox.gfx.makeParameters(this.text,typeof _d0=="string"?{text:_d0}:_d0);
this._setText();
return this;
},setFont:function(_d1){
this.fontStyle=typeof _d1=="string"?dojox.gfx.splitFontString(_d1):dojox.gfx.makeParameters(dojox.gfx.defaultFont,_d1);
this._setFont();
return this;
},_setText:function(){
this.bbox=null;
var r=this.rawNode,s=this.text,p=null,t=null,c=r.childNodes;
for(var i=0;i<c.length;++i){
var tag=c[i].tagName;
if(tag=="path"){
p=c[i];
if(t){
break;
}
}else{
if(tag=="textpath"){
t=c[i];
if(p){
break;
}
}
}
}
if(!p){
p=this.rawNode.ownerDocument.createElement("v:path");
r.appendChild(p);
}
if(!t){
t=this.rawNode.ownerDocument.createElement("v:textpath");
r.appendChild(t);
}
p.textPathOk=true;
t.on=true;
var a=dojox.gfx.vml.text_alignment[s.align];
t.style["v-text-align"]=a?a:"left";
t.style["text-decoration"]=s.decoration;
t.style["v-rotate-letters"]=s.rotated;
t.style["v-text-kern"]=s.kerning;
t.string=s.text;
},_setFont:function(){
var f=this.fontStyle,c=this.rawNode.childNodes;
for(var i=0;i<c.length;++i){
if(c[i].tagName=="textpath"){
c[i].style.font=dojox.gfx.makeFontString(f);
break;
}
}
}});
dojox.gfx.TextPath.nodeType="shape";
dojo.declare("dojox.gfx.Surface",dojox.gfx.shape.Surface,{constructor:function(){
dojox.gfx.vml.Container._init.call(this);
},setDimensions:function(_dd,_de){
this.width=dojox.gfx.normalizedLength(_dd);
this.height=dojox.gfx.normalizedLength(_de);
if(!this.rawNode){
return this;
}
var cs=this.clipNode.style,r=this.rawNode,rs=r.style,bs=this.bgNode.style;
cs.width=_dd;
cs.height=_de;
cs.clip="rect(0px "+_dd+"px "+_de+"px 0px)";
rs.width=_dd;
rs.height=_de;
r.coordsize=_dd+" "+_de;
bs.width=_dd;
bs.height=_de;
return this;
},getDimensions:function(){
var t=this.rawNode?{width:dojox.gfx.normalizedLength(this.rawNode.style.width),height:dojox.gfx.normalizedLength(this.rawNode.style.height)}:null;
if(t.width<=0){
t.width=this.width;
}
if(t.height<=0){
t.height=this.height;
}
return t;
}});
dojox.gfx.createSurface=function(_e4,_e5,_e6){
if(!_e5){
_e5="100%";
}
if(!_e6){
_e6="100%";
}
var s=new dojox.gfx.Surface(),p=dojo.byId(_e4),c=s.clipNode=p.ownerDocument.createElement("div"),r=s.rawNode=p.ownerDocument.createElement("v:group"),cs=c.style,rs=r.style;
s._parent=p;
s._nodes.push(c);
p.style.width=_e5;
p.style.height=_e6;
cs.position="absolute";
cs.width=_e5;
cs.height=_e6;
cs.clip="rect(0px "+_e5+"px "+_e6+"px 0px)";
rs.position="absolute";
rs.width=_e5;
rs.height=_e6;
r.coordsize=(_e5=="100%"?_e5:parseFloat(_e5))+" "+(_e6=="100%"?_e6:parseFloat(_e6));
r.coordorigin="0 0";
var b=s.bgNode=r.ownerDocument.createElement("v:rect"),bs=b.style;
bs.left=bs.top=0;
bs.width=rs.width;
bs.height=rs.height;
b.filled=b.stroked="f";
r.appendChild(b);
c.appendChild(r);
p.appendChild(c);
s.width=dojox.gfx.normalizedLength(_e5);
s.height=dojox.gfx.normalizedLength(_e6);
return s;
};
dojox.gfx.vml.Container={_init:function(){
dojox.gfx.shape.Container._init.call(this);
},add:function(_ef){
if(this!=_ef.getParent()){
this.rawNode.appendChild(_ef.rawNode);
if(!_ef.getParent()){
_ef.setFill(_ef.getFill());
_ef.setStroke(_ef.getStroke());
}
dojox.gfx.shape.Container.add.apply(this,arguments);
}
return this;
},remove:function(_f0,_f1){
if(this==_f0.getParent()){
if(this.rawNode==_f0.rawNode.parentNode){
this.rawNode.removeChild(_f0.rawNode);
}
dojox.gfx.shape.Container.remove.apply(this,arguments);
}
return this;
},clear:function(){
var r=this.rawNode;
while(r.firstChild!=r.lastChild){
if(r.firstChild!=this.bgNode){
r.removeChild(r.firstChild);
}
if(r.lastChild!=this.bgNode){
r.removeChild(r.lastChild);
}
}
return dojox.gfx.shape.Container.clear.apply(this,arguments);
},_moveChildToFront:dojox.gfx.shape.Container._moveChildToFront,_moveChildToBack:dojox.gfx.shape.Container._moveChildToBack};
dojo.mixin(dojox.gfx.shape.Creator,{createGroup:function(){
var g=this.createObject(dojox.gfx.Group,null);
var r=g.rawNode.ownerDocument.createElement("v:rect");
r.style.left=r.style.top=0;
r.style.width=g.rawNode.style.width;
r.style.height=g.rawNode.style.height;
r.filled=r.stroked="f";
g.rawNode.appendChild(r);
g.bgNode=r;
return g;
},createImage:function(_f5){
if(!this.rawNode){
return null;
}
var _f6=new dojox.gfx.Image(),_f7=this.rawNode.ownerDocument.createElement("div");
_f7.style.position="absolute";
_f7.style.width=this.rawNode.style.width;
_f7.style.height=this.rawNode.style.height;
var img=this.rawNode.ownerDocument.createElement("img");
img.style.position="relative";
_f7.appendChild(img);
_f6.setRawNode(_f7);
this.rawNode.appendChild(_f7);
_f6.setShape(_f5);
this.add(_f6);
return _f6;
},createObject:function(_f9,_fa){
if(!this.rawNode){
return null;
}
var _fb=new _f9(),_fc=this.rawNode.ownerDocument.createElement("v:"+_f9.nodeType);
_fb.setRawNode(_fc);
this.rawNode.appendChild(_fc);
switch(_f9){
case dojox.gfx.Group:
case dojox.gfx.Line:
case dojox.gfx.Polyline:
case dojox.gfx.Text:
case dojox.gfx.Path:
case dojox.gfx.TextPath:
this._overrideSize(_fc);
}
_fb.setShape(_fa);
this.add(_fb);
return _fb;
},_overrideSize:function(_fd){
var p=this;
while(p&&!(p instanceof dojox.gfx.Surface)){
p=p.parent;
}
_fd.style.width=p.width;
_fd.style.height=p.height;
_fd.coordsize=p.width+" "+p.height;
}});
dojo.extend(dojox.gfx.Group,dojox.gfx.vml.Container);
dojo.extend(dojox.gfx.Group,dojox.gfx.shape.Creator);
dojo.extend(dojox.gfx.Surface,dojox.gfx.vml.Container);
dojo.extend(dojox.gfx.Surface,dojox.gfx.shape.Creator);
}
