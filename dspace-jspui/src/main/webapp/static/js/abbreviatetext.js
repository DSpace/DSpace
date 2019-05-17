/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

//  @Author: Cornelius Matějka <cornelius.matejka@uni-bamberg.de>

function abbreviate(el, count, index) {
	var elementToAbbreviate = j(el);
	if (elementToAbbreviate.html().length > (count + 300)) {
		let shortText = j("<div id=\"div_abbreviated_" +index+ "\"></div>").html((elementToAbbreviate.html().substring(0, count)) + "...")
		elementToAbbreviate.before(shortText);

		let anchorElement = j("<a id=\"a_abbreviated_" +index+ "\"></a>");
		anchorElement.toggleClass("fa fa-chevron-down");
        anchorElement.css("cursor", "pointer");
        anchorElement.css("text-decoration", "none");
		anchorElement.click(function () {
			if(elementToAbbreviate.is(":visible")) elementToAbbreviate.stop().slideUp(0);
			else elementToAbbreviate.stop().slideDown(0);

			anchorElement.toggleClass("fa-chevron-down");
			anchorElement.toggleClass("fa-chevron-up");

			shortText.toggle();
		});

		elementToAbbreviate.after(anchorElement);
		elementToAbbreviate.hide();
	}
}

j(function () {
	j(".abbreviate-me").each(function(index) {
		abbreviate(this, 400, index);
	});
});
