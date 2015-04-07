/*
* jQuery Sapling - http://tameraydin.github.com/jquery-sapling/
*
*/

;(function ($, window, document, undefined) {

	$.sapling = function(element, options) {

		var plugin = this,
			$element = $(element),

			defaults = {
				multiexpand: true,
				animation: false
			};

		var expand = function(el) {
				el.addClass('sapling-expanded');
			},
			collapse = function(el) {
				el.removeClass('sapling-expanded');
			},
			click = function(e) {
				if (e.target.nodeName !== 'A') {
					if ($(this).hasClass('sapling-expanded')) {
						collapse($(this));

					} else {
						if (!plugin.settings.multiexpand) {
							$element.find('.sapling-expanded').not($(this).parents()).trigger('click');
						}

						expand($(this));
					}
				}
			};

		plugin.settings = {};

		plugin.init = function() {
			plugin.settings = $.extend({}, defaults, options);

			if (plugin.settings.animation) {
				expand = function(el) {
					el.children('ul, ol').slideDown(function() {
						el.addClass('sapling-expanded');
					});
				};

				collapse = function(el) {
					el.children('ul, ol').slideUp(function() {
						el.removeClass('sapling-expanded');
					});
				};
			}

			$element.addClass('sapling-list');
			$element.children('li').addClass('sapling-top-level');
			$element.find('li').each(function() {
				var $this = $(this);
				var childrenTrees = $this.children('ul, ol');

				if (childrenTrees.index() !== -1) {
					$this.addClass('sapling-item');
					$this.bind('click.sapling', click);
					childrenTrees.bind('click.sapling', function(e) {
						if ($(e.target).parent().get(0).nodeName != 'A') {
							return false;
						}
					});
				}
			});
		};

		plugin.expand = function() {
			expand($element.find('.sapling-item'));
		};

		plugin.collapse = function() {
			collapse($element.find('.sapling-expanded'));
		};

		plugin.destroy = function() {
			$element.removeClass('sapling-list');
			$element.children('li').removeClass('sapling-top-level');
			$element.find('li').each(function() {
				var $this = $(this);
				var childrenTrees = $this.children('ul, ol');

				if (childrenTrees.index() !== -1) {
					$this.removeClass('sapling-item');
					$this.unbind('.sapling');
					childrenTrees.removeAttr('style');
					childrenTrees.unbind('.sapling');
				}
			});
			$element.find('.sapling-expanded').removeClass('sapling-expanded');
			$element.data('sapling', null);
		};

		plugin.init();
	};

	$.fn.sapling = function(options) {
		return this.each(function() {
			var saplingData = $(this).data('sapling');
			if (saplingData === undefined || saplingData === null) {
				var plugin = new $.sapling(this, options);
				$(this).data('sapling', plugin);
			}
		});
	};

})(jQuery, window, document);