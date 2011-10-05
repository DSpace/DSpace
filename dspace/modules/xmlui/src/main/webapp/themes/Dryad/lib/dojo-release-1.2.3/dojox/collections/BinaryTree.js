/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.collections.BinaryTree"]){
dojo._hasResource["dojox.collections.BinaryTree"]=true;
dojo.provide("dojox.collections.BinaryTree");
dojo.require("dojox.collections._base");
dojox.collections.BinaryTree=function(_1){
function node(_2,_3,_4){
this.value=_2||null;
this.right=_3||null;
this.left=_4||null;
this.clone=function(){
var c=new node();
if(this.value.value){
c.value=this.value.clone();
}else{
c.value=this.value;
}
if(this.left!=null){
c.left=this.left.clone();
}
if(this.right!=null){
c.right=this.right.clone();
}
return c;
};
this.compare=function(n){
if(this.value>n.value){
return 1;
}
if(this.value<n.value){
return -1;
}
return 0;
};
this.compareData=function(d){
if(this.value>d){
return 1;
}
if(this.value<d){
return -1;
}
return 0;
};
};
function inorderTraversalBuildup(_8,a){
if(_8){
inorderTraversalBuildup(_8.left,a);
a.push(_8.value);
inorderTraversalBuildup(_8.right,a);
}
};
function preorderTraversal(_a,_b){
var s="";
if(_a){
s=_a.value.toString()+_b;
s+=preorderTraversal(_a.left,_b);
s+=preorderTraversal(_a.right,_b);
}
return s;
};
function inorderTraversal(_d,_e){
var s="";
if(_d){
s=inorderTraversal(_d.left,_e);
s+=_d.value.toString()+_e;
s+=inorderTraversal(_d.right,_e);
}
return s;
};
function postorderTraversal(_10,sep){
var s="";
if(_10){
s=postorderTraversal(_10.left,sep);
s+=postorderTraversal(_10.right,sep);
s+=_10.value.toString()+sep;
}
return s;
};
function searchHelper(_13,_14){
if(!_13){
return null;
}
var i=_13.compareData(_14);
if(i==0){
return _13;
}
if(i>0){
return searchHelper(_13.left,_14);
}else{
return searchHelper(_13.right,_14);
}
};
this.add=function(_16){
var n=new node(_16);
var i;
var _19=_1a;
var _1b=null;
while(_19){
i=_19.compare(n);
if(i==0){
return;
}
_1b=_19;
if(i>0){
_19=_19.left;
}else{
_19=_19.right;
}
}
this.count++;
if(!_1b){
_1a=n;
}else{
i=_1b.compare(n);
if(i>0){
_1b.left=n;
}else{
_1b.right=n;
}
}
};
this.clear=function(){
_1a=null;
this.count=0;
};
this.clone=function(){
var c=new dojox.collections.BinaryTree();
var itr=this.getIterator();
while(!itr.atEnd()){
c.add(itr.get());
}
return c;
};
this.contains=function(_1e){
return this.search(_1e)!=null;
};
this.deleteData=function(_1f){
var _20=_1a;
var _21=null;
var i=_20.compareData(_1f);
while(i!=0&&_20!=null){
if(i>0){
_21=_20;
_20=_20.left;
}else{
if(i<0){
_21=_20;
_20=_20.right;
}
}
i=_20.compareData(_1f);
}
if(!_20){
return;
}
this.count--;
if(!_20.right){
if(!_21){
_1a=_20.left;
}else{
i=_21.compare(_20);
if(i>0){
_21.left=_20.left;
}else{
if(i<0){
_21.right=_20.left;
}
}
}
}else{
if(!_20.right.left){
if(!_21){
_1a=_20.right;
}else{
i=_21.compare(_20);
if(i>0){
_21.left=_20.right;
}else{
if(i<0){
_21.right=_20.right;
}
}
}
}else{
var _23=_20.right.left;
var _24=_20.right;
while(_23.left!=null){
_24=_23;
_23=_23.left;
}
_24.left=_23.right;
_23.left=_20.left;
_23.right=_20.right;
if(!_21){
_1a=_23;
}else{
i=_21.compare(_20);
if(i>0){
_21.left=_23;
}else{
if(i<0){
_21.right=_23;
}
}
}
}
}
};
this.getIterator=function(){
var a=[];
inorderTraversalBuildup(_1a,a);
return new dojox.collections.Iterator(a);
};
this.search=function(_26){
return searchHelper(_1a,_26);
};
this.toString=function(_27,sep){
if(!_27){
_27=dojox.collections.BinaryTree.TraversalMethods.Inorder;
}
if(!sep){
sep=",";
}
var s="";
switch(_27){
case dojox.collections.BinaryTree.TraversalMethods.Preorder:
s=preorderTraversal(_1a,sep);
break;
case dojox.collections.BinaryTree.TraversalMethods.Inorder:
s=inorderTraversal(_1a,sep);
break;
case dojox.collections.BinaryTree.TraversalMethods.Postorder:
s=postorderTraversal(_1a,sep);
break;
}
if(s.length==0){
return "";
}else{
return s.substring(0,s.length-sep.length);
}
};
this.count=0;
var _1a=this.root=null;
if(_1){
this.add(_1);
}
};
dojox.collections.BinaryTree.TraversalMethods={Preorder:1,Inorder:2,Postorder:3};
}
