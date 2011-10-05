/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.json.ref"]){
dojo._hasResource["dojox.json.ref"]=true;
dojo.provide("dojox.json.ref");
dojo.require("dojo.date.stamp");
dojox.json.ref={resolveJson:function(_1,_2){
_2=_2||{};
var _3=_2.idAttribute||"id";
var _4=_2.idPrefix||"/";
var _5=_2.assignAbsoluteIds;
var _6=_2.index||{};
var _7,_8=[];
var _9=/^(.*\/)?(\w+:\/\/)|[^\/\.]+\/\.\.\/|^.*\/(\/)/;
var _a=this._addProp;
function walk(it,_c,_d,_e){
var _f,val,id=it[_3]||_d;
if(id!==undefined){
id=(_4+id).replace(_9,"$2$3");
}
var _12=_e||it;
if(id!==undefined){
if(_5){
it.__id=id;
}
if(_6[id]&&((it instanceof Array)==(_6[id] instanceof Array))){
_12=_6[id];
delete _12.$ref;
_f=true;
}else{
var _13=_2.schemas&&(!(it instanceof Array))&&(val=id.match(/^(.+\/)[^\.\[]*$/))&&(val=_2.schemas[val[1]])&&val.prototype;
if(_13){
var F=function(){
};
F.prototype=_13;
_12=new F();
}
}
_6[id]=_12;
}
for(var i in it){
if(it.hasOwnProperty(i)){
if((typeof (val=it[i])=="object")&&val){
_7=val.$ref;
if(_7){
var _16=_7.replace(/\\./g,"@").replace(/"[^"\\\n\r]*"/g,"");
if(/[\w\[\]\.\$# \/\r\n\t]/.test(_16)&&!/\=|((^|\W)new\W)/.test(_16)){
delete it[i];
var _17=_7.match(/(^([^\[]*\/)?[^\.\[]*)([\.\[].*)?/);
if((_7=(_17[1]=="$"||_17[1]=="this"||_17[1]=="#")?_1:_6[(_4+_17[1]).replace(_9,"$2$3")])){
try{
_7=_17[3]?eval("ref"+_17[3].replace(/^#/,"").replace(/^([^\[\.])/,".$1").replace(/\.([\w$_]+)/g,"[\"$1\"]")):_7;
}
catch(e){
_7=null;
}
}
if(_7){
val=_7;
}else{
if(!_c){
var _18;
if(!_18){
_8.push(_12);
}
_18=true;
}else{
val=walk(val,false,val.$ref);
val._loadObject=_2.loader;
}
}
}
}else{
if(!_c){
val=walk(val,_8==it,id&&_a(id,i),_12!=it&&typeof _12[i]=="object"&&_12[i]);
}
}
}
it[i]=val;
if(_12!=it){
var old=_12[i];
_12[i]=val;
if(_f&&val!==old){
if(_6.onUpdate){
_6.onUpdate(_12,i,old,val);
}
}
}
}
}
if(_f){
for(i in _12){
if(!it.hasOwnProperty(i)&&i!="__id"&&i!="__clientId"&&!(_12 instanceof Array&&isNaN(i))){
if(_6.onUpdate){
_6.onUpdate(_12,i,_12[i],undefined);
}
delete _12[i];
while(_12 instanceof Array&&_12.length&&_12[_12.length-1]===undefined){
_12.length--;
}
}
}
}else{
if(_6.onLoad){
_6.onLoad(_12);
}
}
return _12;
};
if(_1&&typeof _1=="object"){
_1=walk(_1,false,_2.defaultId);
walk(_8,false);
}
return _1;
},fromJson:function(str,_1b){
function ref(_1c){
return {$ref:_1c};
};
var _1d=eval("("+str+")");
if(_1d){
return this.resolveJson(_1d,_1b);
}
return _1d;
},toJson:function(it,_1f,_20,_21){
var _22=this._useRefs;
var _23=this._addProp;
_20=_20||"";
var _24=_21||{};
function serialize(it,_26,_27){
if(typeof it=="object"&&it){
var _28;
if(it instanceof Date){
return "\""+dojo.date.stamp.toISOString(it,{zulu:true})+"\"";
}
var id=it.__id;
if(id){
if(_26!="#"&&(_22||_24[id])){
var ref=id;
if(id.charAt(0)!="#"){
if(id.substring(0,_20.length)==_20){
ref=id.substring(_20.length);
}else{
ref=id;
}
}
return serialize({$ref:ref},"#");
}
_26=id;
}else{
it.__id=_26;
_24[_26]=it;
}
_27=_27||"";
var _2b=_1f?_27+dojo.toJsonIndentStr:"";
var _2c=_1f?"\n":"";
var sep=_1f?" ":"";
if(it instanceof Array){
var res=dojo.map(it,function(obj,i){
var val=serialize(obj,_23(_26,i),_2b);
if(typeof val!="string"){
val="undefined";
}
return _2c+_2b+val;
});
return "["+res.join(","+sep)+_2c+_27+"]";
}
var _32=[];
for(var i in it){
if(it.hasOwnProperty(i)){
var _34;
if(typeof i=="number"){
_34="\""+i+"\"";
}else{
if(typeof i=="string"&&i.charAt(0)!="_"){
_34=dojo._escapeString(i);
}else{
continue;
}
}
var val=serialize(it[i],_23(_26,i),_2b);
if(typeof val!="string"){
continue;
}
_32.push(_2c+_2b+_34+":"+sep+val);
}
}
return "{"+_32.join(","+sep)+_2c+_27+"}";
}else{
if(typeof it=="function"&&dojox.json.ref.serializeFunctions){
return it.toString();
}
}
return dojo.toJson(it);
};
var _36=serialize(it,"#","");
if(!_21){
for(i in _24){
delete _24[i].__id;
}
}
return _36;
},_addProp:function(id,_38){
return id+(id.match(/#/)?"":"#")+(typeof _38=="string"?_38.match(/^[a-zA-Z]\w*$/)?("."+_38):("["+dojo._escapeString(_38).replace(/"/g,"'")+"]"):("["+_38+"]"));
},_useRefs:false,serializeFunctions:false};
}
