/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.Declaration"]){
dojo._hasResource["dijit.Declaration"]=true;
dojo.provide("dijit.Declaration");
dojo.require("dijit._Widget");
dojo.require("dijit._Templated");
dojo.declare("dijit.Declaration",dijit._Widget,{_noScript:true,widgetClass:"",replaceVars:true,defaults:null,mixins:[],buildRendering:function(){
var _1=this.srcNodeRef.parentNode.removeChild(this.srcNodeRef),_2=dojo.query("> script[type='dojo/method'][event='preamble']",_1).orphan(),_3=dojo.query("> script[type^='dojo/method'][event]",_1).orphan(),_4=dojo.query("> script[type^='dojo/method']",_1).orphan(),_5=dojo.query("> script[type^='dojo/connect']",_1).orphan(),_6=_1.nodeName;
var _7=this.defaults||{};
dojo.forEach(_3,function(s){
var _9=s.getAttribute("event"),_a=dojo.parser._functionFromScript(s);
_7[_9]=_a;
});
this.mixins=this.mixins.length?dojo.map(this.mixins,function(_b){
return dojo.getObject(_b);
}):[dijit._Widget,dijit._Templated];
_7.widgetsInTemplate=true;
_7._skipNodeCache=true;
_7.templateString="<"+_6+" class='"+_1.className+"' dojoAttachPoint='"+(_1.getAttribute("dojoAttachPoint")||"")+"' dojoAttachEvent='"+(_1.getAttribute("dojoAttachEvent")||"")+"' >"+_1.innerHTML.replace(/\%7B/g,"{").replace(/\%7D/g,"}")+"</"+_6+">";
dojo.query("[dojoType]",_1).forEach(function(_c){
_c.removeAttribute("dojoType");
});
var wc=dojo.declare(this.widgetClass,this.mixins,_7);
var _e=_5.concat(_4);
dojo.forEach(_e,function(s){
var evt=s.getAttribute("event")||"postscript",_11=dojo.parser._functionFromScript(s);
dojo.connect(wc.prototype,evt,_11);
});
}});
}
