/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.foo"]){
dojo._hasResource["dojo.foo"]=true;
(function(){
var d=dojo;
d.mixin(d,{_loadedModules:{},_inFlightCount:0,_hasResource:{},_modulePrefixes:{dojo:{name:"dojo",value:"."},doh:{name:"doh",value:"../util/doh"},tests:{name:"tests",value:"tests"}},_moduleHasPrefix:function(_2){
var mp=this._modulePrefixes;
return !!(mp[_2]&&mp[_2].value);
},_getModulePrefix:function(_4){
var mp=this._modulePrefixes;
if(this._moduleHasPrefix(_4)){
return mp[_4].value;
}
return _4;
},_loadedUrls:[],_postLoad:false,_loaders:[],_unloaders:[],_loadNotifying:false});
dojo._loadPath=function(_6,_7,cb){
var _9=((_6.charAt(0)=="/"||_6.match(/^\w+:/))?"":this.baseUrl)+_6;
try{
return !_7?this._loadUri(_9,cb):this._loadUriAndCheck(_9,_7,cb);
}
catch(e){
console.error(e);
return false;
}
};
dojo._loadUri=function(_a,cb){
if(this._loadedUrls[_a]){
return true;
}
var _c=this._getText(_a,true);
if(!_c){
return false;
}
this._loadedUrls[_a]=true;
this._loadedUrls.push(_a);
if(cb){
_c="("+_c+")";
}else{
_c=this._scopePrefix+_c+this._scopeSuffix;
}
if(d.isMoz){
_c+="\r\n//@ sourceURL="+_a;
}
var _d=d["eval"](_c);
if(cb){
cb(_d);
}
return true;
};
dojo._loadUriAndCheck=function(_e,_f,cb){
var ok=false;
try{
ok=this._loadUri(_e,cb);
}
catch(e){
console.error("failed loading "+_e+" with error: "+e);
}
return !!(ok&&this._loadedModules[_f]);
};
dojo.loaded=function(){
this._loadNotifying=true;
this._postLoad=true;
var mll=d._loaders;
this._loaders=[];
for(var x=0;x<mll.length;x++){
mll[x]();
}
this._loadNotifying=false;
if(d._postLoad&&d._inFlightCount==0&&mll.length){
d._callLoaded();
}
};
dojo.unloaded=function(){
var mll=this._unloaders;
while(mll.length){
(mll.pop())();
}
};
d._onto=function(arr,obj,fn){
if(!fn){
arr.push(obj);
}else{
if(fn){
var _18=(typeof fn=="string")?obj[fn]:fn;
arr.push(function(){
_18.call(obj);
});
}
}
};
dojo.addOnLoad=function(obj,_1a){
d._onto(d._loaders,obj,_1a);
if(d._postLoad&&d._inFlightCount==0&&!d._loadNotifying){
d._callLoaded();
}
};
var dca=d.config.addOnLoad;
if(dca){
d.addOnLoad[(dca instanceof Array?"apply":"call")](d,dca);
}
dojo.addOnUnload=function(obj,_1d){
d._onto(d._unloaders,obj,_1d);
};
dojo._modulesLoaded=function(){
if(d._postLoad){
return;
}
if(d._inFlightCount>0){
console.warn("files still in flight!");
return;
}
d._callLoaded();
};
dojo._callLoaded=function(){
if(typeof setTimeout=="object"||(dojo.config.useXDomain&&d.isOpera)){
if(dojo.isAIR){
setTimeout(function(){
dojo.loaded();
},0);
}else{
setTimeout(dojo._scopeName+".loaded();",0);
}
}else{
d.loaded();
}
};
dojo._getModuleSymbols=function(_1e){
var _1f=_1e.split(".");
for(var i=_1f.length;i>0;i--){
var _21=_1f.slice(0,i).join(".");
if((i==1)&&!this._moduleHasPrefix(_21)){
_1f[0]="../"+_1f[0];
}else{
var _22=this._getModulePrefix(_21);
if(_22!=_21){
_1f.splice(0,i,_22);
break;
}
}
}
return _1f;
};
dojo._global_omit_module_check=false;
dojo.loadInit=function(_23){
_23();
};
dojo._loadModule=dojo.require=function(_24,_25){
_25=this._global_omit_module_check||_25;
var _26=this._loadedModules[_24];
if(_26){
return _26;
}
var _27=this._getModuleSymbols(_24).join("/")+".js";
var _28=(!_25)?_24:null;
var ok=this._loadPath(_27,_28);
if(!ok&&!_25){
throw new Error("Could not load '"+_24+"'; last tried '"+_27+"'");
}
if(!_25&&!this._isXDomain){
_26=this._loadedModules[_24];
if(!_26){
throw new Error("symbol '"+_24+"' is not defined after loading '"+_27+"'");
}
}
return _26;
};
dojo.provide=function(_2a){
_2a=_2a+"";
return (d._loadedModules[_2a]=d.getObject(_2a,true));
};
dojo.platformRequire=function(_2b){
var _2c=_2b.common||[];
var _2d=_2c.concat(_2b[d._name]||_2b["default"]||[]);
for(var x=0;x<_2d.length;x++){
var _2f=_2d[x];
if(_2f.constructor==Array){
d._loadModule.apply(d,_2f);
}else{
d._loadModule(_2f);
}
}
};
dojo.requireIf=function(_30,_31){
if(_30===true){
var _32=[];
for(var i=1;i<arguments.length;i++){
_32.push(arguments[i]);
}
d.require.apply(d,_32);
}
};
dojo.requireAfterIf=d.requireIf;
dojo.registerModulePath=function(_34,_35){
d._modulePrefixes[_34]={name:_34,value:_35};
};
dojo.requireLocalization=function(_36,_37,_38,_39){
d.require("dojo.i18n");
d.i18n._requireLocalization.apply(d.hostenv,arguments);
};
var ore=new RegExp("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?$");
var ire=new RegExp("^((([^\\[:]+):)?([^@]+)@)?(\\[([^\\]]+)\\]|([^\\[:]*))(:([0-9]+))?$");
dojo._Url=function(){
var n=null;
var _a=arguments;
var uri=[_a[0]];
for(var i=1;i<_a.length;i++){
if(!_a[i]){
continue;
}
var _40=new d._Url(_a[i]+"");
var _41=new d._Url(uri[0]+"");
if(_40.path==""&&!_40.scheme&&!_40.authority&&!_40.query){
if(_40.fragment!=n){
_41.fragment=_40.fragment;
}
_40=_41;
}else{
if(!_40.scheme){
_40.scheme=_41.scheme;
if(!_40.authority){
_40.authority=_41.authority;
if(_40.path.charAt(0)!="/"){
var _42=_41.path.substring(0,_41.path.lastIndexOf("/")+1)+_40.path;
var _43=_42.split("/");
for(var j=0;j<_43.length;j++){
if(_43[j]=="."){
if(j==_43.length-1){
_43[j]="";
}else{
_43.splice(j,1);
j--;
}
}else{
if(j>0&&!(j==1&&_43[0]=="")&&_43[j]==".."&&_43[j-1]!=".."){
if(j==(_43.length-1)){
_43.splice(j,1);
_43[j-1]="";
}else{
_43.splice(j-1,2);
j-=2;
}
}
}
}
_40.path=_43.join("/");
}
}
}
}
uri=[];
if(_40.scheme){
uri.push(_40.scheme,":");
}
if(_40.authority){
uri.push("//",_40.authority);
}
uri.push(_40.path);
if(_40.query){
uri.push("?",_40.query);
}
if(_40.fragment){
uri.push("#",_40.fragment);
}
}
this.uri=uri.join("");
var r=this.uri.match(ore);
this.scheme=r[2]||(r[1]?"":n);
this.authority=r[4]||(r[3]?"":n);
this.path=r[5];
this.query=r[7]||(r[6]?"":n);
this.fragment=r[9]||(r[8]?"":n);
if(this.authority!=n){
r=this.authority.match(ire);
this.user=r[3]||n;
this.password=r[4]||n;
this.host=r[6]||r[7];
this.port=r[9]||n;
}
};
dojo._Url.prototype.toString=function(){
return this.uri;
};
dojo.moduleUrl=function(_46,url){
var loc=d._getModuleSymbols(_46).join("/");
if(!loc){
return null;
}
if(loc.lastIndexOf("/")!=loc.length-1){
loc+="/";
}
var _49=loc.indexOf(":");
if(loc.charAt(0)!="/"&&(_49==-1||_49>loc.indexOf("/"))){
loc=d.baseUrl+loc;
}
return new d._Url(loc,url);
};
})();
}
