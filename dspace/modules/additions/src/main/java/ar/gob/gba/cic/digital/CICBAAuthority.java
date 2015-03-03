package ar.gob.gba.cic.digital;

import org.dspace.core.ConfigurationManager;
import ar.edu.unlp.sedici.dspace.authority.SPARQLAuthorityProvider;

public abstract class CICBAAuthority extends SPARQLAuthorityProvider {
	
	protected static final String NS_CIC = "http://www.cic.gba.gov.ar/ns#";

	protected String getSparqlEndpoint() {
		return ConfigurationManager.getProperty("sparql-authorities", "sparql-authorities.endpoint.url");
	}

}
