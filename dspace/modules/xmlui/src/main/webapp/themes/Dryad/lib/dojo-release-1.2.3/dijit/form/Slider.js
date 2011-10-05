/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.form.Slider"]){
dojo._hasResource["dijit.form.Slider"]=true;
dojo.provide("dijit.form.Slider");
dojo.require("dijit.form._FormWidget");
dojo.require("dijit._Container");
dojo.require("dojo.dnd.move");
dojo.require("dijit.form.Button");
dojo.require("dojo.number");
dojo.require("dojo._base.fx");
dojo.declare("dijit.form.HorizontalSlider",[dijit.form._FormValueWidget,dijit._Container],{templateString:"<table class=\"dijit dijitReset dijitSlider\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" rules=\"none\" dojoAttachEvent=\"onkeypress:_onKeyPress\"\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\" colspan=\"2\"></td\n\t\t><td dojoAttachPoint=\"containerNode,topDecoration\" class=\"dijitReset\" style=\"text-align:center;width:100%;\"></td\n\t\t><td class=\"dijitReset\" colspan=\"2\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerH\"\n\t\t\t><div class=\"dijitSliderDecrementIconH\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"decrementButton\"><span class=\"dijitSliderButtonInner\">-</span></div\n\t\t></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperH dijitSliderLeftBumper dijitSliderLeftBumper\" dojoAttachEvent=\"onclick:_onClkDecBumper\"></div\n\t\t></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><input dojoAttachPoint=\"valueNode\" type=\"hidden\" name=\"${name}\"\n\t\t\t/><div class=\"dijitReset dijitSliderBarContainerH\" waiRole=\"presentation\" dojoAttachPoint=\"sliderBarContainer\"\n\t\t\t\t><div waiRole=\"presentation\" dojoAttachPoint=\"progressBar\" class=\"dijitSliderBar dijitSliderBarH dijitSliderProgressBar dijitSliderProgressBarH\" dojoAttachEvent=\"onclick:_onBarClick\"\n\t\t\t\t\t><div class=\"dijitSliderMoveable dijitSliderMoveableH\" \n\t\t\t\t\t\t><div dojoAttachPoint=\"sliderHandle,focusNode\" class=\"dijitSliderImageHandle dijitSliderImageHandleH\" dojoAttachEvent=\"onmousedown:_onHandleClick\" waiRole=\"slider\" valuemin=\"${minimum}\" valuemax=\"${maximum}\"></div\n\t\t\t\t\t></div\n\t\t\t\t></div\n\t\t\t\t><div waiRole=\"presentation\" dojoAttachPoint=\"remainingBar\" class=\"dijitSliderBar dijitSliderBarH dijitSliderRemainingBar dijitSliderRemainingBarH\" dojoAttachEvent=\"onclick:_onBarClick\"></div\n\t\t\t></div\n\t\t></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperH dijitSliderRightBumper dijitSliderRightBumper\" dojoAttachEvent=\"onclick:_onClkIncBumper\"></div\n\t\t></td\n\t\t><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerH\" style=\"right:0px;\"\n\t\t\t><div class=\"dijitSliderIncrementIconH\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"incrementButton\"><span class=\"dijitSliderButtonInner\">+</span></div\n\t\t></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\" colspan=\"2\"></td\n\t\t><td dojoAttachPoint=\"containerNode,bottomDecoration\" class=\"dijitReset\" style=\"text-align:center;\"></td\n\t\t><td class=\"dijitReset\" colspan=\"2\"></td\n\t></tr\n></table>\n",value:0,showButtons:true,minimum:0,maximum:100,discreteValues:Infinity,pageIncrement:2,clickSelect:true,slideDuration:dijit.defaultDuration,widgetsInTemplate:true,attributeMap:dojo.mixin(dojo.clone(dijit.form._FormWidget.prototype.attributeMap),{id:"",name:"valueNode"}),baseClass:"dijitSlider",_mousePixelCoord:"pageX",_pixelCount:"w",_startingPixelCoord:"x",_startingPixelCount:"l",_handleOffsetCoord:"left",_progressPixelSize:"width",_onKeyPress:function(e){
if(this.disabled||this.readOnly||e.altKey||e.ctrlKey){
return;
}
switch(e.charOrCode){
case dojo.keys.HOME:
this._setValueAttr(this.minimum,true);
break;
case dojo.keys.END:
this._setValueAttr(this.maximum,true);
break;
case ((this._descending||this.isLeftToRight())?dojo.keys.RIGHT_ARROW:dojo.keys.LEFT_ARROW):
case (this._descending===false?dojo.keys.DOWN_ARROW:dojo.keys.UP_ARROW):
case (this._descending===false?dojo.keys.PAGE_DOWN:dojo.keys.PAGE_UP):
this.increment(e);
break;
case ((this._descending||this.isLeftToRight())?dojo.keys.LEFT_ARROW:dojo.keys.RIGHT_ARROW):
case (this._descending===false?dojo.keys.UP_ARROW:dojo.keys.DOWN_ARROW):
case (this._descending===false?dojo.keys.PAGE_UP:dojo.keys.PAGE_DOWN):
this.decrement(e);
break;
default:
this.inherited(arguments);
return;
}
dojo.stopEvent(e);
},_onHandleClick:function(e){
if(this.disabled||this.readOnly){
return;
}
if(!dojo.isIE){
dijit.focus(this.sliderHandle);
}
dojo.stopEvent(e);
},_isReversed:function(){
return !this.isLeftToRight();
},_onBarClick:function(e){
if(this.disabled||this.readOnly||!this.clickSelect){
return;
}
dijit.focus(this.sliderHandle);
dojo.stopEvent(e);
var _4=dojo.coords(this.sliderBarContainer,true);
var _5=e[this._mousePixelCoord]-_4[this._startingPixelCoord];
this._setPixelValue(this._isReversed()?(_4[this._pixelCount]-_5):_5,_4[this._pixelCount],true);
},_setPixelValue:function(_6,_7,_8){
if(this.disabled||this.readOnly){
return;
}
_6=_6<0?0:_7<_6?_7:_6;
var _9=this.discreteValues;
if(_9<=1||_9==Infinity){
_9=_7;
}
_9--;
var _a=_7/_9;
var _b=Math.round(_6/_a);
this._setValueAttr((this.maximum-this.minimum)*_b/_9+this.minimum,_8);
},_setValueAttr:function(_c,_d){
this.valueNode.value=this.value=_c;
dijit.setWaiState(this.focusNode,"valuenow",_c);
this.inherited(arguments);
var _e=(_c-this.minimum)/(this.maximum-this.minimum);
var _f=(this._descending===false)?this.remainingBar:this.progressBar;
var _10=(this._descending===false)?this.progressBar:this.remainingBar;
if(this._inProgressAnim&&this._inProgressAnim.status!="stopped"){
this._inProgressAnim.stop(true);
}
if(_d&&this.slideDuration>0&&_f.style[this._progressPixelSize]){
var _11=this;
var _12={};
var _13=parseFloat(_f.style[this._progressPixelSize]);
var _14=this.slideDuration*(_e-_13/100);
if(_14==0){
return;
}
if(_14<0){
_14=0-_14;
}
_12[this._progressPixelSize]={start:_13,end:_e*100,units:"%"};
this._inProgressAnim=dojo.animateProperty({node:_f,duration:_14,onAnimate:function(v){
_10.style[_11._progressPixelSize]=(100-parseFloat(v[_11._progressPixelSize]))+"%";
},onEnd:function(){
delete _11._inProgressAnim;
},properties:_12});
this._inProgressAnim.play();
}else{
_f.style[this._progressPixelSize]=(_e*100)+"%";
_10.style[this._progressPixelSize]=((1-_e)*100)+"%";
}
},_bumpValue:function(_16){
if(this.disabled||this.readOnly){
return;
}
var s=dojo.getComputedStyle(this.sliderBarContainer);
var c=dojo._getContentBox(this.sliderBarContainer,s);
var _19=this.discreteValues;
if(_19<=1||_19==Infinity){
_19=c[this._pixelCount];
}
_19--;
var _1a=(this.value-this.minimum)*_19/(this.maximum-this.minimum)+_16;
if(_1a<0){
_1a=0;
}
if(_1a>_19){
_1a=_19;
}
_1a=_1a*(this.maximum-this.minimum)/_19+this.minimum;
this._setValueAttr(_1a,true);
},_onClkIncBumper:function(){
this._setValueAttr(this._descending===false?this.minimum:this.maximum,true);
},_onClkDecBumper:function(){
this._setValueAttr(this._descending===false?this.maximum:this.minimum,true);
},decrement:function(e){
this._bumpValue(e.charOrCode==dojo.keys.PAGE_DOWN?-this.pageIncrement:-1);
},increment:function(e){
this._bumpValue(e.charOrCode==dojo.keys.PAGE_UP?this.pageIncrement:1);
},_mouseWheeled:function(evt){
dojo.stopEvent(evt);
var _1e=!dojo.isMozilla;
var _1f=evt[(_1e?"wheelDelta":"detail")]*(_1e?1:-1);
this[(_1f<0?"decrement":"increment")](evt);
},startup:function(){
dojo.forEach(this.getChildren(),function(_20){
if(this[_20.container]!=this.containerNode){
this[_20.container].appendChild(_20.domNode);
}
},this);
},_typematicCallback:function(_21,_22,e){
if(_21==-1){
return;
}
this[(_22==(this._descending?this.incrementButton:this.decrementButton))?"decrement":"increment"](e);
},postCreate:function(){
if(this.showButtons){
this.incrementButton.style.display="";
this.decrementButton.style.display="";
this._connects.push(dijit.typematic.addMouseListener(this.decrementButton,this,"_typematicCallback",25,500));
this._connects.push(dijit.typematic.addMouseListener(this.incrementButton,this,"_typematicCallback",25,500));
}
this.connect(this.domNode,!dojo.isMozilla?"onmousewheel":"DOMMouseScroll","_mouseWheeled");
var _24=this;
var _25=function(){
dijit.form._SliderMover.apply(this,arguments);
this.widget=_24;
};
dojo.extend(_25,dijit.form._SliderMover.prototype);
this._movable=new dojo.dnd.Moveable(this.sliderHandle,{mover:_25});
var _26=dojo.query("label[for=\""+this.id+"\"]");
if(_26.length){
_26[0].id=(this.id+"_label");
dijit.setWaiState(this.focusNode,"labelledby",_26[0].id);
}
dijit.setWaiState(this.focusNode,"valuemin",this.minimum);
dijit.setWaiState(this.focusNode,"valuemax",this.maximum);
this.inherited(arguments);
},destroy:function(){
this._movable.destroy();
if(this._inProgressAnim&&this._inProgressAnim.status!="stopped"){
this._inProgressAnim.stop(true);
}
this.inherited(arguments);
}});
dojo.declare("dijit.form.VerticalSlider",dijit.form.HorizontalSlider,{templateString:"<table class=\"dijitReset dijitSlider\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" rules=\"none\" dojoAttachEvent=\"onkeypress:_onKeyPress\"\n><tbody class=\"dijitReset\"\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\"></td\n\t\t><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerV\"\n\t\t\t><div class=\"dijitSliderIncrementIconV\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"decrementButton\"><span class=\"dijitSliderButtonInner\">+</span></div\n\t\t></td\n\t\t><td class=\"dijitReset\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\"></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><center><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperV dijitSliderTopBumper dijitSliderTopBumper\" dojoAttachEvent=\"onclick:_onClkIncBumper\"></div></center\n\t\t></td\n\t\t><td class=\"dijitReset\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td dojoAttachPoint=\"leftDecoration\" class=\"dijitReset\" style=\"text-align:center;height:100%;\"></td\n\t\t><td class=\"dijitReset\" style=\"height:100%;\"\n\t\t\t><input dojoAttachPoint=\"valueNode\" type=\"hidden\" name=\"${name}\"\n\t\t\t/><center class=\"dijitReset dijitSliderBarContainerV\" waiRole=\"presentation\" dojoAttachPoint=\"sliderBarContainer\"\n\t\t\t\t><div waiRole=\"presentation\" dojoAttachPoint=\"remainingBar\" class=\"dijitSliderBar dijitSliderBarV dijitSliderRemainingBar dijitSliderRemainingBarV\" dojoAttachEvent=\"onclick:_onBarClick\"><!--#5629--></div\n\t\t\t\t><div waiRole=\"presentation\" dojoAttachPoint=\"progressBar\" class=\"dijitSliderBar dijitSliderBarV dijitSliderProgressBar dijitSliderProgressBarV\" dojoAttachEvent=\"onclick:_onBarClick\"\n\t\t\t\t\t><div class=\"dijitSliderMoveable\" style=\"vertical-align:top;\" \n\t\t\t\t\t\t><div dojoAttachPoint=\"sliderHandle,focusNode\" class=\"dijitSliderImageHandle dijitSliderImageHandleV\" dojoAttachEvent=\"onmousedown:_onHandleClick\" waiRole=\"slider\" valuemin=\"${minimum}\" valuemax=\"${maximum}\"></div\n\t\t\t\t\t></div\n\t\t\t\t></div\n\t\t\t></center\n\t\t></td\n\t\t><td dojoAttachPoint=\"containerNode,rightDecoration\" class=\"dijitReset\" style=\"text-align:center;height:100%;\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\"></td\n\t\t><td class=\"dijitReset\"\n\t\t\t><center><div class=\"dijitSliderBar dijitSliderBumper dijitSliderBumperV dijitSliderBottomBumper dijitSliderBottomBumper\" dojoAttachEvent=\"onclick:_onClkDecBumper\"></div></center\n\t\t></td\n\t\t><td class=\"dijitReset\"></td\n\t></tr\n\t><tr class=\"dijitReset\"\n\t\t><td class=\"dijitReset\"></td\n\t\t><td class=\"dijitReset dijitSliderButtonContainer dijitSliderButtonContainerV\"\n\t\t\t><div class=\"dijitSliderDecrementIconV\" tabIndex=\"-1\" style=\"display:none\" dojoAttachPoint=\"incrementButton\"><span class=\"dijitSliderButtonInner\">-</span></div\n\t\t></td\n\t\t><td class=\"dijitReset\"></td\n\t></tr\n></tbody></table>\n",_mousePixelCoord:"pageY",_pixelCount:"h",_startingPixelCoord:"y",_startingPixelCount:"t",_handleOffsetCoord:"top",_progressPixelSize:"height",_descending:true,startup:function(){
if(this._started){
return;
}
if(!this.isLeftToRight()&&dojo.isMoz){
if(this.leftDecoration){
this._rtlRectify(this.leftDecoration);
}
if(this.rightDecoration){
this._rtlRectify(this.rightDecoration);
}
}
this.inherited(arguments);
},_isReversed:function(){
return this._descending;
},_rtlRectify:function(_27){
var _28=[];
while(_27.firstChild){
_28.push(_27.firstChild);
_27.removeChild(_27.firstChild);
}
for(var i=_28.length-1;i>=0;i--){
if(_28[i]){
_27.appendChild(_28[i]);
}
}
}});
dojo.declare("dijit.form._SliderMover",dojo.dnd.Mover,{onMouseMove:function(e){
var _2b=this.widget;
var _2c=_2b._abspos;
if(!_2c){
_2c=_2b._abspos=dojo.coords(_2b.sliderBarContainer,true);
_2b._setPixelValue_=dojo.hitch(_2b,"_setPixelValue");
_2b._isReversed_=_2b._isReversed();
}
var _2d=e[_2b._mousePixelCoord]-_2c[_2b._startingPixelCoord];
_2b._setPixelValue_(_2b._isReversed_?(_2c[_2b._pixelCount]-_2d):_2d,_2c[_2b._pixelCount],false);
},destroy:function(e){
dojo.dnd.Mover.prototype.destroy.apply(this,arguments);
var _2f=this.widget;
_2f._abspos=null;
_2f._setValueAttr(_2f.value,true);
}});
dojo.declare("dijit.form.HorizontalRule",[dijit._Widget,dijit._Templated],{templateString:"<div class=\"dijitRuleContainer dijitRuleContainerH\"></div>",count:3,container:"containerNode",ruleStyle:"",_positionPrefix:"<div class=\"dijitRuleMark dijitRuleMarkH\" style=\"left:",_positionSuffix:"%;",_suffix:"\"></div>",_genHTML:function(pos,ndx){
return this._positionPrefix+pos+this._positionSuffix+this.ruleStyle+this._suffix;
},_isHorizontal:true,postCreate:function(){
var _32;
if(this.count==1){
_32=this._genHTML(50,0);
}else{
var i;
var _34=100/(this.count-1);
if(!this._isHorizontal||this.isLeftToRight()){
_32=this._genHTML(0,0);
for(i=1;i<this.count-1;i++){
_32+=this._genHTML(_34*i,i);
}
_32+=this._genHTML(100,this.count-1);
}else{
_32=this._genHTML(100,0);
for(i=1;i<this.count-1;i++){
_32+=this._genHTML(100-_34*i,i);
}
_32+=this._genHTML(0,this.count-1);
}
}
this.domNode.innerHTML=_32;
}});
dojo.declare("dijit.form.VerticalRule",dijit.form.HorizontalRule,{templateString:"<div class=\"dijitRuleContainer dijitRuleContainerV\"></div>",_positionPrefix:"<div class=\"dijitRuleMark dijitRuleMarkV\" style=\"top:",_isHorizontal:false});
dojo.declare("dijit.form.HorizontalRuleLabels",dijit.form.HorizontalRule,{templateString:"<div class=\"dijitRuleContainer dijitRuleContainerH dijitRuleLabelsContainer dijitRuleLabelsContainerH\"></div>",labelStyle:"",labels:[],numericMargin:0,minimum:0,maximum:1,constraints:{pattern:"#%"},_positionPrefix:"<div class=\"dijitRuleLabelContainer dijitRuleLabelContainerH\" style=\"left:",_labelPrefix:"\"><span class=\"dijitRuleLabel dijitRuleLabelH\">",_suffix:"</span></div>",_calcPosition:function(pos){
return pos;
},_genHTML:function(pos,ndx){
return this._positionPrefix+this._calcPosition(pos)+this._positionSuffix+this.labelStyle+this._labelPrefix+this.labels[ndx]+this._suffix;
},getLabels:function(){
var _38=this.labels;
if(!_38.length){
_38=dojo.query("> li",this.srcNodeRef).map(function(_39){
return String(_39.innerHTML);
});
}
this.srcNodeRef.innerHTML="";
if(!_38.length&&this.count>1){
var _3a=this.minimum;
var inc=(this.maximum-_3a)/(this.count-1);
for(var i=0;i<this.count;i++){
_38.push((i<this.numericMargin||i>=(this.count-this.numericMargin))?"":dojo.number.format(_3a,this.constraints));
_3a+=inc;
}
}
return _38;
},postMixInProperties:function(){
this.inherited(arguments);
this.labels=this.getLabels();
this.count=this.labels.length;
}});
dojo.declare("dijit.form.VerticalRuleLabels",dijit.form.HorizontalRuleLabels,{templateString:"<div class=\"dijitRuleContainer dijitRuleContainerV dijitRuleLabelsContainer dijitRuleLabelsContainerV\"></div>",_positionPrefix:"<div class=\"dijitRuleLabelContainer dijitRuleLabelContainerV\" style=\"top:",_labelPrefix:"\"><span class=\"dijitRuleLabel dijitRuleLabelV\">",_calcPosition:function(pos){
return 100-pos;
},_isHorizontal:false});
}
