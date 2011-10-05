/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.jsonPath.query"]){
dojo._hasResource["dojox.jsonPath.query"]=true;
dojo.provide("dojox.jsonPath.query");
dojox.jsonPath.query=function(_1,_2,_3){
var re=dojox.jsonPath._regularExpressions;
if(!_3){
_3={};
}
var _5=[];
function _str(i){
return _5[i];
};
var _7;
if(_3.resultType=="PATH"&&_3.evalType=="RESULT"){
throw Error("RESULT based evaluation not supported with PATH based results");
}
var P={resultType:_3.resultType||"VALUE",normalize:function(_9){
var _a=[];
_9=_9.replace(/'([^']|'')*'/g,function(t){
return "_str("+(_5.push(eval(t))-1)+")";
});
var ll=-1;
while(ll!=_a.length){
ll=_a.length;
_9=_9.replace(/(\??\([^\(\)]*\))/g,function($0){
return "#"+(_a.push($0)-1);
});
}
_9=_9.replace(/[\['](#[0-9]+)[\]']/g,"[$1]").replace(/'?\.'?|\['?/g,";").replace(/;;;|;;/g,";..;").replace(/;$|'?\]|'$/g,"");
ll=-1;
while(ll!=_9){
ll=_9;
_9=_9.replace(/#([0-9]+)/g,function($0,$1){
return _a[$1];
});
}
return _9.split(";");
},asPaths:function(_10){
for(var j=0;j<_10.length;j++){
var p="$";
var x=_10[j];
for(var i=1,n=x.length;i<n;i++){
p+=/^[0-9*]+$/.test(x[i])?("["+x[i]+"]"):("['"+x[i]+"']");
}
_10[j]=p;
}
return _10;
},exec:function(_16,val,rb){
var _19=["$"];
var _1a=rb?val:[val];
var _1b=[_19];
function add(v,p,def){
if(v&&v.hasOwnProperty(p)&&P.resultType!="VALUE"){
_1b.push(_19.concat([p]));
}
if(def){
_1a=v[p];
}else{
if(v&&v.hasOwnProperty(p)){
_1a.push(v[p]);
}
}
};
function desc(v){
_1a.push(v);
_1b.push(_19);
P.walk(v,function(i){
if(typeof v[i]==="object"){
var _21=_19;
_19=_19.concat(i);
desc(v[i]);
_19=_21;
}
});
};
function slice(loc,val){
if(val instanceof Array){
var len=val.length,_25=0,end=len,_27=1;
loc.replace(/^(-?[0-9]*):(-?[0-9]*):?(-?[0-9]*)$/g,function($0,$1,$2,$3){
_25=parseInt($1||_25);
end=parseInt($2||end);
_27=parseInt($3||_27);
});
_25=(_25<0)?Math.max(0,_25+len):Math.min(len,_25);
end=(end<0)?Math.max(0,end+len):Math.min(len,end);
for(var i=_25;i<end;i+=_27){
add(val,i);
}
}
};
function repStr(str){
var i=loc.match(/^_str\(([0-9]+)\)$/);
return i?_5[i[1]]:str;
};
function oper(val){
if(/^\(.*?\)$/.test(loc)){
add(val,P.eval(loc,val),rb);
}else{
if(loc==="*"){
P.walk(val,rb&&val instanceof Array?function(i){
P.walk(val[i],function(j){
add(val[i],j);
});
}:function(i){
add(val,i);
});
}else{
if(loc===".."){
desc(val);
}else{
if(/,/.test(loc)){
for(var s=loc.split(/'?,'?/),i=0,n=s.length;i<n;i++){
add(val,repStr(s[i]));
}
}else{
if(/^\?\(.*?\)$/.test(loc)){
P.walk(val,function(i){
if(P.eval(loc.replace(/^\?\((.*?)\)$/,"$1"),val[i])){
add(val,i);
}
});
}else{
if(/^(-?[0-9]*):(-?[0-9]*):?([0-9]*)$/.test(loc)){
slice(loc,val);
}else{
loc=repStr(loc);
if(rb&&val instanceof Array&&!/^[0-9*]+$/.test(loc)){
P.walk(val,function(i){
add(val[i],loc);
});
}else{
add(val,loc,rb);
}
}
}
}
}
}
}
};
while(_16.length){
var loc=_16.shift();
if((val=_1a)===null||val===undefined){
return val;
}
_1a=[];
var _39=_1b;
_1b=[];
if(rb){
oper(val);
}else{
P.walk(val,function(i){
_19=_39[i]||_19;
oper(val[i]);
});
}
}
if(P.resultType=="BOTH"){
_1b=P.asPaths(_1b);
var _3b=[];
for(var i=0;i<_1b.length;i++){
_3b.push({path:_1b[i],value:_1a[i]});
}
return _3b;
}
return P.resultType=="PATH"?P.asPaths(_1b):_1a;
},walk:function(val,f){
if(val instanceof Array){
for(var i=0,n=val.length;i<n;i++){
if(i in val){
f(i);
}
}
}else{
if(typeof val==="object"){
for(var m in val){
if(val.hasOwnProperty(m)){
f(m);
}
}
}
}
},eval:function(x,_v){
try{
return $&&_v&&eval(x.replace(/@/g,"_v"));
}
catch(e){
throw new SyntaxError("jsonPath: "+e.message+": "+x.replace(/@/g,"_v").replace(/\^/g,"_a"));
}
}};
var $=_1;
if(_2&&_1){
return P.exec(P.normalize(_2).slice(1),_1,_3.evalType=="RESULT");
}
return false;
};
}
