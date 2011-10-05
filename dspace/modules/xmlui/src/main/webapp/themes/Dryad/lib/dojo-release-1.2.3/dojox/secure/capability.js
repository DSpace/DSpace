/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.secure.capability"]){
dojo._hasResource["dojox.secure.capability"]=true;
dojo.provide("dojox.secure.capability");
dojox.secure.badProps=/^__|^(apply|call|callee|caller|constructor|eval|prototype|this|unwatch|valueOf|watch)$|__$/;
dojox.secure.capability={keywords:["break","case","catch","const","continue","debugger","default","delete","do","else","enum","false","finally","for","function","if","in","instanceof","new","null","yield","return","switch","throw","true","try","typeof","var","void","while"],validate:function(_1,_2,_3){
var _4=this.keywords;
for(var i=0;i<_4.length;i++){
_3[_4[i]]=true;
}
var _6="|this| keyword in object literal without a Class call";
var _7=[];
if(_1.match(/[\u200c-\u200f\u202a-\u202e\u206a-\u206f\uff00-\uffff]/)){
throw new Error("Illegal unicode characters detected");
}
if(_1.match(/\/\*@cc_on/)){
throw new Error("Conditional compilation token is not allowed");
}
_1=_1.replace(/\\["'\\\/bfnrtu]/g,"@").replace(/\/\/.*|\/\*[\w\W]*?\*\/|\/(\\[\/\\]|[^*\/])(\\.|[^\/\n\\])*\/[gim]*|("[^"]*")|('[^']*')/g,function(t){
return t.match(/^\/\/|^\/\*/)?" ":"0";
}).replace(/\.\s*([a-z\$_A-Z][\w\$_]*)|([;,{])\s*([a-z\$_A-Z][\w\$_]*\s*):/g,function(t,_a,_b,_c){
_a=_a||_c;
if(/^__|^(apply|call|callee|caller|constructor|eval|prototype|this|unwatch|valueOf|watch)$|__$/.test(_a)){
throw new Error("Illegal property name "+_a);
}
return (_b&&(_b+"0:"))||"~";
});
_1.replace(/([^\[][\]\}]\s*=)|((\Wreturn|\S)\s*\[\s*\+?)|([^=!][=!]=[^=])/g,function(_d){
if(!_d.match(/((\Wreturn|[=\&\|\:\?\,])\s*\[)|\[\s*\+$/)){
throw new Error("Illegal operator "+_d.substring(1));
}
});
_1=_1.replace(new RegExp("("+_2.join("|")+")[\\s~]*\\(","g"),function(_e){
return "new(";
});
function findOuterRefs(_f,_10){
var _11={};
_f.replace(/#\d/g,function(b){
var _13=_7[b.substring(1)];
for(var i in _13){
if(i==_6){
throw i;
}
if(i=="this"&&_13[":method"]&&_13["this"]==1){
i=_6;
}
if(i!=":method"){
_11[i]=2;
}
}
});
_f.replace(/(\W|^)([a-z_\$A-Z][\w_\$]*)/g,function(t,a,_17){
if(_17.charAt(0)=="_"){
throw new Error("Names may not start with _");
}
_11[_17]=1;
});
return _11;
};
var _18,_19;
function parseBlock(t,_1b,a,b,_1e,_1f){
_1f.replace(/(^|,)0:\s*function#(\d)/g,function(t,a,b){
var _23=_7[b];
_23[":method"]=1;
});
_1f=_1f.replace(/(^|[^_\w\$])Class\s*\(\s*([_\w\$]+\s*,\s*)*#(\d)/g,function(t,p,a,b){
var _28=_7[b];
delete _28[_6];
return (p||"")+(a||"")+"#"+b;
});
_19=findOuterRefs(_1f,_1b);
function parseVars(t,a,b,_2c){
_2c.replace(/,?([a-z\$A-Z][_\w\$]*)/g,function(t,_2e){
if(_2e=="Class"){
throw new Error("Class is reserved");
}
delete _19[_2e];
});
};
if(_1b){
parseVars(t,a,a,_1e);
}
_1f.replace(/(\W|^)(var) ([ \t,_\w\$]+)/g,parseVars);
return (a||"")+(b||"")+"#"+(_7.push(_19)-1);
};
do{
_18=_1.replace(/((function|catch)(\s+[_\w\$]+)?\s*\(([^\)]*)\)\s*)?{([^{}]*)}/g,parseBlock);
}while(_18!=_1&&(_1=_18));
parseBlock(0,0,0,0,0,_1);
for(i in _19){
if(!(i in _3)){
throw new Error("Illegal reference to "+i);
}
}
}};
}
