/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.layout.GridContainer"]){
dojo._hasResource["dojox.layout.GridContainer"]=true;
dojo.provide("dojox.layout.GridContainer");
dojo.experimental("dojox.layout.GridContainer");
dojo.require("dijit._base.focus");
dojo.require("dijit._Templated");
dojo.require("dijit._Container");
dojo.require("dojo.dnd.move");
dojo.require("dojox.layout.dnd.PlottedDnd");
dojo.requireLocalization("dojox.layout","GridContainer",null,"en,ROOT,fr");
dojo.declare("dojox.layout.GridContainer",[dijit._Widget,dijit._Templated,dijit._Container,dijit._Contained],{templateString:"<div id=\"${id}\" class=\"gridContainer\" dojoAttachPoint=\"containerNode\" tabIndex=\"0\" dojoAttachEvent=\"onkeypress:_selectFocus\">\n\t<table class=\"gridContainerTable\" dojoAttachPoint=\"gridContainerTable\" cellspacing=\"0\" cellpadding=\"0\">\n\t\t<tbody>\n\t\t\t<tr dojoAttachPoint=\"gridNode\"></tr>\n\t\t</tbody>\n\t</table>\n</div>\n",isContainer:true,i18n:null,isAutoOrganized:true,isRightFixed:false,isLeftFixed:false,hasResizableColumns:true,nbZones:1,opacity:1,minColWidth:20,minChildWidth:150,acceptTypes:[],mode:"right",allowAutoScroll:false,timeDisplayPopup:1500,isOffset:false,offsetDrag:{},withHandles:false,handleClasses:[],_draggedWidget:null,_isResized:false,_activeGrip:null,_oldwidth:0,_oldheight:0,_a11yOn:false,_canDisplayPopup:true,constructor:function(_1,_2){
this.acceptTypes=_1["acceptTypes"]||["dijit.layout.ContentPane"];
this.dragOffset=_1["dragOffset"]||{x:0,y:0};
},postMixInProperties:function(){
this.i18n=dojo.i18n.getLocalization("dojox.layout","GridContainer");
},_createCells:function(){
if(this.nbZones===0){
this.nbZones=1;
}
var _3=100/this.nbZones;
if(dojo.isIE&&dojo.marginBox(this.gridNode).height){
var _4=document.createTextNode(" ");
this.gridNode.appendChild(_4);
}
var _5=[];
this.cell=[];
var i=0;
while(i<this.nbZones){
var _7=dojo.doc.createElement("td");
dojo.addClass(_7,"gridContainerZone");
_7.id=this.id+"_dz"+i;
_7.style.width=_3+"%";
var _8=this.gridNode.appendChild(_7);
this.cell[i]=_8;
i++;
}
},startup:function(){
this._createCells();
if(this.usepref!==true){
this[(this.isAutoOrganized?"_organizeServices":"_organizeServicesManually")]();
}else{
return;
}
this.init();
},init:function(){
this.grid=this._createGrid();
this.connect(dojo.global,"onresize","onResized");
this.connect(this,"onDndDrop","_placeGrips");
this.dropHandler=dojo.subscribe("/dnd/drop",this,"_placeGrips");
this._oldwidth=this.domNode.offsetWidth;
if(this.hasResizableColumns){
this._initPlaceGrips();
this._placeGrips();
}
},destroy:function(){
for(var i=0;i<this.handleDndStart;i++){
dojo.disconnect(this.handleDndStart[i]);
}
dojo.unsubscribe(this.dropHandler);
this.inherited(arguments);
},onResized:function(){
if(this.hasResizableColumns){
this._placeGrips();
this._oldwidth=this.domNode.offsetWidth;
this._oldheight=this.domNode.offsetHeight;
}
},_organizeServices:function(){
var _a=this.nbZones;
var _b=this.getChildren().length;
var _c=Math.floor(_b/_a);
var _d=_b%_a;
var i=0;
for(var z=0;z<_a;z++){
for(var r=0;r<_c;r++){
this._insertService(z,i++,0,true);
}
if(_d>0){
try{
this._insertService(z,i++,0,true);
}
catch(e){
console.error("Unable to insert service in grid container",e,this.getChildren());
}
_d--;
}else{
if(_c===0){
break;
}
}
}
},_organizeServicesManually:function(){
var _11=this.getChildren();
for(var i=0;i<_11.length;i++){
try{
this._insertService(_11[i].column-1,i,0,true);
}
catch(e){
console.error("Unable to insert service in grid container",e,_11[i]);
}
}
},_insertService:function(z,p,i,_16){
var _17=this.cell[z];
var _18=_17.childNodes.length;
var _19=this.getChildren()[(i?i:0)];
if(typeof (p)=="undefined"||p>_18){
p=_18;
}
var _1a=dojo.place(_19.domNode,_17,p);
_19.domNode.setAttribute("tabIndex",0);
if(!_19.dragRestriction){
dojo.addClass(_19.domNode,"dojoDndItem");
}
if(!_19.domNode.getAttribute("dndType")){
_19.domNode.setAttribute("dndType",_19.declaredClass);
}
dojox.layout.dnd._setGcDndHandle(_19,this.withHandles,this.handleClasses,_16);
if(this.hasResizableColumns){
if(_19.onLoad){
this.connect(_19,"onLoad","_placeGrips");
}
if(_19.onExecError){
this.connect(_19,"onExecError","_placeGrips");
}
if(_19.onUnLoad){
this.connect(_19,"onUnLoad","_placeGrips");
}
}
this._placeGrips();
return _19.id;
},addService:function(_1b,z,p){
_1b.domNode.id=_1b.id;
this.addChild(_1b);
if(p<=0){
p=0;
}
var _1e=this._insertService(z,p);
this.grid[z].setItem(_1b.id,{data:_1b.domNode,type:[_1b.domNode.getAttribute("dndType")]});
return _1e;
},_createGrid:function(){
var _1f=[];
var i=0;
this.tabDZ=[];
while(i<this.nbZones){
var _21=this.cell[i];
this.tabDZ[i]=this._createZone(_21);
if(this.hasResizableColumns&&i!=(this.nbZones-1)){
this._createGrip(this.tabDZ[i]);
}
_1f.push(this.tabDZ[i]);
i++;
}
if(this.hasResizableColumns){
this.handleDndStart=[];
for(var j=0;j<this.tabDZ.length;j++){
var dz=this.tabDZ[j];
var _24=this;
this.handleDndStart.push(dojo.connect(dz,"onDndStart",dz,function(_25){
if(_25==this){
_24.handleDndInsertNodes=[];
for(i=0;i<_24.tabDZ.length;i++){
_24.handleDndInsertNodes.push(dojo.connect(_24.tabDZ[i],"insertNodes",_24,function(){
_24._disconnectDnd();
}));
}
_24.handleDndInsertNodes.push(dojo.connect(dz,"onDndCancel",_24,_24._disconnectDnd));
_24.onResized();
}
}));
}
}
return _1f;
},_disconnectDnd:function(){
dojo.forEach(this.handleDndInsertNodes,dojo.disconnect);
setTimeout(dojo.hitch(this,"onResized"),0);
},_createZone:function(_26){
var dz=null;
dz=new dojox.layout.dnd.PlottedDnd(_26.id,{accept:this.acceptTypes,withHandles:this.withHandles,handleClasses:this.handleClasses,singular:true,hideSource:true,opacity:this.opacity,dom:this.domNode,allowAutoScroll:this.allowAutoScroll,isOffset:this.isOffset,offsetDrag:this.offsetDrag});
this.connect(dz,"insertDashedZone","_placeGrips");
this.connect(dz,"deleteDashedZone","_placeGrips");
return dz;
},_createGrip:function(dz){
var _29=document.createElement("div");
_29.className="gridContainerGrip";
_29.setAttribute("tabIndex","0");
var _2a=this;
this.onMouseOver=this.connect(_29,"onmouseover",function(e){
var _2c=false;
for(var i=0;i<_2a.grid.length-1;i++){
if(dojo.hasClass(_2a.grid[i].grip,"gridContainerGripShow")){
_2c=true;
break;
}
}
if(!_2c){
dojo.removeClass(e.target,"gridContainerGrip");
dojo.addClass(e.target,"gridContainerGripShow");
}
});
this.connect(_29,"onmouseout",function(e){
if(!_2a._isResized){
dojo.removeClass(e.target,"gridContainerGripShow");
dojo.addClass(e.target,"gridContainerGrip");
}
});
this.connect(_29,"onmousedown",function(e){
_2a._a11yOn=false;
_2a._activeGrip=e.target;
_2a.resizeColumnOn(e);
});
this.domNode.appendChild(_29);
dz.grip=_29;
},_initPlaceGrips:function(){
var dcs=dojo.getComputedStyle(this.domNode);
var gcs=dojo.getComputedStyle(this.gridContainerTable);
this._x=parseInt(dcs.paddingLeft);
this._topGrip=parseInt(dcs.paddingTop);
if(dojo.isIE||gcs.borderCollapse!="collapse"){
var ex=dojo._getBorderExtents(this.gridContainerTable);
this._x+=ex.l;
this._topGrip+=ex.t;
}
this._topGrip+="px";
dojo.forEach(this.grid,function(_33){
if(_33.grip){
var _34=_33.grip;
if(!dojo.isIE){
_33.pad=dojo._getPadBorderExtents(_33.node).w;
}
_34.style.top=this._topGrip;
}
},this);
},_placeGrips:function(){

var _35;
if(this.allowAutoScroll){
_35=this.gridNode.scrollHeight;
}else{
_35=dojo.contentBox(this.gridNode).h;
}
var _36=this._x;
dojo.forEach(this.grid,function(_37){
if(_37.grip){
var _38=_37.grip;
_36+=dojo[(dojo.isIE?"marginBox":"contentBox")](_37.node).w+(dojo.isIE?0:_37.pad);
dojo.style(_38,{left:_36+"px",height:_35+"px"});
}
},this);
},_getZoneByIndex:function(n){
return this.grid[(n>=0&&n<this.grid.length?n:0)];
},getIndexZone:function(_3a){
for(var z=0;z<this.grid.length;z++){
if(this.grid[z].domNode==_3a){
return z;
}
}
return -1;
},resizeColumnOn:function(e){
var k=dojo.keys;
if(this._a11yOn&&e.keyCode!=k.LEFT_ARROW&&e.keyCode!=k.RIGHT_ARROW){
return;
}
e.preventDefault();
dojo.body().style.cursor="ew-resize";
this._isResized=true;
this.initX=e.pageX;
var _3e=[];
for(var i=0;i<this.grid.length;i++){
_3e[i]=dojo.contentBox(this.grid[i].node).w;
}
this.oldTabSize=_3e;
for(var i=0;i<this.grid.length;i++){
if(this._activeGrip==this.grid[i].grip){
this.currentColumn=this.grid[i].node;
this.currentColumnWidth=_3e[i];
this.nextColumn=this.currentColumn.nextSibling;
this.nextColumnWidth=_3e[i+1];
}
this.grid[i].node.style.width=_3e[i]+"px";
}
var _40=function(_41,_42){
var _43=0;
var _44=0;
dojo.forEach(_41,function(_45){
if(_45.nodeType==1){
var _46=dojo.getComputedStyle(_45);
var _47=(dojo.isIE?_42:parseInt(_46.minWidth));
_44=_47+parseInt(_46.marginLeft)+parseInt(_46.marginRight);
if(_43<_44){
_43=_44;
}
}
});
return _43;
};
var _48=_40(this.currentColumn.childNodes,this.minChildWidth);
var _49=_40(this.nextColumn.childNodes,this.minChildWidth);
var _4a=Math.round((dojo.marginBox(this.gridContainerTable).w*this.minColWidth)/100);
this.currentMinCol=_48;
this.nextMinCol=_49;
if(_4a>this.currentMinCol){
this.currentMinCol=_4a;
}
if(_4a>this.nextMinCol){
this.nextMinCol=_4a;
}
if(this._a11yOn){
this.connectResizeColumnMove=this.connect(dojo.doc,"onkeypress","resizeColumnMove");
}else{
this.connectResizeColumnMove=this.connect(dojo.doc,"onmousemove","resizeColumnMove");
this.connectResizeColumnOff=this.connect(document,"onmouseup","resizeColumnOff");
}
},resizeColumnMove:function(e){
var d=0;
if(this._a11yOn){
var k=dojo.keys;
switch(e.keyCode){
case k.LEFT_ARROW:
d=-10;
break;
case k.RIGHT_ARROW:
d=10;
break;
}
}else{
e.preventDefault();
d=e.pageX-this.initX;
}
if(d==0){
return;
}
if(!(this.currentColumnWidth+d<this.currentMinCol||this.nextColumnWidth-d<this.nextMinCol)){
this.currentColumnWidth+=d;
this.nextColumnWidth-=d;
this.initX=e.pageX;
this.currentColumn.style["width"]=this.currentColumnWidth+"px";
this.nextColumn.style["width"]=this.nextColumnWidth+"px";
this._activeGrip.style.left=parseInt(this._activeGrip.style.left)+d+"px";
this._placeGrips();
}
if(this._a11yOn){
this.resizeColumnOff(e);
}
},resizeColumnOff:function(e){
dojo.body().style.cursor="default";
if(this._a11yOn){
this.disconnect(this.connectResizeColumnMove);
this._a11yOn=false;
}else{
this.disconnect(this.connectResizeColumnMove);
this.disconnect(this.connectResizeColumnOff);
}
var _4f=[];
var _50=[];
var _51=this.gridContainerTable.clientWidth;
for(var i=0;i<this.grid.length;i++){
var _cb=dojo.contentBox(this.grid[i].node);
if(dojo.isIE){
_4f[i]=dojo.marginBox(this.grid[i].node).w;
_50[i]=_cb.w;
}else{
_4f[i]=_cb.w;
_50=_4f;
}
}
var _54=false;
for(var i=0;i<_50.length;i++){
if(_50[i]!=this.oldTabSize[i]){
_54=true;
break;
}
}
if(_54){
var mul=dojo.isIE?100:10000;
for(var i=0;i<this.grid.length;i++){
this.grid[i].node.style.width=Math.round((100*mul*_4f[i])/_51)/mul+"%";
}
this._placeGrips();
}
if(this._activeGrip){
dojo.removeClass(this._activeGrip,"gridContainerGripShow");
dojo.addClass(this._activeGrip,"gridContainerGrip");
}
this._isResized=false;
},setColumns:function(_56){
if(_56>0){
var _57=this.grid.length-_56;
if(_57>0){
var _58=[];
var _59,_5a,end;
if(this.mode=="right"){
end=(this.isLeftFixed&&this.grid.length>0)?1:0;
_5a=this.grid.length-(this.isRightFixed?2:1);
for(var z=_5a;z>=end;z--){
var _5d=0;
var _59=this.grid[z].node;
for(var j=0;j<_59.childNodes.length;j++){
if(_59.childNodes[j].nodeType==1&&!(_59.childNodes[j].id=="")){
_5d++;
break;
}
}
if(_5d==0){
_58[_58.length]=z;
}
if(_58.length>=_57){
this._deleteColumn(_58);
break;
}
}
if(_58.length<_57){
console.error(this.i18n.err_onSetNbColsRightMode);
}
}else{
if(this.isLeftFixed&&this.grid.length>0){
_5a=1;
}else{
_5a=0;
}
if(this.isRightFixed){
end=this.grid.length-1;
}else{
end=this.grid.length;
}
for(var z=_5a;z<end;z++){
var _5d=0;
var _59=this.grid[z].node;
for(var j=0;j<_59.childNodes.length;j++){
if(_59.childNodes[j].nodeType==1&&!(_59.childNodes[j].id=="")){
_5d++;
break;
}
}
if(_5d==0){
_58[_58.length]=z;
}
if(_58.length>=_57){
this._deleteColumn(_58);
break;
}
}
if(_58.length<_57){
alert(this.i18n.err_onSetNbColsLeftMode);
}
}
}else{
if(_57<0){
this._addColumn(Math.abs(_57));
}
}
this._initPlaceGrips();
this._placeGrips();
}
},_addColumn:function(_5f){
var _60;
if(this.hasResizableColumns&&!this.isRightFixed&&this.mode=="right"){
_60=this.grid[this.grid.length-1];
this._createGrip(_60);
}
for(i=0;i<_5f;i++){
_60=dojo.doc.createElement("td");
dojo.addClass(_60,"gridContainerZone");
_60.id=this.id+"_dz"+this.nbZones;
var dz;
if(this.mode=="right"){
if(this.isRightFixed){
this.grid[this.grid.length-1].node.parentNode.insertBefore(_60,this.grid[this.grid.length-1].node);
dz=this._createZone(_60);
this.tabDZ.splice(this.tabDZ.length-1,0,dz);
this.grid.splice(this.grid.length-1,0,dz);
this.cell.splice(this.cell.length-1,0,_60);
}else{
var _62=this.gridNode.appendChild(_60);
dz=this._createZone(_60);
this.tabDZ.push(dz);
this.grid.push(dz);
this.cell.push(_60);
}
}else{
if(this.isLeftFixed){
(this.grid.length==1)?this.grid[0].node.parentNode.appendChild(_60,this.grid[0].node):this.grid[1].node.parentNode.insertBefore(_60,this.grid[1].node);
dz=this._createZone(_60);
this.tabDZ.splice(1,0,dz);
this.grid.splice(1,0,dz);
this.cell.splice(1,0,_60);
}else{
this.grid[this.grid.length-this.nbZones].node.parentNode.insertBefore(_60,this.grid[this.grid.length-this.nbZones].node);
dz=this._createZone(_60);
this.tabDZ.splice(this.tabDZ.length-this.nbZones,0,dz);
this.grid.splice(this.grid.length-this.nbZones,0,dz);
this.cell.splice(this.cell.length-this.nbZones,0,_60);
}
}
if(this.hasResizableColumns){
var _63=this;
var _64=dojo.connect(dz,"onDndStart",dz,function(_65){
if(_65==this){
_63.handleDndInsertNodes=[];
for(var o=0;o<_63.tabDZ.length;o++){
_63.handleDndInsertNodes.push(dojo.connect(_63.tabDZ[o],"insertNodes",_63,function(){
_63._disconnectDnd();
}));
}
_63.handleDndInsertNodes.push(dojo.connect(dz,"onDndCancel",_63,_63._disconnectDnd));
_63.onResized();
}
});
if(this.mode=="right"){
if(this.isRightFixed){
this.handleDndStart.splice(this.handleDndStart.length-1,0,_64);
}else{
this.handleDndStart.push(_64);
}
}else{
if(this.isLeftFixed){
this.handleDndStart.splice(1,0,_64);
}else{
this.handleDndStart.splice(this.handleDndStart.length-this.nbZones,0,_64);
}
}
this._createGrip(dz);
}
this.nbZones++;
}
this._updateColumnsWidth();
},_deleteColumn:function(_67){
var _68,_69,_6a;
_6a=0;
for(var i=0;i<_67.length;i++){
var idx=_67[i];
if(this.mode=="right"){
_68=this.grid[idx];
}else{
_68=this.grid[idx-_6a];
}
for(var j=0;j<_68.node.childNodes.length;j++){
if(_68.node.childNodes[j].nodeType!=1){
continue;
}
_69=dijit.byId(_68.node.childNodes[j].id);
for(var x=0;x<this.getChildren().length;x++){
if(this.getChildren()[x]===_69){
this.getChildren().splice(x,1);
break;
}
}
}
_68.node.parentNode.removeChild(_68.node);
if(this.mode=="right"){
if(this.hasResizableColumns){
dojo.disconnect(this.handleDndStart[idx]);
}
this.grid.splice(idx,1);
this.tabDZ.splice(idx,1);
this.cell.splice(idx,1);
}else{
if(this.hasResizableColumns){
dojo.disconnect(this.handleDndStart[idx-_6a]);
}
this.grid.splice(idx-_6a,1);
this.tabDZ.splice(idx-_6a,1);
this.cell.splice(idx-_6a,1);
}
this.nbZones--;
_6a++;
if(_68.grip){
this.domNode.removeChild(_68.grip);
}
}
this._updateColumnsWidth();
},_updateColumnsWidth:function(){
var _6f=100/this.nbZones;
var _70;
for(var z=0;z<this.grid.length;z++){
_70=this.grid[z].node;
_70.style.width=_6f+"%";
}
},_selectFocus:function(_72){
var e=_72.keyCode;
var _74=null;
var _75=dijit.getFocus();
var _76=_75.node;
var k=dojo.keys;
var _78=(e==k.UP_ARROW||e==k.LEFT_ARROW)?"lastChild":"firstChild";
var pos=(e==k.UP_ARROW||e==k.LEFT_ARROW)?"previousSibling":"nextSibling";
if(_76==this.containerNode){
switch(e){
case k.DOWN_ARROW:
case k.RIGHT_ARROW:
for(var i=0;i<this.gridNode.childNodes.length;i++){
_74=this.gridNode.childNodes[i].firstChild;
var _7b=false;
while(!_7b){
if(_74!=null){
if(_74.style.display!=="none"){
dijit.focus(_74);
dojo.stopEvent(_72);
_7b=true;
}else{
_74=_74[pos];
}
}else{
break;
}
}
if(_7b){
break;
}
}
break;
case k.UP_ARROW:
case k.LEFT_ARROW:
for(var i=this.gridNode.childNodes.length-1;i>=0;i--){
_74=this.gridNode.childNodes[i].lastChild;
var _7b=false;
while(!_7b){
if(_74!=null){
if(_74.style.display!=="none"){
dijit.focus(_74);
dojo.stopEvent(_72);
_7b=true;
}else{
_74=_74[pos];
}
}else{
break;
}
}
if(_7b){
break;
}
}
break;
}
}else{
if(_76.parentNode.parentNode==this.gridNode){
switch(e){
case k.UP_ARROW:
case k.DOWN_ARROW:
dojo.stopEvent(_72);
var _7c=0;
dojo.forEach(_76.parentNode.childNodes,function(_7d){
if(_7d.style.display!=="none"){
_7c++;
}
});
if(_7c==1){
return;
}
var _7b=false;
_74=_76[pos];
while(!_7b){
if(_74==null){
_74=_76.parentNode[_78];
if(_74.style.display!=="none"){
_7b=true;
}else{
_74=_74[pos];
}
}else{
if(_74.style.display!=="none"){
_7b=true;
}else{
_74=_74[pos];
}
}
}
if(_72.shiftKey){
if(dijit.byNode(_76).dragRestriction){
return;
}
var _7e=_76.getAttribute("dndtype");
var _7f=false;
for(var i=0;i<this.acceptTypes.length;i++){
if(_7e==this.acceptTypes[i]){
var _7f=true;
break;
}
}
if(_7f){
var _80=_76.parentNode;
var _81=_80.firstChild;
var _82=_80.lastChild;
while(_81.style.display=="none"||_82.style.display=="none"){
if(_81.style.display=="none"){
_81=_81.nextSibling;
}
if(_82.style.display=="none"){
_82=_82.previousSibling;
}
}
if(e==k.UP_ARROW){
var r=_80.removeChild(_76);
if(r==_81){
_80.appendChild(r);
}else{
_80.insertBefore(r,_74);
}
r.setAttribute("tabIndex","0");
dijit.focus(r);
}else{
if(_76==_82){
var r=_80.removeChild(_76);
_80.insertBefore(r,_74);
r.setAttribute("tabIndex","0");
dijit.focus(r);
}else{
var r=_80.removeChild(_74);
_80.insertBefore(r,_76);
_76.setAttribute("tabIndex","0");
dijit.focus(_76);
}
}
}else{
this._displayPopup();
}
}else{
dijit.focus(_74);
}
break;
case k.RIGHT_ARROW:
case k.LEFT_ARROW:
dojo.stopEvent(_72);
if(_72.shiftKey){
if(dijit.byNode(_76).dragRestriction){
return;
}
var z=0;
if(_76.parentNode[pos]==null){
if(e==k.LEFT_ARROW){
var z=this.gridNode.childNodes.length-1;
}
}else{
if(_76.parentNode[pos].nodeType==3){
z=this.gridNode.childNodes.length-2;
}else{
for(var i=0;i<this.gridNode.childNodes.length;i++){
if(_76.parentNode[pos]==this.gridNode.childNodes[i]){
break;
}
z++;
}
}
}
var _7e=_76.getAttribute("dndtype");
var _7f=false;
for(var i=0;i<this.acceptTypes.length;i++){
if(_7e==this.acceptTypes[i]){
_7f=true;
break;
}
}
if(_7f){
var _85=_76.parentNode;
var _86=dijit.byNode(_76);
var r=_85.removeChild(_76);
var _87=(e==k.RIGHT_ARROW?0:this.gridNode.childNodes[z].length);
this.addService(_86,z,_87);
r.setAttribute("tabIndex","0");
dijit.focus(r);
this._placeGrips();
}else{
this._displayPopup();
}
}else{
var _88=_76.parentNode;
while(_74===null){
if(_88[pos]!==null&&_88[pos].nodeType!==3){
_88=_88[pos];
}else{
if(pos==="previousSibling"){
_88=_88.parentNode.childNodes[_88.parentNode.childNodes.length-1];
}else{
_88=_88.parentNode.childNodes[0];
}
}
var _7b=false;
var _89=_88[_78];
while(!_7b){
if(_89!=null){
if(_89.style.display!=="none"){
_74=_89;
_7b=true;
}else{
_89=_89[pos];
}
}else{
break;
}
}
}
dijit.focus(_74);
}
break;
}
}else{
if(dojo.hasClass(_76,"gridContainerGrip")||dojo.hasClass(_76,"gridContainerGripShow")){
this._activeGrip=_72.target;
this._a11yOn=true;
this.resizeColumnOn(_72);
}
}
}
},_displayPopup:function(){
if(this._canDisplayPopup){
var _8a=dojo.doc.createElement("div");
dojo.addClass(_8a,"gridContainerPopup");
_8a.innerHTML=this.i18n.alertPopup;
var _8b=this.containerNode.appendChild(_8a);
this._canDisplayPopup=false;
setTimeout(dojo.hitch(this,function(){
this.containerNode.removeChild(_8b);
dojo._destroyElement(_8b);
this._canDisplayPopup=true;
}),this.timeDisplayPopup);
}
}});
dojo.extend(dijit._Widget,{dragRestriction:false,column:"1",group:""});
}
