/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.data.ItemFileReadStore"]){
dojo._hasResource["dojo.data.ItemFileReadStore"]=true;
dojo.provide("dojo.data.ItemFileReadStore");
dojo.require("dojo.data.util.filter");
dojo.require("dojo.data.util.simpleFetch");
dojo.require("dojo.date.stamp");
dojo.declare("dojo.data.ItemFileReadStore",null,{constructor:function(_1){
this._arrayOfAllItems=[];
this._arrayOfTopLevelItems=[];
this._loadFinished=false;
this._jsonFileUrl=_1.url;
this._jsonData=_1.data;
this._datatypeMap=_1.typeMap||{};
if(!this._datatypeMap["Date"]){
this._datatypeMap["Date"]={type:Date,deserialize:function(_2){
return dojo.date.stamp.fromISOString(_2);
}};
}
this._features={"dojo.data.api.Read":true,"dojo.data.api.Identity":true};
this._itemsByIdentity=null;
this._storeRefPropName="_S";
this._itemNumPropName="_0";
this._rootItemPropName="_RI";
this._reverseRefMap="_RRM";
this._loadInProgress=false;
this._queuedFetches=[];
if(_1.urlPreventCache!==undefined){
this.urlPreventCache=_1.urlPreventCache?true:false;
}
if(_1.clearOnClose){
this.clearOnClose=true;
}
},url:"",data:null,typeMap:null,clearOnClose:false,urlPreventCache:false,_assertIsItem:function(_3){
if(!this.isItem(_3)){
throw new Error("dojo.data.ItemFileReadStore: Invalid item argument.");
}
},_assertIsAttribute:function(_4){
if(typeof _4!=="string"){
throw new Error("dojo.data.ItemFileReadStore: Invalid attribute argument.");
}
},getValue:function(_5,_6,_7){
var _8=this.getValues(_5,_6);
return (_8.length>0)?_8[0]:_7;
},getValues:function(_9,_a){
this._assertIsItem(_9);
this._assertIsAttribute(_a);
return _9[_a]||[];
},getAttributes:function(_b){
this._assertIsItem(_b);
var _c=[];
for(var _d in _b){
if((_d!==this._storeRefPropName)&&(_d!==this._itemNumPropName)&&(_d!==this._rootItemPropName)&&(_d!==this._reverseRefMap)){
_c.push(_d);
}
}
return _c;
},hasAttribute:function(_e,_f){
return this.getValues(_e,_f).length>0;
},containsValue:function(_10,_11,_12){
var _13=undefined;
if(typeof _12==="string"){
_13=dojo.data.util.filter.patternToRegExp(_12,false);
}
return this._containsValue(_10,_11,_12,_13);
},_containsValue:function(_14,_15,_16,_17){
return dojo.some(this.getValues(_14,_15),function(_18){
if(_18!==null&&!dojo.isObject(_18)&&_17){
if(_18.toString().match(_17)){
return true;
}
}else{
if(_16===_18){
return true;
}
}
});
},isItem:function(_19){
if(_19&&_19[this._storeRefPropName]===this){
if(this._arrayOfAllItems[_19[this._itemNumPropName]]===_19){
return true;
}
}
return false;
},isItemLoaded:function(_1a){
return this.isItem(_1a);
},loadItem:function(_1b){
this._assertIsItem(_1b.item);
},getFeatures:function(){
return this._features;
},getLabel:function(_1c){
if(this._labelAttr&&this.isItem(_1c)){
return this.getValue(_1c,this._labelAttr);
}
return undefined;
},getLabelAttributes:function(_1d){
if(this._labelAttr){
return [this._labelAttr];
}
return null;
},_fetchItems:function(_1e,_1f,_20){
var _21=this;
var _22=function(_23,_24){
var _25=[];
if(_23.query){
var _26=_23.queryOptions?_23.queryOptions.ignoreCase:false;
var _27={};
for(var key in _23.query){
var _29=_23.query[key];
if(typeof _29==="string"){
_27[key]=dojo.data.util.filter.patternToRegExp(_29,_26);
}
}
for(var i=0;i<_24.length;++i){
var _2b=true;
var _2c=_24[i];
if(_2c===null){
_2b=false;
}else{
for(var key in _23.query){
var _29=_23.query[key];
if(!_21._containsValue(_2c,key,_29,_27[key])){
_2b=false;
}
}
}
if(_2b){
_25.push(_2c);
}
}
_1f(_25,_23);
}else{
for(var i=0;i<_24.length;++i){
var _2d=_24[i];
if(_2d!==null){
_25.push(_2d);
}
}
_1f(_25,_23);
}
};
if(this._loadFinished){
_22(_1e,this._getItemsArray(_1e.queryOptions));
}else{
if(this._jsonFileUrl){
if(this._loadInProgress){
this._queuedFetches.push({args:_1e,filter:_22});
}else{
this._loadInProgress=true;
var _2e={url:_21._jsonFileUrl,handleAs:"json-comment-optional",preventCache:this.urlPreventCache};
var _2f=dojo.xhrGet(_2e);
_2f.addCallback(function(_30){
try{
_21._getItemsFromLoadedData(_30);
_21._loadFinished=true;
_21._loadInProgress=false;
_22(_1e,_21._getItemsArray(_1e.queryOptions));
_21._handleQueuedFetches();
}
catch(e){
_21._loadFinished=true;
_21._loadInProgress=false;
_20(e,_1e);
}
});
_2f.addErrback(function(_31){
_21._loadInProgress=false;
_20(_31,_1e);
});
}
}else{
if(this._jsonData){
try{
this._loadFinished=true;
this._getItemsFromLoadedData(this._jsonData);
this._jsonData=null;
_22(_1e,this._getItemsArray(_1e.queryOptions));
}
catch(e){
_20(e,_1e);
}
}else{
_20(new Error("dojo.data.ItemFileReadStore: No JSON source data was provided as either URL or a nested Javascript object."),_1e);
}
}
}
},_handleQueuedFetches:function(){
if(this._queuedFetches.length>0){
for(var i=0;i<this._queuedFetches.length;i++){
var _33=this._queuedFetches[i];
var _34=_33.args;
var _35=_33.filter;
if(_35){
_35(_34,this._getItemsArray(_34.queryOptions));
}else{
this.fetchItemByIdentity(_34);
}
}
this._queuedFetches=[];
}
},_getItemsArray:function(_36){
if(_36&&_36.deep){
return this._arrayOfAllItems;
}
return this._arrayOfTopLevelItems;
},close:function(_37){
if(this.clearOnClose&&(this._jsonFileUrl!=="")){
this._arrayOfAllItems=[];
this._arrayOfTopLevelItems=[];
this._loadFinished=false;
this._itemsByIdentity=null;
this._loadInProgress=false;
this._queuedFetches=[];
}
},_getItemsFromLoadedData:function(_38){
var _39=false;
function valueIsAnItem(_3a){
var _3b=((_3a!=null)&&(typeof _3a=="object")&&(!dojo.isArray(_3a)||_39)&&(!dojo.isFunction(_3a))&&(_3a.constructor==Object||dojo.isArray(_3a))&&(typeof _3a._reference=="undefined")&&(typeof _3a._type=="undefined")&&(typeof _3a._value=="undefined"));
return _3b;
};
var _3c=this;
function addItemAndSubItemsToArrayOfAllItems(_3d){
_3c._arrayOfAllItems.push(_3d);
for(var _3e in _3d){
var _3f=_3d[_3e];
if(_3f){
if(dojo.isArray(_3f)){
var _40=_3f;
for(var k=0;k<_40.length;++k){
var _42=_40[k];
if(valueIsAnItem(_42)){
addItemAndSubItemsToArrayOfAllItems(_42);
}
}
}else{
if(valueIsAnItem(_3f)){
addItemAndSubItemsToArrayOfAllItems(_3f);
}
}
}
}
};
this._labelAttr=_38.label;
var i;
var _44;
this._arrayOfAllItems=[];
this._arrayOfTopLevelItems=_38.items;
for(i=0;i<this._arrayOfTopLevelItems.length;++i){
_44=this._arrayOfTopLevelItems[i];
if(dojo.isArray(_44)){
_39=true;
}
addItemAndSubItemsToArrayOfAllItems(_44);
_44[this._rootItemPropName]=true;
}
var _45={};
var key;
for(i=0;i<this._arrayOfAllItems.length;++i){
_44=this._arrayOfAllItems[i];
for(key in _44){
if(key!==this._rootItemPropName){
var _47=_44[key];
if(_47!==null){
if(!dojo.isArray(_47)){
_44[key]=[_47];
}
}else{
_44[key]=[null];
}
}
_45[key]=key;
}
}
while(_45[this._storeRefPropName]){
this._storeRefPropName+="_";
}
while(_45[this._itemNumPropName]){
this._itemNumPropName+="_";
}
while(_45[this._reverseRefMap]){
this._reverseRefMap+="_";
}
var _48;
var _49=_38.identifier;
if(_49){
this._itemsByIdentity={};
this._features["dojo.data.api.Identity"]=_49;
for(i=0;i<this._arrayOfAllItems.length;++i){
_44=this._arrayOfAllItems[i];
_48=_44[_49];
var _4a=_48[0];
if(!this._itemsByIdentity[_4a]){
this._itemsByIdentity[_4a]=_44;
}else{
if(this._jsonFileUrl){
throw new Error("dojo.data.ItemFileReadStore:  The json data as specified by: ["+this._jsonFileUrl+"] is malformed.  Items within the list have identifier: ["+_49+"].  Value collided: ["+_4a+"]");
}else{
if(this._jsonData){
throw new Error("dojo.data.ItemFileReadStore:  The json data provided by the creation arguments is malformed.  Items within the list have identifier: ["+_49+"].  Value collided: ["+_4a+"]");
}
}
}
}
}else{
this._features["dojo.data.api.Identity"]=Number;
}
for(i=0;i<this._arrayOfAllItems.length;++i){
_44=this._arrayOfAllItems[i];
_44[this._storeRefPropName]=this;
_44[this._itemNumPropName]=i;
}
for(i=0;i<this._arrayOfAllItems.length;++i){
_44=this._arrayOfAllItems[i];
for(key in _44){
_48=_44[key];
for(var j=0;j<_48.length;++j){
_47=_48[j];
if(_47!==null&&typeof _47=="object"){
if(_47._type&&_47._value){
var _4c=_47._type;
var _4d=this._datatypeMap[_4c];
if(!_4d){
throw new Error("dojo.data.ItemFileReadStore: in the typeMap constructor arg, no object class was specified for the datatype '"+_4c+"'");
}else{
if(dojo.isFunction(_4d)){
_48[j]=new _4d(_47._value);
}else{
if(dojo.isFunction(_4d.deserialize)){
_48[j]=_4d.deserialize(_47._value);
}else{
throw new Error("dojo.data.ItemFileReadStore: Value provided in typeMap was neither a constructor, nor a an object with a deserialize function");
}
}
}
}
if(_47._reference){
var _4e=_47._reference;
if(!dojo.isObject(_4e)){
_48[j]=this._itemsByIdentity[_4e];
}else{
for(var k=0;k<this._arrayOfAllItems.length;++k){
var _50=this._arrayOfAllItems[k];
var _51=true;
for(var _52 in _4e){
if(_50[_52]!=_4e[_52]){
_51=false;
}
}
if(_51){
_48[j]=_50;
}
}
}
if(this.referenceIntegrity){
var _53=_48[j];
if(this.isItem(_53)){
this._addReferenceToMap(_53,_44,key);
}
}
}else{
if(this.isItem(_47)){
if(this.referenceIntegrity){
this._addReferenceToMap(_47,_44,key);
}
}
}
}
}
}
}
},_addReferenceToMap:function(_54,_55,_56){
},getIdentity:function(_57){
var _58=this._features["dojo.data.api.Identity"];
if(_58===Number){
return _57[this._itemNumPropName];
}else{
var _59=_57[_58];
if(_59){
return _59[0];
}
}
return null;
},fetchItemByIdentity:function(_5a){
if(!this._loadFinished){
var _5b=this;
if(this._jsonFileUrl){
if(this._loadInProgress){
this._queuedFetches.push({args:_5a});
}else{
this._loadInProgress=true;
var _5c={url:_5b._jsonFileUrl,handleAs:"json-comment-optional",preventCache:this.urlPreventCache};
var _5d=dojo.xhrGet(_5c);
_5d.addCallback(function(_5e){
var _5f=_5a.scope?_5a.scope:dojo.global;
try{
_5b._getItemsFromLoadedData(_5e);
_5b._loadFinished=true;
_5b._loadInProgress=false;
var _60=_5b._getItemByIdentity(_5a.identity);
if(_5a.onItem){
_5a.onItem.call(_5f,_60);
}
_5b._handleQueuedFetches();
}
catch(error){
_5b._loadInProgress=false;
if(_5a.onError){
_5a.onError.call(_5f,error);
}
}
});
_5d.addErrback(function(_61){
_5b._loadInProgress=false;
if(_5a.onError){
var _62=_5a.scope?_5a.scope:dojo.global;
_5a.onError.call(_62,_61);
}
});
}
}else{
if(this._jsonData){
_5b._getItemsFromLoadedData(_5b._jsonData);
_5b._jsonData=null;
_5b._loadFinished=true;
var _63=_5b._getItemByIdentity(_5a.identity);
if(_5a.onItem){
var _64=_5a.scope?_5a.scope:dojo.global;
_5a.onItem.call(_64,_63);
}
}
}
}else{
var _63=this._getItemByIdentity(_5a.identity);
if(_5a.onItem){
var _64=_5a.scope?_5a.scope:dojo.global;
_5a.onItem.call(_64,_63);
}
}
},_getItemByIdentity:function(_65){
var _66=null;
if(this._itemsByIdentity){
_66=this._itemsByIdentity[_65];
}else{
_66=this._arrayOfAllItems[_65];
}
if(_66===undefined){
_66=null;
}
return _66;
},getIdentityAttributes:function(_67){
var _68=this._features["dojo.data.api.Identity"];
if(_68===Number){
return null;
}else{
return [_68];
}
},_forceLoad:function(){
var _69=this;
if(this._jsonFileUrl){
var _6a={url:_69._jsonFileUrl,handleAs:"json-comment-optional",preventCache:this.urlPreventCache,sync:true};
var _6b=dojo.xhrGet(_6a);
_6b.addCallback(function(_6c){
try{
if(_69._loadInProgress!==true&&!_69._loadFinished){
_69._getItemsFromLoadedData(_6c);
_69._loadFinished=true;
}else{
if(_69._loadInProgress){
throw new Error("dojo.data.ItemFileReadStore:  Unable to perform a synchronous load, an async load is in progress.");
}
}
}
catch(e){

throw e;
}
});
_6b.addErrback(function(_6d){
throw _6d;
});
}else{
if(this._jsonData){
_69._getItemsFromLoadedData(_69._jsonData);
_69._jsonData=null;
_69._loadFinished=true;
}
}
}});
dojo.extend(dojo.data.ItemFileReadStore,dojo.data.util.simpleFetch);
}
