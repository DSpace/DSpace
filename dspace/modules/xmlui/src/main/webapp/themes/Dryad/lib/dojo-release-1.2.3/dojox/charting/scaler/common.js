/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.charting.scaler.common"]){
dojo._hasResource["dojox.charting.scaler.common"]=true;
dojo.provide("dojox.charting.scaler.common");
(function(){
var eq=function(a,b){
return Math.abs(a-b)<=0.000001*(Math.abs(a)+Math.abs(b));
};
dojo.mixin(dojox.charting.scaler.common,{findString:function(_4,_5){
_4=_4.toLowerCase();
for(var i=0;i<_5.length;++i){
if(_4==_5[i]){
return true;
}
}
return false;
},getNumericLabel:function(_7,_8,_9){
if(_9.labels){
var l=_9.labels,lo=0,hi=l.length;
while(lo<hi){
var _d=Math.floor((lo+hi)/2),_e=l[_d].value;
if(_e<_7){
lo=_d+1;
}else{
hi=_d;
}
}
if(lo<l.length&&eq(l[lo].value,_7)){
return l[lo].text;
}
--lo;
if(lo>=0&&lo<l.length&&eq(l[lo].value,_7)){
return l[lo].text;
}
lo+=2;
if(lo<l.length&&eq(l[lo].value,_7)){
return l[lo].text;
}
}
return _9.fixed?_7.toFixed(_8<0?-_8:0):_7.toString();
}});
})();
}
