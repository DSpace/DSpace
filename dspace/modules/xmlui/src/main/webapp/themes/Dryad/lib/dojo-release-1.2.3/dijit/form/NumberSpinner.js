/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dijit.form.NumberSpinner"]){
dojo._hasResource["dijit.form.NumberSpinner"]=true;
dojo.provide("dijit.form.NumberSpinner");
dojo.require("dijit.form._Spinner");
dojo.require("dijit.form.NumberTextBox");
dojo.declare("dijit.form.NumberSpinner",[dijit.form._Spinner,dijit.form.NumberTextBoxMixin],{required:true,adjust:function(_1,_2){
if(isNaN(_1)&&_2!=0){
var _3=(_2>0),_4=(typeof this.constraints.max=="number"),_5=(typeof this.constraints.min=="number");
_1=_3?(_5?this.constraints.min:(_4?this.constraints.max:0)):(_4?this.constraints.max:(_5?this.constraints.min:0));
}
var _6=_1+_2;
if(isNaN(_1)||isNaN(_6)){
return _1;
}
if((typeof this.constraints.max=="number")&&(_6>this.constraints.max)){
_6=this.constraints.max;
}
if((typeof this.constraints.min=="number")&&(_6<this.constraints.min)){
_6=this.constraints.min;
}
return _6;
},_onKeyPress:function(e){
if((e.charOrCode==dojo.keys.HOME||e.charOrCode==dojo.keys.END)&&!e.ctrlKey&&!e.altKey){
var _8=e.charOrCode==dojo.keys.HOME?this.constraints["min"]:this.constraints["max"];
if(_8){
this._setValueAttr(_8,true);
}
dojo.stopEvent(e);
return false;
}else{
return this.inherited(arguments);
}
}});
}
