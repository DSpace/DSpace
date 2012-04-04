$(document).ready(function() {
	// iniciamos el slideshow
	$("#slides").slides({
		play: 8000,
		pause: 2000,
		hoverPause: true,
		animationStart: function(current){
			$('.caption').animate({
				bottom:-65
			},100);
		},
		animationComplete: function(current){
			$('.caption').animate({
				bottom:0
			},200);
		},
		slidesLoaded: function() {
			$('.caption').animate({
				bottom:0
			},200);
		}
	});
});
