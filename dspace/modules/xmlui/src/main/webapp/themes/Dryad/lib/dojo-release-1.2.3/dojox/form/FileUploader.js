/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.form.FileUploader"]){
dojo._hasResource["dojox.form.FileUploader"]=true;
dojo.provide("dojox.form.FileUploader");
dojo.experimental("dojox.form.FileUploader");
dojox.form.FileUploader=function(_1){
this.degradable=false;
this.uploadUrl="";
this.button=null;
if(dojox.embed.Flash.available>9&&_1.degradable&&!dojo.isOpera){
return new dojox.form.FileInputOverlay({button:_1.button,uploadOnChange:_1.uploadOnChange,uploadUrl:_1.uploadUrl,selectMultipleFiles:_1.selectMultipleFiles,id:_1.id});
}else{
return new dojox.form.FileInputFlash({button:_1.button,uploadOnChange:_1.uploadOnChange,uploadUrl:_1.uploadUrl,id:_1.id,selectMultipleFiles:_1.selectMultipleFiles,fileMask:_1.fileMask,isDebug:_1.isDebug});
}
};
dojo.require("dojox.embed.Flash");
dojo.declare("dojox.form.FileInputFlash",null,{uploadUrl:"",button:null,uploadOnChange:false,selectMultipleFiles:true,fileMask:[],degradable:false,_swfPath:dojo.moduleUrl("dojox.form","resources/uploader.swf"),flashObject:null,flashMovie:null,flashDiv:null,constructor:function(_2){

this.fileList=[];
this._subs=[];
this._cons=[];
this.button=_2.button;
this.uploadUrl=_2.uploadUrl;
this.uploadOnChange=_2.uploadOnChange;
if(this.uploadUrl.toLowerCase().indexOf("http")<0){
var _3=window.location.href.split("/");
_3.pop();
_3=_3.join("/")+"/";
this.uploadUrl=_3+this.uploadUrl;

}
this.selectMultipleFiles=(_2.selectMultipleFiles===undefined)?this.selectMultipleFiles:_2.selectMultipleFiles;
this.fileMask=_2.fileMask||this.fileMask;
this.id=_2.id||dijit.getUniqueId("flashuploader");
var _4={path:this._swfPath.uri,width:1,height:1,vars:{uploadUrl:this.uploadUrl,uploadOnSelect:this.uploadOnChange,selectMultipleFiles:this.selectMultipleFiles,id:this.id,isDebug:_2.isDebug}};
this.flashDiv=dojo.doc.createElement("div");
dojo.body().appendChild(this.flashDiv);
dojo.style(this.flashDiv,"position","absolute");
dojo.style(this.flashDiv,"top","0");
dojo.style(this.flashDiv,"left","0");
this._subs.push(dojo.subscribe(this.id+"/filesSelected",this,"_change"));
this._subs.push(dojo.subscribe(this.id+"/filesUploaded",this,"_complete"));
this._subs.push(dojo.subscribe(this.id+"/filesProgress",this,"_progress"));
this._subs.push(dojo.subscribe(this.id+"/filesError",this,"_error"));
this.flashObject=new dojox.embed.Flash(_4,this.flashDiv);
this.flashObject.onLoad=dojo.hitch(this,function(_5){
this.flashMovie=_5;
this.flashMovie.setFileMask(this.fileMask);
});
this._cons.push(dojo.connect(this.button,"onClick",this,"_openDialog"));
},onChange:function(_6){
},onProgress:function(_7){
},onComplete:function(_8){
},onError:function(_9){
console.warn("FLASH/ERROR "+_9.type.toUpperCase()+":",_9);
},upload:function(){
this.flashMovie.doUpload();
},_error:function(_a){
this.onError(_a);
},_openDialog:function(_b){
this.flashMovie.openDialog();
},_change:function(_c){
this.fileList=this.fileList.concat(_c);
this.onChange(_c);
if(this.uploadOnChange){
this.upload();
}
},_complete:function(_d){
for(var i=0;i<this.fileList.length;i++){
this.fileList[i].percent=100;
}
this.onProgress(this.fileList);
this.fileList=[];
this.onComplete(_d);
},_progress:function(_f){
for(var i=0;i<this.fileList.length;i++){
var f=this.fileList[i];
if(f.name==_f.name){
f.bytesLoaded=_f.bytesLoaded;
f.bytesTotal=_f.bytesTotal;
f.percent=Math.ceil(f.bytesLoaded/f.bytesTotal*100);
}else{
if(!f.percent){
f.bytesLoaded=0;
f.bytesTotal=0;
f.percent=0;
}
}
}
this.onProgress(this.fileList);
},destroyAll:function(){
this.button.destroy();
this.destroy();
},destroy:function(){
if(!this.flashMovie){
this._cons.push(dojo.connect(this,"onLoad",this,"destroy"));
return;
}
dojo.forEach(this._subs,function(s){
dojo.unsubscribe(s);
});
dojo.forEach(this._cons,function(c){
dojo.disconnect(c);
});
this.flashObject.destroy();
dojo._destroyElement(this.flashDiv);
}});
dojo.require("dojo.io.iframe");
dojo.require("dojox.html.styles");
dojo.experimental("dojox.form.FileInputOverlay");
dojo.declare("dojox.form.FileInputOverlay",null,{_fileInput:null,_formNode:null,uploadUrl:"",button:null,uploadOnChange:false,fieldName:"uploadedfile",id:"",selectMultipleFiles:false,constructor:function(_14){
this.button=_14.button;
this.uploadUrl=_14.uploadUrl;
this.uploadOnChange=_14.uploadOnChange;
this.selectMultipleFiles=_14.selectMultipleFiles,this.id=_14.id||dijit.getUniqueId("fileInput");
this.fileCount=0;
this._cons=[];
this.fileInputs=[];
if(dojo.isIE==6){
setTimeout(dojo.hitch(this,"createFileInput"),1);
}else{
this.createFileInput();
}
},onChange:function(_15){
if(this.uploadOnChange){
this.upload();
}else{
if(this.selectMultipleFiles){
this.createFileInput();
}
}
},onProgress:function(_16){
},onComplete:function(_17){
for(var i=0;i<_17.length;i++){
_17[i].percent=100;
_17[i].name=_17[i].file.split("/")[_17[i].file.split("/").length-1];
}
this.onProgress(_17);
this._removeFileInput();
this.createFileInput();
},upload:function(){
dojo.io.iframe.send({url:this.uploadUrl,form:this._formNode,handleAs:"json",handle:dojo.hitch(this,function(_19,_1a,_1b){
this.onComplete(this.selectMultipleFiles?_19:[_19]);
})});
},createFileInput:function(){
if(!this.button.id){
this.button.id=dijit.getUniqueId("btn");
}
var _1c;
if(this.button.domNode){
_1c=dojo.byId(this.button.id).parentNode.parentNode;
_1c.parentNode.onmousedown=function(){
};
}else{
_1c=this.button.parentNode;
}
this._buildForm(_1c);
this._buildFileInput(_1c);
this.setPosition();
this._connectInput();
setTimeout(dojo.hitch(this,"setPosition"),500);
},setPosition:function(){
var _1d=this._getFakeButtonSize();
var _1e=dojo.marginBox(this._fileInput);
var _1f="rect(0px "+_1e.w+"px "+_1d.h+"px "+(_1e.w-_1d.w)+"px)";
this._fileInput.style.clip=_1f;
this._fileInput.style.left=(_1d.x+_1d.w-_1e.w)+"px";
this._fileInput.style.top=_1d.y+"px";
},_getFakeButtonSize:function(){
var _20=(this.button.domNode)?dojo.byId(this.button.id).parentNode:dojo.byId(this.button.id);
var _21=dojo.coords(_20,true);
_21.w=(dojo.style(_20,"display")=="block")?dojo.style(_20,"width"):_21.w;
var p=_20.parentNode.parentNode;
if(p&&dojo.style(p,"position")=="relative"){
_21.x=dojo.style(p,"left");
_21.y=dojo.style(p,"top");
}
if(p&&dojo.style(p,"position")=="absolute"){
_21.x=0;
_21.y=0;
}
var s=3;
_21.x-=s;
_21.y-=s;
_21.w+=s*2;
_21.h+=s*2;
return _21;
},_buildFileInput:function(_24){
if(this._fileInput){
this._disconnectInput();
dojo.style(this._fileInput,"display","none");
}
this._fileInput=document.createElement("input");
this._fileInput.setAttribute("type","file");
this.fileInputs.push(this._fileInput);
var nm=this.fieldName;
var _id=this.id;
if(this.selectMultipleFiles){
nm+=this.fileCount;
_id+=this.fileCount;
this.fileCount++;
}
this._fileInput.setAttribute("id",this.id);
this._fileInput.setAttribute("name",nm);
dojo.addClass(this._fileInput,"dijitFileInputReal");
this._formNode.appendChild(this._fileInput);
},_removeFileInput:function(){
dojo.forEach(this.fileInputs,function(inp){
inp.parentNode.removeChild(inp);
});
this.fileInputs=[];
this.fileCount=0;
},_buildForm:function(_28){
if(this._formNode){
return;
}
if(dojo.isIE){
this._formNode=document.createElement("<form enctype=\"multipart/form-data\" method=\"post\">");
this._formNode.encoding="multipart/form-data";
}else{
this._formNode=document.createElement("form");
this._formNode.setAttribute("enctype","multipart/form-data");
}
this._formNode.id=dijit.getUniqueId("form");
if(_28&&dojo.style(_28,"display").indexOf("inline")>-1){
document.body.appendChild(this._formNode);
}else{
_28.appendChild(this._formNode);
}
this._setFormStyle();
},_connectInput:function(){
this._disconnectInput();
this._cons.push(dojo.connect(this._fileInput,"mouseover",this,function(evt){
this.onMouseOver(evt);
}));
this._cons.push(dojo.connect(this._fileInput,"mouseout",this,function(evt){
this.onMouseOut(evt);
}));
this._cons.push(dojo.connect(this._fileInput,"change",this,function(){
this.onChange([{name:this._fileInput.value,type:"",size:0}]);
}));
this._cons.push(dojo.connect(window,"resize",this,"setPosition"));
},_disconnectInput:function(){
dojo.forEach(this._cons,function(c){
dojo.disconnect(c);
});
},_setFormStyle:function(){
var _2c=this._getFakeButtonSize();
var _2d=Math.max(2,Math.max(Math.ceil(_2c.w/60),Math.ceil(_2c.h/15)));
dojox.html.insertCssRule("#"+this._formNode.id+" input","font-size:"+_2d+"em");
},onMouseOver:function(evt){
if(this.button.domNode){
dojo.addClass(this.button.domNode,"dijitButtonHover dijitHover");
}
},onMouseOut:function(evt){
if(this.button.domNode){
dojo.removeClass(this.button.domNode,"dijitButtonHover dijitHover");
}
},destroyAll:function(){
this.button.destroy();
this.destroy();
},destroy:function(){
this._disconnectInput();
dojo._destroyElement(this._formNode);
}});
}
