/*
  kubrick-jquery.js

  Version: $Revision: 1.18 $
 
  Date: $Date: 2006/05/01 21:56:29 $
 
  Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
  Institute of Technology.  All rights reserved.
 
  Redistribution and use in source and binary forms, with or without
  modification, are permitted provided that the following conditions are
  met:
 
  - Redistributions of source code must retain the above copyright
  notice, this list of conditions and the following disclaimer.
 
  - Redistributions in binary form must reproduce the above copyright
  notice, this list of conditions and the following disclaimer in the
  documentation and/or other materials provided with the distribution.
 
  - Neither the name of the Hewlett-Packard Company nor the name of the
  Massachusetts Institute of Technology nor the names of their
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.
 
  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
  ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
  HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
  INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
  BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
  OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
  TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
  USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
  DAMAGE.
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