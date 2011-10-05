/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.JsonRestStore"]){
dojo._hasResource["dojox.data.JsonRestStore"]=true;
dojo.provide("dojox.data.JsonRestStore");
dojo.require("dojox.data.ServiceStore");
dojo.require("dojox.rpc.JsonRest");
dojo.declare("dojox.data.JsonRestStore",dojox.data.ServiceStore,{constructor:function(_1){
dojo.connect(dojox.rpc.Rest._index,"onUpdate",this,function(_2,_3,_4,_5){
var _6=this.service.servicePath;
if(!_2.__id){

}else{
if(_2.__id.substring(0,_6.length)==_6){
this.onSet(_2,_3,_4,_5);
}
}
});
this.idAttribute=this.idAttribute||"id";
if(typeof _1.target=="string"&&!this.service){
this.service=dojox.rpc.Rest(this.target,true);
}
dojox.rpc.JsonRest.registerService(this.service,_1.target,this.schema);
this.schema=this.service._schema=this.schema||this.service._schema||{};
this.service._store=this;
this.schema._idAttr=this.idAttribute;
this._constructor=dojox.rpc.JsonRest.getConstructor(this.service);
this._index=dojox.rpc.Rest._index;
},target:"",newItem:function(_7,_8){
_7=new this._constructor(_7);
if(_8){
var _9=this.getValue(_8.parent,_8.attribute,[]);
this.setValue(_8.parent,_8.attribute,_9.concat([_7]));
}
this.onNew(_7);
return _7;
},deleteItem:function(_a){
dojox.rpc.JsonRest.deleteObject(_a);
var _b=dojox.data._getStoreForItem(_a);
_b.onDelete(_a);
},changing:function(_c,_d){
dojox.rpc.JsonRest.changing(_c,_d);
},setValue:function(_e,_f,_10){
var old=_e[_f];
var _12=_e.__id?dojox.data._getStoreForItem(_e):this;
if(dojox.json.schema&&_12.schema&&_12.schema.properties){
var _13=dojox.json.schema.checkPropertyChange(_10,_12.schema.properties[_f]);
if(!_13.valid){
throw new Error(dojo.map(_13.errors,function(_14){
return _14.message;
}).join(","));
}
}
if(old!==_10){
_12.changing(_e);
_e[_f]=_10;
_12.onSet(_e,_f,old,_10);
}
},setValues:function(_15,_16,_17){
if(!dojo.isArray(_17)){
throw new Error("setValues expects to be passed an Array object as its value");
}
this.setValue(_15,_16,_17);
},unsetAttribute:function(_18,_19){
this.changing(_18);
var old=_18[_19];
delete _18[_19];
this.onSet(_18,_19,old,undefined);
},save:function(_1b){
if(!(_1b&&_1b.global)){
(_1b=_1b||{}).service=this.service;
}
var _1c=dojox.rpc.JsonRest.commit(_1b);
this.serverVersion=this._updates&&this._updates.length;
return _1c;
},revert:function(){
var _1d=dojox.rpc.JsonRest.getDirtyObjects().concat([]);
while(_1d.length>0){
var d=_1d.pop();
var _1f=dojox.data._getStoreForItem(d.object||d.old);
if(!d.object){
_1f.onNew(d.old);
}else{
if(!d.old){
_1f.onDelete(d.object);
}else{
for(var i in d.object){
if(d.object[i]!=d.old[i]){
_1f.onSet(d.object,i,d.object[i],d.old[i]);
}
}
}
}
}
dojox.rpc.JsonRest.revert();
},isDirty:function(_21){
return dojox.rpc.JsonRest.isDirty(_21);
},isItem:function(_22){
return _22&&_22.__id&&this.service==dojox.rpc.JsonRest.getServiceAndId(_22.__id).service;
},_doQuery:function(_23){
var _24=typeof _23.queryStr=="string"?_23.queryStr:_23.query;
return dojox.rpc.JsonRest.query(this.service,_24,_23);
},_processResults:function(_25,_26){
var _27=_25.length;
return {totalCount:_26.fullLength||(_26.request.count==_27?_27*2:_27),items:_25};
},getConstructor:function(){
return this._constructor;
},getIdentity:function(_28){
var id=_28.__clientId||_28.__id;
if(!id){
this.inherited(arguments);
}
var _2a=this.service.servicePath;
return id.substring(0,_2a.length)!=_2a?id:id.substring(_2a.length);
},fetchItemByIdentity:function(_2b){
var id=_2b.identity;
var _2d=this;
if(id.match(/^(\w*:)?\//)){
var _2e=dojox.rpc.JsonRest.getServiceAndId(id);
_2d=_2e.service._store;
_2b.identity=_2e.id;
}
_2b._prefix=_2d.service.servicePath;
return _2d.inherited(arguments);
},onSet:function(){
},onNew:function(){
},onDelete:function(){
},getFeatures:function(){
var _2f=this.inherited(arguments);
_2f["dojo.data.api.Write"]=true;
_2f["dojo.data.api.Notification"]=true;
return _2f;
}});
dojox.data._getStoreForItem=function(_30){
return dojox.rpc.JsonRest.services[_30.__id.match(/.*\//)[0]]._store;
};
}
