/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.cris.configuration;

import org.dspace.app.cris.model.ACrisObject;
import org.dspace.core.Context;

public interface RelationPreferenceExtraAction
{
    
    public void setRelationName(String name);
    
    public String getRelationName();

    public boolean executeExtraAction(Context context, ACrisObject cris,
            int itemID, String previousAction, int previousPriority,
            String action, int priority);
}
