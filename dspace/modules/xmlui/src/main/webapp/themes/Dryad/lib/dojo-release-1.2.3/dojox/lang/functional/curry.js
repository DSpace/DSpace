/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.lang.functional.curry"]){
dojo._hasResource["dojox.lang.functional.curry"]=true;
dojo.provide("dojox.lang.functional.curry");
dojo.require("dojox.lang.functional.lambda");
(function(){
var df=dojox.lang.functional,ap=Array.prototype;
var _3=function(_4){
return function(){
if(arguments.length+_4.args.length<_4.arity){
return _3({func:_4.func,arity:_4.arity,args:ap.concat.apply(_4.args,arguments)});
}
return _4.func.apply(this,ap.concat.apply(_4.args,arguments));
};
};
dojo.mixin(df,{curry:function(f,_6){
f=df.lambda(f);
_6=typeof _6=="number"?_6:f.length;
return _3({func:f,arity:_6,args:[]});
},arg:{},partial:function(f){
var a=arguments,_9=new Array(a.length-1),p=[];
f=df.lambda(f);
for(var i=1;i<a.length;++i){
var t=a[i];
_9[i-1]=t;
if(t==df.arg){
p.push(i-1);
}
}
return function(){
var t=ap.slice.call(_9,0);
for(var i=0;i<p.length;++i){
t[p[i]]=arguments[i];
}
return f.apply(this,t);
};
},mixer:function(f,mix){
f=df.lambda(f);
return function(){
var t=new Array(mix.length);
for(var i=0;i<mix.length;++i){
t[i]=arguments[mix[i]];
}
return f.apply(this,t);
};
},flip:function(f){
f=df.lambda(f);
return function(){
var a=arguments,l=a.length-1,t=new Array(l+1),i;
for(i=0;i<=l;++i){
t[l-i]=a[i];
}
return f.apply(this,t);
};
}});
})();
}
