/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.jsonPathStore"]){
dojo._hasResource["dojox.data.jsonPathStore"]=true;
dojo.provide("dojox.data.jsonPathStore");
dojo.require("dojox.jsonPath");
dojo.require("dojo.date");
dojo.require("dojo.date.locale");
dojo.require("dojo.date.stamp");
dojox.data.ASYNC_MODE=0;
dojox.data.SYNC_MODE=1;
dojo.declare("dojox.data.jsonPathStore",null,{mode:dojox.data.ASYNC_MODE,metaLabel:"_meta",hideMetaAttributes:false,autoIdPrefix:"_auto_",autoIdentity:true,idAttribute:"_id",indexOnLoad:true,labelAttribute:"",url:"",_replaceRegex:/\'\]/gi,constructor:function(_1){
this.byId=this.fetchItemByIdentity;
if(_1){
dojo.mixin(this,_1);
}
this._dirtyItems=[];
this._autoId=0;
this._referenceId=0;
this._references={};
this._fetchQueue=[];
this.index={};
var _2="("+this.metaLabel+"'])";
this.metaRegex=new RegExp(_2);
if(!this.data&&!this.url){
this.setData({});
}
if(this.data&&!this.url){
this.setData(this.data);
delete this.data;
}
if(this.url){
dojo.xhrGet({url:_1.url,handleAs:"json",load:dojo.hitch(this,"setData"),sync:this.mode});
}
},_loadData:function(_3){
if(this._data){
delete this._data;
}
if(dojo.isString(_3)){
this._data=dojo.fromJson(_3);
}else{
this._data=_3;
}
if(this.indexOnLoad){
this.buildIndex();
}
this._updateMeta(this._data,{path:"$"});
this.onLoadData(this._data);
},onLoadData:function(_4){
while(this._fetchQueue.length>0){
var _5=this._fetchQueue.shift();
this.fetch(_5);
}
},setData:function(_6){
this._loadData(_6);
},buildIndex:function(_7,_8){
if(!this.idAttribute){
throw new Error("buildIndex requires idAttribute for the store");
}
_8=_8||this._data;
var _9=_7;
_7=_7||"$";
_7+="[*]";
var _a=this.fetch({query:_7,mode:dojox.data.SYNC_MODE});
for(var i=0;i<_a.length;i++){
var _c,_d;
if(dojo.isObject(_a[i])){
var _e=_a[i][this.metaLabel]["path"];
if(_9){
_c=_9.split("['");
_d=_c[_c.length-1].replace(this._replaceRegex,"");
if(!dojo.isArray(_a[i])){
this._addReference(_a[i],{parent:_8,attribute:_d});
this.buildIndex(_e,_a[i]);
}else{
this.buildIndex(_e,_8);
}
}else{
_c=_e.split("['");
_d=_c[_c.length-1].replace(this._replaceRegex,"");
this._addReference(_a[i],{parent:this._data,attribute:_d});
this.buildIndex(_e,_a[i]);
}
}
}
},_correctReference:function(_f){
if(this.index[_f[this.idAttribute]][this.metaLabel]===_f[this.metaLabel]){
return this.index[_f[this.idAttribute]];
}
return _f;
},getValue:function(_10,_11){
_10=this._correctReference(_10);
return _10[_11];
},getValues:function(_12,_13){
_12=this._correctReference(_12);
return dojo.isArray(_12[_13])?_12[_13]:[_12[_13]];
},getAttributes:function(_14){
_14=this._correctReference(_14);
var res=[];
for(var i in _14){
if(this.hideMetaAttributes&&(i==this.metaLabel)){
continue;
}
res.push(i);
}
return res;
},hasAttribute:function(_17,_18){
_17=this._correctReference(_17);
if(_18 in _17){
return true;
}
return false;
},containsValue:function(_19,_1a,_1b){
_19=this._correctReference(_19);
if(_19[_1a]&&_19[_1a]==_1b){
return true;
}
if(dojo.isObject(_19[_1a])||dojo.isObject(_1b)){
if(this._shallowCompare(_19[_1a],_1b)){
return true;
}
}
return false;
},_shallowCompare:function(a,b){
if((dojo.isObject(a)&&!dojo.isObject(b))||(dojo.isObject(b)&&!dojo.isObject(a))){
return false;
}
if(a["getFullYear"]||b["getFullYear"]){
if((a["getFullYear"]&&!b["getFullYear"])||(b["getFullYear"]&&!a["getFullYear"])){
return false;
}else{
if(!dojo.date.compare(a,b)){
return true;
}
return false;
}
}
for(var i in b){
if(dojo.isObject(b[i])){
if(!a[i]||!dojo.isObject(a[i])){
return false;
}
if(b[i]["getFullYear"]){
if(!a[i]["getFullYear"]){
return false;
}
if(dojo.date.compare(a,b)){
return false;
}
}else{
if(!this._shallowCompare(a[i],b[i])){
return false;
}
}
}else{
if(!b[i]||(a[i]!=b[i])){
return false;
}
}
}
for(var i in a){
if(!b[i]){
return false;
}
}
return true;
},isItem:function(_1f){
if(!dojo.isObject(_1f)||!_1f[this.metaLabel]){
return false;
}
if(this.requireId&&this._hasId&&!_1f[this._id]){
return false;
}
return true;
},isItemLoaded:function(_20){
_20=this._correctReference(_20);
return this.isItem(_20);
},loadItem:function(_21){
return true;
},_updateMeta:function(_22,_23){
if(_22&&_22[this.metaLabel]){
dojo.mixin(_22[this.metaLabel],_23);
return;
}
_22[this.metaLabel]=_23;
},cleanMeta:function(_24,_25){
_24=_24||this._data;
if(_24[this.metaLabel]){
if(_24[this.metaLabel]["autoId"]){
delete _24[this.idAttribute];
}
delete _24[this.metaLabel];
}
if(dojo.isArray(_24)){
for(var i=0;i<_24.length;i++){
if(dojo.isObject(_24[i])||dojo.isArray(_24[i])){
this.cleanMeta(_24[i]);
}
}
}else{
if(dojo.isObject(_24)){
for(var i in _24){
this.cleanMeta(_24[i]);
}
}
}
},fetch:function(_27){
if(!this._data){
this._fetchQueue.push(_27);
return _27;
}
if(dojo.isString(_27)){
_28=_27;
_27={query:_28,mode:dojox.data.SYNC_MODE};
}
var _28;
if(!_27||!_27.query){
if(!_27){
var _27={};
}
if(!_27.query){
_27.query="$..*";
_28=_27.query;
}
}
if(dojo.isObject(_27.query)){
if(_27.query.query){
_28=_27.query.query;
}else{
_28=_27.query="$..*";
}
if(_27.query.queryOptions){
_27.queryOptions=_27.query.queryOptions;
}
}else{
_28=_27.query;
}
if(!_27.mode){
_27.mode=this.mode;
}
if(!_27.queryOptions){
_27.queryOptions={};
}
_27.queryOptions.resultType="BOTH";
var _29=dojox.jsonPath.query(this._data,_28,_27.queryOptions);
var tmp=[];
var _2b=0;
for(var i=0;i<_29.length;i++){
if(_27.start&&i<_27.start){
continue;
}
if(_27.count&&(_2b>=_27.count)){
continue;
}
var _2d=_29[i]["value"];
var _2e=_29[i]["path"];
if(!dojo.isObject(_2d)){
continue;
}
if(this.metaRegex.exec(_2e)){
continue;
}
this._updateMeta(_2d,{path:_29[i].path});
if(this.autoIdentity&&!_2d[this.idAttribute]){
var _2f=this.autoIdPrefix+this._autoId++;
_2d[this.idAttribute]=_2f;
_2d[this.metaLabel]["autoId"]=true;
}
if(_2d[this.idAttribute]){
this.index[_2d[this.idAttribute]]=_2d;
}
_2b++;
tmp.push(_2d);
}
_29=tmp;
var _30=_27.scope||dojo.global;
if("sort" in _27){

}
if(_27.mode==dojox.data.SYNC_MODE){
return _29;
}
if(_27.onBegin){
_27["onBegin"].call(_30,_29.length,_27);
}
if(_27.onItem){
for(var i=0;i<_29.length;i++){
_27["onItem"].call(_30,_29[i],_27);
}
}
if(_27.onComplete){
_27["onComplete"].call(_30,_29,_27);
}
return _27;
},dump:function(_31){
var _31=_31||{};
var d=_31.data||this._data;
if(!_31.suppressExportMeta&&_31.clone){
_33=dojo.clone(d);
if(_33[this.metaLabel]){
_33[this.metaLabel]["clone"]=true;
}
}else{
var _33=d;
}
if(!_31.suppressExportMeta&&_33[this.metaLabel]){
_33[this.metaLabel]["last_export"]=new Date().toString();
}
if(_31.cleanMeta){
this.cleanMeta(_33);
}
switch(_31.type){
case "raw":
return _33;
case "json":
default:
return dojo.toJson(_33,_31.pretty||false);
}
},getFeatures:function(){
return {"dojo.data.api.Read":true,"dojo.data.api.Identity":true,"dojo.data.api.Write":true,"dojo.data.api.Notification":true};
},getLabel:function(_34){
_34=this._correctReference(_34);
var _35="";
if(dojo.isFunction(this.createLabel)){
return this.createLabel(_34);
}
if(this.labelAttribute){
if(dojo.isArray(this.labelAttribute)){
for(var i=0;i<this.labelAttribute.length;i++){
if(i>0){
_35+=" ";
}
_35+=_34[this.labelAttribute[i]];
}
return _35;
}else{
return _34[this.labelAttribute];
}
}
return _34.toString();
},getLabelAttributes:function(_37){
_37=this._correctReference(_37);
return dojo.isArray(this.labelAttribute)?this.labelAttribute:[this.labelAttribute];
},sort:function(a,b){

},getIdentity:function(_3a){
if(this.isItem(_3a)){
return _3a[this.idAttribute];
}
throw new Error("Id not found for item");
},getIdentityAttributes:function(_3b){
return [this.idAttribute];
},fetchItemByIdentity:function(_3c){
var id;
if(dojo.isString(_3c)){
id=_3c;
_3c={identity:id,mode:dojox.data.SYNC_MODE};
}else{
if(_3c){
id=_3c["identity"];
}
if(!_3c.mode){
_3c.mode=this.mode;
}
}
if(this.index&&(this.index[id]||this.index["identity"])){
if(_3c.mode==dojox.data.SYNC_MODE){
return this.index[id];
}
if(_3c.onItem){
_3c["onItem"].call(_3c.scope||dojo.global,this.index[id],_3c);
}
return _3c;
}else{
if(_3c.mode==dojox.data.SYNC_MODE){
return false;
}
}
if(_3c.onError){
_3c["onItem"].call(_3c.scope||dojo.global,new Error("Item Not Found: "+id),_3c);
}
return _3c;
},newItem:function(_3e,_3f){
var _40={};
var _41={item:this._data};
if(_3f){
if(_3f.parent){
_3f.item=_3f.parent;
}
dojo.mixin(_41,_3f);
}
if(this.idAttribute&&!_3e[this.idAttribute]){
if(this.requireId){
throw new Error("requireId is enabled, new items must have an id defined to be added");
}
if(this.autoIdentity){
var _42=this.autoIdPrefix+this._autoId++;
_3e[this.idAttribute]=_42;
_40["autoId"]=true;
}
}
if(!_41&&!_41.attribute&&!this.idAttribute&&!_3e[this.idAttribute]){
throw new Error("Adding a new item requires, at a minumum, either the pInfo information, including the pInfo.attribute, or an id on the item in the field identified by idAttribute");
}
if(!_41.attribute){
_41.attribute=_3e[this.idAttribute];
}
_41.oldValue=this._trimItem(_41.item[_41.attribute]);
if(dojo.isArray(_41.item[_41.attribute])){
this._setDirty(_41.item);
_41.item[_41.attribute].push(_3e);
}else{
this._setDirty(_41.item);
_41.item[_41.attribute]=_3e;
}
_41.newValue=_41.item[_41.attribute];
if(_3e[this.idAttribute]){
this.index[_3e[this.idAttribute]]=_3e;
}
this._updateMeta(_3e,_40);
this._addReference(_3e,_41);
this._setDirty(_3e);
this.onNew(_3e,_41);
return _3e;
},_addReference:function(_43,_44){
var rid="_ref_"+this._referenceId++;
if(!_43[this.metaLabel]["referenceIds"]){
_43[this.metaLabel]["referenceIds"]=[];
}
_43[this.metaLabel]["referenceIds"].push(rid);
this._references[rid]=_44;
},deleteItem:function(_46){
_46=this._correctReference(_46);

if(this.isItem(_46)){
while(_46[this.metaLabel]["referenceIds"].length>0){


var rid=_46[this.metaLabel]["referenceIds"].pop();
var _48=this._references[rid];

var _49=_48.parent;
var _4a=_48.attribute;
if(_49&&_49[_4a]&&!dojo.isArray(_49[_4a])){
this._setDirty(_49);
this.unsetAttribute(_49,_4a);
delete _49[_4a];
}
if(dojo.isArray(_49[_4a])){

var _4b=this._trimItem(_49[_4a]);
var _4c=false;
for(var i=0;i<_49[_4a].length&&!_4c;i++){
if(_49[_4a][i][this.metaLabel]===_46[this.metaLabel]){
_4c=true;
}
}
if(_4c){
this._setDirty(_49);
var del=_49[_4a].splice(i-1,1);
delete del;
}
var _4f=this._trimItem(_49[_4a]);
this.onSet(_49,_4a,_4b,_4f);
}
delete this._references[rid];
}
this.onDelete(_46);
delete _46;
}
},_setDirty:function(_50){
for(var i=0;i<this._dirtyItems.length;i++){
if(_50[this.idAttribute]==this._dirtyItems[i][this.idAttribute]){
return;
}
}
this._dirtyItems.push({item:_50,old:this._trimItem(_50)});
this._updateMeta(_50,{isDirty:true});
},setValue:function(_52,_53,_54){
_52=this._correctReference(_52);
this._setDirty(_52);
var old=_52[_53]|undefined;
_52[_53]=_54;
this.onSet(_52,_53,old,_54);
},setValues:function(_56,_57,_58){
_56=this._correctReference(_56);
if(!dojo.isArray(_58)){
throw new Error("setValues expects to be passed an Array object as its value");
}
this._setDirty(_56);
var old=_56[_57]||null;
_56[_57]=_58;
this.onSet(_56,_57,old,_58);
},unsetAttribute:function(_5a,_5b){
_5a=this._correctReference(_5a);
this._setDirty(_5a);
var old=_5a[_5b];
delete _5a[_5b];
this.onSet(_5a,_5b,old,null);
},save:function(_5d){
var _5e=[];
if(!_5d){
_5d={};
}
while(this._dirtyItems.length>0){
var _5f=this._dirtyItems.pop()["item"];
var t=this._trimItem(_5f);
var d;
switch(_5d.format){
case "json":
d=dojo.toJson(t);
break;
case "raw":
default:
d=t;
}
_5e.push(d);
this._markClean(_5f);
}
this.onSave(_5e);
},_markClean:function(_62){
if(_62&&_62[this.metaLabel]&&_62[this.metaLabel]["isDirty"]){
delete _62[this.metaLabel]["isDirty"];
}
},revert:function(){
while(this._dirtyItems.length>0){
var d=this._dirtyItems.pop();
this._mixin(d.item,d.old);
}
this.onRevert();
},_mixin:function(_64,_65){
var mix;
if(dojo.isObject(_65)){
if(dojo.isArray(_65)){
while(_64.length>0){
_64.pop();
}
for(var i=0;i<_65.length;i++){
if(dojo.isObject(_65[i])){
if(dojo.isArray(_65[i])){
mix=[];
}else{
mix={};
if(_65[i][this.metaLabel]&&_65[i][this.metaLabel]["type"]&&_65[i][this.metaLabel]["type"]=="reference"){
_64[i]=this.index[_65[i][this.idAttribute]];
continue;
}
}
this._mixin(mix,_65[i]);
_64.push(mix);
}else{
_64.push(_65[i]);
}
}
}else{
for(var i in _64){
if(i in _65){
continue;
}
delete _64[i];
}
for(var i in _65){
if(dojo.isObject(_65[i])){
if(dojo.isArray(_65[i])){
mix=[];
}else{
if(_65[i][this.metaLabel]&&_65[i][this.metaLabel]["type"]&&_65[i][this.metaLabel]["type"]=="reference"){
_64[i]=this.index[_65[i][this.idAttribute]];
continue;
}
mix={};
}
this._mixin(mix,_65[i]);
_64[i]=mix;
}else{
_64[i]=_65[i];
}
}
}
}
},isDirty:function(_68){
_68=this._correctReference(_68);
return _68&&_68[this.metaLabel]&&_68[this.metaLabel]["isDirty"];
},_createReference:function(_69){
var obj={};
obj[this.metaLabel]={type:"reference"};
obj[this.idAttribute]=_69[this.idAttribute];
return obj;
},_trimItem:function(_6b){
var _6c;
if(dojo.isArray(_6b)){
_6c=[];
for(var i=0;i<_6b.length;i++){
if(dojo.isArray(_6b[i])){
_6c.push(this._trimItem(_6b[i]));
}else{
if(dojo.isObject(_6b[i])){
if(_6b[i]["getFullYear"]){
_6c.push(dojo.date.stamp.toISOString(_6b[i]));
}else{
if(_6b[i][this.idAttribute]){
_6c.push(this._createReference(_6b[i]));
}else{
_6c.push(this._trimItem(_6b[i]));
}
}
}else{
_6c.push(_6b[i]);
}
}
}
return _6c;
}
if(dojo.isObject(_6b)){
_6c={};
for(var _6e in _6b){
if(!_6b[_6e]){
_6c[_6e]=undefined;
continue;
}
if(dojo.isArray(_6b[_6e])){
_6c[_6e]=this._trimItem(_6b[_6e]);
}else{
if(dojo.isObject(_6b[_6e])){
if(_6b[_6e]["getFullYear"]){
_6c[_6e]=dojo.date.stamp.toISOString(_6b[_6e]);
}else{
if(_6b[_6e][this.idAttribute]){
_6c[_6e]=this._createReference(_6b[_6e]);
}else{
_6c[_6e]=this._trimItem(_6b[_6e]);
}
}
}else{
_6c[_6e]=_6b[_6e];
}
}
}
return _6c;
}
},onSet:function(){
},onNew:function(){
},onDelete:function(){
},onSave:function(_6f){
},onRevert:function(){
}});
dojox.data.jsonPathStore.byId=dojox.data.jsonPathStore.fetchItemByIdentity;
}
