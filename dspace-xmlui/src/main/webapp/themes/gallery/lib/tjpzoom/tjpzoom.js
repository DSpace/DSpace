// TJPzoom 3 * János Pál Tóth
// 2007.07.12
// Docs @ http://valid.tjp.hu/tjpzoom/ 
// News @ http://tjpzoom.blogspot.com/

function TJPzoomswitch(obj) {
 TJPon[obj]=((TJPon[obj])?(0):(1));
 return TJPon[obj];
}

function TJPzoomif(obj,highres) {
 if(TJPon[obj]) {TJPzoom(obj,highres);}
}

function TJPzoom(obj,highres) {
 TJPzoomratio=TJPzoomheight/TJPzoomwidth;
 if(TJPzoomoffsetx > 1) {
  TJPzoomoffset='dumb';
  TJPzoomoffsetx=TJPzoomoffsetx/TJPzoomwidth;
  TJPzoomoffsety=TJPzoomoffsety/TJPzoomheight;
 }
 if(!obj.style.width) {
  if(obj.width > 0) {
   //educated guess
   obj.style.width=obj.width+'px';
   obj.style.height=obj.height+'px';
  }
 }
 if(typeof(highres) != typeof('')) {highres=obj.src}
 var TJPstage=document.createElement("div");
 TJPstage.style.width=obj.style.width;
 TJPstage.style.height=obj.style.height;
 TJPstage.style.overflow='hidden';
 TJPstage.style.position='absolute';
 if(typeof(TJPstage.style.filter) != typeof(nosuchthing)) {
  //hi IE
  if(navigator.appVersion.indexOf('Mac') == -1) { //hi Mac IE
   TJPstage.style.filter='alpha(opacity=0)';
   TJPstage.style.backgroundColor='#ffffff';
  }
 } else {
  //hi decent gentlemen
  TJPstage.style.backgroundImage='transparent';
 }
 TJPstage.setAttribute('onmousemove','TJPhandlemouse(event,this);');
 TJPstage.setAttribute('onmousedown','TJPhandlemouse(event,this);');
 TJPstage.setAttribute('onmouseup','TJPhandlemouse(event,this);');
 TJPstage.setAttribute('onmouseout','TJPhandlemouse(event,this);');
 if(navigator.userAgent.indexOf('MSIE')>-1) {
  TJPstage.onmousemove = function() {TJPhandlemouse(event,this);}
  TJPstage.onmousedown = function() {TJPhandlemouse(event,this);}
  TJPstage.onmouseup = function() {TJPhandlemouse(event,this);}
  TJPstage.onmouseout = function() {TJPhandlemouse(event,this);}
 }
 obj.parentNode.insertBefore(TJPstage,obj);
 
 TJPwin=document.createElement("div");
 TJPwin.style.width='0px';
 TJPwin.style.height='0px';
 TJPwin.style.overflow='hidden';
 TJPwin.style.position='absolute';
 TJPwin.style.display='none';
 tw1='<div style="position:absolute;overflow:hidden;margin:';
 TJPwin.innerHTML= 
 tw1+TJPshadowthick+'px 0 0 '+TJPshadowthick+'px; background-color:'+TJPbordercolor+'; width:'+(TJPzoomwidth-TJPshadowthick*2)+'px;height:'+(TJPzoomheight-TJPshadowthick*2)+'px"></div>' +
 tw1+(TJPshadowthick+TJPborderthick)+'px 0 0 '+(TJPshadowthick+TJPborderthick)+'px; width:'+(TJPzoomwidth-TJPshadowthick*2-TJPborderthick*2)+'px;height:'+(TJPzoomheight-TJPshadowthick*2-TJPborderthick*2)+'px;"><img src="'+obj.src+'" style="position:absolute;margin:0;padding:0;border:0; width:'+(TJPzoomamount*parseInt(obj.style.width))+'px;height:'+(TJPzoomamount*parseInt(obj.style.height))+'px;" />'+((obj.src!=highres)?('<img src="'+highres+'" style="position:absolute;margin:0;padding:0;border:0; width:'+(TJPzoomamount*parseInt(obj.style.width))+'px;height:'+(TJPzoomamount*parseInt(obj.style.height))+'px;" onload="if(this.parentNode) {this.parentNode.parentNode.getElementsByTagName(\'div\')[2].style.display=\'none\';}" />'):(''))+'</div>';
 if(highres != obj.src) {
  TJPwin.innerHTML+='<div style="position:absolute; margin:'+(TJPshadowthick+TJPborderthick)+'px 0 0 '+(TJPshadowthick+TJPborderthick)+'px;">'+TJPloading+'</div>';
 }
 if(TJPshadowthick>0) {
  st1='<span style="position:absolute; display:inline-block; margin: ';
  st2='filter:progid:DXImageTransform.Microsoft.AlphaImageLoader(sizingMethod=\'scale\',src='
  st3='filter:alpha(opacity=0);margin:0;padding:0;border:0;"/></span>';
  TJPwin.innerHTML+=
  st1+'0 0 0 0    ; width:'+TJPshadowthick*2+'px; height:'+TJPshadowthick*2+'px;'+st2+'\''+TJPshadow+'nw.png\')"><img src="'+TJPshadow+'nw.png" style="width:'+TJPshadowthick*2+'px; height:'+TJPshadowthick*2+'px;'+st3 +
  st1+'0 0 0 '+(TJPzoomwidth-TJPshadowthick*2)+'px; width:'+TJPshadowthick*2+'px; height:'+TJPshadowthick*2+'px;'+st2+'\''+TJPshadow+'ne.png\')"><img src="'+TJPshadow+'ne.png" style="width:'+TJPshadowthick*2+'px; height:'+TJPshadowthick*2+'px;'+st3 +
  st1+''+(TJPzoomheight-TJPshadowthick*2)+'px 0 0 0px; width:'+TJPshadowthick*2+'px; height:'+TJPshadowthick*2+'px;'+st2+'\''+TJPshadow+'sw.png\',sizingMethod=\'scale\')"><img src="'+TJPshadow+'sw.png" style="width:'+TJPshadowthick*2+'px; height:'+TJPshadowthick*2+'px;'+st3 +
  st1+''+(TJPzoomheight-TJPshadowthick*2)+'px 0 0 '+(TJPzoomwidth-TJPshadowthick*2)+'px; width:'+TJPshadowthick*2+'px; height:'+TJPshadowthick*2+'px;'+st2+'\''+TJPshadow+'se.png\',sizingMethod=\'scale\')"><img src="'+TJPshadow+'se.png" style="width:'+TJPshadowthick*2+'px; height:'+TJPshadowthick*2+'px;'+st3 +
  
  st1+'0 0 0 '+(TJPshadowthick*2)+'px; width:'+(TJPzoomwidth-TJPshadowthick*4)+'px; height:'+TJPshadowthick*2+'px;'+st2+'\''+TJPshadow+'n.png\',sizingMethod=\'scale\')"><img src="'+TJPshadow+'n.png" style="width:'+(TJPzoomwidth-TJPshadowthick*4)+'px; height:'+TJPshadowthick*2+'px;'+st3 +
  st1+''+(TJPshadowthick*2)+'px 0 0 0; width:'+(TJPshadowthick*2)+'px; height:'+(TJPzoomheight-TJPshadowthick*4)+'px;'+st2+'\''+TJPshadow+'w.png\',sizingMethod=\'scale\')"><img src="'+TJPshadow+'w.png" style="width:'+(TJPshadowthick*2)+'px; height:'+(TJPzoomheight-TJPshadowthick*4)+'px;'+st3 +
  st1+''+(TJPshadowthick*2)+'px 0 0 '+(TJPzoomwidth-TJPshadowthick*2)+'px; width:'+(TJPshadowthick*2)+'px; height:'+(TJPzoomheight-TJPshadowthick*4)+'px;'+st2+'\''+TJPshadow+'e.png\',sizingMethod=\'scale\')"><img src="'+TJPshadow+'e.png" style="width:'+(TJPshadowthick*2)+'px; height:'+(TJPzoomheight-TJPshadowthick*4)+'px;'+st3 +
  st1+''+(TJPzoomheight-TJPshadowthick*2)+'px 0 0 '+(TJPshadowthick*2)+'px; width:'+(TJPzoomwidth-TJPshadowthick*4)+'px; height:'+TJPshadowthick*2+'px;'+st2+'\''+TJPshadow+'s.png\',sizingMethod=\'scale\')"><img src="'+TJPshadow+'s.png" style="width:'+(TJPzoomwidth-TJPshadowthick*4)+'px; height:'+TJPshadowthick*2+'px;'+st3;
 }
 ;
 //marker - zoomer
 obj.parentNode.insertBefore(TJPwin,TJPstage);

 TJPresize(obj);
}

function TJPresize(obj) {
 sbr=0; sbl=0;
 if(TJPzoomwidth-2*TJPborderthick-3*TJPshadowthick < 22) {sbr=1}
 if(TJPzoomheight-2*TJPborderthick-3*TJPshadowthick < 22) {sbr=1}
 if(TJPzoomwidth > parseFloat(obj.style.width)) {sbl=1;}
 if(TJPzoomheight > parseFloat(obj.style.height)) {sbl=1}
 
 if(sbr==1 && sbl == 1) {
  TJPzoomwidth=parseFloat(obj.style.width)/2;
  TJPzoomheight=parseFloat(obj.style.height)/2;
  TJPzoomratio=TJPzoomheight/TJPzoomwidth;
 }

 if(sbr==1) {
  if(TJPzoomwidth<TJPzoomheight) {
   TJPzoomheight=TJPzoomheight/TJPzoomwidth*(22+2*TJPborderthick+3*TJPshadowthick); TJPzoomwidth=22+2*TJPborderthick+3*TJPshadowthick;
  } else {
   TJPzoomwidth=TJPzoomwidth/TJPzoomheight*(22+2*TJPborderthick+3*TJPshadowthick); TJPzoomheight=22+2*TJPborderthick+3*TJPshadowthick;
  }
 }
 

 if(sbl==1) {
  if(parseFloat(obj.style.width)/parseFloat(obj.style.height) > TJPzoomwidth/TJPzoomheight) {
   TJPzoomheight=parseFloat(obj.style.height);
   TJPzoomwidth=TJPzoomheight/TJPzoomratio;
  } else {
   TJPzoomwidth=parseFloat(obj.style.width);
   TJPzoomheight=TJPzoomratio*TJPzoomwidth;
  }
 }

 TJPzoomwidth=Math.floor(TJPzoomwidth/2)*2;
 TJPzoomheight=Math.floor(TJPzoomheight/2)*2;

 ww=obj.parentNode.getElementsByTagName('div')[0];
 ww.style.width=TJPzoomwidth+'px';
 ww.style.height=TJPzoomheight+'px';
 w=ww.getElementsByTagName('div')[0];
 w.style.width=TJPzoomwidth-TJPshadowthick*2+'px';
 w.style.height=TJPzoomheight-TJPshadowthick*2+'px';
 w=ww.getElementsByTagName('div')[1];
 w.style.width=TJPzoomwidth-TJPshadowthick*2-TJPborderthick*2+'px';
 w.style.height=TJPzoomheight-TJPshadowthick*2-TJPborderthick*2+'px';
 if(TJPshadowthick > 0) {
  w=ww.getElementsByTagName('span')[1]; w.style.margin='0 0 0 '+(TJPzoomwidth-TJPshadowthick*2)+'px';
  w=ww.getElementsByTagName('span')[2]; w.style.margin=(TJPzoomheight-TJPshadowthick*2)+'px 0 0 0px';
  w=ww.getElementsByTagName('span')[3]; w.style.margin=(TJPzoomheight-TJPshadowthick*2)+'px 0 0 '+(TJPzoomwidth-TJPshadowthick*2)+'px';

  w=ww.getElementsByTagName('span')[6]; w.style.margin=(TJPshadowthick*2)+'px 0 0 '+(TJPzoomwidth-TJPshadowthick*2)+'px';
  w=ww.getElementsByTagName('span')[7]; w.style.margin=(TJPzoomheight-TJPshadowthick*2)+'px 0 0 '+(TJPshadowthick*2)+'px';

  www=(TJPzoomwidth-TJPshadowthick*4)+'px';
  w=ww.getElementsByTagName('span')[4]; w.style.width=www;
  w=w.getElementsByTagName('img')[0]; w.style.width=www;
  w=ww.getElementsByTagName('span')[7]; w.style.width=www;
  w=w.getElementsByTagName('img')[0]; w.style.width=www;
  
  www=(TJPzoomheight-TJPshadowthick*4)+'px';
  w=ww.getElementsByTagName('span')[5]; w.style.height=www;
  w=w.getElementsByTagName('img')[0]; w.style.height=www;
  w=ww.getElementsByTagName('span')[6]; w.style.height=www;
  w=w.getElementsByTagName('img')[0]; w.style.height=www;
 }
}

function TJPfindposy(obj) {
 var curtop = 0;
 if(!obj) {return 0;}
 if (obj.offsetParent) {
  while (obj.offsetParent) {
   curtop += obj.offsetTop
   obj = obj.offsetParent;
  }
 } else if (obj.y) {
  curtop += obj.y;
 }
 return curtop;
}

function TJPfindposx(obj) {
 var curleft = 0;
 if(!obj) {return 0;}
 if (obj && obj.offsetParent) {
  while (obj.offsetParent) {
   curleft += obj.offsetLeft
   obj = obj.offsetParent;
  }
 } else if (obj.x) {
  curleft += obj.x;
 }
 return curleft;
}


function TJPhandlemouse(evt,obj) {
 var evt = evt?evt:window.event?window.event:null; if(!evt) { return false; }
 if(evt.pageX) {
  nowx=evt.pageX-TJPfindposx(obj)-TJPadjustx;
  nowy=evt.pageY-TJPfindposy(obj)-TJPadjusty;
 } else {
  if(document.documentElement && document.documentElement.scrollTop) {
   nowx=evt.clientX+document.documentElement.scrollLeft-TJPfindposx(obj)-TJPadjustx;
   nowy=evt.clientY+document.documentElement.scrollTop-TJPfindposy(obj)-TJPadjusty;
  } else {
   nowx=evt.x+document.body.scrollLeft-TJPfindposx(obj)-TJPadjustx;
   nowy=evt.y+document.body.scrollTop-TJPfindposy(obj)-TJPadjusty;
  }
 }
 if(evt.type == 'mousemove') {
  TJPsetwin(obj,nowx,nowy);
 } else if(evt.type == 'mousedown') {
  TJPmouse=1; //left: 1, middle: 2, right: 3
  TJPmousey=nowy;
  TJPmousex=nowx;
 } else if(evt.type =='mouseup') {
  TJPmouse=0;
 } else if(evt.type =='mouseout') {
  TJPmouse=0;
  if(navigator.appVersion.indexOf('Mac') == -1 || navigator.appVersion.indexOf('MSIE') == -1) { //hi Mac IE
   x=obj.parentNode;
   x.removeChild(x.getElementsByTagName('div')[0]);
   x.removeChild(x.getElementsByTagName('div')[0]);
  }
 }
}


// TJPzoom 3 * János Pál Tóth
// Docs @ http://valid.tjp.hu/tjpzoom/ 
// News @ http://tjpzoom.blogspot.com/


function TJPsetwin(obj,nowx,nowy) {
 obj.parentNode.getElementsByTagName('div')[0].style.display='block';
 if(TJPzoomoffset=='smart') {
  TJPzoomoffsetx=.1+.8*nowx/parseFloat(obj.style.width);
  TJPzoomoffsety=.1+.8*nowy/parseFloat(obj.style.height);
 }

 stage=obj.parentNode.getElementsByTagName('div')[0];
 if(TJPmouse == 1) {
  if(Math.abs(nowy-TJPmousey) >= 1) {
   TJPzoomamount*=((nowy>TJPmousey)?(0.909):(1.1));
   TJPmousey=nowy;
   if(TJPzoomamount < TJPzoomamountmin) {TJPzoomamount=TJPzoomamountmin;}
   if(TJPzoomamount > TJPzoomamountmax) {TJPzoomamount=TJPzoomamountmax;}
   stage.getElementsByTagName('div')[1].getElementsByTagName('img')[0].style.width=  parseInt(obj.style.width)*TJPzoomamount+'px';
   stage.getElementsByTagName('div')[1].getElementsByTagName('img')[0].style.height=  parseInt(obj.style.height)*TJPzoomamount+'px';
   if(stage.getElementsByTagName('div')[1].getElementsByTagName('img')[1]) {
    stage.getElementsByTagName('div')[1].getElementsByTagName('img')[1].style.width= stage.getElementsByTagName('div')[1].getElementsByTagName('img')[0].style.width;
    stage.getElementsByTagName('div')[1].getElementsByTagName('img')[1].style.height= stage.getElementsByTagName('div')[1].getElementsByTagName('img')[0].style.height;
   }
  }
  if(Math.abs(nowx-TJPmousex) >= 12 && TJPzoomwindowlock==0) {
   TJPzoomwidth*=((nowx>TJPmousex)?(1.1):(0.909));
   TJPzoomheight=TJPzoomwidth*TJPzoomratio;
   TJPresize(obj);
   TJPmousex=nowx;
  }
 }
 stage.style.marginLeft=nowx-(TJPzoomwidth -2*TJPborderthick-2*TJPshadowthick)*TJPzoomoffsetx-TJPborderthick-TJPshadowthick+'px';
 stage.style.marginTop= nowy-(TJPzoomheight-2*TJPborderthick-2*TJPshadowthick)*TJPzoomoffsety-TJPborderthick-TJPshadowthick+'px';
 clip1=0; clip2=TJPzoomwidth; clip3=TJPzoomheight; clip4=0;
 nwidth=TJPzoomwidth; nheight=TJPzoomheight;
 tmp=(1-2*TJPzoomoffsetx)*(TJPborderthick+TJPshadowthick);
 
 if(nowx-TJPzoomwidth*TJPzoomoffsetx < tmp) {
  clip4=TJPzoomwidth*TJPzoomoffsetx-nowx + tmp;
 } else if(parseFloat(nowx-TJPzoomwidth*TJPzoomoffsetx+TJPzoomwidth) > parseFloat(obj.style.width)+tmp) {
  clip2= TJPzoomwidth*TJPzoomoffsetx - nowx + parseFloat(obj.style.width)+tmp;
  nwidth=TJPzoomwidth*TJPzoomoffsetx-nowx+parseInt(obj.style.width)+TJPborderthick+TJPshadowthick;
 }
 
 tmp=(1-2*TJPzoomoffsety)*(TJPborderthick+TJPshadowthick);
 
 if(nowy-TJPzoomheight*TJPzoomoffsety < tmp) {
  clip1=TJPzoomheight*TJPzoomoffsety-nowy+tmp;
 } else if(parseFloat(nowy-TJPzoomheight*TJPzoomoffsety+TJPzoomheight) > parseFloat(obj.style.height)+tmp) {
  clip3= TJPzoomheight*TJPzoomoffsety - nowy + parseFloat(obj.style.height)+tmp;
  nheight=TJPzoomheight*TJPzoomoffsety - nowy + parseFloat(obj.style.height)+TJPborderthick+TJPshadowthick;
 }
 stage.style.width=nwidth+'px';
 stage.style.height=nheight+'px';

 stage.style.clip='rect('+clip1+'px,'+clip2+'px,'+clip3+'px,'+clip4+'px)';

 if(nowy-TJPzoomoffsety*(TJPzoomheight-2*TJPborderthick-2*TJPshadowthick) < 0) { t=-(nowy-TJPzoomoffsety*(TJPzoomheight-2*TJPborderthick-2*TJPshadowthick))} 
 else if(nowy-TJPzoomoffsety*(TJPzoomheight-2*TJPborderthick-2*TJPshadowthick) > parseFloat(obj.style.height)-TJPzoomheight+TJPborderthick*2+TJPshadowthick*2) { t=-TJPzoomamount*parseFloat(obj.style.height)+TJPzoomheight-TJPborderthick*2-TJPshadowthick*2-((nowy-TJPzoomoffsety*(TJPzoomheight-2*TJPborderthick-2*TJPshadowthick))-(parseFloat(obj.style.height)-TJPzoomheight+TJPborderthick*2+TJPshadowthick*2)); }
 else { t=(-TJPzoomamount*parseFloat(obj.style.height)+TJPzoomheight-TJPborderthick*2-TJPshadowthick*2)/(parseFloat(obj.style.height)-TJPzoomheight+TJPborderthick*2+TJPshadowthick*2)*(nowy-TJPzoomoffsety*(TJPzoomheight-2*TJPborderthick-2*TJPshadowthick)) }
 stage.getElementsByTagName('div')[1].getElementsByTagName('img')[0].style.marginTop=t+'px';

 if(stage.getElementsByTagName('div')[1].getElementsByTagName('img')[1]) {
  stage.getElementsByTagName('div')[1].getElementsByTagName('img')[1].style.marginTop=t+'px';
 }

 if(nowx-TJPzoomoffsetx*(TJPzoomwidth-2*TJPborderthick-2*TJPshadowthick) < 0) { t=-(nowx-TJPzoomoffsetx*(TJPzoomwidth-2*TJPborderthick-2*TJPshadowthick))} 
 else if(nowx-TJPzoomoffsetx*(TJPzoomwidth-2*TJPborderthick-2*TJPshadowthick) > parseFloat(obj.style.width)-TJPzoomwidth+TJPborderthick*2+TJPshadowthick*2) { t=-TJPzoomamount*parseFloat(obj.style.width)+TJPzoomwidth-TJPborderthick*2-TJPshadowthick*2-((nowx-TJPzoomoffsetx*(TJPzoomwidth-2*TJPborderthick-2*TJPshadowthick))-(parseFloat(obj.style.width)-TJPzoomwidth+TJPborderthick*2+TJPshadowthick*2)); }
 else { t=(-TJPzoomamount*parseFloat(obj.style.width)+TJPzoomwidth-TJPborderthick*2-TJPshadowthick*2)/(parseFloat(obj.style.width)-TJPzoomwidth+TJPborderthick*2+TJPshadowthick*2)*(nowx-TJPzoomoffsetx*(TJPzoomwidth-2*TJPborderthick-2*TJPshadowthick)) }
 stage.getElementsByTagName('div')[1].getElementsByTagName('img')[0].style.marginLeft=t+'px';

 if(stage.getElementsByTagName('div')[1].getElementsByTagName('img')[1]) {
  stage.getElementsByTagName('div')[1].getElementsByTagName('img')[1].style.marginLeft=t+'px';
 }
}

function TJPinit() {
 TJPadjustx=0; TJPadjusty=0;
 if(navigator.userAgent.indexOf('MSIE')>-1) {TJPadjustx=2;TJPadjusty=2;}
 if(navigator.userAgent.indexOf('Opera')>-1) {TJPadjustx=0; TJPadjusty=0;}
 if(navigator.userAgent.indexOf('Safari')>-1) {TJPadjustx=1; TJPadjusty=2;}
}

// configuration - do not modify the following, instead read the behaviors.html file in the tutorial!
var TJPon=new Array();
var TJPadjustx,TJPadjusty;
var TJPmouse=0; var TJPmousey; var TJPmousex;
var TJPloading='<div style="background-color: #ffeb77; color: #333333; padding:2px; font-family: verdana,arial,helvetica; font-size: 10px;">Loading...</div>';

var TJPzoomwidth=160;
var TJPzoomheight=120;
var TJPzoomratio;
var TJPzoomwindowlock=0;

var TJPzoomoffsetx=.5;
var TJPzoomoffsety=.5;
var TJPzoomoffset;

var TJPzoomamount=4;
var TJPzoomamountmax=12;
var TJPzoomamountmin=1;

var TJPborderthick=2;
var TJPbordercolor='#888888';

var TJPshadowthick=8;
var TJPshadow='dropshadow/';

TJPinit();

// TJPzoom 3 * János Pál Tóth
// Docs @ http://valid.tjp.hu/tjpzoom/ 
// News @ http://tjpzoom.blogspot.com/
