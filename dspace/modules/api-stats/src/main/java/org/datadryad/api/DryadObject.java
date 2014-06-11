/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOIIdentifierProvider;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class DryadObject {
    private static final String PUBLICATION_NAME_SCHEMA = "prism";
    private static final String PUBLICATION_NAME_ELEMENT = "publicationName";
    private static final String PUBLICATION_NAME_QUALIFIER = null;
    private static Logger log = Logger.getLogger(DryadObject.class);

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

    public void setPublicationName(String publicationName) throws SQLException {
        getItem().clearMetadata(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, null);
        getItem().addMetadata(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, null, publicationName);
        try {
            getWorkspaceItem().update();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception setting publication name", ex);
        } catch (IOException ex) {
            log.error("IO exception setting publication name", ex);
        }
    }

}
