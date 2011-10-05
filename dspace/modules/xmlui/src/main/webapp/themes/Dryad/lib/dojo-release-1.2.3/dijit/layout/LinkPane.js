/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.layout.LinkPane"]){
dojo._hasResource["dijit.layout.LinkPane"]=true;
dojo.provide("dijit.layout.LinkPane");
dojo.require("dijit.layout.ContentPane");
dojo.require("dijit._Templated");
dojo.declare("dijit.layout.LinkPane",[dijit.layout.ContentPane,dijit._Templated],{templateString:"<div class=\"dijitLinkPane\"></div>",buildRendering:function(){
this.inherited(arguments);
this.containerNode=this.domNode;
},postCreate:function(){
if(this.srcNodeRef){
this.title+=this.srcNodeRef.innerHTML;
}
this.inherited("postCreate",arguments);
}});
}
