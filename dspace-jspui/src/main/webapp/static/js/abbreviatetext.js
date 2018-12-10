/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

//  @Author: Cornelius Matějka <cornelius.matejka@uni-bamberg.de>

function abbreviate(el, count) {
	var $el = j(el);
	if ($el.html().length > (count + 300)) {
		let $shortText = j("<div></div>").html(($el.html().substring(0, count)) + "...")
		$el.before($shortText);

		let $a = j("<a></a>");
		$a.toggleClass("fa fa-plus");
		$a.click(function () {
			if($el.is(":visible")) $el.stop().slideUp("fast");
			else $el.stop().slideDown("fast");

			$a.toggleClass("fa-plus");
			$a.toggleClass("fa-minus");

			$shortText.toggle();
		});

		$el.after($a);
		$el.hide();
	}
}

j(function () {
	j(".abbreviate-me").each(function() {
		abbreviate(this, 400);
	});
});