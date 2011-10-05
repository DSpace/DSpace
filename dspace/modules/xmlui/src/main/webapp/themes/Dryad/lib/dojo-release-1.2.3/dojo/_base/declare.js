/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.declare"]){
dojo._hasResource["dojo._base.declare"]=true;
dojo.provide("dojo._base.declare");
dojo.require("dojo._base.lang");
dojo.declare=function(_1,_2,_3){
var dd=arguments.callee,_5;
if(dojo.isArray(_2)){
_5=_2;
_2=_5.shift();
}
if(_5){
dojo.forEach(_5,function(m){
if(!m){
throw (_1+": mixin #"+i+" is null");
}
_2=dd._delegate(_2,m);
});
}
var _7=dd._delegate(_2);
_3=_3||{};
_7.extend(_3);
dojo.extend(_7,{declaredClass:_1,_constructor:_3.constructor});
_7.prototype.constructor=_7;
return dojo.setObject(_1,_7);
};
dojo.mixin(dojo.declare,{_delegate:function(_8,_9){
var bp=(_8||0).prototype,mp=(_9||0).prototype,dd=dojo.declare;
var _d=dd._makeCtor();
dojo.mixin(_d,{superclass:bp,mixin:mp,extend:dd._extend});
if(_8){
_d.prototype=dojo._delegate(bp);
}
dojo.extend(_d,dd._core,mp||0,{_constructor:null,preamble:null});
_d.prototype.constructor=_d;
_d.prototype.declaredClass=(bp||0).declaredClass+"_"+(mp||0).declaredClass;
return _d;
},_extend:function(_e){
var i,fn;
for(i in _e){
if(dojo.isFunction(fn=_e[i])&&!0[i]){
fn.nom=i;
fn.ctor=this;
}
}
dojo.extend(this,_e);
},_makeCtor:function(){
return function(){
this._construct(arguments);
};
},_core:{_construct:function(_11){
var c=_11.callee,s=c.superclass,ct=s&&s.constructor,m=c.mixin,mct=m&&m.constructor,a=_11,ii,fn;
if(a[0]){
if(((fn=a[0].preamble))){
a=fn.apply(this,a)||a;
}
}
if((fn=c.prototype.preamble)){
a=fn.apply(this,a)||a;
}
if(ct&&ct.apply){
ct.apply(this,a);
}
if(mct&&mct.apply){
mct.apply(this,a);
}
if((ii=c.prototype._constructor)){
ii.apply(this,_11);
}
if(this.constructor.prototype==c.prototype&&(ct=this.postscript)){
ct.apply(this,_11);
}
},_findMixin:function(_1a){
var c=this.constructor,p,m;
while(c){
p=c.superclass;
m=c.mixin;
if(m==_1a||(m instanceof _1a.constructor)){
return p;
}
if(m&&m._findMixin&&(m=m._findMixin(_1a))){
return m;
}
c=p&&p.constructor;
}
},_findMethod:function(_1e,_1f,_20,has){
var p=_20,c,m,f;
do{
c=p.constructor;
m=c.mixin;
if(m&&(m=this._findMethod(_1e,_1f,m,has))){
return m;
}
if((f=p[_1e])&&(has==(f==_1f))){
return p;
}
p=c.superclass;
}while(p);
return !has&&(p=this._findMixin(_20))&&this._findMethod(_1e,_1f,p,has);
},inherited:function(_26,_27,_28){
var a=arguments;
if(!dojo.isString(a[0])){
_28=_27;
_27=_26;
_26=_27.callee.nom;
}
a=_28||_27;
var c=_27.callee,p=this.constructor.prototype,fn,mp;
if(this[_26]!=c||p[_26]==c){
mp=(c.ctor||0).superclass||this._findMethod(_26,c,p,true);
if(!mp){
throw (this.declaredClass+": inherited method \""+_26+"\" mismatch");
}
p=this._findMethod(_26,c,mp,false);
}
fn=p&&p[_26];
if(!fn){
throw (mp.declaredClass+": inherited method \""+_26+"\" not found");
}
return fn.apply(this,a);
}}});
}
