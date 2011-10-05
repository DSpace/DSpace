/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.json.query"]){
dojo._hasResource["dojox.json.query"]=true;
dojo.provide("dojox.json.query");
(function(){
function slice(_1,_2,_3,_4){
var _5=_1.length,_6=[];
_3=_3||_5;
_2=(_2<0)?Math.max(0,_2+_5):Math.min(_5,_2);
_3=(_3<0)?Math.max(0,_3+_5):Math.min(_5,_3);
for(var i=_2;i<_3;i+=_4){
_6.push(_1[i]);
}
return _6;
};
function expand(_8,_9){
var _a=[];
function walk(_b){
if(_9){
if(_9===true&&!(_b instanceof Array)){
_a.push(_b);
}else{
if(_b[_9]){
_a.push(_b[_9]);
}
}
}
for(var i in _b){
var _d=_b[i];
if(!_9){
_a.push(_d);
}else{
if(_d&&typeof _d=="object"){
walk(_d);
}
}
}
};
if(_9 instanceof Array){
if(_9.length==1){
return _8[_9[0]];
}
for(var i=0;i<_9.length;i++){
_a.push(_8[_9[i]]);
}
}else{
walk(_8);
}
return _a;
};
dojox.json.query=function(_f,obj){
tokens=[];
var _11=0;
var str=[];
_f=_f.replace(/"(\\.|[^"\\])*"|'(\\.|[^'\\])*'|[\[\]]/g,function(t){
_11+=t=="["?1:t=="]"?-1:0;
return (t=="]"&&_11>0)?"`]":(t.charAt(0)=="\""||t.charAt(0)=="'")?"`"+(str.push(t)-1):t;
});
var _14="";
function call(_15){
_14=_15+"("+_14;
};
function makeRegex(t,a,b,c,d){
return str[d].match(/[\*\?]/)?"/"+str[d].substring(1,str[d].length-1).replace(/\\([btnfr\\"'])|([^\w\*\?])/g,"\\$1$2").replace(/([\*\?])/g,".$1")+(c=="~"?"/i":"/")+".test("+a+")":t;
};
_f.replace(/(\]|\)|push|pop|shift|splice|sort|reverse)\s*\(/,function(){
throw new Error("Unsafe function call");
});
_f=_f.replace(/([^=]=)([^=])/g,"$1=$2").replace(/@|(\.\s*)?[a-zA-Z\$_]+(\s*:)?/g,function(t){
return t.charAt(0)=="."?t:t=="@"?"$obj":(t.match(/:|^(\$|Math)$/)?"":"$obj.")+t;
}).replace(/\.?\.?\[(`\]|[^\]])*\]|\?.*|\.\.([\w\$_]+)|\.\*/g,function(t,a,b){
var _1f=t.match(/^\.?\.?(\[\s*\?|\?|\[\s*==)(.*?)\]?$/);
if(_1f){
var _20="";
if(t.match(/^\./)){
call("expand");
_20=",true)";
}
call(_1f[1].match(/\=/)?"dojo.map":"dojo.filter");
return _20+",function($obj){return "+_1f[2]+"})";
}
_1f=t.match(/^\[\s*([\/\\].*)\]/);
if(_1f){
return ".concat().sort(function(a,b){"+_1f[1].replace(/\s*,?\s*([\/\\])\s*([^,\\\/]+)/g,function(t,a,b){
return "var av= "+b.replace(/\$obj/,"a")+",bv= "+b.replace(/\$obj/,"b")+";if(av>bv||bv==null){return "+(a=="/"?1:-1)+";}\n"+"if(bv>av||av==null){return "+(a=="/"?-1:1)+";}\n";
})+"})";
}
_1f=t.match(/^\[(-?[0-9]*):(-?[0-9]*):?(-?[0-9]*)\]/);
if(_1f){
call("slice");
return ","+(_1f[1]||0)+","+(_1f[2]||0)+","+(_1f[3]||1)+")";
}
if(t.match(/^\.\.|\.\*|\[\s*\*\s*\]|,/)){
call("expand");
return (t.charAt(1)=="."?",'"+b+"'":t.match(/,/)?","+t:"")+")";
}
return t;
}).replace(/(\$obj\s*(\.\s*[\w_$]+\s*)*)(==|~)\s*`([0-9]+)/g,makeRegex).replace(/`([0-9]+)\s*(==|~)\s*(\$obj(\s*\.\s*[\w_$]+)*)/g,function(t,a,b,c,d){
return makeRegex(t,c,d,b,a);
});
_f=_14+(_f.charAt(0)=="$"?"":"$")+_f.replace(/`([0-9]+|\])/g,function(t,a){
return a=="]"?"]":str[a];
});
var _2b=eval("1&&function($,$1,$2,$3,$4,$5,$6,$7,$8,$9){var $obj=$;return "+_f+"}");
for(var i=0;i<arguments.length-1;i++){
arguments[i]=arguments[i+1];
}
return obj?_2b.apply(this,arguments):_2b;
};
})();
}
