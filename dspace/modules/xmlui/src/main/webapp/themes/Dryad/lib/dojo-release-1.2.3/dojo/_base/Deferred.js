/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.Deferred"]){
dojo._hasResource["dojo._base.Deferred"]=true;
dojo.provide("dojo._base.Deferred");
dojo.require("dojo._base.lang");
dojo.Deferred=function(_1){
this.chain=[];
this.id=this._nextId();
this.fired=-1;
this.paused=0;
this.results=[null,null];
this.canceller=_1;
this.silentlyCancelled=false;
};
dojo.extend(dojo.Deferred,{_nextId:(function(){
var n=1;
return function(){
return n++;
};
})(),cancel:function(){
var _3;
if(this.fired==-1){
if(this.canceller){
_3=this.canceller(this);
}else{
this.silentlyCancelled=true;
}
if(this.fired==-1){
if(!(_3 instanceof Error)){
var _4=_3;
_3=new Error("Deferred Cancelled");
_3.dojoType="cancel";
_3.cancelResult=_4;
}
this.errback(_3);
}
}else{
if((this.fired==0)&&(this.results[0] instanceof dojo.Deferred)){
this.results[0].cancel();
}
}
},_resback:function(_5){
this.fired=((_5 instanceof Error)?1:0);
this.results[this.fired]=_5;
this._fire();
},_check:function(){
if(this.fired!=-1){
if(!this.silentlyCancelled){
throw new Error("already called!");
}
this.silentlyCancelled=false;
return;
}
},callback:function(_6){
this._check();
this._resback(_6);
},errback:function(_7){
this._check();
if(!(_7 instanceof Error)){
_7=new Error(_7);
}
this._resback(_7);
},addBoth:function(cb,_9){
var _a=dojo.hitch.apply(dojo,arguments);
return this.addCallbacks(_a,_a);
},addCallback:function(cb,_c){
return this.addCallbacks(dojo.hitch.apply(dojo,arguments));
},addErrback:function(cb,_e){
return this.addCallbacks(null,dojo.hitch.apply(dojo,arguments));
},addCallbacks:function(cb,eb){
this.chain.push([cb,eb]);
if(this.fired>=0){
this._fire();
}
return this;
},_fire:function(){
var _11=this.chain;
var _12=this.fired;
var res=this.results[_12];
var _14=this;
var cb=null;
while((_11.length>0)&&(this.paused==0)){
var f=_11.shift()[_12];
if(!f){
continue;
}
var _17=function(){
var ret=f(res);
if(typeof ret!="undefined"){
res=ret;
}
_12=((res instanceof Error)?1:0);
if(res instanceof dojo.Deferred){
cb=function(res){
_14._resback(res);
_14.paused--;
if((_14.paused==0)&&(_14.fired>=0)){
_14._fire();
}
};
this.paused++;
}
};
if(dojo.config.isDebug){
_17.call(this);
}else{
try{
_17.call(this);
}
catch(err){
_12=1;
res=err;
}
}
}
this.fired=_12;
this.results[_12]=res;
if((cb)&&(this.paused)){
res.addBoth(cb);
}
}});
}
