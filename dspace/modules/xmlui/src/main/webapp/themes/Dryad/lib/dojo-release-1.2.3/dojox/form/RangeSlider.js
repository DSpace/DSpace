/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.form.RangeSlider"]){
dojo._hasResource["dojox.form.RangeSlider"]=true;
dojo.provide("dojox.form.RangeSlider");
dojo.require("dijit.form.Slider");
dojo.require("dojox.fx");
dojo.declare("dojox.form._RangeSliderMixin",null,{value:[0,100],postCreate:function(){
this.inherited(arguments);
if(this._isReversed()){
this.value.sort(function(a,b){
return b-a;
});
}else{
this.value.sort(function(a,b){
return a-b;
});
}
var _5=this;
var _6=function(){
dijit.form._SliderMoverMax.apply(this,arguments);
this.widget=_5;
};
dojo.extend(_6,dijit.form._SliderMoverMax.prototype);
this._movableMax=new dojo.dnd.Moveable(this.sliderHandleMax,{mover:_6});
dijit.setWaiState(this.focusNodeMax,"valuemin",this.minimum);
dijit.setWaiState(this.focusNodeMax,"valuemax",this.maximum);
var _7=function(){
dijit.form._SliderBarMover.apply(this,arguments);
this.widget=_5;
};
dojo.extend(_7,dijit.form._SliderBarMover.prototype);
this._movableBar=new dojo.dnd.Moveable(this.progressBar,{mover:_7});
},destroy:function(){
this.inherited(arguments);
this._movableMax.destroy();
this._movableBar.destroy();
},_onKeyPress:function(e){
if(this.disabled||this.readOnly||e.altKey||e.ctrlKey){
return;
}
var _9=e.currentTarget;
var _a=false;
var _b=false;
var _c;
if(_9==this.sliderHandle){
_a=true;
}else{
if(_9==this.progressBar){
_b=true;
_a=true;
}else{
if(_9==this.sliderHandleMax){
_b=true;
}
}
}
switch(e.keyCode){
case dojo.keys.HOME:
this._setValueAttr(this.minimum,true,_b);
break;
case dojo.keys.END:
this._setValueAttr(this.maximum,true,_b);
break;
case ((this._descending||this.isLeftToRight())?dojo.keys.RIGHT_ARROW:dojo.keys.LEFT_ARROW):
case (this._descending===false?dojo.keys.DOWN_ARROW:dojo.keys.UP_ARROW):
case (this._descending===false?dojo.keys.PAGE_DOWN:dojo.keys.PAGE_UP):
if(_a&&_b){
_c=Array();
_c[0]={"change":e.keyCode==dojo.keys.PAGE_UP?this.pageIncrement:1,"useMaxValue":true};
_c[1]={"change":e.keyCode==dojo.keys.PAGE_UP?this.pageIncrement:1,"useMaxValue":false};
this._bumpValue(_c);
}else{
if(_a){
this._bumpValue(e.keyCode==dojo.keys.PAGE_UP?this.pageIncrement:1,true);
}else{
if(_b){
this._bumpValue(e.keyCode==dojo.keys.PAGE_UP?this.pageIncrement:1);
}
}
}
break;
case ((this._descending||this.isLeftToRight())?dojo.keys.LEFT_ARROW:dojo.keys.RIGHT_ARROW):
case (this._descending===false?dojo.keys.UP_ARROW:dojo.keys.DOWN_ARROW):
case (this._descending===false?dojo.keys.PAGE_UP:dojo.keys.PAGE_DOWN):
if(_a&&_b){
_c=Array();
_c[0]={"change":e.keyCode==dojo.keys.PAGE_DOWN?-this.pageIncrement:-1,"useMaxValue":false};
_c[1]={"change":e.keyCode==dojo.keys.PAGE_DOWN?-this.pageIncrement:-1,"useMaxValue":true};
this._bumpValue(_c);
}else{
if(_a){
this._bumpValue(e.keyCode==dojo.keys.PAGE_DOWN?-this.pageIncrement:-1);
}else{
if(_b){
this._bumpValue(e.keyCode==dojo.keys.PAGE_DOWN?-this.pageIncrement:-1,true);
}
}
}
break;
default:
dijit.form._FormValueWidget.prototype._onKeyPress.apply(this,arguments);
this.inherited(arguments);
return;
}
dojo.stopEvent(e);
},_onHandleClickMax:function(e){
if(this.disabled||this.readOnly){
return;
}
if(!dojo.isIE){
dijit.focus(this.sliderHandleMax);
}
dojo.stopEvent(e);
},_onClkIncBumper:function(){
this._setValueAttr(this._descending===false?this.minimum:this.maximum,true,true);
},_bumpValue:function(_e,_f){
if(!dojo.isArray(_e)){
value=this._getBumpValue(_e,_f);
}else{
value=Array();
value[0]=this._getBumpValue(_e[0]["change"],_e[0]["useMaxValue"]);
value[1]=this._getBumpValue(_e[1]["change"],_e[1]["useMaxValue"]);
}
this._setValueAttr(value,true,!dojo.isArray(_e)&&((_e>0&&!_f)||(_f&&_e<0)));
},_getBumpValue:function(_10,_11){
var s=dojo.getComputedStyle(this.sliderBarContainer);
var c=dojo._getContentBox(this.sliderBarContainer,s);
var _14=this.discreteValues;
if(_14<=1||_14==Infinity){
_14=c[this._pixelCount];
}
_14--;
var _15=!_11?this.value[0]:this.value[1];
if((this._isReversed()&&_10<0)||(_10>0&&!this._isReversed())){
_15=!_11?this.value[1]:this.value[0];
}
var _16=(_15-this.minimum)*_14/(this.maximum-this.minimum)+_10;
if(_16<0){
_16=0;
}
if(_16>_14){
_16=_14;
}
return _16*(this.maximum-this.minimum)/_14+this.minimum;
},_onBarClick:function(e){
if(this.disabled||this.readOnly){
return;
}
if(!dojo.isIE){
dijit.focus(this.progressBar);
}
dojo.stopEvent(e);
},_onRemainingBarClick:function(e){
if(this.disabled||this.readOnly){
return;
}
if(!dojo.isIE){
dijit.focus(this.progressBar);
}
var _19=dojo.coords(this.sliderBarContainer,true);
var bar=dojo.coords(this.progressBar,true);
var _1b=e[this._mousePixelCoord]-_19[this._startingPixelCoord];
var _1c=bar[this._startingPixelCount];
var _1d=bar[this._startingPixelCount]+bar[this._pixelCount];
var _1e=this._isReversed()?_1b<=_1c:_1b>=_1d;
this._setPixelValue(this._isReversed()?(_19[this._pixelCount]-_1b):_1b,_19[this._pixelCount],true,_1e);
dojo.stopEvent(e);
},_setPixelValue:function(_1f,_20,_21,_22){
if(this.disabled||this.readOnly){
return;
}
var _23=this._getValueByPixelValue(_1f,_20);
this._setValueAttr(_23,_21,_22);
},_getValueByPixelValue:function(_24,_25){
_24=_24<0?0:_25<_24?_25:_24;
var _26=this.discreteValues;
if(_26<=1||_26==Infinity){
_26=_25;
}
_26--;
var _27=_25/_26;
var _28=Math.round(_24/_27);
return (this.maximum-this.minimum)*_28/_26+this.minimum;
},_setValueAttr:function(_29,_2a,_2b){
var _2c=this.value;
if(!dojo.isArray(_29)){
if(_2b){
if(this._isReversed()){
_2c[0]=_29;
}else{
_2c[1]=_29;
}
}else{
if(this._isReversed()){
_2c[1]=_29;
}else{
_2c[0]=_29;
}
}
}else{
_2c=_29;
}
this._lastValueReported="";
this.valueNode.value=this.value=_29=_2c;
dijit.setWaiState(this.focusNode,"valuenow",_2c[0]);
dijit.setWaiState(this.focusNodeMax,"valuenow",_2c[1]);
if(this._isReversed()){
this.value.sort(function(a,b){
return b-a;
});
}else{
this.value.sort(function(a,b){
return a-b;
});
}
dijit.form._FormValueWidget.prototype._setValueAttr.apply(this,arguments);
this._printSliderBar(_2a,_2b);
},_printSliderBar:function(_31,_32){
var _33=(this.value[0]-this.minimum)/(this.maximum-this.minimum);
var _34=(this.value[1]-this.minimum)/(this.maximum-this.minimum);
var _35=_33;
if(_33>_34){
_33=_34;
_34=_35;
}
var _36=this._isReversed()?((1-_33)*100):(_33*100);
var _37=this._isReversed()?((1-_34)*100):(_34*100);
var _38=this._isReversed()?((1-_34)*100):(_33*100);
if(_31&&this.slideDuration>0&&this.progressBar.style[this._progressPixelSize]){
var _39=_32?_34:_33;
var _3a=this;
var _3b={};
var _3c=parseFloat(this.progressBar.style[this._handleOffsetCoord]);
var _3d=this.slideDuration/10;
if(_3d===0){
return;
}
if(_3d<0){
_3d=0-_3d;
}
var _3e={};
var _3f={};
var _40={};
_3e[this._handleOffsetCoord]={start:this.sliderHandle.style[this._handleOffsetCoord],end:_36,units:"%"};
_3f[this._handleOffsetCoord]={start:this.sliderHandleMax.style[this._handleOffsetCoord],end:_37,units:"%"};
_40[this._handleOffsetCoord]={start:this.progressBar.style[this._handleOffsetCoord],end:_38,units:"%"};
_40[this._progressPixelSize]={start:this.progressBar.style[this._progressPixelSize],end:(_34-_33)*100,units:"%"};
var _41=dojo.animateProperty({node:this.sliderHandle,duration:_3d,properties:_3e});
var _42=dojo.animateProperty({node:this.sliderHandleMax,duration:_3d,properties:_3f});
var _43=dojo.animateProperty({node:this.progressBar,duration:_3d,properties:_40});
var _44=dojo.fx.combine([_41,_42,_43]);
_44.play();
}else{
this.sliderHandle.style[this._handleOffsetCoord]=_36+"%";
this.sliderHandleMax.style[this._handleOffsetCoord]=_37+"%";
this.progressBar.style[this._handleOffsetCoord]=_38+"%";
this.progressBar.style[this._progressPixelSize]=((_34-_33)*100)+"%";
}
}});
dojo.declare("dijit.form._SliderMoverMax",dijit.form._SliderMover,{onMouseMove:function(e){
var _46=this.widget;
var _47=_46._abspos;
if(!_47){
_47=_46._abspos=dojo.coords(_46.sliderBarContainer,true);
_46._setPixelValue_=dojo.hitch(_46,"_setPixelValue");
_46._isReversed_=_46._isReversed();
}
var _48=e[_46._mousePixelCoord]-_47[_46._startingPixelCoord];
_46._setPixelValue_(_46._isReversed_?(_47[_46._pixelCount]-_48):_48,_47[_46._pixelCount],false,true);
},destroy:function(e){
dojo.dnd.Mover.prototype.destroy.apply(this,arguments);
var _4a=this.widget;
_4a._abspos=null;
_4a._setValueAttr(_4a.value,true);
}});
dojo.declare("dijit.form._SliderBarMover",dojo.dnd.Mover,{onMouseMove:function(e){
var _4c=this.widget;
if(_4c.disabled||_4c.readOnly){
return;
}
var _4d=_4c._abspos;
var bar=_4c._bar;
var _4f=_4c._mouseOffset;
if(!_4d){
_4d=_4c._abspos=dojo.coords(_4c.sliderBarContainer,true);
_4c._setPixelValue_=dojo.hitch(_4c,"_setPixelValue");
_4c._getValueByPixelValue_=dojo.hitch(_4c,"_getValueByPixelValue");
_4c._isReversed_=_4c._isReversed();
}
if(!bar){
bar=_4c._bar=dojo.coords(_4c.progressBar,true);
}
if(!_4f){
_4f=_4c._mouseOffset=e[_4c._mousePixelCoord]-_4d[_4c._startingPixelCoord]-bar[_4c._startingPixelCount];
}
var _50=e[_4c._mousePixelCoord]-_4d[_4c._startingPixelCoord]-_4f;
var _51=e[_4c._mousePixelCoord]-_4d[_4c._startingPixelCoord]-_4f+bar[_4c._pixelCount];
var _52=[_50,_51];
_52.sort(function(a,b){
return a-b;
});
if(_52[0]<=0){
_52[0]=0;
_52[1]=bar[_4c._pixelCount];
}
if(_52[1]>=_4d[_4c._pixelCount]){
_52[1]=_4d[_4c._pixelCount];
_52[0]=_4d[_4c._pixelCount]-bar[_4c._pixelCount];
}
var _55=[_4c._getValueByPixelValue(_4c._isReversed_?(_4d[_4c._pixelCount]-_52[0]):_52[0],_4d[_4c._pixelCount]),_4c._getValueByPixelValue(_4c._isReversed_?(_4d[_4c._pixelCount]-_52[1]):_52[1],_4d[_4c._pixelCount])];
_4c._setValueAttr(_55,false,false);
},destroy:function(e){
dojo.dnd.Mover.prototype.destroy.apply(this,arguments);
var _57=this.widget;
_57._abspos=null;
_57._bar=null;
_57._mouseOffset=null;
_57._setValueAttr(_57.value,true);
}});
dojo.declare("dojox.form.HorizontalRangeSlider",[dijit.form.HorizontalSlider,dojox.form._RangeSliderMixin],{templateString:"<table class=\"dijit dijitReset dijitSlider dojoxRangeSlider\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" rules=\"none\"\n    ><tr class=\"dijitReset\"\n        ><td class=\"dijitReset\" colspan=\"2\"></td\n        ><td dojoAttachPoint=\"containerNode,topDecoration\" class=\"dijitReset\" style=\"text-align:center;width:100%;\"></td\n        ><td class=\"dijitReset\" colspan=\"2\"></td\n    ></tr\n    ><tr class=\"dijitReset\"\n        ><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerH\"\n            ><div class=\"dijitSliderDecrementIconH\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"decrementButton\" dojoAttachEvent=\"onclick: decrement\"><span class=\"dijitSliderButtonInner\">-</span></div\n        ></td\n        ><td class=\"dijitReset\"\n            ><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperH dijitSliderLeftBumper dijitSliderLeftBumperH\" dojoAttachEvent=\"onclick:_onClkDecBumper\"></div\n        ></td\n        ><td class=\"dijitReset\"\n            ><input dojoAttachPoint=\"valueNode\" type=\"hidden\" name=\"${name}\"\n            /><div waiRole=\"presentation\" class=\"dojoxRangeSliderBarContainer\" dojoAttachPoint=\"sliderBarContainer\"\n                ><div dojoAttachPoint=\"sliderHandle\" tabIndex=\"${tabIndex}\" class=\"dijitSliderMoveable\" dojoAttachEvent=\"onkeypress:_onKeyPress,onmousedown:_onHandleClick\" waiRole=\"slider\" valuemin=\"${minimum}\" valuemax=\"${maximum}\"\n                    ><div class=\"dijitSliderImageHandle dijitSliderImageHandleH\"></div\n                ></div\n                ><div waiRole=\"presentation\" dojoAttachPoint=\"progressBar,focusNode\" class=\"dijitSliderBar dijitSliderBarH dijitSliderProgressBar dijitSliderProgressBarH\" dojoAttachEvent=\"onkeypress:_onKeyPress,onmousedown:_onBarClick\"></div\n                ><div dojoAttachPoint=\"sliderHandleMax,focusNodeMax\" tabIndex=\"${tabIndex}\" class=\"dijitSliderMoveable\" dojoAttachEvent=\"onkeypress:_onKeyPress,onmousedown:_onHandleClickMax\" waiRole=\"sliderMax\" valuemin=\"${minimum}\" valuemax=\"${maximum}\"\n                    ><div class=\"dijitSliderImageHandle dijitSliderImageHandleH\"></div\n                ></div\n                ><div waiRole=\"presentation\" dojoAttachPoint=\"remainingBar\" class=\"dijitSliderBar dijitSliderBarH dijitSliderRemainingBar dijitSliderRemainingBarH\" dojoAttachEvent=\"onmousedown:_onRemainingBarClick\"></div\n            ></div\n        ></td\n        ><td class=\"dijitReset\"\n            ><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperH dijitSliderRightBumper dijitSliderRightBumperH\" dojoAttachEvent=\"onclick:_onClkIncBumper\"></div\n        ></td\n        ><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerH\"\n            ><div class=\"dijitSliderIncrementIconH\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"incrementButton\" dojoAttachEvent=\"onclick: increment\"><span class=\"dijitSliderButtonInner\">+</span></div\n        ></td\n    ></tr\n    ><tr class=\"dijitReset\"\n        ><td class=\"dijitReset\" colspan=\"2\"></td\n        ><td dojoAttachPoint=\"containerNode,bottomDecoration\" class=\"dijitReset\" style=\"text-align:center;\"></td\n        ><td class=\"dijitReset\" colspan=\"2\"></td\n    ></tr\n></table>\n"});
dojo.declare("dojox.form.VerticalRangeSlider",[dijit.form.VerticalSlider,dojox.form._RangeSliderMixin],{templateString:"<table class=\"dijitReset dijitSlider dojoxRangeSlider\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" rules=\"none\"\n><tbody class=\"dijitReset\"\n    ><tr class=\"dijitReset\"\n        ><td class=\"dijitReset\"></td\n        ><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerV\"\n            ><div class=\"dijitSliderIncrementIconV\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"incrementButton\" dojoAttachEvent=\"onclick: increment\"><span class=\"dijitSliderButtonInner\">+</span></div\n        ></td\n        ><td class=\"dijitReset\"></td\n    ></tr\n    ><tr class=\"dijitReset\"\n        ><td class=\"dijitReset\"></td\n        ><td class=\"dijitReset\"\n            ><center><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperV dijitSliderTopBumper dijitSliderTopBumperV\" dojoAttachEvent=\"onclick:_onClkIncBumper\"></div></center\n        ></td\n        ><td class=\"dijitReset\"></td\n    ></tr\n    ><tr class=\"dijitReset\"\n        ><td dojoAttachPoint=\"leftDecoration\" class=\"dijitReset\" style=\"text-align:center;height:100%;\"></td\n        ><td class=\"dijitReset\" style=\"height:100%;\"\n            ><input dojoAttachPoint=\"valueNode\" type=\"hidden\" name=\"${name}\"\n            /><center waiRole=\"presentation\" style=\"position:relative;height:100%;\" dojoAttachPoint=\"sliderBarContainer\"\n                ><div waiRole=\"presentation\" dojoAttachPoint=\"remainingBar\" class=\"dijitSliderBar dijitSliderBarV dijitSliderRemainingBar dijitSliderRemainingBarV\" dojoAttachEvent=\"onmousedown:_onRemainingBarClick\"\n                    ><div dojoAttachPoint=\"sliderHandle\" tabIndex=\"${tabIndex}\" class=\"dijitSliderMoveable\" dojoAttachEvent=\"onkeypress:_onKeyPress,onmousedown:_onHandleClick\" style=\"vertical-align:top;\" waiRole=\"slider\" valuemin=\"${minimum}\" valuemax=\"${maximum}\"\n                        ><div class=\"dijitSliderImageHandle dijitSliderImageHandleV\"></div\n                    ></div\n                    ><div waiRole=\"presentation\" dojoAttachPoint=\"progressBar,focusNode\" tabIndex=\"${tabIndex}\" class=\"dijitSliderBar dijitSliderBarV dijitSliderProgressBar dijitSliderProgressBarV\" dojoAttachEvent=\"onkeypress:_onKeyPress,onmousedown:_onBarClick\"\n                    ></div\n                    ><div dojoAttachPoint=\"sliderHandleMax,focusNodeMax\" tabIndex=\"${tabIndex}\" class=\"dijitSliderMoveable\" dojoAttachEvent=\"onkeypress:_onKeyPress,onmousedown:_onHandleClickMax\" style=\"vertical-align:top;\" waiRole=\"slider\" valuemin=\"${minimum}\" valuemax=\"${maximum}\"\n                        ><div class=\"dijitSliderImageHandle dijitSliderImageHandleV\"></div\n                    ></div\n                ></div\n            ></center\n        ></td\n        ><td dojoAttachPoint=\"containerNode,rightDecoration\" class=\"dijitReset\" style=\"text-align:center;height:100%;\"></td\n    ></tr\n    ><tr class=\"dijitReset\"\n        ><td class=\"dijitReset\"></td\n        ><td class=\"dijitReset\"\n            ><center><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperV dijitSliderBottomBumper dijitSliderBottomBumperV\" dojoAttachEvent=\"onclick:_onClkDecBumper\"></div></center\n        ></td\n        ><td class=\"dijitReset\"></td\n    ></tr\n    ><tr class=\"dijitReset\"\n        ><td class=\"dijitReset\"></td\n        ><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerV\"\n            ><div class=\"dijitSliderDecrementIconV\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"decrementButton\" dojoAttachEvent=\"onclick: decrement\"><span class=\"dijitSliderButtonInner\">-</span></div\n        ></td\n        ><td class=\"dijitReset\"></td\n    ></tr\n></tbody></table>\n"});
}
