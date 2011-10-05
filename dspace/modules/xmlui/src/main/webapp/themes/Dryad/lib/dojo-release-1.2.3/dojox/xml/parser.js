/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.xml.parser"]){
dojo._hasResource["dojox.xml.parser"]=true;
dojo.provide("dojox.xml.parser");
dojox.xml.parser.parse=function(_1){
if(dojo.isIE){
var _2=new ActiveXObject("Microsoft.XMLDOM");
_2.async="false";
_2.loadXML(_1);
var pe=_2.parseError;
if(pe.errorCode!==0){
throw new Error("Line: "+pe.line+"\n"+"Col: "+pe.linepos+"\n"+"Reason: "+pe.reason+"\n"+"Error Code: "+pe.errorCode+"\n"+"Source: "+pe.srcText);
}
return _2;
}else{
var _4=new DOMParser();
var _2=_4.parseFromString(_1,"text/xml");
var de=_2.documentElement;
var _6="http://www.mozilla.org/newlayout/xml/parsererror.xml";
if(de.nodeName=="parsererror"&&de.namespaceURI==_6){
var _7=de.getElementsByTagNameNS(_6,"sourcetext")[0];
if(!_7){
_7=_7.firstChild.data;
}
throw new Error("Error parsing text "+_2.documentElement.firstChild.data+" \n"+_7);
}
return _2;
}
};
}
