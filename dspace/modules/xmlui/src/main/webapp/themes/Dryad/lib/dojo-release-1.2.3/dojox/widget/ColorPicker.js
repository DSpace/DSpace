/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.widget.ColorPicker"]){
dojo._hasResource["dojox.widget.ColorPicker"]=true;
dojo.provide("dojox.widget.ColorPicker");
dojo.experimental("dojox.widget.ColorPicker");
dojo.require("dijit.form._FormWidget");
dojo.require("dojo.dnd.move");
dojo.require("dojo.fx");
(function(){
var _1=function(h,s,v,_5){
if(dojo.isArray(h)){
if(s){
_5=s;
}
v=h[2]||0;
s=h[1]||0;
h=h[0]||0;
}
var _6={inputRange:(_5&&_5.inputRange)?_5.inputRange:[255,255,255],outputRange:(_5&&_5.outputRange)?_5.outputRange:255};
switch(_6.inputRange[0]){
case 1:
h=h*360;
break;
case 100:
h=(h/100)*360;
break;
case 360:
h=h;
break;
default:
h=(h/255)*360;
}
if(h==360){
h=0;
}
switch(_6.inputRange[1]){
case 100:
s/=100;
break;
case 255:
s/=255;
}
switch(_6.inputRange[2]){
case 100:
v/=100;
break;
case 255:
v/=255;
}
var r=null;
var g=null;
var b=null;
if(s==0){
r=v;
g=v;
b=v;
}else{
var _a=h/60;
var i=Math.floor(_a);
var f=_a-i;
var p=v*(1-s);
var q=v*(1-(s*f));
var t=v*(1-(s*(1-f)));
switch(i){
case 0:
r=v;
g=t;
b=p;
break;
case 1:
r=q;
g=v;
b=p;
break;
case 2:
r=p;
g=v;
b=t;
break;
case 3:
r=p;
g=q;
b=v;
break;
case 4:
r=t;
g=p;
b=v;
break;
case 5:
r=v;
g=p;
b=q;
break;
}
}
switch(_6.outputRange){
case 1:
r=dojo.math.round(r,2);
g=dojo.math.round(g,2);
b=dojo.math.round(b,2);
break;
case 100:
r=Math.round(r*100);
g=Math.round(g*100);
b=Math.round(b*100);
break;
default:
r=Math.round(r*255);
g=Math.round(g*255);
b=Math.round(b*255);
}
return [r,g,b];
};
var _10=function(hex){
return hex;
};
dojo.declare("dojox.widget.ColorPicker",dijit.form._FormWidget,{showRgb:true,showHsv:true,showHex:true,webSafe:true,animatePoint:true,slideDuration:250,liveUpdate:false,_underlay:dojo.moduleUrl("dojox.widget","ColorPicker/images/underlay.png"),templateString:"<div class=\"dojoxColorPicker\" dojoAttachEvent=\"onkeypress: _handleKey\">\n\t<div class=\"dojoxColorPickerBox\">\n\t\t<div dojoAttachPoint=\"cursorNode\" tabIndex=\"0\" class=\"dojoxColorPickerPoint\"></div>\n\t\t<img dojoAttachPoint=\"colorUnderlay\" dojoAttachEvent=\"onclick: _setPoint\" class=\"dojoxColorPickerUnderlay\" src=\"${_underlay}\">\n\t</div>\n\t<div class=\"dojoxHuePicker\">\n\t\t<div dojoAttachPoint=\"hueCursorNode\" tabIndex=\"0\" class=\"dojoxHuePickerPoint\"></div>\n\t\t<div dojoAttachPoint=\"hueNode\" class=\"dojoxHuePickerUnderlay\" dojoAttachEvent=\"onclick: _setHuePoint\"></div>\n\t</div>\n\t<div dojoAttachPoint=\"previewNode\" class=\"dojoxColorPickerPreview\"></div>\n\t<div dojoAttachPoint=\"safePreviewNode\" class=\"dojoxColorPickerWebSafePreview\"></div>\n\t<div class=\"dojoxColorPickerOptional\" dojoAttachEvent=\"onchange: _updatePoints\">\n\t\t<div class=\"dijitInline dojoxColorPickerRgb\" dojoAttachPoint=\"rgbNode\">\n\t\t\t<table>\n\t\t\t<tr><td>r</td><td><input disabled=\"disabled\" readonly=\"true\" dojoAttachPoint=\"Rval\" size=\"1\"></td></tr>\n\t\t\t<tr><td>g</td><td><input disabled=\"disabled\" readonly=\"true\" dojoAttachPoint=\"Gval\" size=\"1\"></td></tr>\n\t\t\t<tr><td>b</td><td><input disabled=\"disabled\" readonly=\"true\" dojoAttachPoint=\"Bval\" size=\"1\"></td></tr>\n\t\t\t</table>\n\t\t</div>\n\t\t<div class=\"dijitInline dojoxColorPickerHsv\" dojoAttachPoint=\"hsvNode\">\n\t\t\t<table>\n\t\t\t<tr><td>h</td><td><input dojoAttachPoint=\"Hval\"size=\"1\" disabled=\"disabled\" readonly=\"true\"> &deg;</td></tr>\n\t\t\t<tr><td>s</td><td><input dojoAttachPoint=\"Sval\" size=\"1\" disabled=\"disabled\" readonly=\"true\"> %</td></tr>\n\t\t\t<tr><td>v</td><td><input dojoAttachPoint=\"Vval\" size=\"1\"disabled=\"disabled\" readonly=\"true\"> %</td></tr>\n\t\t\t</table>\n\t\t</div>\n\t\t<div class=\"dojoxColorPickerHex\" dojoAttachPoint=\"hexNode\">\t\n\t\t\thex: <input dojoAttachPoint=\"hexCode, focusNode\" size=\"6\" class=\"dojoxColorPickerHexCode\">\n\t\t</div>\n\t</div>\n</div>\n",postCreate:function(){
if(dojo.isIE<7){
this.colorUnderlay.style.filter="progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"+this._underlay+"', sizingMethod='scale')";
this.colorUnderlay.src=this._blankGif.toString();
}
if(!this.showRgb){
this.rgbNode.style.display="none";
}
if(!this.showHsv){
this.hsvNode.style.display="none";
}
if(!this.showHex){
this.hexNode.style.display="none";
}
if(!this.webSafe){
this.safePreviewNode.style.visibility="hidden";
}
},startup:function(){
this._offset=0;
var cmb=dojo.marginBox(this.cursorNode);
var hmb=dojo.marginBox(this.hueCursorNode);
this._shift={hue:{x:Math.round(hmb.w/2)-1,y:Math.round(hmb.h/2)-1},picker:{x:Math.floor(cmb.w/2),y:Math.floor(cmb.h/2)}};
var ox=this._shift.picker.x;
var oy=this._shift.picker.y;
this._mover=new dojo.dnd.Moveable(this.cursorNode,{mover:dojo.dnd.boxConstrainedMover({t:0-oy,l:0-ox,w:151,h:151})});
this._hueMover=new dojo.dnd.Moveable(this.hueCursorNode,{mover:dojo.dnd.boxConstrainedMover({t:0-this._shift.hue.y,l:0,w:0,h:150})});
dojo.subscribe("/dnd/move/stop",dojo.hitch(this,"_clearTimer"));
dojo.subscribe("/dnd/move/start",dojo.hitch(this,"_setTimer"));
this._sc=(1/dojo.coords(this.colorUnderlay).w);
this._hueSc=255/150;
this._updateColor();
},_setTimer:function(_16){
dijit.focus(_16.node);
dojo.setSelectable(this.domNode,false);
this._timer=setInterval(dojo.hitch(this,"_updateColor"),45);
},_clearTimer:function(_17){
clearInterval(this._timer);
this._timer=null;
this.onChange(this.value);
dojo.setSelectable(this.domNode,true);
},_setHue:function(h){
var hue=dojo.colorFromArray(_1(h,1,1,{inputRange:1})).toHex();
dojo.style(this.colorUnderlay,"backgroundColor",hue);
},_updateColor:function(){
var _1a=dojo.style(this.hueCursorNode,"top")+this._shift.hue.y;
var _1b=dojo.style(this.cursorNode,"top")+this._shift.picker.y;
var _1c=dojo.style(this.cursorNode,"left")+this._shift.picker.x;
var h=Math.round(255-(_1a*this._hueSc));
var s=Math.round(_1c*this._sc*100);
var v=Math.round(100-(_1b*this._sc)*100);
if(h!=this._hue){
this._setHue(h);
}
var rgb=_1(h,s/100,v/100,{inputRange:1});
var hex=dojo.colorFromArray(rgb).toHex();
this.previewNode.style.backgroundColor=hex;
if(this.webSafe){
this.safePreviewNode.style.backgroundColor=_10(hex);
}
if(this.showHex){
this.hexCode.value=hex;
}
if(this.showRgb){
this.Rval.value=rgb[0];
this.Gval.value=rgb[1];
this.Bval.value=rgb[2];
}
if(this.showHsv){
this.Hval.value=Math.round((h*360)/255);
this.Sval.value=s;
this.Vval.value=v;
}
this.value=hex;
if((!this._timer&&!arguments[1])||this.liveUpdate){
this.setAttribute("value",hex);
this.onChange(hex);
}
},_updatePoints:function(e){
},_setHuePoint:function(evt){
var _24=evt.layerY-this._shift.hue.y;
if(this.animatePoint){
dojo.fx.slideTo({node:this.hueCursorNode,duration:this.slideDuration,top:_24,left:0,onEnd:dojo.hitch(this,"_updateColor",true)}).play();
}else{
dojo.style(this.hueCursorNode,"top",_24+"px");
this._updateColor(false);
}
},_setPoint:function(evt){
var _26=evt.layerY-this._shift.picker.y;
var _27=evt.layerX-this._shift.picker.x;
if(evt){
dijit.focus(evt.target);
}
if(this.animatePoint){
dojo.fx.slideTo({node:this.cursorNode,duration:this.slideDuration,top:_26,left:_27,onEnd:dojo.hitch(this,"_updateColor",true)}).play();
}else{
dojo.style(this.cursorNode,{left:_27+"px",top:_26+"px"});
this._updateColor(false);
}
},_handleKey:function(e){
var _29=dojo.keys;
}});
})();
}
