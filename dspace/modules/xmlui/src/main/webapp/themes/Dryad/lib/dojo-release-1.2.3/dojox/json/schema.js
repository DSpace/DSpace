/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.json.schema"]){
dojo._hasResource["dojox.json.schema"]=true;
dojo.provide("dojox.json.schema");
dojox.json.schema.validate=function(_1,_2){
return this._validate(_1,_2,false);
};
dojox.json.schema.checkPropertyChange=function(_3,_4){
return this._validate(_3,_4,true);
};
dojox.json.schema._validate=function(_5,_6,_7){
var _8=[];
function checkProp(_9,_a,_b,i){
if(typeof _a!="object"){
return null;
}
_b+=_b?typeof i=="number"?"["+i+"]":typeof i=="undefined"?"":"."+i:i;
function addError(_d){
_8.push({property:_b,message:_d});
};
if(_7&&_a.readonly){
addError("is a readonly field, it can not be changed");
}
if(_a instanceof Array){
if(!(_9 instanceof Array)){
return [{property:_b,message:"An array tuple is required"}];
}
for(i=0;i<_a.length;i++){
_8.concat(checkProp(_9[i],_a[i],_b,i));
}
return _8;
}
if(_a["extends"]){
checkProp(_9,_a["extends"],_b,i);
}
function checkType(_e,_f){
if(_e){
if(typeof _e=="string"&&_e!="any"&&(_e=="null"?_f!==null:typeof _f!=_e)&&!(_f instanceof Array&&_e=="array")&&!(_e=="integer"&&!(_f%1))){
return [{property:_b,message:(typeof _f)+" value found, but a "+_e+" is required"}];
}
if(_e instanceof Array){
var _10=[];
for(var j=0;j<_e.length;j++){
if(!(_10=checkType(_e[j],_f)).length){
break;
}
}
if(_10.length){
return _10;
}
}else{
if(typeof _e=="object"){
checkProp(_f,_e,_b);
}
}
}
return [];
};
if(_9!==null){
if(_9===undefined){
if(!_a.optional){
addError("is missing and it is not optional");
}
}else{
_8=_8.concat(checkType(_a.type,_9));
if(_a.disallow&&!checkType(_a.disallow,_9).length){
addError(" disallowed value was matched");
}
if(_9 instanceof Array){
if(_a.items){
for(i=0,l=_9.length;i<l;i++){
_8.concat(checkProp(_9[i],_a.items,_b,i));
}
}
if(_a.minItems&&_9.length<_a.minItems){
addError("There must be a minimum of "+_a.minItems+" in the array");
}
if(_a.maxItems&&_9.length>_a.maxItems){
addError("There must be a maximum of "+_a.maxItems+" in the array");
}
}else{
if(_a.properties&&typeof _9=="object"){
_8.concat(checkObj(_9,_a.properties,_b,_a.additionalProperties));
}
}
if(_a.pattern&&typeof _9=="string"&&!_9.match(_a.pattern)){
addError("does not match the regex pattern "+_a.pattern);
}
if(_a.maxLength&&typeof _9=="string"&&_9.length>_a.maxLength){
addError("may only be "+_a.maxLength+" characters long");
}
if(_a.minLength&&typeof _9=="string"&&_9.length<_a.minLength){
addError("must be at least "+_a.minLength+" characters long");
}
if(typeof _a.minimum!==undefined&&typeof _9==typeof _a.minimum&&_a.minimum>_9){
addError("must have a minimum value of "+_a.minimum);
}
if(typeof _a.maximum!==undefined&&typeof _9==typeof _a.maximum&&_a.maximum<_9){
addError("must have a maximum value of "+_a.maximum);
}
if(_a["enum"]){
var _12=_a["enum"];
l=_12.length;
var _13;
for(var j=0;j<l;j++){
if(_12[j]===_9){
_13=1;
break;
}
}
if(!_13){
addError("does not have a value in the enumeration "+_12.join(", "));
}
}
if(typeof _a.maxDecimal=="number"&&(_9*10^_a.maxDecimal)%1){
addError("may only have "+_a.maxDecimal+" digits of decimal places");
}
}
}
return null;
};
function checkObj(_15,_16,_17,_18){
if(typeof _16=="object"){
if(typeof _15!="object"||_15 instanceof Array){
_8.push({property:_17,message:"an object is required"});
}
for(var i in _16){
if(_16.hasOwnProperty(i)){
var _1a=_15[i];
var _1b=_16[i];
checkProp(_1a,_1b,_17,i);
}
}
}
for(i in _15){
if(_15.hasOwnProperty(i)&&_16&&!_16[i]&&_18===false){
_8.push({property:_17,message:(typeof _1a)+"The property "+i+" is not defined in the objTypeDef and the objTypeDef does not allow additional properties"});
}
var _1c=_16&&_16[i]&&_16[i].requires;
if(_1c&&!(_1c in _15)){
_8.push({property:_17,message:"the presence of the property "+i+" requires that "+_1c+" also be present"});
}
_1a=_15[i];
if(_16&&typeof _16=="object"&&!(i in _16)){
checkProp(_1a,_18,_17,i);
}
if(!_7&&_1a&&_1a.$schema){
_8=_8.concat(checkProp(_1a,_1a.$schema,_17,i));
}
}
return _8;
};
if(_6){
checkProp(_5,_6,"","");
}
if(!_7&&_5.$schema){
checkProp(_5,_5.$schema,"","");
}
return {valid:!_8.length,errors:_8};
};
}
