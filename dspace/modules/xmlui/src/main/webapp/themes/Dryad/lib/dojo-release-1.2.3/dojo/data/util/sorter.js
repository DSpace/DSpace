/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojo.data.util.sorter"]){
dojo._hasResource["dojo.data.util.sorter"]=true;
dojo.provide("dojo.data.util.sorter");
dojo.data.util.sorter.basicComparator=function(a,b){
var _3=0;
if(a>b||typeof a==="undefined"||a===null){
_3=1;
}else{
if(a<b||typeof b==="undefined"||b===null){
_3=-1;
}
}
return _3;
};
dojo.data.util.sorter.createSortFunction=function(_4,_5){
var _6=[];
function createSortFunction(_7,_8){
return function(_9,_a){
var a=_5.getValue(_9,_7);
var b=_5.getValue(_a,_7);
var _d=null;
if(_5.comparatorMap){
if(typeof _7!=="string"){
_7=_5.getIdentity(_7);
}
_d=_5.comparatorMap[_7]||dojo.data.util.sorter.basicComparator;
}
_d=_d||dojo.data.util.sorter.basicComparator;
return _8*_d(a,b);
};
};
var _e;
for(var i=0;i<_4.length;i++){
_e=_4[i];
if(_e.attribute){
var _10=(_e.descending)?-1:1;
_6.push(createSortFunction(_e.attribute,_10));
}
}
return function(_11,_12){
var i=0;
while(i<_6.length){
var ret=_6[i++](_11,_12);
if(ret!==0){
return ret;
}
}
return 0;
};
};
}
