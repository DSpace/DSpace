/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.sketch.SingleArrowAnnotation"]){
dojo._hasResource["dojox.sketch.SingleArrowAnnotation"]=true;
dojo.provide("dojox.sketch.SingleArrowAnnotation");
dojo.require("dojox.sketch.Annotation");
dojo.require("dojox.sketch.Anchor");
(function(){
var ta=dojox.sketch;
ta.SingleArrowAnnotation=function(_2,id){
ta.Annotation.call(this,_2,id);
this.transform={dx:0,dy:0};
this.start={x:0,y:0};
this.control={x:100,y:-50};
this.end={x:200,y:0};
this.textPosition={x:0,y:0};
this.textOffset=4;
this.textAlign="middle";
this.textYOffset=10;
this.rotation=0;
this.pathShape=null;
this.arrowhead=null;
this.arrowheadGroup=null;
this.labelShape=null;
this.anchors.start=new ta.Anchor(this,"start");
this.anchors.control=new ta.Anchor(this,"control");
this.anchors.end=new ta.Anchor(this,"end");
};
ta.SingleArrowAnnotation.prototype=new ta.Annotation;
var p=ta.SingleArrowAnnotation.prototype;
p.constructor=ta.SingleArrowAnnotation;
p.type=function(){
return "SingleArrow";
};
p.getType=function(){
return ta.SingleArrowAnnotation;
};
p._rot=function(){
var _5=this.start.y-this.control.y;
var _6=this.start.x-this.control.x;
if(!_6){
_6=1;
}
this.rotation=Math.atan(_5/_6);
};
p._pos=function(){
var _7=this.textOffset,x=0,y=0;
var _a=this.calculate.slope(this.control,this.end);
if(Math.abs(_a)>=1){
x=this.end.x+this.calculate.dx(this.control,this.end,_7);
if(this.control.y>this.end.y){
y=this.end.y-_7;
}else{
y=this.end.y+_7+this.textYOffset;
}
}else{
if(_a==0){
x=this.end.x+_7;
y=this.end.y+this.textYOffset;
}else{
if(this.start.x>this.end.x){
x=this.end.x-_7;
this.textAlign="end";
}else{
x=this.end.x+_7;
this.textAlign="start";
}
if(this.start.y<this.end.y){
y=this.end.y+this.calculate.dy(this.control,this.end,_7)+this.textYOffset;
}else{
y=this.end.y+this.calculate.dy(this.control,this.end,-_7);
}
}
}
this.textPosition={x:x,y:y};
};
p.apply=function(_b){
if(!_b){
return;
}
if(_b.documentElement){
_b=_b.documentElement;
}
this.readCommonAttrs(_b);
for(var i=0;i<_b.childNodes.length;i++){
var c=_b.childNodes[i];
if(c.localName=="text"){
this.property("label",c.childNodes.length?c.childNodes[0].nodeValue:"");
}else{
if(c.localName=="path"){
var d=c.getAttribute("d").split(" ");
var s=d[0].split(",");
this.start.x=parseFloat(s[0].substr(1),10);
this.start.y=parseFloat(s[1],10);
s=d[1].split(",");
this.control.x=parseFloat(s[0].substr(1),10);
this.control.y=parseFloat(s[1],10);
s=d[2].split(",");
this.end.x=parseFloat(s[0],10);
this.end.y=parseFloat(s[1],10);
}
}
}
};
p.initialize=function(obj){
var _11=(ta.Annotation.labelFont)?ta.Annotation.labelFont:{family:"Times",size:"16px"};
this.apply(obj);
this._rot();
this._pos();
var rot=this.rotation;
if(this.control.x>=this.end.x&&this.control.x<this.start.x){
rot+=Math.PI;
}
var _13=dojox.gfx.matrix.rotate(rot);
this.shape=this.figure.group.createGroup();
this.shape.getEventSource().setAttribute("id",this.id);
if(this.transform.dx||this.transform.dy){
this.shape.setTransform(this.transform);
}
this.pathShape=this.shape.createPath("M"+this.start.x+","+this.start.y+" Q"+this.control.x+","+this.control.y+" "+this.end.x+","+this.end.y+" l0,0").setStroke(this.property("stroke"));
this.arrowheadGroup=this.shape.createGroup().setTransform({dx:this.start.x,dy:this.start.y}).applyTransform(_13);
this.arrowhead=this.arrowheadGroup.createPath("M0,0 l20,-5 -3,5 3,5 Z").setFill(this.property("fill"));
this.labelShape=this.shape.createText({x:this.textPosition.x,y:this.textPosition.y,text:this.property("label"),align:this.textAlign}).setFont(_11).setFill(this.property("fill"));
};
p.destroy=function(){
if(!this.shape){
return;
}
this.arrowheadGroup.remove(this.arrowhead);
this.shape.remove(this.arrowheadGroup);
this.shape.remove(this.pathShape);
this.shape.remove(this.labelShape);
this.figure.group.remove(this.shape);
this.shape=this.pathShape=this.labelShape=this.arrowheadGroup=this.arrowhead=null;
};
p.draw=function(obj){
this.apply(obj);
this._rot();
this._pos();
var rot=this.rotation;
if(this.control.x<this.start.x){
rot+=Math.PI;
}
var _16=dojox.gfx.matrix.rotate(rot);
this.shape.setTransform(this.transform);
this.pathShape.setShape("M"+this.start.x+","+this.start.y+" Q"+this.control.x+","+this.control.y+" "+this.end.x+","+this.end.y+" l0,0").setStroke(this.property("stroke"));
this.arrowheadGroup.setTransform({dx:this.start.x,dy:this.start.y}).applyTransform(_16);
this.arrowhead.setFill(this.property("fill"));
this.labelShape.setShape({x:this.textPosition.x,y:this.textPosition.y,text:this.property("label"),align:this.textAlign}).setFill(this.property("fill"));
};
p.getBBox=function(){
var x=Math.min(this.start.x,this.control.x,this.end.x);
var y=Math.min(this.start.y,this.control.y,this.end.y);
var w=Math.max(this.start.x,this.control.x,this.end.x)-x;
var h=Math.max(this.start.y,this.control.y,this.end.y)-y;
return {x:x,y:y,width:w,height:h};
};
p.serialize=function(){
var s=this.property("stroke");
var r=this.rotation*(180/Math.PI);
if(this.start.x>this.end.x){
r-=180;
}
r=Math.round(r*Math.pow(10,4))/Math.pow(10,4);
return "<g "+this.writeCommonAttrs()+">"+"<path style=\"stroke:"+s.color+";stroke-width:"+s.width+";fill:none;\" d=\""+"M"+this.start.x+","+this.start.y+" "+"Q"+this.control.x+","+this.control.y+" "+this.end.x+","+this.end.y+"\" />"+"<g transform=\"translate("+this.start.x+","+this.start.y+") "+"rotate("+r+")\">"+"<path style=\"fill:"+s.color+";\" d=\"M0,0 l20,-5, -3,5, 3,5 Z\" />"+"</g>"+"<text style=\"fill:"+s.color+";text-anchor:"+this.textAlign+"\" font-weight=\"bold\" "+"x=\""+this.textPosition.x+"\" "+"y=\""+this.textPosition.y+"\">"+this.property("label")+"</text>"+"</g>";
};
ta.Annotation.register("SingleArrow");
})();
}
