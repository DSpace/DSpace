jQuery.noConflict();

	/**
	 * Registra listener para componentes de checkbox
	 */
	jQuery(document).ready(function(){
	
		jQuery("input[id^='folder_']").click(function(){
	
		
		if(this.id.search('parent') != -1)
		{
			/** Verify all children selected **/
			handleChildSelection(this);
		}
		else
		{
			/** Checkbox agrupador foi selecionado **/
				handleParentSelection(this);
			}
			
		});
		
		
	});

	/**
	 * Trata seleção de checkbox "filhos", em outras palavras, sub-pastas
	 * @param childObject Checkbox selecionado
	 */
	function handleChildSelection(childObject)
	{	
		jQuery("input[id='folder_" + childObject.id.split("_parent_")[1] + "']").prop('checked', 
				/** true caso todos os filhos estejam selecionados **/
				jQuery("input[id*='parent_" + childObject.id.split("_parent_")[1] + "']:checked").length == jQuery("input[id*='parent_" + childObject.id.split("_parent_")[1] + "']").length);
	}

	/**
	 * Trata seleção
	 * @param parentObject
	 */
	function handleParentSelection(parentObject)
	{
		jQuery("input[id*='parent_" + parentObject.id.split("_")[1] + "']").prop('checked', parentObject.checked);
	}