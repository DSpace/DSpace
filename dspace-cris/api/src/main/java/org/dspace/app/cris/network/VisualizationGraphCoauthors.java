/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;


public class VisualizationGraphCoauthors extends AVisualizationGraphModeOne {

	

	public final static String CONNECTION_NAME = "coauthors";
	
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
    protected String buildExtraCustom(String extra)
    {
        // TODO Auto-generated method stub
        return null;
    }
}
