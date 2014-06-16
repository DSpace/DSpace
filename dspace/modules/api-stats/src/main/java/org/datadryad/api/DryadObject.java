/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
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

    private static final String DATE_ACCESSIONED_SCHEMA = "dc";
    private static final String DATE_ACCESSIONED_ELEMENT = "date";
    private static final String DATE_ACCESSIONED_QUALIFIER = "accessioned";

    private static Logger log = Logger.getLogger(DryadObject.class);
    protected static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

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

    public Date getDateAccessioned() {
        Date dateAccessioned = null;
        DCValue[] metadata = item.getMetadata(DATE_ACCESSIONED_SCHEMA, DATE_ACCESSIONED_ELEMENT, DATE_ACCESSIONED_QUALIFIER, Item.ANY);
        if(metadata.length > 0) {
            DCValue firstDate = metadata[0];
            try {
                dateAccessioned = DATE_FORMAT.parse(firstDate.value);
            } catch (ParseException ex) {
                log.error("Error parsing " + firstDate.value, ex);
            }
        }
        return dateAccessioned;
    }
}
