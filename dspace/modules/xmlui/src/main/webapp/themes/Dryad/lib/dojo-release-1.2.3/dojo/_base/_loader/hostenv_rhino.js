/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(dojo.config["baseUrl"]){
dojo.baseUrl=dojo.config["baseUrl"];
}else{
dojo.baseUrl="./";
}
dojo.locale=dojo.locale||String(java.util.Locale.getDefault().toString().replace("_","-").toLowerCase());
dojo._name="rhino";
dojo.isRhino=true;
if(typeof print=="function"){
console.debug=print;
}
if(typeof dojo["byId"]=="undefined"){
dojo.byId=function(id,_2){
if(id&&(typeof id=="string"||id instanceof String)){
if(!_2){
_2=document;
}
return _2.getElementById(id);
}
return id;
};
}
dojo._loadUri=function(_3,cb){
try{
var _5=(new java.io.File(_3)).exists();
if(!_5){
try{
var _6=(new java.net.URL(_3)).openStream();
_6.close();
}
catch(e){
return false;
}
}
if(cb){
var _7=(_5?readText:readUri)(_3,"UTF-8");
cb(eval("("+_7+")"));
}else{
load(_3);
}
return true;
}
catch(e){

return false;
}
};
dojo.exit=function(_8){
quit(_8);
};
dojo._rhinoCurrentScriptViaJava=function(_9){
var _a=Packages.org.mozilla.javascript.Context.getCurrentContext().getOptimizationLevel();
var _b=new java.io.CharArrayWriter();
var pw=new java.io.PrintWriter(_b);
var _d=new java.lang.Exception();
var s=_b.toString();
var _f=s.match(/[^\(]*\.js\)/gi);
if(!_f){
throw Error("cannot parse printStackTrace output: "+s);
}
var _10=((typeof _9!="undefined")&&(_9))?_f[_9+1]:_f[_f.length-1];
_10=_f[3];
if(!_10){
_10=_f[1];
}
if(!_10){
throw Error("could not find js file in printStackTrace output: "+s);
}
return _10;
};
function readText(_11,_12){
_12=_12||"utf-8";
var jf=new java.io.File(_11);
var is=new java.io.FileInputStream(jf);
return dj_readInputStream(is,_12);
};
function readUri(uri,_16){
var _17=(new java.net.URL(uri)).openConnection();
_16=_16||_17.getContentEncoding()||"utf-8";
var is=_17.getInputStream();
return dj_readInputStream(is,_16);
};
function dj_readInputStream(is,_1a){
var _1b=new java.io.BufferedReader(new java.io.InputStreamReader(is,_1a));
try{
var sb=new java.lang.StringBuffer();
var _1d="";
while((_1d=_1b.readLine())!==null){
sb.append(_1d);
sb.append(java.lang.System.getProperty("line.separator"));
}
return sb.toString();
}
finally{
_1b.close();
}
};
if((!dojo.config.libraryScriptUri)||(!dojo.config.libraryScriptUri.length)){
try{
dojo.config.libraryScriptUri=dojo._rhinoCurrentScriptViaJava(1);
}
catch(e){
if(dojo.config["isDebug"]){
print("\n");
print("we have no idea where Dojo is located.");
print("Please try loading rhino in a non-interpreted mode or set a");
print("\n\tdjConfig.libraryScriptUri\n");
print("Setting the dojo path to './'");
print("This is probably wrong!");
print("\n");
print("Dojo will try to load anyway");
}
dojo.config.libraryScriptUri="./";
}
}
dojo.doc=typeof (document)!="undefined"?document:null;
dojo.body=function(){
return document.body;
};
try{
setTimeout;
clearTimeout;
}
catch(e){
dojo._timeouts=[];
function clearTimeout(idx){
if(!dojo._timeouts[idx]){
return;
}
dojo._timeouts[idx].stop();
};
function setTimeout(_1f,_20){
var def={sleepTime:_20,hasSlept:false,run:function(){
if(!this.hasSlept){
this.hasSlept=true;
java.lang.Thread.currentThread().sleep(this.sleepTime);
}
try{
_1f();
}
catch(e){

}
}};
var _22=new java.lang.Runnable(def);
var _23=new java.lang.Thread(_22);
_23.start();
return dojo._timeouts.push(_23)-1;
};
}
if(dojo.config["modulePaths"]){
for(var param in dojo.config["modulePaths"]){
dojo.registerModulePath(param,dojo.config["modulePaths"][param]);
}
}
