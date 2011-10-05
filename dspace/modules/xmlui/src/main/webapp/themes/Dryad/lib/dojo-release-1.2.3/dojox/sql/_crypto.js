/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.sql._crypto"]){
dojo._hasResource["dojox.sql._crypto"]=true;
dojo.provide("dojox.sql._crypto");
dojo.mixin(dojox.sql._crypto,{_POOL_SIZE:100,encrypt:function(_1,_2,_3){
this._initWorkerPool();
var _4={plaintext:_1,password:_2};
_4=dojo.toJson(_4);
_4="encr:"+String(_4);
this._assignWork(_4,_3);
},decrypt:function(_5,_6,_7){
this._initWorkerPool();
var _8={ciphertext:_5,password:_6};
_8=dojo.toJson(_8);
_8="decr:"+String(_8);
this._assignWork(_8,_7);
},_initWorkerPool:function(){
if(!this._manager){
try{
this._manager=google.gears.factory.create("beta.workerpool","1.0");
this._unemployed=[];
this._employed={};
this._handleMessage=[];
var _9=this;
this._manager.onmessage=function(_a,_b){
var _c=_9._employed["_"+_b];
_9._employed["_"+_b]=undefined;
_9._unemployed.push("_"+_b);
if(_9._handleMessage.length){
var _d=_9._handleMessage.shift();
_9._assignWork(_d.msg,_d.callback);
}
_c(_a);
};
var _e="function _workerInit(){"+"gearsWorkerPool.onmessage = "+String(this._workerHandler)+";"+"}";
var _f=_e+" _workerInit();";
for(var i=0;i<this._POOL_SIZE;i++){
this._unemployed.push("_"+this._manager.createWorker(_f));
}
}
catch(exp){
throw exp.message||exp;
}
}
},_assignWork:function(msg,_12){
if(!this._handleMessage.length&&this._unemployed.length){
var _13=this._unemployed.shift().substring(1);
this._employed["_"+_13]=_12;
this._manager.sendMessage(msg,parseInt(_13,10));
}else{
this._handleMessage={msg:msg,callback:_12};
}
},_workerHandler:function(msg,_15){
var _16=[99,124,119,123,242,107,111,197,48,1,103,43,254,215,171,118,202,130,201,125,250,89,71,240,173,212,162,175,156,164,114,192,183,253,147,38,54,63,247,204,52,165,229,241,113,216,49,21,4,199,35,195,24,150,5,154,7,18,128,226,235,39,178,117,9,131,44,26,27,110,90,160,82,59,214,179,41,227,47,132,83,209,0,237,32,252,177,91,106,203,190,57,74,76,88,207,208,239,170,251,67,77,51,133,69,249,2,127,80,60,159,168,81,163,64,143,146,157,56,245,188,182,218,33,16,255,243,210,205,12,19,236,95,151,68,23,196,167,126,61,100,93,25,115,96,129,79,220,34,42,144,136,70,238,184,20,222,94,11,219,224,50,58,10,73,6,36,92,194,211,172,98,145,149,228,121,231,200,55,109,141,213,78,169,108,86,244,234,101,122,174,8,186,120,37,46,28,166,180,198,232,221,116,31,75,189,139,138,112,62,181,102,72,3,246,14,97,53,87,185,134,193,29,158,225,248,152,17,105,217,142,148,155,30,135,233,206,85,40,223,140,161,137,13,191,230,66,104,65,153,45,15,176,84,187,22];
var _17=[[0,0,0,0],[1,0,0,0],[2,0,0,0],[4,0,0,0],[8,0,0,0],[16,0,0,0],[32,0,0,0],[64,0,0,0],[128,0,0,0],[27,0,0,0],[54,0,0,0]];
function Cipher(_18,w){
var Nb=4;
var Nr=w.length/Nb-1;
var _1c=[[],[],[],[]];
for(var i=0;i<4*Nb;i++){
_1c[i%4][Math.floor(i/4)]=_18[i];
}
_1c=AddRoundKey(_1c,w,0,Nb);
for(var _1e=1;_1e<Nr;_1e++){
_1c=SubBytes(_1c,Nb);
_1c=ShiftRows(_1c,Nb);
_1c=MixColumns(_1c,Nb);
_1c=AddRoundKey(_1c,w,_1e,Nb);
}
_1c=SubBytes(_1c,Nb);
_1c=ShiftRows(_1c,Nb);
_1c=AddRoundKey(_1c,w,Nr,Nb);
var _1f=new Array(4*Nb);
for(var i=0;i<4*Nb;i++){
_1f[i]=_1c[i%4][Math.floor(i/4)];
}
return _1f;
};
function SubBytes(s,Nb){
for(var r=0;r<4;r++){
for(var c=0;c<Nb;c++){
s[r][c]=_16[s[r][c]];
}
}
return s;
};
function ShiftRows(s,Nb){
var t=new Array(4);
for(var r=1;r<4;r++){
for(var c=0;c<4;c++){
t[c]=s[r][(c+r)%Nb];
}
for(var c=0;c<4;c++){
s[r][c]=t[c];
}
}
return s;
};
function MixColumns(s,Nb){
for(var c=0;c<4;c++){
var a=new Array(4);
var b=new Array(4);
for(var i=0;i<4;i++){
a[i]=s[i][c];
b[i]=s[i][c]&128?s[i][c]<<1^283:s[i][c]<<1;
}
s[0][c]=b[0]^a[1]^b[1]^a[2]^a[3];
s[1][c]=a[0]^b[1]^a[2]^b[2]^a[3];
s[2][c]=a[0]^a[1]^b[2]^a[3]^b[3];
s[3][c]=a[0]^b[0]^a[1]^a[2]^b[3];
}
return s;
};
function AddRoundKey(_2f,w,rnd,Nb){
for(var r=0;r<4;r++){
for(var c=0;c<Nb;c++){
_2f[r][c]^=w[rnd*4+c][r];
}
}
return _2f;
};
function KeyExpansion(key){
var Nb=4;
var Nk=key.length/4;
var Nr=Nk+6;
var w=new Array(Nb*(Nr+1));
var _3a=new Array(4);
for(var i=0;i<Nk;i++){
var r=[key[4*i],key[4*i+1],key[4*i+2],key[4*i+3]];
w[i]=r;
}
for(var i=Nk;i<(Nb*(Nr+1));i++){
w[i]=new Array(4);
for(var t=0;t<4;t++){
_3a[t]=w[i-1][t];
}
if(i%Nk==0){
_3a=SubWord(RotWord(_3a));
for(var t=0;t<4;t++){
_3a[t]^=_17[i/Nk][t];
}
}else{
if(Nk>6&&i%Nk==4){
_3a=SubWord(_3a);
}
}
for(var t=0;t<4;t++){
w[i][t]=w[i-Nk][t]^_3a[t];
}
}
return w;
};
function SubWord(w){
for(var i=0;i<4;i++){
w[i]=_16[w[i]];
}
return w;
};
function RotWord(w){
w[4]=w[0];
for(var i=0;i<4;i++){
w[i]=w[i+1];
}
return w;
};
function AESEncryptCtr(_42,_43,_44){
if(!(_44==128||_44==192||_44==256)){
return "";
}
var _45=_44/8;
var _46=new Array(_45);
for(var i=0;i<_45;i++){
_46[i]=_43.charCodeAt(i)&255;
}
var key=Cipher(_46,KeyExpansion(_46));
key=key.concat(key.slice(0,_45-16));
var _49=16;
var _4a=new Array(_49);
var _4b=(new Date()).getTime();
for(var i=0;i<4;i++){
_4a[i]=(_4b>>>i*8)&255;
}
for(var i=0;i<4;i++){
_4a[i+4]=(_4b/4294967296>>>i*8)&255;
}
var _4c=KeyExpansion(key);
var _4d=Math.ceil(_42.length/_49);
var _4e=new Array(_4d);
for(var b=0;b<_4d;b++){
for(var c=0;c<4;c++){
_4a[15-c]=(b>>>c*8)&255;
}
for(var c=0;c<4;c++){
_4a[15-c-4]=(b/4294967296>>>c*8);
}
var _51=Cipher(_4a,_4c);
var _52=b<_4d-1?_49:(_42.length-1)%_49+1;
var ct="";
for(var i=0;i<_52;i++){
var _54=_42.charCodeAt(b*_49+i);
var _55=_54^_51[i];
ct+=String.fromCharCode(_55);
}
_4e[b]=escCtrlChars(ct);
}
var _56="";
for(var i=0;i<8;i++){
_56+=String.fromCharCode(_4a[i]);
}
_56=escCtrlChars(_56);
return _56+"-"+_4e.join("-");
};
function AESDecryptCtr(_57,_58,_59){
if(!(_59==128||_59==192||_59==256)){
return "";
}
var _5a=_59/8;
var _5b=new Array(_5a);
for(var i=0;i<_5a;i++){
_5b[i]=_58.charCodeAt(i)&255;
}
var _5d=KeyExpansion(_5b);
var key=Cipher(_5b,_5d);
key=key.concat(key.slice(0,_5a-16));
var _5f=KeyExpansion(key);
_57=_57.split("-");
var _60=16;
var _61=new Array(_60);
var _62=unescCtrlChars(_57[0]);
for(var i=0;i<8;i++){
_61[i]=_62.charCodeAt(i);
}
var _63=new Array(_57.length-1);
for(var b=1;b<_57.length;b++){
for(var c=0;c<4;c++){
_61[15-c]=((b-1)>>>c*8)&255;
}
for(var c=0;c<4;c++){
_61[15-c-4]=((b/4294967296-1)>>>c*8)&255;
}
var _66=Cipher(_61,_5f);
_57[b]=unescCtrlChars(_57[b]);
var pt="";
for(var i=0;i<_57[b].length;i++){
var _68=_57[b].charCodeAt(i);
var _69=_68^_66[i];
pt+=String.fromCharCode(_69);
}
_63[b-1]=pt;
}
return _63.join("");
};
function escCtrlChars(str){
return str.replace(/[\0\t\n\v\f\r\xa0!-]/g,function(c){
return "!"+c.charCodeAt(0)+"!";
});
};
function unescCtrlChars(str){
return str.replace(/!\d\d?\d?!/g,function(c){
return String.fromCharCode(c.slice(1,-1));
});
};
function encrypt(_6e,_6f){
return AESEncryptCtr(_6e,_6f,256);
};
function decrypt(_70,_71){
return AESDecryptCtr(_70,_71,256);
};
var cmd=msg.substr(0,4);
var arg=msg.substr(5);
if(cmd=="encr"){
arg=eval("("+arg+")");
var _74=arg.plaintext;
var _75=arg.password;
var _76=encrypt(_74,_75);
gearsWorkerPool.sendMessage(String(_76),_15);
}else{
if(cmd=="decr"){
arg=eval("("+arg+")");
var _77=arg.ciphertext;
var _75=arg.password;
var _76=decrypt(_77,_75);
gearsWorkerPool.sendMessage(String(_76),_15);
}
}
}});
}
