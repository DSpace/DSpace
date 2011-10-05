/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.io.xhrMultiPart"]){
dojo._hasResource["dojox.io.xhrMultiPart"]=true;
dojo.provide("dojox.io.xhrMultiPart");
dojo.require("dojox.uuid.generateRandomUuid");
(function(){
function _createPart(_1,_2){
if(!_1["name"]&&!_1["content"]){
throw new Error("Each part of a multi-part request requires 'name' and 'content'.");
}
var _3=[];
_3.push("--"+_2,"Content-Disposition: form-data; name=\""+_1.name+"\""+(_1["filename"]?"; filename=\""+_1.filename+"\"":""));
if(_1["contentType"]){
var ct="Content-Type: "+_1.contentType;
if(_1["charset"]){
ct+="; Charset="+_1.charset;
}
_3.push(ct);
}
if(_1["contentTransferEncoding"]){
_3.push("Content-Transfer-Encoding: "+_1.contentTransferEncoding);
}
_3.push("",_1.content);
return _3;
};
function _partsFromNode(_5,_6){
var o=dojo.formToObject(_5),_8=[];
for(var p in o){
if(dojo.isArray(o[p])){
dojo.forEach(o[p],function(_a){
_8=_8.concat(_createPart({name:p,content:_a},_6));
});
}else{
_8=_8.concat(_createPart({name:p,content:o[p]},_6));
}
}
return _8;
};
dojox.io.xhrMultiPart=function(_b){
if(!_b["file"]&&!_b["content"]&&!_b["form"]){
throw new Error("content, file or form must be provided to dojox.io.xhrMultiPart's arguments");
}
var _c=dojox.uuid.generateRandomUuid(),_d=[],_e="";
if(_b["file"]||_b["content"]){
var v=_b["file"]||_b["content"];
dojo.forEach((dojo.isArray(v)?v:[v]),function(_10){
_d=_d.concat(_createPart(_10,_c));
});
}else{
if(_b["form"]){
if(dojo.query("input[type=file]",_b["form"]).length){
throw new Error("dojox.io.xhrMultiPart cannot post files that are values of an INPUT TYPE=FILE.  Use dojo.io.iframe.send() instead.");
}
_d=_partsFromNode(_b["form"],_c);
}
}
if(_d.length){
_d.push("--"+_c+"--","");
_e=_d.join("\r\n");
}

return dojo.rawXhrPost(dojo.mixin(_b,{contentType:"multipart/form-data; boundary="+_c,postData:_e}));
};
})();
}
