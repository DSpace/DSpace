/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.av.FLVideo"]){
dojo._hasResource["dojox.av.FLVideo"]=true;
dojo.provide("dojox.av.FLVideo");
dojo.experimental("dojox.av.FLVideo");
dojo.require("dijit._Widget");
dojo.require("dojox.embed.Flash");
dojo.require("dojox.av._Media");
dojo.declare("dojox.av.FLVideo",[dijit._Widget,dojox.av._Media],{_swfPath:dojo.moduleUrl("dojox.av","resources/video.swf"),postCreate:function(){
this._subs=[];
this._cons=[];
this.mediaUrl=this._normalizeUrl(this.mediaUrl);
this.initialVolume=this._normalizeVolume(this.initialVolume);
var _1={path:this._swfPath.uri,width:"100%",height:"100%",params:{allowFullScreen:true,wmode:"transparent"},vars:{videoUrl:this.mediaUrl,id:this.id,autoPlay:this.autoPlay,volume:this.initialVolume,isDebug:this.isDebug}};
this._sub("stageClick","onClick");
this._sub("stageSized","onSwfSized");
this._sub("mediaStatus","onPlayerStatus");
this._sub("mediaMeta","onMetaData");
this._sub("mediaError","onError");
this._sub("mediaStart","onStart");
this._sub("mediaEnd","onEnd");
this._flashObject=new dojox.embed.Flash(_1,this.domNode);
this._flashObject.onLoad=dojo.hitch(this,function(_2){
this.flashMedia=_2;
this.isPlaying=this.autoPlay;
this.isStopped=!this.autoPlay;
this.onLoad(this.flashMedia);
this._initStatus();
this._update();
});
},play:function(_3){
this.isPlaying=true;
this.isStopped=false;
this.flashMedia.doPlay(this._normalizeUrl(_3));
},pause:function(){
this.isPlaying=false;
this.isStopped=false;
this.flashMedia.pause();
},seek:function(_4){
this.flashMedia.seek(_4);
},volume:function(_5){
if(_5){
if(!this.flashMedia){
this.initialVolume=_5;
}
this.flashMedia.setVolume(this._normalizeVolume(_5));
}
if(!this.flashMedia){
return this.initialVolume;
}
return this.flashMedia.getVolume();
},_checkBuffer:function(_6,_7){
if(this.percentDownloaded==100){
if(this.isBuffering){
this.onBuffer(false);
this.flashMedia.doPlay();
}
return;
}
if(!this.isBuffering&&_7<0.1){
this.onBuffer(true);
this.flashMedia.pause();
return;
}
var _8=this.percentDownloaded*0.01*this.duration;
if(!this.isBuffering&&_6+this.minBufferTime*0.001>_8){
this.onBuffer(true);
this.flashMedia.pause();
}else{
if(this.isBuffering&&_6+this.bufferTime*0.001<=_8){
this.onBuffer(false);
this.flashMedia.doPlay();
}
}
},_update:function(){
var _9=Math.min(this.getTime()||0,this.duration);
var _a=this.flashMedia.getLoaded();
this.percentDownloaded=Math.ceil(_a.bytesLoaded/_a.bytesTotal*100);
this.onDownloaded(this.percentDownloaded);
this.onPosition(_9);
if(this.duration){
this._checkBuffer(_9,_a.buffer);
}
setTimeout(dojo.hitch(this,"_update"),this.updateTime);
},_normalizeUrl:function(_b){
if(_b&&_b.toLowerCase().indexOf("http")<0){
var _c=window.location.href.split("/");
_c.pop();
_c=_c.join("/")+"/";
_b=_c+_b;
}
return _b;
},_normalizeVolume:function(_d){
if(_d>1){
while(_d>1){
_d*=0.1;
}
}
return _d;
},_sub:function(_e,_f){
dojo.subscribe(this.id+"/"+_e,this,_f);
},destroy:function(){
if(!this.flashMedia){
this._cons.push(dojo.connect(this,"onLoad",this,"destroy"));
return;
}
dojo.forEach(this._subs,function(s){
dojo.unsubscribe(s);
});
dojo.forEach(this._cons,function(c){
dojo.disconnect(c);
});
this._flashObject.destroy();
}});
}
