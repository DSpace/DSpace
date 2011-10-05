/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.date.locale"]){
dojo._hasResource["dojo.date.locale"]=true;
dojo.provide("dojo.date.locale");
dojo.require("dojo.date");
dojo.require("dojo.cldr.supplemental");
dojo.require("dojo.regexp");
dojo.require("dojo.string");
dojo.require("dojo.i18n");
dojo.requireLocalization("dojo.cldr","gregorian",null,"zh-cn,zh,en-ca,ko-kr,pt,pt-br,it-it,ROOT,en-gb,de,ja,en,en-au,fr,es,ko,zh-tw,it,es-es");
(function(){
function formatPattern(_1,_2,_3,_4){
return _4.replace(/([a-z])\1*/ig,function(_5){
var s,_7;
var c=_5.charAt(0);
var l=_5.length;
var _a=["abbr","wide","narrow"];
switch(c){
case "G":
s=_2[(l<4)?"eraAbbr":"eraNames"][_1.getFullYear()<0?0:1];
break;
case "y":
s=_1.getFullYear();
switch(l){
case 1:
break;
case 2:
if(!_3){
s=String(s);
s=s.substr(s.length-2);
break;
}
default:
_7=true;
}
break;
case "Q":
case "q":
s=Math.ceil((_1.getMonth()+1)/3);
_7=true;
break;
case "M":
var m=_1.getMonth();
if(l<3){
s=m+1;
_7=true;
}else{
var _c=["months","format",_a[l-3]].join("-");
s=_2[_c][m];
}
break;
case "w":
var _d=0;
s=dojo.date.locale._getWeekOfYear(_1,_d);
_7=true;
break;
case "d":
s=_1.getDate();
_7=true;
break;
case "D":
s=dojo.date.locale._getDayOfYear(_1);
_7=true;
break;
case "E":
var d=_1.getDay();
if(l<3){
s=d+1;
_7=true;
}else{
var _f=["days","format",_a[l-3]].join("-");
s=_2[_f][d];
}
break;
case "a":
var _10=(_1.getHours()<12)?"am":"pm";
s=_2[_10];
break;
case "h":
case "H":
case "K":
case "k":
var h=_1.getHours();
switch(c){
case "h":
s=(h%12)||12;
break;
case "H":
s=h;
break;
case "K":
s=(h%12);
break;
case "k":
s=h||24;
break;
}
_7=true;
break;
case "m":
s=_1.getMinutes();
_7=true;
break;
case "s":
s=_1.getSeconds();
_7=true;
break;
case "S":
s=Math.round(_1.getMilliseconds()*Math.pow(10,l-3));
_7=true;
break;
case "v":
case "z":
s=dojo.date.getTimezoneName(_1);
if(s){
break;
}
l=4;
case "Z":
var _12=_1.getTimezoneOffset();
var tz=[(_12<=0?"+":"-"),dojo.string.pad(Math.floor(Math.abs(_12)/60),2),dojo.string.pad(Math.abs(_12)%60,2)];
if(l==4){
tz.splice(0,0,"GMT");
tz.splice(3,0,":");
}
s=tz.join("");
break;
default:
throw new Error("dojo.date.locale.format: invalid pattern char: "+_4);
}
if(_7){
s=dojo.string.pad(s,l);
}
return s;
});
};
dojo.date.locale.format=function(_14,_15){
_15=_15||{};
var _16=dojo.i18n.normalizeLocale(_15.locale);
var _17=_15.formatLength||"short";
var _18=dojo.date.locale._getGregorianBundle(_16);
var str=[];
var _1a=dojo.hitch(this,formatPattern,_14,_18,_15.fullYear);
if(_15.selector=="year"){
var _1b=_14.getFullYear();
if(_16.match(/^zh|^ja/)){
_1b+="年";
}
return _1b;
}
if(_15.selector!="time"){
var _1c=_15.datePattern||_18["dateFormat-"+_17];
if(_1c){
str.push(_processPattern(_1c,_1a));
}
}
if(_15.selector!="date"){
var _1d=_15.timePattern||_18["timeFormat-"+_17];
if(_1d){
str.push(_processPattern(_1d,_1a));
}
}
var _1e=str.join(" ");
return _1e;
};
dojo.date.locale.regexp=function(_1f){
return dojo.date.locale._parseInfo(_1f).regexp;
};
dojo.date.locale._parseInfo=function(_20){
_20=_20||{};
var _21=dojo.i18n.normalizeLocale(_20.locale);
var _22=dojo.date.locale._getGregorianBundle(_21);
var _23=_20.formatLength||"short";
var _24=_20.datePattern||_22["dateFormat-"+_23];
var _25=_20.timePattern||_22["timeFormat-"+_23];
var _26;
if(_20.selector=="date"){
_26=_24;
}else{
if(_20.selector=="time"){
_26=_25;
}else{
_26=_24+" "+_25;
}
}
var _27=[];
var re=_processPattern(_26,dojo.hitch(this,_buildDateTimeRE,_27,_22,_20));
return {regexp:re,tokens:_27,bundle:_22};
};
dojo.date.locale.parse=function(_29,_2a){
var _2b=dojo.date.locale._parseInfo(_2a);
var _2c=_2b.tokens,_2d=_2b.bundle;
var re=new RegExp("^"+_2b.regexp+"$",_2b.strict?"":"i");
var _2f=re.exec(_29);
if(!_2f){
return null;
}
var _30=["abbr","wide","narrow"];
var _31=[1970,0,1,0,0,0,0];
var _32="";
var _33=dojo.every(_2f,function(v,i){
if(!i){
return true;
}
var _36=_2c[i-1];
var l=_36.length;
switch(_36.charAt(0)){
case "y":
if(l!=2&&_2a.strict){
_31[0]=v;
}else{
if(v<100){
v=Number(v);
var _38=""+new Date().getFullYear();
var _39=_38.substring(0,2)*100;
var _3a=Math.min(Number(_38.substring(2,4))+20,99);
var num=(v<_3a)?_39+v:_39-100+v;
_31[0]=num;
}else{
if(_2a.strict){
return false;
}
_31[0]=v;
}
}
break;
case "M":
if(l>2){
var _3c=_2d["months-format-"+_30[l-3]].concat();
if(!_2a.strict){
v=v.replace(".","").toLowerCase();
_3c=dojo.map(_3c,function(s){
return s.replace(".","").toLowerCase();
});
}
v=dojo.indexOf(_3c,v);
if(v==-1){
return false;
}
}else{
v--;
}
_31[1]=v;
break;
case "E":
case "e":
var _3e=_2d["days-format-"+_30[l-3]].concat();
if(!_2a.strict){
v=v.toLowerCase();
_3e=dojo.map(_3e,function(d){
return d.toLowerCase();
});
}
v=dojo.indexOf(_3e,v);
if(v==-1){
return false;
}
break;
case "D":
_31[1]=0;
case "d":
_31[2]=v;
break;
case "a":
var am=_2a.am||_2d.am;
var pm=_2a.pm||_2d.pm;
if(!_2a.strict){
var _42=/\./g;
v=v.replace(_42,"").toLowerCase();
am=am.replace(_42,"").toLowerCase();
pm=pm.replace(_42,"").toLowerCase();
}
if(_2a.strict&&v!=am&&v!=pm){
return false;
}
_32=(v==pm)?"p":(v==am)?"a":"";
break;
case "K":
if(v==24){
v=0;
}
case "h":
case "H":
case "k":
if(v>23){
return false;
}
_31[3]=v;
break;
case "m":
_31[4]=v;
break;
case "s":
_31[5]=v;
break;
case "S":
_31[6]=v;
}
return true;
});
var _43=+_31[3];
if(_32==="p"&&_43<12){
_31[3]=_43+12;
}else{
if(_32==="a"&&_43==12){
_31[3]=0;
}
}
var _44=new Date(_31[0],_31[1],_31[2],_31[3],_31[4],_31[5],_31[6]);
if(_2a.strict){
_44.setFullYear(_31[0]);
}
var _45=_2c.join("");
if(!_33||(_45.indexOf("M")!=-1&&_44.getMonth()!=_31[1])||(_45.indexOf("d")!=-1&&_44.getDate()!=_31[2])){
return null;
}
return _44;
};
function _processPattern(_46,_47,_48,_49){
var _4a=function(x){
return x;
};
_47=_47||_4a;
_48=_48||_4a;
_49=_49||_4a;
var _4c=_46.match(/(''|[^'])+/g);
var _4d=_46.charAt(0)=="'";
dojo.forEach(_4c,function(_4e,i){
if(!_4e){
_4c[i]="";
}else{
_4c[i]=(_4d?_48:_47)(_4e);
_4d=!_4d;
}
});
return _49(_4c.join(""));
};
function _buildDateTimeRE(_50,_51,_52,_53){
_53=dojo.regexp.escapeString(_53);
if(!_52.strict){
_53=_53.replace(" a"," ?a");
}
return _53.replace(/([a-z])\1*/ig,function(_54){
var s;
var c=_54.charAt(0);
var l=_54.length;
var p2="",p3="";
if(_52.strict){
if(l>1){
p2="0"+"{"+(l-1)+"}";
}
if(l>2){
p3="0"+"{"+(l-2)+"}";
}
}else{
p2="0?";
p3="0{0,2}";
}
switch(c){
case "y":
s="\\d{2,4}";
break;
case "M":
s=(l>2)?"\\S+?":p2+"[1-9]|1[0-2]";
break;
case "D":
s=p2+"[1-9]|"+p3+"[1-9][0-9]|[12][0-9][0-9]|3[0-5][0-9]|36[0-6]";
break;
case "d":
s="[12]\\d|"+p2+"[1-9]|3[01]";
break;
case "w":
s=p2+"[1-9]|[1-4][0-9]|5[0-3]";
break;
case "E":
s="\\S+";
break;
case "h":
s=p2+"[1-9]|1[0-2]";
break;
case "k":
s=p2+"\\d|1[01]";
break;
case "H":
s=p2+"\\d|1\\d|2[0-3]";
break;
case "K":
s=p2+"[1-9]|1\\d|2[0-4]";
break;
case "m":
case "s":
s="[0-5]\\d";
break;
case "S":
s="\\d{"+l+"}";
break;
case "a":
var am=_52.am||_51.am||"AM";
var pm=_52.pm||_51.pm||"PM";
if(_52.strict){
s=am+"|"+pm;
}else{
s=am+"|"+pm;
if(am!=am.toLowerCase()){
s+="|"+am.toLowerCase();
}
if(pm!=pm.toLowerCase()){
s+="|"+pm.toLowerCase();
}
if(s.indexOf(".")!=-1){
s+="|"+s.replace(/\./g,"");
}
}
s=s.replace(/\./g,"\\.");
break;
default:
s=".*";
}
if(_50){
_50.push(_54);
}
return "("+s+")";
}).replace(/[\xa0 ]/g,"[\\s\\xa0]");
};
})();
(function(){
var _5c=[];
dojo.date.locale.addCustomFormats=function(_5d,_5e){
_5c.push({pkg:_5d,name:_5e});
};
dojo.date.locale._getGregorianBundle=function(_5f){
var _60={};
dojo.forEach(_5c,function(_61){
var _62=dojo.i18n.getLocalization(_61.pkg,_61.name,_5f);
_60=dojo.mixin(_60,_62);
},this);
return _60;
};
})();
dojo.date.locale.addCustomFormats("dojo.cldr","gregorian");
dojo.date.locale.getNames=function(_63,_64,use,_66){
var _67;
var _68=dojo.date.locale._getGregorianBundle(_66);
var _69=[_63,use,_64];
if(use=="standAlone"){
var key=_69.join("-");
_67=_68[key];
if(_67[0]==1){
_67=undefined;
}
}
_69[1]="format";
return (_67||_68[_69.join("-")]).concat();
};
dojo.date.locale.isWeekend=function(_6b,_6c){
var _6d=dojo.cldr.supplemental.getWeekend(_6c);
var day=(_6b||new Date()).getDay();
if(_6d.end<_6d.start){
_6d.end+=7;
if(day<_6d.start){
day+=7;
}
}
return day>=_6d.start&&day<=_6d.end;
};
dojo.date.locale._getDayOfYear=function(_6f){
return dojo.date.difference(new Date(_6f.getFullYear(),0,1,_6f.getHours()),_6f)+1;
};
dojo.date.locale._getWeekOfYear=function(_70,_71){
if(arguments.length==1){
_71=0;
}
var _72=new Date(_70.getFullYear(),0,1).getDay();
var adj=(_72-_71+7)%7;
var _74=Math.floor((dojo.date.locale._getDayOfYear(_70)+adj-1)/7);
if(_72==_71){
_74++;
}
return _74;
};
}
