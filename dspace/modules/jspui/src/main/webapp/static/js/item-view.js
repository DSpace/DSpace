jQuery.noConflict();

jQuery(document).ready(function(){
	
	jQuery(".reveal-link").click(function(){
		
		jQuery('#modal-' + this.id).show();
		jQuery('.reveal-modal-bg').show();
		
		jQuery('#modal-' + this.id).foundation('reveal', 'open', {
			animation: 'fadeAndPop',
			animation_speed: 250,
			close_on_background_click: true,
			close_on_esc: true,
			close_on_background_click: true,
			dismiss_modal_class: 'close-reveal-modal',
			bg_class: 'reveal-modal-bg',
			root_element: 'body'
		});
		
		
	});
	
	jQuery(".close-reveal-modal").click(function(){
		jQuery('.reveal-modal').hide();
		jQuery('.reveal-modal-bg').hide();
	});
	
});