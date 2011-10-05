/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.html"]){
dojo._hasResource["dojo._base.html"]=true;
dojo.require("dojo._base.lang");
dojo.provide("dojo._base.html");
try{
document.execCommand("BackgroundImageCache",false,true);
}
catch(e){
}
if(dojo.isIE||dojo.isOpera){
dojo.byId=function(id,_2){
if(dojo.isString(id)){
var _d=_2||dojo.doc;
var te=_d.getElementById(id);
if(te&&te.attributes.id.value==id){
return te;
}else{
var _5=_d.all[id];
if(!_5||!_5.length){
return _5;
}
var i=0;
while((te=_5[i++])){
if(te.attributes.id.value==id){
return te;
}
}
}
}else{
return id;
}
};
}else{
dojo.byId=function(id,_8){
return dojo.isString(id)?(_8||dojo.doc).getElementById(id):id;
};
}
(function(){
var d=dojo;
var _a=null;
dojo.addOnWindowUnload(function(){
_a=null;
});
dojo._destroyElement=function(_b){
_b=d.byId(_b);
try{
if(!_a||_a.ownerDocument!=_b.ownerDocument){
_a=_b.ownerDocument.createElement("div");
}
_a.appendChild(_b.parentNode?_b.parentNode.removeChild(_b):_b);
_a.innerHTML="";
}
catch(e){
}
};
dojo.isDescendant=function(_c,_d){
try{
_c=d.byId(_c);
_d=d.byId(_d);
while(_c){
if(_c===_d){
return true;
}
_c=_c.parentNode;
}
}
catch(e){
}
return false;
};
dojo.setSelectable=function(_e,_f){
_e=d.byId(_e);
if(d.isMozilla){
_e.style.MozUserSelect=_f?"":"none";
}else{
if(d.isKhtml){
_e.style.KhtmlUserSelect=_f?"auto":"none";
}else{
if(d.isIE){
var v=(_e.unselectable=_f?"":"on");
d.query("*",_e).forEach("item.unselectable = '"+v+"'");
}
}
}
};
var _11=function(_12,ref){
ref.parentNode.insertBefore(_12,ref);
return true;
};
var _14=function(_15,ref){
var pn=ref.parentNode;
if(ref==pn.lastChild){
pn.appendChild(_15);
}else{
return _11(_15,ref.nextSibling);
}
return true;
};
dojo.place=function(_18,_19,_1a){
if(!_18||!_19){
return false;
}
_18=d.byId(_18);
_19=d.byId(_19);
if(typeof _1a=="number"){
var cn=_19.childNodes;
if(!cn.length||cn.length<=_1a){
_19.appendChild(_18);
return true;
}
return _11(_18,_1a<=0?_19.firstChild:cn[_1a]);
}
switch(_1a){
case "before":
return _11(_18,_19);
case "after":
return _14(_18,_19);
case "first":
if(_19.firstChild){
return _11(_18,_19.firstChild);
}
default:
_19.appendChild(_18);
return true;
}
};
dojo.boxModel="content-box";
if(d.isIE){
var _1c=document.compatMode;
d.boxModel=_1c=="BackCompat"||_1c=="QuirksMode"||d.isIE<6?"border-box":"content-box";
}
var gcs;
if(d.isSafari){
gcs=function(_1e){
var s;
if(_1e instanceof HTMLElement){
var dv=_1e.ownerDocument.defaultView;
s=dv.getComputedStyle(_1e,null);
if(!s&&_1e.style){
_1e.style.display="";
s=dv.getComputedStyle(_1e,null);
}
}
return s||{};
};
}else{
if(d.isIE){
gcs=function(_21){
return _21.nodeType==1?_21.currentStyle:{};
};
}else{
gcs=function(_22){
return _22 instanceof HTMLElement?_22.ownerDocument.defaultView.getComputedStyle(_22,null):{};
};
}
}
dojo.getComputedStyle=gcs;
if(!d.isIE){
dojo._toPixelValue=function(_23,_24){
return parseFloat(_24)||0;
};
}else{
dojo._toPixelValue=function(_25,_26){
if(!_26){
return 0;
}
if(_26=="medium"){
return 4;
}
if(_26.slice&&(_26.slice(-2)=="px")){
return parseFloat(_26);
}
with(_25){
var _27=style.left;
var _28=runtimeStyle.left;
runtimeStyle.left=currentStyle.left;
try{
style.left=_26;
_26=style.pixelLeft;
}
catch(e){
_26=0;
}
style.left=_27;
runtimeStyle.left=_28;
}
return _26;
};
}
var px=d._toPixelValue;
var _2a="DXImageTransform.Microsoft.Alpha";
var af=function(n,f){
try{
return n.filters.item(_2a);
}
catch(e){
return f?{}:null;
}
};
dojo._getOpacity=d.isIE?function(_2e){
try{
return af(_2e).Opacity/100;
}
catch(e){
return 1;
}
}:function(_2f){
return gcs(_2f).opacity;
};
dojo._setOpacity=d.isIE?function(_30,_31){
var ov=_31*100;
_30.style.zoom=1;
af(_30,1).Enabled=(_31==1?false:true);
if(!af(_30)){
_30.style.filter+=" progid:"+_2a+"(Opacity="+ov+")";
}else{
af(_30,1).Opacity=ov;
}
if(_30.nodeName.toLowerCase()=="tr"){
d.query("> td",_30).forEach(function(i){
d._setOpacity(i,_31);
});
}
return _31;
}:function(_34,_35){
return _34.style.opacity=_35;
};
var _36={left:true,top:true};
var _37=/margin|padding|width|height|max|min|offset/;
var _38=function(_39,_3a,_3b){
_3a=_3a.toLowerCase();
if(d.isIE){
if(_3b=="auto"){
if(_3a=="height"){
return _39.offsetHeight;
}
if(_3a=="width"){
return _39.offsetWidth;
}
}
if(_3a=="fontweight"){
switch(_3b){
case 700:
return "bold";
case 400:
default:
return "normal";
}
}
}
if(!(_3a in _36)){
_36[_3a]=_37.test(_3a);
}
return _36[_3a]?px(_39,_3b):_3b;
};
var _3c=d.isIE?"styleFloat":"cssFloat";
var _3d={"cssFloat":_3c,"styleFloat":_3c,"float":_3c};
dojo.style=function(_3e,_3f,_40){
var n=d.byId(_3e),_42=arguments.length,op=(_3f=="opacity");
_3f=_3d[_3f]||_3f;
if(_42==3){
return op?d._setOpacity(n,_40):n.style[_3f]=_40;
}
if(_42==2&&op){
return d._getOpacity(n);
}
var s=gcs(n);
if(_42==2&&!d.isString(_3f)){
for(var x in _3f){
d.style(_3e,x,_3f[x]);
}
return s;
}
return (_42==1)?s:_38(n,_3f,s[_3f]||n.style[_3f]);
};
dojo._getPadExtents=function(n,_47){
var s=_47||gcs(n),l=px(n,s.paddingLeft),t=px(n,s.paddingTop);
return {l:l,t:t,w:l+px(n,s.paddingRight),h:t+px(n,s.paddingBottom)};
};
dojo._getBorderExtents=function(n,_4c){
var ne="none",s=_4c||gcs(n),bl=(s.borderLeftStyle!=ne?px(n,s.borderLeftWidth):0),bt=(s.borderTopStyle!=ne?px(n,s.borderTopWidth):0);
return {l:bl,t:bt,w:bl+(s.borderRightStyle!=ne?px(n,s.borderRightWidth):0),h:bt+(s.borderBottomStyle!=ne?px(n,s.borderBottomWidth):0)};
};
dojo._getPadBorderExtents=function(n,_52){
var s=_52||gcs(n),p=d._getPadExtents(n,s),b=d._getBorderExtents(n,s);
return {l:p.l+b.l,t:p.t+b.t,w:p.w+b.w,h:p.h+b.h};
};
dojo._getMarginExtents=function(n,_57){
var s=_57||gcs(n),l=px(n,s.marginLeft),t=px(n,s.marginTop),r=px(n,s.marginRight),b=px(n,s.marginBottom);
if(d.isSafari&&(s.position!="absolute")){
r=l;
}
return {l:l,t:t,w:l+r,h:t+b};
};
dojo._getMarginBox=function(_5d,_5e){
var s=_5e||gcs(_5d),me=d._getMarginExtents(_5d,s);
var l=_5d.offsetLeft-me.l,t=_5d.offsetTop-me.t,p=_5d.parentNode;
if(d.isMoz){
var sl=parseFloat(s.left),st=parseFloat(s.top);
if(!isNaN(sl)&&!isNaN(st)){
l=sl,t=st;
}else{
if(p&&p.style){
var pcs=gcs(p);
if(pcs.overflow!="visible"){
var be=d._getBorderExtents(p,pcs);
l+=be.l,t+=be.t;
}
}
}
}else{
if(d.isOpera){
if(p){
var be=d._getBorderExtents(p);
l-=be.l;
t-=be.t;
}
}
}
return {l:l,t:t,w:_5d.offsetWidth+me.w,h:_5d.offsetHeight+me.h};
};
dojo._getContentBox=function(_68,_69){
var s=_69||gcs(_68),pe=d._getPadExtents(_68,s),be=d._getBorderExtents(_68,s),w=_68.clientWidth,h;
if(!w){
w=_68.offsetWidth,h=_68.offsetHeight;
}else{
h=_68.clientHeight,be.w=be.h=0;
}
if(d.isOpera){
pe.l+=be.l;
pe.t+=be.t;
}
return {l:pe.l,t:pe.t,w:w-pe.w-be.w,h:h-pe.h-be.h};
};
dojo._getBorderBox=function(_6f,_70){
var s=_70||gcs(_6f),pe=d._getPadExtents(_6f,s),cb=d._getContentBox(_6f,s);
return {l:cb.l-pe.l,t:cb.t-pe.t,w:cb.w+pe.w,h:cb.h+pe.h};
};
dojo._setBox=function(_74,l,t,w,h,u){
u=u||"px";
var s=_74.style;
if(!isNaN(l)){
s.left=l+u;
}
if(!isNaN(t)){
s.top=t+u;
}
if(w>=0){
s.width=w+u;
}
if(h>=0){
s.height=h+u;
}
};
dojo._isButtonTag=function(_7b){
return _7b.tagName=="BUTTON"||_7b.tagName=="INPUT"&&_7b.getAttribute("type").toUpperCase()=="BUTTON";
};
dojo._usesBorderBox=function(_7c){
var n=_7c.tagName;
return d.boxModel=="border-box"||n=="TABLE"||dojo._isButtonTag(_7c);
};
dojo._setContentSize=function(_7e,_7f,_80,_81){
if(d._usesBorderBox(_7e)){
var pb=d._getPadBorderExtents(_7e,_81);
if(_7f>=0){
_7f+=pb.w;
}
if(_80>=0){
_80+=pb.h;
}
}
d._setBox(_7e,NaN,NaN,_7f,_80);
};
dojo._setMarginBox=function(_83,_84,_85,_86,_87,_88){
var s=_88||gcs(_83);
var bb=d._usesBorderBox(_83),pb=bb?_8c:d._getPadBorderExtents(_83,s);
if(dojo.isSafari){
if(dojo._isButtonTag(_83)){
var ns=_83.style;
if(_86>=0&&!ns.width){
ns.width="4px";
}
if(_87>=0&&!ns.height){
ns.height="4px";
}
}
}
var mb=d._getMarginExtents(_83,s);
if(_86>=0){
_86=Math.max(_86-pb.w-mb.w,0);
}
if(_87>=0){
_87=Math.max(_87-pb.h-mb.h,0);
}
d._setBox(_83,_84,_85,_86,_87);
};
var _8c={l:0,t:0,w:0,h:0};
dojo.marginBox=function(_8f,box){
var n=d.byId(_8f),s=gcs(n),b=box;
return !b?d._getMarginBox(n,s):d._setMarginBox(n,b.l,b.t,b.w,b.h,s);
};
dojo.contentBox=function(_94,box){
var n=d.byId(_94),s=gcs(n),b=box;
return !b?d._getContentBox(n,s):d._setContentSize(n,b.w,b.h,s);
};
var _99=function(_9a,_9b){
if(!(_9a=(_9a||0).parentNode)){
return 0;
}
var val,_9d=0,_b=d.body();
while(_9a&&_9a.style){
if(gcs(_9a).position=="fixed"){
return 0;
}
val=_9a[_9b];
if(val){
_9d+=val-0;
if(_9a==_b){
break;
}
}
_9a=_9a.parentNode;
}
return _9d;
};
dojo._docScroll=function(){
var _b=d.body(),_w=d.global,de=d.doc.documentElement;
return {y:(_w.pageYOffset||de.scrollTop||_b.scrollTop||0),x:(_w.pageXOffset||d._fixIeBiDiScrollLeft(de.scrollLeft)||_b.scrollLeft||0)};
};
dojo._isBodyLtr=function(){
return !("_bodyLtr" in d)?d._bodyLtr=gcs(d.body()).direction=="ltr":d._bodyLtr;
};
dojo._getIeDocumentElementOffset=function(){
var de=d.doc.documentElement;
return (d.isIE>=7)?{x:de.getBoundingClientRect().left,y:de.getBoundingClientRect().top}:{x:d._isBodyLtr()||window.parent==window?de.clientLeft:de.offsetWidth-de.clientWidth-de.clientLeft,y:de.clientTop};
};
dojo._fixIeBiDiScrollLeft=function(_a3){
var dd=d.doc;
if(d.isIE&&!dojo._isBodyLtr()){
var de=dd.compatMode=="BackCompat"?dd.body:dd.documentElement;
return _a3+de.clientWidth-de.scrollWidth;
}
return _a3;
};
dojo._abs=function(_a6,_a7){
var _a8=_a6.ownerDocument;
var ret={x:0,y:0};
var db=d.body();
if(d.isIE||(d.isFF>=3)){
var _ab=_a6.getBoundingClientRect();
var cs;
if(d.isFF){
var dv=_a6.ownerDocument.defaultView;
cs=dv.getComputedStyle(db.parentNode,null);
}
var _ae=(d.isIE)?d._getIeDocumentElementOffset():{x:px(db.parentNode,cs.marginLeft),y:px(db.parentNode,cs.marginTop)};
ret.x=_ab.left-_ae.x;
ret.y=_ab.top-_ae.y;
}else{
if(_a6["offsetParent"]){
var _af;
if(d.isSafari&&(gcs(_a6).position=="absolute")&&(_a6.parentNode==db)){
_af=db;
}else{
_af=db.parentNode;
}
var cs=gcs(_a6);
var n=_a6;
if(d.isOpera&&cs.position!="absolute"){
n=n.offsetParent;
}
ret.x-=_99(n,"scrollLeft");
ret.y-=_99(n,"scrollTop");
var _b1=_a6;
do{
var n=_b1.offsetLeft;
if(!d.isOpera||n>0){
ret.x+=isNaN(n)?0:n;
}
var t=_b1.offsetTop;
ret.y+=isNaN(t)?0:t;
var cs=gcs(_b1);
if(_b1!=_a6){
if(d.isSafari){
ret.x+=px(_b1,cs.borderLeftWidth);
ret.y+=px(_b1,cs.borderTopWidth);
}else{
if(d.isFF){
ret.x+=2*px(_b1,cs.borderLeftWidth);
ret.y+=2*px(_b1,cs.borderTopWidth);
}
}
}
if(d.isFF&&cs.position=="static"){
var _b3=_b1.parentNode;
while(_b3!=_b1.offsetParent){
var pcs=gcs(_b3);
if(pcs.position=="static"){
ret.x+=px(_b1,pcs.borderLeftWidth);
ret.y+=px(_b1,pcs.borderTopWidth);
}
_b3=_b3.parentNode;
}
}
_b1=_b1.offsetParent;
}while((_b1!=_af)&&_b1);
}else{
if(_a6.x&&_a6.y){
ret.x+=isNaN(_a6.x)?0:_a6.x;
ret.y+=isNaN(_a6.y)?0:_a6.y;
}
}
}
if(_a7){
var _b5=d._docScroll();
ret.y+=_b5.y;
ret.x+=_b5.x;
}
return ret;
};
dojo.coords=function(_b6,_b7){
var n=d.byId(_b6),s=gcs(n),mb=d._getMarginBox(n,s);
var abs=d._abs(n,_b7);
mb.x=abs.x;
mb.y=abs.y;
return mb;
};
var _bc=d.isIE<8;
var _bd=function(_be){
switch(_be.toLowerCase()){
case "tabindex":
return _bc?"tabIndex":"tabindex";
case "for":
case "htmlfor":
return _bc?"htmlFor":"for";
case "class":
return d.isIE?"className":"class";
default:
return _be;
}
};
var _bf={colspan:"colSpan",enctype:"enctype",frameborder:"frameborder",method:"method",rowspan:"rowSpan",scrolling:"scrolling",shape:"shape",span:"span",type:"type",valuetype:"valueType"};
dojo.hasAttr=function(_c0,_c1){
_c0=d.byId(_c0);
var _c2=_bd(_c1);
_c2=_c2=="htmlFor"?"for":_c2;
var _c3=_c0.getAttributeNode&&_c0.getAttributeNode(_c2);
return _c3?_c3.specified:false;
};
var _c4={};
var _c5=0;
var _c6=dojo._scopeName+"attrid";
dojo.attr=function(_c7,_c8,_c9){
var _ca=arguments.length;
if(_ca==2&&!d.isString(_c8)){
for(var x in _c8){
d.attr(_c7,x,_c8[x]);
}
return;
}
_c7=d.byId(_c7);
_c8=_bd(_c8);
if(_ca==3){
if(d.isFunction(_c9)){
var _cc=d.attr(_c7,_c6);
if(!_cc){
_cc=_c5++;
d.attr(_c7,_c6,_cc);
}
if(!_c4[_cc]){
_c4[_cc]={};
}
var h=_c4[_cc][_c8];
if(h){
d.disconnect(h);
}else{
try{
delete _c7[_c8];
}
catch(e){
}
}
_c4[_cc][_c8]=d.connect(_c7,_c8,_c9);
}else{
if((typeof _c9=="boolean")||(_c8=="innerHTML")){
_c7[_c8]=_c9;
}else{
if((_c8=="style")&&(!d.isString(_c9))){
d.style(_c7,_c9);
}else{
_c7.setAttribute(_c8,_c9);
}
}
}
return;
}else{
var _ce=_bf[_c8.toLowerCase()];
if(_ce){
return _c7[_ce];
}else{
var _cf=_c7[_c8];
return (typeof _cf=="boolean"||typeof _cf=="function")?_cf:(d.hasAttr(_c7,_c8)?_c7.getAttribute(_c8):null);
}
}
};
dojo.removeAttr=function(_d0,_d1){
d.byId(_d0).removeAttribute(_bd(_d1));
};
var _d2="className";
dojo.hasClass=function(_d3,_d4){
return ((" "+d.byId(_d3)[_d2]+" ").indexOf(" "+_d4+" ")>=0);
};
dojo.addClass=function(_d5,_d6){
_d5=d.byId(_d5);
var cls=_d5[_d2];
if((" "+cls+" ").indexOf(" "+_d6+" ")<0){
_d5[_d2]=cls+(cls?" ":"")+_d6;
}
};
dojo.removeClass=function(_d8,_d9){
_d8=d.byId(_d8);
var t=d.trim((" "+_d8[_d2]+" ").replace(" "+_d9+" "," "));
if(_d8[_d2]!=t){
_d8[_d2]=t;
}
};
dojo.toggleClass=function(_db,_dc,_dd){
if(_dd===undefined){
_dd=!d.hasClass(_db,_dc);
}
d[_dd?"addClass":"removeClass"](_db,_dc);
};
})();
}
