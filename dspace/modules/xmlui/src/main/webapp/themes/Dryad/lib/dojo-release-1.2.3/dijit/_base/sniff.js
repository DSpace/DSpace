/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit._base.sniff"]){
dojo._hasResource["dijit._base.sniff"]=true;
dojo.provide("dijit._base.sniff");
(function(){
var d=dojo;
var ie=d.isIE;
var _3=d.isOpera;
var _4=Math.floor;
var ff=d.isFF;
var _6=d.boxModel.replace(/-/,"");
var _7={dj_ie:ie,dj_ie6:_4(ie)==6,dj_ie7:_4(ie)==7,dj_iequirks:ie&&d.isQuirks,dj_opera:_3,dj_opera8:_4(_3)==8,dj_opera9:_4(_3)==9,dj_khtml:d.isKhtml,dj_safari:d.isSafari,dj_gecko:d.isMozilla,dj_ff2:_4(ff)==2,dj_ff3:_4(ff)==3};
_7["dj_"+_6]=true;
var _8=dojo.doc.documentElement;
for(var p in _7){
if(_7[p]){
if(_8.className){
_8.className+=" "+p;
}else{
_8.className=p;
}
}
}
dojo._loaders.unshift(function(){
if(!dojo._isBodyLtr()){
_8.className+=" dijitRtl";
for(var p in _7){
if(_7[p]){
_8.className+=" "+p+"-rtl";
}
}
}
});
})();
}
