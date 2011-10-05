/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.sketch.PreexistingAnnotation"]){
dojo._hasResource["dojox.sketch.PreexistingAnnotation"]=true;
dojo.provide("dojox.sketch.PreexistingAnnotation");
dojo.require("dojox.sketch.Annotation");
dojo.require("dojox.sketch.Anchor");
(function(){
var ta=dojox.sketch;
ta.PreexistingAnnotation=function(_2,id){
ta.Annotation.call(this,_2,id);
this.transform={dx:0,dy:0};
this.start={x:0,y:0};
this.end={x:200,y:200};
this.radius=8;
this.textPosition={x:196,y:196};
this.textOffset=4;
this.textAlign="end";
this.property("label",this.id);
this.rectShape=null;
this.labelShape=null;
this.anchors.start=new ta.Anchor(this,"start");
this.anchors.end=new ta.Anchor(this,"end");
};
ta.PreexistingAnnotation.prototype=new ta.Annotation;
var p=ta.PreexistingAnnotation.prototype;
p.constructor=ta.PreexistingAnnotation;
p.type=function(){
return "Preexisting";
};
p.getType=function(){
return ta.PreexistingAnnotation;
};
p._pos=function(){
var x=Math.min(this.start.x,this.end.x);
var y=Math.min(this.start.y,this.end.y);
var w=Math.max(this.start.x,this.end.x);
var h=Math.max(this.start.y,this.end.y);
this.start={x:x,y:y};
this.end={x:w,y:h};
this.textPosition={x:this.end.x-this.textOffset,y:this.end.y-this.textOffset};
};
p.apply=function(_9){
if(!_9){
return;
}
if(_9.documentElement){
_9=_9.documentElement;
}
this.readCommonAttrs(_9);
for(var i=0;i<_9.childNodes.length;i++){
var c=_9.childNodes[i];
if(c.localName=="text"){
this.property("label",c.childNodes.length?c.childNodes[0].nodeValue:"");
}else{
if(c.localName=="rect"){
if(c.getAttribute("x")!==null){
this.start.x=parseFloat(c.getAttribute("x"),10);
}
if(c.getAttribute("width")!==null){
this.end.x=parseFloat(c.getAttribute("width"),10)+parseFloat(c.getAttribute("x"),10);
}
if(c.getAttribute("y")!==null){
this.start.y=parseFloat(c.getAttribute("y"),10);
}
if(c.getAttribute("height")!==null){
this.end.y=parseFloat(c.getAttribute("height"),10)+parseFloat(c.getAttribute("y"),10);
}
if(c.getAttribute("r")!==null){
this.radius=parseFloat(c.getAttribute("r"),10);
}
}
}
}
};
p.initialize=function(_c){
var _d=(ta.Annotation.labelFont)?ta.Annotation.labelFont:{family:"Times",size:"16px"};
this.apply(_c);
this._pos();
this.shape=this.figure.group.createGroup();
this.shape.getEventSource().setAttribute("id",this.id);
if(this.transform.dx||this.transform.dy){
this.shape.setTransform(this.transform);
}
this.rectShape=this.shape.createRect({x:this.start.x,y:this.start.y,width:this.end.x-this.start.x,height:this.end.y-this.start.y,r:this.radius}).setStroke({color:this.property("fill"),width:1}).setFill([255,255,255,0.1]);
this.rectShape.getEventSource().setAttribute("shape-rendering","crispEdges");
this.labelShape=this.shape.createText({x:this.textPosition.x,y:this.textPosition.y,text:this.property("label"),align:this.textAlign}).setFont(_d).setFill(this.property("fill"));
};
p.destroy=function(){
if(!this.shape){
return;
}
this.shape.remove(this.rectShape);
this.shape.remove(this.labelShape);
this.figure.group.remove(this.shape);
this.shape=this.rectShape=this.labelShape=null;
};
p.getBBox=function(){
var x=Math.min(this.start.x,this.end.x);
var y=Math.min(this.start.y,this.end.y);
var w=Math.max(this.start.x,this.end.x)-x;
var h=Math.max(this.start.y,this.end.y)-y;
return {x:x-2,y:y-2,width:w+4,height:h+4};
};
p.draw=function(obj){
this.apply(obj);
this._pos();
this.shape.setTransform(this.transform);
this.rectShape.setShape({x:this.start.x,y:this.start.y,width:this.end.x-this.start.x,height:this.end.y-this.start.y,r:this.radius}).setStroke({color:this.property("fill"),width:1}).setFill([255,255,255,0.1]);
this.labelShape.setShape({x:this.textPosition.x,y:this.textPosition.y,text:this.property("label")}).setFill(this.property("fill"));
};
p.serialize=function(){
var s=this.property("stroke");
return "<g "+this.writeCommonAttrs()+">"+"<rect style=\"stroke:"+s.color+";stroke-weight:1;fill:none;\" "+"x=\""+this.start.x+"\" "+"width=\""+(this.end.x-this.start.x)+"\" "+"y=\""+this.start.y+"\" "+"height=\""+(this.end.y-this.start.y)+"\" "+"rx=\""+this.radius+"\" "+"ry=\""+this.radius+"\" "+" />"+"<text style=\"fill:"+s.color+";text-anchor:"+this.textAlign+"\" font-weight=\"bold\" "+"x=\""+this.textPosition.x+"\" "+"y=\""+this.textPosition.y+"\">"+this.property("label")+"</text>"+"</g>";
};
ta.Annotation.register("Preexisting");
})();
}
