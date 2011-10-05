/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.lang.functional.array"]){
dojo._hasResource["dojox.lang.functional.array"]=true;
dojo.provide("dojox.lang.functional.array");
dojo.require("dojox.lang.functional.lambda");
(function(){
var d=dojo,df=dojox.lang.functional,_3={};
d.mixin(df,{filter:function(a,f,o){
if(typeof a=="string"){
a=a.split("");
}
o=o||d.global;
f=df.lambda(f);
var t=[],v;
if(d.isArray(a)){
for(var i=0,n=a.length;i<n;++i){
v=a[i];
if(f.call(o,v,i,a)){
t.push(v);
}
}
}else{
for(var i=0;a.hasNext();){
v=a.next();
if(f.call(o,v,i++,a)){
t.push(v);
}
}
}
return t;
},forEach:function(a,f,o){
if(typeof a=="string"){
a=a.split("");
}
o=o||d.global;
f=df.lambda(f);
if(d.isArray(a)){
for(var i=0,n=a.length;i<n;f.call(o,a[i],i,a),++i){
}
}else{
if(typeof a.hasNext=="function"&&typeof a.next=="function"){
for(var i=0;a.hasNext();f.call(o,a.next(),i++,a)){
}
}else{
for(var i in a){
if(i in _3){
continue;
}
f.call(o,a[i],i,a);
}
}
}
return o;
},map:function(a,f,o){
if(typeof a=="string"){
a=a.split("");
}
o=o||d.global;
f=df.lambda(f);
var t,n;
if(d.isArray(a)){
t=new Array(n=a.length);
for(var i=0;i<n;t[i]=f.call(o,a[i],i,a),++i){
}
}else{
t=[];
for(var i=0;a.hasNext();t.push(f.call(o,a.next(),i++,a))){
}
}
return t;
},every:function(a,f,o){
if(typeof a=="string"){
a=a.split("");
}
o=o||d.global;
f=df.lambda(f);
if(d.isArray(a)){
for(var i=0,n=a.length;i<n;++i){
if(!f.call(o,a[i],i,a)){
return false;
}
}
}else{
if(typeof a.hasNext=="function"&&typeof a.next=="function"){
for(var i=0;a.hasNext();){
if(!f.call(o,a.next(),i++,a)){
return false;
}
}
}else{
for(var i in a){
if(i in _3){
continue;
}
if(!f.call(o,a[i],i,a)){
return false;
}
}
}
}
return true;
},some:function(a,f,o){
if(typeof a=="string"){
a=a.split("");
}
o=o||d.global;
f=df.lambda(f);
if(d.isArray(a)){
for(var i=0,n=a.length;i<n;++i){
if(f.call(o,a[i],i,a)){
return true;
}
}
}else{
if(typeof a.hasNext=="function"&&typeof a.next=="function"){
for(var i=0;a.hasNext();){
if(f.call(o,a.next(),i++,a)){
return true;
}
}
}else{
for(var i in a){
if(i in _3){
continue;
}
if(f.call(o,a[i],i,a)){
return true;
}
}
}
}
return false;
}});
})();
}
