/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.CsvStore"]){
dojo._hasResource["dojox.data.CsvStore"]=true;
dojo.provide("dojox.data.CsvStore");
dojo.require("dojo.data.util.filter");
dojo.require("dojo.data.util.simpleFetch");
dojo.declare("dojox.data.CsvStore",null,{constructor:function(_1){
this._attributes=[];
this._attributeIndexes={};
this._dataArray=[];
this._arrayOfAllItems=[];
this._loadFinished=false;
if(_1.url){
this.url=_1.url;
}
this._csvData=_1.data;
if(_1.label){
this.label=_1.label;
}else{
if(this.label===""){
this.label=undefined;
}
}
this._storeProp="_csvStore";
this._idProp="_csvId";
this._features={"dojo.data.api.Read":true,"dojo.data.api.Identity":true};
this._loadInProgress=false;
this._queuedFetches=[];
},url:"",label:"",_assertIsItem:function(_2){
if(!this.isItem(_2)){
throw new Error("dojox.data.CsvStore: a function was passed an item argument that was not an item");
}
},_assertIsAttribute:function(_3){
if(!dojo.isString(_3)){
throw new Error("dojox.data.CsvStore: a function was passed an attribute argument that was not an attribute object nor an attribute name string");
}
},getValue:function(_4,_5,_6){
this._assertIsItem(_4);
this._assertIsAttribute(_5);
var _7=_6;
if(this.hasAttribute(_4,_5)){
var _8=this._dataArray[this.getIdentity(_4)];
_7=_8[this._attributeIndexes[_5]];
}
return _7;
},getValues:function(_9,_a){
var _b=this.getValue(_9,_a);
return (_b?[_b]:[]);
},getAttributes:function(_c){
this._assertIsItem(_c);
var _d=[];
var _e=this._dataArray[this.getIdentity(_c)];
for(var i=0;i<_e.length;i++){
if(_e[i]!==""){
_d.push(this._attributes[i]);
}
}
return _d;
},hasAttribute:function(_10,_11){
this._assertIsItem(_10);
this._assertIsAttribute(_11);
var _12=this._attributeIndexes[_11];
var _13=this._dataArray[this.getIdentity(_10)];
return (typeof _12!=="undefined"&&_12<_13.length&&_13[_12]!=="");
},containsValue:function(_14,_15,_16){
var _17=undefined;
if(typeof _16==="string"){
_17=dojo.data.util.filter.patternToRegExp(_16,false);
}
return this._containsValue(_14,_15,_16,_17);
},_containsValue:function(_18,_19,_1a,_1b){
var _1c=this.getValues(_18,_19);
for(var i=0;i<_1c.length;++i){
var _1e=_1c[i];
if(typeof _1e==="string"&&_1b){
return (_1e.match(_1b)!==null);
}else{
if(_1a===_1e){
return true;
}
}
}
return false;
},isItem:function(_1f){
if(_1f&&_1f[this._storeProp]===this){
var _20=_1f[this._idProp];
if(_20>=0&&_20<this._dataArray.length){
return true;
}
}
return false;
},isItemLoaded:function(_21){
return this.isItem(_21);
},loadItem:function(_22){
},getFeatures:function(){
return this._features;
},getLabel:function(_23){
if(this.label&&this.isItem(_23)){
return this.getValue(_23,this.label);
}
return undefined;
},getLabelAttributes:function(_24){
if(this.label){
return [this.label];
}
return null;
},_fetchItems:function(_25,_26,_27){
var _28=this;
var _29=function(_2a,_2b){
var _2c=null;
if(_2a.query){
_2c=[];
var _2d=_2a.queryOptions?_2a.queryOptions.ignoreCase:false;
var _2e={};
for(var key in _2a.query){
var _30=_2a.query[key];
if(typeof _30==="string"){
_2e[key]=dojo.data.util.filter.patternToRegExp(_30,_2d);
}
}
for(var i=0;i<_2b.length;++i){
var _32=true;
var _33=_2b[i];
for(var key in _2a.query){
var _30=_2a.query[key];
if(!_28._containsValue(_33,key,_30,_2e[key])){
_32=false;
}
}
if(_32){
_2c.push(_33);
}
}
}else{
if(_2b.length>0){
_2c=_2b.slice(0,_2b.length);
}
}
_26(_2c,_2a);
};
if(this._loadFinished){
_29(_25,this._arrayOfAllItems);
}else{
if(this.url!==""){
if(this._loadInProgress){
this._queuedFetches.push({args:_25,filter:_29});
}else{
this._loadInProgress=true;
var _34={url:_28.url,handleAs:"text"};
var _35=dojo.xhrGet(_34);
_35.addCallback(function(_36){
_28._processData(_36);
_29(_25,_28._arrayOfAllItems);
_28._handleQueuedFetches();
});
_35.addErrback(function(_37){
_28._loadInProgress=false;
if(_27){
_27(_37,_25);
}else{
throw _37;
}
});
}
}else{
if(this._csvData){
this._processData(this._csvData);
this._csvData=null;
_29(_25,this._arrayOfAllItems);
}else{
var _38=new Error("dojox.data.CsvStore: No CSV source data was provided as either URL or String data input.");
if(_27){
_27(_38,_25);
}else{
throw _38;
}
}
}
}
},close:function(_39){
},_getArrayOfArraysFromCsvFileContents:function(_3a){
if(dojo.isString(_3a)){
var _3b=new RegExp("\r\n|\n|\r");
var _3c=new RegExp("^\\s+","g");
var _3d=new RegExp("\\s+$","g");
var _3e=new RegExp("\"\"","g");
var _3f=[];
var _40=this._splitLines(_3a);
for(var i=0;i<_40.length;++i){
var _42=_40[i];
if(_42.length>0){
var _43=_42.split(",");
var j=0;
while(j<_43.length){
var _45=_43[j];
var _46=_45.replace(_3c,"");
var _47=_46.replace(_3d,"");
var _48=_47.charAt(0);
var _49=_47.charAt(_47.length-1);
var _4a=_47.charAt(_47.length-2);
var _4b=_47.charAt(_47.length-3);
if(_47.length===2&&_47=="\"\""){
_43[j]="";
}else{
if((_48=="\"")&&((_49!="\"")||((_49=="\"")&&(_4a=="\"")&&(_4b!="\"")))){
if(j+1===_43.length){
return null;
}
var _4c=_43[j+1];
_43[j]=_46+","+_4c;
_43.splice(j+1,1);
}else{
if((_48=="\"")&&(_49=="\"")){
_47=_47.slice(1,(_47.length-1));
_47=_47.replace(_3e,"\"");
}
_43[j]=_47;
j+=1;
}
}
}
_3f.push(_43);
}
}
this._attributes=_3f.shift();
for(var i=0;i<this._attributes.length;i++){
this._attributeIndexes[this._attributes[i]]=i;
}
this._dataArray=_3f;
}
},_splitLines:function(_4d){
var _4e=[];
var i;
var _50="";
var _51=false;
for(i=0;i<_4d.length;i++){
var c=_4d.charAt(i);
switch(c){
case "\"":
_51=!_51;
_50+=c;
break;
case "\r":
if(_51){
_50+=c;
}else{
_4e.push(_50);
_50="";
if(i<(_4d.length-1)&&_4d.charAt(i+1)=="\n"){
i++;
}
}
break;
case "\n":
if(_51){
_50+=c;
}else{
_4e.push(_50);
_50="";
}
break;
default:
_50+=c;
}
}
if(_50!==""){
_4e.push(_50);
}
return _4e;
},_processData:function(_53){
this._getArrayOfArraysFromCsvFileContents(_53);
this._arrayOfAllItems=[];
for(var i=0;i<this._dataArray.length;i++){
this._arrayOfAllItems.push(this._createItemFromIdentity(i));
}
this._loadFinished=true;
this._loadInProgress=false;
},_createItemFromIdentity:function(_55){
var _56={};
_56[this._storeProp]=this;
_56[this._idProp]=_55;
return _56;
},getIdentity:function(_57){
if(this.isItem(_57)){
return _57[this._idProp];
}
return null;
},fetchItemByIdentity:function(_58){
if(!this._loadFinished){
var _59=this;
if(this.url!==""){
if(this._loadInProgress){
this._queuedFetches.push({args:_58});
}else{
this._loadInProgress=true;
var _5a={url:_59.url,handleAs:"text"};
var _5b=dojo.xhrGet(_5a);
_5b.addCallback(function(_5c){
var _5d=_58.scope?_58.scope:dojo.global;
try{
_59._processData(_5c);
var _5e=_59._createItemFromIdentity(_58.identity);
if(!_59.isItem(_5e)){
_5e=null;
}
if(_58.onItem){
_58.onItem.call(_5d,_5e);
}
_59._handleQueuedFetches();
}
catch(error){
if(_58.onError){
_58.onError.call(_5d,error);
}
}
});
_5b.addErrback(function(_5f){
this._loadInProgress=false;
if(_58.onError){
var _60=_58.scope?_58.scope:dojo.global;
_58.onError.call(_60,_5f);
}
});
}
}else{
if(this._csvData){
_59._processData(_59._csvData);
_59._csvData=null;
var _61=_59._createItemFromIdentity(_58.identity);
if(!_59.isItem(_61)){
_61=null;
}
if(_58.onItem){
var _62=_58.scope?_58.scope:dojo.global;
_58.onItem.call(_62,_61);
}
}
}
}else{
var _61=this._createItemFromIdentity(_58.identity);
if(!this.isItem(_61)){
_61=null;
}
if(_58.onItem){
var _62=_58.scope?_58.scope:dojo.global;
_58.onItem.call(_62,_61);
}
}
},getIdentityAttributes:function(_63){
return null;
},_handleQueuedFetches:function(){
if(this._queuedFetches.length>0){
for(var i=0;i<this._queuedFetches.length;i++){
var _65=this._queuedFetches[i];
var _66=_65.filter;
var _67=_65.args;
if(_66){
_66(_67,this._arrayOfAllItems);
}else{
this.fetchItemByIdentity(_65.args);
}
}
this._queuedFetches=[];
}
}});
dojo.extend(dojox.data.CsvStore,dojo.data.util.simpleFetch);
}
