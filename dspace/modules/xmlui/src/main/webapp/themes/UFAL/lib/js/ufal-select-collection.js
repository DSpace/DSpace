/*global jQuery */
/*jshint globalstrict: true*/
'use strict';
var ufal = ufal || {};

ufal.selectCollection = {
		
	// TODO: use css classes instead of inline styling

	model : null,
	
	getSelectCommunityDiv : function() {
		return $('#cz_cuni_mff_ufal_dspace_app_xmlui_aspect_submission_submit_SelectCollectionStep_div_select-community-div');
	},
	
	getSelectCommunityCommunitiesListDiv : function() {
		return $('#cz_cuni_mff_ufal_dspace_app_xmlui_aspect_submission_submit_SelectCollectionStep_div_communities-list');
	},
		
	getSelectCommunityCommunitiesListLinks : function() {
		return $('#cz_cuni_mff_ufal_dspace_app_xmlui_aspect_submission_submit_SelectCollectionStep_div_communities-list a');
	},	
	
	getSelectCollectionSelect: function() {
		return $('#cz_cuni_mff_ufal_dspace_app_xmlui_aspect_submission_submit_SelectCollectionStep_field_handle');
	},
	
	getSelectCollectionDiv : function() {
		return $('#cz_cuni_mff_ufal_dspace_app_xmlui_aspect_submission_submit_SelectCollectionStep_div_select-collection-div');
	},
	
	getSelectCollectionSubmitButton : function() {
		return $('#cz_cuni_mff_ufal_dspace_app_xmlui_aspect_submission_submit_SelectCollectionStep_field_submit');
	},
	
	getCommunitiesModelInput : function() {
		return $('#cz_cuni_mff_ufal_dspace_app_xmlui_aspect_submission_submit_SelectCollectionStep_field_communities-model');
	},
	
	createCommunitiesGUI : function(model) {
		var html = '';				
		html += '<div style="margin-bottom:20px;">';
		html += '<div class="well well-light" style="display: table-row;" class="text-center" id="communities">';		
		for ( var i in model.communities) {
			if(i > 0) {
				html += '<div style="display: table-cell; vertical-align: top; width: 2%;"></div>';
			}
			var community = model.communities[i];
			html += '<div style="display: table-cell; vertical-align: top; width: 49%;">';
			html += '<a href="#" class="thumbnail" style="display: block; line-height: inherit; padding: 1em 2em;" id="community_' + community.id + '">';			
			if (community.logoURL != "") {
				html += '<div style="line-height: 200px; text-align: center;">';
				html += '<img src="' + community.logoURL + '" alt="'
						+ community.name + '" />';
				html += '</div>';
			}			
			html += '<div style="min-height: 7em;">';
			html += '<h4 class="text-center">' + community.name + '</h4>';
			if (community.shortDescription != "") {
				html += '<p>' + community.shortDescription + '</p>';
			}
			html += '</div>';
			html += '</a>';
			html += '</div>';

		}		
		html += '</div>';
		html += '</div>';
		ufal.selectCollection.getSelectCommunityCommunitiesListDiv().html(html);		
	},

	getCommunitiesModel : function() {
		var model = {};
		var modelJSON = ufal.selectCollection.getCommunitiesModelInput().val();
		if (modelJSON != "") {
			model = jQuery.parseJSON(modelJSON);
		}
		return model;
	},		

	populateCollections : function(communityID, model) {
		var select = ufal.selectCollection.getSelectCollectionSelect();
		select.find('option:gt(0)').remove();
		for ( var i in model.communities) {
			var community = model.communities[i];
			if (community.id == communityID) {
				for ( var j in community.collections) {
					var collection = community.collections[j];
					select.append('<option value="' + collection.handle + '">'
							+ collection.name + '</option>');
				}
				break;
			}
		}
	},		
	
	showCommunitiesGUI: function() {
		ufal.selectCollection.getSelectCommunityDiv().removeClass('hidden');				
	},

	hideCollectionsGUI : function() {
		ufal.selectCollection.getSelectCollectionDiv().hide();
		ufal.selectCollection.getSelectCollectionSubmitButton().attr('disabled', 'disabled');
	},

	showCollectionsGUI : function() {		
		ufal.selectCollection.getSelectCollectionDiv().show();
		if(ufal.selectCollection.getSelectCollectionDiv().find('select').val() == "") {
			ufal.selectCollection.getSelectCollectionSubmitButton().attr('disabled', 'disabled');
		}
		else {
			ufal.selectCollection.getSelectCollectionSubmitButton().removeAttr('disabled');
		}
	},
	
	showNextButtonOnly : function() {		
		ufal.selectCollection.getSelectCollectionDiv().hide();		
		ufal.selectCollection.getSelectCollectionSubmitButton().removeAttr('disabled');
	},
	
	onCommunityClick : function(e) {
		ufal.selectCollection.getSelectCommunityCommunitiesListLinks().removeClass('alert-info');
		$(this).toggleClass('alert-info');			
		var name = $(this).attr('id');
		var communityID = name.replace(/^.*_(\d+)/, '$1');
		ufal.selectCollection.populateCollections(communityID, ufal.selectCollection.model);
		var collectionSelect = ufal.selectCollection.getSelectCollectionSelect();
		if(collectionSelect.find('option').length == 2) {
			collectionSelect.find('option:eq(1)').prop('selected', true);
			ufal.selectCollection.showNextButtonOnly();			
		}
		else {
			ufal.selectCollection.showCollectionsGUI();								
			$('html, body').delay(100).animate({
		        scrollTop: ufal.selectCollection.getSelectCollectionDiv().offset().top		        
		    }, 200);
		}
		return false;
	},
	
	onCollectionChange : function(e) {
		if ($(this).val() != "") {				
			ufal.selectCollection.getSelectCollectionSubmitButton().removeAttr('disabled');
		} else {				
			ufal.selectCollection.getSelectCollectionSubmitButton().attr('disabled', 'disabled');
		}
	},
	
	init : function() {
		// remove well-small added in general xsl transformation				
		
		ufal.selectCollection.model = ufal.selectCollection.getCommunitiesModel();
		
		if (ufal.selectCollection.model.communities.length == 1
				&& ufal.selectCollection.model.communities[0].collections.length == 1) {
			// only one selectable collection - bypass this step
			ufal.selectCollection.getSelectCollectionSubmitButton().click();			
		}
		else {
			// hide collections and wait for selection of community
			ufal.selectCollection.hideCollectionsGUI();
						
			// create list of communities
			ufal.selectCollection.createCommunitiesGUI(ufal.selectCollection.model);
			
			// show list of communities
			ufal.selectCollection.showCommunitiesGUI();
			
			// bind events
			ufal.selectCollection.getSelectCommunityCommunitiesListLinks().on('click', ufal.selectCollection.onCommunityClick);		
			ufal.selectCollection.getSelectCollectionSelect().on('change', ufal.selectCollection.onCollectionChange);
		}
	}

};

jQuery(document).ready(function() {
	ufal.selectCollection.init();
}); // ready
