
/* VARIABLES

/*  
Used on the gallery page.
Records all ids assigned to items in order to initialize popups in the gallery display 
*/
var itemids = Array();

/* 
Used on the individual item display page.
Not currently used, but is populated and intended for later development.

An array of Javascript object of JPEGs in the file, each with the 
properties size and url, as in 
	o.size
	o.url
The app can then make user of these as necessary. For example, 
could be used to display a service image if it was under 1 MB, etc 
*/
var imageJpegArray = Array();

/* 
Convenience access to the service image's url
*/
var serviceImgUrl = '';


/**
* JQuery initialization routine 
*/
$(document).ready(function() { 
	initGalleryPopups();
	initZoomableImage();
	
	// initialize the about popup
	$("a#about").fancybox({
		'hideOnContentClick':false
	});
});

/**
* Initializes popups on the gallery view page
*/
function initGalleryPopups() {
	for (var i=0; i != itemids.length; i++) { 
		var sel = "a#anchor" + itemids[i]; 
		$(sel).fancybox({ 'hideOnContentClick': false }); 
		sel = "a#image" + itemids[i]; 
		$(sel).fancybox({ 'hideOnContentClick': false }); 
	} 
}

/**
* If there is a JPEG image less than MAX_SERVICE_IMG_SIZE, 
* set the largest image found as the service image
* and display it as a zoomable image
*/
function initZoomableImage() {
	
	if (imageJpegArray.length >0)  {
	
		var serviceImg = new Object();
		serviceImg.size = 0;
		
		for ( var i=0; i<imageJpegArray.length; i++) {
			if (imageJpegArray[i].size < MAX_SERVICE_IMG_SIZE && 
				( imageJpegArray[i].size > serviceImg.size))  {
				serviceImg = imageJpegArray[i];
			}
		}
		
		if (serviceImg.size > 0) {
			var html =  "<img src ='"+serviceImg.url+"' alt='zoomable image' onmouseover='TJPzoom(this);' width='"+ZOOMABLE_IMG_WIDTH+"'>";
			html+=	"</img>"
			$("#image-zoom-panel").prepend(html);
			
			serviceImgUrl = serviceImg.url;
			
			// add the puzzle "easter egg" to the footer
			html = "<a href='javascript:showPuzzle()'>...</a>";
			$("#ds-footer-links").append(html);
		}
		
	}
}

/**
* Shows the puzzle
*/
function showPuzzle() {
	if (serviceImgUrl != '') {
		displayPuzzle(serviceImgUrl);
	}
}

/** 
* Displays the puzzle on the interface 
*/
function displayPuzzle(imgUrl) {
	var html = "<div id='puzzle'>";
	html += "<img src='" + imgUrl + "' id='puzzleimage' width='400' />";
	html += "<a href='javascript:hidePuzzle()'>Hide Puzzle</a>";
	html += "</div>";
	$("body").append(html);
	
	$("#puzzle").css("position","absolute");
	$("#puzzle").css("left",50);
	$("#puzzle").css("top",50);
	$("#puzzle").css("width",400);
	$("#puzzle").css("background-color","#fff");
	$("#puzzle").css("border-width","1px");
	$("#puzzle").css("border-color","#999");
	// $("#puzzle").css("background-color","#fff");	

	//$("#puzzleimage").css("width",400);
	var settings = { 
    hole: 6,                   // initial hole position [1 ... rows*columns] 
    shuffle: true,             // initially show shuffled pieces [true|false] 
    numbers: false,              // initially show numbers on pieces [true|false] 
 
    // display additional gui controls 
    control: { 
        shufflePieces: true,    // display 'Shuffle' button [true|false] 
        confirmShuffle: true,   // ask before shuffling [true|false] 
        toggleOriginal: true,   // display 'Original' button [true|false] 
        toggleNumbers: true,    // display 'Numbers' button [true|false] 
        counter: true,          // display moves counter [true|false] 
        timer: true,            // display timer (seconds) [true|false] 
        pauseTimer: true        // pause timer if 'Original' button is activated 
                                // [true|false] 
    }};
	$('#puzzleimage').jqPuzzle(settings);
}

function hidePuzzle() {
	$("#puzzle").remove();
}

function showAbout() {
	$("#gallery-about").load(ABOUT_PAGE_URL);
}