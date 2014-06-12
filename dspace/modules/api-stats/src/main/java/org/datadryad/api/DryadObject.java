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
 * Wraps a DSpace Item and provides Dryad-specific behaviors
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class DryadObject {
    private static final String PUBLICATION_NAME_SCHEMA = "prism";
    private static final String PUBLICATION_NAME_ELEMENT = "publicationName";
    private static final String PUBLICATION_NAME_QUALIFIER = null;
    private static Logger log = Logger.getLogger(DryadObject.class);

    protected Item item;
    protected DryadObject(Item item) {
        if(item == null) {
            throw new RuntimeException("Cannot instantiate a DryadObject with null item");
        }
        this.item = item;
    }
    protected static Collection collectionFromHandle(Context context, String handle) throws SQLException {
        DSpaceObject object = HandleManager.resolveToObject(context, handle);
        if(object.getType() == Constants.COLLECTION) {
            return (Collection)object;
        } else {
            return null;
        }
    }

    public Item getItem() {
        return item;
    }

    public String getIdentifier() {
        return DOIIdentifierProvider.getDoiValue(getItem());
    }

    public void setPublicationName(String publicationName) throws SQLException {
        getItem().clearMetadata(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, null);
        getItem().addMetadata(PUBLICATION_NAME_SCHEMA, PUBLICATION_NAME_ELEMENT, PUBLICATION_NAME_QUALIFIER, null, publicationName);
        try {
            getItem().update();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception setting publication name", ex);
        }
    }

    protected final void addToCollectionAndArchive(Collection collection) throws SQLException {
        getItem().setOwningCollection(collection);
        try {
            collection.addItem(getItem());
            getItem().setArchived(true);
            getItem().update();
            collection.update();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception adding to collection", ex);
        } catch (IOException ex) {
            log.error("IO exception adding to collection", ex);
        }
    }
}
