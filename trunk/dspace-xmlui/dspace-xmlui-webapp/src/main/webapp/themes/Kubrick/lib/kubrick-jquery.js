/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

// jQuery code for the Kubrick theme.  
// 
//  This applies some style that couldn't be achieved with css due to 
//  poor browser implementations of the W3C standard. Also, this provides 
//  the interactive sliders that hide and reveal metadata for specific 
//  items when browsing lists.


$(document).ready(function(){

	//alert("Render mode: "+ document.compatMode);
	
	//First, some css that couldn't be achieved with css selectors
	$("table:not(.ds-includeSet-metadata-table) tr td:has(span[class=bold])").css({ textAlign:"right", verticalAlign:"top" });
	$("table.ds-includeSet-metadata-table tr td:has(span[class=bold])").css({ textAlign:"left", verticalAlign:"top" });
	$("fieldset#aspect_submission_StepTransformer_list_submit-describe ol li.odd div.ds-form-content input#aspect_submission_StepTransformer_field_dc_subject ~ input.ds-button-field").css({display: "inline"});

	//The metadata sliders for ds-artifact-item-with-popup's
	$("div.item_metadata_more").toggle(function(){
		$(this).children(".item_more_text").hide();
		$(this).children(".item_less_text").show();
		$(this).next().slideDown();
	},function(){
		$(this).children(".item_more_text").show();
		$(this).children(".item_less_text").hide();
		$(this).next().slideUp();
	});
	
});