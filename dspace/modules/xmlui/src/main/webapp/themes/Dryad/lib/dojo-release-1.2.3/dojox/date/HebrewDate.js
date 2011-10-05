/*
	Copyright (c) 2004-2008, The Dojo Foundation All Rights Reserved.
	Available via Academic Free License >= 2.1 OR the modified BSD license.
	see: http://dojotoolkit.org/license for details
*/


if(!dojo._hasResource["dojox.date.HebrewDate"]){
dojo._hasResource["dojox.date.HebrewDate"]=true;
dojo.provide("dojox.date.HebrewDate");
dojo.experimental("dojox.date.HebrewDate");
dojo.require("dojo.date.locale");
dojo.requireLocalization("dojo.cldr","hebrew",null,"");
dojo.declare("dojox.date.HebrewDate",null,{TISHRI:0,HESHVAN:1,KISLEV:2,TEVET:3,SHEVAT:4,ADAR_1:5,ADAR:6,NISAN:7,IYAR:8,SIVAN:9,TAMUZ:10,AV:11,ELUL:12,_HOUR_PARTS:1080,_DAY_PARTS:24*1080,_MONTH_DAYS:29,_MONTH_FRACT:12*1080+793,_MONTH_PARTS:29*24*1080+12*1080+793,BAHARAD:11*1080+204,JAN_1_1_JULIAN_DAY:1721426,_MONTH_LENGTH:[[30,30,30],[29,29,30],[29,30,30],[29,29,29],[30,30,30],[30,30,30],[29,29,29],[30,30,30],[29,29,29],[30,30,30],[29,29,29],[30,30,30],[29,29,29]],_MONTH_START:[[0,0,0],[30,30,30],[59,59,60],[88,89,90],[117,118,119],[147,148,149],[147,148,149],[176,177,178],[206,207,208],[235,236,237],[265,266,267],[294,295,296],[324,325,326],[353,354,355]],LEAP_MONTH_START:[[0,0,0],[30,30,30],[59,59,60],[88,89,90],[117,118,119],[147,148,149],[177,178,179],[206,207,208],[236,237,238],[265,266,267],[295,296,297],[324,325,326],[354,355,356],[383,384,385]],GREGORIAN_MONTH_COUNT:[[31,31,0,0],[28,29,31,31],[31,31,59,60],[30,30,90,91],[31,31,120,121],[30,30,151,152],[31,31,181,182],[31,31,212,213],[30,30,243,244],[31,31,273,274],[30,30,304,305],[31,31,334,335]],_date:0,_month:0,_year:0,_hours:0,_minutes:0,_seconds:0,_milliseconds:0,_day:0,constructor:function(){
var _1=arguments.length;
if(_1==0){
var _2=new Date();
var _3=this._computeHebrewFields(_2);
this._date=_3[2];
this._month=_3[1];
this._year=_3[0];
this._hours=_2.getHours();
this._minutes=_2.getMinutes();
this._seconds=_2.getSeconds();
this._milliseconds=_2.getMilliseconds();
this._day=_2.getDay();
}else{
if(_1==1){
this.parse(arguments[0]);
}else{
if(_1>=3){
this._date=parseInt(arguments[2]);
this._month=parseInt(arguments[1]);
this._year=parseInt(arguments[0]);
this._hours=(arguments[3]!=null)?parseInt(arguments[3]):0;
this._minutes=(arguments[4]!=null)?parseInt(arguments[4]):0;
this._seconds=(arguments[5]!=null)?parseInt(arguments[5]):0;
this._milliseconds=(arguments[6]!=null)?parseInt(arguments[6]):0;
}
}
}
var _4=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
_4+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
_4+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
_4+=(this._date-1);
this._day=((_4+1)%7);
},getDate:function(){
return parseInt(this._date);
},getMonth:function(){
return parseInt(this._month);
},getFullYear:function(){
return parseInt(this._year);
},getHours:function(){
return this._hours;
},getMinutes:function(){
return this._minutes;
},getSeconds:function(){
return this._seconds;
},getMilliseconds:function(){
return this._milliseconds;
},setDate:function(_5){
_5=parseInt(_5);
var _6;
if(_5>0){
for(_6=this.getDaysInHebrewMonth(this._month,this._year);_5>_6;_5-=_6,_6=this.getDaysInHebrewMonth(this._month,this._year)){
this._month++;
if(!this._isLeapYear(this._year)&&(this._month==5)){
this._month++;
}
if(this._month>=13){
this._year++;
this._month-=13;
}
}
this._date=_5;
}else{
for(_6=this.getDaysInHebrewMonth((this._month-1)>=0?(this._month-1):12,((this._month-1)>=0)?this._year:this._year-1);_5<=0;_6=this.getDaysInHebrewMonth((this._month-1)>=0?(this._month-1):12,((this._month-1)>=0)?this._year:this._year-1)){
this._month--;
if(!this._isLeapYear(this._year)&&this._month==5){
this._month--;
}
if(this._month<0){
this._year--;
this._month+=13;
}
_5+=_6;
}
this._date=_5;
}
var _7=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
_7+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
_7+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
_7+=(this._date-1);
this._day=((_7+1)%7);
return this;
},setYear:function(_8){
this._year=parseInt(_8);
if(!this._isLeapYear(this._year)&&this._month==5){
this._month++;
}
var _9=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
_9+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
_9+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
_9+=(this._date-1);
this._day=((_9+1)%7);
return this;
},setMonth:function(_a){
_a=parseInt(_a);
if(_a>=0){
this._year+=Math.floor(_a/13);
this._month=Math.floor(_a%13);
}else{
this._year+=Math.floor(_a/13);
this._month=13-Math.floor(-1*_a%13);
}
if(!this._isLeapYear(this._year)&&(this._month==5)){
this._month++;
}
var _b=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
_b+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
_b+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
_b+=(this._date-1);
this._day=((_b+1)%7);
return this;
},setHours:function(){
var _c=arguments.length;
var _d=0;
if(_c>=1){
_d=parseInt(arguments[0]);
}
if(_c>=2){
this._minutes=parseInt(arguments[1]);
}
if(_c>=3){
this._seconds=parseInt(arguments[2]);
}
if(_c==4){
this._milliseconds=parseInt(arguments[3]);
}
while(_d>=24){
this._date++;
var _e=this.getDaysInHebrewMonth(this._month,this._year);
if(this._date>_e){
this._month++;
if(!this._isLeapYear(this._year)&&(this._month==5)){
this._month++;
}
if(this._month>=13){
this._year++;
this._month-=13;
}
this._date-=_e;
}
_d-=24;
}
this._hours=_d;
var _f=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
_f+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
_f+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
_f+=(this._date-1);
this._day=((_f+1)%7);
return this;
},setMinutes:function(_10){
while(_10>=60){
this._hours++;
if(this._hours>=24){
this._date++;
this._hours-=24;
var _11=this.getDaysInHebrewMonth(this._month,this._year);
if(this._date>_11){
this._month++;
if(!this._isLeapYear(this._year)&&(this._month==5)){
this._month++;
}
if(this._month>=13){
this._year++;
this._month-=13;
}
this._date-=_11;
}
}
_10-=60;
}
this._minutes=_10;
var day=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
day+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
day+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
day+=(this._date-1);
this._day=((day+1)%7);
return this;
},setSeconds:function(_13){
while(_13>=60){
this._minutes++;
if(this._minutes>=60){
this._hours++;
this._minutes-=60;
if(this._hours>=24){
this._date++;
this._hours-=24;
var _14=this.getDaysInHebrewMonth(this._month,this._year);
if(this._date>_14){
this._month++;
if(!this._isLeapYear(this._year)&&(this._month==5)){
this._month++;
}
if(this._month>=13){
this._year++;
this._month-=13;
}
this._date-=_14;
}
}
}
_13-=60;
}
this._seconds=_13;
var day=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
day+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
day+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
day+=(this._date-1);
this._day=((day+1)%7);
return this;
},setMilliseconds:function(_16){
while(_16>=1000){
this.setSeconds++;
if(this.setSeconds>=60){
this._minutes++;
this._seconds-=60;
if(this._minutes>=60){
this._hours++;
this._minutes-=60;
if(this._hours>=24){
this._date++;
this._hours-=24;
var _17=this.getDaysInHebrewMonth(this._month,this._year);
if(this._date>_17){
this._month++;
if(!this._isLeapYear(this._year)&&(this._month==5)){
this._month++;
}
if(this._month>=13){
this._year++;
this._month-=13;
}
this._date-=_17;
}
}
}
}
_16-=1000;
}
this._milliseconds=_16;
var day=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
day+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
day+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
day+=(this._date-1);
this._day=((day+1)%7);
return this;
},toString:function(){
var x=new Date();
x.setHours(this._hours);
x.setMinutes(this._minutes);
x.setSeconds(this._seconds);
x.setMilliseconds(this._milliseconds);
var _1a=x.toTimeString();
return dojox.date.HebrewDate.weekDays[this._day]+" "+dojox.date.HebrewDate.months[this._month]+" "+this._date+" "+this._year+" "+_1a;
},parse:function(_1b){
var _1c=_1b.toString();
var _1d=/\d{1,2}\D\d{1,2}\D\d{4}/;
var mD=_1c.match(_1d);
if(mD!=null){
mD=mD.toString();
var sD=mD.split(/\D/);
this._month=sD[0]-1;
this._date=sD[1];
this._year=sD[2];
}else{
mD=_1c.match(/\D{3}\s\D{2,}\s\d{1,2}\s\d{4}/);
if(mD!=null){
mD=mD.toString();
var _20=mD.match(/\d{1,2}\s\d{4}/);
_20=_20.toString();
var _21=mD.replace(/\s\d{1,2}\s\d{4}/,"");
_21=_21.toString();
var _21=_21.replace(/\D{3}\s/,"");
_21=_21.toString();
this._month=dojo.indexOf(dojox.date.HebrewDate.months,_21);
var sD=_20.split(/\s/);
this._date=sD[0];
this._year=sD[1];
var day=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
day+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
day+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
day+=(this._date-1);
this._day=((day+1)%7);
}else{
mD=_1c.match(/\D{2,}\s\d{1,2}\s\d{4}/);
if(mD!=null){
mD=mD.toString();
var _20=mD.match(/\d{1,2}\s\d{4}/);
_20=_20.toString();
var _21=mD.replace(/\s\d{1,2}\s\d{4}/,"");
_21=_21.toString();
this._month=dojo.indexOf(dojox.date.HebrewDate.months,_21);
var sD=_20.split(/\s/);
this._date=sD[0];
this._year=sD[1];
var day=this._startOfYear(this._year);
if(this._month!=0){
if(this._isLeapYear(this._year)){
day+=this.LEAP_MONTH_START[this._month][this._yearType(this._year)];
}else{
day+=this._MONTH_START[this._month][this._yearType(this._year)];
}
}
day+=(this._date-1);
this._day=((day+1)%7);
}
}
}
var _23=_1c.match(/\d{2}:/);
if(_23!=null){
_23=_23.toString();
var _24=_23.split(":");
this._hours=_24[0];
_23=_1c.match(/\d{2}:\d{2}/);
if(_23){
_23=_23.toString();
_24=_23.split(":");
}
this._minutes=_24[1]!=null?_24[1]:0;
_23=_1c.match(/\d{2}:\d{2}:\d{2}/);
if(_23){
_23=_23.toString();
_24=_23.split(":");
}
this._seconds=_24[2]!=null?_24[2]:0;
}else{
this._hours=0;
this._minutes=0;
this._seconds=0;
}
this._milliseconds=0;
},valueOf:function(){
return this.toGregorian().valueOf();
},getDaysInHebrewMonth:function(_25,_26){
switch(_25){
case this.HESHVAN:
case this.KISLEV:
return this._MONTH_LENGTH[_25][this._yearType(_26)];
default:
return this._MONTH_LENGTH[_25][0];
}
},_yearType:function(_27){
var _28=this._handleGetYearLength(Number(_27));
if(_28>380){
_28-=30;
}
switch(_28){
case 353:
return 0;
case 354:
return 1;
case 355:
return 2;
}
throw new Error("Illegal year length "+_28+" in year "+_27);
},_handleGetYearLength:function(_29){
return this._startOfYear(_29+1)-this._startOfYear(_29);
},_startOfYear:function(_2a){
var _2b=Math.floor((235*_2a-234)/19);
var _2c=_2b*this._MONTH_FRACT+this.BAHARAD;
var day=_2b*29+Math.floor(_2c/this._DAY_PARTS);
_2c=_2c%this._DAY_PARTS;
var wd=day%7;
if(wd==2||wd==4||wd==6){
day+=1;
wd=day%7;
}
if(wd==1&&_2c>15*this.HOUR_PARTS+204&&!this._isLeapYear(_2a)){
day+=2;
}else{
if(wd==0&&_2c>21*this.HOUR_PARTS+589&&this._isLeapYear(_2a-1)){
day+=1;
}
}
return day;
},_isLeapYear:function(_2f){
var x=(_2f*12+17)%19;
return x>=((x<0)?-7:12);
},fromGregorian:function(_31){
var _32=this._computeHebrewFields(_31);
this._year=_32[0];
this._month=_32[1];
this._date=_32[2];
this._hours=_31.getHours();
this._milliseconds=_31.getMilliseconds();
this._minutes=_31.getMinutes();
this._seconds=_31.getSeconds();
return this;
},_computeHebrewFields:function(_33){
var _34=this._getJulianDayFromGregorianDate(_33);
var d=_34-347997;
var m=Math.floor((d*this._DAY_PARTS)/this._MONTH_PARTS);
var _37=Math.floor((19*m+234)/235)+1;
var ys=this._startOfYear(_37);
var _39=(d-ys);
while(_39<1){
_37--;
ys=this._startOfYear(_37);
_39=(d-ys);
}
var _3a=this._yearType(_37);
var _3b=this._isLeapYear(_37)?this.LEAP_MONTH_START:this._MONTH_START;
var _3c=0;
while(_39>_3b[_3c][_3a]){
_3c++;
}
_3c--;
var _3d=_39-_3b[_3c][_3a];
var _3e=new Array(3);
_3e[0]=_37;
_3e[1]=_3c;
_3e[2]=_3d;
return _3e;
},toGregorian:function(){
var _3f=this._year;
var _40=this._month;
var _41=this._date;
var day=this._startOfYear(_3f);
if(_40!=0){
if(this._isLeapYear(_3f)){
day+=this.LEAP_MONTH_START[_40][this._yearType(_3f)];
}else{
day+=this._MONTH_START[_40][this._yearType(_3f)];
}
}
var _43=(_41+day+347997);
var _44=_43-this.JAN_1_1_JULIAN_DAY;
var rem=new Array(1);
var _46=this._floorDivide(_44,146097,rem);
var _47=this._floorDivide(rem[0],36524,rem);
var n4=this._floorDivide(rem[0],1461,rem);
var n1=this._floorDivide(rem[0],365,rem);
var _4a=400*_46+100*_47+4*n4+n1;
var _4b=rem[0];
if(_47==4||n1==4){
_4b=365;
}else{
++_4a;
}
var _4c=(_4a%4==0)&&(_4a%100!=0||_4a%400==0);
var _4d=0;
var _4e=_4c?60:59;
if(_4b>=_4e){
_4d=_4c?1:2;
}
var _4f=Math.floor((12*(_4b+_4d)+6)/367);
var _50=_4b-this.GREGORIAN_MONTH_COUNT[_4f][_4c?3:2]+1;
return new Date(_4a,_4f,_50,this._hours,this._minutes,this._seconds,this._milliseconds);
},_floorDivide:function(_51,_52,_53){
if(_51>=0){
_53[0]=(_51%_52);
return Math.floor(_51/_52);
}
var _54=Math.floor(_51/_52);
_53[0]=_51-(_54*_52);
return _54;
},getDay:function(){
var _55=this._year;
var _56=this._month;
var _57=this._date;
var day=this._startOfYear(_55);
if(_56!=0){
if(this._isLeapYear(_55)){
day+=this.LEAP_MONTH_START[_56][this._yearType(_55)];
}else{
day+=this._MONTH_START[_56][this._yearType(_55)];
}
}
day+=(_57-1);
return ((day+1)%7);
},_getJulianDayFromGregorianDate:function(_59){
var _5a=_59.getFullYear();
var _5b=_59.getMonth();
var d=_59.getDate();
var _5d=(_5a%4==0)&&((_5a%100!=0)||(_5a%400==0));
var y=_5a-1;
var _5f=365*y+Math.floor(y/4)-Math.floor(y/100)+Math.floor(y/400)+this.JAN_1_1_JULIAN_DAY-1;
if(_5b!=0){
_5f+=this.GREGORIAN_MONTH_COUNT[_5b][_5d?3:2];
}
_5f+=d;
return _5f;
}});
dojox.date.HebrewDate.getDaysInHebrewMonth=function(_60){
return new dojox.date.HebrewDate().getDaysInHebrewMonth(_60.getMonth(),_60.getFullYear());
};
dojox.date.HebrewDate._getNames=function(_61,_62,use,_64){
var _65;
var _66=dojo.i18n.getLocalization("dojo.cldr","hebrew",_64);
var _67=[_61,use,_62];
if(use=="standAlone"){
_65=_66[_67.join("-")];
}
_67[1]="format";
return (_65||_66[_67.join("-")]).concat();
};
dojox.date.HebrewDate.weekDays=dojox.date.HebrewDate._getNames("days","wide","format");
dojox.date.HebrewDate.months=dojox.date.HebrewDate._getNames("months","wide","abbr");
}
