/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.dtl.tag.misc"]){
dojo._hasResource["dojox.dtl.tag.misc"]=true;
dojo.provide("dojox.dtl.tag.misc");
dojo.require("dojox.dtl._base");
(function(){
var dd=dojox.dtl;
var _2=dd.tag.misc;
_2.DebugNode=dojo.extend(function(_3){
this.text=_3;
},{render:function(_4,_5){
var _6=_4.getKeys();
var _7="";
for(var i=0,_9;_9=_6[i];i++){

_7+=_9+": "+dojo.toJson(_4[_9])+"\n\n";
}
return this.text.set(_7).render(_4,_5,this);
},unrender:function(_a,_b){
return _b;
},clone:function(_c){
return new this.constructor(this.text.clone(_c));
},toString:function(){
return "ddtm.DebugNode";
}});
_2.FilterNode=dojo.extend(function(_d,_e){
this._varnode=_d;
this._nodelist=_e;
},{render:function(_f,_10){
var _11=this._nodelist.render(_f,new dojox.string.Builder());
_f=_f.update({"var":_11.toString()});
var _12=this._varnode.render(_f,_10);
_f=_f.pop();
return _10;
},unrender:function(_13,_14){
return _14;
},clone:function(_15){
return new this.constructor(this._expression,this._nodelist.clone(_15));
}});
_2.FirstOfNode=dojo.extend(function(_16,_17){
this._vars=_16;
this.vars=dojo.map(_16,function(_18){
return new dojox.dtl._Filter(_18);
});
this.contents=_17;
},{render:function(_19,_1a){
for(var i=0,_1c;_1c=this.vars[i];i++){
var _1d=_1c.resolve(_19);
if(typeof _1d!="undefined"){
if(_1d===null){
_1d="null";
}
this.contents.set(_1d);
return this.contents.render(_19,_1a);
}
}
return this.contents.unrender(_19,_1a);
},unrender:function(_1e,_1f){
return this.contents.unrender(_1e,_1f);
},clone:function(_20){
return new this.constructor(this._vars,this.contents.clone(_20));
}});
_2.SpacelessNode=dojo.extend(function(_21,_22){
this.nodelist=_21;
this.contents=_22;
},{render:function(_23,_24){
if(_24.getParent){
var _25=[dojo.connect(_24,"onAddNodeComplete",this,"_watch"),dojo.connect(_24,"onSetParent",this,"_watchParent")];
_24=this.nodelist.render(_23,_24);
dojo.disconnect(_25[0]);
dojo.disconnect(_25[1]);
}else{
var _26=this.nodelist.dummyRender(_23);
this.contents.set(_26.replace(/>\s+</g,"><"));
_24=this.contents.render(_23,_24);
}
return _24;
},unrender:function(_27,_28){
return this.nodelist.unrender(_27,_28);
},clone:function(_29){
return new this.constructor(this.nodelist.clone(_29),this.contents.clone(_29));
},_isEmpty:function(_2a){
return (_2a.nodeType==3&&!_2a.data.match(/[^\s\n]/));
},_watch:function(_2b){
if(this._isEmpty(_2b)){
var _2c=false;
if(_2b.parentNode.firstChild==_2b){
_2b.parentNode.removeChild(_2b);
}
}else{
var _2d=_2b.parentNode.childNodes;
if(_2b.nodeType==1&&_2d.length>2){
for(var i=2,_2f;_2f=_2d[i];i++){
if(_2d[i-2].nodeType==1&&this._isEmpty(_2d[i-1])){
_2b.parentNode.removeChild(_2d[i-1]);
return;
}
}
}
}
},_watchParent:function(_30){
var _31=_30.childNodes;
if(_31.length){
while(_30.childNodes.length){
var _32=_30.childNodes[_30.childNodes.length-1];
if(!this._isEmpty(_32)){
return;
}
_30.removeChild(_32);
}
}
}});
_2.TemplateTagNode=dojo.extend(function(tag,_34){
this.tag=tag;
this.contents=_34;
},{mapping:{openblock:"{%",closeblock:"%}",openvariable:"{{",closevariable:"}}",openbrace:"{",closebrace:"}",opencomment:"{#",closecomment:"#}"},render:function(_35,_36){
this.contents.set(this.mapping[this.tag]);
return this.contents.render(_35,_36);
},unrender:function(_37,_38){
return this.contents.unrender(_37,_38);
},clone:function(_39){
return new this.constructor(this.tag,this.contents.clone(_39));
}});
_2.WidthRatioNode=dojo.extend(function(_3a,max,_3c,_3d){
this.current=new dd._Filter(_3a);
this.max=new dd._Filter(max);
this.width=_3c;
this.contents=_3d;
},{render:function(_3e,_3f){
var _40=+this.current.resolve(_3e);
var max=+this.max.resolve(_3e);
if(typeof _40!="number"||typeof max!="number"||!max){
this.contents.set("");
}else{
this.contents.set(""+Math.round((_40/max)*this.width));
}
return this.contents.render(_3e,_3f);
},unrender:function(_42,_43){
return this.contents.unrender(_42,_43);
},clone:function(_44){
return new this.constructor(this.current.getExpression(),this.max.getExpression(),this.width,this.contents.clone(_44));
}});
_2.WithNode=dojo.extend(function(_45,_46,_47){
this.target=new dd._Filter(_45);
this.alias=_46;
this.nodelist=_47;
},{render:function(_48,_49){
var _4a=this.target.resolve(_48);
_48=_48.push();
_48[this.alias]=_4a;
_49=this.nodelist.render(_48,_49);
_48=_48.pop();
return _49;
},unrender:function(_4b,_4c){
return _4c;
},clone:function(_4d){
return new this.constructor(this.target.getExpression(),this.alias,this.nodelist.clone(_4d));
}});
dojo.mixin(_2,{comment:function(_4e,_4f){
_4e.skip_past("endcomment");
return dd._noOpNode;
},debug:function(_50,_51){
return new _2.DebugNode(_50.create_text_node());
},filter:function(_52,_53){
var _54=_53.contents.split(null,1)[1];
var _55=_52.create_variable_node("var|"+_54);
var _56=_52.parse(["endfilter"]);
_52.next_token();
return new _2.FilterNode(_55,_56);
},firstof:function(_57,_58){
var _59=_58.split_contents().slice(1);
if(!_59.length){
throw new Error("'firstof' statement requires at least one argument");
}
return new _2.FirstOfNode(_59,_57.create_text_node());
},spaceless:function(_5a,_5b){
var _5c=_5a.parse(["endspaceless"]);
_5a.delete_first_token();
return new _2.SpacelessNode(_5c,_5a.create_text_node());
},templatetag:function(_5d,_5e){
var _5f=_5e.contents.split();
if(_5f.length!=2){
throw new Error("'templatetag' statement takes one argument");
}
var tag=_5f[1];
var _61=_2.TemplateTagNode.prototype.mapping;
if(!_61[tag]){
var _62=[];
for(var key in _61){
_62.push(key);
}
throw new Error("Invalid templatetag argument: '"+tag+"'. Must be one of: "+_62.join(", "));
}
return new _2.TemplateTagNode(tag,_5d.create_text_node());
},widthratio:function(_64,_65){
var _66=_65.contents.split();
if(_66.length!=4){
throw new Error("widthratio takes three arguments");
}
var _67=+_66[3];
if(typeof _67!="number"){
throw new Error("widthratio final argument must be an integer");
}
return new _2.WidthRatioNode(_66[1],_66[2],_67,_64.create_text_node());
},with_:function(_68,_69){
var _6a=_69.split_contents();
if(_6a.length!=4||_6a[2]!="as"){
throw new Error("do_width expected format as 'with value as name'");
}
var _6b=_68.parse(["endwith"]);
_68.next_token();
return new _2.WithNode(_6a[1],_6a[3],_6b);
}});
})();
}
