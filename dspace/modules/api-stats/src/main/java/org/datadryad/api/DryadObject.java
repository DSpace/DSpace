/*
 */
package org.datadryad.api;

import java.sql.SQLException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.doi.DOI;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOIIdentifierProvider;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class DryadObject {
    private static final String PUBLICATION_NAME_SCHEMA = "prism";
    private static final String PUBLICATION_NAME_ELEMENT = "publicationName";
    private static final String PUBLICATION_NAME_QUALIFIER = Item.ANY;

    protected WorkspaceItem workspaceItem;
    protected DryadObject(WorkspaceItem workspaceItem) {
        this.workspaceItem = workspaceItem;
    }
    protected static Collection collectionFromHandle(Context context, String handle) throws SQLException {
        DSpaceObject object = HandleManager.resolveToObject(context, handle);
        if(object.getType() == Constants.COLLECTION) {
            return (Collection)object;
        } else {
            return null;
        }
    }

    public WorkspaceItem getWorkspaceItem() {
        return workspaceItem;
    }

    public Item getItem() {
        if(workspaceItem != null) {
            return workspaceItem.getItem();
        }
        return null;
    }

    public String getIdentifier() {
        return DOIIdentifierProvider.getDoiValue(getItem());
    }

    public void setPublicationName(String publicationName) {
        getItem().clearMetadata(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, null);
        getItem().addMetadata(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, null, publicationName);
    }

}
