/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.form.SimpleTextarea"]){
dojo._hasResource["dijit.form.SimpleTextarea"]=true;
dojo.provide("dijit.form.SimpleTextarea");
dojo.require("dijit.form.TextBox");
dojo.declare("dijit.form.SimpleTextarea",dijit.form.TextBox,{baseClass:"dijitTextArea",attributeMap:dojo.mixin(dojo.clone(dijit.form._FormValueWidget.prototype.attributeMap),{rows:"textbox",cols:"textbox"}),rows:"",cols:"",templatePath:null,templateString:"<textarea name='${name}' dojoAttachPoint='focusNode,containerNode,textbox' autocomplete='off'></textarea>",postMixInProperties:function(){
if(this.srcNodeRef){
this.value=this.srcNodeRef.value;
}
},filter:function(_1){
if(_1){
_1=_1.replace(/\r/g,"");
}
return this.inherited(arguments);
}});
}
