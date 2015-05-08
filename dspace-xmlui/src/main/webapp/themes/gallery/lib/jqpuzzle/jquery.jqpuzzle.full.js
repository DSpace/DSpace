(function($) {
/*
 * jqPuzzle - Sliding Puzzles with jQuery
 * Version 1.02
 * 
 * Copyright (c) 2008 Ralf Stoltze, http://www.2meter3.de/jqPuzzle/
 * Dual-licensed under the MIT and GPL licenses.
 */
$.fn.jqPuzzle = function(settings, texts) {

	// default settings
	var defaults = {
		
		rows: 4, 					// number of rows [3 ... 9]
		cols: 4,		 			// number of columns [3 ... 9]
		hole: 16,					// initial hole position [1 ... rows*columns]
		shuffle: false,				// initially show shuffled pieces [true|false]
		numbers: true,				// initially show numbers on pieces [true|false]
		language: 'en',				// language for gui elements [language code]
		
		// display additional gui controls
		control: {
			shufflePieces: true,	// display 'Shuffle' button [true|false]
			confirmShuffle: true,	// ask before shuffling [true|false]
			toggleOriginal: true,	// display 'Original' button [true|false]
			toggleNumbers: true,	// display 'Numbers' button [true|false]
			counter: true,			// display moves counter [true|false]
			timer: true,			// display timer (seconds) [true|false]
			pauseTimer: false		// pause timer if 'Original' button is activated 
									// [true|false]
		},
		
		// perform actions when the puzzle is solved sucessfully
		success: {
			fadeOriginal: true,		// cross-fade original image [true|false]
			callback: undefined,	// callback a user-defined function [function]
									// the function is passed an object as its argument
									// which includes the fields 'moves' and 'seconds'
			callbackTimeout: 300	// time in ms after which the callback is called
		},		
		
		// animation speeds and settings
		animation: {
			shuffleRounds: 3,		// number of shuffle rounds [1 ... ]
			shuffleSpeed: 800,		// time in ms to perform a shuffle round
			slidingSpeed: 200,		// time in ms for a single move
			fadeOriginalSpeed: 600	// time in ms to cross-fade original image
		},
		
		// additional style information not specified via css
		style: {
			gridSize: 2,			// space between two pieces in px
			overlap: true,			// if true, adjacent piece borders will overlap
									// applies only if gridSize is set to 0
			backgroundOpacity: 0.1	// opacity of the original image behind the pieces
									// [0 ... 1] (0 means no display)
		}
	};
	
	// language localizations
	var i18n = {
		en: {
			shuffleLabel: 			'Shuffle',
			toggleOriginalLabel: 	'Original',
			toggleNumbersLabel: 	'Numbers',
			confirmShuffleMessage: 	'Do you really want to shuffle?',
			movesLabel:				'moves',
			secondsLabel:			'seconds'
		},
		fr: {
			shuffleLabel: 			'Mélanger',
			toggleOriginalLabel: 	'Original',
			toggleNumbersLabel: 	'Nombres',
			confirmShuffleMessage: 	'Veux-tu vraiment mélanger?',
			movesLabel:				'mouvements',
			secondsLabel:			'secondes'
		},
		de: {
			shuffleLabel:			'Mischen',
			toggleOriginalLabel: 	'Original',
			toggleNumbersLabel: 	'Nummern',
			confirmShuffleMessage: 	'Willst du wirklich mischen?',
			movesLabel:				'Züge',
			secondsLabel:			'Sekunden'
		},
		pt: {
			shuffleLabel:			'Embaralhar',
			toggleOriginalLabel:	'Original',
			toggleNumbersLabel:		'Numeros',
			confirmShuffleMessage:	'Tem certeza que deseja reembralhar?',
			movesLabel:				'movimentos',
			secondsLabel:			'segundos'
		}
	};
	
	// if rows or cols, but no hole was user-defined,
	// explicitly set hole position to last piece (bottom right)
	if(settings && !settings.hole && (settings.rows || settings.cols)) {
		settings.hole = (settings.rows || defaults.rows) * (settings.cols || defaults.cols);
	}

	// extend the user-defined settings object with default settings
	settings = $.extend(true, {}, defaults, settings);

	// extend the user-defined texts object with current language texts
	texts = $.extend((i18n[settings.language] || i18n[defaults.language]), texts);
	
	// create some handy shortcut variables
	var rows = settings.rows, 
		cols = settings.cols, 
		hole = settings.hole;
	var control = settings.control,
		success = settings.success,
		animation = settings.animation,
		style = settings.style;

	// check settings for valid values

	// keep rows and columns within limits
	if(rows < 3 || rows > 9) rows = defaults.rows;
	if(cols < 3 || cols > 9) cols = defaults.rows;

	// keep hole position within limits
	if((hole > (rows*cols)) || (hole < 1)) hole = rows*cols;
	hole--; // zero-based index
	
	// animation speed = 0 doesn't work
	if(animation.slidingSpeed < 1) animation.slidingSpeed = 1;
	if(animation.shuffleSpeed < 1)  animation.shuffleSpeed = 1;
	if(animation.fadeOriginalSpeed < 1) animation.fadeOriginalSpeed = 1;
	
	// keep shuffle rounds within limits
	if(animation.shuffleRounds < 1) animation.shuffleRounds = 1;


	// helper functions --------------------------------------------------------
	
	// checks if the puzzle is solved
	var checkSolution = function($pieces) {
		// iterate over pieces and check each piece
		for(var i = 0; i < $pieces.length; i++) {
			// since the hole is not saved in the pieces array, 
			// adjust the index if it is bejond the hole position
			var pieceIndex = (i < hole) ? i : i + 1;
			
			// check if current position match target (index) position
			if($pieces.eq(i).attr('current') != pieceIndex) return false;
		}
		return true;
	};
	
	// checks if the puzzle can be solved (pure math ...)
	var checkOrder = function(numbersArray) {
		var product = 1;
		for(var i = 1; i <= (rows*cols-1); i++) {
			for(var j = (i+1); j <= (rows*cols); j++) {
				product *= ((numbersArray[i-1] - numbersArray[j-1]) / (i-j));
			}
		}
		return Math.round(product) == 1;
	};
	
	// get the linear index from a row/col pair (zero-based)
	var getLinearPosition = function(row, col) {
		return parseInt(row)*cols + parseInt(col);
	};
	
	// get the row/col pair from a linear index (zero-based)
	var getMatrixPosition = function(index) {
		return {row: (Math.floor(index/cols)), col: (index%cols)};
	};
	
	// get the pixel width of a border (internet explorer returns keywords)
	// the left side values will be used
	var getBorderWidth = function($element) {
		// the reported css value
		var property = $element.css('border-left-width');
		// a border style must be set to get a valid border width
		if($element.css('border-left-style') != 'none') {
			switch(property) {
				case 'thin': return 2;
				case 'medium': return 4;
				case 'thick': return 6;
				default:
					return parseInt(property) || 0;	// parse pixel value
			}
		}
		return 0;
	};

	// a reusable timer component
	// pass in a timeout interval in ms, after which callback is called
	// callback gets passed one argument, the elapsed time is ms
	var Timer = function(interval, callback) {
		var startTime;
		var startPauseTime;
		var totalPause = 0;
		var timeout;
		
		var run = function() {	
			update(new Date().getTime());
			timeout = setTimeout(run, interval);
		};
		
		var update = function(now) {
			callback(now - totalPause - startTime);
		};

		// start the timer
		this.start = function() {
			if(startTime) return false;
			startTime = new Date().getTime();
			run();
		};

		// stop the timer
		this.stop = function() {
			if(!startTime) return false;
			clearTimeout(timeout);
			var now = new Date().getTime();
			if(startPauseTime) totalPause += now - startPauseTime;
			update(now);
			startTime = startPauseTime = undefined;
			totalPause = 0;
		};

		// pause the timer
		this.pause = function() {
			if(!startTime || startPauseTime) return false;
			clearTimeout(timeout);
			startPauseTime = new Date().getTime();
		};

		// resume the timer
		this.resume = function() {
			if(!startPauseTime) return false;
			totalPause += new Date().getTime() - startPauseTime;
			startPauseTime = undefined;
			run();
		};
	};


	// apply jqPuzzle to each image element within selection -------------------

	return this.filter('img').each(function(){
		var $srcImg = $(this);			// source image as jQuery object
		var lock = false;				// flag if animations are running
		var moves = 0;					// counter for single moves
		var seconds = 0;				// counter for seconds after first move
		var solved;						// flag if the puzzle is solved by the user
		var shuffled = settings.shuffle;// flag if the puzzle was shuffled
		var timer;						// a timer component
					
		// save the current hole position for further manipulation
		var currHole = hole;
		
		
		// create dummy elements to get computed css properties
		var $dummyPiece = $('<div/>').addClass('jqp-piece');
		var $dummyWrapper = $('<div/>').addClass('jqp-wrapper').append($dummyPiece);
		var $dummyGui = $('<div/>')
			.attr('class', $srcImg.attr('class') || '') // transfer classes
			.addClass('jqPuzzle')
			.append($dummyWrapper);		
	
		// replace original image with dummy
		$srcImg.replaceWith($dummyGui);
	
		// assign old image id to dummy
		$dummyGui.attr('id', $srcImg.attr('id') || '');
		
		// get computed css properties of dummy elements
		var computedStyles = {
			gui: {
				border: getBorderWidth($dummyGui),
				padding: {
					left: parseInt($dummyGui.css('padding-left')) || 0,
					right: parseInt($dummyGui.css('padding-right')) || 0,
					top: parseInt($dummyGui.css('padding-top')) || 0,
					bottom: parseInt($dummyGui.css('padding-bottom')) || 0
				}
			},
			wrapper: {
				border: getBorderWidth($dummyWrapper),
				padding: parseInt($dummyWrapper.css('padding-left')) || 0
			},
			piece: {
				border: getBorderWidth($dummyPiece)
			}
		};
		
		// re-replace dummy elements with original image
		$dummyGui.removeAttr('id');
		$dummyGui.replaceWith($srcImg);
		
		
		// wait for the image to be loaded, to be able to get its real width/height
		$srcImg.one('load', function() {

			// overlap piece borders if there is no margin between pieces
			// this way, piece borders will not be doubled
			var overlap = (style.gridSize === 0 && style.overlap);
					
			// total space of piece borders and grid lines, which will cover parts of the image
			var coveredWidth  = cols*(2*computedStyles.piece.border) + (cols-1)*style.gridSize;
			var coveredHeight = rows*(2*computedStyles.piece.border) + (rows-1)*style.gridSize;
			
			// recalc if overlap
			if(overlap) {
				coveredWidth  -= (cols-1)*computedStyles.piece.border;
				coveredHeight -= (rows-1)*computedStyles.piece.border;
			}

			// make sure to get the original image size, not scaled values
			// in mozilla, width() and height() do not work with hidden elements
			$srcImg.css({width: 'auto', height: 'auto', visibility: 'visible'}); 
			
			// pieces width and height, based on original image size
			var width  = Math.floor(($srcImg.width()-coveredWidth) / cols);
			var height = Math.floor(($srcImg.height()-coveredHeight) / rows);
			
			// reject too small images
			if(width < 30 || height < 30) return false;

			// recalc full image width and height to avoid rounding errors
			var fullWidth  = cols*width + coveredWidth;
			var fullHeight = rows*height + coveredHeight;

			// image source path
			var imgSrc = $srcImg.attr('src');
			
			// total width/height of a piece (including piece border and 1 grid size)
			var totalPieceWidth  = width + 2*computedStyles.piece.border + style.gridSize;
			var totalPieceHeight = height + 2*computedStyles.piece.border + style.gridSize;

			// handle internet explorer quirks mode box model
			var boxModelHack = {
				piece : $.boxModel ? 0 : 2*computedStyles.piece.border,
				wrapper: $.boxModel ? 0 : 2*(computedStyles.wrapper.border + computedStyles.wrapper.padding),
				gui: {
					width: $.boxModel ? 0 : 2*computedStyles.gui.border + 
						computedStyles.gui.padding.left + computedStyles.gui.padding.right,
					height: $.boxModel ? 0 : 2*computedStyles.gui.border + 
						computedStyles.gui.padding.top + computedStyles.gui.padding.bottom
				}
			};
			
			
			// helper functions ------------------------------------------------
			
			// pixel offset of an element, based on matrix position
			var getOffset = function(row, col) {
				var offset = {
					left: computedStyles.wrapper.padding + col*totalPieceWidth,
					top:  computedStyles.wrapper.padding + row*totalPieceHeight
				};
				
				if(overlap) {
					offset.left -= col * computedStyles.piece.border;
					offset.top  -= row * computedStyles.piece.border;
				}
				
				return offset;
			};

			// shuffle pieces
			var shuffle = function(rounds, speed) {
				
				// when speed is defined, the function was triggered by a user event (button click)
				if(speed) {
					// do nothing, if disabled
					if($shuffleButton.is('.jqp-disabled')) return false;

					// do nothing, if locked
					if(lock) return false;
					
					// ask for confirmation
					if(control.confirmShuffle && (moves > 0) && 
					!window.confirm(texts.confirmShuffleMessage)) return false;

					lock = true; // set lock
					
					// if the puzzle is solved
					if(solved) {
						// reset gui
						$gui.removeClass('jqp-solved');
						
						// fade out original
						$background.fadeTo(animation.fadeOriginalSpeed, style.backgroundOpacity, function() {
							// opera gets kicked without remove()
							$background.remove().prependTo($wrapper);

							// re-enable all buttons
							$buttons.removeClass('jqp-disabled');							
						});
					}
				}

				// stop the timer
				if(timer) timer.stop();

				// reset flag and counters
				solved = false;
				shuffled = true;
				moves = 0;
				seconds = 0;
				
				// reset display
				if($display) $display.removeClass('jqp-disabled');
				if($counter) $counter.val(moves);
				if($timer) $timer.val(seconds);
				
				var shuffles = []; 
				var i = 0;
				// generate orders for several shuffle rounds
				while(i < rounds) {				
					// create an array for choosing random positions
					// based on its lenght, we can select free positions
					var choices = [];
					for(var j = 0; j < rows*cols; j++) {
						choices[j] = j;
					}
					// remove element on initial hole position
					choices.splice(hole, 1);
					
					shuffles[i] = [];
					// generate random numbers
					for(var j = 0; j < rows*cols; j++) {
						
						// but keep hole at initial position
						if(j == hole) {
							shuffles[i][j] = hole;
							continue;
						}
						
						// select a random position based on the length of the choices
						var randomIndex = Math.floor(Math.random()*choices.length);
						
						// save the value at this index as the next number in the current order
						shuffles[i][j] = choices[randomIndex];
						
						// remove this value from the choices array (reducing its length)
						choices.splice(randomIndex, 1);
					}
					
					// don't increase i if we are in last round 
					// and the generated order is not solvable
					if(((i+1) < rounds) || checkOrder(shuffles[i])) i++;
				}

				var animCounter = 0; // animation counter for save unlock
				
				// shuffle pieces in several rounds
				for(var i = 0; i < rounds; i++) {

					// set flag for the last round
					var lastRound = ((i+1) == rounds);

					// iterate over the generated orders
					// with j being the linear index for the destination order
					for(var j = 0; j < shuffles[i].length; j++) {
						
						// we cannot move the hole
						if(j == hole) {
							// update hole position
							if(lastRound) currHole = hole;
							continue;
						}

						// the value is the index of the current piece
						// in the original, ordered $pieces array
						var pieceIndex = shuffles[i][j];

						// since the hole is not saved in the $pieces array, 
						// adjust the index if it is bejond the hole position
						if(pieceIndex > hole) pieceIndex -= 1;
						
						// get the actual piece to be moved
						var $piece = $pieces.eq(pieceIndex);

						// get target position
						var target = getMatrixPosition(j);

						// get pixel offset new position
						var offset = getOffset(target.row, target.col);
						
						// update current row/cal in last round	(ie needs a string)
						if(lastRound) $piece.attr('current', j.toString());
						
						// either just set or animate styles
						if(speed === undefined) {
							$piece.css({left: offset.left, top: offset.top});
						} else {
							// animate!
							$piece.animate({left: offset.left, top: offset.top}, speed, null, function() {
								// unlock after last animation in last round
								animCounter++;
								if(animCounter == animation.shuffleRounds*(rows*cols-1)) {
									lock = false;
									animCounter = 0;
								}
							});
						}
					}
				}
			};


			// create elements -------------------------------------------------

			// create a wrapper for the pieces
			var $wrapper = $('<div/>')
				.addClass('jqp-wrapper')
				.css({	
					width: fullWidth + boxModelHack.wrapper,
					height: fullHeight + boxModelHack.wrapper,
					borderWidth: computedStyles.wrapper.border,
					padding: computedStyles.wrapper.padding,
					margin: 0,
					position: 'relative',
					overflow: 'hidden',
					display: 'block',
					visibility: 'inherit'
				});

			// create a single piece prototype to be cloned for the actual pieces
			var $protoPiece = $('<div/>')
				.addClass('jqp-piece')
				.css({
					width: width + boxModelHack.piece,
					height: height + boxModelHack.piece,
					backgroundImage: 'url(' + imgSrc + ')',
					borderWidth: computedStyles.piece.border,
					margin: 0,
					padding: 0,
					position: 'absolute',
					overflow: 'hidden',
					display: 'block',
					visibility: 'inherit',
					cursor: 'default'
				})
				.append($('<span/>')); // will hold the numbers

			// create pieces inside wrapper
			var $pieces = $([]); // create an empty jQuery object
			for(var i = 0; i < rows; i++) {
				for(var j = 0; j < cols; j++) {
					var index = getLinearPosition(i,j); // linear index
					
					// do not create piece at initial hole position
					if(index == hole) continue;
						
					// get piece position offset
					var offset = getOffset(i,j);

					// calculate background offset
					var bgLeft = -1 * (j*totalPieceWidth + computedStyles.piece.border);
					var bgTop  = -1 * (i*totalPieceHeight + computedStyles.piece.border);

					// recalc if overlap
					if(overlap) {
						bgLeft += j*computedStyles.piece.border;
						bgTop  += i*computedStyles.piece.border;
					}

					// create single pieces from prototype
					$pieces = $pieces.add($protoPiece.clone()
						.css({
							left: offset.left,
							top: offset.top,
							backgroundPosition: (bgLeft + 'px ' + bgTop + 'px')
						})
						// add expando property to save the current position
						.attr('current', String(index)) // ie hack: String()
						.appendTo($wrapper)
						// add number to inner span
						.children().text(index + 1).end()
					);
				}
			}
	
			// initially shuffle pieces
			if(settings.shuffle) shuffle(1);
			
			// create background (original image) inside wrapper
			var $background = $('<div/>')
				.css({
					width: fullWidth,
					height: fullHeight,
					left: computedStyles.wrapper.padding,
					top: computedStyles.wrapper.padding,
					backgroundImage: 'url(' + imgSrc + ')',
					borderWidth: 0,
					margin: 0,
					padding: 0,
					position: 'absolute',
					opacity: style.backgroundOpacity
				})
				.prependTo($wrapper);
			
			// create controls which will hold the buttons and the display
			var $controls = $('<div/>')
				.addClass('jqp-controls')
				.css({
					visibility: 'inherit',
					display: 'block',
					position: 'static'
				});

			var $shuffleButton, $originalButton, $numbersButton;
			
			// create a button prototype to be cloned for the actual buttons
			var $protoButton = $('<a/>').css('cursor', 'default');

			// create shuffle button
			if(control.shufflePieces) {
				$shuffleButton = $protoButton.clone()
					.text(texts.shuffleLabel)
					.appendTo($controls);
			}

			// create toggle original button
			if(control.toggleOriginal) {
				$originalButton = $protoButton.clone()
					.text(texts.toggleOriginalLabel)
					.appendTo($controls);
			}

			// create toggle numbers button
			if(control.toggleNumbers) {
				$numbersButton = $protoButton.clone()
					.text(texts.toggleNumbersLabel)
					.appendTo($controls);
				// immediately toggle button, if numbers are initially shown
				if(settings.numbers) $numbersButton.addClass('jqp-toggle');		
			}
			
			// keep a reference to all buttons for convenience
			var $buttons = $controls.children();

			var $display, $counter, $timer;
			if(control.counter || control.timer) {

				// create wrapper for counter/timer
				$display = $('<span/>')
					.css('cursor', 'default')
					.appendTo($controls);
			
				// create a text field prototype to be cloned for actual text fields
				var $protoField = $('<input/>')
					.val(0)
					.css({
						width: '5ex',
						cursor: 'default'
					})
					.attr('readonly', 'readonly');

				// create counter component
				if(control.counter)	$counter = $protoField.clone()
					.appendTo($display)
					.after(texts.movesLabel + ' ');
					
				// create timer component
				if(control.timer) $timer = $protoField.clone()
					.appendTo($display)
					.after(texts.secondsLabel);

				// disable display if the puzzle is not shuffled yet
				if(!settings.shuffle) $display.addClass('jqp-disabled');
			}
			
			// add link to jqPuzzle homepage
			var $credits = $('<a/>')
				.text('jqPuzzle')
				.attr('href', 'http://www.2meter3.de/jqPuzzle/')
				.css({
					'float': 'right',
					fontFamily: 'Verdana, Arial, Helvetica, sans-serif',
					fontSize: '9px',
					lineHeight: '12px',
					textDecoration: 'none',
					color: '#FFFFFF',
					backgroundColor: '#777777',
					backgroundImage: 'none',
					borderBottom: '1px dotted #FFFFFF',
					padding: '1px 3px 2px',
					marginRight: computedStyles.wrapper.border,
					position: 'static',
					display: 'inline',
					visibility: 'inherit'
				});

			// panel which holds controls and credits, used for height() calculations
			var $panel = $('<div/>')
				.css({
					width: fullWidth + 2*(computedStyles.wrapper.padding + computedStyles.wrapper.border),
					position: 'absolute', 
					display: 'block',
					visibility: 'inherit',//'visible',
					margin: '0px',
					padding: '0px',
					backgroundColor: 'transparent'
				})
				.append($credits).append($controls);

			// full gui (including wrapper and panel)
			var $gui = $('<div/>')
				.attr('class', $srcImg.attr('class') || '') // transfer classes
				.addClass('jqPuzzle') // always assign class jqPuzzle
				.css({
					width: fullWidth + 2*(computedStyles.wrapper.padding + computedStyles.wrapper.border) + boxModelHack.gui.width,
					height: fullHeight + 2*(computedStyles.wrapper.padding + computedStyles.wrapper.border) + boxModelHack.gui.height,
					textAlign: 'left',
					overflow: 'hidden', 
					display: 'block'
				})
				.append($wrapper).append($panel);

			// replace source image with jqPuzzle
			$srcImg.replaceWith($gui);

			// assign source image id to jqPuzzle
			var id = $srcImg.attr('id');
			if(id) $gui.attr('id', id);
			
			// opera has strange effect when calling hide() and val() before 
			// the elements are attached to the dom
			// hide numbers
			if(!settings.numbers) $pieces.children().hide();
			// fill inputs
			if($display) $display.children('input').val(0);

			// now, after everything is rendered, recalc gui height
			var guiHeight = $gui.height();
			var panelHeight = $panel.height();
			
			$gui.height($gui.height() + $panel.height());


			// attach events ---------------------------------------------------
			
			// prevent text selection
			if($.browser.msie) $gui[0].onselectstart = function() { return false; };
			else  $gui.mousedown(function() { return false; });
			
			// button press on mousedown
			$buttons.mousedown(function() {
				if(!$(this).is('.jqp-disabled')) $(this).addClass('jqp-down');
			});
			$buttons.mouseout(function() {
				$(this).removeClass('jqp-down');
			});
			$buttons.mouseup(function() {
				$(this).removeClass('jqp-down');	
			});
			
			// swap pieces on click
			$pieces.click(function() {
				// do nothing, if locked
				if(lock) return false;
				
				// do nothing, if solved after being shuffled
				if(solved) return false;
				
				lock = true; // set lock
				
				var $piece = $(this);
				
				// get current position from expando
				var current = $piece.attr('current');
				
				// get current matrix positions for piece and hole
				var source = getMatrixPosition(current);
				var dest = getMatrixPosition(currHole);

				// only swap pieces adjacent to the hole
				if(Math.abs(source.row - dest.row) + Math.abs(source.col - dest.col) != 1) {
						lock = false;
						return false;
					}

				// get offset for the new position
				var offset = getOffset(dest.row, dest.col);

				// update piece expando and current hole position
				$piece.attr('current', String(currHole)); // ie hack: String()
				currHole = current;

				// increase moves counter only if the puzzle was shuffled
				if(shuffled) moves++;

				// update counter field
				if($counter) $counter.val(moves);

				// start timer, if needed
				if(moves == 1) {
					// initiate timer with update function
					if(!timer) timer = new Timer(333, function(ms) {
						seconds = Math.floor(ms/1000);
						if($timer) $timer.val(seconds);
					});
					timer.start();
				}
				
				// animate!
				$piece.animate({left: offset.left, top: offset.top}, animation.slidingSpeed, null, function() {
					// only check if the puzzle was shuffled
					if(shuffled) {
						// check if the puzzle is solved
						solved = checkSolution($pieces);
						if(solved) {
							if(timer) timer.stop();
							shuffled = false;
							$gui.addClass('jqp-solved');
							window.setTimeout(finishGame, 100);	
						}
						else lock = false;						
					}
					else lock = false;
				});
			});

			// shuffle pieces on click
			if(control.shufflePieces) $shuffleButton.click(function() {
				shuffle(animation.shuffleRounds, animation.shuffleSpeed);	
			});

			// toggle original on click
			if(control.toggleOriginal) $originalButton.click(function() {
				// do nothing, if disabled
				if($originalButton.is('.jqp-disabled')) return false;

				// do nothing, if locked
				if(lock) return false;
				
				lock = true; // set lock
				
				if($originalButton.is('.jqp-toggle')) {
					// re-enable other buttons
					if(control.shufflePieces) $shuffleButton.removeClass('jqp-disabled');
					if(control.toggleNumbers) $numbersButton.removeClass('jqp-disabled');
			
					$originalButton.removeClass('jqp-toggle');
					
					// fade out original
					$background.fadeTo(animation.fadeOriginalSpeed, style.backgroundOpacity, function() {
						$(this).prependTo($wrapper);
						
						// resume timer
						if(control.pauseTimer && timer) timer.resume();
						
						lock = false;
					});
				} else {
					// disable other buttons
					if(control.shufflePieces) $shuffleButton.addClass('jqp-disabled');
					if(control.toggleNumbers) $numbersButton.addClass('jqp-disabled');
					
					$originalButton.addClass('jqp-toggle');

					// pause timer
					if(control.pauseTimer && timer) timer.pause();
					
					// fade in original
					$background.appendTo($wrapper).fadeTo(animation.fadeOriginalSpeed, 1, function() {
			
						lock = false;
					});
				}
				return false; // prevent default action
			});

			// toggle numbers on click
			if(control.toggleNumbers) $numbersButton.click(function() {
				// do nothing, if disabled
				if($numbersButton.is('.jqp-disabled')) return false;
				
				if ($numbersButton.is('.jqp-toggle')) {
					$numbersButton.removeClass('jqp-toggle');
					$pieces.children().hide();
				} else {
					$numbersButton.addClass('jqp-toggle');
					$pieces.children().show();
				}
			});		
			
			
			// work to do when the puzzle is solved
			var finishGame = function() {
				if(success.fadeOriginal) {	
					// disable buttons
					if(control.toggleOriginal) $originalButton.addClass('jqp-disabled');
					if(control.toggleNumbers) $numbersButton.addClass('jqp-disabled');
					
					// fade in original
					$background.appendTo($wrapper).fadeTo(animation.fadeOriginalSpeed, 1.0, function() {
						lock = false; // reset lock
						solutionCallback(); // call user callback
					});
				} else {
					lock = false; // reset lock
					solutionCallback(); // call user callback	
				}
			};

			// call a user-defined callback after a timeout, when the puzzle is solved
			var solutionCallback = function() {
				if($.isFunction(success.callback)) {
					setTimeout(function() {
						success.callback({moves: moves, seconds: seconds});
					}, success.callbackTimeout);	
				}
			};

		}); // img load
		
		// unfortunately, image load does not fire consistently across browsers
		// (especially with cached images)
		// therefore, check image.load periodically (bah, brute force...)
		var interval = setInterval(function() {
			if($srcImg[0].complete) {
				clearInterval(interval);
				$srcImg.trigger('load');
			}
		}, 333);
		
		/*
		var interval;
		(function waitForImage($image) {
			if(!$image[0].complete) {
				interval = setInterval(function() {
					waitForImage($image);
				}, 100);
			} else {
				clearInterval(interval);
				$image.trigger('load');
			}
		})($srcImg);*/
		
		// ie shows some inconsistencies with img load and cached images
		// fortunately, in this cases img.complete is true from the beginning
		//if($.browser.msie && $srcImg[0] && $srcImg[0].complete) $srcImg.trigger('load');


	}).end(); // return full collection to allow chaining
};

// automagically apply jqPuzzle to all images with class 'jqPuzzle'
$(document).ready(function() {

	$('img.jqPuzzle').each(function() {

		// define an additional micro format (to be used as a class name)
		
		/* Syntax:   .jqp[-LANGUAGE]-rROWS-cCOLS[-hHOLE][-sSHUFFLE_ROUNDS][-FLAGS]
		 * 
		 * Flags:    S - initially shuffle pieces
		 *           N - initially hide numbers
		 *           A - hide 'Shuffle' button
		 *           B - hide 'Original' button
		 *           C - hide 'Numbers' button
		 *           D - hide 'moves' display
		 *           E - hide 'seconds' display
		 */
		var microFormat = /\bjqp(-[a-z]{2})?-r(\d)-c(\d)(-h(\d+))?(-s(\d+))?(-[A-Z]+)?\b/;
				
		// execute regex and save matches
		var match = microFormat.exec(this.className);
		
		// build settings object from micro format
		var settings;
		if(match) {
			settings = {	
				rows: parseInt(match[2]),
				cols: parseInt(match[3]), 
				hole: parseInt(match[5]) || null,
				shuffle: match[8] && match[8].indexOf('S') != -1,
				numbers: match[8] ? match[8].indexOf('N') == -1 : true,
				language: match[1] && match[1].substring(1)
			};
			
			if(match[7]) {
				settings.animation = {};
				settings.animation.shuffleRounds = parseInt(match[7]);
			}
			
			if(match[8] && match[8].search(/[ABCDE]/) != -1) {
				settings.control = 	{};
				settings.control.shufflePieces = match[8].indexOf('A') == -1;
				settings.control.toggleOriginal = match[8].indexOf('B') == -1;
				settings.control.toggleNumbers = match[8].indexOf('C') == -1;
				settings.control.counter = match[8].indexOf('D') == -1;
				settings.control.timer = match[8].indexOf('E') == -1;
			}
		}
		// call the plugin
		$(this).jqPuzzle(settings);
	});
});

})(jQuery);