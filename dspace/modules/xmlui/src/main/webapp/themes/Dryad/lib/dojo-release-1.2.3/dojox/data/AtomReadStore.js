/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.data.AtomReadStore"]){
dojo._hasResource["dojox.data.AtomReadStore"]=true;
dojo.provide("dojox.data.AtomReadStore");
dojo.require("dojo.data.util.simpleFetch");
dojo.require("dojo.data.util.filter");
dojo.require("dojo.date.stamp");
dojo.experimental("dojox.data.AtomReadStore");
dojo.declare("dojox.data.AtomReadStore",null,{constructor:function(_1){
if(_1){
this.url=_1.url;
this.rewriteUrl=_1.rewriteUrl;
this.label=_1.label||this.label;
this.sendQuery=(_1.sendQuery||_1.sendquery||this.sendQuery);
this.unescapeHTML=_1.unescapeHTML;
}
if(!this.url){
throw new Error("AtomReadStore: a URL must be specified when creating the data store");
}
},url:"",label:"title",sendQuery:false,unescapeHTML:false,getValue:function(_2,_3,_4){
this._assertIsItem(_2);
this._assertIsAttribute(_3);
this._initItem(_2);
_3=_3.toLowerCase();
if(!_2._attribs[_3]&&!_2._parsed){
this._parseItem(_2);
_2._parsed=true;
}
var _5=_2._attribs[_3];
if(!_5&&_3=="summary"){
var _6=this.getValue(_2,"content");
var _7=new RegExp("/(<([^>]+)>)/g","i");
var _8=_6.text.replace(_7,"");
_5={text:_8.substring(0,Math.min(400,_8.length)),type:"text"};
_2._attribs[_3]=_5;
}
if(_5&&this.unescapeHTML){
if((_3=="content"||_3=="summary"||_3=="subtitle")&&!_2["_"+_3+"Escaped"]){
_5.text=this._unescapeHTML(_5.text);
_2["_"+_3+"Escaped"]=true;
}
}
return _5?dojo.isArray(_5)?_5[0]:_5:_4;
},getValues:function(_9,_a){
this._assertIsItem(_9);
this._assertIsAttribute(_a);
this._initItem(_9);
_a=_a.toLowerCase();
if(!_9._attribs[_a]){
this._parseItem(_9);
}
var _b=_9._attribs[_a];
return _b?((_b.length!==undefined&&typeof (_b)!=="string")?_b:[_b]):undefined;
},getAttributes:function(_c){
this._assertIsItem(_c);
if(!_c._attribs){
this._initItem(_c);
this._parseItem(_c);
}
var _d=[];
for(var x in _c._attribs){
_d.push(x);
}
return _d;
},hasAttribute:function(_f,_10){
return (this.getValue(_f,_10)!==undefined);
},containsValue:function(_11,_12,_13){
var _14=this.getValues(_11,_12);
for(var i=0;i<_14.length;i++){
if((typeof _13==="string")){
if(_14[i].toString&&_14[i].toString()===_13){
return true;
}
}else{
if(_14[i]===_13){
return true;
}
}
}
return false;
},isItem:function(_16){
if(_16&&_16.element&&_16.store&&_16.store===this){
return true;
}
return false;
},isItemLoaded:function(_17){
return this.isItem(_17);
},loadItem:function(_18){
},getFeatures:function(){
var _19={"dojo.data.api.Read":true};
return _19;
},getLabel:function(_1a){
if((this.label!=="")&&this.isItem(_1a)){
var _1b=this.getValue(_1a,this.label);
if(_1b&&_1b.text){
return _1b.text;
}else{
if(_1b){
return _1b.toString();
}else{
return undefined;
}
}
}
return undefined;
},getLabelAttributes:function(_1c){
if(this.label!==""){
return [this.label];
}
return null;
},getFeedValue:function(_1d,_1e){
var _1f=this.getFeedValues(_1d,_1e);
if(dojo.isArray(_1f)){
return _1f[0];
}
return _1f;
},getFeedValues:function(_20,_21){
if(!this.doc){
return _21;
}
if(!this._feedMetaData){
this._feedMetaData={element:this.doc.getElementsByTagName("feed")[0],store:this,_attribs:{}};
this._parseItem(this._feedMetaData);
}
return this._feedMetaData._attribs[_20]||_21;
},_initItem:function(_22){
if(!_22._attribs){
_22._attribs={};
}
},_fetchItems:function(_23,_24,_25){
var url=this._getFetchUrl(_23);
if(!url){
_25(new Error("No URL specified."));
return;
}
var _27=(!this.sendQuery?_23:null);
var _28=this;
var _29=function(_2a){
_28.doc=_2a;
var _2b=_28._getItems(_2a,_27);
var _2c=_23.query;
if(_2c){
if(_2c.id){
_2b=dojo.filter(_2b,function(_2d){
return (_28.getValue(_2d,"id")==_2c.id);
});
}else{
if(_2c.category){
_2b=dojo.filter(_2b,function(_2e){
var _2f=_28.getValues(_2e,"category");
if(!_2f){
return false;
}
return dojo.some(_2f,"return item.term=='"+_2c.category+"'");
});
}
}
}
if(_2b&&_2b.length>0){
_24(_2b,_23);
}else{
_24([],_23);
}
};
if(this.doc){
_29(this.doc);
}else{
var _30={url:url,handleAs:"xml"};
var _31=dojo.xhrGet(_30);
_31.addCallback(_29);
_31.addErrback(function(_32){
_25(_32,_23);
});
}
},_getFetchUrl:function(_33){
if(!this.sendQuery){
return this.url;
}
var _34=_33.query;
if(!_34){
return this.url;
}
if(dojo.isString(_34)){
return this.url+_34;
}
var _35="";
for(var _36 in _34){
var _37=_34[_36];
if(_37){
if(_35){
_35+="&";
}
_35+=(_36+"="+_37);
}
}
if(!_35){
return this.url;
}
var _38=this.url;
if(_38.indexOf("?")<0){
_38+="?";
}else{
_38+="&";
}
return _38+_35;
},_getItems:function(_39,_3a){
if(this._items){
return this._items;
}
var _3b=[];
var _3c=[];
if(_39.childNodes.length<1){
this._items=_3b;

return _3b;
}
var _3d=dojo.filter(_39.childNodes,"return item.tagName && item.tagName.toLowerCase() == 'feed'");
var _3e=_3a.query;
if(!_3d||_3d.length!=1){

return _3b;
}
_3c=dojo.filter(_3d[0].childNodes,"return item.tagName && item.tagName.toLowerCase() == 'entry'");
if(_3a.onBegin){
_3a.onBegin(_3c.length);
}
for(var i=0;i<_3c.length;i++){
var _40=_3c[i];
if(_40.nodeType!=1){
continue;
}
_3b.push(this._getItem(_40));
}
this._items=_3b;
return _3b;
},close:function(_41){
},_getItem:function(_42){
return {element:_42,store:this};
},_parseItem:function(_43){
var _44=_43._attribs;
var _45=this;
var _46,_47;
function getNodeText(_48){
var txt=_48.textContent||_48.innerHTML||_48.innerXML;
if(!txt&&_48.childNodes[0]){
var _4a=_48.childNodes[0];
if(_4a&&(_4a.nodeType==3||_4a.nodeType==4)){
txt=_48.childNodes[0].nodeValue;
}
}
return txt;
};
function parseTextAndType(_4b){
return {text:getNodeText(_4b),type:_4b.getAttribute("type")};
};
dojo.forEach(_43.element.childNodes,function(_4c){
var _4d=_4c.tagName?_4c.tagName.toLowerCase():"";
switch(_4d){
case "title":
_44[_4d]={text:getNodeText(_4c),type:_4c.getAttribute("type")};
break;
case "subtitle":
case "summary":
case "content":
_44[_4d]=parseTextAndType(_4c);
break;
case "author":
var _4e,_4f;
dojo.forEach(_4c.childNodes,function(_50){
if(!_50.tagName){
return;
}
switch(_50.tagName.toLowerCase()){
case "name":
_4e=_50;
break;
case "uri":
_4f=_50;
break;
}
});
var _51={};
if(_4e&&_4e.length==1){
_51.name=getNodeText(_4e[0]);
}
if(_4f&&_4f.length==1){
_51.uri=getNodeText(_4f[0]);
}
_44[_4d]=_51;
break;
case "id":
_44[_4d]=getNodeText(_4c);
break;
case "updated":
_44[_4d]=dojo.date.stamp.fromISOString(getNodeText(_4c));
break;
case "published":
_44[_4d]=dojo.date.stamp.fromISOString(getNodeText(_4c));
break;
case "category":
if(!_44[_4d]){
_44[_4d]=[];
}
_44[_4d].push({scheme:_4c.getAttribute("scheme"),term:_4c.getAttribute("term")});
break;
case "link":
if(!_44[_4d]){
_44[_4d]=[];
}
var _52={rel:_4c.getAttribute("rel"),href:_4c.getAttribute("href"),type:_4c.getAttribute("type")};
_44[_4d].push(_52);
if(_52.rel=="alternate"){
_44["alternate"]=_52;
}
break;
default:
break;
}
});
},_unescapeHTML:function(_53){
_53=_53.replace(/&#8217;/m,"'").replace(/&#8243;/m,"\"").replace(/&#60;/m,">").replace(/&#62;/m,"<").replace(/&#38;/m,"&");
return _53;
},_assertIsItem:function(_54){
if(!this.isItem(_54)){
throw new Error("dojox.data.AtomReadStore: Invalid item argument.");
}
},_assertIsAttribute:function(_55){
if(typeof _55!=="string"){
throw new Error("dojox.data.AtomReadStore: Invalid attribute argument.");
}
}});
dojo.extend(dojox.data.AtomReadStore,dojo.data.util.simpleFetch);
}
