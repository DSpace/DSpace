/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.widget.Roller"]){
dojo._hasResource["dojox.widget.Roller"]=true;
dojo.provide("dojox.widget.Roller");
dojo.require("dijit._Widget");
dojo.declare("dojox.widget.Roller",dijit._Widget,{delay:2000,autoStart:true,postCreate:function(){
if(!this["items"]){
this.items=[];
}
this._idx=-1;
dojo.addClass(this.domNode,"dojoxRoller");
dojo.query("li",this.domNode).forEach(function(_1){
this.items.push(_1.innerHTML);
dojo._destroyElement(_1);
},this);
this._roller=dojo.doc.createElement("li");
this.domNode.appendChild(this._roller);
this.makeAnims();
if(this.autoStart){
this.start();
}
},makeAnims:function(){
var n=this.domNode;
dojo.mixin(this,{_anim:{"in":dojo.fadeIn({node:n,duration:400}),"out":dojo.fadeOut({node:n,duration:275})}});
this._setupConnects();
},_setupConnects:function(){
var _3=this._anim;
this.connect(_3["out"],"onEnd",function(){
this._set(this._idx+1);
_3["in"].play(15);
});
this.connect(_3["in"],"onEnd",function(){
this._timeout=setTimeout(dojo.hitch(this,"_run"),this.delay);
});
},start:function(){
if(!this.rolling){
this.rolling=true;
this._run();
}
},_run:function(){
this._anim["out"].gotoPercent(0,true);
},stop:function(){
this.rolling=false;
var m=this._anim,t=this._timeout;
if(t){
clearTimeout(t);
}
m["in"].stop();
m["out"].stop();
},_set:function(i){
var l=this.items.length-1;
if(i<0){
i=l;
}
if(i>l){
i=0;
}
this._roller.innerHTML=this.items[i]||"error!";
this._idx=i;
}});
dojo.declare("dojox.widget.RollerSlide",dojox.widget.Roller,{makeAnims:function(){
var n=this.domNode;
var _9="position";
dojo.style(n,_9,"relative");
dojo.style(this._roller,_9,"absolute");
var _a={top:{end:0,start:25},opacity:1};
dojo.mixin(this,{_anim:{"in":dojo.animateProperty({node:n,duration:400,properties:_a}),"out":dojo.fadeOut({node:n,duration:175})}});
this._setupConnects();
}});
}
