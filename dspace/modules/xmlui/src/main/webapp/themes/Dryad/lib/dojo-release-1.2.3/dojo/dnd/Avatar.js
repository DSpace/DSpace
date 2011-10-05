/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.dnd.Avatar"]){
dojo._hasResource["dojo.dnd.Avatar"]=true;
dojo.provide("dojo.dnd.Avatar");
dojo.require("dojo.dnd.common");
dojo.declare("dojo.dnd.Avatar",null,{constructor:function(_1){
this.manager=_1;
this.construct();
},construct:function(){
var a=dojo.doc.createElement("table");
a.className="dojoDndAvatar";
a.style.position="absolute";
a.style.zIndex=1999;
a.style.margin="0px";
var b=dojo.doc.createElement("tbody");
var tr=dojo.doc.createElement("tr");
tr.className="dojoDndAvatarHeader";
var td=dojo.doc.createElement("td");
td.innerHTML=this._generateText();
tr.appendChild(td);
dojo.style(tr,"opacity",0.9);
b.appendChild(tr);
var k=Math.min(5,this.manager.nodes.length);
var _7=this.manager.source,_8;
for(var i=0;i<k;++i){
tr=dojo.doc.createElement("tr");
tr.className="dojoDndAvatarItem";
td=dojo.doc.createElement("td");
if(_7.creator){
_8=_7._normalizedCreator(_7.getItem(this.manager.nodes[i].id).data,"avatar").node;
}else{
_8=this.manager.nodes[i].cloneNode(true);
if(_8.tagName.toLowerCase()=="tr"){
var _a=dojo.doc.createElement("table"),_b=dojo.doc.createElement("tbody");
_b.appendChild(_8);
_a.appendChild(_b);
_8=_a;
}
}
_8.id="";
td.appendChild(_8);
tr.appendChild(td);
dojo.style(tr,"opacity",(9-i)/10);
b.appendChild(tr);
}
a.appendChild(b);
this.node=a;
},destroy:function(){
dojo._destroyElement(this.node);
this.node=false;
},update:function(){
dojo[(this.manager.canDropFlag?"add":"remove")+"Class"](this.node,"dojoDndAvatarCanDrop");
dojo.query("tr.dojoDndAvatarHeader td",this.node).forEach(function(_c){
_c.innerHTML=this._generateText();
},this);
},_generateText:function(){
return this.manager.nodes.length.toString();
}});
}
