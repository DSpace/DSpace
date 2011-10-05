/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.form._FormSelectWidget"]){
dojo._hasResource["dojox.form._FormSelectWidget"]=true;
dojo.provide("dojox.form._FormSelectWidget");
dojo.require("dijit.form._FormWidget");
dojo.declare("dojox.form._FormSelectWidget",dijit.form._FormValueWidget,{multiple:"",_multiValue:false,options:null,getOptions:function(_1){
var _2=_1,_3=this.options||[],l=_3.length;
if(_2===undefined){
return _3;
}
if(dojo.isArray(_2)){
return dojo.map(_2,"return this.getOptions(item);",this);
}
if(dojo.isObject(_1)){
if(!dojo.some(this.options,function(o,_6){
if(o===_2||(o.value&&o.value===_2.value)){
_2=_6;
return true;
}
return false;
})){
_2=-1;
}
}
if(typeof _2=="string"){
for(var i=0;i<l;i++){
if(_3[i].value===_2){
_2=i;
break;
}
}
}
if(typeof _2=="number"&&_2>=0&&_2<l){
return this.options[_2];
}
return null;
},addOption:function(_8){
if(!dojo.isArray(_8)){
_8=[_8];
}
dojo.forEach(_8,function(i){
if(i&&dojo.isObject(i)){
this.options.push(i);
}
},this);
this._loadChildren();
},removeOption:function(_a){
if(!dojo.isArray(_a)){
_a=[_a];
}
var _b=this.getOptions(_a);
dojo.forEach(_b,function(i){
this.options=dojo.filter(this.options,function(_d,_e){
return (_d.value!==i.value);
});
this._removeOptionItem(i);
},this);
this._loadChildren();
},updateOption:function(_f){
if(!dojo.isArray(_f)){
_f=[_f];
}
dojo.forEach(_f,function(i){
var _11=this.getOptions(i),k;
if(_11){
for(k in i){
_11[k]=i[k];
}
}
},this);
this._loadChildren();
},_setValueAttr:function(_13,_14){
var _15=this.getOptions()||[];
if(!dojo.isArray(_13)){
_13=[_13];
}
dojo.forEach(_13,function(i,idx){
if(!dojo.isObject(i)){
i=i+"";
}
if(typeof i==="string"){
_13[idx]=dojo.filter(_15,function(_18){
return _18.value===i;
})[0]||{value:"",label:""};
}
},this);
_13=dojo.filter(_13,function(i){
return i&&i.value;
});
if(!this._multiValue&&(!_13[0]||!_13[0].value)&&_15.length){
_13[0]=_15[0];
}
dojo.forEach(_15,function(i){
i.selected=dojo.some(_13,function(v){
return v.value===i.value;
});
});
var val=dojo.map(_13,function(i){
return i.value;
}),_1e=dojo.map(_13,function(i){
return i.label;
});
this.value=this._multiValue?val:val[0];
this._setDisplay(this._multiValue?_1e:_1e[0]);
this._updateSelection();
this._handleOnChange(this.value,_14);
},_getValueDeprecated:false,getValue:function(){
return this._lastValue;
},undo:function(){
this._setValueAttr(this._lastValueReported,false);
},_loadChildren:function(){
dojo.forEach(this._getChildren(),function(_20){
_20.destroyRecursive();
});
dojo.forEach(this.options,this._addOptionItem,this);
this._updateSelection();
},_updateSelection:function(){
this.value=this._getValueFromOpts();
var val=this.value;
if(!dojo.isArray(val)){
val=[val];
}
if(val&&val[0]){
dojo.forEach(this._getChildren(),function(_22){
var _23=dojo.some(val,function(v){
return _22.option&&(v===_22.option.value);
});
dojo.toggleClass(_22.domNode,this.baseClass+"SelectedOption",_23);
dijit.setWaiState(_22.domNode,"selected",_23);
},this);
}
this._handleOnChange(this.value);
},_getValueFromOpts:function(){
var _25=this.getOptions()||[];
if(!this._multiValue&&_25.length){
var opt=dojo.filter(_25,function(i){
return i.selected;
})[0];
if(opt&&opt.value){
return opt.value;
}else{
_25[0].selected=true;
return _25[0].value;
}
}else{
if(this._multiValue){
return dojo.map(dojo.filter(_25,function(i){
return i.selected;
}),function(i){
return i.value;
})||[];
}
}
return "";
},postMixInProperties:function(){
this._multiValue=(this.multiple.toLowerCase()==="true");
this.inherited(arguments);
},_fillContent:function(){
var _2a=this.options;
if(!_2a){
_2a=this.options=this.srcNodeRef?dojo.query(">",this.srcNodeRef).map(function(_2b){
if(_2b.getAttribute("type")==="separator"){
return {value:"",label:"",selected:false,disabled:false};
}
return {value:_2b.getAttribute("value"),label:String(_2b.innerHTML),selected:_2b.getAttribute("selected")||false,disabled:_2b.getAttribute("disabled")||false};
},this):[];
}
if(!this.value){
this.value=this._getValueFromOpts();
}else{
if(this._multiValue&&typeof this.value=="string"){
this.value=this.value.split(",");
}
}
},postCreate:function(){
dojo.setSelectable(this.focusNode,false);
this.inherited(arguments);
this.connect(this,"onChange","_updateSelection");
this.connect(this,"startup","_loadChildren");
this._setValueAttr(this.value,null);
},_addOptionItem:function(_2c){
},_removeOptionItem:function(_2d){
},_setDisplay:function(_2e){
},_getChildren:function(){
return [];
},_getSelectedOptionsAttr:function(){
return this.getOptions(this.attr("value"));
}});
}
