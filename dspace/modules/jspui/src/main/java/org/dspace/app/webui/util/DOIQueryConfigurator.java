package org.dspace.app.webui.util;


public class DOIQueryConfigurator extends ASolrQueryConfigurator {
	
	private static final String componentID = "doi";
	
	@Override
	protected String getComponentIdentifier() {
		return componentID;
	}

	
}
