/*
   IIPImage Javascript Viewer <http://iipimage.sourceforge.net>
                        Version 2.0

   Copyright (c) 2007-2011 Ruven Pillay <ruven@users.sourceforge.net>


   Built using the Mootools 1.3.2 javascript framework <http://www.mootools.net>


   ---------------------------------------------------------------------------

   This program is free software; you can redistribute it and/or modify
   it under the terms of the GNU General Public License as published by
   the Free Software Foundation; either version 3 of the License, or
   (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program; if not, write to the Free Software
   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

   ---------------------------------------------------------------------------


  Example:

   iip = new IIPMooViewer( 'div_id', { server: '/fcgi-bin/iipsrv.fcgi',
                              image: '/images/test.tif',
                              credit: 'copyright me 2011',
			      prefix: '/prefix/',
			      render: 'random',
                              showNavButtons: whether to show navigation buttons: true (default) or false
			      scale: 100 } );

   where the arguments are:
	i) The id of the main div element in which to create the viewer window
	ii) A hash containting:
	      image: the full image path (or array of paths) on the server (required)
              server: the iipsrv server full URL (defaults to "/fcgi-bin/iipsrv.fcgi")
	      credit: image copyright or information (optional)
	      prefix: path prefix if images or javascript subdirectory moved (default 'images/')
              render: tile rendering style - 'spiral' for a spiral from the centre or
                      'random' for a rendering of tiles in a random order
	      scale: pixels per mm
	      showNavWindow: whether to show the navigation window. Default true
	      showNavButtons: whether to show the navigation buttons. Default true
	      protocol: iip (default), zoomify or deepzoom
	      enableFullscreen: allow full screen mode. Default true
	      viewport: object containing x, y, resolution of initial view
	      winResize: whether view is reflowed on window resize. Default true

   Note: Requires mootools version 1.3.2 <http://www.mootools.net>
       : The page MUST have a standard-compliant HTML declaration at the beginning

*/



/* IIPMooViewer Javascript Class
 */
var IIPMooViewer = new Class({


  version: '2.0',


  /* Constructor
   */
  initialize: function( main_id, options ) {

    this.source = main_id || alert( 'No element ID given to IIPMooViewer constructor' );

    this.server = options.server || '/fcgi-bin/iipsrv.fcgi';

    this.render = options.render || 'spiral';

    this.images = new Array(options['image'].length);
    options.image || alert( 'Image location not set in class constructor options');
    if( typeOf(options.image) == 'array' ){
       for( i=0; i<options.image.length;i++ ){
	 this.images[i] = { src:options.image[i], sds:"0,90", cnt:1.0 };
       }
    }
    else this.images = [{ src:options.image, sds:"0,90", cnt:1.0} ];

    this.loadoptions = options.load || null;

    this.credit = options.credit || null;

    this.scale = options.scale || null;

    // Set the initial zoom resolution and viewport
    this.viewport = null;
    if( options.viewport ){
      this.viewport = {
	resolution: (typeof(options.viewport.resolution)=='undefined') ? null : parseInt(options.viewport.resolution),
	x: (typeof(options.viewport.x)=='undefined') ? null : parseFloat(options.viewport.x),
	y: (typeof(options.viewport.y)=='undefined') ? null : parseFloat(options.viewport.y)
      }
    }

    // Enable fullscreen mode?
    this.enableFullscreen = (options.enableFullscreen==false)? false : true;

    // Disable the right click context menu on image tiles?
    this.disableContextMenu = true;


    // Navigation window options
    this.showNavWindow = (options.showNavWindow == false) ? false : true;
    this.showNavButtons = (options.showNavButtons == false) ? false : true;
    this.navWinSize = options.navWinSize || 0.2;

    this.winResize = (options.winResize==false)? false : true;

    this.prefix = options.prefix || 'images/';

    // Set up our protocol handler
    switch( options.protocol ){
      case 'zoomify':
	this.protocol = new Zoomify();
	break;
      case 'deepzoom':
	this.protocol = new DeepZoom();
	break;
      case 'djatoka':
        this.protocol = new Djatoka();
	break;
      default:
	this.protocol = new IIP();
    }


    // Preload tiles surrounding view window?
    this.preload = false;
    this.effects = false;

    // Annotations
    this.annotations = options.annotations || null;
    this.annotationTip = null;
    this.annotationsVisible = true;


    // If we want to assign a function for a click within the image
    // - used for multispectral curve visualization, for example
    this.targetclick = options.targetclick || null;

    this.max_size = {};      // Dimensions of largest resolution
    this.navWin = {w:0,h:0}; // Dimensions of navigation window
    this.opacity = 0;
    this.wid = 0;             // Width of current resolution
    this.hei = 0;             // Height of current resolution
    this.resolutions;         // List of available resolutions
    this.num_resolutions = 0; // Number of available resolutions
    this.view = { x: 0,       // Location and dimensions of current visible view
		  y: 0,
		  w: this.wid,
		  h: this.hei,
		  res: 0      // Current resolution
                };

    this.navpos = {};         // Location of navigation drag zone
    this.tileSize = {};       // Tile size in pixels {w,h}


    // Number of tiles loaded
    this.tiles = new Array(); // List of tiles currently displayed
    this.nTilesLoaded = 0;
    this.nTilesToLoad = 0;
    this.locked = false;
    this.orientation = 0;
    this.targetsize;
    this.fullscreen = false;

    // CSS3: Need to prefix depending on browser. Cannot handle IE<9
    this.CSSprefix = '';
    if( Browser.firefox ) this.CSSprefix = '-moz-';
    else if( Browser.chrome || Browser.safari || Browser.Platform.ios ) this.CSSprefix = '-webkit-';
    else if( Browser.opera ) this.CSSprefix = '-o-';
    else if( Browser.ie ) this.CSSprefix = 'ms-';  // Note that there should be no leading "-" !!


    /* Load us up when the DOM is fully loaded! 
     */
    window.addEvent( 'domready', function(){ this.load(); }.bind(this) );
  },



  /* Create the appropriate CGI strings and change the image sources
   */
  requestImages: function() {

    // Re-orient our canvas to 0 degrees rotation
    if( this.orientation != 0 ){
      this.orientation = 0;
      this.canvas.setStyle(this.CSSprefix+'transform', 'rotate(0deg)');
    }

    // Set our cursor
    this.canvas.setStyle( 'cursor', 'wait' );

    // Delete our annotations
    if( this.annotationTip ) this.annotationTip.detach( this.canvas.getChildren('div.annotation') );
    this.canvas.getChildren('div.annotation').each(function(el){
	el.eliminate('tip:text');
	el.destroy();
    });

    // Load our image mosaic
    this.loadGrid();

    // Create new annotations
    this.createAnnotations();
    if( this.annotationTip ) this.annotationTip.attach( this.canvas.getChildren('div.annotation') );
  },



  /* Create a grid of tiles with the appropriate tile request and positioning
   */
  loadGrid: function(){

    if( this.locked ) return;
    this.locked = true;
    var border = this.preload ? 1 : 0

    // Get the start points for our tiles
    var startx = Math.floor( this.view.x / this.tileSize.w ) - border;
    var starty = Math.floor( this.view.y / this.tileSize.h ) - border;
    if( startx<0 ) startx = 0;
    if( starty<0 ) starty = 0;


    // If our size is smaller than the display window, only get these tiles!
    var len = this.view.w;
    if( this.wid < this.view.w ) len = this.wid;
    var endx =  Math.ceil( ((len + this.view.x)/this.tileSize.w) - 1 ) + border;


    len = this.view.h;
    if( this.hei < this.view.h ) len = this.hei;
    var endy = Math.ceil( ( (len + this.view.y)/this.tileSize.h) - 1 ) + border;


    // Number of tiles is dependent on view width and height
    var xtiles = Math.ceil( this.wid / this.tileSize.h );
    var ytiles = Math.ceil( this.hei / this.tileSize.h );

    if( endx >= xtiles ) endx = xtiles-1;
    if( endy >= ytiles ) endy = ytiles-1;


    /* Calculate the offset from the tile top left that we want to display.
       Also Center the image if our viewable image is smaller than the window
    */
    var xoffset = Math.floor(this.view.x % this.tileSize.w);
    if( this.wid < this.view.w ) xoffset -=  (this.view.w - this.wid)/2;

    var yoffset = Math.floor(this.view.y % this.tileSize.h);
    if( this.hei < this.view.h ) yoffset -= (this.view.h - this.hei)/2;

    var tile;
    var i, j, k, n;
    var left, top;
    k = 0;
    n = 0;

    var centerx = startx + Math.round((endx-startx)/2);
    var centery = starty + Math.round((endy-starty)/2);

    var map = new Array((endx-startx)*(endx-startx));
    var newTiles = new Array((endx-startx)*(endx-startx));
    newTiles.empty();

    // Should put this into
    var ntiles = 0;
    for( j=starty; j<=endy; j++ ){
      for (i=startx;i<=endx; i++) {

	map[ntiles] = {};
	if( this.render == 'spiral' ){
	  // Calculate the distance from the centre of the image
	  map[ntiles].n = Math.abs(centery-j)* Math.abs(centery-j) + Math.abs(centerx-i)*Math.abs(centerx-i);
	}
	// Otherwise do a random rendering
	else map[ntiles].n = Math.random();

	map[ntiles].x = i;
	map[ntiles].y = j;
	ntiles++;

	k = i + (j*xtiles);
	newTiles.push(k);

      }
    }

    this.nTilesLoaded = 0;
    this.nTilesToLoad = ntiles*this.images.length;

    // Delete the tiles from our old image mosaic which are not in our new list of tiles
    var _this = this;
    this.canvas.getChildren('img').each( function(el){
      var index = parseInt(el.retrieve('tile'));
      if( !newTiles.contains(index) ){
        el.destroy();
	_this.tiles.erase(index);
      }
    });

    map.sort(function s(a,b){return a.n - b.n;});

    for( var m=0; m<ntiles; m++ ){

      var i = map[m].x;
      var j = map[m].y;

      // Sequential index of the tile in the tif image
      k = i + (j*xtiles);

      if( this.tiles.contains(k) ){
	this.nTilesLoaded++;
        if( this.showNavWindow ) this.refreshLoadBar(); 
	if( this.nTilesLoaded >= this.nTilesToLoad ) this.canvas.setStyle( 'cursor', 'move' );
	continue;
      }

      // Iterate over the number of layers we have
      var n;
      for(n=0;n<this.images.length;n++){

        var tile = new Element('img', {
          'class': 'layer'+n,
          'styles': {
	    left: i*this.tileSize.w,
	    top: j*this.tileSize.h
          }
        });
	// Move this out of the main constructor to avoid DOM attribute bloat
	if( this.effects ) tile.setStyle('opacity',0.1);

	// Inject into our canvas
	tile.inject(this.canvas);

	// Get tile URL
	var src = this.protocol.getTileURL( this.server, this.images[n].src, this.view.res, this.images[n].sds, this.images[n].cnt, k, i, j );

	// Add our tile event functions after injection otherwise we get no event
	tile.addEvents({
	  'load': function(tiles){    
	     var tile = tiles[0];
	     var id = tiles[1];
	     if( this.effects ) tile.setStyle('opacity',1);
	     if(!(tile.width&&tile.height)){
	       tile.fireEvent('error');
	       return;
	     }
	     this.nTilesLoaded++;
	     if( this.showNavWindow ) this.refreshLoadBar();
	     if( this.nTilesLoaded >= this.nTilesToLoad ) this.canvas.setStyle( 'cursor', 'move' );
	     this.tiles.push(id); // Add to our list of loaded tiles
	  }.bind(this,[tile,k]),
	  'error': function(){
	      // Try to reload if we have an error.
	      // Add a suffix to prevent cacheing, but only reload once to avoid endless loops
	      if( parseInt(this.retrieve('error')) > 0 ) return;
	      var src = this.src;
	      this.set( 'src', src + '?'+ Date.now() );
	      this.store('error',"1");
	  }
	});

	// We must set the source at the end so that the 'load' function is properly fired
	tile.set( 'src', src );
	tile.store('tile',k);
      }

    }

    if( this.images.length > 1 ){
      var selector = 'img.layer'+(n-1);
      this.canvas.getChildren(selector).set( 'opacity', this.opacity );
    }

    this.locked = false;

  },


  /* Get a URL for the screenshot of the view area
   */
  getRegionURL: function(){
    var w = this.resolutions[this.view.res].w;
    var h = this.resolutions[this.view.res].h;
    var url = this.server + this.protocol.getRegionURL(this.images[0].src,this.view.x/w,this.view.y/h,this.view.w/w,this.view.h/h);
    return url;
  },


  /* Handle various keyboard events such as allowing us to navigate within the image via the arrow keys etc.
   */
  key: function(e){

    var event = new Event(e);
    event.preventDefault();

    var d = Math.round(this.view.w/4);

    switch( e.code ){
    case 37: // left
      this.nudge(-d,0);
      if( IIPMooViewer.sync ){
	IIPMooViewer.windows(this).each( function(el){ el.nudge(-d,0); });
      }
      break;
    case 38: // up
      this.nudge(0,-d);
      if( IIPMooViewer.sync ){
	IIPMooViewer.windows(this).each( function(el){ el.nudge(0,-d); });
      }
      break;
    case 39: // right
      this.nudge(d,0);
      if( IIPMooViewer.sync ){
	IIPMooViewer.windows(this).each( function(el){ el.nudge(d,0); });
      }
      break;
    case 40: // down
      this.nudge(0,d);
      if( IIPMooViewer.sync ){
	IIPMooViewer.windows(this).each( function(el){ el.nudge(0,d); });
      }
      break;
    case 107: // plus
      if(!e.control) this.zoomIn();
      if( IIPMooViewer.sync ){
	IIPMooViewer.windows(this).each( function(el){ el.zoomIn(); });
      }
      break;
    case 109: // minus
      if(!e.control) this.zoomOut();
      if( IIPMooViewer.sync ){
	IIPMooViewer.windows(this).each( function(el){ el.zoomOut(); });
      }
      break;
    case 189: // minus
      if(!e.control) this.zoomOut();
      break;
    case 72: // h
      // For removing the navigation window if it exists - must use the get('reveal')
      // otherwise we do not have the Mootools extended object
      if( document.id(this.source).getElement('div.navcontainer') ){
	document.id(this.source).getElement('div.navcontainer').get('reveal').toggle();
      }
      break;
    case 82: // r
      if(e.shift) this.orientation -= 45 % 360;
      else this.orientation += 45 % 360;

      this.rotate( this.orientation );
      if( IIPMooViewer.sync ){
	var r = this.orientation;
	IIPMooViewer.windows(this).each( function(el){ el.rotate(r); });
      }

      break;
    case 65: // a
      if( this.annotations ) this.toggleAnnotations();
      break;
    case 27: // esc
      if( this.fullscreen ) if(!IIPMooViewer.sync) this.toggleFullScreen();
      document.id(this.source).getElement('div.info').fade(0);
      break;
    case 70: // f fullscreen, but if we have multiple views
      if(!IIPMooViewer.sync) this.toggleFullScreen();
      break;
    default:
      break;
    }

  },


  /* Rotate our view
   */
  rotate: function( r ){
    // Rotation works in Firefox 3.5+, Chrome, Safari and IE9
    if( Browser.ie && Browser.version<9 ) return;

    var pos = this.canvas.getPosition();

    // Set our origin - calculate differently if canvas is smaller than view port
    var origin_x = ( this.wid>this.view.w ? Math.round(this.view.x+this.view.w/2) : Math.round(this.wid/2) ) + "px";
    var origin_y = ( this.hei>this.view.h ? Math.round(this.view.y+this.view.h/2) : Math.round(this.hei/2) ) + "px";
    var origin = origin_x + " " + origin_y;

    var angle = 'rotate(' + r + 'deg)';

    this.canvas.setStyle( this.CSSprefix+'transform-origin', origin );
    this.canvas.setStyle( this.CSSprefix+'transform', angle );
  },


  /* Toggle fullscreen
   */
  toggleFullScreen: function(){
    var l,t,w,h;

    if(!this.enableFullscreen) return;

    if( !this.fullscreen ){
      this.targetsize = {
	pos: document.id(this.source).getPosition(),
	size: document.id(this.source).getSize()
      };
      l = 0;
      t = 0;
      w = '100%';
      h = '100%';
    }
    else{
      l = this.targetsize.pos.x;
      t = this.targetsize.pos.y;
      w = this.targetsize.size.x;
      h = this.targetsize.size.y;
    }
    document.id(this.source).setStyles({
      left: l,
      top: t,
      width: w,
      height: h
    });
    this.fullscreen = !this.fullscreen;

    // Create a fullscreen message, then delete after a timeout
    if( this.fullscreen ) this.showPopUp( 'Press Esc to exit fullscreen mode' );

    this.reload();

  },



  /* Show a message, then delete after a timeout
   */
  showPopUp: function( text ) {
    var fs = new Element('div',{
      'class': 'message',
      'html': text
    }).inject( document.id(this.source) );
    var del;
    if(Browser.ie&&Browser.version<9) del = function(){ fs.destroy(); };
    else del = function(){ fs.fade('out').get('tween').chain( function(){ fs.destroy(); } ); };
    del.delay(3000);
  },



  /* Scroll resulting from a drag of the navigation window
   */
  scrollNavigation: function( e ) {

    var xmove = 0;
    var ymove = 0;

    var zone_size = this.zone.getSize();
    var zone_w = zone_size.x;
    var zone_h = zone_size.y;

    // From a mouse click
    if( e.event ){
      e.stop();
      var pos = this.zone.getParent().getPosition();
      xmove = e.event.clientX - pos.x - zone_w/2;
      ymove = e.event.clientY - pos.y - zone_h/2;
    }
    else{
      // From a drag
      xmove = e.offsetLeft;
      ymove = e.offsetTop-10;
      if( (Math.abs(xmove-this.navpos.x) < 3) && (Math.abs(ymove-this.navpos.y) < 3) ) return;
    }

    if( xmove > (this.navWin.w - zone_w) ) xmove = this.navWin.w - zone_w;
    if( ymove > (this.navWin.h - zone_h) ) ymove = this.navWin.h - zone_h;
    if( xmove < 0 ) xmove = 0;
    if( ymove < 0 ) ymove = 0;

    xmove = Math.round(xmove * this.wid / this.navWin.w);
    ymove = Math.round(ymove * this.hei / this.navWin.h);

    // Only morph transition if we have moved a short distance
    var morphable = Math.abs(xmove-this.view.x)<this.view.w/2 && Math.abs(ymove-this.view.y)<this.view.h/2;
    if( morphable ){
      this.canvas.morph({
	left: (this.wid>this.view.w)? -xmove : Math.round((this.view.w-this.wid)/2),
	top: (this.hei>this.view.h)? -ymove : Math.round((this.view.h-this.hei)/2)
      });
    }
    else{
      this.canvas.setStyles({
	left: (this.wid>this.view.w)? -xmove : Math.round((this.view.w-this.wid)/2),
	top: (this.hei>this.view.h)? -ymove : Math.round((this.view.h-this.hei)/2)
      });
    }

    // Re-orient our canvas to 0 degrees rotation
    this.orientation = 0;
    this.canvas.setStyle(this.CSSprefix+'transform', 'rotate(0deg)');
    this.zone.setStyle(this.CSSprefix+'transform', 'rotate(0deg)');

    this.view.x = xmove;
    this.view.y = ymove;
 
    // The morph event automatically calls requestImages
    if( !morphable ){
      this.requestImages();
    }

    // Position the zone after a click, but not for zone drags
    if( e.event ) this.positionZone();

    if(IIPMooViewer.sync){
      IIPMooViewer.windows(this).each( function(el){ el.moveTo(xmove,ymove); });
    }
  },



  /* Scroll from a drag event on the tile canvas
   */
  scroll: function(e) {

    var pos = this.canvas.getPosition(this.source);
    pos.x = this.canvas.getStyle('left').toInt();
    pos.y = this.canvas.getStyle('top').toInt();
    //    pos.y = pos.y + Math.sin( this.orientation*Math.PI*2 / 360 ) * this.view.w / 2;
    //    pos.x = pos.x + (this.view.w/2) - Math.cos( this.orientation*Math.PI*2 / 360 ) * this.view.w / 2;
    var xmove =  -pos.x;
    var ymove =  -pos.y;
    this.moveTo( xmove, ymove );

    if( IIPMooViewer.sync ){
      IIPMooViewer.windows(this).each( function(el){ el.moveTo(xmove,ymove); });
    }
  },



  /* Check our scroll bounds. 
   */
  checkBounds: function( x, y ) {

    if( x > this.wid-this.view.w ) x = this.wid - this.view.w;
    if( y > this.hei-this.view.h ) y = this.hei - this.view.h;

    if( x < 0 || this.wid < this.view.w ) x = 0;
    if( y < 0 || this.hei < this.view.h ) y = 0;

    this.view.x = x;
    this.view.y = y;
  },



  /* Move to a particular position on the image
   */
  moveTo: function( x, y ){

    // To avoid unnecessary redrawing ...
    if( x==this.view.x && y==this.view.y ) return;

    this.checkBounds(x,y);
    this.canvas.setStyles({
      left: (this.wid>this.view.w)? -this.view.x : Math.round((this.view.w-this.wid)/2),
      top: (this.hei>this.view.h)? -this.view.y : Math.round((this.view.h-this.hei)/2)
    });

    this.requestImages();
    this.positionZone();
  },



  /* Nudge the view by a small amount
   */
  nudge: function( dx, dy ){

    this.checkBounds(this.view.x+dx,this.view.y+dy);

    // Check whether image size is less than viewport
    this.canvas.morph({
      left: (this.wid>this.view.w)? -this.view.x : Math.round((this.view.w-this.wid)/2),
      top: (this.hei>this.view.h)? -this.view.y : Math.round((this.view.h-this.hei)/2)
    });

    this.positionZone();
  },



  /* Generic zoom function for mouse wheel or click events
   */
  zoom: function( e ) {

    var event = new Event(e);

    // Stop all mousewheel events in order to prevent stray scrolling events
    event.stop();

    // Set z to +1 if zooming in and -1 if zooming out
    var z = 1;

    // For mouse scrolls
    if( event.wheel && event.wheel < 0 ) z = -1;
    // For double clicks
    else if( event.shift ) z = -1;
    else z = 1;


    var ct = event.target;
    if( ct ){
      var cc = ct.get('class');
      var pos, xmove, ymove;

      if( cc != "zone" & cc != 'navimage' ){
	pos = this.canvas.getPosition();

	// Center our zooming on the mouse position when over the main target window
	// - use clientX/Y because pageX/Y does not exist in IE
	this.view.x = event.event.clientX - pos.x - (this.view.w/2);
	this.view.y = event.event.clientY - pos.y - (this.view.h/2);
      }
      else{
	// For zooms with the mouse over the navigation window
	pos = this.zone.getParent().getPosition();
	var n_size = this.zone.getParent().getSize();
	var z_size = this.zone.getSize();
	this.view.x = (event.event.clientX - pos.x - z_size.x/2) * this.wid/n_size.x;
	this.view.y = (event.event.clientY - pos.y - z_size.y/2) * this.hei/n_size.y;
      }

      if( IIPMooViewer.sync ){
	var _x = this.view.x;
	var _y = this.view.y;
	IIPMooViewer.windows(this).each( function(el){
	  el.view.x = _x;
	  el.view.y = _y;
        });
      }
    }

    // Now do our actual zoom
    if( z == -1 ) this.zoomOut();
    else this.zoomIn();

    if( IIPMooViewer.sync ){
      IIPMooViewer.windows(this).each( function(el){
	if( z==-1) el.zoomOut();
	else el.zoomIn();
      });
    }

  },



  /* Zoom in by a factor of 2
   */
  zoomIn: function(){

    if( this.view.res < this.num_resolutions-1 ){

      this.view.res++;

      // Get the image size for this resolution
      this.wid = this.resolutions[this.view.res].w;
      this.hei = this.resolutions[this.view.res].h;

      var xoffset = (this.resolutions[this.view.res-1].w > this.view.w) ? this.view.w : this.resolutions[this.view.res-1].w;
      this.view.x = Math.round( 2*(this.view.x + xoffset/4) );
      if( this.view.x > (this.wid-this.view.w) ) this.view.x = this.wid - this.view.w;
      if( this.view.x < 0 ) this.view.x = 0;

      this.view.y = Math.round( 2*(this.view.y + this.view.h/4) );
      if( this.view.y > (this.hei-this.view.h) ) this.view.y = this.hei - this.view.h;
      if( this.view.y < 0 ) this.view.y = 0;

      this.canvas.setStyles({
	 left: (this.wid>this.view.w)? -this.view.x : Math.round((this.view.w-this.wid)/2),
	 top: (this.hei>this.view.h)? -this.view.y : Math.round((this.view.h-this.hei)/2),
	 width: this.wid,
	 height: this.hei
      });

      // Center or contstrain our canvas to our containing div
      if( this.wid < this.view.w || this.hei < this.view.h ) this.reCenter();
      else this.touch.options.limit = { x: Array(this.view.w-this.wid,0), y: Array(this.view.h-this.hei,0) };

      this.canvas.getChildren('img').destroy();
      this.tiles.empty();

      this.requestImages();
      this.positionZone();
      if( this.scale ) this.updateScale();
    }
  },



  /* Zoom out by a factor of 2
   */
  zoomOut: function(){

    if( this.view.res > 0 ){

      this.view.res--;

      // Get the image size for this resolution
      this.wid = this.resolutions[this.view.res].w;
      this.hei = this.resolutions[this.view.res].h;

      this.view.x = this.view.x/2 - (this.view.w/4);
      if( this.view.x + this.view.w > this.wid ) this.view.x = this.wid - this.view.w;

      this.view.y = this.view.y/2 - (this.view.h/4);
      if( this.view.y + this.view.h > this.hei ) this.view.y = this.hei - this.view.h;

      var xoffset = (this.wid > this.view.w ) ? this.view.x : (this.wid-this.view.w)/2;
      var yoffset = (this.hei > this.view.h ) ? this.view.y : (this.hei-this.view.h)/2;

      if( this.view.x < 0 ) this.view.x = 0;
      if( this.view.y < 0 ) this.view.y = 0;

      // Make sure we don't have -ve offsets when zooming out
      if( xoffset < 0 ) xoffset = 0;
      if( yoffset < 0 ) yoffset = 0;

      this.canvas.setStyles({
	 left: -xoffset,
	 top: -yoffset,
	 width: this.wid,
	 height: this.hei
      });

      if( this.wid < this.view.w || this.hei < this.view.h ) this.reCenter();
      else this.touch.options.limit = { x: Array(this.view.w-this.wid,0), y: Array(this.view.h-this.hei,0) };

      // Delete our image tiles
      this.canvas.getChildren('img').destroy();
      this.tiles.empty();

      this.requestImages();
      this.positionZone();
      if( this.scale ) this.updateScale();
    }
  },


  /* Calculate some dimensions
   */
  calculateSizes: function(){

    var tx = this.max_size.w;
    var ty = this.max_size.h;
    var thumb_width;

    // Set up our default sizes 
    var target_size = document.id(this.source).getSize();
    this.view.w = target_size.x;
    this.view.h = target_size.y;
    thumb_width = this.view.w * this.navWinSize;

    // For panoramic images, use a large navigation window
    if( tx > 2*ty ) thumb_width = this.view.w / 2;

    //if( (ty/tx)*thumb_width > this.view.h*0.5 ) thumb_width = Math.round( this.view.h * 0.5 * tx/ty );

    this.navWin.w = thumb_width;
    this.navWin.h = Math.round( (ty/tx)*thumb_width );

    // Determine the image size for this image view
    this.view.res = this.num_resolutions;
    tx = this.max_size.w;
    ty = this.max_size.h;

    // Calculate our list of resolution sizes and the best resolution
    // for our window size
    this.resolutions = new Array(this.num_resolutions);
    this.resolutions.push({w:tx,h:ty});
    this.view.res = 0;
    for( var i=1; i<this.num_resolutions; i++ ){
      tx = Math.floor(tx/2);
      ty = Math.floor(ty/2);
      this.resolutions.push({w:tx,h:ty});
      if( tx < this.view.w && ty < this.view.h ) this.view.res++;
    }
    this.view.res -= 1;

    // Sanity check and watch our for small screen displays causing the res to be negative
    if( this.view.res < 0 ) this.view.res = 0;
    if( this.view.res >= this.num_resolutions ) this.view.res = this.num_resolutions-1;

    // We reverse so that the smallest resolution is at index 0
    this.resolutions.reverse();
    this.wid = this.resolutions[this.view.res].w;
    this.hei = this.resolutions[this.view.res].h;

  },


  /* Set the message in the credit div
   */
  setCredit: function(message){
    document.id(this.source).getElement('div.credit').set( 'html', message );
  },


  /* Create our main and navigation windows
   */
  createWindows: function(){

    // Setup our class. Get it's current position as we will convert it to absolute positioning
    var container = document.id(this.source);
    var pos = container.getPosition();

    // Disable fullscreen mode if we are already at 100% size
    if( container.getStyle('width') == '100%' && container.getStyle('height') == '100%' ){
      this.enableFullscreen = false;
    }

    var size = container.getSize();
    container.addClass( 'iipmooviewer' );
    container.setStyle( 'position', 'absolute' );

    // Our modal information box
    new Element( 'div', {
	'class': 'info',
	'styles': { opacity: 0 },
	'events': {
	   click: function(){ this.fade(0); }
	},
	'html': '<div><div><h2><a href="http://iipimage.sourceforge.net"><img src="'+this.prefix+'iip.32x32.png"/></a>IIPMooViewer</h2>IIPImage HTML5 Ajax High Resolution Image Viewer - Version '+this.version+'<br/><ul><li>To navigate within image: drag image within main window or drag zone within the navigation window or click an area within navigation window</li><li>To zoom in: double click with the mouse or use the mouse scroll wheel or simply press the "+" key</li><li>To zoom out: shift double click with the mouse or use the mouse wheel or press the "-" key</li><li>To rotate image clockwise: press the "r" key, anti-clockwise: press shift and "r"</li><li>To resize to full screen: press the "f" key<li>To toggle any annotations: press the "a" key</li><li>To show/hide navigation window: press "h" key</li></ul><br/>For more information visit <a href="http://iipimage.sourceforge.net">http://iipimage.sourceforge.net</a></div></div>'
    }).inject( container );


    // Use a lexical closure rather than binding to pass this to anonymous functions
    var _this = this;

    // Create our main window target div, add our events and inject inside the frame
    this.canvas = new Element('div', {
       'class': 'canvas',
	'morph': {
	    transition: Fx.Transitions.Quad.easeInOut,
	    onComplete: function(){
	       _this.requestImages();
	    }
	}
    });


    // Create our main view drag object for our canvas
    this.touch = new Drag( this.canvas, {
      onComplete: this.scroll.bind(this)
    });


    // Inject our canvas into the container, but events need to be added after injection
    this.canvas.inject( container );
    this.canvas.addEvents({
	'mousewheel:throttle(75)': this.zoom.bind(this),
        'dblclick': this.zoom.bind(this),
	'mousedown': function(e){ var event = new Event(e); event.stop(); }
    });

    // Display / hide our annotations if we have any
    if( this.annotations ){
      this.canvas.addEvent( 'mouseenter', function(){
        if( _this.annotationsVisible ){
	  if( Browser.ie&&Browser.version<9 ){
	    _this.canvas.getElements('div.annotation').setStyle('visibility','visible');
	  }
	  else _this.canvas.getElements('div.annotation').tween( 'opacity', [0,1] );
	}
      });
      this.canvas.addEvent( 'mouseleave', function(){
	if( _this.annotationsVisible ){
	  if( Browser.ie&&Browser.version<9 ){
	    _this.canvas.getElements('div.annotation').setStyle('visibility','hidden');
	  }
	  else _this.canvas.getElements('div.annotation').tween('opacity',0);
	}
      });
    }

    // Disable the right click context menu if requested and show our info window instead
    if( this.disableContextMenu ){
      container.addEvent( 'contextmenu', function(e){
					   var event = new Event(e);
					   event.stop();
					   container.getElement('div.info').fade(0.95);
					   return false;
					 } )
    }


    // Add an external callback if we have been given one
    if( this.targetclick ) this.canvas.addEvent( 'click', this.targetclick.bind(this) );

    // Add our keyboard events, but only when we are over the enclosing div
    // In order to add keyboard events to the div, we need to give it a tabindex and focus it
    container.set( 'tabindex', 0 );
    container.focus();

    // Focus and defocus when we move into and out of the div,
    // get key presses and prevent default scrolling via mousewheel
    container.addEvents({
      'keydown': this.key.bind(this),
      'mouseover': function(){ container.focus(); },
      'mouseout': function(){ container.blur(); },
      'mousewheel': function(e){ e.preventDefault(); }
    });

    // Add gesture support for mobile iOS and android
    if( Browser.Platform.ios || Browser.Platform.android ){

      // Prevent dragging on the container div
      container.addEvent('touchmove', function(e){ e.preventDefault(); } );

      // Disable elastic scrolling and handle changes in orientation on mobile devices.
      // These events need to be added to the document body itself
      document.body.addEvents({
	'touchmove': function(e){ e.preventDefault(); },
	'orientationchange': function(){
	   document.id(this.source).setStyles({
	     'width': '100%',
	     'height': '100%'
	    });
	    // Need to set a timeout the div is not resized immediately on some versions of iOS
	    this.reload.delay(500,this);
	  }.bind(this)
      });

      // Now add our touch canvas events
      this.canvas.addEvents({
        'touchstart': function(e){
	  e.preventDefault();
	  // Only handle single finger events
          if(e.touches.length == 1){
	    // Simulate a double click with a timer
	    var t1 = _this.canvas.retrieve('taptime') || 0;
	    var t2 = Date.now();
	    _this.canvas.store( 'taptime', t2 );
	    _this.canvas.store( 'tapstart', 1 );
	    if( t2-t1 < 500 ){
	      _this.canvas.eliminate('taptime');
	      _this.zoomIn();
	    }
	    else{
	      var pos = _this.canvas.getPosition();
	      _this.touchstart = { x: e.touches[0].clientX - pos.x, y: e.touches[0].clientY - pos.y };
	    }
	  }
        },
	'touchmove': function(e){
	  // Only handle single finger events
	  if(e.touches.length == 1){
	    _this.view.x = _this.touchstart.x - e.touches[0].clientX;
	    _this.view.y = _this.touchstart.y - e.touches[0].clientY;
	    // Limit the scroll
	    if( _this.view.x > _this.wid-_this.view.w ) _this.view.x = _this.wid-_this.view.w;
	    if( _this.view.y > _this.hei-_this.view.h ) _this.view.y = _this.hei-_this.view.h;
	    if( _this.view.x < 0 ) _this.view.x = 0;
	    if( _this.view.y < 0 ) _this.view.y = 0;
	    _this.canvas.setStyles({
	      left: (_this.wid>_this.view.w) ? -_this.view.x : Math.round((_this.view.w-_this.wid)/2),
	      top: (_this.hei>_this.view.h) ? -_this.view.y : Math.round((_this.view.h-_this.hei)/2)
	    });
	  }
        },
	'touchend': function(e){
	  // Update our tiles and navigation window
	  if( _this.canvas.retrieve('tapstart') == 1 ){
	    _this.canvas.eliminate('tapstart');
	    _this.requestImages();
	    _this.positionZone();
	  }
        },
	'gesturestart': function(e){
	  e.preventDefault();
	  _this.canvas.store('tapstart', 1);
	},
	'gesturechange': function(e){
	  e.preventDefault();
	},
	'gestureend': function(e){
	  if( _this.canvas.retrieve('tapstart') == 1 ){
	    _this.canvas.eliminate('tapstart');
	    // Handle scale
	    if( Math.abs(1-e.scale)>0.1 ){
	      if( e.scale > 1 ) _this.zoomIn();
	      else _this.zoomOut();
	    }
	    // And rotation
	    else if( Math.abs(e.rotation) > 10 ){
	      if( e.rotation > 0 ) _this.orientation += 45 % 360;
	      else _this.orientation -= 45 % 360;
	      _this.rotate(_this.orientation);
	    }
	  }
	}
      });
    }

    // Add our logo and a tooltip explaining how to use the viewer
    var info = new Element( 'img', {
      'src': this.prefix+'iip.32x32.png',
      'class': 'logo',
      'title': 'click for help',
      'events': {
	click: function(){ container.getElement('div.info').fade(0.95); },
	// Opacity changes to non-rectangular PNGs in IE don't work
	mouseover: function(){ if(!(Browser.ie&&Browser.version<9)) this.fade(1); },
	mouseout: function(){ if(!(Browser.ie&&Browser.version<9)) this.fade(0.65); },
	// Prevent user from dragging image
	mousedown: function(e){ var event = new Event(e); event.stop(); }
      }
    }).inject(container);

    // For standalone iphone/ipad the logo gets covered by the status bar
    if( Browser.Platform.ios && window.navigator.standalone ) info.setStyle( 'top', 15 );

    // Add some information or credit
    if( this.credit ){
      new Element( 'div', {
	'class': 'credit',
	'html': this.credit,
	'styles': { opacity: 0.65 },
	'events': {
	  // We specify the start value to stop a strange problem where on the first
	  // mouseover we get a sudden transition to opacity 1.0
	  mouseover: function(){ this.fade([0.6,0.9]); },
	  mouseout: function(){ this.fade(0.6); }
	}
      }).inject(container);
    }


    // Add a scale if requested. Make it draggable and add a tween transition on rescaling
    if( this.scale ){
      var scale = new Element( 'div', {
	 'class': 'scale',
	 'title': 'draggable scale',
	 'html': '<div class="ruler"></div><div class="label"></div>'
      }).inject(container);
      scale.makeDraggable({container: container});
      scale.getElement('div.ruler').set('tween', {
         transition: Fx.Transitions.Quad.easeInOut
      });
    }


    // Calculate some sizes and create the navigation window
    this.calculateSizes();    
    this.createNavigationWindow();
    this.createAnnotations();


    if( !(Browser.Platform.ios||Browser.Platform.android) ){
      var tip_list = 'img.logo, div.toolbar, div.scale';
      if( Browser.ie8||Browser.ie7 ) tip_list = 'img.logo, div.toolbar'; // IE8 bug which triggers window resize
      new Tips( tip_list, {
	className: 'tip', // We need this to force the tip in front of nav window
	  onShow: function(tip,el){
	    tip.setStyles({
	       visibility: 'hidden',
	       display: 'block'
	    }).fade([0,0.9]);
	  },
	  onHide: function(tip, el){
	    tip.fade('out').get('tween').chain( function(){ tip.setStyle('display', 'none'); } );
	  }
	});
    }

    // Set our initial viewport resolution if this has been set
    if( this.viewport && this.viewport.resolution!=null ){
      this.view.res = this.viewport.resolution;
      this.wid = this.resolutions[this.view.res].w;
      this.hei = this.resolutions[this.view.res].h;
      this.touch.options.limit = { x: Array(this.view.w-this.wid,0), y: Array(this.view.h-this.hei,0) };
    }

    // Center our view or move to initial viewport position
    if( this.viewport && this.viewport.x!=null && this.viewport.y!=null ){
      this.moveTo( this.viewport.x*this.wid, this.viewport.y*this.hei );
    }
    else this.reCenter();
 

    // Set the size of the canvas to that of the full image at the current resolution
    this.canvas.setStyles({
      width: this.wid,
      height: this.hei
    });


    // Load our images
    this.requestImages();
    this.positionZone();
    if( this.scale ) this.updateScale();

    // Add our key press and window resize events. Do this at the end to avoid reloading before
    // we are fully set up
    if(this.winResize) window.addEvent( 'resize', this.reload.bind(this) );

    window.fireEvent('iiploaded');

  },



  /* Create our navigation window
   */
  createNavigationWindow: function() {

    // If the user does not want a navigation window, do not create one!
    if( (!this.showNavWindow) && (!this.showNavButtons) ) return;

    var navcontainer = new Element( 'div', {
      'class': 'navcontainer',
      'styles': {
	position: 'absolute',
	width: this.navWin.w
      }
    });


    var toolbar = new Element( 'div', {
      'class': 'toolbar',
      'events': {
	 dblclick: function(source){
	   document.id(source).getElement('div.navbuttons').get('slide').toggle();
         }.pass(this.source)
      }
    });
    toolbar.store( 'tip:text', '* Drag to move<br/>* Double Click to show/hide buttons<br/>* Press h to hide' );
    toolbar.inject(navcontainer);


    // Create our navigation div and inject it inside our frame if requested
    if( this.showNavWindow ){

      var navwin = new Element( 'div', {
	'class': 'navwin',
	'styles': {
	  height: this.navWin.h
	}
      });
      navwin.inject( navcontainer );


      // Create our navigation image and inject inside the div we just created
      var navimage = new Element( 'img', {
	'class': 'navimage',
	'src': this.server + '?FIF=' + this.images[0].src + '&SDS=' + this.images[0].sds +
               '&WID=' + this.navWin.w + '&QLT=99&CVT=jpeg',
        'events': {
          'click': this.scrollNavigation.bind(this),
          'mousewheel:throttle(75)': this.zoom.bind(this),
          // Prevent user from dragging navigation image
          'mousedown': function(e){ var event = new Event(e); event.stop(); }
        }
      });
      navimage.inject(navwin);


      // Create our navigation zone and inject inside the navigation div
      this.zone = new Element( 'div', {
        'class': 'zone',
        'morph': {
	  duration: 500,
	  transition: Fx.Transitions.Quad.easeInOut
        },
	'events': {
 	  'mousewheel:throttle(75)': this.zoom.bind(this),
 	  'dblclick': this.zoom.bind(this)
	}
      });
      this.zone.inject(navwin);

    }


    // Create our nav buttons if requested
    if( this.showNavButtons ){

      var navbuttons = new Element('div', {
	  'class': 'navbuttons'
      });

      // Create our buttons as SVG with fallback to PNG
      var prefix = this.prefix;
      ['reset','zoomIn','zoomOut'].each( function(k){
	new Element('img',{
	  'src': prefix + k + '.svg',
	  'class': k,
 	  'events':{
	    'error': function(){
	      this.removeEvents('error'); // Prevent infinite reloading
	      this.src = this.src.replace('.svg','.png');
	    }
	  }
	}).inject(navbuttons);
      });

      navbuttons.inject(navcontainer);

      // Need to set this after injection
      navbuttons.set('slide', {duration: 300, transition: Fx.Transitions.Quad.easeInOut, mode:'vertical'});

      // Add events to our buttons
      navbuttons.getElement('img.zoomIn').addEvent( 'click', function(){
	IIPMooViewer.windows(this).each( function(el){ el.zoomIn(); });
	this.zoomIn();
      }.bind(this) );

      navbuttons.getElement('img.zoomOut').addEvent( 'click', function(){
	IIPMooViewer.windows(this).each( function(el){ el.zoomOut(); });
	this.zoomOut();
      }.bind(this) );

      navbuttons.getElement('img.reset').addEvent( 'click', function(){
	IIPMooViewer.windows(this).each( function(el){ el.reload(); });
	this.reload();
      }.bind(this) );

    }

    // Add a progress bar only if we have the navigation window visible
    if( this.showNavWindow ){

      // Create our progress bar
      var loadBarContainer = new Element('div', {
	'class': 'loadBarContainer',
        'html': '<div class="loadBar"></div>',
        'styles': {
           width: this.navWin.w - 2
         },
         'tween': {
           duration: 1000,
           transition: Fx.Transitions.Sine.easeOut,
	   link: 'cancel'
         }
      });
      loadBarContainer.inject(navcontainer);
    }


    // Inject our navigation container into our holding div
    navcontainer.inject(this.source);


    if( this.showNavWindow ){
      this.zone.makeDraggable({
	container: document.id(this.source).getElement('div.navcontainer div.navwin'),
          // Take a note of the starting coords of our drag zone
          onStart: function() {
	    var pos = this.zone.getPosition();
	    this.navpos = {x: pos.x, y: pos.y-10};
	  }.bind(this),
	onComplete: this.scrollNavigation.bind(this)
        });
    }

    navcontainer.makeDraggable( {container:this.source, handle:toolbar} );

  },


  // Create annotations if they are contained within our current view
  createAnnotations: function() {

    // Sort our annotations by size to make sure it's always possible to interact
    // with annotations within annotations
    if( !this.annotations ) return;
    this.annotations.sort( function(a,b){ return (b.w*b.h)-(a.w*a.h); } );

    for( var i=0; i<this.annotations.length; i++ ){

      // Check whether this annotation is within our view
      if( this.wid*(this.annotations[i].x+this.annotations[i].w) > this.view.x &&
	  this.wid*this.annotations[i].x < this.view.x+this.view.w &&
	  this.hei*(this.annotations[i].y+this.annotations[i].h) > this.view.y &&
	  this.hei*this.annotations[i].y < this.view.y+this.view.h
	  // Also don't show annotations that entirely fill the screen
	  //	  (this.hei*this.annotations[i].x < this.view.x && this.hei*this.annotations[i].y < this.view.y &&
	  //	   this.wid*(this.annotations[i].x+this.annotations[i].w) > this.view.x+this.view.w && 
      ){

	var annotation = new Element('div', {
          'class': 'annotation',
          'styles': {
            left: Math.round(this.wid * this.annotations[i].x),
            top: Math.round(this.hei * this.annotations[i].y ),
	    width: this.wid * this.annotations[i].w,
	    height: this.hei * this.annotations[i].h
	  }
        }).inject( this.canvas );

	if( this.annotationsVisible==false ){
	  if( Browser.ie&&Browser.version<9 ) annotation.setStyle('visibility','hidden');
	  else  annotation.setStyle('opacity',0);
	}

	// On IE, the mouseleave event is triggered on traversal of the border, so add
	// a transparent background so that it does not trigger inside the div itself
	if(Browser.ie) annotation.setStyle( 'background-image', 'url('+this.prefix+'blank.gif)' );

	var text = this.annotations[i].text;
	if( this.annotations[i].title ) text = '<h1>'+this.annotations[i].title+'</h1>' + text;
        annotation.store( 'tip:text', text );
      }
    }


    if( !this.annotationTip ){
      var _this = this;
      this.annotationTip = new Tips( 'div.annotation', {
        className: 'tip', // We need this to force the tip in front of nav window
	fixed: true,
	offset: {x:30,y:30},
	hideDelay: 300,
	link: 'chain',
        onShow: function(t,el){
	  if(Browser.ie)this.tip.setStyle('visibility','visible');
	  else this.tip.fade(0.9);
	  // Prevent the tip from fading when we are hovering on the tip itself and not
	  // just when we leave the annotated zone
	  this.tip.addEvents({
	    'mouseleave':  function(){
	      this.active = false;
	      if(Browser.ie) this.setStyle('visibility','hidden');
	      else this.fade(0);
	    },
	    'mouseenter': function(){ this.active = true; }
	  })
        },
        onHide: function(t, el){
	  if( !this.tip.active ){
	    if(Browser.ie) this.tip.setStyle('visibility','hidden');
	    else this.tip.fade(0);
	    this.tip.removeEvents(['mouseenter','mouseleave']);
	  }
        }
      });
    }

  },



  /* Toggle visibility of any annotations
   */
  toggleAnnotations: function() {
    var el;
    if( el = this.canvas.getElements('div.annotation') ){
      if( this.annotationsVisible ){
	if(Browser.ie&&Browser.version<9) el.setStyle('visibility','hidden');
	else el.tween('opacity',[1,0]);
	this.annotationsVisible = false;
	this.showPopUp( 'Annotations disabled<br/>Press "a" to re-enable' );
      }
      else{
	if(Browser.ie&&Browser.version<9) el.setStyle('visibility','visible');
	else el.tween('opacity',[0,1]);
	this.annotationsVisible = true;
      }
    }
  },



  /* Update the tile download progress bar
   */
  refreshLoadBar: function() {

    // Update the loaded tiles number, grow the loadbar size
    var w = (this.nTilesLoaded / this.nTilesToLoad) * this.navWin.w;

    var loadBarContainer = document.id(this.source).getElement('div.navcontainer div.loadBarContainer');
    var loadBar = loadBarContainer.getElement('div.loadBar');
    loadBar.setStyle( 'width', w );

    // Display the % in the progress bar
    loadBar.set( 'html', 'loading&nbsp;:&nbsp;'+Math.round(this.nTilesLoaded/this.nTilesToLoad*100) + '%' );

    if( loadBarContainer.style.opacity != 0.85 ){
      loadBarContainer.setStyle( 'opacity', 0.85 );
    }

    // If we're done with loading, fade out the load bar
    if( this.nTilesLoaded >= this.nTilesToLoad ){
      // Fade out our progress bar and loading animation in a chain
      loadBarContainer.fade('out');
    }

  },



  /* Update the scale on our image - change the units if necessary
   */
  updateScale: function() {

    // Allow a range of units and multiples
    var dims =   ["p", "n", "&#181;", "m", "c", "", "k"];
    var orders = [ 1e-12, 1e-9, 1e-6, 0.001, 0.01, 1, 1000 ];
    var mults = [1,2,5,10,50];

    // Determine the number of pixels a unit takes at this scale. x1000 because we want per m
    var pixels = 1000 * this.scale * this.wid / this.max_size.w;

    // Loop through until we get a good fit scale. Be careful to break fully from the outer loop
    var i, j;
    outer: for( i=0;i<orders.length;i++ ){
      for( j=0; j<mults.length; j++ ){
	if( orders[i]*mults[j]*pixels > this.view.w/20 ) break outer;
      }
    }
    // Make sure we don't overrun the end of our array if we don't find a match
    if( i >= orders.length ) i = orders.length-1;
    if( j >= mults.length ) j = mults.length-1;

    var label = mults[j] + dims[i] + 'm';
    pixels = pixels*orders[i]*mults[j];

    // Use a smooth transition to resize and set the units
    document.id(this.source).getElement('div.scale div.ruler').tween( 'width', pixels );
    document.id(this.source).getElement('div.scale div.label').set( 'html', label );

  },



  /* Use an AJAX request to get the image size, tile size and number of resolutions from the server
   */
  load: function(){

    // If we have supplied the relevent information, simply use the given data
    if( this.loadoptions ){
      this.max_size = this.loadoptions.size;
      this.tileSize = this.loadoptions.tiles;
      this.num_resolutions = this.loadoptions.resolutions;
      this.createWindows();
    }
    else{
      var metadata = new Request(
        {
	  method: 'get',
	  url: this.server,
	  onComplete: function(transport){
	    var response = transport || alert( "Error: No response from server " + this.server );

	    // Parse the result
            var result = this.protocol.parseMetaData( response );
            this.max_size = result.max_size;
            this.tileSize = result.tileSize;
	    this.num_resolutions = result.num_resolutions;

	    this.createWindows();
          }.bind(this),
	  onFailure: function(){ alert('Error: Unable to get image and tile sizes from server!'); }
	} );

      // Send the metadata request
      metadata.send( this.protocol.getMetaDataURL(this.images[0].src) );
    }
  },



  /* Reload our view
   */
  reload: function(){

    // First cancel any effects on the canvas and delete the tiles within
    this.canvas.get('morph').cancel();
    this.canvas.getChildren('img').destroy();
    this.tiles.empty();
    this.calculateSizes();

    // Resize our navigation window
    document.id(this.source).getElements('div.navcontainer, div.navcontainer div.loadBarContainer').setStyle('width', this.navWin.w);

    // And reposition the navigation window
    if( this.showNavWindow ){
      var navcontainer = document.id(this.source).getElement('div.navcontainer');
      if( navcontainer ) navcontainer.setStyles({
	'top': 10,
	'left': document.id(this.source).getPosition(this.source).x + document.id(this.source).getSize().x - navcontainer.getSize().x - 10
      });

      // Resize our navigation window image
      if(this.zone){
        this.zone.getParent().setStyles({
	  height: this.navWin.h
        });
      }
    }

    // Reset and reposition our scale
    if( this.scale ){
      this.updateScale();
      pos = document.id(this.source).getSize().y -
	document.id(this.source).getElement('div.scale').getSize().y - 10;
      document.id(this.source).getElement('div.scale').setStyles({
	'left': 10,
	'top': pos
      });
    }

    // Resize the main tile canvas
    var origin_property = this.CSSprefix+'transform-origin';
    var transform_property = this.CSSprefix+'transform';
    this.canvas.setStyles({
      width: this.wid,
      height: this.hei
    });
    this.canvas.setStyle( origin_property, '50% 50%' );
    this.canvas.setStyle( transform_property, 'rotate(0deg)' );
    if( this.zone ) this.zone.setStyle( transform_property, 'rotate(0deg)' );
    this.orientation = 0;

    this.reCenter();
    this.requestImages();
    this.positionZone();

  },


  /* Recenter the image view
   */
  reCenter: function(){

    // Calculate the x,y for a centered view, making sure we have no negative
    // in case our resolution is smaller than the viewport
    var xoffset = Math.round( (this.wid-this.view.w)/2 );
    this.view.x = (xoffset<0)? 0 : xoffset;

    var yoffset = Math.round( (this.hei-this.view.h)/2 );
    this.view.y = (yoffset<0)? 0 : yoffset;

    // Center our canvas, taking into account images smaller than the viewport
    this.canvas.setStyles({
      left: (this.wid>this.view.w)? -this.view.x : Math.round((this.view.w-this.wid)/2),
      top : (this.hei>this.view.h)? -this.view.y : Math.round((this.view.h-this.hei)/2)
    });

    // Constrain the movement of our canvas to our containing div
    var ax = this.wid<this.view.w ? Array(Math.round((this.view.w-this.wid)/2), Math.round((this.view.w-this.wid)/2)) : Array(this.view.w-this.wid,0);
    var ay = this.hei<this.view.h ? Array(Math.round((this.view.h-this.hei)/2), Math.round((this.view.h-this.hei)/2)) : Array(this.view.h-this.hei,0);

    this.touch.options.limit = { x: ax, y: ay };

  },



  /* Reposition the navigation rectangle on the overview image
   */
  positionZone: function(){

    if( !this.showNavWindow ) return;

    var pleft = (this.view.x/this.wid) * (this.navWin.w);
    if( pleft > this.navWin.w ) pleft = this.navWin.w;
    if( pleft < 0 ) pleft = 0;

    var ptop = (this.view.y/this.hei) * (this.navWin.h);
    if( ptop > this.navWin.h ) ptop = this.navWin.h;
    if( ptop < 0 ) ptop = 0;

    var width = (this.view.w/this.wid) * (this.navWin.w);
    if( pleft+width > this.navWin.w ) width = this.navWin.w - pleft;

    var height = (this.view.h/this.hei) * (this.navWin.h);
    if( height+ptop > this.navWin.h ) height = this.navWin.h - ptop;

    var border = this.zone.offsetHeight - this.zone.clientHeight;

    // Move the zone to the new size and position
    this.zone.morph({
      left: pleft,
      top: ptop + 10, // 10px for the toolbar
      width: (width-border>0)? width - border : 1, // Watch out for zero sizes!
      height: (height-border>0)? height - border : 1
    });

    }

});


/* Static function to synchronize iipmooviewer instances
 */
IIPMooViewer.synchronize = function(viewers){
  this.sync = viewers;
};


/* Static function get get an array of the windows that are
   synchronized to this one
 */
IIPMooViewer.windows = function(s){
  if( !this.sync ) return Array();
  return this.sync.filter( function(t){
     return (t!=s);
  });
};
