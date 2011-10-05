/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.dtl.tag.loader"]){
dojo._hasResource["dojox.dtl.tag.loader"]=true;
dojo.provide("dojox.dtl.tag.loader");
dojo.require("dojox.dtl._base");
(function(){
var dd=dojox.dtl;
var _2=dd.tag.loader;
_2.BlockNode=dojo.extend(function(_3,_4){
this.name=_3;
this.nodelist=_4;
},{"super":function(){
if(this.parent){
var _5=this.parent.nodelist.dummyRender(this.context,null,true);
if(typeof _5=="string"){
_5=new String(_5);
}
_5.safe=true;
return _5;
}
return "";
},render:function(_6,_7){
var _8=this.name;
var _9=this.nodelist;
var _a;
if(_7.blocks){
var _b=_7.blocks[_8];
if(_b){
_a=_b.parent;
_9=_b.nodelist;
_b.used=true;
}
}
this.rendered=_9;
_6=_6.push();
this.context=_6;
this.parent=null;
if(_9!=this.nodelist){
this.parent=this;
}
_6["block"]=this;
_7=_9.render(_6,_7,this);
_6=_6.pop();
return _7;
},unrender:function(_c,_d){
return this.rendered.unrender(_c,_d);
},clone:function(_e){
return new this.constructor(this.name,this.nodelist.clone(_e));
},toString:function(){
return "dojox.dtl.tag.loader.BlockNode";
}});
_2.ExtendsNode=dojo.extend(function(_f,_10,_11,_12,key){
this.getTemplate=_f;
this.nodelist=_10;
this.shared=_11;
this.parent=_12;
this.key=key;
},{parents:{},getParent:function(_14){
var _15=this.parent;
if(!_15){
var _16;
_15=this.parent=_14.get(this.key,false);
if(!_15){
throw new Error("extends tag used a variable that did not resolve");
}
if(typeof _15=="object"){
var url=_15.url||_15.templatePath;
if(_15.shared){
this.shared=true;
}
if(url){
_15=this.parent=url.toString();
}else{
if(_15.templateString){
_16=_15.templateString;
_15=this.parent=" ";
}else{
_15=this.parent=this.parent.toString();
}
}
}
if(_15&&_15.indexOf("shared:")==0){
this.shared=true;
_15=this.parent=_15.substring(7,_15.length);
}
}
if(!_15){
throw new Error("Invalid template name in 'extends' tag.");
}
if(_15.render){
return _15;
}
if(this.parents[_15]){
return this.parents[_15];
}
this.parent=this.getTemplate(_16||dojox.dtl.text.getTemplateString(_15));
if(this.shared){
this.parents[_15]=this.parent;
}
return this.parent;
},render:function(_18,_19){
var _1a=this.getParent(_18);
_1a.blocks=_1a.blocks||{};
_19.blocks=_19.blocks||{};
for(var i=0,_1c;_1c=this.nodelist.contents[i];i++){
if(_1c instanceof dojox.dtl.tag.loader.BlockNode){
var old=_1a.blocks[_1c.name];
if(old&&old.nodelist!=_1c.nodelist){
_19=old.nodelist.unrender(_18,_19);
}
_1a.blocks[_1c.name]=_19.blocks[_1c.name]={shared:this.shared,nodelist:_1c.nodelist,used:false};
}
}
this.rendered=_1a;
return _1a.nodelist.render(_18,_19,this);
},unrender:function(_1e,_1f){
return this.rendered.unrender(_1e,_1f,this);
},toString:function(){
return "dojox.dtl.block.ExtendsNode";
}});
_2.IncludeNode=dojo.extend(function(_20,_21,_22,_23,_24){
this._path=_20;
this.constant=_21;
this.path=(_21)?_20:new dd._Filter(_20);
this.getTemplate=_22;
this.text=_23;
this.parsed=(arguments.length==5)?_24:true;
},{_cache:[{},{}],render:function(_25,_26){
var _27=((this.constant)?this.path:this.path.resolve(_25)).toString();
var _28=Number(this.parsed);
var _29=false;
if(_27!=this.last){
_29=true;
if(this.last){
_26=this.unrender(_25,_26);
}
this.last=_27;
}
var _2a=this._cache[_28];
if(_28){
if(!_2a[_27]){
_2a[_27]=dd.text._resolveTemplateArg(_27,true);
}
if(_29){
var _2b=this.getTemplate(_2a[_27]);
this.rendered=_2b.nodelist;
}
return this.rendered.render(_25,_26,this);
}else{
if(this.text instanceof dd._TextNode){
if(_29){
this.rendered=this.text;
this.rendered.set(dd.text._resolveTemplateArg(_27,true));
}
return this.rendered.render(_25,_26);
}else{
if(!_2a[_27]){
var _2c=[];
var div=document.createElement("div");
div.innerHTML=dd.text._resolveTemplateArg(_27,true);
var _2e=div.childNodes;
while(_2e.length){
var _2f=div.removeChild(_2e[0]);
_2c.push(_2f);
}
_2a[_27]=_2c;
}
if(_29){
this.nodelist=[];
var _30=true;
for(var i=0,_32;_32=_2a[_27][i];i++){
this.nodelist.push(_32.cloneNode(true));
}
}
for(var i=0,_33;_33=this.nodelist[i];i++){
_26=_26.concat(_33);
}
}
}
return _26;
},unrender:function(_34,_35){
if(this.rendered){
_35=this.rendered.unrender(_34,_35);
}
if(this.nodelist){
for(var i=0,_37;_37=this.nodelist[i];i++){
_35=_35.remove(_37);
}
}
return _35;
},clone:function(_38){
return new this.constructor(this._path,this.constant,this.getTemplate,this.text.clone(_38),this.parsed);
}});
dojo.mixin(_2,{block:function(_39,_3a){
var _3b=_3a.contents.split();
var _3c=_3b[1];
_39._blocks=_39._blocks||{};
_39._blocks[_3c]=_39._blocks[_3c]||[];
_39._blocks[_3c].push(_3c);
var _3d=_39.parse(["endblock","endblock "+_3c]).rtrim();
_39.next_token();
return new dojox.dtl.tag.loader.BlockNode(_3c,_3d);
},extends_:function(_3e,_3f){
var _40=_3f.contents.split();
var _41=false;
var _42=null;
var key=null;
if(_40[1].charAt(0)=="\""||_40[1].charAt(0)=="'"){
_42=_40[1].substring(1,_40[1].length-1);
}else{
key=_40[1];
}
if(_42&&_42.indexOf("shared:")==0){
_41=true;
_42=_42.substring(7,_42.length);
}
var _44=_3e.parse();
return new dojox.dtl.tag.loader.ExtendsNode(_3e.getTemplate,_44,_41,_42,key);
},include:function(_45,_46){
var _47=_46.contents.split();
if(_47.length!=2){
throw new Error(_47[0]+" tag takes one argument: the name of the template to be included");
}
var _48=_47[1];
var _49=false;
if((_48.charAt(0)=="\""||_48.slice(-1)=="'")&&_48.charAt(0)==_48.slice(-1)){
_48=_48.slice(1,-1);
_49=true;
}
return new _2.IncludeNode(_48,_49,_45.getTemplate,_45.create_text_node());
},ssi:function(_4a,_4b){
var _4c=_4b.contents.split();
var _4d=false;
if(_4c.length==3){
_4d=(_4c.pop()=="parsed");
if(!_4d){
throw new Error("Second (optional) argument to ssi tag must be 'parsed'");
}
}
var _4e=_2.include(_4a,new dd.Token(_4b.token_type,_4c.join(" ")));
_4e.parsed=_4d;
return _4e;
}});
})();
}
