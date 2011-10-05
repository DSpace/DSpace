/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.CssRuleStore"]){
dojo._hasResource["dojox.data.CssRuleStore"]=true;
dojo.provide("dojox.data.CssRuleStore");
dojo.require("dojo.data.util.filter");
dojo.require("dojo.data.util.sorter");
dojo.require("dojox.data.css");
dojo.declare("dojox.data.CssRuleStore",null,{_storeRef:"_S",_labelAttribute:"selector",_cache:null,_browserMap:null,_cName:"dojox.data.CssRuleStore",constructor:function(_1){
if(_1){
dojo.mixin(this,_1);
}
this._cache={};
this._allItems=null;
this._waiting=[];
this.gatherHandle=null;
var _2=this;
function gatherRules(){
try{
_2.context=dojox.data.css.determineContext(_2.context);
if(_2.gatherHandle){
clearInterval(_2.gatherHandle);
_2.gatherHandle=null;
}
while(_2._waiting.length){
var _3=_2._waiting.pop();
dojox.data.css.rules.forEach(_3.forFunc,null,_2.context);
_3.finishFunc();
}
}
catch(e){
}
};
this.gatherHandle=setInterval(gatherRules,250);
},setContext:function(_4){
if(_4){
this.close();
this.context=dojox.data.css.determineContext(_4);
}
},getFeatures:function(){
return {"dojo.data.api.Read":true};
},isItem:function(_5){
if(_5&&_5[this._storeRef]==this){
return true;
}
return false;
},hasAttribute:function(_6,_7){
this._assertIsItem(_6);
this._assertIsAttribute(_7);
var _8=this.getAttributes(_6);
if(dojo.indexOf(_8,_7)!=-1){
return true;
}
return false;
},getAttributes:function(_9){
this._assertIsItem(_9);
var _a=["selector","classes","rule","style","cssText","styleSheet","parentStyleSheet","parentStyleSheetHref"];
var _b=_9.rule.style;
if(_b){
var _c;
for(_c in _b){
_a.push("style."+_c);
}
}
return _a;
},getValue:function(_d,_e,_f){
var _10=this.getValues(_d,_e);
var _11=_f;
if(_10&&_10.length>0){
return _10[0];
}
return _f;
},getValues:function(_12,_13){
this._assertIsItem(_12);
this._assertIsAttribute(_13);
var _14=null;
if(_13==="selector"){
_14=_12.rule["selectorText"];
if(_14&&dojo.isString(_14)){
_14=_14.split(",");
}
}else{
if(_13==="classes"){
_14=_12.classes;
}else{
if(_13==="rule"){
_14=_12.rule.rule;
}else{
if(_13==="style"){
_14=_12.rule.style;
}else{
if(_13==="cssText"){
if(dojo.isIE){
if(_12.rule.style){
_14=_12.rule.style.cssText;
if(_14){
_14="{ "+_14.toLowerCase()+" }";
}
}
}else{
_14=_12.rule.cssText;
if(_14){
_14=_14.substring(_14.indexOf("{"),_14.length);
}
}
}else{
if(_13==="styleSheet"){
_14=_12.rule.styleSheet;
}else{
if(_13==="parentStyleSheet"){
_14=_12.rule.parentStyleSheet;
}else{
if(_13==="parentStyleSheetHref"){
if(_12.href){
_14=_12.href;
}
}else{
if(_13.indexOf("style.")===0){
var _15=_13.substring(_13.indexOf("."),_13.length);
_14=_12.rule.style[_15];
}else{
_14=[];
}
}
}
}
}
}
}
}
}
if(_14!==undefined){
if(!dojo.isArray(_14)){
_14=[_14];
}
}
return _14;
},getLabel:function(_16){
this._assertIsItem(_16);
return this.getValue(_16,this._labelAttribute);
},getLabelAttributes:function(_17){
return [this._labelAttribute];
},containsValue:function(_18,_19,_1a){
var _1b=undefined;
if(typeof _1a==="string"){
_1b=dojo.data.util.filter.patternToRegExp(_1a,false);
}
return this._containsValue(_18,_19,_1a,_1b);
},isItemLoaded:function(_1c){
return this.isItem(_1c);
},loadItem:function(_1d){
this._assertIsItem(_1d.item);
},fetch:function(_1e){
_1e=_1e||{};
if(!_1e.store){
_1e.store=this;
}
var _1f=_1e.scope||dojo.global;
if(this._pending&&this._pending.length>0){
this._pending.push({request:_1e,fetch:true});
}else{
this._pending=[{request:_1e,fetch:true}];
this._fetch(_1e);
}
return _1e;
},_fetch:function(_20){
var _21=_20.scope||dojo.global;
if(this._allItems===null){
this._allItems={};
try{
if(this.gatherHandle){
this._waiting.push({"forFunc":dojo.hitch(this,this._handleRule),"finishFunc":dojo.hitch(this,this._handleReturn)});
}else{
dojox.data.css.rules.forEach(dojo.hitch(this,this._handleRule),null,this.context);
this._handleReturn();
}
}
catch(e){
if(_20.onError){
_20.onError.call(_21,e,_20);
}
}
}else{
this._handleReturn();
}
},_handleRule:function(_22,_23,_24){
var _25=_22["selectorText"];
var s=_25.split(" ");
var _27=[];
for(j=0;j<s.length;j++){
var tmp=s[j];
var _29=tmp.indexOf(".");
if(tmp&&tmp.length>0&&_29!==-1){
var _2a=tmp.indexOf(",")||tmp.indexOf("[");
tmp=tmp.substring(_29,((_2a!==-1&&_2a>_29)?_2a:tmp.length));
_27.push(tmp);
}
}
var _2b={};
_2b.rule=_22;
_2b.styleSheet=_23;
_2b.href=_24;
_2b.classes=_27;
_2b[this._storeRef]=this;
if(!this._allItems[_25]){
this._allItems[_25]=[];
}
this._allItems[_25].push(_2b);
},_handleReturn:function(){
var _2c=[];
var _2d=[];
var _2e=null;
for(var i in this._allItems){
_2e=this._allItems[i];
for(var j in _2e){
_2d.push(_2e[j]);
}
}
var _31;
while(this._pending.length){
_31=this._pending.pop();
_31.request._items=_2d;
_2c.push(_31);
}
while(_2c.length){
_31=_2c.pop();
this._handleFetchReturn(_31.request);
}
},_handleFetchReturn:function(_32){
var _33=_32.scope||dojo.global;
var _34=[];
var _35="all";
var i;
if(_32.query){
_35=dojo.toJson(_32.query);
}
if(this._cache[_35]){
_34=this._cache[_35];
}else{
if(_32.query){
for(i in _32._items){
var _37=_32._items[i];
var _38=dojo.isSafari?true:(_32.queryOptions?_32.queryOptions.ignoreCase:false);
var _39={};
var key;
var _3b;
for(key in _32.query){
_3b=_32.query[key];
if(typeof _3b==="string"){
_39[key]=dojo.data.util.filter.patternToRegExp(_3b,_38);
}
}
var _3c=true;
for(key in _32.query){
_3b=_32.query[key];
if(!this._containsValue(_37,key,_3b,_39[key])){
_3c=false;
}
}
if(_3c){
_34.push(_37);
}
}
this._cache[_35]=_34;
}else{
for(i in _32._items){
_34.push(_32._items[i]);
}
}
}
var _3d=_34.length;
if(_32.sort){
_34.sort(dojo.data.util.sorter.createSortFunction(_32.sort,this));
}
var _3e=0;
var _3f=_34.length;
if(_32.start>0&&_32.start<_34.length){
_3e=_32.start;
}
if(_32.count&&_32.count){
_3f=_32.count;
}
var _40=_3e+_3f;
if(_40>_34.length){
_40=_34.length;
}
_34=_34.slice(_3e,_40);
if(_32.onBegin){
_32.onBegin.call(_33,_3d,_32);
}
if(_32.onItem){
if(dojo.isArray(_34)){
for(i=0;i<_34.length;i++){
_32.onItem.call(_33,_34[i],_32);
}
if(_32.onComplete){
_32.onComplete.call(_33,null,_32);
}
}
}else{
if(_32.onComplete){
_32.onComplete.call(_33,_34,_32);
}
}
return _32;
},close:function(){
this._cache={};
this._allItems=null;
},_assertIsItem:function(_41){
if(!this.isItem(_41)){
throw new Error(this._cName+": Invalid item argument.");
}
},_assertIsAttribute:function(_42){
if(typeof _42!=="string"){
throw new Error(this._cName+": Invalid attribute argument.");
}
},_containsValue:function(_43,_44,_45,_46){
return dojo.some(this.getValues(_43,_44),function(_47){
if(_47!==null&&!dojo.isObject(_47)&&_46){
if(_47.toString().match(_46)){
return true;
}
}else{
if(_45===_47){
return true;
}
}
return false;
});
}});
}
