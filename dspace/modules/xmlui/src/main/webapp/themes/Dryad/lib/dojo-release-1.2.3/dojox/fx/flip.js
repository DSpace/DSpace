/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.fx.flip"]){
dojo._hasResource["dojox.fx.flip"]=true;
dojo.provide("dojox.fx.flip");
dojo.experimental("dojox.fx.flip");
dojo.require("dojo.fx");
(function(){
var _1="border",_2="Width",_3="Height",_4="Top",_5="Right",_6="Left",_7="Bottom";
dojox.fx.flip=function(_8){
var _9=dojo.doc.createElement("div");
var _a=_8.node=dojo.byId(_8.node),s=_a.style,_c=null,hs=null,pn=null,_f=_8.lightColor||"#dddddd",_10=_8.darkColor||"#555555",_11=dojo.style(_a,"backgroundColor"),_12=_8.endColor||_11,_13={},_14=[],_15=_8.duration?_8.duration/2:250,dir=_8.dir||"left",_17=0.6,_18="transparent",_19=_8.whichAnim,_1a=1;
var _1b=function(_1c){
return ((new dojo.Color(_1c)).toHex()==="#000000")?"#000001":_1c;
};
if(dojo.isIE<7){
_12=_1b(_12);
_f=_1b(_f);
_10=_1b(_10);
_11=_1b(_11);
_18="black";
_9.style.filter="chroma(color='#000000')";
}
var _1d=(function(n){
return function(){
var ret=dojo.coords(n,true);
_c={top:ret.y,left:ret.x,width:ret.w,height:ret.h};
};
})(_a);
_1d();
if(_19){
_17=0.5;
_1a=0;
}
hs={position:"absolute",top:_c["top"]+"px",left:_c["left"]+"px",height:"0",width:"0",zIndex:_8.zIndex||(s.zIndex||0),border:"0 solid "+_18,fontSize:"0",visibility:"hidden"};
_c["endHeight"]=_c["height"]*_17;
_c["endWidth"]=_c["width"]*_17;
var _20=[{},{top:_c["top"],left:_c["left"]}];
var _21={left:[_6,_5,_4,_7,_2,_3,"end"+_3,_6],right:[_5,_6,_4,_7,_2,_3,"end"+_3,_6],top:[_4,_7,_6,_5,_3,_2,"end"+_2,_4],bottom:[_7,_4,_6,_5,_3,_2,"end"+_2,_4]};
pn=_21[dir];
_13[pn[5].toLowerCase()]=_c[pn[5].toLowerCase()]+"px";
_13[pn[4].toLowerCase()]="0";
_13[_1+pn[1]+_2]=_c[pn[4].toLowerCase()]+"px";
_13[_1+pn[1]+"Color"]=_11;
var p0=_20[0];
p0[_1+pn[1]+_2]=0;
p0[_1+pn[1]+"Color"]=_10;
p0[_1+pn[2]+_2]=_c[pn[6]]/2;
p0[_1+pn[3]+_2]=_c[pn[6]]/2;
p0[pn[2].toLowerCase()]=_c[pn[2].toLowerCase()]-_1a*(_c[pn[6]]/8);
p0[pn[7].toLowerCase()]=_c[pn[7].toLowerCase()]+_c[pn[4].toLowerCase()]/2+(_8.shift||0);
p0[pn[5].toLowerCase()]=_c[pn[6]];
var p1=_20[1];
p1[_1+pn[0]+"Color"]={start:_f,end:_12};
p1[_1+pn[0]+_2]=_c[pn[4].toLowerCase()];
p1[_1+pn[2]+_2]=0;
p1[_1+pn[3]+_2]=0;
p1[pn[5].toLowerCase()]={start:_c[pn[6]],end:_c[pn[5].toLowerCase()]};
dojo.mixin(hs,_13);
dojo.style(_9,hs);
dojo.body().appendChild(_9);
var _24=function(){
_9.parentNode.removeChild(_9);
s.backgroundColor=_12;
s.visibility="visible";
};
if(_19=="last"){
for(var i in p0){
p0[i]={start:p0[i]};
}
p0[_1+pn[1]+"Color"]={start:_10,end:_12};
p1=p0;
}
if(!_19||_19=="first"){
_14.push(dojo.animateProperty({node:_9,duration:_15,properties:p0}));
}
if(!_19||_19=="last"){
_14.push(dojo.animateProperty({node:_9,duration:_15,properties:p1,onEnd:_24}));
}
dojo.connect(_14[0],"play",function(){
_9.style.visibility="visible";
s.visibility="hidden";
});
return dojo.fx.chain(_14);
};
dojox.fx.flipCube=function(_26){
var _27=[],mb=dojo.marginBox(_26.node),_29=mb.w/2,_2a=mb.h/2,_2b={top:{pName:"height",args:[{whichAnim:"first",dir:"top",shift:-_2a},{whichAnim:"last",dir:"bottom",shift:_2a}]},right:{pName:"width",args:[{whichAnim:"first",dir:"right",shift:_29},{whichAnim:"last",dir:"left",shift:-_29}]},bottom:{pName:"height",args:[{whichAnim:"first",dir:"bottom",shift:_2a},{whichAnim:"last",dir:"top",shift:-_2a}]},left:{pName:"width",args:[{whichAnim:"first",dir:"left",shift:-_29},{whichAnim:"last",dir:"right",shift:_29}]}};
var d=_2b[_26.dir||"left"],p=d.args;
for(var i=p.length-1;i>=0;i--){
dojo.mixin(_26,p[i]);
_27.push(dojox.fx.flip(_26));
}
return dojo.fx.combine(_27);
};
})();
}
