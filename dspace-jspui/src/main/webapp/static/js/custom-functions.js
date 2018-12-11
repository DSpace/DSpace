/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
if (typeof jQuery === 'undefined') {
  throw new Error('Bootstrap\'s JavaScript requires jQuery')
}

+function ($) {
  'use strict';
  var version = $.fn.jquery.split(' ')[0].split('.')
  if ((version[0] < 2 && version[1] < 9) || (version[0] == 1 && version[1] == 9 && version[2] < 1)) {
    throw new Error('Bootstrap\'s JavaScript requires jQuery version 1.9.1 or higher')
  }
}(jQuery);

jQuery(document).ready(function($){
	$(".vertical-carousel").each(function(){
		var itemsToShow = $(this).data('itemstoshow');
		$(this).data('current', 0);
		var moreToShow = false;
		$(this).find(".list-groups > .list-group-item").each(function(index){
			if (index < itemsToShow) return;
			if (index == itemsToShow) {
				$(this).css('opacity',0.5);
			}
			else {
				$(this).hide();	
			}
			moreToShow = true;
		});
		var controllerDiv = $('<div class="pull-right">');
		var backArrow = $('<i class="fa fa-chevron-left text-muted">');
		var nextArrow = $('<i class="fa fa-chevron-right">');
		if (!moreToShow) nextArrow.addClass('text-muted');
		backArrow.data('vertical-carousel', this);
		backArrow.data('otherArrow', nextArrow);
		nextArrow.data('vertical-carousel', this);
		nextArrow.data('otherArrow', backArrow);
		backArrow.click(function(){
			if ($(this).hasClass('text-muted')) return;

			var vc = $(this).data('vertical-carousel');
			var itemsToShow = $(vc).data('itemstoshow');
			var currIdx = $(vc).data('current');
			$(vc).data('current', --currIdx);
			$(vc).find(".list-groups > .list-group-item").each(function(index){
				if (index - currIdx < itemsToShow && index >= currIdx) {
					$(this).fadeTo('slow',1);
				}
				else {
					if (index == currIdx+itemsToShow) {
						$(this).fadeTo('slow',0.5);
					}
					else {
						$(this).fadeOut();
					}
				}
			});
			$($(this).data('otherArrow')).removeClass('text-muted');
			if (currIdx == 0) $(this).addClass('text-muted');
		});
		nextArrow.click(function(){
			if ($(this).hasClass('text-muted')) return;

			var vc = $(this).data('vertical-carousel');
			var itemsToShow = $(vc).data('itemstoshow');
			var currIdx = $(vc).data('current');
			$(vc).data('current', ++currIdx);
			$(vc).find(".list-groups > .list-group-item").each(function(index){
				if (index - currIdx < itemsToShow && index >= currIdx) {
					$(this).fadeTo('slow',1);
				}
				else {
					if (index == currIdx+itemsToShow) {
						$(this).fadeTo('slow',0.5);
					}
					else {
						$(this).fadeOut();
					}
				}
			});
			$($(this).data('otherArrow')).removeClass('text-muted');
			if (currIdx + itemsToShow == $(vc).find(".list-groups > .list-group-item").size()) 
				$(this).addClass('text-muted');
		});
		
		controllerDiv.append(backArrow);
		controllerDiv.append('&nbsp;');
		controllerDiv.append(nextArrow);
		$(this).find('.panel-heading > .panel-title').append(controllerDiv);
	});
    addConfirmationBeforeAnyDeleteAction();
});

/*
 * Adds an event listener to any button/link that potentially deletes
 * items, e.g. CRIS items, boxes, tabs, etc.
 */
function addConfirmationBeforeAnyDeleteAction() {
  function addConfirmation() {
    this.addEventListener('click', function(event) {
      event.preventDefault();
      var choice = confirm("Are you sure you want delete this item?");
      if (choice) window.location.href = this.getAttribute('href');
    });
  }

  // CRIS items, like RPs, OrgUnits, Projects, etc.
  j('[id^=delete]').each(addConfirmation);

  // JDynA items, like tabs, boxes, etc.
  j('.jdynaremovebutton').each(addConfirmation);
}

