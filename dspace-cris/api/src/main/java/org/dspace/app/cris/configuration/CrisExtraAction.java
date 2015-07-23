/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import java.util.List;

import org.dspace.app.cris.integration.RPAuthority;
import org.dspace.app.cris.model.ACrisObject;
import org.dspace.core.Context;

public class CrisExtraAction implements RelationPreferenceExtraAction
{
    private String relationName;
    
    private String authorityName;

    private List<String> metadata;
    
//    @Required
    public void setRelationName(String relationName)
    {
        this.relationName = relationName;
    }

    public void setAuthorityName(String authorityName)
    {
        this.authorityName = authorityName;
    }

    public void setMetadata(List<String> metadata)
    {
        this.metadata = metadata;
    }

    public String getRelationName()
    {
        return relationName;
    }

    public String getAuthorityName()
    {
        if(this.authorityName==null) {
            this.authorityName = RPAuthority.RP_AUTHORITY_NAME;
        }
        return authorityName;
    }

    public List<String> getMetadata()
    {
        return metadata;
    }

    @Override
    public boolean executeExtraAction(Context context, ACrisObject cris,
            int itemID, String previousAction, int previousPriority,
            String action, int priority)
    {
    	return false;
    }
}
