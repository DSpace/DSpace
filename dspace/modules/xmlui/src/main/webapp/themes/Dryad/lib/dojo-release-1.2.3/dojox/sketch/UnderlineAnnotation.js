/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.sketch.UnderlineAnnotation"]){
dojo._hasResource["dojox.sketch.UnderlineAnnotation"]=true;
dojo.provide("dojox.sketch.UnderlineAnnotation");
dojo.require("dojox.sketch.Annotation");
dojo.require("dojox.sketch.Anchor");
(function(){
var ta=dojox.sketch;
ta.UnderlineAnnotation=function(_2,id){
ta.Annotation.call(this,_2,id);
this.transform={dx:0,dy:0};
this.start={x:0,y:0};
this.property("label",this.id);
this.labelShape=null;
this.lineShape=null;
this.anchors.start=new ta.Anchor(this,"start",false);
};
ta.UnderlineAnnotation.prototype=new ta.Annotation;
var p=ta.UnderlineAnnotation.prototype;
p.constructor=ta.UnderlineAnnotation;
p.type=function(){
return "Underline";
};
p.getType=function(){
return ta.UnderlineAnnotation;
};
p.apply=function(_5){
if(!_5){
return;
}
if(_5.documentElement){
_5=_5.documentElement;
}
this.readCommonAttrs(_5);
for(var i=0;i<_5.childNodes.length;i++){
var c=_5.childNodes[i];
if(c.localName=="text"){
this.property("label",c.childNodes[0].nodeValue);
}
}
};
p.initialize=function(_8){
var _9=(ta.Annotation.labelFont)?ta.Annotation.labelFont:{family:"Times",size:"16px"};
this.apply(_8);
this.shape=this.figure.group.createGroup();
this.shape.getEventSource().setAttribute("id",this.id);
if(this.transform.dx||this.transform.dy){
this.shape.setTransform(this.transform);
}
this.labelShape=this.shape.createText({x:0,y:0,text:this.property("label"),align:"start"}).setFont(_9).setFill(this.property("fill"));
this.lineShape=this.shape.createLine({x1:1,x2:this.labelShape.getTextWidth(),y1:2,y2:2}).setStroke({color:this.property("fill"),width:1});
this.lineShape.getEventSource().setAttribute("shape-rendering","crispEdges");
};
p.destroy=function(){
if(!this.shape){
return;
}
this.shape.remove(this.labelShape);
this.shape.remove(this.lineShape);
this.figure.group.remove(this.shape);
this.shape=this.lineShape=this.labelShape=null;
};
p.getBBox=function(){
var b=this.getTextBox();
return {x:0,y:b.h*-1+4,width:b.w+2,height:b.h};
};
p.draw=function(_b){
this.apply(_b);
this.shape.setTransform(this.transform);
this.labelShape.setShape({x:0,y:0,text:this.property("label")}).setFill(this.property("fill"));
this.lineShape.setShape({x1:1,x2:this.labelShape.getTextWidth()+1,y1:2,y2:2}).setStroke({color:this.property("fill"),width:1});
};
p.serialize=function(){
var s=this.property("stroke");
return "<g "+this.writeCommonAttrs()+">"+"<line x1=\"1\" x2=\""+this.labelShape.getTextWidth()+1+"\" y1=\"5\" y2=\"5\" style=\"stroke:"+s.color+";stroke-weight:"+s.width+"\" />"+"<text style=\"fill:"+this.property("fill")+";\" font-weight=\"bold\" "+"x=\"0\" y=\"0\">"+this.property("label")+"</text>"+"</g>";
};
ta.Annotation.register("Underline");
})();
}
