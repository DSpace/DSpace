/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.xmpp.PresenceService"]){
dojo._hasResource["dojox.xmpp.PresenceService"]=true;
dojo.provide("dojox.xmpp.PresenceService");
dojox.xmpp.presence={UPDATE:201,SUBSCRIPTION_REQUEST:202,SUBSCRIPTION_SUBSTATUS_NONE:204,SUBSCRIPTION_NONE:"none",SUBSCRIPTION_FROM:"from",SUBSCRIPTION_TO:"to",SUBSCRIPTION_BOTH:"both",SUBSCRIPTION_REQUEST_PENDING:"pending",STATUS_ONLINE:"online",STATUS_AWAY:"away",STATUS_CHAT:"chat",STATUS_DND:"dnd",STATUS_EXTENDED_AWAY:"xa",STATUS_OFFLINE:"offline",STATUS_INVISIBLE:"invisible"};
dojo.declare("dojox.xmpp.PresenceService",null,{constructor:function(_1){
this.session=_1;
this.isInvisible=false;
this.avatarHash=null;
this.presence=null;
this.restrictedContactjids={};
},publish:function(_2){
this.presence=_2;
this._setPresence();
},sendAvatarHash:function(_3){
this.avatarHash=_3;
this._setPresence();
},_setPresence:function(){
var _4=this.presence;
var p={xmlns:"jabber:client"};
if(_4&&_4.to){
p.to=_4.to;
}
if(_4.show&&_4.show==dojox.xmpp.presence.STATUS_OFFLINE){
p.type="unavailable";
}
if(_4.show&&_4.show==dojox.xmpp.presence.STATUS_INVISIBLE){
this._setInvisible();
this.isInvisible=true;
return;
}
if(this.isInvisible){
this._setVisible();
}
var _6=new dojox.string.Builder(dojox.xmpp.util.createElement("presence",p,false));
if(_4.show&&_4.show!=dojox.xmpp.presence.STATUS_OFFLINE){
_6.append(dojox.xmpp.util.createElement("show",{},false));
_6.append(_4.show);
_6.append("</show>");
}
if(_4.status){
_6.append(dojox.xmpp.util.createElement("status",{},false));
_6.append(_4.status);
_6.append("</status>");
}
if(this.avatarHash){
_6.append(dojox.xmpp.util.createElement("x",{xmlns:"vcard-temp:x:update"},false));
_6.append(dojox.xmpp.util.createElement("photo",{},false));
_6.append(this.avatarHash);
_6.append("</photo>");
_6.append("</x>");
}
if(_4.priority&&_4.show!=dojox.xmpp.presence.STATUS_OFFLINE){
if(_4.priority>127||_4.priority<-128){
_4.priority=5;
}
_6.append(dojox.xmpp.util.createElement("priority",{},false));
_6.append(_4.priority);
_6.append("</priority>");
}
_6.append("</presence>");
this.session.dispatchPacket(_6.toString());
},toggleBlockContact:function(_7){
if(!this.restrictedContactjids[_7]){
this.restrictedContactjids[_7]=this._createRestrictedJid();
}
this.restrictedContactjids[_7].blocked=!this.restrictedContactjids[_7].blocked;
this._updateRestricted();
return this.restrictedContactjids;
},toggleContactInvisiblity:function(_8){
if(!this.restrictedContactjids[_8]){
this.restrictedContactjids[_8]=this._createRestrictedJid();
}
this.restrictedContactjids[_8].invisible=!this.restrictedContactjids[_8].invisible;
this._updateRestricted();
return this.restrictedContactjids;
},_createRestrictedJid:function(){
return {invisible:false,blocked:false};
},_updateRestricted:function(){
var _9={id:this.session.getNextIqId(),from:this.session.jid+"/"+this.session.resource,type:"set"};
var _a=new dojox.string.Builder(dojox.xmpp.util.createElement("iq",_9,false));
_a.append(dojox.xmpp.util.createElement("query",{xmlns:"jabber:iq:privacy"},false));
_a.append(dojox.xmpp.util.createElement("list",{name:"iwcRestrictedContacts"},false));
var _b=1;
for(jid in this.restrictedContactjids){
var _c=this.restrictedContactjids[jid];
if(_c.blocked||_c.invisible){
_a.append(dojox.xmpp.util.createElement("item",{value:dojox.xmpp.util.encodeJid(jid),action:"deny",order:_b++},false));
if(_c.blocked){
_a.append(dojox.xmpp.util.createElement("message",{},true));
}
if(_c.invisible){
_a.append(dojox.xmpp.util.createElement("presence-out",{},true));
}
_a.append("</item>");
}else{
delete this.restrictedContactjids[jid];
}
}
_a.append("</list>");
_a.append("</query>");
_a.append("</iq>");
var _d=new dojox.string.Builder(dojox.xmpp.util.createElement("iq",_9,false));
_d.append(dojox.xmpp.util.createElement("query",{xmlns:"jabber:iq:privacy"},false));
_d.append(dojox.xmpp.util.createElement("active",{name:"iwcRestrictedContacts"},true));
_d.append("</query>");
_d.append("</iq>");
this.session.dispatchPacket(_a.toString());
this.session.dispatchPacket(_d.toString());
},_setVisible:function(){
var _e={id:this.session.getNextIqId(),from:this.session.jid+"/"+this.session.resource,type:"set"};
var _f=new dojox.string.Builder(dojox.xmpp.util.createElement("iq",_e,false));
_f.append(dojox.xmpp.util.createElement("query",{xmlns:"jabber:iq:privacy"},false));
_f.append(dojox.xmpp.util.createElement("active",{},true));
_f.append("</query>");
_f.append("</iq>");
this.session.dispatchPacket(_f.toString());
},_setInvisible:function(){
var _10={id:this.session.getNextIqId(),from:this.session.jid+"/"+this.session.resource,type:"set"};
var req=new dojox.string.Builder(dojox.xmpp.util.createElement("iq",_10,false));
req.append(dojox.xmpp.util.createElement("query",{xmlns:"jabber:iq:privacy"},false));
req.append(dojox.xmpp.util.createElement("list",{name:"invisible"},false));
req.append(dojox.xmpp.util.createElement("item",{action:"deny",order:"1"},false));
req.append(dojox.xmpp.util.createElement("presence-out",{},true));
req.append("</item>");
req.append("</list>");
req.append("</query>");
req.append("</iq>");
_10={id:this.session.getNextIqId(),from:this.session.jid+"/"+this.session.resource,type:"set"};
var _12=new dojox.string.Builder(dojox.xmpp.util.createElement("iq",_10,false));
_12.append(dojox.xmpp.util.createElement("query",{xmlns:"jabber:iq:privacy"},false));
_12.append(dojox.xmpp.util.createElement("active",{name:"invisible"},true));
_12.append("</query>");
_12.append("</iq>");
this.session.dispatchPacket(req.toString());
this.session.dispatchPacket(_12.toString());
},_manageSubscriptions:function(_13,_14){
if(!_13){
return;
}
if(_13.indexOf("@")==-1){
_13+="@"+this.session.domain;
}
var req=dojox.xmpp.util.createElement("presence",{to:_13,type:_14},true);
this.session.dispatchPacket(req);
},subscribe:function(_16){
this._manageSubscriptions(_16,"subscribe");
},approveSubscription:function(_17){
this._manageSubscriptions(_17,"subscribed");
},unsubscribe:function(_18){
this._manageSubscriptions(_18,"unsubscribe");
},declineSubscription:function(_19){
this._manageSubscriptions(_19,"unsubscribed");
},cancelSubscription:function(_1a){
this._manageSubscriptions(_1a,"unsubscribed");
}});
}
