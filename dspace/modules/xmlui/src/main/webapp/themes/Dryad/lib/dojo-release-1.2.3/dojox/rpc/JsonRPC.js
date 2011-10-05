/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.rpc.JsonRPC"]){
dojo._hasResource["dojox.rpc.JsonRPC"]=true;
dojo.provide("dojox.rpc.JsonRPC");
dojo.require("dojox.rpc.Service");
(function(){
function jsonRpcEnvelope(_1){
return {serialize:function(_2,_3,_4,_5){
var d={id:this._requestId++,method:_3.name,params:_4};
if(_1){
d.jsonrpc=_1;
}
return {data:dojo.toJson(d),handleAs:"json",contentType:"application/json",transport:"POST"};
},deserialize:function(_7){
if("Error"==_7.name){
_7=dojo.fromJson(_7.responseText);
}
if(_7.error){
var e=new Error(_7.error.message||_7.error);
e._rpcErrorObject=_7.error;
return e;
}
return _7.result;
}};
};
dojox.rpc.envelopeRegistry.register("JSON-RPC-1.0",function(_9){
return _9=="JSON-RPC-1.0";
},dojo.mixin({namedParams:false},jsonRpcEnvelope()));
dojox.rpc.envelopeRegistry.register("JSON-RPC-2.0",function(_a){
return _a=="JSON-RPC-2.0";
},jsonRpcEnvelope("2.0"));
})();
}
