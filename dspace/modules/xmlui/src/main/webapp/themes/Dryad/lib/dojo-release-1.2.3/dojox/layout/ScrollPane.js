/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.layout.ScrollPane"]){
dojo._hasResource["dojox.layout.ScrollPane"]=true;
dojo.provide("dojox.layout.ScrollPane");
dojo.experimental("dojox.layout.ScrollPane");
dojo.require("dijit.layout._LayoutWidget");
dojo.require("dijit._Templated");
dojo.declare("dojox.layout.ScrollPane",[dijit.layout._LayoutWidget,dijit._Templated],{_line:null,_lo:null,_offset:15,orientation:"vertical",templateString:"<div class=\"dojoxScrollWindow\" dojoAttachEvent=\"onmouseenter: _enter, onmouseleave: _leave\">\n    <div class=\"dojoxScrollWrapper\" style=\"${style}\" dojoAttachPoint=\"wrapper\" dojoAttachEvent=\"onmousemove: _calc\">\n\t<div class=\"dojoxScrollPane\" dojoAttachPoint=\"containerNode\"></div>\n    </div>\n    <div dojoAttachPoint=\"helper\" class=\"dojoxScrollHelper\"><span class=\"helperInner\">|</span></div>\n</div>\n",layout:function(){
var _1=this._dir,_2=this._vertical,_3=this.containerNode[(_2?"scrollHeight":"scrollWidth")];
dojo.style(this.wrapper,_1,this.domNode.style[_1]);
this._lo=dojo.coords(this.wrapper,true);
this._size=Math.max(0,_3-this._lo[(_2?"h":"w")]);
this._line=new dojo._Line(0-this._offset,this._size+(this._offset*2));
var u=this._lo[(_2?"h":"w")],r=Math.min(1,u/_3),s=u*r,c=Math.floor(u-(u*r));
this._helpLine=new dojo._Line(0,c);
dojo.style(this.helper,_1,Math.floor(s)+"px");
},postCreate:function(){
this.inherited(arguments);
this._showAnim=dojo._fade({node:this.helper,end:0.5,duration:350});
this._hideAnim=dojo.fadeOut({node:this.helper,duration:750});
this._vertical=(this.orientation=="vertical");
if(!this._vertical){
dojo.addClass(this.containerNode,"dijitInline");
this._dir="width";
this._edge="left";
}else{
this._dir="height";
this._edge="top";
}
this._hideAnim.play();
dojo.style(this.wrapper,"overflow","hidden");
},_set:function(n){
this.wrapper[(this._vertical?"scrollTop":"scrollLeft")]=Math.floor(this._line.getValue(n));
dojo.style(this.helper,this._edge,Math.floor(this._helpLine.getValue(n))+"px");
},_calc:function(e){
this._set(this._vertical?((e.pageY-this._lo.y)/this._lo.h):((e.pageX-this._lo.x)/this._lo.w));
},_enter:function(e){
if(this._hideAnim&&this._hideAnim.status()=="playing"){
this._hideAnim.stop();
}
this._showAnim.play();
},_leave:function(e){
this._hideAnim.play();
}});
}
