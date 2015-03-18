package ar.gob.gba.cic.digital;

import org.dspace.core.ConfigurationManager;

import ar.edu.unlp.sedici.dspace.authority.SPARQLAuthorityProvider;

public abstract class CICBAAuthority extends SPARQLAuthorityProvider {
	
	protected static final String NS_CIC = "http://www.cic.gba.gov.ar/ns#";

	protected String getSparqlEndpoint() {
		String endpoint = ConfigurationManager.getProperty("sparql-authorities", "sparql-authorities.endpoint.url");
		if (endpoint != null) {
			return endpoint;
		} else {
			throw new NullPointerException("Missing endpoint configuration.");
		}
	}

}
