/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.AndOrReadStore"]){
dojo._hasResource["dojox.data.AndOrReadStore"]=true;
dojo.provide("dojox.data.AndOrReadStore");
dojo.require("dojo.data.util.filter");
dojo.require("dojo.data.util.simpleFetch");
dojo.require("dojo.date.stamp");
dojo.declare("dojox.data.AndOrReadStore",null,{constructor:function(_1){
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
throw new Error("dojox.data.AndOrReadStore: Invalid item argument.");
}
},_assertIsAttribute:function(_4){
if(typeof _4!=="string"){
throw new Error("dojox.data.AndOrReadStore: Invalid attribute argument.");
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
var _27=_23.query;
if(typeof _27!="string"){
_27=dojo.toJson(_27);
_27=_27.replace(/\\\\/g,"\\");
}
_27=_27.replace(/\\"/g,"\"");
var _28=dojo.trim(_27.replace(/{|}/g,""));
var _29,i;
if(_28.match(/"? *complexQuery *"?:/)){
_28=dojo.trim(_28.replace(/"?\s*complexQuery\s*"?:/,""));
var _2b=["'","\""];
var _2c,_2d;
var _2e=false;
for(i=0;i<_2b.length;i++){
_2c=_28.indexOf(_2b[i]);
_29=_28.indexOf(_2b[i],1);
_2d=_28.indexOf(":",1);
if(_2c===0&&_29!=-1&&_2d<_29){
_2e=true;
break;
}
}
if(_2e){
_28=_28.replace(/^\"|^\'|\"$|\'$/g,"");
}
}
var _2f=_28;
var _30=/^,|^NOT |^AND |^OR |^\(|^\)|^!|^&&|^\|\|/i;
var _31="";
var op="";
var val="";
var pos=-1;
var err=false;
var key="";
var _37="";
var tok="";
_29=-1;
for(i=0;i<_24.length;++i){
var _39=true;
var _3a=_24[i];
if(_3a===null){
_39=false;
}else{
_28=_2f;
_31="";
while(_28.length>0&&!err){
op=_28.match(_30);
while(op&&!err){
_28=dojo.trim(_28.replace(op[0],""));
op=dojo.trim(op[0]).toUpperCase();
op=op=="NOT"?"!":op=="AND"||op==","?"&&":op=="OR"?"||":op;
op=" "+op+" ";
_31+=op;
op=_28.match(_30);
}
if(_28.length>0){
pos=_28.indexOf(":");
if(pos==-1){
err=true;
break;
}else{
key=dojo.trim(_28.substring(0,pos).replace(/\"|\'/g,""));
_28=dojo.trim(_28.substring(pos+1));
tok=_28.match(/^\'|^\"/);
if(tok){
tok=tok[0];
pos=_28.indexOf(tok);
_29=_28.indexOf(tok,pos+1);
if(_29==-1){
err=true;
break;
}
_37=_28.substring(pos+1,_29);
if(_29==_28.length-1){
_28="";
}else{
_28=dojo.trim(_28.substring(_29+1));
}
_31+=_21._containsValue(_3a,key,_37,dojo.data.util.filter.patternToRegExp(_37,_26));
}else{
tok=_28.match(/\s|\)|,/);
if(tok){
var _3b=new Array(tok.length);
for(var j=0;j<tok.length;j++){
_3b[j]=_28.indexOf(tok[j]);
}
pos=_3b[0];
if(_3b.length>1){
for(var j=1;j<_3b.length;j++){
pos=Math.min(pos,_3b[j]);
}
}
_37=dojo.trim(_28.substring(0,pos));
_28=dojo.trim(_28.substring(pos));
}else{
_37=dojo.trim(_28);
_28="";
}
_31+=_21._containsValue(_3a,key,_37,dojo.data.util.filter.patternToRegExp(_37,_26));
}
}
}
}
_39=eval(_31);
}
if(_39){
_25.push(_3a);
}
}
if(err){
_25=[];

}
_1f(_25,_23);
}else{
for(var i=0;i<_24.length;++i){
var _3d=_24[i];
if(_3d!==null){
_25.push(_3d);
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
var _3e={url:_21._jsonFileUrl,handleAs:"json-comment-optional",preventCache:this.urlPreventCache};
var _3f=dojo.xhrGet(_3e);
_3f.addCallback(function(_40){
try{
_21._getItemsFromLoadedData(_40);
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
_3f.addErrback(function(_41){
_21._loadInProgress=false;
_20(_41,_1e);
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
_20(new Error("dojox.data.AndOrReadStore: No JSON source data was provided as either URL or a nested Javascript object."),_1e);
}
}
}
},_handleQueuedFetches:function(){
if(this._queuedFetches.length>0){
for(var i=0;i<this._queuedFetches.length;i++){
var _43=this._queuedFetches[i];
var _44=_43.args;
var _45=_43.filter;
if(_45){
_45(_44,this._getItemsArray(_44.queryOptions));
}else{
this.fetchItemByIdentity(_44);
}
}
this._queuedFetches=[];
}
},_getItemsArray:function(_46){
if(_46&&_46.deep){
return this._arrayOfAllItems;
}
return this._arrayOfTopLevelItems;
},close:function(_47){
if(this.clearOnClose&&(this._jsonFileUrl!=="")){
this._arrayOfAllItems=[];
this._arrayOfTopLevelItems=[];
this._loadFinished=false;
this._itemsByIdentity=null;
this._loadInProgress=false;
this._queuedFetches=[];
}
},_getItemsFromLoadedData:function(_48){
function valueIsAnItem(_49){
var _4a=((_49!==null)&&(typeof _49==="object")&&(!dojo.isArray(_49))&&(!dojo.isFunction(_49))&&(_49.constructor==Object)&&(typeof _49._reference==="undefined")&&(typeof _49._type==="undefined")&&(typeof _49._value==="undefined"));
return _4a;
};
var _4b=this;
function addItemAndSubItemsToArrayOfAllItems(_4c){
_4b._arrayOfAllItems.push(_4c);
for(var _4d in _4c){
var _4e=_4c[_4d];
if(_4e){
if(dojo.isArray(_4e)){
var _4f=_4e;
for(var k=0;k<_4f.length;++k){
var _51=_4f[k];
if(valueIsAnItem(_51)){
addItemAndSubItemsToArrayOfAllItems(_51);
}
}
}else{
if(valueIsAnItem(_4e)){
addItemAndSubItemsToArrayOfAllItems(_4e);
}
}
}
}
};
this._labelAttr=_48.label;
var i;
var _53;
this._arrayOfAllItems=[];
this._arrayOfTopLevelItems=_48.items;
for(i=0;i<this._arrayOfTopLevelItems.length;++i){
_53=this._arrayOfTopLevelItems[i];
addItemAndSubItemsToArrayOfAllItems(_53);
_53[this._rootItemPropName]=true;
}
var _54={};
var key;
for(i=0;i<this._arrayOfAllItems.length;++i){
_53=this._arrayOfAllItems[i];
for(key in _53){
if(key!==this._rootItemPropName){
var _56=_53[key];
if(_56!==null){
if(!dojo.isArray(_56)){
_53[key]=[_56];
}
}else{
_53[key]=[null];
}
}
_54[key]=key;
}
}
while(_54[this._storeRefPropName]){
this._storeRefPropName+="_";
}
while(_54[this._itemNumPropName]){
this._itemNumPropName+="_";
}
while(_54[this._reverseRefMap]){
this._reverseRefMap+="_";
}
var _57;
var _58=_48.identifier;
if(_58){
this._itemsByIdentity={};
this._features["dojo.data.api.Identity"]=_58;
for(i=0;i<this._arrayOfAllItems.length;++i){
_53=this._arrayOfAllItems[i];
_57=_53[_58];
var _59=_57[0];
if(!this._itemsByIdentity[_59]){
this._itemsByIdentity[_59]=_53;
}else{
if(this._jsonFileUrl){
throw new Error("dojox.data.AndOrReadStore:  The json data as specified by: ["+this._jsonFileUrl+"] is malformed.  Items within the list have identifier: ["+_58+"].  Value collided: ["+_59+"]");
}else{
if(this._jsonData){
throw new Error("dojox.data.AndOrReadStore:  The json data provided by the creation arguments is malformed.  Items within the list have identifier: ["+_58+"].  Value collided: ["+_59+"]");
}
}
}
}
}else{
this._features["dojo.data.api.Identity"]=Number;
}
for(i=0;i<this._arrayOfAllItems.length;++i){
_53=this._arrayOfAllItems[i];
_53[this._storeRefPropName]=this;
_53[this._itemNumPropName]=i;
}
for(i=0;i<this._arrayOfAllItems.length;++i){
_53=this._arrayOfAllItems[i];
for(key in _53){
_57=_53[key];
for(var j=0;j<_57.length;++j){
_56=_57[j];
if(_56!==null&&typeof _56=="object"){
if(_56._type&&_56._value){
var _5b=_56._type;
var _5c=this._datatypeMap[_5b];
if(!_5c){
throw new Error("dojox.data.AndOrReadStore: in the typeMap constructor arg, no object class was specified for the datatype '"+_5b+"'");
}else{
if(dojo.isFunction(_5c)){
_57[j]=new _5c(_56._value);
}else{
if(dojo.isFunction(_5c.deserialize)){
_57[j]=_5c.deserialize(_56._value);
}else{
throw new Error("dojox.data.AndOrReadStore: Value provided in typeMap was neither a constructor, nor a an object with a deserialize function");
}
}
}
}
if(_56._reference){
var _5d=_56._reference;
if(!dojo.isObject(_5d)){
_57[j]=this._itemsByIdentity[_5d];
}else{
for(var k=0;k<this._arrayOfAllItems.length;++k){
var _5f=this._arrayOfAllItems[k];
var _60=true;
for(var _61 in _5d){
if(_5f[_61]!=_5d[_61]){
_60=false;
}
}
if(_60){
_57[j]=_5f;
}
}
}
if(this.referenceIntegrity){
var _62=_57[j];
if(this.isItem(_62)){
this._addReferenceToMap(_62,_53,key);
}
}
}else{
if(this.isItem(_56)){
if(this.referenceIntegrity){
this._addReferenceToMap(_56,_53,key);
}
}
}
}
}
}
}
},_addReferenceToMap:function(_63,_64,_65){
},getIdentity:function(_66){
var _67=this._features["dojo.data.api.Identity"];
if(_67===Number){
return _66[this._itemNumPropName];
}else{
var _68=_66[_67];
if(_68){
return _68[0];
}
}
return null;
},fetchItemByIdentity:function(_69){
if(!this._loadFinished){
var _6a=this;
if(this._jsonFileUrl){
if(this._loadInProgress){
this._queuedFetches.push({args:_69});
}else{
this._loadInProgress=true;
var _6b={url:_6a._jsonFileUrl,handleAs:"json-comment-optional",preventCache:this.urlPreventCache};
var _6c=dojo.xhrGet(_6b);
_6c.addCallback(function(_6d){
var _6e=_69.scope?_69.scope:dojo.global;
try{
_6a._getItemsFromLoadedData(_6d);
_6a._loadFinished=true;
_6a._loadInProgress=false;
var _6f=_6a._getItemByIdentity(_69.identity);
if(_69.onItem){
_69.onItem.call(_6e,_6f);
}
_6a._handleQueuedFetches();
}
catch(error){
_6a._loadInProgress=false;
if(_69.onError){
_69.onError.call(_6e,error);
}
}
});
_6c.addErrback(function(_70){
_6a._loadInProgress=false;
if(_69.onError){
var _71=_69.scope?_69.scope:dojo.global;
_69.onError.call(_71,_70);
}
});
}
}else{
if(this._jsonData){
_6a._getItemsFromLoadedData(_6a._jsonData);
_6a._jsonData=null;
_6a._loadFinished=true;
var _72=_6a._getItemByIdentity(_69.identity);
if(_69.onItem){
var _73=_69.scope?_69.scope:dojo.global;
_69.onItem.call(_73,_72);
}
}
}
}else{
var _72=this._getItemByIdentity(_69.identity);
if(_69.onItem){
var _73=_69.scope?_69.scope:dojo.global;
_69.onItem.call(_73,_72);
}
}
},_getItemByIdentity:function(_74){
var _75=null;
if(this._itemsByIdentity){
_75=this._itemsByIdentity[_74];
}else{
_75=this._arrayOfAllItems[_74];
}
if(_75===undefined){
_75=null;
}
return _75;
},getIdentityAttributes:function(_76){
var _77=this._features["dojo.data.api.Identity"];
if(_77===Number){
return null;
}else{
return [_77];
}
},_forceLoad:function(){
var _78=this;
if(this._jsonFileUrl){
var _79={url:_78._jsonFileUrl,handleAs:"json-comment-optional",preventCache:this.urlPreventCache,sync:true};
var _7a=dojo.xhrGet(_79);
_7a.addCallback(function(_7b){
try{
if(_78._loadInProgress!==true&&!_78._loadFinished){
_78._getItemsFromLoadedData(_7b);
_78._loadFinished=true;
}else{
if(_78._loadInProgress){
throw new Error("dojox.data.AndOrReadStore:  Unable to perform a synchronous load, an async load is in progress.");
}
}
}
catch(e){

throw e;
}
});
_7a.addErrback(function(_7c){
throw _7c;
});
}else{
if(this._jsonData){
_78._getItemsFromLoadedData(_78._jsonData);
_78._jsonData=null;
_78._loadFinished=true;
}
}
}});
dojo.extend(dojox.data.AndOrReadStore,dojo.data.util.simpleFetch);
}
