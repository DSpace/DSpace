/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.rpc.Rest"]){
dojo._hasResource["dojox.rpc.Rest"]=true;
dojo.provide("dojox.rpc.Rest");
(function(){
if(dojox.rpc&&dojox.rpc.transportRegistry){
dojox.rpc.transportRegistry.register("REST",function(_1){
return _1=="REST";
},{getExecutor:function(_2,_3,_4){
return new dojox.rpc.Rest(_3.name,(_3.contentType||_4._smd.contentType||"").match(/json|javascript/),null,function(id,_6){
var _7=_4._getRequest(_3,[id]);
_7.url=_7.target+(_7.data?"?"+_7.data:"");
return _7;
});
}});
}
var _8;
function index(_9,_a,_b,id){
_9.addCallback(function(_d){
if(_b){
_b=_9.ioArgs.xhr&&_9.ioArgs.xhr.getResponseHeader("Content-Range");
_9.fullLength=_b&&(_b=_b.match(/\/(.*)/))&&parseInt(_b[1]);
}
return _d;
});
return _9;
};
_8=dojox.rpc.Rest=function(_e,_f,_10,_11){
var _12;
_e=_e.match(/\/$/)?_e:(_e+"/");
_12=function(id,_14){
return _8._get(_12,id,_14);
};
_12.isJson=_f;
_12._schema=_10;
_12.cache={serialize:_f?((dojox.json&&dojox.json.ref)||dojo).toJson:function(_15){
return _15;
}};
_12._getRequest=_11||function(id,_17){
return {url:_e+(dojo.isObject(id)?"?"+dojo.objectToQuery(id):id==null?"":id),handleAs:_f?"json":"text",contentType:_f?"application/json":"text/plain",sync:dojox.rpc._sync,headers:{Accept:_f?"application/json,application/javascript":"*/*",Range:_17&&(_17.start>=0||_17.count>=0)?"items="+(_17.start||"0")+"-"+((_17.count&&(_17.count+(_17.start||0)-1))||""):undefined}};
};
function makeRest(_18){
_12[_18]=function(id,_1a){
return _8._change(_18,_12,id,_1a);
};
};
makeRest("put");
makeRest("post");
makeRest("delete");
_12.servicePath=_e;
return _12;
};
_8._index={};
_8._change=function(_1b,_1c,id,_1e){
var _1f=_1c._getRequest(id);
_1f[_1b+"Data"]=_1e;
return index(dojo.xhr(_1b.toUpperCase(),_1f,true),_1c);
};
_8._get=function(_20,id,_22){
_22=_22||{};
return index(dojo.xhrGet(_20._getRequest(id,_22)),_20,(_22.start>=0||_22.count>=0),id);
};
})();
}
