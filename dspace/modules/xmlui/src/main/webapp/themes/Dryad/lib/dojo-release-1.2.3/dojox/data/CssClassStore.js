/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.CssClassStore"]){
dojo._hasResource["dojox.data.CssClassStore"]=true;
dojo.provide("dojox.data.CssClassStore");
dojo.require("dojox.data.CssRuleStore");
dojo.declare("dojox.data.CssClassStore",dojox.data.CssRuleStore,{_labelAttribute:"class",_idAttribute:"class",_cName:"dojox.data.CssClassStore",getFeatures:function(){
return {"dojo.data.api.Read":true,"dojo.data.api.Identity":true};
},getAttributes:function(_1){
this._assertIsItem(_1);
return ["class","classSans"];
},getValue:function(_2,_3,_4){
var _5=this.getValues(_2,_3);
if(_5&&_5.length>0){
return _5[0];
}
return _4;
},getValues:function(_6,_7){
this._assertIsItem(_6);
this._assertIsAttribute(_7);
var _8=[];
if(_7==="class"){
_8=[_6.className];
}else{
if(_7==="classSans"){
_8=[_6.className.replace(/\./g,"")];
}
}
return _8;
},_handleRule:function(_9,_a,_b){
var _c={};
var s=_9["selectorText"].split(" ");
for(j=0;j<s.length;j++){
var _e=s[j];
var _f=_e.indexOf(".");
if(_e&&_e.length>0&&_f!==-1){
var _10=_e.indexOf(",")||_e.indexOf("[");
_e=_e.substring(_f,((_10!==-1&&_10>_f)?_10:_e.length));
_c[_e]=true;
}
}
for(var key in _c){
if(!this._allItems[key]){
var _12={};
_12.className=key;
_12[this._storeRef]=this;
this._allItems[key]=_12;
}
}
},_handleReturn:function(){
var _13=[];
var _14={};
for(var i in this._allItems){
_14[i]=this._allItems[i];
}
var _16;
while(this._pending.length){
_16=this._pending.pop();
_16.request._items=_14;
_13.push(_16);
}
while(_13.length){
_16=_13.pop();
if(_16.fetch){
this._handleFetchReturn(_16.request);
}else{
this._handleFetchByIdentityReturn(_16.request);
}
}
},_handleFetchByIdentityReturn:function(_17){
var _18=_17._items;
var _19=_18[(dojo.isSafari?_17.identity.toLowerCase():_17.identity)];
if(!this.isItem(_19)){
_19=null;
}
if(_17.onItem){
var _1a=_17.scope||dojo.global;
_17.onItem.call(_1a,_19);
}
},getIdentity:function(_1b){
this._assertIsItem(_1b);
return this.getValue(_1b,this._idAttribute);
},getIdentityAttributes:function(_1c){
this._assertIsItem(_1c);
return [this._idAttribute];
},fetchItemByIdentity:function(_1d){
_1d=_1d||{};
if(!_1d.store){
_1d.store=this;
}
if(this._pending&&this._pending.length>0){
this._pending.push({request:_1d});
}else{
this._pending=[{request:_1d}];
this._fetch(_1d);
}
return _1d;
}});
}
