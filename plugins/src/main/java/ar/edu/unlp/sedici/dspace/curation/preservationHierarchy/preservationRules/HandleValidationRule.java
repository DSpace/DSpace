package ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.preservationRules;

import org.dspace.content.Item;


/**
 * @author terru
 *
 */
public class HandleValidationRule extends Rule {
	/* (non-Javadoc)
	 * @see ar.edu.unlp.sedici.dspace.curation.preservationHierarchy.preservationRules.Rule#evaluate(org.dspace.content.Item)
	 */
	@Override
	public int evaluate(Item item) {
		String handle = item.getHandle();
		//toma el handle del item y evalúa:
			//que el handle exista
			//que no sea 123456789
		if(!(handle == null) && (!handle.equals("123456789"))){
			return 1;
		}
		return 0;
	}
}
