/*
 */
package org.datadryad.api;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.HandleManager;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

/**
 * Wraps a DSpace Item and provides Dryad-specific behaviors
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public abstract class DryadObject {
    private static final String DATE_ACCESSIONED_SCHEMA = "dc";
    private static final String DATE_ACCESSIONED_ELEMENT = "date";
    private static final String DATE_ACCESSIONED_QUALIFIER = "accessioned";

    // File/package relationships
    static final String RELATION_SCHEMA = "dc";
    static final String RELATION_ELEMENT = "relation";

    // File ispartof package
    static final String RELATION_ISPARTOF_QUALIFIER = "ispartof";

    // Package haspart file
    static final String RELATION_HASPART_QUALIFIER = "haspart";

    private static Logger log = Logger.getLogger(DryadObject.class);

    /* Considered using DCDate instead of a distinct SimpleDateFormat,
     * but there is no option to choose a granularity or formatter when
     * getting strings from dates. So we'd need our own formatter anyways
     */
    protected static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    protected Item item;
    protected DryadObject(Item item) {
        if(item == null) {
            throw new RuntimeException("Cannot instantiate a DryadObject with null item");
        }
        this.item = item;
    }

    @Override
    public boolean equals(Object otherObject) {
        if (otherObject == null) {
            return false;
        }
        if (getClass() != otherObject.getClass()) {
            return false;
        }
        final DryadObject otherDryadObject = (DryadObject)otherObject;
        return getItem().equals(otherDryadObject.getItem());
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 113 * hash + (this.getItem() != null ? this.getItem().hashCode() : 0);
        return hash;
    }

    protected static Collection collectionFromHandle(Context context, String handle) throws SQLException {
        DSpaceObject object = HandleManager.resolveToObject(context, handle);
        if(object.getType() == Constants.COLLECTION) {
            return (Collection)object;
        } else {
            return null;
        }
    }
    static Date[] extractDatesFromMetadata(DCValue[] values) {
        Date[] dates = new Date[values.length];
        for (int i = 0; i < values.length; i++) {
            try {
                dates[i] = parseDate(values[i].value);
            } catch (ParseException ex) {
                log.error("Exception parsing date from '" + values[i].value + "'", ex);
            }
        }
        return dates;
    }

    static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }

    static Date getEarliestDate(DCValue[] values) {
        Date[] embargoDates = extractDatesFromMetadata(values);
        return getEarliestDate(embargoDates);
    }

    static Date getEarliestDate(Date[] dates) {
        if (dates.length > 0) {
            Arrays.sort(dates);
            return dates[0];
        } else {
            return null;
        }
    }

    static Date parseDate(String dateString) throws ParseException {
        return DATE_FORMAT.parse(dateString);
    }

    /**
     * Return a set of related DryadObjects (packages or files) from the specified
     * collection. Since many metadata fields are package-only or file-only, this
     * allows us to easily get the related objects (or the object itself)
     * @param c a DSpace collection, e.g. Dryad Data Files or Dryad Data Packages
     * @return a set of objects, related to this object, and in the specified collection
     */
    public Set<DryadObject> getRelatedObjectsInCollection(Context context, Collection collection) throws SQLException {
        Set<DryadObject> relatedObjects = null;
        if(ArrayUtils.contains(this.getCollections(), collection)) {
            final DryadObject finalThis = this;
            relatedObjects = new HashSet<DryadObject>() {{
               add(finalThis);
           }};
        } else {
            relatedObjects = getRelatedObjects(context);
        }
        return relatedObjects;
    }

    /**
     * Get the set of related Dryad objects. Packages should return their files.
     * Files should return a set containing their package
     * @return The set of related objects, not including the object itself.
     */
    abstract Set<DryadObject> getRelatedObjects(final Context context) throws SQLException;
    protected final Collection[] getCollections() throws SQLException {
        return getItem().getCollections();
    }

    public Item getItem() {
        return item;
    }

    public String getIdentifier() {
        return DOIIdentifierProvider.getDoiValue(getItem());
    }

    protected void createIdentifier(Context context) throws SQLException, IdentifierException {
        if(getIdentifier() != null) {
            throw new IllegalStateException("Object already has an identifier");
        }
        IdentifierService service = new DSpace().getSingletonService(IdentifierService.class);
        try {
            service.reserve(context, getItem());
        } catch (AuthorizeException ex) {
            log.error("Authorize exception reserving identifier", ex);
        }
    }

    protected final void addToCollectionAndArchive(Collection collection) throws SQLException {
        getItem().setOwningCollection(collection);
        try {
            collection.addItem(getItem());
            getItem().setArchived(true);
            setDateAccessioned(new Date());
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
                dateAccessioned = parseDate(firstDate.value);
            } catch (ParseException ex) {
                log.error("Error parsing " + firstDate.value, ex);
            }
        }
        return dateAccessioned;
    }

    public void setDateAccessioned(Date dateAccessioned) throws SQLException {
        String dateAccessionedString = formatDate(dateAccessioned);
        getItem().clearMetadata(DATE_ACCESSIONED_SCHEMA, DATE_ACCESSIONED_ELEMENT, DATE_ACCESSIONED_QUALIFIER, null);
        getItem().addMetadata(DATE_ACCESSIONED_SCHEMA, DATE_ACCESSIONED_ELEMENT, DATE_ACCESSIONED_QUALIFIER, null, dateAccessionedString);
        try {
            getItem().update();
        } catch (AuthorizeException ex) {
            log.error("Authorize exception setting date accessioned", ex);
        }
    }
}
