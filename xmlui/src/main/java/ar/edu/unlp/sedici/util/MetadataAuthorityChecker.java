package ar.edu.unlp.sedici.util;

import org.apache.log4j.Logger;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.Choices;

public class MetadataAuthorityChecker {

	private static final Logger log = Logger.getLogger(MetadataAuthorityChecker.class);
	
	public static void checkMetadata(MetadataValue m) {

		if((m.confidence == Choices.CF_ACCEPTED || m.confidence == Choices.CF_UNCERTAIN)
				&& (m.authority == null || m.authority == "")) {
			
			try {
				throw new Exception("Authority sin key asignada [itemID: "+m.getItemId()+" - fieldID: "+m.getFieldId()+" - value: ("+m.getValueId()+") "+m.getValue()+"]");
			} catch(Exception e) {
				log.warn(e.getMessage(), e);
			}
			
		}
	}
	
}
