(function() {
	
	jQuery.noConflict();
	jQuery.datepicker.setDefaults( jQuery.datepicker.regional[ "pt_BR" ] );
	jQuery(document).ready(function($) {
       
		$(".submit-date-field").datepicker({
        	dateFormat: "yy-mm-dd",
        	 changeMonth: true,
        	 changeYear: true
        });
        
    });
	

    
})();


jQuery.noConflict();


jQuery(document).ready(function(){
	
	jQuery("[tooltiped]").click(function(){return false;});
	
	jQuery("[tooltiped]").each(function (){
		var splitedHref = jQuery(this).attr("href").split("#");
		var urlHref = splitedHref[0];
		var anchor = splitedHref[1];
		
		jQuery(this).qtip({
			content: {
				ajax: {
					url: urlHref,
					type: 'GET',
					data: {}, 
					success: function(data, status) {
						
						var htmlparts = jQuery.parseHTML(data);
						var foundID = null;
						
						for(var i = 0; i < htmlparts.length; i++)
						{
							if(jQuery(htmlparts[i]).attr("id") == anchor)
							{
								foundID = jQuery(htmlparts[i]);
							}
						}
						
						this.set('content.text', foundID);
					}
				}
			}
		});
		
	});
	
	
});
