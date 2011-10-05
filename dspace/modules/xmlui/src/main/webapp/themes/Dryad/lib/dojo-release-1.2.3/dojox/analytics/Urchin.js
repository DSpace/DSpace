/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.analytics.Urchin"]){
dojo._hasResource["dojox.analytics.Urchin"]=true;
dojo.provide("dojox.analytics.Urchin");
dojo.declare("dojox.analytics.Urchin",null,{acct:dojo.config.urchin,loadInterval:420,constructor:function(_1){
this.tracker=null;
dojo.mixin(this,_1);
this._loadGA.apply(this,arguments);
},_loadGA:function(){
var _2=("https:"==document.location.protocol)?"https://ssl.":"http://www.";
var s=dojo.doc.createElement("script");
s.src=_2+"google-analytics.com/ga.js";
dojo.doc.getElementsByTagName("head")[0].appendChild(s);
setTimeout(dojo.hitch(this,"_checkGA"),this.loadInterval);
},_checkGA:function(){
setTimeout(dojo.hitch(this,!window["_gat"]?"_checkGA":"_gotGA"),this.loadInterval);
},_gotGA:function(){
this.tracker=_gat._getTracker(this.acct);
this.tracker._initData();
this.GAonLoad.apply(this,arguments);
},GAonLoad:function(){
this.trackPageView();
},trackPageView:function(_4){
this.tracker._trackPageview.apply(this,arguments);
}});
}
