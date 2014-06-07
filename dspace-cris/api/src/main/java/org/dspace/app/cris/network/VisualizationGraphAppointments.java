/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;

import org.dspace.core.ConfigurationManager;


public class VisualizationGraphAppointments extends AVisualizationGraphModeTwo  {
	
    public final static String CONNECTION_NAME = "appointments";
	private static final String FIELD_QUERY = "network.apointment_keyword";
		
	@Override
	public String getType() {
		return CONNECTION_NAME;
	}

	@Override
	public String getLineWidthToOverride() {
		return "1.5";
	}


	@Override
	public String getConnectionName() {
		return CONNECTION_NAME;
	}

	@Override
	protected String getFacetFieldQuery() {
		return FIELD_QUERY;
	}

	@Override
	public String getFacet(String value) {
		return FA_VALUE;
	}

    @Override
    protected String buildExtraCustom(String extra)
    {
        // TODO Auto-generated method stub
        return null;
    }

    protected String getQuery()
    {
        return ConfigurationManager.getProperty(NetworkPlugin.CFG_MODULE, "network.query.appointment");
    }

    @Override
    protected boolean useJoin()
    {        
        return true;
    }
}
