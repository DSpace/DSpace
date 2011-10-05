/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.charting.widget.Chart2D"]){
dojo._hasResource["dojox.charting.widget.Chart2D"]=true;
dojo.provide("dojox.charting.widget.Chart2D");
dojo.require("dijit._Widget");
dojo.require("dojox.charting.Chart2D");
dojo.require("dojox.lang.functional");
dojo.require("dojox.charting.action2d.Highlight");
dojo.require("dojox.charting.action2d.Magnify");
dojo.require("dojox.charting.action2d.MoveSlice");
dojo.require("dojox.charting.action2d.Shake");
dojo.require("dojox.charting.action2d.Tooltip");
(function(){
var _1,_2,_3,_4,_5,_6=function(o){
return o;
},df=dojox.lang.functional,du=dojox.lang.utils,dc=dojox.charting,d=dojo;
dojo.declare("dojox.charting.widget.Chart2D",dijit._Widget,{theme:null,margins:null,stroke:null,fill:null,buildRendering:function(){
var n=this.domNode=this.srcNodeRef;
var _d=d.query("> .axis",n).map(_2).filter(_6),_e=d.query("> .plot",n).map(_3).filter(_6),_f=d.query("> .action",n).map(_4).filter(_6),_10=d.query("> .series",n).map(_5).filter(_6);
n.innerHTML="";
var c=this.chart=new dc.Chart2D(n,{margins:this.margins,stroke:this.stroke,fill:this.fill});
if(this.theme){
c.setTheme(this.theme);
}
_d.forEach(function(_12){
c.addAxis(_12.name,_12.kwArgs);
});
_e.forEach(function(_13){
c.addPlot(_13.name,_13.kwArgs);
});
this.actions=_f.map(function(_14){
return new _14.action(c,_14.plot,_14.kwArgs);
});
var _15=df.foldl(_10,function(_16,_17){
if(_17.type=="data"){
c.addSeries(_17.name,_17.data,_17.kwArgs);
_16=true;
}else{
c.addSeries(_17.name,[0],_17.kwArgs);
var kw={};
du.updateWithPattern(kw,_17.kwArgs,{"query":"","queryOptions":null,"start":0,"count":1},true);
if(_17.kwArgs.sort){
kw.sort=dojo.clone(_17.kwArgs.sort);
}
d.mixin(kw,{onComplete:function(_19){
var _1a;
if("valueFn" in _17.kwArgs){
var fn=_17.kwArgs.valueFn;
_1a=d.map(_19,function(x){
return fn(_17.data.getValue(x,_17.field,0));
});
}else{
_1a=d.map(_19,function(x){
return _17.data.getValue(x,_17.field,0);
});
}
c.addSeries(_17.name,_1a,_17.kwArgs).render();
}});
_17.data.fetch(kw);
}
return _16;
},false);
if(_15){
c.render();
}
},destroy:function(){
this.chart.destroy();
this.inherited(arguments);
},resize:function(box){
if(box.w>0&&box.h>0){
dojo.marginBox(this.domNode,box);
this.chart.resize();
}
}});
_1=function(_1f,_20,kw){
var dp=eval("("+_20+".prototype.defaultParams)");
var x,_24;
for(x in dp){
if(x in kw){
continue;
}
_24=_1f.getAttribute(x);
kw[x]=du.coerceType(dp[x],_24==null||typeof _24=="undefined"?dp[x]:_24);
}
var op=eval("("+_20+".prototype.optionalParams)");
for(x in op){
if(x in kw){
continue;
}
_24=_1f.getAttribute(x);
if(_24!=null){
kw[x]=du.coerceType(op[x],_24);
}
}
};
_2=function(_26){
var _27=_26.getAttribute("name"),_28=_26.getAttribute("type");
if(!_27){
return null;
}
var o={name:_27,kwArgs:{}},kw=o.kwArgs;
if(_28){
if(dc.axis2d[_28]){
_28=dojox._scopeName+".charting.axis2d."+_28;
}
var _2b=eval("("+_28+")");
if(_2b){
kw.type=_2b;
}
}else{
_28=dojox._scopeName+".charting.axis2d.Default";
}
_1(_26,_28,kw);
return o;
};
_3=function(_2c){
var _2d=_2c.getAttribute("name"),_2e=_2c.getAttribute("type");
if(!_2d){
return null;
}
var o={name:_2d,kwArgs:{}},kw=o.kwArgs;
if(_2e){
if(dc.plot2d[_2e]){
_2e=dojox._scopeName+".charting.plot2d."+_2e;
}
var _31=eval("("+_2e+")");
if(_31){
kw.type=_31;
}
}else{
_2e=dojox._scopeName+".charting.plot2d.Default";
}
_1(_2c,_2e,kw);
return o;
};
_4=function(_32){
var _33=_32.getAttribute("plot"),_34=_32.getAttribute("type");
if(!_33){
_33="default";
}
var o={plot:_33,kwArgs:{}},kw=o.kwArgs;
if(_34){
if(dc.action2d[_34]){
_34=dojox._scopeName+".charting.action2d."+_34;
}
var _37=eval("("+_34+")");
if(!_37){
return null;
}
o.action=_37;
}else{
return null;
}
_1(_32,_34,kw);
return o;
};
_5=function(_38){
var ga=d.partial(d.attr,_38);
var _3a=ga("name");
if(!_3a){
return null;
}
var o={name:_3a,kwArgs:{}},kw=o.kwArgs,t;
t=ga("plot");
if(t!=null){
kw.plot=t;
}
t=ga("marker");
if(t!=null){
kw.marker=t;
}
t=ga("stroke");
if(t!=null){
kw.stroke=eval("("+t+")");
}
t=ga("fill");
if(t!=null){
kw.fill=eval("("+t+")");
}
t=ga("legend");
if(t!=null){
kw.legend=t;
}
t=ga("data");
if(t!=null){
o.type="data";
o.data=dojo.map(String(t).split(","),Number);
return o;
}
t=ga("array");
if(t!=null){
o.type="data";
o.data=eval("("+t+")");
return o;
}
t=ga("store");
if(t!=null){
o.type="store";
o.data=eval("("+t+")");
t=ga("field");
o.field=t!=null?t:"value";
t=ga("query");
if(!!t){
kw.query=t;
}
t=ga("queryOptions");
if(!!t){
kw.queryOptions=eval("("+t+")");
}
t=ga("start");
if(!!t){
kw.start=Number(t);
}
t=ga("count");
if(!!t){
kw.count=Number(t);
}
t=ga("sort");
if(!!t){
kw.sort=eval("("+t+")");
}
t=ga("valueFn");
if(!!t){
kw.valueFn=df.lambda(t);
}
return o;
}
return null;
};
})();
}
