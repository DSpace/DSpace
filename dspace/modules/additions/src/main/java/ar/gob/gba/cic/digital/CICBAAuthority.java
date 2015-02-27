package ar.gob.gba.cic.digital;

import ar.edu.unlp.sedici.dspace.authority.SPARQLAuthorityProvider;


public abstract class CICBAAuthority extends SPARQLAuthorityProvider {
	
	protected static final String NS_CIC = "http://www.cic.gba.gov.ar/ns#";

	public CICBAAuthority() {
		super("http://digital.cic.gba.gob.ar/auth/sparql");
	}

}
