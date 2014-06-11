/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.network;

import java.util.LinkedList;
import java.util.List;

public class VisualizationGraphCoawards extends AVisualizationGraphModeFour
{

    public final static String CONNECTION_NAME = "cowinners";

    private static final String FIELD_QUERY = "network.awards_keyword";
    
    @Override
    public String getType()
    {
        return CONNECTION_NAME;
    }

    @Override
    public String getLineWidthToOverride()
    {
        return "1.5";
    }

    @Override
    public String getConnectionName()
    {
        return CONNECTION_NAME;
    }

    @Override
    protected String buildExtraCustom(String extra)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected String getFacetFieldQuery()
    {
        return FIELD_QUERY;
    }

    @Override
    protected List<String> getValues(String facetValue)
    {
        // e.g.
        String[] temp = facetValue.split("\\|#\\|#\\|#");
        String tt = temp[1];
        String[] temp2 = tt.split("###");
        List<String> result = new LinkedList<String>();
        for(String ss : temp2) {
            result.add(ss);
        }
        return result;
    }
    
}
