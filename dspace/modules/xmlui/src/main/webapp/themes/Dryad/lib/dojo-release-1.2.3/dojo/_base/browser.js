/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo._base.browser"]){
dojo._hasResource["dojo._base.browser"]=true;
dojo.provide("dojo._base.browser");
dojo.require("dojo._base.window");
dojo.require("dojo._base.event");
dojo.require("dojo._base.html");
dojo.require("dojo._base.NodeList");
dojo.require("dojo._base.query");
dojo.require("dojo._base.xhr");
dojo.require("dojo._base.fx");
if(dojo.config.require){
dojo.forEach(dojo.config.require,"dojo['require'](item);");
}
}
