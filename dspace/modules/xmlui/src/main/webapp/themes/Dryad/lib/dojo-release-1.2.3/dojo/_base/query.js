/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.query"]){
dojo._hasResource["dojo._base.query"]=true;
dojo.provide("dojo._base.query");
dojo.require("dojo._base.NodeList");
(function(){
var d=dojo;
var _2=dojo.isIE?"children":"childNodes";
var _3=false;
var _4=function(_5){
if(">~+".indexOf(_5.charAt(_5.length-1))>=0){
_5+=" *";
}
_5+=" ";
var ts=function(s,e){
return d.trim(_5.slice(s,e));
};
var _9=[];
var _a=-1;
var _b=-1;
var _c=-1;
var _d=-1;
var _e=-1;
var _f=-1;
var _10=-1;
var lc="";
var cc="";
var _13;
var x=0;
var ql=_5.length;
var _16=null;
var _cp=null;
var _18=function(){
if(_10>=0){
var tv=(_10==x)?null:ts(_10,x);
_16[(">~+".indexOf(tv)<0)?"tag":"oper"]=tv;
_10=-1;
}
};
var _1a=function(){
if(_f>=0){
_16.id=ts(_f,x).replace(/\\/g,"");
_f=-1;
}
};
var _1b=function(){
if(_e>=0){
_16.classes.push(ts(_e+1,x).replace(/\\/g,""));
_e=-1;
}
};
var _1c=function(){
_1a();
_18();
_1b();
};
for(;lc=cc,cc=_5.charAt(x),x<ql;x++){
if(lc=="\\"){
continue;
}
if(!_16){
_13=x;
_16={query:null,pseudos:[],attrs:[],classes:[],tag:null,oper:null,id:null};
_10=x;
}
if(_a>=0){
if(cc=="]"){
if(!_cp.attr){
_cp.attr=ts(_a+1,x);
}else{
_cp.matchFor=ts((_c||_a+1),x);
}
var cmf=_cp.matchFor;
if(cmf){
if((cmf.charAt(0)=="\"")||(cmf.charAt(0)=="'")){
_cp.matchFor=cmf.substring(1,cmf.length-1);
}
}
_16.attrs.push(_cp);
_cp=null;
_a=_c=-1;
}else{
if(cc=="="){
var _1e=("|~^$*".indexOf(lc)>=0)?lc:"";
_cp.type=_1e+cc;
_cp.attr=ts(_a+1,x-_1e.length);
_c=x+1;
}
}
}else{
if(_b>=0){
if(cc==")"){
if(_d>=0){
_cp.value=ts(_b+1,x);
}
_d=_b=-1;
}
}else{
if(cc=="#"){
_1c();
_f=x+1;
}else{
if(cc=="."){
_1c();
_e=x;
}else{
if(cc==":"){
_1c();
_d=x;
}else{
if(cc=="["){
_1c();
_a=x;
_cp={};
}else{
if(cc=="("){
if(_d>=0){
_cp={name:ts(_d+1,x),value:null};
_16.pseudos.push(_cp);
}
_b=x;
}else{
if(cc==" "&&lc!=cc){
_1c();
if(_d>=0){
_16.pseudos.push({name:ts(_d+1,x)});
}
_16.hasLoops=(_16.pseudos.length||_16.attrs.length||_16.classes.length);
_16.query=ts(_13,x);
_16.otag=_16.tag=(_16["oper"])?null:(_16.tag||"*");
if(_16.tag){
_16.tag=_16.tag.toUpperCase();
}
_9.push(_16);
_16=null;
}
}
}
}
}
}
}
}
}
return _9;
};
var _1f={"*=":function(_20,_21){
return "[contains(@"+_20+", '"+_21+"')]";
},"^=":function(_22,_23){
return "[starts-with(@"+_22+", '"+_23+"')]";
},"$=":function(_24,_25){
return "[substring(@"+_24+", string-length(@"+_24+")-"+(_25.length-1)+")='"+_25+"']";
},"~=":function(_26,_27){
return "[contains(concat(' ',@"+_26+",' '), ' "+_27+" ')]";
},"|=":function(_28,_29){
return "[contains(concat(' ',@"+_28+",' '), ' "+_29+"-')]";
},"=":function(_2a,_2b){
return "[@"+_2a+"='"+_2b+"']";
}};
var _2c=function(_2d,_2e,_2f,_30){
d.forEach(_2e.attrs,function(_31){
var _32;
if(_31.type&&_2d[_31.type]){
_32=_2d[_31.type](_31.attr,_31.matchFor);
}else{
if(_31.attr.length){
_32=_2f(_31.attr);
}
}
if(_32){
_30(_32);
}
});
};
var _33=function(_34){
var _35=".";
var _36=_4(d.trim(_34));
while(_36.length){
var tqp=_36.shift();
var _38;
var _39="";
if(tqp.oper==">"){
_38="/";
tqp=_36.shift();
}else{
if(tqp.oper=="~"){
_38="/following-sibling::";
tqp=_36.shift();
}else{
if(tqp.oper=="+"){
_38="/following-sibling::";
_39="[position()=1]";
tqp=_36.shift();
}else{
_38="//";
}
}
}
_35+=_38+tqp.tag+_39;
if(tqp.id){
_35+="[@id='"+tqp.id+"'][1]";
}
d.forEach(tqp.classes,function(cn){
var cnl=cn.length;
var _3c=" ";
if(cn.charAt(cnl-1)=="*"){
_3c="";
cn=cn.substr(0,cnl-1);
}
_35+="[contains(concat(' ',@class,' '), ' "+cn+_3c+"')]";
});
_2c(_1f,tqp,function(_3d){
return "[@"+_3d+"]";
},function(_3e){
_35+=_3e;
});
}
return _35;
};
var _3f={};
var _40=function(_41){
if(_3f[_41]){
return _3f[_41];
}
var doc=d.doc;
var _43=_33(_41);
var tf=function(_45){
var ret=[];
var _47;
var _48=doc;
if(_45){
_48=(_45.nodeType==9)?_45:_45.ownerDocument;
}
try{
_47=_48.evaluate(_43,_45,null,XPathResult.ANY_TYPE,null);
}
catch(e){


}
var _49=_47.iterateNext();
while(_49){
ret.push(_49);
_49=_47.iterateNext();
}
return ret;
};
return _3f[_41]=tf;
};
var _4a={};
var _4b={};
var _4c=function(_4d,_4e){
if(!_4d){
return _4e;
}
if(!_4e){
return _4d;
}
return function(){
return _4d.apply(window,arguments)&&_4e.apply(window,arguments);
};
};
var _4f=function(_50){
var ret=[];
var te,x=0,_54=_50[_2];
while((te=_54[x++])){
if(te.nodeType==1){
ret.push(te);
}
}
return ret;
};
var _55=function(_56,_57){
var ret=[];
var te=_56;
while(te=te.nextSibling){
if(te.nodeType==1){
ret.push(te);
if(_57){
break;
}
}
}
return ret;
};
var _5a=function(_5b,_5c,_5d,idx){
var _5f=idx+1;
var _60=(_5c.length==_5f);
var tqp=_5c[idx];
if(tqp.oper){
var ecn=(tqp.oper==">")?_4f(_5b):_55(_5b,(tqp.oper=="+"));
if(!ecn||!ecn.length){
return;
}
_5f++;
_60=(_5c.length==_5f);
var tf=_64(_5c[idx+1]);
for(var x=0,_66=ecn.length,te;x<_66,te=ecn[x];x++){
if(tf(te)){
if(_60){
_5d.push(te);
}else{
_5a(te,_5c,_5d,_5f);
}
}
}
}
var _68=_69(tqp)(_5b);
if(_60){
while(_68.length){
_5d.push(_68.shift());
}
}else{
while(_68.length){
_5a(_68.shift(),_5c,_5d,_5f);
}
}
};
var _6a=function(_6b,_6c){
var ret=[];
var x=_6b.length-1,te;
while((te=_6b[x--])){
_5a(te,_6c,ret,0);
}
return ret;
};
var _64=function(q){
if(_4a[q.query]){
return _4a[q.query];
}
var ff=null;
if(q.tag){
if(q.tag=="*"){
ff=_4c(ff,function(_72){
return (_72.nodeType==1);
});
}else{
ff=_4c(ff,function(_73){
return ((_73.nodeType==1)&&(q[_3?"otag":"tag"]==_73.tagName));
});
}
}
if(q.id){
ff=_4c(ff,function(_74){
return ((_74.nodeType==1)&&(_74.id==q.id));
});
}
if(q.hasLoops){
ff=_4c(ff,_75(q));
}
return _4a[q.query]=ff;
};
var _76=function(_77){
var pn=_77.parentNode;
var pnc=pn.childNodes;
var _7a=-1;
var _7b=pn.firstChild;
if(!_7b){
return _7a;
}
var ci=_77["__cachedIndex"];
var cl=pn["__cachedLength"];
if(((typeof cl=="number")&&(cl!=pnc.length))||(typeof ci!="number")){
pn["__cachedLength"]=pnc.length;
var idx=1;
do{
if(_7b===_77){
_7a=idx;
}
if(_7b.nodeType==1){
_7b["__cachedIndex"]=idx;
idx++;
}
_7b=_7b.nextSibling;
}while(_7b);
}else{
_7a=ci;
}
return _7a;
};
var _7f=0;
var _80="";
var _81=function(_82,_83){
if(_83=="class"){
return _82.className||_80;
}
if(_83=="for"){
return _82.htmlFor||_80;
}
if(_83=="style"){
return _82.style.cssText||_80;
}
return (_3?_82.getAttribute(_83):_82.getAttribute(_83,2))||_80;
};
var _84={"*=":function(_85,_86){
return function(_87){
return (_81(_87,_85).indexOf(_86)>=0);
};
},"^=":function(_88,_89){
return function(_8a){
return (_81(_8a,_88).indexOf(_89)==0);
};
},"$=":function(_8b,_8c){
var _8d=" "+_8c;
return function(_8e){
var ea=" "+_81(_8e,_8b);
return (ea.lastIndexOf(_8c)==(ea.length-_8c.length));
};
},"~=":function(_90,_91){
var _92=" "+_91+" ";
return function(_93){
var ea=" "+_81(_93,_90)+" ";
return (ea.indexOf(_92)>=0);
};
},"|=":function(_95,_96){
var _97=" "+_96+"-";
return function(_98){
var ea=" "+(_98.getAttribute(_95,2)||"");
return ((ea==_96)||(ea.indexOf(_97)==0));
};
},"=":function(_9a,_9b){
return function(_9c){
return (_81(_9c,_9a)==_9b);
};
}};
var _9d={"checked":function(_9e,_9f){
return function(_a0){
return !!d.attr(_a0,"checked");
};
},"first-child":function(_a1,_a2){
return function(_a3){
if(_a3.nodeType!=1){
return false;
}
var fc=_a3.previousSibling;
while(fc&&(fc.nodeType!=1)){
fc=fc.previousSibling;
}
return (!fc);
};
},"last-child":function(_a5,_a6){
return function(_a7){
if(_a7.nodeType!=1){
return false;
}
var nc=_a7.nextSibling;
while(nc&&(nc.nodeType!=1)){
nc=nc.nextSibling;
}
return (!nc);
};
},"empty":function(_a9,_aa){
return function(_ab){
var cn=_ab.childNodes;
var cnl=_ab.childNodes.length;
for(var x=cnl-1;x>=0;x--){
var nt=cn[x].nodeType;
if((nt==1)||(nt==3)){
return false;
}
}
return true;
};
},"contains":function(_b0,_b1){
return function(_b2){
if(_b1.charAt(0)=="\""||_b1.charAt(0)=="'"){
_b1=_b1.substr(1,_b1.length-2);
}
return (_b2.innerHTML.indexOf(_b1)>=0);
};
},"not":function(_b3,_b4){
var ntf=_64(_4(_b4)[0]);
return function(_b6){
return (!ntf(_b6));
};
},"nth-child":function(_b7,_b8){
var pi=parseInt;
if(_b8=="odd"){
_b8="2n+1";
}else{
if(_b8=="even"){
_b8="2n";
}
}
if(_b8.indexOf("n")!=-1){
var _ba=_b8.split("n",2);
var _bb=_ba[0]?(_ba[0]=="-"?-1:pi(_ba[0])):1;
var idx=_ba[1]?pi(_ba[1]):0;
var lb=0,ub=-1;
if(_bb>0){
if(idx<0){
idx=(idx%_bb)&&(_bb+(idx%_bb));
}else{
if(idx>0){
if(idx>=_bb){
lb=idx-idx%_bb;
}
idx=idx%_bb;
}
}
}else{
if(_bb<0){
_bb*=-1;
if(idx>0){
ub=idx;
idx=idx%_bb;
}
}
}
if(_bb>0){
return function(_bf){
var i=_76(_bf);
return (i>=lb)&&(ub<0||i<=ub)&&((i%_bb)==idx);
};
}else{
_b8=idx;
}
}
var _c1=pi(_b8);
return function(_c2){
return (_76(_c2)==_c1);
};
}};
var _c3=(d.isIE)?function(_c4){
var clc=_c4.toLowerCase();
return function(_c6){
return (_3?_c6.getAttribute(_c4):_c6[_c4]||_c6[clc]);
};
}:function(_c7){
return function(_c8){
return (_c8&&_c8.getAttribute&&_c8.hasAttribute(_c7));
};
};
var _75=function(_c9){
var _ca=(_4b[_c9.query]||_4a[_c9.query]);
if(_ca){
return _ca;
}
var ff=null;
if(_c9.id){
if(_c9.tag!="*"){
ff=_4c(ff,function(_cc){
return (_cc.tagName==_c9[_3?"otag":"tag"]);
});
}
}
d.forEach(_c9.classes,function(_cd,idx,arr){
var _d0=_cd.charAt(_cd.length-1)=="*";
if(_d0){
_cd=_cd.substr(0,_cd.length-1);
}
var re=new RegExp("(?:^|\\s)"+_cd+(_d0?".*":"")+"(?:\\s|$)");
ff=_4c(ff,function(_d2){
return re.test(_d2.className);
});
ff.count=idx;
});
d.forEach(_c9.pseudos,function(_d3){
if(_9d[_d3.name]){
ff=_4c(ff,_9d[_d3.name](_d3.name,_d3.value));
}
});
_2c(_84,_c9,_c3,function(_d4){
ff=_4c(ff,_d4);
});
if(!ff){
ff=function(){
return true;
};
}
return _4b[_c9.query]=ff;
};
var _d5={};
var _69=function(_d6,_d7){
var _d8=_d5[_d6.query];
if(_d8){
return _d8;
}
if(_d6.id&&!_d6.hasLoops&&!_d6.tag){
return _d5[_d6.query]=function(_d9){
return [d.byId(_d6.id)];
};
}
var _da=_75(_d6);
var _db;
if(_d6.tag&&_d6.id&&!_d6.hasLoops){
_db=function(_dc){
var te=d.byId(_d6.id,(_dc.ownerDocument||_dc));
if(_da(te)){
return [te];
}
};
}else{
var _de;
if(!_d6.hasLoops){
_db=function(_df){
var ret=[];
var te,x=0,_de=_df.getElementsByTagName(_d6[_3?"otag":"tag"]);
while((te=_de[x++])){
ret.push(te);
}
return ret;
};
}else{
_db=function(_e3){
var ret=[];
var te,x=0,_de=_e3.getElementsByTagName(_d6[_3?"otag":"tag"]);
while((te=_de[x++])){
if(_da(te)){
ret.push(te);
}
}
return ret;
};
}
}
return _d5[_d6.query]=_db;
};
var _e7={};
var _e8={"*":d.isIE?function(_e9){
return _e9.all;
}:function(_ea){
return _ea.getElementsByTagName("*");
},"~":_55,"+":function(_eb){
return _55(_eb,true);
},">":_4f};
var _ec=function(_ed){
var _ee=_4(d.trim(_ed));
if(_ee.length==1){
var tt=_69(_ee[0]);
tt.nozip=true;
return tt;
}
var sqf=function(_f1){
var _f2=_ee.slice(0);
var _f3;
if(_f2[0].oper==">"){
_f3=[_f1];
}else{
_f3=_69(_f2.shift())(_f1);
}
return _6a(_f3,_f2);
};
return sqf;
};
var _f4=((document["evaluate"]&&!d.isSafari)?function(_f5,_f6){
var _f7=_f5.split(" ");
if((!_3)&&(document["evaluate"])&&(_f5.indexOf(":")==-1)&&(_f5.indexOf("+")==-1)){
if(((_f7.length>2)&&(_f5.indexOf(">")==-1))||(_f7.length>3)||(_f5.indexOf("[")>=0)||((1==_f7.length)&&(0<=_f5.indexOf(".")))){
return _40(_f5);
}
}
return _ec(_f5);
}:_ec);
var _f8=function(_f9){
if(_e8[_f9]){
return _e8[_f9];
}
if(0>_f9.indexOf(",")){
return _e8[_f9]=_f4(_f9);
}else{
var _fa=_f9.split(/\s*,\s*/);
var tf=function(_fc){
var _fd=0;
var ret=[];
var tp;
while((tp=_fa[_fd++])){
ret=ret.concat(_f4(tp,tp.indexOf(" "))(_fc));
}
return ret;
};
return _e8[_f9]=tf;
}
};
var _100=0;
var _zip=function(arr){
if(arr&&arr.nozip){
return d.NodeList._wrap(arr);
}
var ret=new d.NodeList();
if(!arr){
return ret;
}
if(arr[0]){
ret.push(arr[0]);
}
if(arr.length<2){
return ret;
}
_100++;
if(d.isIE&&_3){
var _104=_100+"";
arr[0].setAttribute("_zipIdx",_104);
for(var x=1,te;te=arr[x];x++){
if(arr[x].getAttribute("_zipIdx")!=_104){
ret.push(te);
}
te.setAttribute("_zipIdx",_104);
}
}else{
arr[0]["_zipIdx"]=_100;
for(var x=1,te;te=arr[x];x++){
if(arr[x]["_zipIdx"]!=_100){
ret.push(te);
}
te["_zipIdx"]=_100;
}
}
return ret;
};
d.query=function(_107,root){
if(_107.constructor==d.NodeList){
return _107;
}
if(!d.isString(_107)){
return new d.NodeList(_107);
}
if(d.isString(root)){
root=d.byId(root);
}
root=root||d.doc;
var od=root.ownerDocument||root.documentElement;
_3=(root.contentType&&root.contentType=="application/xml")||(!!od)&&(d.isIE?od.xml:(root.xmlVersion||od.xmlVersion));
return _zip(_f8(_107)(root));
};
d.query.pseudos=_9d;
d._filterQueryResult=function(_10a,_10b){
var tnl=new d.NodeList();
var ff=(_10b)?_64(_4(_10b)[0]):function(){
return true;
};
for(var x=0,te;te=_10a[x];x++){
if(ff(te)){
tnl.push(te);
}
}
return tnl;
};
})();
}
