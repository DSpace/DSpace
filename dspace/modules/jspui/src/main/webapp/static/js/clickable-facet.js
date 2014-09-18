/**
 * Gerencia abertura/fechamento de facetas
 * @author Márcio Ribeiro Gurgel do Amaral
 */
jQuery.noConflict();
jQuery(document).ready(function(){
	
	jQuery(".facet-panel").click(function(){
		
		var listGroup = jQuery(this).parent().find(".list-group");
		if(listGroup)
		{
			if(!jQuery(listGroup).is(":visible"))
			{
				jQuery(listGroup).show(450);
				jQuery(listGroup).parent().find(".glyphicon").attr("class", "glyphicon glyphicon-minus pull-right");
			}
			else
			{
				jQuery(listGroup).hide(450);
				jQuery(listGroup).parent().find(".glyphicon").attr("class", "glyphicon glyphicon-plus pull-right");
			}
		}
	});
});