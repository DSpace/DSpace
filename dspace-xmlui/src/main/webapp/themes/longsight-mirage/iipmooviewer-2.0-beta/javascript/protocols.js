/*
   IIPImage Javascript Viewer <http://iipimage.sourceforge.net>
                          Version 2.0

   Protocol classes for handling IIP, Zoomify, Deepzoom and Djatoka protocols


   Copyright (c) 2007-2011 Ruven Pillay <ruven@users.sourceforge.net>

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

*/



/* IIP Protocol Handler
 */
var IIP = new Class({

  /* Return metadata URL
   */
  getMetaDataURL: function(image){
    return "FIF=" + image + "&obj=IIP,1.0&obj=Max-size&obj=Tile-size&obj=Resolution-number";
  },

  /* Return an individual tile request URL
   */
  getTileURL: function(server,image,resolution,sds,contrast,k,x,y){
    return server+"?FIF="+image+"&CNT="+contrast+"&SDS="+sds+"&JTL="+resolution+"," + k;	
  },

  /* Parse an IIP protocol metadata request
   */
  parseMetaData: function(response){
    var tmp = response.split( "Max-size" );
    if(!tmp[1]) alert( "Error: Unexpected response from server " + this.server );
    var size = tmp[1].split(" ");
    var max_size = { w: parseInt(size[0].substring(1,size[0].length)),
		     h: parseInt(size[1]) };
    tmp = response.split( "Tile-size" );
    size = tmp[1].split(" ");
    var tileSize = { w: parseInt(size[0].substring(1,size[0].length)),
		     h: parseInt(size[1]) };
    tmp = response.split( "Resolution-number" );
    num_resolutions = parseInt( tmp[1].substring(1,tmp[1].length) );
    var result = {
      'max_size': max_size,
      'tileSize': tileSize,
      'num_resolutions': num_resolutions
    };
    return result;
  },

  /* Return URL for a full view
   */
  getRegionURL: function(image,x,y,w,h){
    var rgn = x + ',' + y + ',' + w + ',' + h;
    return '?FIF='+image+'&WID='+w+'&RGN='+rgn+'&CVT=jpeg';
  }

});



/* Zoomify Protocol Handler
 */
var Zoomify = new Class({

  /* Return metadata URL
   */
  getMetaDataURL: function(image){
    return "Zoomify=" + image + "/ImageProperties.xml";
  },

  /* Return an individual tile request URL
   */
  getTileURL: function(server,image,resolution,sds,contrast,k,x,y){
    return server+"?Zoomify="+image+"/TileGroup0/"+resolution+"-"+x+"-"+y+".jpg";
  },

  /* Parse a Zoomify protocol metadata request
   */
  parseMetaData: function(response){
    // Simply split the reponse as a string
    var tmp = response.split('"');
    var w = parseInt(tmp[1]);
    var h = parseInt(tmp[3]);
    var ts = parseInt(tmp[11]);
    // Calculate the number of resolutions - smallest fits into a tile
    var max = (w>h)? w : h;
    var n = 1;
    while( max > ts ){
      max = Math.floor( max/2 );
      n++;
    }
    var result = {
      'max_size': { w: w, h: h },
      'tileSize': { w: ts, h: ts },
      'num_resolutions': n
    };
    return result;
  },

  /* Return URL for a full view - not possible with Zoomify
   */
  getRegionURL: function(image,x,y,w,h){
    return null;
  }

});



/* Djatoka Protocol Handler
 */
var Djatoka = new Class({

  'svc_val_fmt': "info:ofi/fmt:kev:mtx:jpeg2000",
  'svc_id': "info:lanl-repo/svc/getRegion",

  /* Return metadata URL
   */
  getMetaDataURL: function(image){
    return "url_ver=Z39.88-2004&rft_id=" + image + "&svc_id=info:lanl-repo/svc/getMetadata";
  },

  /* Return an individual tile request URL
   */
  getTileURL: function(server,image,resolution,sds,contrast,k,x,y){
    var src = server + "?url_ver=Z39.88-2004&rft_id="
      + image + "&svc_id=" + this.svc_id
      + "&svc_val_fmt=" + this.svc_val_fmt
      + "&svc.format=image/jpeg&svc.level="
      + resolution + "&svc.rotate=0&svc.region="
      + djatoka_y + "," + djatoka_x + ",256,256";
    return src;
  },

  /* Parse a Djatoka protocol metadata request
   */
  parseMetaData: function(response){
    var p = eval("(" + response + ")");
    var tmp = p.levels;
    var w = parseInt(p.width);
    var h = parseInt(p.height);
    var num_resolutions = parseInt(p.levels);
    var result = {
      'max_size': { w: w, h: h },
      'tileSize': { w: 256, h: 256 },
      'num_resolutions': num_resolutions
    };
    return result;
    },

  /* Return URL for a full view
   */
  getRegionURL: function(image,x,y,w,h){
    return null;
  }

});



/* DeepZoom Protocol Handler
 */
var DeepZoom = new Class({

  /* Return metadata URL
   */
  getMetaDataURL: function(image){
    return "Deepzoom=" + image + ".dzi";
  },

  /* Return an individual tile request URL
   */
  getTileURL: function(server,image,resolution,sds,contrast,k,x,y){
    return server+'?DeepZoom='+image+'_files/'+(resolution+1)+'/'+x+'_'+y+'.jpg';
  },

  /* Parse a Deepzoom protocol metadata request
   */
  parseMetaData: function(response){
    var ts = parseInt( /TileSize="(\d+)/.exec(response)[1] );
    var w = parseInt( /Width="(\d+)/.exec(response)[1] );
    var h = parseInt( /Height="(\d+)/.exec(response)[1] );
    // Number of resolutions is the ceiling of Log2(max)
    var max = (w>h)? w : h;
    var result = {
      'max_size': { w: w, h: h },
      'tileSize': { w: ts, h: ts },
      'num_resolutions': Math.ceil( Math.log(max)/Math.LN2 )
    };
    return result;
  },

  /* Return URL for a full view - not possible with Deepzoom
   */
  getRegionURL: function(image,x,y,w,h){
    return null;
  }

});
