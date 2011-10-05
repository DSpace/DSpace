/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.XmlStore"]){
dojo._hasResource["dojox.data.XmlStore"]=true;
dojo.provide("dojox.data.XmlStore");
dojo.provide("dojox.data.XmlItem");
dojo.require("dojo.data.util.simpleFetch");
dojo.require("dojo.data.util.filter");
dojo.require("dojox.data.dom");
dojo.declare("dojox.data.XmlStore",null,{constructor:function(_1){

if(_1){
this.url=_1.url;
this.rootItem=(_1.rootItem||_1.rootitem||this.rootItem);
this.keyAttribute=(_1.keyAttribute||_1.keyattribute||this.keyAttribute);
this._attributeMap=(_1.attributeMap||_1.attributemap);
this.label=_1.label||this.label;
this.sendQuery=(_1.sendQuery||_1.sendquery||this.sendQuery);
}
this._newItems=[];
this._deletedItems=[];
this._modifiedItems=[];
},url:"",rootItem:"",keyAttribute:"",label:"",sendQuery:false,getValue:function(_2,_3,_4){
var _5=_2.element;
if(_3==="tagName"){
return _5.nodeName;
}else{
if(_3==="childNodes"){
for(var i=0;i<_5.childNodes.length;i++){
var _7=_5.childNodes[i];
if(_7.nodeType===1){
return this._getItem(_7);
}
}
return _4;
}else{
if(_3==="text()"){
for(var i=0;i<_5.childNodes.length;i++){
var _7=_5.childNodes[i];
if(_7.nodeType===3||_7.nodeType===4){
return _7.nodeValue;
}
}
return _4;
}else{
_3=this._getAttribute(_5.nodeName,_3);
if(_3.charAt(0)==="@"){
var _8=_3.substring(1);
var _9=_5.getAttribute(_8);
return (_9!==undefined)?_9:_4;
}else{
for(var i=0;i<_5.childNodes.length;i++){
var _7=_5.childNodes[i];
if(_7.nodeType===1&&_7.nodeName===_3){
return this._getItem(_7);
}
}
return _4;
}
}
}
}
},getValues:function(_a,_b){
var _c=_a.element;
if(_b==="tagName"){
return [_c.nodeName];
}else{
if(_b==="childNodes"){
var _d=[];
for(var i=0;i<_c.childNodes.length;i++){
var _f=_c.childNodes[i];
if(_f.nodeType===1){
_d.push(this._getItem(_f));
}
}
return _d;
}else{
if(_b==="text()"){
var _d=[],ec=_c.childNodes;
for(var i=0;i<ec.length;i++){
var _f=ec[i];
if(_f.nodeType===3||_f.nodeType===4){
_d.push(_f.nodeValue);
}
}
return _d;
}else{
_b=this._getAttribute(_c.nodeName,_b);
if(_b.charAt(0)==="@"){
var _11=_b.substring(1);
var _12=_c.getAttribute(_11);
return (_12!==undefined)?[_12]:[];
}else{
var _d=[];
for(var i=0;i<_c.childNodes.length;i++){
var _f=_c.childNodes[i];
if(_f.nodeType===1&&_f.nodeName===_b){
_d.push(this._getItem(_f));
}
}
return _d;
}
}
}
}
},getAttributes:function(_13){
var _14=_13.element;
var _15=[];
_15.push("tagName");
if(_14.childNodes.length>0){
var _16={};
var _17=true;
var _18=false;
for(var i=0;i<_14.childNodes.length;i++){
var _1a=_14.childNodes[i];
if(_1a.nodeType===1){
var _1b=_1a.nodeName;
if(!_16[_1b]){
_15.push(_1b);
_16[_1b]=_1b;
}
_17=true;
}else{
if(_1a.nodeType===3){
_18=true;
}
}
}
if(_17){
_15.push("childNodes");
}
if(_18){
_15.push("text()");
}
}
for(var i=0;i<_14.attributes.length;i++){
_15.push("@"+_14.attributes[i].nodeName);
}
if(this._attributeMap){
for(var key in this._attributeMap){
var i=key.indexOf(".");
if(i>0){
var _1d=key.substring(0,i);
if(_1d===_14.nodeName){
_15.push(key.substring(i+1));
}
}else{
_15.push(key);
}
}
}
return _15;
},hasAttribute:function(_1e,_1f){
return (this.getValue(_1e,_1f)!==undefined);
},containsValue:function(_20,_21,_22){
var _23=this.getValues(_20,_21);
for(var i=0;i<_23.length;i++){
if((typeof _22==="string")){
if(_23[i].toString&&_23[i].toString()===_22){
return true;
}
}else{
if(_23[i]===_22){
return true;
}
}
}
return false;
},isItem:function(_25){
if(_25&&_25.element&&_25.store&&_25.store===this){
return true;
}
return false;
},isItemLoaded:function(_26){
return this.isItem(_26);
},loadItem:function(_27){
},getFeatures:function(){
var _28={"dojo.data.api.Read":true,"dojo.data.api.Write":true};
return _28;
},getLabel:function(_29){
if((this.label!=="")&&this.isItem(_29)){
var _2a=this.getValue(_29,this.label);
if(_2a){
return _2a.toString();
}
}
return undefined;
},getLabelAttributes:function(_2b){
if(this.label!==""){
return [this.label];
}
return null;
},_fetchItems:function(_2c,_2d,_2e){
var url=this._getFetchUrl(_2c);

if(!url){
_2e(new Error("No URL specified."));
return;
}
var _30=(!this.sendQuery?_2c:null);
var _31=this;
var _32={url:url,handleAs:"xml",preventCache:true};
var _33=dojo.xhrGet(_32);
_33.addCallback(function(_34){
var _35=_31._getItems(_34,_30);

if(_35&&_35.length>0){
_2d(_35,_2c);
}else{
_2d([],_2c);
}
});
_33.addErrback(function(_36){
_2e(_36,_2c);
});
},_getFetchUrl:function(_37){
if(!this.sendQuery){
return this.url;
}
var _38=_37.query;
if(!_38){
return this.url;
}
if(dojo.isString(_38)){
return this.url+_38;
}
var _39="";
for(var _3a in _38){
var _3b=_38[_3a];
if(_3b){
if(_39){
_39+="&";
}
_39+=(_3a+"="+_3b);
}
}
if(!_39){
return this.url;
}
var _3c=this.url;
if(_3c.indexOf("?")<0){
_3c+="?";
}else{
_3c+="&";
}
return _3c+_39;
},_getItems:function(_3d,_3e){
var _3f=null;
if(_3e){
_3f=_3e.query;
}
var _40=[];
var _41=null;

if(this.rootItem!==""){
_41=_3d.getElementsByTagName(this.rootItem);
}else{
_41=_3d.documentElement.childNodes;
}
for(var i=0;i<_41.length;i++){
var _43=_41[i];
if(_43.nodeType!=1){
continue;
}
var _44=this._getItem(_43);
if(_3f){
var _45=true;
var _46=_3e.queryOptions?_3e.queryOptions.ignoreCase:false;
var _47={};
for(var key in _3f){
var _49=_3f[key];
if(typeof _49==="string"){
_47[key]=dojo.data.util.filter.patternToRegExp(_49,_46);
}
}
for(var _4a in _3f){
var _49=this.getValue(_44,_4a);
if(_49){
var _4b=_3f[_4a];
if((typeof _49)==="string"&&(_47[_4a])){
if((_49.match(_47[_4a]))!==null){
continue;
}
}else{
if((typeof _49)==="object"){
if(_49.toString&&(_47[_4a])){
var _4c=_49.toString();
if((_4c.match(_47[_4a]))!==null){
continue;
}
}else{
if(_4b==="*"||_4b===_49){
continue;
}
}
}
}
}
_45=false;
break;
}
if(!_45){
continue;
}
}
_40.push(_44);
}
dojo.forEach(_40,function(_4d){
_4d.element.parentNode.removeChild(_4d.element);
},this);
return _40;
},close:function(_4e){
},newItem:function(_4f){

_4f=(_4f||{});
var _50=_4f.tagName;
if(!_50){
_50=this.rootItem;
if(_50===""){
return null;
}
}
var _51=this._getDocument();
var _52=_51.createElement(_50);
for(var _53 in _4f){
if(_53==="tagName"){
continue;
}else{
if(_53==="text()"){
var _54=_51.createTextNode(_4f[_53]);
_52.appendChild(_54);
}else{
_53=this._getAttribute(_50,_53);
if(_53.charAt(0)==="@"){
var _55=_53.substring(1);
_52.setAttribute(_55,_4f[_53]);
}else{
var _56=_51.createElement(_53);
var _54=_51.createTextNode(_4f[_53]);
_56.appendChild(_54);
_52.appendChild(_56);
}
}
}
}
var _57=this._getItem(_52);
this._newItems.push(_57);
return _57;
},deleteItem:function(_58){

var _59=_58.element;
if(_59.parentNode){
this._backupItem(_58);
_59.parentNode.removeChild(_59);
return true;
}
this._forgetItem(_58);
this._deletedItems.push(_58);
return true;
},setValue:function(_5a,_5b,_5c){
if(_5b==="tagName"){
return false;
}
this._backupItem(_5a);
var _5d=_5a.element;
if(_5b==="childNodes"){
var _5e=_5c.element;
_5d.appendChild(_5e);
}else{
if(_5b==="text()"){
while(_5d.firstChild){
_5d.removeChild(_5d.firstChild);
}
var _5f=this._getDocument(_5d).createTextNode(_5c);
_5d.appendChild(_5f);
}else{
_5b=this._getAttribute(_5d.nodeName,_5b);
if(_5b.charAt(0)==="@"){
var _60=_5b.substring(1);
_5d.setAttribute(_60,_5c);
}else{
var _5e=null;
for(var i=0;i<_5d.childNodes.length;i++){
var _62=_5d.childNodes[i];
if(_62.nodeType===1&&_62.nodeName===_5b){
_5e=_62;
break;
}
}
var _63=this._getDocument(_5d);
if(_5e){
while(_5e.firstChild){
_5e.removeChild(_5e.firstChild);
}
}else{
_5e=_63.createElement(_5b);
_5d.appendChild(_5e);
}
var _5f=_63.createTextNode(_5c);
_5e.appendChild(_5f);
}
}
}
return true;
},setValues:function(_64,_65,_66){
if(_65==="tagName"){
return false;
}
this._backupItem(_64);
var _67=_64.element;
if(_65==="childNodes"){
while(_67.firstChild){
_67.removeChild(_67.firstChild);
}
for(var i=0;i<_66.length;i++){
var _69=_66[i].element;
_67.appendChild(_69);
}
}else{
if(_65==="text()"){
while(_67.firstChild){
_67.removeChild(_67.firstChild);
}
var _6a="";
for(var i=0;i<_66.length;i++){
_6a+=_66[i];
}
var _6b=this._getDocument(_67).createTextNode(_6a);
_67.appendChild(_6b);
}else{
_65=this._getAttribute(_67.nodeName,_65);
if(_65.charAt(0)==="@"){
var _6c=_65.substring(1);
_67.setAttribute(_6c,_66[0]);
}else{
for(var i=_67.childNodes.length-1;i>=0;i--){
var _6d=_67.childNodes[i];
if(_6d.nodeType===1&&_6d.nodeName===_65){
_67.removeChild(_6d);
}
}
var _6e=this._getDocument(_67);
for(var i=0;i<_66.length;i++){
var _69=_6e.createElement(_65);
var _6b=_6e.createTextNode(_66[i]);
_69.appendChild(_6b);
_67.appendChild(_69);
}
}
}
}
return true;
},unsetAttribute:function(_6f,_70){
if(_70==="tagName"){
return false;
}
this._backupItem(_6f);
var _71=_6f.element;
if(_70==="childNodes"||_70==="text()"){
while(_71.firstChild){
_71.removeChild(_71.firstChild);
}
}else{
_70=this._getAttribute(_71.nodeName,_70);
if(_70.charAt(0)==="@"){
var _72=_70.substring(1);
_71.removeAttribute(_72);
}else{
for(var i=_71.childNodes.length-1;i>=0;i--){
var _74=_71.childNodes[i];
if(_74.nodeType===1&&_74.nodeName===_70){
_71.removeChild(_74);
}
}
}
}
return true;
},save:function(_75){
if(!_75){
_75={};
}
for(var i=0;i<this._modifiedItems.length;i++){
this._saveItem(this._modifiedItems[i],_75,"PUT");
}
for(var i=0;i<this._newItems.length;i++){
var _77=this._newItems[i];
if(_77.element.parentNode){
this._newItems.splice(i,1);
i--;
continue;
}
this._saveItem(this._newItems[i],_75,"POST");
}
for(var i=0;i<this._deletedItems.length;i++){
this._saveItem(this._deletedItems[i],_75,"DELETE");
}
},revert:function(){



this._newItems=[];
this._restoreItems(this._deletedItems);
this._deletedItems=[];
this._restoreItems(this._modifiedItems);
this._modifiedItems=[];
return true;
},isDirty:function(_78){
if(_78){
var _79=this._getRootElement(_78.element);
return (this._getItemIndex(this._newItems,_79)>=0||this._getItemIndex(this._deletedItems,_79)>=0||this._getItemIndex(this._modifiedItems,_79)>=0);
}else{
return (this._newItems.length>0||this._deletedItems.length>0||this._modifiedItems.length>0);
}
},_saveItem:function(_7a,_7b,_7c){
var url;
if(_7c==="PUT"){
url=this._getPutUrl(_7a);
}else{
if(_7c==="DELETE"){
url=this._getDeleteUrl(_7a);
}else{
url=this._getPostUrl(_7a);
}
}
if(!url){
if(_7b.onError){
_7b.onError.call(_7e,new Error("No URL for saving content: "+this._getPostContent(_7a)));
}
return;
}
var _7f={url:url,method:(_7c||"POST"),contentType:"text/xml",handleAs:"xml"};
var _80;
if(_7c==="PUT"){
_7f.putData=this._getPutContent(_7a);
_80=dojo.rawXhrPut(_7f);
}else{
if(_7c==="DELETE"){
_80=dojo.xhrDelete(_7f);
}else{
_7f.postData=this._getPostContent(_7a);
_80=dojo.rawXhrPost(_7f);
}
}
var _7e=(_7b.scope||dojo.global);
var _81=this;
_80.addCallback(function(_82){
_81._forgetItem(_7a);
if(_7b.onComplete){
_7b.onComplete.call(_7e);
}
});
_80.addErrback(function(_83){
if(_7b.onError){
_7b.onError.call(_7e,_83);
}
});
},_getPostUrl:function(_84){
return this.url;
},_getPutUrl:function(_85){
return this.url;
},_getDeleteUrl:function(_86){
var url=this.url;
if(_86&&this.keyAttribute!==""){
var _88=this.getValue(_86,this.keyAttribute);
if(_88){
var key=this.keyAttribute.charAt(0)==="@"?this.keyAttribute.substring(1):this.keyAttribute;
url+=url.indexOf("?")<0?"?":"&";
url+=key+"="+_88;
}
}
return url;
},_getPostContent:function(_8a){
var _8b=_8a.element;
var _8c="<?xml version=\"1.0\"?>";
return _8c+dojox.data.dom.innerXML(_8b);
},_getPutContent:function(_8d){
var _8e=_8d.element;
var _8f="<?xml version=\"1.0\"?>";
return _8f+dojox.data.dom.innerXML(_8e);
},_getAttribute:function(_90,_91){
if(this._attributeMap){
var key=_90+"."+_91;
var _93=this._attributeMap[key];
if(_93){
_91=_93;
}else{
_93=this._attributeMap[_91];
if(_93){
_91=_93;
}
}
}
return _91;
},_getItem:function(_94){
return new dojox.data.XmlItem(_94,this);
},_getItemIndex:function(_95,_96){
for(var i=0;i<_95.length;i++){
if(_95[i].element===_96){
return i;
}
}
return -1;
},_backupItem:function(_98){
var _99=this._getRootElement(_98.element);
if(this._getItemIndex(this._newItems,_99)>=0||this._getItemIndex(this._modifiedItems,_99)>=0){
return;
}
if(_99!=_98.element){
_98=this._getItem(_99);
}
_98._backup=_99.cloneNode(true);
this._modifiedItems.push(_98);
},_restoreItems:function(_9a){
dojo.forEach(_9a,function(_9b){
if(_9b._backup){
_9b.element=_9b._backup;
_9b._backup=null;
}
},this);
},_forgetItem:function(_9c){
var _9d=_9c.element;
var _9e=this._getItemIndex(this._newItems,_9d);
if(_9e>=0){
this._newItems.splice(_9e,1);
}
_9e=this._getItemIndex(this._deletedItems,_9d);
if(_9e>=0){
this._deletedItems.splice(_9e,1);
}
_9e=this._getItemIndex(this._modifiedItems,_9d);
if(_9e>=0){
this._modifiedItems.splice(_9e,1);
}
},_getDocument:function(_9f){
if(_9f){
return _9f.ownerDocument;
}else{
if(!this._document){
return dojox.data.dom.createDocument();
}
}
},_getRootElement:function(_a0){
while(_a0.parentNode){
_a0=_a0.parentNode;
}
return _a0;
}});
dojo.declare("dojox.data.XmlItem",null,{constructor:function(_a1,_a2){
this.element=_a1;
this.store=_a2;
},toString:function(){
var str="";
if(this.element){
for(var i=0;i<this.element.childNodes.length;i++){
var _a5=this.element.childNodes[i];
if(_a5.nodeType===3||_a5.nodeType===4){
str+=_a5.nodeValue;
}
}
}
return str;
}});
dojo.extend(dojox.data.XmlStore,dojo.data.util.simpleFetch);
}
