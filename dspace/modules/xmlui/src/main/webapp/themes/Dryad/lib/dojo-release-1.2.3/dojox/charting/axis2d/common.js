/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.charting.axis2d.common"]){
dojo._hasResource["dojox.charting.axis2d.common"]=true;
dojo.provide("dojox.charting.axis2d.common");
dojo.require("dojox.gfx");
(function(){
var g=dojox.gfx;
function clearNode(s){
s.marginLeft="0px";
s.marginTop="0px";
s.marginRight="0px";
s.marginBottom="0px";
s.paddingLeft="0px";
s.paddingTop="0px";
s.paddingRight="0px";
s.paddingBottom="0px";
s.borderLeftWidth="0px";
s.borderTopWidth="0px";
s.borderRightWidth="0px";
s.borderBottomWidth="0px";
};
dojo.mixin(dojox.charting.axis2d.common,{createText:{gfx:function(_3,_4,x,y,_7,_8,_9,_a){
return _4.createText({x:x,y:y,text:_8,align:_7}).setFont(_9).setFill(_a);
},html:function(_b,_c,x,y,_f,_10,_11,_12){
var p=dojo.doc.createElement("div"),s=p.style;
clearNode(s);
s.font=_11;
p.innerHTML=String(_10).replace(/\s/g,"&nbsp;");
s.color=_12;
s.position="absolute";
s.left="-10000px";
dojo.body().appendChild(p);
var _15=g.normalizedLength(g.splitFontString(_11).size),box=dojo.marginBox(p);
dojo.body().removeChild(p);
s.position="relative";
switch(_f){
case "middle":
s.left=Math.floor(x-box.w/2)+"px";
break;
case "end":
s.left=Math.floor(x-box.w)+"px";
break;
default:
s.left=Math.floor(x)+"px";
break;
}
s.top=Math.floor(y-_15)+"px";
var _17=dojo.doc.createElement("div"),w=_17.style;
clearNode(w);
w.width="0px";
w.height="0px";
_17.appendChild(p);
_b.node.insertBefore(_17,_b.node.firstChild);
return _17;
}}});
})();
}
