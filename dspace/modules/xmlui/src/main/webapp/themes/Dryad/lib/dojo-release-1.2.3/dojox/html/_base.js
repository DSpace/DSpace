/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.html._base"]){
dojo._hasResource["dojox.html._base"]=true;
dojo.provide("dojox.html._base");
dojo.require("dojo.html");
(function(){
if(dojo.isIE){
var _1=/(AlphaImageLoader\([^)]*?src=(['"]))(?![a-z]+:|\/)([^\r\n;}]+?)(\2[^)]*\)\s*[;}]?)/g;
}
var _2=/(?:(?:@import\s*(['"])(?![a-z]+:|\/)([^\r\n;{]+?)\1)|url\(\s*(['"]?)(?![a-z]+:|\/)([^\r\n;]+?)\3\s*\))([a-z, \s]*[;}]?)/g;
var _3=dojox.html._adjustCssPaths=function(_4,_5){
if(!_5||!_4){
return;
}
if(_1){
_5=_5.replace(_1,function(_6,_7,_8,_9,_a){
return _7+(new dojo._Url(_4,"./"+_9).toString())+_a;
});
}
return _5.replace(_2,function(_b,_c,_d,_e,_f,_10){
if(_d){
return "@import \""+(new dojo._Url(_4,"./"+_d).toString())+"\""+_10;
}else{
return "url("+(new dojo._Url(_4,"./"+_f).toString())+")"+_10;
}
});
};
var _11=/(<[a-z][a-z0-9]*\s[^>]*)(?:(href|src)=(['"]?)([^>]*?)\3|style=(['"]?)([^>]*?)\5)([^>]*>)/gi;
var _12=dojox.html._adjustHtmlPaths=function(_13,_14){
var url=_13||"./";
return _14.replace(_11,function(tag,_17,_18,_19,_1a,_1b,_1c,end){
return _17+(_18?(_18+"="+_19+(new dojo._Url(url,_1a).toString())+_19):("style="+_1b+_3(url,_1c)+_1b))+end;
});
};
var _1e=dojox.html._snarfStyles=function(_1f,_20,_21){
_21.attributes=[];
return _20.replace(/(?:<style([^>]*)>([\s\S]*?)<\/style>|<link\s+(?=[^>]*rel=['"]?stylesheet)([^>]*?href=(['"])([^>]*?)\4[^>\/]*)\/?>)/gi,function(_22,_23,_24,_25,_26,_27){
var i,_29=(_23||_25||"").replace(/^\s*([\s\S]*?)\s*$/i,"$1");
if(_24){
i=_21.push(_1f?_3(_1f,_24):_24);
}else{
i=_21.push("@import \""+_27+"\";");
_29=_29.replace(/\s*(?:rel|href)=(['"])?[^\s]*\1\s*/gi,"");
}
if(_29){
_29=_29.split(/\s+/);
var _2a={},tmp;
for(var j=0,e=_29.length;j<e;j++){
tmp=_29[j].split("=");
_2a[tmp[0]]=tmp[1].replace(/^\s*['"]?([\s\S]*?)['"]?\s*$/,"$1");
}
_21.attributes[i-1]=_2a;
}
return "";
});
};
var _2e=dojox.html._snarfScripts=function(_2f,_30){
_30.code="";
function download(src){
if(_30.downloadRemote){
dojo.xhrGet({url:src,sync:true,load:function(_32){
_30.code+=_32+";";
},error:_30.errBack});
}
};
return _2f.replace(/<script\s*(?![^>]*type=['"]?dojo)(?:[^>]*?(?:src=(['"]?)([^>]*?)\1[^>]*)?)*>([\s\S]*?)<\/script>/gi,function(_33,_34,src,_36){
if(src){
download(src);
}else{
_30.code+=_36;
}
return "";
});
};
var _37=dojox.html.evalInGlobal=function(_38,_39){
_39=_39||dojo.doc.body;
var n=_39.ownerDocument.createElement("script");
n.type="text/javascript";
_39.appendChild(n);
n.text=_38;
};
dojo.declare("dojox.html._ContentSetter",[dojo.html._ContentSetter],{adjustPaths:false,referencePath:".",renderStyles:false,executeScripts:false,scriptHasHooks:false,scriptHookReplacement:null,_renderStyles:function(_3b){
this._styleNodes=[];
var st,att,_3e,doc=this.node.ownerDocument;
var _40=doc.getElementsByTagName("head")[0];
for(var i=0,e=_3b.length;i<e;i++){
_3e=_3b[i];
att=_3b.attributes[i];
st=doc.createElement("style");
st.setAttribute("type","text/css");
for(var x in att){
st.setAttribute(x,att[x]);
}
this._styleNodes.push(st);
_40.appendChild(st);
if(st.styleSheet){
st.styleSheet.cssText=_3e;
}else{
st.appendChild(doc.createTextNode(_3e));
}
}
},empty:function(){
this.inherited("empty",arguments);
this._styles=[];
},onBegin:function(){
this.inherited("onBegin",arguments);
var _44=this.content,_45=this.node;
var _46=this._styles;
if(dojo.isString(_44)){
if(this.adjustPaths&&this.referencePath){
_44=_12(this.referencePath,_44);
}
if(this.renderStyles||this.cleanContent){
_44=_1e(this.referencePath,_44,_46);
}
if(this.executeScripts){
var _t=this;
var _48={downloadRemote:true,errBack:function(e){
_t._onError.call(_t,"Exec","Error downloading remote script in \""+_t.id+"\"",e);
}};
_44=_2e(_44,_48);
this._code=_48.code;
}
}
this.content=_44;
},onEnd:function(){
var _4a=this._code,_4b=this._styles;
if(this._styleNodes&&this._styleNodes.length){
while(this._styleNodes.length){
dojo._destroyElement(this._styleNodes.pop());
}
}
if(this.renderStyles&&_4b&&_4b.length){
this._renderStyles(_4b);
}
if(this.executeScripts&&_4a){
if(this.cleanContent){
_4a=_4a.replace(/(<!--|(?:\/\/)?-->|<!\[CDATA\[|\]\]>)/g,"");
}
if(this.scriptHasHooks){
_4a=_4a.replace(/_container_(?!\s*=[^=])/g,this.scriptHookReplacement);
}
try{
_37(_4a,this.node);
}
catch(e){
this._onError("Exec","Error eval script in "+this.id+", "+e.message,e);
}
}
this.inherited("onEnd",arguments);
},tearDown:function(){
this.inherited(arguments);
delete this._styles;
if(this._styleNodes&&this._styleNodes.length){
while(this._styleNodes.length){
dojo._destroyElement(this._styleNodes.pop());
}
}
delete this._styleNodes;
dojo.mixin(this,dojo.getObject(this.declaredClass).prototype);
}});
dojox.html.set=function(_4c,_4d,_4e){
if(!_4e){
return dojo.html._setNodeContent(_4c,_4d,true);
}else{
var op=new dojox.html._ContentSetter(dojo.mixin(_4e,{content:_4d,node:_4c}));
return op.set();
}
};
})();
}
