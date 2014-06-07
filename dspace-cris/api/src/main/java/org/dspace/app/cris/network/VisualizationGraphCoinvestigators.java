/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;

import org.dspace.app.cris.model.CrisConstants;


public class VisualizationGraphCoinvestigators extends AVisualizationGraphModeThree  {

	public final static String CONNECTION_NAME = "coinvestigators";
	private static final String[] FIELDS = {"search.resourceid", "search.resourcetype",
                "objectpeople_filter", "crisproject.title", "crisproject.code"};
	private static final String RESOURCETYPE = String.valueOf(CrisConstants.PROJECT_TYPE_ID);
	
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

    @Override
    protected String getResourceType()
    {
        return RESOURCETYPE;
    }

    @Override
    protected String[] getFields()
    {
        return FIELDS;
    }
  
}
