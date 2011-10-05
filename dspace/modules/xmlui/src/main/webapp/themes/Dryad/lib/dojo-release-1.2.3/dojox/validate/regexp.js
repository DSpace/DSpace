/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.validate.regexp"]){
dojo._hasResource["dojox.validate.regexp"]=true;
dojo.provide("dojox.validate.regexp");
dojo.require("dojo.regexp");
dojox.regexp={ca:{},us:{}};
dojox.regexp.tld=function(_1){
_1=(typeof _1=="object")?_1:{};
if(typeof _1.allowCC!="boolean"){
_1.allowCC=true;
}
if(typeof _1.allowInfra!="boolean"){
_1.allowInfra=true;
}
if(typeof _1.allowGeneric!="boolean"){
_1.allowGeneric=true;
}
var _2="arpa";
var _3="aero|biz|com|coop|edu|gov|info|int|mil|museum|name|net|org|pro|travel|xxx|jobs|mobi|post";
var _4="ac|ad|ae|af|ag|ai|al|am|an|ao|aq|ar|as|at|au|aw|az|ba|bb|bd|be|bf|bg|bh|bi|bj|bm|bn|bo|br|"+"bs|bt|bv|bw|by|bz|ca|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|cr|cu|cv|cx|cy|cz|de|dj|dk|dm|do|dz|"+"ec|ee|eg|er|eu|es|et|fi|fj|fk|fm|fo|fr|ga|gd|ge|gf|gg|gh|gi|gl|gm|gn|gp|gq|gr|gs|gt|gu|gw|"+"gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|io|ir|is|it|je|jm|jo|jp|ke|kg|kh|ki|km|kn|kr|kw|ky|kz|"+"la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mk|ml|mm|mn|mo|mp|mq|mr|ms|mt|mu|mv|mw|mx|"+"my|mz|na|nc|ne|nf|ng|ni|nl|no|np|nr|nu|nz|om|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|ps|pt|pw|py|qa|"+"re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sk|sl|sm|sn|sr|st|su|sv|sy|sz|tc|td|tf|tg|th|tj|tk|tm|"+"tn|to|tr|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|ye|yt|yu|za|zm|zw";
var a=[];
if(_1.allowInfra){
a.push(_2);
}
if(_1.allowGeneric){
a.push(_3);
}
if(_1.allowCC){
a.push(_4);
}
var _6="";
if(a.length>0){
_6="("+a.join("|")+")";
}
return _6;
};
dojox.regexp.ipAddress=function(_7){
_7=(typeof _7=="object")?_7:{};
if(typeof _7.allowDottedDecimal!="boolean"){
_7.allowDottedDecimal=true;
}
if(typeof _7.allowDottedHex!="boolean"){
_7.allowDottedHex=true;
}
if(typeof _7.allowDottedOctal!="boolean"){
_7.allowDottedOctal=true;
}
if(typeof _7.allowDecimal!="boolean"){
_7.allowDecimal=true;
}
if(typeof _7.allowHex!="boolean"){
_7.allowHex=true;
}
if(typeof _7.allowIPv6!="boolean"){
_7.allowIPv6=true;
}
if(typeof _7.allowHybrid!="boolean"){
_7.allowHybrid=true;
}
var _8="((\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])";
var _9="(0[xX]0*[\\da-fA-F]?[\\da-fA-F]\\.){3}0[xX]0*[\\da-fA-F]?[\\da-fA-F]";
var _a="(0+[0-3][0-7][0-7]\\.){3}0+[0-3][0-7][0-7]";
var _b="(0|[1-9]\\d{0,8}|[1-3]\\d{9}|4[01]\\d{8}|42[0-8]\\d{7}|429[0-3]\\d{6}|"+"4294[0-8]\\d{5}|42949[0-5]\\d{4}|429496[0-6]\\d{3}|4294967[01]\\d{2}|42949672[0-8]\\d|429496729[0-5])";
var _c="0[xX]0*[\\da-fA-F]{1,8}";
var _d="([\\da-fA-F]{1,4}\\:){7}[\\da-fA-F]{1,4}";
var _e="([\\da-fA-F]{1,4}\\:){6}"+"((\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])\\.){3}(\\d|[1-9]\\d|1\\d\\d|2[0-4]\\d|25[0-5])";
var a=[];
if(_7.allowDottedDecimal){
a.push(_8);
}
if(_7.allowDottedHex){
a.push(_9);
}
if(_7.allowDottedOctal){
a.push(_a);
}
if(_7.allowDecimal){
a.push(_b);
}
if(_7.allowHex){
a.push(_c);
}
if(_7.allowIPv6){
a.push(_d);
}
if(_7.allowHybrid){
a.push(_e);
}
var _10="";
if(a.length>0){
_10="("+a.join("|")+")";
}
return _10;
};
dojox.regexp.host=function(_11){
_11=(typeof _11=="object")?_11:{};
if(typeof _11.allowIP!="boolean"){
_11.allowIP=true;
}
if(typeof _11.allowLocal!="boolean"){
_11.allowLocal=false;
}
if(typeof _11.allowPort!="boolean"){
_11.allowPort=true;
}
if(typeof _11.allowNamed!="boolean"){
_11.allowNamed=false;
}
var _12="([0-9a-zA-Z]([-0-9a-zA-Z]{0,61}[0-9a-zA-Z])?\\.)+"+dojox.regexp.tld(_11);
var _13=_11.allowPort?"(\\:\\d+)?":"";
var _14=_12;
if(_11.allowIP){
_14+="|"+dojox.regexp.ipAddress(_11);
}
if(_11.allowLocal){
_14+="|localhost";
}
if(_11.allowNamed){
_14+="|^[^-][a-zA-Z0-9_-]*";
}
return "("+_14+")"+_13;
};
dojox.regexp.url=function(_15){
_15=(typeof _15=="object")?_15:{};
if(!("scheme" in _15)){
_15.scheme=[true,false];
}
var _16=dojo.regexp.buildGroupRE(_15.scheme,function(q){
if(q){
return "(https?|ftps?)\\://";
}
return "";
});
var _18="(/([^?#\\s/]+/)*)?([^?#\\s/]+(\\?[^?#\\s/]*)?(#[A-Za-z][\\w.:-]*)?)?";
return _16+dojox.regexp.host(_15)+_18;
};
dojox.regexp.emailAddress=function(_19){
_19=(typeof _19=="object")?_19:{};
if(typeof _19.allowCruft!="boolean"){
_19.allowCruft=false;
}
_19.allowPort=false;
var _1a="([\\da-zA-Z]+[-._+&'])*[\\da-zA-Z]+";
var _1b=_1a+"@"+dojox.regexp.host(_19);
if(_19.allowCruft){
_1b="<?(mailto\\:)?"+_1b+">?";
}
return _1b;
};
dojox.regexp.emailAddressList=function(_1c){
_1c=(typeof _1c=="object")?_1c:{};
if(typeof _1c.listSeparator!="string"){
_1c.listSeparator="\\s;,";
}
var _1d=dojox.regexp.emailAddress(_1c);
var _1e="("+_1d+"\\s*["+_1c.listSeparator+"]\\s*)*"+_1d+"\\s*["+_1c.listSeparator+"]?\\s*";
return _1e;
};
dojox.regexp.us.state=function(_1f){
_1f=(typeof _1f=="object")?_1f:{};
if(typeof _1f.allowTerritories!="boolean"){
_1f.allowTerritories=true;
}
if(typeof _1f.allowMilitary!="boolean"){
_1f.allowMilitary=true;
}
var _20="AL|AK|AZ|AR|CA|CO|CT|DE|DC|FL|GA|HI|ID|IL|IN|IA|KS|KY|LA|ME|MD|MA|MI|MN|MS|MO|MT|"+"NE|NV|NH|NJ|NM|NY|NC|ND|OH|OK|OR|PA|RI|SC|SD|TN|TX|UT|VT|VA|WA|WV|WI|WY";
var _21="AS|FM|GU|MH|MP|PW|PR|VI";
var _22="AA|AE|AP";
if(_1f.allowTerritories){
_20+="|"+_21;
}
if(_1f.allowMilitary){
_20+="|"+_22;
}
return "("+_20+")";
};
dojox.regexp.ca.postalCode=function(){
var _23="[A-Z][0-9][A-Z] [0-9][A-Z][0-9]";
return "("+_23+")";
};
dojox.regexp.ca.province=function(){
var _24="AB|BC|MB|NB|NL|NS|NT|NU|ON|PE|QC|SK|YT";
return "("+_24+")";
};
dojox.regexp.numberFormat=function(_25){
_25=(typeof _25=="object")?_25:{};
if(typeof _25.format=="undefined"){
_25.format="###-###-####";
}
var _26=function(_27){
_27=dojo.regexp.escapeString(_27,"?");
_27=_27.replace(/\?/g,"\\d?");
_27=_27.replace(/#/g,"\\d");
return _27;
};
return dojo.regexp.buildGroupRE(_25.format,_26);
};
}
