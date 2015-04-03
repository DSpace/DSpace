package cz.cuni.mff.ufal.lindat.utilities.functionalities;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

public class ShibbolethScopeAffiliation {

	private static Logger log = Logger.getLogger(ShibbolethScopeAffiliation.class);

	public static String scopeRecognition(String affiliation) {
		//if (!FunctionalityManager.manager.isFunctionalityEnabled("lr.scope.recognition") || affiliation == null) {
		//	return affiliation;
		//}

		log .log(Level.INFO,
				"The scopeRecognition plugin has sense only if in case ignore-scope is set to false");
		int index = affiliation.indexOf("@");
		// This has to be really safe - so the scope needs to start with @
		if (index != -1) {
			affiliation = affiliation.substring(index, affiliation.length());
		}
		log.log(Level.INFO, "Current affiliation ='" + affiliation + "'");

		return affiliation;
	}
}

