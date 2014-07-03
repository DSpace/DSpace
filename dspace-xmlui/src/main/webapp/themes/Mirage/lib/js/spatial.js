/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
// Set up Basemap
var wms = new OpenLayers.Layer.WMS( "OpenStreetMaps WMS", "http://129.206.228.72/cached/osm", {layers: 'osm_auto:all'});
// Greece Bounding Box
var initBound = OpenLayers.Bounds.fromArray([18.02,34.58,32.99,44.17]);
// Map Object
var map;
// Layer that will hold Bounding Box
var box = new OpenLayers.Layer.Vector("Polygon Layer");
// Control for drawing Box during submission
var submit_control = new OpenLayers.Control();
// Control for drawing Box on spatial search
var search_control = new OpenLayers.Control();

// Extend Submission Control 
OpenLayers.Util.extend(submit_control, {
       	draw: function () {
               	// this Handler.Box will intercept the shift-mousedown before Control.MouseDefault gets to see it
               	this.box = new OpenLayers.Handler.Box( submit_control,{"done": this.notice},{keyMask: OpenLayers.Handler.MOD_SHIFT});
                this.box.activate();
       },

       	notice: function (bounds) {
		var ll = map.getLonLatFromPixel(new OpenLayers.Pixel(bounds.left, bounds.bottom)); 
		var ur = map.getLonLatFromPixel(new OpenLayers.Pixel(bounds.right, bounds.top));
		var bound=OpenLayers.Bounds.fromArray([ll.lon,ll.lat,ur.lon,ur.lat]);
		box.removeAllFeatures();
		box.addFeatures(new OpenLayers.Feature.Vector(bound.toGeometry()));
		// Fill text form inputs with item's Bounding Box
		document.getElementById (fieldID + '_west').value=ll.lon;
		document.getElementById (fieldID + '_east').value=ur.lon;
		document.getElementById (fieldID + '_south').value=ll.lat;
		document.getElementById (fieldID + '_north').value=ur.lat;
	}
});

OpenLayers.Util.extend(search_control, {
       	draw: function () {
               	// this Handler.Box will intercept the shift-mousedown before Control.MouseDefault gets to see it
               	this.box = new OpenLayers.Handler.Box( search_control,{"done": this.notice},{keyMask: OpenLayers.Handler.MOD_SHIFT});
                this.box.activate();
       },

       	notice: function (bounds) {
		var ll = map.getLonLatFromPixel(new OpenLayers.Pixel(bounds.left, bounds.bottom)); 
		var ur = map.getLonLatFromPixel(new OpenLayers.Pixel(bounds.right, bounds.top));
		var bound=OpenLayers.Bounds.fromArray([ll.lon,ll.lat,ur.lon,ur.lat]);
		box.removeAllFeatures();
		box.addFeatures(new OpenLayers.Feature.Vector(bound.toGeometry()));
		// Fill hidden form input with search bounding box
		document.getElementById(fieldID).value = ll.lon + "," + ur.lon + "," + ll.lat + "," + ur.lat;
	}
});


// Add Map function with the proper layers and controls
function addMap(mapID, control){
	map = new OpenLayers.Map(mapID);
	map.addLayers([wms,box]);
	// Load to map the proper control depending on it's purpose
	if (control=='submit') {
		map.addControl(submit_control);
	} else if (control=='search') {	
		map.addControl(search_control);
	} else {}
	map.zoomToExtent(initBound);

}

// Adds an Item's Bounding Box to map and zooms on it
function addItemsBoxToMap(west, south, east, north){
	var itemBounds = OpenLayers.Bounds.fromArray([west, south, east, north]);
	box.addFeatures(new OpenLayers.Feature.Vector(itemBounds.toGeometry()));
	map.zoomToExtent(itemBounds);
}

// Draws Bounding Box on Map based when user enter coordinates manually on text inputs during submission
function updateMap(id){
	var fid=id.substring(0,id.lastIndexOf('_'));
	var bounds=OpenLayers.Bounds.fromArray([document.getElementById (fid + '_west').value,document.getElementById (fid + '_south').value,document.getElementById (fid + '_east').value,document.getElementById (fid + '_north').value]);
	box.removeAllFeatures();
	box.addFeatures(new OpenLayers.Feature.Vector(bounds.toGeometry()));
	map.zoomToExtent(bounds);
}

// Clears spatial-query hidden input value an removes Box from map
function clearMap(){
	document.getElementById(fieldID).value="";
	box.removeAllFeatures();
}




