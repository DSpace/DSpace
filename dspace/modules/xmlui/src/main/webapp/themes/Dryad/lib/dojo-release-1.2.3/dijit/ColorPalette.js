/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.ColorPalette"]){
dojo._hasResource["dijit.ColorPalette"]=true;
dojo.provide("dijit.ColorPalette");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.require("dojo.colors");
dojo.require("dojo.i18n");
dojo.requireLocalization("dojo","colors",null,"zh,ca,pt,da,tr,ru,de,sv,ja,he,fi,nb,el,ar,pt-pt,cs,fr,es,ko,nl,zh-tw,pl,th,it,hu,ROOT,sk,sl");
dojo.declare("dijit.ColorPalette",[dijit._Widget,dijit._Templated],{defaultTimeout:500,timeoutChangeRate:0.9,palette:"7x10",value:null,_currentFocus:0,_xDim:null,_yDim:null,_palettes:{"7x10":[["white","seashell","cornsilk","lemonchiffon","lightyellow","palegreen","paleturquoise","lightcyan","lavender","plum"],["lightgray","pink","bisque","moccasin","khaki","lightgreen","lightseagreen","lightskyblue","cornflowerblue","violet"],["silver","lightcoral","sandybrown","orange","palegoldenrod","chartreuse","mediumturquoise","skyblue","mediumslateblue","orchid"],["gray","red","orangered","darkorange","yellow","limegreen","darkseagreen","royalblue","slateblue","mediumorchid"],["dimgray","crimson","chocolate","coral","gold","forestgreen","seagreen","blue","blueviolet","darkorchid"],["darkslategray","firebrick","saddlebrown","sienna","olive","green","darkcyan","mediumblue","darkslateblue","darkmagenta"],["black","darkred","maroon","brown","darkolivegreen","darkgreen","midnightblue","navy","indigo","purple"]],"3x4":[["white","lime","green","blue"],["silver","yellow","fuchsia","navy"],["gray","red","purple","black"]]},_imagePaths:{"7x10":dojo.moduleUrl("dijit.themes","a11y/colors7x10.png"),"3x4":dojo.moduleUrl("dijit.themes","a11y/colors3x4.png")},_paletteCoords:{"leftOffset":3,"topOffset":3,"cWidth":20,"cHeight":20},templateString:"<div class=\"dijitInline dijitColorPalette\">\n\t<div class=\"dijitColorPaletteInner\" dojoAttachPoint=\"divNode\" waiRole=\"grid\" tabIndex=\"${tabIndex}\">\n\t\t<img class=\"dijitColorPaletteUnder\" dojoAttachPoint=\"imageNode\" waiRole=\"presentation\">\n\t</div>\t\n</div>\n",_paletteDims:{"7x10":{"width":"206px","height":"145px"},"3x4":{"width":"86px","height":"64px"}},tabIndex:"0",postCreate:function(){
dojo.mixin(this.divNode.style,this._paletteDims[this.palette]);
this.imageNode.setAttribute("src",this._imagePaths[this.palette]);
var _1=this._palettes[this.palette];
this.domNode.style.position="relative";
this._cellNodes=[];
this.colorNames=dojo.i18n.getLocalization("dojo","colors",this.lang);
var _2=this._blankGif,_3=new dojo.Color(),_4=this._paletteCoords;
for(var _5=0;_5<_1.length;_5++){
for(var _6=0;_6<_1[_5].length;_6++){
var _7=dojo.doc.createElement("img");
_7.src=_2;
dojo.addClass(_7,"dijitPaletteImg");
var _8=_1[_5][_6],_9=_3.setColor(dojo.Color.named[_8]);
_7.alt=this.colorNames[_8];
_7.color=_9.toHex();
var _a=_7.style;
_a.color=_a.backgroundColor=_7.color;
var _b=dojo.doc.createElement("span");
_b.appendChild(_7);
dojo.forEach(["Dijitclick","MouseEnter","Focus","Blur"],function(_c){
this.connect(_b,"on"+_c.toLowerCase(),"_onCell"+_c);
},this);
this.divNode.appendChild(_b);
var _d=_b.style;
_d.top=_4.topOffset+(_5*_4.cHeight)+"px";
_d.left=_4.leftOffset+(_6*_4.cWidth)+"px";
dojo.attr(_b,"tabindex","-1");
_b.title=this.colorNames[_8];
dojo.addClass(_b,"dijitPaletteCell");
dijit.setWaiRole(_b,"gridcell");
_b.index=this._cellNodes.length;
this._cellNodes.push(_b);
}
}
this._xDim=_1[0].length;
this._yDim=_1.length;
this.connect(this.divNode,"onfocus","_onDivNodeFocus");
var _e={UP_ARROW:-this._xDim,DOWN_ARROW:this._xDim,RIGHT_ARROW:1,LEFT_ARROW:-1};
for(var _f in _e){
this._connects.push(dijit.typematic.addKeyListener(this.domNode,{charOrCode:dojo.keys[_f],ctrlKey:false,altKey:false,shiftKey:false},this,function(){
var _10=_e[_f];
return function(_11){
this._navigateByKey(_10,_11);
};
}(),this.timeoutChangeRate,this.defaultTimeout));
}
},focus:function(){
this._focusFirst();
},onChange:function(_12){
},_focusFirst:function(){
this._currentFocus=0;
var _13=this._cellNodes[this._currentFocus];
window.setTimeout(function(){
dijit.focus(_13);
},0);
},_onDivNodeFocus:function(evt){
if(evt.target===this.divNode){
this._focusFirst();
}
},_onFocus:function(){
dojo.attr(this.divNode,"tabindex","-1");
},_onBlur:function(){
this._removeCellHighlight(this._currentFocus);
dojo.attr(this.divNode,"tabindex",this.tabIndex);
},_onCellDijitclick:function(evt){
var _16=evt.currentTarget;
if(this._currentFocus!=_16.index){
this._currentFocus=_16.index;
window.setTimeout(function(){
dijit.focus(_16);
},0);
}
this._selectColor(_16);
dojo.stopEvent(evt);
},_onCellMouseEnter:function(evt){
var _18=evt.currentTarget;
this._setCurrent(_18);
window.setTimeout(function(){
dijit.focus(_18);
},0);
},_onCellFocus:function(evt){
this._setCurrent(evt.currentTarget);
},_setCurrent:function(_1a){
this._removeCellHighlight(this._currentFocus);
this._currentFocus=_1a.index;
dojo.addClass(_1a,"dijitPaletteCellHighlight");
},_onCellBlur:function(evt){
this._removeCellHighlight(this._currentFocus);
},_removeCellHighlight:function(_1c){
dojo.removeClass(this._cellNodes[_1c],"dijitPaletteCellHighlight");
},_selectColor:function(_1d){
var img=_1d.getElementsByTagName("img")[0];
this.onChange(this.value=img.color);
},_navigateByKey:function(_1f,_20){
if(_20==-1){
return;
}
var _21=this._currentFocus+_1f;
if(_21<this._cellNodes.length&&_21>-1){
var _22=this._cellNodes[_21];
_22.focus();
}
}});
}
