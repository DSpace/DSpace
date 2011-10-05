/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.NodeList"]){
dojo._hasResource["dojo._base.NodeList"]=true;
dojo.provide("dojo._base.NodeList");
dojo.require("dojo._base.lang");
dojo.require("dojo._base.array");
(function(){
var d=dojo;
var _2=function(_3){
_3.constructor=dojo.NodeList;
dojo._mixin(_3,dojo.NodeList.prototype);
return _3;
};
var _4=function(_5,_6){
return function(){
var _a=arguments;
var aa=d._toArray(_a,0,[null]);
var s=this.map(function(i){
aa[0]=i;
return d[_5].apply(d,aa);
});
return (_6||((_a.length>1)||!d.isString(_a[0])))?this:s;
};
};
dojo.NodeList=function(){
return _2(Array.apply(null,arguments));
};
dojo.NodeList._wrap=_2;
dojo.extend(dojo.NodeList,{slice:function(){
var a=d._toArray(arguments);
return _2(a.slice.apply(this,a));
},splice:function(){
var a=d._toArray(arguments);
return _2(a.splice.apply(this,a));
},concat:function(){
var a=d._toArray(arguments,0,[this]);
return _2(a.concat.apply([],a));
},indexOf:function(_e,_f){
return d.indexOf(this,_e,_f);
},lastIndexOf:function(){
return d.lastIndexOf.apply(d,d._toArray(arguments,0,[this]));
},every:function(_10,_11){
return d.every(this,_10,_11);
},some:function(_12,_13){
return d.some(this,_12,_13);
},map:function(_14,obj){
return d.map(this,_14,obj,d.NodeList);
},forEach:function(_16,_17){
d.forEach(this,_16,_17);
return this;
},coords:function(){
return d.map(this,d.coords);
},attr:_4("attr"),style:_4("style"),addClass:_4("addClass",true),removeClass:_4("removeClass",true),toggleClass:_4("toggleClass",true),connect:_4("connect",true),place:function(_18,_19){
var _1a=d.query(_18)[0];
return this.forEach(function(i){
d.place(i,_1a,_19);
});
},orphan:function(_1c){
return (_1c?d._filterQueryResult(this,_1c):this).forEach("if(item.parentNode){ item.parentNode.removeChild(item); }");
},adopt:function(_1d,_1e){
var _1f=this[0];
return d.query(_1d).forEach(function(ai){
d.place(ai,_1f,_1e||"last");
});
},query:function(_21){
if(!_21){
return this;
}
var ret=d.NodeList();
this.forEach(function(_23){
ret=ret.concat(d.query(_21,_23).filter(function(_24){
return (_24!==undefined);
}));
});
return ret;
},filter:function(_25){
var _26=this;
var _a=arguments;
var r=d.NodeList();
var rp=function(t){
if(t!==undefined){
r.push(t);
}
};
if(d.isString(_25)){
_26=d._filterQueryResult(this,_a[0]);
if(_a.length==1){
return _26;
}
_a.shift();
}
d.forEach(d.filter(_26,_a[0],_a[1]),rp);
return r;
},addContent:function(_2b,_2c){
var ta=d.doc.createElement("span");
if(d.isString(_2b)){
ta.innerHTML=_2b;
}else{
ta.appendChild(_2b);
}
if(_2c===undefined){
_2c="last";
}
var ct=(_2c=="first"||_2c=="after")?"lastChild":"firstChild";
this.forEach(function(_2f){
var tn=ta.cloneNode(true);
while(tn[ct]){
d.place(tn[ct],_2f,_2c);
}
});
return this;
},empty:function(){
return this.forEach("item.innerHTML='';");
},instantiate:function(_31,_32){
var c=d.isFunction(_31)?_31:d.getObject(_31);
return this.forEach(function(i){
new c(_32||{},i);
});
},at:function(){
var nl=new dojo.NodeList();
dojo.forEach(arguments,function(i){
if(this[i]){
nl.push(this[i]);
}
},this);
return nl;
}});
d.forEach(["blur","focus","click","keydown","keypress","keyup","mousedown","mouseenter","mouseleave","mousemove","mouseout","mouseover","mouseup","submit","load","error"],function(evt){
var _oe="on"+evt;
d.NodeList.prototype[_oe]=function(a,b){
return this.connect(_oe,a,b);
};
});
})();
}
