/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.number"]){
dojo._hasResource["dojo.number"]=true;
dojo.provide("dojo.number");
dojo.require("dojo.i18n");
dojo.requireLocalization("dojo.cldr","number",null,"zh-cn,zh,ko-kr,pt,en-us,en-gb,de,ja,ja-jp,en,ROOT,en-au,fr,es,ko,zh-tw,it,es-es,de-de");
dojo.require("dojo.string");
dojo.require("dojo.regexp");
dojo.number.format=function(_1,_2){
_2=dojo.mixin({},_2||{});
var _3=dojo.i18n.normalizeLocale(_2.locale);
var _4=dojo.i18n.getLocalization("dojo.cldr","number",_3);
_2.customs=_4;
var _5=_2.pattern||_4[(_2.type||"decimal")+"Format"];
if(isNaN(_1)){
return null;
}
return dojo.number._applyPattern(_1,_5,_2);
};
dojo.number._numberPatternRE=/[#0,]*[#0](?:\.0*#*)?/;
dojo.number._applyPattern=function(_6,_7,_8){
_8=_8||{};
var _9=_8.customs.group;
var _a=_8.customs.decimal;
var _b=_7.split(";");
var _c=_b[0];
_7=_b[(_6<0)?1:0]||("-"+_c);
if(_7.indexOf("%")!=-1){
_6*=100;
}else{
if(_7.indexOf("‰")!=-1){
_6*=1000;
}else{
if(_7.indexOf("¤")!=-1){
_9=_8.customs.currencyGroup||_9;
_a=_8.customs.currencyDecimal||_a;
_7=_7.replace(/\u00a4{1,3}/,function(_d){
var _e=["symbol","currency","displayName"][_d.length-1];
return _8[_e]||_8.currency||"";
});
}else{
if(_7.indexOf("E")!=-1){
throw new Error("exponential notation not supported");
}
}
}
}
var _f=dojo.number._numberPatternRE;
var _10=_c.match(_f);
if(!_10){
throw new Error("unable to find a number expression in pattern: "+_7);
}
if(_8.fractional===false){
_8.places=0;
}
return _7.replace(_f,dojo.number._formatAbsolute(_6,_10[0],{decimal:_a,group:_9,places:_8.places,round:_8.round}));
};
dojo.number.round=function(_11,_12,_13){
var _14=String(_11).split(".");
var _15=(_14[1]&&_14[1].length)||0;
if(_15>_12){
var _16=Math.pow(10,_12);
if(_13>0){
_16*=10/_13;
_12++;
}
_11=Math.round(_11*_16)/_16;
_14=String(_11).split(".");
_15=(_14[1]&&_14[1].length)||0;
if(_15>_12){
_14[1]=_14[1].substr(0,_12);
_11=Number(_14.join("."));
}
}
return _11;
};
dojo.number._formatAbsolute=function(_17,_18,_19){
_19=_19||{};
if(_19.places===true){
_19.places=0;
}
if(_19.places===Infinity){
_19.places=6;
}
var _1a=_18.split(".");
var _1b=(_19.places>=0)?_19.places:(_1a[1]&&_1a[1].length)||0;
if(!(_19.round<0)){
_17=dojo.number.round(_17,_1b,_19.round);
}
var _1c=String(Math.abs(_17)).split(".");
var _1d=_1c[1]||"";
if(_19.places){
var _1e=dojo.isString(_19.places)&&_19.places.indexOf(",");
if(_1e){
_19.places=_19.places.substring(_1e+1);
}
_1c[1]=dojo.string.pad(_1d.substr(0,_19.places),_19.places,"0",true);
}else{
if(_1a[1]&&_19.places!==0){
var pad=_1a[1].lastIndexOf("0")+1;
if(pad>_1d.length){
_1c[1]=dojo.string.pad(_1d,pad,"0",true);
}
var _20=_1a[1].length;
if(_20<_1d.length){
_1c[1]=_1d.substr(0,_20);
}
}else{
if(_1c[1]){
_1c.pop();
}
}
}
var _21=_1a[0].replace(",","");
pad=_21.indexOf("0");
if(pad!=-1){
pad=_21.length-pad;
if(pad>_1c[0].length){
_1c[0]=dojo.string.pad(_1c[0],pad);
}
if(_21.indexOf("#")==-1){
_1c[0]=_1c[0].substr(_1c[0].length-pad);
}
}
var _22=_1a[0].lastIndexOf(",");
var _23,_24;
if(_22!=-1){
_23=_1a[0].length-_22-1;
var _25=_1a[0].substr(0,_22);
_22=_25.lastIndexOf(",");
if(_22!=-1){
_24=_25.length-_22-1;
}
}
var _26=[];
for(var _27=_1c[0];_27;){
var off=_27.length-_23;
_26.push((off>0)?_27.substr(off):_27);
_27=(off>0)?_27.slice(0,off):"";
if(_24){
_23=_24;
delete _24;
}
}
_1c[0]=_26.reverse().join(_19.group||",");
return _1c.join(_19.decimal||".");
};
dojo.number.regexp=function(_29){
return dojo.number._parseInfo(_29).regexp;
};
dojo.number._parseInfo=function(_2a){
_2a=_2a||{};
var _2b=dojo.i18n.normalizeLocale(_2a.locale);
var _2c=dojo.i18n.getLocalization("dojo.cldr","number",_2b);
var _2d=_2a.pattern||_2c[(_2a.type||"decimal")+"Format"];
var _2e=_2c.group;
var _2f=_2c.decimal;
var _30=1;
if(_2d.indexOf("%")!=-1){
_30/=100;
}else{
if(_2d.indexOf("‰")!=-1){
_30/=1000;
}else{
var _31=_2d.indexOf("¤")!=-1;
if(_31){
_2e=_2c.currencyGroup||_2e;
_2f=_2c.currencyDecimal||_2f;
}
}
}
var _32=_2d.split(";");
if(_32.length==1){
_32.push("-"+_32[0]);
}
var re=dojo.regexp.buildGroupRE(_32,function(_34){
_34="(?:"+dojo.regexp.escapeString(_34,".")+")";
return _34.replace(dojo.number._numberPatternRE,function(_35){
var _36={signed:false,separator:_2a.strict?_2e:[_2e,""],fractional:_2a.fractional,decimal:_2f,exponent:false};
var _37=_35.split(".");
var _38=_2a.places;
if(_37.length==1||_38===0){
_36.fractional=false;
}else{
if(_38===undefined){
_38=_2a.pattern?_37[1].lastIndexOf("0")+1:Infinity;
}
if(_38&&_2a.fractional==undefined){
_36.fractional=true;
}
if(!_2a.places&&(_38<_37[1].length)){
_38+=","+_37[1].length;
}
_36.places=_38;
}
var _39=_37[0].split(",");
if(_39.length>1){
_36.groupSize=_39.pop().length;
if(_39.length>1){
_36.groupSize2=_39.pop().length;
}
}
return "("+dojo.number._realNumberRegexp(_36)+")";
});
},true);
if(_31){
re=re.replace(/([\s\xa0]*)(\u00a4{1,3})([\s\xa0]*)/g,function(_3a,_3b,_3c,_3d){
var _3e=["symbol","currency","displayName"][_3c.length-1];
var _3f=dojo.regexp.escapeString(_2a[_3e]||_2a.currency||"");
_3b=_3b?"[\\s\\xa0]":"";
_3d=_3d?"[\\s\\xa0]":"";
if(!_2a.strict){
if(_3b){
_3b+="*";
}
if(_3d){
_3d+="*";
}
return "(?:"+_3b+_3f+_3d+")?";
}
return _3b+_3f+_3d;
});
}
return {regexp:re.replace(/[\xa0 ]/g,"[\\s\\xa0]"),group:_2e,decimal:_2f,factor:_30};
};
dojo.number.parse=function(_40,_41){
var _42=dojo.number._parseInfo(_41);
var _43=(new RegExp("^"+_42.regexp+"$")).exec(_40);
if(!_43){
return NaN;
}
var _44=_43[1];
if(!_43[1]){
if(!_43[2]){
return NaN;
}
_44=_43[2];
_42.factor*=-1;
}
_44=_44.replace(new RegExp("["+_42.group+"\\s\\xa0"+"]","g"),"").replace(_42.decimal,".");
return Number(_44)*_42.factor;
};
dojo.number._realNumberRegexp=function(_45){
_45=_45||{};
if(!("places" in _45)){
_45.places=Infinity;
}
if(typeof _45.decimal!="string"){
_45.decimal=".";
}
if(!("fractional" in _45)||/^0/.test(_45.places)){
_45.fractional=[true,false];
}
if(!("exponent" in _45)){
_45.exponent=[true,false];
}
if(!("eSigned" in _45)){
_45.eSigned=[true,false];
}
var _46=dojo.number._integerRegexp(_45);
var _47=dojo.regexp.buildGroupRE(_45.fractional,function(q){
var re="";
if(q&&(_45.places!==0)){
re="\\"+_45.decimal;
if(_45.places==Infinity){
re="(?:"+re+"\\d+)?";
}else{
re+="\\d{"+_45.places+"}";
}
}
return re;
},true);
var _4a=dojo.regexp.buildGroupRE(_45.exponent,function(q){
if(q){
return "([eE]"+dojo.number._integerRegexp({signed:_45.eSigned})+")";
}
return "";
});
var _4c=_46+_47;
if(_47){
_4c="(?:(?:"+_4c+")|(?:"+_47+"))";
}
return _4c+_4a;
};
dojo.number._integerRegexp=function(_4d){
_4d=_4d||{};
if(!("signed" in _4d)){
_4d.signed=[true,false];
}
if(!("separator" in _4d)){
_4d.separator="";
}else{
if(!("groupSize" in _4d)){
_4d.groupSize=3;
}
}
var _4e=dojo.regexp.buildGroupRE(_4d.signed,function(q){
return q?"[-+]":"";
},true);
var _50=dojo.regexp.buildGroupRE(_4d.separator,function(sep){
if(!sep){
return "(?:0|[1-9]\\d*)";
}
sep=dojo.regexp.escapeString(sep);
if(sep==" "){
sep="\\s";
}else{
if(sep==" "){
sep="\\s\\xa0";
}
}
var grp=_4d.groupSize,_53=_4d.groupSize2;
if(_53){
var _54="(?:0|[1-9]\\d{0,"+(_53-1)+"}(?:["+sep+"]\\d{"+_53+"})*["+sep+"]\\d{"+grp+"})";
return ((grp-_53)>0)?"(?:"+_54+"|(?:0|[1-9]\\d{0,"+(grp-1)+"}))":_54;
}
return "(?:0|[1-9]\\d{0,"+(grp-1)+"}(?:["+sep+"]\\d{"+grp+"})*)";
},true);
return _4e+_50;
};
}
