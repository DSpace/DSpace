/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.dtl.html"]){
dojo._hasResource["dojox.dtl.html"]=true;
dojo.provide("dojox.dtl.html");
dojo.require("dojox.dtl._base");
dojo.require("dojox.dtl.Context");
(function(){
var dd=dojox.dtl;
dd.TOKEN_CHANGE=-11;
dd.TOKEN_ATTR=-12;
dd.TOKEN_CUSTOM=-13;
dd.TOKEN_NODE=1;
var _2=dd.text;
var _3=dd.html={_attributes:{},_re4:/^function anonymous\(\)\s*{\s*(.*)\s*}$/,getTemplate:function(_4){
if(typeof this._commentable=="undefined"){
this._commentable=false;
var _5=document.createElement("div");
_5.innerHTML="<!--Test comment handling, and long comments, using comments whenever possible.-->";
if(_5.childNodes.length&&_5.childNodes[0].nodeType==8&&_5.childNodes[0].data=="comment"){
this._commentable=true;
}
}
if(!this._commentable){
_4=_4.replace(/<!--({({|%).*?(%|})})-->/g,"$1");
}
var _6;
var _7=[[true,"select","option"],[dojo.isSafari,"tr","th"],[dojo.isSafari,"tr","td"],[dojo.isSafari,"thead","tr","th"],[dojo.isSafari,"tbody","tr","td"]];
for(var i=0,_9;_9=_7[i];i++){
if(!_9[0]){
continue;
}
if(_4.indexOf("<"+_9[1])!=-1){
var _a=new RegExp("<"+_9[1]+"[\\s\\S]*?>([\\s\\S]+?)</"+_9[1]+">","ig");
while(_6=_a.exec(_4)){
var _b=false;
var _c=dojox.string.tokenize(_6[1],new RegExp("(<"+_9[2]+"[\\s\\S]*?>[\\s\\S]*?</"+_9[2]+">)","ig"),function(_d){
_b=true;
return {data:_d};
});
if(_b){
var _e=[];
for(var j=0;j<_c.length;j++){
if(dojo.isObject(_c[j])){
_e.push(_c[j].data);
}else{
var _10=_9[_9.length-1];
var k,_12="";
for(k=2;k<_9.length-1;k++){
_12+="<"+_9[k]+">";
}
_12+="<"+_10+" iscomment=\"true\">"+dojo.trim(_c[j])+"</"+_10+">";
for(k=2;k<_9.length-1;k++){
_12+="</"+_9[k]+">";
}
_e.push(_12);
}
}
_4=_4.replace(_6[1],_e.join(""));
}
}
}
}
var re=/\b([a-zA-Z]+)=['"]/g;
while(_6=re.exec(_4)){
this._attributes[_6[1].toLowerCase()]=true;
}
var _5=document.createElement("div");
_5.innerHTML=_4;
var _14={nodes:[]};
while(_5.childNodes.length){
_14.nodes.push(_5.removeChild(_5.childNodes[0]));
}
return _14;
},tokenize:function(_15){
var _16=[];
for(var i=0,_18;_18=_15[i++];){
if(_18.nodeType!=1){
this.__tokenize(_18,_16);
}else{
this._tokenize(_18,_16);
}
}
return _16;
},_swallowed:[],_tokenize:function(_19,_1a){
var _1b=false;
var _1c=this._swallowed;
var i,j,tag,_20;
if(!_1a.first){
_1b=_1a.first=true;
var _21=dd.register.getAttributeTags();
for(i=0;tag=_21[i];i++){
try{
(tag[2])({swallowNode:function(){
throw 1;
}},new dd.Token(dd.TOKEN_ATTR,""));
}
catch(e){
_1c.push(tag);
}
}
}
for(i=0;tag=_1c[i];i++){
var _22=_19.getAttribute(tag[0]);
if(_22){
var _1c=false;
var _23=(tag[2])({swallowNode:function(){
_1c=true;
return _19;
}},_22);
if(_1c){
if(_19.parentNode&&_19.parentNode.removeChild){
_19.parentNode.removeChild(_19);
}
_1a.push([dd.TOKEN_CUSTOM,_23]);
return;
}
}
}
var _24=[];
if(dojo.isIE&&_19.tagName=="SCRIPT"){
_24.push({nodeType:3,data:_19.text});
_19.text="";
}else{
for(i=0;_20=_19.childNodes[i];i++){
_24.push(_20);
}
}
_1a.push([dd.TOKEN_NODE,_19]);
var _25=false;
if(_24.length){
_1a.push([dd.TOKEN_CHANGE,_19]);
_25=true;
}
for(var key in this._attributes){
var _27="";
if(key=="class"){
_27=_19.className||_27;
}else{
if(key=="for"){
_27=_19.htmlFor||_27;
}else{
if(key=="value"&&_19.value==_19.innerHTML){
continue;
}else{
if(_19.getAttribute){
_27=_19.getAttribute(key,2)||_27;
if(key=="href"||key=="src"){
if(dojo.isIE){
var _28=location.href.lastIndexOf(location.hash);
var _29=location.href.substring(0,_28).split("/");
_29.pop();
_29=_29.join("/")+"/";
if(_27.indexOf(_29)==0){
_27=_27.replace(_29,"");
}
_27=decodeURIComponent(_27);
}
if(_27.indexOf("{%")!=-1||_27.indexOf("{{")!=-1){
_19.setAttribute(key,"");
}
}
}
}
}
}
if(typeof _27=="function"){
_27=_27.toString().replace(this._re4,"$1");
}
if(!_25){
_1a.push([dd.TOKEN_CHANGE,_19]);
_25=true;
}
_1a.push([dd.TOKEN_ATTR,_19,key,_27]);
}
for(i=0,_20;_20=_24[i];i++){
if(_20.nodeType==1&&_20.getAttribute("iscomment")){
_20.parentNode.removeChild(_20);
_20={nodeType:8,data:_20.innerHTML};
}
this.__tokenize(_20,_1a);
}
if(!_1b&&_19.parentNode&&_19.parentNode.tagName){
if(_25){
_1a.push([dd.TOKEN_CHANGE,_19,true]);
}
_1a.push([dd.TOKEN_CHANGE,_19.parentNode]);
_19.parentNode.removeChild(_19);
}else{
_1a.push([dd.TOKEN_CHANGE,_19,true,true]);
}
},__tokenize:function(_2a,_2b){
var _2c=_2a.data;
switch(_2a.nodeType){
case 1:
this._tokenize(_2a,_2b);
return;
case 3:
if(_2c.match(/[^\s\n]/)&&(_2c.indexOf("{{")!=-1||_2c.indexOf("{%")!=-1)){
var _2d=_2.tokenize(_2c);
for(var j=0,_2f;_2f=_2d[j];j++){
if(typeof _2f=="string"){
_2b.push([dd.TOKEN_TEXT,_2f]);
}else{
_2b.push(_2f);
}
}
}else{
_2b.push([_2a.nodeType,_2a]);
}
if(_2a.parentNode){
_2a.parentNode.removeChild(_2a);
}
return;
case 8:
if(_2c.indexOf("{%")==0){
var _2f=dojo.trim(_2c.slice(2,-2));
if(_2f.substr(0,5)=="load "){
var _30=dojo.trim(_2f).split(/\s+/g);
for(var i=1,_32;_32=_30[i];i++){
dojo["require"](_32);
}
}
_2b.push([dd.TOKEN_BLOCK,_2f]);
}
if(_2c.indexOf("{{")==0){
_2b.push([dd.TOKEN_VAR,dojo.trim(_2c.slice(2,-2))]);
}
if(_2a.parentNode){
_2a.parentNode.removeChild(_2a);
}
return;
}
}};
dd.HtmlTemplate=dojo.extend(function(obj){
if(!obj.nodes){
var _34=dojo.byId(obj);
if(_34&&_34.nodeType==1){
dojo.forEach(["class","src","href","name","value"],function(_35){
_3._attributes[_35]=true;
});
obj={nodes:[_34]};
}else{
if(typeof obj=="object"){
obj=_2.getTemplateString(obj);
}
obj=_3.getTemplate(obj);
}
}
var _36=_3.tokenize(obj.nodes);
if(dd.tests){
this.tokens=_36.slice(0);
}
var _37=new dd._HtmlParser(_36);
this.nodelist=_37.parse();
},{_count:0,_re:/\bdojo:([a-zA-Z0-9_]+)\b/g,setClass:function(str){
this.getRootNode().className=str;
},getRootNode:function(){
return this.buffer.rootNode;
},getBuffer:function(){
return new dd.HtmlBuffer();
},render:function(_39,_3a){
_3a=this.buffer=_3a||this.getBuffer();
this.rootNode=null;
var _3b=this.nodelist.render(_39||new dd.Context({}),_3a);
for(var i=0,_3d;_3d=_3a._cache[i];i++){
if(_3d._cache){
_3d._cache.length=0;
}
}
return _3b;
},unrender:function(_3e,_3f){
return this.nodelist.unrender(_3e,_3f);
}});
dd.HtmlBuffer=dojo.extend(function(_40){
this._parent=_40;
this._cache=[];
},{concat:function(_41){
var _42=this._parent;
if(_41.parentNode&&_41.parentNode.tagName&&_42&&!_42._dirty){
return this;
}
if(_41.nodeType==1&&!this.rootNode){
this.rootNode=_41||true;
return this;
}
if(!_42){
if(_41.nodeType==3&&dojo.trim(_41.data)){
throw new Error("Text should not exist outside of the root node in template");
}
return this;
}
if(this._closed){
if(_41.nodeType==3&&!dojo.trim(_41.data)){
return this;
}else{
throw new Error("Content should not exist outside of the root node in template");
}
}
if(_42._dirty){
if(_41._drawn&&_41.parentNode==_42){
var _43=_42._cache;
if(_43){
for(var i=0,_45;_45=_43[i];i++){
this.onAddNode&&this.onAddNode(_45);
_42.insertBefore(_45,_41);
this.onAddNodeComplete&&this.onAddNodeComplete(_45);
}
_43.length=0;
}
}
_42._dirty=false;
}
if(!_42._cache){
_42._cache=[];
this._cache.push(_42);
}
_42._dirty=true;
_42._cache.push(_41);
return this;
},remove:function(obj){
if(typeof obj=="string"){
if(this._parent){
this._parent.removeAttribute(obj);
}
}else{
if(obj.nodeType==1&&!this.getRootNode()&&!this._removed){
this._removed=true;
return this;
}
if(obj.parentNode){
this.onRemoveNode&&this.onRemoveNode(obj);
if(obj.parentNode){
obj.parentNode.removeChild(obj);
}
}
}
return this;
},setAttribute:function(key,_48){
var old=dojo.attr(this._parent,key);
if(this.onChangeAttribute&&old!=_48){
this.onChangeAttribute(this._parent,key,old,_48);
}
dojo.attr(this._parent,key,_48);
return this;
},addEvent:function(_4a,_4b,fn,_4d){
if(!_4a.getThis()){
throw new Error("You must use Context.setObject(instance)");
}
this.onAddEvent&&this.onAddEvent(this.getParent(),_4b,fn);
var _4e=fn;
if(dojo.isArray(_4d)){
_4e=function(e){
this[fn].apply(this,[e].concat(_4d));
};
}
return dojo.connect(this.getParent(),_4b,_4a.getThis(),_4e);
},setParent:function(_50,up,_52){
if(!this._parent){
this._parent=this._first=_50;
}
if(up&&_52&&_50===this._first){
this._closed=true;
}
if(up){
var _53=this._parent;
var _54="";
var ie=dojo.isIE&&_53.tagName=="SCRIPT";
if(ie){
_53.text="";
}
if(_53._dirty){
var _56=_53._cache;
var _57=(_53.tagName=="SELECT"&&!_53.options.length);
for(var i=0,_59;_59=_56[i];i++){
if(_59!==_53){
this.onAddNode&&this.onAddNode(_59);
if(ie){
_54+=_59.data;
}else{
_53.appendChild(_59);
if(_57&&_59.defaultSelected&&i){
_57=i;
}
}
this.onAddNodeComplete&&this.onAddNodeComplete(_59);
}
}
if(_57){
_53.options.selectedIndex=(typeof _57=="number")?_57:0;
}
_56.length=0;
_53._dirty=false;
}
if(ie){
_53.text=_54;
}
}
this.onSetParent&&this.onSetParent(_50,up);
this._parent=_50;
return this;
},getParent:function(){
return this._parent;
},getRootNode:function(){
return this.rootNode;
}});
dd._HtmlNode=dojo.extend(function(_5a){
this.contents=_5a;
},{render:function(_5b,_5c){
this._rendered=true;
return _5c.concat(this.contents);
},unrender:function(_5d,_5e){
if(!this._rendered){
return _5e;
}
this._rendered=false;
return _5e.remove(this.contents);
},clone:function(_5f){
return new this.constructor(this.contents);
}});
dd._HtmlNodeList=dojo.extend(function(_60){
this.contents=_60||[];
},{push:function(_61){
this.contents.push(_61);
},unshift:function(_62){
this.contents.unshift(_62);
},render:function(_63,_64,_65){
_64=_64||dd.HtmlTemplate.prototype.getBuffer();
if(_65){
var _66=_64.getParent();
}
for(var i=0;i<this.contents.length;i++){
_64=this.contents[i].render(_63,_64);
if(!_64){
throw new Error("Template node render functions must return their buffer");
}
}
if(_66){
_64.setParent(_66);
}
return _64;
},dummyRender:function(_68,_69,_6a){
var div=document.createElement("div");
var _6c=_69.getParent();
var old=_6c._clone;
_6c._clone=div;
var _6e=this.clone(_69,div);
if(old){
_6c._clone=old;
}else{
_6c._clone=null;
}
_69=dd.HtmlTemplate.prototype.getBuffer();
_6e.unshift(new dd.ChangeNode(div));
_6e.unshift(new dd._HtmlNode(div));
_6e.push(new dd.ChangeNode(div,true));
_6e.render(_68,_69);
if(_6a){
return _69.getRootNode();
}
var _6f=div.innerHTML;
return (dojo.isIE)?_6f.replace(/\s*_(dirty|clone)="[^"]*"/g,""):_6f;
},unrender:function(_70,_71,_72){
if(_72){
var _73=_71.getParent();
}
for(var i=0;i<this.contents.length;i++){
_71=this.contents[i].unrender(_70,_71);
if(!_71){
throw new Error("Template node render functions must return their buffer");
}
}
if(_73){
_71.setParent(_73);
}
return _71;
},clone:function(_75){
var _76=_75.getParent();
var _77=this.contents;
var _78=new dd._HtmlNodeList();
var _79=[];
for(var i=0;i<_77.length;i++){
var _7b=_77[i].clone(_75);
if(_7b instanceof dd.ChangeNode||_7b instanceof dd._HtmlNode){
var _7c=_7b.contents._clone;
if(_7c){
_7b.contents=_7c;
}else{
if(_76!=_7b.contents&&_7b instanceof dd._HtmlNode){
var _7d=_7b.contents;
_7b.contents=_7b.contents.cloneNode(false);
_75.onClone&&_75.onClone(_7d,_7b.contents);
_79.push(_7d);
_7d._clone=_7b.contents;
}
}
}
_78.push(_7b);
}
for(var i=0,_7b;_7b=_79[i];i++){
_7b._clone=null;
}
return _78;
},rtrim:function(){
while(1){
i=this.contents.length-1;
if(this.contents[i] instanceof dd._HtmlTextNode&&this.contents[i].isEmpty()){
this.contents.pop();
}else{
break;
}
}
return this;
}});
dd._HtmlVarNode=dojo.extend(function(str){
this.contents=new dd._Filter(str);
},{render:function(_7f,_80){
var str=this.contents.resolve(_7f);
var _82="text";
if(str){
if(str.render&&str.getRootNode){
_82="injection";
}else{
if(str.safe){
if(str.nodeType){
_82="node";
}else{
if(str.toString){
str=str.toString();
_82="html";
}
}
}
}
}
if(this._type&&_82!=this._type){
this.unrender(_7f,_80);
}
this._type=_82;
switch(_82){
case "text":
this._rendered=true;
this._txt=this._txt||document.createTextNode(str);
if(this._txt.data!=str){
var old=this._txt.data;
this._txt.data=str;
_80.onChangeData&&_80.onChangeData(this._txt,old,this._txt.data);
}
return _80.concat(this._txt);
case "injection":
var _84=str.getRootNode();
if(this._rendered&&_84!=this._root){
_80=this.unrender(_7f,_80);
}
this._root=_84;
var _85=this._injected=new dd._HtmlNodeList();
_85.push(new dd.ChangeNode(_80.getParent()));
_85.push(new dd._HtmlNode(_84));
_85.push(str);
_85.push(new dd.ChangeNode(_80.getParent()));
this._rendered=true;
return _85.render(_7f,_80);
case "node":
this._rendered=true;
this._node=str;
return _80.concat(str);
case "html":
if(this._rendered&&this._src!=str){
_80=this.unrender(_7f,_80);
}
this._src=str;
if(!this._rendered){
this._rendered=true;
this._html=this._html||[];
var div=(this._div=this._div||document.createElement("div"));
div.innerHTML=str;
var _87=div.childNodes;
while(_87.length){
var _88=div.removeChild(_87[0]);
this._html.push(_88);
_80=_80.concat(_88);
}
}
return _80;
defaul:
return _80;
}
},unrender:function(_89,_8a){
if(!this._rendered){
return _8a;
}
this._rendered=false;
switch(this._type){
case "text":
return _8a.remove(this._txt);
case "injection":
return this._injection.unrender(_89,_8a);
case "node":
return _8a.remove(this._node);
case "html":
for(var i=0,l=this._html.length;i<l;i++){
_8a=_8a.remove(this._html[i]);
}
return _8a;
default:
return _8a;
}
},clone:function(){
return new this.constructor(this.contents.getExpression());
}});
dd.ChangeNode=dojo.extend(function(_8d,up,_8f){
this.contents=_8d;
this.up=up;
this.root=_8f;
},{render:function(_90,_91){
return _91.setParent(this.contents,this.up,this.root);
},unrender:function(_92,_93){
if(!_93.getParent()){
return _93;
}
return _93.setParent(this.contents);
},clone:function(){
return new this.constructor(this.contents,this.up,this.root);
}});
dd.AttributeNode=dojo.extend(function(key,_95){
this.key=key;
this.value=_95;
if(this._pool[_95]){
this.nodelist=this._pool[_95];
}else{
if(!(this.nodelist=dd.quickFilter(_95))){
this.nodelist=(new dd.Template(_95,true)).nodelist;
}
this._pool[_95]=this.nodelist;
}
this.contents="";
},{_pool:{},render:function(_96,_97){
var key=this.key;
var _99=this.nodelist.dummyRender(_96);
if(this._rendered){
if(_99!=this.contents){
this.contents=_99;
return _97.setAttribute(key,_99);
}
}else{
this._rendered=true;
this.contents=_99;
return _97.setAttribute(key,_99);
}
return _97;
},unrender:function(_9a,_9b){
this._rendered=false;
return _9b.remove(this.key);
},clone:function(_9c){
return new this.constructor(this.key,this.value);
}});
dd._HtmlTextNode=dojo.extend(function(str){
this.contents=document.createTextNode(str);
this.upcoming=str;
},{set:function(_9e){
this.upcoming=_9e;
return this;
},render:function(_9f,_a0){
if(this.contents.data!=this.upcoming){
var old=this.contents.data;
this.contents.data=this.upcoming;
_a0.onChangeData&&_a0.onChangeData(this.contents,old,this.upcoming);
}
return _a0.concat(this.contents);
},unrender:function(_a2,_a3){
return _a3.remove(this.contents);
},isEmpty:function(){
return !dojo.trim(this.contents.data);
},clone:function(){
return new this.constructor(this.contents.data);
}});
dd._HtmlParser=dojo.extend(function(_a4){
this.contents=_a4;
},{i:0,parse:function(_a5){
var _a6={};
var _a7=this.contents;
if(!_a5){
_a5=[];
}
for(var i=0;i<_a5.length;i++){
_a6[_a5[i]]=true;
}
var _a9=new dd._HtmlNodeList();
while(this.i<_a7.length){
var _aa=_a7[this.i++];
var _ab=_aa[0];
var _ac=_aa[1];
if(_ab==dd.TOKEN_CUSTOM){
_a9.push(_ac);
}else{
if(_ab==dd.TOKEN_CHANGE){
var _ad=new dd.ChangeNode(_ac,_aa[2],_aa[3]);
_ac[_ad.attr]=_ad;
_a9.push(_ad);
}else{
if(_ab==dd.TOKEN_ATTR){
var fn=_2.getTag("attr:"+_aa[2],true);
if(fn&&_aa[3]){
_a9.push(fn(null,new dd.Token(_ab,_aa[2]+" "+_aa[3])));
}else{
if(dojo.isString(_aa[3])&&(_aa[3].indexOf("{%")!=-1||_aa[3].indexOf("{{")!=-1)){
_a9.push(new dd.AttributeNode(_aa[2],_aa[3]));
}
}
}else{
if(_ab==dd.TOKEN_NODE){
var fn=_2.getTag("node:"+_ac.tagName.toLowerCase(),true);
if(fn){
_a9.push(fn(null,new dd.Token(_ab,_ac),_ac.tagName.toLowerCase()));
}
_a9.push(new dd._HtmlNode(_ac));
}else{
if(_ab==dd.TOKEN_VAR){
_a9.push(new dd._HtmlVarNode(_ac));
}else{
if(_ab==dd.TOKEN_TEXT){
_a9.push(new dd._HtmlTextNode(_ac.data||_ac));
}else{
if(_ab==dd.TOKEN_BLOCK){
if(_a6[_ac]){
--this.i;
return _a9;
}
var cmd=_ac.split(/\s+/g);
if(cmd.length){
cmd=cmd[0];
var fn=_2.getTag(cmd);
if(typeof fn!="function"){
throw new Error("Function not found for "+cmd);
}
var tpl=fn(this,new dd.Token(_ab,_ac));
if(tpl){
_a9.push(tpl);
}
}
}
}
}
}
}
}
}
}
if(_a5.length){
throw new Error("Could not find closing tag(s): "+_a5.toString());
}
return _a9;
},next_token:function(){
var _b1=this.contents[this.i++];
return new dd.Token(_b1[0],_b1[1]);
},delete_first_token:function(){
this.i++;
},skip_past:function(_b2){
return dd.Parser.prototype.skip_past.call(this,_b2);
},create_variable_node:function(_b3){
return new dd._HtmlVarNode(_b3);
},create_text_node:function(_b4){
return new dd._HtmlTextNode(_b4||"");
},getTemplate:function(loc){
return new dd.HtmlTemplate(_3.getTemplate(loc));
}});
})();
}
